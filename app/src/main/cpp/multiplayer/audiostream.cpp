#include <memory>
#include <unistd.h>
#include "audiostream.h"
#include "log.h"
#include "game/game.h"
#include "../main.h"
#include "../vendor/bass/bass.h"

extern CGame* pGame;

HSTREAM bassStream = 0;
char g_szAudioStreamUrl[256+1];
float g_fAudioStreamX;
float g_fAudioStreamY;
float g_fAudioStreamZ;
float g_fAudioStreamRadius;
bool g_audioStreamUsePos;
bool g_bAudioStreamStop;
bool g_bAudioStreamThreadWorked;

void* audioStreamThread(void*)
{
    g_bAudioStreamThreadWorked = true;
    if (bassStream)
    {
        BASS_ChannelStop(bassStream);
        bassStream = 0;
    }

    bassStream = BASS_StreamCreateURL(g_szAudioStreamUrl, 0, 0x940000, 0,0);
    BASS_ChannelPlay(bassStream, 0);

    BASS_ChannelSetSync(bassStream, 4, 0, 0, 0);
    BASS_ChannelSetSync(bassStream, 12, 0, 0, 0);

    while (!g_bAudioStreamStop)
    {
        // ..
        usleep(2000);
    }

    BASS_ChannelStop(bassStream);
    bassStream = 0;
    g_bAudioStreamThreadWorked = false;
    pthread_exit(0);
}

CAudioStream::CAudioStream()
{
	m_bInited = false;
}

bool CAudioStream::Initialize()
{
    bassStream = 0;
    //BASS_Free();
    //if (!BASS_Init(-1, 44100, 0)) return false;		//if (!BASS_Init(-1, 48000, 0)) return false;

    BASS_SetConfigPtr(16, "SA-MP/0.3");
    //BASS_SetConfig(5, ) volume
    BASS_SetConfig(21, 1);			// BASS_CONFIG_NET_PLAYLIST
    BASS_SetConfig(11, 10000);		// BASS_CONFIG_NET_TIMEOUT

    m_bInited = true;
    return true;
}

bool CAudioStream::Play(const char* szUrl, float fX, float fY, float fZ, float fRadius, bool bUsePos) { FLog("Play: %s", szUrl); if (!m_bInited) return false; Stop(true); if (bassStream) { BASS_ChannelStop(bassStream); bassStream = 0; } memset(g_szAudioStreamUrl, 0, sizeof(g_szAudioStreamUrl)); strncpy(g_szAudioStreamUrl, szUrl, 256); g_fAudioStreamX = fX; g_fAudioStreamY = fY; g_fAudioStreamZ = fZ; g_fAudioStreamRadius = fRadius; g_audioStreamUsePos = bUsePos; g_bAudioStreamStop = false; pthread_t thread; pthread_create(&thread, 0, audioStreamThread, 0); return true; }

void CAudioStream::Process(float listenerX, float listenerY, float listenerZ)
{
    if (!bassStream) return;

    float dx = listenerX - g_fAudioStreamX;
    float dy = listenerY - g_fAudioStreamY;
    float dz = listenerZ - g_fAudioStreamZ;
    float distance = sqrt(dx*dx + dy*dy + dz*dz);

    if (distance > g_fAudioStreamRadius)
    {
        if (!g_bAudioStreamStop)
        {
            FLog("Stop (distance exceeded): %s", g_szAudioStreamUrl);
            g_bAudioStreamStop = true;
            BASS_ChannelStop(bassStream);
        }
    }
    else
    {
        if (g_bAudioStreamStop)
        {
            FLog("Play (within distance): %s", g_szAudioStreamUrl);
            g_bAudioStreamStop = false;
            BASS_ChannelPlay(bassStream, FALSE);
        }

        if (g_audioStreamUsePos)
        {
            BASS_3DVECTOR pos = { g_fAudioStreamX, g_fAudioStreamY, g_fAudioStreamZ };
            BASS_3DVECTOR orient = {0.0f,0.0f,0.0f};
            BASS_3DVECTOR vel = {0.0f,0.0f,0.0f};
            BASS_ChannelSet3DPosition(bassStream, &pos, &orient, &vel);
            BASS_ChannelSet3DAttributes(bassStream, BASS_3DMODE_NORMAL, std::max(g_fAudioStreamRadius*0.1f,5.0f), g_fAudioStreamRadius, -1,-1,0);
            BASS_Apply3D();
        }
    }
}

// 0.3.7
bool CAudioStream::Stop(bool bWaitThread)
{
    //FLog("Stop: %s", g_szAudioStreamUrl);

    if (!m_bInited || !g_bAudioStreamThreadWorked) {
        //FLog("Stop aborted: not inited or thread not working (m_bInited=%d, g_bAudioStreamThreadWorked=%d)", m_bInited, g_bAudioStreamThreadWorked);
    }

    g_bAudioStreamStop = true;
    //FLog("g_bAudioStreamStop set to true");

    if (bWaitThread)
    {
        //FLog("Waiting for audio thread to stop...");
        int waitCount = 0;
        while (g_bAudioStreamThreadWorked)
        {
            usleep(200);
            if (++waitCount % 5000 == 0)
                FLog("Still waiting for audio thread to stop...");
        }
        //FLog("Audio thread stopped");
    }

    if (bassStream)
    {
        //FLog("Freeing BASS stream");
        BASS_StreamFree(bassStream);
        bassStream = 0;
    }

    //FLog("Stop completed successfully");
    return true;
}
#include "main.h"
#include "settings.h"
#include "vendor/inih/cpp/INIReader.h"

#include <algorithm>
#include "vendor/SimpleIni/SimpleIni.h"
#include "game/game.h"

extern CGame *pGame;
extern "C" void SetClassicHudMode(bool classic);

CSettings::CSettings()
{
    FLog("Loading settings..");

    char buff[0x7F];
    sprintf(buff, "%sSAMP/settings.ini", g_pszStorage);

    INIReader reader(buff);

    if(reader.ParseError() < 0)
    {
        FLog("Error: can't load %s", buff);
        std::terminate();
        return;
    }

    // client
    size_t length = 0;
    sprintf(buff, "__android_%d%d", rand() % 1000, rand() % 1000);
    length = reader.Get("client", "name", buff).copy(m_Settings.szNickName, 24);
    m_Settings.szNickName[length] = '\0';
    length = reader.Get("client", "host", "127.0.0.1").copy(m_Settings.szHost, MAX_SETTINGS_STRING);
    m_Settings.szHost[length] = '\0';
    length = reader.Get("client", "password", "").copy(m_Settings.szPassword, MAX_SETTINGS_STRING);
    m_Settings.szPassword[length] = '\0';
    length = reader.Get("client", "version", "0.3.7").copy(m_Settings.szVersion, MAX_SETTINGS_STRING);
    m_Settings.szVersion[length] = '\0';
    m_Settings.iPort = reader.GetInteger("client", "port", 7777);
    m_Settings.bAutoAim = reader.GetBoolean("client", "autoaim", false);

    // debug
    m_Settings.bDebug = reader.GetBoolean("debug", "debug", false);
    m_Settings.bOnline = reader.GetBoolean("debug", "online", false);

    // gui
    length = reader.Get("gui", "Font", "arial.ttf").copy(m_Settings.szFont, MAX_SETTINGS_STRING);
    m_Settings.szFont[length] = '\0';
    float rawFont = reader.GetReal("gui", "FontSize", 20.0f);
    if (rawFont > 36.0f || rawFont < 14.0f) rawFont = 20.0f;
    m_Settings.fFontSize = rawFont;
    m_Settings.iFontOutline = reader.GetInteger("gui", "FontOutline", 2);

    // chat
    float rawChatX = reader.GetReal("gui", "ChatPosX", 0.015f);
    float rawChatY = reader.GetReal("gui", "ChatPosY", 0.025f);
    float rawChatW = reader.GetReal("gui", "ChatSizeX", 0.45f);

    m_Settings.fChatPosX = rawChatX;
    m_Settings.fChatPosY = rawChatY;
    m_Settings.fChatSizeX = rawChatW;
    m_Settings.fChatSizeY = 0.0f;
    m_Settings.iChatMaxMessages = 250;

    // spawnscreen
    m_Settings.fSpawnScreenPosX = reader.GetReal("gui", "SpawnScreenPosX", 660.0f);
    m_Settings.fSpawnScreenPosY = reader.GetReal("gui", "SpawnScreenPosY", 950.0f);
    m_Settings.fSpawnScreenSizeX = reader.GetReal("gui", "SpawnScreenSizeX", 600.0f);
    m_Settings.fSpawnScreenSizeY = reader.GetReal("gui", "SpawnScreenSizeY", 100.0f);

    // nametags
    m_Settings.fHealthBarWidth = reader.GetReal("gui", "HealthBarWidth", 100.0f);
    m_Settings.fHealthBarHeight = reader.GetReal("gui", "HealthBarHeight", 10.0f);

    // scoreboard
    m_Settings.fScoreBoardSizeX = reader.GetReal("gui", "ScoreBoardSizeX", 846.0f);
    m_Settings.fScoreBoardSizeY = reader.GetReal("gui", "ScoreBoardSizeY", 614.0f);

    // passenger
    m_Settings.bPassengerUseTexture = reader.GetBoolean("gui", "PassengerUseTexture", true);
    m_Settings.fPassengerTextureSize = reader.GetReal("gui", "PassengerTextureSize", 30.0f);
    m_Settings.fPassengerTextureX = reader.GetReal("gui", "PassengerTexturePosX", 120.0f);
    m_Settings.fPassengerTextureY = reader.GetReal("gui", "PassengerTexturePosY", 430.0f);

    m_Settings.iDialog = reader.GetBoolean("gui", "Dialog", true);

    m_Settings.bVoiceChatEnable = reader.GetBoolean("gui", "VoiceChatEnable", false);
    m_Settings.iVoiceChatKey = reader.GetInteger("gui", "VoiceChatKey", 66);
    m_Settings.fVoiceChatSize = reader.GetReal("gui", "VoiceChatSize", 30.0f);
    m_Settings.fVoiceChatPosX = reader.GetReal("gui", "VoiceChatPosX", 1520.0f);
    m_Settings.fVoiceChatPosY = reader.GetReal("gui", "VoiceChatPosY", 480.0f);

    m_Settings.iAndroidKeyboard = reader.GetBoolean("gui", "androidkeyboard", false);
    m_Settings.iFirstPerson = reader.GetBoolean("gui", "firstperson", true);
    m_Settings.iCutout = reader.GetBoolean("gui", "cutout", false);
    m_Settings.iFPSCounter = reader.GetBoolean("gui", "fps", false);
    m_Settings.iFPSCount = reader.GetInteger("gui", "FPSLimit", 60);
    m_Settings.iHPArmourText = reader.GetBoolean("gui", "hparmourtext", false);
    m_Settings.iOutfitGuns = reader.GetBoolean("gui", "outfitguns", false);
    m_Settings.iPCMoney = reader.GetBoolean("gui", "pcmoney", false);
    m_Settings.iRadarRect = reader.GetBoolean("gui", "radarrect", false);
    m_Settings.iSkyBox = reader.GetBoolean("gui", "skybox", false);
    m_Settings.iSnow = reader.GetBoolean("gui", "snow", false);

    // FPS info setting (новое поле)
    m_Settings.bFPSInfo = reader.GetBoolean("gui", "FPSInfo", false);

    // hud radar — old launcher stores radar in a 640x480 space with default 50/70
    float radarX = reader.GetReal("hud", "hud_position_x_6", -1.0f);
    float radarY = reader.GetReal("hud", "hud_position_y_6", -1.0f);
    float radarScale = reader.GetReal("hud", "hud_scale_x_6", -1.0f);

    m_Settings.fHudRadarX = -1.0f;
    m_Settings.fHudRadarY = -1.0f;
    m_Settings.fHudRadarScale = -1.0f;
    m_Settings.fHudRadarScaleY = -1.0f;

    m_Settings.fButtonAttackX = reader.GetReal("gui", "ButtonAttackX", -1.0f);
    m_Settings.fButtonAttackY = reader.GetReal("gui", "ButtonAttackY", -1.0f);
    m_Settings.fButtonAttackSize = reader.GetReal("gui", "ButtonAttackSize", 100.0f);
    m_Settings.fButtonAttackSensitivity = reader.GetReal("gui", "ButtonAttackSensitivity", 0.0f);
    m_Settings.iButtonAttackNew = reader.GetInteger("gui", "buttonAttackNew", 1);

    std::string hudType = reader.Get("gui", "hud_type", reader.Get("client", "hud_type", "glory"));
    SetClassicHudMode(hudType == "classic");

    FLog("Settings loaded (hud_type=%s).", hudType.c_str());
}

// Добавляем деструктор
CSettings::~CSettings()
{
    SaveSettings();
}

void CSettings::SaveSettings()
{
    char buff[0x7F];
    sprintf(buff, "%sSAMP/settings.ini", g_pszStorage);

    CSimpleIniA ini;
    ini.SetUnicode(true);

    // Загружаем существующий файл
    SI_Error rc = ini.LoadFile(buff);
    if (rc < 0) {
        FLog("Error loading settings file for saving");
        return;
    }

    // Обновляем настройки
    ini.SetBoolValue("gui", "fpsinfo", m_Settings.bFPSInfo);

    // Сохраняем
    rc = ini.SaveFile(buff);
    if (rc < 0) {
        FLog("Error saving settings file");
    } else {
        FLog("Settings saved.");
    }
}

void CSettings::SetFPSInfo(bool enabled)
{
    m_Settings.bFPSInfo = enabled;
    SaveSettings();
}

bool CSettings::GetFPSInfo() const
{
    return m_Settings.bFPSInfo;
}
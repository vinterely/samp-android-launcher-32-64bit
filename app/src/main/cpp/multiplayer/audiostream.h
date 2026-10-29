#pragma once

#include "vendor/bass/bass.h"
#include "game/Enums/eSoundAttachedTo.h"

class CAudioStream
{
public:
	CAudioStream();

	bool Initialize();

	void Process(float listenerX, float listenerY, float listenerZ);

	bool Play(const char* szUrl, float fX, float fY, float fZ, float fRadius, bool bUsePos);
	bool Stop(bool bWaitThread);



private:
	bool m_bInited;
};
//
// Created by GLORY TEAM.
//

#ifndef GLORY_TIMECYCLE_H
#define GLORY_TIMECYCLE_H


#include "ColourSet.h"

class CTimeCycle {

public:
    static inline CColourSet m_CurrentColours;
    static inline RwRGBA m_BelowHorizonGrey;

public:
    static void InjectHooks();
};


#endif //GLORY_TIMECYCLE_H

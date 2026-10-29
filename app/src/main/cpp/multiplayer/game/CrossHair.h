//
// Created by GLORY TEAM.
//

#ifndef GLORY_CROSSHAIR_H
#define GLORY_CROSSHAIR_H


#include "RW/rwcore.h"
#include "sprite2d.h"

class CCrossHair {
public:
    static inline CSprite2d* pCircleTex;
    static inline bool m_UsedCrossHair{};

public:
    static void Init();
    static void Render();
};


#endif //GLORY_CROSSHAIR_H

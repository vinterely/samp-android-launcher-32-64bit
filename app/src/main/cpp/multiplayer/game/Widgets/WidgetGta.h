//
// Created by GLORY TEAM.
//

#pragma once

#include <cstdint>
#include "game/sprite2d.h"
#include "game/Core/Rect.h"
#include "game/rgba.h"
#include "game/Enums/HIDMapping.h"
#include "../common.h"

class CWidgetGta {
public:
    uintptr* vtable;
    HIDMapping m_HIDMapping;
    CSprite2d m_Sprite;
    float m_fOriginX;
    float m_fOriginY;
    float m_fScaleX;
    float m_fScaleY;
    float m_fFadeRate;
    CRect m_RectScreen;
    float m_fExtraPaddingScale;
    CRect m_RectPaddedScreen;
    float m_fTapHoldTime;
    bool m_bTaphold;
    CRGBA m_Color;
    bool m_bEnabled;
    bool m_bCachedEnabled;
    uint8_t pad0;
    float m_fTapHistory[10];
    int m_nTouchIndex;
    int m_nFrameCount;
    unsigned int m_nFlags;
    float m_fUserData;
    uintptr_t *m_pHoldEffect;
    float m_fHoldEffectPeriod;

public:
    static void InjectHooks();

    void SetEnabled(bool bEnabled);
    void SetTexture(const char* name);
    bool IsReleased(CVector2D *pVecOut);
    bool IsTouched(CVector2D *pVecOut);
    //void SetPositionAndScale(float originX, float originY, float scaleX, float scaleY);
};
VALIDATE_SIZE(CWidgetGta, (VER_x32 ? 0x90 : 0xA8));

class CGtaWidgets {
public:
    static CWidgetGta* m_pWidgets[190];

    static void setEnabled(CWidgetGta* thiz, bool bEnabled);
    static bool IsTouched(CWidgetGta* widget, CVector2D* pVecOut);
    static void setPositionAndScale(CWidgetGta* widget, float originX, float originY, float scaleX, float scaleY);
    static void init();
    bool IsReleased(CWidgetGta* widget, CVector2D* pVecOut);
};
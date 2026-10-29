//
// Created by GLORY TEAM.
//
#pragma once

#include "main.h"

class CObject;

class Skybox {
public:
    static void Init();
    static void Process();
    static void SetTexture(const char* texName);
    static bool IsNeedRender();
    static CObject* GetSkyObject();

private:
    static void ReTexture();
    static uintptr_t RwFrameForAllObjectsCallback(uintptr_t object, CObject* pObject);

private:
    static CObject* m_pSkyBox;
    static bool m_bNeedRender;
    static float m_fRotSpeed;
    static float m_fRotate;
};
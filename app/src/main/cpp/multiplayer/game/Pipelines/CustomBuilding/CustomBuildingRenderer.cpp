//
// Created by GLORY TEAM.
//

#include "CustomBuildingRenderer.h"
#include "../vendor/armhook/patch.h"

bool CCustomBuildingRenderer::Initialise() {
    return CHook::CallFunction<bool>("_ZN23CCustomBuildingRenderer10InitialiseEv");
}

void CCustomBuildingRenderer::Update() {
    CHook::CallFunction<void>(g_libGTASA + (VER_x32 ? 0x2CA3A4 + 1 : 0x38B6DC));
}

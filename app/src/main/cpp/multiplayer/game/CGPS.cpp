//
// Created by GLORY TEAM.
//
#include "CGPS.hpp"
#include "gui/gui.h"
#include "../game/game.h"
#include "../game/playerped.h"
#include "../net/netgame.h"
#include "net/localplayer.h"

extern CNetGame* pNetGame;
extern UI *pGUI;
extern CGame *pGame;

GPS::GPS() {
    enabled = false;
    to = CVector{0.0f, 0.0f, 0.0f};

    marker = CUtil::LoadTextureFromDB("txd", "radar_waypoint");
}

void GPS::Set(CVector pos, bool toggle) {
    to = pos;
    enabled = toggle;
}

void GPS::RenderMarker() {
    if (!enabled || !marker || !marker->raster || !pNetGame) return;

    CLocalPlayer* pLocalPlayer = pNetGame->GetPlayerPool()->GetLocalPlayer();
    if (!pLocalPlayer) return;

    CPlayerPed* pPlayerPed = pLocalPlayer->GetPlayerPed();
    if (!pPlayerPed || !pPlayerPed->m_pPed) return;

    CVector targetPos = to;
    float camDist = pPlayerPed->m_pPed->GetDistanceFromCamera();
    targetPos.z += 0.25f + (camDist * 0.0475f);

    CVector vecScreen;
    ((void (*)(CVector*, CVector*, float*, float*, bool, bool))(g_libGTASA + 0x6E9DF8))(
            &targetPos, &vecScreen, 0, 0, 0, 0
    );

    if (vecScreen.z < 1.0f) return;

    float size = 46.0f;
    float x = vecScreen.x - size * 0.5f;
    float y = vecScreen.y - size * 0.5f;

    auto drawList = ImGui::GetBackgroundDrawList();
    if (!drawList) return;

    drawList->AddImage((ImTextureID)marker->raster, ImVec2(x, y), ImVec2(x + size, y + size));

    if (!pGame || !pGame->FindPlayerPed() || !pGame->FindPlayerPed()->m_pPed) return;

    CVector playerPos = pGame->FindPlayerPed()->m_pPed->m_matrix->m_pos;
    float dist = sqrtf(
            (playerPos.x - targetPos.x) * (playerPos.x - targetPos.x) +
            (playerPos.y - targetPos.y) * (playerPos.y - targetPos.y) +
            (playerPos.z - targetPos.z) * (playerPos.z - targetPos.z)
    );

    char szText[32];
    if (dist < 1000.0f)
        snprintf(szText, sizeof(szText), "%.2f m", dist);
    else
        snprintf(szText, sizeof(szText), "%.2f km", dist / 1000.0f);

    // Уменьшаем текст через масштаб шрифта
    ImFont* font = ImGui::GetFont();
    float oldSize = font->FontSize;
    font->FontSize = 100.0f;
    font->Scale = 1.0f;

    ImVec2 textPos = ImVec2(vecScreen.x - 30, vecScreen.y + 40);
    drawList->AddText(textPos, IM_COL32(255, 255, 255, 255), szText);

    font->FontSize = oldSize;
    font->Scale = 1.0f;
}

void GPS::DoPathDraw() {
    RenderMarker();
}

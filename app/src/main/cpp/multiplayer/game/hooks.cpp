#include <GLES2/gl2.h>

#include "../main.h"

#include "../vendor/armhook/patch.h"

#include "game.h"

#include "../net/netgame.h"

#include "../gui/gui.h"

#include "Textures/TextureDatabase.h"

#include "Textures/TextureDatabaseEntry.h"

#include "Textures/TextureDatabaseRuntime.h"

#include "Scene.h"

#include "sprite2d.h"

#include "Entity/PlayerPedGta.h"

#include "Pools.h"

#include "java/jniutil.h"

#include "game/Models/ModelInfo.h"

#include "MatrixLink.h"

#include "MatrixLinkList.h"

#include "game/Collision/Collision.h"

#include "TxdStore.h"

#include "util/CUtil.h"

#include "Coronas.h"

#include "multitouch.h"

#include "Streaming.h"

#include "References.h"

#include "VisibilityPlugins.h"

#include "game/Animation/AnimManager.h"

#include "FileLoader.h"

#include "Renderer.h"

#include "CrossHair.h"

#include "World.h"

#include "util.h"

#include "Mobile/MobileMenu/MobileMenu.h"

#include "../settings.h"

#include "../diag.h"

#include "../gui/samp_widgets/chat.h"



extern UI* pUI;

extern CGame* pGame;

extern CNetGame *pNetGame;

extern CSettings* pSettings;

extern Chat* pChat;

extern MaterialTextGenerator* pMaterialTextGenerator;

extern "C" void CheckWannaOpenMap();
extern "C" void TriggerOpenMap();



uint8_t byteInternalPlayer = 0;

CPedGTA* dwCurPlayerActor = 0;

uint8_t byteCurPlayer = 0;

uint8_t byteCurDriver = 0;



extern "C" uintptr_t get_lib()

{

    return g_libGTASA;

}

// 0.3.7

PLAYERID FindPlayerIDFromGtaPtr(CEntityGTA* pEntity)

{

    if (pEntity == nullptr) return INVALID_PLAYER_ID;



    CPlayerPool* pPlayerPool = pNetGame->GetPlayerPool();

    CVehiclePool* pVehiclePool = pNetGame->GetVehiclePool();



    PLAYERID PlayerID = pPlayerPool->FindRemotePlayerIDFromGtaPtr((CPedGTA*)pEntity);

    if (PlayerID != INVALID_PLAYER_ID) return PlayerID;



    VEHICLEID VehicleID = pVehiclePool->FindIDFromGtaPtr((CVehicleGTA*)pEntity);

    if (VehicleID != INVALID_VEHICLE_ID)

    {

        for (PLAYERID i = 0; i < MAX_PLAYERS; i++)

        {

            CRemotePlayer* pRemotePlayer = pPlayerPool->GetAt(i);

            if (pRemotePlayer && pRemotePlayer->CurrentVehicleID() == VehicleID) {

                return i;

            }

        }

    }



    return INVALID_PLAYER_ID;

}

// 0.3.7

PLAYERID FindActorIDFromGtaPtr(CPedGTA* pPed)

{

    if (pPed) {

        return pNetGame->GetActorPool()->FindIDFromGtaPtr(pPed);

    }



    return INVALID_PLAYER_ID;

}



/* =============================================================================== */



void RenderEffects() {

//	RenderEffects();

    CHook::CallFunction<void>(g_libGTASA + (VER_x32 ? 0x0059DA40 + 1 : 0x6C1D6C));

    CHook::CallFunction<void>(g_libGTASA + (VER_x32 ? 0x005BE914 + 1 : 0x6E2FB4));

//    CRopes::Render();

//    CGlass::Render();

    CHook::CallFunction<void>(g_libGTASA + (VER_x32 ? 0x005A6BC8 + 1 : 0x6CA5D0));

    CVisibilityPlugins::RenderReallyDrawLastObjects();

    CCoronas::Render();



    // FIXME

    CCamera& TheCamera = *reinterpret_cast<CCamera*>(g_libGTASA + (VER_x32 ? 0x00951FA8 : 0xBBA8D0));

    auto g_fx = *(uintptr_t *) (g_libGTASA + (VER_x32 ? 0x00820520 : 0xA062A8));

    CHook::CallFunction<void>(g_libGTASA + (VER_x32 ? 0x00363DF0 + 1 : 0x433F54), &g_fx, TheCamera.m_pRwCamera, false);



    CHook::CallFunction<void>(g_libGTASA + (VER_x32 ? 0x005CBBAC + 1 : 0x6F054C));

    CHook::CallFunction<void>(g_libGTASA + (VER_x32 ? 0x0059BF84 + 1 : 0x6C0268));



    //CClouds - hmm

    CHook::CallFunction<void>(g_libGTASA + (VER_x32 ? 0x005A1C38 + 1 : 0x6C552C));

    //   CClouds::VolumetricCloudsRender();

////    if (CHeli::NumberOfSearchLights || CTheScripts::NumberOfScriptSearchLights) {

////        CHeli::Pre_SearchLightCone();

////        CHeli::RenderAllHeliSearchLights();

////        CTheScripts::RenderAllSearchLights();

////        CHeli::Post_SearchLightCone();

////    }

    CHook::CallFunction<void>(g_libGTASA + (VER_x32 ? 0x005E3390 + 1 : 0x708DF0));

////    if (CReplay::Mode != MODE_PLAYBACK && !CPad::GetPad(0)->DisablePlayerControls) {

////        FindPlayerPed()->DrawTriangleForMouseRecruitPed();

////    }

    CHook::CallFunction<void>(g_libGTASA + (VER_x32 ? 0x005C0B14 + 1 : 0x6E50CC));

//    //CVehicleRecording::Render();

    CHook::CallFunction<void>(g_libGTASA + (VER_x32 ? 0x005B19D0 + 1 : 0x6D6068));

//    //CRenderer::RenderFirstPersonVehicle();

    CHook::CallFunction<void>(g_libGTASA + (VER_x32 ? 0x005B5F78 + 1 : 0x6DA2B8));



    //DebugModules::Render3D();

}



extern CJavaWrapper *pJavaWrapper;

extern "C" bool IsClassicHudMode();



void ShowHud()

{

    if (Diag_IsNoWeaponHud()) return;

    if (!pNetGame || !pGame) return;

    CPlayerPool *pPlayerPool = pNetGame->GetPlayerPool();

    if (!pPlayerPool) return;

    CPlayerPed *pPed = pGame->FindPlayerPed();

    if (!pPed) return;

    CWeapon *pWeapon = pPed->GetCurrentWeaponSlot();

    if (!pWeapon) return;



    if (IsClassicHudMode())

    {

        pGame->DisplayHUD(true);

        return;

    }



    if (!pJavaWrapper) return;

    pGame->DisplayHUD(false);

    pJavaWrapper->UpdateHudInfo(

            pPed->GetHealth(),

            pPed->GetArmour(),

            pWeapon->dwType,

            pWeapon->dwAmmoInClip,

            pWeapon->dwAmmo,

            pGame->GetLocalMoney(),

            pGame->GetWantedLevel(),

            pUI ? pUI->GetEat() : 0,

            pUI ? pUI->GetDrink() : 0,

            pUI ? pUI->GetBankMoney() : 0,

            (int)pPlayerPool->GetLocalPlayerID()

    );

    *(uint8_t*)(g_libGTASA + (VER_x32 ? 0x00819D88 + 1 : 0x009ff3A8)) = 0;

}

#include "Widgets/WidgetGta.h"

#include "Widgets/TouchInterface.h"

/*void InitRadarWidget() {

    if(!CGtaWidgets::m_pWidgets[WidgetIDs::WIDGET_RADAR]) {

        CGtaWidgets::m_pWidgets[WidgetIDs::WIDGET_RADAR] = new CWidgetGta();

        CGtaWidgets::m_pWidgets[WidgetIDs::WIDGET_RADAR]->SetEnabled(true);

        CGtaWidgets::m_pWidgets[WidgetIDs::WIDGET_RADAR]->SetTexture("radar");

    }

}*/

// 0.3.7

extern "C" void SwitchNextWeapon()

{

    CPlayerPed* pPlayer = pGame->FindPlayerPed();

    if (!pPlayer || !pPlayer->m_pPed) return;



    int currentSlot = pPlayer->m_pPed->m_nActiveWeaponSlot;

    int nextSlot = currentSlot;



    // Cycle through slots to find the next valid weapon

    for (int i = 0; i < 13; i++)

    {

        nextSlot = (nextSlot + 1) % 13;

        // Slot 0 is always valid (Empty/Fist). Other slots need a valid weapon type and ammo.

        if (nextSlot == 0 || (pPlayer->m_pPed->m_aWeapons[nextSlot].dwType != 0 && pPlayer->m_pPed->m_aWeapons[nextSlot].dwAmmo > 0))

        {

            pPlayer->SetArmedWeapon(pPlayer->m_pPed->m_aWeapons[nextSlot].dwType, false);

            return;

        }

    }

}









#include <time.h>

#include <algorithm>

#include <android/log.h>



static void UpdatePerfTracking()

{

    if (Diag_IsNoPerfLogger()) return;

    static const int MAX_SAMPLES = 600;

    static float s_frameTimes[MAX_SAMPLES];

    static int s_sampleCount = 0;

    static uint64_t s_lastFrameNs = 0;

    static uint64_t s_lastReportNs = 0;



    struct timespec ts;

    clock_gettime(CLOCK_MONOTONIC, &ts);

    uint64_t nowNs = (uint64_t)ts.tv_sec * 1000000000ULL + (uint64_t)ts.tv_nsec;



    if (s_lastFrameNs == 0) {

        s_lastFrameNs = nowNs;

        s_lastReportNs = nowNs;

        return;

    }



    float deltaMs = (float)(nowNs - s_lastFrameNs) / 1000000.0f;

    s_lastFrameNs = nowNs;



    if (s_sampleCount < MAX_SAMPLES) {

        s_frameTimes[s_sampleCount++] = deltaMs;

    }



    if (nowNs - s_lastReportNs >= 5000000000ULL) {

        if (s_sampleCount > 0) {

            float totalMs = 0.0f;

            float maxMs = 0.0f;

            for (int i = 0; i < s_sampleCount; ++i) {

                totalMs += s_frameTimes[i];

                if (s_frameTimes[i] > maxMs) maxMs = s_frameTimes[i];

            }

            float avgMs = totalMs / (float)s_sampleCount;

            float avgFps = (avgMs > 0.001f) ? (1000.0f / avgMs) : 0.0f;



            std::sort(s_frameTimes, s_frameTimes + s_sampleCount);

            int p99Idx = (int)((float)s_sampleCount * 0.99f);

            if (p99Idx >= s_sampleCount) p99Idx = s_sampleCount - 1;

            float worst1PctMs = s_frameTimes[p99Idx];



            __android_log_print(ANDROID_LOG_INFO, "PERF",

                "[PERF] Avg FPS: %.1f | Avg Frame: %.2f ms | 99th%% (worst 1%%): %.2f ms | Max: %.2f ms | Frames: %d",

                avgFps, avgMs, worst1PctMs, maxMs, s_sampleCount);

        }

        s_sampleCount = 0;

        s_lastReportNs = nowNs;

    }

}



void Render2dStuff()

{

    static bool s_firstFrameLogged = false;

    if (!s_firstFrameLogged) {

        s_firstFrameLogged = true;

        Diag_Breadcrumb("First frame rendered (Render2dStuff)");

    }

    ShowHud();





    if( CHook::CallFunction<bool>(g_libGTASA + (VER_x32 ? 0x001BB7F4 + 1 : 0x24EA90)) ) // emu_IsAltRenderTarget()

        CHook::CallFunction<void>(g_libGTASA + (VER_x32 ? 0x001BC20C + 1 : 0x24F5B8)); // emu_FlushAltRenderTarget()



    RwRenderStateSet(rwRENDERSTATEZTESTENABLE, RWRSTATE(FALSE));

    RwRenderStateSet(rwRENDERSTATEZWRITEENABLE, RWRSTATE(FALSE));

    RwRenderStateSet(rwRENDERSTATEVERTEXALPHAENABLE, RWRSTATE(TRUE));

    RwRenderStateSet(rwRENDERSTATESRCBLEND, RWRSTATE(rwBLENDSRCALPHA));

    RwRenderStateSet(rwRENDERSTATEDESTBLEND, RWRSTATE(rwBLENDINVSRCALPHA));

    RwRenderStateSet(rwRENDERSTATEFOGENABLE, RWRSTATE(rwRENDERSTATENARENDERSTATE));

    RwRenderStateSet(rwRENDERSTATECULLMODE, RWRSTATE(rwCULLMODECULLNONE));



#if VER_x32

    ( ( void(*)() )(g_libGTASA + 0x00437200 + 1) )(); // прицел

#else

    ( ( void(*)() )(g_libGTASA + 0x0051C694) )(); // прицелCHud::DrawCrossHairs(void)	000000000051C694

#endif



    CheckWannaOpenMap();



    auto radar = CTouchInterface::m_pWidgets[WIDGET_RADAR];

    if (radar) {

        radar->m_bEnabled = true;

        radar->m_Color = CRGBA(255, 255, 255, 255);



        float centerX = 50.0f;

        float centerY = 70.0f;

        float radiusX = 45.0f;

        float radiusY = 45.0f;



        if (!IsClassicHudMode()) {

            float virtWidth = (RsGlobal && RsGlobal->maximumHeight > 0)
                ? (448.0f * (float)RsGlobal->maximumWidth / (float)RsGlobal->maximumHeight)
                : 800.0f;

            centerX = virtWidth - 85.0f;

            centerY = 75.0f;

            radiusX = 40.0f;

            radiusY = 40.0f;

        }



        if (pSettings && pSettings->Get().fHudRadarX > 0.0f) {

            centerX = pSettings->Get().fHudRadarX;

            centerY = pSettings->Get().fHudRadarY;

            radiusX = pSettings->Get().fHudRadarScale;

            radiusY = (pSettings->Get().fHudRadarScaleY > 0.0f) ? pSettings->Get().fHudRadarScaleY : radiusX;

        }



        radar->m_fOriginX = centerX;

        radar->m_fOriginY = centerY;

        radar->m_fScaleX  = radiusX;

        radar->m_fScaleY  = radiusY;



        if (RsGlobal && RsGlobal->maximumHeight > 0) {

            float mult = (float)RsGlobal->maximumHeight / 448.0f;

            radar->m_RectScreen.left   = (centerX - radiusX) * mult;

            radar->m_RectScreen.right  = (centerX + radiusX) * mult;

            radar->m_RectScreen.top    = (centerY - radiusY) * mult;

            radar->m_RectScreen.bottom = (centerY + radiusY) * mult;

        }

    }



    // Reset CEntryExitManager::ms_exitEnterState so CHud::DrawRadar and CRadar::DrawMap never get skipped

    *(uint32_t*)(g_libGTASA + (VER_x32 ? 0x7a1eac : 0x982090)) = 0;



#if VER_x32

    ((void (*)())(g_libGTASA + 0x00437B0C + 1))(); // DrawRadar

#else

    ((void (*)())(g_libGTASA + 0x51CFF0))();

#endif



    ((void(*)(bool))(g_libGTASA + (VER_x32 ? 0x002B0BD8 + 1 : 0x36FB00)))(false);

    CHook::CallFunction<void>("_Z12emu_GammaSeth", 1);

    ((void (*)(bool))(g_libGTASA + (VER_x32 ? 0x0054BDD4 + 1 : 0x66B678)))(1u); // CMessages::Display

    ((void (*)(bool))(g_libGTASA + (VER_x32 ? 0x005A9120 + 1 : 0x6CCEA0)))(1u); // CFont::RenderFontBuffer

    CHook::CallFunction<void>("_Z12emu_GammaSeth", 0);



    if (pNetGame) {

        CTextDrawPool* pTextDrawPool = pNetGame->GetTextDrawPool();

        if (pTextDrawPool) pTextDrawPool->Draw();

    }



    if (pUI) pUI->render();

    UpdatePerfTracking();

}





/* =============================================================================== */

#include "game/CGPS.hpp"

extern GPS* pGPS;



int (*CRadar__SetCoordBlip)(int r0, float X, float Y, float Z, int r4, int r5, char *name);

int CRadar__SetCoordBlip_hook(int r0, float X, float Y, float Z, int r4, int r5, char *name)

{

    if (pNetGame && !strncmp(name, "CODEWAY", 7))

    {

        float findZ = CWorld::FindGroundZForCoord(X, Y) + 1.5f;

        CVector pos = { X, Y, findZ - 1.5f };





        GPS::Set(pos, true);

        RakNet::BitStream bsSend;

        bsSend.Write(X);

        bsSend.Write(Y);

        bsSend.Write(findZ);

        pNetGame->GetRakClient()->RPC(&RPC_MapMarker, &bsSend, HIGH_PRIORITY, RELIABLE, 0, false, UNASSIGNED_NETWORK_ID, nullptr);

    }



    return CRadar__SetCoordBlip(r0, X, Y, Z, r4, r5, name);

}







/* =============================================================================== */



void(*CRadar_DrawRadarGangOverlay)(uint32_t unk);

void CRadar_DrawRadarGangOverlay_hook(uint32_t unk)

{

    if (pNetGame)

    {

        CGangZonePool *pGangZonePool = pNetGame->GetGangZonePool();

        if (pGangZonePool) {

            pGangZonePool->Draw(unk);

        }

    }

}



/* =============================================================================== */



typedef struct {

    CVector     vecPosObject;

    CQuaternion m_qRotation;

    int32       wModelIndex;

    union {

        struct { // CFileObjectInstanceType

            uint32 m_nAreaCode : 8;

            uint32 m_bRedundantStream : 1;

            uint32 m_bDontStream : 1; // Merely assumed, no countercheck possible.

            uint32 m_bUnderwater : 1;

            uint32 m_bTunnel : 1;

            uint32 m_bTunnelTransition : 1;

            uint32 m_nReserved : 19;

        };

        uint32 m_nInstanceType;

    };

    int32 m_nLodInstanceIndex; // -1 - without LOD model

} stLoadObjectInstance;

VALIDATE_SIZE(stLoadObjectInstance, (VER_x32 ? 0x28 : 0x28));



extern int iBuildingToRemoveCount;

extern std::list<REMOVE_BUILDING_DATA> RemoveBuildingData;

void (*CEntity_Render)(CEntityGTA* pEntity);

int g_iLastRenderedObject;



void CEntity_Render_hook(CEntityGTA* pEntity)

{

    if (!pEntity) return;



#if defined(ENABLE_RENDER_DIAGNOSTICS) || defined(DIAG_BUILD)

    g_RenderBreadcrumb.entityPtr = (uintptr_t)pEntity;

    g_RenderBreadcrumb.modelIndex = pEntity->m_nModelIndex;

    g_RenderBreadcrumb.entityType = (uint32_t)pEntity->GetType();

    g_RenderBreadcrumb.entityRenderCount++;

#endif



    if (iBuildingToRemoveCount > 0)

    {

        eEntityType entityType = pEntity->GetType();

        if (entityType == ENTITY_TYPE_BUILDING || entityType == ENTITY_TYPE_DUMMY)

        {

            uint16_t modelId = pEntity->m_nModelIndex;

            const CVector& pos = pEntity->GetPosition();



            for (const auto &entry : RemoveBuildingData)

            {

                if (entry.usModelIndex == (uint16_t)-1 || entry.usModelIndex == modelId)

                {

                    float fDistance = GetDistance(entry.vecPos, pos);

                    if (fDistance <= entry.fRange)

                    {

                        HideEntity(pEntity);

                        return;

                    }

                }

            }

        }

    }



    g_iLastRenderedObject = pEntity->GetModelId();



    CEntity_Render(pEntity);

}

void (*CObject_Render)(CObjectGta* thiz);

void CObject_Render_hook(CObjectGta* thiz)

{

    if (!thiz) return;



#if defined(ENABLE_RENDER_DIAGNOSTICS) || defined(DIAG_BUILD)

    g_RenderBreadcrumb.entityPtr = (uintptr_t)thiz;

    g_RenderBreadcrumb.modelIndex = thiz->m_nModelIndex;

    g_RenderBreadcrumb.entityType = (uint32_t)thiz->GetType();

    g_RenderBreadcrumb.entityRenderCount++;

#endif



    if (pNetGame && pNetGame->GetObjectPool())

    {

        CObject *pObject = pNetGame->GetObjectPool()->FindObjectFromGtaPtr(thiz);

        if (pObject && pObject->m_pEntity)

        {

            RwObject* rwObject = (RwObject*)pObject->m_pEntity->m_pRwObject;

            if (rwObject && rwObject->parent)

            {

                // SetObjectMaterial

                if (pObject->m_bHasMaterial || pObject->m_bHasMaterialText)

                {

                    RwFrameForAllObjects((RwFrame*)rwObject->parent, (RwObject *(*)(RwObject *, void *))ObjectMaterialCallBack, pObject);

                }

                // SetObjectMaterialText

                if (pObject->m_bHasMaterialText)

                {

                    RwFrameForAllObjects((RwFrame*)rwObject->parent, (RwObject *(*)(RwObject *, void *))ObjectMaterialTextCallBack, pObject);

                }

            }

        }

    }



    CObject_Render(thiz);

}



void (*CTaskComplexLeaveCar)(uintptr_t** thiz, CVehicleGTA* pVehicle, int iTargetDoor, int iDelayTime, bool bSensibleLeaveCar, bool bForceGetOut);

void CTaskComplexLeaveCar_hook(uintptr_t** thiz, CVehicleGTA* pVehicle, int iTargetDoor, int iDelayTime, bool bSensibleLeaveCar, bool bForceGetOut)

{

    uintptr_t dwRetAddr = 0;

    __asm__ volatile ("mov %0, lr" : "=r" (dwRetAddr));

    dwRetAddr -= g_libGTASA;



    if (dwRetAddr == 0x409A42+1 || dwRetAddr == 0x40A818+1)

    {

        if (pNetGame)

        {

            if ((CVehicleGTA*)GamePool_FindPlayerPed()->pVehicle == pVehicle)

            {

                CVehiclePool* pVehiclePool = pNetGame->GetVehiclePool();

                VEHICLEID VehicleID = pVehiclePool->FindIDFromGtaPtr((CVehicleGTA*)GamePool_FindPlayerPed()->pVehicle);

                if (VehicleID != INVALID_VEHICLE_ID)

                {

                    CVehicle* pVehicle = pVehiclePool->GetAt(VehicleID);

                    CLocalPlayer* pLocalPlayer = pNetGame->GetPlayerPool()->GetLocalPlayer();

                    if (pVehicle && pLocalPlayer)

                    {

                        if (pVehicle->IsATrainPart())

                        {

                            RwMatrix mat = pVehicle->m_pVehicle->GetMatrix().ToRwMatrix();

                            pLocalPlayer->GetPlayerPed()->RemoveFromVehicleAndPutAt(mat.pos.x + 2.5f, mat.pos.y + 2.5f, mat.pos.z);

                        }

                        else

                        {

                            pLocalPlayer->SendExitVehicleNotification(VehicleID);

                        }

                    }

                }

            }

        }

    }



    (*CTaskComplexLeaveCar)(thiz, pVehicle, iTargetDoor, iDelayTime, bSensibleLeaveCar, bForceGetOut);

}



/* =============================================================================== */



uint32_t CRadar__GetRadarTraceColor(uint32_t color, uint8_t bright, uint8_t friendly)

{

    return TranslateColorCodeToRGBA(color);

}



#if VER_x32

uint32_t CHudColours__GetIntColour(uint32 colour_id)

{

	return TranslateColorCodeToRGBA(colour_id);

}

#else

uint32_t CHudColours__GetIntColour(uintptr* thiz, uint8 colour_id)

{

    return TranslateColorCodeToRGBA(colour_id);

}

#endif



/* =============================================================================== */



void (*AND_TouchEvent)(int type, int num, int posX, int posY);

void AND_TouchEvent_hook(int type, int num, int posX, int posY)

{

    // imgui

    if (pUI) {

        pUI->OnTouchEvent(type, num, posX, posY);

    }



    if (pGame->IsGamePaused())

        return AND_TouchEvent(type, num, posX, posY);



    if (pUI != nullptr)

    {

        switch (type)

        {

            case 2: // push

                pUI->touchEvent(ImVec2(posX, posY), TouchType::push);

                break;



            case 3: // move

                pUI->touchEvent(ImVec2(posX, posY), TouchType::move);

                break;



            case 1: // pop

                pUI->touchEvent(ImVec2(posX, posY), TouchType::pop);

                break;

        }







        if (pUI->keyboard()->visible() || pUI->dialog()->visible()) {

            AND_TouchEvent(1, 0, 0, 0);

            return;

        }



        if (pUI->isKeysWindowHovered(ImVec2(posX, posY))) {

            AND_TouchEvent(1, 0, 0, 0);

            return;

        }



        if (!Diag_IsNoChatTouch() && pChat && pChat->isVisible() && (pChat->contains(ImVec2(posX, posY)) || pChat->isInteracting())) {

            AND_TouchEvent(1, 0, 0, 0);

            return;

        }

        else

        {

            if (pNetGame && pNetGame->GetTextDrawPool())

            {

                if (!pNetGame->GetTextDrawPool()->onTouchEvent(type, num, posX, posY)) {

                    return AND_TouchEvent(1, 0, 0, 0);

                }

            }

        }

    }



    // Check Radar tap to open map

    static bool s_bRadarTouchDown = false;

    static int s_iRadarDownPointer = -1;

    static int s_iRadarDownX = 0;

    static int s_iRadarDownY = 0;



    auto radarWidget = (CTouchInterface::m_pWidgets) ? CTouchInterface::m_pWidgets[WIDGET_RADAR] : nullptr;

    if (radarWidget && radarWidget->m_bEnabled)

    {

        float pad = 20.0f;

        bool bInside = (posX >= (radarWidget->m_RectScreen.left - pad) &&

                        posX <= (radarWidget->m_RectScreen.right + pad) &&

                        posY >= (radarWidget->m_RectScreen.top - pad) &&

                        posY <= (radarWidget->m_RectScreen.bottom + pad));



        if (type == 2) // TOUCH_PUSH

        {

            if (bInside) {

                s_bRadarTouchDown = true;

                s_iRadarDownPointer = num;

                s_iRadarDownX = posX;

                s_iRadarDownY = posY;

                AND_TouchEvent(1, 0, 0, 0);

                return;

            }

        }

        else if (type == 1) // TOUCH_POP

        {

            if (s_bRadarTouchDown && (s_iRadarDownPointer == num || s_iRadarDownPointer == -1)) {

                s_bRadarTouchDown = false;

                s_iRadarDownPointer = -1;

                if (abs(posX - s_iRadarDownX) < 80 && abs(posY - s_iRadarDownY) < 80) {


                    TriggerOpenMap();

                }

                AND_TouchEvent(1, 0, 0, 0);

                return;

            }

        }

        else if (type == 3) // TOUCH_MOVE

        {

            if (s_bRadarTouchDown && s_iRadarDownPointer == num) {

                AND_TouchEvent(1, 0, 0, 0);

                return;

            }

        }

    }



    if (pGame->IsGameInputEnabled())

        AND_TouchEvent(type, num, posX, posY);

    else

        AND_TouchEvent(1, 0, 0, 0);

}



uint32_t (*CPed__GetWeaponSkill)(CPedGTA *thiz, uint32_t weaponType);

uint32_t CPed__GetWeaponSkill_hook(CPedGTA *thiz, uint32_t weaponType)

{

    bool bWeaponSkillStored = false;



    dwCurPlayerActor = thiz;

    byteInternalPlayer = CWorld::PlayerInFocus;

    byteCurPlayer = FindPlayerNumFromPedPtr(dwCurPlayerActor);



    if(dwCurPlayerActor && byteCurPlayer != 0 && CWorld::PlayerInFocus == 0)

    {

        GameStoreLocalPlayerSkills();

        GameSetRemotePlayerSkills(byteCurPlayer);

        bWeaponSkillStored = true;

    }



    uint32_t result = 2; // Hitman level by default for proper weapon holding stance

    if (CPed__GetWeaponSkill) {

        result = CPed__GetWeaponSkill(thiz, weaponType);

        if (result < 2) result = 2;

    }



    if(bWeaponSkillStored)

    {

        GameSetLocalPlayerSkills();

        bWeaponSkillStored = false;

    }



    return result;

}



/* =============================================================================== */



extern CPlayerPed* g_pCurrentFiredPed;

extern BULLET_DATA* g_pCurrentBulletData;



extern int g_iLagCompensationMode;



void SendBulletSync(CVector *vecOrigin, CVector *vecEnd, CVector *vecPos, CEntityGTA **ppEntity)

{

    BULLET_DATA bulletData;

    memset(&bulletData, 0, sizeof(BULLET_DATA));



    bulletData.vecOrigin.x = vecOrigin->x;

    bulletData.vecOrigin.y = vecOrigin->y;

    bulletData.vecOrigin.z = vecOrigin->z;



    bulletData.vecPos.x = vecPos->x;

    bulletData.vecPos.y = vecPos->y;

    bulletData.vecPos.z = vecPos->z;



    if(ppEntity)

    {

        static CEntityGTA *pEntity;

        pEntity = *ppEntity;

        if(pEntity)

        {

            if(pEntity)

            {

                if(g_iLagCompensationMode =! 0)

                {

                    bulletData.vecOffset.x = vecPos->x - pEntity->m_matrix->m_pos.x;

                    bulletData.vecOffset.y = vecPos->y - pEntity->m_matrix->m_pos.y;

                    bulletData.vecOffset.z = vecPos->z - pEntity->m_matrix->m_pos.z;

                }

                else

                {

                    static RwMatrix mat1;

                    memset(&mat1, 0, sizeof(mat1));



                    static RwMatrix mat2;

                    memset(&mat2, 0, sizeof(mat2));

                    auto entMat = pEntity->GetMatrix().ToRwMatrix();

                    RwMatrixOrthoNormalize(reinterpret_cast<RwMatrix *>(&mat2), &entMat);

                    RwMatrixInvert(&mat1, &mat2);

                    ProjectMatrix(&bulletData.vecOffset, reinterpret_cast<CMatrix *>(&mat1), vecPos);

                }

            }



            bulletData.pEntity = pEntity;

        }

    }



    pGame->FindPlayerPed()->ProcessBulletData(&bulletData);

}



extern bool g_customFire;



uint32_t(*CWeapon__FireInstantHit)(CWeapon* thiz, CPedGTA* pFiringEntity, CVector* vecOrigin, CVector* muzzlePosn, CEntityGTA* targetEntity,

                                   CVector* target, CVector* originForDriveBy, bool arg6, bool muzzle);

uint32_t CWeapon__FireInstantHit_hook(CWeapon* thiz, CPedGTA* pFiringEntity, CVector* vecOrigin, CVector* muzzlePosn, CEntityGTA* targetEntity,

                                      CVector* target, CVector* originForDriveBy, bool arg6, bool muzzle)

{

    FLog("[WEAPON] FireInstantHit called - pFiringEntity: %p, targetEntity: %p", pFiringEntity, targetEntity);



    if (pNetGame && pNetGame->GetPlayerPool()->GetLocalPlayer()->GetPlayerPed()->m_pPed)		// CWeapon::Fire

    {

        		if(pFiringEntity != GamePool_FindPlayerPed())

        		{

        			FLog("[WEAPON] Not local player, returning muzzle");

        			return muzzle;

        		}        FLog("[WEAPON] Local player firing!");



        if(pNetGame)

        {

            pNetGame->GetPlayerPool()->ApplyCollisionChecking();

        }



        if(pGame)

        {

            CPlayerPed *pPlayerPed = pGame->FindPlayerPed();

            if(pPlayerPed)

            {

                FLog("[WEAPON] Calling FireInstant()");

                pPlayerPed->FireInstant();

            }

            else

            {

                FLog("[WEAPON] ERROR: pPlayerPed is NULL!");

            }

        }



        if(pNetGame)

        {

            pNetGame->GetPlayerPool()->ResetCollisionChecking();

        }



        return muzzle;

    }



    FLog("[WEAPON] Calling original FireInstantHit");

    return CWeapon__FireInstantHit(thiz, pFiringEntity, vecOrigin, muzzlePosn, targetEntity,

                                   target, originForDriveBy, arg6, muzzle);

}





bool g_bForceWorldProcessLineOfSight = false;

uint32_t (*CWeapon__ProcessLineOfSight)(CVector *vecOrigin, CVector *vecEnd, CVector *vecPos, CPedGTA **ppEntity, CWeapon *pWeaponSlot, CPedGTA **ppEntity2, bool b1, bool b2, bool b3, bool b4, bool b5, bool b6, bool b7);

uint32_t CWeapon__ProcessLineOfSight_hook(CVector *vecOrigin, CVector *vecEnd, CVector *vecPos, CPedGTA **ppEntity, CWeapon *pWeaponSlot, CPedGTA **ppEntity2, bool b1, bool b2, bool b3, bool b4, bool b5, bool b6, bool b7)

{

    uintptr_t dwRetAddr = 0;

    GET_LR(dwRetAddr);



    FLog("dwRetAddr CWeapon__ProcessLineOfSight_hook 0x%llx", dwRetAddr);

#if VER_x32

    if(dwRetAddr >= 0x005DC178 && dwRetAddr <= 0x005DD684)

		g_bForceWorldProcessLineOfSight = true;

#else

    if(dwRetAddr >= 0x701494 && dwRetAddr <= 0x702B18)

        g_bForceWorldProcessLineOfSight = true;

#endif



    return CWeapon__ProcessLineOfSight(vecOrigin, vecEnd, vecPos, ppEntity, pWeaponSlot, ppEntity2, b1, b2, b3, b4, b5, b6, b7);

}



uint32_t(*CWorld__ProcessLineOfSight)(CVector*, CVector*, CColPoint *colPoint, CEntityGTA**, bool, bool, bool, bool, bool, bool, bool, bool);

uint32_t CWorld__ProcessLineOfSight_hook(CVector* vecOrigin, CVector* vecEnd, CColPoint *colPoint, CEntityGTA** ppEntity,

                                         bool b1, bool b2, bool b3, bool b4, bool b5, bool b6, bool b7, bool b8)

{

    uintptr_t dwRetAddr = 0;

    GET_LR(dwRetAddr);



    if(dwRetAddr == (VER_x32 ? 0x005dd0b0 + 1 : 0x70253C) || g_bForceWorldProcessLineOfSight)

    {

        g_bForceWorldProcessLineOfSight = false;

        static CVector vecPosPlusOffset;



        if (g_iLagCompensationMode != 2)

        {

            if (g_pCurrentFiredPed != pGame->FindPlayerPed())

            {

                if (g_pCurrentBulletData && g_pCurrentBulletData->pEntity)

                {

                    if (*(uintptr_t*)(g_pCurrentBulletData->pEntity) != g_libGTASA+(VER_x32 ? 0x667D18:0x8300A0)) // CPlaceable

                    {

                        if (g_iLagCompensationMode)

                        {

                            vecPosPlusOffset.x = g_pCurrentBulletData->pEntity->GetPosition().x + g_pCurrentBulletData->vecOffset.x;

                            vecPosPlusOffset.y = g_pCurrentBulletData->pEntity->GetPosition().y + g_pCurrentBulletData->vecOffset.y;

                            vecPosPlusOffset.z = g_pCurrentBulletData->pEntity->GetPosition().z + g_pCurrentBulletData->vecOffset.z;

                        }

                        else

                        {

                            ProjectMatrix((CVector*)&vecPosPlusOffset, &g_pCurrentBulletData->pEntity->GetMatrix(), &g_pCurrentBulletData->vecOffset);

                        }



                        vecEnd->x = vecPosPlusOffset.x - vecOrigin->x + vecPosPlusOffset.x;

                        vecEnd->y = vecPosPlusOffset.y - vecOrigin->y + vecPosPlusOffset.y;

                        vecEnd->z = vecPosPlusOffset.z - vecOrigin->z + vecPosPlusOffset.z;

                    }

                }

            }

        }



        uint32_t result = CWorld__ProcessLineOfSight(vecOrigin, vecEnd, colPoint, ppEntity, b1, b2, b3, b4, b5, b6, b7, b8);



        if (g_iLagCompensationMode == 2)

        {

            if (g_pCurrentFiredPed == pGame->FindPlayerPed())

                SendBulletSync(vecOrigin, vecEnd, reinterpret_cast<CVector *>(colPoint), (CEntityGTA **) ppEntity);



            return result;

        }



        if (g_pCurrentFiredPed)

        {

            if (g_pCurrentFiredPed != pGame->FindPlayerPed())

            {

                if (g_pCurrentBulletData)

                {

                    if (g_pCurrentBulletData->pEntity == nullptr)

                    {

                        CPedGTA* pLocalPed = GamePool_FindPlayerPed();

                        if (*ppEntity == GamePool_FindPlayerPed() ||

                            pLocalPed->IsInVehicle() && *ppEntity == pLocalPed->pVehicle)

                        {

                            result = 0;

                            *ppEntity = nullptr;

                            colPoint->m_vecPoint.x = 0.0f;

                            colPoint->m_vecPoint.y = 0.0f;

                            colPoint->m_vecPoint.z = 0.0f;

                            return result;

                        }

                    }

                }

            }

            else {

                SendBulletSync(vecOrigin, vecEnd, reinterpret_cast<CVector *>(colPoint), (CEntityGTA **)ppEntity);

            }

        }



        return result;

    }



    return CWorld__ProcessLineOfSight(vecOrigin, vecEnd, colPoint, ppEntity, b1, b2, b3, b4, b5, b6, b7, b8);

}

// 0.3.7

uint32_t(*CWeapon__FireSniper)(CWeapon* thiz, CPedGTA* pFiringEntity, CEntityGTA* victim, CVector* target);

uint32_t CWeapon__FireSniper_hook(CWeapon* thiz, CPedGTA* pFiringEntity, CEntityGTA* victim, CVector* target)

{

    		if (pFiringEntity == GamePool_FindPlayerPed())

    		{

    			if (pGame)

    			{

    				CPlayerPed* pPlayerPed = pGame->FindPlayerPed();

    				if (pPlayerPed) {

    					pPlayerPed->FireInstant();

    				}

    			}

    		}

    	

    		return true;}

// 0.3.7

bool(*CBulletInfo_AddBullet)(CEntityGTA* creator, int weaponType, CVector pos, CVector velocity);

bool CBulletInfo_AddBullet_hook(CEntityGTA* creator, int weaponType, CVector pos, CVector velocity)

{

    velocity.x *= 50.0f;

    velocity.y *= 50.0f;

    velocity.z *= 50.0f;



    CBulletInfo_AddBullet(creator, weaponType, pos, velocity);



    // CBulletInfo::Update

    CHook::CallFunction<void>("_ZN11CBulletInfo6UpdateEv");

    return true;

}



#pragma pack(push, 1)

struct CPedDamageResponseCalculator

{

    CPedGTA* m_pDamager;

    float m_fDamageFactor;

    int m_pedPieceType;

    int m_weaponType;

};

#pragma pack(pop)

// 0.3.7

bool ComputeDamageResponse(CPedDamageResponseCalculator* calculator, CPedGTA* pPed)

{

    CPedGTA* pGamePed = GamePool_FindPlayerPed();

    bool isLocalPed = false;



    if (!pNetGame) return false;



    CPedGTA* pDamager = calculator->m_pDamager;

    if (pDamager != pGamePed && IsValidGamePed(pGamePed)) /* CCivilianPed */

        return true;



    if (pPed == pGamePed) {

        isLocalPed = true;

    }

    else if (pDamager != pGamePed) {

        return false;

    }



    CPlayerPool* pPlayerPool = pNetGame->GetPlayerPool();

    CLocalPlayer* pLocalPlayer = pPlayerPool->GetLocalPlayer();

    PLAYERID PlayerID;



    if (isLocalPed)

    {

        PlayerID = FindPlayerIDFromGtaPtr(pDamager);

        pLocalPlayer->SendTakeDamageEvent(PlayerID,

                                          calculator->m_fDamageFactor,

                                          calculator->m_weaponType,

                                          calculator->m_pedPieceType);

    }

    else

    {

        PlayerID = FindPlayerIDFromGtaPtr(pPed);

        if (PlayerID != INVALID_PLAYER_ID)

        {

            pLocalPlayer->SendGiveDamageEvent(PlayerID,

                                              calculator->m_fDamageFactor,

                                              calculator->m_weaponType,

                                              calculator->m_pedPieceType);

            if (pPlayerPool->GetAt(PlayerID)->IsNPC())

                return true;

        }

        else

        {

            PLAYERID ActorID = FindActorIDFromGtaPtr(pPed);

            if (ActorID != INVALID_PLAYER_ID) {

                pLocalPlayer->SendGiveDamageEvent(ActorID,

                                                  calculator->m_fDamageFactor,

                                                  calculator->m_weaponType,

                                                  calculator->m_pedPieceType);

                return true;

            }

        }

    }





    // :check_friendly_fire

    if (!pNetGame->m_pNetSet->bFriendlyFire)

        return false;

    uint8_t byteTeam = pPlayerPool->GetLocalPlayer()->m_byteTeam;

    if (byteTeam == NO_TEAM ||

        PlayerID == INVALID_PLAYER_ID ||

        pPlayerPool->GetAt(PlayerID)->m_byteTeam != byteTeam) {

        return false;

    }



    return true;

}



// 0.3.7

void (*CPedDamageResponseCalculator__ComputeDamageResponse)(CPedDamageResponseCalculator* thiz, CPedGTA* pPed, uintptr_t* a3, uint32_t a4);

void CPedDamageResponseCalculator__ComputeDamageResponse_hook(CPedDamageResponseCalculator* thiz, CPedGTA* pPed, uintptr_t *a3, uint32_t a4)

{

    if (thiz == nullptr || pPed == nullptr || a3 == nullptr) return;



    if (ComputeDamageResponse(thiz, pPed))

        return;



    CPedDamageResponseCalculator__ComputeDamageResponse(thiz, pPed, a3, a4);

}



#include "CFirstPersonCamera.hpp"

#include "CHandlingDefault.h"



void (*CCam__Process)(uintptr_t thiz);



void CCam__Process_hook(uintptr_t thiz)

{

    if (!CFirstPersonCamera::IsEnabled()) {

        CCam__Process(thiz);

        return;

    }



    CVector vecSavedSpeed;

    CVehicle* pVeh = nullptr;



    float v6 = *(float*)(g_libGTASA + (VER_x32 ? 0x6A9FD0 : 0x8855D4));



    if (pNetGame && (*(uint16_t*)(thiz + 14) == 16 || *(uint16_t*)(thiz + 14) == 18)) {

        if (auto playerPool = pNetGame->GetPlayerPool()) {

            if (auto localPlayer = playerPool->GetLocalPlayer()) {

                CPlayerPed* pPed = localPlayer->GetPlayerPed();

                CVehicleGTA* contactVeh = (CVehicleGTA*)pPed->GetEntityUnderPlayer();

                    VEHICLEID vehicleId = pNetGame->GetVehiclePool()->FindIDFromGtaPtr(contactVeh);

                CVehicle* pVeh = pNetGame->GetVehiclePool()->GetAt(vehicleId);



                if (pVeh && pVeh->m_pVehicle) {

                    CVector vecSavedSpeed = pVeh->m_pVehicle->m_vecMoveSpeed;



                    pVeh->m_pVehicle->m_vecMoveSpeed.x *= 6.0f;

                    pVeh->m_pVehicle->m_vecMoveSpeed.y *= 6.0f;

                    pVeh->m_pVehicle->m_vecMoveSpeed.z *= 6.0f;



                    CCam__Process(thiz);



                    pVeh->m_pVehicle->m_vecMoveSpeed = vecSavedSpeed;



                    *(float*)(g_libGTASA + (VER_x32 ? 0x6A9FD0 : 0x8855D4)) = 200.0f;

                }

            }

        }

    }



    CCam__Process(thiz);



    if (pVeh && pVeh->m_pVehicle) {

        pVeh->m_pVehicle->m_vecMoveSpeed = vecSavedSpeed;

        *(float*)(g_libGTASA + (VER_x32 ? 0x6A9FD0 : 0x8855D4)) = v6;

    }



    if (*(uint16_t*)(thiz + 14) == 4 || *(uint16_t*)(thiz + 14) == 53) {

        if (auto playerPool = pNetGame->GetPlayerPool()) {

            if (auto localPlayer = playerPool->GetLocalPlayer()) {

                CPlayerPed* pPed = localPlayer->GetPlayerPed();

                if (pPed) {

#if VER_x32

                    *(uint32_t*)(g_libGTASA + 0x00951FA8 + 120) = 0xFFFFFFFF;

                    *(uint32_t*)(g_libGTASA + 0x00951FA8 + 124) = 0xFFFFFFFF;

                    *(uint8_t*)(g_libGTASA + 0x00951FA8 + 40) = 0;

#else

                    *(uint32_t*)(g_libGTASA + 0xBBA8D0 + 128) = 0xFFFFFFFFFFFFFFFFLL;

                    *(uint8_t*)(g_libGTASA + 0xBBA8D0 + 48) = 0;

#endif

                    CFirstPersonCamera::ProcessCameraOnFoot(thiz, pPed);

                }

            }

        }

    }



    if (*(uint16_t*)(thiz + 14) == 16 || *(uint16_t*)(thiz + 14) == 18) {

        if (auto playerPool = pNetGame->GetPlayerPool()) {

            if (auto localPlayer = playerPool->GetLocalPlayer()) {

                CPlayerPed* pPed = localPlayer->GetPlayerPed();

                if (pPed) {

#if VER_x32

                    *(uint32_t*)(g_libGTASA + 0x00951FA8 + 120) = 0xFFFFFFFF;

                    *(uint32_t*)(g_libGTASA + 0x00951FA8 + 124) = 0xFFFFFFFF;

                    *(uint8_t*)(g_libGTASA + 0x00951FA8 + 40) = 0;

#else

                    *(uint32_t*)(g_libGTASA + 0xBBA8D0 + 128) = 0xFFFFFFFFFFFFFFFFLL;

                    *(uint8_t*)(g_libGTASA + 0xBBA8D0 + 48) = 0;

#endif

                    CFirstPersonCamera::ProcessCameraInVeh(thiz, pPed, pVeh);

                }

            }

        }

    }

}



static char szLastBufferedName[40];

int (*cHandlingDataMgr__FindExactWord)(uintptr_t *thiz, char* line, char* nameTable, int entrySize, int entryCount);

int cHandlingDataMgr__FindExactWord_hook(uintptr_t *thiz, char* line, char* nameTable, int entrySize, int entryCount)

{

    strncpy(&szLastBufferedName[0], line, entrySize);

    Log("Handling Data: line %s, nameTable %s, entrySize %d, entryCount %d", line, nameTable, entrySize, entryCount);

    return cHandlingDataMgr__FindExactWord(thiz, line, nameTable, entrySize, entryCount);

}



void cHandlingDataMgr__ConvertDataToGameUnits(uintptr_t *thiz, tHandlingData* handling)

{



    int32_t iHandling = ((int(*)(uintptr_t*, const char*))(g_libGTASA + (VER_x32 ? 0x00570D28 + 1 : 0x693378)))(thiz, &szLastBufferedName[0]);



    if(!CHandlingDefault::bIsSlotUsed[iHandling])

    {

        CHandlingDefault::FillDefaultHandling((uint16_t)iHandling, handling);

    }



    (( void (*)(uintptr_t*, tHandlingData* handling))(g_libGTASA + (VER_x32 ? 0x00570DC8 + 1 : 0x69343C)))(thiz, handling);

}



int g_iCounterVehicleCamera = 0;

int (*CPad__CycleCameraModeDownJustDown)(void*);

int CPad__CycleCameraModeDownJustDown_hook(void* thiz)

{

    CPedGTA* pPed = GamePool_FindPlayerPed();

    if (!pPed)

        return 0;



    static uint32_t lastTick = GetTickCount();

    bool bPressed = false;



    if (bIsTouchCameraButt && GetTickCount() - lastTick >= 250)

    {

        bIsTouchCameraButt = false;

        bPressed = true;

        lastTick = GetTickCount();

    }



    if (pPed->bInVehicle)

    {

        if (bPressed)

        {

            g_iCounterVehicleCamera++;

        }



        if (g_iCounterVehicleCamera == 6)

        {

            CFirstPersonCamera::SetEnabled(true);

            return 0;

        }

        else if (g_iCounterVehicleCamera >= 7)

        {

            g_iCounterVehicleCamera = 0;

            CFirstPersonCamera::SetEnabled(false);

            return 1;

        }

        else

        {

            CFirstPersonCamera::SetEnabled(false);

        }



        return bPressed;

    }



    return 0;

}



#include "CFPSFix.h"

#include "ES2VertexBuffer.h"

#include "RQ_Commands.h"

#include "Pickups.h"

#include "TimeCycle.h"

#include "game/Pipelines/CustomCar/CustomCarEnvMapPipeline.h"

#include "game/Pipelines/CustomBuilding/CustomBuildingDNPipeline.h"

#include "COcclusion.h"

#include "RealTimeShadowManager.h"

#include "game/Widgets/WidgetGta.h"



static constexpr float ar43 = 4.0f/3.0f;

float *ms_fAspectRatio;

void (*DrawCrosshair)(uintptr_t* thiz);

void DrawCrosshair_hook(uintptr_t* thiz)

{

    float save1 = CCamera::m_f3rdPersonCHairMultX;

    CCamera::m_f3rdPersonCHairMultX = 0.530f - (*ms_fAspectRatio - ar43) * 0.01125f;



    float save2 = CCamera::m_f3rdPersonCHairMultY;

    CCamera::m_f3rdPersonCHairMultY = 0.400f + (*ms_fAspectRatio - ar43) * 0.03600f;



    DrawCrosshair(thiz);



    CCamera::m_f3rdPersonCHairMultX = save1;

    CCamera::m_f3rdPersonCHairMultY = save2;

}



CVector& (*FindPlayerSpeed)(int a1);

CVector& FindPlayerSpeed_hook(int a1)

{

    uintptr_t dwRetAddr = 0;

    __asm__ volatile ("mov %0, lr":"=r" (dwRetAddr));

    dwRetAddr -= g_libGTASA;



    if(dwRetAddr == 0x43E1F6 + 1)

    {

        if(pNetGame)

        {

            CPlayerPed *pPlayerPed = pGame->FindPlayerPed();

            if(pPlayerPed &&

               pPlayerPed->IsInVehicle() &&

               pPlayerPed->IsAPassenger())

            {

                CVector vec = CVector(-1.0f);

                return vec;

            }

        }

    }



    return FindPlayerSpeed(a1);

}



int (*RwFrameAddChild)(int a1, int a2);

int RwFrameAddChild_hook(int a1, int a2)

{

    if(a1 == 0 || a2 == 0) return 0;

    return RwFrameAddChild(a1, a2);

}



int (*CTextureDatabaseRuntime__GetEntry)(uintptr_t thiz, const char* a2, bool* a3);

int CTextureDatabaseRuntime__GetEntry_hook(uintptr_t thiz, const char* a2, bool* a3)

{

    if (!thiz)

    {

        return -1;

    }

    return CTextureDatabaseRuntime__GetEntry(thiz, a2, a3);

}



uintptr_t (*CTxdStore__TxdStoreFindCB)(const char *a1);

uintptr_t CTxdStore__TxdStoreFindCB_hook(const char *a1)

{

    static char* texdbs[] = { "samp", "gta_int", "gta3" };

    for(auto &texdb : texdbs)

    {

        // TextureDatabaseRuntime::GetDatabase

        uintptr_t db_handle = ((uintptr_t (*)(const char *))(g_libGTASA+0x1EAC8C+1))(texdb);



        // TextureDatabaseRuntime::registered

        uint32_t unk_61B8D4 = *(uint32_t*)(g_libGTASA+0x6BD174+4);

        if(unk_61B8D4)

        {

            // TextureDatabaseRuntime::registered

            uintptr_t dword_61B8D8 = *(uintptr_t*)(g_libGTASA+0x6BD174+8);



            int index = 0;

            while(*(uint32_t*)(dword_61B8D8 + 4 * index) != db_handle)

            {

                if(++index >= unk_61B8D4)

                    goto GetTheTexture;

            }



            continue;

        }



        GetTheTexture:

        // TextureDatabaseRuntime::Register

        ((void (*)(int))(g_libGTASA+0x1E9BC8+1))(db_handle);



        // TextureDatabaseRuntime::GetTexture

        uintptr_t tex = ((uintptr_t (*)(const char *))(g_libGTASA+0x1E9C64+1))(a1);



        // TextureDatabaseRuntime::Unregister

        ((void (*)(int))(g_libGTASA+0x1E9C80+1))(db_handle);



        if(tex) return tex;

    }



    // RwTexDictionaryGetCurrent

    int current = ((int (*)(void))(g_libGTASA+0x1DBA64+1))();

    if(current)

    {

        while(true)

        {

            // RwTexDictionaryFindNamedTexture

            uintptr_t tex = ((int (*)(int, const char *))(g_libGTASA+0x1DB9B0+1))(current, a1);

            if(tex) return tex;



            // CTxdStore::GetTxdParent

            current = ((int (*)(int))(g_libGTASA+0x5D428C+1))(current);

            if(!current) return 0;

        }

    }



    return 0;

}



int (*CCustomRoadsignMgr_RenderRoadsignAtomic)(int a1, int a2);

int CCustomRoadsignMgr_RenderRoadsignAtomic_hook(int a1, int a2)

{

    if ( a1 )

        return CCustomRoadsignMgr_RenderRoadsignAtomic(a1, a2);

}



int (*_RwTextureDestroy)(int a1);

int _RwTextureDestroy_hook(int a1)

{

    int result; // r0



    if ( (unsigned int)(a1 + 1) >= 2 )

        result = _RwTextureDestroy(a1);

    else

        result = 0;

    return result;

}



int (*CPed_UpdatePosition)(CPedGTA* a1);

int CPed_UpdatePosition_hook(CPedGTA* a1)

{

    int result; // r0



    if ( GamePool_FindPlayerPed() == a1 )

        result = CPed_UpdatePosition(a1);

    return result;

}



extern CJavaWrapper* pJavaWrapper;

void (*MainMenuScreen__OnExit)();

void MainMenuScreen__OnExit_hook()

{

    pGame->bIsGameExiting = true;



    pNetGame->GetRakClient()->Disconnect(0);



    pJavaWrapper->exitGame();

}



// Safe memory address check for 32-bit and 64-bit user-space pointers

static inline bool IsValidAddress(uintptr_t addr)

{

    if (VER_x32) {

        return (addr >= 0x1000 && addr <= 0xFFFFFF00);

    } else {

        return (addr >= 0x1000 && addr <= 0x00007FFFFFFFFFFFULL);

    }

}



// Safe dummy material to prevent SIGSEGV fault 0xb when mesh->material is null in 64-bit CCustomBuildingDNPipeline

static struct {

    void* texture;

    uint8_t r, g, b, a;

    uint32_t pad[8];

} s_SafeDefaultMaterial = { nullptr, 255, 255, 255, 255, {0} };



// Sanitizes ResEntry mesh materials and textures in-place.

// Never drops atomics or aborts rendering, ensuring world geometry is fully rendered.

static inline bool CheckResEntryValid(RwResEntry* resEntry, uint32_t flags)

{

    if (!resEntry || !IsValidAddress((uintptr_t)resEntry)) return false;

    uint32_t validFlag = flags & 0x84;



    if (VER_x32)

    {

        uint16_t size = *(uint16_t*)((uintptr_t)resEntry + 26);

        if (size > 2048) return true;

        if (size)

        {

            uintptr_t arr = (uintptr_t)resEntry + 28;

            for (int i = 0; i < size; i++)

            {

                uintptr_t mesh = arr + i * 56;

                uintptr_t validate = *(uintptr_t*)(mesh + 48);

                if (!validate || validate > 0xFFFFFF00 || validate < 0x1000) {

                    *(uintptr_t*)(mesh + 48) = (uintptr_t)&s_SafeDefaultMaterial;

                    continue;

                }

                if (validFlag) {

                    uintptr_t* v4 = *(uintptr_t**)validate;

                    if (!v4 || (uintptr_t)v4 > 0xFFFFFF00 || (uintptr_t)v4 < 0x1000) {

                        *(uintptr_t*)validate = 0;

                        continue;

                    }

                    uintptr_t tex = *(uintptr_t*)v4;

                    if (!tex || tex < 0x1000 || tex > 0xFFFFFF00) {

                        *(uintptr_t*)validate = 0;

                        continue;

                    }

                }

            }

        }

    }

    else

    {

        uint16_t size = *(uint16_t*)((uintptr_t)resEntry + 0x32);

        if (size > 2048) return true;

        if (size)

        {

            uintptr_t arr = (uintptr_t)resEntry + 0x34;

            for (int i = 0; i < size; i++)

            {

                uintptr_t mesh = arr + i * 0x58;

                uintptr_t validate = *(uintptr_t*)(mesh + 0x48);

                if (!validate || validate < 0x1000 || validate > 0x00007FFFFFFFFFFFULL) {

                    *(uintptr_t*)(mesh + 0x48) = (uintptr_t)&s_SafeDefaultMaterial;

                    continue;

                }

                if (validFlag) {

                    uintptr_t* v4 = *(uintptr_t**)validate;

                    if (!v4 || (uintptr_t)v4 < 0x1000 || (uintptr_t)v4 > 0x00007FFFFFFFFFFFULL) {

                        *(uintptr_t*)validate = 0;

                        continue;

                    }

                    uintptr_t tex = *(uintptr_t*)v4;

                    if (!tex || tex < 0x1000 || tex > 0x00007FFFFFFFFFFFULL) {

                        *(uintptr_t*)validate = 0;

                        continue;

                    }

                }

            }

        }

    }

    return true;

}



int (*CCustomBuildingPipeline__CustomPipeRenderCB)(RwResEntry* resEntry, uintptr_t object, uint8_t type, uint32_t flags);

int CCustomBuildingPipeline__CustomPipeRenderCB_hook(RwResEntry* resEntry, uintptr_t object, uint8_t type, uint32_t flags)

{

    if (!resEntry || !flags || !CCustomBuildingPipeline__CustomPipeRenderCB)

        return 0;



#if defined(ENABLE_RENDER_DIAGNOSTICS) || defined(DIAG_BUILD)

    g_RenderBreadcrumb.resEntryPtr = (uintptr_t)resEntry;

    g_RenderBreadcrumb.objectPtr = object;

    g_RenderBreadcrumb.pipeType = (uint32_t)type;

    g_RenderBreadcrumb.pipeFlags = flags;

    RpAtomic* pAtomic = (type == 1 && object && IsValidAddress(object)) ? (RpAtomic*)object : nullptr;

    g_RenderBreadcrumb.geometryPtr = pAtomic ? (uintptr_t)pAtomic->geometry : 0;

    if (resEntry && IsValidAddress((uintptr_t)resEntry)) {

        g_RenderBreadcrumb.numMeshes = *(uint16_t*)((uintptr_t)resEntry + (VER_x32 ? 26 : 0x32));

    }

    g_RenderBreadcrumb.pipeRenderCount++;

#endif



    CheckResEntryValid(resEntry, flags);



    return CCustomBuildingPipeline__CustomPipeRenderCB(resEntry, object, type, flags);

}



int (*CCustomCarEnvMapPipeline__CustomPipeRenderCB)(RwResEntry* resEntry, uintptr_t object, uint8_t type, uint32_t flags);

int CCustomCarEnvMapPipeline__CustomPipeRenderCB_hook(RwResEntry* resEntry, uintptr_t object, uint8_t type, uint32_t flags)

{

    if (!resEntry || !flags || !CCustomCarEnvMapPipeline__CustomPipeRenderCB)

        return 0;



#if defined(ENABLE_RENDER_DIAGNOSTICS) || defined(DIAG_BUILD)

    g_RenderBreadcrumb.resEntryPtr = (uintptr_t)resEntry;

    g_RenderBreadcrumb.objectPtr = object;

    g_RenderBreadcrumb.pipeType = (uint32_t)type;

    g_RenderBreadcrumb.pipeFlags = flags;

    RpAtomic* pAtomic = (type == 1 && object && IsValidAddress(object)) ? (RpAtomic*)object : nullptr;

    g_RenderBreadcrumb.geometryPtr = pAtomic ? (uintptr_t)pAtomic->geometry : 0;

    if (resEntry && IsValidAddress((uintptr_t)resEntry)) {

        g_RenderBreadcrumb.numMeshes = *(uint16_t*)((uintptr_t)resEntry + (VER_x32 ? 26 : 0x32));

    }

    g_RenderBreadcrumb.pipeRenderCount++;

#endif



    CheckResEntryValid(resEntry, flags);



    return CCustomCarEnvMapPipeline__CustomPipeRenderCB(resEntry, object, type, flags);

}



int (*rxOpenGLDefaultAllInOneRenderCB)(RwResEntry* resEntry, uintptr_t object, uint8_t type, uint32_t flags);

int rxOpenGLDefaultAllInOneRenderCB_hook(RwResEntry* resEntry, uintptr_t object, uint8_t type, uint32_t flags)

{

    if (!resEntry || !flags || !rxOpenGLDefaultAllInOneRenderCB)

        return 0;



#if defined(ENABLE_RENDER_DIAGNOSTICS) || defined(DIAG_BUILD)

    g_RenderBreadcrumb.resEntryPtr = (uintptr_t)resEntry;

    g_RenderBreadcrumb.objectPtr = object;

    g_RenderBreadcrumb.pipeType = (uint32_t)type;

    g_RenderBreadcrumb.pipeFlags = flags;

    RpAtomic* pAtomic = (type == 1 && object && IsValidAddress(object)) ? (RpAtomic*)object : nullptr;

    g_RenderBreadcrumb.geometryPtr = pAtomic ? (uintptr_t)pAtomic->geometry : 0;

    if (resEntry && IsValidAddress((uintptr_t)resEntry)) {

        g_RenderBreadcrumb.numMeshes = *(uint16_t*)((uintptr_t)resEntry + (VER_x32 ? 26 : 0x32));

    }

    g_RenderBreadcrumb.pipeRenderCount++;

#endif



    CheckResEntryValid(resEntry, flags);



    return rxOpenGLDefaultAllInOneRenderCB(resEntry, object, type, flags);

}



// 00677CB4

int (*CCustomBuildingDNPipeline__CustomPipeRenderCB)(RwResEntry* resEntry, uintptr_t object, uint8_t type, uint32_t flags);

int CCustomBuildingDNPipeline__CustomPipeRenderCB_hook(RwResEntry* resEntry, uintptr_t object, uint8_t type, uint32_t flags)

{

    if (!resEntry || !flags || !CCustomBuildingDNPipeline__CustomPipeRenderCB)

        return 0;



#if defined(ENABLE_RENDER_DIAGNOSTICS) || defined(DIAG_BUILD)

    g_RenderBreadcrumb.resEntryPtr = (uintptr_t)resEntry;

    g_RenderBreadcrumb.objectPtr = object;

    g_RenderBreadcrumb.pipeType = (uint32_t)type;

    g_RenderBreadcrumb.pipeFlags = flags;

    RpAtomic* pAtomic = (type == 1 && object && IsValidAddress(object)) ? (RpAtomic*)object : nullptr;

    g_RenderBreadcrumb.geometryPtr = pAtomic ? (uintptr_t)pAtomic->geometry : 0;

    if (resEntry && IsValidAddress((uintptr_t)resEntry)) {

        g_RenderBreadcrumb.numMeshes = *(uint16_t*)((uintptr_t)resEntry + (VER_x32 ? 26 : 0x32));

    }

    g_RenderBreadcrumb.pipeRenderCount++;

#endif



    CheckResEntryValid(resEntry, flags);



    return CCustomBuildingDNPipeline__CustomPipeRenderCB(resEntry, object, type, flags);

}



int (*_Z17RpMaterialDestroyP10RpMaterial)(uintptr_t mat);

int RpMaterialDestroy_hook(uintptr_t mat)

{

    if (mat) return _Z17RpMaterialDestroyP10RpMaterial(mat);

    return 1;

}



int (*CAnimManager_UncompressAnimation)(int result);

int CAnimManager_UncompressAnimation_hook(int result)

{

    if ( result )

        return CAnimManager_UncompressAnimation(result);

    return 0;

}

//  _Z16_rxPacketDestroyP8RxPacket

void (*rxPacketDestroy)(RxPacket* pkt);



void rxPacketDestroy_hook(RxPacket* pkt)

{

    if (!pkt) return;



    Log("rxPacketDestroy hooked");



    /*

    for (int i = 0; i < pkt->numClusters; i++) {

        auto& cl = pkt->clusters[i];

        cl.data = nullptr;

        cl.clusterRef = nullptr;

        cl.numAlloced = 0;

        cl.flags = 0;

    }

    */

    rxPacketDestroy(pkt);

}





extern VehicleAudioPropertiesStruct VehicleAudioProperties[20000];



void (*CRadar_ClearBlip)(uint32_t a2);

void CRadar_ClearBlip_hook(uint32_t a2)

{

    uintptr_t dwRetAddr = 0;

    GET_LR(dwRetAddr);



    if ((uint16_t)a2 > 249) {

        LOGI("[CRadar::ClearBlip]: Invalid blip ID (%d) called from 0x%X", (uint16_t)a2, dwRetAddr);

        return;

    }



    int32 raceBlipIndex = *(int32 *)(g_libGTASA + (VER_x32 ? 0x6E00BC : 0x8BE7E8));

    if (raceBlipIndex == a2) {

        GPS::Set(pGame->m_vecRaceCheckpointPos, pGame->m_bRaceCheckpointsEnabled);

    }



    CRadar_ClearBlip(a2);

}



bool m_bUseSquareRadar = false; // todo: move to settings.ini

void(*DrawRadarMask)();

void DrawRadarMask_hook()

{

    CHook::UnFuck(g_libGTASA + (VER_x32? 0x444510 : 0x710A20));

    if(m_bUseSquareRadar)

        *(float*)(g_libGTASA + (VER_x32? 0x444510 : 0x710A20)) = 0.0001f;

    else

        *(float*)(g_libGTASA + (VER_x32? 0x444510 : 0x710A20)) = 1.5708f;



    DrawRadarMask();

#if !VER_x32

    *(float*)(g_libGTASA + 0x710A20) = 1.5708f;

#endif

}



float (*CRadar__LimitRadarPoint)(float* pos);

float CRadar__LimitRadarPoint_hook(float* pos)

{

    float r, angle;

    r = sqrtf(pos[0] * pos[0] + pos[1] * pos[1]);



    if (m_bUseSquareRadar)

    {

        if (r > 1.0)

        {

            if (pos[0] > -1.0f && pos[0] < 1.0f && pos[1] > -1.0f && pos[1] < 1.0f)

                r = 0.99f;

            else

            {

                angle = atan2f(pos[1], pos[0]) * 57.295779513f + 180.0f;

                if (angle <= 45.0f || angle > 315.0f)

                {

                    pos[0] = 1.0f;

                    pos[1] = sinf(angle / 57.295779513f) * 1.4142135623f;

                }

                else if (angle > 45 && angle <= 135)

                {

                    pos[0] = cosf(angle / 57.295779513f) * 1.4142135623f;

                    pos[1] = 1.0f;

                }

                else if (angle > 135 && angle <= 225)

                {

                    pos[0] = -1.0f;

                    pos[1] = sinf(angle / 57.295779513f) * 1.4142135623f;

                }

                else

                {

                    pos[0] = cosf(angle / 57.295779513f) * 1.4142135623f;

                    pos[1] = -1.0f;

                }



                pos[0] *= (-1.0f);

                pos[1] *= (-1.0f);

            }

        }

        return r;

    }



    return CRadar__LimitRadarPoint(pos);

}



int (*CRadar__DrawRadarSprite)(unsigned int iconID, float x, float y, unsigned int alpha);

int CRadar__DrawRadarSprite_hook(unsigned int iconID, float x, float y, unsigned int alpha)

{

    if(iconID == 4)

    {

        return 0;

    }

    return CRadar__DrawRadarSprite(iconID, x, y, alpha);

}



/* =============================================================================== */



void ReadSettingFile();

void ApplyFPSPatch(uint8_t fps);

void (*NvUtilInit)();

void NvUtilInit_hook()

{

    FLog("NvUtilInit");



    NvUtilInit();



    g_pszStorage = (char*)(g_libGTASA + (VER_x32 ? 0x6D687C : 0x8B46A8)); // StorageRootBuffer



    ReadSettingFile();



    ApplyFPSPatch(999);

}



struct stFile

{

    int isFileExist;

    FILE *f;

};



char lastFile[123];



stFile* NvFOpen(const char* r0, const char* r1, int r2, int r3)

{

    strcpy(lastFile, r1);



    static char path[255]{};

    memset(path, 0, sizeof(path));



    sprintf(path, "%s%s", g_pszStorage, r1);



    // ----------------------------

    if(!strncmp(r1+12, "mainV1.scm", 10))

    {

        sprintf(path, "%sSAMP/main.scm", g_pszStorage);

        FLog("Loading %s", path);

    }

    // ----------------------------

    if(!strncmp(r1+12, "SCRIPTV1.IMG", 12))

    {

        sprintf(path, "%sSAMP/script.img", g_pszStorage);

        FLog("Loading script.img..");

    }

    // ----------------------------

    if(!strncmp(r1, "DATA/PEDS.IDE", 13))

    {

        sprintf(path, "%sSAMP/peds.ide", g_pszStorage);

        FLog("Loading peds.ide..");

    }

    // ----------------------------

    if(!strncmp(r1, "DATA/VEHICLES.IDE", 17))

    {

        sprintf(path, "%sSAMP/vehicles.ide", g_pszStorage);

        FLog("Loading vehicles.ide..");

    }



    if (!strncmp(r1, "DATA/GTA.DAT", 12))

    {

        sprintf(path, "%sSAMP/gta.dat", g_pszStorage);

        FLog("Loading gta.dat..");

    }



    if (!strncmp(r1, "DATA/HANDLING.CFG", 17))

    {

        sprintf(path, "%sSAMP/handling.cfg", g_pszStorage);

        FLog("Loading handling.cfg..");

    }



    if (!strncmp(r1, "DATA/WEAPON.DAT", 15))

    {

        sprintf(path, "%sSAMP/weapon.dat", g_pszStorage);

        FLog("Loading weapon.dat..");

    }



    if (!strncmp(r1, "DATA/FONTS.DAT", 15))

    {

        sprintf(path, "%sdata/fonts.dat", g_pszStorage);

        FLog("Loading weapon.dat..");

    }



    if (!strncmp(r1, "DATA/PEDSTATS.DAT", 15))

    {

        sprintf(path, "%sdata/pedstats.dat", g_pszStorage);

        FLog("Loading weapon.dat..");

    }



    if (!strncmp(r1, "DATA/TIMECYC.DAT", 15))

    {

        sprintf(path, "%sdata/timecyc.dat", g_pszStorage);

        FLog("Loading weapon.dat..");

    }



    if (!strncmp(r1, "DATA/POPCYCLE.DAT", 15))

    {

        sprintf(path, "%sdata/popcycle.dat", g_pszStorage);

        FLog("Loading weapon.dat..");

    }



#if VER_x32

    auto *st = (stFile*)malloc(8);

#else

    auto *st = (stFile*)malloc(0x10);

#endif

    st->isFileExist = false;



    FILE *f  = fopen(path, "rb");



    if(f)

    {

        st->isFileExist = true;

        st->f = f;

        return st;

    }

    else

    {

        FLog("NVFOpen hook | Error: file not found (%s)", path);

        free(st);

        return nullptr;

    }

}



bool g_bPlaySAMP = false;



void MainMenu_OnStartSAMP()

{

    if(g_bPlaySAMP) return;



    //InitInMenu();

    pGame->StartGame();



    // StartGameScreen::OnNewGameCheck()

    (( void (*)())(g_libGTASA + (VER_x32 ? 0x002A7270 + 1 : 0x365EA0)))();



    g_bPlaySAMP = true;

}



unsigned int (*MainMenuScreen__Update)(uintptr_t thiz, float a2);

unsigned int MainMenuScreen__Update_hook(uintptr_t thiz, float a2)

{

    unsigned int ret = MainMenuScreen__Update(thiz, a2);

    MainMenu_OnStartSAMP();

    return ret;

}



void (*StartGameScreen__OnNewGameCheck)();

void StartGameScreen__OnNewGameCheck_hook()

{

    // отключить кнопку начать игру

    if(g_bPlaySAMP)

        return;



}



static bool IsAnimGroupLoaded(int animGroup)

{

    if (animGroup < 0)

        return false;



    if (CAnimManager::ms_aAnimAssocGroups && animGroup < CAnimManager::ms_numAnimAssocDefinitions) {

        if (CAnimManager::ms_aAnimAssocGroups[animGroup].m_pAssociations)

            return true;

    }



    uintptr_t gotAddr = g_libGTASA + (VER_x32 ? 0x00677F80 : 0x84DF30);

    uintptr_t pVar = *(uintptr_t*)gotAddr;

    if (pVar) {

        uintptr_t pGroups = *(uintptr_t*)pVar;

        if (pGroups) {

            uintptr_t group = pGroups + (size_t)animGroup * (VER_x32 ? 0x14 : 0x20);

            uintptr_t pAssocs = *(uintptr_t*)(group + (VER_x32 ? 0x04 : 0x08));

            if (pAssocs)

                return true;

        }

    }



    return false;

}



void (*CTaskSimpleUseGun__RemoveStanceAnims)(uintptr* thiz, void* ped, float a3);

void CTaskSimpleUseGun__RemoveStanceAnims_hook(uintptr* thiz, void* ped, float a3)

{

    if (!thiz || !CTaskSimpleUseGun__RemoveStanceAnims)

        return;



#if VER_x32

    uintptr_t m_pAnim = *(uintptr_t*)((uintptr_t)thiz + 0x2c);

    if (m_pAnim) {

        if (!*(uintptr_t*)(m_pAnim + 0x14))

            return;

    }

#else

    uintptr_t m_pAnim = *(uintptr_t*)((uintptr_t)thiz + 0x40);

    if (m_pAnim) {

        if (!*(uintptr_t*)(m_pAnim + 0x28))

            return;

    }

#endif

    CTaskSimpleUseGun__RemoveStanceAnims(thiz, ped, a3);

}



void (*CTaskSimpleUseGun__StartAnim)(uintptr_t* thiz, void* ped) = nullptr;

void CTaskSimpleUseGun__StartAnim_hook(uintptr_t* thiz, void* ped)

{

    if (!thiz || !ped || !CTaskSimpleUseGun__StartAnim)

        return;



    uintptr_t pWeaponInfo = *(uintptr_t*)((uintptr_t)thiz + (VER_x32 ? 0x30 : 0x48));

    if (pWeaponInfo) {

        int animGroup = *(int*)(pWeaponInfo + 0x1c);

        if (animGroup >= 0 && !IsAnimGroupLoaded(animGroup)) {

            // Weapon animations not loaded yet.

            // Skipping StartAnim until streaming loads the IFP prevents the fatal

            // SIGSEGV crash inside CAnimManager::BlendAnimation (fault 0x1c0e).

            return;

        }

    }



    CTaskSimpleUseGun__StartAnim(thiz, ped);

}



CAnimBlendAssociation* (*CAnimManager__BlendAnimation2)(RpClump* clump, AssocGroupId groupId, AnimationId animId, float clumpAssocBlendData) = nullptr;

CAnimBlendAssociation* CAnimManager__BlendAnimation2_hook(RpClump* clump, AssocGroupId groupId, AnimationId animId, float clumpAssocBlendData)

{

    if (!clump || !CAnimManager__BlendAnimation2)

        return nullptr;



    if (!IsAnimGroupLoaded((int)groupId))

        return nullptr;



    return CAnimManager__BlendAnimation2(clump, groupId, animId, clumpAssocBlendData);

}



void (*CAnimBlendAssociation__SetFinishCallback)(void* thiz, void (*callback)(void*, void*), void* data) = nullptr;

void CAnimBlendAssociation__SetFinishCallback_hook(void* thiz, void (*callback)(void*, void*), void* data)

{

    if (!thiz)

        return;

    if (CAnimBlendAssociation__SetFinishCallback)

        CAnimBlendAssociation__SetFinishCallback(thiz, callback, data);

}



int (*CCollision__ProcessVerticalLine)(const void *a1, const void *a2, void *a3, void *a4, float *a5, bool a6, bool a7, void *a8);

int CCollision__ProcessVerticalLine_hook(const void *a1, const void *a2, void *a3, void *a4, float *a5, bool a6, bool a7, void *a8)

{

    int result;

    if (a3 && CCollision__ProcessVerticalLine)

        result = CCollision__ProcessVerticalLine(a1, a2, a3, a4, a5, a6, a7, a8);

    else

        result = 0;

    return result;

}



int(*CUpsideDownCarCheck__IsCarUpsideDown)(int, int);

int CUpsideDownCarCheck__IsCarUpsideDown_hook(int a1, int a2)

{

    /* Passengers leave the vehicle out of fear if it overturns */



    return 0;

}



int (*CTaskSimpleGetUp__ProcessPed)(uintptr_t* thiz, CPedGTA* ped);

int CTaskSimpleGetUp__ProcessPed_hook(uintptr_t* thiz, CPedGTA* ped)

{

    //return false;

    if(!ped)return 0;

    int res = 0;

    try {

        res = CTaskSimpleGetUp__ProcessPed(thiz, ped);

    }

    catch(...) {

        return 0;

    }



    return res;

}



int64 getmip()

{

    return 1;

}



uint64_t* RQCommand_rqSetAlphaTest(uint64_t *result)

{

    *result += 8;

    return result;

}



int64 GetInputType(void)

{

    return 0LL;

}



int(*CAnimBlendNode__FindKeyFrame)(int, float, int, int);

int CAnimBlendNode__FindKeyFrame_hook(int a1, float a2, int a3, int a4)

{

    if (*(uintptr_t*)(a1 + 16))

    {

        return CAnimBlendNode__FindKeyFrame(a1, a2, a3, a4);

    }

    else return 0;

}



RwFrame* CClumpModelInfo_GetFrameFromId_Post(RwFrame* pFrameResult, RpClump* pClump, int id)

{

    if (pFrameResult)

        return pFrameResult;



    uintptr_t calledFrom = 0;

    __asm__ volatile ("mov %0, lr" : "=r" (calledFrom));

    calledFrom -= g_libGTASA;



    if (calledFrom == 0x00515708                // CVehicle::SetWindowOpenFlag

        || calledFrom == 0x00515730             // CVehicle::ClearWindowOpenFlag

        || calledFrom == 0x00338698             // CVehicleModelInfo::GetOriginalCompPosition

        || calledFrom == 0x00338B2C)            // CVehicleModelInfo::CreateInstance

        return nullptr;



    for (uint i = 2; i < 40; i++)

    {

        RwFrame* pNewFrameResult = nullptr;

        uint     uiNewId = id + (i / 2) * ((i & 1) ? -1 : 1);



        pNewFrameResult = ((RwFrame * (*)(RpClump * pClump, int id))(g_libGTASA + (VER_2_1 ? 0x003856D0 : 0x00335CC0) + 1))(pClump, i);



        if (pNewFrameResult)

        {

            return pNewFrameResult;

        }

    }



    return nullptr;

}

RwFrame* (*CClumpModelInfo_GetFrameFromId)(RpClump*, int);

RwFrame* CClumpModelInfo_GetFrameFromId_hook(RpClump* a1, int a2)

{

    return CClumpModelInfo_GetFrameFromId_Post(CClumpModelInfo_GetFrameFromId(a1, a2), a1, a2);

}



void (*FxEmitterBP_c__Render)(uintptr_t* a1, int a2, int a3, float a4, char a5);

void FxEmitterBP_c__Render_hook(uintptr_t* a1, int a2, int a3, float a4, char a5)

{

    if(!a1 || !a2) return;

    uintptr_t* temp = *((uintptr_t**)a1 + 3);

    if (!temp)

    {

        return;

    }

    FxEmitterBP_c__Render(a1, a2, a3, a4, a5);

}



bool (*RwResourcesFreeResEntry)(void* entry);

bool RwResourcesFreeResEntry_hook(void* entry)

{

    bool result;

    if (entry) result = RwResourcesFreeResEntry(entry);

    else result = false;

    return result;

}



static uint32_t dwRLEDecompressSourceSize = 0;



size_t (*OS_FileRead)(OSFile a1, void *buffer, size_t numBytes);

size_t OS_FileRead_hook(OSFile a1, void *buffer, size_t numBytes)

{

    dwRLEDecompressSourceSize = numBytes;



    return OS_FileRead(a1, buffer, numBytes);

}



uint8_t* (*RLEDecompress)(uint8_t* pDest, size_t uiDestSize, uint8_t const* pSrc, size_t uiSegSize, uint32_t uiEscape);

uint8_t* RLEDecompress_hook(uint8_t* pDest, size_t uiDestSize, const uint8_t* pSrc, size_t uiSegSize, uint32_t uiEscape) {

    if (!pDest) return pDest;

    if (!pSrc) return pDest;



    if (dwRLEDecompressSourceSize == 0) {

        if (RLEDecompress) return RLEDecompress(pDest, uiDestSize, pSrc, uiSegSize, uiEscape);

        return pDest;

    }



    uint8_t* pTempDest = pDest;

    const uint8_t* pTempSrc = pSrc;

    uint8_t* pEndOfDest = &pDest[uiDestSize];

    auto* pEndOfSrc = (uint8_t*)&pSrc[dwRLEDecompressSourceSize];



    if (pDest < pEndOfDest) {

        do {

            if (*pTempSrc == uiEscape) {

                uint8_t ucCurSeg = pTempSrc[1];

                if (ucCurSeg) {

                    uint8_t* ucCurDest = pTempDest;

                    uint8_t ucCount = 0;

                    do {

                        ++ucCount;

                        if (ucCurDest + uiSegSize <= pEndOfDest && pTempSrc + 2 + uiSegSize <= pEndOfSrc) {

                            memcpy(ucCurDest, pTempSrc + 2, uiSegSize);

                        }

                        ucCurDest += uiSegSize;

                    } while (ucCurSeg != ucCount && ucCurDest < pEndOfDest);



                    pTempDest += uiSegSize * ucCurSeg;

                }

                pTempSrc += 2 + uiSegSize;

            } else {

                if (pTempSrc + uiSegSize > pEndOfSrc || pTempDest + uiSegSize > pEndOfDest)

                    break;

                memcpy(pTempDest, pTempSrc, uiSegSize);

                pTempDest += uiSegSize;

                pTempSrc += uiSegSize;

            }

        } while (pEndOfDest > pTempDest && pTempSrc < pEndOfSrc);

    }



    dwRLEDecompressSourceSize = 0;

    return pDest;

}





float (*CDraw__SetFOV)(float thiz, float a2);

float CDraw__SetFOV_hook(float thiz, float a2)

{

    float tmp = (float)((float)((float)(*(float *)&*(float *)(g_libGTASA + (VER_x32 ? 0x00A26A90 : 0xCC7F00)) - 1.3333) * 11.0) / 0.44444) + thiz;

    if(tmp > 100) tmp = 100.0;

    *(float *)(g_libGTASA + (VER_x32 ? 0x006B1CB8 : 0x88E6BC)) = tmp;

    return thiz;

}



void(*CStreaming__Init2)();

void CStreaming__Init2_hook()

{

    CStreaming__Init2();

    CStreaming::ms_memoryAvailable = 256 * 1024 * 1024;



    //*(uint32_t*)(g_libGTASA+(VER_x32 ? 0x00685FA0:0x85EBD8)) = 268435456;

    // try this

    //*(uint32_t*)(g_libGTASA+(VER_x32 ? 0x0046BE88:0x55774C)) = 268435456;

}





bool(*CPools_Load)();

bool CPools_Load_hook()

{

    int numPeds = CPools::ms_pPedPool ? CPools::ms_pPedPool->GetSize() : 0;

    int numVehicles = CPools::ms_pVehiclePool ? CPools::ms_pVehiclePool->GetSize() : 0;



    FLog("Peds: %d, Vehicles: %d", numPeds, numVehicles);



    if(numPeds > 299 && numVehicles > 611)

        FLog("large pools detected");



    return CPools_Load();

}



int(*mpg123_param)(void* mh, int key, long val, int ZERO, double fval);

int mpg123_param_hook(void* mh, int key, long val, int ZERO, double fval)

{

    // 0x2000 = MPG123_SKIP_ID3V2

    // 0x200  = MPG123_FUZZY

    // 0x100  = MPG123_SEEKBUFFER

    // 0x40   = MPG123_GAPLESS

    return mpg123_param(mh, key, val | (0x2000 | 0x200 | 0x100 | 0x40), ZERO, fval);

}



#include "Widgets/TouchInterface.h"

#include "game/Mobile/MobileSettings/MobileSettings.h"

void InjectHooks()

{

    FLog("InjectHooks");

    CHook::Write(g_libGTASA + (VER_x32 ? 0x678954 : 0x84F2D0), &Scene);



#if !VER_x32

    CHook::RET("_ZN11CPlayerInfo14LoadPlayerSkinEv");

    CHook::RET("_ZN11CPopulation10InitialiseEv");

#endif

    CCustomCarEnvMapPipeline::InjectHooks();

    CCamera::InjectHooks(); //

    CReferences::InjectHooks(); //

    CModelInfo::injectHooks(); //

    CTimer::InjectHooks(); //

    //cTransmission::InjectHooks(); //

    CAnimBlendAssociation::InjectHooks(); //

    //cHandlingDataMgr::InjectHooks(); //

    CPools::InjectHooks(); //

   // CHook::InlineHook("_ZN6CPools4LoadEv", CPools_Load, CPools_Load_hook);

    CVehicleGTA::InjectHooks(); //

    CMatrixLink::InjectHooks(); //

    CMatrixLinkList::InjectHooks(); //

    //CHook::InlineHook("_ZN10CStreaming5Init2Ev", &CStreaming__Init2_hook, &CStreaming__Init2);

    CStreaming::InjectHooks();

    CPlaceable::InjectHooks(); //

    CMatrix::InjectHooks(); //

    CCollision::InjectHooks(); //

    //CIdleCam::InjectHooks(); //

    CTouchInterface::InjectHooks(); //

    CWidgetGta::InjectHooks();

    CEntityGTA::InjectHooks(); //

    CPhysical::InjectHooks(); //

    CAnimManager::InjectHooks(); //

    //CCarEnterExit::InjectHooks();

    CPlayerPedGta::InjectHooks(); //

    CTaskManager::InjectHooks(); //

    //CPedIntelligence::InjectHooks(); //

    CWorld::InjectHooks(); //

    CGame::InjectHooks();

    ES2VertexBuffer::InjectHooks();

    CRQ_Commands::InjectHooks();

    CTxdStore::InjectHooks();

    CVisibilityPlugins::InjectHooks();

    //CAdjustableHUD::InjectHooks();

    // new

    //CClouds::InjectHooks();

    //CWeather::InjectHooks();

    //RenderBuffer::InjectHooks();

    CTimeCycle::InjectHooks();

    CCoronas::InjectHooks();

    //CDraw::InjectHooks();

    //CClock::InjectHooks();

    //CBirds::Init();

    CVehicleModelInfo::InjectHooks();

    //CPathFind::InjectHooks();

    CSprite2d::InjectHooks();

    //CFileLoader::InjectHooks();

    //CShadows::InjectHooks();

    CPickups::InjectHooks();

    CRenderer::InjectHooks();

    CStreamingInfo::InjectHooks();

    TextureDatabase::InjectHooks();

    TextureDatabaseEntry::InjectHooks();

    TextureDatabaseRuntime::InjectHooks();

    CCustomBuildingDNPipeline::InjectHooks();



    CMobileMenu::InjectHooks();

    CMobileSettings::InjectHooks();



    CHook::Write(g_libGTASA+(VER_x32 ? 0xA41140 : 0xCE3EE8), &COcclusion::aOccluders);

    CHook::Write(g_libGTASA+(VER_x32 ? 0xA45790:0xCE8538), &COcclusion::NumOccludersOnMap);

}



uintptr_t (*GetMeshPriority)(uintptr_t);

uintptr_t GetMeshPriority_hook(uintptr_t rpMesh)

{

    if (rpMesh)

    {

        RpMaterial *rpMeshMat = *(RpMaterial **)(rpMesh + (VER_x32 ? 8 : 16));

        if (rpMeshMat && (uintptr_t)rpMeshMat >= 0x1000)

        {

            if (rpMeshMat->texture && (uintptr_t)rpMeshMat->texture >= 0x1000)

            {

                if (!rpMeshMat->texture->raster || (uintptr_t)rpMeshMat->texture->raster < 0x1000)

                    return 0;

            }

        }

    }

    return GetMeshPriority(rpMesh);

}



// _ZN18CVisibilityPlugins19InitAlphaEntityListEv



void (*CRenderer__ScanWorld)(CRenderer* self);



void CRenderer__ScanWorld_hook(CRenderer* self)

{

    volatile uint16_t* ms_nCurrentScanCode = reinterpret_cast<volatile uint16_t*>(g_libGTASA + (VER_x32 ? 0x0098960C + 1 : 0x00C18350));



    if (*ms_nCurrentScanCode == 0xFFFF) {

        FLog("ScanWorld: ms_nCurrentScanCode == 0xFFFF");

        *ms_nCurrentScanCode = 0;

    }



    CRenderer__ScanWorld(self);

}



// void InstallHooks(); // Forward declaration removed







void InstallSpecialHooks()

{

    InjectHooks();

    // InstallHooks(); // Dictionary removed to fix black screen



    CHook::InstallPLT(g_libGTASA + (VER_x32 ? 0x6701D4 : 0x840708), &RLEDecompress_hook, &RLEDecompress);



    CHook::Redirect("_ZN5CGame20InitialiseRenderWareEv", &CGame::InitialiseRenderWare);

    CHook::InstallPLT(g_libGTASA + (VER_x32 ? 0x6785FC : 0x84EC20), &StartGameScreen__OnNewGameCheck_hook, &StartGameScreen__OnNewGameCheck);



    CHook::InlineHook("_Z10NvUtilInitv", &NvUtilInit_hook, &NvUtilInit);



    CHook::RET("_ZN12CCutsceneMgr16LoadCutsceneDataEPKc"); // LoadCutsceneData

    CHook::RET("_ZN12CCutsceneMgr10InitialiseEv");			// CCutsceneMgr::Initialise



    CHook::Redirect("_Z7NvFOpenPKcS0_bb", &NvFOpen);



    CHook::InlineHook("_ZN14MainMenuScreen6UpdateEf", &MainMenuScreen__Update_hook, &MainMenuScreen__Update);



    CHook::InlineHook("_ZN10CStreaming5Init2Ev", &CStreaming__Init2_hook, &CStreaming__Init2);



    CHook::RET("_ZN4CPed31RemoveWeaponWhenEnteringVehicleEi"); // CPed::RemoveWeaponWhenEnteringVehicle

    CHook::InlineHook("_Z11OS_FileReadPvS_i", &OS_FileRead_hook, &OS_FileRead);

    CHook::InlineHook("_Z32_rxOpenGLDefaultAllInOneRenderCBP10RwResEntryPvhj", &rxOpenGLDefaultAllInOneRenderCB_hook, &rxOpenGLDefaultAllInOneRenderCB);

    if (!Diag_IsNoHookPipelines()) {

        CHook::InlineHook("_ZN25CCustomBuildingDNPipeline18CustomPipeRenderCBEP10RwResEntryPvhj", &CCustomBuildingDNPipeline__CustomPipeRenderCB_hook, &CCustomBuildingDNPipeline__CustomPipeRenderCB);

        CHook::InlineHook("_ZN23CCustomBuildingPipeline18CustomPipeRenderCBEP10RwResEntryPvhj", &CCustomBuildingPipeline__CustomPipeRenderCB_hook, &CCustomBuildingPipeline__CustomPipeRenderCB);

        CHook::InlineHook("_ZN24CCustomCarEnvMapPipeline18CustomPipeRenderCBEP10RwResEntryPvhj", &CCustomCarEnvMapPipeline__CustomPipeRenderCB_hook, &CCustomCarEnvMapPipeline__CustomPipeRenderCB);

    } else {

        LOGI("[HOOK] Pipelines hooks skipped by bisect switch nohook_pipelines");

    }



    CHook::InlineHook("_Z17RpMaterialDestroyP10RpMaterial", &RpMaterialDestroy_hook, &_Z17RpMaterialDestroyP10RpMaterial);



    CHook::InlineHook("_Z15GetMeshPriorityPK6RpMesh", GetMeshPriority_hook, &GetMeshPriority);

    CHook::InlineHook("_ZN4CPed14GetWeaponSkillE11eWeaponType", &CPed__GetWeaponSkill_hook, &CPed__GetWeaponSkill);

    

    // --- WEAPON & BULLET SYNC HOOKS (Installed exactly once) ---

    if (!Diag_IsNoWeaponHooks()) {

        LOGI("[HOOK] Installing Weapon & Bullet Sync hooks...");

        

        // FireInstantHit

        CHook::InlineHook("_ZN7CWeapon14FireInstantHitEP7CEntityP7CVectorS3_S1_S3_S3_bb",

                          &CWeapon__FireInstantHit_hook, &CWeapon__FireInstantHit);

                          

        // FireSniper

        CHook::InlineHook("_ZN7CWeapon10FireSniperEP4CPedP7CEntityP7CVector", &CWeapon__FireSniper_hook,

                          &CWeapon__FireSniper);

                          

        // ProcessLineOfSight (World)

        CHook::InlineHook("_ZN6CWorld18ProcessLineOfSightERK7CVectorS2_R9CColPointRP7CEntitybbbbbbbb",

                          &CWorld__ProcessLineOfSight_hook, &CWorld__ProcessLineOfSight);

                          

        // ComputeDamageResponse

        CHook::InlineHook("_ZN28CPedDamageResponseCalculator21ComputeDamageResponseEP4CPedR18CPedDamageResponseb",

                 &CPedDamageResponseCalculator__ComputeDamageResponse_hook,

                 &CPedDamageResponseCalculator__ComputeDamageResponse);

                 

        // ProcessLineOfSight (Weapon)

        CHook::InlineHook("_ZN7CWeapon18ProcessLineOfSightERK7CVectorS2_R9CColPointRP7CEntity11eWeaponTypeS6_bbbbbbb",

                &CWeapon__ProcessLineOfSight_hook, &CWeapon__ProcessLineOfSight);

                

        // AddBullet

        CHook::InlineHook("_ZN11CBulletInfo9AddBulletEP7CEntity11eWeaponType7CVectorS3_",

                          &CBulletInfo_AddBullet_hook, &CBulletInfo_AddBullet);



        // Collision ProcessVerticalLine

        CHook::InlineHook("_ZN10CCollision19ProcessVerticalLineERK8CColLineRK7CMatrixR9CColModelR9CColPointRfbbP15CStoredCollPoly",

                &CCollision__ProcessVerticalLine_hook, &CCollision__ProcessVerticalLine);



        // CTaskSimpleUseGun

        CHook::InlineHook("_ZN17CTaskSimpleUseGun17RemoveStanceAnimsEP4CPedf",

                          &CTaskSimpleUseGun__RemoveStanceAnims_hook,

                          &CTaskSimpleUseGun__RemoveStanceAnims);

        CHook::InlineHook("_ZN17CTaskSimpleUseGun9StartAnimEP4CPed",

                          &CTaskSimpleUseGun__StartAnim_hook,

                          &CTaskSimpleUseGun__StartAnim);

        CHook::InlineHook("_ZN12CAnimManager14BlendAnimationEP7RpClump12AssocGroupId11AnimationIdf",

                          &CAnimManager__BlendAnimation2_hook,

                          &CAnimManager__BlendAnimation2);

        CHook::InlineHook("_ZN21CAnimBlendAssociation17SetFinishCallbackEPFvPS_PvES1_",

                          &CAnimBlendAssociation__SetFinishCallback_hook,

                          &CAnimBlendAssociation__SetFinishCallback);

                          

        LOGI("[HOOK] Weapon & Bullet Sync hooks installed");

    } else {

        LOGI("[HOOK] Weapon & bullet sync hooks skipped by bisect switch no_weapon_hooks");

    }



    // CWorld::ProcessPedsAfterPreRender (Moved from CWorld::InjectHooks)

    CHook::InstallPLT(g_libGTASA + (VER_x32 ? 0x675C58 : 0x849A20), &CWorld::ProcessPedsAfterPreRender);

}



/*signed int (*OS_FileOpen)(unsigned int a1, int* a2, const char* a3, int a4);



signed int OS_FileOpen_hook(unsigned int a1, int* a2, const char* a3, int a4)

{

    uintptr_t calledFrom = 0;



#ifdef __aarch64__

    __asm__ volatile("mov %0, x30" : "=r"(calledFrom));

#else

    __asm__ volatile("mov %0, lr" : "=r"(calledFrom));

#endif



    calledFrom -= g_libGTASA;



    signed int retn = OS_FileOpen(a1, a2, a3, a4);



    if (

#ifdef __aarch64__

calledFrom == 0x00284278

#else

        calledFrom == 0x001E8124 + 1

#endif

            )

    {

        if (isEncrypted(a3))

        {

            lastOpenedFile = *a2;

        }

    }



    return retn;

}*/









#include <EGL/egl.h>

#include <GLES2/gl2.h>   // If using OpenGL ES 2.0 or 3.0



void InstallHooks() {

    CHook::Redirect("_Z13Render2dStuffv", &Render2dStuff);

    CHook::Redirect("_Z13RenderEffectsv", &RenderEffects);

    CHook::InlineHook("_Z14AND_TouchEventiiii", &AND_TouchEvent_hook, &AND_TouchEvent);



    CHook::Redirect("_ZN11CHudColours12GetIntColourEh", &CHudColours__GetIntColour); // dangerous

    CHook::Redirect("_ZN6CRadar19GetRadarTraceColourEjhh",

                    &CRadar__GetRadarTraceColor); // dangerous

    CHook::InlineHook("_ZN6CRadar12SetCoordBlipE9eBlipType7CVectorj12eBlipDisplayPc",

                      &CRadar__SetCoordBlip_hook, &CRadar__SetCoordBlip);

    CHook::InlineHook("_ZN6CRadar20DrawRadarGangOverlayEb", &CRadar_DrawRadarGangOverlay_hook,

                      &CRadar_DrawRadarGangOverlay);



    CHook::Redirect("_Z10GetTexturePKc", &CUtil::GetTexture);



    CHook::InlineHook("_ZN14MainMenuScreen6OnExitEv", &MainMenuScreen__OnExit_hook,

                      &MainMenuScreen__OnExit);



    // Note: CTaskSimpleUseGun, CWeapon and CCollision hooks are installed once in InstallSpecialHooks()



    CHook::InlineHook("_ZN6CRadar9ClearBlipEi", &CRadar_ClearBlip_hook, &CRadar_ClearBlip);



    CHook::InlineHook("_ZN19CUpsideDownCarCheck15IsCarUpsideDownEPK8CVehicle",

                      &CUpsideDownCarCheck__IsCarUpsideDown_hook,

                      &CUpsideDownCarCheck__IsCarUpsideDown);



    CHook::InlineHook("_ZN16CTaskSimpleGetUp10ProcessPedEP4CPed",

                      &CTaskSimpleGetUp__ProcessPed_hook,

                      &CTaskSimpleGetUp__ProcessPed); // CTaskSimpleGetUp::ProcessPed

    CHook::InlineHook("_ZN7CObject6RenderEv", &CObject_Render_hook, &CObject_Render);



    CHook::Redirect("_Z19PlayerIsEnteringCarv", &PlayerIsEnteringCar);

    if (*(uint8_t *) (g_libGTASA + (VER_x32 ? 0x6B8B9C : 0x896135))) {

        CHook::Redirect("_ZNK14TextureListing11GetMipCountEv", &getmip);

    }



    if (!eglGetProcAddress("glAlphaFuncQCOM")) {

        // If "glAlphaFuncQCOM" is not available, try "glAlphaFunc"



        if (eglGetProcAddress("glAlphaFunc")) {

            // If "glAlphaFunc" is found, store the address in the global library

            *((void **) (g_libGTASA +

                         (VER_x32 ? 0x6BCBF8 : 0x89A1B0))) = (void *) eglGetProcAddress(

                    "glAlphaFunc");

        } else {

            // If neither function is available, hook the fallback symbol

            CHook::Redirect("_Z25RQ_Command_rqSetAlphaTestRPc", &RQCommand_rqSetAlphaTest);

        }

    }



    CHook::Redirect("_ZN4CHID12GetInputTypeEv", &GetInputType);



#if VER_x32

    CHook::InlineHook("_ZN14CAnimBlendNode12FindKeyFrameEf", &CAnimBlendNode__FindKeyFrame_hook, &CAnimBlendNode__FindKeyFrame);

    CHook::InlineHook("_ZN15CClumpModelInfo14GetFrameFromIdEP7RpClumpi", &CClumpModelInfo_GetFrameFromId_hook, &CClumpModelInfo_GetFrameFromId);

#endif



    CHook::InlineHook("_ZN13FxEmitterBP_c6RenderEP8RwCamerajfh", &FxEmitterBP_c__Render_hook,

                      &FxEmitterBP_c__Render);

    CHook::InlineHook("_Z23RwResourcesFreeResEntryP10RwResEntry", &RwResourcesFreeResEntry_hook,

                      &RwResourcesFreeResEntry);



    ms_fAspectRatio = (float *) (g_libGTASA + (VER_x32 ? 0xA26A90 : 0xCC7F00));

    CHook::InlineHook("_ZN4CHud14DrawCrossHairsEv", &DrawCrosshair_hook, &DrawCrosshair);



    // retexture

    CHook::InlineHook("_ZN7CEntity6RenderEv", &CEntity_Render_hook, &CEntity_Render);



#if VER_x32

    CHook::UnFuck(g_libGTASA + 0x4DD9E8);

    *(float*)(g_libGTASA + 0x4DD9E8) = 0.015f;

#else

    CHook::Write(g_libGTASA + 0x5DF790, 0x90000AA9);

    CHook::Write(g_libGTASA + 0x5DF794, 0xBD48D521);

#endif



    CHook::InlineHook("_ZN5CDraw6SetFOVEfb", &CDraw__SetFOV_hook, &CDraw__SetFOV);



    CHook::InlineHook("mpg123_param", &mpg123_param_hook, &mpg123_param);



    CHook::InlineHook("_ZN4CPad27CycleCameraModeDownJustDownEv",

                      CPad__CycleCameraModeDownJustDown_hook, &CPad__CycleCameraModeDownJustDown);



    CHook::InlineHook("_ZN4CCam7ProcessEv", CCam__Process_hook, &CCam__Process);



#if VER_x32

    CHook::NOP(g_libGTASA + 0x002A4A62, 2); // Game - SocialClub

#else

    CHook::NOP(g_libGTASA + 0x003634A4, 1); // Game - SocialClub

#endif

    //CHook::InlineHook("_ZN6CRadar9ClearBlipEi", CRadar__ClearBlip_hook, &CRadar__ClearBlip);

    CHook::InlineHook("_ZN6CRadar13DrawRadarMaskEv", &DrawRadarMask_hook, &DrawRadarMask);

    CHook::InlineHook("_ZN6CRadar15LimitRadarPointER9CVector2D", &CRadar__LimitRadarPoint_hook, &CRadar__LimitRadarPoint);

    CHook::InlineHook("_ZN6CRadar15DrawRadarSpriteEtffh", (uintptr_t) CRadar__DrawRadarSprite_hook,

                      (uintptr_t *) &CRadar__DrawRadarSprite);





    //CHook::InlineHook("_Z16_rxPacketDestroyP8RxPacket", rxPacketDestroy_hook, rxPacketDestroy);



    //CHook::InlineHook("_ZN18CVisibilityPlugins19InitAlphaEntityListEv", CVisibilityPlugins__InitAlphaEntityList_hook, CVisibilityPlugins__InitAlphaEntityList);



    //CHook::InlineHook("_ZN9CRenderer9ScanWorldEv", CRenderer__ScanWorld_hook, CRenderer__ScanWorld);



    CHook::InlineHook("_ZN16cHandlingDataMgr13FindExactWordEPcS0_ii", &cHandlingDataMgr__FindExactWord_hook, &cHandlingDataMgr__FindExactWord);

	//CHook::Redirect("_ZN16cHandlingDataMgr22ConvertDataToGameUnitsEP13tHandlingData", &cHandlingDataMgr__ConvertDataToGameUnits);

    HookCPad();

}


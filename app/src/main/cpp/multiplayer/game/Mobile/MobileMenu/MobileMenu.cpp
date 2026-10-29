//
// Upd by GLORY on 12.12.2025.
//

#include "MobileMenu.h"
#include "patch.h"
#include "game/game.h"
#include "../../net/netgame.h"
#include "../../java/jniutil.h"
extern CNetGame* pNetGame;
extern CJavaWrapper* pJavaWrapper;

CMobileMenu* gMobileMenu;
bool MobileMenuBool = false;

void (*InitForPause)(uintptr* thiz);
void (*CMobileMenu__Unload)(uintptr* thiz);
void (*CMobileMenu__Initiliaze)(uintptr* thiz);
void (*CMobileMenu__0CMobileMenu)(uintptr* thiz);

bool bAFKInitialized = false;

void InitForPause_hook(uintptr* thiz)
{
    if(!bAFKInitialized) {
        MobileMenuBool = true;
        bAFKInitialized = true;
        pJavaWrapper->HideHud();
        pJavaWrapper->HideSpeed();
    }
    InitForPause(thiz);
}

void CMobileMenu__Unload_hook(uintptr* thiz) {
    MobileMenuBool = false;
    bAFKInitialized = false;
    pJavaWrapper->ShowHud();
    pJavaWrapper->HideSpeed();
    CMobileMenu__Unload(thiz);
}

void CMobileMenu__Initiliaze_hook(uintptr* thiz) {
    MobileMenuBool = false;
    bAFKInitialized = false;
    CMobileMenu__Initiliaze(thiz);
}

void CMobileMenu__0CMobileMenu_hook(uintptr* thiz) {
    MobileMenuBool = false;
    bAFKInitialized = false;
    CMobileMenu__0CMobileMenu(thiz);
}

extern "C" void OpenMobileMap() {

    if (!gMobileMenu) {
        gMobileMenu = (CMobileMenu*)(g_libGTASA + (VER_x32 ? 0x006E0074 : 0x8BE780));
    }
    if (gMobileMenu && InitForPause) {
        InitForPause_hook((uintptr*)gMobileMenu);
        #if VER_x32
        *(uint8_t*)((uintptr_t)gMobileMenu + 0x74) = 1; // DisplayingMap
        *(uint8_t*)((uintptr_t)gMobileMenu + 0x75) = 1; // isMapMode
        #else
        *(uint8_t*)((uintptr_t)gMobileMenu + 0x8C) = 1; // DisplayingMap
        *(uint8_t*)((uintptr_t)gMobileMenu + 0x8D) = 1; // isMapMode
        #endif
        gMobileMenu->DisplayingMap = true;
        gMobileMenu->isMapMode = true;
    }
}

void CMobileMenu::InjectHooks() {
    gMobileMenu = (CMobileMenu*)(g_libGTASA + (VER_x32 ? 0x006E0074 : 0x8BE780));

    CHook::InlineHook("_ZN10MobileMenu12InitForPauseEv", &InitForPause_hook, &InitForPause);
    CHook::InlineHook("_ZN10MobileMenu6UnloadEv", &CMobileMenu__Unload_hook, &CMobileMenu__Unload);
    //CHook::InlineHook("_ZN10MobileMenuD2Ev", &CMobileMenu__0CMobileMenu_hook, &CMobileMenu__0CMobileMenu);
    //CHook::InlineHook("j__ZN10MobileMenu10InitializeEv", &CMobileMenu__Initiliaze_hook, &CMobileMenu__Initiliaze);
}
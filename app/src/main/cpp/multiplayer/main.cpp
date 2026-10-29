#include <jni.h>

#include <pthread.h>

#include <syscall.h>



#include "main.h"

#include "diag.h"

#include "game/game.h"

#include "net/netgame.h"

#include "gui/gui.h"

#include "playertags.h"

#include "audiostream.h"

#include "java/jniutil.h"

#include <dlfcn.h>

#include "StackTrace.h"

#include "glory.h"



// voice

#include "voice_new/Plugin.h"



#include "vendor/armhook/patch.h"

#include "vendor/str_obfuscator/str_obfuscator.hpp"



#include "settings.h"



#include "crashlytics.h"



#include "game/CGPS.hpp"

#include "game/CFirstPersonCamera.hpp"

#include "game/Widgets/TouchInterface.h"



/*

Peerapol Unarak

*/



JavaVM* javaVM;



char* g_pszStorage = nullptr;



UI* pUI = nullptr;

CGame *pGame = nullptr;



CNetGame *pNetGame = nullptr;

CPlayerTags* pPlayerTags = nullptr;

CSnapShotHelper* pSnapShotHelper = nullptr;

CAudioStream* pAudioStream = nullptr;

CJavaWrapper* pJavaWrapper = nullptr;

CSettings* pSettings = nullptr;

//CVoice* pVoice = nullptr;

GPS*      pGPS= nullptr;

MaterialTextGenerator* pMaterialTextGenerator = nullptr;



bool bDebug = false;

bool bGameInited = false;

bool bNetworkInited = false;



uintptr_t g_libGTASA = 0x00;

uintptr_t g_libSAMP = 0x00;



void ApplyGlobalPatches();

void ApplyPatches_level0();

void ApplyMultiTouchPatches();

void InstallGlobalHooks();

void InstallSpecialHooks();

void InitRenderWareFunctions();

void FLog(const char* fmt, ...);

void Log(const char* fmt, ...);

//void MyLog(const char* fmt, ...);



int work = 0;



void ReadSettingFile()

{

	/*char path[255] = { 0 };

	//sprintf(path, "%ssamp.set", g_pszStorage);

	sprintf(path, "%sNickName.ini", g_pszStorage);



	FILE* fp = fopen(path, "r");

	if (fp == NULL) return;



	char buf[1024];



	// nickname

	if (fgets(buf, 1024, fp) != NULL) {

		buf[strcspn(buf, "\n\r")] = 0;

		strcpy(g_nick, buf);

	}



	fclose(fp);*/



	pSettings = new CSettings();



	firebase::crashlytics::SetUserId(pSettings->Get().szNickName);

}



int hashing(const char* str) {

	int hashing = 5381;

	int c;

	while (c = *str++) {

		hashing = ((hashing << 5) + hashing) + c; /* hash * 33 + c */

		if (hashing < 0) hashing = 100;

	}

	if (hashing < 0) hashing = 100;

	return hashing;

}



void DoDebugLoop()

{

	// ...

}



void DoDebugStuff()

{

	// ...



	RwMatrix mat = pGame->FindPlayerPed()->m_pPed->GetMatrix().ToRwMatrix();

	

	for (int i = 0; i < 100; i++)

	{

		CPlayerPed* ped = pGame->NewPlayer(i, mat.pos.x + i, mat.pos.y, mat.pos.z, 0.0f, false, false);

		//ped->SetCollisionChecking(false);

		//ped->SetGravityProcessing(false);

	}

}

struct sigaction act_old;

struct sigaction act1_old;

struct sigaction act2_old;

struct sigaction act3_old;



extern int g_iLastProcessedSkinCollision, g_iLastProcessedEntityCollision, g_iLastRenderedObject;

extern uintptr_t g_dwLastRetAddrCrash;

void handler(int signum, siginfo_t *info, void* contextPtr)

{

	ucontext* context = (ucontext_t*)contextPtr;



	if (act_old.sa_sigaction)

	{

		act_old.sa_sigaction(signum, info, contextPtr);

	}



	if(info->si_signo == SIGSEGV)

	{

		FLog("SIGSEGV | Fault address: 0x%x", info->si_addr);



		PRINT_CRASH_STATES(context);



		CStackTrace::printBacktrace();

	}



	return;

}



void handler1(int signum, siginfo_t *info, void* contextPtr)

{

	ucontext* context = (ucontext_t*)contextPtr;



	if (act1_old.sa_sigaction)

	{

		act1_old.sa_sigaction(signum, info, contextPtr);

	}



	if(info->si_signo == SIGABRT)

	{

		FLog("SIGABRT | Fault address: 0x%x", info->si_addr);



		PRINT_CRASH_STATES(context);



		CStackTrace::printBacktrace();

	}



	return;

}



void handler2(int signum, siginfo_t *info, void* contextPtr)

{

	ucontext* context = (ucontext_t*)contextPtr;



	if (act2_old.sa_sigaction)

	{

		act2_old.sa_sigaction(signum, info, contextPtr);

	}



	if(info->si_signo == SIGFPE)

	{

		FLog("SIGFPE | Fault address: 0x%x", info->si_addr);



		PRINT_CRASH_STATES(context);



		CStackTrace::printBacktrace();

	}



	return;

}



void handler3(int signum, siginfo_t *info, void* contextPtr)

{

	ucontext* context = (ucontext_t*)contextPtr;



	if (act3_old.sa_sigaction)

	{

		act3_old.sa_sigaction(signum, info, contextPtr);

	}



	if(info->si_signo == SIGBUS)

	{

		FLog("SIGBUS | Fault address: 0x%x", info->si_addr);



		PRINT_CRASH_STATES(context);



		CStackTrace::printBacktrace();

	}



	return;

}



void DoInitStuff()

{

	if (bGameInited == false)

	{

		pPlayerTags = new CPlayerTags();

		pSnapShotHelper = new CSnapShotHelper();

		pMaterialTextGenerator = new MaterialTextGenerator();

		pAudioStream = new CAudioStream();

		pAudioStream->Initialize();

        //pGPSNavigator = new CGPSNavigator();



        pGPS = new GPS();



		pUI->splashscreen()->setVisible(false);

		pUI->chat()->setVisible(true);

		pUI->buttonpanel()->setVisible(false);



		pGame->Initialize();

		pGame->SetMaxStats();

		pGame->ToggleThePassingOfTime(false);



		// voice

		LogVoice("[dbg:samp:load] : module loading...");



		for (const auto& loadCallback : Samp::loadCallbacks) {

			if (loadCallback != nullptr) {

				loadCallback();

			}

		}



		Samp::loadStatus = true;



		LogVoice("[dbg:samp:load] : module loaded");



		/*if (bDebug)

		{

            CCamera& TheCamera = *reinterpret_cast<CCamera*>(g_libGTASA + (VER_x32 ? 0x00951FA8 : 0xBBA8D0));

            //TheCamera.Restore();

            CCamera::SetBehindPlayer();

			pGame->DisplayHUD(true);

			pGame->EnableClock(false);



			DoDebugStuff();

		}*/



		bGameInited = true;

	}



    if (!bNetworkInited && !bDebug)

    {

        ReadSettingFile();



        Glory glory = GetGlory();

 

        pNetGame = new CNetGame(

                glory.szHost.c_str(),

                glory.iPort,

                pSettings->Get().szNickName,

                pSettings->Get().szPassword

        );



        bNetworkInited = true;



        FLog("Network game initialized.",

             glory.szHost.c_str(),

             glory.iPort);

    }

}

CVector2D radarPos = {0,0};

CVector2D radarSize = {0,0};

CVector2D radarBgPos1 = {0,0};

CVector2D radarBgPos2 = {0,0};



extern "C" {

	JNIEXPORT void JNICALL Java_com_glory_game_SAMP_initializeSAMP(JNIEnv *pEnv, jobject thiz)

	{

		pJavaWrapper = new CJavaWrapper(pEnv, thiz);



	}

	JNIEXPORT void JNICALL Java_com_glory_game_SAMP_onInputEnd(JNIEnv *pEnv, jobject thiz, jbyteArray str)

	{

		if(pUI)

		{

			pUI->keyboard()->sendForGB(pEnv, thiz, str);

		}

	}

	JNIEXPORT void JNICALL Java_com_glory_game_SAMP_onEventBackPressed(JNIEnv *pEnv, jobject thiz)

	{

		if(pSettings)

		{

			if(pSettings->Get().iAndroidKeyboard)

				pJavaWrapper->HideKeyboard();

		}

	}

	JNIEXPORT void JNICALL Java_com_glory_game_ui_dialog_DialogManager_sendDialogResponse(JNIEnv* pEnv, jobject thiz, jint i3, jint i, jint i2, jbyteArray str)

	{

		jboolean isCopy = true;



		jbyte* pMsg = pEnv->GetByteArrayElements(str, &isCopy);

		jsize length = pEnv->GetArrayLength(str);



		std::string szStr((char*)pMsg, length);

		

		if(pNetGame) {

			pNetGame->SendDialogResponse(i, i3, i2, (char*)szStr.c_str());

			//pGame->FindPlayerPed()->TogglePlayerControllableWithoutLock(true);

		}



		pEnv->ReleaseByteArrayElements(str, pMsg, JNI_ABORT);

	}



JNIEXPORT void JNICALL Java_com_nvidia_devtech_NvEventQueueActivity_onTabClose(JNIEnv *pEnv, jobject thiz) {

    PlayerTabList* tab = pUI->playertablist();

    tab->hide();

    pJavaWrapper->ShowHud();

}

extern "C"

JNIEXPORT void JNICALL

Java_com_glory_game_ui_Hud_clickCameraMode(JNIEnv *env, jobject thiz) {

//

    if(!pNetGame)return;

    if(!pNetGame->GetPlayerPool())return;



    CLocalPlayer *pPlayer = pNetGame->GetPlayerPool()->GetLocalPlayer();

    if(!pPlayer)return;



    CPlayerPed *pPed = pPlayer->GetPlayerPed();

    if(!pPed) return;



    if(pPed->IsInVehicle()) {

        bIsTouchCameraButt = true;

    }

    else {

        CFirstPersonCamera::Toggle();

    }

}

extern "C"

JNIEXPORT void JNICALL

Java_com_glory_game_ui_Hud_sendAltKey(JNIEnv *env, jobject thiz) {

    LocalPlayerKeys.bKeys[ePadKeys::KEY_WALK] = true;

}



extern "C"

#ifndef CTR_DEBUG

#define CTR_DEBUG 0

#endif



extern "C"

JNIEXPORT void JNICALL

Java_com_glory_game_ui_Hud_sendCtrlKey(JNIEnv *env, jobject thiz) {

#if CTR_DEBUG

    __android_log_print(ANDROID_LOG_INFO, "CTR_DBG", "[CTR_DBG] sendCtrlKey native JNI called");

#endif

    LocalPlayerKeys.bKeys[ePadKeys::KEY_CTRL_BACK] = true;

}



extern "C"

JNIEXPORT void JNICALL

Java_com_glory_game_ui_Hud_sendTabKey(JNIEnv *env, jobject thiz) {

    if(pUI) {

        PlayerTabList* tab = pUI->playertablist();

        if(tab) {

            if(tab->visible()) {

                tab->hide();

            } else {

                tab->show();

            }

        }

    }

}



extern "C"

JNIEXPORT void JNICALL

Java_com_glory_game_ui_Hud_sendGKey(JNIEnv *env, jobject thiz) {

    LocalPlayerKeys.bKeys[ePadKeys::KEY_SUBMISSION] = true;

}



extern "C"

JNIEXPORT void JNICALL

Java_com_glory_game_ui_Hud_sendYKey(JNIEnv *env, jobject thiz) {

    LocalPlayerKeys.bKeys[ePadKeys::KEY_YES] = true;

}



extern "C"

JNIEXPORT void JNICALL

Java_com_glory_game_ui_Hud_sendNKey(JNIEnv *env, jobject thiz) {

    LocalPlayerKeys.bKeys[ePadKeys::KEY_NO] = true;

}



extern "C"

JNIEXPORT void JNICALL

Java_com_glory_game_ui_Hud_sendHKey(JNIEnv *env, jobject thiz) {

    LocalPlayerKeys.bKeys[ePadKeys::KEY_CTRL_BACK] = true;

}



extern "C"

JNIEXPORT void JNICALL

Java_com_nvidia_devtech_NvEventQueueActivity_sendALT(JNIEnv *env, jobject thiz) {

    LocalPlayerKeys.bKeys[ePadKeys::KEY_WALK] = true;

}



extern "C"

JNIEXPORT void JNICALL

Java_com_nvidia_devtech_NvEventQueueActivity_sendCTRL(JNIEnv *env, jobject thiz) {

    LocalPlayerKeys.bKeys[ePadKeys::KEY_CTRL_BACK] = true;

}



extern "C"

JNIEXPORT void JNICALL

Java_com_nvidia_devtech_NvEventQueueActivity_sendY(JNIEnv *env, jobject thiz) {

    LocalPlayerKeys.bKeys[ePadKeys::KEY_YES] = true;

}



extern "C"

JNIEXPORT void JNICALL

Java_com_nvidia_devtech_NvEventQueueActivity_sendN(JNIEnv *env, jobject thiz) {

    LocalPlayerKeys.bKeys[ePadKeys::KEY_NO] = true;

}



extern "C"

JNIEXPORT void JNICALL

Java_com_nvidia_devtech_NvEventQueueActivity_sendH(JNIEnv *env, jobject thiz) {

    LocalPlayerKeys.bKeys[ePadKeys::KEY_CTRL_BACK] = true;

}



extern "C"

JNIEXPORT void JNICALL

Java_com_nvidia_devtech_NvEventQueueActivity_sendG(JNIEnv *env, jobject thiz) {

    LocalPlayerKeys.bKeys[ePadKeys::KEY_SUBMISSION] = true;

}



extern "C"

JNIEXPORT void JNICALL

Java_com_glory_game_ui_Hud_nativeSetRadarPos(JNIEnv *env, jobject thiz, jfloat x, jfloat y, jfloat width, jfloat height) {

    radarPos.x = x;

    radarPos.y = y;

    radarSize.x = width;

    radarSize.y = height;



    //FLog("HUD Pos received: X=%f Y=%f W=%f H=%f", x, y, width, height);

}

extern "C"

JNIEXPORT void JNICALL

Java_com_glory_game_ui_Hud_SetRadarBgPos(JNIEnv *env, jobject thiz, jfloat x1, jfloat y1,

                                               jfloat x2, jfloat y2) {

    radarBgPos1.x = x1;

    radarBgPos1.y = y1;

    radarBgPos2.x = x2;

    radarBgPos2.y = y2;

}



extern "C"

JNIEXPORT void JNICALL

Java_com_glory_game_ui_Hud_setNativeHudRadar(JNIEnv *env, jobject thiz, jfloat centerX, jfloat centerY, jfloat radiusX, jfloat radiusY) {

    if (!pSettings) return;



    pSettings->Get().fHudRadarX = centerX;

    pSettings->Get().fHudRadarY = centerY;

    pSettings->Get().fHudRadarScale = radiusX;

    pSettings->Get().fHudRadarScaleY = radiusY;



    if (CTouchInterface::m_pWidgets) {

        auto radar = CTouchInterface::m_pWidgets[WIDGET_RADAR];

        if (radar) {

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

    }

}



#include <atomic>



static std::atomic<bool> g_bClassicHud(false);



extern "C" bool IsClassicHudMode() {

    return g_bClassicHud.load();

}



extern "C" void SetClassicHudMode(bool classic) {

    g_bClassicHud.store(classic);

}



extern "C"

JNIEXPORT void JNICALL

Java_com_glory_game_ui_Hud_setNativeHudMode(JNIEnv *env, jobject thiz, jboolean isClassic) {

    g_bClassicHud.store(isClassic);

    if (pGame) {

        pGame->DisplayHUD(isClassic);

    }

    if (pSettings) {

        if (isClassic) {

            pSettings->Get().fHudRadarX = 50.0f;

            pSettings->Get().fHudRadarY = 70.0f;

            pSettings->Get().fHudRadarScale = 45.0f;

            pSettings->Get().fHudRadarScaleY = 45.0f;

        } else {

            pSettings->Get().fHudRadarX = -1.0f;

            pSettings->Get().fHudRadarY = -1.0f;

            pSettings->Get().fHudRadarScale = -1.0f;

            pSettings->Get().fHudRadarScaleY = -1.0f;

        }

    }

}



static std::atomic<bool> g_bWannaOpenMap(false);



extern "C" void TriggerOpenMap() {

    g_bWannaOpenMap.store(true);

}



extern "C"

JNIEXPORT void JNICALL

Java_com_glory_game_ui_Hud_nativeOpenMap(JNIEnv *env, jobject thiz) {

    TriggerOpenMap();

}



extern "C"

JNIEXPORT void JNICALL

Java_com_glory_game_ui_Hud_setNativeChatConfig(JNIEnv *env, jobject thiz,

                                              jfloat xPercent, jfloat yPercent,

                                              jfloat widthPercent, jint lines, jfloat fontSize) {

    UISettings::UpdateChatConfig(xPercent, yPercent, widthPercent, lines, fontSize);

}



extern "C" void CheckWannaOpenMap() {

    if (g_bWannaOpenMap.exchange(false)) {

        extern void OpenMobileMap();

        OpenMobileMap();

    }

}



extern void SwitchNextWeapon();



JNIEXPORT void JNICALL

Java_com_glory_game_ui_Hud_cycleWeapon(JNIEnv *env, jobject thiz) {

    FLog("Java_com_glory_game_ui_Hud_cycleWeapon called");

    SwitchNextWeapon();

}



JNIEXPORT void JNICALL

Java_com_glory_game_ui_Hud_fireWeapon(JNIEnv *env, jobject thiz) {

    LocalPlayerKeys.bKeys[ePadKeys::KEY_FIRE] = true;

}



JNIEXPORT void JNICALL

Java_com_glory_game_ui_Hud_setNativeWidgetShoot(JNIEnv *env, jobject thiz, jfloat x, jfloat y, jfloat size, jfloat sensitivity, jboolean enabled) {

    if (pSettings) {

        pSettings->Get().fButtonAttackX = x;

        pSettings->Get().fButtonAttackY = y;

        pSettings->Get().fButtonAttackSize = size;

        pSettings->Get().fButtonAttackSensitivity = sensitivity;

        pSettings->Get().iButtonAttackNew = enabled ? 1 : 0;

    }



}

}



void MainLoop()

{

    if (pGame->bIsGameExiting) return;



    DoInitStuff();



    if (pNetGame) {

        pNetGame->Process();

    }



    CVector localPlayerPos{0.0f, 0.0f, 0.0f};

    if (pNetGame && pNetGame->GetPlayerPool() && pNetGame->GetPlayerPool()->GetLocalPlayer())

    {

        CPlayerPed* pPlayerPed = pNetGame->GetPlayerPool()->GetLocalPlayer()->GetPlayerPed();

        if (pPlayerPed && pPlayerPed->m_pPed)

        {

            localPlayerPos = pPlayerPed->m_pPed->GetPosition();

        }

    }

    if (pAudioStream)

    {

        float lx = localPlayerPos.x;

        float ly = localPlayerPos.y;

        float lz = localPlayerPos.z;

        pAudioStream->Process(lx, ly, lz);

    }

}



void InitGui()

{

	// new voice

	Plugin::OnPluginLoad();

	Plugin::OnSampLoad();



	std::string font_path = string_format("%sfonts/%s", g_pszStorage, FONT_NAME);

	pUI = new UI(ImVec2(RsGlobal->maximumWidth, RsGlobal->maximumHeight), font_path.c_str());

	pUI->initialize();

	pUI->performLayout();

	// pWidgetShoot removed

}



#include "game/multitouch.h"

#include "armhook/patch.h"

#include "util/CUtil.h"



jint JNI_OnLoad(JavaVM* vm, void* reserved)

{

	javaVM = vm;

	LOGI("SA-MP library loaded! Build time: " __DATE__ " " __TIME__);

	Diag_Init();

	Diag_Breadcrumb("Native library loaded (JNI_OnLoad)");



	g_libGTASA = CUtil::FindLib("libGTASA.so");

	if (g_libGTASA == 0x00) {

		LOGE("libGTASA.so address was not found! ");

		return JNI_VERSION_1_6;

	}



	g_libSAMP = CUtil::FindLib("libmultiplayer.so");

	if (g_libSAMP == 0x00) {

		LOGE("libmultiplayer.so address was not found! ");

		return JNI_VERSION_1_6;

	}



	firebase::crashlytics::Initialize();



	uintptr_t libgtasa = CUtil::FindLib("libGTASA.so");

	uintptr_t libmultiplayer = CUtil::FindLib("libmultiplayer.so");

	uintptr_t libc = CUtil::FindLib("libc.so");



	FLog("libGTASA.so: 0x%x", libgtasa);

	FLog("libmultiplayer.so: 0x%x", libmultiplayer);

	FLog("libc.so: 0x%x", libc);



	char str[100];



	sprintf(str, "0x%x", libgtasa);

	firebase::crashlytics::SetCustomKey("libGTASA.so", str);

	

	sprintf(str, "0x%x", libmultiplayer);

	firebase::crashlytics::SetCustomKey("libmultiplayer.so", str);



	sprintf(str, "0x%x", libc);

	firebase::crashlytics::SetCustomKey("libc.so", str);



	CHook::InitHookStuff();

	InstallSpecialHooks();

	ApplyPatches_level0();

    InitRenderWareFunctions();

    MultiTouch::initialize();



	pGame = new CGame();



	//pVoice = new CVoice();

	//pVoice->Initialize(VOICE_FREQUENCY, CODEC_FREQUENCY, VOICE_SENDRRATE);



	//pthread_t thread;

	//pthread_create(&thread, 0, Init, 0);



	// Signal handlers installed by Diag_Init with alternate stack and crash logging

		

	return JNI_VERSION_1_6;

}



uint32_t GetTickCount()

{

    return CTimer::m_snTimeInMillisecondsNonClipped;

}	



void FLog(const char* fmt, ...)

{

#ifdef NDEBUG

    // In release builds, avoid synchronous disk file I/O on hot paths

    return;

#else

	char buffer[0xFF];

	static FILE* flLog = nullptr;

	const char* pszStorage = g_pszStorage;





	if (flLog == nullptr && pszStorage != nullptr)

	{

		sprintf(buffer, "%s/logcat.txt", pszStorage);

		//LOGI("buffer: %s", buffer);

		flLog = fopen(buffer, "a");

	}



	memset(buffer, 0, sizeof(buffer));



	va_list arg;

	va_start(arg, fmt);

	vsnprintf(buffer, sizeof(buffer), fmt, arg);

	va_end(arg);



	LOGI("%s", buffer);

	firebase::crashlytics::Log(buffer);



	if (flLog == nullptr) return;

	fprintf(flLog, "%s\n", buffer);

	fflush(flLog);



	return;

#endif

}



void Log(const char* fmt, ...)

{

    char buffer[0xFF];

    static FILE* flLog = nullptr;

    const char* pszStorage = g_pszStorage;





    if (flLog == nullptr && pszStorage != nullptr)

    {

        sprintf(buffer, "%s/logcat.txt", pszStorage);

        //LOGI("buffer: %s", buffer);

        flLog = fopen(buffer, "a");

    }



    memset(buffer, 0, sizeof(buffer));



    va_list arg;

    va_start(arg, fmt);

    vsnprintf(buffer, sizeof(buffer), fmt, arg);

    va_end(arg);



    LOGI("%s", buffer);

    firebase::crashlytics::Log(buffer);



    if (flLog == nullptr) return;

    fprintf(flLog, "%s\n", buffer);

    fflush(flLog);



    return;

}



void ChatLog(const char* fmt, ...)

{

	/*char buffer[0xFF];

	static FILE* flLog = nullptr;

	const char* pszStorage = g_pszStorage;





	if (flLog == nullptr && pszStorage != nullptr)

	{

		sprintf(buffer, "%s/chat_log.txt", pszStorage);

		flLog = fopen(buffer, "a");

	}



	memset(buffer, 0, sizeof(buffer));



	va_list arg;

	va_start(arg, fmt);

	vsnprintf(buffer, sizeof(buffer), fmt, arg);

	va_end(arg);



	if (flLog == nullptr) return;

	fprintf(flLog, "%s\n", buffer);

	fflush(flLog);*/



	return;

}



void MyLog(const char* fmt, ...)

{

	char buffer[0xFF];

	static FILE* flLog = nullptr;

	const char* pszStorage = g_pszStorage;





	if (flLog == nullptr && pszStorage != nullptr)

	{

		sprintf(buffer, "%s/logcat.txt", pszStorage);

		flLog = fopen(buffer, "a");

	}



	memset(buffer, 0, sizeof(buffer));



	va_list arg;

	va_start(arg, fmt);

	vsnprintf(buffer, sizeof(buffer), fmt, arg);

	va_end(arg);



	if (flLog == nullptr) return;

	fprintf(flLog, "%s\n", buffer);

	fflush(flLog);



	return;

}



void MyLog2(const char* fmt, ...)

{

/*	char buffer[0xFF];

	static FILE* flLog = nullptr;

	const char* pszStorage = g_pszStorage;





	if (flLog == nullptr && pszStorage != nullptr)

	{

		sprintf(buffer, "%s/samp.txt", pszStorage);

		flLog = fopen(buffer, "a");

	}



	memset(buffer, 0, sizeof(buffer));



	va_list arg;

	va_start(arg, fmt);

	vsnprintf(buffer, sizeof(buffer), fmt, arg);

	va_end(arg);



	if (pUI) pUI->chat()->addDebugMessage(buffer);



	if (flLog == nullptr) return;

	fprintf(flLog, "%s\n", buffer);

	fflush(flLog);*/

	return;

}



void LogVoice(const char* fmt, ...)

{

	char buffer[0xFF];

	static FILE* flLog = nullptr;

	const char* pszStorage = g_pszStorage;



	if (flLog == nullptr && pszStorage != nullptr)

	{

		sprintf(buffer, "%s/logcat.txt", pszStorage);

		//LOGI("buffer: %s", buffer);

		flLog = fopen(buffer, "a");

	}



	memset(buffer, 0, sizeof(buffer));



	va_list arg;

	va_start(arg, fmt);

	vsnprintf(buffer, sizeof(buffer), fmt, arg);

	va_end(arg);



	__android_log_write(ANDROID_LOG_INFO, "AXL", buffer);



	if (flLog == nullptr) return;

	fprintf(flLog, "%s\n", buffer);

	fflush(flLog);



	return;

}




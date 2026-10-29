#include "jniutil.h"
#include "../diag.h"
#include "game/game.h"
#include "net/netgame.h"

extern CGame *pGame;
extern CNetGame* pNetGame;

CJavaWrapper::CJavaWrapper(JNIEnv *env, jobject activity)
{
    this->activity = env->NewGlobalRef(activity);

    jclass clas = env->GetObjectClass(activity);
    if(!clas)
    {
        FLog("no clas");
        return;
    }

    s_showTab = env->GetMethodID(clas, "showTab", "()V");
    s_hideTab = env->GetMethodID(clas, "hideTab", "()V");
    s_clearTab = env->GetMethodID(clas, "clearTab", "()V");
    s_setTab = env->GetMethodID(clas, "setTab", "(ILjava/lang/String;II)V");

    s_showLoadingScreen = env->GetMethodID(clas, "showLoadingScreen", "()V");
    s_hideLoadingScreen = env->GetMethodID(clas, "hideLoadingScreen", "()V");

    s_setPauseState = env->GetMethodID(clas, "setPauseState", "(Z)V");

    s_ShowDialog = env->GetMethodID(clas, "showDialog", "(II[B[B[B[B)V");

    s_showInputLayout = env->GetMethodID(clas, "showKeyboard", "()V");
    s_hideInputLayout = env->GetMethodID(clas, "hideKeyboard", "()V");

    s_exitGame = env->GetMethodID(clas, "exitGame", "()V");

    s_showEditObject = env->GetMethodID(clas, "showEditObject", "()V");
    s_hideEditObject = env->GetMethodID(clas, "hideEditObject", "()V");
    s_showHudEditor = env->GetMethodID(clas, "showHudEditor", "()V");

    s_showTabWindow = env->GetMethodID(clas, "showTabWindow", "()V");
    s_setTabStat = env->GetMethodID(clas, "setTabStat", "(ILjava/lang/String;II)V");

    s_updateHudInfo = env->GetMethodID(clas, "updateHudInfo", "(IIIIIIIIIII)V");
    s_showHud = env->GetMethodID(clas, "showHud", "()V");
    s_hideHud = env->GetMethodID(clas, "hideHud", "()V");
    s_showHudLogo = env->GetMethodID(clas, "showHudLogo", "()V");
    s_hideHudLogo = env->GetMethodID(clas, "hideHudLogo", "()V");

    s_updateSpeedInfo = env->GetMethodID(clas, "updateSpeedInfo", "(IIIIIIII)V");
    s_showSpeed = env->GetMethodID(clas, "showSpeed", "()V");
    s_hideSpeed = env->GetMethodID(clas, "hideSpeed", "()V");

    env->DeleteLocalRef(clas);
}

void CJavaWrapper::ShowKeyboard()
{
    JNIEnv* p;
    javaVM->GetEnv((void**)&p, JNI_VERSION_1_6);
    p->CallVoidMethod(activity, s_showInputLayout);
    EXCEPTION_CHECK(p);
}

void CJavaWrapper::ShowHudEditor()
{
    JNIEnv* p;
    javaVM->GetEnv((void**)&p, JNI_VERSION_1_6);
    p->CallVoidMethod(activity, s_showHudEditor);
    EXCEPTION_CHECK(p);
}

void CJavaWrapper::UpdateHudInfo(int health, int armour, int weaponid, int ammo, int ammoinclip,
                                 int money, int wanted, int eat, int drink, int bankMoney, int playerid)
{
    if (Diag_IsNoWeaponHud()) return;

    JNIEnv* env = nullptr;
    int status = javaVM->GetEnv((void**)&env, JNI_VERSION_1_6);
    bool attached = false;
    if (status == JNI_EDETACHED) {
        if (javaVM->AttachCurrentThread(&env, nullptr) != 0) return;
        attached = true;
    } else if (status != JNI_OK || !env) {
        return;
    }

    if (this->activity && this->s_updateHudInfo) {
        env->CallVoidMethod(this->activity, this->s_updateHudInfo,
                            health, armour, weaponid, ammo, ammoinclip,
                            money, wanted, eat, drink, bankMoney, playerid);
    }

    if (attached) {
        javaVM->DetachCurrentThread();
    }
}

void CJavaWrapper::ShowHud()
{
    JNIEnv* env;
    javaVM->GetEnv((void**)&env, JNI_VERSION_1_6);

    if (!env)
    {
        FLog("No env");
        return;
    }
    //g_pJavaWrapper->ShowNotification(4, "HUD показан.", 5, "", "Хорошо");
    env->CallVoidMethod(this->activity, this->s_showHud);
}

void CJavaWrapper::HideHud()
{
    JNIEnv* env;
    javaVM->GetEnv((void**)&env, JNI_VERSION_1_6);

    if (!env)
    {
        FLog("No env");
        return;
    }
    //g_pJavaWrapper->ShowNotification(4, "HUD скрыт.", 5, "", "Хорошо");
    env->CallVoidMethod(this->activity, this->s_hideHud);
}

void CJavaWrapper::ShowHudLogo()
{
    JNIEnv* env;
    javaVM->GetEnv((void**)&env, JNI_VERSION_1_6);

    if (!env)
    {
        Log("No env");
        return;
    }
    env->CallVoidMethod(this->activity, this->s_showHudLogo);
}

void CJavaWrapper::HideHudLogo()
{
    JNIEnv* env;
    javaVM->GetEnv((void**)&env, JNI_VERSION_1_6);

    if (!env)
    {
        Log("No env");
        return;
    }
    env->CallVoidMethod(this->activity, this->s_hideHudLogo);
}


void CJavaWrapper::ShowSpeed()
{
    JNIEnv* env;
    javaVM->GetEnv((void**)&env, JNI_VERSION_1_6);

    if (!env)
    {
        Log("No env");
        return;
    }
    env->CallVoidMethod(this->activity, this->s_showSpeed);
}

void CJavaWrapper::HideSpeed()
{
    JNIEnv* env;
    javaVM->GetEnv((void**)&env, JNI_VERSION_1_6);

    if (!env)
    {
        Log("No env");
        return;
    }
    env->CallVoidMethod(this->activity, this->s_hideSpeed);
}

void CJavaWrapper::UpdateSpeedInfo(int speed, int fuel, int hp, int mileage, int engine, int light, int belt, int lock)
{
    JNIEnv* env;
    javaVM->GetEnv((void**)&env, JNI_VERSION_1_6);;

    if (!env)
    {
        Log("No env");
        return;
    }

    env->CallVoidMethod(this->activity, this->s_updateSpeedInfo, speed, fuel, hp, mileage, engine, light, belt, lock);
}

void CJavaWrapper::SetTabStat(int id, char* name, int score, int ping)
{
    JNIEnv* env = nullptr;
    if (javaVM->GetEnv(reinterpret_cast<void**>(&env), JNI_VERSION_1_6) != JNI_OK || !env)
    {
        LOGI("SetTab: No env");
        return;
    }

    jclass strClass = env->FindClass("java/lang/String");
    jmethodID ctorID = env->GetMethodID(strClass, "<init>", "([BLjava/lang/String;)V");
    jstring encoding = env->NewStringUTF("UTF-8");

    jbyteArray bytes = env->NewByteArray(strlen(name));
    env->SetByteArrayRegion(bytes, 0, strlen(name), reinterpret_cast<const jbyte*>(name));
    jstring jname = (jstring)env->NewObject(strClass, ctorID, bytes, encoding);

    env->CallVoidMethod(activity, s_setTabStat, id, jname, score, ping);

    EXCEPTION_CHECK(env);

    env->DeleteLocalRef(bytes);
    env->DeleteLocalRef(jname);
    env->DeleteLocalRef(encoding);
    env->DeleteLocalRef(strClass);
}

void CJavaWrapper::ShowTabWindow()
{
    JNIEnv* env = nullptr;
    if (javaVM->GetEnv(reinterpret_cast<void**>(&env), JNI_VERSION_1_6) != JNI_OK || !env)
    {
        LOGI("ShowTab: No env");
        return;
    }

    env->CallVoidMethod(activity, s_showTabWindow);

    EXCEPTION_CHECK(env);
}

void CJavaWrapper::HideKeyboard()
{
    JNIEnv* p;
    javaVM->GetEnv((void**)&p, JNI_VERSION_1_6);
    p->CallVoidMethod(activity, s_hideInputLayout);
    EXCEPTION_CHECK(p);
}

void CJavaWrapper::ShowLoadingScreen()
{
    JNIEnv* p;
    javaVM->GetEnv((void**)&p, JNI_VERSION_1_6);
    p->CallVoidMethod(activity, s_showLoadingScreen);
    EXCEPTION_CHECK(p);
}

void CJavaWrapper::HideLoadingScreen()
{
    JNIEnv* p;
    javaVM->GetEnv((void**)&p, JNI_VERSION_1_6);
    p->CallVoidMethod(activity, s_hideLoadingScreen);
    EXCEPTION_CHECK(p);
}

void CJavaWrapper::SetPauseState(bool pause)
{
    JNIEnv* p;
    javaVM->GetEnv((void**)&p, JNI_VERSION_1_6);
    p->CallVoidMethod(activity, s_setPauseState, pause);
    EXCEPTION_CHECK(p);
}

void CJavaWrapper::SetTab(int id, char* names, int score, int pings)
{
    JNIEnv* global_env;
    javaVM->GetEnv((void**)&global_env, JNI_VERSION_1_6);

    if (!global_env)
    {
        LOGI("No env");
        return;
    }

    jclass strClass = global_env->FindClass("java/lang/String");
    jmethodID ctorID = global_env->GetMethodID(strClass, "<init>", "([BLjava/lang/String;)V");
    jstring encoding = global_env->NewStringUTF("UTF-8");

    jbyteArray bytes = global_env->NewByteArray(strlen(names));
    global_env->SetByteArrayRegion(bytes, 0, strlen(names), (jbyte*)names);
    jstring str1 = (jstring)global_env->NewObject(strClass, ctorID, bytes, encoding);

    global_env->CallVoidMethod(activity, s_setTab, id, str1, score, pings);

    EXCEPTION_CHECK(global_env);
}

void CJavaWrapper::ShowTab()
{
    JNIEnv* p;
    javaVM->GetEnv((void**)&p, JNI_VERSION_1_6);
    p->CallVoidMethod(activity, s_showTab);
    EXCEPTION_CHECK(p);
}

void CJavaWrapper::HideTab()
{
    JNIEnv* p;
    javaVM->GetEnv((void**)&p, JNI_VERSION_1_6);
    p->CallVoidMethod(activity, s_hideTab);
    EXCEPTION_CHECK(p);
}

void CJavaWrapper::ClearTab()
{
    JNIEnv* p;
    javaVM->GetEnv((void**)&p, JNI_VERSION_1_6);
    p->CallVoidMethod(activity, s_clearTab);
    EXCEPTION_CHECK(p);
}

void CJavaWrapper::ShowDialog(int dialogStyle, int dialogID, char* title, char* text, char* button1, char* button2)
{
    JNIEnv* env;
    javaVM->GetEnv((void**)&env, JNI_VERSION_1_6);

    if (!env)
    {
        FLog("No env");
        return;
    }

    std::string sTitle(title);
    std::string sText(text);
    std::string sButton1(button1);
    std::string sButton2(button2);

    jbyteArray jstrTitle = as_byte_array((unsigned char*)sTitle.c_str(), sTitle.length());
    jbyteArray jstrText = as_byte_array((unsigned char*)sText.c_str(), sText.length());
    jbyteArray jstrButton1 = as_byte_array((unsigned char*)sButton1.c_str(), sButton1.length());
    jbyteArray jstrButton2 = as_byte_array((unsigned char*)sButton2.c_str(), sButton2.length());

    env->CallVoidMethod(activity, s_ShowDialog, dialogID, dialogStyle, jstrTitle, jstrText, jstrButton1, jstrButton2);

    EXCEPTION_CHECK(env);
}

void CJavaWrapper::exitGame() {

    JNIEnv* env;
    javaVM->GetEnv((void**)&env, JNI_VERSION_1_6);

    if (!env)
    {
        FLog("No env");
        return;
    }

    env->CallVoidMethod(this->activity, this->s_exitGame);
}

void CJavaWrapper::ShowEditObject() {

    JNIEnv* env;
    javaVM->GetEnv((void**)&env, JNI_VERSION_1_6);

    if (!env)
    {
        FLog("No env");
        return;
    }

    env->CallVoidMethod(this->activity, this->s_showEditObject);
}

void CJavaWrapper::HideEditObject() {

    JNIEnv* env;
    javaVM->GetEnv((void**)&env, JNI_VERSION_1_6);

    if (!env)
    {
        FLog("No env");
        return;
    }

    env->CallVoidMethod(this->activity, this->s_hideEditObject);
}

extern "C" JNIEXPORT void JNICALL Java_com_nvidia_devtech_NvEventQueueActivity_sendCommand(JNIEnv* pEnv, jobject thiz, jbyteArray str)
{
jboolean isCopy = true;

jbyte* pMsg = pEnv->GetByteArrayElements(str, &isCopy);
jsize length = pEnv->GetArrayLength(str);

std::string szStr((char*)pMsg, length);

if(pNetGame) {
pNetGame->SendChatCommand((char*)szStr.c_str());
}

	pEnv->ReleaseByteArrayElements(str, pMsg, JNI_ABORT);
}
// Force recompile to fix stale objects

#include "../main.h"



#include "../game/game.h"



#include "../net/netgame.h"



#include "gui.h"



#include "../playertags.h"



#include "../net/playerbubblepool.h"



#include "vendor/str_obfuscator/str_obfuscator.hpp"



#include "../game/mobile/MobileMenu/MobileMenu.h"



// voice



#include "../voice_new/Plugin.h"



#include "../voice_new/MicroIcon.h"



#include "../voice_new/SpeakerList.h"



#include "../voice_new/Network.h"







#include "../gui/samp_widgets/voicebutton.h"



#include "game/Textures/TextureDatabaseRuntime.h"



#include "game/Streaming.h"



#include "game/Pools.h"



#include "game/CGPS.hpp"







#include "../java/jniutil.h"



#include "../game/pad.h"



#include "../net/localplayer.h"







extern GPS* pGPS;



extern CNetGame* pNetGame;



extern CPlayerTags* pPlayerTags;



extern CJavaWrapper* pJavaWrapper;



extern UI* pUI;



extern CMobileMenu* CMobileMenu;



extern CSettings* pSettings;



extern CGame* pGame;



extern "C" bool IsClassicHudMode();







static bool bAFKInitialized = false;







UI::UI(const ImVec2& display_size, const std::string& font_path)



        : Widget(), ImGuiWrapper(display_size, font_path), m_pJavaWrapper(nullptr)



{



    UISettings::Initialize(display_size);



    this->setFixedSize(display_size);







    // Загружаем сохраненное состояние FPS info



    if(pSettings) {



        m_bShowFPSInfo = pSettings->GetFPSInfo();



    }



}







bool UI::initialize()



{



    if (!ImGuiWrapper::initialize()) return false;







    m_splashScreen = new SplashScreen();



    this->addChild(m_splashScreen);



    m_splashScreen->setFixedSize(size());



    m_splashScreen->setPosition(ImVec2(0.0f, 0.0f));



    m_splashScreen->setVisible(true);







    m_chat = new Chat();



    this->addChild(m_chat);



    m_chat->setFixedSize(UISettings::chatSize());



    m_chat->setPosition(UISettings::chatPos());



    m_chat->setItemSize(UISettings::chatItemSize());



    m_chat->setVisible(false);







    m_buttonPanel = new ButtonPanel();



    this->addChild(m_buttonPanel);



    m_buttonPanel->setFixedSize(UISettings::buttonPanelSize());



    m_buttonPanel->setPosition(UISettings::buttonPanelPos());



    m_buttonPanel->setVisible(false);







    m_voiceButton = new VoiceButton();



    this->addChild(m_voiceButton);



    m_voiceButton->setFixedSize(UISettings::buttonVoiceSize());



    m_voiceButton->setPosition(UISettings::buttonVoicePos());



    m_voiceButton->setVisible(false);







    m_spawn = new Spawn();



    this->addChild(m_spawn);



    m_spawn->setFixedSize(UISettings::spawnSize());



    m_spawn->setPosition(UISettings::spawnPos());



    m_spawn->setVisible(false);







    m_dialog = new Dialog();



    this->addChild(m_dialog);



    m_dialog->setVisible(false);



    m_dialog->setMinSize(UISettings::dialogMinSize());



    m_dialog->setMaxSize(UISettings::dialogMaxSize());







    m_keyboard = new Keyboard();



    this->addChild(m_keyboard);



    m_keyboard->setFixedSize(UISettings::keyboardSize());



    m_keyboard->setPosition(UISettings::keyboardPos());



    m_keyboard->setVisible(false);







    m_playerTabList = new PlayerTabList();



    //this->addChild(m_playerTabList);







    label = new Label(" ", ImColor(1.0f, 1.0f, 1.0f), true, UISettings::fontSize() / 2);



    pUI->addChild(label);







    label2 = new Label(" ", ImColor(1.0f, 1.0f, 1.0f), true, UISettings::fontSize() / 2);



    pUI->addChild(label2);







    label3 = new Label(" ", ImColor(1.0f, 1.0f, 1.0f), true, UISettings::fontSize() / 2);



    pUI->addChild(label3);







    label4 = new Label(" ", ImColor(1.0f, 1.0f, 1.0f), true, UISettings::fontSize() / 2);



    pUI->addChild(label4);







/*



    Label* d_label1;



    if(VER_x32)



    {



        d_label1 = new Label(cryptor::create("2.10 SA-MP (armeabi-v7a)").decrypt(), ImColor(1.0f, 1.0f, 1.0f), true, UISettings::fontSize() / 2.5);



        this->addChild(d_label1);



        d_label1->setPosition(ImVec2(3.0, 3.0));



    }



    else



    {



        d_label1 = new Label(cryptor::create("2.10 SA-MP (arm64-v8a)").decrypt(), ImColor(1.0f, 1.0f, 1.0f), true, UISettings::fontSize() / 2.5);



        this->addChild(d_label1);



        d_label1->setPosition(ImVec2(3.0, 3.0));



    }



*/







    return true;



}







#include "../game/CGPS.hpp"



#include "../game/pad.h"



void UI::render()



{



    ImGuiWrapper::render();







    if(m_bShowFPSInfo) {



        renderDebug();



    } else {



        if(label) label->setText(" ");



        if(label2) label2->setText(" ");



        if(label3) label3->setText(" ");



        if(label4) label4->setText(" ");



    }















    ProcessPushedTextdraws();







    if (m_bNeedClearMousePos) {



        ImGuiIO& io = ImGui::GetIO();



        io.MousePos = ImVec2(-1, -1);



        m_bNeedClearMousePos = false;



    }







    if (m_playerTabList && m_playerTabList->visible())



        m_playerTabList->Tick();



}







void UI::ToggleFPSInfo()



{



    m_bShowFPSInfo = !m_bShowFPSInfo;







    // Сохраняем настройку



    if(pSettings) {



        pSettings->SetFPSInfo(m_bShowFPSInfo);



    }



}







void UI::shutdown()



{



    ImGuiWrapper::shutdown();



}







void UI::drawList()



{



    if (!visible()) return;







    if (pPlayerTags) pPlayerTags->Render(renderer());



    if (pNetGame && pNetGame->GetTextLabelPool()) pNetGame->GetTextLabelPool()->Render(renderer());



    if (pNetGame && pNetGame->GetPlayerBubblePool()) pNetGame->GetPlayerBubblePool()->Render(renderer());







    if (pGPS) pGPS->DoPathDraw();







    draw(renderer());



}







void UI::touchEvent(const ImVec2& pos, TouchType type)



{



    if(!visible()) return;







    if (m_keyboard->visible() && m_keyboard->contains(pos))



    {



        m_keyboard->touchEvent(pos, type);



        return;



    }







    if (m_dialog->visible() && m_dialog->contains(pos))



    {



        m_dialog->touchEvent(pos, type);



        return;



    }







    if (m_chat && m_chat->isVisible() && (m_chat->contains(pos) || m_chat->isInteracting()))



    {



        m_chat->touchEvent(pos, type);



        return;



    }







    Widget::touchEvent(pos, type);



}







enum eTouchType



{



    TOUCH_POP = 1,



    TOUCH_PUSH = 2,



    TOUCH_MOVE = 3



};







static ImVec4 s_keysWindowRect = ImVec4(0.0f, 0.0f, 0.0f, 0.0f);







bool UI::isKeysWindowHovered(const ImVec2& pos)



{



    if (!visible()) return false;



    if (!IsClassicHudMode()) return false;



    return (pos.x >= s_keysWindowRect.x && pos.x <= s_keysWindowRect.z &&



            pos.y >= s_keysWindowRect.y && pos.y <= s_keysWindowRect.w);



}







static int s_iActiveImGuiPointer = -1;







bool UI::OnTouchEvent(int type, int num, int x, int y)



{



    ImGuiIO& io = ImGui::GetIO();







    switch (type)



    {



        case TOUCH_PUSH:



        {



            bool isKeysHit = isKeysWindowHovered(ImVec2(x, y));



            if (isKeysHit || s_iActiveImGuiPointer == -1)



            {



                s_iActiveImGuiPointer = num;



                io.MousePos = ImVec2(x, y);



                io.MouseDown[0] = true;



            }



            break;



        }







        case TOUCH_POP:



        {



            if (s_iActiveImGuiPointer == num || s_iActiveImGuiPointer == -1)



            {



                s_iActiveImGuiPointer = -1;



                io.MouseDown[0] = false;



                m_bNeedClearMousePos = true;



            }



            break;



        }







        case TOUCH_MOVE:



        {



            if (s_iActiveImGuiPointer == num || s_iActiveImGuiPointer == -1)



            {



                io.MousePos = ImVec2(x, y);



            }



            break;



        }



    }







    return true;



}







#include "../settings.h"



void UI::renderDebug()



{



    static bool s_labelsCleared = false;



    if (!s_labelsCleared) {



        if(label2) label2->setText(" ");



        if(label3) label3->setText(" ");



        if(label4) label4->setText(" ");



        s_labelsCleared = true;



    }







    // Throttle expensive mallinfo heap walk to once every 3 seconds



    static uint32_t s_lastMemCheck = 0;



    uint32_t nowTicks = CTimer::m_snTimeInMillisecondsNonClipped;



    if (nowTicks - s_lastMemCheck > 3000) {



        s_lastMemCheck = nowTicks;



        struct mallinfo memInfo = mallinfo();



        int totalAllocatedMB  = memInfo.uordblks / (1024 * 1024);



        if (totalAllocatedMB >= 600)



        {



            CStreaming::MakeSpaceFor(500);



        }



    }







    if (!m_bShowFPSInfo || !pSettings->Get().iFPSCounter || m_chat->active() || m_keyboard->visible())



    {



        if(label) label->setText(" ");



        return;



    }







    static float fps = 60.f;



    static auto lastTick = CTimer::m_snTimeInMillisecondsNonClipped;



    if(CTimer::m_snTimeInMillisecondsNonClipped - lastTick > 500) {



        lastTick = CTimer::m_snTimeInMillisecondsNonClipped;



        fps = std::clamp(CTimer::game_FPS, 10.f, (float) 120);



    }







    char szStr[30];



    snprintf(&szStr[0], sizeof(szStr), "FPS: %.0f", fps);



    ImVec2 pos = ImVec2(pUI->ScaleX(40.0f), pUI->ScaleY(1080.0f - UISettings::fontSize() * 3.5));



    label->setText(&szStr[0]);



    label->setPosition(pos);



}







void UI::PushToBufferedQueueTextDrawPressed(uint16_t textdrawId)



{



    BUFFERED_COMMAND_TEXTDRAW* pCmd = m_BufferedCommandTextdraws.WriteLock();







    pCmd->textdrawId = textdrawId;







    m_BufferedCommandTextdraws.WriteUnlock();



}







void UI::ProcessPushedTextdraws()



{



    BUFFERED_COMMAND_TEXTDRAW* pCmd = nullptr;



    while (pCmd = m_BufferedCommandTextdraws.ReadLock())



    {



        RakNet::BitStream bs;



        bs.Write(pCmd->textdrawId);



        pNetGame->GetRakClient()->RPC(&RPC_ClickTextDraw, &bs, HIGH_PRIORITY, RELIABLE_SEQUENCED, 0, false, UNASSIGNED_NETWORK_ID, 0);



        m_BufferedCommandTextdraws.ReadUnlock();



    }



}







void UI::SetEat(float eate){



    eat = static_cast<int>(eate);



}



void UI::SetDrink(float drinke){



    drink = static_cast<int>(drinke);



}







void UI::SetBankMoney(uint32_t bank)



{



    bankMoney = bank;



    FLog("SetBankMoney: %u", bankMoney);



}







int UI::GetEat(){



    return eat;



}







int UI::GetDrink(){



    return drink;



}



int UI::GetBankMoney() {



    return bankMoney;



}







void UI::SetFuel(float fuel) {



    m_fuel = static_cast<uint8_t>(fuel);



}







void UI::ShowSpeed(){



    if (!pJavaWrapper) return;



    static bool s_speedAlreadyHidden = false;



    if (s_speedAlreadyHidden) return;



    s_speedAlreadyHidden = true;







    if (!pGame || !pNetGame || !pGame->FindPlayerPed()->IsInVehicle()) {



        pJavaWrapper->HideSpeed();



        return;



    }



    if (pGame->FindPlayerPed()->IsAPassenger()) {



        pJavaWrapper->HideSpeed();



        return;



    }



    //скрытие спидака ебучего #CRMPGOVNO



    if (gMobileMenu->pendingScreen){



        pJavaWrapper->HideSpeed();



        return;



    }







    int i_speed = 0;



    int vehicleHP = 0;



    CVehicle *pVehicle = nullptr;



    CVehiclePool *pVehiclePool = pNetGame->GetVehiclePool();



    CPlayerPed *pPlayerPed = pGame->FindPlayerPed();



    VEHICLEID id = pVehiclePool->FindIDFromGtaPtr(pPlayerPed->GetGtaVehicle());



    pVehicle = pVehiclePool->GetAt(id);







    if (pUI->m_chat->active()){



        pJavaWrapper->HideSpeed();



        return;



    }







    if(pPlayerPed)



    {



        if(pVehicle && pVehicle->m_pVehicle)



        {



            CVector vecMoveSpeed = pVehicle->m_pVehicle->GetMoveSpeed();;



            i_speed = sqrt((vecMoveSpeed.x * vecMoveSpeed.x) + (vecMoveSpeed.y * vecMoveSpeed.y) + (vecMoveSpeed.z * vecMoveSpeed.z)) * 180;



            vehicleHP = (int)(pVehicle->m_pVehicle->fHealth / 10.0f);







            // Holatlarni to'g'ridan-to'g'ri avtomobildan olamiz



            bEngine = pVehicle->m_pVehicle->m_nVehicleFlags.bEngineOn;



            bLights = pVehicle->m_pVehicle->m_nVehicleFlags.bLightsOn;



            



            // Eshiklar holati (0 - ochiq, boshqa qiymatlar - qulflangan)



            bDoor = (pVehicle->m_pVehicle->m_nDoorLock != CARLOCK_UNLOCKED && 



                     pVehicle->m_pVehicle->m_nDoorLock != CARLOCK_NOT_USED) ? 1 : 0;



        }



    }



    // Disabled launcher speedometer per user request (server has its own speedometer)



    pJavaWrapper->HideSpeed();



}



void UI::renderKeys()
{
    if (!pNetGame) return;
    if (m_dialog && m_dialog->visible()) return;
    if (pGame && !pGame->IsToggledHUDElement(HUD_ELEMENT_BUTTONS)) return;

    ImGuiIO& io = ImGui::GetIO();
    float baseH = IsClassicHudMode() ? 44.0f : 50.0f;
    float btnH = ScaleY(baseH);
    if (btnH < 38.0f) btnH = 38.0f;
    float btnW = btnH * 1.35f;
    ImVec2 vecButSize = ImVec2(btnW, btnH);

    ImGui::PushStyleVar(ImGuiStyleVar_WindowPadding, ImVec2(2.0f, 2.0f));
    ImGui::PushStyleVar(ImGuiStyleVar_ItemSpacing, ImVec2(ScaleX(5.0f), 0.0f));
    ImGui::PushStyleVar(ImGuiStyleVar_FrameRounding, ScaleY(5.0f));
    ImGui::PushStyleVar(ImGuiStyleVar_FramePadding, ImVec2(0.0f, 0.0f));

    ImGui::PushStyleColor(ImGuiCol_Text, ImVec4(0.96f, 0.70f, 0.16f, 1.0f));
    ImGui::PushStyleColor(ImGuiCol_Button, ImVec4(0.24f, 0.26f, 0.28f, 0.80f));
    ImGui::PushStyleColor(ImGuiCol_ButtonHovered, ImVec4(0.35f, 0.38f, 0.42f, 0.90f));
    ImGui::PushStyleColor(ImGuiCol_ButtonActive, ImVec4(0.18f, 0.20f, 0.22f, 1.0f));
    ImGui::PushStyleColor(ImGuiCol_WindowBg, ImVec4(0.0f, 0.0f, 0.0f, 0.0f));
    ImGui::PushStyleColor(ImGuiCol_Border, ImVec4(0.0f, 0.0f, 0.0f, 0.0f));

    float keysPosY = std::max(io.DisplaySize.y * 0.33f, UISettings::chatPos().y + UISettings::chatSize().y + ScaleY(10.0f));
    float keysPosX = UISettings::chatPos().x;
    ImGui::SetNextWindowPos(ImVec2(keysPosX, keysPosY), ImGuiCond_Always);
    ImGui::Begin("###keys", nullptr,
        ImGuiWindowFlags_NoTitleBar |
        ImGuiWindowFlags_NoResize |
        ImGuiWindowFlags_NoScrollbar |
        ImGuiWindowFlags_NoSavedSettings |
        ImGuiWindowFlags_AlwaysAutoResize);
    ImGui::SetWindowFontScale(0.80f);

    // 1. Toggle button << / >>
    static uint32_t s_lastToggleTick = 0;
    if (ImGui::Button(m_bKeysStatus ? "<<" : ">>", vecButSize))
    {
        uint32_t now = GetTickCount();
        if (now - s_lastToggleTick >= 300)
        {
            s_lastToggleTick = now;
            m_bKeysStatus = !m_bKeysStatus;
        }
    }

    // 2. Alt button
    ImGui::SameLine();
    static uint32_t s_lastAltTick = 0;
    if (ImGui::Button("Alt", vecButSize))
    {
        uint32_t now = GetTickCount();
        if (now - s_lastAltTick >= 400)
        {
            s_lastAltTick = now;
            CPlayerPool* pPlayerPool = pNetGame->GetPlayerPool();
            if (pPlayerPool && pPlayerPool->GetLocalPlayer())
            {
                CPlayerPed* pPed = pPlayerPool->GetLocalPlayer()->GetPlayerPed();
                if (pPed && !pPed->IsInVehicle() && !pPed->IsAPassenger())
                {
                    LocalPlayerKeys.bKeys[ePadKeys::KEY_WALK] = true;
                }
                else
                {
                    LocalPlayerKeys.bKeys[ePadKeys::KEY_FIRE] = true;
                }
            }
        }
    }

    // Jetpack button (if in jetpack mode)
    CPlayerPool* pJetpackCheckPool = pNetGame->GetPlayerPool();
    if (pJetpackCheckPool && pJetpackCheckPool->GetLocalPlayer() && pJetpackCheckPool->GetLocalPlayer()->GetPlayerPed() &&
        pJetpackCheckPool->GetLocalPlayer()->GetPlayerPed()->IsInJetpackMode())
    {
        ImGui::SameLine();
        ImGui::Button("^", vecButSize);
        LocalPlayerKeys.bKeys[ePadKeys::KEY_ACTION] = ImGui::IsItemActive();
    }

    // 3. Situational G button
    CVehiclePool* pVehiclePool = pNetGame->GetVehiclePool();
    if (pVehiclePool)
    {
        CPlayerPool* pPlayerPool = pNetGame->GetPlayerPool();
        if (pPlayerPool && pPlayerPool->GetLocalPlayer())
        {
            CPlayerPed* pPed = pPlayerPool->GetLocalPlayer()->GetPlayerPed();
            if (pPed && !pPed->IsInVehicle() && !pPed->IsAPassenger())
            {
                VEHICLEID sNearestID = pVehiclePool->FindNearestToLocalPlayerPed();
                if (sNearestID < MAX_VEHICLES && pVehiclePool->GetSlotState(sNearestID))
                {
                    CVehicle* pVehicle = pVehiclePool->GetAt(sNearestID);
                    if (pVehicle && pVehicle->m_pVehicle)
                    {
                        if (pVehicle->m_pVehicle->GetDistanceFromLocalPlayerPed() < 5.0f)
                        {
                            ImGui::SameLine();
                            static uint32_t s_lastGTick = 0;
                            if (ImGui::Button("G", vecButSize))
                            {
                                uint32_t now = GetTickCount();
                                if (now - s_lastGTick >= 1000)
                                {
                                    s_lastGTick = now;
                                    pPed->EnterVehicle(pVehicle->m_dwGTAId, true);
                                    pPlayerPool->GetLocalPlayer()->SendEnterVehicleNotification(sNearestID, true);
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // 4. Situational Ctrl button (L.CTRL motor toggle, debounced 700ms, single key trigger)
    CPlayerPool* pPlayerPool = pNetGame->GetPlayerPool();
    if (pPlayerPool && pPlayerPool->GetLocalPlayer())
    {
        CPlayerPed* pPed = pPlayerPool->GetLocalPlayer()->GetPlayerPed();
        if (pPed && pPed->IsInVehicle() && !pPed->IsAPassenger())
        {
            ImGui::SameLine();
            static uint32_t s_lastCtrlTick = 0;
            if (ImGui::Button("Ctrl", vecButSize))
            {
                uint32_t now = GetTickCount();
                if (now - s_lastCtrlTick >= 700)
                {
                    s_lastCtrlTick = now;
                    LocalPlayerKeys.bKeys[ePadKeys::KEY_ACTION] = true;
                }
            }
        }
    }

    // 5. Expandable buttons Tab, Y, N, H (pure SA-MP keys, debounced 400ms)
    if (m_bKeysStatus)
    {
        static uint32_t s_lastTabTick = 0;
        static uint32_t s_lastYTick = 0;
        static uint32_t s_lastNTick = 0;
        static uint32_t s_lastHTick = 0;
        uint32_t now = GetTickCount();

        ImGui::SameLine();
        if (ImGui::Button("Tab", vecButSize))
        {
            if (now - s_lastTabTick >= 350)
            {
                s_lastTabTick = now;
                PlayerTabList* tab = playertablist();
                if (tab) {
                    if (tab->visible()) tab->hide();
                    else tab->show();
                }
            }
        }

        ImGui::SameLine();
        if (ImGui::Button("Y", vecButSize))
        {
            if (now - s_lastYTick >= 400)
            {
                s_lastYTick = now;
                LocalPlayerKeys.bKeys[ePadKeys::KEY_YES] = true;
            }
        }

        ImGui::SameLine();
        if (ImGui::Button("N", vecButSize))
        {
            if (now - s_lastNTick >= 400)
            {
                s_lastNTick = now;
                LocalPlayerKeys.bKeys[ePadKeys::KEY_NO] = true;
            }
        }

        ImGui::SameLine();
        if (ImGui::Button("H", vecButSize))
        {
            if (now - s_lastHTick >= 400)
            {
                s_lastHTick = now;
                LocalPlayerKeys.bKeys[ePadKeys::KEY_CTRL_BACK] = true;
            }
        }
    }

    ImVec2 winPos = ImGui::GetWindowPos();
    ImVec2 winSize = ImGui::GetWindowSize();
    s_keysWindowRect = ImVec4(winPos.x, winPos.y, winPos.x + winSize.x, winPos.y + winSize.y);

    ImGui::SetWindowFontScale(1.0f);
    ImGui::End();
    ImGui::PopStyleColor(6);
    ImGui::PopStyleVar(4);
}




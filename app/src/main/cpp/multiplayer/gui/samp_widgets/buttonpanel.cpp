#include "../../main.h"
#include "../gui.h"
#include "../../game/game.h"
#include "../../net/netgame.h"
#include "../../net/localplayer.h"
#include "../../net/netgame.h"

extern UI* pUI;
extern CNetGame* pNetGame;
extern CGame *pGame;

bool bNeedEnterVehicle = false;
bool OpenButton = false;
int Tab = 0;

ButtonPanel::ButtonPanel()
        : Layout(Orientation::HORIZONTAL)
{
    m_bAlt = new CButton("ALT", UISettings::fontSize() / 2);
    m_bToggle = new OButton(">>", UISettings::fontSize() / 2);
    m_bClose = new CButton("<<", UISettings::fontSize() / 2);
    m_bEsc = new CButton("ESC", UISettings::fontSize() / 2);
    m_bTab = new CButton("TAB", UISettings::fontSize() / 2);
    m_bH = new CButton("H", UISettings::fontSize() / 2);
    m_bY = new CButton("Y", UISettings::fontSize() / 2);
    m_bN = new CButton("N", UISettings::fontSize() / 2);
    m_bF = new CButton("F", UISettings::fontSize() / 2);
    m_bP = new CButton("+", UISettings::fontSize() / 2);
    m_bG = new CButton("G", UISettings::fontSize() / 2);
    m_bFOOD = new CButton("FOOD", UISettings::fontSize() / 2);
    m_bGPS = new CButton("GPS", UISettings::fontSize() / 2);
    m_bD = new CButton("D", UISettings::fontSize() / 2);
    m_bUSE = new CButton("USE", UISettings::fontSize() / 2);
    m_b2 = new CButton("2", UISettings::fontSize() / 2);

    m_bTab->setCallback([this]()
                        {
                            if (Tab == 0) {
                                if (pUI) {
                                    pUI->playertablist()->show();
                                    Tab = 1;
                                }
                            }
                            else {
                                Tab = 0;
                            }
                        });

    m_bToggle->setCallback([this]() {
        OpenButton = !OpenButton;
        m_bToggle->setCaption(OpenButton ? "<<" : ">>");


        if(m_bH) m_bH->setVisible(OpenButton);
        if(m_bN) m_bN->setVisible(OpenButton);
        if(m_bG) m_bG->setVisible(OpenButton);
        if(m_bY) m_bY->setVisible(OpenButton);
        if(m_bF) m_bF->setVisible(OpenButton);

        if(m_bEsc) m_bEsc->setVisible(false);
        if(m_b2) m_b2->setVisible(false);

        this->performLayout();
    });

    m_bEsc->setCallback([this]() {
        if (pNetGame && m_bEsc->visible()) {
            CTextDrawPool* pTextDrawPool = pNetGame->GetTextDrawPool();
            if (pTextDrawPool) {
                pTextDrawPool->SetSelectState(false, 0);
            }
        }
    });

    m_bAlt->setCallback([this]() {
        if(m_bAlt->visible())
            LocalPlayerKeys.bKeys[ePadKeys::KEY_WALK] = true;
    });

    m_bH->setCallback([this]() {
        if(m_bH->visible())
            LocalPlayerKeys.bKeys[ePadKeys::KEY_CTRL_BACK] = true;
    });

    m_bY->setCallback([this]() {
        if(m_bY->visible())
            LocalPlayerKeys.bKeys[ePadKeys::KEY_YES] = true;
    });

    m_bN->setCallback([this]() {
        if(m_bN->visible())
            LocalPlayerKeys.bKeys[ePadKeys::KEY_NO] = true;
    });

    m_bF->setCallback([this]() {
        if(m_bF->visible())
            LocalPlayerKeys.bKeys[ePadKeys::KEY_SECONDARY_ATTACK] = true;
    });

    m_bP->setCallback([this]() {
        if(m_bP->visible())
            LocalPlayerKeys.bKeys[ePadKeys::KEY_SUBMISSION] = true;
    });

    m_bG->setCallback([this]() {
        if(m_bG->visible())
            bNeedEnterVehicle = true;
    });

    m_bFOOD->setCallback([]() {
        // pGame->FindPlayerPed()->SetCuffedOrCarry(1, 0);
        // pGame->FindPlayerPed()->ProcessCuffAndCarry();
    });

    m_bGPS->setCallback([]() {
        pNetGame->SendChatCommand("/gps");
    });

    m_bD->setCallback([]() {
        LocalPlayerKeys.bKeys[ePadKeys::KEY_SUBMISSION] = true;
    });

    m_bUSE->setCallback([]() {
        LocalPlayerKeys.bKeys[ePadKeys::KEY_ANALOG_LEFT] = true;
    });

    m_b2->setCallback([this]() {
        if(m_b2->visible())
            LocalPlayerKeys.bKeys[ePadKeys::KEY_SUBMISSION] = true;
    });

    // Keep TAB and ALT visible in both states
    m_bTab->setAlwaysVisible(true);
    m_bAlt->setAlwaysVisible(true);

    m_bEsc->setVisible(false);
    m_bH->setVisible(false);
    m_bF->setVisible(false);
    m_bY->setVisible(false);
    m_bN->setVisible(false);
    m_bG->setVisible(false);
    m_b2->setVisible(false);

    this->addChild(m_bTab);
    this->addChild(m_bAlt);
    this->addChild(m_bToggle);

    this->addChild(m_bH);
    this->addChild(m_bN);
    this->addChild(m_bG);
    this->addChild(m_bY);
    this->addChild(m_bF);

    this->addChild(m_bEsc);
    //if (Server == 10) this->addChild(m_bP);
    //if (Server == 24) this->addChild(m_bD);
    //if (Server == 24) this->addChild(m_bUSE);
    //this->addChild(m_bFOOD);
    //if (Server == 21) this->addChild(m_bGPS);
    this->addChild(m_b2);
}
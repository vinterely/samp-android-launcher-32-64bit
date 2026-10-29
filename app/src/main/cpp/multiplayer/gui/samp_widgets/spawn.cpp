#include "../gui.h"
#include "../../game/game.h"
#include "../../net/netgame.h"

extern CNetGame* pNetGame;

Spawn::Spawn()
        : Layout(Orientation::HORIZONTAL)
{
    m_buttonPrev = new Button(std::string((const char*)u8"<<"), UISettings::fontSize());
    m_buttonSpawn = new Button(std::string((const char*)u8"Spawn"), UISettings::fontSize());
    m_buttonNext = new Button(std::string((const char*)u8">>"), UISettings::fontSize());

    m_buttonPrev->setCallback([]() { if (pNetGame) pNetGame->SendPrevClass(); });
    m_buttonSpawn->setCallback([]() { if (pNetGame) pNetGame->SendSpawn(); });
    m_buttonNext->setCallback([]() { if (pNetGame) pNetGame->SendNextClass(); });

    m_buttonPrev->setVisible(false);
    m_buttonSpawn->setVisible(false);
    m_buttonNext->setVisible(false);

    this->addChild(m_buttonPrev);
    this->addChild(m_buttonSpawn);
    this->addChild(m_buttonNext);
}
#include "playerTabList.h"
#include "../../game/game.h"
#include "../../net/netgame.h"
#include "../gui/gui.h"
#include "../../main.h"
#include "../java/jniutil.h"
#include <chrono>
#include <algorithm>

extern UI* pUI;
extern CGame* pGame;
extern CNetGame* pNetGame;
extern CJavaWrapper* pJavaWrapper;

PlayerTabList::PlayerTabList()
        : m_bVisible(false),
          m_lastUpdateTick(0)
{
    Tick();
}

PlayerTabList::~PlayerTabList()
{
    m_players.clear();
}

void PlayerTabList::show()
{
    if (m_bVisible || !pNetGame) return;

    LOGI("PlayerTabList shown");

    pNetGame->UpdatePlayerScoresAndPings();
    Update();

    pGame->EnableGameInput(false);
    pUI->keyboard()->setVisible(false);
    pJavaWrapper->ShowTabWindow();

    m_bVisible = true;
}

void PlayerTabList::hide()
{
    if (!m_bVisible) return;

    pGame->EnableGameInput(true);
    m_bVisible = false;
}

void PlayerTabList::Update()
{
    if (!pNetGame) return;

    CPlayerPool* pool = pNetGame->GetPlayerPool();
    if (!pool) return;

    PLAYERID totalPlayers = pool->GetCount() + 1;
    m_players.resize(totalPlayers);

    // --- LocalPlayer ---
    m_players[0].dwID = pool->GetLocalPlayerID();
    m_players[0].szName = pool->GetLocalPlayerName();
    m_players[0].iScore = pool->GetLocalPlayerScore();
    m_players[0].dwPing = pool->GetLocalPlayerPing();
    m_players[0].dwColor = 0xFFFFFFFF;
    m_players[0].iState = 0;

    pJavaWrapper->SetTabStat(
            m_players[0].dwID,
            m_players[0].szName,
            m_players[0].iScore,
            m_players[0].dwPing
    );

    pJavaWrapper->HideHud();

    // --- RemotePlayer ---
    PLAYERID idx = 1;
    for (PLAYERID i = 0; i < MAX_PLAYERS; i++)
    {
        if (!pool->GetSlotState(i)) continue;

        m_players[idx].dwID = i;
        m_players[idx].szName = pool->GetPlayerName(i);
        m_players[idx].iScore = pool->GetPlayerScore(i);
        m_players[idx].dwPing = pool->GetPlayerPing(i);
        m_players[idx].dwColor = 0xFFFFFFFF;
        m_players[idx].iState = 0;

        pJavaWrapper->SetTabStat(
                m_players[idx].dwID,
                m_players[idx].szName,
                m_players[idx].iScore,
                m_players[idx].dwPing
        );

        idx++;
    }
}

void PlayerTabList::Tick()
{
    if (!m_bVisible || !pNetGame) return;

    uint32_t now = static_cast<uint32_t>(
            std::chrono::duration_cast<std::chrono::milliseconds>(
                    std::chrono::steady_clock::now().time_since_epoch()
            ).count()
    );

    if (now - m_lastUpdateTick >= 500)
    {
        m_lastUpdateTick = now;
        pNetGame->UpdatePlayerScoresAndPings();
        Update();
    }
}

void PlayerTabList::activateEvent(bool active)
{
    if (!active) hide();
}

bool PlayerTabList::visible() const
{
    return m_bVisible;
}

void PlayerTabList::setVisible(bool v)
{
    m_bVisible = v;
}
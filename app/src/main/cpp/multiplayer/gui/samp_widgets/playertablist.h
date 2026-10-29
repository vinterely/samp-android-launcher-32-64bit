#pragma once
#include <cstdint>
#include <vector>
#include <string>

typedef unsigned short PLAYERID;

struct PLAYER_SCORE_INFO
{
    uint32_t dwID;
    char* szName;
    int iScore;
    int32_t dwPing;
    uint32_t dwColor;
    int iState;
};

class CNetGame;
class CPlayerPool;
class CJavaWrapper;

class PlayerTabList
{
public:
    PlayerTabList();
    ~PlayerTabList();

    void show();
    void hide();
    void Update();
    void Tick();

    void activateEvent(bool active);
    bool visible() const;
    void setVisible(bool v);

private:
    bool m_bVisible;
    uint32_t m_lastUpdateTick;

    std::vector<PLAYER_SCORE_INFO> m_players;
};
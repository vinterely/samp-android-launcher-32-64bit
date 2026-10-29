//
// Created by GLORY TEAM.
//

#pragma once


struct CTimeInfo {
    uint8_t m_nTimeOn;
    uint8_t m_nTimeOff;
    int16_t m_nOtherTimeModel = -1;
};


static_assert(sizeof(CTimeInfo) == 0x4, "Invalid size CTimeInfo");
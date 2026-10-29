#pragma once

#include <string>

struct Glory
{
    std::string szHost = "play.glory-rp.ir";
    int iPort = 7777;
};

inline Glory GetGlory()
{
    Glory config;
    return config;
}

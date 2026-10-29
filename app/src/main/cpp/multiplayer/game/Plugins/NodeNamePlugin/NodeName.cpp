//
// Created by GLORY TEAM.
//

#include "NodeName.h"
#include "../vendor/armhook/patch.h"

const RwChar* GetFrameNodeName(RwFrame* frame) {
    return CHook::CallFunction<const RwChar*>(g_libGTASA + (VER_x32 ? 0x48248C + 1 : 0x574218), frame);
}
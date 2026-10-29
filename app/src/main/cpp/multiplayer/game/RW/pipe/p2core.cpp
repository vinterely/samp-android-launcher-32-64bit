//
// Created by GLORY TEAM.
//

#include "p2core.h"
#include "../vendor/armhook//patch.h"

RxPipeline* RxPipelineCreate() {
    return CHook::CallFunction<RxPipeline*>("_Z16RxPipelineCreatev");
}
//
// Created by GLORY TEAM.
//

#include "RxPipeline.h"
#include "../vendor/armhook/patch.h"
#include "pipe/p2core.h"

RxNodeDefinition* RxNodeDefinitionGetOpenGLAtomicAllInOne() {
    return CHook::CallFunction<RxNodeDefinition*>("_Z39RxNodeDefinitionGetOpenGLAtomicAllInOnev");
}

void RxOpenGLAllInOneSetInstanceCallBack(RxPipelineNode *node, RxOpenGLAllInOneInstanceCallBack instanceCB) {
    CHook::CallFunction<void>("_Z35RxOpenGLAllInOneSetInstanceCallBackP14RxPipelineNodePFiPvP24RxOpenGLMeshInstanceDataiiE", node, instanceCB);
}

void RxOpenGLAllInOneSetRenderCallBack(RxPipelineNode* node, RxOpenGLAllInOneRenderCallBack renderCB) {
    CHook::CallFunction<void>("_Z33RxOpenGLAllInOneSetRenderCallBackP14RxPipelineNodePFvP10RwResEntryPvhjE", node, renderCB);
}

void _rxPipelineDestroy(RxPipeline *Pipeline) {
    CHook::CallFunction<void>("_Z18_rxPipelineDestroyP10RxPipeline", Pipeline);
}
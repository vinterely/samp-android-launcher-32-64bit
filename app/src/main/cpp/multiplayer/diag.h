#pragma once
#include <stdint.h>
#include <stdbool.h>

#ifdef __cplusplus
extern "C" {
#endif

void Diag_Init();
void Diag_Breadcrumb(const char* tag);
void Diag_HookLog(const char* name, void* addr, bool success);

// Lock-free, allocation-free render breadcrumb for crash diagnostics
typedef struct {
    uintptr_t entityPtr;
    uint32_t  modelIndex;
    uint32_t  entityType;
    uint32_t  pad0;
    uint64_t  entityRenderCount;

    uintptr_t resEntryPtr;
    uintptr_t objectPtr;
    uintptr_t geometryPtr;
    uint32_t  pipeType;
    uint32_t  pipeFlags;
    uint16_t  numMeshes;
    uint16_t  pad1;
    uint32_t  pad2;
    uint64_t  pipeRenderCount;
} RenderBreadcrumb_t;

extern RenderBreadcrumb_t g_RenderBreadcrumb;

void Diag_LogInvalidMesh(uint32_t modelIndex, const char* reason);

bool Diag_IsNoHookPipelines();
bool Diag_IsNoWeaponHooks();
bool Diag_IsNoRadarSync();
bool Diag_IsNoChatTouch();
bool Diag_IsNoChat();
bool Diag_IsNoMoneySync();
bool Diag_IsNoWeaponHud();
bool Diag_IsNoPerfLogger();

#ifdef __cplusplus
}
#endif

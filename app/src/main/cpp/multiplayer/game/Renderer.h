//
// Created by GLORY TEAM.
//

#ifndef GLORY_RENDERER_H
#define GLORY_RENDERER_H

#include "game/common.h"

struct CEntityGTA;
constexpr auto MAX_INVISIBLE_ENTITY_PTRS = 150u;
constexpr auto MAX_VISIBLE_ENTITY_PTRS   = 1000u;
constexpr auto MAX_VISIBLE_LOD_PTRS      = 1000u;
constexpr auto MAX_VISIBLE_SUPERLOD_PTRS = 50u;

class CRenderer {
public:
    static inline bool ms_bRenderOutsideTunnels;
    static inline bool m_loadingPriority;

    static inline CEntityGTA* ms_aVisibleEntityPtrs[MAX_VISIBLE_ENTITY_PTRS];
    static inline int32 ms_nNoOfVisibleEntities;

public:
    static void InjectHooks();
};


#endif //GLORY_RENDERER_H

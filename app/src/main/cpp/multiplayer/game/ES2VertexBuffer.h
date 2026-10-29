#pragma once

#include <GLES3/gl32.h>
#include "RQVertexBuffer.h"

struct ES2VertexBuffer : RQVertexBuffer
{
    unsigned int    size;
    GLuint          bufferId;

public:
    static inline void *curCPUBuffer = nullptr;

public:
    static void InjectHooks();
};
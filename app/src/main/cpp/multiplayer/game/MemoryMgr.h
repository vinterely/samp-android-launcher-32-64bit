//
// Created by GLORY TEAM.
//

#ifndef GLORY_MEMORYMGR_H
#define GLORY_MEMORYMGR_H


class CMemoryMgr {

public:
    static void Init();
    static void *Malloc(unsigned int size, unsigned int nHint);

    static void Free(void *memory);

    static void *Malloc(unsigned int size);

    static void FreeAlign(void *memory);

    static unsigned char *MallocAlign(unsigned int size, unsigned int align, unsigned int nHint);

    static void *MallocAlign(unsigned int size, unsigned int align);

    static void *MoveMemory(void *memory);
};


#endif //GLORY_MEMORYMGR_H
//
// Created by GLORY TEAM.
//

#include <dlfcn.h>
#include "CUtil.h"
#include "../diag.h"
#include "game/Textures/TextureDatabaseRuntime.h"
#include "vendor/armhook/patch.h"


uintptr_t CUtil::FindLib(const char* libname)
{
    void* handle = dlopen(libname, RTLD_LAZY);
    if (handle) {
        void* symbol = dlsym(handle, "JNI_OnLoad");
        if (symbol) {
            Dl_info info;
            if (dladdr(symbol, &info) != 0) {
                return reinterpret_cast<uintptr_t>(info.dli_fbase);
            }
        }
        dlclose(handle);
    }

    return 0;
}

RwTexture* CUtil::LoadTextureFromDB(const char* dbname, const char* texture)
{
    TextureDatabaseRuntime* db_handle = TextureDatabaseRuntime::GetDatabase(dbname);
    if(!db_handle)
    {
        FLog("Error: Database not found! (%s)", dbname);
        return nullptr;
    }

    TextureDatabaseRuntime::Register(db_handle);

    auto tex = CUtil::GetTexture(texture);
    if(!tex)
    {
        FLog("Error: Texture (%s) not found in database (%s)", dbname, texture);
        return nullptr;
    }

    TextureDatabaseRuntime::Unregister(db_handle);

    return tex;
}

RwTexture* CUtil::GetTexture(const char* name)
{
    static bool s_texDbLogged = false;
    if (!s_texDbLogged) {
        s_texDbLogged = true;
        Diag_Breadcrumb("Texture DB loaded / lookup (CUtil::GetTexture)");
    }
    auto tex = TextureDatabaseRuntime::GetTexture(name);
    if (!tex)
    {
        // Fallback: check CTxdStore::TxdStoreFindCB (which checks gta_int, gta3, and TXD dictionary)
        tex = CHook::CallFunction<RwTexture*>(g_libGTASA + (VER_x32 ? 0x005D3F4C + 1 : 0x6F8B54), name);
    }
    if (!tex)
    {
        // Extra fallback: temporarily register other databases
        static const char* const s_extraDbs[] = { "samp", "mobile", "txd", "gta3", "gta_int" };
        for (const char* dbName : s_extraDbs) {
            TextureDatabaseRuntime* db = TextureDatabaseRuntime::GetDatabase(dbName);
            if (db) {
                TextureDatabaseRuntime::Register(db);
                tex = TextureDatabaseRuntime::GetTexture(name);
                TextureDatabaseRuntime::Unregister(db);
                if (tex) break;
            }
        }
    }
    if (!tex)
    {
        FLog("WARNING! No tex = %s", name);
        return nullptr;
    }
    ++tex->refCount;

    return tex;
}

void __fastcall CUtil::TransformPoint(RwV3d &result, const CSimpleTransform &t, const RwV3d &v)
{
    float cos_heading = cosf(t.m_fHeading);
    float sin_heading = sinf(t.m_fHeading);

    result = {
            t.m_vPosn.x + cos_heading * v.x - sin_heading * v.y,
            t.m_vPosn.y + sin_heading * v.x + cos_heading * v.y,
            v.z + t.m_vPosn.z
    };
}
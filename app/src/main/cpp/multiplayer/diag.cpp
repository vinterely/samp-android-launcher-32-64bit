#include "diag.h"
#include <fcntl.h>
#include <unistd.h>
#include <signal.h>
#include <pthread.h>
#include <ucontext.h>
#include <dlfcn.h>
#include <unwind.h>
#include <stdio.h>
#include <string.h>
#include <time.h>
#include <sys/stat.h>
#include <sys/types.h>
#include <android/log.h>
#include "main.h"

extern char* g_pszStorage;

static bool s_diagInitialized = false;

// Global lock-free breadcrumb for render tracking
RenderBreadcrumb_t g_RenderBreadcrumb = {0};

void Diag_LogInvalidMesh(uint32_t modelIndex, const char* reason)
{
    static uint32_t s_lastLoggedModel = 0xFFFFFFFF;
    static time_t s_lastLogTime = 0;
    time_t now = time(nullptr);
    if (modelIndex != s_lastLoggedModel || (now - s_lastLogTime) >= 2) {
        s_lastLoggedModel = modelIndex;
        s_lastLogTime = now;
        __android_log_print(ANDROID_LOG_WARN, "GLORY_RENDER",
            "[RENDER_SKIP] Skipped atomic render: model=%u, reason=%s",
            modelIndex, reason ? reason : "invalid pointer");
    }
}

// Bisect switches
static bool s_noHookPipelines = false;
static bool s_noWeaponHooks   = false;
static bool s_noRadarSync     = false;
static bool s_noChatTouch     = false;
static bool s_noChat          = false;
static bool s_noMoneySync     = false;
static bool s_noWeaponHud     = false;
static bool s_noPerfLogger    = false;

static struct sigaction s_oldSigActionSegv;
static struct sigaction s_oldSigActionAbrt;
static struct sigaction s_oldSigActionBus;
static struct sigaction s_oldSigActionFpe;
static struct sigaction s_oldSigActionIll;

static uint8_t s_altStackBuffer[SIGSTKSZ * 4];

static void GetDiagPath(char* outPath, size_t outSize, const char* filename)
{
    if (g_pszStorage && g_pszStorage[0]) {
        snprintf(outPath, outSize, "%s%s", g_pszStorage, filename);
    } else {
        snprintf(outPath, outSize, "/storage/emulated/0/GloryMobile/%s", filename);
    }
}

static void SafeWrite(int fd, const char* str)
{
    if (fd < 0 || !str) return;
    size_t len = 0;
    while (str[len]) len++;
    write(fd, str, len);
}

void Diag_Breadcrumb(const char* tag)
{
    if (!tag) return;
    char path[512];
    GetDiagPath(path, sizeof(path), "diag_log.txt");

    int fd = open(path, O_WRONLY | O_CREAT | O_APPEND, 0666);
    if (fd >= 0) {
        struct timespec ts;
        clock_gettime(CLOCK_REALTIME, &ts);
        struct tm tm_buf;
        localtime_r(&ts.tv_sec, &tm_buf);

        char buf[256];
        int len = snprintf(buf, sizeof(buf), "[%04d-%02d-%02d %02d:%02d:%02d.%03ld] [DIAG] %s\n",
                           tm_buf.tm_year + 1900, tm_buf.tm_mon + 1, tm_buf.tm_mday,
                           tm_buf.tm_hour, tm_buf.tm_min, tm_buf.tm_sec,
                           ts.tv_nsec / 1000000L, tag);
        if (len > 0) write(fd, buf, len);
        fsync(fd);
        close(fd);
    }
    __android_log_print(ANDROID_LOG_INFO, "GLORY_DIAG", "[BREADCRUMB] %s", tag);
}

void Diag_HookLog(const char* name, void* addr, bool success)
{
    char buf[256];
    snprintf(buf, sizeof(buf), "Hook %s (%p): %s",
             name ? name : "unknown", addr, success ? "SUCCESS" : "FAILED");
    Diag_Breadcrumb(buf);
}

struct UnwindState {
    int fd;
    int frame;
};

static _Unwind_Reason_Code UnwindCallback(struct _Unwind_Context* context, void* arg)
{
    UnwindState* state = (UnwindState*)arg;
    uintptr_t pc = _Unwind_GetIP(context);
    if (pc) {
        Dl_info info;
        char line[256];
        if (dladdr((void*)pc, &info) && info.dli_fname) {
            const char* sname = info.dli_sname ? info.dli_sname : "???";
            uintptr_t rel_pc = pc - (uintptr_t)info.dli_fbase;
            uintptr_t sym_offset = info.dli_saddr ? (pc - (uintptr_t)info.dli_saddr) : 0;
            int len = snprintf(line, sizeof(line), "  #%02d pc 0x%08lx %s (%s+0x%lx)\n",
                               state->frame++, (unsigned long)rel_pc, info.dli_fname, sname, (unsigned long)sym_offset);
            if (len > 0) write(state->fd, line, len);
        } else {
            int len = snprintf(line, sizeof(line), "  #%02d pc 0x%08lx <unknown>\n",
                               state->frame++, (unsigned long)pc);
            if (len > 0) write(state->fd, line, len);
        }
    }
    if (state->frame >= 32) return _URC_END_OF_STACK;
    return _URC_NO_REASON;
}

static void SignalCrashHandler(int sig, siginfo_t* info, void* ucontext)
{
    char path[512];
    GetDiagPath(path, sizeof(path), "diag_log.txt");

    int fd = open(path, O_WRONLY | O_CREAT | O_APPEND, 0666);
    if (fd >= 0) {
        char threadName[64] = "unknown";
        pthread_getname_np(pthread_self(), threadName, sizeof(threadName));

        const char* sigName = "UNKNOWN";
        switch (sig) {
            case SIGSEGV: sigName = "SIGSEGV (Segmentation violation)"; break;
            case SIGABRT: sigName = "SIGABRT (Abort)"; break;
            case SIGBUS:  sigName = "SIGBUS (Bus error)"; break;
            case SIGFPE:  sigName = "SIGFPE (Floating point exception)"; break;
            case SIGILL:  sigName = "SIGILL (Illegal instruction)"; break;
        }

        char header[512];
        int hlen = snprintf(header, sizeof(header),
            "\n========== CRASH REPORT ==========\n"
            "Signal: %d [%s]\n"
            "Thread: %s (tid %d)\n"
            "Fault Address: %p\n",
            sig, sigName, threadName, gettid(), info ? info->si_addr : nullptr);
        if (hlen > 0) write(fd, header, hlen);

#if defined(__aarch64__)
        ucontext_t* uc = (ucontext_t*)ucontext;
        if (uc) {
            uintptr_t pc = uc->uc_mcontext.pc;
            uintptr_t lr = uc->uc_mcontext.regs[30];
            uintptr_t sp = uc->uc_mcontext.sp;
            char rBuf[1200];
            int rlen = snprintf(rBuf, sizeof(rBuf),
                "Registers (ARM64 full dump):\n"
                "  pc:  %016lx  lr:  %016lx  sp:  %016lx  pstate: %016lx\n"
                "  x0:  %016lx  x1:  %016lx  x2:  %016lx  x3:  %016lx\n"
                "  x4:  %016lx  x5:  %016lx  x6:  %016lx  x7:  %016lx\n"
                "  x8:  %016lx  x9:  %016lx  x10: %016lx  x11: %016lx\n"
                "  x12: %016lx  x13: %016lx  x14: %016lx  x15: %016lx\n"
                "  x16: %016lx  x17: %016lx  x18: %016lx  x19: %016lx\n"
                "  x20: %016lx  x21: %016lx  x22: %016lx  x23: %016lx\n"
                "  x24: %016lx  x25: %016lx  x26: %016lx  x27: %016lx\n"
                "  x28: %016lx  x29: %016lx  x30: %016lx\n",
                (unsigned long)pc, (unsigned long)lr, (unsigned long)sp, (unsigned long)uc->uc_mcontext.pstate,
                (unsigned long)uc->uc_mcontext.regs[0], (unsigned long)uc->uc_mcontext.regs[1],
                (unsigned long)uc->uc_mcontext.regs[2], (unsigned long)uc->uc_mcontext.regs[3],
                (unsigned long)uc->uc_mcontext.regs[4], (unsigned long)uc->uc_mcontext.regs[5],
                (unsigned long)uc->uc_mcontext.regs[6], (unsigned long)uc->uc_mcontext.regs[7],
                (unsigned long)uc->uc_mcontext.regs[8], (unsigned long)uc->uc_mcontext.regs[9],
                (unsigned long)uc->uc_mcontext.regs[10], (unsigned long)uc->uc_mcontext.regs[11],
                (unsigned long)uc->uc_mcontext.regs[12], (unsigned long)uc->uc_mcontext.regs[13],
                (unsigned long)uc->uc_mcontext.regs[14], (unsigned long)uc->uc_mcontext.regs[15],
                (unsigned long)uc->uc_mcontext.regs[16], (unsigned long)uc->uc_mcontext.regs[17],
                (unsigned long)uc->uc_mcontext.regs[18], (unsigned long)uc->uc_mcontext.regs[19],
                (unsigned long)uc->uc_mcontext.regs[20], (unsigned long)uc->uc_mcontext.regs[21],
                (unsigned long)uc->uc_mcontext.regs[22], (unsigned long)uc->uc_mcontext.regs[23],
                (unsigned long)uc->uc_mcontext.regs[24], (unsigned long)uc->uc_mcontext.regs[25],
                (unsigned long)uc->uc_mcontext.regs[26], (unsigned long)uc->uc_mcontext.regs[27],
                (unsigned long)uc->uc_mcontext.regs[28], (unsigned long)uc->uc_mcontext.regs[29],
                (unsigned long)uc->uc_mcontext.regs[30]);
            if (rlen > 0) write(fd, rBuf, rlen);

            // Safe dump of 32 bytes of code at pc via /proc/self/mem
            SafeWrite(fd, "Code at PC:\n  ");
            int mem_fd = open("/proc/self/mem", O_RDONLY);
            uint8_t codeBytes[32];
            bool codeRead = false;
            if (mem_fd >= 0) {
                if (lseek64(mem_fd, (off64_t)pc, SEEK_SET) != (off64_t)-1) {
                    ssize_t bytesRead = read(mem_fd, codeBytes, sizeof(codeBytes));
                    if (bytesRead > 0) {
                        codeRead = true;
                        char hexBuf[160];
                        int hpos = 0;
                        for (int i = 0; i < bytesRead; i++) {
                            hpos += snprintf(hexBuf + hpos, sizeof(hexBuf) - hpos, "%02x ", codeBytes[i]);
                            if ((i + 1) % 16 == 0 && i + 1 < bytesRead) {
                                hpos += snprintf(hexBuf + hpos, sizeof(hexBuf) - hpos, "\n  ");
                            }
                        }
                        hpos += snprintf(hexBuf + hpos, sizeof(hexBuf) - hpos, "\n");
                        write(fd, hexBuf, hpos);
                    }
                }
                close(mem_fd);
            }
            if (!codeRead) {
                SafeWrite(fd, "<unreadable>\n");
            }
        }
#elif defined(__arm__)
        ucontext_t* uc = (ucontext_t*)ucontext;
        if (uc) {
            uintptr_t pc = uc->uc_mcontext.arm_pc;
            uintptr_t lr = uc->uc_mcontext.arm_lr;
            uintptr_t sp = uc->uc_mcontext.arm_sp;
            char rBuf[600];
            int rlen = snprintf(rBuf, sizeof(rBuf),
                "Registers (ARM32 full dump):\n"
                "  pc:  %08lx  lr:  %08lx  sp:  %08lx  cpsr: %08lx\n"
                "  r0:  %08lx  r1:  %08lx  r2:  %08lx  r3:  %08lx\n"
                "  r4:  %08lx  r5:  %08lx  r6:  %08lx  r7:  %08lx\n"
                "  r8:  %08lx  r9:  %08lx  r10: %08lx  r11: %08lx  r12: %08lx\n",
                (unsigned long)pc, (unsigned long)lr, (unsigned long)sp, (unsigned long)uc->uc_mcontext.arm_cpsr,
                (unsigned long)uc->uc_mcontext.arm_r0, (unsigned long)uc->uc_mcontext.arm_r1,
                (unsigned long)uc->uc_mcontext.arm_r2, (unsigned long)uc->uc_mcontext.arm_r3,
                (unsigned long)uc->uc_mcontext.arm_r4, (unsigned long)uc->uc_mcontext.arm_r5,
                (unsigned long)uc->uc_mcontext.arm_r6, (unsigned long)uc->uc_mcontext.arm_r7,
                (unsigned long)uc->uc_mcontext.arm_r8, (unsigned long)uc->uc_mcontext.arm_r9,
                (unsigned long)uc->uc_mcontext.arm_r10, (unsigned long)uc->uc_mcontext.arm_fp,
                (unsigned long)uc->uc_mcontext.arm_ip);
            if (rlen > 0) write(fd, rBuf, rlen);

            SafeWrite(fd, "Code at PC:\n  ");
            int mem_fd = open("/proc/self/mem", O_RDONLY);
            uint8_t codeBytes[32];
            bool codeRead = false;
            if (mem_fd >= 0) {
                if (lseek64(mem_fd, (off64_t)(pc & ~1), SEEK_SET) != (off64_t)-1) {
                    ssize_t bytesRead = read(mem_fd, codeBytes, sizeof(codeBytes));
                    if (bytesRead > 0) {
                        codeRead = true;
                        char hexBuf[160];
                        int hpos = 0;
                        for (int i = 0; i < bytesRead; i++) {
                            hpos += snprintf(hexBuf + hpos, sizeof(hexBuf) - hpos, "%02x ", codeBytes[i]);
                            if ((i + 1) % 16 == 0 && i + 1 < bytesRead) {
                                hpos += snprintf(hexBuf + hpos, sizeof(hexBuf) - hpos, "\n  ");
                            }
                        }
                        hpos += snprintf(hexBuf + hpos, sizeof(hexBuf) - hpos, "\n");
                        write(fd, hexBuf, hpos);
                    }
                }
                close(mem_fd);
            }
            if (!codeRead) {
                SafeWrite(fd, "<unreadable>\n");
            }
        }
#endif

        char bcBuf[512];
        int bclen = snprintf(bcBuf, sizeof(bcBuf),
            "Last Render Breadcrumb:\n"
            "  Entity: ptr=%p model=%u type=%u (count=%llu)\n"
            "  Pipe:   resEntry=%p object=%p geom=%p type=%u flags=0x%x meshes=%u (count=%llu)\n",
            (void*)g_RenderBreadcrumb.entityPtr,
            g_RenderBreadcrumb.modelIndex,
            g_RenderBreadcrumb.entityType,
            (unsigned long long)g_RenderBreadcrumb.entityRenderCount,
            (void*)g_RenderBreadcrumb.resEntryPtr,
            (void*)g_RenderBreadcrumb.objectPtr,
            (void*)g_RenderBreadcrumb.geometryPtr,
            g_RenderBreadcrumb.pipeType,
            g_RenderBreadcrumb.pipeFlags,
            g_RenderBreadcrumb.numMeshes,
            (unsigned long long)g_RenderBreadcrumb.pipeRenderCount);
        if (bclen > 0) write(fd, bcBuf, bclen);

        SafeWrite(fd, "Backtrace:\n");
        UnwindState state = { fd, 0 };
        _Unwind_Backtrace(UnwindCallback, &state);

        SafeWrite(fd, "==================================\n\n");
        fsync(fd);
        close(fd);
    }

    _exit(1);
}

static bool CheckFlagFile(const char* flagName)
{
    char path[512];
    GetDiagPath(path, sizeof(path), flagName);
    return (access(path, F_OK) == 0);
}

void Diag_Init()
{
    if (s_diagInitialized) return;
    s_diagInitialized = true;

    // Check bisect switches
    s_noHookPipelines = CheckFlagFile("nohook_pipelines");
    s_noWeaponHooks   = CheckFlagFile("no_weapon_hooks");
    s_noRadarSync     = CheckFlagFile("no_radar_sync");
    s_noChatTouch     = CheckFlagFile("no_chat_touch");
    s_noChat          = CheckFlagFile("no_chat");
    s_noMoneySync     = CheckFlagFile("no_money_sync");
    s_noWeaponHud     = CheckFlagFile("no_weapon_hud");
    s_noPerfLogger    = CheckFlagFile("no_perf_logger");

    // Install alternate signal stack
    stack_t altStack;
    memset(&altStack, 0, sizeof(altStack));
    altStack.ss_sp = s_altStackBuffer;
    altStack.ss_size = sizeof(s_altStackBuffer);
    altStack.ss_flags = 0;
    sigaltstack(&altStack, nullptr);

    // Install signal handlers
    struct sigaction sa;
    memset(&sa, 0, sizeof(sa));
    sa.sa_sigaction = SignalCrashHandler;
    sa.sa_flags = SA_SIGINFO | SA_ONSTACK;

    sigaction(SIGSEGV, &sa, &s_oldSigActionSegv);
    sigaction(SIGABRT, &sa, &s_oldSigActionAbrt);
    sigaction(SIGBUS,  &sa, &s_oldSigActionBus);
    sigaction(SIGFPE,  &sa, &s_oldSigActionFpe);
    sigaction(SIGILL,  &sa, &s_oldSigActionIll);

    Diag_Breadcrumb("Diagnostic system initialized");
}

bool Diag_IsNoHookPipelines() { return s_noHookPipelines; }
bool Diag_IsNoWeaponHooks()   { return s_noWeaponHooks; }
bool Diag_IsNoRadarSync()     { return s_noRadarSync; }
bool Diag_IsNoChatTouch()     { return s_noChatTouch; }
bool Diag_IsNoChat()          { return s_noChat; }
bool Diag_IsNoMoneySync()     { return s_noMoneySync; }
bool Diag_IsNoWeaponHud()     { return s_noWeaponHud; }
bool Diag_IsNoPerfLogger()    { return s_noPerfLogger; }

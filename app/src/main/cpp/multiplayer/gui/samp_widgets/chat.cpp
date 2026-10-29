#include "chat.h"
#include "../../diag.h"
#include "../gui.h"
#include "../../main.h"
#include "../../game/game.h"
#include "../../net/netgame.h"
#include "../../gui/gui.h"
#include "../settings.h"
#include "java/jniutil.h"
#include "../../playertags.h"

#include <algorithm>
#include <cctype>
#include <cmath>
#include <cstdarg>

extern UI* pUI;
extern CGame* pGame;
extern CNetGame* pNetGame;
extern CSettings* pSettings;
extern CJavaWrapper* pJavaWrapper;

Chat* pChat = nullptr;
std::string Chat::sDevTagNick = "";

static uint64_t GetMonotonicTimeMs()
{
    struct timespec ts;
    clock_gettime(CLOCK_MONOTONIC, &ts);
    return (uint64_t)ts.tv_sec * 1000ULL + (uint64_t)(ts.tv_nsec / 1000000ULL);
}

void Chat::resetFadeTimer()
{
    uint64_t now = GetMonotonicTimeMs();
    m_lastActivityTimeMs = now;
    if (m_fadeState == FadeState::HIDDEN || m_fadeState == FadeState::FADING_OUT) {
        m_fadeState = FadeState::FADING_IN;
        m_fadeTransitionStartTimeMs = now;
    }
}

void Chat::updateFade(uint64_t now)
{
    bool inhibitFade = m_keyboardActive ||
                       (m_scrollPixels > 5.0f) ||
                       m_isInteracting ||
                       m_isDragging ||
                       m_hudEditorOpen ||
                       m_lines.empty();

    if (inhibitFade) {
        m_lastActivityTimeMs = now;
        m_fadeState = FadeState::VISIBLE;
        m_fadeAlpha = 1.0f;
        return;
    }

    if (m_fadeState == FadeState::VISIBLE) {
        if (now - m_lastActivityTimeMs >= CHAT_FADE_DELAY_MS) {
            m_fadeState = FadeState::FADING_OUT;
            m_fadeTransitionStartTimeMs = now;
        }
    }

    if (m_fadeState == FadeState::FADING_OUT) {
        uint64_t elapsed = now - m_fadeTransitionStartTimeMs;
        if (elapsed >= CHAT_FADE_OUT_MS) {
            m_fadeAlpha = 0.0f;
            m_fadeState = FadeState::HIDDEN;
        } else {
            m_fadeAlpha = 1.0f - ((float)elapsed / (float)CHAT_FADE_OUT_MS);
        }
    } else if (m_fadeState == FadeState::FADING_IN) {
        uint64_t elapsed = now - m_fadeTransitionStartTimeMs;
        if (elapsed >= CHAT_FADE_IN_MS) {
            m_fadeAlpha = 1.0f;
            m_fadeState = FadeState::VISIBLE;
            m_lastActivityTimeMs = now;
        } else {
            m_fadeAlpha = (float)elapsed / (float)CHAT_FADE_IN_MS;
        }
    } else if (m_fadeState == FadeState::HIDDEN) {
        m_fadeAlpha = 0.0f;
    }
}

Chat::Chat()
    : ListBox(),
      m_showingChat(true),
      m_keyboardActive(false),
      m_isInteracting(false),
      m_isDragging(false),
      m_touchStartPos(0.0f, 0.0f),
      m_touchLastPos(0.0f, 0.0f),
      m_scrollPixels(0.0f),
      m_pillMin(0.0f, 0.0f),
      m_pillMax(0.0f, 0.0f),
      m_pillVisible(false),
      m_itemSize(0.0f, 0.0f),
      m_fadeState(FadeState::VISIBLE),
      m_lastActivityTimeMs(GetMonotonicTimeMs()),
      m_fadeTransitionStartTimeMs(0),
      m_fadeAlpha(1.0f),
      m_wasFadedOnTouch(false),
      m_hudEditorOpen(false)
{
    pChat = this;
}

void Chat::appendWrappedMessage(const std::string& message, const ImColor& defaultColor)
{
    if (message.empty()) return;

    static bool s_firstChatLogged = false;
    if (!s_firstChatLogged) {
        s_firstChatLogged = true;
        Diag_Breadcrumb("First chat message added (Chat::appendWrappedMessage)");
    }

    std::lock_guard<std::mutex> lock(m_mutex);

    float maxWidth = this->width() - 16.0f;
    if (maxWidth < 120.0f) maxWidth = 400.0f;

    float fontSize = UISettings::chatFontSize();
    if (fontSize < 14.0f) fontSize = 14.0f;

    auto isSpace = [](char c) {
        return c == ' ' || c == '\t' || c == '\r' || c == '\n';
    };

    auto getColorTagAt = [](const std::string& s, size_t i) -> std::string {
        if (i + 7 < s.size() && s[i] == '{' && s[i + 7] == '}') {
            bool validHex = true;
            for (size_t k = 1; k <= 6; ++k) {
                if (!isxdigit((unsigned char)s[i + k])) {
                    validHex = false;
                    break;
                }
            }
            if (validHex) return s.substr(i, 8);
        }
        return "";
    };

    auto measureWidth = [&](const std::string& s) -> float {
        if (s.empty()) return 0.0f;
        if (pUI && pUI->renderer()) {
            return pUI->renderer()->calculateTextSize(s, fontSize).x;
        }
        float visibleChars = 0.0f;
        for (size_t k = 0; k < s.size(); ) {
            std::string tag = getColorTagAt(s, k);
            if (!tag.empty()) {
                k += 8;
            } else {
                if (((unsigned char)s[k] & 0xC0) != 0x80) visibleChars += 1.0f;
                k++;
            }
        }
        return visibleChars * (fontSize * 0.55f);
    };

    std::vector<std::string> wrappedLines;
    std::string currentLine;
    std::string activeColorTag = "";

    size_t i = 0;
    const size_t len = message.size();

    while (i < len)
    {
        char c = message[i];

        if (c == '\r') {
            i++;
            continue;
        }

        if (c == '\n') {
            wrappedLines.push_back(currentLine.empty() ? " " : currentLine);
            currentLine = activeColorTag;
            i++;
            continue;
        }

        std::string tag = getColorTagAt(message, i);
        if (!tag.empty()) {
            activeColorTag = tag;
            currentLine += tag;
            i += 8;
            continue;
        }

        if (c == ' ' || c == '\t') {
            if (!currentLine.empty()) {
                currentLine += c;
            }
            i++;
            continue;
        }

        std::string word;
        std::string wordLastColor = activeColorTag;

        while (i < len && !isSpace(message[i])) {
            std::string innerTag = getColorTagAt(message, i);
            if (!innerTag.empty()) {
                word += innerTag;
                wordLastColor = innerTag;
                i += 8;
            } else {
                word += message[i];
                i++;
            }
        }

        std::string testLine = currentLine + word;
        float testW = measureWidth(testLine);

        if (testW <= maxWidth || currentLine.empty() || currentLine == activeColorTag) {
            currentLine += word;
            activeColorTag = wordLastColor;
        } else {
            wrappedLines.push_back(currentLine);
            currentLine = activeColorTag + word;
            activeColorTag = wordLastColor;
        }
    }

    if (!currentLine.empty() && currentLine != activeColorTag) {
        wrappedLines.push_back(currentLine);
    } else if (wrappedLines.empty() && !currentLine.empty()) {
        wrappedLines.push_back(currentLine);
    }

    float lineHeight = fontSize * 1.35f;
    if (lineHeight < 16.0f) lineHeight = 16.0f;
    int newLinesCount = (int)wrappedLines.size();

    for (const auto& lineText : wrappedLines) {
        m_lines.push_back({lineText, defaultColor});
        if (m_lines.size() > MAX_CHAT_HISTORY) {
            m_lines.pop_front();
        }
    }

    // Auto-follow vs scroll lock
    if (m_scrollPixels > 5.0f) {
        m_scrollPixels += (float)newLinesCount * lineHeight;
    } else {
        m_scrollPixels = 0.0f;
    }
    resetFadeTimer();
}

void Chat::addChatMessage(const std::string& message, const std::string& nick, const ImColor& nick_color)
{
    if (!m_showingChat) return;

    char hexCol[12];
    snprintf(hexCol, sizeof(hexCol), "{%02X%02X%02X}",
        (int)(nick_color.Value.x * 255.0f + 0.5f),
        (int)(nick_color.Value.y * 255.0f + 0.5f),
        (int)(nick_color.Value.z * 255.0f + 0.5f));

    std::string fullMsg = std::string(hexCol) + nick + ": {FFFFFF}" + message;
    appendWrappedMessage(fullMsg, ImColor(1.0f, 1.0f, 1.0f));
}

void Chat::addClientMessage(const std::string& message, const ImColor& color)
{
    if (!m_showingChat) return;
    appendWrappedMessage(message, color);
}

void Chat::addInfoMessage(const std::string& format, ...)
{
    char tmp_buf[512];
    va_list args;
    va_start(args, format);
    vsnprintf(tmp_buf, sizeof(tmp_buf), format.c_str(), args);
    va_end(args);

    if (!m_showingChat) return;
    appendWrappedMessage(std::string(tmp_buf), ImColor(0x00, 0xc8, 0xc8));
}

void Chat::addDebugMessage(const std::string& format, ...)
{
    char tmp_buf[512];
    va_list args;
    va_start(args, format);
    vsnprintf(tmp_buf, sizeof(tmp_buf), format.c_str(), args);
    va_end(args);

    if (!m_showingChat) return;
    appendWrappedMessage(std::string(tmp_buf), ImColor(0xbe, 0xbe, 0xbe));
}

void Chat::touchEvent(const ImVec2& pos, TouchType type)
{
    std::lock_guard<std::mutex> lock(m_mutex);
    if (!m_showingChat || !visible()) {
        m_isInteracting = false;
        m_isDragging = false;
        m_wasFadedOnTouch = false;
        return;
    }

    if (type == TouchType::push)
    {
        bool bInPill = m_pillVisible &&
                       pos.x >= (m_pillMin.x - 20.0f) && pos.x <= (m_pillMax.x + 20.0f) &&
                       pos.y >= (m_pillMin.y - 20.0f) && pos.y <= (m_pillMax.y + 20.0f);

        if (contains(pos) || bInPill) {
            m_isInteracting = true;
            m_isDragging = false;
            m_touchStartPos = pos;
            m_touchLastPos = pos;

            resetFadeTimer();
            m_fadeAlpha = 1.0f;
            m_fadeState = FadeState::VISIBLE;
        }
    }
    else if (type == TouchType::move)
    {
        if (m_isInteracting) {
            float moveDistY = std::abs(pos.y - m_touchStartPos.y);
            if (moveDistY > 25.0f) {
                m_isDragging = true;
            }

            if (m_isDragging) {
                float deltaY = pos.y - m_touchLastPos.y;
                m_scrollPixels += deltaY;

                float fontSize = UISettings::chatFontSize();
                float lineHeight = fontSize * 1.35f;
                if (lineHeight < 16.0f) lineHeight = 16.0f;
                int visibleCount = (int)(height() / lineHeight);
                float maxScroll = std::max(0.0f, (float)((int)m_lines.size() - visibleCount) * lineHeight);

                if (m_scrollPixels > maxScroll) m_scrollPixels = maxScroll;
                if (m_scrollPixels < 0.0f) m_scrollPixels = 0.0f;
            }
            m_touchLastPos = pos;
        }
    }
    else if (type == TouchType::pop)
    {
        if (m_isInteracting) {
            if (!m_isDragging) {
                // If tapped on pill or anywhere on chat (not dragged)
                if (m_pillVisible &&
                    pos.x >= (m_pillMin.x - 20.0f) && pos.x <= (m_pillMax.x + 20.0f) &&
                    pos.y >= (m_pillMin.y - 20.0f) && pos.y <= (m_pillMax.y + 20.0f))
                {
                    m_scrollPixels = 0.0f;
                }

                if (pUI && (!pUI->playertablist() || !pUI->playertablist()->visible())) {
                    if (pUI->keyboard()) {
                        pUI->keyboard()->show(this);
                        m_keyboardActive = true;
                    }
                }
            }
            m_isInteracting = false;
            m_isDragging = false;
            m_wasFadedOnTouch = false;
        }
    }
}

void Chat::keyboardEvent(const std::string& input)
{
    if (input.empty() || !pNetGame) return;

    if (input[0] == '/')
    {
        if (!commandClient(input))
            pNetGame->SendChatCommand(input.c_str());
    }
    else
        pNetGame->SendChatMessage(input.c_str());

    if (pUI && pUI->keyboard())
        pUI->keyboard()->hide();

    m_keyboardActive = false;
}

bool Chat::commandClient(const std::string& command)
{
    if (command == "/tab")
    {
        PlayerTabList* tab = pUI ? pUI->playertablist() : nullptr;
        if (pNetGame && tab) {
            pNetGame->UpdatePlayerScoresAndPings();
            tab->show();
        }
        return true;
    }

    if (command == "/hidechat")
    {
        if (pChat) pChat->toggleVisibility();
        return true;
    }

    if (command == "/fpsinfo")
    {
        if (pUI) {
            pUI->ToggleFPSInfo();
        }
        return true;
    }

    if (command == "/hudeditor")
    {
        if (pJavaWrapper) {
            pJavaWrapper->ShowHudEditor();
        }
        return true;
    }

    return false;
}

void Chat::draw(ImGuiRenderer* renderer)
{
    if (Diag_IsNoChat()) return;
    if (!renderer) return;

    std::lock_guard<std::mutex> lock(m_mutex);
    if (!m_showingChat || !visible()) return;
    if (m_lines.empty()) return;

    uint64_t now = GetMonotonicTimeMs();
    updateFade(now);

    if (m_fadeAlpha <= 0.005f) {
        return; // Fully hidden
    }

    float oldAlpha = ImGui::GetStyle().Alpha;
    ImGui::GetStyle().Alpha = oldAlpha * m_fadeAlpha;

    ImVec2 pos = absolutePosition();
    ImVec2 sz = size();
    float fontSize = UISettings::chatFontSize();
    float lineHeight = fontSize * 1.35f;
    if (lineHeight < 16.0f) lineHeight = 16.0f;

    int totalLines = (int)m_lines.size();
    int visibleCount = (int)(sz.y / lineHeight);
    if (visibleCount < 1) visibleCount = 1;

    float maxScroll = std::max(0.0f, (float)(totalLines - visibleCount) * lineHeight);
    if (m_scrollPixels > maxScroll) m_scrollPixels = maxScroll;
    if (m_scrollPixels < 0.0f) m_scrollPixels = 0.0f;

    // Viewport scissor clipping
    renderer->pushClipRect(pos, ImVec2(pos.x + sz.x, pos.y + sz.y), true);

    float yBottom = pos.y + sz.y;
    for (int i = 0; i < totalLines; ++i) {
        float lineY = yBottom - (float)(totalLines - i) * lineHeight + m_scrollPixels;
        if (lineY + lineHeight < pos.y || lineY > pos.y + sz.y) {
            continue;
        }
        ImVec2 linePos(pos.x + 4.0f, lineY);
        renderer->drawText(linePos, m_lines[i].baseColor, m_lines[i].text, true, fontSize);
    }

    renderer->popClipRect();

    // Show jump-to-bottom indicator if scrolled up
    if (m_scrollPixels > 10.0f) {
        m_pillVisible = true;
        float pillW = 150.0f;
        float pillH = 28.0f;
        m_pillMin = ImVec2(pos.x + sz.x - pillW - 8.0f, pos.y + sz.y - pillH - 4.0f);
        m_pillMax = ImVec2(m_pillMin.x + pillW, m_pillMin.y + pillH);

        // Dark pill with gold border
        renderer->drawRoundedRect(m_pillMin, m_pillMax, ImColor(18, 16, 26, 220), pillH * 0.5f, true);
        renderer->drawRoundedRect(m_pillMin, m_pillMax, ImColor(218, 165, 32, 230), pillH * 0.5f, false, 1.5f);

        const char* pillText = "▼ جدیدترین پیام‌ها";
        float pillFontSize = 13.0f;
        ImVec2 textSz = renderer->calculateTextSize(pillText, pillFontSize);
        ImVec2 textPos(m_pillMin.x + (pillW - textSz.x) * 0.5f, m_pillMin.y + (pillH - textSz.y) * 0.5f);
        renderer->drawText(textPos, ImColor(255, 255, 255, 240), pillText, true, pillFontSize);
    } else {
        m_pillVisible = false;
    }

    ImGui::GetStyle().Alpha = oldAlpha;
}

extern "C"
JNIEXPORT void JNICALL
Java_com_glory_game_ui_HudEditorDialog_setChatEditorOpen(JNIEnv* env, jclass clazz, jboolean open)
{
    if (pChat) {
        pChat->setHudEditorOpen(open);
    }
}

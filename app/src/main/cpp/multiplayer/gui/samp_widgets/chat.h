#pragma once

#include <deque>
#include <string>
#include <vector>
#include <mutex>
#include "../gui.h"

class Chat : public ListBox
{
public:
    Chat();

    void addChatMessage(const std::string& message, const std::string& nick, const ImColor& nick_color);
    void addInfoMessage(const std::string& format, ...);
    void addDebugMessage(const std::string& format, ...);
    void addClientMessage(const std::string& message, const ImColor& color);

    virtual void draw(ImGuiRenderer* renderer) override;

    virtual void touchEvent(const ImVec2& pos, TouchType type) override;
    virtual void keyboardEvent(const std::string& input) override;

    [[maybe_unused]] static bool commandClient(const std::string& command);

    static std::string sDevTagNick;

    void toggleVisibility() { m_showingChat = !m_showingChat; }
    bool isVisible() const { return m_showingChat && const_cast<Chat*>(this)->visible(); }

    bool isKeyboardActive() const { return m_keyboardActive; }
    void setKeyboardActive(bool active) { m_keyboardActive = active; }

    bool isInteracting() const { return m_isInteracting; }
    bool isDragging() const { return m_isDragging; }

    static constexpr uint64_t CHAT_FADE_DELAY_MS = 10000;
    static constexpr uint64_t CHAT_FADE_OUT_MS = 400;
    static constexpr uint64_t CHAT_FADE_IN_MS = 150;

    enum class FadeState
    {
        VISIBLE,
        FADING_OUT,
        FADING_IN,
        HIDDEN
    };

    void resetFadeTimer();
    void setHudEditorOpen(bool open) { m_hudEditorOpen = open; }
    bool isHudEditorOpen() const { return m_hudEditorOpen; }

    void scrollToBottom() {
        std::lock_guard<std::mutex> lock(m_mutex);
        m_scrollPixels = 0.0f;
    }

    void setItemSize(const ImVec2& size) { m_itemSize = size; }
    const ImVec2& itemSize() const { return m_itemSize; }

private:
    struct ChatLine
    {
        std::string text;
        ImColor baseColor;
    };

    void appendWrappedMessage(const std::string& message, const ImColor& defaultColor);

    static const size_t MAX_CHAT_HISTORY = 250;
    mutable std::mutex m_mutex;
    std::deque<ChatLine> m_lines;

    bool m_showingChat = true;
    bool m_keyboardActive = false;
    bool m_isInteracting = false;
    bool m_isDragging = false;

    ImVec2 m_touchStartPos = ImVec2(0.0f, 0.0f);
    ImVec2 m_touchLastPos = ImVec2(0.0f, 0.0f);

    float m_scrollPixels = 0.0f;
    ImVec2 m_pillMin = ImVec2(0.0f, 0.0f);
    ImVec2 m_pillMax = ImVec2(0.0f, 0.0f);
    bool m_pillVisible = false;

    ImVec2 m_itemSize = ImVec2(0.0f, 0.0f);

    FadeState m_fadeState = FadeState::VISIBLE;
    uint64_t m_lastActivityTimeMs = 0;
    uint64_t m_fadeTransitionStartTimeMs = 0;
    float m_fadeAlpha = 1.0f;
    bool m_wasFadedOnTouch = false;
    bool m_hudEditorOpen = false;

    void updateFade(uint64_t now);
};
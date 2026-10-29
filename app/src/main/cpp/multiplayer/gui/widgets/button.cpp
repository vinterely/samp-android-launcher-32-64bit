#include "../../main.h"
#include "..//gui.h"
#include "button.h"

extern bool OpenButton;

Button::Button(const std::string& caption, float font_size)
{
    m_callback = nullptr;

    m_label = new Label(caption, ImColor(1.0f, 1.0f, 1.0f), false, font_size);
    this->addChild(m_label);

    m_color = UISettings::buttonColor();
    m_colorFocused = UISettings::buttonFocusedColor();
}

void Button::performLayout()
{
    float padding = UISettings::padding();

    m_label->performLayout();
    ImVec2 desired = m_label->size() + ImVec2(padding * 2, padding / 2 * 2);
    // Preserve larger preset size (so label stays centered inside stretched buttons)
    this->setSize(ImVec2(ImMax(width(), desired.x), ImMax(height(), desired.y)));

    m_label->setPosition((size() - m_label->size()) / 2);
}

void Button::draw(ImGuiRenderer* renderer)
{
    // rounded background (no outline)
    renderer->drawRoundedRect(absolutePosition(), absolutePosition() + size(),
                              focused() ? m_colorFocused : m_color, 10.0f, true);

    Widget::draw(renderer);
}

void Button::touchPopEvent()
{
    if (m_callback) m_callback();
}


//============== Custom Button=========================//
CButton::CButton(const std::string& caption, float font_size)
{
    m_callback = nullptr;

    m_label = new Label(caption, ImColor(1.0f, 1.0f, 1.0f), false, font_size);
    this->addChild(m_label);

    m_color = UISettings::buttonColor();
    m_colorFocused = UISettings::buttonFocusedColor();
}

void CButton::performLayout()
{
    float padding = UISettings::padding();

    m_label->performLayout();
    this->setSize(m_label->size() + ImVec2(padding * 2, padding / 2 * 2));

    m_label->setPosition((size() - m_label->size()) / 2);
}

void CButton::draw(ImGuiRenderer* renderer)
{
    if (!m_alwaysVisible && OpenButton == false) return;

    // rounded background
    renderer->drawRoundedRect(absolutePosition(), absolutePosition() + size(),
                              focused() ? m_colorFocused : m_color, 10.0f, true);

    Widget::draw(renderer);
}

void CButton::touchPopEvent()
{
    if (m_callback) m_callback();
}
//=======================================//

//======== >> Button ====================//
OButton::OButton(const std::string& caption, float font_size)
{
    m_callback = nullptr;

    m_label = new Label(caption, ImColor(1.0f, 1.0f, 1.0f), false, font_size);
    this->addChild(m_label);

    m_color = UISettings::buttonColor();
    m_colorFocused = UISettings::buttonFocusedColor();
}

void OButton::performLayout()
{
    float padding = UISettings::padding();

    m_label->performLayout();
    this->setSize(m_label->size() + ImVec2(padding * 2, padding / 2 * 2));

    m_label->setPosition((size() - m_label->size()) / 2);
    //m_label->setPosition((m_label->size()) / 2);

}

void OButton::draw(ImGuiRenderer* renderer)
{
    renderer->drawRoundedRect(absolutePosition(), absolutePosition() + size(),
                              focused() ? m_colorFocused : m_color, 10.0f, true);

    Widget::draw(renderer);

    // keep layout-driven positioning
}

void OButton::touchPopEvent()
{
    if (m_callback) m_callback();
}
//======================================//
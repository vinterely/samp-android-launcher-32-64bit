#define IMGUI_DEFINE_MATH_OPERATORS
#include "../vendor/imgui/imgui.h"
#include "../vendor/imgui/imgui_internal.h"

#include <string>
#include <algorithm>
#include "uisettings.h"
#include "../main.h"
#include "../settings.h"
#include "samp_widgets/chat.h"

extern CSettings* pSettings;
extern Chat* pChat;

/* scaling */
ImVec2 UISettings::m_baseSize = ImVec2(640.0f, 480.0f);
ImVec2 UISettings::m_scaleFactor = ImVec2(1.0f, 1.0f);
ImVec2 UISettings::m_displaySize = ImVec2(1920.0f, 1080.0f);

/* font */
float UISettings::m_fontSize = 28.0f;//26.0f;//24.0f;
float UISettings::m_outlineSize = 2.0f;

/* ////////////////// pos & size ////////////////// */

/* widgets */
float UISettings::m_padding = 15.0f;

/* keyboard */
ImVec2 UISettings::m_keyboardPos = ImVec2(0.0f, 213.0f/*180.0f*/);
ImVec2 UISettings::m_keyboardSize = ImVec2(640.0f, 300.0f);//ImVec2(640.0f, 300.0f);
float UISettings::m_keyboardRowHeight = 52.0f;//52.0f;

/* splashscreen */
ImVec2 UISettings::m_splashscreenLogoPos;
ImVec2 UISettings::m_splashscreenLogoSize = ImVec2(640.0f, 480.0f);
ImVec2 UISettings::m_splashScreenPBarPos = ImVec2(100.0f, 460.0f);//ImVec2(100.0f, 380.0f);
ImVec2 UISettings::m_splashScreenPBarSize = ImVec2(440.0f, 10.0f);

/* chat */
int UISettings::m_chatDispMesssages = 8;
int UISettings::m_chatMaxMessages = 250;
ImVec2 UISettings::m_chatPos = ImVec2(15.0f, 15.0f);
ImVec2 UISettings::m_chatSize = ImVec2(550.0f, 0.0f);
ImVec2 UISettings::m_chatItemSize = ImVec2(400.0f, 26.0f);
float UISettings::m_chatFontSize = 20.0f;
ImVec2 UISettings::m_chatPosNorm = ImVec2(0.015f, 0.025f);
float UISettings::m_chatWidthNorm = 0.45f;

/* spawn */
ImVec2 UISettings::m_spawnPos = ImVec2(200.0f, 420.0f);
ImVec2 UISettings::m_spawnSize = ImVec2(240.0f, 50.0f);

/* nametag */
ImVec2 UISettings::m_nametagBarSize = ImVec2(25.0f, 6.0f);

/* dialog */
ImVec2 UISettings::m_dialogButtonPanelSize = ImVec2(150.0f, 45.0f);
ImVec2 UISettings::m_dialogMinSize = ImVec2(150.0f, 120.0f);
ImVec2 UISettings::m_dialogMaxSize = ImVec2(620.0f, 400.0f);
float UISettings::m_dialogTitleHeight = 20.0f;
float UISettings::m_dialogListItemHeight = 30.0f;

/* buttonpanel */
ImVec2 UISettings::m_buttonPanelPos = ImVec2(0.0f, 0.0f/*150.0f*/);
ImVec2 UISettings::m_buttonPanelSize = ImVec2(0.0f/*250.0f*//*180.0f*//*250.0f*/, 0.0f); //ImVec2(120.0f, 45.0f);

/* voice button */
ImVec2 UISettings::m_buttonVoicePos = ImVec2(500.0f, 180.0f/*170.0f*/);
ImVec2 UISettings::m_buttonVoiceSize = ImVec2(55.0f, 80.0f);

/* ////////////////// colors ////////////////// */

/* button */
ImColor UISettings::m_buttonColor = ImColor(1.0f, 1.0f, 1.0f, 0.18f);
ImColor UISettings::m_buttonFocusedColor = ImColor(1.0f, 1.0f, 1.0f, 0.30f);

/* keyboard */
ImColor UISettings::m_keyboardBackgroundColor = ImColor(0, 0, 0, 150);

/* dialog */
ImColor UISettings::m_dialogBackgroundColor = ImColor(0, 0, 0, 200);
ImColor UISettings::m_dialogTitleBackgroundColor = ImColor(0, 0, 0, 200);/*ImColor(0xF5, 0x91, 0x32);*/// ImColor(50, 50, 50, 255);

void UISettings::Initialize(const ImVec2& display_size)
{
	if(!pSettings) return;
	
	m_displaySize = display_size;
	m_scaleFactor = display_size / m_baseSize;

	/* font */
	m_fontSize *= m_scaleFactor.y;

	/* keyboard */
	m_keyboardSize = m_keyboardSize * m_scaleFactor;
	m_keyboardPos = m_keyboardPos * m_scaleFactor;
	m_keyboardRowHeight *= m_scaleFactor.y;

	/* splashscreen */
	m_splashscreenLogoSize = m_splashscreenLogoSize * m_scaleFactor;
	m_splashscreenLogoPos = ImVec2(0.0f, 0.0f);
	m_splashScreenPBarPos = m_splashScreenPBarPos * m_scaleFactor;
	m_splashScreenPBarSize = m_splashScreenPBarSize * m_scaleFactor;

	/* chat */
	float chatX = 0.015f;
	float chatY = 0.025f;
	float chatW = 0.45f;
	float chatFont = 20.0f;

	if (pSettings) {
		if (pSettings->Get().fChatPosX > 1.0f && display_size.x > 0.0f) {
			chatX = pSettings->Get().fChatPosX / display_size.x;
		} else if (pSettings->Get().fChatPosX > 0.0f) {
			chatX = pSettings->Get().fChatPosX;
		}

		if (pSettings->Get().fChatPosY > 1.0f && display_size.y > 0.0f) {
			chatY = pSettings->Get().fChatPosY / display_size.y;
		} else if (pSettings->Get().fChatPosY > 0.0f) {
			chatY = pSettings->Get().fChatPosY;
		}

		if (pSettings->Get().fChatSizeX > 1.0f && display_size.x > 0.0f) {
			chatW = pSettings->Get().fChatSizeX / display_size.x;
		} else if (pSettings->Get().fChatSizeX > 0.0f) {
			chatW = pSettings->Get().fChatSizeX;
		}

		if (pSettings->Get().fFontSize > 0.0f) {
			chatFont = pSettings->Get().fFontSize;
		}
	}

	if (chatW > 1.0f) chatW = chatW / 100.0f;
	if (chatW < 0.25f || chatW > 0.85f) chatW = 0.50f;
	if (chatFont < 12.0f || chatFont > 36.0f) chatFont = 20.0f;

	UpdateChatConfig(chatX, chatY, chatW, 0, chatFont);

	/* spawn */
	m_spawnPos = m_spawnPos * m_scaleFactor;
	m_spawnSize = m_spawnSize * m_scaleFactor;

	/* nametag */
	m_nametagBarSize = m_nametagBarSize * m_scaleFactor;

	/* dialog */
	m_dialogButtonPanelSize = m_dialogButtonPanelSize * m_scaleFactor;
	m_dialogMinSize = m_dialogMinSize * m_scaleFactor;
	m_dialogMaxSize = m_dialogMaxSize * m_scaleFactor;
	m_dialogTitleHeight *= m_scaleFactor.y;
	m_dialogListItemHeight *= m_scaleFactor.y;

	/* buttonpanel */
	m_buttonPanelPos = m_buttonPanelPos * m_scaleFactor;
	m_buttonPanelSize = m_buttonPanelSize * m_scaleFactor;

	/* button voice */
	m_buttonVoicePos = m_buttonVoicePos * m_scaleFactor;
	m_buttonVoiceSize = m_buttonVoiceSize * m_scaleFactor;
}

void UISettings::UpdateChatConfig(float xPercent, float yPercent, float widthPercent, int lines, float fontSize)
{
	if (xPercent < 0.0f || xPercent > 0.85f) {
		xPercent = 0.015f;
	}
	if (yPercent < 0.0f || yPercent > 0.85f) {
		yPercent = 0.025f;
	}

	if (widthPercent > 1.0f) widthPercent = widthPercent / 100.0f;
	if (widthPercent < 0.25f || widthPercent > 0.85f) widthPercent = 0.50f;
	if (fontSize < 12.0f || fontSize > 36.0f) fontSize = 20.0f;

	m_chatPosNorm = ImVec2(xPercent, yPercent);
	m_chatWidthNorm = widthPercent;
	m_chatFontSize = fontSize;

	if (pSettings) {
		pSettings->Get().fChatPosX = xPercent;
		pSettings->Get().fChatPosY = yPercent;
		pSettings->Get().fChatSizeX = widthPercent;
		pSettings->Get().fFontSize = fontSize;
		pSettings->Get().iChatMaxMessages = 250;
	}

	ImVec2 display_size = m_displaySize;
	if (display_size.x <= 0.0f || display_size.y <= 0.0f) {
		if (RsGlobal && RsGlobal->maximumWidth > 0 && RsGlobal->maximumHeight > 0) {
			display_size = ImVec2((float)RsGlobal->maximumWidth, (float)RsGlobal->maximumHeight);
		} else {
			display_size = ImVec2(1920.0f, 1080.0f);
		}
	}

	m_chatPos = ImVec2(display_size.x * m_chatPosNorm.x, display_size.y * m_chatPosNorm.y);
	m_chatSize.x = display_size.x * m_chatWidthNorm;
	if (m_chatSize.x < 360.0f) m_chatSize.x = 360.0f;

	float lineHeight = m_chatFontSize * 1.35f;
	if (lineHeight < 16.0f) lineHeight = 16.0f;

	// Target height ~35% of display height; derive integer visible lines so lines are never clipped awkwardly
	float targetHeight = display_size.y * 0.35f;
	int visibleLines = (int)(targetHeight / lineHeight);
	if (visibleLines < 5) visibleLines = 5;

	m_chatDispMesssages = visibleLines;
	m_chatSize.y = (float)visibleLines * lineHeight;
	m_chatItemSize = ImVec2(m_chatSize.x, lineHeight);

	if (pChat && ImGui::GetCurrentContext() != nullptr) {
		pChat->setPosition(m_chatPos);
		pChat->setFixedSize(m_chatSize);
		pChat->setItemSize(m_chatItemSize);
		pChat->performLayout();
	}
}
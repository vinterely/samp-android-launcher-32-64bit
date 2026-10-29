#pragma once

#include <string>

#include "Color.h"
#include "ColouredText.h"

#include "stb_truetype.h"

#define __IGNORE_VALUE__ UINT_MAX

class CTextRasterizer {
public:
	enum eTextAlignment : uint8_t {
		TextAlignmentLeft,
		TextAlignmentRight,
		TextAlignmentCenter
	};

private:
	bool m_bFontInitialised = false;

	stbtt_fontinfo m_stbFont;

	uint8_t* m_pFontBuffer = nullptr;
	long m_lFontBufferSize = 0;
	float m_fFontSize = 0.0f;
	int m_iLineAscent = 0;

	uint32_t m_uiPositionX = 0;
	uint32_t m_uiCurrentX = 0;
	uint32_t m_uiCurrentY = 0;
	uint8_t m_ucTextAlignment = 0;

	uint32_t m_uiBitmapWidth = 0;
	uint32_t m_uiBitmapHeight = 0;
	uint32_t m_uiBitmapSize = 0;
	uint32_t* m_pBitmap = nullptr;

public:
	CTextRasterizer(uint32_t, uint32_t);
	~CTextRasterizer();

	uint8_t* GetBitmap();
	void FreeBitmap();

	void SetupFont(std::string const&, float);
	void SetFontSize(float);

	uint8_t* GetFont();
	void FreeFont();

	void SetPosition(uint32_t = __IGNORE_VALUE__, uint32_t = __IGNORE_VALUE__);
	void SetTextAlignment(uint8_t);
	void DrawText_(CColouredTextMultiLine&);

	void FillBitmapWithColor(CColor&);
	void DrawBitmap(const uint8_t*, uint32_t, uint32_t, uint32_t, uint32_t, uint32_t, uint32_t, uint32_t, uint32_t, CColor&);

	bool IsFontReady() {
		return m_bFontInitialised;
	};

private:
	uint32_t GetTextDrawLength(const std::string&);
	uint32_t GetTextDrawHeight(uint32_t);
	uint32_t GetGlyphDrawLength(char32_t, char32_t);
	void DrawTextLine(const std::string&, CColor&, bool);
};

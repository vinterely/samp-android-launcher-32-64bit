#include "TextRasterizer.h"

#define STB_TRUETYPE_IMPLEMENTATION
#include "stb_truetype.h"

#include "TextCharset.h"

CTextRasterizer::CTextRasterizer(uint32_t uiWidth, uint32_t uiHeight) {
	m_uiBitmapWidth = uiWidth;
	m_uiBitmapHeight = uiHeight;
	m_uiBitmapSize = m_uiBitmapWidth * m_uiBitmapHeight;
	m_pBitmap = (uint32_t*)calloc(m_uiBitmapSize * sizeof(uint32_t), sizeof(uint8_t));
	memset(&m_stbFont, 0, sizeof(stbtt_fontinfo));
}

CTextRasterizer::~CTextRasterizer() {
	FreeFont();
	FreeBitmap();

	m_bFontInitialised = false;
}

uint8_t* CTextRasterizer::GetBitmap() {
	return (uint8_t*)m_pBitmap;
}

void CTextRasterizer::FreeBitmap() {
	if (m_pBitmap) {
		free(m_pBitmap);
	}
	m_pBitmap = nullptr;
}


void CTextRasterizer::SetupFont(std::string const& sFontPath, float fFontSize) {
	FILE* pFile = fopen(sFontPath.c_str(), "rb");
	if (!pFile) {
		return;
	}

	fseek(pFile, 0, SEEK_END);
	m_lFontBufferSize = ftell(pFile);
	rewind(pFile);

	m_pFontBuffer = (uint8_t*)malloc(m_lFontBufferSize);
	if (m_pFontBuffer) {
		fread(m_pFontBuffer, m_lFontBufferSize, 1, pFile);
	}

	fclose(pFile);

	if (!m_pFontBuffer) {
		return;
	}

	int iFontOffset = stbtt_GetFontOffsetForIndex(m_pFontBuffer, 0);
	stbtt_InitFont(&m_stbFont, m_pFontBuffer, iFontOffset);
	SetFontSize(fFontSize);
	SetPosition(0, 0);

	m_bFontInitialised = true;
}

void CTextRasterizer::SetFontSize(float fSize) {
	m_fFontSize = stbtt_ScaleForPixelHeight(&m_stbFont, fSize);
	stbtt_GetFontVMetrics(&m_stbFont, &m_iLineAscent, 0, 0);
}

uint8_t* CTextRasterizer::GetFont() {
	return (uint8_t*)m_pFontBuffer;
}

void CTextRasterizer::FreeFont() {
	if (m_pFontBuffer) {
		free(m_pFontBuffer);
	}
	m_pFontBuffer = nullptr;
}

void CTextRasterizer::SetPosition(uint32_t uiPositionX, uint32_t uiPositionY) {
	if (uiPositionX != __IGNORE_VALUE__) {
		m_uiCurrentX = m_uiPositionX = uiPositionX;
	}

	if (uiPositionY != __IGNORE_VALUE__) {
		m_uiCurrentY = uiPositionY;
	}
}

void CTextRasterizer::SetTextAlignment(uint8_t ucTextAlignment) {
	m_ucTextAlignment = ucTextAlignment;
}

void CTextRasterizer::DrawText_(CColouredTextMultiLine& colouredText) {
	switch (m_ucTextAlignment) {
	case TextAlignmentLeft: {
		SetPosition(__IGNORE_VALUE__, 0);
		break;
	}
	case TextAlignmentCenter: {
		SetPosition(__IGNORE_VALUE__, m_uiBitmapHeight / 2 - GetTextDrawHeight(colouredText.GetLinesCount()) / 2);
		break;
	}
	case TextAlignmentRight: {
		SetPosition(__IGNORE_VALUE__, 0);
		break;
	}
	}

	colouredText.Foreach(
		[&](size_t uiCurrentLineId, CColouredTextLine& ColouredTextLine) -> void {
			switch (m_ucTextAlignment) {
			case TextAlignmentLeft: {
				SetPosition(0, __IGNORE_VALUE__);
				break;
			}
			case TextAlignmentCenter: {
				SetPosition(m_uiBitmapWidth / 2 - GetTextDrawLength(ColouredTextLine.GetPlainText()) / 2, __IGNORE_VALUE__);
				break;
			}
			case TextAlignmentRight: {
				SetPosition(m_uiBitmapWidth - GetTextDrawLength(ColouredTextLine.GetPlainText()), __IGNORE_VALUE__);
				break;
			}
			}

			ColouredTextLine.Foreach(
				[&](size_t uiCurrentSectionId, CColouredTextSection& ColouredTextSection) -> void {
					DrawTextLine(ColouredTextSection.GetText(), ColouredTextSection.GetTextColour(), (uiCurrentLineId != 0 && uiCurrentSectionId == 0) ? true : false);
				}
			);
		}
	);
}

void CTextRasterizer::FillBitmapWithColor(CColor& colValue) {
	if (!m_pBitmap) {
		return;
	}

	for (uint32_t iCurLine = 0; iCurLine < m_uiBitmapHeight; iCurLine++) {
		for (uint32_t iCurRow = 0; iCurRow < m_uiBitmapWidth; iCurRow++) {
			m_pBitmap[iCurLine * m_uiBitmapWidth + iCurRow] = colValue.Get(CColor::eColorEndianness::COLOR_ENDIAN_ABGR);
		}
	}
}

void CTextRasterizer::DrawBitmap(const uint8_t* pSource, uint32_t uiDestX, uint32_t uiDestY, uint32_t uiSrcX, uint32_t uiSrcY, uint32_t uiWidth, uint32_t uiHeight, uint32_t uiDestStride, uint32_t uiSourceStride, CColor& colValue) {
	for (uint32_t uiCurLine = 0; uiCurLine < uiHeight; uiCurLine++) {
		for (uint32_t uiCurRow = 0; uiCurRow < uiWidth; uiCurRow++) {
			const uint32_t uiDestOffset = (uiDestY + uiCurLine) * uiDestStride + uiDestX + uiCurRow;
			const uint32_t uiSourceOffset = (uiSrcY + uiCurLine) * uiSourceStride + uiSrcX + uiCurRow;

			if (uiDestOffset > m_uiBitmapSize) {
				break;
			}

			uint8_t ucSourcePixel = pSource[uiSourceOffset];
			if (ucSourcePixel > 0) {
				CColor colBackground{ CColor::eColorEndianness::COLOR_ENDIAN_ABGR, m_pBitmap[uiDestOffset] };
				const float fGrayToneHalf = ucSourcePixel / 255.0f;
				const float fAlpha = colValue.GetAlpha() / 255.0f;
				const float fOneminusAlpha = 1.0f - fAlpha;
				CColor colOutput{
					(CColor::ColorChannelValue)((colBackground.GetRed() * fOneminusAlpha) + (fAlpha * fGrayToneHalf * colValue.GetRed())),
					(CColor::ColorChannelValue)((colBackground.GetGreen() * fOneminusAlpha) + (fAlpha * fGrayToneHalf * colValue.GetGreen())),
					(CColor::ColorChannelValue)((colBackground.GetBlue() * fOneminusAlpha) + (fAlpha * fGrayToneHalf * colValue.GetBlue())),
					(CColor::ColorChannelValue)((colBackground.GetAlpha() * fOneminusAlpha) + colValue.GetAlpha())
				};

				m_pBitmap[uiDestOffset] = colOutput.Get(CColor::eColorEndianness::COLOR_ENDIAN_ABGR);
			}
		}
	}
}

uint32_t CTextRasterizer::GetTextDrawLength(const std::string& szInput) {
	uint32_t uiTextDrawLength = 0;
	std::u32string u32sInput{ CTextCharset::CP1251ToUTF32(szInput) };
	if (!u32sInput.empty()) {
		for (int iCurrentGlyph = 0; u32sInput[iCurrentGlyph] != '\0'; iCurrentGlyph++) {
			uiTextDrawLength += GetGlyphDrawLength(u32sInput[iCurrentGlyph], u32sInput[iCurrentGlyph + 1]);
		}
	}
	return uiTextDrawLength;
}

uint32_t CTextRasterizer::GetTextDrawHeight(uint32_t uiLinesCount) {
	return roundf((float)(uiLinesCount * m_iLineAscent) * m_fFontSize * 1.2);
}

uint32_t CTextRasterizer::GetGlyphDrawLength(char32_t c32CurrentSymbol, char32_t c32NextSymbol) {
	uint32_t uiTextDrawLength = 0;

	int iCharAdvanceWidth = 0;
	stbtt_GetCodepointHMetrics(&m_stbFont, c32CurrentSymbol, &iCharAdvanceWidth, 0);
	uiTextDrawLength += iCharAdvanceWidth * m_fFontSize;

	if (c32NextSymbol) {
		uiTextDrawLength += stbtt_GetCodepointKernAdvance(&m_stbFont, c32CurrentSymbol, c32NextSymbol) * m_fFontSize;
	}
	return uiTextDrawLength;
}

void CTextRasterizer::DrawTextLine(const std::string& szInput, CColor& colValue, bool bFromNewLine) {
	int iCurrentX = m_uiCurrentX;
	int iCurrentY = m_uiCurrentY;

	if (bFromNewLine) {
		iCurrentX = m_uiPositionX;
		iCurrentY += GetTextDrawHeight(1);
	}

	std::u32string u32sInput{ CTextCharset::CP1251ToUTF32(szInput) };
	if (!u32sInput.empty()) {
		for (uint32_t uiCurrentGlyph = 0; u32sInput[uiCurrentGlyph] != '\0'; uiCurrentGlyph++) {
			int iGlyphSize[2][2] = { {0, 0}, {0, 0} };
			uint8_t* pucGlyph = stbtt_GetCodepointBitmap(&m_stbFont, m_fFontSize, m_fFontSize, u32sInput[uiCurrentGlyph], &iGlyphSize[0][0], &iGlyphSize[0][1], &iGlyphSize[1][0], &iGlyphSize[1][1]);
			DrawBitmap(pucGlyph, iCurrentX + iGlyphSize[1][0], iCurrentY + m_iLineAscent * m_fFontSize + iGlyphSize[1][1], 0, 0, iGlyphSize[0][0], iGlyphSize[0][1], m_uiBitmapWidth, iGlyphSize[0][0], colValue);
			iCurrentX += GetGlyphDrawLength(u32sInput[uiCurrentGlyph], u32sInput[uiCurrentGlyph + 1]);
		}
	}

	m_uiCurrentX = iCurrentX;
	m_uiCurrentY = iCurrentY;
}

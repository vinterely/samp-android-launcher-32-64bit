#include "../main.h"
#include "game.h"
#include "materialtextgenerator.h"
#include "RW/RenderWare.h"
#include "TextRasterizer/TextRasterizer.h"
#include <cstdio>
#include <cstring>
#include <algorithm>

extern char* g_pszStorage;

MaterialTextGenerator::MaterialTextGenerator()
{
}

MaterialTextGenerator::~MaterialTextGenerator()
{
}

void MaterialTextGenerator::GetMaterialSize(int size, int* x, int* y)
{
	const int sizes[14][2] = {
		{ 32, 32 },   { 64, 32 },   { 64, 64 },   { 128, 32 },  { 128, 64 },
		{ 128, 128 }, { 256, 32 },  { 256, 64 },  { 256, 128 }, { 256, 256 },
		{ 512, 64 },  { 512, 128 }, { 512, 256 }, { 512, 512 }
	};

	int del = (size < 10 ? size : size / 10) - 1;
	if (del < 0) del = 0;
	if (del >= 14) del = 13;

	*x = sizes[del][0];
	*y = sizes[del][1];
}

RwTexture* MaterialTextGenerator::Generate(const char* text, int size, int font_size, bool bold, uint32_t font_color, uint32_t background_color, int alignment)
{
	if (!text) return nullptr;

	int width = 0, height = 0;
	GetMaterialSize(size, &width, &height);

	CTextRasterizer textRasterizer(width, height);

	CColor colBackground;
	colBackground.Set(CColor::eColorEndianness::COLOR_ENDIAN_ARGB, background_color);
	textRasterizer.FillBitmapWithColor(colBackground);

	if (font_size > 0)
	{
		char path[260];
		if (g_pszStorage) {
			snprintf(path, sizeof(path), "%sfonts/arial.ttf", g_pszStorage);
			FILE* f = fopen(path, "r");
			if (f) {
				fclose(f);
			} else {
				snprintf(path, sizeof(path), "%sfonts/arial_bold.ttf", g_pszStorage);
			}
		} else {
			snprintf(path, sizeof(path), "fonts/arial.ttf");
		}

		textRasterizer.SetupFont(path, static_cast<float>(font_size));
		if (textRasterizer.IsFontReady())
		{
			switch (alignment)
			{
			case 0:
				textRasterizer.SetTextAlignment(CTextRasterizer::eTextAlignment::TextAlignmentLeft);
				break;
			case 1:
				textRasterizer.SetTextAlignment(CTextRasterizer::eTextAlignment::TextAlignmentCenter);
				break;
			case 2:
				textRasterizer.SetTextAlignment(CTextRasterizer::eTextAlignment::TextAlignmentRight);
				break;
			default:
				textRasterizer.SetTextAlignment(CTextRasterizer::eTextAlignment::TextAlignmentLeft);
				break;
			}

			CColor colFont;
			colFont.Set(CColor::eColorEndianness::COLOR_ENDIAN_ARGB, font_color);
			CColouredTextMultiLine colouredText(colFont, text, false);
			textRasterizer.DrawText_(colouredText);
			textRasterizer.FreeFont();
		}
	}

	uint8_t* pBitmap = textRasterizer.GetBitmap();
	if (!pBitmap) return nullptr;

	RwImage* pRwImage = RwImageCreate(width, height, 32);
	if (!pRwImage) return nullptr;

	RwImageAllocatePixels(pRwImage);
	for (int y = 0; y < height; y++)
	{
		memcpy(pRwImage->cpPixels + pRwImage->stride * y, pBitmap + width * 4 * y, width * 4);
	}

	int iRasterWidth = 0;
	int iRasterHeight = 0;
	int iRasterDepth = 0;
	int iRasterFlags = 0;
	RwImageFindRasterFormat(
		pRwImage,
		rwRASTERTYPETEXTURE,
		&iRasterWidth,
		&iRasterHeight,
		&iRasterDepth,
		&iRasterFlags
	);

	RwRaster* pRwRaster = RwRasterCreate(iRasterWidth, iRasterHeight, iRasterDepth, iRasterFlags);
	if (!pRwRaster)
	{
		RwImageDestroy(pRwImage);
		return nullptr;
	}

	RwRasterSetFromImage(pRwRaster, pRwImage);
	RwImageDestroy(pRwImage);

	RwTexture* pRwTexture = RwTextureCreate(pRwRaster);
	if (pRwTexture)
	{
		++pRwTexture->refCount;
	}
	return pRwTexture;
}

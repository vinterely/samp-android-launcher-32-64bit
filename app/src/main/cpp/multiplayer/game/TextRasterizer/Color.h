#pragma once

#include <string>

class CColor {
public:
	enum eColorEndianness : unsigned char {
		COLOR_ENDIAN_RGB,
		COLOR_ENDIAN_RGBA,
		COLOR_ENDIAN_ARGB,
		COLOR_ENDIAN_BGR,
		COLOR_ENDIAN_BGRA,
		COLOR_ENDIAN_ABGR,
	};

	typedef unsigned int ColorValue;
	typedef unsigned char ColorChannelValue;

private:
	ColorChannelValue red, green, blue, alpha;

public:
	void Set(ColorChannelValue r, ColorChannelValue g, ColorChannelValue b, ColorChannelValue a) {
		red = r;
		green = g;
		blue = b;
		alpha = a;
	};

	void Set(const CColor& colInput) {
		Set(colInput.red, colInput.green, colInput.blue, colInput.alpha);
	};

	void Set(eColorEndianness ucColorEndian, ColorValue uiColor) {
		switch (ucColorEndian) {
		case COLOR_ENDIAN_RGB: {
			Set(
				(uiColor >> 16) & 0xFF,
				(uiColor >> 8) & 0xFF,
				uiColor & 0xFF,
				0xFF
			);
			break;
		}

		case COLOR_ENDIAN_RGBA: {
			Set(
				(uiColor >> 24) & 0xFF,
				(uiColor >> 16) & 0xFF,
				(uiColor >> 8) & 0xFF,
				uiColor & 0xFF
			);
			break;
		}

		case COLOR_ENDIAN_ARGB: {
			Set(
				(uiColor >> 16) & 0xFF,
				(uiColor >> 8) & 0xFF,
				uiColor & 0xFF,
				(uiColor >> 24) & 0xFF
			);
			break;
		}

		case COLOR_ENDIAN_BGR: {
			Set(
				uiColor & 0xFF,
				(uiColor >> 8) & 0xFF,
				(uiColor >> 16) & 0xFF,
				0xFF
			);
			break;
		}

		case COLOR_ENDIAN_BGRA: {
			Set(
				(uiColor >> 8) & 0xFF,
				(uiColor >> 16) & 0xFF,
				(uiColor >> 24) & 0xFF,
				uiColor & 0xFF
			);
			break;
		}

		case COLOR_ENDIAN_ABGR: {
			Set(
				uiColor & 0xFF,
				(uiColor >> 8) & 0xFF,
				(uiColor >> 16) & 0xFF,
				(uiColor >> 24) & 0xFF
			);
			break;
		}
		}
	};

	ColorValue Get(eColorEndianness ucColorEndian) {
		switch (ucColorEndian) {
		case COLOR_ENDIAN_RGB: {
			return blue | (green << 8) | (red << 16);
			break;
		}
		case COLOR_ENDIAN_RGBA: {
			return alpha | (blue << 8) | (green << 16) | (red << 24);
			break;
		}

		case COLOR_ENDIAN_ARGB: {
			return blue | (green << 8) | (red << 16) | (alpha << 24);
			break;
		}

		case COLOR_ENDIAN_BGR: {
			return red | (green << 8) | (blue << 16);
			break;
		}

		case COLOR_ENDIAN_BGRA: {
			return alpha | (red << 8) | (green << 16) | (blue << 24);
			break;
		}

		case COLOR_ENDIAN_ABGR: {
			return red | (green << 8) | (blue << 16) | (alpha << 24);
			break;
		}
		default: {
			return 0xFFFFFFFF;
			break;
		}
		}
	};

	std::string Stringify(eColorEndianness ucColorEndian, size_t uiHexLen = 8) {
		static const char* szAlphabet = "0123456789ABCDEF";
		const ColorValue uiColor = Get(ucColorEndian);
		std::string szResult{ uiHexLen, '\0', std::allocator<char>() };
		for (size_t i = 0, j = (uiHexLen - 1) * 4; i < uiHexLen; ++i, j -= 4) {
			szResult[i] = szAlphabet[(uiColor >> j) & 0x0F];
		}
		return szResult;
	};

	CColor(ColorChannelValue r, ColorChannelValue g, ColorChannelValue b, ColorChannelValue a) {
		Set(r, g, b, a);
	};

	CColor(ColorChannelValue r, ColorChannelValue g, ColorChannelValue b) {
		Set(r, g, b, 0xFF);
	};

	CColor(eColorEndianness ucColorEndian, ColorValue uiColor) {
		Set(ucColorEndian, uiColor);
	};

	void Clear() {
		Set(0xFF, 0xFF, 0xFF, 0xFF);
	};

	CColor() {
		Clear();
	};

	void SetRed(ColorChannelValue r) {
		red = r;
	};

	void SetRed(const CColor& colInput) {
		red = colInput.red;
	};

	void SetGreen(ColorChannelValue g) {
		green = g;
	};

	void SetGreen(const CColor& colInput) {
		green = colInput.green;
	};

	void SetBlue(ColorChannelValue b) {
		blue = b;
	};

	void SetBlue(const CColor& colInput) {
		blue = colInput.blue;
	};

	void SetAlpha(ColorChannelValue a) {
		alpha = a;
	};

	void SetAlpha(const CColor& colInput) {
		alpha = colInput.alpha;
	};

	ColorChannelValue GetRed() {
		return red;
	};

	ColorChannelValue GetGreen() {
		return green;
	};

	ColorChannelValue GetBlue() {
		return blue;
	};

	ColorChannelValue GetAlpha() {
		return alpha;
	};
};

#pragma once

#include <string>
#include <vector>

#include "Color.h"

#define MAX_SECTIONS (32)
#define	MAX_LINES (112)

class CColouredTextSection {
private:
	CColor m_TextColour{ 0x00, 0x00, 0x00, 0x00 };
	std::string m_szText{ "\0" };

public:
	void SetTextColour(const CColor& textColour);
	CColor& GetTextColour();

	void SetText(const std::string& szText);
	void Clear();
	std::string& GetText();

	CColouredTextSection();
	~CColouredTextSection();
	CColouredTextSection(const CColor& textColour, const std::string& szText);
};

class CColouredTextLine {
private:
	std::vector<CColouredTextSection> m_TextSections;
	std::string m_szPlainText{ "\0" };

public:
	void Push(CColor&, const std::string&);
	void Clear();
	CColouredTextSection& GetTextSection(const size_t uiId);

	std::string& GetPlainText();
	size_t GetSectionsCount();

	CColouredTextLine();
	~CColouredTextLine();
	CColouredTextLine(CColor& textColour, const std::string& szText);

	template<typename InputEventFunc>
	void Foreach(InputEventFunc inputEventFunc) {
		for (size_t i = 0; i < GetSectionsCount(); ++i) {
			inputEventFunc(i, m_TextSections.at(i));
		}
	};
};

class CColouredTextMultiLine {
private:
	std::vector<CColouredTextLine> m_TextLines;

public:
	void Push(const CColor&, const std::string&, bool);
	void Clear();
	CColouredTextLine& GetTextLine(const size_t uiId);

	size_t GetLinesCount();

	CColouredTextMultiLine();
	~CColouredTextMultiLine();
	CColouredTextMultiLine(const CColor& textColour, const std::string& szText, bool bResetColorsPerLine);

	template<typename InputEventFunc>
	void Foreach(InputEventFunc inputEventFunc) {
		for (size_t i = 0; i < GetLinesCount(); ++i) {
			inputEventFunc(i, m_TextLines.at(i));
		}
	};
};

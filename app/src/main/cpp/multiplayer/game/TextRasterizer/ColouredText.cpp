#include "ColouredText.h"

#include <algorithm>

void CColouredTextSection::SetTextColour(const CColor& textColour) {
	m_TextColour.Set(textColour);
}

CColor& CColouredTextSection::GetTextColour() {
	return m_TextColour;
}

void CColouredTextSection::SetText(const std::string& szText) {
	m_szText = szText;
}

void CColouredTextSection::Clear() {
	m_TextColour.Clear();
	m_szText.clear();
}

std::string& CColouredTextSection::GetText() {
	return m_szText;
}

CColouredTextSection::CColouredTextSection() {
	Clear();
}

CColouredTextSection::~CColouredTextSection() {
	Clear();
}

CColouredTextSection::CColouredTextSection(const CColor& textColour, const std::string& szText) {
	Clear();
	SetTextColour(textColour);
	SetText(szText);
}

void CColouredTextLine::Push(CColor& colTextColour, const std::string& szText) {
	if (szText.empty()) {
		m_TextSections.push_back({ });
		auto& textSection = m_TextSections.back();
		textSection.Clear();
		textSection.SetText(" ");
		textSection.SetTextColour(colTextColour);
	}
	else {
		const size_t uiColorTagSize = 6;

		std::string szLastResolvedText{ "\0" };
		CColor colLastResolvedColour{ colTextColour };

		const size_t uiInputLength = szText.length();
		for (size_t uiCurrentCharPosition = 0; uiCurrentCharPosition < uiInputLength; ++uiCurrentCharPosition) {
			if (szText[uiCurrentCharPosition] == '{' && uiInputLength - uiCurrentCharPosition > uiColorTagSize + 1) {
				if (szText[uiCurrentCharPosition + uiColorTagSize + 1] == '}') {
					std::string szColor{ szText.substr(uiCurrentCharPosition + 1, uiColorTagSize) };
					if (std::all_of(szColor.begin(), szColor.end(), [](char cOut) { return cOut >= '0' && cOut <= '9' || cOut >= 'a' && cOut <= 'f' || cOut >= 'A' && cOut <= 'F'; })) {
						if (!szLastResolvedText.empty()) {
							m_szPlainText += szLastResolvedText;
							m_TextSections.push_back({ });
							auto& textSection = m_TextSections.back();
							textSection.Clear();
							textSection.SetText(szLastResolvedText);
							textSection.SetTextColour(colLastResolvedColour);
							szLastResolvedText.clear();
						}

						CColor::ColorChannelValue colChannelPreviousAlpha = colLastResolvedColour.GetAlpha();
						colLastResolvedColour.Set(CColor::eColorEndianness::COLOR_ENDIAN_RGB, strtol(szColor.data(), nullptr, 16));
						colLastResolvedColour.SetAlpha(colChannelPreviousAlpha);
						uiCurrentCharPosition += uiColorTagSize + 1;
						continue;
					}
				}
			}
			szLastResolvedText.push_back(szText[uiCurrentCharPosition]);
		}

		if (!szLastResolvedText.empty()) {
			m_szPlainText += szLastResolvedText;
			m_TextSections.push_back({ });
			auto& textSection = m_TextSections.back();
			textSection.Clear();
			textSection.SetText(szLastResolvedText);
			textSection.SetTextColour(colLastResolvedColour);
			szLastResolvedText.clear();
		}

		colTextColour.Set(colLastResolvedColour);
	}
}

void CColouredTextLine::Clear() {
	m_TextSections.clear();
	m_szPlainText.clear();
}

CColouredTextSection& CColouredTextLine::GetTextSection(const size_t uiId) {
	return *std::next(m_TextSections.begin(), uiId);
}

std::string& CColouredTextLine::GetPlainText() {
	return m_szPlainText;
}

size_t CColouredTextLine::GetSectionsCount() {
	return m_TextSections.size();
}

CColouredTextLine::CColouredTextLine() {
	Clear();
}

CColouredTextLine::~CColouredTextLine() {
	Clear();
}

CColouredTextLine::CColouredTextLine(CColor& textColour, const std::string& szText) {
	Clear();
	Push(textColour, szText);
}

void CColouredTextMultiLine::Push(const CColor& colTextColour, const std::string& szText, bool bResetColorsPerLine) {
	typedef std::vector<std::string> TextLinesList;
	const auto fSplitTextForLines = [](const std::string& szInput, TextLinesList& vecOutputLines) -> void {
		std::string::size_type uiPrevPos = 0;
		std::string::size_type uiCurPos = 0;
		while ((uiCurPos = szInput.find('\n', uiCurPos)) != std::string::npos) {
			vecOutputLines.push_back(szInput.substr(uiPrevPos, uiCurPos - uiPrevPos));
			uiPrevPos = ++uiCurPos;
		}
		vecOutputLines.push_back(szInput.substr(uiPrevPos, uiCurPos - uiPrevPos));
	};

	TextLinesList vecLines;
	fSplitTextForLines(szText, vecLines);

	CColor colLastPushedColor{ colTextColour };
	for (const std::string& szEntry : vecLines) {
		m_TextLines.push_back({ });
		auto& textLine = m_TextLines.back();
		textLine.Clear();
		textLine.Push(colLastPushedColor, szEntry);
		if (bResetColorsPerLine) {
			colLastPushedColor.Clear();
			colLastPushedColor.SetAlpha(colTextColour);
		}
	}
}

void CColouredTextMultiLine::Clear() {
	m_TextLines.clear();
};

CColouredTextLine& CColouredTextMultiLine::GetTextLine(const size_t uiId) {
	return *std::next(m_TextLines.begin(), uiId);
};

size_t CColouredTextMultiLine::GetLinesCount() {
	return m_TextLines.size();
};

CColouredTextMultiLine::CColouredTextMultiLine() {
	Clear();
};

CColouredTextMultiLine::~CColouredTextMultiLine() {
	Clear();
};

CColouredTextMultiLine::CColouredTextMultiLine(const CColor& textColour, const std::string& szText, bool bResetColorsPerLine) {
	Clear();
	Push(textColour, szText, bResetColorsPerLine);
};
#pragma once

struct CustomTag {
    std::string nick;
    std::string tagText;
    ImU32 tagColor;
};

class CPlayerTags
{
public:
    CPlayerTags();
    ~CPlayerTags();

    void Render(ImGuiRenderer* renderer);

    std::vector<CustomTag> m_customTags;

private:
    void Draw(ImGuiRenderer* renderer, CVector* vec, const char* szNick, uint32_t dwColor, float fDist, float fHealth, float fArmour, bool bAfk, bool bMicro);

    RwTexture* m_pAFKIconTexture;
    RwTexture* m_pMicroIconTexture;
};
#pragma once

class ButtonPanel : public Layout
{
public:
    ButtonPanel();

    CButton* m_bH;
    CButton* m_bAlt;
    CButton* m_bEsc;
    CButton* m_bF;
    CButton* m_bY;
    CButton* m_bN;
    CButton* m_bG;
    CButton* m_b2;
    CButton* m_bTab;
    CButton* m_bClose;

private:
    OButton* m_bToggle;
    CButton* m_bP;
    CButton* m_bFOOD;
    CButton* m_bGPS;
    CButton* m_bD;
    CButton* m_bUSE;

};
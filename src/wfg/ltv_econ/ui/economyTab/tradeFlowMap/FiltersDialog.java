package wfg.ltv_econ.ui.economyTab.tradeFlowMap;

import static wfg.native_ui.util.UIConstants.*;

import com.fs.starfarer.api.ui.ButtonAPI.UICheckboxSize;

import wfg.native_ui.internal.ui.Side;
import wfg.native_ui.ui.panels.CheckboxButton;
import wfg.native_ui.ui.panels.DockPanel;

public class FiltersDialog extends DockPanel {


    public FiltersDialog(int width, int height) {
        super(width, height, Side.RIGHT);

        buildUI();
    }

    @Override
    public void buildUI() {
        final CheckboxButton checkbox1 = new CheckboxButton(m_panel, 20,
            "This is good", null, null, UICheckboxSize.SMALL, false);
        final CheckboxButton checkbox2 = new CheckboxButton(m_panel, 20,
            "This is glowing", null, null, UICheckboxSize.SMALL, true);
        final CheckboxButton checkbox3 = new CheckboxButton(m_panel, 40,
            "This is big", null, null, UICheckboxSize.SMALL, false);
        final CheckboxButton checkbox4 = new CheckboxButton(m_panel, 60,
            "This is massive", null, null, UICheckboxSize.LARGE, false);

        add(checkbox1).inTL(pad, opad);
        add(checkbox2).inTL(pad, opad*2 + 20);
        add(checkbox3).inTL(pad, opad*3 + 40);
        add(checkbox4).inTL(pad, opad*4 + 80);
    }
}
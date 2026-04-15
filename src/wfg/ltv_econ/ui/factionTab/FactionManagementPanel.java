package wfg.ltv_econ.ui.factionTab;

import java.util.ArrayList;
import java.util.List;

import com.fs.starfarer.api.ui.UIPanelAPI;

import wfg.ltv_econ.ui.economyTab.FactionAdministrationPanel;
import wfg.ltv_econ.ui.reusable.AbstractManagementPanel;

public class FactionManagementPanel extends AbstractManagementPanel<FactionManagementPanel> {

    public FactionManagementPanel(UIPanelAPI parent) {
        super(parent);

        buildUI();
    }

    protected final String getTitle() {
        return "Faction Management";
    }

    protected final String getSubtitle() {
        return "Strategic oversight of assets";
    }

    protected final List<NavButtonDef> getNavButtonDefs() {
        final List<NavButtonDef> defs = new ArrayList<>();


        defs.add(new NavButtonDef("Faction Administration",
            () -> {
                final FactionAdministrationPanel content = new FactionAdministrationPanel(
                    contentPanel, CONTENT_PANEL_W, CONTENT_PANEL_H
                );
                contentPanel.addComponent(content.getPanel()).inBL(0f, 0f);
            }
        ));

        return defs;
    }
}
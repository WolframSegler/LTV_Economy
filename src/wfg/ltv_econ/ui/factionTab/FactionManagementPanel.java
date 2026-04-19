package wfg.ltv_econ.ui.factionTab;

import java.util.ArrayList;
import java.util.List;

import org.lwjgl.input.Keyboard;

import com.fs.starfarer.api.ui.UIPanelAPI;

import wfg.ltv_econ.ui.reusable.AbstractManagementPanel;

public class FactionManagementPanel extends AbstractManagementPanel {

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

        defs.add(new NavButtonDef("Faction Hangar", Keyboard.KEY_NONE, () -> {
            final ShipInventoryPanel content = new ShipInventoryPanel(
                contentPanel, CONTENT_PANEL_W, CONTENT_PANEL_H
            );
            contentPanel.addComponent(content.getPanel()).inBL(0f, 0f);
        }));

        defs.add(new NavButtonDef("Planned Orders", Keyboard.KEY_Q, () -> {
            final PlannedOrdersPanel content = new PlannedOrdersPanel(
                contentPanel, CONTENT_PANEL_W, CONTENT_PANEL_H
            );
            contentPanel.addComponent(content.getPanel()).inBL(0f, 0f);
        }));

        defs.add(new NavButtonDef("Production Queue", Keyboard.KEY_W, () -> {
            final ActiveQueuePanel content = new ActiveQueuePanel(
                contentPanel, CONTENT_PANEL_W, CONTENT_PANEL_H
            );
            contentPanel.addComponent(content.getPanel()).inBL(0f, 0f);
        }));

        defs.add(new NavButtonDef("Faction Administration", Keyboard.KEY_NONE, () -> {
            final FactionAdministrationPanel content = new FactionAdministrationPanel(
                contentPanel, CONTENT_PANEL_W, CONTENT_PANEL_H
            );
            contentPanel.addComponent(content.getPanel()).inBL(0f, 0f);
        }));

        return defs;
    }
}
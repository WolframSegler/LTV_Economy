package wfg.ltv_econ.ui.factionTab;

import java.util.ArrayList;
import java.util.List;

import org.lwjgl.input.Keyboard;

import com.fs.starfarer.api.ui.UIPanelAPI;

import wfg.ltv_econ.ui.reusable.AbstractManagementPanel;

import static wfg.ltv_econ.constants.strings.LocalizedStrings.*;

public class FactionManagementPanel extends AbstractManagementPanel {

    public FactionManagementPanel(UIPanelAPI parent) {
        super(parent);

        buildUI();
    }

    protected final String getTitle() {
        return str("uiFactionMenuTitle");
    }

    protected final String getSubtitle() {
        return str("uiFactionMenuSubtitle");
    }

    protected final List<NavButtonDef> getNavButtonDefs() {
        final List<NavButtonDef> defs = new ArrayList<>();

        defs.add(new NavButtonDef(str("uiShipHangarTitle"), Keyboard.KEY_Q, () -> {
            final ShipInventoryPanel content = new ShipInventoryPanel(
                contentPanel, CONTENT_PANEL_W, CONTENT_PANEL_H
            );
            contentPanel.addComponent(content.getPanel()).inBL(0f, 0f);
        }));

        defs.add(new NavButtonDef(str("uiFactionShipPlannedOrdersTitle"), Keyboard.KEY_W, () -> {
            final PlannedOrdersPanel content = new PlannedOrdersPanel(
                contentPanel, CONTENT_PANEL_W, CONTENT_PANEL_H
            );
            contentPanel.addComponent(content.getPanel()).inBL(0f, 0f);
        }));

        defs.add(new NavButtonDef(str("uiAssemblyLineTitle"), Keyboard.KEY_A, () -> {
            final ActiveQueuePanel content = new ActiveQueuePanel(
                contentPanel, CONTENT_PANEL_W, CONTENT_PANEL_H
            );
            contentPanel.addComponent(content.getPanel()).inBL(0f, 0f);
        }));

        defs.add(new NavButtonDef(str("uiAdministrationBtnTitle"), Keyboard.KEY_S, () -> {
            final FactionAdministrationPanel content = new FactionAdministrationPanel(
                contentPanel, CONTENT_PANEL_W, CONTENT_PANEL_H
            );
            contentPanel.addComponent(content.getPanel()).inBL(0f, 0f);
        }));

        return defs;
    }
}
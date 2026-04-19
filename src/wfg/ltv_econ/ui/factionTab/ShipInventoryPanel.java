package wfg.ltv_econ.ui.factionTab;

import static wfg.native_ui.util.Globals.settings;
import static wfg.native_ui.util.UIConstants.*;

import com.fs.starfarer.api.impl.campaign.DebugFlags;
import com.fs.starfarer.api.ui.Fonts;
import com.fs.starfarer.api.ui.LabelAPI;
import com.fs.starfarer.api.ui.UIPanelAPI;

import wfg.ltv_econ.economy.engine.EconomyEngine;
import wfg.ltv_econ.ui.factionTab.dialog.FactionSelectionDialog;
import wfg.native_ui.ui.core.UIBuildableAPI;
import wfg.native_ui.ui.functional.DockButton;
import wfg.native_ui.ui.functional.Button.CutStyle;
import wfg.native_ui.ui.panel.CustomPanel;

public class ShipInventoryPanel extends CustomPanel implements UIBuildableAPI {
    
    public ShipInventoryPanel(UIPanelAPI parent, int w, int h) {
        super(parent, w, h);

        buildUI();
    }

    @Override
    public void buildUI() {
        clearChildren();
        
        final boolean hasColony = EconomyEngine.instance().getPlayerMarketData().size() > 0;
        if (!DebugFlags.COLONY_DEBUG && !hasColony) {
            final LabelAPI lbl = settings.createLabel("No static assets", Fonts.DEFAULT_SMALL);
            lbl.setColor(gray);
            add(lbl).inMid();
            return;
        }
        final int panelW = (int) pos.getWidth();
        final int panelH = (int) pos.getHeight();

        final ShipInventoryNavbar navbar = new ShipInventoryNavbar(m_panel, panelW, 130);
        add(navbar).inTL(0f, 0f);

        final FactionShipGrid grid = new FactionShipGrid(m_panel, panelW, panelH - 160, navbar);
        add(grid).inTL(0f, 170f);

        final ShipFiltersPanel filters = new ShipFiltersPanel(m_panel, panelW, grid);
        add(filters).inTL(0f, 140f);

        if (DebugFlags.COLONY_DEBUG) {
            final DockButton<FactionSelectionDialog> factionSelection = new DockButton<>(
                m_panel, 120, 28, "Pick Faction", null, () -> new FactionSelectionDialog(this)
            );
            factionSelection.cutStyle = CutStyle.ALL;
            add(factionSelection).inTMid(pad);
        }
    }
}
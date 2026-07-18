package wfg.ltv_econ.ui.factionTab;

import static wfg.ltv_econ.constant.strings.LocalizedStrings.*;
import static wfg.native_ui.util.Globals.settings;
import static wfg.native_ui.util.UIConstants.*;

import com.fs.starfarer.api.impl.campaign.DebugFlags;
import com.fs.starfarer.api.ui.Fonts;
import com.fs.starfarer.api.ui.LabelAPI;

import wfg.ltv_econ.economy.engine.EconomyEngine;
import wfg.ltv_econ.ui.factionTab.dialog.FactionSelectionDialog;
import wfg.native_ui.internal.ui.core.UIContainer;
import wfg.native_ui.ui.core.UIBuildableAPI;
import wfg.native_ui.ui.functional.DockButton;
import wfg.native_ui.ui.functional.Button.CutStyle;

public final class ShipInventoryPanel extends UIContainer implements UIBuildableAPI {
    
    public ShipInventoryPanel(int w, int h) {
        super(w, h);

        buildUI();
    }

    @Override
    public void buildUI() {
        clearChildren();
        
        final boolean hasColony = EconomyEngine.instance().getMarketPopulationData().size() > 0;
        if (!DebugFlags.COLONY_DEBUG && !hasColony) {
            final LabelAPI lbl = settings.createLabel(str("uiNoStaticAssets"), Fonts.DEFAULT_SMALL);
            lbl.setColor(gray);
            add(lbl).inMid();
            return;
        }
        final int panelW = (int) getWidth();
        final int panelH = (int) getHeight();

        final ShipInventoryNavbar navbar = new ShipInventoryNavbar(panelW, 130);
        add(navbar).inTL(0f, 0f);

        final FactionShipGrid grid = new FactionShipGrid(panelW, panelH - 160, navbar);
        add(grid).inTL(0f, 170f);

        final ShipFiltersPanel filters = new ShipFiltersPanel(panelW, grid);
        add(filters).inTL(0f, 140f);

        if (DebugFlags.COLONY_DEBUG) {
            final DockButton<FactionSelectionDialog> factionSelection = new DockButton<>(
                120, 28, str("uiBtnTitlePickFaction"), null, () -> new FactionSelectionDialog(this)
            );
            factionSelection.setCutStyle(CutStyle.ALL);
            add(factionSelection).inTMid(pad);
        }
    }
}
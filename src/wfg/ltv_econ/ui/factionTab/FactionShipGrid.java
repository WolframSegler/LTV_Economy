package wfg.ltv_econ.ui.factionTab;

import static wfg.native_ui.util.Globals.settings;
import static wfg.native_ui.util.UIConstants.*;

import java.util.ArrayList;
import java.util.List;

import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.ui.Alignment;
import com.fs.starfarer.api.ui.Fonts;
import com.fs.starfarer.api.ui.LabelAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.ui.UIPanelAPI;

import wfg.ltv_econ.economy.fleet.FactionShipInventory;
import wfg.ltv_econ.economy.fleet.ShipTypeData;
import wfg.ltv_econ.ui.fleet.InventoryShipWidget;
import wfg.ltv_econ.ui.fleet.ShipFilters;
import wfg.native_ui.ui.ComponentFactory;
import wfg.native_ui.ui.core.UIBuildableAPI;
import wfg.native_ui.ui.panel.CustomPanel;

public class FactionShipGrid extends CustomPanel implements UIBuildableAPI {
    
    private final FactionShipInventory inv;

    public FactionShipGrid(UIPanelAPI parent, int w, int h, FactionShipInventory inv) {
        super(parent, w, h);

        this.inv = inv;

        buildUI();
    }

    @Override
    public void buildUI() {
        clearChildren();

        final List<ShipTypeData> ships = getFilteredAndSortedShips();
        if (ships.isEmpty()) {
            showEmptyState();
            return;
        }

        final int cardW = InventoryShipWidget.WIDTH;
        final int cardH = InventoryShipWidget.HEIGHT;
        final int gap = pad * 2;

        final int availableWidth = (int)pos.getWidth() - opad * 2;
        final int cols = Math.max(1, (availableWidth + gap) / (cardW + gap));

        final TooltipMakerAPI container = ComponentFactory.createTooltip(pos.getWidth(), true);

        float contH = 0f;
        for (int i = 0; i < ships.size(); i++) {
            final InventoryShipWidget widget = new InventoryShipWidget(container, ships.get(i));

            final int row = i / cols;
            final int col = i % cols;
            final int x = opad + col * (cardW + gap);
            final int y = opad + row * (cardH + gap);

            container.addCustom(widget.getPanel(), 0f).getPosition().inTL(x, y);
            contH = y + cardH;
        }

        container.setHeightSoFar(contH);
        ComponentFactory.addTooltip(container, pos.getHeight(), true, m_panel).inBL(0f, 0f);
    }

    private final List<ShipTypeData> getFilteredAndSortedShips() {
        final List<ShipTypeData> list = new ArrayList<>(inv.getShips().values());

        list.removeIf(this::shouldFilterOut);
        list.sort(this::compareShips);

        return list;
    }

    private final boolean shouldFilterOut(ShipTypeData data) {
        if (!ShipFilters.showCivilian && data.spec.getDesignation().equals(ShipTypeData.CIVILIAN)) return true;
        if (!ShipFilters.showCombat && !data.spec.getDesignation().equals(ShipTypeData.CIVILIAN)) return true;
        if (ShipFilters.showOnlyIdle && data.getIdle() < 1) return true;

        final HullSize size = data.spec.getHullSize();
        if (!ShipFilters.showFrigates && size == HullSize.FRIGATE) return true;
        if (!ShipFilters.showDestroyers && size == HullSize.DESTROYER) return true;
        if (!ShipFilters.showCruisers && size == HullSize.CRUISER) return true;
        if (!ShipFilters.showCapitals && size == HullSize.CAPITAL_SHIP) return true;

        final String query = ShipFilters.searchQuery.trim().toLowerCase();
        if (!query.isEmpty()) {
            final String name = data.spec.getHullName().toLowerCase();
            final String id = data.hullID.toLowerCase();
            if (!name.contains(query) && !id.contains(query)) return true;
        }

        return false;
    }

    private final int compareShips(ShipTypeData a, ShipTypeData b) {
        return switch (ShipFilters.sortMode) {
            case NAME -> a.spec.getHullName().compareToIgnoreCase(b.spec.getHullName());
            case CARGO -> Float.compare(b.spec.getCargo(), a.spec.getCargo());
            case FUEL -> Float.compare(b.spec.getFuel(), a.spec.getFuel());
            case CREW -> Integer.compare(b.getCrewCapacityPerShip(), a.getCrewCapacityPerShip());
            case COMBAT -> Float.compare(b.getCombatPower(), a.getCombatPower());
            case COUNT -> Integer.compare(b.getOwned(), a.getOwned());
        };
    }

    private final void showEmptyState() {
        final LabelAPI emptyLbl = settings.createLabel("No match.", Fonts.DEFAULT_SMALL);
        emptyLbl.setColor(gray);
        emptyLbl.setAlignment(Alignment.MID);
        add(emptyLbl).inMid();
    }
}
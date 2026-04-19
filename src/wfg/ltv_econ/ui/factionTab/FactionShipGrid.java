package wfg.ltv_econ.ui.factionTab;

import static wfg.native_ui.util.UIConstants.*;

import java.util.ArrayList;
import java.util.List;

import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.impl.campaign.DebugFlags;
import com.fs.starfarer.api.ui.UIPanelAPI;

import wfg.ltv_econ.economy.fleet.ShipTypeData;
import wfg.ltv_econ.serializable.StaticData;
import wfg.ltv_econ.ui.fleet.InventoryShipWidget;
import wfg.ltv_econ.ui.fleet.ShipFilters;
import wfg.native_ui.ui.core.UIBuildableAPI;
import wfg.native_ui.ui.table.GridTable;
import wfg.native_ui.util.NativeUiUtils;
import wfg.native_ui.util.NativeUiUtils.AnchorType;

public class FactionShipGrid extends GridTable<ShipTypeData, InventoryShipWidget> {
    
    private final UIBuildableAPI navbar;

    public FactionShipGrid(UIPanelAPI parent, int w, int h, UIBuildableAPI navbar) {
        super(parent, w, h, InventoryShipWidget.WIDTH, InventoryShipWidget.HEIGHT, opad*2);
        this.navbar = navbar;

        uniformOuterGap = true;
        justifyGrid = true;
        
        buildUI();
    }

    protected final List<ShipTypeData> getDataList() {
        final List<ShipTypeData> list = new ArrayList<>(StaticData.inv.getShips().values());
        if (!DebugFlags.COLONY_DEBUG) {
            list.removeIf(d -> d.getOwned() < 1);
        }
        list.removeIf(this::shouldFilterOut);
        list.sort(this::compareShips);

        return list;
    }

    protected InventoryShipWidget createWidget(ShipTypeData item, int index) {
        final InventoryShipWidget widget = new InventoryShipWidget(container, item, navbar);

        widget.tooltip.positioner = (tp, exp) -> {
            NativeUiUtils.anchorPanel(tp, widget.getPanel(), (calculateColumns() > 2 ?
                AnchorType.LeftTop : AnchorType.RightTop), opad
            );
        };

        return widget;
    }

    protected void onWidgetClicked(InventoryShipWidget source) {}

    protected String getEmptyMessage() {
        return "No match.";
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
}
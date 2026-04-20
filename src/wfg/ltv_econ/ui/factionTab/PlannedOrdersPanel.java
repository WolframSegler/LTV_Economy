package wfg.ltv_econ.ui.factionTab;

import static wfg.ltv_econ.constants.Sprites.CHECKLIST;
import static wfg.ltv_econ.constants.Sprites.SHIPS;
import static wfg.ltv_econ.constants.Sprites.STOPWATCH;
import static wfg.ltv_econ.constants.Sprites.WAGES;
import static wfg.native_ui.util.Globals.settings;
import static wfg.native_ui.util.UIConstants.*;

import java.util.List;

import org.lwjgl.input.Keyboard;

import com.fs.starfarer.api.impl.campaign.DebugFlags;
import com.fs.starfarer.api.impl.campaign.ids.Commodities;
import com.fs.starfarer.api.impl.campaign.ids.Strings;
import com.fs.starfarer.api.ui.Alignment;
import com.fs.starfarer.api.ui.Fonts;
import com.fs.starfarer.api.ui.LabelAPI;
import com.fs.starfarer.api.ui.UIPanelAPI;

import wfg.ltv_econ.config.EconConfig;
import wfg.ltv_econ.economy.engine.EconomyEngine;
import wfg.ltv_econ.economy.fleet.PlannedOrder;
import wfg.ltv_econ.serializable.StaticData;
import wfg.ltv_econ.ui.factionTab.dialog.ClearAllDialog;
import wfg.ltv_econ.ui.factionTab.dialog.FactionSelectionDialog;
import wfg.ltv_econ.ui.factionTab.dialog.ShipCommissionDialog;
import wfg.ltv_econ.ui.fleet.PlannedOrderWidget;
import wfg.ltv_econ.ui.reusable.WidgetSelectionState;
import wfg.native_ui.ui.component.InteractionComp;
import wfg.native_ui.ui.component.NativeComponents;
import wfg.native_ui.ui.core.UIBuildableAPI;
import wfg.native_ui.ui.core.UIElementFlags.HasInteraction;
import wfg.native_ui.ui.functional.Button;
import wfg.native_ui.ui.functional.DockButton;
import wfg.native_ui.ui.functional.Button.CutStyle;
import wfg.native_ui.ui.panel.CustomPanel;
import wfg.native_ui.ui.table.GridTable;
import wfg.native_ui.ui.visual.IconValuePairTp;
import wfg.native_ui.util.NativeUiUtils;
import wfg.native_ui.util.NativeUiUtils.AnchorType;

public class PlannedOrdersPanel extends CustomPanel implements UIBuildableAPI, HasInteraction {
    private static final int HEADER_HEIGHT = 50;

    private final InteractionComp<PlannedOrdersPanel> interaction = comp().get(NativeComponents.INTERACTION); 

    public PlannedOrderGrid grid;
    
    public PlannedOrdersPanel(UIPanelAPI parent, int w, int h) {
        super(parent, w, h);

        interaction.onClicked = (panel, isLeftClick) -> {
            grid.clearSelection();
        };

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

        final List<PlannedOrder> orders = StaticData.inv.getPlannedOrders();
        final long totalCost = orders.stream().mapToLong(o -> o.credits).sum();
        final int totalTime = orders.stream().mapToInt(o -> o.days).sum();
        final double totalShips = orders.stream().mapToDouble(o -> o.commodities.getOrDefault(Commodities.SHIPS, 0f)).sum();

        final int titleW = 220;
        final int entryW = 100;
        final int entryH = 32;

        final DockButton<ShipCommissionDialog> commissionBtn = new DockButton<>(
            m_panel, 120, entryH, "Order", null, () -> new ShipCommissionDialog(this)
        );
        commissionBtn.cutStyle = CutStyle.ALL;
        commissionBtn.setShortcutAndAppendToText(Keyboard.KEY_A);
        add(commissionBtn).inTR(BUTTON_W, hpad);

        final Button clearAllBtn = new Button(m_panel, 120, entryH, "Clear All", null, (btn) -> {
            new ClearAllDialog(this).show(0.3f, 0.3f);
        });
        clearAllBtn.cutStyle = CutStyle.ALL;
        clearAllBtn.setEnabled(orders.size() > 0);
        add(clearAllBtn).inTR(BUTTON_W*2, hpad);

        final LabelAPI title = settings.createLabel("Vessels on Order", Fonts.INSIGNIA_VERY_LARGE);
        add(title).inTL(hpad, hpad).setSize(titleW, entryH);
        title.setAlignment(Alignment.LMID);

        final IconValuePairTp ordersPair = new IconValuePairTp(m_panel, entryW, entryH, CHECKLIST, orders.size(), true, null);
        final IconValuePairTp costPair = new IconValuePairTp(m_panel, entryW, entryH, WAGES, totalCost, false, null);
        final IconValuePairTp timePair = new IconValuePairTp(m_panel, entryW, entryH, STOPWATCH, totalTime, false, null);
        final IconValuePairTp shipsPair = new IconValuePairTp(m_panel, entryW, entryH, SHIPS, totalShips, false, null);
        costPair.label().setText(costPair.label().getText() + Strings.C);

        add(ordersPair).inTL(hpad + titleW, hpad);
        add(costPair).inTL(hpad + titleW + entryW, hpad);
        add(timePair).inTL(hpad + titleW + entryW*2, hpad);
        add(shipsPair).inTL(hpad + titleW + entryW*3, hpad);

        ordersPair.tooltip.builder = (tp, expanded) -> {
            tp.addTitle("Pending Orders", base);
            tp.addPara("The number of ship orders awaiting resource allocation. Once the required commodities are available, they will be moved to the production queue.", pad);
        };
        costPair.tooltip.builder = (tp, expanded) -> {
            tp.addTitle("Total Credit Cost", base);
            tp.addPara("The sum of credits required to fulfill all pending orders. This amount will be deducted from the faction's capital as each order is activated.", pad);
        };
        timePair.tooltip.builder = (tp, expanded) -> {
            tp.addTitle("Total Build Time", base);
            tp.addPara("The cumulative number of days needed to construct all pending vessels, assuming they are built sequentially. Actual completion may be shorter (up to %s production lines may operate concurrently).", pad, highlight, String.valueOf(StaticData.inv.getAssemblyLines()));
        };
        shipsPair.tooltip.builder = (tp, expanded) -> {
            tp.addTitle("Ships Commodity Cost", base);
            tp.addPara("The total amount of the \"Ships\" commodity required across all pending orders. This resource represents the hull components and prefabricated sections needed for construction.", pad);
        };

        ordersPair.tooltip.positioner = (tp, exp) -> NativeUiUtils.anchorPanel(tp, ordersPair.getPanel(), AnchorType.RightTop, hpad);
        costPair.tooltip.positioner = (tp, exp) -> NativeUiUtils.anchorPanel(tp, costPair.getPanel(), AnchorType.RightTop, hpad);
        timePair.tooltip.positioner = (tp, exp) -> NativeUiUtils.anchorPanel(tp, timePair.getPanel(), AnchorType.RightTop, hpad);
        shipsPair.tooltip.positioner = (tp, exp) -> NativeUiUtils.anchorPanel(tp, shipsPair.getPanel(), AnchorType.RightTop, hpad);

        if (DebugFlags.COLONY_DEBUG) {
            final DockButton<FactionSelectionDialog> factionSelection = new DockButton<>(
                m_panel, 120, 28, "Pick Faction", null, () -> new FactionSelectionDialog(this)
            );
            factionSelection.cutStyle = CutStyle.ALL;
            add(factionSelection).inTR(hpad, hpad);
        }

        grid = new PlannedOrderGrid(m_panel, (int) pos.getWidth(), (int) (pos.getHeight() - HEADER_HEIGHT));
        add(grid).inTL(0, HEADER_HEIGHT);
    }

    public class PlannedOrderGrid extends GridTable<PlannedOrder, PlannedOrderWidget> {

        public PlannedOrderGrid(UIPanelAPI parent, int width, int height) {
            super(parent, width, height, PlannedOrderWidget.WIDTH, PlannedOrderWidget.HEIGHT, opad*2);
            uniformOuterGap = true;
            justifyGrid = true;
            isSelectionEnabled = true;
            buildUI();
        }

        public final void clearSelection() {
            for (PlannedOrderWidget w : grid.widgets) {
                w.selectionState = WidgetSelectionState.NONE;
                w.buildUI();
            }
            selectedWidget = null;
        }

        protected List<PlannedOrder> getDataList() {
            List<PlannedOrder> orders = StaticData.inv.getPlannedOrders();
            if (orders.size() > EconConfig.MAX_VISIBLE_PLANNED_ORDERS) {
                orders = orders.subList(0, EconConfig.MAX_VISIBLE_PLANNED_ORDERS);
            }
            return orders;
        }

        protected PlannedOrderWidget createWidget(PlannedOrder item, int index) {
            return new PlannedOrderWidget(PlannedOrdersPanel.this, item, index);
        }

        protected void onWidgetClicked(PlannedOrderWidget source) {
            switch (source.selectionState) {
            case NONE:
                if (NativeUiUtils.isShiftDown()) {
                    StaticData.inv.removePlannedOrder(source.index);
                    buildUI();
                    break;
                }
                source.selectionState = WidgetSelectionState.REMOVE;
                source.buildUI();
                selectedWidget = source;
                for (PlannedOrderWidget widget : widgets) {
                    if (widget == source) continue;

                    widget.selectionState = WidgetSelectionState.SWAP;
                    widget.buildUI();
                }
                break;

            case REMOVE:
                StaticData.inv.removePlannedOrder(source.index);
                buildUI();
                break;

            case SWAP:
                StaticData.inv.swapPlannedOrders(source.index, selectedWidget.index);
                NativeUiUtils.swapPositions(source.getPanel(), selectedWidget.getPanel());

                clearSelection();
                break;
            }
        }

        protected String getEmptyMessage() {
            return "No planned orders";
        }
    }
}
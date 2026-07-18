package wfg.ltv_econ.ui.factionTab;

import static wfg.ltv_econ.constant.Sprites.CHECKLIST;
import static wfg.ltv_econ.constant.Sprites.SHIPS;
import static wfg.ltv_econ.constant.Sprites.STOPWATCH;
import static wfg.ltv_econ.constant.Sprites.WAGES;
import static wfg.ltv_econ.constant.strings.LocalizedStrings.*;
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

import wfg.ltv_econ.config.EconConfig;
import wfg.ltv_econ.economy.engine.EconomyEngine;
import wfg.ltv_econ.economy.fleet.PlannedOrder;
import wfg.ltv_econ.serializable.StaticData;
import wfg.ltv_econ.ui.factionTab.dialog.ClearAllDialog;
import wfg.ltv_econ.ui.factionTab.dialog.FactionSelectionDialog;
import wfg.ltv_econ.ui.factionTab.dialog.ShipCommissionDialog;
import wfg.ltv_econ.ui.fleet.PlannedOrderWidget;
import wfg.ltv_econ.ui.reusable.WidgetSelectionState;
import wfg.native_ui.internal.ui.core.UIContainer;
import wfg.native_ui.ui.component.InteractionComp;
import wfg.native_ui.ui.component.NativeComponents;
import wfg.native_ui.ui.core.UIBuildableAPI;
import wfg.native_ui.ui.core.UIElementFlags.HasInteraction;
import wfg.native_ui.ui.functional.Button;
import wfg.native_ui.ui.functional.DockButton;
import wfg.native_ui.ui.functional.Button.CutStyle;
import wfg.native_ui.ui.table.GridTable;
import wfg.native_ui.ui.visual.IconValuePairTp;
import wfg.native_ui.util.NativeUiUtils;
import wfg.native_ui.util.NativeUiUtils.AnchorType;

public final class PlannedOrdersPanel extends UIContainer implements UIBuildableAPI, HasInteraction {
    private static final int HEADER_HEIGHT = 50;

    private final InteractionComp<PlannedOrdersPanel> interaction = comp().get(NativeComponents.INTERACTION); 

    public PlannedOrderGrid grid;
    
    public PlannedOrdersPanel(int w, int h) {
        super(w, h);

        interaction.onClicked = (panel, isLeftClick) -> {
            grid.clearSelection();
        };

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

        final List<PlannedOrder> orders = StaticData.inv.getPlannedOrders();
        final long totalCost = orders.stream().mapToLong(o -> o.credits).sum();
        final int totalTime = orders.stream().mapToInt(o -> o.days).sum();
        final double totalShips = orders.stream().mapToDouble(o -> o.commodities.getOrDefault(Commodities.SHIPS, 0f)).sum();

        final int titleW = 220;
        final int entryW = 100;
        final int entryH = 32;

        final DockButton<ShipCommissionDialog> commissionBtn = new DockButton<>(
            120, entryH, str("uiBtnTitleHullOrder"), null, () -> new ShipCommissionDialog(this)
        );
        commissionBtn.setCutStyle(CutStyle.ALL);
        commissionBtn.setShortcutAndAppendToText(Keyboard.KEY_T);
        add(commissionBtn).inTR(BUTTON_W, hpad);

        final Button clearAllBtn = new Button(120, entryH, str("uiBtnTitleClearAll"), null, (btn) -> {
            new ClearAllDialog(this).show(0.3f, 0.3f);
        });
        clearAllBtn.setCutStyle(CutStyle.ALL);
        clearAllBtn.setEnabled(orders.size() > 0);
        add(clearAllBtn).inTR(BUTTON_W*2, hpad);

        final LabelAPI title = settings.createLabel(str("uiTitleOrderedHulls"), Fonts.INSIGNIA_VERY_LARGE);
        add(title).inTL(hpad, hpad).setSize(titleW, entryH);
        title.setAlignment(Alignment.LMID);

        final IconValuePairTp ordersPair = new IconValuePairTp(entryW, entryH, CHECKLIST, orders.size(), true, null);
        final IconValuePairTp costPair = new IconValuePairTp(entryW, entryH, WAGES, totalCost, false, null);
        final IconValuePairTp timePair = new IconValuePairTp(entryW, entryH, STOPWATCH, totalTime, true, null);
        final IconValuePairTp shipsPair = new IconValuePairTp(entryW, entryH, SHIPS, totalShips, true, null);
        costPair.label().setText(costPair.label().getText() + Strings.C);

        add(ordersPair).inTL(hpad + titleW, hpad);
        add(costPair).inTL(hpad + titleW + entryW, hpad);
        add(timePair).inTL(hpad + titleW + entryW*2, hpad);
        add(shipsPair).inTL(hpad + titleW + entryW*3, hpad);

        ordersPair.tooltip.builder = (tp, expanded) -> {
            tp.addTitle(str("uiTitlePendingOrders"), base);
            tp.addPara(str("uiTpTxtPendingOrders"), pad);
        };
        costPair.tooltip.builder = (tp, expanded) -> {
            tp.addTitle(str("uiTitleTotalCreditCost"), base);
            tp.addPara(str("uiTpTitleTotalCreditCostPendingOrders"), pad);
        };
        timePair.tooltip.builder = (tp, expanded) -> {
            tp.addTitle(str("uiTitleHullsTotalBuildTime"), base);
            tp.addPara(str("uiTpTxtActiveHullsTotalBuildTime"), pad, highlight, String.valueOf(StaticData.inv.getAssemblyLines()));
        };
        shipsPair.tooltip.builder = (tp, expanded) -> {
            tp.addTitle(str("uiTitleShipsCommodityCost"), base);
            tp.addPara(str("uiTpTxtShipsCommodityCost"), pad);
        };

        ordersPair.tooltip.positioner = (tp, exp) -> NativeUiUtils.anchorPanel(tp, ordersPair, AnchorType.RightTop, hpad);
        costPair.tooltip.positioner = (tp, exp) -> NativeUiUtils.anchorPanel(tp, costPair, AnchorType.RightTop, hpad);
        timePair.tooltip.positioner = (tp, exp) -> NativeUiUtils.anchorPanel(tp, timePair, AnchorType.RightTop, hpad);
        shipsPair.tooltip.positioner = (tp, exp) -> NativeUiUtils.anchorPanel(tp, shipsPair, AnchorType.RightTop, hpad);

        if (DebugFlags.COLONY_DEBUG) {
            final DockButton<FactionSelectionDialog> factionSelection = new DockButton<>(
                120, 28, str("uiBtnTitlePickFaction"), null, () -> new FactionSelectionDialog(this)
            );
            factionSelection.setCutStyle(CutStyle.ALL);
            add(factionSelection).inTR(hpad, hpad);
        }

        grid = new PlannedOrderGrid((int) getWidth(), (int) (getHeight() - HEADER_HEIGHT));
        add(grid).inTL(0, HEADER_HEIGHT);
    }

    public class PlannedOrderGrid extends GridTable<PlannedOrder, PlannedOrderWidget> {

        public PlannedOrderGrid(int width, int height) {
            super(width, height, PlannedOrderWidget.WIDTH, PlannedOrderWidget.HEIGHT, opad*2);
            uniformOuterGap = true;
            justifyGrid = true;
            isSelectionEnabled = true;
            buildUI();
        }

        public final void clearSelection() {
            for (PlannedOrderWidget w : widgets) {
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
            return new PlannedOrderWidget(item, index);
        }

        protected void onWidgetClicked(PlannedOrderWidget source) {
            switch (source.selectionState) {
            case NONE:
                if (NativeUiUtils.isShiftDown()) {
                    StaticData.inv.removePlannedOrder(source.index);
                    PlannedOrdersPanel.this.buildUI();
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
                PlannedOrdersPanel.this.buildUI();
                break;

            case SWAP:
                StaticData.inv.swapPlannedOrders(source.index, selectedWidget.index);
                NativeUiUtils.swapPositions(source, selectedWidget);

                clearSelection();
                break;
            }
        }

        protected String getEmptyMessage() {
            return str("uiTitleNoPlannedOrders");
        }
    }
}
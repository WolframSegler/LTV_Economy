package wfg.ltv_econ.ui.factionTab;

import static wfg.ltv_econ.constants.Sprites.CHECKLIST;
import static wfg.ltv_econ.constants.Sprites.STOPWATCH;
import static wfg.native_ui.util.Globals.settings;
import static wfg.native_ui.util.UIConstants.*;

import java.util.List;

import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.impl.campaign.DebugFlags;
import com.fs.starfarer.api.ui.Alignment;
import com.fs.starfarer.api.ui.Fonts;
import com.fs.starfarer.api.ui.LabelAPI;
import com.fs.starfarer.api.ui.UIPanelAPI;

import wfg.ltv_econ.config.EconConfig;
import wfg.ltv_econ.economy.engine.EconomyEngine;
import wfg.ltv_econ.economy.fleet.ShipProductionManager;
import wfg.ltv_econ.economy.fleet.ShipProductionOrder;
import wfg.ltv_econ.serializable.StaticData;
import wfg.ltv_econ.ui.factionTab.dialog.DiscardAllDialog;
import wfg.ltv_econ.ui.factionTab.dialog.FactionSelectionDialog;
import wfg.ltv_econ.ui.fleet.ShipProductionWidget;
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

public class ActiveQueuePanel extends CustomPanel implements UIBuildableAPI, HasInteraction {
    private static final SpriteAPI PRODUCTION = settings.getSprite("income_report", "production");
    private static final int HEADER_HEIGHT = 50;

    private final InteractionComp<ActiveQueuePanel> interaction = comp().get(NativeComponents.INTERACTION); 

    private ActiveQueueGrid grid;

    public ActiveQueuePanel(UIPanelAPI parent, int w, int h) {
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

        final int prodLines = StaticData.inv.getAssemblyLines();
        final int titleW = 220;
        final int entryW = 100;
        final int entryH = 32;

        final List<ShipProductionOrder> orders = StaticData.inv.getActiveProductionQueue();
        final int totalTime = orders.stream().mapToInt(o -> o.daysRemaining).sum();
        final int estimatedTime = prodLines < 1 ? 0 : totalTime / prodLines;

        final Button clearAllBtn = new Button(m_panel, 120, entryH, "Clear All", null, (btn) -> {
            new DiscardAllDialog(this).show(0.3f, 0.3f);
        });
        clearAllBtn.cutStyle = CutStyle.ALL;
        clearAllBtn.setEnabled(orders.size() > 0);
        add(clearAllBtn).inTR(BUTTON_W, hpad);

        final LabelAPI title = settings.createLabel("Assembly Line", Fonts.INSIGNIA_VERY_LARGE);
        add(title).inTL(hpad, hpad).setSize(titleW, entryH);
        title.setAlignment(Alignment.LMID);

        final IconValuePairTp ordersPair = new IconValuePairTp(m_panel, entryW, entryH, CHECKLIST, orders.size(), true, null);
        final IconValuePairTp timePair = new IconValuePairTp(m_panel, entryW, entryH, STOPWATCH, estimatedTime, true, null);
        final IconValuePairTp prodPair = new IconValuePairTp(m_panel, entryW, entryH, PRODUCTION, prodLines, true, null);

        add(ordersPair).inTL(hpad + titleW, hpad);
        add(timePair).inTL(hpad + titleW + entryW, hpad);
        add(prodPair).inTL(hpad + titleW + entryW*2, hpad);

        ordersPair.tooltip.builder = (tp, expanded) -> {
            tp.addTitle("Active Orders", base);
            tp.addPara("Number of ships on the production queue.", pad);
        };
        timePair.tooltip.builder = (tp, expanded) -> {
            tp.addTitle("Total Build Time", base);
            tp.addPara("Estimated number of days needed to construct all queued vessels assuming all the assembly lines are utilized.", pad, highlight, String.valueOf(StaticData.inv.getAssemblyLines()));
        };
        prodPair.tooltip.builder = (tp, expanded) -> {
            tp.addTitle("Assembly Lines", base);
            tp.addPara("Number of assembly lines that can concurrently produce ships. Can be increased with policies.", pad, highlight, String.valueOf(StaticData.inv.getAssemblyLines()));
        };

        ordersPair.tooltip.positioner = (tp, exp) -> NativeUiUtils.anchorPanel(tp, ordersPair.getPanel(), AnchorType.RightTop, hpad);
        timePair.tooltip.positioner = (tp, exp) -> NativeUiUtils.anchorPanel(tp, timePair.getPanel(), AnchorType.RightTop, hpad);
        prodPair.tooltip.positioner = (tp, exp) -> NativeUiUtils.anchorPanel(tp, prodPair.getPanel(), AnchorType.RightTop, hpad);

        if (DebugFlags.COLONY_DEBUG) {
            final DockButton<FactionSelectionDialog> factionSelection = new DockButton<>(
                m_panel, 120, 28, "Pick Faction", null, () -> new FactionSelectionDialog(this)
            );
            factionSelection.cutStyle = CutStyle.ALL;
            add(factionSelection).inTR(hpad, hpad);
        }

        grid = new ActiveQueueGrid(m_panel, (int) pos.getWidth(), (int) (pos.getHeight() - HEADER_HEIGHT));
        add(grid).inTL(0, HEADER_HEIGHT);
    }

    private class ActiveQueueGrid extends GridTable<ShipProductionOrder, ShipProductionWidget> {
        public ActiveQueueGrid(UIPanelAPI parent, int width, int height) {
            super(parent, width, height, ShipProductionWidget.WIDTH, ShipProductionWidget.HEIGHT, opad*2);
            uniformOuterGap = true;
            justifyGrid = false;
            isSelectionEnabled = true;
            buildUI();
        }

        protected List<ShipProductionOrder> getDataList() {
            List<ShipProductionOrder> orders = StaticData.inv.getActiveProductionQueue();
            if (orders.size() > EconConfig.MAX_VISIBLE_PLANNED_ORDERS) {
                orders = orders.subList(0, EconConfig.MAX_VISIBLE_PLANNED_ORDERS);
            }
            return orders;
        }

        protected ShipProductionWidget createWidget(ShipProductionOrder item, int index) {
            return new ShipProductionWidget(m_panel, item, index);
        }

        @Override
        protected void onWidgetClicked(ShipProductionWidget source) {
            switch (source.selectionState) {
            case NONE:
                if (NativeUiUtils.isShiftDown()) {
                    StaticData.inv.removeActiveOrder(source.index);
                    ActiveQueuePanel.this.buildUI();
                    break;
                }
                source.selectionState = WidgetSelectionState.REMOVE;
                source.buildUI();
                selectedWidget = source;
                for (ShipProductionWidget widget : widgets) {
                    if (widget == source) continue;

                    widget.selectionState = WidgetSelectionState.SWAP;
                    widget.buildUI();
                }
                break;

            case REMOVE:
                ShipProductionManager.addScrapsToCapital(StaticData.inv, 1, source.spec);
                StaticData.inv.removeActiveOrder(source.index);
                ActiveQueuePanel.this.buildUI();
                break;

            case SWAP:
                StaticData.inv.swapActiveOrders(source.index, selectedWidget.index);
                NativeUiUtils.swapPositions(source.getPanel(), selectedWidget.getPanel());

                source.buildUI();
                selectedWidget.buildUI();

                clearSelection();
                break;
            }
        }

        protected String getEmptyMessage() {
            return "No ships in production";
        }

        public final void clearSelection() {
            for (ShipProductionWidget w : grid.widgets) {
                w.selectionState = WidgetSelectionState.NONE;
                w.buildUI();
            }
            selectedWidget = null;
        }
    }
}
package wfg.ltv_econ.ui.fleet;

import static wfg.native_ui.util.UIConstants.*;
import static wfg.native_ui.util.Globals.settings;

import java.awt.Color;

import com.fs.starfarer.api.campaign.FactionSpecAPI;
import com.fs.starfarer.api.campaign.econ.CommoditySpecAPI;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import com.fs.starfarer.api.impl.campaign.ids.Strings;
import com.fs.starfarer.api.ui.Alignment;
import com.fs.starfarer.api.ui.Fonts;
import com.fs.starfarer.api.ui.LabelAPI;
import com.fs.starfarer.api.ui.PositionAPI;
import com.fs.starfarer.api.ui.UIPanelAPI;

import wfg.ltv_econ.economy.fleet.FactionShipInventory;
import wfg.ltv_econ.economy.fleet.PlannedOrder;
import wfg.ltv_econ.ui.reusable.WidgetSelectionState;
import wfg.native_ui.internal.util.BorderRenderer;
import wfg.native_ui.ui.component.NativeComponents;
import wfg.native_ui.ui.component.TooltipComp;
import wfg.native_ui.ui.core.UIBuildableAPI;
import wfg.native_ui.ui.functional.UIClickable;
import wfg.native_ui.ui.panel.BasePanel;
import wfg.native_ui.ui.visual.IconValuePair;
import wfg.native_ui.ui.visual.SpritePanel.Base;
import wfg.native_ui.util.NativeUiUtils;
import wfg.native_ui.util.NumFormat;

public class PlannedOrderWidget extends UIClickable<PlannedOrderWidget> implements UIBuildableAPI {
    private static final int WIDTH = 450;
    private static final int HEIGHT = 60;

    private final TooltipComp tooltip = comp().get(NativeComponents.TOOLTIP);
    private final BorderRenderer border = new BorderRenderer(UI_BORDER_4, true, WIDTH, HEIGHT);

    private final PlannedOrder order;
    private final ShipHullSpecAPI spec;

    public WidgetSelectionState selectionState = WidgetSelectionState.NONE;

    public PlannedOrderWidget(UIPanelAPI parent, PlannedOrder order, FactionShipInventory inv) {
        super(parent, WIDTH, HEIGHT, null);

        onClicked = (btn) -> {
            switch (selectionState) {
            case NONE:
                selectionState = WidgetSelectionState.REMOVE;
                break;

            case SWAP:
                // TODO notify parent it was clicked for swapping.
                break;

            case REMOVE:
                // TODO rebuild parent UI after removing itself from the data list
                break;
            }
        };

        spec = settings.getHullSpec(order.hullId);

        this.order = order;
        tooltip.builder = (tp, expanded) -> {
            final FactionSpecAPI faction = settings.getFactionSpec(inv.factionID);
            tp.addTitle("Ship Order", base);

            tp.addPara(
                "The " + spec.getHullNameWithDashClass() + " production order will enter the queue once the required resources " +
                "are allocated. A total cost of %s will be charged to the faction's capital, %s, and construction " +
                "is expected to take %s before the ship is ready for service.",
                pad, new Color[]{highlight, faction.getBaseUIColor(), highlight},
                NumFormat.formatCreditAbs(order.credits), inv.getCapital().getName(),
                order.days + (order.days == 1 ? " day" : " days")
            );

            final int gridWidth = 390;
            final int valueWidth = 40;
            int rowCount = 0;

            tp.addPara("Resource List", base, opad);
            tp.beginGridFlipped(gridWidth, 2, valueWidth, hpad);
            for (var e : order.commodities.singleEntrySet()) {
                final CommoditySpecAPI com = settings.getCommoditySpec(e.getKey());
                final String amountStr = Strings.X + NumFormat.engNotate(e.getValue());

                tp.addToGrid(0, rowCount++, com.getName(), amountStr);
            }
            tp.addGrid(0);
        };

        buildUI();
    }

    @Override
    public void buildUI() {
        clearChildren();

        final int shipS = HEIGHT - opad;
        final int CONTENT_W = WIDTH - HEIGHT;
        final Base shipSprite = new Base(m_panel, shipS, shipS, spec.getSpriteName(), null, null);
        add(shipSprite).inBL(hpad, hpad);

        final String shipStr = spec.getHullNameWithDashClass();
        final String costStr = NumFormat.formatCredit(order.credits);
        final String timeStr = order.days + (order.days <= 1 ? " Day" : " Days");
        final String gapStr = " - ";
        final LabelAPI topSection = settings.createLabel(
            shipStr + gapStr + costStr + gapStr + timeStr, Fonts.DEFAULT_SMALL
        );
        topSection.setHighlightColors(highlight, base);
        topSection.setHighlight(costStr, timeStr);
        topSection.setAlignment(Alignment.LMID);
        topSection.autoSizeToWidth(CONTENT_W);
        add(topSection).inTL(HEIGHT, hpad);

        final int iconS = 28;
        final int pairW = iconS + 80; 
        int currW = HEIGHT + opad;
        for (var e : order.commodities.singleEntrySet()) {
            final IconValuePair pair = new IconValuePair(m_panel, pairW, iconS, e.getKey(), e.getValue(),
                true, null
            );

            add(pair).inBL(currW, hpad);
            currW += pairW + opad;

            if (currW > WIDTH) {
                remove(pair);
                break;
            }
        }
    
        addSelectionUI(shipSprite.getPanel(), selectionState);
    }

    @Override
    public void renderBelow(float alpha) {
        super.renderBelow(alpha);

        border.render(pos.getX(), pos.getY(), alpha);
    }

    public static final void addSelectionUI(UIPanelAPI container, WidgetSelectionState state) {
        if (state != WidgetSelectionState.NONE) {
            final PositionAPI cPos = container.getPosition();
            final BasePanel bgPanel = new BasePanel(container, (int) cPos.getWidth(), (int) cPos.getHeight());
            container.addComponent(bgPanel.getPanel());
            bgPanel.bg.alpha = 0.8f;
            bgPanel.bg.color = dark;

            if (state == WidgetSelectionState.REMOVE) {
                final LabelAPI removeLabel = settings.createLabel("Click to remove", Fonts.DEFAULT_SMALL);
                removeLabel.setColor(base);
                removeLabel.setHighlightColor(
                    NativeUiUtils.adjustBrightness(removeLabel.getColor(), 1.33f)
                );
                bgPanel.add(removeLabel).inMid();
    
            } else if (state == WidgetSelectionState.SWAP) {
                final LabelAPI swapLabel = settings.createLabel("Click to swap", Fonts.DEFAULT_SMALL);
                swapLabel.setColor(base);
                swapLabel.setHighlightColor(
                    NativeUiUtils.adjustBrightness(swapLabel.getColor(), 1.33f)
                );
                bgPanel.add(swapLabel).inMid();
            }
        }
    }
}
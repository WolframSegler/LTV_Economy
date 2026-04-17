package wfg.ltv_econ.ui.fleet;

import static wfg.native_ui.util.UIConstants.*;
import static wfg.native_ui.util.Globals.settings;

import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import com.fs.starfarer.api.ui.Alignment;
import com.fs.starfarer.api.ui.Fonts;
import com.fs.starfarer.api.ui.LabelAPI;
import com.fs.starfarer.api.ui.UIPanelAPI;

import wfg.ltv_econ.economy.fleet.ShipProductionOrder;
import wfg.ltv_econ.ui.reusable.WidgetSelectionState;
import wfg.native_ui.internal.util.BorderRenderer;
import wfg.native_ui.ui.component.NativeComponents;
import wfg.native_ui.ui.component.TooltipComp;
import wfg.native_ui.ui.core.UIBuildableAPI;
import wfg.native_ui.ui.functional.UIClickable;
import wfg.native_ui.ui.visual.SpritePanel.Base;
import wfg.native_ui.ui.widget.Slider;

public class ShipProductionWidget extends UIClickable<ShipProductionWidget> implements UIBuildableAPI {
    private static final int WIDTH = 450;
    private static final int HEIGHT = 60;

    private final TooltipComp tooltip = comp().get(NativeComponents.TOOLTIP);
    private final BorderRenderer border = new BorderRenderer(UI_BORDER_4, true, WIDTH, HEIGHT);

    private final ShipProductionOrder order;
    private final ShipHullSpecAPI spec;

    public WidgetSelectionState selectionState = WidgetSelectionState.NONE;

    public ShipProductionWidget(UIPanelAPI parent, ShipProductionOrder order, boolean isBeingProduced) {
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
            tp.addTitle("Production Item", base);

            final String statusStr = "The " + spec.getHullNameWithDashClass() + (isBeingProduced ?
                " is being assembled in the shipyards." :
                " awaits its turn in the production queue.");
            tp.addPara(
                statusStr + " The production will be completed in %s. Cancelling a production line does not refund the expended resources.",
                pad, highlight, order.daysRemaining + (order.daysRemaining <= 1 ? " Day" : " Days")
            );
        };

        buildUI();
    }

    @Override
    public void buildUI() {
        final int shipS = HEIGHT - opad;
        final int CONTENT_W = WIDTH - HEIGHT;
        final Base shipSprite = new Base(m_panel, shipS, shipS, spec.getSpriteName(), null, null);
        add(shipSprite).inBL(hpad, hpad);

        final String shipStr = spec.getHullNameWithDashClass();
        final String timeStr = order.daysRemaining + (order.daysRemaining <= 1 ? " Day" : " Days");
        final LabelAPI topSection = settings.createLabel(
            shipStr + "  -  Remaining: " + timeStr, Fonts.DEFAULT_SMALL
        );
        topSection.setHighlightColor(base);
        topSection.setHighlight(timeStr);
        topSection.setAlignment(Alignment.LMID);
        topSection.autoSizeToWidth(CONTENT_W);
        add(topSection).inTL(HEIGHT, hpad);

        final String sliderTxt = order.daysRemaining + "d / " + order.days + "d";
        final Slider timeSlider = new Slider(m_panel, sliderTxt, 0f, order.days, CONTENT_W - opad, 32);
        timeSlider.showLabelOnly = true;
        timeSlider.setUserAdjustable(false);
        timeSlider.setProgress(order.days - order.daysRemaining);
        add(timeSlider).inBL(HEIGHT, hpad);

        PlannedOrderWidget.addSelectionUI(shipSprite.getPanel(), selectionState);
    }

    @Override
    public void renderBelow(float alpha) {
        super.renderBelow(alpha);

        border.render(pos.getX(), pos.getY(), alpha);
    }
}
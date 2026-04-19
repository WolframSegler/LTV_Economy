package wfg.ltv_econ.ui.fleet;

import static wfg.native_ui.util.UIConstants.*;
import static wfg.native_ui.util.Globals.settings;

import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.ui.Alignment;
import com.fs.starfarer.api.ui.Fonts;
import com.fs.starfarer.api.ui.LabelAPI;
import com.fs.starfarer.api.ui.UIComponentAPI;
import com.fs.starfarer.api.ui.UIPanelAPI;

import wfg.ltv_econ.constants.UIColors;
import wfg.ltv_econ.economy.fleet.ShipProductionOrder;
import wfg.ltv_econ.serializable.StaticData;
import wfg.ltv_econ.ui.reusable.WidgetSelectionState;
import wfg.native_ui.internal.util.BorderRenderer;
import wfg.native_ui.ui.component.HoverGlowComp;
import wfg.native_ui.ui.component.HoverGlowComp.GlowType;
import wfg.native_ui.ui.component.InteractionComp;
import wfg.native_ui.ui.component.NativeComponents;
import wfg.native_ui.ui.component.TooltipComp;
import wfg.native_ui.ui.core.UIElementFlags.HasHoverGlow;
import wfg.native_ui.ui.core.UIElementFlags.HasTooltip;
import wfg.native_ui.ui.functional.UIClickable;
import wfg.native_ui.ui.table.WidgetAPI;
import wfg.native_ui.ui.visual.SpritePanelWithTp;
import wfg.native_ui.ui.widget.Slider;

public class ShipProductionWidget extends UIClickable<ShipProductionWidget> implements WidgetAPI<ShipProductionWidget>,
    HasTooltip, HasHoverGlow
{
    public static final int WIDTH = 460;
    public static final int HEIGHT = 60;

    private final TooltipComp tooltip = comp().get(NativeComponents.TOOLTIP);
    private final HoverGlowComp glow = comp().get(NativeComponents.HOVER_GLOW);
    private final BorderRenderer border = new BorderRenderer(UI_BORDER_4, true, WIDTH, HEIGHT);

    private final ShipProductionOrder order;
    private final ShipHullSpecAPI spec;

    public final int index;

    public WidgetSelectionState selectionState = WidgetSelectionState.NONE;

    public ShipProductionWidget(UIPanelAPI parent, ShipProductionOrder order, int index) {
        super(parent, WIDTH, HEIGHT, null);

        this.order = order;
        this.index = index;
        spec = settings.getHullSpec(order.hullId);

        border.centerColor = InventoryShipWidget.WIDGET_BG;

        glow.type = GlowType.UNDERLAY;
        glow.overlayBrightness = 0.6f;
        glow.color = UIColors.IN_FACTION;

        final boolean isBeingProduced = index <= StaticData.inv.getAssemblyLines() - 1;

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
        final SpriteAPI sprite = settings.getSprite(spec.getSpriteName());

        final int CONTENT_W = WIDTH - HEIGHT;
        final int maxSize = HEIGHT - opad;
        final float spriteW = sprite.getWidth();
        final float spriteH = sprite.getHeight();
        final float scale = Math.min(maxSize / spriteW, maxSize / spriteH);
        final int scaledW = (int) (spriteW * scale);
        final int scaledH = (int) (spriteH * scale);

        final SpritePanelWithTp shipSprite = new SpritePanelWithTp(m_panel, scaledW, scaledH, sprite, null, null);
        shipSprite.tooltip.enabled = false;
        shipSprite.audio.enabled = false;
        shipSprite.glow.isFaderOwner = false;
        shipSprite.glow.fader = glow.fader;
        shipSprite.glow.type = GlowType.ADDITIVE;
        shipSprite.glow.additiveBrightness = 0.8f;
        add(shipSprite).inLMid(hpad + (maxSize - scaledW) / 2);

        final String shipStr = spec.getHullNameWithDashClass();
        final String timeStr = order.daysRemaining + (order.daysRemaining <= 1 ? " Day" : " Days");
        final LabelAPI topSection = settings.createLabel(
            shipStr + "  •  Remaining: " + timeStr, Fonts.DEFAULT_SMALL
        );
        topSection.setHighlightColor(base);
        topSection.setHighlight(timeStr);
        topSection.setAlignment(Alignment.LMID);
        topSection.autoSizeToWidth(CONTENT_W);
        add(topSection).inTL(HEIGHT, hpad);

        final boolean isBeingProduced = index <= StaticData.inv.getAssemblyLines() - 1;

        final String sliderTxt = isBeingProduced ? order.daysRemaining + "d / " + order.days + "d" : "Waiting";
        final Slider timeSlider = new Slider(m_panel, sliderTxt, 0f, order.days, CONTENT_W - opad, 32);
        timeSlider.showLabelOnly = true;
        timeSlider.setUserAdjustable(false);
        timeSlider.setProgress(isBeingProduced ? order.days - order.daysRemaining : order.days);
        if (!isBeingProduced) timeSlider.setBarColor(UIColors.CARGO_COLOR);
        add(timeSlider).inBL(HEIGHT, hpad);

        PlannedOrderWidget.addSelectionUI(m_panel, selectionState);
    }

    @Override
    public void renderBelow(float alpha) {
        super.renderBelow(alpha);

        border.render(pos.getX(), pos.getY(), alpha);
    }

    public UIComponentAPI getElement() {
        return m_panel;
    }

    public InteractionComp<ShipProductionWidget> getInteraction() {
        return interaction;
    }
}
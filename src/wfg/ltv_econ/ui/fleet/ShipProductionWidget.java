package wfg.ltv_econ.ui.fleet;

import static wfg.native_ui.util.UIConstants.*;
import static wfg.ltv_econ.constant.strings.LocalizedStrings.*;
import static wfg.native_ui.util.Globals.settings;

import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.ui.Alignment;
import com.fs.starfarer.api.ui.Fonts;
import com.fs.starfarer.api.ui.LabelAPI;

import wfg.ltv_econ.constant.UIColors;
import wfg.ltv_econ.economy.fleet.ShipProductionOrder;
import wfg.ltv_econ.serializable.StaticData;
import wfg.ltv_econ.ui.reusable.WidgetSelectionState;
import wfg.ltv_econ.util.UIUtils;
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
import wfg.native_ui.ui.visual.InteractiveSprite;
import wfg.native_ui.ui.widget.Slider;

public final class ShipProductionWidget extends UIClickable<ShipProductionWidget> implements WidgetAPI<ShipProductionWidget>,
    HasTooltip, HasHoverGlow
{
    public static final int WIDTH = 460;
    public static final int HEIGHT = 60;

    private final TooltipComp tooltip = comp().get(NativeComponents.TOOLTIP);
    private final HoverGlowComp glow = comp().get(NativeComponents.HOVER_GLOW);
    private final BorderRenderer border = new BorderRenderer(UI_BORDER_4, true, WIDTH, HEIGHT);

    public final ShipHullSpecAPI spec;
    public final ShipProductionOrder order;
    public final int index;

    public WidgetSelectionState selectionState = WidgetSelectionState.NONE;

    public ShipProductionWidget(ShipProductionOrder order, int index) {
        super(WIDTH, HEIGHT, null);

        this.order = order;
        this.index = index;
        spec = settings.getHullSpec(order.hullId);

        border.centerColor = UIColors.WIDGET_BG;

        glow.type = GlowType.UNDERLAY;
        glow.glowBrightness = 0.6f;
        glow.flashBrightness = 1.2f;
        glow.color = UIColors.IN_FACTION;

        final boolean isBeingProduced = index <= StaticData.inv.getAssemblyLines() - 1;

        tooltip.builder = (tp, expanded) -> {
            tp.addTitle(str("productionItemTitle"), base);

            final String statusStr = strf(isBeingProduced ? "shipProductionItemTpTxt1" : "shipProductionItemTpTxt2", spec.getHullNameWithDashClass());

            tp.addPara(statusStr + str("shipProductionItemTpTxt3"),
                pad, highlight, UIUtils.getTimeWithDay(order.daysRemaining)
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

        final InteractiveSprite shipSprite = new InteractiveSprite(scaledW, scaledH, sprite, null, null);
        shipSprite.tooltip.enabled = false;
        shipSprite.audio.enabled = false;
        shipSprite.glow.isFaderOwner = false;
        shipSprite.glow.fader = glow.fader;
        shipSprite.glow.type = GlowType.ADDITIVE;
        shipSprite.glow.glowBrightness = 0.8f;
        shipSprite.glow.flashBrightness = 1.2f;
        add(shipSprite).inLMid(hpad + (maxSize - scaledW) / 2);

        final String shipStr = spec.getHullNameWithDashClass();
        final String timeStr = UIUtils.getTimeWithDay(order.daysRemaining);
        final LabelAPI topSection = settings.createLabel(
            shipStr + str("uiTitleRemainingSection") + timeStr, Fonts.DEFAULT_SMALL
        );
        topSection.setHighlightColor(base);
        topSection.setHighlight(timeStr);
        topSection.setAlignment(Alignment.LMID);
        topSection.autoSizeToWidth(CONTENT_W);
        add(topSection).inTL(HEIGHT, hpad);

        final boolean isBeingProduced = index <= StaticData.inv.getAssemblyLines() - 1;

        final String sliderTxt = isBeingProduced ? order.daysRemaining + str("uiSingleLetterDay") +" / " + order.days + str("uiSingleLetterDay") : str("waitingTitle");
        final Slider timeSlider = new Slider(sliderTxt, 0f, order.days, CONTENT_W - opad, 32);
        timeSlider.showLabelOnly = true;
        timeSlider.setUserAdjustable(false);
        timeSlider.setProgress(isBeingProduced ? order.days - order.daysRemaining : order.days);
        if (!isBeingProduced) timeSlider.setBarColor(UIColors.CARGO_COLOR);
        add(timeSlider).inBL(HEIGHT, hpad);

        PlannedOrderWidget.addSelectionUI(this, selectionState);
    }

    @Override
    public void renderBelowImpl(float alpha) {
        border.render(getX(), getY(), alpha);
    }

    public InteractionComp<ShipProductionWidget> getInteraction() {
        return interaction;
    }
}
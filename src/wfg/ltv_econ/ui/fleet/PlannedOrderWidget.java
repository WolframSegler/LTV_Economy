package wfg.ltv_econ.ui.fleet;

import static wfg.native_ui.util.UIConstants.*;
import static wfg.native_ui.util.Globals.settings;

import java.awt.Color;

import com.fs.starfarer.api.campaign.FactionSpecAPI;
import com.fs.starfarer.api.campaign.econ.CommoditySpecAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.impl.codex.CodexDataV2;
import com.fs.starfarer.api.ui.Alignment;
import com.fs.starfarer.api.ui.Fonts;
import com.fs.starfarer.api.ui.LabelAPI;
import com.fs.starfarer.api.ui.PositionAPI;
import com.fs.starfarer.api.ui.UIComponentAPI;
import com.fs.starfarer.api.ui.UIPanelAPI;

import wfg.ltv_econ.constants.UIColors;
import wfg.ltv_econ.economy.fleet.PlannedOrder;
import wfg.ltv_econ.serializable.StaticData;
import wfg.ltv_econ.ui.factionTab.PlannedOrdersPanel;
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
import wfg.native_ui.ui.panel.BasePanel;
import wfg.native_ui.ui.table.WidgetAPI;
import wfg.native_ui.ui.visual.IconValuePair;
import wfg.native_ui.ui.visual.SpritePanelWithTp;
import wfg.native_ui.util.NativeUiUtils;
import wfg.native_ui.util.NumFormat;

public class PlannedOrderWidget extends UIClickable<PlannedOrderWidget> implements
    WidgetAPI<PlannedOrderWidget>, HasTooltip, HasHoverGlow
{
    public static final int WIDTH = 320;
    public static final int HEIGHT = 60;
    public static final Color AMBER_BG = new Color(84, 61, 32);

    private final TooltipComp tooltip = comp().get(NativeComponents.TOOLTIP);
    private final HoverGlowComp glow = comp().get(NativeComponents.HOVER_GLOW);
    private final BorderRenderer border = new BorderRenderer(UI_BORDER_4, true, WIDTH, HEIGHT);

    private final PlannedOrder order;
    private final ShipHullSpecAPI spec;

    public WidgetSelectionState selectionState = WidgetSelectionState.NONE;
    public int index = 0;

    public PlannedOrderWidget(PlannedOrdersPanel parent, PlannedOrder order, int index) {
        super(parent.getPanel(), WIDTH, HEIGHT, null);
        this.order = order;
        this.index = index;

        glow.type = GlowType.UNDERLAY;
        glow.overlayBrightness = 0.6f;
        glow.color = UIColors.IN_FACTION;

        border.centerColor = InventoryShipWidget.WIDGET_BG;

        spec = settings.getHullSpec(order.hullId);

        tooltip.codexID = CodexDataV2.getShipEntryId(order.hullId);
        tooltip.builder = (tp, expanded) -> {
            final FactionSpecAPI faction = settings.getFactionSpec(StaticData.inv.factionID);
            final MarketAPI capital = StaticData.inv.getCapital();
            final String capitalName = capital == null ? "None" : capital.getName();

            tp.addTitle("Ship Order", base);

            tp.addPara(
                "The " + spec.getHullNameWithDashClass() + " production order will enter the queue once the required resources " +
                "are allocated. A total cost of %s will be charged to the faction's capital, %s, and construction " +
                "is expected to take %s before the ship is ready for service.",
                pad, new Color[]{highlight, faction.getBaseUIColor(), highlight},
                NumFormat.formatCreditAbs(order.credits), capitalName,
                order.days + (order.days == 1 ? " day" : " days")
            );

            final int gridWidth = 390;
            final int valueWidth = 40;
            int rowCount = 0;

            tp.addPara("Required Resources", base, opad);
            tp.beginGridFlipped(gridWidth, 2, valueWidth, hpad);
            for (var e : order.commodities.singleEntrySet()) {
                final CommoditySpecAPI com = settings.getCommoditySpec(e.getKey());
                final String amountStr = NumFormat.engNotate(e.getValue());

                tp.addToGrid(0, rowCount++, com.getName(), amountStr);
            }
            tp.addGrid(0);

            tp.addPara("%s + %s to remove instantly.", opad, highlight, "Shift", "Click");
        };

        buildUI();
    }

    @Override
    public void buildUI() {
        clearChildren();

        final SpriteAPI sprite = settings.getSprite(spec.getSpriteName());

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
        final String costStr = NumFormat.formatCredit(order.credits);
        final String timeStr = order.days + (order.days <= 1 ? " Day" : " Days");
        final String gapStr = " • ";
        final LabelAPI topSection = settings.createLabel(
            shipStr + gapStr + costStr + gapStr + timeStr, Fonts.DEFAULT_SMALL
        );
        topSection.setHighlightColors(highlight, base);
        topSection.setHighlight(costStr, timeStr);
        topSection.setAlignment(Alignment.LMID);
        topSection.autoSizeToWidth(WIDTH - HEIGHT);
        add(topSection).inTL(HEIGHT, hpad);

        final int iconS = 28;
        final int pairW = iconS + 60; 
        int currW = HEIGHT + opad;
        for (var e : order.commodities.singleEntrySet()) {
            final CommoditySpecAPI spec = settings.getCommoditySpec(e.getKey());
            final IconValuePair pair = new IconValuePair(m_panel, pairW, iconS, spec.getIconName(), e.getValue(),
                true, null
            );

            add(pair).inBL(currW, hpad);
            currW += pairW + opad;

            if (currW > WIDTH) {
                remove(pair);
                break;
            }
        }
    
        addSelectionUI(m_panel, selectionState);
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
            bgPanel.bg.color = AMBER_BG;
            bgPanel.bg.offset.setOffset(4, 4, -8, -8);

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

    public InteractionComp<PlannedOrderWidget> getInteraction() {
        return interaction;
    }

    public UIComponentAPI getElement() {
        return m_panel;
    }
}
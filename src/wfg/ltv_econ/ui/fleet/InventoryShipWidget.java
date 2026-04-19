package wfg.ltv_econ.ui.fleet;

import java.awt.Color;

import static wfg.native_ui.util.UIConstants.*;
import static wfg.ltv_econ.constants.Sprites.*;
import static wfg.native_ui.util.Globals.settings;

import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.impl.campaign.DebugFlags;
import com.fs.starfarer.api.impl.campaign.ids.Strings;
import com.fs.starfarer.api.impl.codex.CodexDataV2;
import com.fs.starfarer.api.ui.Alignment;
import com.fs.starfarer.api.ui.Fonts;
import com.fs.starfarer.api.ui.LabelAPI;
import com.fs.starfarer.api.ui.UIComponentAPI;
import com.fs.starfarer.api.ui.UIPanelAPI;

import wfg.ltv_econ.config.EconConfig;
import wfg.ltv_econ.constants.UIColors;
import wfg.ltv_econ.economy.fleet.ShipTypeData;
import wfg.ltv_econ.ui.factionTab.dialog.AddShipDialog;
import wfg.ltv_econ.ui.factionTab.dialog.RemoveShipDialog;
import wfg.ltv_econ.ui.reusable.WidgetSelectionState;
import wfg.native_ui.internal.util.BorderRenderer;
import wfg.native_ui.ui.component.AudioFeedbackComp;
import wfg.native_ui.ui.component.HoverGlowComp;
import wfg.native_ui.ui.component.InteractionComp;
import wfg.native_ui.ui.component.NativeComponents;
import wfg.native_ui.ui.component.TooltipComp;
import wfg.native_ui.ui.component.HoverGlowComp.GlowType;
import wfg.native_ui.ui.core.UIBuildableAPI;
import wfg.native_ui.ui.core.UIElementFlags.HasAudioFeedback;
import wfg.native_ui.ui.core.UIElementFlags.HasHoverGlow;
import wfg.native_ui.ui.core.UIElementFlags.HasTooltip;
import wfg.native_ui.ui.functional.Button;
import wfg.native_ui.ui.functional.Button.CutStyle;
import wfg.native_ui.ui.panel.BasePanel;
import wfg.native_ui.ui.panel.CustomPanel;
import wfg.native_ui.ui.system.NativeSystems;
import wfg.native_ui.ui.system.TooltipSystem;
import wfg.native_ui.ui.table.WidgetAPI;
import wfg.native_ui.ui.visual.IconValuePair;
import wfg.native_ui.ui.visual.SpritePanelWithTp;
import wfg.native_ui.util.NativeUiUtils;
import wfg.native_ui.util.NumFormat;

public class InventoryShipWidget extends CustomPanel implements WidgetAPI<InventoryShipWidget>,
    HasAudioFeedback, HasHoverGlow, HasTooltip
{
    public static final int WIDTH = 180;
    public static final int HEIGHT = 300;
    public static final Color WIDGET_BG = new Color(28, 35, 48, 240);

    private final AudioFeedbackComp audio = comp().get(NativeComponents.AUDIO_FEEDBACK);
    private final HoverGlowComp glow = comp().get(NativeComponents.HOVER_GLOW);
    private final BorderRenderer border = new BorderRenderer(UI_BORDER_3, true, WIDTH, HEIGHT);
    public final TooltipComp tooltip = comp().get(NativeComponents.TOOLTIP);

    private final UIBuildableAPI navbar;
    public final ShipTypeData data;

    public WidgetSelectionState selectionState = WidgetSelectionState.NONE;

    public InventoryShipWidget(UIPanelAPI parent, ShipTypeData data, UIBuildableAPI navbar) {
        super(parent, WIDTH, HEIGHT);
        this.data = data;
        this.navbar = navbar;

        audio.hoverOnly = true;

        glow.type = GlowType.UNDERLAY;
        glow.overlayBrightness = 0.6f;
        glow.color = UIColors.IN_FACTION;

        border.centerColor = WIDGET_BG;

        tooltip.width = 500f;
        tooltip.codexID = CodexDataV2.getShipEntryId(data.hullID);
        tooltip.builder = (tp, expanded) -> {
            tp.addTitle("Faction Ship", base);

            tp.addPara(
                "The faction inventory owns a total of %s " + data.spec.getHullNameWithDashClass() + " ships, " +
                "%s of which are in-use and the rest (%s) are idle. It takes %s tons of supplies per day to maintain " +
                "the ships, where the maintenance costs of idle ships are reduced by %s.", pad,
                new Color[] {highlight, highlight, highlight, highlight, base},
                NumFormat.engNotate(data.getOwned()), NumFormat.engNotate(data.getInUse()),
                NumFormat.engNotate(data.getIdle()), NumFormat.engNotate(data.getDailyMaintenanceCost()),
                Integer.toString((int) (100f - EconConfig.IDLE_SHIP_MAINTENANCE_MULT*100f)) + "%"
            );

            tp.addPara(
                "It takes %s crew to maintain the ships throughout the month. Each active crew member is paid %s " +
                "per month, whereas the wages of idle ship crew are reduced by %s, for a total of %s. The " +
                data.spec.getHullNameWithDashClass() + " boasts a combat power of %s for a total power of %s.",
                pad, new Color[]{highlight, highlight, base, highlight, highlight, highlight},
                NumFormat.engNotate(data.getTotalCrew()), NumFormat.formatCredit(EconConfig.CREW_WAGE_PER_MONTH),
                Integer.toString((int) (100f - EconConfig.IDLE_CREW_WAGE_MULT*100f)) + "%",
                NumFormat.formatCredit(data.getMonthlyCrewWages()), NumFormat.engNotate(data.getCombatPower()),
                NumFormat.engNotate(data.getTotalCombatPower())
            );
        };

        buildUI();
    }

    @Override
    public void buildUI() {
        clearChildren();

        final SpriteAPI sprite = settings.getSprite(data.spec.getSpriteName());

        final int maxSize = WIDTH - opad * 3;
        final float spriteW = sprite.getWidth();
        final float spriteH = sprite.getHeight();
        final float scale = Math.min(maxSize / spriteW, maxSize / spriteH);
        final int scaledW = (int) (spriteW * scale);
        final int scaledH = (int) (spriteH * scale);
        
        final SpritePanelWithTp shipSprite = new SpritePanelWithTp(m_panel, scaledW, scaledH, data.spec.getSpriteName(), null, null);
        shipSprite.tooltip.enabled = false;
        shipSprite.audio.enabled = false;
        shipSprite.glow.isFaderOwner = false;
        shipSprite.glow.fader = glow.fader;
        shipSprite.glow.type = GlowType.ADDITIVE;
        shipSprite.glow.additiveBrightness = 0.8f;
        add(shipSprite).inTMid(hpad + (maxSize - scaledH) / 2);

        final LabelAPI nameLbl = settings.createLabel(data.spec.getHullNameWithDashClass(), Fonts.DEFAULT_SMALL);
        nameLbl.setColor(base);
        nameLbl.setAlignment(Alignment.MID);
        nameLbl.autoSizeToWidth(WIDTH);
        add(nameLbl).inTL(0f, WIDTH - opad*2);

        final BasePanel separator = new BasePanel(
            m_panel, WIDTH, 1
        ) {{ bg.color = gray;}};
        add(separator).inTL(0f, WIDTH);

        final int GAP_TOP_1 = WIDTH + hpad;
        final int gapFor2Lbl = maxSize / 2 + pad;

        final String idleStr = Strings.X + NumFormat.engNotate(data.getIdle());
        final String inUseStr = Strings.X + NumFormat.engNotate(data.getInUse());
        final LabelAPI idleLbl = settings.createLabel("Idle: " + idleStr, Fonts.DEFAULT_SMALL);
        final LabelAPI inUseLbl = settings.createLabel("In Use: " + inUseStr, Fonts.DEFAULT_SMALL);
        idleLbl.setHighlightColor(highlight);
        inUseLbl.setHighlightColor(highlight);
        idleLbl.setHighlight(idleStr);
        inUseLbl.setHighlight(inUseStr);
        add(idleLbl).inTL(opad, GAP_TOP_1);
        add(inUseLbl).inTL(opad + gapFor2Lbl, GAP_TOP_1);

        final int GAP_TOP_2 = GAP_TOP_1 + 20;
        final int iconS = 28;

        final IconValuePair crewPair = new IconValuePair(m_panel, gapFor2Lbl, iconS, CREW, data.getTotalCrew(), true, null);
        final IconValuePair combatPair = new IconValuePair(m_panel, gapFor2Lbl, iconS, COMBAT, data.getTotalCombatPower(), true, null);

        add(crewPair).inTL(hpad*3, GAP_TOP_2);
        add(combatPair).inTL(hpad*3 + gapFor2Lbl, GAP_TOP_2);

        final int GAP_TOP_3 = GAP_TOP_2 + 30;

        final IconValuePair suppliesPair = new IconValuePair(m_panel, gapFor2Lbl, iconS, SUPPLIES, data.getDailyMaintenanceCost(), true, null);
        final IconValuePair wagesPair = new IconValuePair(m_panel, gapFor2Lbl, iconS, WAGES, data.getMonthlyCrewWages(), false, null);
        wagesPair.label().setText(wagesPair.label().getText() + Strings.C);

        add(suppliesPair).inTL(hpad*3, GAP_TOP_3);
        add(wagesPair).inTL(hpad*3 + gapFor2Lbl, GAP_TOP_3);

        final Button removeBtn = new Button(m_panel, 30, 30, NumFormat.MINUS, Fonts.DEFAULT_SMALL, null);
        removeBtn.bgColor = UIColors.REMOVE_COLOR;
        removeBtn.bgDisabledColor = UIColors.REMOVE_COLOR;
        removeBtn.bgDisabledAlpha = 0.6f;
        removeBtn.setEnabled(data.getIdle() > 0);
        removeBtn.cutStyle = CutStyle.TL;
        add(removeBtn).inBR(pad, pad);
        removeBtn.onClicked = (btn) -> {
            if (NativeUiUtils.isShiftDown()) {
                data.addShip(-10);
                buildUI();
            } else if (NativeUiUtils.isCtrlDown()) {
                data.addShip(-1);
                buildUI();
            } else {
                final RemoveShipDialog dialog = new RemoveShipDialog(this);
                dialog.show(0.1f, 0.1f);
            }
            navbar.buildUI();
        };
        removeBtn.system().setIfNotPresent(
            NativeSystems.TOOLTIP, TooltipSystem.get(), removeBtn
        );
        final TooltipComp removeTp = removeBtn.comp().get(NativeComponents.TOOLTIP);
        removeTp.builder = (tp, expanded) -> {
            tp.addPara("%s + %s to remove one. %s + %s to remove ten", pad, highlight, "Ctrl", "Click", "Shift", "Click");
        };

        if (DebugFlags.COLONY_DEBUG) {
            final Button addBtn = new Button(m_panel, 30, 30, "+", Fonts.DEFAULT_SMALL, null);
            addBtn.bgColor = UIColors.ADD_COLOR;
            addBtn.cutStyle = CutStyle.TR;
            add(addBtn).inBL(pad, pad);
            addBtn.onClicked = (btn) -> {
                if (NativeUiUtils.isCtrlDown()) {
                    final AddShipDialog dialog = new AddShipDialog(this);
                    dialog.show(0.1f, 0.1f);
                } else if (NativeUiUtils.isShiftDown()) {
                    data.addShip(10);
                    buildUI();
                } else {
                    data.addShip(1);
                    buildUI();
                }
                navbar.buildUI();
            };
            addBtn.system().setIfNotPresent(
                NativeSystems.TOOLTIP, TooltipSystem.get(), addBtn
            );
            final TooltipComp addTp = addBtn.comp().get(NativeComponents.TOOLTIP);
            addTp.builder = (tp, expanded) -> {
                tp.addPara("%s + %s to open the dialog. %s + %s to add ten", pad, highlight, "Ctrl", "Click", "Shift", "Click");
            };
        }
    }

    @Override
    public void renderBelow(float alpha) {
        super.renderBelow(alpha);

        border.render(pos.getX(), pos.getY(), alpha);
    }

    public UIComponentAPI getElement() {
        return m_panel;
    }

    public InteractionComp<InventoryShipWidget> getInteraction() {
        return null;
    }
}
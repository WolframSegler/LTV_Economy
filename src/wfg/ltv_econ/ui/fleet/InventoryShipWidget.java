package wfg.ltv_econ.ui.fleet;

import java.awt.Color;

import static wfg.native_ui.util.UIConstants.*;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.SettingsAPI;
import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.impl.campaign.ids.Commodities;
import com.fs.starfarer.api.impl.campaign.ids.Strings;
import com.fs.starfarer.api.ui.Alignment;
import com.fs.starfarer.api.ui.Fonts;
import com.fs.starfarer.api.ui.LabelAPI;
import com.fs.starfarer.api.ui.UIPanelAPI;

import wfg.ltv_econ.config.EconConfig;
import wfg.ltv_econ.economy.fleet.ShipTypeData;
import wfg.native_ui.internal.util.BorderRenderer;
import wfg.native_ui.ui.component.NativeComponents;
import wfg.native_ui.ui.component.TooltipComp;
import wfg.native_ui.ui.core.UIBuildableAPI;
import wfg.native_ui.ui.panel.BasePanel;
import wfg.native_ui.ui.panel.CustomPanel;
import wfg.native_ui.ui.visual.SpritePanel.Base;
import wfg.native_ui.util.NativeUiUtils;
import wfg.native_ui.util.NativeUiUtils.AnchorType;
import wfg.native_ui.util.NumFormat;

// TODO add click functionality
public class InventoryShipWidget extends CustomPanel<InventoryShipWidget> implements UIBuildableAPI {
    private static final SettingsAPI settings = Global.getSettings();
    private static final SpriteAPI SUPPLIES = settings.getSprite(settings.getCommoditySpec(Commodities.SUPPLIES).getIconName());
    private static final SpriteAPI CREW = settings.getSprite(settings.getCommoditySpec(Commodities.CREW).getIconName());
    private static final SpriteAPI COMBAT = settings.getSprite("ui", "icon_kinetic");
    private static final SpriteAPI WAGES = settings.getSprite("icons", "ratio_chart");

    private static final int WIDTH = 180;
    private static final int HEIGHT = 300;

    private final TooltipComp tooltip = comp().get(NativeComponents.TOOLTIP);
    private final BorderRenderer border = new BorderRenderer(UI_BORDER_3, true, WIDTH, HEIGHT);

    private final ShipTypeData data;

    public InventoryShipWidget(UIPanelAPI parent, ShipTypeData data) {
        super(parent, WIDTH, HEIGHT);

        this.data = data;
        tooltip.positioner = (tp, exp) -> {
            NativeUiUtils.anchorPanel(tp, m_panel, AnchorType.LeftTop, pad);
        };
        tooltip.builder = (tp, expanded) -> {
            tp.addTitle("Faction Ship", base);

            tp.addPara(
                "The faction inventory owns a total of %s " + data.spec.getHullNameWithDashClass() + " ships, " +
                "%s of which are in-use and rest (%s) are idle. It takes %s tons of supplies per month to maintain " +
                "the ships, where the maintenance costs of idle ships are reduced by %s.", pad,
                new Color[] {highlight, highlight, highlight, highlight, base},
                NumFormat.engNotate(data.getOwned()), NumFormat.engNotate(data.getInUse()),
                NumFormat.engNotate(data.getIdle()), NumFormat.engNotate(data.getMonthlyMaintenanceCost()),
                Integer.toString((int) (100f - EconConfig.IDLE_SHIP_MAINTENANCE_MULT*100f)) + "%"
            );

            tp.addPara(
                "It takes %s crew to maintain the ships throughout the month. Each active crew member is paid %s " +
                "a month, whereas the wages of idle ship crew are reduced by %s, for a total of %s. The " +
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
        final int shipS = WIDTH - opad*2;
        final Base shipSprite = new Base(m_panel, shipS, shipS, data.spec.getSpriteName(), null, null);
        add(shipSprite).inTL(opad, opad);

        final LabelAPI nameLbl = settings.createLabel(data.spec.getHullNameWithDashClass(), Fonts.DEFAULT_SMALL);
        nameLbl.setColor(base);
        nameLbl.setAlignment(Alignment.MID);
        nameLbl.autoSizeToWidth(WIDTH);
        add(nameLbl).inTL(0f, WIDTH - hpad);

        final BasePanel separator = new BasePanel(
            m_panel, WIDTH, 1
        ) {{ bg.color = gray;}};
        add(separator).inTL(0f, WIDTH);

        final int GAP_TOP_1 = WIDTH + hpad;
        final int gapFor3Lbl = shipS / 3;

        final String idleStr = Strings.X + NumFormat.engNotate(data.getIdle());
        final String inUseStr = Strings.X + NumFormat.engNotate(data.getInUse());
        final String ownedStr = Strings.X + NumFormat.engNotate(data.getOwned());
        final LabelAPI idleLbl = settings.createLabel("Idle: " + idleStr, Fonts.DEFAULT_SMALL);
        final LabelAPI inUseLbl = settings.createLabel("In Use: " + inUseStr, Fonts.DEFAULT_SMALL);
        final LabelAPI ownedLbl = settings.createLabel("Owned: " + ownedStr, Fonts.DEFAULT_SMALL);
        idleLbl.setHighlightColor(highlight);
        inUseLbl.setHighlightColor(highlight);
        ownedLbl.setHighlightColor(highlight);
        add(idleLbl).inTL(opad, GAP_TOP_1);
        add(inUseLbl).inTL(opad + gapFor3Lbl, GAP_TOP_1);
        add(ownedLbl).inTL(opad+ gapFor3Lbl*2, GAP_TOP_1);

        final int GAP_TOP_2 = GAP_TOP_1 + 30;
        final int gapFor2Lbl = shipS / 2;
        final int iconS = 28;

        final Base crewIcon = new Base(m_panel, iconS, iconS, CREW, null, null);
        final Base combatIcon = new Base(m_panel, iconS, iconS, COMBAT, null, null);
        add(crewIcon).inTL(opad, GAP_TOP_2);
        add(combatIcon).inTL(opad + gapFor2Lbl, GAP_TOP_2);

        final LabelAPI crewLbl = settings.createLabel(Strings.X + NumFormat.engNotate(data.getTotalCrew()), Fonts.DEFAULT_SMALL);
        final LabelAPI combatLbl = settings.createLabel(Strings.X + NumFormat.engNotate(data.getTotalCombatPower()), Fonts.DEFAULT_SMALL);
        crewLbl.setAlignment(Alignment.LMID);
        combatLbl.setAlignment(Alignment.LMID);
        crewLbl.setColor(highlight);
        combatLbl.setColor(highlight);
        add(crewLbl).setSize(gapFor2Lbl, iconS).inTL(opad + iconS, GAP_TOP_2);
        add(combatLbl).setSize(gapFor2Lbl, iconS).inTL(opad + iconS + gapFor2Lbl, GAP_TOP_2);

        final int GAP_TOP_3 = GAP_TOP_2 + 30;

        final Base suppliesIcon = new Base(m_panel, iconS, iconS, SUPPLIES, null, null);
        final Base wagesIcon = new Base(m_panel, iconS, iconS, WAGES, null, null);
        add(suppliesIcon).inTL(opad, GAP_TOP_3);
        add(wagesIcon).inTL(opad + gapFor2Lbl, GAP_TOP_3);

        final LabelAPI suppliesLbl = settings.createLabel(Strings.X + NumFormat.engNotate(data.getMonthlyMaintenanceCost()), Fonts.DEFAULT_SMALL);
        final LabelAPI wageLbl = settings.createLabel(Strings.X + NumFormat.formatCreditAbs(data.getMonthlyCrewWages()), Fonts.DEFAULT_SMALL);
        suppliesLbl.setAlignment(Alignment.LMID);
        wageLbl.setAlignment(Alignment.LMID);
        suppliesLbl.setColor(highlight);
        wageLbl.setColor(highlight);
        add(suppliesLbl).setSize(gapFor2Lbl, iconS).inTL(opad + iconS, GAP_TOP_3);
        add(wageLbl).setSize(gapFor2Lbl, iconS).inTL(opad + iconS + gapFor2Lbl, GAP_TOP_3);
    }

    @Override
    public void renderBelow(float alpha) {
        super.renderBelow(alpha);

        border.render(pos.getX(), pos.getY(), alpha);
    }
}
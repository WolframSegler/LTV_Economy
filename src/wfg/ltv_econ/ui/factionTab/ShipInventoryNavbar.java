package wfg.ltv_econ.ui.factionTab;

import static wfg.native_ui.util.UIConstants.*;
import static wfg.ltv_econ.constant.Sprites.*;
import static wfg.ltv_econ.constant.strings.LocalizedStrings.*;
import static wfg.native_ui.util.Globals.settings;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.campaign.FactionSpecAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.combat.ShipHullSpecAPI.ShipTypeHints;
import com.fs.starfarer.api.impl.campaign.ids.Strings;
import com.fs.starfarer.api.ui.Fonts;
import com.fs.starfarer.api.ui.LabelAPI;
import com.fs.starfarer.api.ui.UIPanelAPI;

import wfg.ltv_econ.config.EconConfig;
import wfg.ltv_econ.constant.UIColors;
import wfg.ltv_econ.economy.fleet.FactionShipInventory;
import wfg.ltv_econ.economy.fleet.ShipTypeData;
import wfg.ltv_econ.serializable.StaticData;
import wfg.native_ui.ui.component.BackgroundComp;
import wfg.native_ui.ui.component.NativeComponents;
import wfg.native_ui.ui.core.UIBuildableAPI;
import wfg.native_ui.ui.core.UIElementFlags.HasBackground;
import wfg.native_ui.ui.panel.CustomPanel;
import wfg.native_ui.ui.visual.IconValuePairTp;
import wfg.native_ui.ui.visual.SpritePanel.Base;
import wfg.native_ui.util.NativeUiUtils;
import wfg.native_ui.util.NumFormat;
import wfg.native_ui.util.NativeUiUtils.AnchorType;

public class ShipInventoryNavbar extends CustomPanel implements UIBuildableAPI, HasBackground {
    private static final float flagRatio = 410f / 256f;

    private final BackgroundComp bg = comp().get(NativeComponents.BACKGROUND);

    public ShipInventoryNavbar(UIPanelAPI parent, int w, int h) {
        super(parent, w, h);

        bg.alpha = 0.6f;
        
        buildUI();
    }

    @Override
    public void buildUI() {
        clearChildren();

        final int w = (int) pos.getWidth();
        final int h = (int) pos.getHeight();
        final int flagH = h - hpad*2;
        final int flagW = (int) (flagH * flagRatio);

        final FactionShipInventory inv = StaticData.inv;
        final FactionSpecAPI factionSpec = settings.getFactionSpec(inv.factionID);
        final FactionAPI faction = Global.getSector().getFaction(inv.factionID);

        final Base banner = new Base(m_panel, flagW, flagH, faction.getLogo(), null, null);
        add(banner).inTL(hpad, hpad);

        final int GAP_LEFT_1 = flagW + opad*2 + pad;

        final LabelAPI title = settings.createLabel(str("uiBtnTitleFactionHangar"), Fonts.INSIGNIA_VERY_LARGE);
        add(title).inTL(GAP_LEFT_1, hpad);

        final MarketAPI market = inv.getCapital();
        final String name = faction.getDisplayName();
        final String capital = market == null ? str("uiNone") : market.getName();
        final LabelAPI subtitle = settings.createLabel(name + str("uiCapitalCityInTxt") + capital, Fonts.DEFAULT_SMALL);
        subtitle.setHighlightColor(factionSpec.getBaseUIColor());
        subtitle.setHighlight(name, capital);
        add(subtitle).inTR(opad, opad);

        final int GAP_TOP_1 = hpad + 60;
        final int perPairW = ((w - flagW)/2 - opad) / 4;
        final int iconS = 32;

        final LabelAPI shipmentLbl = settings.createLabel(str("uiTitleHullCapacities"), Fonts.INSIGNIA_LARGE);
        add(shipmentLbl).inTL(GAP_LEFT_1, GAP_TOP_1);

        final IconValuePairTp cargoPair = new IconValuePairTp(m_panel, perPairW, iconS, CRATES, inv.getTotalCargoCapacity(), true, null);
        final IconValuePairTp fuelPair = new IconValuePairTp(m_panel, perPairW, iconS, FUEL, inv.getTotalFuelCapacity(), true, null);
        final IconValuePairTp crewPair = new IconValuePairTp(m_panel, perPairW, iconS, BERTH, inv.getTotalCrewCapacity(), true, null);
        final IconValuePairTp combatPair = new IconValuePairTp(m_panel, perPairW, iconS, COMBAT, inv.getTotalCombatPower(), true, null);

        final int GAP_TOP_2 = GAP_TOP_1 + 25;

        add(cargoPair).inTL(GAP_LEFT_1, GAP_TOP_2);
        add(fuelPair).inTL(GAP_LEFT_1 + perPairW, GAP_TOP_2);
        add(crewPair).inTL(GAP_LEFT_1 + perPairW*2, GAP_TOP_2);
        add(combatPair).inTL(GAP_LEFT_1 + perPairW*3, GAP_TOP_2);

        final int GAP_LEFT_2 = GAP_LEFT_1 + (w - GAP_LEFT_1)/2 + opad;

        final LabelAPI operationLbl = settings.createLabel(str("uiTitleOperations"), Fonts.INSIGNIA_LARGE);
        add(operationLbl).inTL(GAP_LEFT_2, GAP_TOP_1);

        final IconValuePairTp suppliesPair = new IconValuePairTp(m_panel, perPairW, iconS, SUPPLIES, inv.getTotalDailyMaintenance(), true, null);
        final IconValuePairTp operatorPair = new IconValuePairTp(m_panel, perPairW, iconS, CREW, inv.getTotalCrew(), true, null);
        final IconValuePairTp wagePair = new IconValuePairTp(m_panel, perPairW, iconS, WAGES, inv.getTotalMonthlyCrewWage(), false, null);
        final IconValuePairTp hullPair = new IconValuePairTp(m_panel, perPairW, iconS, SHIPS, inv.getOwnedShips(), true, null);

        add(suppliesPair).inTL(GAP_LEFT_2, GAP_TOP_2);
        add(operatorPair).inTL(GAP_LEFT_2 + perPairW, GAP_TOP_2);
        add(wagePair).inTL(GAP_LEFT_2 + perPairW*2, GAP_TOP_2);
        add(hullPair).inTL(GAP_LEFT_2 + perPairW*3, GAP_TOP_2);

        cargoPair.icon().texColor = UIColors.CARGO_COLOR;
        wagePair.label().setText(wagePair.label().getText() + Strings.C);

        cargoPair.tooltip.builder = (tp, expanded) -> {
            tp.addTitle(str("uiTitleCargoCapacity"), base);

            tp.addPara(str("uiTpTxtCargoCapacity"), pad);
            final float idle = inv.getIdleCargoCapacity();
            final float inUse = inv.getTotalCargoCapacity() - idle;
            tp.addPara(str("uiIdleInUseSectionTxt"), pad, highlight, 
                NumFormat.engNotate(idle), NumFormat.engNotate(inUse)
            );
        };

        fuelPair.tooltip.builder = (tp, expanded) -> {
            tp.addTitle(str("uiTitleFuelCapacity"), base);

            tp.addPara(str("uiTpTxtFuelCapacity"), pad);
            final float idle = inv.getIdleFuelCapacity();
            final float inUse = inv.getTotalFuelCapacity() - idle;
            tp.addPara(str("uiIdleInUseSectionTxt"), pad, highlight, 
                NumFormat.engNotate(idle), NumFormat.engNotate(inUse));
        };

        crewPair.tooltip.builder = (tp, expanded) -> {
            tp.addTitle(str("uiTitleCrewCapacity"), base);

            tp.addPara(str("uiTpTxtCrewCapacity"), pad);
            final float idle = inv.getIdleCrewCapacity();
            final float inUse = inv.getTotalCrewCapacity() - idle;
            tp.addPara(str("uiIdleInUseSectionTxt"), pad, highlight, 
                NumFormat.engNotate(idle), NumFormat.engNotate(inUse));
        };

        combatPair.tooltip.builder = (tp, expanded) -> {
            tp.addTitle(str("uiTitleCombatPower"), base);

            tp.addPara(str("uiTpTxtCombatPower"), pad);
            final float idle = inv.getIdleCombatPower();
            final float inUse = inv.getTotalCombatPower() - idle;
            tp.addPara(str("uiIdleInUseSectionTxt"), pad, highlight, 
                NumFormat.engNotate(idle), NumFormat.engNotate(inUse));
        };

        suppliesPair.tooltip.builder = (tp, expanded) -> {
            tp.addTitle(str("uiTitleDailyMaintenance"), base);

            tp.addPara(str("uiDailyMaintenanceTpStr"), pad, highlight, (int)(EconConfig.IDLE_SHIP_MAINTENANCE_MULT * 100) + "%");
            final float daily = inv.getTotalDailyMaintenance();
            tp.addPara(str("uiDailyMaintenanceSupplyUsageTxt"), pad, highlight, NumFormat.engNotate(daily));
        };

        operatorPair.tooltip.builder = (tp, expanded) -> {
            tp.addTitle(str("uiTitleCrewComplement"), base);

            tp.addPara(str("uiTpTxtCrewComplement"), pad);
            final int idle = inv.getIdleCrew();
            final int inUse = inv.getTotalCrew() - idle;
            tp.addPara(str("uiIdleActiveSectionTxt"), pad, highlight, 
                NumFormat.engNotate(idle), NumFormat.engNotate(inUse));
        };

        wagePair.tooltip.builder = (tp, expanded) -> {
            tp.addTitle(str("uiTitleMonthlyCrewWages"), base);
            
            tp.addPara(str("uiTpTxtMonthlyCrewWages1"), pad, highlight, (int)(EconConfig.IDLE_CREW_WAGE_MULT * 100) + "%");
            float wages = inv.getTotalMonthlyCrewWage();
            tp.addPara(str("uiTpTxtMonthlyCrewWages2"), pad, highlight, NumFormat.formatCreditAbs(wages));
        };

        hullPair.tooltip.builder = (tp, expanded) -> {
            int civilianCount = 0;
            int frigateCount = 0;
            int destroyerCount = 0;
            int cruiserCount = 0;
            int capitalCount = 0;
            for (ShipTypeData data : inv.getShips().values()) {
                if (data.spec.getHints().contains(ShipTypeHints.CIVILIAN)) civilianCount += data.getOwned();
                switch (data.spec.getHullSize()) {
                    case FRIGATE -> ++frigateCount;
                    case DESTROYER -> ++destroyerCount;
                    case CRUISER -> ++cruiserCount;
                    case CAPITAL_SHIP -> ++capitalCount;
                    default -> {}
                }
            }
            final int totalCount = inv.getOwnedShips();
            final int combatCount = totalCount - civilianCount;

            tp.addTitle(str("uiTitleOwnedHulls"), base);
            
            tp.addPara(str("uiTpTxtOwnedHulls"), pad, highlight, Integer.toString(totalCount), Integer.toString(inv.getIdleShips()));
            
            final int gridWidth = 390;
            final int valueWidth = 40;

            tp.addPara(str("uiTpTitleOwnedHulls1"), base, opad);
            tp.beginGridFlipped(gridWidth, 2, valueWidth, hpad);
            tp.addToGrid(0, 0, str("uiTitleCivilianShipType"), NumFormat.engNotate(civilianCount));
            tp.addToGrid(0, 1, str("uiTitleCombatShipType"), NumFormat.engNotate(combatCount));
            tp.addGrid(0f);

            tp.addPara(str("uiTpTitleOwnedHulls2"), base, opad);
            tp.beginGridFlipped(gridWidth, 2, valueWidth, hpad);
            tp.addToGrid(0, 0, str("uiTitleFrigatesShipType"), NumFormat.engNotate(frigateCount));
            tp.addToGrid(0, 1, str("uiTitleDestroyersShipType"), NumFormat.engNotate(destroyerCount));
            tp.addToGrid(0, 2, str("uiTitleCruisersShipType"), NumFormat.engNotate(cruiserCount));
            tp.addToGrid(0, 3, str("uiTitleCapitalsShipType"), NumFormat.engNotate(capitalCount));
            tp.addGrid(0f);
        };

        cargoPair.tooltip.positioner = (tp, exp) -> NativeUiUtils.anchorPanelWithBounds(
            tp, cargoPair.getPanel(), AnchorType.RightTop, opad);
        fuelPair.tooltip.positioner = (tp, exp) -> NativeUiUtils.anchorPanelWithBounds(
            tp, fuelPair.getPanel(), AnchorType.RightTop, opad);
        crewPair.tooltip.positioner = (tp, exp) -> NativeUiUtils.anchorPanelWithBounds(
            tp, crewPair.getPanel(), AnchorType.RightTop, opad);
        combatPair.tooltip.positioner = (tp, exp) -> NativeUiUtils.anchorPanelWithBounds(
            tp, combatPair.getPanel(), AnchorType.RightTop, opad);
        suppliesPair.tooltip.positioner = (tp, exp) -> NativeUiUtils.anchorPanelWithBounds(
            tp, suppliesPair.getPanel(), AnchorType.LeftTop, opad);
        operatorPair.tooltip.positioner = (tp, exp) -> NativeUiUtils.anchorPanelWithBounds(
            tp, operatorPair.getPanel(), AnchorType.LeftTop, opad);
        wagePair.tooltip.positioner = (tp, exp) -> NativeUiUtils.anchorPanelWithBounds(
            tp, wagePair.getPanel(), AnchorType.LeftTop, opad);
        hullPair.tooltip.positioner = (tp, exp) -> NativeUiUtils.anchorPanelWithBounds(
            tp, hullPair.getPanel(), AnchorType.LeftTop, opad);
    }
}
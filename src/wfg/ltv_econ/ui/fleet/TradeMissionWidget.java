package wfg.ltv_econ.ui.fleet;

import java.awt.Color;

import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import com.fs.starfarer.api.combat.MutableStat.StatMod;
import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.impl.campaign.ids.Strings;
import com.fs.starfarer.api.ui.Fonts;
import com.fs.starfarer.api.ui.LabelAPI;
import com.fs.starfarer.api.ui.UIPanelAPI;
import com.fs.starfarer.api.util.Misc;

import wfg.ltv_econ.config.EconConfig;
import wfg.ltv_econ.constants.UIColors;
import wfg.ltv_econ.economy.commodity.TradeCom;
import wfg.ltv_econ.economy.engine.EconomyEngine;
import wfg.ltv_econ.economy.fleet.TradeMission;
import wfg.ltv_econ.util.UIUtils;
import wfg.native_ui.internal.util.BorderRenderer;
import wfg.native_ui.ui.component.NativeComponents;
import wfg.native_ui.ui.component.TooltipComp;
import wfg.native_ui.ui.component.OutlineComp.OutlineType;
import wfg.native_ui.ui.component.TooltipComp.TooltipBuilder;
import wfg.native_ui.ui.container.DockPanel;
import wfg.native_ui.ui.core.UIBuildableAPI;
import wfg.native_ui.ui.core.UIElementFlags.HasTooltip;
import wfg.native_ui.ui.panel.CustomPanel;
import wfg.native_ui.ui.visual.IconValuePair;
import wfg.native_ui.ui.visual.SpritePanel.Base;
import wfg.native_ui.ui.widget.Slider;
import wfg.native_ui.util.NativeUiUtils;
import wfg.native_ui.util.NumFormat;
import wfg.native_ui.util.NativeUiUtils.AnchorType;

import static wfg.native_ui.util.UIConstants.*;
import static wfg.ltv_econ.constants.Sprites.*;
import static wfg.native_ui.util.Globals.settings;
import static wfg.ltv_econ.constants.strings.LocalizedStrings.*;

public class TradeMissionWidget extends CustomPanel implements UIBuildableAPI, HasTooltip {
    private static final SpriteAPI SHIP_OUTLINE = settings.getSprite("icons", "ship_outline");

    private final TooltipComp tooltip = comp().get(NativeComponents.TOOLTIP);
    private final BorderRenderer border = new BorderRenderer(UI_BORDER_3, true);

    private final TradeMission mission;
    private final boolean isSrcMarket;

    public TradeMissionWidget(UIPanelAPI parent, int w, int h, TradeMission mission, boolean isSrcMarket, DockPanel dock) {
        super(parent, w, h);

        border.setSize(w, h);
        border.centerColor = new Color(30, 45, 40, 220);

        this.mission = mission;
        this.isSrcMarket = isSrcMarket;

        tooltip.width = 500f;
        tooltip.positioner = (tp, exp) -> {
            NativeUiUtils.anchorPanel(tp, dock.getPanel(), AnchorType.RightTop, pad*2);
        };
        tooltip.builder = createMissionTp(mission, true);
        
        buildUI();
    }

    @Override
    public void buildUI() {
        final int panelW = (int) pos.getWidth();

        final LabelAPI statusLabel = settings.createLabel(mission.status.getDisplayText(), Fonts.ORBITRON_12);
        statusLabel.setColor(mission.status.getDisplayColor());
        add(statusLabel).inTL(opad, opad);

        if (mission.smuggling) {
            final int statusW = (int) statusLabel.getPosition().getWidth();
            final Base smugglingIcon = new Base(m_panel, 19, 9, SMUGGLING, null, null);
            add(smugglingIcon).inTL(opad + pad + statusW, opad);
        }

        final String crestID = (isSrcMarket ? mission.dest : mission.src).getFaction().getCrest();
        final Base factionIcon = new Base(m_panel, 20, 20, crestID, null, null);
        add(factionIcon).inTR(opad, opad);
        if (mission.inFaction) {
            factionIcon.outline.type = OutlineType.VERY_THIN;
            factionIcon.outline.color = UIColors.IN_FACTION;
            factionIcon.outline.enabled = true;
        }

        final Base fleetIcon = new Base(m_panel, 10, 20, SHIP_OUTLINE, null, null);
        fleetIcon.texHaloColor = mission.usedFactionFleet ? UIColors.IN_FACTION : gray; 
        fleetIcon.drawTextureHalo = true;
        add(fleetIcon).inTR(opad*2 + 20, opad);

        if (mission.usedFactionFleet && !mission.usedFuelFromStockpiles) {
            final Base fuelWarningIcon = new Base(m_panel, 20, 20, FUEL, null, null);
            fuelWarningIcon.texHaloColor = UIColors.COM_DEFICIT;
            fuelWarningIcon.drawTextureHalo = true;
            add(fuelWarningIcon).inTR(opad*2 + hpad + 30, opad);
        }

        final int GAP_TOP_1 = 40;
        final int nameGap = 32;
        final int arrowS = 16;

        final LabelAPI srcLbl = settings.createLabel(mission.src.getName(), Fonts.DEFAULT_SMALL);
        final LabelAPI destLbl = settings.createLabel(mission.dest.getName(), Fonts.DEFAULT_SMALL);
        final Base destArrow = new Base(m_panel, arrowS, arrowS, ARROW, null, null);
        srcLbl.setColor(mission.src.getFaction().getBaseUIColor());
        destLbl.setColor(mission.dest.getFaction().getBaseUIColor());
        final float srcLblW = srcLbl.getPosition().getWidth();
        add(srcLbl).inTL(opad, GAP_TOP_1);
        add(destLbl).inTL(opad + srcLblW + pad*2 + nameGap, GAP_TOP_1);
        add(destArrow).inTL(opad + srcLblW + pad + (nameGap - arrowS) / 2, GAP_TOP_1);

        final int GAP_TOP_2 = GAP_TOP_1 + 20;

        final String distValue = Misc.getRoundedValueOneAfterDecimalIfNotWhole(mission.dist) + str("lightYearsAbb");
        final LabelAPI distLbl = settings.createLabel(str("uiDistTxt") + distValue, Fonts.DEFAULT_SMALL);
        distLbl.setHighlightColor(highlight);
        distLbl.setHighlight(distValue);
        add(distLbl).inTL(opad, GAP_TOP_2);

        final String durValue =UIUtils.getTimeWithDay(mission.totalDur, true);
        final LabelAPI durLbl = settings.createLabel(str("uiTotalDurTxt") + durValue, Fonts.DEFAULT_SMALL);
        durLbl.setHighlightColor(highlight);
        durLbl.setHighlight(durValue);
        add(durLbl).inTL(opad + opad*2 + 70, GAP_TOP_2);

        final String fuelValue = NumFormat.engNotate(mission.fuelCost) + str("uiUnits");
        final LabelAPI fuelCostLbl = settings.createLabel(str("uiFuelNeededTxt") + fuelValue, Fonts.DEFAULT_SMALL);
        fuelCostLbl.setHighlightColor(highlight);
        fuelCostLbl.setHighlight(fuelValue);
        add(fuelCostLbl).inTL(opad + opad*3 + 200, GAP_TOP_2);

        final int GAP_TOP_3 = GAP_TOP_2 + 20;

        final String sliderTxt = switch(mission.status) {
            case SCHEDULED, DELIVERED, CANCELLED, LOST -> mission.status.getDisplayText();
            default -> UIUtils.getTimeWithDay(mission.durRemaining, true);
        };
        final Slider timeSlider = new Slider(m_panel, sliderTxt, 0f, mission.totalDur, panelW - opad*2, 32);
        timeSlider.showLabelOnly = true;
        timeSlider.setUserAdjustable(false);
        add(timeSlider).inTL(opad, GAP_TOP_3);
        switch (mission.status) {
        case LOST:
            timeSlider.setBarColor(UIColors.COM_DEFICIT);
            break;
        case DELIVERED:
            timeSlider.setBarColor(UIColors.COM_EXPORT);
            timeSlider.setProgress(mission.totalDur);
            break;
        case CANCELLED:
            timeSlider.setBarColor(UIColors.COM_IMPORT);
            break;
        default:
            timeSlider.setProgress(mission.totalDur - mission.durRemaining);
            break;
        }

        final int GAP_TOP_4 = GAP_TOP_3 + 42;
        final int perEntryW = (panelW - opad*2) / 4;
        final int iconS = 28;

        final IconValuePair cargoPair = new IconValuePair(m_panel, perEntryW, iconS, CRATES, mission.cargoAmount, true, null);
        final IconValuePair fuelPair = new IconValuePair(m_panel, perEntryW, iconS, FUEL, mission.fuelAmount, true, null);
        final IconValuePair crewPair = new IconValuePair(m_panel, perEntryW, iconS, BERTH, mission.crewAmount, true, null);
        final IconValuePair combatPair = new IconValuePair(m_panel, perEntryW, iconS, COMBAT, mission.combatPowerTarget, true, null);

        cargoPair.icon().texColor = UIColors.CARGO_COLOR;

        add(cargoPair).inTL(opad, GAP_TOP_4);
        add(fuelPair).inTL(opad + perEntryW, GAP_TOP_4);
        add(crewPair).inTL(opad + perEntryW*2, GAP_TOP_4);
        add(combatPair).inTL(opad + perEntryW*3, GAP_TOP_4);

        final long creditsValue = (long) mission.credits.computeEffective(0f);
        final String creditsStr = NumFormat.formatCreditAbs(creditsValue);
        final LabelAPI costLbl = settings.createLabel(str("uiLedgerTxt") + creditsStr, Fonts.INSIGNIA_LARGE);
        costLbl.setHighlightColor(creditsValue < 0l ? negative : highlight);
        costLbl.setHighlight(creditsStr);
        add(costLbl).inBL(opad, opad);
    }

    @Override
    public void renderBelow(float alpha) {
        super.renderBelow(alpha);

        border.render(pos.getX(), pos.getY(), alpha);
    }

    public static final TooltipBuilder createMissionTp(TradeMission mission, boolean detailed) {
        return (tp, expanded) -> {
            final EconomyEngine engine = EconomyEngine.instance();
            tp.addTitle(detailed ? str("tradeMissionTitle") : str("tradeFleetTitle"), base);

            final int beginTime = mission.startOffset - engine.getCyclesSinceTrade();
            final int arrivalTime = mission.durRemaining - (int) mission.transferDur;

            final String statusStr = switch(mission.status) {
                case SCHEDULED -> !detailed ? str("tradeMissionStatusScheduled") :
                    strf("tradeMissionScheduledTxt", beginTime, UIUtils.getDayOrDays(beginTime));
                case IN_SRC_ORBIT_LOADING -> strf("tradeMissionLoadingTxt", mission.src.getName());
                case IN_TRANSIT -> strf("tradeMissionTransitTxt", arrivalTime, UIUtils.getDayOrDays(arrivalTime));
                case IN_DST_ORBIT_UNLOADING -> strf("tradeMissionUnloadingTxt", mission.dest.getName());
                case DELIVERED -> str("tradeMissionStatusDelivered");
                case CANCELLED -> str("tradeMissionStatusCancelled");
                case LOST -> str("tradeMissionLostTxt");
            };

            if (detailed) {
                String largestCom = "";
                double amount = 0;
                for (TradeCom flow : mission.cargo) {
                    if (flow.amount > amount) {
                        amount = flow.amount;
                        largestCom = flow.comID;
                    }
                }
                largestCom = settings.getCommoditySpec(largestCom).getName();

                final String fleetOriginStr = mission.usedFactionFleet ?
                    str("tradeMissionWidgetTpTxt1") : str("tradeMissionWidgetTpTxt2");
                final String fuelOriginStr = mission.usedFuelFromStockpiles ?
                    str("tradeMissionWidgetTpTxt3") : str("tradeMissionWidgetTpTxt4");

                tp.addPara(strf("tradeMissionWidgetTpTxt5", statusStr, fleetOriginStr, fuelOriginStr),
                    pad, new Color[]{
                        base,
                        mission.src.getFaction().getBaseUIColor(),
                        mission.dest.getFaction().getBaseUIColor(),
                        highlight, highlight, highlight, base, highlight, negative,
                        highlight
                    },
                    mission.inFaction ? str("uiInfactionLowercase") : str("uiGlobalLowercase"),
                    mission.src.getName(), mission.dest.getName(),
                    Misc.getRoundedValueOneAfterDecimalIfNotWhole(mission.dist) + str("lightYearsAbb"),
                    UIUtils.getTimeWithDay(mission.totalDur, true),
                    NumFormat.engNotate(mission.fuelCost),
                    mission.crewAmount > mission.cargoAmount ? str("uiCrewTxt") : mission.fuelAmount > mission.cargoAmount ? str("uiFuelTxt") : str("uiCargoTxt"),
                    largestCom, NumFormat.formatCreditAbs(mission.credits.computeEffective(0f)),
                    NumFormat.engNotate(mission.combatPowerTarget)
                );
            } else {
                tp.addPara(str("tradeMissionWidgetTpTxt6"), pad,
                    new Color[]{mission.src.getFaction().getBaseUIColor(), mission.dest.getFaction().getBaseUIColor(),
                        highlight, highlight
                    }, mission.src.getName(), mission.dest.getName(),
                    Misc.getRoundedValueOneAfterDecimalIfNotWhole(mission.dist) + str("lightYearsAbb"),
                    UIUtils.getTimeWithDay(arrivalTime)
                );

                final int fleetSize = mission.allocatedShips.size();
                tp.addPara(str("tradeMissionWidgetTpTxt7"), pad, highlight, String.valueOf(fleetSize));
            }

            final int gridWidth = 390;
            final int valueWidth = 60;
            int rowCount = 0;

            tp.addPara(str("uiShipmentListTitle"), base, opad);
            tp.beginGridFlipped(gridWidth, 2, valueWidth, hpad);
            for (TradeCom flow : mission.cargo) {
                tp.addToGrid(0, rowCount++, settings.getCommoditySpec(flow.comID).getName(),
                    NumFormat.engNotate(flow.amount));
            }
            tp.addGrid(0);

            if (detailed) {
                rowCount = 0;
    
                tp.addPara(str("uiMissionLedgerTitle"), base, opad);
                tp.beginGridFlipped(gridWidth, 2, valueWidth, hpad);
                for (StatMod mod : mission.credits.getFlatBonuses().values()) {
                    tp.addToGrid(0, rowCount++, mod.desc, NumFormat.engNotate(mod.value)
                        +Strings.C, mod.value < 0f ? negative : positive
                    );
                }
                tp.addGrid(0);
    
                final int totalEntries = mission.allocatedShips.size();
                rowCount = 0;
                
                tp.addPara(str("uiFleetMembersTitle"), base, opad);
                tp.beginGridFlipped(gridWidth/2, 4, valueWidth, hpad);
                for (var entry : mission.allocatedShips.singleEntrySet()) {
                    if (rowCount >= EconConfig.TRADE_MISSION_MAX_DISPLAYED_SHIPS) break;
    
                    final ShipHullSpecAPI spec = settings.getHullSpec(entry.getKey());
                    final String name = spec.getHullNameWithDashClass();
                    tp.addToGrid((rowCount % 2 == 0 ? 0 : 1), rowCount / 2, name, entry.getValue().toString());
                    rowCount++;
                }
    
                tp.addGrid(0);
    
                if (totalEntries > EconConfig.TRADE_MISSION_MAX_DISPLAYED_SHIPS) {
                    final int remaining = totalEntries - EconConfig.TRADE_MISSION_MAX_DISPLAYED_SHIPS;
                    tp.addPara(str("tradeMissionWidgetTpTxt8"), opad, highlight, String.valueOf(remaining));
                }
            }
            
            final String virtualStateStr = mission.spawnedFleetFinishedJob ? str("virtualFleetTpTxt") : str("activeFleetTpTxt");
            tp.addPara(virtualStateStr, gray, opad);
        };
    }
}
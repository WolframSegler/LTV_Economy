package wfg.ltv_econ.ui.fleet;

import java.awt.Color;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.SettingsAPI;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.impl.campaign.ids.Commodities;
import com.fs.starfarer.api.impl.campaign.ids.Strings;
import com.fs.starfarer.api.ui.Alignment;
import com.fs.starfarer.api.ui.Fonts;
import com.fs.starfarer.api.ui.LabelAPI;
import com.fs.starfarer.api.ui.UIPanelAPI;
import com.fs.starfarer.api.util.Misc;

import wfg.ltv_econ.constants.UIColors;
import wfg.ltv_econ.economy.commodity.TradeCom;
import wfg.ltv_econ.economy.engine.EconomyEngine;
import wfg.ltv_econ.economy.fleet.TradeMission;
import wfg.native_ui.internal.util.BorderRenderer;
import wfg.native_ui.ui.component.NativeComponents;
import wfg.native_ui.ui.component.TooltipComp;
import wfg.native_ui.ui.component.OutlineComp.OutlineType;
import wfg.native_ui.ui.container.DockPanel;
import wfg.native_ui.ui.core.UIBuildableAPI;
import wfg.native_ui.ui.core.UIElementFlags.HasTooltip;
import wfg.native_ui.ui.panel.CustomPanel;
import wfg.native_ui.ui.visual.SpritePanel.Base;
import wfg.native_ui.ui.widget.Slider;
import wfg.native_ui.util.NativeUiUtils;
import wfg.native_ui.util.NumFormat;
import wfg.native_ui.util.NativeUiUtils.AnchorType;

import static wfg.native_ui.util.UIConstants.*;

public class TradeMissionWidget extends CustomPanel<TradeMissionWidget> implements UIBuildableAPI, HasTooltip {
    private static final SettingsAPI settings = Global.getSettings();
    private static final SpriteAPI SMUGGLING = settings.getSprite("icons", "smuggling");
    private static final SpriteAPI SHIP_OUTLINE = settings.getSprite("icons", "ship_outline");
    private static final SpriteAPI ARROW = settings.getSprite("ui", "arrow");
    private static final SpriteAPI CRATES = settings.getSprite("icons", "cargo_crates");
    private static final SpriteAPI FUEL = settings.getSprite(settings.getCommoditySpec(Commodities.FUEL).getIconName());
    private static final SpriteAPI CREW = settings.getSprite(settings.getCommoditySpec(Commodities.CREW).getIconName());
    private static final SpriteAPI COMBAT = settings.getSprite("ui", "icon_kinetic");
    private static final int MAX_DISPLAYED_SHIPS = 20; // TODO add to config

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
        tooltip.builder = (tp, expanded) -> {
            final EconomyEngine engine = EconomyEngine.instance();
            tp.addTitle("Trade Mission", base);

            final int beginTime = mission.startOffset - engine.getCyclesSinceTrade();
            final int arrivalTime = mission.durRemaining - (int) mission.transferDur;

            final String statusStr = switch(mission.status) {
                case SCHEDULED -> "The mission is scheduled to begin preparations in " + beginTime + (beginTime < 2 ? " Day." : " Days.");
                case IN_SRC_ORBIT_LOADING -> "The shipment is currently being loaded into the fleet.";
                case IN_TRANSIT -> "The shipment is in-transit and is projected to arrive to its destination in " + arrivalTime + (arrivalTime < 2 ? " Day." : " Days.");
                case IN_DST_ORBIT_UNLOADING -> "The fleet is orbiting " + mission.dest.getName() + " and unloading its shipment.";
                case DELIVERED -> "The shipment has been delivered.";
                case CANCELLED -> "The shipment was cancelled.";
                case LOST -> "The shipment, along with the fleet, was lost in combat.";
            };

            final String fleetOriginStr = mission.usedFactionFleet ?
                "The fleet was assembled using ships from the faction inventory." :
                "The fleet belongs to an independent captain who was hired to deliver the shipment.";

            final String fuelOriginStr = mission.usedFuelFromStockpiles ?
                "The fuel for the journey was taken from local stockpiles." :
                "The fuel was purchased at a premium from independent merchants.";

            String largestCom = "";
            double amount = 0;
            for (TradeCom flow : mission.cargo) {
                if (flow.amount > amount) {
                    amount = flow.amount;
                    largestCom = flow.comID;
                }
            }
            largestCom = settings.getCommoditySpec(largestCom).getName();

            tp.addPara("The %s trade mission from %s to %s is expected to cover a distance of %s in %s and burn %s units of fuel. " +
                statusStr + " The shipment consists primarily of %s and the single most abundant commodity is %s. " +
                fleetOriginStr + " The costs incurred for this shipment was %s. " + fuelOriginStr +
                " The fleet posesses a combat power of %s.",
                pad, new Color[]{
                    base,
                    mission.src.getFaction().getBaseUIColor(),
                    mission.dest.getFaction().getBaseUIColor(),
                    highlight, highlight, highlight, base, highlight, negative,
                    highlight
                },
                mission.inFaction ? "in-faction" : "global",
                mission.src.getName(), mission.dest.getName(),
                Misc.getRoundedValueOneAfterDecimalIfNotWhole(mission.dist) + "LY",
                mission.totalDur + (mission.totalDur <= 1 ? " Day" : " Days"),
                NumFormat.engNotate(mission.fuelCost),
                mission.crewAmount > mission.cargoAmount ? "crew" : mission.fuelAmount > mission.cargoAmount ? "fuel" : "cargo",
                largestCom, NumFormat.formatCreditAbs(mission.credits.computeEffective(0f)),
                NumFormat.engNotate(mission.combatPowerTarget)
            );

            final int gridWidth = 390;
            final int valueWidth = 40;
            int rowCount = 0;

            tp.addPara("Shipment List", base, opad);
            tp.beginGridFlipped(gridWidth, 2, valueWidth, hpad);
            for (TradeCom flow : mission.cargo) {
                tp.addToGrid(0, rowCount++, settings.getCommoditySpec(flow.comID).getName(),
                    NumFormat.engNotate(flow.amount));
            }
            tp.addGrid(0);

            final int totalEntries = mission.allocatedShips.size();
            rowCount = 0;
            
            tp.addPara("Fleet Members", base, opad);
            tp.beginGridFlipped(gridWidth/2, 4, valueWidth, hpad);
            for (var entry : mission.allocatedShips.singleEntrySet()) {
                if (rowCount >= MAX_DISPLAYED_SHIPS) break;

                final ShipHullSpecAPI spec = settings.getHullSpec(entry.getKey());
                final String name = spec.getHullNameWithDashClass();
                tp.addToGrid((rowCount % 2 == 0 ? 0 : 1), rowCount / 2, name, entry.getValue().toString());
                rowCount++;
            }

            tp.addGrid(0);

            if (totalEntries > MAX_DISPLAYED_SHIPS) {
                final int remaining = totalEntries - MAX_DISPLAYED_SHIPS;
                tp.addPara("... and %s more ship type%s", opad, Misc.getHighlightColor(),
                    String.valueOf(remaining), remaining == 1 ? "" : "s"
                );
            }
        };
        
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

        final String distValue = Misc.getRoundedValueOneAfterDecimalIfNotWhole(mission.dist) + "LY";
        final LabelAPI distLbl = settings.createLabel("Dist: " + distValue, Fonts.DEFAULT_SMALL);
        distLbl.setHighlightColor(highlight);
        distLbl.setHighlight(distValue);
        add(distLbl).inTL(opad, GAP_TOP_2);

        final String durValue = mission.totalDur + (mission.totalDur <= 1 ? " Day" : " Days");
        final LabelAPI durLbl = settings.createLabel("Total Dur: " + durValue, Fonts.DEFAULT_SMALL);
        durLbl.setHighlightColor(highlight);
        durLbl.setHighlight(durValue);
        add(durLbl).inTL(opad + opad*2 + 70, GAP_TOP_2);

        final String fuelValue = NumFormat.engNotate(mission.fuelCost) + (mission.fuelCost <= 1 ? " Unit" : " Units");
        final LabelAPI fuelCostLbl = settings.createLabel("Fuel Needed: " + fuelValue, Fonts.DEFAULT_SMALL);
        fuelCostLbl.setHighlightColor(highlight);
        fuelCostLbl.setHighlight(fuelValue);
        add(fuelCostLbl).inTL(opad + opad*3 + 200, GAP_TOP_2);

        final int GAP_TOP_3 = GAP_TOP_2 + 20;

        final String sliderTxt = switch(mission.status) {
            case SCHEDULED, DELIVERED, CANCELLED, LOST -> mission.status.getDisplayText();
            default -> mission.durRemaining + (mission.durRemaining <= 1 ? " Day" : " Days");
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
        final int iconLS = 32;

        final Base cargoIcon = new Base(m_panel, iconS, iconS, CRATES, UIColors.CARGO_COLOR, null);
        final Base fuelIcon = new Base(m_panel, iconLS, iconLS, FUEL, null, null);
        final Base crewIcon = new Base(m_panel, iconS, iconS, CREW, null, null);
        final Base combatIcon = new Base(m_panel, iconS, iconS, COMBAT, null, null);
        add(cargoIcon).inTL(opad, GAP_TOP_4);
        add(fuelIcon).inTL(opad + perEntryW, GAP_TOP_4);
        add(crewIcon).inTL(opad + perEntryW*2, GAP_TOP_4);
        add(combatIcon).inTL(opad + perEntryW*3, GAP_TOP_4);

        final LabelAPI cargoLbl = settings.createLabel(Strings.X + NumFormat.engNotate(mission.cargoAmount), Fonts.DEFAULT_SMALL);
        final LabelAPI fuelLbl = settings.createLabel(Strings.X + NumFormat.engNotate(mission.fuelAmount), Fonts.DEFAULT_SMALL);
        final LabelAPI crewLbl = settings.createLabel(Strings.X + NumFormat.engNotate(mission.crewAmount), Fonts.DEFAULT_SMALL);
        final LabelAPI combatLbl = settings.createLabel(Strings.X + NumFormat.engNotate(mission.combatPowerTarget), Fonts.DEFAULT_SMALL);
        cargoLbl.setAlignment(Alignment.LMID);
        fuelLbl.setAlignment(Alignment.LMID);
        crewLbl.setAlignment(Alignment.LMID);
        combatLbl.setAlignment(Alignment.LMID);
        cargoLbl.setColor(highlight);
        fuelLbl.setColor(highlight);
        crewLbl.setColor(highlight);
        combatLbl.setColor(highlight);
        add(cargoLbl).setSize(perEntryW, iconS).inTL(opad + iconS + pad, GAP_TOP_4);
        add(fuelLbl).setSize(perEntryW, iconS).inTL(opad + iconS + perEntryW + pad*2, GAP_TOP_4);
        add(crewLbl).setSize(perEntryW, iconS).inTL(opad + iconS + perEntryW*2 + pad*3, GAP_TOP_4);
        add(combatLbl).setSize(perEntryW, iconS).inTL(opad + iconS + perEntryW*3 + pad*4, GAP_TOP_4);

        final long creditsValue = (long) mission.credits.computeEffective(0f);
        final String creditsStr = NumFormat.formatCreditAbs(creditsValue);
        final LabelAPI costLbl = settings.createLabel("Expenses: " + creditsStr, Fonts.INSIGNIA_LARGE);
        costLbl.setHighlightColor(creditsValue < 0l ? negative : highlight);
        costLbl.setHighlight(creditsStr);
        add(costLbl).inBL(opad, opad);
    }

    @Override
    public void renderBelow(float alpha) {
        super.renderBelow(alpha);

        border.render(pos.getX(), pos.getY(), alpha);
    }
}
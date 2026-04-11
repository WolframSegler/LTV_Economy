package wfg.ltv_econ.ui.fleet;

import java.awt.Color;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.SettingsAPI;
import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.impl.campaign.ids.Commodities;
import com.fs.starfarer.api.impl.campaign.ids.Strings;
import com.fs.starfarer.api.ui.Fonts;
import com.fs.starfarer.api.ui.LabelAPI;
import com.fs.starfarer.api.ui.UIPanelAPI;
import com.fs.starfarer.api.util.Misc;

import wfg.ltv_econ.constants.UIColors;
import wfg.ltv_econ.economy.commodity.ComTradeFlow;
import wfg.ltv_econ.economy.engine.EconomyEngine;
import wfg.ltv_econ.economy.fleet.TradeMission;
import wfg.native_ui.internal.util.BorderRenderer;
import wfg.native_ui.ui.component.NativeComponents;
import wfg.native_ui.ui.component.TooltipComp;
import wfg.native_ui.ui.core.UIBuildableAPI;
import wfg.native_ui.ui.core.UIElementFlags.HasTooltip;
import wfg.native_ui.ui.panel.CustomPanel;
import wfg.native_ui.ui.visual.SpritePanel.Base;
import wfg.native_ui.ui.widget.Slider;
import wfg.native_ui.util.NumFormat;

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

    private final TooltipComp tooltip = comp().get(NativeComponents.TOOLTIP);
    private final BorderRenderer border = new BorderRenderer("ui_border1", true);

    private final TradeMission mission;
    private final boolean isSrcMarket;

    public TradeMissionWidget(UIPanelAPI parent, int w, int h, TradeMission mission, boolean isSrcMarket) {
        super(parent, w, h);

        border.setSize(w + opad, h + opad);
        border.centerColor = new Color(20, 25, 35, 220);

        this.mission = mission;
        this.isSrcMarket = isSrcMarket;

        tooltip.width = 500f;
        tooltip.builder = (tp, expanded) -> {
            final EconomyEngine engine = EconomyEngine.instance();
            tp.addTitle("Trade Mission", base);

            // TODO make sure the text here is correct

            final int beginTime = mission.startOffset - engine.getCyclesSinceTrade();
            final int arrivalTime = mission.durRemaining - (int) mission.transferDur;

            final String statusStr = switch(mission.status) {
                case SCHEDULED -> "The mission is scheduled to begin preparations in " + beginTime + (beginTime < 2 ? "Day." : "Days.");
                case IN_SRC_ORBIT_LOADING -> "The shipments are currently being loaded into the fleet.";
                case IN_TRANSIT -> "The shipment is in-transit and is projected to arrive to its destination in " + arrivalTime + (arrivalTime < 2 ? "Day." : "Days.");
                case IN_DST_ORBIT_UNLOADING -> "The fleet is orbiting " + mission.dest.getName() + " and unloading its shipment";
                case DELIVERED -> "The ship has been delivered.";
                case CANCELLED -> "The shipment was cancelled";
                case LOST -> "The shipment, along with the fleet, was lost in combat.";
            };

            final String fleetOriginStr = mission.usedFactionFleet ?
                "The fleet was assembled using ships from the faction inventory." :
                "The fleet belongs to an independent captain who was hired to deliver the shipment.";

            final String fuelOriginStr = mission.usedFuelFromStockpiles ?
                "The fuel for the journey was taken from local stockpiles." :
                "The fuel was purchased at a premium from independent merchants.";

            String largestCom = "";
            float amount = 0;
            for (ComTradeFlow flow : mission.cargo) {
                if (flow.amount > amount) {
                    amount = flow.amount;
                    largestCom = flow.comID;
                }
            }

            tp.addPara("The %s trade mission is from %s to %s and is expected to cover a distance of %s in %s and burn %s units of fuel. " +
                statusStr + " The shipment consists primarily of %s and the single most abundant commodity was %s. " +
                fleetOriginStr + " The costs incurred for this shipment was %s credits. " + fuelOriginStr +
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
                NumFormat.engNotation(mission.fuelCost),
                mission.crewAmount > mission.cargoAmount ? "crew" : mission.fuelAmount > mission.cargoAmount ? "fuel" : "cargo",
                largestCom, NumFormat.formatCredit((long) mission.credits.computeEffective(0f)),
                NumFormat.engNotation(mission.combatPowerTarget)
            );

            final int gridWidth = 430;
            final int valueWidth = 50;
            int rowCount = 0;

            tp.beginGridFlipped(gridWidth, 2, valueWidth, hpad);

            tp.addPara("Shipment List", base, pad);
            for (ComTradeFlow flow : mission.cargo) {
                tp.addToGrid(0, rowCount++, settings.getCommoditySpec(flow.comID).getName(),
                    NumFormat.engNotation(flow.amount));
            }
            tp.addGrid(0);
        };
        
        buildUI();
    }

    @Override
    public void buildUI() {
        final int panelW = (int) pos.getWidth();

        final LabelAPI statusLabel = settings.createLabel(mission.status.getDisplayText(), Fonts.ORBITRON_12);
        statusLabel.setColor(mission.status.getDisplayColor());
        add(statusLabel).inTL(hpad, hpad);

        if (mission.smuggling) {
            final int statusW = (int) statusLabel.getPosition().getWidth();
            final Base smugglingIcon = new Base(m_panel, 19, 9, null, null, null);
            smugglingIcon.setSprite(SMUGGLING);
            add(smugglingIcon).inTL(hpad + pad + statusW, hpad);
        }

        final String crestID = (isSrcMarket ? mission.dest : mission.src).getFaction().getCrest();
        final Base factionIcon = new Base(m_panel, 20, 20, crestID, null, null);
        add(factionIcon).inTR(hpad, hpad);
        if (mission.inFaction) {
            factionIcon.texHaloColor = UIColors.IN_FACTION;
            factionIcon.drawTextureHalo = true;
        }

        final Base fleetIcon = new Base(m_panel, 16, 32, null, null, null);
        fleetIcon.setSprite(SHIP_OUTLINE);
        fleetIcon.texHaloColor = mission.usedFactionFleet ? UIColors.IN_FACTION : gray; 
        fleetIcon.drawTextureHalo = true;
        add(fleetIcon).inTR(hpad + pad + 20, hpad);

        if (mission.usedFactionFleet && !mission.usedFuelFromStockpiles) {
            final Base fuelWarningIcon = new Base(m_panel, 20, 20, null, null, null);
            fuelWarningIcon.setSprite(FUEL);
            fuelWarningIcon.texHaloColor = UIColors.COM_DEFICIT;
            fuelWarningIcon.drawTextureHalo = true;
            add(fuelWarningIcon).inTR(hpad + pad*2 + 36, hpad);
        }

        final int GAP_TOP_1 = 50;

        final LabelAPI srcLbl = settings.createLabel(mission.src.getName(), Fonts.DEFAULT_SMALL);
        final LabelAPI destLbl = settings.createLabel(mission.dest.getName(), Fonts.DEFAULT_SMALL);
        final Base destArrow = new Base(m_panel, 24, 18, null, Color.YELLOW, null);
        destArrow.setSprite(ARROW);
        final float srcLblW = srcLbl.getPosition().getWidth();
        add(srcLbl).inTL(hpad, GAP_TOP_1);
        add(destLbl).inTL(hpad + srcLblW + 32, GAP_TOP_1);
        add(destArrow).inTL(hpad + srcLblW + 4, GAP_TOP_1);

        final int GAP_TOP_2 = GAP_TOP_1 + 20;

        final String distValue = Misc.getRoundedValueOneAfterDecimalIfNotWhole(mission.dist) + "LY";
        final LabelAPI distLbl = settings.createLabel("Dist: " + distValue, Fonts.DEFAULT_SMALL);
        distLbl.setHighlightColor(highlight);
        distLbl.setHighlight(distValue);
        add(distLbl).inTL(hpad, GAP_TOP_2);

        final String durValue = mission.totalDur + (mission.totalDur <= 1 ? " Day" : " Days");
        final LabelAPI durLbl = settings.createLabel("Total Dur: " + durValue, Fonts.DEFAULT_SMALL);
        durLbl.setHighlightColor(highlight);
        durLbl.setHighlight(durValue);
        add(durLbl).inTL(hpad + opad*2 + 100, GAP_TOP_2);

        final String fuelValue = mission.fuelCost + (mission.fuelCost <= 1 ? " Unit" : " Units");
        final LabelAPI fuelCostLbl = settings.createLabel("Fuel Needed: " + fuelValue, Fonts.DEFAULT_SMALL);
        fuelCostLbl.setHighlightColor(highlight);
        fuelCostLbl.setHighlight(fuelValue);
        add(fuelCostLbl).inTL(hpad + opad*3 + 200, GAP_TOP_2);

        final int GAP_TOP_3 = GAP_TOP_2 + 20;

        final String sliderTxt = switch(mission.status) {
            case SCHEDULED, DELIVERED, CANCELLED, LOST -> mission.status.getDisplayText();
            default -> mission.durRemaining + (mission.durRemaining <= 1 ? " Day" : " Days");
        };
        final Slider timeSlider = new Slider(m_panel, sliderTxt, 0f, mission.totalDur, panelW - opad, 32);
        timeSlider.showLabelOnly = true;
        timeSlider.setUserAdjustable(false);
        timeSlider.setProgress(mission.totalDur - mission.durRemaining);
        add(timeSlider).inTL(hpad, GAP_TOP_3);
        switch (mission.status) {
            case LOST: timeSlider.setBarColor(UIColors.COM_DEFICIT); break;
            case DELIVERED: timeSlider.setBarColor(UIColors.COM_EXPORT); break;
            case CANCELLED: timeSlider.setBarColor(UIColors.COM_IMPORT); break;
            default: break;
        }

        final int GAP_TOP_4 = GAP_TOP_3 + 42;
        final int perEntryW = (panelW - opad) / 4;
        final int iconS = 28;

        final Base cargoIcon = new Base(m_panel, iconS, iconS, null, null, null);
        final Base fuelIcon = new Base(m_panel, iconS, iconS, null, null, null);
        final Base crewIcon = new Base(m_panel, iconS, iconS, null, null, null);
        final Base combatIcon = new Base(m_panel, iconS, iconS, null, null, null);
        cargoIcon.setSprite(CRATES);
        fuelIcon.setSprite(FUEL);
        crewIcon.setSprite(CREW);
        combatIcon.setSprite(COMBAT);
        add(cargoIcon).inTL(hpad, GAP_TOP_4);
        add(fuelIcon).inTL(hpad + perEntryW, GAP_TOP_4);
        add(crewIcon).inTL(hpad + perEntryW*2, GAP_TOP_4);
        add(combatIcon).inTL(hpad + perEntryW*3, GAP_TOP_4);

        final LabelAPI cargoLbl = settings.createLabel(Strings.X + NumFormat.engNotation(mission.cargoAmount), Fonts.DEFAULT_SMALL);
        final LabelAPI fuelLbl = settings.createLabel(Strings.X + NumFormat.engNotation(mission.fuelAmount), Fonts.DEFAULT_SMALL);
        final LabelAPI crewLbl = settings.createLabel(Strings.X + NumFormat.engNotation(mission.crewAmount), Fonts.DEFAULT_SMALL);
        final LabelAPI combatLbl = settings.createLabel(Strings.X + NumFormat.engNotation(mission.combatPowerTarget), Fonts.DEFAULT_SMALL);
        cargoLbl.setColor(highlight);
        fuelLbl.setColor(highlight);
        crewLbl.setColor(highlight);
        combatLbl.setColor(highlight);
        add(cargoLbl).inTL(hpad + iconS + pad, GAP_TOP_4);
        add(fuelLbl).inTL(hpad + perEntryW + iconS*2 + pad*2, GAP_TOP_4);
        add(crewLbl).inTL(hpad + perEntryW*2 + iconS*3 + pad*3, GAP_TOP_4);
        add(combatLbl).inTL(hpad + perEntryW*3 + iconS*4 + pad*4, GAP_TOP_4);

        final long creditsValue = (long) mission.credits.computeEffective(0f);
        final String creditsStr = NumFormat.formatCredit(creditsValue);
        final LabelAPI costLbl = settings.createLabel("Credits: " + creditsStr, Fonts.INSIGNIA_LARGE);
        costLbl.setHighlightColor(creditsValue < 0l ? negative : highlight);
        costLbl.setHighlight(creditsStr);
        add(costLbl).inBL(hpad, hpad);
    }

    @Override
    public void renderBelow(float alpha) {
        super.renderBelow(alpha);

        border.render(pos.getX() - hpad, pos.getY() - hpad, alpha);
    }
}
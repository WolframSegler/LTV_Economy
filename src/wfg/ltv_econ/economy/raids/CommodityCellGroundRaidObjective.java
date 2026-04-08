package wfg.ltv_econ.economy.raids;

import static wfg.native_ui.util.UIConstants.*;

import java.util.List;
import java.util.Map;
import java.util.Random;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CargoAPI;
import com.fs.starfarer.api.campaign.CargoAPI.CargoItemType;
import com.fs.starfarer.api.campaign.CargoStackAPI;
import com.fs.starfarer.api.campaign.TextPanelAPI;
import com.fs.starfarer.api.campaign.econ.CommoditySpecAPI;
import com.fs.starfarer.api.campaign.econ.Industry;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.combat.MutableStat;
import com.fs.starfarer.api.impl.campaign.graid.BaseGroundRaidObjectivePluginImpl;
import com.fs.starfarer.api.impl.campaign.rulecmd.salvage.MarketCMD;
import com.fs.starfarer.api.impl.campaign.rulecmd.salvage.MarketCMD.RaidDangerLevel;
import com.fs.starfarer.api.loading.Description;
import com.fs.starfarer.api.loading.Description.Type;
import com.fs.starfarer.api.ui.CustomPanelAPI;
import com.fs.starfarer.api.ui.IconGroupAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.ui.UIComponentAPI;
import com.fs.starfarer.api.ui.UIPanelAPI;
import com.fs.starfarer.api.util.Misc;

import rolflectionlib.util.RolfLectionUtil;
import wfg.ltv_econ.config.EconomyConfig;
import wfg.ltv_econ.constants.UIColors;
import wfg.ltv_econ.economy.CompatLayer;
import wfg.ltv_econ.economy.commodity.CommodityCell;
import wfg.ltv_econ.ui.marketInfo.CommodityRowPanel;
import wfg.ltv_econ.ui.reusable.StockpileInfoBar;
import wfg.ltv_econ.util.Arithmetic;
import wfg.native_ui.ui.panel.CustomPanel;
import wfg.native_ui.util.NumFormat;

public class CommodityCellGroundRaidObjective extends BaseGroundRaidObjectivePluginImpl {
    public final CommodityCell cell;
	public int deficitActuallyCaused;

	private UIPanelAPI iconPanel = null;
	
	public CommodityCellGroundRaidObjective(CommodityCell cell) {
		super(cell.market, cell.comID);

		this.cell = cell;
		setSource(computeCommoditySource(cell));
	}

    @Override
	public final int performRaid(CargoAPI loot, Random random, float lootMult, TextPanelAPI text) {
		if (marinesAssigned <= 0) return 0;
		
		lootMult *= 0.9f + random.nextFloat() * 0.2f;
        quantityLooted = (int) Math.max(1.0, Math.floor(getQuantity(marinesAssigned, lootMult)));
		
		loot.addCommodity(id, quantityLooted);
		cell.addStoredAmount(-quantityLooted);
		
		xpGained = (int) (quantityLooted * cell.spec.getBasePrice() * XP_GAIN_VALUE_MULT);
		return xpGained;
	}

    @Override
	public final int getDeficitCaused() {
		return CompatLayer.cargoUnitToEconUnit(getQuantity(getMarinesAssigned()));
	}

    @Override
	public final int getCargoSpaceNeeded() {
		if (!cell.spec.isFuel() && !cell.spec.isPersonnel()) {
			return (int) (cell.spec.getCargoSpace() * getQuantity(getMarinesAssigned()));
		}
		return 0;
	}
	
    @Override
	public final int getFuelSpaceNeeded() {
		if (cell.spec.isFuel()) {
			return (int) (cell.spec.getCargoSpace() * getQuantity(getMarinesAssigned()));
		}
		return 0;
	}
	
    @Override
	public final int getProjectedCreditsValue() {
		return (int) (cell.spec.getBasePrice() * getQuantity(getMarinesAssigned()));
	}

    @Override
	public float getQuantity(int marines) {
        return getQuantity(marines, 1.0);
	}

    public final float getQuantity(int marines, double mult) {
        return getQuantity(cell, marines, mult);
	}

	@Override
	public final void createTooltip(TooltipMakerAPI tp, boolean expanded) {
		final Description comDesc = Global.getSettings().getDescription(id, Type.RESOURCE);
		
		tp.addPara(comDesc.getText1FirstPara(), 0f);
		
		tp.addPara("Base value: %s per unit", opad, highlight, Misc.getDGSCredits(cell.spec.getBasePrice()));

        if (expanded) addStockpileLegend(tp, expanded);
	}
    
	@Override
	public String getQuantityString(int marines) {
		return NumFormat.engNotation(getQuantity(Math.max(1, marines)));
	}

    @Override
	public final RaidDangerLevel getDangerLevel() {
		return getDangerLevel(cell.spec, cell, source);
	}

    @Override
	public final void addIcons(IconGroupAPI iconGroup) {
		iconPanel = (UIPanelAPI) iconGroup;
	}

    @Override
	public final String getCommodityIdForDeficitIcons() {
		addInfoBar(iconPanel, cell);

		return cell.comID;
	}

    @Override
	public final boolean hasTooltip() {
		return true;
	}

    @Override
    public final boolean isTooltipExpandable() {
        return true;
    }

    @Override
	public final String getName() {
		return cell.spec.getName();
	}

    @Override
	public final CargoStackAPI getStackForIcon() {
		return Global.getFactory().createCargoStack(CargoItemType.RESOURCES, getId(), null);
	}

    @Override
	public final int getValue(int marines) {
		return (int) (getQuantity(marines) * cell.spec.getBasePrice());
	}

    @Override
	public final float getQuantitySortValue() {
		return QUANTITY_SORT_TIER_0 + getQuantity(1);
	}

	@SuppressWarnings("unchecked")
	protected static final void addInfoBar(final UIPanelAPI panel, final CommodityCell cell) {
		if (panel == null) return;
		final List<UIComponentAPI> children = (List<UIComponentAPI>)
			RolfLectionUtil.invokeMethodDirectly(CustomPanel.getChildrenNonCopyMethod, panel
		);
		for (UIComponentAPI child : children) {
			if (child instanceof CustomPanelAPI custom) {
				if (custom.getPlugin() instanceof StockpileInfoBar) return;
			}
		}

		final int h = 24;
		final StockpileInfoBar infoBar = new StockpileInfoBar(panel, 130, h, true, cell);
        panel.addComponent(infoBar.getPanel()).inBL(opad + pad, -h/2);
	}

    protected static final Industry computeCommoditySource(CommodityCell cell) {
		final RaidDangerLevel base = cell.spec.getBaseDanger();
        final MarketAPI market = cell.market;
		Industry best = null;
		float bestScore = 0;

        for (Map.Entry<String, MutableStat> entry : cell.getIndProductionStats().singleEntrySet()) {
            final Industry ind = market.getIndustry(entry.getKey());
            if (ind == null) continue;

            final RaidDangerLevel danger = ind.adjustCommodityDangerLevel(cell.comID, base);
			final float score = entry.getValue().getModifiedValue() - danger.ordinal();
			if (score > bestScore) {
				bestScore = score;
				best = ind;
			}
        }

        for (Map.Entry<String, MutableStat> entry : cell.getIndConsumptionStats().singleEntrySet()) {
            final Industry ind = market.getIndustry(entry.getKey());
            if (ind == null) continue;

            final RaidDangerLevel danger = ind.adjustCommodityDangerLevel(cell.comID, base);
			final float score = entry.getValue().getModifiedValue() - danger.ordinal();
			if (score > bestScore) {
				bestScore = score;
				best = ind;
			}
        }

		return best;
	}

	protected static final float getQuantity(CommodityCell cell, int marines, double mult) {
        final double stored = cell.getStored();
		final double maxQuantity = stored * EconomyConfig.RAID_STOCKPILES_ACCESS_RATIO;
        final double marineQuantity = maxQuantity * marines / MarketCMD.MAX_MARINE_TOKENS;
        final double deficitMult = 1.0 / (2.0 - cell.getStoredAvailabilityRatio());
        final double excessMult = Math.min(3.0, (cell.getStoredExcess() + stored) / stored);
        final double prodMult = Arithmetic.clamp(cell.getProduction(true) / (1.0 + cell.getTargetQuantum(true)), 1.0, 2.0);

        final double value = marineQuantity * EconomyConfig.RAID_BASE_EFF * deficitMult * excessMult * prodMult * mult;
        return (float) Math.min(maxQuantity, value);
	}

	protected static final RaidDangerLevel getDangerLevel(CommoditySpecAPI spec, CommodityCell cell, Industry source) {
		RaidDangerLevel danger = cell.spec.getBaseDanger();

        if (cell.getDesiredAvailabilityRatio() < 0.7f) {
            danger = danger.next();
        }
        if (cell.getStoredExcess() > 0.0) {
            danger = danger.prev();
        }
        if (source != null) {
            danger = source.adjustCommodityDangerLevel(cell.comID, danger);
        }
		return danger;
	}

	protected static final void addStockpileLegend(TooltipMakerAPI tp, boolean expanded) {
		final int iconSize = CommodityRowPanel.iconSize;
        int y = (int) tp.getHeightSoFar() + pad;
        String desc;

        desc = "Reserved stockpiles for local demand.";
        CommodityRowPanel.legendRowHelper(tp, y, null, desc, iconSize, false, UIColors.STOCKPILES_TARGET);
        
        y += iconSize + pad;

        desc = "Shortfall between the target and current stockpiles.";
        CommodityRowPanel.legendRowHelper(tp, y, null, desc, iconSize, false, UIColors.STOCKPILES_DEFICIT);
        
        y += iconSize + pad;

        desc = "Surplus stock available for export.";
        CommodityRowPanel.legendRowHelper(tp, y, null, desc, iconSize, false, UIColors.STOCKPILES_EXCESS);
        
        y += iconSize + pad;
    
        tp.setHeightSoFar(y);
	}
}
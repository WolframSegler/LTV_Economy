package wfg_ltv_econ;

import java.awt.Color;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.SpecialItemData;
import com.fs.starfarer.api.campaign.econ.CommodityOnMarketAPI;
import com.fs.starfarer.api.impl.campaign.ids.Commodities;
import com.fs.starfarer.api.impl.campaign.ids.Conditions;
import com.fs.starfarer.api.impl.campaign.ids.Industries;
import com.fs.starfarer.api.impl.campaign.ids.Items;
import com.fs.starfarer.api.impl.campaign.ids.Stats;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import com.fs.starfarer.api.util.Pair;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.List;

public class LtvHeavyIndustry extends LtvBaseIndustry {

	public static float ORBITAL_WORKS_QUALITY_BONUS = 0.2f;
	
	public static float DAYS_BEFORE_POLLUTION = 0f;
	public static float DAYS_BEFORE_POLLUTION_PERMANENT = 180f;

	public static int DAILY_BASE_PROD_HEAVY_MACHINERY = 13;// 150$
	public static int DAILY_BASE_PROD_SUPPLIES = 20; 		// 100$
	public static int DAILY_BASE_PROD_HAND_WEAPONS = 4;	// 500$
	public static int DAILY_BASE_PROD_SHIPS = 6;			// 300$

	public static float METALS_WEIGHT_FOR_HEAVY_MACHINERY = 0.8f;
	public static float RARE_METALS_WEIGHT_FOR_HEAVY_MACHINERY = 0.2f;
	public static float METALS_WEIGHT_FOR_SUPPLIES = 0.9f;
	public static float RARE_METALS_WEIGHT_FOR_SUPPLIES = 0.1f;
	public static float METALS_WEIGHT_FOR_HAND_WEAPONS = 0.4f;
	public static float RARE_METALS_WEIGHT_FOR_HAND_WEAPONS = 0.6f;
	public static float METALS_WEIGHT_FOR_SHIPS = 0.6f;
	public static float RARE_METALS_WEIGHT_FOR_SHIPS = 0.4f;

	Map<String, List<Pair<String, Float>>> CommodityList = new HashMap<>();

	public LtvHeavyIndustry() {
		List<Pair<String, Float>> _Commodity_Info = new ArrayList<Pair<String,Float>>();

		// Inserts relevant info into a list 
		_Commodity_Info.add(new Pair<String,Float>(Commodities.METALS, METALS_WEIGHT_FOR_HEAVY_MACHINERY));
		_Commodity_Info.add(new Pair<String,Float>(Commodities.RARE_METALS, RARE_METALS_WEIGHT_FOR_HEAVY_MACHINERY));
		CommodityList.put(Commodities.HEAVY_MACHINERY, new ArrayList<>(_Commodity_Info));
		_Commodity_Info.clear();

		_Commodity_Info.add(new Pair<String,Float>(Commodities.METALS, METALS_WEIGHT_FOR_SUPPLIES));
		_Commodity_Info.add(new Pair<String,Float>(Commodities.RARE_METALS, RARE_METALS_WEIGHT_FOR_SUPPLIES));
		CommodityList.put(Commodities.SUPPLIES, new ArrayList<>(_Commodity_Info));
		_Commodity_Info.clear();

		_Commodity_Info.add(new Pair<String,Float>(Commodities.METALS, METALS_WEIGHT_FOR_HAND_WEAPONS));
		_Commodity_Info.add(new Pair<String,Float>(Commodities.RARE_METALS, RARE_METALS_WEIGHT_FOR_HAND_WEAPONS));
		CommodityList.put(Commodities.HAND_WEAPONS, new ArrayList<>(_Commodity_Info));
		_Commodity_Info.clear();

		_Commodity_Info.add(new Pair<String,Float>(Commodities.METALS, METALS_WEIGHT_FOR_SHIPS));
		_Commodity_Info.add(new Pair<String,Float>(Commodities.RARE_METALS, RARE_METALS_WEIGHT_FOR_SHIPS));
		CommodityList.put(Commodities.SHIPS, new ArrayList<>(_Commodity_Info));
		_Commodity_Info.clear();
	}

	public void HeavyIndustryModifiers(){
		boolean OrbitalWorks = Industries.ORBITALWORKS.equals(getId());

		if (OrbitalWorks) {
			market.getStats().getDynamic().getMod(Stats.PRODUCTION_QUALITY_MOD).modifyFlat(getModId(1), ORBITAL_WORKS_QUALITY_BONUS, "Orbital works");
		}

		// Adjust qualityBonus dependent on Stability
		float stability = market.getPrevStability();
		if (stability < 5) {
			float stabilityMod = (stability - 5f) / 5f;
			stabilityMod *= 0.5f; // Stability penalty affect Fleet Quality half as much
			market.getStats().getDynamic().getMod(Stats.PRODUCTION_QUALITY_MOD).modifyFlat(getModId(0), stabilityMod, getNameForModifier() + " - low stability");
		}
	}

	
	@Override
	protected void addPostDemandSection(TooltipMakerAPI tooltip, boolean hasDemand, IndustryTooltipMode mode) {
	
		if (mode != IndustryTooltipMode.NORMAL || isFunctional()) {			
			if (Industries.ORBITALWORKS.equals(getId())) {
				String totalStr = "+" + (int)Math.round(ORBITAL_WORKS_QUALITY_BONUS * 100f) + "%";
				Color h = Misc.getHighlightColor();
				if (ORBITAL_WORKS_QUALITY_BONUS < 0) {
					h = Misc.getNegativeHighlightColor();
					totalStr = "" + (int)Math.round(ORBITAL_WORKS_QUALITY_BONUS * 100f) + "%";
				}
				float opad = 10f;
				if (ORBITAL_WORKS_QUALITY_BONUS >= 0) {
					tooltip.addPara("Ship quality: %s", opad, h, totalStr);
					tooltip.addPara("*Quality bonus only applies for the largest ship producer in the faction.", 
							Misc.getGrayColor(), opad);
				}
			}
		}
	}

	public boolean isDemandLegal(CommodityOnMarketAPI com) {
		return true;
	}

	public boolean isSupplyLegal(CommodityOnMarketAPI com) {
		return true;
	}

	@Override
	protected boolean canImproveToIncreaseProduction() {
		return true;
	}
	
	@Override
	public boolean wantsToUseSpecialItem(SpecialItemData data) {
		if (special != null && Items.CORRUPTED_NANOFORGE.equals(special.getId()) &&
				data != null && Items.PRISTINE_NANOFORGE.equals(data.getId())) {
			return true;
		}
		return super.wantsToUseSpecialItem(data);
	}

	protected boolean permaPollution = false;
	protected boolean addedPollution = false;
	protected float daysWithNanoforge = 0f;
	float TimeSinceLoop = 0f;
	
	public void apply() {
		super.apply();

		demand(Commodities.METALS, Math.round(ltv_precalculateconsumption(
			DAILY_BASE_PROD_HEAVY_MACHINERY*METALS_WEIGHT_FOR_HEAVY_MACHINERY,
			DAILY_BASE_PROD_SUPPLIES*METALS_WEIGHT_FOR_SUPPLIES,
			DAILY_BASE_PROD_HAND_WEAPONS*METALS_WEIGHT_FOR_HAND_WEAPONS,
			DAILY_BASE_PROD_SHIPS*METALS_WEIGHT_FOR_SHIPS
		)));

		demand(Commodities.RARE_METALS, Math.round(ltv_precalculateconsumption(
			DAILY_BASE_PROD_HEAVY_MACHINERY*RARE_METALS_WEIGHT_FOR_HEAVY_MACHINERY,
			DAILY_BASE_PROD_SUPPLIES*RARE_METALS_WEIGHT_FOR_SUPPLIES,
			DAILY_BASE_PROD_HAND_WEAPONS*RARE_METALS_WEIGHT_FOR_HAND_WEAPONS,
			DAILY_BASE_PROD_SHIPS*RARE_METALS_WEIGHT_FOR_SHIPS
		)));

		supply(Commodities.HEAVY_MACHINERY, DAILY_BASE_PROD_HEAVY_MACHINERY);
		supply(Commodities.SUPPLIES, DAILY_BASE_PROD_SUPPLIES);
		supply(Commodities.HAND_WEAPONS, DAILY_BASE_PROD_HAND_WEAPONS);
		supply(Commodities.SHIPS, DAILY_BASE_PROD_SHIPS);

		HeavyIndustryModifiers();
		
		if (!isFunctional()) {
			supply.clear();
			unapply();
		}
	}

	@Override
	public void unapply() {
		super.unapply();

		supply.clear();
		demand.clear();

		if (addedPollution && !permaPollution) {
			market.removeCondition(Conditions.POLLUTION);
			addedPollution = false;
		}

		daysWithNanoforge = 0f;
		TimeSinceLoop = 0f;
		
		market.getStats().getDynamic().getMod(Stats.PRODUCTION_QUALITY_MOD).unmodifyFlat(getModId(0));
		market.getStats().getDynamic().getMod(Stats.PRODUCTION_QUALITY_MOD).unmodifyFlat(getModId(1));
	}

	@Override
	public void advance(float amount) {
		super.advance(amount);

		float days = Global.getSector().getClock().convertToDays(amount);
		TimeSinceLoop += Global.getSector().getClock().convertToDays(amount);

		if (special != null && !isPermaPollution()) {
			daysWithNanoforge += days;
			updatePollutionStatus();
		}

		if (TimeSinceLoop >= 1) { // Consumption&Production

			ltv_WeightedDeficitModifiers(CommodityList);

			ltv_consume(Commodities.METALS);
			ltv_consume(Commodities.RARE_METALS);

			ltv_produce(CommodityList);
			
			TimeSinceLoop = 0f;
		}
	}
	
	protected void updatePollutionStatus() {
		if (!market.hasCondition(Conditions.HABITABLE)) return;
		
		if (special != null) {
			if (!addedPollution && daysWithNanoforge >= DAYS_BEFORE_POLLUTION) {
				if (market.hasCondition(Conditions.POLLUTION)) {
					permaPollution = true;
				} else {
					market.addCondition(Conditions.POLLUTION);
					addedPollution = true;
				}
			}
			if (addedPollution && !permaPollution) {
				if (daysWithNanoforge > DAYS_BEFORE_POLLUTION_PERMANENT) {
					permaPollution = true;
				}
			}
		} else if (addedPollution && !permaPollution) {
			market.removeCondition(Conditions.POLLUTION);
			addedPollution = false;
		}
	}

	public boolean isPermaPollution() {
		return permaPollution;
	}

	public void setPermaPollution(boolean permaPollution) {
		this.permaPollution = permaPollution;
	}

	public boolean isAddedPollution() {
		return addedPollution;
	}

	public void setAddedPollution(boolean addedPollution) {
		this.addedPollution = addedPollution;
	}

	public float getDaysWithNanoforge() {
		return daysWithNanoforge;
	}

	public void setDaysWithNanoforge(float daysWithNanoforge) {
		this.daysWithNanoforge = daysWithNanoforge;
	}

	@Override
	public void setSpecialItem(SpecialItemData special) {
		super.setSpecialItem(special);
		
		updatePollutionStatus();
	}
}
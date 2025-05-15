package wfg_ltv_econ.industry;

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

import java.util.Map;
import java.util.List;

public class LtvHeavyIndustry extends LtvBaseIndustry {

	public final static float ORBITAL_WORKS_QUALITY_BONUS = 0.2f;

	public final static float DAYS_BEFORE_POLLUTION = 0f;
	public final static float DAYS_BEFORE_POLLUTION_PERMANENT = 180f;

	public final static int DAILY_BASE_PROD_HEAVY_MACHINERY = 13; // 150$
	public final static int DAILY_BASE_PROD_SUPPLIES = 20; // 100$
	public final static int DAILY_BASE_PROD_HAND_WEAPONS = 4; // 500$
	public final static int DAILY_BASE_PROD_SHIPS = 6; // 300$

	public final static float METALS_WEIGHT_FOR_HEAVY_MACHINERY = 0.8f;
	public final static float RARE_METALS_WEIGHT_FOR_HEAVY_MACHINERY = 0.2f;
	public final static float METALS_WEIGHT_FOR_SUPPLIES = 0.9f;
	public final static float RARE_METALS_WEIGHT_FOR_SUPPLIES = 0.1f;
	public final static float METALS_WEIGHT_FOR_HAND_WEAPONS = 0.4f;
	public final static float RARE_METALS_WEIGHT_FOR_HAND_WEAPONS = 0.6f;
	public final static float METALS_WEIGHT_FOR_SHIPS = 0.6f;
	public final static float RARE_METALS_WEIGHT_FOR_SHIPS = 0.4f;

	protected static Map<String, List<Pair<String, Float>>> COMMODITY_LIST;

	static {
		COMMODITY_LIST = Map.of(
    		Commodities.HEAVY_MACHINERY, List.of(
    	    	new Pair<>(Commodities.METALS, METALS_WEIGHT_FOR_HEAVY_MACHINERY),
    	    	new Pair<>(Commodities.RARE_METALS, RARE_METALS_WEIGHT_FOR_HEAVY_MACHINERY)
    		),
			Commodities.SUPPLIES, List.of(
				new Pair<>(Commodities.METALS, METALS_WEIGHT_FOR_SUPPLIES),
				new Pair<>(Commodities.RARE_METALS, RARE_METALS_WEIGHT_FOR_SUPPLIES)
			),
			Commodities.HAND_WEAPONS, List.of(
				new Pair<>(Commodities.METALS, METALS_WEIGHT_FOR_HAND_WEAPONS),
				new Pair<>(Commodities.RARE_METALS, RARE_METALS_WEIGHT_FOR_HAND_WEAPONS)
			),
			Commodities.SHIPS, List.of(
				new Pair<>(Commodities.METALS, METALS_WEIGHT_FOR_SHIPS),
				new Pair<>(Commodities.RARE_METALS, RARE_METALS_WEIGHT_FOR_SHIPS)
			)
		);
	}

	public void HeavyIndustryModifiers() {
		if (Industries.ORBITALWORKS.equals(getId())) {
			market.getStats().getDynamic().getMod(Stats.PRODUCTION_QUALITY_MOD).modifyFlat(getModId(1), ORBITAL_WORKS_QUALITY_BONUS, "Orbital works");
		}

		// Adjust qualityBonus dependent on Stability
		float stability = market.getPrevStability();
		if (stability < 5) {
			float stabilityMod = (stability - 5f) / 5f;
			stabilityMod *= 0.5f; // Stability penalty affect Fleet Quality half as much
			market.getStats().getDynamic().getMod(Stats.PRODUCTION_QUALITY_MOD).modifyFlat(getModId(0), stabilityMod,
					getNameForModifier() + " - low stability");
		}
	}

	@Override
	protected void addPostDemandSection(TooltipMakerAPI tooltip, boolean hasDemand, IndustryTooltipMode mode) {

		if (mode != IndustryTooltipMode.NORMAL || isFunctional()) {
			if (Industries.ORBITALWORKS.equals(getId())) {
				String totalStr = "+" + (int) Math.round(ORBITAL_WORKS_QUALITY_BONUS * 100f) + "%";
				Color h = Misc.getHighlightColor();
				if (ORBITAL_WORKS_QUALITY_BONUS < 0) {
					h = Misc.getNegativeHighlightColor();
					totalStr = "" + (int) Math.round(ORBITAL_WORKS_QUALITY_BONUS * 100f) + "%";
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
		if (special != null && Items.CORRUPTED_NANOFORGE.equals(special.getId()) && data != null && Items.PRISTINE_NANOFORGE.equals(data.getId())) {
			return true;
		}
		return super.wantsToUseSpecialItem(data);
	}

	public void apply() {
		super.apply();

		demand(Commodities.METALS, Math.round(ltv_precalculateconsumption(
				DAILY_BASE_PROD_HEAVY_MACHINERY * METALS_WEIGHT_FOR_HEAVY_MACHINERY,
				DAILY_BASE_PROD_SUPPLIES * METALS_WEIGHT_FOR_SUPPLIES,
				DAILY_BASE_PROD_HAND_WEAPONS * METALS_WEIGHT_FOR_HAND_WEAPONS,
				DAILY_BASE_PROD_SHIPS * METALS_WEIGHT_FOR_SHIPS)));

		demand(Commodities.RARE_METALS, Math.round(ltv_precalculateconsumption(
				DAILY_BASE_PROD_HEAVY_MACHINERY * RARE_METALS_WEIGHT_FOR_HEAVY_MACHINERY,
				DAILY_BASE_PROD_SUPPLIES * RARE_METALS_WEIGHT_FOR_SUPPLIES,
				DAILY_BASE_PROD_HAND_WEAPONS * RARE_METALS_WEIGHT_FOR_HAND_WEAPONS,
				DAILY_BASE_PROD_SHIPS * RARE_METALS_WEIGHT_FOR_SHIPS)));

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
		dayTracker = -1;

		market.getStats().getDynamic().getMod(Stats.PRODUCTION_QUALITY_MOD).unmodifyFlat(getModId(0));
		market.getStats().getDynamic().getMod(Stats.PRODUCTION_QUALITY_MOD).unmodifyFlat(getModId(1));
	}

	protected boolean permaPollution = false;
	protected boolean addedPollution = false;
	protected float daysWithNanoforge = 0f;
	protected int dayTracker = -1;

	@Override
	public void advance(float amount) {
		super.advance(amount);

		int day = Global.getSector().getClock().getDay();

		if (dayTracker == -1) { // if not initialized
			dayTracker = day;
		}

		if (dayTracker != day) { //Production

			ltv_WeightedDeficitModifiers(COMMODITY_LIST);

			//All the consumption is done by Population and Infrastructure

			ltv_produce(COMMODITY_LIST);

			dayTracker = day;

			if (special != null && !isPermaPollution()) {
				daysWithNanoforge++;
				updatePollutionStatus();
			}
		}
	}

	protected void updatePollutionStatus() {
		if (!market.hasCondition(Conditions.HABITABLE))
			return;

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
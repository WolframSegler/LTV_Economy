package wfg_ltv_econ.industry;

import java.awt.Color;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.SpecialItemData;
import com.fs.starfarer.api.campaign.econ.CommodityOnMarketAPI;
import com.fs.starfarer.api.impl.campaign.ids.Conditions;
import com.fs.starfarer.api.impl.campaign.ids.Industries;
import com.fs.starfarer.api.impl.campaign.ids.Items;
import com.fs.starfarer.api.impl.campaign.ids.Stats;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;

public class LtvHeavyIndustry extends LtvBaseIndustry {

	public static float ORBITAL_WORKS_QUALITY_BONUS = 0.2f;

	public final static float DAYS_BEFORE_POLLUTION = 0f;
	public final static float DAYS_BEFORE_POLLUTION_PERMANENT = 180f;

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
		super.apply(true);

		HeavyIndustryModifiers();

		if (!isFunctional()) {
			supply.clear();
			unapply();
		}
	}

	@Override
	public void unapply() {
		super.unapply();

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
		int day = Global.getSector().getClock().getDay();
		super.advance(day);

		if (dayTracker != day) {

			if (special != null && !isPermaPollution()) {
				daysWithNanoforge++;
				updatePollutionStatus();
			}

			dayTracker = day; // Do this at the end of the advance() method
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
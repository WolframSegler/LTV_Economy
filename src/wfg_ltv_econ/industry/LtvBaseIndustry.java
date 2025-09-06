package wfg_ltv_econ.industry;

import java.util.List;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.econ.MarketImmigrationModifier;
import com.fs.starfarer.api.combat.MutableStat;
import com.fs.starfarer.api.impl.campaign.DebugFlags;
import com.fs.starfarer.api.impl.campaign.econ.impl.BaseIndustry;
import com.fs.starfarer.api.impl.campaign.econ.impl.InstallableItemEffect;
import com.fs.starfarer.api.util.Misc;
import com.fs.starfarer.api.util.Pair;

import wfg_ltv_econ.economy.CommodityStats;
import wfg_ltv_econ.economy.EconomyEngine;

public abstract class LtvBaseIndustry extends BaseIndustry {
	@Override
	protected LtvBaseIndustry clone() {
		LtvBaseIndustry copy = null;

		copy = (LtvBaseIndustry) super.clone();

		return copy;
	}

	public LtvBaseIndustry() {}

	@Override
	public void apply(boolean updateIncomeAndUpkeep) {
		updateSupplyAndDemandModifiers();

		if (updateIncomeAndUpkeep) {
			updateIncomeAndUpkeep();
		}
		
		applyAICoreModifiers();
		applyImproveModifiers();

		if (this instanceof MarketImmigrationModifier) {
			market.addTransientImmigrationModifier((MarketImmigrationModifier) this);
		}

		if (special != null) {
			InstallableItemEffect effect = LtvItemEffectsRepo.ITEM_EFFECTS.get(special.getId());
			if (effect != null) {
				List<String> unmet = effect.getUnmetRequirements(this);
				if (unmet == null || unmet.isEmpty()) {
					effect.apply(this);
				} else {
					effect.unapply(this);
				}
			}
		}
	}

	@Override
	public void updateIncomeAndUpkeep() {
		int sizeMult = market.getSize() - 2;
		float stabilityMult = market.getIncomeMult().getModifiedValue();
		float upkeepMult = market.getUpkeepMult().getModifiedValue();

		int income = (int) (getSpec().getIncome() * sizeMult);
		if (income != 0) {
			getIncome().modifyFlatAlways("ind_base", income, "Base value");
			getIncome().modifyMultAlways("ind_stability", stabilityMult, "Market income multiplier");
		} else {
			getIncome().unmodifyFlat("ind_base");
			getIncome().unmodifyMult("ind_stability");
		}

		int upkeep = (int) (getSpec().getUpkeep() * sizeMult);
		if (upkeep != 0) {
			getUpkeep().modifyFlatAlways("ind_base", upkeep, "Base value");
			getUpkeep().modifyMultAlways("ind_hazard", upkeepMult, "Market upkeep multiplier");
		} else {
			getUpkeep().unmodifyFlat("ind_base");
			getUpkeep().unmodifyMult("ind_hazard");
		}

		applyAICoreToIncomeAndUpkeep();

		if (!isFunctional()) {
			getIncome().unmodifyFlat("ind_base");
			getIncome().unmodifyMult("ind_stability");
		}
	}

	@Override
	public float getBaseUpkeep() {
		float sizeMult = market.getSize();
		sizeMult = Math.max(1, sizeMult - 2);
		return getSpec().getUpkeep() * sizeMult;
	}

	protected float dayTracker = -1;

	@Override
	public void advance(float day) {
		if (dayTracker == -1) {
			dayTracker = day;
		}
		if (dayTracker != day) {
			boolean disrupted = isDisrupted();
			if (!disrupted && wasDisrupted) {
				disruptionFinished();
			}
			wasDisrupted = disrupted;

			if (building && !disrupted) {
				buildProgress++;

				if ((buildProgress >= buildTime) || DebugFlags.COLONY_DEBUG) {
					finishBuildingOrUpgrading();
				}
			}
		}
	}

	@Override
	public MutableStat getIncome() {
		return income;
	}

	@Override
	public MutableStat getUpkeep() {
		return upkeep;
	}

	public Pair<String, Float> ltv_getMaxDeficit(String... commodityIds) {
		// 1 is no deficit and 0 is 100% deficit
		Pair<String, Float> result = new Pair<String, Float>();
		result.two = 1f;
		if (Global.CODEX_TOOLTIP_MODE || !EconomyEngine.isInitialized()) return result;

		for (String id : commodityIds) {
			final CommodityStats stats = EconomyEngine.getInstance().getComStats(id, market.getId());
			if (stats == null) {
				return result;
			}

			float available = stats.getStoredCoverageRatio();

			if (available < result.two) {
				result.one = id;
				result.two = available;
			}
		}
		return result;
	}

	@Override
	@Deprecated
	protected int getStabilityPenalty() {
		return super.getStabilityPenalty();
	}

	@Override
	public int getImproveStoryPoints() {
		int base = Global.getSettings().getInt("industryImproveBase");
		return base * (int) Math.round(Math.pow(2, Misc.getNumImprovedIndustries(market)));
	}
}
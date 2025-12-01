package wfg.ltv_econ.industry;

import org.json.JSONArray;
import org.json.JSONException;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.FactionDoctrineAPI;
import com.fs.starfarer.api.campaign.econ.Industry;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.impl.campaign.econ.CommRelayCondition;
import com.fs.starfarer.api.impl.campaign.econ.impl.PopulationAndInfrastructure;
import com.fs.starfarer.api.impl.campaign.econ.impl.ConstructionQueue.ConstructionQueueItem;
import com.fs.starfarer.api.impl.campaign.fleets.FleetFactoryV3;
import com.fs.starfarer.api.impl.campaign.ids.Commodities;
import com.fs.starfarer.api.impl.campaign.ids.Conditions;
import com.fs.starfarer.api.impl.campaign.ids.Industries;
import com.fs.starfarer.api.impl.campaign.ids.Stats;
import com.fs.starfarer.api.loading.IndustrySpecAPI;
import com.fs.starfarer.api.util.Misc;
import com.fs.starfarer.api.util.Pair;

import wfg.ltv_econ.economy.EconomyEngine;

public class LtvPopulationAndInfrastructure extends PopulationAndInfrastructure {

	public final static float BASE_STABILITY = Global.getSettings().getFloat("stabilityBaseValue");
	public final static float IN_FACTION_IMPORT_BONUS = Global.getSettings().getFloat(
		"upkeepReductionFromInFactionImports"
	);

	public void apply() {
		super.apply(true);
		modifyStability(this, market, getModId(3));

		market.getStats().getDynamic().getMod(Stats.MAX_INDUSTRIES).modifyFlat(getModId(), getMaxIndustries(), null);
		market.addTransientImmigrationModifier(this);

		final int size = market.getSize();
		final int luxuryThreshold = 3;

		AccessModifierSpaceport(size);

		int stability = (int) market.getPrevStability();

		StabilityModifierDemand(luxuryThreshold);
		StabilityModifierFleetAndDefenses(stability);
		StabilityModifierIndustryCap(this, market, getModId(3));

		FleetSizeMultipliers(stability);

		SpawnAdminsAndOfficers();
	}

	public void StabilityModifierDemand(int luxuryThreshold) {
		Pair<String, Float> availableRatio = EconomyEngine.getMaxDeficit(market, Commodities.DOMESTIC_GOODS);
		if (availableRatio.two > 0.9) { // If 90% or more is satisfied
			market.getStability().modifyFlat(getModId(0), 1, "Domestic goods demand met");
		} else {
			market.getStability().unmodifyFlat(getModId(0));
		}

		availableRatio = EconomyEngine.getMaxDeficit(market, Commodities.LUXURY_GOODS);
		if (availableRatio.two > 0.9 && market.getSize() > luxuryThreshold) { // If 90% or more is satisfied
			market.getStability().modifyFlat(getModId(1), 1, "Luxury goods demand met");
		} else {
			market.getStability().unmodifyFlat(getModId(1));
		}

		availableRatio = EconomyEngine.getMaxDeficit(market, Commodities.FOOD);
		if (!market.hasCondition(Conditions.HABITABLE)) {
			availableRatio = EconomyEngine.getMaxDeficit(market, Commodities.FOOD, Commodities.ORGANICS);
		}
		if (availableRatio.two < 0.9) { // If less than 90% is satisfied
			int stabilityPenalty = 
				availableRatio.two < 0.1 ? -3 :
                availableRatio.two < 0.4 ? -2 :
                availableRatio.two < 0.7 ? -1 : 0;
			market.getStability().modifyFlat(getModId(2), stabilityPenalty, getDeficitText(availableRatio.one));
		} else {
			market.getStability().unmodifyFlat(getModId(2));
		}
	}

	public void AccessModifierSpaceport(int size) {
		boolean spaceportFirstInQueue = false;
		for (ConstructionQueueItem item : market.getConstructionQueue().getItems()) {
			IndustrySpecAPI spec = Global.getSettings().getIndustrySpec(item.id);
			if (spec.hasTag(Industries.TAG_SPACEPORT)) {
				spaceportFirstInQueue = true;
			}
			break;
		}
		if (spaceportFirstInQueue && Misc.getCurrentlyBeingConstructed(market) != null) {
			spaceportFirstInQueue = false;
		}
		if (!market.hasSpaceport() && !spaceportFirstInQueue) {
			float accessibilityNoSpaceport = Global.getSettings().getFloat("accessibilityNoSpaceport");
			market.getAccessibilityMod().modifyFlat(getModId(0), accessibilityNoSpaceport, "No spaceport");
		}

		if (getAccessibilityBonus(size) > 0) {
			market.getAccessibilityMod().modifyFlat(getModId(1), getAccessibilityBonus(size), "Colony size");
		}
	}

	public void StabilityModifierFleetAndDefenses(int stability) {
		float stabilityQualityMod = FleetFactoryV3.getShipQualityModForStability(stability);
		float doctrineQualityMod = market.getFaction().getDoctrine().getShipQualityContribution();

		market.getStats().getDynamic().getMod(Stats.FLEET_QUALITY_MOD).modifyFlatAlways(getModId(0),
				stabilityQualityMod, "Stability");

		market.getStats().getDynamic().getMod(Stats.FLEET_QUALITY_MOD).modifyFlatAlways(getModId(1), doctrineQualityMod,
				Misc.ucFirst(market.getFaction().getEntityNamePrefix()) + " fleet doctrine");

		float stabilityDefenseMult = 0.25f + stability / 10f * 0.75f;
		market.getStats().getDynamic().getMod(Stats.GROUND_DEFENSES_MOD).modifyMultAlways(getModId(),
				stabilityDefenseMult, "Stability");

		float baseDef = getBaseGroundDefenses(market.getSize());
		market.getStats().getDynamic().getMod(Stats.GROUND_DEFENSES_MOD).modifyFlatAlways(getModId(), baseDef,
				"Base value for a size " + market.getSize() + " colony");

		if (HAZARD_INCREASES_DEFENSE) {
			market.getStats().getDynamic().getMod(Stats.GROUND_DEFENSES_MOD).modifyMultAlways(getModId(1),
					Math.max(market.getHazardValue(), 1f), "Colony hazard rating");
		}
	}

	public void FleetSizeMultipliers(int stability) {
		FactionDoctrineAPI doctrine = market.getFaction().getDoctrine();
		float doctrineShipsMult = FleetFactoryV3.getDoctrineNumShipsMult(doctrine.getNumShips());
		float marketSizeShipsMult = FleetFactoryV3.getNumShipsMultForMarketSize(market.getSize());
		float deficitShipsMult = FleetFactoryV3.getShipDeficitFleetSizeMult(market);
		float stabilityShipsMult = FleetFactoryV3.getNumShipsMultForStability(stability);

		market.getStats().getDynamic().getMod(Stats.COMBAT_FLEET_SIZE_MULT).modifyFlatAlways(getModId(0),
				marketSizeShipsMult, "Colony size");

		market.getStats().getDynamic().getMod(Stats.COMBAT_FLEET_SIZE_MULT).modifyMultAlways(getModId(1),
				doctrineShipsMult, Misc.ucFirst(market.getFaction().getEntityNamePrefix()) + " fleet doctrine");

		if (deficitShipsMult != 1f) {
			market.getStats().getDynamic().getMod(Stats.COMBAT_FLEET_SIZE_MULT).modifyMult(getModId(2),
					deficitShipsMult, getDeficitText(Commodities.SHIPS));
		} else {
			market.getStats().getDynamic().getMod(Stats.COMBAT_FLEET_SIZE_MULT).modifyMultAlways(getModId(2),
					deficitShipsMult, getDeficitText(Commodities.SHIPS).replaceAll("shortage", "demand met"));
		}

		market.getStats().getDynamic().getMod(Stats.COMBAT_FLEET_SIZE_MULT).modifyMultAlways(getModId(3),
				stabilityShipsMult, "Stability");
	}

	public void SpawnAdminsAndOfficers() {
		// chance of spawning officers and admins; some industries further modify this
		market.getStats().getDynamic().getMod(Stats.OFFICER_PROB_MOD).modifyFlat(getModId(0), OFFICER_BASE_PROB);
		market.getStats().getDynamic().getMod(Stats.OFFICER_PROB_MOD).modifyFlat(getModId(1),
				OFFICER_PROB_PER_SIZE * Math.max(0, market.getSize() - 3));

		market.getStats().getDynamic().getMod(Stats.OFFICER_ADDITIONAL_PROB_MULT_MOD).modifyFlat(getModId(0),
				OFFICER_ADDITIONAL_BASE_PROB);
		market.getStats().getDynamic().getMod(Stats.OFFICER_IS_MERC_PROB_MOD).modifyFlat(getModId(0),
				OFFICER_BASE_MERC_PROB);

		market.getStats().getDynamic().getMod(Stats.ADMIN_PROB_MOD).modifyFlat(getModId(0), ADMIN_BASE_PROB);
		market.getStats().getDynamic().getMod(Stats.ADMIN_PROB_MOD).modifyFlat(getModId(1),
				ADMIN_PROB_PER_SIZE * Math.max(0, market.getSize() - 3));
	}

	public static float getAccessibilityBonus(int marketSize) {
		if (marketSize <= 4)
			return 0f;
		if (marketSize == 5)
			return 0.1f;
		if (marketSize == 6)
			return 0.15f;
		if (marketSize == 7)
			return 0.2f;
		if (marketSize == 8)
			return 0.25f;
		return 0.3f;
	}

	public static float getBaseGroundDefenses(int marketSize) {
		if (marketSize <= 1)
			return 10;
		if (marketSize <= 2)
			return 20;
		if (marketSize <= 3)
			return 50;

		return (marketSize - 3) * 100;
	}

	public static float getIncomeStabilityMult(float stability) {
		if (stability <= 5) return Math.max(0, stability / 5f);
		return 1f;
	}

	public static float getUpkeepHazardMult(float hazard) {
		float hazardMult = hazard;
		float min = Global.getSettings().getFloat("minUpkeepMult");
		if (hazardMult < min)
			hazardMult = min;
		return hazardMult;
	}

	public static void StabilityModifierIndustryCap(Industry industry, MarketAPI market, String modId) {

		if (Misc.getNumIndustries(market) > Misc.getMaxIndustries(market)) {
			market.getStability().modifyFlat("_" + modId + "_overmax", -Misc.OVER_MAX_INDUSTRIES_PENALTY, "Maximum number of industries exceeded");
		} else {
			market.getStability().unmodifyFlat("_" + modId + "_overmax");
		}
	}

	public static void modifyStability(Industry industry, MarketAPI market, String modId) {
		market.getIncomeMult().modifyMultAlways(modId, getIncomeStabilityMult(
			market.getPrevStability()), "Stability"
		);

		market.getStability().modifyFlat("_" + modId + "_ms", BASE_STABILITY, "Base value");

		if (market.isPlayerOwned() && market.getAdmin().isPlayer()) {
			int penalty = getMismanagementPenalty();
			if (penalty > 0) {
				market.getStability().modifyFlat("_" + modId + "_mm", -penalty, "Mismanagement penalty");
			} else if (penalty < 0) {
				market.getStability().modifyFlat("_" + modId + "_mm", -penalty, "Management bonus");
			} else {
				market.getStability().unmodifyFlat("_" + modId + "_mm");
			}
		} else {
			market.getStability().unmodifyFlat(modId + "_mm");
		}

		if (!market.hasCondition(Conditions.COMM_RELAY)) {
			market.getStability().modifyFlat(CommRelayCondition.COMM_RELAY_MOD_ID, CommRelayCondition.NO_RELAY_PENALTY,
					"No active comm relay in-system");
		}
	}

	public static int getMaxIndustries(int size) {
		if (MAX_IND == null) {
			try {
				MAX_IND = new int[10];
				JSONArray a = Global.getSettings().getJSONArray("maxIndustries");
				for (int i = 0; i < MAX_IND.length; i++) {
					MAX_IND[i] = a.getInt(i);
				}
			} catch (JSONException e) {
				throw new RuntimeException(e);
			}
		}
		size--;
		if (size < 0)
			size = 0;
		if (size > 9)
			size = 9;
		return MAX_IND[size];
	}
}
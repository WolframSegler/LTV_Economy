package wfg_ltv_econ.industry;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.awt.Color;

import org.json.JSONArray;
import org.json.JSONException;
import org.lwjgl.util.vector.Vector2f;

import com.fs.starfarer.api.EveryFrameScript;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.FactionDoctrineAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.SpecialItemData;
import com.fs.starfarer.api.campaign.comm.IntelInfoPlugin;
import com.fs.starfarer.api.campaign.econ.CommodityMarketDataAPI;
import com.fs.starfarer.api.campaign.econ.CommodityOnMarketAPI;
import com.fs.starfarer.api.campaign.econ.Industry;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.campaign.econ.MarketImmigrationModifier;
import com.fs.starfarer.api.campaign.listeners.ColonyOtherFactorsListener;
import com.fs.starfarer.api.characters.MarketConditionSpecAPI;
import com.fs.starfarer.api.combat.MutableStat;
import com.fs.starfarer.api.combat.MutableStat.StatMod;
import com.fs.starfarer.api.impl.campaign.econ.CommRelayCondition;
import com.fs.starfarer.api.impl.campaign.econ.impl.ConstructionQueue.ConstructionQueueItem;
import com.fs.starfarer.api.impl.campaign.fleets.FleetFactoryV3;
import com.fs.starfarer.api.impl.campaign.ids.Commodities;
import com.fs.starfarer.api.impl.campaign.ids.Conditions;
import com.fs.starfarer.api.impl.campaign.ids.Entities;
import com.fs.starfarer.api.impl.campaign.ids.Factions;
import com.fs.starfarer.api.impl.campaign.ids.Industries;
import com.fs.starfarer.api.impl.campaign.ids.Items;
import com.fs.starfarer.api.impl.campaign.ids.Stats;
import com.fs.starfarer.api.impl.campaign.intel.misc.HypershuntIntel;
import com.fs.starfarer.api.impl.campaign.population.PopulationComposition;
import com.fs.starfarer.api.impl.campaign.procgen.themes.BaseThemeGenerator;
import com.fs.starfarer.api.impl.campaign.procgen.themes.BaseThemeGenerator.AddedEntity;
import com.fs.starfarer.api.impl.campaign.procgen.themes.BaseThemeGenerator.EntityLocation;
import com.fs.starfarer.api.loading.IndustrySpecAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI.StatModValueGetter;
import com.fs.starfarer.api.util.Misc;
import com.fs.starfarer.api.util.Pair;

public class LtvPopulationAndInfrastructure extends LtvBaseIndustry implements MarketImmigrationModifier {

	public final static float OFFICER_BASE_PROB = Global.getSettings().getFloat("officerBaseProb");
	public final static float OFFICER_PROB_PER_SIZE = Global.getSettings().getFloat("officerProbPerColonySize");
	public final static float OFFICER_ADDITIONAL_BASE_PROB = Global.getSettings().getFloat("officerAdditionalBaseProb");
	public final static float OFFICER_BASE_MERC_PROB = Global.getSettings().getFloat("officerBaseMercProb");
	public final static float ADMIN_BASE_PROB = Global.getSettings().getFloat("adminBaseProb");
	public final static float ADMIN_PROB_PER_SIZE = Global.getSettings().getFloat("adminProbPerColonySize");
	public final static float BASE_STABILITY = Global.getSettings().getFloat("stabilityBaseValue");
	public final static float IN_FACTION_IMPORT_BONUS = Global.getSettings().getFloat("upkeepReductionFromInFactionImports");

	public final static int IMPROVE_STABILITY_BONUS = 1;

	public final static boolean HAZARD_INCREASES_DEFENSE = false;

	public final static float DAILY_BASE_PROD_CREW = 3000f/(25f*365f*20f*10f);

	public final static float FOOD_WEIGHT_FOR_CREW = 0.4f;
	public final static float DOMESTIC_GOODS_WEIGHT_FOR_CREW = 0.2f;
	public final static float LUXURY_GOODS_WEIGHT_FOR_CREW = 0.1f;
	public final static float SUPPLIES_WEIGHT_FOR_CREW = 0.1f;
	public final static float DOMESTIC_GOODS_WEIGHT_FOR_DRUGS = 0.5f;
	public final static float FOOD_WEIGHT_FOR_ORGANS = 0.7f;

	protected static Map<String, List<Pair<String, Float>>> COMMODITY_LIST;

	static {
		COMMODITY_LIST = Map.of(
			Commodities.CREW, List.of(
					new Pair<>(Commodities.FOOD, FOOD_WEIGHT_FOR_CREW),
					new Pair<>(Commodities.DOMESTIC_GOODS, DOMESTIC_GOODS_WEIGHT_FOR_CREW),
					new Pair<>(Commodities.LUXURY_GOODS, LUXURY_GOODS_WEIGHT_FOR_CREW),
					new Pair<>(Commodities.SUPPLIES, SUPPLIES_WEIGHT_FOR_CREW)
					),
			Commodities.DRUGS, List.of(
					new Pair<>(Commodities.DOMESTIC_GOODS, DOMESTIC_GOODS_WEIGHT_FOR_DRUGS)
					),
			Commodities.ORGANS, List.of(
					new Pair<>(Commodities.FOOD, FOOD_WEIGHT_FOR_ORGANS)
					)
				);
	}

	// The apply() methods are called according to their order inside the market. This means the apply() of PopulationAndInfrastructure gets called first
	public void apply() {
		super.apply(true);
		modifyStability(this, market, getModId(3));

		market.getStats().getDynamic().getMod(Stats.MAX_INDUSTRIES).modifyFlat(getModId(), getMaxIndustries(), null);
		market.addTransientImmigrationModifier(this);

		int size = market.getSize();
		int luxuryThreshold = 3;

		demand(Commodities.FOOD, (int) ((5.0/3.0)*Math.pow(10, size - 3)));
		demand(Commodities.DOMESTIC_GOODS, (int) ((1.0/3.0)*Math.pow(10, size - 3)));
		demand(Commodities.LUXURY_GOODS, (int) ((1.0/30.0)*Math.pow(10, size - luxuryThreshold)));
		demand(Commodities.SUPPLIES, (int) ((1.0/15.0)*Math.pow(10, size - 3)));
		demand(Commodities.DRUGS, (int) ((0.025)*Math.pow(10, size - 3)));
		demand(Commodities.ORGANS, (int) ((3.0/233.0)*Math.pow(10, size - 5)));
		if (!market.hasCondition(Conditions.HABITABLE)) {
			demand(Commodities.ORGANICS, (int) ((1.0/6.0)*Math.pow(10, size - 3)));
		}

		supply(Commodities.CREW, (int) (DAILY_BASE_PROD_CREW*Math.pow(10, size - 3)));
		supply(Commodities.DRUGS, (int) ((0.025)*Math.pow(10, size - 5)));
		supply(Commodities.ORGANS, (int) ((3.0/1168.0)*Math.pow(10, size - 5)));

		AccessModifierSpaceport(size);

		int stability = (int) market.getPrevStability();

		StabilityModifierDemand(luxuryThreshold);
		StabilityModifierFleetAndDefenses(stability);
		StabilityModifierIndustryCap(this, market, getModId(3));

		FleetSizeMultipliers(stability);

		SpawnAdminsAndOfficers();
	}

	public void StabilityModifierDemand(int luxuryThreshold) {
		Pair<String, Float> deficit = ltv_getMaxDeficit(Commodities.DOMESTIC_GOODS);
		if (deficit.two <= 0.1) { // If 90% or more is satisfied
			market.getStability().modifyFlat(getModId(0), 1, "Domestic goods demand met");
		} else {
			market.getStability().unmodifyFlat(getModId(0));
		}

		deficit = ltv_getMaxDeficit(Commodities.LUXURY_GOODS);
		if (deficit.two <= 0.1 && market.getSize() > luxuryThreshold) { // If 90% or more is satisfied
			market.getStability().modifyFlat(getModId(1), 1, "Luxury goods demand met");
		} else {
			market.getStability().unmodifyFlat(getModId(1));
		}

		deficit = ltv_getMaxDeficit(Commodities.FOOD);
		if (!market.hasCondition(Conditions.HABITABLE)) {
			deficit = ltv_getMaxDeficit(Commodities.FOOD, Commodities.ORGANICS);
		}
		if (deficit.two > 0.1) { // If less than 90% is satisfied
			int stabilityPenalty = deficit.two > 0.7f ? -3 : (deficit.two > 0.4f ? -2 : (deficit.two > 0.1f ? -1 : 0));
			market.getStability().modifyFlat(getModId(2), stabilityPenalty, getDeficitText(deficit.one));
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

	@Override
	public void unapply() {
		super.unapply();

		market.getStability().unmodify(getModId(0));
		market.getStability().unmodify(getModId(1));
		market.getStability().unmodify(getModId(2));

		market.getAccessibilityMod().unmodifyFlat(getModId(0));
		market.getAccessibilityMod().unmodifyFlat(getModId(1));

		market.getStats().getDynamic().getMod(Stats.FLEET_QUALITY_MOD).unmodifyFlat(getModId(0));
		market.getStats().getDynamic().getMod(Stats.FLEET_QUALITY_MOD).unmodifyFlat(getModId(1));

		market.getStats().getDynamic().getMod(Stats.GROUND_DEFENSES_MOD).unmodifyFlat(getModId());
		market.getStats().getDynamic().getMod(Stats.GROUND_DEFENSES_MOD).unmodifyMult(getModId());
		if (HAZARD_INCREASES_DEFENSE) {
			market.getStats().getDynamic().getMod(Stats.GROUND_DEFENSES_MOD).unmodifyMult(getModId(1)); // hazard value modifier
		}

		market.getStats().getDynamic().getMod(Stats.MAX_INDUSTRIES).unmodifyFlat(getModId());

		market.getStats().getDynamic().getMod(Stats.COMBAT_FLEET_SIZE_MULT).unmodifyFlat(getModId(0));
		market.getStats().getDynamic().getMod(Stats.COMBAT_FLEET_SIZE_MULT).unmodifyMult(getModId(1));
		market.getStats().getDynamic().getMod(Stats.COMBAT_FLEET_SIZE_MULT).unmodifyMult(getModId(2));
		market.getStats().getDynamic().getMod(Stats.COMBAT_FLEET_SIZE_MULT).unmodifyMult(getModId(3));

		market.getStats().getDynamic().getMod(Stats.OFFICER_PROB_MOD).unmodifyFlat(getModId(0));
		market.getStats().getDynamic().getMod(Stats.OFFICER_PROB_MOD).unmodifyFlat(getModId(1));
		market.getStats().getDynamic().getMod(Stats.OFFICER_ADDITIONAL_PROB_MULT_MOD).unmodifyFlat(getModId(0));
		market.getStats().getDynamic().getMod(Stats.OFFICER_IS_MERC_PROB_MOD).unmodifyFlat(getModId(0));
		market.getStats().getDynamic().getMod(Stats.ADMIN_PROB_MOD).unmodifyFlat(getModId(0));
		market.getStats().getDynamic().getMod(Stats.ADMIN_PROB_MOD).unmodifyFlat(getModId(1));

		unmodifyStability(market, getModId(3));

		market.removeTransientImmigrationModifier(this);
	}

	protected int dayTracker = -1;

	@Override
	public void advance(float amount) {
		int day = Global.getSector().getClock().getDay();
		super.advance(day);

		if (dayTracker != day) { // Consumption&Production
			// All the industries set their own demand.
			// But only Population and Infrastructure consume the demanded items.
			// This way the industries don't have to keep track of their demands.
			// This works, because WieghtedDeficitModifiers only looks at the total demand
			// and not the individual demands of industries.

			ltv_WeightedDeficitModifiers(COMMODITY_LIST);

			// Consume all commodities with demand
			for (CommodityOnMarketAPI com : market.getAllCommodities()) {
				int demand = com.getMaxDemand();
				if (demand > 0) {
					ltv_consume(com.getId());
				}
			}

			ltv_produce(COMMODITY_LIST);

			dayTracker = day; // Do this at the end of the advance() method
		}
	}

	protected boolean hasPostDemandSection(boolean hasDemand, IndustryTooltipMode mode) {
		return true;
	}

	@Override
	protected void addPostDemandSection(TooltipMakerAPI tooltip, boolean hasDemand, IndustryTooltipMode mode) {
		if (mode != IndustryTooltipMode.NORMAL || isFunctional()) {

			MutableStat stabilityMods = new MutableStat(0);

			float total = 0;
			for (StatMod mod : market.getStability().getFlatMods().values()) {
				if (mod.source.startsWith(getModId())) {
					stabilityMods.modifyFlat(mod.source, mod.value, mod.desc);
					total += mod.value;
				}
			}

			String totalStr = "+" + (int) Math.round(total);
			Color h = Misc.getHighlightColor();
			if (total < 0) {
				totalStr = "" + (int) Math.round(total);
				h = Misc.getNegativeHighlightColor();
			}
			float opad = 10f;
			float pad = 3f;
			if (total >= 0) {
				tooltip.addPara("Stability bonus: %s", opad, h, totalStr);
			} else {
				tooltip.addPara("Stability penalty: %s", opad, h, totalStr);
			}
			tooltip.addStatModGrid(400, 30, opad, pad, stabilityMods, new StatModValueGetter() {
				public String getPercentValue(StatMod mod) {
					return null;
				}

				public String getMultValue(StatMod mod) {
					return null;
				}

				public Color getModColor(StatMod mod) {
					if (mod.value < 0)
						return Misc.getNegativeHighlightColor();
					return null;
				}

				public String getFlatValue(StatMod mod) {
					return null;
				}
			});
		}
	}

	@Override
	public String getCurrentImage() {
		if (market.getSize() <= SIZE_FOR_SMALL_IMAGE) {
			return Global.getSettings().getSpriteName("industry", "pop_low");
		}
		if (market.getSize() >= SIZE_FOR_LARGE_IMAGE) {
			return Global.getSettings().getSpriteName("industry", "pop_high");
		}

		return super.getCurrentImage();
	}

	public static float getIncomeStabilityMult(float stability) {
		if (stability <= 5) {
			return Math.max(0, stability / 5f);
		}
		return 1f;
	}

	public static float getUpkeepHazardMult(float hazard) {
		float hazardMult = hazard;
		float min = Global.getSettings().getFloat("minUpkeepMult");
		if (hazardMult < min)
			hazardMult = min;
		return hazardMult;
	}

	public static int getMismanagementPenalty() {
		int outposts = 0;
		for (MarketAPI curr : Global.getSector().getEconomy().getMarketsCopy()) {
			if (!curr.isPlayerOwned())
				continue;

			if (curr.getAdmin().isPlayer()) {
				outposts++;
			}
		}

		int overOutposts = outposts - Global.getSector().getCharacterData().getPerson().getStats().getOutpostNumber().getModifiedInt();

		int penaltyOrBonus = (int) (overOutposts * Misc.getOutpostPenalty());

		return penaltyOrBonus;
	}

	public static void StabilityModifierIndustryCap(Industry industry, MarketAPI market, String modId) {

		if (Misc.getNumIndustries(market) > Misc.getMaxIndustries(market)) {
			market.getStability().modifyFlat("_" + modId + "_overmax", -Misc.OVER_MAX_INDUSTRIES_PENALTY, "Maximum number of industries exceeded");
		} else {
			market.getStability().unmodifyFlat("_" + modId + "_overmax");
		}
	}

	/**
	 * Called from core code after all industry effects are applied.
	 * 
	 * @param industry
	 * @param market
	 * @param modId
	 */
	public static void modifyUpkeepByHazardRating(MarketAPI market, String modId) {
		market.getUpkeepMult().modifyMultAlways(modId, getUpkeepHazardMult(market.getHazardValue()), "Hazard rating");
	}

	public static void modifyStability(Industry industry, MarketAPI market, String modId) {
		market.getIncomeMult().modifyMultAlways(modId, getIncomeStabilityMult(market.getPrevStability()), "Stability");

		market.getStability().modifyFlat("_" + modId + "_ms", BASE_STABILITY, "Base value");

		float inFactionSupply = 0f;
		float totalDemand = 0f;
		for (CommodityOnMarketAPI com : market.getCommoditiesCopy()) {
			if (com.isNonEcon())
				continue;

			int d = com.getMaxDemand();
			if (d <= 0)
				continue;

			totalDemand += d;
			CommodityMarketDataAPI cmd = com.getCommodityMarketData();
			int inFaction = Math.max(Math.min(com.getMaxSupply(), com.getAvailable()),
					Math.min(cmd.getMaxShipping(market, true), cmd.getMaxExport(market.getFactionId())));
			if (inFaction > d)
				inFaction = d;
			if (inFaction < d)
				inFaction = Math.max(Math.min(com.getMaxSupply(), com.getAvailable()), 0);

			inFactionSupply += Math.max(0, Math.min(inFaction, com.getAvailable()));
		}

		if (totalDemand > 0) {
			float f = inFactionSupply / totalDemand;
			if (f < 0)
				f = 0;
			if (f > 1)
				f = 1;
			if (f > 0) {
				float mult = Math.round(100f - (f * IN_FACTION_IMPORT_BONUS * 100f)) / 100f;
				String desc = "Demand supplied in-faction (" + Math.round(f * 100f) + "%)";
				if (f == 1f)
					desc = "All demand supplied in-faction";
				market.getUpkeepMult().modifyMultAlways(modId + "ifi", mult, desc);
			} else {
				market.getUpkeepMult().modifyMultAlways(modId + "ifi", 1f,
						"All demand supplied out-of-faction; no upkeep reduction");
			}
		}

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

	public static void unmodifyStability(MarketAPI market, String modId) {
		market.getIncomeMult().unmodifyMult(modId);
		market.getUpkeepMult().unmodifyMult(modId);
		market.getUpkeepMult().unmodifyMult(modId + "ifi");

		market.getStability().unmodifyFlat(modId);

		market.getStability().unmodifyFlat("_" + modId + "_mm");
		market.getStability().unmodifyFlat("_" + modId + "_ms");
		market.getStability().unmodifyFlat("_" + modId + "_overmax");

		if (!market.hasCondition(Conditions.COMM_RELAY)) {
			market.getStability().unmodifyFlat(CommRelayCondition.COMM_RELAY_MOD_ID);
		}

	}

	@Override
	public boolean showShutDown() {
		return false;
	}

	@Override
	public String getCanNotShutDownReason() {
		// return "Use \"Abandon Colony\" instead.";
		return null;
	}

	@Override
	public boolean canShutDown() {
		return false;
	}

	@Override
	protected String getDescriptionOverride() {
		int size = market.getSize();
		String cid = null;
		if (size >= 1 && size <= 9) {
			cid = "population_" + size;
			MarketConditionSpecAPI mcs = Global.getSettings().getMarketConditionSpec(cid);
			if (mcs != null) {
				return spec.getDesc() + "\n\n" + mcs.getDesc().replaceAll("\\$marketName", market.getName()).replaceAll("\\$MarketName", market.getName());
			}
		}
		return super.getDescriptionOverride();
	}

	public String getBuildOrUpgradeProgressText() {
		if (isUpgrading()) {
			return "total growth: " + Misc.getRoundedValue(Misc.getMarketSizeProgress(market) * 100f) + "%";
		}
		return super.getBuildOrUpgradeProgressText();
	}

	@Override
	public float getBuildOrUpgradeProgress() {
		if (!super.isBuilding() && market.getSize() < Misc.getMaxMarketSize(market)) {
			return Misc.getMarketSizeProgress(market);
		}
		return super.getBuildOrUpgradeProgress();
	}

	@Override
	public boolean isBuilding() {
		if (!super.isBuilding() && market.getSize() < Misc.getMaxMarketSize(market) && getBuildOrUpgradeProgress() > 0)
			return true;

		return super.isBuilding();
	}

	@Override
	public boolean isUpgrading() {
		if (!super.isBuilding() && market.getSize() < Misc.getMaxMarketSize(market))
			return true;

		return super.isUpgrading();
	}

	public void modifyIncoming(MarketAPI market, PopulationComposition incoming) {
		float patherLevel = 0;
		for (Industry curr : market.getIndustries()) {
			patherLevel += getAICoreImpact(curr.getAICoreId());
		}

		String adminCoreId = market.getAdmin().getAICoreId();
		if (adminCoreId != null) {
			patherLevel += 10f * getAICoreImpact(adminCoreId);
		}

		List<String> targeted = new ArrayList<String>();
		targeted.add(Industries.TECHMINING);
		targeted.add(Industries.HEAVYINDUSTRY);
		targeted.add(Industries.FUELPROD);
		targeted.add(Industries.STARFORTRESS);

		for (String curr : targeted) {
			if (market.hasIndustry(curr)) {
				patherLevel += 10f;
			}
		}

		if (patherLevel > 0) {
			incoming.add(Factions.LUDDIC_PATH, patherLevel * 0.2f);
		}
	}

	private float getAICoreImpact(String coreId) {
		if (Commodities.ALPHA_CORE.equals(coreId))
			return 10f;
		if (Commodities.BETA_CORE.equals(coreId))
			return 4f;
		if (Commodities.GAMMA_CORE.equals(coreId))
			return 1f;
		return 0f;
	}

	public boolean canBeDisrupted() {
		return false;
	}

	public int getMaxIndustries() {
		return getMaxIndustries(market.getSize());
	}

	public static int[] MAX_IND = null;

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

	@Override
	public boolean canImprove() {
		return true;
	}

	protected void applyImproveModifiers() {
		if (isImproved()) {
			market.getStability().modifyFlat("PAI_improve", IMPROVE_STABILITY_BONUS,
					getImprovementsDescForModifiers() + " (" + getNameForModifier() + ")");
		} else {
			market.getStability().unmodifyFlat("PAI_improve");
		}
	}

	public void addImproveDesc(TooltipMakerAPI info, ImprovementDescriptionMode mode) {
		float opad = 10f;
		Color highlight = Misc.getHighlightColor();

		if (mode == ImprovementDescriptionMode.INDUSTRY_TOOLTIP) {
			info.addPara("Stability increased by %s.", 0f, highlight, "" + IMPROVE_STABILITY_BONUS);
		} else {
			info.addPara("Increases stability by %s.", 0f, highlight, "" + IMPROVE_STABILITY_BONUS);
		}

		info.addSpacer(opad);
		super.addImproveDesc(info, mode);
	}

	protected static class LampRemover implements EveryFrameScript {
		protected SectorEntityToken lamp;
		protected MarketAPI market;
		protected LtvPopulationAndInfrastructure industry;

		public LampRemover(SectorEntityToken lamp, MarketAPI market, LtvPopulationAndInfrastructure industry) {
			this.lamp = lamp;
			this.market = market;
			this.industry = industry;
		}

		public void advance(float amount) {
			Industry ind = market.getIndustry(Industries.POPULATION);
			SpecialItemData item = ind == null ? null : ind.getSpecialItem();
			if (item == null || !item.getId().equals(Items.ORBITAL_FUSION_LAMP)) {
				Misc.fadeAndExpire(lamp);
				industry.lamp = null;
				lamp = null;
			}
		}

		public boolean isDone() {
			return lamp == null;
		}

		public boolean runWhilePaused() {
			return false;
		}
	}

	protected String addedHeatCondition = null;
	protected String removedHeatCondition = null;
	protected SectorEntityToken lamp;

	@Override
	public void setSpecialItem(SpecialItemData special) {
		super.setSpecialItem(special);

		if (addedHeatCondition != null && (special == null || !special.getId().equals(Items.ORBITAL_FUSION_LAMP))) {
			market.removeCondition(addedHeatCondition);
			addedHeatCondition = null;
			if (removedHeatCondition != null) {
				market.addCondition(removedHeatCondition);
				removedHeatCondition = null;
			}
		}

		if (special != null && special.getId().equals(Items.ORBITAL_FUSION_LAMP)) {
			if (lamp == null) {
				SectorEntityToken focus = market.getPlanetEntity();
				if (focus == null)
					focus = market.getPrimaryEntity();
				if (focus != null) {
					EntityLocation loc = new EntityLocation();
					float radius = focus.getRadius() + 100f;
					loc.orbit = Global.getFactory().createCircularOrbit(focus, (float) Math.random() * 360f, radius,
							radius / (10f + 10f * (float) Math.random()));
					AddedEntity added = BaseThemeGenerator.addNonSalvageEntity(market.getContainingLocation(), loc,
							Entities.FUSION_LAMP, getMarket().getFactionId());
					if (added != null) {
						lamp = added.entity;
						market.getContainingLocation().addScript(new LampRemover(lamp, market, this));
					}
				}
			}
			if (addedHeatCondition == null &&
					!market.hasCondition(Conditions.COLD) &&
					!market.hasCondition(Conditions.VERY_COLD) &&
					!market.hasCondition(Conditions.VERY_HOT)) {
				if (market.hasCondition(Conditions.HOT)) {
					addedHeatCondition = Conditions.VERY_HOT;
					removedHeatCondition = Conditions.HOT;
				} else {
					addedHeatCondition = Conditions.HOT;
				}
				if (removedHeatCondition != null)
					market.removeCondition(removedHeatCondition);
				if (addedHeatCondition != null)
					market.addCondition(addedHeatCondition);
			}
		}
	}

	@Override
	public boolean wantsToUseSpecialItem(SpecialItemData data) {
		if (special != null)
			return false;

		if (Items.ORBITAL_FUSION_LAMP.equals(data.getId())) {
			for (String mc : LtvItemEffectsRepo.FUSION_LAMP_CONDITIONS) {
				if (market.hasCondition(mc))
					return true;
			}
			return false;
		}
		return super.wantsToUseSpecialItem(data);
	}

	public static Pair<SectorEntityToken, Float> getNearestCoronalTap(Vector2f locInHyper, boolean usable) {
		return getNearestCoronalTap(locInHyper, usable, false);
	}

	public static Pair<SectorEntityToken, Float> getNearestCoronalTap(Vector2f locInHyper, boolean usable,
			boolean requireDefendersDefeated) {
		SectorEntityToken nearest = null;
		float minDist = Float.MAX_VALUE;

		for (IntelInfoPlugin intel : Global.getSector().getIntelManager().getIntel(HypershuntIntel.class)) {
			HypershuntIntel hypershunt = (HypershuntIntel) intel;
			if (requireDefendersDefeated && !hypershunt.defendersDefeated()) {
				continue;
			}
			SectorEntityToken entity = hypershunt.getEntity();
			if (!usable || entity.getMemoryWithoutUpdate().contains("$usable")) {
				float dist = Misc.getDistanceLY(locInHyper, entity.getLocationInHyperspace());
				if (dist > LtvItemEffectsRepo.CORONAL_TAP_LIGHT_YEARS &&
						Math.round(dist * 10f) <= LtvItemEffectsRepo.CORONAL_TAP_LIGHT_YEARS * 10f) {
					dist = LtvItemEffectsRepo.CORONAL_TAP_LIGHT_YEARS;
				}
				if (dist < minDist) {
					minDist = dist;
					nearest = entity;
				}
			}
		}

		if (nearest == null)
			return null;

		return new Pair<SectorEntityToken, Float>(nearest, minDist);
	}

	public static class CoronalTapFactor implements ColonyOtherFactorsListener {
		public boolean isActiveFactorFor(SectorEntityToken entity) {
			return getNearestCoronalTap(entity.getLocationInHyperspace(), true) != null;
		}

		public void printOtherFactors(TooltipMakerAPI text, SectorEntityToken entity) {
			Pair<SectorEntityToken, Float> p = getNearestCoronalTap(entity.getLocationInHyperspace(), true);
			if (p == null) {
				return;
			}
			Color h = Misc.getHighlightColor();
			float opad = 10f;

			String dStr = Misc.getRoundedValueMaxOneAfterDecimal(p.two);
			String lights = "light-years";
			if (dStr.equals("1")) {
				lights = "light-year";
			}

			if (p.two > LtvItemEffectsRepo.CORONAL_TAP_LIGHT_YEARS) {
				text.addPara(
						"The nearest coronal tap is located in the "
								+ p.one.getContainingLocation().getNameWithLowercaseType() + ", %s " + lights
								+ " away. The maximum range at a portal can connect to a tap is %s light-years.",
						opad, h,
						Misc.getRoundedValueMaxOneAfterDecimal(p.two),
						Integer.toString(LtvItemEffectsRepo.CORONAL_TAP_LIGHT_YEARS));
			} else {
				text.addPara(
						"The nearest coronal tap is located in the "
								+ p.one.getContainingLocation().getNameWithLowercaseType() + ", %s " + lights
								+ " away, allowing " + "a coronal portal located here to connect to it.",
						opad, h,
						Misc.getRoundedValueMaxOneAfterDecimal(p.two));
			}
		}
	}

}
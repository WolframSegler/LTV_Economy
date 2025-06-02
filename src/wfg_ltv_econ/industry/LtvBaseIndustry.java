package wfg_ltv_econ.industry;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.awt.Color;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CargoAPI;
import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.campaign.SpecialItemData;
import com.fs.starfarer.api.campaign.SpecialItemSpecAPI;
import com.fs.starfarer.api.campaign.comm.CommMessageAPI.MessageClickAction;
import com.fs.starfarer.api.campaign.econ.CommodityOnMarketAPI;
import com.fs.starfarer.api.campaign.econ.CommoditySpecAPI;
import com.fs.starfarer.api.campaign.econ.Industry;
import com.fs.starfarer.api.campaign.econ.InstallableIndustryItemPlugin;
import com.fs.starfarer.api.campaign.econ.InstallableIndustryItemPlugin.InstallableItemDescriptionMode;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI.MarketInteractionMode;
import com.fs.starfarer.api.campaign.econ.MarketImmigrationModifier;
import com.fs.starfarer.api.campaign.econ.MutableCommodityQuantity;
import com.fs.starfarer.api.campaign.impl.items.GenericSpecialItemPlugin;
import com.fs.starfarer.api.campaign.listeners.ListenerUtil;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.combat.MutableStat;
import com.fs.starfarer.api.combat.MutableStat.StatMod;
import com.fs.starfarer.api.impl.SharedUnlockData;
import com.fs.starfarer.api.impl.campaign.DebugFlags;
import com.fs.starfarer.api.impl.campaign.econ.impl.InstallableItemEffect;
import com.fs.starfarer.api.impl.campaign.econ.impl.ItemEffectsRepo;
import com.fs.starfarer.api.impl.campaign.econ.impl.ConstructionQueue.ConstructionQueueItem;
import com.fs.starfarer.api.impl.campaign.ids.Commodities;
import com.fs.starfarer.api.impl.campaign.ids.Industries;
import com.fs.starfarer.api.impl.campaign.ids.Items;
import com.fs.starfarer.api.impl.campaign.ids.Sounds;
import com.fs.starfarer.api.impl.campaign.ids.Stats;
import com.fs.starfarer.api.impl.campaign.ids.Strings;
import com.fs.starfarer.api.impl.campaign.ids.Submarkets;
import com.fs.starfarer.api.impl.campaign.ids.Tags;
import com.fs.starfarer.api.impl.campaign.intel.BaseIntelPlugin;
import com.fs.starfarer.api.impl.campaign.intel.MessageIntel;
import com.fs.starfarer.api.impl.campaign.rulecmd.salvage.MarketCMD.RaidDangerLevel;
import com.fs.starfarer.api.impl.codex.CodexDataV2;
import com.fs.starfarer.api.loading.IndustrySpecAPI;
import com.fs.starfarer.api.ui.Alignment;
import com.fs.starfarer.api.ui.IconRenderMode;
import com.fs.starfarer.api.ui.LabelAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.ui.UIComponentAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI.StatModValueGetter;
import com.fs.starfarer.api.util.Misc;
import com.fs.starfarer.api.util.Pair;
import wfg_ltv_econ.util.LtvNumFormat;

public abstract class LtvBaseIndustry implements Industry, Cloneable {

	public static final int SIZE_FOR_SMALL_IMAGE = 3;
	public static final int SIZE_FOR_LARGE_IMAGE = 6;

	public static final String BASE_VALUE_TEXT = "Base value for colony size";

	public static final float DEFAULT_IMPROVE_PRODUCTION_BONUS = 1.3f; // +30% output
	public static final float DEFAULT_INPUT_REDUCTION_BONUS = 0.75f; // -20% input

	public static final float ALPHA_CORE_PRODUCTION_BOOST = 1.3f; // +30% output

	public static final float ALPHA_CORE_UPKEEP_REDUCTION_MULT = 0.75f; // -20% upkeep
	public static final float BETA_CORE_UPKEEP_REDUCTION_MULT = 0.75f; // -20% upkeep

	public static final float ALPHA_CORE_INPUT_REDUCTION = 0.75f; // -20% input
	public static final float BETA_CORE_INPUT_REDUCTION = 0.75f; // -20% input
	public static final float GAMMA_CORE_INPUT_REDUCTION = 0.75f; // -20% input

	@Override
	protected LtvBaseIndustry clone() {
		LtvBaseIndustry copy = null;
		try {
			copy = (LtvBaseIndustry) super.clone();
		} catch (CloneNotSupportedException e) {
			e.printStackTrace();
		}
		return copy;
	}

	public static String getDeficitText(String commodityId) {
		if (commodityId == null) {
			return "Various shortages";
		}
		CommoditySpecAPI spec = Global.getSettings().getCommoditySpec(commodityId);
		return Misc.ucFirst(spec.getName().toLowerCase() + " shortage");
	}

	protected Map<String, MutableCommodityQuantity> supply = new LinkedHashMap<String, MutableCommodityQuantity>();
	protected Map<String, MutableCommodityQuantity> demand = new LinkedHashMap<String, MutableCommodityQuantity>();

	protected MutableStat income = new MutableStat(0f);
	protected MutableStat upkeep = new MutableStat(0f);
	protected MarketAPI market;

	protected String id;

	protected float buildProgress = 0f;
	protected float buildTime = 1f;
	protected boolean building = false;
	protected Boolean improved = null;
	protected String upgradeId = null;

	protected transient IndustrySpecAPI spec = null;

	protected String aiCoreId = null;

	protected MutableStat demandReduction = new MutableStat(0);
	protected MutableStat supplyBonus = new MutableStat(0);

	protected transient MutableStat demandReductionFromOther = new MutableStat(0);
	protected transient MutableStat supplyBonusFromOther = new MutableStat(0);

	protected int workersAssigned = 1000;

	public LtvBaseIndustry() {

	}

	public MutableStat getDemandReduction() {
		return demandReduction;
	}

	public MutableStat getSupplyBonus() {
		return supplyBonus;
	}

	public MutableStat getDemandReductionFromOther() {
		if (demandReductionFromOther == null) {
			demandReductionFromOther = new MutableStat(0);
		}
		return demandReductionFromOther;
	}

	public MutableStat getSupplyBonusFromOther() {
		if (supplyBonusFromOther == null) {
			supplyBonusFromOther = new MutableStat(0);
		}
		return supplyBonusFromOther;
	}

	public void setMarket(MarketAPI market) {
		this.market = market;
	}

	public void init(String id, MarketAPI market) {
		this.id = id;
		this.market = market;
		readResolve();
	}

	private transient String modId;
	private transient String[] modIds;

	protected Object readResolve() {
		spec = Global.getSettings().getIndustrySpec(id);

		if (buildTime < 1f)
			buildTime = 1f;

		modId = "ind_" + id;
		modIds = new String[10];
		for (int i = 0; i < modIds.length; i++) {
			modIds[i] = modId + "_" + i;
		}

		if (demandReduction == null)
			demandReduction = new MutableStat(0);
		if (supplyBonus == null)
			supplyBonus = new MutableStat(0);

		if (supply != null) {
			for (String id : new ArrayList<String>(supply.keySet())) {
				MutableCommodityQuantity stat = supply.get(id);
				stat.getQuantity().unmodifyFlat("ind_sb");
			}
		}
		if (demand != null) {
			for (String id : new ArrayList<String>(demand.keySet())) {
				MutableCommodityQuantity stat = demand.get(id);
				stat.getQuantity().unmodifyFlat("ind_dr");
			}
		}

		return this;
	}

	protected Object writeReplace() {
		clearUnmodified();
		return this;
	}

	public void apply(Boolean updateIncomeAndUpkeep) {
		// Increased Production also increases the Demand
		updateSupplyAndDemandModifiers();

		if (updateIncomeAndUpkeep) {
			updateIncomeAndUpkeep();
		}
		applyModifiers();

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

	public void unapply() {
		applyNoAICoreModifiers();
		
		Boolean wasImproved = improved;
		improved = null;
		applyImproveModifiers(); // to unapply them
		improved = wasImproved;
		
		if (this instanceof MarketImmigrationModifier && market != null) {
			market.removeTransientImmigrationModifier((MarketImmigrationModifier) this);
		}
		
		if (special != null) {
			InstallableItemEffect effect = ItemEffectsRepo.ITEM_EFFECTS.get(special.getId());
			if (effect != null) {
				effect.unapply(this);
			}
		}
	}

	protected void applyModifiers() {
	}

	protected void applyNoAICoreModifiers() {
	}

	protected String getModId() {
		return modId;
	}

	protected String getModId(int index) {
		return modIds[index];
	}

	public boolean isPlayerOwned(MarketAPI market) {
		if (market == null)
			return false;

		FactionAPI playerFaction = Global.getSector().getPlayerFaction();
		return market.getFaction().equals(playerFaction);
	}

	protected void ltv_WeightedDeficitModifiers(Map<String, List<Pair<String, Float>>> CommodityList) {
		// The List<Float> contains the weight of resources (between 0 and 1) needed for
		// each commodity inside the Map

		for (Map.Entry<String, List<Pair<String, Float>>> commodity : CommodityList.entrySet()) {
			MutableStat commodity_supply = getSupply(commodity.getKey()).getQuantity();
			commodity_supply.unmodifyMult("ltv_weighted_deficit"); // clear previous deficit modifier

			float supplyMultiplier = 1f; // 1 means no reduction. 0 means complete reduction

			for (Pair<String, Float> element : commodity.getValue()) {
				float demand = getDemand(element.one).getQuantity().getModifiedValue();
				if (demand <= 0) {
					continue;
				}

				String Submarket = Submarkets.SUBMARKET_OPEN;
				if (market.getSubmarket(Submarkets.LOCAL_RESOURCES) != null) {
					Submarket = Submarkets.LOCAL_RESOURCES;
				} else if (market.getSubmarket(Submarkets.SUBMARKET_OPEN) == null) {
					return;
				}
				float available = market.getSubmarket(Submarket).getCargo().getCommodityQuantity(element.one);
				float availabilityRatio = Math.min(available / demand, 1); // rate of deficit compared to total demand
				float deficitImpact = element.two * (1f - availabilityRatio); // Weight × shortage

				// Apply the worst-case deficit
				supplyMultiplier = Math.min(supplyMultiplier, 1f - deficitImpact);
			}

			supplyMultiplier = Math.max(supplyMultiplier, 0.1f); // Minimum 10% production
			commodity_supply.modifyMult("ltv_weighted_deficit", supplyMultiplier);
		}
	}

	public void demand(String commodityId, int quantity) {
		demand(0, commodityId, quantity, BASE_VALUE_TEXT);
	}

	public void demand(String commodityId, int quantity, String desc) {
		demand(0, commodityId, quantity, desc);
	}

	public void demand(int index, String commodityId, int quantity, String desc) {
		demand(getModId(index), commodityId, quantity, desc);
	}

	public void demand(String modId, String commodityId, int quantity, String desc) {
		if (quantity == 0) {
			getDemand(commodityId).getQuantity().unmodifyFlat(modId);
		} else {
			getDemand(commodityId).getQuantity().modifyFlat(modId, quantity, desc);
		}

		if (quantity > 0) {
			if (!demandReduction.isUnmodified()) {
				getDemand(commodityId).getQuantity().modifyMult("ind_dr", demandReduction.getMult());
				getDemand(commodityId).getQuantity().modifyFlat("ind_dr", demandReduction.getFlatMod());
			} else {
				getDemand(commodityId).getQuantity().unmodifyMult("ind_dr");
				getDemand(commodityId).getQuantity().unmodifyFlat("ind_dr");
			}
		}
	}

	public void ltv_consume(String resource) {
		float consumption_amount = getDemand(resource).getQuantity().getModifiedValue();

		if (market == null)
			return;
		// Consume the commodity from the market’s stockpile
		String Submarket = Submarkets.SUBMARKET_OPEN;
		if (market.isPlayerOwned() && market.getSubmarket(Submarkets.LOCAL_RESOURCES) != null) {
			Submarket = Submarkets.LOCAL_RESOURCES;
		} else if (market.getSubmarket(Submarkets.SUBMARKET_OPEN) == null) {
			return;
		}

		float available = market.getSubmarket(Submarket).getCargo().getCommodityQuantity(resource);
		consumption_amount = Math.min(consumption_amount, available); // only take what’s available

		market.getSubmarket(Submarket).getCargo().removeItems(CargoAPI.CargoItemType.RESOURCES, resource, consumption_amount);
	}

	public float ltv_precalculatecost(float... costlist) {
		float cost = 0f;
		for (float elementcost : costlist) {
			cost += elementcost;
		}
		return cost;
	}

	public void supply(String commodityId, int quantity) {
		supply(0, commodityId, quantity, BASE_VALUE_TEXT);
	}

	public void supply(String commodityId, int quantity, String desc) {
		supply(0, commodityId, quantity, desc);
	}

	public void supply(int index, String commodityId, int quantity, String desc) {
		supply(getModId(index), commodityId, quantity, desc);
	}

	public void supply(String modId, String commodityId, int quantity, String desc) {
		if (quantity == 0) {
			getSupply(commodityId).getQuantity().unmodifyFlat(modId);
		} else {
			getSupply(commodityId).getQuantity().modifyFlat(modId, quantity, desc);
		}

		if (quantity > 0) {
			if (!supplyBonus.isUnmodified()) {
				getSupply(commodityId).getQuantity().modifyMult("ind_sb", supplyBonus.getMult());
				getSupply(commodityId).getQuantity().modifyFlat("ind_sb", supplyBonus.getFlatMod());
			} else {
				getSupply(commodityId).getQuantity().unmodifyMult("ind_sb");
				getSupply(commodityId).getQuantity().unmodifyFlat("ind_sb");
			}
		}
	}

	public void ltv_produce(Map<String, List<Pair<String, Float>>> production) { // Commodity and the amount to produce
		for (Map.Entry<String, List<Pair<String, Float>>> commodity : production.entrySet()) {
			if (market.getCommodityData(commodity.getKey()).isMeta()) {
				continue;
			}
			int ProductionAmount = getSupply(commodity.getKey()).getQuantity().getModifiedInt();

			if (ProductionAmount <= 0 || market == null) {
				continue;
			}

			if (isPlayerOwned(market) && market.getSubmarket(Submarkets.LOCAL_RESOURCES) != null) {
				market.getSubmarket(Submarkets.LOCAL_RESOURCES).getCargo().addCommodity(commodity.getKey(),
						ProductionAmount);
				continue;
			}
			if (market.getSubmarket(Submarkets.SUBMARKET_OPEN) != null) {
				market.getSubmarket(Submarkets.SUBMARKET_OPEN).getCargo().addCommodity(commodity.getKey(),
						ProductionAmount);
			}
		}
	}

	protected void applyDeficitToProduction(int index, Pair<String, Integer> deficit, String... commodities) {
		for (String commodity : commodities) {
			if (getSupply(commodity).getQuantity().isUnmodified())
				continue;
			supply(index, commodity, -deficit.two, getDeficitText(deficit.one));
		}
	}

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

	public float getBuildTime() {
		return getSpec().getBuildTime();
	}

	protected Float buildCostOverride = null;

	public Float getBuildCostOverride() {
		return buildCostOverride;
	}

	public void setBuildCostOverride(float buildCostOverride) {
		this.buildCostOverride = buildCostOverride;
	}

	public float getBuildCost() {
		if (buildCostOverride != null)
			return buildCostOverride;
		return getSpec().getCost();
	}

	public float getBaseUpkeep() {
		float sizeMult = market.getSize();
		sizeMult = Math.max(1, sizeMult - 2);
		return getSpec().getUpkeep() * sizeMult;
	}

	protected boolean wasDisrupted = false;

	public void advance(float amount) {
		boolean disrupted = isDisrupted();
		if (!disrupted && wasDisrupted) {
			disruptionFinished();
		}
		wasDisrupted = disrupted;

		if (building && !disrupted) {
			float days = Global.getSector().getClock().convertToDays(amount);
			if (DebugFlags.COLONY_DEBUG) {
				days *= 100f;
			}
			buildProgress += days;

			if (buildProgress >= buildTime) {
				finishBuildingOrUpgrading();
			}
		}
	}

	protected void notifyDisrupted() {

	}

	protected void disruptionFinished() {

	}

	public boolean isBuilding() {
		return building;
	}

	public boolean isFunctional() {
		if (isDisrupted())
			return false;
		return !(isBuilding() && !isUpgrading());
	}

	public boolean isUpgrading() {
		return building && upgradeId != null;
	}

	public float getBuildOrUpgradeProgress() {
		if (isDisrupted()) {
			return 0f;
		}
		if (!isBuilding())
			return 0f;

		return Math.min(1f, buildProgress / buildTime);
	}

	public String getBuildOrUpgradeDaysText() {
		if (isDisrupted()) {
			int left = (int) getDisruptedDays();
			if (left < 1)
				left = 1;
			String days = "days";
			if (left == 1)
				days = "day";

			return "" + left + " " + days + "";
		}

		int left = (int) (buildTime - buildProgress);
		if (left < 1)
			left = 1;
		String days = "days";
		if (left == 1)
			days = "day";

		return left + " " + days;
	}

	public String getBuildOrUpgradeProgressText() {
		if (isDisrupted()) {
			int left = (int) getDisruptedDays();
			if (left < 1)
				left = 1;
			String days = "days";
			if (left == 1)
				days = "day";

			return "Disrupted: " + left + " " + days + " left";
		}

		int left = (int) (buildTime - buildProgress);
		if (left < 1)
			left = 1;
		String days = "days";
		if (left == 1)
			days = "day";

		if (isUpgrading()) {
			return "Upgrading: " + left + " " + days + " left";
		} else {
			return "Building: " + left + " " + days + " left";
		}
	}

	public void startBuilding() {
		building = true;
		buildProgress = 0;
		upgradeId = null;

		buildTime = spec.getBuildTime();
		unapply();
	}

	public void finishBuildingOrUpgrading() {
		building = false;
		buildProgress = 0;
		buildTime = 1f;
		if (upgradeId != null) {
			market.removeIndustry(getId(), null, true);
			market.addIndustry(upgradeId);
			LtvBaseIndustry industry = (LtvBaseIndustry) market.getIndustry(upgradeId);
			industry.setAICoreId(getAICoreId());
			industry.setImproved(isImproved());
			industry.upgradeFinished(this);
			industry.reapply();
		} else {
			buildingFinished();
			reapply();
		}
	}

	public void startUpgrading() {
		building = true;
		buildProgress = 0;
		upgradeId = getSpec().getUpgrade();

		IndustrySpecAPI upgrade = Global.getSettings().getIndustrySpec(upgradeId);
		buildTime = upgrade.getBuildTime();
	}

	public void cancelUpgrade() {
		building = false;
		buildProgress = 0;
		upgradeId = null;
	}

	public void downgrade() {
		building = true;
		buildProgress = 0;
		upgradeId = getSpec().getDowngrade();
		finishBuildingOrUpgrading();
	}

	public void reapply() {
		unapply();
		apply();
	}

	protected void buildingFinished() {
		sendBuildOrUpgradeMessage();
		buildNextInQueue(market);
	}

	public static void buildNextInQueue(MarketAPI market) {
		ConstructionQueueItem next = null;
		Iterator<ConstructionQueueItem> iter = market.getConstructionQueue().getItems().iterator();
		while (iter.hasNext()) {
			next = iter.next();
			iter.remove();

			Industry ind = market.instantiateIndustry(next.id);
			int num = Misc.getNumIndustries(market);
			int max = Misc.getMaxIndustries(market);
			if (ind.isAvailableToBuild() && (num <= max || !ind.isIndustry())) { // <= because num includes what's queued
				break;
			} else {
				if (market.isPlayerOwned()) {
					MessageIntel intel = new MessageIntel(ind.getCurrentName() + " at " + market.getName(),
							Misc.getBasePlayerColor());
					intel.addLine(BaseIntelPlugin.BULLET + "Construction aborted");

					int refund = next.cost;
					Global.getSector().getPlayerFleet().getCargo().getCredits().add(refund);
					intel.addLine(BaseIntelPlugin.BULLET + "%s refunded",
							Misc.getTextColor(),
							new String[] { Misc.getDGSCredits(refund) }, Misc.getHighlightColor());
					intel.setIcon(Global.getSector().getPlayerFaction().getCrest());
					intel.setSound(BaseIntelPlugin.getSoundStandardUpdate());
					Global.getSector().getCampaignUI().addMessage(intel, MessageClickAction.COLONY_INFO, market);
				}
				next = null;
			}
		}

		if (next != null) {
			market.addIndustry(next.id);
			Industry ind = market.getIndustry(next.id);
			ind.startBuilding();
			if (ind instanceof LtvBaseIndustry) {
				((LtvBaseIndustry) ind).setBuildCostOverride(next.cost);
			}

			if (market.isPlayerOwned()) {
				MessageIntel intel = new MessageIntel(ind.getCurrentName() + " at " + market.getName(),
						Misc.getBasePlayerColor());
				intel.addLine(BaseIntelPlugin.BULLET + "Construction started");
				intel.setIcon(Global.getSector().getPlayerFaction().getCrest());
				intel.setSound(BaseIntelPlugin.getSoundStandardUpdate());
				Global.getSector().getCampaignUI().addMessage(intel, MessageClickAction.COLONY_INFO, market);
			}
		}
	}

	protected void upgradeFinished(Industry previous) {
		sendBuildOrUpgradeMessage();

		setSpecialItem(previous.getSpecialItem());
	}

	protected void sendBuildOrUpgradeMessage() {
		if (market.isPlayerOwned()) {
			MessageIntel intel = new MessageIntel(getCurrentName() + " at " + market.getName(),
					Misc.getBasePlayerColor());
			intel.addLine(BaseIntelPlugin.BULLET + "Construction completed");
			intel.setIcon(Global.getSector().getPlayerFaction().getCrest());
			intel.setSound(BaseIntelPlugin.getSoundStandardUpdate());
			Global.getSector().getCampaignUI().addMessage(intel, MessageClickAction.COLONY_INFO, market);
		}
	}

	public void notifyBeingRemoved(MarketInteractionMode mode, boolean forUpgrade) {
		if (aiCoreId != null && !forUpgrade) {
			CargoAPI cargo = getCargoForInteractionMode(mode);
			if (cargo != null) {
				cargo.addCommodity(aiCoreId, 1);
			}
		}

		if (special != null && !forUpgrade) {
			CargoAPI cargo = getCargoForInteractionMode(mode);
			if (cargo != null) {
				cargo.addSpecial(special, 1);
			}
		}
	}

	protected CargoAPI getCargoForInteractionMode(MarketInteractionMode mode) {
		CargoAPI cargo = null;
		if (mode == null)
			return null;

		if (mode == MarketInteractionMode.REMOTE) {
			cargo = Misc.getStorageCargo(market);
		} else {
			cargo = Global.getSector().getPlayerFleet().getCargo();
		}
		return cargo;
	}

	public String getId() {
		return id;
	}

	public IndustrySpecAPI getSpec() {
		if (spec == null)
			spec = Global.getSettings().getIndustrySpec(id);
		return spec;
	}

	public void clearUnmodified() {
		if (supply != null) {
			for (String id : new ArrayList<String>(supply.keySet())) {
				MutableCommodityQuantity stat = supply.get(id);
				if (stat != null && (stat.getQuantity().isUnmodified() || stat.getQuantity().getModifiedValue() <= 0)) {
					supply.remove(id);
				}
			}
		}
		if (demand != null) {
			for (String id : new ArrayList<String>(demand.keySet())) {
				MutableCommodityQuantity stat = demand.get(id);
				if (stat != null && (stat.getQuantity().isUnmodified() || stat.getQuantity().getModifiedValue() <= 0)) {
					demand.remove(id);
				}
			}
		}
	}

	public List<MutableCommodityQuantity> getAllDemand() {
		List<MutableCommodityQuantity> result = new ArrayList<MutableCommodityQuantity>();
		for (MutableCommodityQuantity q : demand.values()) {
			if (q.getQuantity().getModifiedValue() > 0) {
				result.add(q);
			}
		}
		return result;
	}

	public List<MutableCommodityQuantity> getAllSupply() {
		List<MutableCommodityQuantity> result = new ArrayList<MutableCommodityQuantity>();
		for (MutableCommodityQuantity q : supply.values()) {
			if (q.getQuantity().getModifiedValue() > 0) {
				result.add(q);
			}
		}
		return result;
	}

	public MutableCommodityQuantity getSupply(String id) {
		MutableCommodityQuantity stat = supply.get(id);
		if (stat == null) {
			stat = new MutableCommodityQuantity(id);
			supply.put(id, stat);
		}
		return stat;
	}

	public MutableCommodityQuantity getDemand(String id) {
		MutableCommodityQuantity stat = demand.get(id);
		if (stat == null) {
			stat = new MutableCommodityQuantity(id);
			demand.put(id, stat);
		}
		return stat;
	}

	public MutableStat getIncome() {
		return income;
	}

	public MutableStat getUpkeep() {
		return upkeep;
	}

	public MarketAPI getMarket() {
		return market;
	}

	public Pair<String, Integer> getMaxDeficit(String... commodityIds) {
		Pair<String, Integer> result = new Pair<String, Integer>();
		result.two = 0;
		if (Global.CODEX_TOOLTIP_MODE)
			return result;
		for (String id : commodityIds) {
			int demand = (int) getDemand(id).getQuantity().getModifiedValue();
			CommodityOnMarketAPI com = market.getCommodityData(id);
			int available = com.getAvailable();

			int deficit = Math.max(demand - available, 0);
			if (deficit > result.two) {
				result.one = id;
				result.two = deficit;
			}
		}
		return result;
	}

	public List<Pair<String, Integer>> getAllDeficit() {
		List<String> commodities = new ArrayList<String>();
		for (MutableCommodityQuantity curr : demand.values()) {
			commodities.add(curr.getCommodityId());
		}
		return getAllDeficit(commodities.toArray(new String[0]));
	}

	public List<Pair<String, Integer>> getAllDeficit(String... commodityIds) {
		List<Pair<String, Integer>> result = new ArrayList<Pair<String, Integer>>();
		for (String id : commodityIds) {
			int demand = (int) getDemand(id).getQuantity().getModifiedValue();
			CommodityOnMarketAPI com = market.getCommodityData(id);
			int available = com.getAvailable();

			int deficit = Math.max(demand - available, 0);
			if (deficit > 0) {
				Pair<String, Integer> curr = new Pair<String, Integer>();
				curr.one = id;
				curr.two = deficit;
				result.add(curr);
			}
		}
		return result;
	}

	public static float getCommodityEconUnitMult(float size) {
		if (size <= 0)
			return 0f;
		return 1f;
	}

	public void doPreSaveCleanup() {
		supply = null;
		demand = null;
		income = null;
		upkeep = null;
	}

	public void doPostSaveRestore() {
		supply = new LinkedHashMap<String, MutableCommodityQuantity>();
		demand = new LinkedHashMap<String, MutableCommodityQuantity>();

		income = new MutableStat(0f);
		upkeep = new MutableStat(0f);
	}

	public String getCurrentImage() {
		return getSpec().getImageName();
	}

	public String getCurrentName() {
		return getSpec().getName();
	}

	public boolean isAvailableToBuild() {
		if (market.hasTag(Tags.MARKET_NO_INDUSTRIES_ALLOWED))
			return false;
		return market.hasIndustry(Industries.POPULATION) && !getId().equals(Industries.POPULATION);
	}

	public boolean showWhenUnavailable() {
		if (market.hasTag(Tags.MARKET_NO_INDUSTRIES_ALLOWED))
			return false;
		return true;
	}

	public String getUnavailableReason() {
		return "Can not be built";
	}

	public boolean isTooltipExpandable() {
		return false;
	}

	public float getTooltipWidth() {
		return 400f;
	}

	protected transient IndustryTooltipMode currTooltipMode = null;

	public void createTooltip(IndustryTooltipMode mode, TooltipMakerAPI tooltip, boolean expanded) {

		if (getSpec() != null && getSpec().hasTag(Tags.CODEX_UNLOCKABLE)) {
			SharedUnlockData.get().reportPlayerAwareOfIndustry(getSpec().getId(), true);
		}
		tooltip.setCodexEntryId(CodexDataV2.getIndustryEntryId(getSpec().getId()));

		currTooltipMode = mode;

		final float pad = 3f;
		final float opad = 10f;

		FactionAPI faction = market.getFaction();
		Color color = faction.getBaseUIColor();
		Color dark = faction.getDarkUIColor();

		Color gray = Misc.getGrayColor();
		Color highlight = Misc.getHighlightColor();
		Color bad = Misc.getNegativeHighlightColor();

		MarketAPI copy = market.clone();
		// the copy is a shallow copy and its conditions point to the original market
		// so, make it share the suppressed conditions list, too, otherwise
		// e.g. SolarArray will suppress conditions in the original market and the copy
		// will still apply them
		copy.setSuppressedConditions(market.getSuppressedConditions());
		copy.setRetainSuppressedConditionsSetWhenEmpty(true);
		market.setRetainSuppressedConditionsSetWhenEmpty(true);
		MarketAPI orig = market;

		market = copy;
		boolean needToAddIndustry = !market.hasIndustry(getId());

		if (needToAddIndustry)
			market.getIndustries().add(this);

		if (mode != IndustryTooltipMode.NORMAL) {
			market.clearCommodities();
			for (CommodityOnMarketAPI curr : market.getAllCommodities()) {
				curr.getAvailableStat().setBaseValue(100);
			}
		}

		market.reapplyConditions();
		reapply();

		String type = "";
		if (isIndustry())
			type = " - Industry";
		if (isStructure())
			type = " - Structure";

		tooltip.addTitle(getCurrentName() + type, color);

		String desc = spec.getDesc();
		String override = getDescriptionOverride();
		if (override != null) {
			desc = override;
		}
		desc = Global.getSector().getRules().performTokenReplacement(null, desc, market.getPrimaryEntity(), null);

		tooltip.addPara(desc, opad);

		if (isIndustry() && (mode == IndustryTooltipMode.ADD_INDUSTRY ||
				mode == IndustryTooltipMode.UPGRADE ||
				mode == IndustryTooltipMode.DOWNGRADE)) {

			int num = Misc.getNumIndustries(market);
			int max = Misc.getMaxIndustries(market);

			// during the creation of the tooltip, the market has both the current industry
			// and the upgrade/downgrade. So if this upgrade/downgrade counts as an
			// industry, it'd count double if
			// the current one is also an industry. Thus reduce num by 1 if that's the case.
			if (isIndustry()) {
				if (mode == IndustryTooltipMode.UPGRADE) {
					for (Industry curr : market.getIndustries()) {
						if (getSpec().getId().equals(curr.getSpec().getUpgrade())) {
							if (curr.isIndustry()) {
								num--;
							}
							break;
						}
					}
				} else if (mode == IndustryTooltipMode.DOWNGRADE) {
					for (Industry curr : market.getIndustries()) {
						if (getSpec().getId().equals(curr.getSpec().getDowngrade())) {
							if (curr.isIndustry()) {
								num--;
							}
							break;
						}
					}
				}
			}

			if (num > max) {

				num--;
				tooltip.addPara("Maximum number of industries reached", bad, opad);
			}
		}

		addRightAfterDescriptionSection(tooltip, mode);

		if (isDisrupted()) {
			int left = (int) getDisruptedDays();
			if (left < 1)
				left = 1;
			String days = "days";
			if (left == 1)
				days = "day";

			tooltip.addPara("Operations disrupted! %s " + days + " until return to normal function.",
					opad, Misc.getNegativeHighlightColor(), highlight, Integer.toString(left));
		}

		if (DebugFlags.COLONY_DEBUG || market.isPlayerOwned()) {
			if (mode == IndustryTooltipMode.NORMAL) {
				if (getSpec().getUpgrade() != null && !isBuilding()) {
					tooltip.addPara("Click to manage or upgrade", Misc.getPositiveHighlightColor(), opad);
				} else {
					tooltip.addPara("Click to manage", Misc.getPositiveHighlightColor(), opad);
				}
			}
		}

		if (mode == IndustryTooltipMode.QUEUED) {
			tooltip.addPara("Click to remove or adjust position in queue", Misc.getPositiveHighlightColor(), opad);
			tooltip.addPara("Currently queued for construction. Does not have any impact on the colony.", opad);

			int left = Math.round((getSpec().getBuildTime()));
			String days = "days";
			if (left < 2)
				days = "day";
			tooltip.addPara("Requires %s " + days + " to build.", opad, highlight, "" + left);

			// return;
		} else if (!isFunctional() && mode == IndustryTooltipMode.NORMAL && !isDisrupted()) {
			tooltip.addPara("Currently under construction and not producing anything or providing other benefits.",
					opad);

			int left = Math.round((buildTime - buildProgress));
			String days = "days";
			if (left < 2)
				days = "day";
			tooltip.addPara("Requires %s more " + days + " to finish building.", opad, highlight, "" + left);
		}

		if (!isAvailableToBuild() &&
				(mode == IndustryTooltipMode.ADD_INDUSTRY ||
						mode == IndustryTooltipMode.UPGRADE ||
						mode == IndustryTooltipMode.DOWNGRADE)) {
			String reason = getUnavailableReason();
			if (reason != null) {
				tooltip.addPara(reason, bad, opad);
			}
		}

		boolean category = getSpec().hasTag(Industries.TAG_PARENT);

		if (!category) {
			int credits = (int) Global.getSector().getPlayerFleet().getCargo().getCredits().get();
			String creditsStr = Misc.getDGSCredits(credits);
			if (mode == IndustryTooltipMode.UPGRADE || mode == IndustryTooltipMode.ADD_INDUSTRY) {
				int cost = (int) getBuildCost();
				String costStr = Misc.getDGSCredits(cost);

				int days = (int) getBuildTime();
				String daysStr = "days";
				if (days == 1)
					daysStr = "day";

				LabelAPI label = null;
				if (mode == IndustryTooltipMode.UPGRADE) {
					label = tooltip.addPara("%s and %s " + daysStr + " to upgrade. You have %s.", opad,
							highlight, costStr, Integer.toString(days), creditsStr);
				} else {
					label = tooltip.addPara("%s and %s " + daysStr + " to build. You have %s.", opad,
							highlight, costStr, Integer.toString(days), creditsStr);
				}
				label.setHighlight(costStr, Integer.toString(days), creditsStr);
				if (credits >= cost) {
					label.setHighlightColors(highlight, highlight, highlight);
				} else {
					label.setHighlightColors(bad, highlight, highlight);
				}
			} else if (mode == IndustryTooltipMode.DOWNGRADE) {
				if (getSpec().getUpgrade() != null) {
					float refundFraction = Global.getSettings().getFloat("industryRefundFraction");

					IndustrySpecAPI spec = Global.getSettings().getIndustrySpec(getSpec().getUpgrade());
					int cost = (int) (spec.getCost() * refundFraction);
					String refundStr = Misc.getDGSCredits(cost);

					tooltip.addPara("%s refunded for downgrade.", opad, highlight, refundStr);
				}
			}

			addPostDescriptionSection(tooltip, mode);

			if (!getIncome().isUnmodified()) {
				int income = getIncome().getModifiedInt();
				tooltip.addPara("Monthly income: %s", opad, highlight, Misc.getDGSCredits(income));
				tooltip.addStatModGrid(300, 65, 10, pad, getIncome(), true, new StatModValueGetter() {
					public String getPercentValue(StatMod mod) {
						return null;
					}

					public String getMultValue(StatMod mod) {
						return null;
					}

					public Color getModColor(StatMod mod) {
						return null;
					}

					public String getFlatValue(StatMod mod) {
						return Misc.getWithDGS(mod.value) + Strings.C;
					}
				});
			}

			if (!getUpkeep().isUnmodified()) {
				int upkeep = getUpkeep().getModifiedInt();
				tooltip.addPara("Monthly upkeep: %s", opad, highlight, Misc.getDGSCredits(upkeep));
				tooltip.addStatModGrid(300, 65, 10, pad, getUpkeep(), true, new StatModValueGetter() {

					public String getPercentValue(StatMod mod) {
						return null;
					}

					public String getMultValue(StatMod mod) {
						return null;
					}

					public Color getModColor(StatMod mod) {
						return null;
					}

					public String getFlatValue(StatMod mod) {
						return Misc.getWithDGS(mod.value) + Strings.C;
					}
				});
			}

			addPostUpkeepSection(tooltip, mode);

			boolean hasSupply = false;
			for (MutableCommodityQuantity curr : supply.values()) {
				int quantity = curr.getQuantity().getModifiedInt();
				if (quantity < 1)
					continue;
				hasSupply = true;
				break;
			}
			boolean hasDemand = false;
			for (MutableCommodityQuantity curr : demand.values()) {
				int quantity = curr.getQuantity().getModifiedInt();
				if (quantity < 1)
					continue;
				hasDemand = true;
				break;
			}

			final float iconSize = 32f;
			final int itemsPerRow = 3;


			if (hasSupply) {
				tooltip.addSectionHeading("Production", color, dark, Alignment.MID, opad);

				float startY = tooltip.getHeightSoFar() + opad;

				float x = opad;
				float y = startY;
				float sectionWidth = (getTooltipWidth() / itemsPerRow) - opad;
				int count = -1;

				for (MutableCommodityQuantity curr : supply.values()) {
					CommoditySpecAPI commodity = market.getCommodityData(curr.getCommodityId()).getCommodity();
					CommoditySpecAPI commoditySpec = Global.getSettings().getCommoditySpec(curr.getCommodityId());
					int pAmount = curr.getQuantity().getModifiedInt();

					if (pAmount < 1) {continue;}

					// wrap to next line if needed
					count++;
					if (count % itemsPerRow == 0 && count !=0) {
						x = opad;
						y += iconSize + 5f; // line height + padding between rows
					}

					// draw icon
					tooltip.beginIconGroup();
					tooltip.setIconSpacingMedium();
					tooltip.addIcons(commodity, 1, IconRenderMode.NORMAL);
					tooltip.addIconGroup(0f);
					UIComponentAPI iconComp = tooltip.getPrev();

					// Add extra padding for thinner icons
					float actualIconWidth = iconSize * commoditySpec.getIconWidthMult();
					iconComp.getPosition().inTL(x + ((iconSize - actualIconWidth)*0.5f), y);

					// draw text
					String txt = Strings.X + LtvNumFormat.formatWithMaxDigits(pAmount);
					LabelAPI lbl = tooltip.addPara(txt + " / Day", 0f, highlight, txt);

					UIComponentAPI lblComp = tooltip.getPrev();
					float textH = lbl.computeTextHeight(txt);
					float textX = x + iconSize + pad;
					float textY = y + (iconSize - textH) * 0.5f;
					lblComp.getPosition().inTL(textX, textY);

					// advance X
					x += sectionWidth + 5f;
				}
				tooltip.setHeightSoFar(y);
				resetFlowLeft(tooltip, opad);
			}

			addPostSupplySection(tooltip, hasSupply, mode);

			if (hasDemand || hasPostDemandSection(hasDemand, mode)) {
				tooltip.addSectionHeading("Demand & effects", color, dark, Alignment.MID, opad);
			}

			if (hasDemand) {
				float startY = tooltip.getHeightSoFar() + opad;
				if (hasSupply) {
					startY += opad*1.5f;
				}

				float x = opad;
				float y = startY;
				float sectionWidth = (getTooltipWidth() / itemsPerRow) - opad;
				int count = -1;

				for (MutableCommodityQuantity curr : demand.values()) {
					CommodityOnMarketAPI commodity = market.getCommodityData(curr.getCommodityId());
					CommoditySpecAPI commoditySpec = Global.getSettings().getCommoditySpec(curr.getCommodityId());
					int dAmount = curr.getQuantity().getModifiedInt();
					int allDeficit = commodity.getDeficitQuantity();

					if (dAmount < 1) {continue;}

					// wrap to next line if needed
					count++;
					if (count % itemsPerRow == 0 && count !=0) {
						x = opad;
						y += iconSize + 5f; // line height + padding between rows
					}

					// draw icon
					tooltip.beginIconGroup();
					tooltip.setIconSpacingMedium();
					if (allDeficit > 0) {
						tooltip.addIcons(commodity, 1, IconRenderMode.DIM_RED);
					} else {
						tooltip.addIcons(commodity, 1, IconRenderMode.NORMAL);
					}
					tooltip.addIconGroup(0f);
					UIComponentAPI iconComp = tooltip.getPrev();

					// Add extra padding for thinner icons
					float actualIconWidth = iconSize * commoditySpec.getIconWidthMult();
					iconComp.getPosition().inTL(x + ((iconSize - actualIconWidth)*0.5f), y);

					// draw text
					String txt = Strings.X + LtvNumFormat.formatWithMaxDigits(dAmount);
					LabelAPI lbl = tooltip.addPara(txt + " / Day", 0f, highlight, txt);

					UIComponentAPI lblComp = tooltip.getPrev();
					float textH = lbl.computeTextHeight(txt);
					float textX = x + iconSize + pad;
					float textY = y + (iconSize - textH) * 0.5f;
					lblComp.getPosition().inTL(textX, textY);

					// advance X
					x += sectionWidth + 5f;
				}
				tooltip.setHeightSoFar(y);
				resetFlowLeft(tooltip, opad);
			}

			addPostDemandSection(tooltip, hasDemand, mode);

			if (!needToAddIndustry) {
				addInstalledItemsSection(mode, tooltip, expanded);
				addImprovedSection(mode, tooltip, expanded);

				ListenerUtil.addToIndustryTooltip(this, mode, tooltip, getTooltipWidth(), expanded);
			}

			tooltip.addPara(
					"*Shown production and demand values are already adjusted based on current market size and local conditions.",
					gray, opad);
			tooltip.addSpacer(opad + pad);
		}

		if (needToAddIndustry) {
			unapply();
			market.getIndustries().remove(this);
		}
		market = orig;
		market.setRetainSuppressedConditionsSetWhenEmpty(null);
		if (!needToAddIndustry) {
			reapply();
		}
		market.reapplyConditions();
	}

	public void addInstalledItemsSection(IndustryTooltipMode mode, TooltipMakerAPI tooltip, boolean expanded) {
		float opad = 10f;

		FactionAPI faction = market.getFaction();
		Color color = faction.getBaseUIColor();
		Color dark = faction.getDarkUIColor();

		LabelAPI heading = tooltip.addSectionHeading("Items", color, dark, Alignment.MID, opad);

		boolean addedSomething = false;
		if (aiCoreId != null) {
			AICoreDescriptionMode aiCoreDescMode = AICoreDescriptionMode.INDUSTRY_TOOLTIP;
			addAICoreSection(tooltip, aiCoreId, aiCoreDescMode);
			addedSomething = true;
		}
		addedSomething |= addNonAICoreInstalledItems(mode, tooltip, expanded);

		if (!addedSomething) {
			heading.setText("No items installed");
		}
	}

	protected boolean addNonAICoreInstalledItems(IndustryTooltipMode mode, TooltipMakerAPI tooltip, boolean expanded) {
		if (special == null)
			return false;

		float opad = 10f;
		SpecialItemSpecAPI spec = Global.getSettings().getSpecialItemSpec(special.getId());

		TooltipMakerAPI text = tooltip.beginImageWithText(spec.getIconName(), 48);
		InstallableItemEffect effect = LtvItemEffectsRepo.ITEM_EFFECTS.get(special.getId());
		effect.addItemDescription(this, text, special, InstallableItemDescriptionMode.INDUSTRY_TOOLTIP);
		tooltip.addImageWithText(opad);

		return true;
	}

	private static final void resetFlowLeft(TooltipMakerAPI tooltip, float opad) {
		LabelAPI alignReset = tooltip.addPara("", 0f);
		alignReset.getPosition().inTL(opad / 2, tooltip.getHeightSoFar());
	}

	public List<SpecialItemData> getVisibleInstalledItems() {
		List<SpecialItemData> result = new ArrayList<SpecialItemData>();
		if (special != null) {
			result.add(special);
		}
		return result;
	}

	public boolean wantsToUseSpecialItem(SpecialItemData data) {
		if (special != null)
			return false;
		SpecialItemSpecAPI spec = Global.getSettings().getSpecialItemSpec(data.getId());
		String[] industries = spec.getParams().split(",");
		Set<String> all = new HashSet<String>();
		for (String ind : industries)
			all.add(ind.trim());
		return all.contains(getId());
	}

	public void addAICoreSection(TooltipMakerAPI tooltip, AICoreDescriptionMode mode) {
		addAICoreSection(tooltip, aiCoreId, mode);
	}

	public void addAICoreSection(TooltipMakerAPI tooltip, String coreId, AICoreDescriptionMode mode) {
		float opad = 10f;

		if (mode == AICoreDescriptionMode.MANAGE_CORE_TOOLTIP) {
			if (coreId == null) {
				tooltip.addPara("No AI core currently assigned. Click to assign an AI core from your cargo.", opad);
				return;
			}
		}

		boolean alpha = coreId.equals(Commodities.ALPHA_CORE);
		boolean beta = coreId.equals(Commodities.BETA_CORE);
		boolean gamma = coreId.equals(Commodities.GAMMA_CORE);

		if (alpha) {
			addAlphaCoreDescription(tooltip, mode);
		} else if (beta) {
			addBetaCoreDescription(tooltip, mode);
		} else if (gamma) {
			addGammaCoreDescription(tooltip, mode);
		} else {
			addUnknownCoreDescription(coreId, tooltip, mode);
		}
	}

	protected void addAlphaCoreDescription(TooltipMakerAPI tooltip, AICoreDescriptionMode mode) {
		float opad = 10f;
		Color highlight = Misc.getHighlightColor();

		String pre = "Alpha-level AI core currently assigned. ";
		if (mode == AICoreDescriptionMode.MANAGE_CORE_DIALOG_LIST || mode == AICoreDescriptionMode.INDUSTRY_TOOLTIP) {
			pre = "Alpha-level AI core. ";
		}
		if (mode == AICoreDescriptionMode.INDUSTRY_TOOLTIP || mode == AICoreDescriptionMode.MANAGE_CORE_TOOLTIP) {
			CommoditySpecAPI coreSpec = Global.getSettings().getCommoditySpec(aiCoreId);
			TooltipMakerAPI text = tooltip.beginImageWithText(coreSpec.getIconName(), 48);
			text.addPara(pre + "Reduces demand by %s. Reduces upkeep cost by %s. " +
					"Increases production by %s. All modifiers are multiplicative", 0f, highlight,
					String.valueOf(Math.round((1f - ALPHA_CORE_INPUT_REDUCTION) * 100f)) + "%",
					String.valueOf(Math.round((1f - ALPHA_CORE_UPKEEP_REDUCTION_MULT) * 100f)) + "%",
					String.valueOf(Math.round((ALPHA_CORE_PRODUCTION_BOOST - 1f) * 100f)) + "%");
			tooltip.addImageWithText(opad);
			return;
		}

		tooltip.addPara(pre + "Reduces demand by %s. Reduces upkeep cost by %s. " +
				"Increases production by %s. All modifiers are multiplicative", 0f, highlight,
				String.valueOf(Math.round((1f - ALPHA_CORE_INPUT_REDUCTION) * 100f)) + "%",
				String.valueOf(Math.round((1f - ALPHA_CORE_UPKEEP_REDUCTION_MULT) * 100f)) + "%",
				String.valueOf(Math.round((ALPHA_CORE_PRODUCTION_BOOST - 1f) * 100f)) + "%");
	}

	protected void addBetaCoreDescription(TooltipMakerAPI tooltip, AICoreDescriptionMode mode) {
		float opad = 10f;
		Color highlight = Misc.getHighlightColor();

		String pre = "Beta-level AI core currently assigned. ";
		if (mode == AICoreDescriptionMode.MANAGE_CORE_DIALOG_LIST || mode == AICoreDescriptionMode.INDUSTRY_TOOLTIP) {
			pre = "Beta-level AI core. ";
		}
		if (mode == AICoreDescriptionMode.INDUSTRY_TOOLTIP || mode == AICoreDescriptionMode.MANAGE_CORE_TOOLTIP) {
			CommoditySpecAPI coreSpec = Global.getSettings().getCommoditySpec(aiCoreId);
			TooltipMakerAPI text = tooltip.beginImageWithText(coreSpec.getIconName(), 48);
			text.addPara(pre + "Reduces demand by %s.Reduces upkeep cost by %s. All modifiers are multiplicative", opad, highlight,
					String.valueOf(Math.round((1f - BETA_CORE_INPUT_REDUCTION) * 100f)) + "%",
					String.valueOf(Math.round((1f - BETA_CORE_UPKEEP_REDUCTION_MULT) * 100f)) + "%");
			tooltip.addImageWithText(opad);
			return;
		}

		tooltip.addPara(pre + "Reduces demand by %s. Reduces upkeep cost by %s. All modifiers are multiplicative", opad, highlight,
				String.valueOf(Math.round((1f - BETA_CORE_INPUT_REDUCTION) * 100f)) + "%",
				String.valueOf(Math.round((1f - BETA_CORE_UPKEEP_REDUCTION_MULT) * 100f)) + "%");
	}

	protected void addGammaCoreDescription(TooltipMakerAPI tooltip, AICoreDescriptionMode mode) {
		float opad = 10f;
		Color highlight = Misc.getHighlightColor();

		String pre = "Gamma-level AI core currently assigned. ";
		if (mode == AICoreDescriptionMode.MANAGE_CORE_DIALOG_LIST || mode == AICoreDescriptionMode.INDUSTRY_TOOLTIP) {
			pre = "Gamma-level AI core. ";
		}
		if (mode == AICoreDescriptionMode.INDUSTRY_TOOLTIP || mode == AICoreDescriptionMode.MANAGE_CORE_TOOLTIP) {
			CommoditySpecAPI coreSpec = Global.getSettings().getCommoditySpec(aiCoreId);
			TooltipMakerAPI text = tooltip.beginImageWithText(coreSpec.getIconName(), 48);

			text.addPara(pre + "Reduces demand by %s. All modifiers are multiplicative", opad, highlight,
					String.valueOf(Math.round((1f - GAMMA_CORE_INPUT_REDUCTION) * 100f)) + "%");
			tooltip.addImageWithText(opad);
			return;
		}

		tooltip.addPara(pre + "Reduces demand by %s. All modifiers are multiplicative", opad, highlight,
				String.valueOf(Math.round((1f - GAMMA_CORE_INPUT_REDUCTION) * 100f)) + "%");
	}

	protected void addUnknownCoreDescription(String coreId, TooltipMakerAPI tooltip, AICoreDescriptionMode mode) {

		CommoditySpecAPI core = Global.getSettings().getCommoditySpec(coreId);
		if (core == null)
			return;

		float opad = 10f;

		String pre = core.getName() + " currently assigned. ";
		if (mode == AICoreDescriptionMode.MANAGE_CORE_DIALOG_LIST || mode == AICoreDescriptionMode.INDUSTRY_TOOLTIP) {
			pre = core.getName() + ". ";
		}

		if (mode == AICoreDescriptionMode.INDUSTRY_TOOLTIP || mode == AICoreDescriptionMode.MANAGE_CORE_TOOLTIP) {
			TooltipMakerAPI text = tooltip.beginImageWithText(core.getIconName(), 48);
			text.addPara(pre + "No effect.", opad);
			tooltip.addImageWithText(opad);
			return;
		}

		tooltip.addPara(pre + "No effect.", opad);
	}

	protected void addPostSupplySection(TooltipMakerAPI tooltip, boolean hasSupply, IndustryTooltipMode mode) {

	}

	protected void addPostDemandSection(TooltipMakerAPI tooltip, boolean hasDemand, IndustryTooltipMode mode) {

	}

	protected void addRightAfterDescriptionSection(TooltipMakerAPI tooltip, IndustryTooltipMode mode) {

	}

	protected void addPostDescriptionSection(TooltipMakerAPI tooltip, IndustryTooltipMode mode) {

	}

	protected void addPostUpkeepSection(TooltipMakerAPI tooltip, IndustryTooltipMode mode) {

	}

	public String getAICoreId() {
		return aiCoreId;
	}

	public void setAICoreId(String aiCoreId) {
		this.aiCoreId = aiCoreId;
	}

	protected void applyAICoreToIncomeAndUpkeep() {
		if (aiCoreId == null || Commodities.GAMMA_CORE.equals(aiCoreId)) {
			getUpkeep().unmodifyMult("ind_core");
			return;
		}

		if (aiCoreId.equals(Commodities.ALPHA_CORE)) {
			getUpkeep().modifyMult("ind_core", ALPHA_CORE_UPKEEP_REDUCTION_MULT, "Alpha Core assigned");

		} else if (aiCoreId.equals(Commodities.BETA_CORE)) {
			getUpkeep().modifyMult("ind_core", BETA_CORE_UPKEEP_REDUCTION_MULT, "Beta Core assigned");

		}
	}

	protected void updateAICoreToSupplyAndDemandModifiers() {
		if (aiCoreId == null) {
			return;
		}

		if (aiCoreId.equals(Commodities.ALPHA_CORE)) {
			supplyBonus.modifyMult(getModId(0), ALPHA_CORE_PRODUCTION_BOOST, "Alpha core");
			demandReduction.modifyMult(getModId(0), ALPHA_CORE_INPUT_REDUCTION, "Alpha core");
			demandReduction.modifyMult(getModId(7) + "increased_production", ALPHA_CORE_PRODUCTION_BOOST);

		} else if (aiCoreId.equals(Commodities.BETA_CORE)) {
			demandReduction.modifyMult(getModId(0), BETA_CORE_INPUT_REDUCTION, "Beta core");

		} else if (aiCoreId.equals(Commodities.GAMMA_CORE)) {
			demandReduction.modifyMult(getModId(0), GAMMA_CORE_INPUT_REDUCTION, "Gamma core");
		}
	}

	protected void updateSupplyAndDemandModifiers() {

		supplyBonus.unmodify();
		demandReduction.unmodify();

		updateAICoreToSupplyAndDemandModifiers();

		updateImprovementSupplyAndDemandModifiers();

		PersonAPI admin = market.getAdmin();
		if (admin != null && admin.getStats() != null) {

			if (admin.getStats().getDynamic().getValue(Stats.SUPPLY_BONUS_MOD, 0) != 0) {
				supplyBonus.modifyMult(getModId(1), getImproveProductionBonus(), "Administrator");
				demandReduction.modifyMult(getModId(9) + "increased_production", getImproveProductionBonus());
			}

			if (admin.getStats().getDynamic().getValue(Stats.DEMAND_REDUCTION_MOD, 0) != 0) {
				demandReduction.modifyMult(getModId(1), DEFAULT_INPUT_REDUCTION_BONUS, "Administrator");
			}
		}

		if (supplyBonusFromOther != null) {
			supplyBonus.applyMods(supplyBonusFromOther);
			demandReduction.applyMods(supplyBonusFromOther);
		}
		if (demandReductionFromOther != null) {
			demandReduction.applyMods(demandReductionFromOther);
		}
	}

	public boolean showShutDown() {
		return true;
	}

	public boolean canShutDown() {
		return true;
	}

	public String getCanNotShutDownReason() {
		return null;
	}

	public boolean canUpgrade() {
		return true;
	}

	public boolean canDowngrade() {
		return true;
	}

	protected String getDescriptionOverride() {
		return null;
	}

	public String getNameForModifier() {
		return Misc.ucFirst(getCurrentName().toLowerCase());
	}

	public boolean isDemandLegal(CommodityOnMarketAPI com) {
		return !com.isIllegal();
	}

	public boolean isSupplyLegal(CommodityOnMarketAPI com) {
		return !com.isIllegal();
	}

	protected boolean isAICoreId(String str) {
		Set<String> cores = new HashSet<String>();
		cores.add(Commodities.ALPHA_CORE);
		cores.add(Commodities.BETA_CORE);
		cores.add(Commodities.GAMMA_CORE);
		return cores.contains(str);
	}

	public void initWithParams(List<String> params) {
		for (String str : params) {
			if (isAICoreId(str)) {
				setAICoreId(str);
				break;
			}
		}

		for (String str : params) {
			SpecialItemSpecAPI spec = Global.getSettings().getSpecialItemSpec(str);
			if (spec == null)
				continue;

			String[] industries = spec.getParams().split(",");
			Set<String> all = new HashSet<String>();
			for (String ind : industries)
				all.add(ind.trim());
			if (all.contains(getId())) {
				setSpecialItem(new SpecialItemData(str, null));
			}
		}
	}

	protected boolean hasPostDemandSection(boolean hasDemand, IndustryTooltipMode mode) {
		return false;
	}

	protected int getBaseStabilityMod() {
		return 0;
	}

	protected void modifyStabilityWithBaseMod() {
		int stabilityMod = getBaseStabilityMod();
		int stabilityPenalty = getStabilityPenalty();
		if (stabilityPenalty > stabilityMod) {
			stabilityPenalty = stabilityMod;
		}
		stabilityMod -= stabilityPenalty;
		if (stabilityMod > 0) {
			market.getStability().modifyFlat(getModId(), stabilityMod, getNameForModifier());
		}
	}

	protected void unmodifyStabilityWithBaseMod() {
		market.getStability().unmodifyFlat(getModId());
	}

	protected Pair<String, Integer> getStabilityAffectingDeficit() {
		return new Pair<String, Integer>(Commodities.SUPPLIES, 0);
	}

	protected int getStabilityPenalty() {
		float deficit = getStabilityAffectingDeficit().two;
		if (deficit < 0)
			deficit = 0;
		return (int) Math.round(deficit);
	}

	protected void addStabilityPostDemandSection(TooltipMakerAPI tooltip, boolean hasDemand, IndustryTooltipMode mode) {
		Color h = Misc.getHighlightColor();
		float opad = 10f;

		MutableStat fake = new MutableStat(0);
		int stabilityMod = getBaseStabilityMod();
		int stabilityPenalty = getStabilityPenalty();

		if (stabilityPenalty > stabilityMod) {
			stabilityPenalty = stabilityMod;
		}

		String str = getDeficitText(getStabilityAffectingDeficit().one);
		fake.modifyFlat("1", stabilityMod, getNameForModifier());
		if (stabilityPenalty != 0) {
			fake.modifyFlat("2", -stabilityPenalty, str);
		}

		int total = stabilityMod - stabilityPenalty;
		String totalStr = "+" + total;
		if (total < 0) {
			totalStr = "" + total;
			h = Misc.getNegativeHighlightColor();
		}
		float pad = 3f;
		if (total >= 0) {
			tooltip.addPara("Stability bonus: %s", opad, h, totalStr);
		} else {
			tooltip.addPara("Stability penalty: %s", opad, h, totalStr);
		}
		tooltip.addStatModGrid(400, 35, opad, pad, fake, new StatModValueGetter() {
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

	public void setHidden(boolean hidden) {
		if (hidden)
			hiddenOverride = true;
		else
			hiddenOverride = null;
	}

	protected Boolean hiddenOverride = null;

	public boolean isHidden() {
		if (hiddenOverride != null)
			return hiddenOverride;
		return false;
	}

	protected transient String dKey = null;

	public String getDisruptedKey() {
		if (dKey != null)
			return dKey;
		dKey = "$core_disrupted_" + getClass().getSimpleName();
		return dKey;
	}

	public void setDisrupted(float days) {
		setDisrupted(days, false);
	}

	public void setDisrupted(float days, boolean useMax) {
		if (!canBeDisrupted())
			return;

		boolean was = isDisrupted();
		String key = getDisruptedKey();

		MemoryAPI memory = market.getMemoryWithoutUpdate();
		float dur = days;
		if (useMax) {
			dur = Math.max(memory.getExpire(key), dur);
		}

		if (dur <= 0) {
			memory.unset(key);
		} else {
			memory.set(key, true, dur);
		}

		if (!was) {
			notifyDisrupted();
		}
	}

	public float getDisruptedDays() {
		String key = getDisruptedKey();
		float dur = market.getMemoryWithoutUpdate().getExpire(key);
		if (dur < 0)
			dur = 0;
		return dur;
	}

	public boolean canBeDisrupted() {
		return true;
	}

	public boolean isDisrupted() {
		String key = getDisruptedKey();
		return market.getMemoryWithoutUpdate().is(key, true);
	}

	public float getPatherInterest() {
		float interest = 0;
		if (Commodities.ALPHA_CORE.equals(aiCoreId)) {
			interest += 4f;
		} else if (Commodities.BETA_CORE.equals(aiCoreId)) {
			interest += 2f;
		} else if (Commodities.GAMMA_CORE.equals(aiCoreId)) {
			interest += 1f;
		}

		if (special != null) {
			SpecialItemSpecAPI spec = Global.getSettings().getSpecialItemSpec(special.getId());
			if (spec != null) {
				if (spec.hasTag(Items.TAG_PATHER1))
					interest += 1;
				else if (spec.hasTag(Items.TAG_PATHER2))
					interest += 2;
				else if (spec.hasTag(Items.TAG_PATHER4))
					interest += 4;
				else if (spec.hasTag(Items.TAG_PATHER6))
					interest += 6;
				else if (spec.hasTag(Items.TAG_PATHER8))
					interest += 8;
				else if (spec.hasTag(Items.TAG_PATHER10))
					interest += 10;
			}
		}

		return interest;
	}

	public CargoAPI generateCargoForGatheringPoint(Random random) {
		return null;
	}

	public String getCargoTitleForGatheringPoint() {
		return getCurrentName();
	}

	protected SpecialItemData special = null;

	public SpecialItemData getSpecialItem() {
		return special;
	}

	public void setSpecialItem(SpecialItemData special) {
		if (this.special != null) {
			InstallableItemEffect effect = LtvItemEffectsRepo.ITEM_EFFECTS.get(this.special.getId());
			if (effect != null) {
				effect.unapply(this);
			}
		}
		this.special = special;
	}

	protected float getDeficitMult(String... commodities) {
		float deficit = getMaxDeficit(commodities).two;
		float demand = 0f;

		for (String id : commodities) {
			demand = Math.max(demand, getDemand(id).getQuantity().getModifiedInt());
		}

		if (deficit < 0)
			deficit = 0f;
		if (demand < 1) {
			demand = 1;
			deficit = 0f;
		}

		float mult = (demand - deficit) / demand;
		if (mult < 0)
			mult = 0;
		if (mult > 1)
			mult = 1;
		return mult;
	}

	protected void addGroundDefensesImpactSection(TooltipMakerAPI tooltip, float bonus, String... commodities) {
		Color h = Misc.getHighlightColor();
		float opad = 10f;

		MutableStat fake = new MutableStat(1);

		fake.modifyFlat("1", bonus, getNameForModifier());

		if (commodities != null) {
			float mult = getDeficitMult(commodities);
			// mult = 0.89f;
			if (mult != 1) {
				String com = getMaxDeficit(commodities).one;
				fake.modifyFlat("2", -(1f - mult) * bonus, getDeficitText(com));
			}
		}

		float total = Misc.getRoundedValueFloat(fake.getModifiedValue());
		String totalStr = Strings.X + total;
		if (total < 1f) {
			h = Misc.getNegativeHighlightColor();
		}
		float pad = 3f;
		tooltip.addPara("Ground defense strength: %s", opad, h, totalStr);
		tooltip.addStatModGrid(400, 35, opad, pad, fake, new StatModValueGetter() {
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
				String r = Misc.getRoundedValue(mod.value);
				if (mod.value >= 0)
					return "+" + r;
				return r;
			}
		});
	}

	public boolean isIndustry() {
		return getSpec().hasTag(Industries.TAG_INDUSTRY);
	}

	public boolean isStructure() {
		return getSpec().hasTag(Industries.TAG_STRUCTURE);
	}

	public boolean isOther() {
		return !isIndustry() && !isStructure();
	}

	public void notifyColonyRenamed() {

	}

	public boolean canImprove() {
		return canImproveToIncreaseProduction();
	}

	public float getImproveBonusXP() {
		return 0;
	}

	public String getImproveMenuText() {
		return "Make improvements...";
	}

	public int getImproveStoryPoints() {
		int base = Global.getSettings().getInt("industryImproveBase");
		return base * (int) Math.round(Math.pow(2, Misc.getNumImprovedIndustries(market)));
	}

	public boolean isImproved() {
		return improved != null && improved;
	}

	public void setImproved(boolean improved) {
		if (!improved) {
			this.improved = null;
		} else {
			this.improved = improved;
		}
	}

	protected void applyImproveModifiers() {
	}

	public void addImproveDesc(TooltipMakerAPI info, ImprovementDescriptionMode mode) {
		float initPad = 0f;
		float opad = 10f;
		boolean addedSomething = false;
		if (canImproveToIncreaseProduction()) {
			if (mode == ImprovementDescriptionMode.INDUSTRY_TOOLTIP) {
				info.addPara("Production increased by %s.", initPad, Misc.getHighlightColor(), Strings.X + getImproveProductionBonus());
			} else {
				info.addPara("Increases production by %s.", initPad, Misc.getHighlightColor(), Strings.X + getImproveProductionBonus());
			}
			initPad = opad;
			addedSomething = true;
		}

		if (mode != ImprovementDescriptionMode.INDUSTRY_TOOLTIP) {

			info.addPara("Each improvement made at a colony doubles the number of " +
					"" + Misc.STORY + " points required to make an additional improvement.", initPad,
					Misc.getStoryOptionColor(), Misc.STORY + " points");
			addedSomething = true;
		}
		if (!addedSomething) {
			info.addSpacer(-opad);
		}
	}

	public String getImproveDialogTitle() {
		return "Improving " + getSpec().getName();
	}

	public String getImproveSoundId() {
		return Sounds.STORY_POINT_SPEND_INDUSTRY;
	}

	protected boolean canImproveToIncreaseProduction() {
		return false;
	}

	protected float getImproveProductionBonus() {
		return DEFAULT_IMPROVE_PRODUCTION_BONUS;
	}

	protected String getImprovementsDescForModifiers() {
		return "Improvements";
	}

	protected void updateImprovementSupplyAndDemandModifiers() {
		if (!canImproveToIncreaseProduction())
			return;
		if (!isImproved())
			return;

		if (getImproveProductionBonus() <= 1f)
			return;

		supplyBonus.modifyMult(getModId(3), getImproveProductionBonus(), getImprovementsDescForModifiers());
		demandReduction.modifyMult(getModId(8) + "increased_production", getImproveProductionBonus());
	}

	public void addImprovedSection(IndustryTooltipMode mode, TooltipMakerAPI tooltip, boolean expanded) {

		if (!isImproved())
			return;

		float opad = 10f;

		tooltip.addSectionHeading("Improvements made", Misc.getStoryOptionColor(),
				Misc.getStoryDarkColor(), Alignment.MID, opad);

		tooltip.addSpacer(opad);
		addImproveDesc(tooltip, ImprovementDescriptionMode.INDUSTRY_TOOLTIP);
	}

	public RaidDangerLevel adjustCommodityDangerLevel(String commodityId, RaidDangerLevel level) {
		return level;
	}

	public RaidDangerLevel adjustItemDangerLevel(String itemId, String data, RaidDangerLevel level) {
		return level;
	}

	public int adjustMarineTokensToRaidItem(String itemId, String data, int marineTokens) {
		return marineTokens;
	}

	public boolean canInstallAICores() {
		return true;
	}

	protected transient Boolean hasInstallableItems = null;

	public List<InstallableIndustryItemPlugin> getInstallableItems() {
		boolean found = false;
		if (hasInstallableItems != null) {
			found = hasInstallableItems;
		} else {
			OUTER: for (SpecialItemSpecAPI spec : Global.getSettings().getAllSpecialItemSpecs()) {
				if (spec.getParams() == null || spec.getParams().isEmpty())
					continue;
				if (spec.getNewPluginInstance(null) instanceof GenericSpecialItemPlugin) {
					for (String id : spec.getParams().split(",")) {
						id = id.trim();
						if (id.equals(getId())) {
							found = true;
							break OUTER;
						}
					}
				}
			}
			hasInstallableItems = found;
		}
		ArrayList<InstallableIndustryItemPlugin> list = new ArrayList<InstallableIndustryItemPlugin>();
		if (found) {
			list.add(new LtvGenericInstallableItemPlugin(this));
		}
		return list;
	}

	public float getBuildProgress() {
		return buildProgress;
	}

	public void setBuildProgress(float buildProgress) {
		this.buildProgress = buildProgress;
	}
}
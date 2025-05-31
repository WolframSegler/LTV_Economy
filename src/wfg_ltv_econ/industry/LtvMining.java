package wfg_ltv_econ.industry;

import java.awt.Color;
import java.util.List;
import java.util.Map;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.PlanetAPI;
import com.fs.starfarer.api.campaign.SpecialItemData;
import com.fs.starfarer.api.campaign.econ.MarketConditionAPI;
import com.fs.starfarer.api.impl.campaign.econ.ResourceDepositsCondition;
import com.fs.starfarer.api.impl.campaign.ids.Commodities;
import com.fs.starfarer.api.impl.campaign.ids.Items;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Pair;
import com.fs.starfarer.api.impl.campaign.ids.Conditions;


public class LtvMining extends LtvBaseIndustry {

	public final static float MINING_NONE = 0;
	public final static float MINING_SPARSE = 0.5f;
	public final static float MINING_ABUNDANT = 1.5f;
	public final static float MINING_RICH = 2f;
	public final static float MINING_ULTRARICH = 3f;

    public final static float DAILY_BASE_PROD_ORE = 2;
    public final static float DAILY_BASE_PROD_RARE_ORE = 0.4f;
    public final static float DAILY_BASE_PROD_ORGANICS = 0.9f;
    public final static float DAILY_BASE_PROD_VOLATILES = 0.1f;

    public final static float HEAVY_MACHINERY_WEIGHT_FOR_MINING = 0.3f;
    public final static float DRUGS_WEIGHT_FOR_MINING = 0.4f;

	protected static Map<String, List<Pair<String, Float>>> COMMODITY_LIST;

    static {
		COMMODITY_LIST = Map.of(
			Commodities.ORE, List.of(
					new Pair<>(Commodities.HEAVY_MACHINERY, HEAVY_MACHINERY_WEIGHT_FOR_MINING),
					new Pair<>(Commodities.DRUGS, DRUGS_WEIGHT_FOR_MINING)
					),
			Commodities.RARE_ORE, List.of(
					new Pair<>(Commodities.HEAVY_MACHINERY, HEAVY_MACHINERY_WEIGHT_FOR_MINING),
					new Pair<>(Commodities.DRUGS, DRUGS_WEIGHT_FOR_MINING)
					),
			Commodities.ORGANICS, List.of(
					new Pair<>(Commodities.HEAVY_MACHINERY, HEAVY_MACHINERY_WEIGHT_FOR_MINING),
					new Pair<>(Commodities.DRUGS, DRUGS_WEIGHT_FOR_MINING)
					),
            Commodities.VOLATILES, List.of(
					new Pair<>(Commodities.HEAVY_MACHINERY, HEAVY_MACHINERY_WEIGHT_FOR_MINING),
					new Pair<>(Commodities.DRUGS, DRUGS_WEIGHT_FOR_MINING)
					)
				);
	}

	public void apply() {
		super.apply(true);

        demand(Commodities.HEAVY_MACHINERY, Math.round(ltv_precalculateconsumption(
				DAILY_BASE_PROD_ORE * HEAVY_MACHINERY_WEIGHT_FOR_MINING,
				DAILY_BASE_PROD_RARE_ORE * HEAVY_MACHINERY_WEIGHT_FOR_MINING,
				DAILY_BASE_PROD_ORGANICS * HEAVY_MACHINERY_WEIGHT_FOR_MINING,
				DAILY_BASE_PROD_VOLATILES * HEAVY_MACHINERY_WEIGHT_FOR_MINING)));

        demand(Commodities.DRUGS, Math.round(ltv_precalculateconsumption(
				DAILY_BASE_PROD_ORE * DRUGS_WEIGHT_FOR_MINING,
				DAILY_BASE_PROD_RARE_ORE * DRUGS_WEIGHT_FOR_MINING,
				DAILY_BASE_PROD_ORGANICS * DRUGS_WEIGHT_FOR_MINING,
				DAILY_BASE_PROD_VOLATILES * DRUGS_WEIGHT_FOR_MINING)));

        supply(Commodities.ORE, (int) (DAILY_BASE_PROD_ORE*workersAssigned));
        supply(Commodities.RARE_ORE, (int) (DAILY_BASE_PROD_RARE_ORE*workersAssigned));
        supply(Commodities.ORGANICS, (int) (DAILY_BASE_PROD_ORGANICS*workersAssigned));
        supply(Commodities.VOLATILES, (int) (DAILY_BASE_PROD_VOLATILES*workersAssigned));

		applyConditionModifiers();
		
		if (!isFunctional()) {
			supply.clear();
		}
	}

	protected void applyConditionModifiers() {
		for(MarketConditionAPI condition : market.getConditions()) {
			String id = condition.getId();

			Global.getLogger(getClass()).error("id of condition: " + id);

			if (id.startsWith(Commodities.ORE)) {
				switch (id) {
            case Conditions.ORE_SPARSE:
                applyCommodityModifier(Commodities.ORE, id, MINING_SPARSE, "Sparse ore deposits");
                break;

			case Conditions.ORE_MODERATE:
				applyCommodityModifier(Commodities.ORE, id, 1, "Moderate ore deposits");
                break;

			case Conditions.ORE_ABUNDANT:
				applyCommodityModifier(Commodities.ORE, id, MINING_ABUNDANT, "Abundant ore deposits");
                break;

			case Conditions.ORE_RICH:
                applyCommodityModifier(Commodities.ORE, id, MINING_RICH, "Rich ore deposits");
                break;

			case Conditions.ORE_ULTRARICH:
                applyCommodityModifier(Commodities.ORE, id, MINING_ULTRARICH, "Ultrarich ore deposits");
                break;

            default:
                applyCommodityModifier(Commodities.ORE, id, MINING_NONE, "No ore deposits");
                break;
        	}
			continue;
			}

			if (id.startsWith(Commodities.RARE_ORE)) {
				switch (id) {
            case Conditions.RARE_ORE_SPARSE:
                applyCommodityModifier(Commodities.RARE_ORE, id, MINING_SPARSE, "Sparse rare ore deposits");
                break;
			
			case Conditions.RARE_ORE_MODERATE:
                applyCommodityModifier(Commodities.RARE_ORE, id, 1, "Moderate rare ore deposits");
                break;

			case Conditions.RARE_ORE_ABUNDANT:
				applyCommodityModifier(Commodities.RARE_ORE, id, MINING_ABUNDANT, "Abundant rare ore deposits");
                break;

			case Conditions.RARE_ORE_RICH:
                applyCommodityModifier(Commodities.RARE_ORE, id, MINING_RICH, "Rich rare ore deposits");
                break;

			case Conditions.RARE_ORE_ULTRARICH:
                applyCommodityModifier(Commodities.RARE_ORE, id, MINING_ULTRARICH, "Ultrarich rare ore deposits");
                break;

            default:
                applyCommodityModifier(Commodities.RARE_ORE, id, MINING_NONE, "No rare ore deposits");
                break;
        	}
			continue;
			}

			if (id.startsWith(Commodities.ORGANICS)) {
				switch (id) {
            case Conditions.ORGANICS_TRACE:
                applyCommodityModifier(Commodities.ORGANICS, id, MINING_SPARSE, "Sparse organics deposits");
                break;

			case Conditions.ORGANICS_COMMON:
                applyCommodityModifier(Commodities.ORGANICS, id, 1, "Common organics deposits");
                break;

			case Conditions.ORGANICS_ABUNDANT:
				applyCommodityModifier(Commodities.ORGANICS, id, MINING_ABUNDANT, "Abundant organics deposits");
                break;

			case Conditions.ORGANICS_PLENTIFUL:
                applyCommodityModifier(Commodities.ORGANICS, id, MINING_RICH, "Rich organics deposits");
                break;

            default:
                applyCommodityModifier(Commodities.ORGANICS, id, MINING_NONE, "No organics deposits");
                break;
        	}
			continue;
			}

			if (id.startsWith(Commodities.VOLATILES)) {
				switch (id) {
            case Conditions.VOLATILES_TRACE:
                applyCommodityModifier(Commodities.VOLATILES, id, 1, "Sparse volatiles deposits");
                break;

			case Conditions.VOLATILES_DIFFUSE:
				applyCommodityModifier(Commodities.VOLATILES, id, 1, "Diffuse volatiles deposits");
                break;

			case Conditions.VOLATILES_ABUNDANT:
				applyCommodityModifier(Commodities.VOLATILES, id, MINING_ABUNDANT, "Abundant volatiles deposits");
                break;

			case Conditions.VOLATILES_PLENTIFUL:
                applyCommodityModifier(Commodities.VOLATILES, id, MINING_RICH, "Rich volatiles deposits");
                break;

            default:
                applyCommodityModifier(Commodities.VOLATILES, id, MINING_NONE, "No volatiles deposits");
                break;
        	}
			continue;
			}
		}
	}

	private void applyCommodityModifier(String commodity, String id, float multiplier, String description) {
		// Remove vanilla modifiers
    	getSupply(commodity).getQuantity().unmodifyFlat(id + "_0");
    	getSupply(commodity).getQuantity().unmodifyFlat(id + "_1");

		// Custom modifiers
    	getSupply(commodity).getQuantity().modifyMult(id + "_ltv_" + commodity, multiplier, description);
	}

	@Override
	public void unapply() {
		super.unapply();
	}

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
		}
	}

	protected boolean hasPostDemandSection(boolean hasDemand, IndustryTooltipMode mode) {
		Pair<String, Integer> deficit = getMaxDeficit(Commodities.DRUGS);
		if (deficit.two <= 0) return false;
		return mode != IndustryTooltipMode.NORMAL || isFunctional();
	}
	
	@Override
	protected void addPostDemandSection(TooltipMakerAPI tooltip, boolean hasDemand, IndustryTooltipMode mode) {
		if (mode != IndustryTooltipMode.NORMAL || isFunctional()) {			
			// Reduce worker productivity instead of reducing immigration
		}
	}
	

	@Override
	public boolean isAvailableToBuild() {
		if (!super.isAvailableToBuild()) return false;
		
		for (MarketConditionAPI mc : market.getConditions()) {
			String commodity = ResourceDepositsCondition.COMMODITY.get(mc.getId());
			if (commodity != null) {
				String industry = ResourceDepositsCondition.INDUSTRY.get(commodity);
				if (getId().equals(industry)) return true;
			}
		}
		return false;
	}

	@Override
	public String getUnavailableReason() {
		if (!super.isAvailableToBuild()) return super.getUnavailableReason();
		
		return "Requires resource deposits";
	}
	
	@Override
	public String getCurrentImage() {
		float size = market.getSize();
		if (market.getPlanetEntity() != null && market.getPlanetEntity().isGasGiant()) {
			return Global.getSettings().getSpriteName("industry", "mining_gas_giant");
		}
		if (size <= SIZE_FOR_SMALL_IMAGE) {
			return Global.getSettings().getSpriteName("industry", "mining_low");
		}
		return super.getCurrentImage();
	}
	
	public float getPatherInterest() {
		return 1f + super.getPatherInterest();
	}
	
	@Override
	protected boolean canImproveToIncreaseProduction() {
		return true;
	}
	
		
	public void applyVisuals(PlanetAPI planet) {
		if (planet == null) return;
		planet.getSpec().setShieldTexture2(Global.getSettings().getSpriteName("industry", "plasma_net_texture"));
		planet.getSpec().setShieldThickness2(0.15f);
		planet.getSpec().setShieldColor2(new Color(255,255,255,255));
		planet.applySpecChanges();
		shownPlasmaNetVisuals = true;
	}
	
	public void unapplyVisuals(PlanetAPI planet) {
		if (planet == null) return;
		planet.getSpec().setShieldTexture2(null);
		planet.getSpec().setShieldThickness2(0f);
		planet.getSpec().setShieldColor2(null);
		planet.applySpecChanges();
		shownPlasmaNetVisuals = false;
	}

	protected boolean shownPlasmaNetVisuals = false;
	
	@Override
	public void setSpecialItem(SpecialItemData special) {
		super.setSpecialItem(special);

		if (shownPlasmaNetVisuals && (special == null || !special.getId().equals(Items.PLASMA_DYNAMO))) {
			unapplyVisuals(market.getPlanetEntity());
		}
		
		if (special != null && special.getId().equals(Items.PLASMA_DYNAMO)) {
			applyVisuals(market.getPlanetEntity());
		}
	}
}
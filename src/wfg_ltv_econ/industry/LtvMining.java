package wfg_ltv_econ.industry;

import java.awt.Color;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.PlanetAPI;
import com.fs.starfarer.api.campaign.SpecialItemData;
import com.fs.starfarer.api.campaign.econ.MarketConditionAPI;
import com.fs.starfarer.api.combat.MutableStat;
import com.fs.starfarer.api.impl.campaign.econ.ResourceDepositsCondition;
import com.fs.starfarer.api.impl.campaign.ids.Commodities;
import com.fs.starfarer.api.impl.campaign.ids.Items;
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

    public final static float HEAVY_MACHINERY_WEIGHT_FOR_MINING = 0.4f;
    public final static float DRUGS_WEIGHT_FOR_MINING = 0.4f;

	protected static Map<String, List<Pair<String, Float>>> COMMODITY_LIST;
	protected static Map<String, Float> MINING_RESOURCES;
	public float demandCostHeavyMachinery = 0;
	public float demandCostDrugs = 0;

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

		MINING_RESOURCES = Map.of(
			Commodities.ORE, DAILY_BASE_PROD_ORE,
			Commodities.RARE_ORE, DAILY_BASE_PROD_RARE_ORE,
			Commodities.ORGANICS, DAILY_BASE_PROD_ORGANICS,
            Commodities.VOLATILES, DAILY_BASE_PROD_VOLATILES
		);
	}

	public void apply() {
		super.apply(true);

		// applyConditionModifiers();

		// // Supply before demand, because demand depends on supply in this case
		// demand(Commodities.HEAVY_MACHINERY, Math.round(demandCostHeavyMachinery));
        // demand(Commodities.DRUGS, Math.round(demandCostDrugs));
		
		if (!isFunctional()) {
			supply.clear();
		}
	}

	private void applyConditionModifiers() {
		for(MarketConditionAPI condition : market.getConditions()) {
			String id = condition.getId();

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
		MutableStat commodityStat = getSupply(commodity).getQuantity();
		for (String modId : new ArrayList<>(commodityStat.getFlatMods().keySet())) {
    		if (modId.startsWith(id)) {
        		commodityStat.unmodifyFlat(modId);
    		}
		}

		// Ltv modifiers
		if (multiplier == 0) {
			return;
		}
		supply(commodity, (int) (MINING_RESOURCES.get(commodity)*getWorkersAssigned()));
    	getSupply(commodity).getQuantity().modifyMult(id + "_ltv_" + commodity, multiplier, description);

		demandCostHeavyMachinery += MINING_RESOURCES.get(commodity)*HEAVY_MACHINERY_WEIGHT_FOR_MINING*getWorkersAssigned();
		demandCostDrugs += MINING_RESOURCES.get(commodity)*DRUGS_WEIGHT_FOR_MINING*getWorkersAssigned();
	}

	@Override
	public void unapply() {
		super.unapply();

		demandCostHeavyMachinery = 0;
		demandCostDrugs = 0;
	}

    protected int dayTracker = -1;
    @Override
	public void advance(float amount) {
		int day = Global.getSector().getClock().getDay();
		super.advance(day);

		if (dayTracker != day) {

			dayTracker = day; // Do this at the end of the advance() method
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
	public boolean isWorkerAssignable() {
		return true;
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
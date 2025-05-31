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


public class LtvMining extends LtvBaseIndustry {

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
		
		if (!isFunctional()) {
			supply.clear();
		}
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
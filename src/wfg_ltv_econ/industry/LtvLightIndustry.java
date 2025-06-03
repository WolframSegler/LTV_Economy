package wfg_ltv_econ.industry;

import java.util.List;
import java.util.Map;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.PlanetAPI;
import com.fs.starfarer.api.impl.campaign.ids.Commodities;
import com.fs.starfarer.api.util.Pair;

public class LtvLightIndustry extends LtvBaseIndustry {

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
		
		int size = market.getSize();
		
		
		
		supply(Commodities.DOMESTIC_GOODS, size);
		if (!market.isIllegal(Commodities.LUXURY_GOODS)) {
			supply(Commodities.LUXURY_GOODS, size - 2);
		}
		if (!market.isIllegal(Commodities.DRUGS)) {
			supply(Commodities.DRUGS, size - 2);
		}
        
        demand(Commodities.ORGANICS, size);
		
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
	
	@Override
	public String getCurrentImage() {
		float size = market.getSize();
		PlanetAPI planet = market.getPlanetEntity();
		if (planet == null || planet.isGasGiant()) {
			if (size <= SIZE_FOR_SMALL_IMAGE) {
				return Global.getSettings().getSpriteName("industry", "light_industry_orbital_low");
			}
			if (size >= SIZE_FOR_LARGE_IMAGE) {
				return Global.getSettings().getSpriteName("industry", "light_industry_orbital_high");
			}
			return Global.getSettings().getSpriteName("industry", "light_industry_orbital");
		}
		else
		{
			if (size <= SIZE_FOR_SMALL_IMAGE) {
				return Global.getSettings().getSpriteName("industry", "light_industry_low");
			}
			if (size >= SIZE_FOR_LARGE_IMAGE) {
				return Global.getSettings().getSpriteName("industry", "light_industry_high");
			}
		}
		
		return super.getCurrentImage();
	}

	@Override
	protected boolean canImproveToIncreaseProduction() {
		return true;
	}
}

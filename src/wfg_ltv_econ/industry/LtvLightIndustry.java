package wfg_ltv_econ.industry;

import java.util.List;
import java.util.Map;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.PlanetAPI;
import com.fs.starfarer.api.impl.campaign.ids.Commodities;
import com.fs.starfarer.api.util.Pair;

public class LtvLightIndustry extends LtvBaseIndustry {

	public static final float DAILY_BASE_PROD_DOMESTIC_GOODS = 0.4f;
	public static final float DAILY_BASE_PROD_LUXURY_GOODS = 0.2f;
	public static final float DAILY_BASE_PROD_DRUGS = 0.1f;

	public static final float ORGANICS_WEIGHT_FOR_LIGHT_INDUSTRY = 1;
    protected static Map<String, List<Pair<String, Float>>> COMMODITY_LIST;
	public float demandCostOrganics = 0;

    static {
		COMMODITY_LIST = Map.of(
			Commodities.DOMESTIC_GOODS, List.of(
					new Pair<>(Commodities.ORGANICS, ORGANICS_WEIGHT_FOR_LIGHT_INDUSTRY)
					),
			Commodities.LUXURY_GOODS, List.of(
					new Pair<>(Commodities.ORGANICS, ORGANICS_WEIGHT_FOR_LIGHT_INDUSTRY)
					),
			Commodities.DRUGS, List.of(
					new Pair<>(Commodities.ORGANICS, ORGANICS_WEIGHT_FOR_LIGHT_INDUSTRY)
					)
		);
	}

	public void apply() {
		super.apply(true);
		
		supply(Commodities.DOMESTIC_GOODS, (int) (DAILY_BASE_PROD_DOMESTIC_GOODS*getWorkerAssigned()));
		demandCostOrganics += DAILY_BASE_PROD_DOMESTIC_GOODS*getWorkerAssigned();
		if (!market.isIllegal(Commodities.LUXURY_GOODS)) {
			supply(Commodities.LUXURY_GOODS, (int) (DAILY_BASE_PROD_LUXURY_GOODS*getWorkerAssigned()));
			demandCostOrganics += DAILY_BASE_PROD_LUXURY_GOODS*getWorkerAssigned();
		}
		if (!market.isIllegal(Commodities.DRUGS)) {
			supply(Commodities.DRUGS, (int) (DAILY_BASE_PROD_DRUGS*getWorkerAssigned()));
			demandCostOrganics += DAILY_BASE_PROD_DRUGS*getWorkerAssigned();
		}
        
        demand(Commodities.ORGANICS, (int)demandCostOrganics);
		
		if (!isFunctional()) {
			supply.clear();
		}
	}

	@Override
	public void unapply() {
		super.unapply();
		demandCostOrganics = 0;
	}

    @Override
	public void advance(float amount) {
		int day = Global.getSector().getClock().getDay();
		super.advance(day);

		if (dayTracker != day) { //Production

			ltv_WeightedDeficitModifiers(COMMODITY_LIST);

			//All the consumption is done by Population and Infrastructure

			ltv_produce(COMMODITY_LIST);

			dayTracker = day; // Do this at the end of the advance() method
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

	@Override
	public boolean isWorkerAssignable() {
		return true;
	}
}

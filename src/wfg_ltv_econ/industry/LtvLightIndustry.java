package wfg_ltv_econ.industry;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.PlanetAPI;

public class LtvLightIndustry extends LtvBaseIndustry {

	public void apply() {
		super.apply(true);
		
		if (!isFunctional()) {
			supply.clear();
		}
	}

	@Override
	public void unapply() {
		super.unapply();
	}

    @Override
	public void advance(float amount) {
		int day = Global.getSector().getClock().getDay();
		super.advance(day);

		if (dayTracker != day) {

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

package wfg.ltv_econ.industry;

import java.awt.Color;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.PlanetAPI;
import com.fs.starfarer.api.campaign.SpecialItemData;
import com.fs.starfarer.api.campaign.econ.MarketConditionAPI;
import com.fs.starfarer.api.impl.campaign.econ.ResourceDepositsCondition;
import com.fs.starfarer.api.impl.campaign.econ.impl.BaseIndustry;
import com.fs.starfarer.api.impl.campaign.ids.Items;


public class LtvMining extends BaseIndustry {

	public final static float MINING_NONE = 0;
	public final static float MINING_SPARSE = 0.5f;
	public final static float MINING_ABUNDANT = 1.5f;
	public final static float MINING_RICH = 2f;
	public final static float MINING_ULTRARICH = 3f;

    public final static float DAILY_BASE_PROD_ORE = 2;
    public final static float DAILY_BASE_PROD_RARE_ORE = 0.4f;
    public final static float DAILY_BASE_PROD_ORGANICS = 0.9f;
    public final static float DAILY_BASE_PROD_VOLATILES = 0.1f;

	public void apply() {
		super.apply(true);
		
		if (!isFunctional()) {
			supply.clear();
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
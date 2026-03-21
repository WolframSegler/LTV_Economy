package wfg.ltv_econ.industry;

import java.util.Collections;
import java.util.List;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CargoStackAPI;
import com.fs.starfarer.api.campaign.econ.InstallableIndustryItemPlugin;
import com.fs.starfarer.api.impl.campaign.econ.impl.BaseIndustry;
import com.fs.starfarer.api.impl.campaign.econ.impl.GenericInstallableItemPlugin;
import com.fs.starfarer.api.impl.campaign.econ.impl.HeavyIndustry;
import com.fs.starfarer.api.impl.campaign.econ.impl.ItemEffectsRepo;
import com.fs.starfarer.api.impl.campaign.ids.Commodities;
import com.fs.starfarer.api.impl.campaign.ids.Conditions;

public class Manufacturing extends BaseIndustry {
    private static final String NANO = "nanoforge";

    public void apply() {
        super.apply(true);

        supply(wfg.ltv_econ.constants.CommoditiesID.LIGHT_COMPONENTS, 1);
        supply(wfg.ltv_econ.constants.CommoditiesID.PRECISION_COMPONENTS, 1);
        supply(wfg.ltv_econ.constants.CommoditiesID.STRUCTURAL_COMPONENTS, 1);
        supply(wfg.ltv_econ.constants.CommoditiesID.SUBASSEMBLY_COMPONENTS, 1);

        demand(Commodities.METALS, 1);
        demand(Commodities.RARE_METALS, 1);
        demand(Commodities.HEAVY_MACHINERY, 1);
        demand(wfg.ltv_econ.constants.CommoditiesID.LIGHT_MACHINERY, 1);
        demand(Commodities.ORGANICS, 1);
    }

    @Override
    public List<InstallableIndustryItemPlugin> getInstallableItems() {
        return Collections.singletonList(new GenericInstallableItemPlugin(this) {
            @Override
            public boolean isInstallableItem(CargoStackAPI stack) {
                return stack.isSpecialStack() &&
                    stack.getPlugin().getSpec().getTags().contains(NANO) && 
                    ItemEffectsRepo.ITEM_EFFECTS.containsKey(stack.getSpecialDataIfSpecial().getId());
            }
        });
    }

    @Override
    protected boolean canImproveToIncreaseProduction() {
        return true;
    }

    private boolean permaPollution = false;
	private boolean addedPollution = false;
	private float daysWithNanoforge = 0f;
	
	@Override
	public void advance(float amount) {
		super.advance(amount);
		
		if (special != null) {
			float days = Global.getSector().getClock().convertToDays(amount);
			daysWithNanoforge += days;
	
			updatePollutionStatus();
		}
	}
	
	private final void updatePollutionStatus() {
		if (!market.hasCondition(Conditions.HABITABLE)) return;
		
		if (special != null) {
			if (!addedPollution && daysWithNanoforge >= HeavyIndustry.DAYS_BEFORE_POLLUTION) {
				if (market.hasCondition(HeavyIndustry.POLLUTION_ID)) {
					permaPollution = true;
				} else {
					market.addCondition(HeavyIndustry.POLLUTION_ID);
					addedPollution = true;
				}
			}
			if (addedPollution && !permaPollution) {
				if (daysWithNanoforge > HeavyIndustry.DAYS_BEFORE_POLLUTION_PERMANENT) {
					permaPollution = true;
				}
			}
		} else if (addedPollution && !permaPollution) {
			market.removeCondition(HeavyIndustry.POLLUTION_ID);
			addedPollution = false;
		}
	}
}
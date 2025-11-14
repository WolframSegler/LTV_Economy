package wfg.ltv_econ.industry;

import com.fs.starfarer.api.impl.campaign.econ.impl.BaseIndustry;

import com.fs.starfarer.api.impl.campaign.ids.Commodities;

public class Manufacturing extends BaseIndustry {
    public void apply() {
        super.apply(true);

        supply(wfg.ltv_econ.constants.Commodities.LIGHT_COMPONENTS, 1);
        supply(wfg.ltv_econ.constants.Commodities.PRECISION_COMPONENTS, 1);
        supply(wfg.ltv_econ.constants.Commodities.STRUCTURAL_COMPONENTS, 1);
        supply(wfg.ltv_econ.constants.Commodities.SUBASSEMBLY_COMPONENTS, 1);

        demand(Commodities.METALS, 1);
        demand(Commodities.RARE_METALS, 1);
        demand(Commodities.HEAVY_MACHINERY, 1);
        demand(wfg.ltv_econ.constants.Commodities.LIGHT_MACHINERY, 1);
        demand(Commodities.ORGANICS, 1);
    }
}
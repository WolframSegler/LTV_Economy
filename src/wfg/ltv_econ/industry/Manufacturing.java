package wfg.ltv_econ.industry;

import com.fs.starfarer.api.impl.campaign.econ.impl.BaseIndustry;

public class Manufacturing extends BaseIndustry {
    @Override
    public void apply() {
        super.apply(true);

        if (!isFunctional()) {
			supply.clear();
			unapply();
		}
    }
}

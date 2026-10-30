package wfg.ltv_econ.industry;

import com.fs.starfarer.api.impl.campaign.econ.impl.BaseIndustry;

public final class Blackhole extends BaseIndustry {
	public static final String id = "wfg_test_blackhole";

    public void apply() {
        super.apply(true);
    }
}
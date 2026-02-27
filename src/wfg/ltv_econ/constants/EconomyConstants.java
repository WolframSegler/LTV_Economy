package wfg.ltv_econ.constants;

import java.util.Collections;
import java.util.List;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.SettingsAPI;
import com.fs.starfarer.api.campaign.econ.CommoditySpecAPI;

public class EconomyConstants {
    private static final SettingsAPI settings = Global.getSettings();

    public static final int MONTH = 30;
    public static final List<String> factionIDs = Collections.unmodifiableList(
        settings.getAllFactionSpecs().stream().map(f -> f.getId()).toList()
    );
    public static final List<String> commodityIDs = Collections.unmodifiableList(
        settings.getAllCommoditySpecs().stream().map(c -> c.getId()).toList()
    );
    public static final List<String> econCommodityIDs = Collections.unmodifiableList(
        settings.getAllCommoditySpecs().stream().filter(spec -> !spec.isNonEcon())
        .map(c -> c.getId()).toList()
    );
    public static final List<CommoditySpecAPI> econCommoditySpecs = Collections.unmodifiableList(
        settings.getAllCommoditySpecs().stream().filter(spec -> !spec.isNonEcon()).toList()
    );
    public static final List<String> industryIDs = Collections.unmodifiableList(
        settings.getAllIndustrySpecs().stream().map(i -> i.getId()).toList()
    ); 
}
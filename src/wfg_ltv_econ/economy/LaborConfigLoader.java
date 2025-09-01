package wfg_ltv_econ.economy;
import org.json.JSONObject;

import com.fs.starfarer.api.Global;

public class LaborConfigLoader {
    private static final String CONFIG_PATH = "./data/config/labor_config.json";

    private static JSONObject config;

    private static void load() {
        try {
            config = Global.getSettings().getMergedJSON(CONFIG_PATH);
        } catch (Exception ex) {
            throw new RuntimeException("Failed to load labor config: " + CONFIG_PATH, ex);
        }
    }

    public static JSONObject getConfig() {
        if (config == null) {
            load();
        }
        return config;
    }

    public static LaborConfig loadAsClass() {
        final JSONObject root = IndustryConfigLoader.getConfig();

        final LaborConfig result = new LaborConfig();

        try {

            result.avg_wage = (float) root.getDouble("avg_wage_month");
            result.RoSV = root.getInt("RoSV");
            result.LPV_month = root.getInt("LPV_month");
            result.LPV_day = (float) root.getDouble("LPV_day");
            result.RoVC_average = (float) root.getDouble("RoVC_average");
            result.RoCC_average = (float) root.getDouble("RoCC_average");
            result.RoVC_industry = (float) root.getDouble("RoVC_industry");
            result.RoCC_industry = (float) root.getDouble("RoCC_industry");
            result.industry_ratio = (float) root.getDouble("industry_ratio");
            result.RoVC_service = (float) root.getDouble("RoVC_service");
            result.RoCC_service = (float) root.getDouble("RoCC_service");
            result.service_ratio = (float) root.getDouble("service_ratio");
            result.RoVC_agriculture = (float) root.getDouble("RoVC_agriculture");
            result.RoCC_agriculture = (float) root.getDouble("RoCC_agriculture");
            result.agriculture_ratio = (float) root.getDouble("agriculture_ratio");
            result.RoVC_manual = (float) root.getDouble("RoVC_manual");
            result.RoCC_manual = (float) root.getDouble("RoCC_manual");
            result.manual_ratio = (float) root.getDouble("manual_ratio");
            result.RoVC_space = (float) root.getDouble("RoVC_space");
            result.RoCC_space = (float) root.getDouble("RoCC_space");
            result.space_ratio = (float) root.getDouble("space_ratio");

        } catch (Exception e) {
            throw new RuntimeException(
                "Failed to load labor configuration from " + CONFIG_PATH + ": "
                + e.getMessage(), e
            );
        }

        return result;
    }

    public static class LaborConfig {

        public float avg_wage;
        public int RoSV;
        public int LPV_month;
        public float LPV_day;
        public float RoVC_average;
        public float RoCC_average;
        public float RoVC_industry;
        public float RoCC_industry;
        public float industry_ratio;
        public float RoVC_service;
        public float RoCC_service;
        public float service_ratio;
        public float RoVC_agriculture;
        public float RoCC_agriculture;
        public float agriculture_ratio;
        public float RoVC_manual;
        public float RoCC_manual;
        public float manual_ratio;
        public float RoVC_space;
        public float RoCC_space;
        public float space_ratio;

        public final float getRoVC(OCCTag tag) {
            switch (tag) {
            case INDUSTRY:
                return RoVC_industry;
            case SERVICE:
                return RoVC_service;
            case AGRICULTURE:
                return RoVC_agriculture;
            case MANUAL:
                return RoVC_manual;
            case SPACE:
                return RoVC_space;
            case AVERAGE:
            default:
                return RoVC_average;
            }
        }

        public final float getRoCC(OCCTag tag) {
            switch (tag) {
            case INDUSTRY:
                return RoCC_industry;
            case SERVICE:
                return RoCC_service;
            case AGRICULTURE:
                return RoCC_agriculture;
            case MANUAL:
                return RoCC_manual;
            case SPACE:
                return RoCC_space;
            case AVERAGE:
            default:
                return RoCC_average;
            }
        }
    }

    public static enum OCCTag {
        AVERAGE,
        INDUSTRY,
        SERVICE,
        AGRICULTURE,
        MANUAL,
        SPACE
    }
}

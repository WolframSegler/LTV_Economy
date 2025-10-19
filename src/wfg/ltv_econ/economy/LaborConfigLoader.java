package wfg.ltv_econ.economy;
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

    public static void loadConfig() {
        final JSONObject root = LaborConfigLoader.getConfig();

        try {
            LaborConfig.RoSV = root.getInt("RoSV");
            LaborConfig.LPV_month = root.getInt("LPV_month");
            LaborConfig.LPV_day = LaborConfig.LPV_month / 30f;
            LaborConfig.avg_wage = LaborConfig.LPV_month / LaborConfig.RoSV;
            LaborConfig.populationRatioThatAreWorkers = (float) root.getDouble("populationRatioThatAreWorkers");
            LaborConfig.RoVC_average = (float) root.getDouble("RoVC_average");
            LaborConfig.RoVC_industry = (float) root.getDouble("RoVC_industry");
            LaborConfig.RoVC_manufacture = (float) root.getDouble("RoVC_manufacture");
            LaborConfig.RoVC_service = (float) root.getDouble("RoVC_service");
            LaborConfig.RoVC_agriculture = (float) root.getDouble("RoVC_agriculture");
            LaborConfig.RoVC_mechanized = (float) root.getDouble("RoVC_mechanized");
            LaborConfig.RoVC_manual = (float) root.getDouble("RoVC_manual");
            LaborConfig.RoVC_space = (float) root.getDouble("RoVC_space");

        } catch (Exception e) {
            throw new RuntimeException(
                "Failed to load labor configuration from " + CONFIG_PATH + ": "
                + e.getMessage(), e
            );
        }
    }

    public static class LaborConfig {

        public static float avg_wage;
        public static int RoSV;
        public static int LPV_month;
        public static float LPV_day;

        public static float populationRatioThatAreWorkers;

        public static float RoVC_average;
        public static float RoVC_industry;
        public static float RoVC_manufacture;
        public static float RoVC_service;
        public static float RoVC_agriculture;
        public static float RoVC_mechanized;
        public static float RoVC_manual;
        public static float RoVC_space;

        static {
            LaborConfigLoader.loadConfig();
        }

        public static final float getRoVC(OCCTag tag) {
            switch (tag) {
            case INDUSTRY:
                return RoVC_industry;
            case MANUFACTURE:
                return RoVC_manufacture;
            case SERVICE:
                return RoVC_service;
            case AGRICULTURE:
                return RoVC_agriculture;
            case MANUAL:
                return RoVC_manual;
            case MECHANIZED:
                return RoVC_mechanized;
            case SPACE:
                return RoVC_space;
            case AVERAGE:
            default:
                return RoVC_average;
            }
        }

        public static final float getRoCC(OCCTag tag) {
            switch (tag) {
            case INDUSTRY:
                return 1f - RoVC_industry;
            case MANUFACTURE:
                return 1f - RoVC_manufacture;
            case SERVICE:
                return 1f - RoVC_service;
            case AGRICULTURE:
                return 1f - RoVC_agriculture;
            case MECHANIZED:
                return 1f - RoVC_mechanized;
            case MANUAL:
                return 1f - RoVC_manual;
            case SPACE:
                return 1f - RoVC_space;
            case AVERAGE:
            default:
                return 1f - RoVC_average;
            }
        }
    }

    public static enum OCCTag {
        AVERAGE,
        INDUSTRY,
        MANUFACTURE,
        SERVICE,
        AGRICULTURE,
        MECHANIZED,
        MANUAL,
        SPACE
    }
}

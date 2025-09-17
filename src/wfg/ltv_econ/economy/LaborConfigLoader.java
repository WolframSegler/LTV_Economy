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

    public static LaborConfig loadAsClass() {
        final JSONObject root = LaborConfigLoader.getConfig();

        final LaborConfig result = new LaborConfig();

        try {

            result.avg_wage = (float) root.getDouble("avg_wage_month");
            result.RoSV = root.getInt("RoSV");
            result.LPV_month = root.getInt("LPV_month");
            result.LPV_day = (float) root.getDouble("LPV_day");
            result.RoVC_average = (float) root.getDouble("RoVC_average");
            result.RoVC_industry = (float) root.getDouble("RoVC_industry");
            result.RoVC_manufacture = (float) root.getDouble("RoVC_manufacture");
            result.RoVC_service = (float) root.getDouble("RoVC_service");
            result.RoVC_agriculture = (float) root.getDouble("RoVC_agriculture");
            result.RoVC_mechanized = (float) root.getDouble("RoVC_mechanized");
            result.RoVC_manual = (float) root.getDouble("RoVC_manual");
            result.RoVC_space = (float) root.getDouble("RoVC_space");

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
        public float RoVC_industry;
        public float RoVC_manufacture;
        public float RoVC_service;
        public float RoVC_agriculture;
        public float RoVC_mechanized;
        public float RoVC_manual;
        public float RoVC_space;

        public final float getRoVC(OCCTag tag) {
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

        public final float getRoCC(OCCTag tag) {
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

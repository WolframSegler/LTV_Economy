package wfg.ltv_econ.configs;
import static wfg.ltv_econ.constants.economyValues.MONTH;
import static wfg.ltv_econ.constants.Mods.*;

import org.json.JSONObject;

import com.fs.starfarer.api.Global;

import lunalib.lunaSettings.LunaSettings;

public class LaborConfigLoader {
    private static final String CONFIG_PATH = "./data/config/ltvEcon/labor_config.json";

    private static JSONObject config;

    private static final void load() {
        try {
            config = Global.getSettings().getMergedJSON(CONFIG_PATH);
        } catch (Exception ex) {
            throw new RuntimeException("Failed to load labor config: " + CONFIG_PATH, ex);
        }
    }

    public static final JSONObject getConfig() {
        if (config == null) load();
        return config;
    }

    public static final void loadConfig() {
        final JSONObject root = getConfig();

        try {
        LaborConfig.RoSV = root.getInt("RoSV");
        LaborConfig.MAX_RoSV = root.getInt("MAX_RoSV");
        LaborConfig.LPV_month = root.getInt("LPV_month");
        LaborConfig.LPV_day = LaborConfig.LPV_month / (float) MONTH;
        LaborConfig.avg_wage = LaborConfig.LPV_month / LaborConfig.RoSV;
        LaborConfig.defaultWorkerCapPerOutput = (float) root.getDouble("defaultWorkerCapPerOutput");
        LaborConfig.dynamicWorkerCapPerOutput = (float) root.getDouble("dynamicWorkerCapPerOutput");
        LaborConfig.RoVC_average = (float) root.getDouble("RoVC_average");
        LaborConfig.RoVC_industry = (float) root.getDouble("RoVC_industry");
        LaborConfig.RoVC_consumer = (float) root.getDouble("RoVC_consumer");
        LaborConfig.RoVC_manufacture = (float) root.getDouble("RoVC_manufacture");
        LaborConfig.RoVC_service = (float) root.getDouble("RoVC_service");
        LaborConfig.RoVC_agriculture = (float) root.getDouble("RoVC_agriculture");
        LaborConfig.RoVC_mechanized = (float) root.getDouble("RoVC_mechanized");
        LaborConfig.RoVC_manual = (float) root.getDouble("RoVC_manual");
        LaborConfig.RoVC_space = (float) root.getDouble("RoVC_space");

        if (Global.getSettings().getModManager().isModEnabled(LUNA_LIB)) {
            LaborConfig.RoSV = LunaSettings.getInt(LTV_ECON, "labor_RoSV");
            LaborConfig.MAX_RoSV = LunaSettings.getInt(LTV_ECON, "labor_maxRoSV");
            LaborConfig.LPV_month = LunaSettings.getInt(LTV_ECON, "labor_lpvMonth");
            LaborConfig.LPV_day = LaborConfig.LPV_month / (float) MONTH;
            LaborConfig.avg_wage = LaborConfig.LPV_month / LaborConfig.RoSV;
            LaborConfig.RoVC_average = LunaSettings.getDouble(LTV_ECON, "labor_RoVC_average").floatValue();
            LaborConfig.RoVC_industry = LunaSettings.getDouble(LTV_ECON, "labor_RoVC_industry").floatValue();
            LaborConfig.RoVC_consumer = LunaSettings.getDouble(LTV_ECON, "labor_RoVC_consumer").floatValue();
            LaborConfig.RoVC_manufacture = LunaSettings.getDouble(LTV_ECON, "labor_RoVC_manufacture").floatValue();
            LaborConfig.RoVC_service = LunaSettings.getDouble(LTV_ECON, "labor_RoVC_service").floatValue();
            LaborConfig.RoVC_agriculture = LunaSettings.getDouble(LTV_ECON, "labor_RoVC_agriculture").floatValue();
            LaborConfig.RoVC_mechanized = LunaSettings.getDouble(LTV_ECON, "labor_RoVC_mechanized").floatValue();
            LaborConfig.RoVC_manual = LunaSettings.getDouble(LTV_ECON, "labor_RoVC_manual").floatValue();
            LaborConfig.RoVC_space = LunaSettings.getDouble(LTV_ECON, "labor_RoVC_space").floatValue();
        }

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
        public static int MAX_RoSV;
        public static int LPV_month;
        public static float LPV_day;

        public static float defaultWorkerCapPerOutput;
        public static float dynamicWorkerCapPerOutput;

        public static float RoVC_average;
        public static float RoVC_industry;
        public static float RoVC_consumer;
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
            case CONSUMER:
                return RoVC_consumer;
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
            case CONSUMER:
                return 1f - RoVC_consumer;
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
        CONSUMER,
        MANUFACTURE,
        SERVICE,
        AGRICULTURE,
        MECHANIZED,
        MANUAL,
        SPACE
    }
}
package wfg.ltv_econ.config.loader;

import static wfg.ltv_econ.constants.EconomyConstants.MONTH;
import static wfg.ltv_econ.constants.Mods.*;
import static wfg.native_ui.util.Globals.settings;

import org.json.JSONArray;
import org.json.JSONObject;

import lunalib.lunaSettings.LunaSettings;
import wfg.ltv_econ.config.LaborConfig;

public class LaborConfigLoader {
    private static final String CONFIG_PATH = "./data/config/ltvEcon/labor_config.json";

    private static JSONObject config;

    public static final String AVERAGE_OCC_TAG = "average"; 

    private LaborConfigLoader() {};
    private static final void load() {
        try {
            config = settings.getMergedJSON(CONFIG_PATH);
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
        LaborConfig.NPC_WORKER_POOL_VISIBLE = root.getBoolean("NPC_WORKER_POOL_VISIBLE");
        LaborConfig.GROWTH_EFFECT_WORKER_POOL = root.getBoolean("GROWTH_EFFECT_WORKER_POOL");

        final JSONArray RoVCList = root.getJSONArray("RoVC_list");
        for (int i = 0; i < RoVCList.length(); i++) {
            final JSONObject RoVCObj = RoVCList.getJSONObject(i);
            final float value = (float) RoVCObj.getDouble("value");
            final String type = RoVCObj.getString("type");
            LaborConfig.RoVC_map.put(type, value);

            if (type.equals(AVERAGE_OCC_TAG)) LaborConfig.RoVC_average = value;
        }

        if (settings.getModManager().isModEnabled(LUNA_LIB)) {
            loadFromLunaSettings();
        }

        } catch (Exception e) {
        throw new RuntimeException(
            "Failed to load labor configuration from " + CONFIG_PATH, e
        );
        }
    }

    public static final void loadFromLunaSettings() {
        LaborConfig.RoSV = LunaSettings.getInt(LTV_ECON, "RoSV");
        LaborConfig.MAX_RoSV = LunaSettings.getInt(LTV_ECON, "MAX_RoSV");
        LaborConfig.LPV_month = LunaSettings.getInt(LTV_ECON, "LPV_month");
        LaborConfig.LPV_day = LaborConfig.LPV_month / (float) MONTH;
        LaborConfig.avg_wage = LaborConfig.LPV_month / LaborConfig.RoSV;

        LaborConfig.NPC_WORKER_POOL_VISIBLE = LunaSettings.getBoolean(LTV_ECON, "NPC_WORKER_POOL_VISIBLE");
        LaborConfig.GROWTH_EFFECT_WORKER_POOL = LunaSettings.getBoolean(LTV_ECON, "GROWTH_EFFECT_WORKER_POOL");

        final float avgValue = LunaSettings.getDouble(LTV_ECON, "RoVC_average").floatValue();
        LaborConfig.RoVC_average = avgValue;
        LaborConfig.RoVC_map.put(AVERAGE_OCC_TAG, avgValue);
        LaborConfig.RoVC_map.put("industry", LunaSettings.getDouble(LTV_ECON, "RoVC_industry").floatValue());
        LaborConfig.RoVC_map.put("consumer", LunaSettings.getDouble(LTV_ECON, "RoVC_consumer").floatValue());
        LaborConfig.RoVC_map.put("manufacture", LunaSettings.getDouble(LTV_ECON, "RoVC_manufacture").floatValue());
        LaborConfig.RoVC_map.put("service", LunaSettings.getDouble(LTV_ECON, "RoVC_service").floatValue());
        LaborConfig.RoVC_map.put("agriculture", LunaSettings.getDouble(LTV_ECON, "RoVC_agriculture").floatValue());
        LaborConfig.RoVC_map.put("mechanized", LunaSettings.getDouble(LTV_ECON, "RoVC_mechanized").floatValue());
        LaborConfig.RoVC_map.put("manual", LunaSettings.getDouble(LTV_ECON, "RoVC_manual").floatValue());
        LaborConfig.RoVC_map.put("space", LunaSettings.getDouble(LTV_ECON, "RoVC_space").floatValue());
    }
}
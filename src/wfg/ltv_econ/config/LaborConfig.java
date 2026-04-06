package wfg.ltv_econ.config;

import java.util.Map;

import wfg.ltv_econ.config.loader.LaborConfigLoader;
import wfg.native_ui.util.ArrayMap;

public class LaborConfig {
    private LaborConfig() {};

    public static boolean NPC_WORKER_POOL_VISIBLE;
    public static boolean GROWTH_EFFECT_WORKER_POOL;

    public static float avg_wage;
    public static int RoSV;
    public static int MAX_RoSV;
    public static int LPV_month;
    public static float LPV_day;

    public static float defaultWorkerCapPerOutput;
    public static float dynamicWorkerCapPerOutput;

    public static float RoVC_average;
    public static final Map<String, Float> RoVC_map = new ArrayMap<>(10);

    static {
        LaborConfigLoader.loadConfig();
    }

    public static final float getRoVC(final String tag) {
        return RoVC_map.getOrDefault(tag, RoVC_average);
    }

    public static final float getRoCC(final String tag) {
        return 1f - RoVC_map.getOrDefault(tag, RoVC_average);
    }
}
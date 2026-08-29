package wfg.ltv_econ.constant.strings;

import static wfg.ltv_econ.constant.strings.LocalizedStrings.*;

public final class Consumption {
    private Consumption() {};
    public static final String FACTION_FLEET_MAINTENANCE_KEY = "ffmk";
    public static final String FACTION_FLEET_MAINTENANCE_DESC = str("FACTION_FLEET_MAINTENANCE_DESC");
    public static final String ORDERS_DEMAND_KEY = "odk";
    public static final String ORDERS_DEMAND_DESC = str("ORDERS_DEMAND_DESC");
    public static final String FUEL_TARGET_TRADE_KEY = "ftt";
    public static final String FUEL_TARGET_TRADE_DESC = str("FUEL_TARGET_TRADE_DESC");
    public static final String DEMAND_ONLY_KEY = "dok";
    public static final String DEMAND_ONLY_DESC = str("uiStatDescDemandNoConsumption");
    public static final String CELL_CONSUMPTION_TARGET_KEY = "cctk";
    public static final String CELL_CONSUMPTION_TARGET_DESC = str("uiStatDescTargetFromConsumption");
    public static final String CELL_PRODUCTION_TARGET_KEY = "cptk";
    public static final String CELL_PRODUCTION_TARGET_DESC = str("uiStatDescTargetFromProduction");
    public static final String CELL_TARGET_PREDICTION_KEY = "ctprk";
    public static final String CELL_TARGET_PREDICTION_DESC = str("uiStatDescTargetFromPrediction");

    public static final String getDesc(String key) {
        switch (key) {
        case FACTION_FLEET_MAINTENANCE_KEY: return FACTION_FLEET_MAINTENANCE_DESC;
        case ORDERS_DEMAND_KEY: return ORDERS_DEMAND_DESC;
        case FUEL_TARGET_TRADE_KEY: return FUEL_TARGET_TRADE_DESC;
        case DEMAND_ONLY_KEY: return DEMAND_ONLY_DESC;
        case CELL_CONSUMPTION_TARGET_KEY: return CELL_CONSUMPTION_TARGET_DESC;
        case CELL_PRODUCTION_TARGET_KEY: return CELL_PRODUCTION_TARGET_DESC;
        case CELL_TARGET_PREDICTION_KEY: return CELL_TARGET_PREDICTION_DESC;
        default: return "";
        }
    }
}
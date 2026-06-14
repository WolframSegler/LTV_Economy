package wfg.ltv_econ.constant.strings;

import static wfg.ltv_econ.constant.strings.LocalizedStrings.*;

public class Consumption {
    public static final String FACTION_FLEET_MAINTENANCE_KEY = "ffmk";
    public static final String FACTION_FLEET_MAINTENANCE_DESC = str("FACTION_FLEET_MAINTENANCE_DESC");
    public static final String ORDERS_DEMAND_KEY = "odk";
    public static final String ORDERS_DEMAND_DESC = str("ORDERS_DEMAND_DESC");
    public static final String FUEL_TARGET_TRADE_KEY = "ftt";
    public static final String FUEL_TARGET_TRADE_DESC = str("FUEL_TARGET_TRADE_DESC");
    public static final String DEMAND_ONLY_KEY = "dok";
    public static final String DEMAND_ONLY_DESC = str("uiStatDescDemandNoConsumption");

    public static final String getDesc(String key) {
        switch (key) {
        case FACTION_FLEET_MAINTENANCE_KEY: return FACTION_FLEET_MAINTENANCE_DESC;
        case ORDERS_DEMAND_KEY: return ORDERS_DEMAND_DESC;
        case FUEL_TARGET_TRADE_KEY: return FUEL_TARGET_TRADE_DESC;
        case DEMAND_ONLY_KEY: return DEMAND_ONLY_DESC;
        default: return "";
        }
    }
}
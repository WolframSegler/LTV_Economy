package wfg.ltv_econ.constants.strings;

public class Consumption {
    public static final String FACTION_FLEET_MAINTENANCE_KEY = "ffmk";
    public static final String FACTION_FLEET_MAINTENANCE_DESC = "Maintenance of faction ships";
    public static final String ORDERS_DEMAND_KEY = "odk";
    public static final String ORDERS_DEMAND_DESC = "Production of faction ships";

    public static final String getDesc(String key) {
        switch (key) {
        case FACTION_FLEET_MAINTENANCE_KEY: return FACTION_FLEET_MAINTENANCE_DESC;
        case ORDERS_DEMAND_KEY: return ORDERS_DEMAND_DESC;
        default: return "";
        }
    }
}
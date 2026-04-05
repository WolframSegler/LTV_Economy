package wfg.ltv_econ.constants.strings;

public class Consumption {
    public static final String FACTION_FLEET_MAINTENANCE_KEY = "faction_fleet_maintenance";
    public static final String FACTION_FLEET_MAINTENANCE_DESC = "Maintenance of faction ships";

    public static final String getDesc(String key) {
        switch (key) {
        case FACTION_FLEET_MAINTENANCE_KEY: return FACTION_FLEET_MAINTENANCE_DESC;
        default: return "";
        }
    }
}
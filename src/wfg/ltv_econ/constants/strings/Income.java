package wfg.ltv_econ.constants.strings;

import com.fs.starfarer.api.impl.campaign.ids.Strings;

import wfg.ltv_econ.config.EconomyConfig;

public class Income {
    public static final String FACTION_CREW_WAGES_KEY = "faction_fleet_crew_wages";
    public static final String FACTION_CREW_WAGES_DESC = "Crew wages for faction ships";

    public static final String FACTION_SHIP_PRODUCTION_KEY = "faction_ship_production";
    public static final String FACTION_SHIP_PRODUCTION_DESC = "Faction ship production";

    public static final String TRADE_FUEL_PREMIUM_KEY = "fuel_premium";

    public static final String TRADE_FLEET_SHIPMENT_KEY = "trade_fleet_shipment";
    public static final String TRADE_FLEET_SHIPMENT_DESC = "Shipment costs for trade fleets";

    public static final String INDEPENDENT_BASE_FEE_KEY = "independent_base_fee";
    public static final String INDEPENDENT_BASE_FEE_DESC = "Independent hauler base fee";

    public static final String INDEPENDENT_PER_TON_KEY = "independent_per_ton";
    public static final String INDEPENDENT_PER_TON_DESC = "Per-ton transport fee";

    public static final String INDEPENDENT_VALUE_PERCENT_KEY = "independent_value_percent";

    public static final String INDEPENDENT_HAZARD_PAY_KEY = "independent_hazard_pay";
    public static final String INDEPENDENT_HAZARD_PAY_DESC = "Hazard pay (base + multiplier for required escort strength)";

    public static final String WORKER_WAGES_KEY = "wages";
    public static final String WORKER_WAGES_DESC = "Worker wages";

    public static final String COLONY_HAZARD_PAY_KEY = "hazard_pay";
    public static final String COLONY_HAZARD_PAY_DESC = "Hazard pay";

    public static final String POLICY_COST_KEY = "policy_cost_";
    public static final String POLICY_COST_DESC = "Policy cost for ";

    public static final String PLAYER_MARKET_TRANSACTION_KEY = "player_market";
    public static final String PLAYER_MARKET_TRANSACTION_DESC = "Player market transactions (buy/sell)";

    public static final String TRADE_EXPORT_KEY = "trade_export_";
    public static final String TRADE_EXPORT_DESC = "Export revenue for ";

    public static final String TRADE_IMPORT_KEY = "trade_import_";
    public static final String TRADE_IMPORT_DESC = "Import cost for ";

    public static final String INDUSTRY_INCOME_KEY = "industry_income_";
    public static final String INDUSTRY_INCOME_DESC = "Industry income";
    public static final String INDUSTRY_UPKEEP_KEY = "industry_upkeep";
    public static final String INDUSTRY_UPKEEP_DESC = "Industry upkeep";

    public static final String REDISTRIBUTION_DISCLAIMER =  "* If the redistribute credits option is enabled, credit reserves may differ from the calculated net income due to internal reallocation.";

    public static String getDesc(String key) {
        switch (key) {
            case FACTION_CREW_WAGES_KEY: return FACTION_CREW_WAGES_DESC;
            case FACTION_SHIP_PRODUCTION_KEY: return FACTION_SHIP_PRODUCTION_DESC;
            case TRADE_FUEL_PREMIUM_KEY: return "Fuel premium (" + Strings.X + EconomyConfig.FORCED_FUEL_IMPORT_COST_MULT + ")";
            case TRADE_FLEET_SHIPMENT_KEY: return TRADE_FLEET_SHIPMENT_DESC;
            case INDEPENDENT_BASE_FEE_KEY: return INDEPENDENT_BASE_FEE_DESC;
            case INDEPENDENT_PER_TON_KEY: return INDEPENDENT_PER_TON_DESC;
            case INDEPENDENT_VALUE_PERCENT_KEY: return "Percentage of cargo value (" + Strings.X + EconomyConfig.INDEPENDENT_TRADE_FLEET_PERCENT_CUT + ")";
            case INDEPENDENT_HAZARD_PAY_KEY: return INDEPENDENT_HAZARD_PAY_DESC;
            case WORKER_WAGES_KEY: return WORKER_WAGES_DESC;
            case POLICY_COST_KEY: return POLICY_COST_DESC;
            case PLAYER_MARKET_TRANSACTION_KEY: return PLAYER_MARKET_TRANSACTION_DESC;
            case TRADE_EXPORT_KEY: return TRADE_EXPORT_DESC;
            case TRADE_IMPORT_KEY: return TRADE_IMPORT_DESC;
            case INDUSTRY_INCOME_KEY: return INDUSTRY_INCOME_DESC;
            case INDUSTRY_UPKEEP_KEY: return INDUSTRY_UPKEEP_DESC;
            case COLONY_HAZARD_PAY_KEY: return COLONY_HAZARD_PAY_DESC;
            default: return "";
        }
    }
}
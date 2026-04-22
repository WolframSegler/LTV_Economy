package wfg.ltv_econ.ui.fleet;

import java.util.HashSet;
import java.util.Set;

public class TradeFilters {
    public static final Set<String> exporterFactionBlacklist = new HashSet<>(12);
    public static final Set<String> importerFactionBlacklist = new HashSet<>(12);
    public static float minTradeAmount = 0f;

    /** all, exporters, importers, in-faction */
    public static int directionMode = 0;

    public static boolean hideVirtualFleets = false;
}
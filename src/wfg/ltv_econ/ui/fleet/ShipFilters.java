package wfg.ltv_econ.ui.fleet;

public class ShipFilters {
    public static String searchQuery = "";

    public static SortMode sortMode = SortMode.NAME;

    public static boolean showCivilian = true;
    public static boolean showCombat = true;
    public static boolean showOnlyIdle = false;

    public static boolean showFrigates = true;
    public static boolean showDestroyers = true;
    public static boolean showCruisers = true;
    public static boolean showCapitals = true;

    public enum SortMode {
        NAME, CARGO, FUEL, CREW, COMBAT, COUNT
    }
}
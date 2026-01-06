package wfg.ltv_econ.economy;

import java.util.HashSet;
import java.util.Set;

public class PlayerFactionSettings {
    public boolean redistributeCredits = false;

    public final Set<String> embargoedFactions = new HashSet<>();
}
package wfg.ltv_econ.economy;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

public class PlayerFactionSettings implements Serializable {
    public boolean redistributeCredits = false;

    public final Set<String> embargoedFactions = new HashSet<>();
}
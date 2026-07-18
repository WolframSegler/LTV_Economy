package wfg.ltv_econ.economy;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

import wfg.ltv_econ.config.PlanConfig.WorkerAllocationPlan;

public class PlayerFactionSettings implements Serializable {
    public boolean redistributeCredits = false;
    public boolean automaticShipProductionForFaction = false;
    public boolean automaticWorkerAllocationForFaction = false;

    public final Set<String> embargoedFactions = new HashSet<>();

    public final Set<String> excludedMarketsFromWorkerAllocation = new HashSet<>();

    public WorkerAllocationPlan factionPlan = null;
}
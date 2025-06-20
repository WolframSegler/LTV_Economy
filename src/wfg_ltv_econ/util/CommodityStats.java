package wfg_ltv_econ.util;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.MutableStat.StatMod;
import com.fs.starfarer.api.campaign.econ.CommodityOnMarketAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;

public class CommodityStats {

    public final CommodityOnMarketAPI com;
    public final MarketAPI market;

    public int localProduction;
    public int localDemand;
    public int localDeficit;

    public int totalImports;
    public int inFactionImports;
    public int externalImports;

    public int totalExports;
    public int inFactionOnlyExport;
    public int globalExport;
    public int canNotExport;

    public int demandMet;
    public int demandMetWithLocal;
    
    public int available;
    public int extra;

    public CommodityStats(CommodityOnMarketAPI com, MarketAPI market) {
        this.com = com;
        this.market = market;

        int shippingGlobal = Global.getSettings().getShippingCapacity(market, false);
        int shippingInFaction = Global.getSettings().getShippingCapacity(market, true);

        available = com.getAvailable();
        localProduction = Math.min(com.getMaxSupply(), available);
        localDemand = com.getMaxDemand();
        demandMet = Math.min(localDemand, available);
        localDeficit = Math.max(0, localDemand - available);

        // Imports are whatever is left after production
        totalImports = Math.max(0, available - localProduction);

        // In-faction imports: scan availability mods for same-faction foreign sources
        inFactionImports = 0;
        for (StatMod mod : com.getAvailableStat().getFlatMods().values()) {
        Object source = mod.getSource();
            if (source instanceof String) {
            String sourceId = (String) source;
            MarketAPI sourceMarket = Global.getSector().getEconomy().getMarket(sourceId);
                if (sourceMarket != null &&
                    !sourceMarket.getId().equals(market.getId()) &&
                    sourceMarket.getFactionId().equals(market.getFactionId())) {
                    inFactionImports += mod.value;
                }
            }
        }

        // Clamp in-faction import to actual imports
        inFactionImports = Math.min(inFactionImports, totalImports);
        externalImports = totalImports - inFactionImports;

        // Calculate exports
        totalExports = Math.min(localProduction, shippingGlobal);
        globalExport = totalExports;
        extra = Math.max(0, available - Math.max(totalExports, localDemand));

        // How much demand is met locally
        demandMetWithLocal = Math.max(0, Math.min(available - extra, localProduction));

        // Remaining local production could be redirected as in-faction only exports
        inFactionOnlyExport = Math.max(0, localProduction - totalExports);

        // Export constraints
        if (globalExport + inFactionOnlyExport > shippingInFaction) {
            canNotExport = (globalExport + inFactionOnlyExport) - shippingInFaction;
            inFactionOnlyExport = Math.max(0, inFactionOnlyExport - canNotExport);
        } else {
            canNotExport = 0;
        }

        // Clamp all negatives to 0
        if (inFactionOnlyExport < 0) inFactionOnlyExport = 0;
        if (externalImports < 0) externalImports = 0;
        if (extra < 0) extra = 0;
        if (demandMetWithLocal < 0) demandMetWithLocal = 0;
    }
}

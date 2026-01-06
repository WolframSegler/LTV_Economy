package wfg.ltv_econ.plugins;

import java.util.List;
import java.util.Map;

import com.fs.starfarer.api.BaseModPlugin;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignEventListener;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.econ.Industry;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.impl.campaign.ids.Industries;

import rolflectionlib.util.RolfLectionUtil;
import wfg.ltv_econ.conditions.WorkerPoolCondition;
import wfg.ltv_econ.economy.CommodityInfo;
import wfg.ltv_econ.economy.CommodityStats;
import wfg.ltv_econ.economy.EconomyEngine;
import wfg.ltv_econ.economy.WorkerRegistry;
import wfg.ltv_econ.industry.LtvPopulationAndInfrastructure;

public class LtvEconomyModPlugin extends BaseModPlugin {
    public static List<CampaignEventListener> listeners;

    @SuppressWarnings("unchecked")
    public static final List<CampaignEventListener> getListeners() {
        if (listeners == null) {
            listeners = (List<CampaignEventListener>) RolfLectionUtil.getPrivateVariable(
            "listeners", Global.getSector());
        }
        return listeners;
    }

    @Override
    public void onApplicationLoad() throws Exception {}

    @Override
    public void onNewGame() {
        WorkerPoolCondition.initialize();
    }

    @Override
    public void onNewGameAfterEconomyLoad() {
        addManufacturingToMarkets();
    }

    @Override
    public void onGameLoad(boolean newGame) {
        WorkerPoolCondition.initialize();

        WorkerRegistry.loadInstance();
        EconomyEngine.loadInstance();

        final SectorAPI sector = Global.getSector();

        sector.addTransientScript(new EconomyButtonInjector());
        sector.addTransientScript(new LtvMarketReplacer());
        sector.getListenerManager().addListener(new AddWorkerIndustryOption(), true);

        if (newGame) injectStockpiles();
    }

    @Override
    public void beforeGameSave() {
        final Map<String, Object> persistentData = Global.getSector().getPersistentData();

        persistentData.put(EconomyEngine.EconEngineSerialID, EconomyEngine.getInstance());
        persistentData.put(WorkerRegistry.WorkerRegSerialID, WorkerRegistry.getInstance());

        Global.getSector().removeListener(EconomyEngine.getInstance());
    }

    @Override
    public void afterGameSave() {
        listeners.removeIf(l -> l.getClass() == EconomyEngine.class);
        listeners.add(0, EconomyEngine.getInstance());
    }

    private static final void addManufacturingToMarkets() {
        for (MarketAPI market : Global.getSector().getEconomy().getMarketsCopy()) {
            if (!market.isInEconomy() || market.hasIndustry("manufacturing")) continue;

            if (market.getSize() < 4 || market.isPlayerOwned()) continue;

            if (market.getPlanetEntity() == null || market.getPlanetEntity().isGasGiant()) continue;

            

            boolean hasRequiredIndustry = false;
            for (Industry ind : market.getIndustries()) {
                if (ind.getId().equals(Industries.HEAVYINDUSTRY) ||
                    ind.getId().equals(Industries.LIGHTINDUSTRY) ||
                    ind.getId().equals(Industries.ORBITALWORKS) ||
                    ind.getId().equals(Industries.REFINING)
                ) {
                    hasRequiredIndustry = true;
                    break;
                }
            }

            if (!hasRequiredIndustry) continue;

            if (LtvPopulationAndInfrastructure.getMaxIndustries(market.getSize()) >
                market.getIndustries().size()
            ) continue;

            market.addIndustry("manufacturing");
            market.reapplyIndustries();
        }
    }

    private static final void injectStockpiles() {
        final EconomyEngine engine = EconomyEngine.getInstance();
        for (CommodityInfo info : engine.getCommodityInfos()) {
            for (CommodityStats stats : info.getAllStats()) {
                stats.addStoredAmount(stats.getPreferredStockpile());
            }
        }
    }
}
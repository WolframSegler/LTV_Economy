package wfg.ltv_econ.plugins;

import java.util.Map;

import com.fs.starfarer.api.BaseModPlugin;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.econ.Industry;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.campaign.listeners.ListenerManagerAPI;
import com.fs.starfarer.api.impl.campaign.ids.Industries;

import wfg.ltv_econ.conditions.WorkerPoolCondition;
import wfg.ltv_econ.economy.EconomyEngine;
import wfg.ltv_econ.economy.WorkerRegistry;
import wfg.ltv_econ.industry.LtvPopulationAndInfrastructure;

public class LtvEconomyModPlugin extends BaseModPlugin {

    public static final String EconEngine = "ltv_econ_econ_engine";
    public static final String WorkerReg = "ltv_econ_worker_registry";

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

        final Map<String, Object> persistentData = Global.getSector().getPersistentData();
        final ListenerManagerAPI listener = Global.getSector().getListenerManager();

        final EconomyEngine engine = (EconomyEngine) persistentData.get(EconEngine);
        final WorkerRegistry workerRegistry = (WorkerRegistry) persistentData.get(WorkerReg);

        if (engine != null) {
            EconomyEngine.setInstance(engine);
        } else {
            EconomyEngine.createInstance();
            if (Global.getSettings().isDevMode()) {
                Global.getLogger(getClass()).info("Economy Engine constructed");
            }
        }
        if (workerRegistry != null) {
            WorkerRegistry.setInstance(workerRegistry);
        } else {
            WorkerRegistry.createInstance();
            if (Global.getSettings().isDevMode()) {
                Global.getLogger(getClass()).info("Worker Registery constructed");
            }
        }

        listener.addListener(EconomyEngine.getInstance(), false);
        listener.addListener(new AddWorkerIndustryOption(), true);
        Global.getSector().addTransientScript(new LtvMarketReplacer());
        Global.getSector().addTransientScript(new EconomyEngineScript());
    }

    @Override
    public void beforeGameSave() {
        final Map<String, Object> persistentData = Global.getSector().getPersistentData();

        persistentData.put(EconEngine, EconomyEngine.getInstance());
        persistentData.put(WorkerReg, WorkerRegistry.getInstance());
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
}
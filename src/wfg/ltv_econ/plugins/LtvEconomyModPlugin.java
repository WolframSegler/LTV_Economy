package wfg.ltv_econ.plugins;

import java.util.List;
import java.util.Map;

import com.fs.starfarer.api.BaseModPlugin;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.SettingsAPI;
import com.fs.starfarer.api.campaign.CampaignEventListener;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.econ.Industry;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.campaign.listeners.ListenerManagerAPI;
import com.fs.starfarer.api.impl.campaign.ids.Industries;

import wfg.ltv_econ.conditions.WorkerPoolCondition;
import wfg.ltv_econ.economy.EconomyEngine;
import wfg.ltv_econ.economy.WorkerRegistry;
import wfg.ltv_econ.industry.LtvPopulationAndInfrastructure;
import wfg.reflection.ReflectionUtils;

public class LtvEconomyModPlugin extends BaseModPlugin {

    public static final String EconEngine = "ltv_econ_econ_engine";
    public static final String WorkerReg = "ltv_econ_worker_registry";

    public static List<CampaignEventListener> listeners;

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
    @SuppressWarnings("unchecked")
    public void onGameLoad(boolean newGame) {
        WorkerPoolCondition.initialize();

        final SectorAPI sector = Global.getSector();
        final SettingsAPI settings = Global.getSettings();
        final Map<String, Object> persistentData = sector.getPersistentData();
        final ListenerManagerAPI listener = sector.getListenerManager();

        WorkerRegistry workerRegistry = (WorkerRegistry) persistentData.get(WorkerReg);
        EconomyEngine engine = (EconomyEngine) persistentData.get(EconEngine);

        if (workerRegistry != null) {
            WorkerRegistry.setInstance(workerRegistry);
        } else {
            workerRegistry = WorkerRegistry.createInstance();
            if (settings.isDevMode()) {
                Global.getLogger(getClass()).info("Worker Registery constructed");
            }
        }
        if (engine != null) {
            EconomyEngine.setInstance(engine);
        } else {
            engine = EconomyEngine.createInstance();
            if (settings.isDevMode()) {
                Global.getLogger(getClass()).info("Economy Engine constructed");
            }
        }

        listeners = (List<CampaignEventListener>) ReflectionUtils.get(
            sector, "listeners", List.class, false
        );

        listeners.removeIf(l -> l.getClass() == EconomyEngine.class);
        listeners.add(0, engine);

        sector.addTransientScript(new EconomyButtonInjector());
        sector.addTransientScript(new LtvMarketReplacer());
        sector.addTransientScript(new EconomyEngineScript());
        listener.addListener(new AddWorkerIndustryOption(), true);
        listener.addListener(engine, true);
    }

    @Override
    public void beforeGameSave() {
        final Map<String, Object> persistentData = Global.getSector().getPersistentData();

        persistentData.put(EconEngine, EconomyEngine.getInstance());
        persistentData.put(WorkerReg, WorkerRegistry.getInstance());

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
}
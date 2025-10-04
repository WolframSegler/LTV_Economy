package wfg.ltv_econ.plugins;

import java.util.Map;

import com.fs.starfarer.api.BaseModPlugin;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.econ.Industry;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.impl.campaign.ids.Industries;

import wfg.ltv_econ.conditions.WorkerPoolCondition;
import wfg.ltv_econ.economy.CommodityStats;
import wfg.ltv_econ.economy.CompatLayer;
import wfg.ltv_econ.economy.EconomyEngine;
import wfg.ltv_econ.economy.IndustryConfigLoader;
import wfg.ltv_econ.economy.WorkerRegistry;
import wfg.ltv_econ.industry.IndustryIOs;
import wfg.ltv_econ.industry.IndustryTooltips;
import wfg.ltv_econ.industry.LtvPopulationAndInfrastructure;
import wfg.ltv_econ.ui.dialogs.ComDetailDialog;
import wfg.ltv_econ.ui.panels.ColonyInventoryButton;
import wfg.ltv_econ.ui.panels.LtvComIconPanel;
import wfg.ltv_econ.ui.panels.LtvCommodityPanel;
import wfg.ltv_econ.ui.panels.LtvCommodityRowPanel;
import wfg.ltv_econ.ui.panels.LtvIndustryListPanel;
import wfg.ltv_econ.ui.panels.LtvIndustryWidget;
import wfg.ltv_econ.util.ListenerFactory;
import wfg.ltv_econ.util.RfReflectionUtils;
import wfg.reflection.ReflectionUtils;

public class LtvEconomyModPlugin extends BaseModPlugin {

    public static final String EconEngine = "ltv_econ_econ_engine";
    public static final String WorkerReg = "ltv_econ_worker_registry";

    @Override
    public void onApplicationLoad() throws Exception {
        // Force early load classes to prevent lag.
        Class<?> clazz = null;
        clazz = ListenerFactory.class;
        clazz = RfReflectionUtils.class;
        clazz = ReflectionUtils.class;
        clazz = CommodityStats.class;
        clazz = CompatLayer.class;
        clazz = IndustryConfigLoader.class;
        clazz = IndustryIOs.class;
        clazz = EconomyEngine.class;
        clazz = WorkerRegistry.class;
        clazz = IndustryTooltips.class;
        clazz = LtvMarketReplacer.class;
        clazz = ComDetailDialog.class;
        clazz = ColonyInventoryButton.class;
        clazz = LtvComIconPanel.class;
        clazz = LtvCommodityPanel.class;
        clazz = LtvCommodityRowPanel.class;
        clazz = LtvIndustryListPanel.class;
        clazz = LtvIndustryWidget.class;

        if (clazz != null) return;
    }

    @Override
    public void onNewGame() {
        WorkerPoolCondition.initialize();
    }

    @Override
    public void onNewGameAfterEconomyLoad() {
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

    @Override
    public void onGameLoad(boolean newGame) {
        WorkerPoolCondition.initialize();

        final Map<String, Object> persistentData = Global.getSector().getPersistentData();

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

        Global.getSector().getListenerManager().addListener(new AddWorkerIndustryOption(), true);
        Global.getSector().addTransientScript(new LtvMarketReplacer());
        Global.getSector().addTransientScript(new EconomyEngineScript());
    }

    @Override
    public void beforeGameSave() {
        final Map<String, Object> persistentData = Global.getSector().getPersistentData();

        persistentData.put(EconEngine, EconomyEngine.getInstance());
        persistentData.put(WorkerReg, WorkerRegistry.getInstance());
    }
}
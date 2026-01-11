package wfg.ltv_econ.plugins;


import com.fs.starfarer.api.BaseModPlugin;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.econ.Industry;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.campaign.listeners.ListenerManagerAPI;
import com.fs.starfarer.api.impl.campaign.econ.impl.PopulationAndInfrastructure;
import com.fs.starfarer.api.impl.campaign.ids.Industries;
import com.fs.starfarer.api.impl.campaign.intel.bar.events.BarEventManager;

import wfg.ltv_econ.conditions.WorkerPoolCondition;
import wfg.ltv_econ.economy.CommodityDomain;
import wfg.ltv_econ.economy.CommodityCell;
import wfg.ltv_econ.economy.WorkerRegistry;
import wfg.ltv_econ.economy.engine.EconomyEngine;
import wfg.ltv_econ.economy.engine.EconomyEngineSerializer;
import wfg.ltv_econ.intel.bar.events.BresVitalisBarEvent.BresVitalisBarEventCreator;
import wfg.ltv_econ.intel.bar.events.ConvergenceFestivalBarEvent.ConvergenceFestivalBarEventCreator;
import wfg.ltv_econ.intel.bar.events.WellnessComplianceBarEvent.WellnessComplianceBarEventCreator;
import wfg.ltv_econ.ui.scripts.UIInjectorListener;

public class LtvEconomyModPlugin extends BaseModPlugin {
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
        WorkerRegistry.loadInstance(false);
        EconomyEngineSerializer.loadInstance(false);

        final ListenerManagerAPI listenerManager = Global.getSector().getListenerManager();
        final BarEventManager barManager = BarEventManager.getInstance();

        listenerManager.addListener(new UIInjectorListener(), true);
        listenerManager.addListener(new AddWorkerIndustryOption(), true);

        barManager.addEventCreator(new BresVitalisBarEventCreator());
        barManager.addEventCreator(new WellnessComplianceBarEventCreator());
        barManager.addEventCreator(new ConvergenceFestivalBarEventCreator());

        if (newGame) injectStockpiles();
    }

    @Override
    public void beforeGameSave() {
        EconomyEngineSerializer.saveInstance();
        WorkerRegistry.saveInstance();
    }

    @Override
    public void afterGameSave() {
        WorkerRegistry.loadInstance(false);
        EconomyEngineSerializer.loadInstance(false);
    }

    private static final void addManufacturingToMarkets() {
        for (MarketAPI market : Global.getSector().getEconomy().getMarketsCopy()) {
            if (market.hasIndustry("manufacturing")) continue;
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

            if (PopulationAndInfrastructure.getMaxIndustries(market.getSize()) >
                market.getIndustries().size()
            ) continue;

            market.addIndustry("manufacturing");
            market.reapplyIndustries();
        }
    }

    private static final void injectStockpiles() {
        final EconomyEngine engine = EconomyEngine.getInstance();
        for (CommodityDomain dom : engine.getComDomains()) {
            for (CommodityCell cell : dom.getAllCells()) {
                cell.addStoredAmount(cell.getPreferredStockpile() * 0.8f);
            }
        }
    }
}
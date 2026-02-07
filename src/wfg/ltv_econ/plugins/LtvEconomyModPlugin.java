package wfg.ltv_econ.plugins;

import com.fs.starfarer.api.BaseModPlugin;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.econ.Industry;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.campaign.listeners.ListenerManagerAPI;
import com.fs.starfarer.api.impl.campaign.ids.Industries;
import com.fs.starfarer.api.impl.campaign.intel.bar.events.BarEventManager;
import com.fs.starfarer.api.util.Misc;

import wfg.ltv_econ.conditions.WorkerPoolCondition;
import wfg.ltv_econ.economy.CommodityDomain;
import wfg.ltv_econ.economy.CommodityCell;
import wfg.ltv_econ.economy.WorkerRegistry;
import wfg.ltv_econ.economy.engine.EconomyEngine;
import wfg.ltv_econ.economy.engine.EconomyEngineSerializer;
import wfg.ltv_econ.intel.bar.events.BresVitalisBarEvent.BresVitalisBarEventCreator;
import wfg.ltv_econ.intel.bar.events.ConvergenceFestivalBarEvent.ConvergenceFestivalBarEventCreator;
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

        listenerManager.addListener(new UIInjectorListener(), true);
        listenerManager.addListener(new AddWorkerIndustryOption(), true);

        registerBarEvents();

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

    private static final void registerBarEvents() {
        final BarEventManager barManager = BarEventManager.getInstance();

        if (!barManager.hasEventCreator(BresVitalisBarEventCreator.class)) {
            barManager.addEventCreator(new BresVitalisBarEventCreator());
        }
        if (!barManager.hasEventCreator(ConvergenceFestivalBarEventCreator.class)) {
            barManager.addEventCreator(new ConvergenceFestivalBarEventCreator());
        }
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
            if (Misc.getNumIndustries(market) >= Misc.getMaxIndustries(market)) continue;

            market.addIndustry("manufacturing");
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
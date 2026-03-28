package wfg.ltv_econ.plugins;

import static wfg.ltv_econ.constants.Mods.*;

import com.fs.starfarer.api.BaseModPlugin;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.ModManagerAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.econ.Industry;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.campaign.listeners.ListenerManagerAPI;
import com.fs.starfarer.api.impl.campaign.fleets.EconomyFleetRouteManager;
import com.fs.starfarer.api.impl.campaign.ids.Industries;
import com.fs.starfarer.api.impl.campaign.intel.bar.events.BarEventManager;
import com.fs.starfarer.api.util.Misc;

import lunalib.lunaSettings.LunaSettings;
import wfg.ltv_econ.conditions.WorkerPoolCondition;
import wfg.ltv_econ.configs.ConfigLunaSettingsListener;
import wfg.ltv_econ.configs.EconomyConfigLoader.EconomyConfig;
import wfg.ltv_econ.economy.commodity.CommodityCell;
import wfg.ltv_econ.economy.commodity.CommodityDomain;
import wfg.ltv_econ.economy.engine.EconomyEngine;
import wfg.ltv_econ.economy.fleet.LtvEconFleetRouteManager;
import wfg.ltv_econ.intel.bar.events.BresVitalisBarEvent.BresVitalisBarEventCreator;
import wfg.ltv_econ.intel.bar.events.ConvergenceFestivalBarEvent.ConvergenceFestivalBarEventCreator;
import wfg.ltv_econ.plugins.industries.AddWorkerIndustryOption;
import wfg.ltv_econ.serializable.LtvEconSaveData;
import wfg.ltv_econ.ui.scripts.UIInjectorListener;

public class LtvEconomyModPlugin extends BaseModPlugin {
    @Override
    public void onApplicationLoad() throws Exception {
        final ModManagerAPI manager = Global.getSettings().getModManager();
        if (manager.isModEnabled(LUNA_LIB)) {
            LunaSettings.addSettingsListener(new ConfigLunaSettingsListener());
        }
    }

    @Override
    public void onNewGameAfterProcGen() {
        final SectorAPI sector = Global.getSector();
        sector.removeScriptsOfClass(EconomyFleetRouteManager.class);
        sector.addScript(new LtvEconFleetRouteManager());
    }

    @Override
    public void onNewGameAfterEconomyLoad() {
        WorkerPoolCondition.initialize();
        addManufacturingToMarkets();
    }

    @Override
    public void onGameLoad(boolean newGame) {
        LtvEconSaveData.loadInstance(false, newGame);

        final ListenerManagerAPI listenerManager = Global.getSector().getListenerManager();

        listenerManager.addListener(new UIInjectorListener(), true);
        listenerManager.addListener(new AddWorkerIndustryOption(), true);

        registerBarEvents();

        if (newGame) injectStockpiles();
    }

    @Override
    public void beforeGameSave() {
        LtvEconSaveData.saveInstance();
    }

    @Override
    public void afterGameSave() {
        LtvEconSaveData.loadInstance(false, false);
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

            if (EconomyConfig.MANUFACTURING_EXCLUSION_LIST.contains(market.getId())) continue;

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
        final EconomyEngine engine = EconomyEngine.instance();
        for (CommodityDomain dom : engine.getComDomains()) {
            for (CommodityCell cell : dom.getAllCells()) {
                cell.addStoredAmount(cell.getPreferredStockpile());
            }
        }
    }
}
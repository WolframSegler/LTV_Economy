package wfg.ltv_econ.plugin;

import static wfg.ltv_econ.constant.Mods.*;
import static wfg.native_ui.util.Globals.settings;

import com.fs.starfarer.api.BaseModPlugin;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.ModManagerAPI;
import com.fs.starfarer.api.campaign.econ.CommoditySpecAPI;
import com.fs.starfarer.api.campaign.econ.Industry;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.campaign.listeners.ListenerManagerAPI;
import com.fs.starfarer.api.impl.campaign.fleets.EconomyFleetRouteManager;
import com.fs.starfarer.api.impl.campaign.ids.Industries;
import com.fs.starfarer.api.util.Misc;

import lunalib.lunaSettings.LunaSettings;
import wfg.ltv_econ.config.loader.ConfigLunaSettingsListener;
import wfg.ltv_econ.config.planning.CommodityTargetGoal;
import wfg.ltv_econ.config.planning.ExportTargetGoal;
import wfg.ltv_econ.config.planning.FactionDemandCoverageGoal;
import wfg.ltv_econ.config.planning.HardAutarkyConstraint;
import wfg.ltv_econ.config.planning.ProfitExportGoal;
import wfg.ltv_econ.constant.EconomyConstants;
import wfg.ltv_econ.condition.WorkerPoolCondition;
import wfg.ltv_econ.config.EconConfig;
import wfg.ltv_econ.economy.commodity.CommodityCell;
import wfg.ltv_econ.economy.commodity.CommodityDomain;
import wfg.ltv_econ.economy.engine.EconomyEngine;
import wfg.ltv_econ.economy.engine.EconomyInfo;
import wfg.ltv_econ.economy.engine.EconomyLoop;
import wfg.ltv_econ.economy.fleet.FactionShipInventory;
import wfg.ltv_econ.economy.fleet.ShipProductionManager;
import wfg.ltv_econ.economy.registry.PlanningGoalRegistry;
import wfg.ltv_econ.industry.Manufacturing;
import wfg.ltv_econ.plugin.industries.AddWorkerIndustryOption;
import wfg.ltv_econ.plugin.industries.ServiceSectorIndustryOption;
import wfg.ltv_econ.serializable.LtvEconSaveData;
import wfg.ltv_econ.ui.scripts.UIInjectorListener;

public class LtvEconomyModPlugin extends BaseModPlugin {

    @Override
    public void onApplicationLoad() throws Exception {
        final ModManagerAPI manager = settings.getModManager();
        if (manager.isModEnabled(LUNA_LIB)) {
            LunaSettings.addSettingsListener(new ConfigLunaSettingsListener());
        }

        for (CommoditySpecAPI spec : EconomyConstants.econCommoditySpecs) {
            final String comID = spec.getId();
            final String name = spec.getName();
            PlanningGoalRegistry.register(CommodityTargetGoal.SERIAL_ID + EconomyLoop.KEY + name, () -> new CommodityTargetGoal(comID, name));
            PlanningGoalRegistry.register(ExportTargetGoal.SERIAL_ID + EconomyLoop.KEY + name, () -> new ExportTargetGoal(comID, name));
        }
        PlanningGoalRegistry.register(FactionDemandCoverageGoal.SERIAL_ID, () -> new FactionDemandCoverageGoal());
        PlanningGoalRegistry.register(HardAutarkyConstraint.SERIAL_ID, () -> new HardAutarkyConstraint());
        PlanningGoalRegistry.register(ProfitExportGoal.SERIAL_ID, () -> new ProfitExportGoal());
    }

    @Override
    public void onNewGameAfterProcGen() {
        Global.getSector().removeScriptsOfClass(EconomyFleetRouteManager.class);
    }

    @Override
    public void onNewGameAfterEconomyLoad() {
        addManufacturingToMarkets();

        WorkerPoolCondition.initialize();
    }

    @Override
    public void onGameLoad(boolean newGame) {
        LtvEconSaveData.loadInstance(false, newGame);

        final ListenerManagerAPI listenerManager = Global.getSector().getListenerManager();

        listenerManager.addListener(new UIInjectorListener(), true);
        listenerManager.addListener(new AddWorkerIndustryOption(), true);
        listenerManager.addListener(new ServiceSectorIndustryOption(), true);

        if (newGame) {
            injectStockpiles();
            injectShips();
        }
    }

    @Override
    public void beforeGameSave() {
        LtvEconSaveData.saveInstance();
    }

    @Override
    public void afterGameSave() {
        LtvEconSaveData.loadInstance(false, false);
    }

    private static final void addManufacturingToMarkets() {
        for (MarketAPI market : EconomyInfo.getMarketsCopy()) {
            if (market.hasIndustry(Manufacturing.id)) continue;
            if (market.getSize() < 4 || market.isPlayerOwned()) continue;
            if (market.getPlanetEntity() == null) continue;

            if (EconConfig.MANUFACTURING_EXCLUSION_LIST.contains(market.getId())) continue;

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

            market.addIndustry(Manufacturing.id);
        }

        // Exception for chicomoztoc to balance sector production better.
        // TODO find a more elegant solution like automatic industry injection when certain commodity deficits arise.
        // Create a new game setup phase where industries are added to markets to balance the economy.
        // Or create a market AI that builds industries over time depending on global/faction/local demand.
        final MarketAPI chico = Global.getSector().getEconomy().getMarket("chicomoztoc"); 
        if (!chico.hasIndustry(Manufacturing.id) && chico.hasIndustry(Industries.MINING)) {
            chico.removeIndustry(Industries.MINING, null, false);
            chico.addIndustry(Manufacturing.id);
        }
    }

    private static final void injectStockpiles() {
        final EconomyEngine engine = EconomyEngine.instance();
        for (CommodityDomain dom : engine.getComDomains()) {
            for (CommodityCell cell : dom.getAllCells()) {
                final float target = cell.getTargetStored();
                cell.addStoredAmount(target * ((1f + EconConfig.EXPORT_THRESHOLD_FACTOR) / 2f));
            }
        }
    }

    private static final void injectShips() {
        final EconomyEngine engine = EconomyEngine.instance();
        for (String factionID : EconomyConstants.visibleFactionIDs) {
            final FactionShipInventory inv = engine.getFactionShipInventory(factionID);

            ShipProductionManager.injectShipGameStart(inv);
        }
    }
}
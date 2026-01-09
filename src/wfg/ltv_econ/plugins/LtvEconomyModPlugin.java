package wfg.ltv_econ.plugins;

import java.util.List;

import com.fs.starfarer.api.BaseModPlugin;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignEventListener;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.econ.Industry;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.impl.campaign.ids.Industries;
import com.fs.starfarer.api.impl.campaign.intel.bar.events.BarEventManager;

import rolflectionlib.util.RolfLectionUtil;
import wfg.ltv_econ.conditions.WorkerPoolCondition;
import wfg.ltv_econ.economy.CommodityDomain;
import wfg.ltv_econ.economy.CommodityCell;
import wfg.ltv_econ.economy.EconomyEngine;
import wfg.ltv_econ.economy.WorkerRegistry;
import wfg.ltv_econ.industry.LtvPopulationAndInfrastructure;
import wfg.ltv_econ.intel.bar.events.BresVitalisBarEvent.BresVitalisBarEventCreator;
import wfg.ltv_econ.intel.bar.events.ConvergenceFestivalBarEvent.ConvergenceFestivalBarEventCreator;
import wfg.ltv_econ.intel.bar.events.WellnessComplianceBarEvent.WellnessComplianceBarEventCreator;

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
        WorkerRegistry.loadInstance(false);
        EconomyEngine.loadInstance(false);

        final SectorAPI sector = Global.getSector();

        sector.addTransientScript(new EconomyButtonInjector());
        sector.addTransientScript(new MarketUIReplacer());
        sector.getListenerManager().addListener(new AddWorkerIndustryOption(), true);

        if (newGame) injectStockpiles();

        final BarEventManager barManager = BarEventManager.getInstance();

        barManager.addEventCreator(new BresVitalisBarEventCreator());
        barManager.addEventCreator(new WellnessComplianceBarEventCreator());
        barManager.addEventCreator(new ConvergenceFestivalBarEventCreator());
    }

    @Override
    public void beforeGameSave() {
        EconomyEngine.saveInstance();
        WorkerRegistry.saveInstance();
    }

    @Override
    public void afterGameSave() {
        WorkerRegistry.loadInstance(false);
        EconomyEngine.loadInstance(false);
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

            if (LtvPopulationAndInfrastructure.getMaxIndustries(market.getSize()) >
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
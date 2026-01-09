package wfg.ltv_econ.economy.engine;

import java.util.List;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignEventListener;
import com.fs.starfarer.api.campaign.SectorAPI;

import wfg.ltv_econ.plugins.LtvEconomyModPlugin;

public class EconomyEngineSerializer {
    public static final String EconEngineSerialID = "ltv_econ_econ_engine";

    public static final EconomyEngine loadInstance(boolean forceRefresh) {
        final SectorAPI sector = Global.getSector();

        EconomyEngine engine = (EconomyEngine) sector.getPersistentData().get(EconEngineSerialID);

        if (engine != null && !forceRefresh) {
            EconomyEngine.setInstance(engine);
        } else {
            new EconomyEngine();
            if (Global.getSettings().isDevMode()) {
                Global.getLogger(EconomyEngine.class).info("Economy Engine constructed");
            }
        }

        final EconomyEngine instance = EconomyEngine.getInstance();
        final List<CampaignEventListener> listeners = LtvEconomyModPlugin.getListeners();

        attachModules(instance);

        listeners.add(0, instance);
        sector.addTransientScript(instance);
        sector.getListenerManager().addListener(instance, true);

        return instance;
    }

    public static final void saveInstance() {
        final SectorAPI sector = Global.getSector();
        final EconomyEngine instance = EconomyEngine.getInstance();

        sector.getPersistentData().put(EconEngineSerialID, instance);

        sector.removeListener(instance);
        EconomyEngine.setInstance(null);
    }

    private static final void attachModules(EconomyEngine engine) {
        engine.logger.engine = engine;
        engine.info.engine = engine;
        engine.loop.engine = engine;
    }
}
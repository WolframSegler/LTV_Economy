package wfg.ltv_econ.economy.engine;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.SectorAPI;

public class EconomyEngineSerializer {
    public static final String EconEngineSerialID = "ltv_econ_econ_engine";

    public static final EconomyEngine loadInstance(boolean forceRefresh) {
        final SectorAPI sector = Global.getSector();

        EconomyEngine engine = (EconomyEngine) sector.getPersistentData().get(EconEngineSerialID);

        if (engine != null && !forceRefresh) {
            EconomyEngine.setInstance(engine);
        } else {
            engine = new EconomyEngine();
            if (Global.getSettings().isDevMode()) {
                Global.getLogger(EconomyEngine.class).info("Economy Engine constructed");
            }
        }

        // Order very important
        attachModules(engine);
        engine.fakeAdvanceWithAssignWorkers();

        sector.addTransientScript(engine);
        sector.addTransientListener(engine);
        sector.getListenerManager().addListener(engine, true);

        return engine;
    }

    public static final void saveInstance() {
        final SectorAPI sector = Global.getSector();
        final EconomyEngine instance = EconomyEngine.getInstance();

        sector.removeListener(instance);
        EconomyEngine.setInstance(null);

        sector.getPersistentData().put(EconEngineSerialID, instance);
    }

    static final void attachModules(EconomyEngine engine) {
        engine.logger.engine = engine;
        engine.info.engine = engine;
        engine.loop.engine = engine;
    }
}
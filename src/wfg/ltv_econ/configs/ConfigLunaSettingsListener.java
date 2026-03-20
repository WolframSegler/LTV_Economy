package wfg.ltv_econ.configs;

import com.fs.starfarer.api.GameState;
import com.fs.starfarer.api.Global;

import lunalib.lunaSettings.LunaSettingsListener;
import wfg.ltv_econ.economy.WorkerRegistry;
import wfg.ltv_econ.economy.engine.EconomyEngineSerializer;
import wfg.ltv_econ.economy.planning.IndustryGrouper;
import wfg.ltv_econ.economy.planning.IndustryMatrix;
import wfg.ltv_econ.industry.IndustryIOs;

public class ConfigLunaSettingsListener implements LunaSettingsListener {
    @Override
    public void settingsChanged(String modID) {
        // LOAD CONFIGS
        EconomyConfigLoader.loadFromLunaSettings();
        LaborConfigLoader.loadFromLunaSettings();

        // REFRESH CONFIG DEPENDENT STATE (ORDER MATTERS)
        IndustryConfigManager.reload();
        IndustryIOs.reload();
        IndustryGrouper.invalidate();
        IndustryMatrix.invalidate();

        // REFRESH THE ENGINE
        if (Global.getSettings().getCurrentState() == GameState.CAMPAIGN) {
            EconomyEngineSerializer.saveInstance();
            WorkerRegistry.saveInstance();

            WorkerRegistry.loadInstance(false);
            EconomyEngineSerializer.loadInstance(false, false);
        }
    }
}
package wfg.ltv_econ.config.loader;

import static wfg.native_ui.util.Globals.settings;

import com.fs.starfarer.api.GameState;

import lunalib.lunaSettings.LunaSettingsListener;
import wfg.ltv_econ.config.IndustryConfigManager;
import wfg.ltv_econ.economy.planning.IndustryGrouper;
import wfg.ltv_econ.economy.planning.IndustryMatrix;
import wfg.ltv_econ.industry.IndustryIOs;
import wfg.ltv_econ.serializable.LtvEconSaveData;

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

        // REFRESH THE DATA
        if (settings.getCurrentState() == GameState.CAMPAIGN) {
            LtvEconSaveData.saveInstance();
            LtvEconSaveData.loadInstance(false, false);
        }
    }
}
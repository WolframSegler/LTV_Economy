package wfg.ltv_econ.ui.panels;

import static wfg.wrap_ui.util.UIConstants.*;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.SettingsAPI;
import com.fs.starfarer.api.ui.CustomPanelAPI;
import com.fs.starfarer.api.ui.Fonts;
import com.fs.starfarer.api.ui.LabelAPI;
import com.fs.starfarer.api.ui.UIPanelAPI;

import wfg.ltv_econ.economy.EconomyEngine;
import wfg.ltv_econ.economy.PlayerFactionSettings;
import wfg.wrap_ui.ui.ComponentFactory;
import wfg.wrap_ui.ui.panels.Button;
import wfg.wrap_ui.ui.panels.CustomPanel;
import wfg.wrap_ui.ui.plugins.BasePanelPlugin;
import wfg.wrap_ui.util.CallbackRunnable;

public class FactionAdministrationPanel extends
    CustomPanel<BasePanelPlugin<FactionAdministrationPanel>, FactionAdministrationPanel, CustomPanelAPI>
{
    public FactionAdministrationPanel(UIPanelAPI parent, int width, int height) {
        super(parent, width, height, new BasePanelPlugin<>());
        getPlugin().init(this);

        createPanel();
    }

    public void createPanel() {
        final SettingsAPI settings = Global.getSettings();
        final EconomyEngine engine = EconomyEngine.getInstance();
        final PlayerFactionSettings factionSettings = engine.playerFactionSettings;
    
        final LabelAPI financePoliciesLbl = settings.createLabel(
            "Financial Policies", Fonts.INSIGNIA_LARGE);
        add(financePoliciesLbl).inTL(0, opad);
        int lblW = (int) financePoliciesLbl.getPosition().getHeight();

        final CallbackRunnable<Button> redistributeRun = (btn) -> {
            btn.checked = !btn.checked;
            factionSettings.redistributeCredits = btn.checked;
        };

        final Button redistributeBtn = ComponentFactory.createCheckboxWithText(
            m_panel, 22,
            "Redistribute credits between markets",
            Fonts.DEFAULT_SMALL, redistributeRun, base, pad
        );
        redistributeBtn.checked = factionSettings.redistributeCredits;
        add(redistributeBtn).inTL(opad, opad + lblW + pad);
    }
}
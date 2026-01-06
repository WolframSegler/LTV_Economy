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

        final int SECTION_I = opad;
        final int SECTION_II = SECTION_I + 420;

        final LabelAPI tradeDiploLbl = settings.createLabel(
            "Trade & Diplomacy", Fonts.INSIGNIA_LARGE);
        add(tradeDiploLbl).inTL(opad, SECTION_I);
        int lblW = (int) tradeDiploLbl.getPosition().getHeight();
        
        final FactionSelectionPanel factionEmbargoPanel = new FactionSelectionPanel(
            m_panel, 200, 350 
        );
        add(factionEmbargoPanel).inTL(opad, SECTION_I + lblW + opad);
        
        final LabelAPI financePoliciesLbl = settings.createLabel(
            "Financial Policies", Fonts.INSIGNIA_LARGE);
        add(financePoliciesLbl).inTL(opad, SECTION_II);
        lblW = (int) financePoliciesLbl.getPosition().getHeight();

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
        add(redistributeBtn).inTL(opad, SECTION_II + lblW + pad);
    }
}
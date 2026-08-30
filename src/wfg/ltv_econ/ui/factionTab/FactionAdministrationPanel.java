package wfg.ltv_econ.ui.factionTab;

import static wfg.ltv_econ.constant.strings.LocalizedStrings.*;
import static wfg.native_ui.util.Globals.settings;
import static wfg.native_ui.util.UIConstants.*;

import com.fs.starfarer.api.ui.Fonts;
import com.fs.starfarer.api.ui.LabelAPI;

import wfg.ltv_econ.economy.PlayerFactionSettings;
import wfg.ltv_econ.serializable.LtvEconSaveData;
import wfg.ltv_econ.ui.economyTab.FactionSelectionPanel;
import wfg.ltv_econ.ui.factionTab.dialog.WorkerAllocationDialog;
import wfg.native_ui.internal.ui.core.UIContainer;
import wfg.native_ui.ui.ComponentFactory;
import wfg.native_ui.ui.core.UIBuildableAPI;
import wfg.native_ui.ui.widget.Button;
import wfg.native_ui.ui.functional.CallbackRunnable;

public final class FactionAdministrationPanel extends UIContainer implements UIBuildableAPI {

    public FactionAdministrationPanel(int width, int height) {
        super(width, height);

        buildUI();
    }

    public void buildUI() {
        final PlayerFactionSettings factionSettings = LtvEconSaveData.instance().playerFactionSettings;

        final int SECTION_I = opad;
        final int SECTION_II = SECTION_I + 420;
        final int SECTION_III = SECTION_II + 80;

        { // SECTION I
        final LabelAPI tradeDiploLbl = settings.createLabel(str("uiTitleTradeDiplo"), Fonts.INSIGNIA_VERY_LARGE);
        add(tradeDiploLbl).inTL(opad, SECTION_I);
        int lblW = (int) tradeDiploLbl.getPosition().getHeight();

        final LabelAPI embargoListLbl = settings.createLabel(str("uiTitleEmbargoMenu"), Fonts.INSIGNIA_LARGE);
        add(embargoListLbl).inTL(opad + pad, SECTION_I + lblW + opad);
        lblW += (int) embargoListLbl.getPosition().getHeight();
        
        final FactionSelectionPanel factionEmbargoPanel = new FactionSelectionPanel(
            220, 320 
        );
        add(factionEmbargoPanel).inTL(opad + pad, SECTION_I + lblW + opad + pad*2);
        }
        
        { // SECTION II
        final LabelAPI financePoliciesLbl = settings.createLabel(str("uiTitleFinancialPolicies"), Fonts.INSIGNIA_VERY_LARGE);
        add(financePoliciesLbl).inTL(opad, SECTION_II);
        final int lblW = (int) financePoliciesLbl.getPosition().getHeight();

        final CallbackRunnable<Button> redistributeRun = (btn) -> {
            btn.setChecked(!btn.isChecked());
            factionSettings.redistributeCredits = btn.isChecked();
        };

        final Button redistributeBtn = ComponentFactory.createCheckboxWithText(22,
            str("uiCheckboxRedistributeCreditsTxt"),
            Fonts.DEFAULT_SMALL, redistributeRun, base, pad
        );
        redistributeBtn.setChecked(factionSettings.redistributeCredits);
        add(redistributeBtn).inTL(opad + pad, SECTION_II + lblW + pad);

        final CallbackRunnable<Button> automaticAssignWorkers = (btn) -> {
            btn.setChecked(!btn.isChecked());
            factionSettings.automaticWorkerAllocationForFaction = btn.isChecked();
        };

        final Button autoAllocatoWorkersBtn = ComponentFactory.createCheckboxWithText(22,
            str("uiCheckboxTxtAutoAssignWorkers"), Fonts.DEFAULT_SMALL, automaticAssignWorkers, base, pad
        );
        autoAllocatoWorkersBtn.setChecked(factionSettings.automaticWorkerAllocationForFaction);
        add(autoAllocatoWorkersBtn).inTL(opad + pad, SECTION_II + (lblW + pad) * 2);

        final CallbackRunnable<Button> tradeSuspensionRunnable = (btn) -> {
            btn.setChecked(!btn.isChecked());
            factionSettings.tradeSuspensionWhenConvoysRaided = btn.isChecked();
        };

        final Button tradeSuspensionBtn = ComponentFactory.createCheckboxWithText(22,
            str("uiCheckboxTxtTradeSuspensionRaid"), Fonts.DEFAULT_SMALL, tradeSuspensionRunnable, base, pad
        );
        tradeSuspensionBtn.setChecked(factionSettings.tradeSuspensionWhenConvoysRaided);
        add(tradeSuspensionBtn).inTL(opad + pad, SECTION_II + (lblW + pad) * 3);
        }

        { // SECTION III
        final LabelAPI shipPoliciesLbl = settings.createLabel(
            str("uiTitleShipHangar"), Fonts.INSIGNIA_VERY_LARGE);
        add(shipPoliciesLbl).inTL(opad, SECTION_III);
        final int lblW = (int) shipPoliciesLbl.getPosition().getHeight();

        final CallbackRunnable<Button> automaticProdRun = (btn) -> {
            btn.setChecked(!btn.isChecked());
            factionSettings.automaticShipProductionForFaction = btn.isChecked();
        };

        final Button automaticProdBtn = ComponentFactory.createCheckboxWithText(22,
            str("uiShipProdCheckboxTxt"), Fonts.DEFAULT_SMALL, automaticProdRun, base, pad
        );
        automaticProdBtn.setChecked(factionSettings.automaticShipProductionForFaction);
        add(automaticProdBtn).inTL(opad + pad, SECTION_III + lblW + pad);
        }

        { // SECTION IV
        final Button workerAllocatorBtn = new Button(150, 35, str("uiDialogBtnTitleWorkforceAllocator"), Fonts.DEFAULT_SMALL, (btn) -> {
            new WorkerAllocationDialog().show(0.3f, 0.3f);
        });
        add(workerAllocatorBtn).inTR(0f, 0f);
        }
    }
}
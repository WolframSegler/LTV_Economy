package wfg.ltv_econ.ui.scripts;

import static wfg.native_ui.util.UIConstants.*;

import java.util.List;

import org.lwjgl.input.Keyboard;

import com.fs.starfarer.api.campaign.CoreUITabId;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.ui.Alignment;
import com.fs.starfarer.api.ui.ButtonAPI;
import com.fs.starfarer.api.ui.Fonts;
import com.fs.starfarer.api.ui.UIComponentAPI;
import com.fs.starfarer.api.ui.UIPanelAPI;
import com.fs.starfarer.campaign.CampaignEngine;
import com.fs.starfarer.campaign.command.CommandTabData;
import com.fs.starfarer.campaign.command.OutpostItemRow;
import com.fs.starfarer.campaign.command.OutpostListPanel;
import com.fs.starfarer.campaign.econ.Market;
import com.fs.starfarer.campaign.ui.UITable;

import rolflectionlib.util.RolfLectionUtil;
import wfg.ltv_econ.ui.factionTab.FactionManagementPanel;
import wfg.ltv_econ.ui.outpostsTab.ColonyPopulationTable;
import wfg.ltv_econ.ui.outpostsTab.FactionResourcesTable;
import wfg.ltv_econ.ui.reusable.AbstractTabButtonInjector;
import wfg.ltv_econ.util.wrappers.MarketWrapper;
import wfg.native_ui.ui.functional.Button;
import wfg.native_ui.ui.functional.Button.CutStyle;

public class OutpostsTabUIBuilder extends AbstractTabButtonInjector {
    private static final Object outpostRowMarketWrapperField = findMarketHolder();
    private static final Class<?> outpostRowMarketWrapperClass = RolfLectionUtil.getFieldType(outpostRowMarketWrapperField);
    private static final Object wrapperMarketField = findMarketField(outpostRowMarketWrapperField);

    private final CommandTabData tabData = CampaignEngine.getInstance().getUIData().getCommandData();

    protected int getCurrentTabIndex() {
        return tabData.getSelectedTabIndex();
    }

    protected void setCurrentTabIndex(int index) {
        tabData.setSelectedTabIndex(index);
    }

    protected CoreUITabId getTargetCoreTabId() {
        return CoreUITabId.OUTPOSTS;
    }

    protected String getButtonLabel() {
        return "Faction";
    }

    protected UIComponentAPI createCustomComponent(UIPanelAPI parent) {
        final FactionManagementPanel panel = new FactionManagementPanel(parent);
        return panel.getPanel();
    }

    @Override
    protected void onPostInject() {
        updateColoniesPanel();
        addColonyInfoButtons();
    }

    private final void addColonyInfoButtons() {
        final OutpostListPanel panel = (OutpostListPanel) RolfLectionUtil.getMethodAndInvokeDirectly(
            "getColoniesPanel", targetTab);

        final UITable marketTable = (UITable) RolfLectionUtil.getAllVariables(panel).stream()
            .filter(e -> e instanceof UITable).findFirst().get();
        final ButtonAPI anchor = (ButtonAPI) RolfLectionUtil.getAllVariables(panel).stream()
            .filter(e -> e instanceof ButtonAPI).map(e -> (ButtonAPI) e)
            .filter(e -> e.getText() != null && e.getText().contains("Manage administrators"))
            .findFirst().get();

        final int tableH = (int) marketTable.getHeight() + 2;
        final ColonyPopulationTable popTable = new ColonyPopulationTable(panel, tableH);
        final FactionResourcesTable facTable = new FactionResourcesTable(panel, tableH);
        panel.addComponent(popTable.getPanel()).inTL(1, opad + 1);
        panel.addComponent(facTable.getPanel()).inTL(1, opad + 1);
        popTable.getPanel().setOpacity(0f);
        facTable.getPanel().setOpacity(0f);

        final String coloniesTxt = "      Owned colonies";
        final String populationTxt = "      Population metrics";
        final String factionTxt = "      Faction resources";

        final Button showColoniesButton = new Button(panel, 280, 24, coloniesTxt,
            Fonts.ORBITRON_12, null);
        final Button showPopButton = new Button(panel, 280, 24, populationTxt,
            Fonts.ORBITRON_12, null);
        final Button showFacButton = new Button(panel, 280, 24, factionTxt,
            Fonts.ORBITRON_12, null);

        final Runnable resetState = () -> {
            marketTable.setOpacity(0f);
            popTable.getPanel().setOpacity(0f);
            facTable.getPanel().setOpacity(0f);
            showColoniesButton.setChecked(false);
            showPopButton.setChecked(false);
            showFacButton.setChecked(false);
        };

        showColoniesButton.onClicked = (btn) -> {
            resetState.run();
            marketTable.setOpacity(1f);
            showColoniesButton.setChecked(true);
        };
        showPopButton.onClicked = (btn) -> {
            resetState.run();
            popTable.getPanel().setOpacity(1f);
            showPopButton.setChecked(true);
        };
        showFacButton.onClicked = (btn) -> {
            resetState.run();
            facTable.getPanel().setOpacity(1f);
            showFacButton.setChecked(true);
        };

        showColoniesButton.cutStyle = CutStyle.TL_BR;
        showPopButton.cutStyle = CutStyle.TL_BR;
        showFacButton.cutStyle = CutStyle.TL_BR;
        showColoniesButton.overrideCutSize = 8;
        showPopButton.overrideCutSize = 8;
        showFacButton.overrideCutSize = 8;
        showColoniesButton.setAlignment(Alignment.LMID);
        showPopButton.setAlignment(Alignment.LMID);
        showFacButton.setAlignment(Alignment.LMID);
        showColoniesButton.setShortcutAndAppendToText(Keyboard.KEY_Q);
        showPopButton.setShortcutAndAppendToText(Keyboard.KEY_A);
        showFacButton.setShortcutAndAppendToText(Keyboard.KEY_S);

        panel.addComponent(showColoniesButton.getPanel()).belowMid(anchor, opad);
        panel.addComponent(showPopButton.getPanel()).belowMid(showColoniesButton.getPanel(), opad);
        panel.addComponent(showFacButton.getPanel()).belowMid(showPopButton.getPanel(), opad);

        showColoniesButton.setChecked(true);
    }

    @SuppressWarnings("unchecked")
    private final void updateColoniesPanel() {
        final OutpostListPanel panel = (OutpostListPanel) RolfLectionUtil.getMethodAndInvokeDirectly(
            "getColoniesPanel", targetTab);

        final UITable table = (UITable) RolfLectionUtil.getAllVariables(panel).stream()
            .filter(e -> e instanceof UITable).findFirst().get();
        final var rows = (List<OutpostItemRow>) RolfLectionUtil.getMethodAndInvokeDirectly(
            "getRows", table);

        for (OutpostItemRow row : rows) {
            final Object wrapper = RolfLectionUtil.getPrivateVariable(outpostRowMarketWrapperField, row);
            final Market original = (Market) RolfLectionUtil.getPrivateVariable(wrapperMarketField, wrapper);
            RolfLectionUtil.setPrivateVariable(wrapperMarketField, wrapper, new MarketWrapper(original));
            row.recreate();
        }
    }

    private static final Object findMarketHolder() {
        for (Object field : RolfLectionUtil.getAllFields(OutpostItemRow.class)) {
            final Class<?> type = RolfLectionUtil.getFieldType(field);
            if (type.getEnclosingClass() == OutpostItemRow.class) {
                try {
                    final long length = RolfLectionUtil.getAllFields(type).stream()
                        .filter(e -> MarketAPI.class.isAssignableFrom(
                            RolfLectionUtil.getFieldType(e)
                        )).count();
                    if (length == 1) return field;
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        }
        return null;
    }

    private static final Object findMarketField(Object field) {
        return RolfLectionUtil.getAllFields(outpostRowMarketWrapperClass).stream()
            .filter(e -> MarketAPI.class.isAssignableFrom(
                RolfLectionUtil.getFieldType(e)
            )).findFirst().get();
    }
}
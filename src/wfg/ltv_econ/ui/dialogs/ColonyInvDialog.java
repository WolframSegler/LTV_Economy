package wfg.ltv_econ.ui.dialogs;

import java.awt.Color;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.SettingsAPI;
import com.fs.starfarer.api.campaign.CustomUIPanelPlugin;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.econ.CommoditySpecAPI;
import com.fs.starfarer.api.impl.codex.CodexDataV2;
import com.fs.starfarer.api.ui.CustomPanelAPI;
import com.fs.starfarer.api.ui.Fonts;
import com.fs.starfarer.api.ui.LabelAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;

import wfg.ltv_econ.economy.CommodityStats;
import wfg.ltv_econ.economy.EconomyEngine;
import wfg.wrap_ui.ui.UIState;
import wfg.wrap_ui.ui.UIState.State;
import wfg.wrap_ui.ui.dialogs.CustomDetailDialogPanel;
import wfg.wrap_ui.ui.dialogs.WrapDialogDelegate;
import wfg.wrap_ui.ui.panels.CustomPanel;
import wfg.wrap_ui.ui.panels.SortableTable;
import wfg.wrap_ui.ui.panels.TextPanel;
import wfg.wrap_ui.ui.panels.CustomPanel.HasActionListener;
import wfg.wrap_ui.ui.panels.SortableTable.cellAlg;
import wfg.wrap_ui.ui.panels.SpritePanel.Base;
import wfg.wrap_ui.ui.plugins.BasePanelPlugin;
import wfg.wrap_ui.ui.plugins.SpritePanelPlugin;
import wfg.wrap_ui.util.NumFormat;
import wfg.wrap_ui.util.WrapUiUtils;
import wfg.wrap_ui.util.WrapUiUtils.AnchorType;

public class ColonyInvDialog implements WrapDialogDelegate, HasActionListener {

    public static final int PANEL_W = 950;
    public static final int PANEL_H = 650;

    private final CustomPanel<?, ?, CustomPanelAPI> m_parentWrapper;
    private InteractionDialogAPI interactionDialog;

    public ColonyInvDialog(CustomPanel<?, ?, CustomPanelAPI> parent) {
        m_parentWrapper = parent;
    }

    public void createCustomDialog(CustomPanelAPI panel, CustomDialogCallback callback) {
        UIState.setState(State.DIALOG);
        final SettingsAPI settings = Global.getSettings();
        final EconomyEngine engine = EconomyEngine.getInstance();

        CustomDetailDialogPanel<?> m_panel = new CustomDetailDialogPanel<>(
            panel,
            m_parentWrapper.getMarket(),
            PANEL_W, PANEL_H,
            null
        );

        panel.addComponent(m_panel.getPanel()).inBL(0, 0);

        final TextPanel creditPanel = new TextPanel(panel, m_parentWrapper.getMarket(),
            200, 40, new BasePanelPlugin<>()
        ) {
            @Override  
            public void createPanel() {
                final String credits = NumFormat.formatCredits(engine.getCredits(getMarket().getId()));

                final LabelAPI creditLabel = settings.createLabel(
                    "Colony Balance: " + credits, Fonts.ORBITRON_16
                );
                creditLabel.setHighlight(credits);
                creditLabel.setHighlightColor(Misc.getHighlightColor());
                add(creditLabel).inTL(0, 0);
                getPos().setSize(creditLabel.getPosition().getWidth(), creditLabel.getPosition().getHeight());
            }

            @Override
            public void initializePlugin(boolean hasPlugin) {
                super.initializePlugin(hasPlugin);
                getPlugin().setIgnoreUIState(true);
            }

            @Override public CustomPanelAPI getTpParent() {
                return getPanel();
            }

            @Override  
            public TooltipMakerAPI createAndAttachTp() {
                final TooltipMakerAPI tp = getPanel().createUIElement(400, 1, false);

                String paragraph2 = "Maintaining a healthy balance is crucial to ensure the colony can afford essential imports and maintain full operational efficiency. If reserves run low, the colony may experience delays in trade and a reduction in productivity.";
                if (getMarket().isPlayerOwned()) {
                    paragraph2 = paragraph2 + "Please note that these credits are separate from your personal funds and are managed by the colony.";
                }                

                tp.addPara("This display shows the colony's current credit reserves. "+
                "These funds are generated from the colony's income and are used to pay for ongoing operations, including import purchases and the maintenance of orbital stations and defenses. \n"+ paragraph2, 3);

                add(tp);
                WrapUiUtils.anchorPanel(tp, getPanel(), AnchorType.RightTop, 5);
                return tp;
            }
        };
        m_panel.add(creditPanel).inTL(10, 10);

        SortableTable table = new SortableTable(
            m_panel.getPanel(),
            PANEL_W - 20, PANEL_H - 70,
            m_parentWrapper.getMarket(),
            20, 30
        );

        final String BaseProdTpTxt = "Theoretical local daily production, assuming no deficits or shortages.";
        final String RealProdTpTxt = "Actual daily production after accounting for stored deficits.";
        final String BaseBalanceTpTxt = "Net daily change in stockpile, ignoring imports or exports.";
        final String RealBalanceTpTxt = "Net daily change in stockpile, including imports or exports.";

        table.addHeaders(
            "", 40, null, true, false, 1,
            "Commodity", 160, "Commodity.", true, true, 1,
            "Stored", 100, "Amount in Colony stockpile.", false, false, -1,
            "Demand", 100, "Total demand by colony.", false, false, -1,
            "Base Prod", 140, BaseProdTpTxt, false, false, -1,
            "Real Prod", 140, RealProdTpTxt, false, false, -1,
            "Base Balance", 130, BaseBalanceTpTxt, false, false, -1,
            "Real Balance", 120, RealBalanceTpTxt, false, false, -1
        );

        for (CommoditySpecAPI com : EconomyEngine.getEconCommodities()) {

            final CommodityStats stats = engine.getComStats(com.getId(), m_panel.getMarket().getId());

            final Base comIcon = new Base(
                m_panel.getPanel(), m_panel.getMarket(),26, 26,
                new SpritePanelPlugin<>(), com.getIconName(), null, null, false
            );
            
            final long stored = stats.getRoundedStored();
            final int demand = (int) stats.getBaseDemand(false);
            final int baseProd = (int) stats.getLocalProduction(false);
            final int modifiedProd = (int) stats.getLocalProduction(true);
            final int baseBalance = (int) (stats.getLocalProduction(true) -
                stats.getBaseDemand(true));
            final int realBalance = (int) stats.getRealBalance();

            Color baseBlcColor = baseBalance < 0 ? 
                Misc.getNegativeHighlightColor() : baseBalance > 0 ?
                Misc.getPositiveHighlightColor() : Misc.getTextColor();

            Color realBlcColor = realBalance < 0 ? 
                Misc.getNegativeHighlightColor() : realBalance > 0 ?
                Misc.getPositiveHighlightColor() : Misc.getTextColor();

            table.addCell(comIcon, cellAlg.MID, null, null);
            table.addCell(com.getName(), cellAlg.LEFT, com.getName(), Misc.getBasePlayerColor());
            table.addCell(NumFormat.engNotation(stored), cellAlg.LEFTOPAD, stored, null);
            table.addCell(NumFormat.engNotation(demand), cellAlg.LEFTOPAD, demand, Misc.getNegativeHighlightColor());
            table.addCell(NumFormat.engNotation(baseProd), cellAlg.LEFTOPAD, baseProd, Misc.getHighlightColor());
            table.addCell(NumFormat.engNotation(modifiedProd), cellAlg.LEFTOPAD, modifiedProd, Misc.getHighlightColor());
            table.addCell(NumFormat.engNotation(baseBalance), cellAlg.LEFTOPAD, baseBalance, baseBlcColor);
            table.addCell(NumFormat.engNotation(realBalance), cellAlg.LEFTOPAD, realBalance, realBlcColor);

            table.pushRow(
                CodexDataV2.getCommodityEntryId(com.getId()), m_panel.getMarket(), null, null, null
            );
        }

        m_panel.add(table.getPanel()).inTL(10, 60);

        table.sortRows(2);

        table.createPanel();
    }

    @Override
    public void setInteractionDialog(InteractionDialogAPI a) {
        interactionDialog = a;
    }

    @Override
    public void customDialogConfirm() {
        customDialogCancel();
    }

    @Override
    public void customDialogCancel() {
        UIState.setState(State.NONE);

        if (interactionDialog != null) {
            interactionDialog.dismiss();
        }
    }

    public String getCancelText() {
        return "Dismiss";
    }

    public String getConfirmText() {
        return "Dismiss";
    }

    public boolean hasCancelButton() {
        return false;
    }

    public CustomUIPanelPlugin getCustomPanelPlugin() {
        return null;
    }
}

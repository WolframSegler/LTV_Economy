package wfg.ltv_econ.ui.dialogs;

import static wfg.wrap_ui.util.UIConstants.*;

import java.awt.Color;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.SettingsAPI;
import com.fs.starfarer.api.campaign.econ.CommoditySpecAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.impl.codex.CodexDataV2;
import com.fs.starfarer.api.ui.Fonts;
import com.fs.starfarer.api.ui.LabelAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.ui.UIComponentAPI;
import com.fs.starfarer.api.ui.UIPanelAPI;
import com.fs.starfarer.api.util.Misc;
import com.fs.starfarer.api.util.MutableValue;

import wfg.ltv_econ.economy.CommodityCell;
import wfg.ltv_econ.economy.PlayerMarketData;
import wfg.ltv_econ.economy.engine.EconomyEngine;
import wfg.ltv_econ.economy.engine.EconomyInfo;
import wfg.ltv_econ.ui.panels.LtvIndustryListPanel;
import wfg.wrap_ui.ui.Attachments;
import wfg.wrap_ui.ui.ComponentFactory;
import wfg.wrap_ui.ui.UIState;
import wfg.wrap_ui.ui.UIState.State;
import wfg.wrap_ui.ui.dialogs.DialogPanel;
import wfg.wrap_ui.ui.panels.Button;
import wfg.wrap_ui.ui.panels.Button.CutStyle;
import wfg.wrap_ui.ui.panels.SortableTable.cellAlg;
import wfg.wrap_ui.ui.panels.Slider;
import wfg.wrap_ui.ui.panels.SortableTable;
import wfg.wrap_ui.ui.panels.SpritePanel.Base;
import wfg.wrap_ui.ui.panels.TextPanel;
import wfg.wrap_ui.ui.plugins.BasePanelPlugin;
import wfg.wrap_ui.util.CallbackRunnable;
import wfg.wrap_ui.util.NumFormat;
import wfg.wrap_ui.util.WrapUiUtils;
import wfg.wrap_ui.util.WrapUiUtils.AnchorType;

public class ColonyInvDialog extends DialogPanel {

    public static final int PANEL_W = 950;
    public static final int PANEL_H = 650;

    private final MarketAPI m_market;

    public ColonyInvDialog(MarketAPI market) {
        super(Attachments.getScreenPanel(), PANEL_W, PANEL_H, null, null, "Dismiss");
        setConfirmShortcut();

        m_market = market;

        getHolo().setBackgroundAlpha(1, 1);
        backgroundDimAmount = 0.2f;

        createPanel();
    }

    @Override
    public void createPanel() {
        UIState.setState(State.DIALOG);
        final SettingsAPI settings = Global.getSettings();
        final EconomyEngine engine = EconomyEngine.getInstance();
        final PlayerMarketData data = engine.getPlayerMarketData(m_market.getId());

        final int sliderH = 32;
        final int sliderW = 300;
        final int buttonH = 28;
        final int buttonW = 70;
        final int tableStartY = 160;
        final int buttonY = (sliderH - buttonH) / 2; 
        final Color withdrawColor = new Color(180, 110, 90);
        final Color depositColor = new Color(90, 150, 110);

        final long colonyCredits = engine.getCredits(m_market.getId());
        final MutableValue playerCredits = Global.getSector().getPlayerFleet().getCargo().getCredits();

        final TextPanel colonyCreditPanel = new TextPanel(innerPanel, 200, 1, new BasePanelPlugin<>()) {
            {
                getPlugin().setIgnoreUIState(true);
            }

            @Override  
            public void createPanel() {
                final String credits = NumFormat.formatCredit(colonyCredits);

                label1 = settings.createLabel(
                    "Colony Balance: " + credits, Fonts.ORBITRON_16
                );
                label1.setHighlight(credits);
                label1.setHighlightColor(highlight);
                final float height = label1.computeTextHeight(label1.getText());
                add(label1).inTL(0, (sliderH - height) / 2f);
                getPos().setSize(label1.getPosition().getWidth(), sliderH);
            }

            @Override
            public UIPanelAPI getTpParent() {
                return Attachments.getScreenPanel();
            }

            @Override  
            public TooltipMakerAPI createAndAttachTp() {
                final TooltipMakerAPI tp = ComponentFactory.createTooltip(400, false);

                tp.addPara(
                    "Shows the colony's current credit reserves. These funds cover operating costs, import purchases, and upkeep for industries and structures. " +
                    "A low balance can slow trade and reduce output. " +
                    (m_market.isPlayerOwned()
                        ? "Colony reserves are separate from your personal credits."
                        : ""),
                    pad
                );

                ComponentFactory.addTooltip(tp, 0f, false);
                WrapUiUtils.anchorPanel(tp, getPanel(), AnchorType.RightTop, 5);
                return tp;
            }
        };
        innerPanel.addComponent(colonyCreditPanel.getPanel()).inTL(opad, 10);

        final TextPanel playerCreditPanel = new TextPanel(innerPanel, 200, 1, new BasePanelPlugin<>()) {
            {
                getPlugin().setIgnoreUIState(true);
            }
            
            @Override  
            public void createPanel() {
                final String credits = NumFormat.formatCredit((long) playerCredits.get());

                label1 = settings.createLabel(
                    "Your Balance: " + credits, Fonts.ORBITRON_16
                );
                label1.setHighlight(credits);
                label1.setHighlightColor(highlight);
                final float height = label1.computeTextHeight(label1.getText());
                add(label1).inTL(0, (sliderH - height) / 2f);
                getPos().setSize(label1.getPosition().getWidth(), sliderH);
            }

            @Override
            public UIPanelAPI getTpParent() {
                return Attachments.getScreenPanel();
            }

            @Override  
            public TooltipMakerAPI createAndAttachTp() {
                final TooltipMakerAPI tp = ComponentFactory.createTooltip(400f, false);

                tp.addPara(
                    "Shows your personal credits for transferring funds to or from the colony's reserves.",
                    pad
                );

                ComponentFactory.addTooltip(tp, 0f, false);
                WrapUiUtils.anchorPanel(tp, getPanel(), AnchorType.RightTop, 5);
                return tp;
            }
        };
        innerPanel.addComponent(playerCreditPanel.getPanel()).inTL(opad, 50);

        final TextPanel playerProfitPanel = new TextPanel(innerPanel, 200, 1, new BasePanelPlugin<>()) {
            {
                getPlugin().setIgnoreUIState(true);
            }
            
            @Override  
            public void createPanel() {
                if (data == null) return;
                final String ratio = Math.round(data.playerProfitRatio * 100) + "%";

                label1 = settings.createLabel(
                    "Auto Transfer Ratio: " + ratio, Fonts.ORBITRON_16
                );
                label1.setHighlight(ratio);
                label1.setHighlightColor(base);
                final float height = label1.computeTextHeight(label1.getText());
                add(label1).inTL(0, (sliderH - height) / 2f);
                getPos().setSize(label1.getPosition().getWidth(), sliderH);
            }

            @Override
            public UIPanelAPI getTpParent() {
                return Attachments.getScreenPanel();
            }

            @Override  
            public TooltipMakerAPI createAndAttachTp() {
                final TooltipMakerAPI tp = ComponentFactory.createTooltip(400f, false);

                tp.addPara(
                    "The ratio of monthly profits that get automatically transferred to the player",
                    pad
                );

                tp.addPara(
                    "You would receive %s from this colony this month. "+
                    "Note: some values are current so far, others are full-month estimates.",
                    pad,
                    highlight,
                    NumFormat.formatCredit((long) (engine.info.getNetIncome(
                        m_market, false)*data.playerProfitRatio
                    ))
                );

                ComponentFactory.addTooltip(tp, 0f, false);
                WrapUiUtils.anchorPanel(tp, getPanel(), AnchorType.RightTop, 5);
                return tp;
            }
        
            @Override
            public boolean isTooltipEnabled() {
                return data == null;
            }
        };
        if (m_market.isPlayerOwned()) innerPanel.addComponent(playerProfitPanel.getPanel()).inTL(opad, 90);

        final LabelAPI withdrawLabel = settings.createLabel(
            "Withdraw:", Fonts.ORBITRON_16
        );
        float labelH = withdrawLabel.computeTextHeight(withdrawLabel.getText());
        innerPanel.addComponent((UIComponentAPI)withdrawLabel).inTL(400, 10 + (sliderH - labelH) / 2f);

        final LabelAPI depositLabel = settings.createLabel(
            "Deposit:", Fonts.ORBITRON_16
        );
        labelH = depositLabel.computeTextHeight(depositLabel.getText());
        innerPanel.addComponent((UIComponentAPI)depositLabel).inTL(400, 50 + (sliderH - labelH) / 2f);

        final LabelAPI profitLabel = settings.createLabel(
            "Allocate:", Fonts.ORBITRON_16
        );
        labelH = profitLabel.computeTextHeight(profitLabel.getText());
        innerPanel.addComponent((UIComponentAPI)profitLabel).inTL(400, 90 + (sliderH - labelH) / 2f);

        
        final Slider withdrawSlider = new Slider(
            innerPanel, "", 0f, colonyCredits, sliderW, sliderH
        );
        withdrawSlider.setHighlightOnMouseover(true);
        withdrawSlider.setUserAdjustable(true);
        withdrawSlider.setBarColor(withdrawColor);
        withdrawSlider.showValueOnly = true;
        withdrawSlider.customText = () -> Misc.getDGSCredits(withdrawSlider.getProgressInterpolated());
        innerPanel.addComponent(withdrawSlider.getPanel()).inTL(500, 10);

        final Slider depositSlider = new Slider(
            innerPanel, "", 0f, playerCredits.get(), sliderW, sliderH
        );
        depositSlider.setHighlightOnMouseover(true);
        depositSlider.setUserAdjustable(true);
        depositSlider.setBarColor(depositColor);
        depositSlider.showValueOnly = true;
        depositSlider.customText = () -> Misc.getDGSCredits(depositSlider.getProgressInterpolated());
        innerPanel.addComponent(depositSlider.getPanel()).inTL(500, 50);

        final Slider profitSlider = new Slider(
            innerPanel, "", 0f, 100f, sliderW, sliderH
        );
        if (data != null) {
            profitSlider.setHighlightOnMouseover(true);
            profitSlider.setUserAdjustable(true);
            profitSlider.showPercent = true;
            profitSlider.roundBarValue = true;
            profitSlider.setProgress(data.playerProfitRatio * 100);
            innerPanel.addComponent(profitSlider.getPanel()).inTL(500, 90);
        }

        final Runnable refreshUI = () -> {
            final float colonyCred = playerCredits.get();
            final long playerCred = engine.getCredits(m_market.getId());
            final int profitRatio = data != null ? (int) (data.playerProfitRatio * 100) : 0;
            final LabelAPI colonyLbl = colonyCreditPanel.label1;
            final LabelAPI playerLbl = playerCreditPanel.label1;
            final LabelAPI profitLbl = playerProfitPanel.label1;

            depositSlider.setProgress(0);
            depositSlider.maxRange = colonyCred;
            withdrawSlider.setProgress(0);
            withdrawSlider.maxRange = playerCred;
            profitSlider.setProgress(profitRatio);

            colonyLbl.setText(
                "Colony Balance: " + NumFormat.formatCredit(playerCred)
            );
            colonyLbl.setHighlight(NumFormat.formatCredit(playerCred));
            colonyLbl.autoSizeToWidth(colonyLbl.computeTextWidth(colonyLbl.getText()));
            colonyCreditPanel.getPos().setSize(colonyLbl.getPosition().getWidth(), sliderH);

            playerLbl.setText(
                "Your Balance: " + NumFormat.formatCredit((long) colonyCred)
            );
            playerLbl.setHighlight(NumFormat.formatCredit((long) colonyCred));
            playerLbl.autoSizeToWidth(playerLbl.computeTextWidth(playerLbl.getText()));
            playerCreditPanel.getPos().setSize(playerLbl.getPosition().getWidth(), sliderH);

            profitLbl.setText("Auto Transfer Ratio: " + profitRatio + "%");
            profitLbl.setHighlight(profitRatio + "%");
            profitLbl.autoSizeToWidth(profitLbl.computeTextWidth(profitLbl.getText()));
            playerProfitPanel.getPos().setSize(profitLbl.getPosition().getWidth(), sliderH);
        };

        final CallbackRunnable<Button> withdrawRunnable = (btn) -> {
            engine.addCredits(m_market.getId(), (int) -withdrawSlider.getProgress());
            playerCredits.add((int) withdrawSlider.getProgress());
            refreshUI.run();
        };
        final CallbackRunnable<Button> depositRunnable = (btn) -> {
            engine.addCredits(m_market.getId(), (int) depositSlider.getProgress());
            playerCredits.add((int) -depositSlider.getProgress());
            refreshUI.run();
        };
        final CallbackRunnable<Button> profitRunnable = (btn) -> {
            if (data != null) data.playerProfitRatio = profitSlider.getProgress() / 100f;
            refreshUI.run();
        };

        final Button withdrawBtn = new Button(
            innerPanel, buttonW, buttonH, "Confirm", Fonts.ORBITRON_12, withdrawRunnable
        );
        final Button depositBtn = new Button(
            innerPanel, buttonW, buttonH, "Confirm", Fonts.ORBITRON_12, depositRunnable
        );
        final Button profitBtn = new Button(
            innerPanel, buttonW, buttonH, "Confirm", Fonts.ORBITRON_12, profitRunnable
        );

        withdrawBtn.quickMode = true;
        depositBtn.quickMode = true;
        profitBtn.quickMode = true;
        withdrawBtn.setCutStyle(CutStyle.ALL);
        depositBtn.setCutStyle(CutStyle.ALL);
        profitBtn.setCutStyle(CutStyle.ALL);
        innerPanel.addComponent(withdrawBtn.getPanel()).inTL(500 + sliderW + opad, 10 + buttonY);
        innerPanel.addComponent(depositBtn.getPanel()).inTL(500 + sliderW + opad, 50 + buttonY);
        innerPanel.addComponent(profitBtn.getPanel()).inTL(500 + sliderW + opad, 90 + buttonY);


        final SortableTable table = new SortableTable(
            innerPanel,
            PANEL_W - 20, PANEL_H - (tableStartY + 10),
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

        for (CommoditySpecAPI com : EconomyInfo.getEconCommodities()) {

            final CommodityCell cell = engine.getComCell(com.getId(), m_market.getId());

            final Base comIcon = new Base(
                innerPanel, 26, 26, com.getIconName(), null, null
            );
            
            final long stored = cell.getRoundedStored();
            final int demand = (int) cell.getBaseDemand(true);
            final int baseProd = (int) cell.getProduction(false);
            final int modifiedProd = (int) cell.getProduction(true);
            final int baseBalance = (int) (cell.getProduction(true) -
                cell.getDemand());
            final int realBalance = (int) cell.getFlowRealBalance();

            Color baseBlcColor = baseBalance < 0 ? 
                negative : baseBalance > 0 ?
                positive : Misc.getTextColor();

            Color realBlcColor = realBalance < 0 ? 
                negative : realBalance > 0 ?
                positive : Misc.getTextColor();

            table.addCell(comIcon, cellAlg.MID, null, null);
            table.addCell(com.getName(), cellAlg.LEFT, com.getName(), base);
            table.addCell(NumFormat.engNotation(stored), cellAlg.LEFTOPAD, stored, null);
            table.addCell(NumFormat.engNotation(demand), cellAlg.LEFTOPAD, demand, negative);
            table.addCell(NumFormat.engNotation(baseProd), cellAlg.LEFTOPAD, baseProd, highlight);
            table.addCell(NumFormat.engNotation(modifiedProd), cellAlg.LEFTOPAD, modifiedProd, highlight);
            table.addCell(NumFormat.engNotation(baseBalance), cellAlg.LEFTOPAD, baseBalance, baseBlcColor);
            table.addCell(NumFormat.engNotation(realBalance), cellAlg.LEFTOPAD, realBalance, realBlcColor);

            table.pushRow(
                CodexDataV2.getCommodityEntryId(com.getId()), null, null, null, null, null
            );
        }

        innerPanel.addComponent(table.getPanel()).inTL(10, tableStartY);

        table.sortRows(2);

        table.createPanel();
    }

    @Override
    public void dismiss(int option) {
        super.dismiss(option);

        UIState.setState(State.NONE);
        LtvIndustryListPanel.refreshPanel();
    }
}
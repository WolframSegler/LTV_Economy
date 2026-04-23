package wfg.ltv_econ.ui.marketInfo.dialogs;

import static wfg.native_ui.util.Globals.settings;
import static wfg.native_ui.util.UIConstants.*;

import java.awt.Color;

import org.lwjgl.input.Keyboard;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.econ.CommoditySpecAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.impl.codex.CodexDataV2;
import com.fs.starfarer.api.ui.Fonts;
import com.fs.starfarer.api.ui.LabelAPI;
import com.fs.starfarer.api.ui.UIComponentAPI;
import com.fs.starfarer.api.util.Misc;
import com.fs.starfarer.api.util.MutableValue;

import wfg.ltv_econ.config.EconConfig;
import wfg.ltv_econ.constants.EconomyConstants;
import wfg.ltv_econ.economy.PlayerMarketData;
import wfg.ltv_econ.economy.commodity.CommodityCell;
import wfg.ltv_econ.economy.engine.EconomyEngine;
import wfg.ltv_econ.economy.registry.MarketFinanceRegistry;
import wfg.ltv_econ.ui.marketInfo.LtvIndustryListPanel;
import wfg.native_ui.ui.dialog.DialogPanel;
import wfg.native_ui.ui.functional.Button;
import wfg.native_ui.ui.functional.Button.CutStyle;
import wfg.native_ui.ui.table.SortableTable.cellAlg;
import wfg.native_ui.ui.widget.Slider;
import wfg.native_ui.ui.table.SortableTable;
import wfg.native_ui.ui.visual.SpritePanel.Base;
import wfg.native_ui.ui.visual.TextPanel;
import wfg.native_ui.util.CallbackRunnable;
import wfg.native_ui.util.NumFormat;
import wfg.native_ui.util.NativeUiUtils;
import wfg.native_ui.util.NativeUiUtils.AnchorType;

public class ColonyInvDialog extends DialogPanel {

    public static final int PANEL_W = 950;
    public static final int PANEL_H = 650;

    private final MarketAPI m_market;

    public ColonyInvDialog(MarketAPI market) {
        super(PANEL_W, PANEL_H, null, null, "Dismiss");
        getButton(0).setShortcutAndAppendToText(Keyboard.KEY_3);

        m_market = market;

        backgroundDimAmount = 0.2f;
        holo.borderAlpha = 0.8f;

        buildUI();
    }

    @Override
    public void buildUI() {
        final EconomyEngine engine = EconomyEngine.instance();
        final PlayerMarketData data = engine.getPlayerMarketData(m_market.getId());
        final boolean hasData = data != null;

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

        final TextPanel colonyCreditPanel = new TextPanel(m_panel, 200, 1) {
            @Override  
            public void buildUI() {
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

            {
                tooltip.builder = (tp, exp) -> {
                    tp.addPara(
                        "Shows the colony's current credit reserves. These funds cover operating costs, import purchases, and upkeep for industries and structures. " +
                        "A low balance can slow trade and reduce output. " +
                        (m_market.isPlayerOwned()
                            ? "Colony reserves are separate from your personal credits."
                            : ""),
                        pad
                    );
                };
                tooltip.positioner = (tp, exp) -> {
                    NativeUiUtils.anchorPanel(tp, m_panel, AnchorType.RightTop, hpad);
                };
            }
        };
        add(colonyCreditPanel).inTL(opad, 10);

        final TextPanel playerCreditPanel = new TextPanel(m_panel, 200, 1) {
            @Override  
            public void buildUI() {
                final String credits = NumFormat.formatCredit(playerCredits.get());

                label1 = settings.createLabel(
                    "Your Balance: " + credits, Fonts.ORBITRON_16
                );
                label1.setHighlight(credits);
                label1.setHighlightColor(highlight);
                final float height = label1.computeTextHeight(label1.getText());
                add(label1).inTL(0, (sliderH - height) / 2f);
                getPos().setSize(label1.getPosition().getWidth(), sliderH);
            }

            {
                tooltip.builder = (tp, exp) -> {
                    tp.addPara(
                        "Shows your personal credits for transferring funds to or from the colony's reserves.",
                        pad
                    );
                };
                tooltip.positioner = (tp, exp) -> {
                    NativeUiUtils.anchorPanel(tp, m_panel, AnchorType.RightTop, hpad);
                };
            }
        };
        add(playerCreditPanel).inTL(opad, 50);

        final TextPanel playerProfitPanel = new TextPanel(m_panel, 200, 1) {
            @Override  
            public void buildUI() {
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
            
            {
                tooltip.enabled = data == null;
                tooltip.builder = (tp, exp) -> {
                    tp.addPara(
                        "The ratio of monthly profits that get automatically transferred to the player",
                        pad
                    );

                    tp.addPara(
                        "You would receive %s from this colony so far this month.",
                        pad, highlight,
                        NumFormat.formatCredit(Math.max(0f, data.playerProfitRatio *
                            MarketFinanceRegistry.instance().getLedger(m_market).getNetCurrentMonth()
                        ))
                    );
                };
                tooltip.positioner = (tp, exp) -> {
                    NativeUiUtils.anchorPanel(tp, m_panel, AnchorType.RightTop, hpad);
                };
            }
        };
        if (data != null) add(playerProfitPanel).inTL(opad, 90);

        final LabelAPI withdrawLabel = settings.createLabel(
            "Withdraw:", Fonts.ORBITRON_16
        );
        float labelH = withdrawLabel.computeTextHeight(withdrawLabel.getText());
        add((UIComponentAPI)withdrawLabel).inTL(400, 10 + (sliderH - labelH) / 2f);

        final LabelAPI depositLabel = settings.createLabel(
            "Deposit:", Fonts.ORBITRON_16
        );
        labelH = depositLabel.computeTextHeight(depositLabel.getText());
        add((UIComponentAPI)depositLabel).inTL(400, 50 + (sliderH - labelH) / 2f);

        final LabelAPI profitLabel = settings.createLabel(
            "Allocate:", Fonts.ORBITRON_16
        );
        labelH = profitLabel.computeTextHeight(profitLabel.getText());
        add((UIComponentAPI)profitLabel).inTL(400, 90 + (sliderH - labelH) / 2f);

        final Slider withdrawSlider = new Slider(
            m_panel, "", 0f, hasData ? data.getWithdrawLimit() : colonyCredits, sliderW, sliderH
        );
        withdrawSlider.setHighlightOnMouseover(true);
        withdrawSlider.setBarColor(withdrawColor);
        withdrawSlider.showValueOnly = true;
        withdrawSlider.customText = () -> Misc.getDGSCredits(withdrawSlider.getProgressInterpolated());
        add(withdrawSlider).inTL(500, 10);

        final Slider depositSlider = new Slider(
            m_panel, "", 0f, playerCredits.get(), sliderW, sliderH
        );
        depositSlider.setHighlightOnMouseover(true);
        depositSlider.setBarColor(depositColor);
        depositSlider.showValueOnly = true;
        depositSlider.customText = () -> Misc.getDGSCredits(depositSlider.getProgressInterpolated());
        add(depositSlider).inTL(500, 50);

        final Slider profitSlider = new Slider(m_panel, "", 0f,
            100f * EconConfig.AUTO_TRANSFER_PROFIT_LIMIT, sliderW, sliderH
        );
        if (data != null) {
            profitSlider.setHighlightOnMouseover(true);
            profitSlider.showPercent = true;
            profitSlider.roundBarValue = true;
            profitSlider.setProgress(data.playerProfitRatio * 100);
            add(profitSlider).inTL(500, 90);
        }

        final Runnable refreshUI = () -> {
            final long colonyCred = engine.getCredits(m_market.getId());
            final float playerCred = playerCredits.get();
            final int profitRatio = data != null ? (int) (data.playerProfitRatio * 100) : 0;
            final LabelAPI colonyLbl = colonyCreditPanel.label1;
            final LabelAPI playerLbl = playerCreditPanel.label1;
            final LabelAPI profitLbl = playerProfitPanel.label1;

            depositSlider.setProgress(0);
            depositSlider.maxRange = playerCred;
            withdrawSlider.setProgress(0);
            withdrawSlider.maxRange = hasData ? data.getWithdrawLimit() : colonyCred;
            profitSlider.setProgress(profitRatio);

            colonyLbl.setText(
                "Colony Balance: " + NumFormat.formatCredit(colonyCred)
            );
            colonyLbl.setHighlight(NumFormat.formatCredit(colonyCred));
            colonyLbl.autoSizeToWidth(colonyLbl.computeTextWidth(colonyLbl.getText()));
            colonyCreditPanel.getPos().setSize(colonyLbl.getPosition().getWidth(), sliderH);

            playerLbl.setText(
                "Your Balance: " + NumFormat.formatCredit(playerCred)
            );
            playerLbl.setHighlight(NumFormat.formatCredit(playerCred));
            playerLbl.autoSizeToWidth(playerLbl.computeTextWidth(playerLbl.getText()));
            playerCreditPanel.getPos().setSize(playerLbl.getPosition().getWidth(), sliderH);

            if (data != null) {
                profitLbl.setText("Auto Transfer Ratio: " + profitRatio + "%");
                profitLbl.setHighlight(profitRatio + "%");
                profitLbl.autoSizeToWidth(profitLbl.computeTextWidth(profitLbl.getText()));
                playerProfitPanel.getPos().setSize(profitLbl.getPosition().getWidth(), sliderH);
            }
        };

        final CallbackRunnable<Button> withdrawRunnable = (btn) -> {
            engine.addCredits(m_market.getId(), (int) -withdrawSlider.getProgress());
            playerCredits.add((int) withdrawSlider.getProgress());
            if (data != null) data.withdrewCreditsThisMonth = true;
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
            m_panel, buttonW, buttonH, "Confirm", Fonts.ORBITRON_12, withdrawRunnable
        );
        final Button depositBtn = new Button(
            m_panel, buttonW, buttonH, "Confirm", Fonts.ORBITRON_12, depositRunnable
        );
        final Button profitBtn = new Button(
            m_panel, buttonW, buttonH, "Confirm", Fonts.ORBITRON_12, profitRunnable
        );

        withdrawBtn.setQuickMode(true);
        depositBtn.setQuickMode(true);
        profitBtn.setQuickMode(true);
        withdrawBtn.cutStyle = CutStyle.ALL;
        depositBtn.cutStyle = CutStyle.ALL;
        profitBtn.cutStyle = CutStyle.ALL;
        add(withdrawBtn).inTL(500 + sliderW + opad, 10 + buttonY);
        add(depositBtn).inTL(500 + sliderW + opad, 50 + buttonY);
        if (data != null) add(profitBtn).inTL(500 + sliderW + opad, 90 + buttonY);


        final SortableTable table = new SortableTable(
            m_panel,
            PANEL_W - 20, PANEL_H - (tableStartY + 10),
            20, 30
        );

        final String BaseProdTpTxt = "Theoretical local daily production, assuming no deficits or shortages.";
        final String RealProdTpTxt = "Actual daily production after accounting for stored deficits.";
        final String BaseBalanceTpTxt = "Net daily change in stockpile, ignoring imports or exports.";
        final String RealBalanceTpTxt = "Net daily change in stockpile, including imports or exports. " +
            "Does not account for discrete expenditures such as hull production for the faction hangar.\n\n"+
            "This value may not reflect the effects of imports and exports due to their periodic nature";

        table.addHeaders(
            "", 40, null, true, false, 1,
            "Commodity", 160, "Commodity.", true, true, 1,
            "Stored", 100, "Amount in Colony stockpile.", false, false, -1,
            "Consumed", 100, "Total demand by colony.", false, false, -1,
            "Base Prod", 140, BaseProdTpTxt, false, false, -1,
            "Real Prod", 140, RealProdTpTxt, false, false, -1,
            "Base Balance", 130, BaseBalanceTpTxt, false, false, -1,
            "Real Balance", 120, RealBalanceTpTxt, false, false, -1
        );

        for (CommoditySpecAPI com : EconomyConstants.econCommoditySpecs) {

            final CommodityCell cell = engine.getComCell(com.getId(), m_market.getId());

            final Base comIcon = new Base(
                m_panel, 26, 26, com.getIconName(), null, null
            );
            
            final long stored = cell.getRoundedStored();
            final int demand = (int) cell.getTargetQuantum(true);
            final int baseProd = (int) cell.getProduction(false);
            final int modifiedProd = (int) cell.getProduction(true);
            final int baseBalance = (int) (cell.getProduction(true) - cell.getConsumption(true));
            final int realBalance = (int) cell.getQuantumRealBalance();

            final Color baseBlcColor = baseBalance < 0 ? 
                negative : baseBalance > 0 ?
                positive : text_color;

            final Color realBlcColor = realBalance < 0 ? 
                negative : realBalance > 0 ?
                positive : text_color;

            table.addCell(comIcon, cellAlg.MID, null, null);
            table.addCell(com.getName(), cellAlg.LEFT, com.getName(), base);
            table.addCell(NumFormat.engNotate(stored), cellAlg.LEFTOPAD, stored, null);
            table.addCell(NumFormat.engNotate(demand), cellAlg.LEFTOPAD, demand, negative);
            table.addCell(NumFormat.engNotate(baseProd), cellAlg.LEFTOPAD, baseProd, highlight);
            table.addCell(NumFormat.engNotate(modifiedProd), cellAlg.LEFTOPAD, modifiedProd, highlight);
            table.addCell(NumFormat.engNotate(baseBalance), cellAlg.LEFTOPAD, baseBalance, baseBlcColor);
            table.addCell(NumFormat.engNotate(realBalance), cellAlg.LEFTOPAD, realBalance, realBlcColor);

            table.pushRow(
                null, null, null, CodexDataV2.getCommodityEntryId(com.getId()), null, null
            );
        }

        add(table).inTL(10, tableStartY);

        table.sortRows(2);
    }

    @Override
    public void dismiss(int option) {
        super.dismiss(option);

        LtvIndustryListPanel.refreshPanel();
    }
}
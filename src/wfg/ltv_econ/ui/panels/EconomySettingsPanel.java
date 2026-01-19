package wfg.ltv_econ.ui.panels;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.SettingsAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.ui.Fonts;
import com.fs.starfarer.api.ui.LabelAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.ui.UIPanelAPI;

import static wfg.wrap_ui.util.UIConstants.*;

import org.apache.log4j.Logger;

import wfg.ltv_econ.economy.CommodityDomain;
import wfg.ltv_econ.economy.CommodityCell;
import wfg.ltv_econ.economy.IndustryMatrix;
import wfg.ltv_econ.economy.engine.EconomyEngine;
import wfg.ltv_econ.industry.IndustryIOs;
import wfg.wrap_ui.ui.Attachments;
import wfg.wrap_ui.ui.ComponentFactory;
import wfg.wrap_ui.ui.panels.Button;
import wfg.wrap_ui.ui.panels.CustomPanel;
import wfg.wrap_ui.ui.plugins.BasePanelPlugin;
import wfg.wrap_ui.util.CallbackRunnable;
import wfg.wrap_ui.util.WrapUiUtils;

public class EconomySettingsPanel extends CustomPanel<
    BasePanelPlugin<EconomySettingsPanel>, EconomySettingsPanel
> {
    public static final int LABEL_W = 150;
    public static final int LABEL_H = 50;
    public static final int BUTTON_W = 250;
    public static final int BUTTON_H = 25;

    public static final Logger logger = Global.getLogger(EconomySettingsPanel.class); 

    public EconomySettingsPanel(UIPanelAPI parent, int width, int height) {
        super(parent, width, height, new BasePanelPlugin<>());
        getPlugin().init(this);

        createPanel();
    }

    public void createPanel() {
        final SettingsAPI settings = Global.getSettings();
        final EconomyEngine engine = EconomyEngine.getInstance();

        final int SECTION_I = opad;

        { // SECTION I
        final LabelAPI debuOptions = settings.createLabel(
            "Debug Options", Fonts.INSIGNIA_VERY_LARGE);
        final int lblH = (int) debuOptions.computeTextHeight("Debug Options");
        add(debuOptions).inTL(opad, SECTION_I);

        { // REFRESH MARKETS
            final CallbackRunnable<Button> run = (btn) -> {
                engine.refreshMarkets();
                
                for (MarketAPI market : Global.getSector().getEconomy().getMarketsCopy()) {
                    final String marketID = market.getId();
                    if (market.isPlayerOwned() && !engine.isPlayerMarket(marketID)) {
                        engine.addPlayerMarketData(marketID);
                    }
                }
            };
            final Button button = new Button(m_panel, BUTTON_W, BUTTON_H,
                "Refresh Markets", Fonts.DEFAULT_SMALL, run
            );
            add(button).inTL(opad, SECTION_I + lblH + pad*2);

            button.setParentSupplier(() -> { return Attachments.getScreenPanel(); });
            button.setTooltipFactory(() -> {
                final TooltipMakerAPI tp = ComponentFactory.createTooltip(400f, false);

                tp.addPara("Synchronizes EconomyEngine markets with vanilla Economy markets. ", pad );

                ComponentFactory.addTooltip(tp, 0f, false);
                WrapUiUtils.mouseCornerPos(tp, opad);
                return tp;
            });
        }
        
        { // RECALCULATE WORKER ASSIGNMENTS
            final CallbackRunnable<Button> run = (btn) -> {
                IndustryMatrix.invalidate();
                
                EconomyEngine.getInstance().fakeAdvanceWithAssignWorkers();
            };
            final Button button = new Button(m_panel, BUTTON_W, BUTTON_H,
                "Recalculate Worker Assignments", Fonts.DEFAULT_SMALL, run
            );
            add(button).inTL(opad, SECTION_I + pad*4 + lblH + BUTTON_H);

            button.setParentSupplier(() -> { return Attachments.getScreenPanel(); });
            button.setTooltipFactory(() -> {
                final TooltipMakerAPI tp = ComponentFactory.createTooltip(400f, false);

                tp.addPara("Recreates the production matrix, and recalculates worker assignments", pad);

                ComponentFactory.addTooltip(tp, 0f, false);
                WrapUiUtils.mouseCornerPos(tp, opad);
                return tp;
            });
        }

        { // LOG ALL COMMODITY CELLS
            final CallbackRunnable<Button> run = (btn) -> {
                for (CommodityDomain dom : engine.getComDomains()) {
                for (CommodityCell cell : dom.getAllCells()) {
                    cell.logAllInfo();
                }
                }
            };
            final Button button = new Button(m_panel, BUTTON_W, BUTTON_H,
                "Log All Commodity Cells", Fonts.DEFAULT_SMALL, run
            );
            add(button).inTL(opad, SECTION_I + pad*6 + lblH + BUTTON_H*2);

            button.setParentSupplier(() -> { return Attachments.getScreenPanel(); });
            button.setTooltipFactory(() -> {
                final TooltipMakerAPI tp = ComponentFactory.createTooltip(400f, false);

                tp.addPara("Logs all the information about the CommodityCell for each cell", pad);

                ComponentFactory.addTooltip(tp, 0f, false);
                WrapUiUtils.mouseCornerPos(tp, opad);
                return tp;
            });
        }

        { // LOG ECONOMY INFO
            final CallbackRunnable<Button> run = (btn) -> {
                engine.logger.logEconomySnapshot();
                engine.logger.logCreditsSnapshot();
            };
            final Button button = new Button(m_panel, BUTTON_W, BUTTON_H,
                "Log Economy Information", Fonts.DEFAULT_SMALL, run
            );
            add(button).inTL(opad, SECTION_I + pad*8 + lblH + BUTTON_H*3);

            button.setParentSupplier(() -> { return Attachments.getScreenPanel(); });
            button.setTooltipFactory(() -> {
                final TooltipMakerAPI tp = ComponentFactory.createTooltip(400f, false);

                tp.addPara("Logs all the information about the Economy", pad);

                ComponentFactory.addTooltip(tp, 0f, false);
                WrapUiUtils.mouseCornerPos(tp, opad);
                return tp;
            });
        }

        { // LOG PLAYER MARKET DATA
            final CallbackRunnable<Button> run = (btn) -> {
                logger.info(engine.getPlayerMarketData().values());
            };
            final Button button = new Button(m_panel, BUTTON_W, BUTTON_H,
                "Log Player Market Data", Fonts.DEFAULT_SMALL, run
            );
            add(button).inTL(opad, SECTION_I + pad*10 + lblH + BUTTON_H*4);

            button.setParentSupplier(() -> { return Attachments.getScreenPanel(); });
            button.setTooltipFactory(() -> {
                final TooltipMakerAPI tp = ComponentFactory.createTooltip(400f, false);

                tp.addPara("Logs all player market data's", pad);

                ComponentFactory.addTooltip(tp, 0f, false);
                WrapUiUtils.mouseCornerPos(tp, opad);
                return tp;
            });
        }

        { // INDUSTRY IO MAPS LOG
            final CallbackRunnable<Button> run = (btn) -> {
                IndustryIOs.logMaps();
            };
            final Button button = new Button(m_panel, BUTTON_W, BUTTON_H,
                "Log Industry IO Maps", Fonts.DEFAULT_SMALL, run
            );
            add(button).inTL(opad, SECTION_I + pad*12 + lblH + BUTTON_H*5);

            button.setParentSupplier(() -> { return Attachments.getScreenPanel(); });
            button.setTooltipFactory(() -> {
                final TooltipMakerAPI tp = ComponentFactory.createTooltip(400f, false);

                tp.addPara("Logs the maps used by IndustryIOs to manage industry inputs and outputs", pad);

                ComponentFactory.addTooltip(tp, 0f, false);
                WrapUiUtils.mouseCornerPos(tp, opad);
                return tp;
            });
        }
        }
    }
}
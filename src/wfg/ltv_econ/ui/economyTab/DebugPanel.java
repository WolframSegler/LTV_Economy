package wfg.ltv_econ.ui.economyTab;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.impl.campaign.DebugFlags;
import com.fs.starfarer.api.impl.campaign.ids.Factions;
import com.fs.starfarer.api.ui.Fonts;
import com.fs.starfarer.api.ui.LabelAPI;
import com.fs.starfarer.api.ui.UIPanelAPI;

import static wfg.native_ui.util.UIConstants.*;

import java.util.List;

import static wfg.ltv_econ.constant.strings.LocalizedStrings.*;
import static wfg.native_ui.util.Globals.settings;

import org.apache.log4j.Logger;

import wfg.ltv_econ.economy.commodity.CommodityCell;
import wfg.ltv_econ.economy.commodity.CommodityDomain;
import wfg.ltv_econ.economy.engine.EconomyEngine;
import wfg.ltv_econ.economy.engine.EconomyInfo;
import wfg.ltv_econ.economy.fleet.ShipProductionManager;
import wfg.ltv_econ.economy.planning.IndustryMatrix;
import wfg.ltv_econ.industry.IndustryIOs;
import wfg.ltv_econ.industry.Manufacturing;
import wfg.native_ui.ui.core.UIBuildableAPI;
import wfg.native_ui.ui.functional.Button;
import wfg.native_ui.ui.panel.CustomPanel;
import wfg.native_ui.util.CallbackRunnable;

public class DebugPanel extends CustomPanel implements UIBuildableAPI {
    private static final Logger logger = Global.getLogger(DebugPanel.class); 

    public static final int LABEL_W = 150;
    public static final int LABEL_H = 50;
    public static final int BUTTON_W = 250;
    public static final int BUTTON_H = 25;

    public DebugPanel(UIPanelAPI parent, int width, int height) {
        super(parent, width, height);

        buildUI();
    }

    public void buildUI() {
        final EconomyEngine engine = EconomyEngine.instance();

        final int SECTION_I = opad;

        { // SECTION I
        final LabelAPI debuOptions = settings.createLabel(
            str("uiTitleDebugOptions"), Fonts.INSIGNIA_VERY_LARGE);
        final int lblH = (int) debuOptions.computeTextHeight(str("uiTitleDebugOptions"));
        add(debuOptions).inTL(opad, SECTION_I);

        { // REFRESH MARKETS
            final CallbackRunnable<Button> run = (btn) -> {
                engine.refreshMarketsHard();
            };
            final Button button = new Button(m_panel, BUTTON_W, BUTTON_H,
                str("uiBtnTitleRefreshMarkets"), Fonts.DEFAULT_SMALL, run
            );
            add(button).inTL(opad, SECTION_I + lblH + pad*2);

            button.tooltip.width = 400f;
            button.tooltip.builder = (tp, expanded) -> {
                tp.addPara(str("uiTpTxtRefreshMarkets"), pad);
            };
        }
        
        { // RECALCULATE WORKER ASSIGNMENTS
            final String btnText = str("uiAllocateWorkersBtnTxt");
            final CallbackRunnable<Button> run = (btn) -> {
                final long startTime = System.nanoTime();

                IndustryMatrix.invalidate();
                engine.fakeAdvanceWithAssignWorkers();

                final long endTime = System.nanoTime();
                final String time = ((endTime - startTime) / 1_000_000) + " " + str("uiMilliseconds");
                logger.info(str("uiElapsedTimeTxt") + time);
                btn.setText(btnText + " - " + time);
            };
            final Button button = new Button(m_panel, BUTTON_W, BUTTON_H,
                btnText, Fonts.DEFAULT_SMALL, run
            );
            add(button).inTL(opad, SECTION_I + pad*4 + lblH + BUTTON_H);

            button.tooltip.builder = (tp, expanded) -> {
                tp.addPara(str("uiTpTxtAllocateWorkers"), pad);
            };
        }

        { // FAKE ADVANCE
            final String btnText = str("uiTitleFakeAdvance");
            final CallbackRunnable<Button> run = (btn) -> {                
                final long startTime = System.nanoTime();
                
                engine.fakeAdvance();

                final long endTime = System.nanoTime();
                final String time = ((endTime - startTime) / 1_000_000) + " " + str("uiMilliseconds");
                logger.info(str("uiElapsedTimeTxt") + time);
                btn.setText(btnText + " - " + time);
            };
            final Button button = new Button(m_panel, BUTTON_W, BUTTON_H,
                btnText, Fonts.DEFAULT_SMALL, run
            );
            add(button).inTL(opad, SECTION_I + pad*6 + lblH + BUTTON_H*2);

            button.tooltip.builder = (tp, expanded) -> {
                tp.addPara(str("uiTpTxtFakeAdvance"), pad);
            };
        }

        { // REAL ADVANCE
            final String btnText = str("uiTitleRealAdvance");
            final CallbackRunnable<Button> run = (btn) -> {                
                final long startTime = System.nanoTime();
                
                engine.realAdvance();

                final long endTime = System.nanoTime();
                final String time = ((endTime - startTime) / 1_000_000) + " " + str("uiMilliseconds");
                logger.info(str("uiElapsedTimeTxt") + time);
                btn.setText(btnText + " - " + time);
            };
            final Button button = new Button(m_panel, BUTTON_W, BUTTON_H,
                btnText, Fonts.DEFAULT_SMALL, run
            );
            add(button).inTL(opad, SECTION_I + pad*8 + lblH + BUTTON_H*3);

            button.setEnabled(DebugFlags.COLONY_DEBUG);

            button.tooltip.builder = (tp, expanded) -> {
                tp.addPara(str("uiTpTxtRealAdvance"), pad);
            };
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
                str("uiBtnTitleLogAllComCells"), Fonts.DEFAULT_SMALL, run
            );
            add(button).inTL(opad, SECTION_I + pad*10 + lblH + BUTTON_H*4);

            button.tooltip.builder = (tp, expanded) -> {
                tp.addPara(str("uiTpTxtLogAllComCells"), pad);
            };
        }

        { // LOG ECONOMY INFO
            final CallbackRunnable<Button> run = (btn) -> {
                engine.logger.logEconomySnapshot();
                engine.logger.logCreditsSnapshot();
            };
            final Button button = new Button(m_panel, BUTTON_W, BUTTON_H,
                str("uiBtnTitleLogEconInfo"), Fonts.DEFAULT_SMALL, run
            );
            add(button).inTL(opad, SECTION_I + pad*12 + lblH + BUTTON_H*5);

            button.tooltip.builder = (tp, expanded) -> {
                tp.addPara(str("uiTpTxtLogEconInfo"), pad);
            };
        }

        { // LOG PLAYER MARKET DATA
            final CallbackRunnable<Button> run = (btn) -> {
                logger.info(engine.getMarketPopulationData().values());
            };
            final Button button = new Button(m_panel, BUTTON_W, BUTTON_H,
                str("uiBtnTitleLogMarketPopData"), Fonts.DEFAULT_SMALL, run
            );
            add(button).inTL(opad, SECTION_I + pad*14 + lblH + BUTTON_H*6);
            button.tooltip.builder = (tp, expanded) -> {
                tp.addPara(str("uiTpTxtLogMarketPopData"), pad);
            };
        }

        { // INDUSTRY IO MAPS LOG
            final CallbackRunnable<Button> run = (btn) -> {
                IndustryIOs.logMaps();
            };
            final Button button = new Button(m_panel, BUTTON_W, BUTTON_H,
                str("uiBtnTitleLogIndIO"), Fonts.DEFAULT_SMALL, run
            );
            add(button).inTL(opad, SECTION_I + pad*16 + lblH + BUTTON_H*7);

            button.tooltip.builder = (tp, expanded) -> {
                tp.addPara(str("uiTpTxtLogIndIO"), pad);
            };
        }

        { // SHIP ALLOCATION
            final String btnText = str("uiBtnTitleAllocateShips");
            final CallbackRunnable<Button> run = (btn) -> {                
                final long startTime = System.nanoTime();
                
                ShipProductionManager.planOrders(engine.getFactionShipInventory(Factions.HEGEMONY));

                final long endTime = System.nanoTime();
                final String time = ((endTime - startTime) / 1_000_000) + " " + str("uiMilliseconds");
                logger.info(str("uiElapsedTimeTxt") + time);
                btn.setText(btnText + " - " + time);
            };
            final Button button = new Button(m_panel, BUTTON_W, BUTTON_H,
                btnText, Fonts.DEFAULT_SMALL, run
            );
            add(button).inTL(opad, SECTION_I + pad*18 + lblH + BUTTON_H*8);

            button.setEnabled(DebugFlags.COLONY_DEBUG);

            button.tooltip.builder = (tp, expanded) -> {
                tp.addPara(str("uiTpTxtAllocateShips"), pad);
            };
        }

        { // SHIP ALLOCATION
            final CallbackRunnable<Button> run = (btn) -> {
                final List<MarketAPI> markets = EconomyInfo.getMarketsCopy();
                markets.removeIf(m -> m.getIndustry(Manufacturing.id) == null);
                logger.info(markets.stream().map(m -> m.getName()).toList());
            };
            final Button button = new Button(m_panel, BUTTON_W, BUTTON_H,
                str("uiBtnTitleLogMarketsWithManufacturing"), Fonts.DEFAULT_SMALL, run
            );
            add(button).inTL(opad, SECTION_I + pad*20 + lblH + BUTTON_H*9);
        }
        }
    }
}
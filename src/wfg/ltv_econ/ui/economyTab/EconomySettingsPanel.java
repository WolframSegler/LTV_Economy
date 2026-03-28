package wfg.ltv_econ.ui.economyTab;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.SettingsAPI;
import com.fs.starfarer.api.ui.Fonts;
import com.fs.starfarer.api.ui.LabelAPI;
import com.fs.starfarer.api.ui.UIPanelAPI;

import static wfg.native_ui.util.UIConstants.*;

import org.apache.log4j.Logger;

import wfg.ltv_econ.economy.commodity.CommodityCell;
import wfg.ltv_econ.economy.commodity.CommodityDomain;
import wfg.ltv_econ.economy.engine.EconomyEngine;
import wfg.ltv_econ.economy.planning.IndustryMatrix;
import wfg.ltv_econ.industry.IndustryIOs;
import wfg.native_ui.ui.core.UIBuildableAPI;
import wfg.native_ui.ui.panels.Button;
import wfg.native_ui.ui.panels.CustomPanel;
import wfg.native_ui.util.CallbackRunnable;

public class EconomySettingsPanel extends CustomPanel<EconomySettingsPanel> implements UIBuildableAPI {
    private static final SettingsAPI settings = Global.getSettings();
    private static final Logger logger = Global.getLogger(EconomySettingsPanel.class); 

    public static final int LABEL_W = 150;
    public static final int LABEL_H = 50;
    public static final int BUTTON_W = 250;
    public static final int BUTTON_H = 25;

    public EconomySettingsPanel(UIPanelAPI parent, int width, int height) {
        super(parent, width, height);

        buildUI();
    }

    public void buildUI() {
        final EconomyEngine engine = EconomyEngine.instance();

        final int SECTION_I = opad;

        { // SECTION I
        final LabelAPI debuOptions = settings.createLabel(
            "Debug Options", Fonts.INSIGNIA_VERY_LARGE);
        final int lblH = (int) debuOptions.computeTextHeight("Debug Options");
        add(debuOptions).inTL(opad, SECTION_I);

        { // REFRESH MARKETS
            final CallbackRunnable<Button> run = (btn) -> {
                engine.refreshMarketsHard();
            };
            final Button button = new Button(m_panel, BUTTON_W, BUTTON_H,
                "Refresh Markets", Fonts.DEFAULT_SMALL, run
            );
            add(button).inTL(opad, SECTION_I + lblH + pad*2);

            button.tooltip.width = 400f;
            button.tooltip.builder = (tp, expanded) -> {
                tp.addPara("Synchronizes EconomyEngine markets with vanilla Economy markets. Does extra checks to make sure no data is missing.", pad);
            };
        }
        
        { // RECALCULATE WORKER ASSIGNMENTS
            final String btnText = "Re-Allocate Workers ";
            final CallbackRunnable<Button> run = (btn) -> {
                final long startTime = System.nanoTime();

                IndustryMatrix.invalidate();
                engine.fakeAdvanceWithAssignWorkers();

                final long endTime = System.nanoTime();
                final String time = ((endTime - startTime) / 1_000_000) + " ms";
                logger.info("Elapsed time: " + time);
                btn.setText(btnText + " - " + time);
            };
            final Button button = new Button(m_panel, BUTTON_W, BUTTON_H,
                btnText, Fonts.DEFAULT_SMALL, run
            );
            add(button).inTL(opad, SECTION_I + pad*4 + lblH + BUTTON_H);

            button.tooltip.builder = (tp, expanded) -> {
                tp.addPara("Recreates the production matrix, and recalculates worker assignments. Also does a fake advance.", pad);
            };
        }

        { // FAKE ADVANCE
            final String btnText = "Fake Advance ";
            final CallbackRunnable<Button> run = (btn) -> {                
                final long startTime = System.nanoTime();
                
                engine.fakeAdvance();

                final long endTime = System.nanoTime();
                final String time = ((endTime - startTime) / 1_000_000) + " ms";
                logger.info("Elapsed time: " + time);
                btn.setText(btnText + " - " + time);
            };
            final Button button = new Button(m_panel, BUTTON_W, BUTTON_H,
                btnText, Fonts.DEFAULT_SMALL, run
            );
            add(button).inTL(opad, SECTION_I + pad*6 + lblH + BUTTON_H*2);

            button.tooltip.builder = (tp, expanded) -> {
                tp.addPara("Advances the economy by one tick without worker assignments or updating any values.", pad);
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
                "Log All Commodity Cells", Fonts.DEFAULT_SMALL, run
            );
            add(button).inTL(opad, SECTION_I + pad*8 + lblH + BUTTON_H*3);

            button.tooltip.builder = (tp, expanded) -> {
                tp.addPara("Logs all the information about the CommodityCell for each cell", pad);
            };
        }

        { // LOG ECONOMY INFO
            final CallbackRunnable<Button> run = (btn) -> {
                engine.logger.logEconomySnapshot();
                engine.logger.logCreditsSnapshot();
            };
            final Button button = new Button(m_panel, BUTTON_W, BUTTON_H,
                "Log Economy Information", Fonts.DEFAULT_SMALL, run
            );
            add(button).inTL(opad, SECTION_I + pad*10 + lblH + BUTTON_H*4);

            button.tooltip.builder = (tp, expanded) -> {
                tp.addPara("Logs all the information about the Economy", pad);
            };
        }

        { // LOG PLAYER MARKET DATA
            final CallbackRunnable<Button> run = (btn) -> {
                logger.info(engine.getPlayerMarketData().values());
            };
            final Button button = new Button(m_panel, BUTTON_W, BUTTON_H,
                "Log Player Market Data", Fonts.DEFAULT_SMALL, run
            );
            add(button).inTL(opad, SECTION_I + pad*12 + lblH + BUTTON_H*5);
            button.tooltip.builder = (tp, expanded) -> {
                tp.addPara("Logs all player market data's", pad);
            };
        }

        { // INDUSTRY IO MAPS LOG
            final CallbackRunnable<Button> run = (btn) -> {
                IndustryIOs.logMaps();
            };
            final Button button = new Button(m_panel, BUTTON_W, BUTTON_H,
                "Log Industry IO Maps", Fonts.DEFAULT_SMALL, run
            );
            add(button).inTL(opad, SECTION_I + pad*14 + lblH + BUTTON_H*6);

            button.tooltip.builder = (tp, expanded) -> {
                tp.addPara("Logs the maps used by IndustryIOs to manage industry inputs and outputs", pad);
            };
        }
        }
    }
}
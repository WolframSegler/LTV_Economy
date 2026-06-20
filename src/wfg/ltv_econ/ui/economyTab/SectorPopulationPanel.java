package wfg.ltv_econ.ui.economyTab;

import static wfg.ltv_econ.constant.strings.LocalizedStrings.*;
import static wfg.native_ui.util.Globals.settings;
import static wfg.native_ui.util.UIConstants.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.ui.Alignment;
import com.fs.starfarer.api.ui.Fonts;
import com.fs.starfarer.api.ui.LabelAPI;
import com.fs.starfarer.api.ui.UIPanelAPI;

import wfg.ltv_econ.economy.engine.EconomyInfo;
import wfg.native_ui.ui.core.UIBuildableAPI;
import wfg.native_ui.ui.panel.CustomPanel;
import wfg.native_ui.ui.visual.PieChart;
import wfg.native_ui.ui.visual.PieChart.PieSlice;
import wfg.native_ui.util.NumFormat;

public class SectorPopulationPanel extends CustomPanel implements UIBuildableAPI {
    private static final int PIECHART_S = 250;
    private static final int TITLE_H = 35;

    public SectorPopulationPanel(UIPanelAPI parent, int width, int height) {
        super(parent, width, height);

        buildUI();
    }

    @Override
    public void buildUI() {
        final SectorAPI sector = Global.getSector();
        final List<FactionAPI> factionList = sector.getAllFactions();

        { // Population by faction chart
        final ArrayList<PieSlice> data = new ArrayList<>();
        final long globalPop = EconomyInfo.getGlobalPopulationCount();

        if (globalPop > 0l) {
            for (FactionAPI faction : factionList) {
                final long factionPop = EconomyInfo.getFactionPopulationCount(faction.getId());
                final float share = (float) (factionPop / (double) globalPop);
                if (share > 0f) {
                    data.add(new PieSlice(faction.getId(), faction.getBaseUIColor(), share));
                }
            }
        }
        Collections.sort(data, (a, b) -> Float.compare(b.fraction, a.fraction));

        final LabelAPI title = settings.createLabel(str("uiTitleSectorPop"), Fonts.ORBITRON_24AABOLD);
        title.setColor(base);
        title.setAlignment(Alignment.TMID);
        add(title).inTL(opad*2, opad*2).setSize(PIECHART_S, TITLE_H);

        final PieChart chart = new PieChart(m_panel, PIECHART_S, PIECHART_S, data);
        add(chart).inTL(opad*2, opad*2 + TITLE_H + hpad);

        chart.tooltip.width = 360;
        chart.tooltip.builder = (tp, exp) -> {
            tp.addTitle(str("uiTitleSectorPopBreakdown"), base);
            
            tp.addPara(str("uiTpTxtSectorPopBreakdown1"), pad);

            tp.beginTable(
                base, dark, highlight, 20, true, true, new Object[] {
                    str("uiTableFaction"), 200, str("uiTablePopulationTitle"), 100
                }
            );

            for (PieSlice slice : data) {
                tp.addRow(new Object[] {
                    slice.color,
                    Global.getSector().getFaction(slice.uniqueID).getDisplayName(),
                    highlight,
                    NumFormat.engNotate(slice.fraction * globalPop)
                });
            }
            tp.addTable("", 0, opad);
        };
        }

        { // Workers by faction chart
        final ArrayList<PieSlice> data = new ArrayList<>();
        final long globalWorkers = EconomyInfo.getGlobalWorkerCount(true);

        if (globalWorkers > 0l) {
            for (FactionAPI faction : factionList) {
                final long factionWorkers = EconomyInfo.getFactionWorkerCount(faction.getId());
                final float share = (float) (factionWorkers / (double) globalWorkers);
                if (share > 0f) {
                    data.add(new PieSlice(faction.getId(), faction.getBaseUIColor(), share));
                }
            }
        }
        Collections.sort(data, (a, b) -> Float.compare(b.fraction, a.fraction));

        final LabelAPI title = settings.createLabel(str("uiTitleSectorWorkforce"), Fonts.ORBITRON_24AABOLD);
        title.setColor(base);
        title.setAlignment(Alignment.TMID);
        add(title).inTL(opad*4 + PIECHART_S, opad*2).setSize(PIECHART_S, TITLE_H);

        final PieChart chart = new PieChart(m_panel, PIECHART_S, PIECHART_S, data);
        add(chart).inTL(opad*4 + PIECHART_S, opad*2 + TITLE_H + hpad);

        chart.tooltip.width = 360;
        chart.tooltip.builder = (tp, exp) -> {
            tp.addTitle(str("uiTitleSectorWorkforceBreakdown"), base);
            
            tp.addPara(str("uiTpTxtSectorWorkforceBreakdown"), pad);

            tp.beginTable(
                base, dark, highlight, 20, true, true, new Object[] {
                    str("uiTableFaction"), 200, str("uiTableWorkersTitle"), 100
                }
            );

            for (PieSlice slice : data) {
                tp.addRow(new Object[] {
                    slice.color,
                    Global.getSector().getFaction(slice.uniqueID).getDisplayName(),
                    highlight,
                    NumFormat.engNotate(slice.fraction * globalWorkers)
                });
            }
            tp.addTable("", 0, opad);
        };
        }
    }
}
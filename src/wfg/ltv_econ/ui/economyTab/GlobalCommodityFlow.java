package wfg.ltv_econ.ui.economyTab;

import java.awt.Color;
import java.util.List;

import java.util.ArrayList;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.econ.CommoditySpecAPI;
import com.fs.starfarer.api.ui.Fonts;
import com.fs.starfarer.api.ui.LabelAPI;
import com.fs.starfarer.api.ui.UIPanelAPI;

import wfg.ltv_econ.config.EconConfig;
import wfg.ltv_econ.constants.UIColors;
import wfg.ltv_econ.economy.commodity.CommodityCell;
import wfg.ltv_econ.economy.commodity.CommodityDomain;
import wfg.ltv_econ.economy.engine.EconomyEngine;
import wfg.ltv_econ.economy.registry.WorkerRegistry;
import wfg.ltv_econ.economy.registry.WorkerRegistry.WorkerIndustryData;
import wfg.ltv_econ.ui.reusable.ComIconPanel;
import wfg.native_ui.ui.ComponentFactory;
import wfg.native_ui.ui.component.InteractionComp.ClickHandler;
import wfg.native_ui.ui.component.TooltipComp.TooltipBuilder;
import wfg.native_ui.ui.core.UIBuildableAPI;
import wfg.native_ui.ui.panel.CustomPanel;
import wfg.native_ui.ui.visual.PieChart;
import wfg.native_ui.ui.table.SortableTable;
import wfg.native_ui.ui.table.SortableTable.RowPanel;
import wfg.native_ui.ui.table.SortableTable.cellAlg;
import wfg.native_ui.ui.visual.SpritePanel.Base;
import wfg.native_ui.ui.visual.PieChart.PieSlice;
import wfg.native_ui.ui.visual.TextPanel;
import wfg.native_ui.util.NativeUiUtils;
import wfg.native_ui.util.NumFormat;
import static wfg.native_ui.util.UIConstants.*;
import static wfg.native_ui.util.Globals.settings;
import static wfg.ltv_econ.constants.strings.LocalizedStrings.*;

public class GlobalCommodityFlow extends CustomPanel implements UIBuildableAPI {
    private static final float PIE_CHART_THRESHOLD = 0.001f;

    public static final int ICON_SIZE = 135;
    public static final int LABEL_W = 150;
    public static final int LABEL_H = 50;
    public static final int TABLE_W = 240;
    public static final int TABLE_H = 28*5 + 20 + opad*2;
    public static final int PIECHART_W = TABLE_H;
    public static final int PIECHART_H = TABLE_H;

    public static final int LEFT_WALL = 320;
    public static final int Right_WALL = 720;

    public GlobalCommodityFlow(UIPanelAPI parent, int width, int height) {
        super(parent, width, height);

        buildUI();
    }

    public void buildUI() {
        final SectorAPI sector = Global.getSector();
        final EconomyEngine engine = EconomyEngine.instance();
        final CommoditySpecAPI com = CommoditySelectionPanel.selectedCom;
        final String comID = com.getId();
        final List<FactionAPI> factionList = sector.getAllFactions();
        final CommodityDomain dom = engine.getComDomain(comID);

        clearChildren();

        final ComIconPanel comIcon = new ComIconPanel(
            m_panel, ICON_SIZE, ICON_SIZE, null, null, com,
            sector.getPlayerFaction().getFactionSpec()
        );
        add(comIcon).inTL((LABEL_W*2 - ICON_SIZE) / 2f, 0);

        { // Total global production
        final TextPanel textPanel = new TextPanel(m_panel, LABEL_W, LABEL_H) {

            public void buildUI() {
                final long value = engine.info.getGlobalProduction(comID);
                final String txt = str("uiGlobalProdTitle");
                final String valueTxt = value < 1 ? "---" : NumFormat.engNotate(value);

                ComponentFactory.addCaptionValueBlock(m_panel, txt, valueTxt, base, LABEL_W);
            }

            {
                tooltip.width = 460f;
                tooltip.builder = (tp, exp) -> {
                    tp.addPara(str("uiGlobalProdTpTxt"),
                        pad, highlight, com.getName()
                    );
                };
            }
        };

        add(textPanel).inTL(0, ICON_SIZE + pad*3);
        }

        { // Total global demand
        final TextPanel textPanel = new TextPanel(m_panel, LABEL_W, LABEL_H) {

            public void buildUI() {
                final long value = engine.info.getGlobalDemand(comID);
                final String txt = str("uiGlobalDemandTitle");
                final String valueTxt = value < 1 ? "---" : NumFormat.engNotate(value);

                ComponentFactory.addCaptionValueBlock(m_panel, txt, valueTxt, base, LABEL_W);
            }

            {
                tooltip.width = 460f;
                tooltip.builder = (tp, exp) -> {
                    tp.addPara(str("uiGlobalDemandTpTxt"),
                        pad, highlight, com.getName()
                    );
                };
            }
        };

        add(textPanel).inTL(LABEL_W, ICON_SIZE + pad*3);
        }

        { // Total global surplus
        final TextPanel textPanel = new TextPanel(m_panel, LABEL_W, LABEL_H) {

            public void buildUI() {
                final long value = engine.info.getGlobalSurplus(comID);
                final String txt = str("uiGlobalSurplusTitle");
                final String valueTxt = value < 1 ? "---" : NumFormat.engNotate(value);

                ComponentFactory.addCaptionValueBlock(m_panel, txt, valueTxt, base, LABEL_W);
            }

            {
                tooltip.width = 460f;
                tooltip.builder = (tp, exp) -> {
                    tp.addPara(str("uiGlobalSurplusTpTxt"),
                        pad, highlight, com.getName()
                    );
                };
            }
        };

        add(textPanel).inTL(0, ICON_SIZE + LABEL_H + pad*4);
        }

        { // Total global deficit
        final TextPanel textPanel = new TextPanel(m_panel, LABEL_W, LABEL_H) {

            public void buildUI() {
                final long value = engine.info.getGlobalDeficit(comID);
                final String txt = str("uiGlobalDeficitTitle");
                final String valueTxt = value < 1 ? "---" : NumFormat.engNotate(value);

                ComponentFactory.addCaptionValueBlock(m_panel, txt, valueTxt, base, LABEL_W);
            }

            {
                tooltip.width = 460f;
                tooltip.builder = (tp, exp) -> {
                    tp.addPara(str("uiGlobalDeficitTpTxt"), pad);
                };
            }
        };

        add(textPanel).inTL(LABEL_W, ICON_SIZE + LABEL_H + pad*4);
        }

        final int largeLabelShift = 30;
        { // Total trade volume (units)
        final TextPanel textPanel = new TextPanel(m_panel, LABEL_W + largeLabelShift, LABEL_H) {

            public void buildUI() {
                final float value = dom.getTradeVolumeHistory();
                final String txt = str("uiSectorTradeVolumeTitle");
                final String valueTxt = value < 1 ? "---" : NumFormat.engNotate(value);

                ComponentFactory.addCaptionValueBlock(m_panel, txt, valueTxt, base, LABEL_W + largeLabelShift);
            }

            {
                tooltip.width = 460f;
                tooltip.builder = (tp, exp) -> {
                    tp.addPara(str("uiSectorTradeVolumeTpTxt"),pad, new Color[] {highlight, base},
                        com.getName(), Integer.toString(EconConfig.HISTORY_LENGTH)
                    );
                };
            }
        };

        add(textPanel).inTL(LEFT_WALL - largeLabelShift/2f, pad);
        }

        { // Total trade value (credits)
        final TextPanel textPanel = new TextPanel(m_panel, LABEL_W + largeLabelShift, LABEL_H) {

            public void buildUI() {
                final long value = dom.getCreditActivityHistory();
                final String txt = str("uiSectorTradeValueTitle");
                final String valueTxt = value < 1 ? "---" : NumFormat.formatCredit(value);

                ComponentFactory.addCaptionValueBlock(m_panel, txt, valueTxt, base, LABEL_W + largeLabelShift);
            }

            {
                tooltip.width = 460f;
                tooltip.builder = (tp, exp) -> {
                    tp.addPara(str("uiSectorTradeValueTpTxt"),
                        pad, new Color[] {highlight, base}, com.getName(), Integer.toString(EconConfig.HISTORY_LENGTH)
                    );
                };
            }
        };

        add(textPanel).inTL(Right_WALL - LABEL_W - largeLabelShift/2f, pad);
        }

        { // Average sector price
        final TextPanel textPanel = new TextPanel(m_panel, LABEL_W, LABEL_H) {

            public void buildUI() {
                final int value = (int) engine.info.getGlobalAveragePrice(comID, 0);
                final String txt = str("uiGlobalAvgPriceTitle");
                final String valueTxt = com.isExotic() ? "Localized" : NumFormat.formatCredit(value);

                ComponentFactory.addCaptionValueBlock(m_panel, txt, valueTxt, base, LABEL_W);
            }

            {
                tooltip.width = 460f;
                tooltip.builder = (tp, exp) -> {
                    tp.addPara(str("uiGlobalAvgPriceTpTxt"), pad, highlight, com.getName());
                };
            }
        };

        add(textPanel).inTL(LEFT_WALL, pad + LABEL_H);
        }

        { // Trade volatility (month-over-month volume change)
        final TextPanel textPanel = new TextPanel(m_panel, LABEL_W, LABEL_H) {

            public void buildUI() {
                final float value = dom.getTradeVolatility();
                final String txt = str("uiSectorTradeVolatilityTitle");
                final String valueTxt = (int) (value * 100f) + "%";

                Color volatilityColor;
                if (value <= 0.1f) volatilityColor = Color.GREEN;
                else if (value <= 0.3f) volatilityColor = Color.YELLOW;
                else if (value <= 0.5f) volatilityColor = Color.ORANGE;
                else volatilityColor = Color.RED;

                ComponentFactory.addCaptionValueBlock(m_panel, txt, valueTxt,
                    base, volatilityColor, LABEL_W
                );
            }

            {
                tooltip.width = 460f;
                tooltip.builder = (tp, exp) -> {
                    tp.addPara(str("uiSectorTradeVolatilityTpTxt"), pad, new Color[] {base, highlight},
                        Integer.toString(EconConfig.HISTORY_LENGTH), com.getName()
                    );
                };
            }
        };

        add(textPanel).inTL(Right_WALL - LABEL_W, pad + LABEL_H);
        }

        { // Global stockpiles
        final TextPanel textPanel = new TextPanel(m_panel, LABEL_W, LABEL_H) {

            public void buildUI() {
                final long value = engine.info.getGlobalStockpiles(comID);
                final String valueTxt = NumFormat.engNotate(value);
                final String txt = str("uiGlobalStockpilesTitle");

                ComponentFactory.addCaptionValueBlock(m_panel, txt, valueTxt, base, base, LABEL_W);
            }

            {
                tooltip.width = 460f;
                tooltip.builder = (tp, exp) -> {
                    tp.addPara(str("uiGlobalStockpilesTpTxt"), pad, highlight, com.getName());
                };
            }
        };

        add(textPanel).inTL(LEFT_WALL, pad + LABEL_H*2);
        }

        { // Worker allocation (total workers producing it)
        final TextPanel textPanel = new TextPanel(m_panel, LABEL_W, LABEL_H) {

            public void buildUI() {
                long value = 0;
                for (WorkerIndustryData data : WorkerRegistry.instance().getRegistry()) {
                    value += data.getAssignedForOutput(comID);
                }
                final String txt = str("uiGlobalWorkersAllocatedTitle");
                final String valueTxt = value < 0l ? "---" : NumFormat.engNotate(value);

                ComponentFactory.addCaptionValueBlock(m_panel, txt, valueTxt, base, base, LABEL_W);
            }

            {
                tooltip.width = 460f;
                tooltip.builder = (tp, exp) -> {
                    tp.addPara(str("uiGlobalWorkersAllocatedTpTxt"), pad, highlight, com.getName());
                };
            }
        };

        add(textPanel).inTL(Right_WALL - LABEL_W, pad + LABEL_H*2);
        }

        { // Number of exporting markets
        final TextPanel textPanel = new TextPanel(m_panel, LABEL_W, LABEL_H) {

            public void buildUI() {
                final long value = engine.info.getGlobalExporterCount(comID);
                final String txt = str("uiGlobalExportersTitle");
                final String valueTxt = value < 1 ? "---" : NumFormat.engNotate(value);

                ComponentFactory.addCaptionValueBlock(m_panel, txt, valueTxt, base, LABEL_W);
            }

            {
                tooltip.width = 460f;
                tooltip.builder = (tp, exp) -> {
                    tp.addPara(str("uiGlobalExportersTpTxt"), pad, highlight, com.getName());
                };
            }
        };

        add(textPanel).inTL(Right_WALL + LABEL_W, pad);
        }

        { // Number of importing markets
        final TextPanel textPanel = new TextPanel(m_panel, LABEL_W, LABEL_H) {

            public void buildUI() {
                final long value = engine.info.getGlobalImporterCount(comID);
                final String txt = str("uiGlobalImportersTitle");
                final String valueTxt = value < 1 ? "---" : NumFormat.engNotate(value);

                ComponentFactory.addCaptionValueBlock(m_panel, txt, valueTxt, base, LABEL_W);
            }

            {
                tooltip.width = 460f;
                tooltip.builder = (tp, exp) -> {
                    tp.addPara(str("uiGlobalImportersTpTxt"), pad, highlight, com.getName());
                };
            }
        };

        add(textPanel).inTL(Right_WALL + LABEL_W, pad + LABEL_H);
        }

        { // Informal production
        final TextPanel textPanel = new TextPanel(m_panel, LABEL_W, LABEL_H) {

            public void buildUI() {
                final float value = dom.getInformalNode().prod;
                final String txt = str("uiInformalProdTitle");
                final String valueTxt = value < 1 ? "---" : NumFormat.engNotate(value);

                ComponentFactory.addCaptionValueBlock(m_panel, txt, valueTxt, base, LABEL_W);
            }

            {
                tooltip.width = 460f;
                tooltip.builder = (tp, exp) -> {
                    tp.addPara(str("uiInformalProdTpTxt"), pad, highlight, com.getName());
                };
            }
        };

        add(textPanel).inTL(Right_WALL + LABEL_W, pad + LABEL_H*2);
        }

        final TooltipBuilder tableTp = (tp, exp) -> {
            tp.addPara(str("uiCtrlClickSetCourseTxt"), pad, highlight, new String[]{
                str("uiCtrlTxt"), str("uiClickTxt")
            });
        };
        { // Top 5 producers
        final SortableTable table = new SortableTable(m_panel, TABLE_W, TABLE_H);

        table.addHeaders(
            "", 40, null, true, false, 1, // Icon header
            str("uiTableColony"), 100, str("uiTableColonyNameTitle"), true, true, 1,
            str("uiTableProductionTitle"), 100, strf("uiTableDailyProdTitle", com.getName()), false, false, -1
        );

        final ArrayList<CommodityCell> producers = dom.getSortedByProduction(5);

        for (CommodityCell cell : producers) {

            final String iconPath = cell.market.getFaction().getCrest();
            final Base iconPanel = new Base(table.getPanel(), 24, 24, iconPath, null, null);
            final Color textColor = cell.market.getFaction().getBaseUIColor();
            final float value = cell.getProduction(true);

            table.addCell(iconPanel, cellAlg.LEFTPAD, null, null);
            table.addCell(cell.market.getName(), cellAlg.LEFT, null, textColor);
            table.addCell(NumFormat.engNotate(value), cellAlg.MID, value, textColor);

            final ClickHandler<RowPanel> run = (row, isLeftClick) -> {
                if (!NativeUiUtils.isCtrlDown()) return;

                final SectorEntityToken target = cell.market.getPrimaryEntity();
                if (target != null) {
                    Global.getSector().layInCourseFor(target);
                }
            };

            table.pushRow(
                null, tableTp, run, null, null, null 
            );
        }

        table.showSortIcon = false;
        table.sortingEnabled = false;
        add(table).inBL(opad*2, TABLE_H + LABEL_H / 2f);
        table.buildUI();

        final LabelAPI label = settings.createLabel(str("uiTop5ProdTitle"), Fonts.ORBITRON_16);
        final float labelW = label.computeTextWidth(label.getText());
        label.setColor(base);
        add(label).inBL(opad*2 + (TABLE_W - labelW) / 2f, TABLE_H*2 + LABEL_H / 2f + pad*2);
        }

        { // Top 5 consumers
        final SortableTable table = new SortableTable(getParent(), TABLE_W, TABLE_H);

        table.addHeaders(
            "", 40, null, true, false, 1, // Icon header
            str("uiTableColony"), 100, str("uiTableColonyNameTitle"), true, true, 1,
            str("uiTableDemandTitle"), 100, strf("uiTableDailyConsumptionTitle", com.getName()), false, false, -1
        );

        final ArrayList<CommodityCell> consumers = dom.getSortedByTargetQuantum(5);

        for (CommodityCell cell : consumers) {

            final String iconPath = cell.market.getFaction().getCrest();
            final Base iconPanel = new Base(table.getPanel(), 24, 24, iconPath, null, null);
            final Color textColor = cell.market.getFaction().getBaseUIColor();
            final float value = cell.getConsumption(true);

            table.addCell(iconPanel, cellAlg.LEFTPAD, null, null);
            table.addCell(cell.market.getName(), cellAlg.LEFT, null, textColor);
            table.addCell(NumFormat.engNotate(value), cellAlg.MID, value, textColor);

            final ClickHandler<RowPanel> run = (row, isLeftClick) -> {
                if (!NativeUiUtils.isCtrlDown()) return;

                final SectorEntityToken target = cell.market.getPrimaryEntity();
                if (target != null) {
                    Global.getSector().layInCourseFor(target);
                }
            };

            table.pushRow(
                null, tableTp, run, null, null, null 
            );
        }

        table.showSortIcon = false;
        table.sortingEnabled = false;
        add(table).inBL(opad*2, 0);
        table.buildUI();

        final LabelAPI label = settings.createLabel(str("uiTop5ConsumptionTitle"), Fonts.ORBITRON_16);
        final float labelW = label.computeTextWidth(label.getText());
        label.setColor(base);
        add(label).inBL(opad*2 + (TABLE_W - labelW) / 2f, TABLE_H + pad*2);
        }
      
        { // Global export share per faction (percent)
        final ArrayList<PieSlice> data = new ArrayList<>();
        final double globalExports = engine.info.getGlobalExports(comID);

        if (globalExports > 0) {
            for (FactionAPI faction : factionList) {
                final double factionFormalExports = engine.info.getFactionFormalGlobalExports(comID, faction.getId());
                final float share = (float) (factionFormalExports / globalExports);
                if (share >= PIE_CHART_THRESHOLD) {
                    data.add(new PieSlice(faction.getId(), faction.getBaseUIColor(), share));
                }
            }

            final double informalExports = dom.getInformalExports().values().stream().mapToDouble(d -> d).sum();
            final float informalShare = (float) (informalExports / globalExports);
            if (informalShare >= PIE_CHART_THRESHOLD) {
                data.add(new PieSlice(null, UIColors.INFORMAL_SECTOR, informalShare));
            }
        }

        final PieChart chart = new PieChart(m_panel, PIECHART_W, PIECHART_H, data);
        add(chart).inBL(360, pad);

        chart.tooltip.builder = (tp, exp) -> {
            tp.addTitle(str("uiGlobalExportShareFactionTitle"), base);

            tp.addPara(str("uiGlobalExportShareFactionTpTxt"), pad);

            tp.beginTable(
                base, dark, highlight, 20, true, true, new Object[] {
                    str("uiTableFaction"), 200, str("uiTableShareTitle"), 100
                }
            );

            for (PieSlice slice : data) {
                String label = (slice.color == UIColors.INFORMAL_SECTOR)
                    ? str("uiInformalSectorTitle")
                    : Global.getSector().getFaction(slice.uniqueID).getDisplayName();

                tp.addRow(new Object[] {
                    slice.color,
                    label,
                    highlight,
                    (int) (slice.fraction * 100) + "%"
                });
            }
            tp.addTable("", 0, opad);
        };

        final LabelAPI label = settings.createLabel(str("uiExportShareTitle"), Fonts.ORBITRON_16);
        final float labelW = label.computeTextWidth(label.getText());
        label.setColor(base);
        add(label).inBL(360 + (PIECHART_W - labelW) / 2f, PIECHART_H + pad * 2);
        }

        { // Global import share per faction (percent)
        final ArrayList<PieSlice> data = new ArrayList<>();
        final double globalImports = engine.info.getGlobalImports(comID);

        if (globalImports > 0d) {
            for (FactionAPI faction : factionList) {
                final double factionFormalImports = engine.info.getFactionFormalGlobalImports(comID, faction.getId());
                final float share = (float) (factionFormalImports / globalImports);
                if (share >= PIE_CHART_THRESHOLD) {
                    data.add(new PieSlice(faction.getId(), faction.getBaseUIColor(), share));
                }
            }
    
            final double informalImports = dom.getInformalImports().values().stream().mapToDouble(d -> d).sum();
            final float informalShare = (float) (informalImports / globalImports);
            if (informalShare >= PIE_CHART_THRESHOLD) {
                data.add(new PieSlice(null, UIColors.INFORMAL_SECTOR, informalShare));
            }
        }

        final PieChart chart = new PieChart(m_panel, PIECHART_W, PIECHART_H, data);
        add(chart).inBL(580, pad);

        chart.tooltip.builder = (tp, exp) -> {
            tp.addTitle(str("uiGlobalImportShareFactionTitle"), base);
            
            tp.addPara(str("uiGlobalImportShareFactionTpTxt"), pad);

            tp.beginTable(
                base, dark, highlight, 20, true, true, new Object[] {
                    str("uiTableFaction"), 200, str("uiTableShareTitle"), 100
                }
            );

            for (PieSlice slice : data) {
                String label = (slice.color == UIColors.INFORMAL_SECTOR)
                    ? str("uiInformalSectorTitle")
                    : Global.getSector().getFaction(slice.uniqueID).getDisplayName();

                tp.addRow(new Object[] {
                    slice.color,
                    label,
                    highlight,
                    (int) (slice.fraction * 100) + "%"
                });
            }
            tp.addTable("", 0, opad);
        };

        final LabelAPI label = settings.createLabel(str("uiImportShareTitle"), Fonts.ORBITRON_16);
        final float labelW = label.computeTextWidth(label.getText());
        label.setColor(base);
        add(label).inBL(580 + (PIECHART_W - labelW) / 2f, PIECHART_H + pad*2);
        }

        { // Trade breakdown (percent)
        final ArrayList<PieSlice> data = new ArrayList<>();
        final double globalExports = engine.info.getGlobalExports(comID);
        final double inFactionExports = engine.info.getInFactionExports(comID);
        final double informalExports = engine.info.getGlobalInformalExports(comID);
        final double total = globalExports + inFactionExports + informalExports;
        final float globalTradeShare = (float) (globalExports / total);
        final float inFactionTradeShare = (float) (inFactionExports / total);
        final float informalTradeShare = (float) (informalExports / total);
            
        data.add(new PieSlice(
            null,
            UIColors.COM_IMPORT,
            globalTradeShare
        ));
        data.add(new PieSlice(
            null,
            UIColors.IN_FACTION,
            inFactionTradeShare
        ));
        data.add(new PieSlice(
            null,
            UIColors.INFORMAL_SECTOR,
            informalTradeShare
        ));

        final PieChart chart = new PieChart(m_panel, PIECHART_W, PIECHART_H, data);
        add(chart).inBL(800, pad);

        chart.tooltip.builder = (tp, exp) -> {
            tp.setParaFont(Fonts.ORBITRON_12);
            tp.setParaFontColor(base);
            tp.addPara(str("uiInFactionTradeRatioTitle"), pad);
            tp.setParaFontDefault();
            tp.setParaFontColor(text_color);
            tp.addPara(str("uiInFactionTradeRatioTpTxt"), pad,
                highlight, ((int)(globalTradeShare*100)) + "%", ((int)((1f-globalTradeShare)*100)) + "%"
            );
        };

        final LabelAPI label = settings.createLabel(str("uiInFactionTradeRatioLblTitle"), Fonts.ORBITRON_16);
        final float labelW = label.computeTextWidth(label.getText());
        label.setColor(base);
        add(label).inBL(800 + (PIECHART_W - labelW) / 2f, PIECHART_H + pad*2);
        }
    }
}
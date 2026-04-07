package wfg.ltv_econ.ui.economyTab;

import java.awt.Color;
import java.util.List;

import org.lwjgl.input.Keyboard;

import java.util.ArrayList;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.SettingsAPI;
import com.fs.starfarer.api.campaign.FactionSpecAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.econ.CommoditySpecAPI;
import com.fs.starfarer.api.ui.Fonts;
import com.fs.starfarer.api.ui.LabelAPI;
import com.fs.starfarer.api.ui.UIPanelAPI;

import wfg.ltv_econ.config.EconomyConfig;
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
import wfg.native_ui.util.NumFormat;
import static wfg.native_ui.util.UIConstants.*;

public class GlobalCommodityFlow extends CustomPanel<GlobalCommodityFlow> implements UIBuildableAPI {
    private static final SettingsAPI settings = Global.getSettings();
    private static final List<FactionSpecAPI> factionList = settings.getAllFactionSpecs();
    private static final float PIE_CHART_THRESHOLD = 0.001f;

    public static final int ICON_SIZE = 135;
    public static final int LABEL_W = 150;
    public static final int LABEL_H = 50;
    public static final int TABLE_W = 240;
    public static final int TABLE_H = 28*5 + 20 + opad*3;
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
                final long value = engine.info.getGlobalProduction(comID)
                    + (long) dom.getInformalNode().prod;
                final String txt = "Global production";
                String valueTxt = NumFormat.engNotation(value);
                if (value < 1) valueTxt = "---";

                ComponentFactory.addCaptionValueBlock(m_panel, txt, valueTxt, base, LABEL_W);
            }

            {
                tooltip.width = 460f;
                tooltip.builder = (tp, exp) -> {
                    tp.addPara(
                        "The combined daily output of %s across all colonies and the informals in the Sector. " +
                        "Represents active industrial and informal production, excluding existing stockpiles.",
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
                final String txt = "Global demand";
                String valueTxt = NumFormat.engNotation(value);
                if (value < 1) valueTxt = "---";

                ComponentFactory.addCaptionValueBlock(m_panel, txt, valueTxt, base, LABEL_W);
            }

            {
                tooltip.width = 460f;
                tooltip.builder = (tp, exp) -> {
                    tp.addPara(
                        "The combined daily demand of all colonies for %s. " +
                        "Demand reflects how much the sector needs to maintain standard production, growth, and stability.",
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
                final String txt = "Global surplus";
                String valueTxt = NumFormat.engNotation(value);
                if (value < 1) valueTxt = "---";

                ComponentFactory.addCaptionValueBlock(m_panel, txt, valueTxt, base, LABEL_W);
            }

            {
                tooltip.width = 460f;
                tooltip.builder = (tp, exp) -> {
                    tp.addPara(
                        "The amount of %s that the sector produced beyond what was demanded. " +
                        "A higher surplus means that, even after all importing markets had their needs filled, some production still remained unused.",
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
                final String txt = "Global deficit";
                String valueTxt = NumFormat.engNotation(value);
                if (value < 1) valueTxt = "---";

                ComponentFactory.addCaptionValueBlock(m_panel, txt, valueTxt, base, LABEL_W);
            }

            {
                tooltip.width = 460f;
                tooltip.builder = (tp, exp) -> {
                    tp.addPara(
                        "Global deficit represents the total amount of demand that remained unfulfilled on the previous day. " +
                        "This value does not track shortages in stockpiles and only measures demand that was not supplied on the previous day. " +
                        "A colony may have large reserves and still contribute to the global deficit if trade routes could not deliver enough units in time.",
                        pad
                    );
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
                final String txt = "Sector-wide trade volume";
                String valueTxt = NumFormat.engNotation(value);
                if (value < 1) valueTxt = "---";

                ComponentFactory.addCaptionValueBlock(m_panel, txt, valueTxt, base, LABEL_W + largeLabelShift);
            }

            {
                tooltip.width = 460f;
                tooltip.builder = (tp, exp) -> {
                    tp.addPara(
                        "Total units of %s traded across the sector for the last %s days, including both in-faction and global cargo.",
                        pad, new Color[] {highlight, base}, com.getName(), Integer.toString(EconomyConfig.HISTORY_LENGTH)
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
                final String txt = "Sector-wide trade value";
                String valueTxt = NumFormat.formatCredit(value);
                if (value < 1) valueTxt = "---";

                ComponentFactory.addCaptionValueBlock(m_panel, txt, valueTxt, base, LABEL_W + largeLabelShift);
            }

            {
                tooltip.width = 460f;
                tooltip.builder = (tp, exp) -> {
                    tp.addPara(
                        "The total monetary value (in credits) of all %s trades across the entire sector for the last %s days. This includes both in-faction and global trade, calculated using the prices at which commodities were exchanged.",
                        pad, new Color[] {highlight, base}, com.getName(), Integer.toString(EconomyConfig.HISTORY_LENGTH)
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
                final String txt = "Global average price";
                final String valueTxt = com.isExotic() ? "Localized"
                    : NumFormat.formatCredit(value);

                ComponentFactory.addCaptionValueBlock(m_panel, txt, valueTxt, base, LABEL_W);
            }

            {
                tooltip.width = 460f;
                tooltip.builder = (tp, exp) -> {
                    tp.addPara(
                        "The average price of %s across all markets in the sector on the previous day, weighted by the quantities traded. This provides a sector-wide benchmark price, reflecting both in-faction and out-of-faction transactions.",
                        pad, highlight, com.getName()
                    );
                };
            }
        };

        add(textPanel).inTL(LEFT_WALL, pad + LABEL_H);
        }

        { // Trade volatility (month-over-month volume change)
        final TextPanel textPanel = new TextPanel(m_panel, LABEL_W, LABEL_H) {

            public void buildUI() {
                final float value = dom.getTradeVolatility();
                final String txt = "Trade volatility";
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
                    tp.addPara(
                        "Trade Volatility for the last %s days. " +
                        "Indicates how much the daily export volume for %s fluctuates relative to its average.",
                        pad,
                        new Color[] {base, highlight},
                        Integer.toString(EconomyConfig.HISTORY_LENGTH),
                        com.getName()
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
                final String valueTxt = NumFormat.engNotation(value);
                final String txt = "Global stockpiles";

                ComponentFactory.addCaptionValueBlock(m_panel, txt, valueTxt, base, base, LABEL_W);
            }

            {
                tooltip.width = 460f;
                tooltip.builder = (tp, exp) -> {
                    tp.addPara(
                        "Shows the total amount of %s currently stored across all markets in the sector. " +
                        "This value reflects available stock and does not account for daily production or consumption. " +
                        "High stockpiles indicate abundance, while low stockpiles signal scarcity and potential trade opportunities.",
                        pad, highlight, com.getName()
                    );
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
                final String txt = "Worker allocation";
                final String valueTxt = NumFormat.engNotation(value);

                ComponentFactory.addCaptionValueBlock(m_panel, txt, valueTxt, base, base, LABEL_W);
            }

            {
                tooltip.width = 460f;
                tooltip.builder = (tp, exp) -> {
                    tp.addPara(
                        "Displays the total number of workers currently assigned to producing %s across all markets in the sector ."+
                        "Workers are counted based on the output of industries producing this commodity, not the industry as a whole.",
                        pad, highlight, com.getName()
                    );
                };
            }
        };

        add(textPanel).inTL(Right_WALL - LABEL_W, pad + LABEL_H*2);
        }

        { // Number of exporting markets
        final TextPanel textPanel = new TextPanel(m_panel, LABEL_W, LABEL_H) {

            public void buildUI() {
                final long value = dom.getExporters().size();
                final String txt = "Global Exporters";
                final String valueTxt = value < 1 ? "---" : NumFormat.engNotation(value);

                ComponentFactory.addCaptionValueBlock(m_panel, txt, valueTxt, base, LABEL_W);
            }

            {
                tooltip.width = 460f;
                tooltip.builder = (tp, exp) -> {
                    tp.addPara(
                        "The total count of markets in the sector that exported %s on the previous day. Only markets that actually sent units to other markets are included, regardless of faction.",
                        pad, highlight, com.getName()
                    );
                };
            }
        };

        add(textPanel).inTL(Right_WALL + LABEL_W, pad);
        }

        { // Number of importing markets
        final TextPanel textPanel = new TextPanel(m_panel, LABEL_W, LABEL_H) {

            public void buildUI() {
                final long value = dom.getImporters().size();
                final String txt = "Global Importers";
                final String valueTxt = value < 1 ? "---" : NumFormat.engNotation(value);

                ComponentFactory.addCaptionValueBlock(m_panel, txt, valueTxt, base, LABEL_W);
            }

            {
                tooltip.width = 460f;
                tooltip.builder = (tp, exp) -> {
                    tp.addPara(
                        "The total count of markets in the sector that imported %s on the previous day. Only markets that actually received units from other markets are included, regardless of faction.",
                        pad, highlight, com.getName()
                    );
                };
            }
        };

        add(textPanel).inTL(Right_WALL + LABEL_W, pad + LABEL_H);
        }

        { // Informal production
        final TextPanel textPanel = new TextPanel(m_panel, LABEL_W, LABEL_H) {

            public void buildUI() {
                final float value = dom.getInformalNode().prod;
                final String txt = "Informal Production";
                final String valueTxt = value < 1 ? "---" : NumFormat.engNotation(value);

                ComponentFactory.addCaptionValueBlock(m_panel, txt, valueTxt, base, LABEL_W);
            }

            {
                tooltip.width = 460f;
                tooltip.builder = (tp, exp) -> {
                    tp.addPara(
                        "The amount of %s produced by the informal sector on the previous day.",
                        pad, highlight, com.getName()
                    );
                };
            }
        };

        add(textPanel).inTL(Right_WALL + LABEL_W, pad + LABEL_H*2);
        }

        final TooltipBuilder tableTp = (tp, exp) -> {
            tp.addPara("Ctrl + Click to set course", pad, highlight, new String[]{
                "Ctrl", "Click"
            });
        };
        { // Top 5 producers
        final SortableTable table = new SortableTable(m_panel, TABLE_W, TABLE_H);

        table.addHeaders(
            "", 40, null, true, false, 1, // Icon header
            "Colony", 100, "Colony name", true, true, 1,
            "Production", 100, "Daily units of " + com.getName() + " produced", false, false, -1
        );

        final ArrayList<CommodityCell> producers = dom.getSortedByProduction(5);

        for (CommodityCell cell : producers) {

            final String iconPath = cell.market.getFaction().getCrest();
            final Base iconPanel = new Base(table.getPanel(), 24, 24, iconPath, null, null);
            final Color textColor = cell.market.getFaction().getBaseUIColor();
            final long value = (long) cell.getProduction(true);

            table.addCell(iconPanel, cellAlg.LEFTPAD, null, null);
            table.addCell(cell.market.getName(), cellAlg.LEFT, null, textColor);
            table.addCell(NumFormat.engNotation(value), cellAlg.MID, value, textColor);

            final ClickHandler<RowPanel> run = (row, isLeftClick) -> {
                if (!Keyboard.isKeyDown(Keyboard.KEY_LCONTROL)&&!Keyboard.isKeyDown(Keyboard.KEY_RCONTROL)) {
                    return;
                }

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

        final LabelAPI label = settings.createLabel("Top 5 producers", Fonts.ORBITRON_16);
        final float labelW = label.computeTextWidth(label.getText());
        label.setColor(base);
        add(label).inBL(opad*2 + (TABLE_W - labelW) / 2f, TABLE_H*2 + LABEL_H / 2f + pad*2);
        }

        { // Top 5 consumers
        final SortableTable table = new SortableTable(getParent(), TABLE_W, TABLE_H);

        table.addHeaders(
            "", 40, null, true, false, 1, // Icon header
            "Colony", 100, "Colony name", true, true, 1,
            "Demand", 100, "Daily units of " + com.getName() + " consumed", false, false, -1
        );

        final ArrayList<CommodityCell> consumers = dom.getSortedByTargetQuantum(5);

        for (CommodityCell cell : consumers) {

            final String iconPath = cell.market.getFaction().getCrest();
            final Base iconPanel = new Base(table.getPanel(), 24, 24, iconPath, null, null);
            final Color textColor = cell.market.getFaction().getBaseUIColor();
            final long value = (long) cell.getConsumption(true);

            table.addCell(iconPanel, cellAlg.LEFTPAD, null, null);
            table.addCell(cell.market.getName(), cellAlg.LEFT, null, textColor);
            table.addCell(NumFormat.engNotation(value), cellAlg.MID, value, textColor);

            final ClickHandler<RowPanel> run = (row, isLeftClick) -> {
                if (!Keyboard.isKeyDown(Keyboard.KEY_LCONTROL)&&!Keyboard.isKeyDown(Keyboard.KEY_RCONTROL)) {
                    return;
                }

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

        final LabelAPI label = settings.createLabel("Top 5 consumers", Fonts.ORBITRON_16);
        final float labelW = label.computeTextWidth(label.getText());
        label.setColor(base);
        add(label).inBL(opad*2 + (TABLE_W - labelW) / 2f, TABLE_H + pad*2);
        }
      
        { // Global export share per faction (percent)
        final ArrayList<PieSlice> data = new ArrayList<>(); 
        final float informalShare = engine.info.getInformalExportShare(comID);

        for (FactionSpecAPI faction : factionList) {
            final float share = engine.info.getFactionExportShareWithInformal(comID, faction.getId());
            if (share < PIE_CHART_THRESHOLD) continue;

            data.add(new PieSlice(
                null,
                faction.getBaseUIColor(),
                share
            ));
        }

        if (informalShare >= PIE_CHART_THRESHOLD) {
            data.add(new PieSlice(
                null,
                UIColors.INFORMAL_SECTOR,
                informalShare
            ));
        }

        final PieChart chart = new PieChart(m_panel, PIECHART_W, PIECHART_H, data);
        add(chart).inBL(360, pad);

        chart.tooltip.builder = (tp, exp) -> {
            tp.setParaFont(Fonts.ORBITRON_12);
            tp.setParaFontColor(base);
            tp.addPara("Global Export Share by Faction", pad);
            tp.setParaFontDefault();
            tp.setParaFontColor(text_color);
            tp.addPara(
                "Shows the percentage of total exports controlled by factions and informals. " +
                "Percentages do not include in-faction trade." +
                "Values are based on the previous day.",
                pad
            );

            tp.beginTable(
                base, dark, highlight, 20, true, true, new Object[] {
                    "Faction", 200, "Share", 100
                }
            );

            for (FactionSpecAPI faction : factionList) {
                final float share = engine.info.getFactionExportShareWithInformal(comID, faction.getId());
                if (share < PIE_CHART_THRESHOLD) continue;

                tp.addRow(new Object[] {
                    faction.getBaseUIColor(),
                    faction.getDisplayName(),
                    highlight, (int) (share * 100) + "%"
                });
            }

            if (informalShare >= PIE_CHART_THRESHOLD) {
                tp.addRow(new Object[] {
                    UIColors.INFORMAL_SECTOR,
                    "Informal Sector",
                    highlight, (int) (informalShare * 100) + "%"
                });
            }

            tp.addTable("", 0, opad);
        };

        final LabelAPI label = settings.createLabel("Export share", Fonts.ORBITRON_16);
        final float labelW = label.computeTextWidth(label.getText());
        label.setColor(base);
        add(label).inBL(360 + (PIECHART_W - labelW) / 2f, PIECHART_H + pad*2);
        }

        { // Global import share per faction (percent)
        final ArrayList<PieSlice> data = new ArrayList<>();
        final float informalShare = engine.info.getInformalImportShare(comID);

        for (FactionSpecAPI faction : factionList) {
            final float share = engine.info.getFactionImportShareWithInformal(comID, faction.getId());

            if (share < PIE_CHART_THRESHOLD) continue;

            data.add(new PieSlice(
                null,
                faction.getBaseUIColor(),
                share
            ));
        }

        if (informalShare >= PIE_CHART_THRESHOLD) {
            data.add(new PieSlice(
                null,
                UIColors.INFORMAL_SECTOR,
                informalShare
            ));
        }

        final PieChart chart = new PieChart(m_panel, PIECHART_W, PIECHART_H, data);
        add(chart).inBL(580, pad);

        chart.tooltip.builder = (tp, exp) -> {
            tp.setParaFont(Fonts.ORBITRON_12);
            tp.setParaFontColor(base);
            tp.addPara("Global Import Share by Faction", pad);
            tp.setParaFontDefault();
            tp.setParaFontColor(text_color);
            tp.addPara(
                "Shows the percentage of total imports made by factions and informals. " +
                "Percentages do not include in-faction trade." +
                "Values are based on the previous day.",
                pad
            );

            tp.beginTable(
                base, dark, highlight, 20, true, true, new Object[] {
                    "Faction", 200, "Share", 100
                }
            );

            for (FactionSpecAPI faction : factionList) {
                final float share = engine.info.getFactionImportShareWithInformal(comID, faction.getId());
                if (share < PIE_CHART_THRESHOLD) continue;

                tp.addRow(new Object[] {
                    faction.getBaseUIColor(),
                    faction.getDisplayName(),
                    highlight, (int) (share * 100) + "%"
                });
            }

            if (informalShare >= PIE_CHART_THRESHOLD) {
                tp.addRow(new Object[] {
                    UIColors.INFORMAL_SECTOR,
                    "Informal Sector",
                    highlight, (int) (informalShare * 100) + "%"
                });
            }

            tp.addTable("", 0, opad);
        };

        final LabelAPI label = settings.createLabel("Import share", Fonts.ORBITRON_16);
        final float labelW = label.computeTextWidth(label.getText());
        label.setColor(base);
        add(label).inBL(580 + (PIECHART_W - labelW) / 2f, PIECHART_H + pad*2);
        }

        { // Trade breakdown (percent)
        final ArrayList<PieSlice> data = new ArrayList<>();
        final double globalExports = engine.info.getGlobalExports(comID);
        final double inFactionExports = engine.info.getInFactionExports(comID);
        final double informalExports = dom.getInformalNode().exports;
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
            tp.addPara("Global vs In-Faction Trade Share", pad);
            tp.setParaFontDefault();
            tp.setParaFontColor(text_color);
            tp.addPara(
                "Shows the proportion of this commodity's total exports that are traded outside the producing faction (%s)" +
                " versus exports consumed within the same faction (%s). " +
                "Values are based on the previous day.",
                pad,
                highlight, ((int)(globalTradeShare*100)) + "%", ((int)((1f-globalTradeShare)*100)) + "%"
            );
        };

        final LabelAPI label = settings.createLabel("Trade Breakdown", Fonts.ORBITRON_16);
        final float labelW = label.computeTextWidth(label.getText());
        label.setColor(base);
        add(label).inBL(800 + (PIECHART_W - labelW) / 2f, PIECHART_H + pad*2);
        }
    }
}
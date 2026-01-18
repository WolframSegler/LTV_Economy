package wfg.ltv_econ.ui.panels;

import java.awt.Color;
import java.util.List;

import org.lwjgl.input.Keyboard;

import java.util.ArrayList;
import java.util.Iterator;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.SettingsAPI;
import com.fs.starfarer.api.campaign.FactionSpecAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.econ.CommoditySpecAPI;
import com.fs.starfarer.api.impl.campaign.ids.Commodities;
import com.fs.starfarer.api.ui.Alignment;
import com.fs.starfarer.api.ui.CustomPanelAPI;
import com.fs.starfarer.api.ui.Fonts;
import com.fs.starfarer.api.ui.LabelAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.ui.UIPanelAPI;
import com.fs.starfarer.api.util.Misc;

import wfg.ltv_econ.configs.EconomyConfigLoader.EconomyConfig;
import wfg.ltv_econ.economy.CommodityDomain;
import wfg.ltv_econ.economy.CommodityCell;
import wfg.ltv_econ.economy.WorkerRegistry;
import wfg.ltv_econ.economy.WorkerRegistry.WorkerIndustryData;
import wfg.ltv_econ.economy.engine.EconomyEngine;
import wfg.ltv_econ.ui.panels.reusable.ComIconPanel;
import wfg.ltv_econ.util.UiUtils;
import wfg.wrap_ui.ui.panels.CustomPanel;
import wfg.wrap_ui.ui.panels.PieChart;
import wfg.wrap_ui.ui.panels.SortableTable;
import wfg.wrap_ui.ui.panels.SortableTable.RowPanel;
import wfg.wrap_ui.ui.panels.SortableTable.cellAlg;
import wfg.wrap_ui.ui.panels.SpritePanel.Base;
import wfg.wrap_ui.ui.panels.CustomPanel.HasTooltip.PendingTooltip;
import wfg.wrap_ui.ui.panels.PieChart.PieSlice;
import wfg.wrap_ui.ui.panels.TextPanel;
import wfg.wrap_ui.ui.plugins.BasePanelPlugin;
import wfg.wrap_ui.util.CallbackRunnable;
import wfg.wrap_ui.util.NumFormat;
import wfg.wrap_ui.util.WrapUiUtils;
import static wfg.wrap_ui.util.UIConstants.*;

public class GlobalCommodityFlow extends
    CustomPanel<BasePanelPlugin<GlobalCommodityFlow>, GlobalCommodityFlow, CustomPanelAPI>
{

    public static final int ICON_SIZE = 135;
    public static final int LABEL_W = 150;
    public static final int LABEL_H = 50;
    public static final int TABLE_W = 240;
    public static final int TABLE_H = 28*5 + 20 + opad*3;
    public static final int PIECHART_W = TABLE_H;
    public static final int PIECHART_H = TABLE_H;

    public static final int LEFT_WALL = 320;
    public static final int Right_WALL = 720;

    public static CommoditySpecAPI selectedCom = Global.getSettings().getCommoditySpec(Commodities.SUPPLIES);

    public GlobalCommodityFlow(UIPanelAPI parent, int width, int height) {
        super(parent, width, height, new BasePanelPlugin<>());

        getPlugin().init(this);
        createPanel();
    }

    public void createPanel() {
        final SettingsAPI settings = Global.getSettings();
        final SectorAPI sector = Global.getSector();
        final EconomyEngine engine = EconomyEngine.getInstance();
        final String comID = selectedCom.getId();
        final CommodityDomain dom = engine.getComDomain(comID);

        clearChildren();

        final ComIconPanel comIcon = new ComIconPanel(
            getPanel(), sector.getPlayerFaction(), ICON_SIZE, ICON_SIZE,
            selectedCom.getIconName(), null, null
        );
        comIcon.setCommodity(selectedCom);
        add(comIcon).inTL((LABEL_W*2 - ICON_SIZE) / 2f, 0);

        { // Total global production
        final TextPanel textPanel = new TextPanel(getPanel(), LABEL_W, LABEL_H) {

            public void createPanel() {
                final long value = engine.info.getGlobalProduction(comID);
                final String txt = "Global production";
                String valueTxt = NumFormat.engNotation(value);
                if (value < 1) {
                    valueTxt = "---";
                }

                final LabelAPI lbl1 = settings.createLabel(txt, Fonts.ORBITRON_12);
                lbl1.setColor(base);
                lbl1.setHighlightOnMouseover(true);
                lbl1.setAlignment(Alignment.MID);

                final LabelAPI lbl2 = settings.createLabel(valueTxt, Fonts.INSIGNIA_VERY_LARGE);
                lbl2.setColor(highlight);
                lbl2.setHighlightOnMouseover(true);
                lbl2.setAlignment(Alignment.MID);

                final float textH1 = lbl1.getPosition().getHeight();

                add(lbl1).inTL(0, 0).setSize(LABEL_W, textH1);
                add(lbl2).inTL(0, textH1 + pad).setSize(LABEL_W, lbl2.getPosition().getHeight());
            }

            public CustomPanelAPI getTpParent() {
                return getPanel();
            }

            @Override
            public TooltipMakerAPI createAndAttachTp() {
                final TooltipMakerAPI tooltip = getPanel().createUIElement(460, 0, false);

                tooltip.addPara(
                    "The combined daily output of %s across all colonies in the Sector. " +
                    "Represents active industrial production, excluding existing stockpiles.",
                    pad, highlight, selectedCom.getName()
                );

                getPanel().addUIElement(tooltip);
                WrapUiUtils.mouseCornerPos(tooltip, opad);

                return tooltip;
            }
        };

        add(textPanel).inTL(0, ICON_SIZE + pad*3);
        }

        { // Total global demand
        final TextPanel textPanel = new TextPanel(getPanel(), LABEL_W, LABEL_H) {

            public void createPanel() {
                final long value = engine.info.getGlobalDemand(comID);
                final String txt = "Global demand";
                String valueTxt = NumFormat.engNotation(value);
                if (value < 1) {
                    valueTxt = "---";
                }

                final LabelAPI lbl1 = settings.createLabel(txt, Fonts.ORBITRON_12);
                lbl1.setColor(base);
                lbl1.setHighlightOnMouseover(true);
                lbl1.setAlignment(Alignment.MID);

                final LabelAPI lbl2 = settings.createLabel(valueTxt, Fonts.INSIGNIA_VERY_LARGE);
                lbl2.setColor(highlight);
                lbl2.setHighlightOnMouseover(true);
                lbl2.setAlignment(Alignment.MID);

                final float textH1 = lbl1.getPosition().getHeight();

                add(lbl1).inTL(0, 0).setSize(LABEL_W, textH1);
                add(lbl2).inTL(0, textH1 + pad).setSize(LABEL_W, lbl2.getPosition().getHeight());
            }

            public CustomPanelAPI getTpParent() {
                return getPanel();
            }

            @Override
            public TooltipMakerAPI createAndAttachTp() {
                final TooltipMakerAPI tooltip = getPanel().createUIElement(460, 0, false);

                tooltip.addPara(
                    "The combined daily demand of all colonies for %s. " +
                    "Demand reflects how much the sector needs to maintain standard production, growth, and stability.",
                    pad, highlight, selectedCom.getName()
                );

                getPanel().addUIElement(tooltip);
                WrapUiUtils.mouseCornerPos(tooltip, opad);

                return tooltip;
            }
        };

        add(textPanel).inTL(LABEL_W, ICON_SIZE + pad*3);
        }

        { // Total global surplus
        final TextPanel textPanel = new TextPanel(getPanel(), LABEL_W, LABEL_H) {

            public void createPanel() {
                final long value = engine.info.getGlobalSurplus(comID);
                final String txt = "Global surplus";
                String valueTxt = NumFormat.engNotation(value);
                if (value < 1) {
                    valueTxt = "---";
                }

                final LabelAPI lbl1 = settings.createLabel(txt, Fonts.ORBITRON_12);
                lbl1.setColor(base);
                lbl1.setHighlightOnMouseover(true);
                lbl1.setAlignment(Alignment.MID);

                final LabelAPI lbl2 = settings.createLabel(valueTxt, Fonts.INSIGNIA_VERY_LARGE);
                lbl2.setColor(highlight);
                lbl2.setHighlightOnMouseover(true);
                lbl2.setAlignment(Alignment.MID);

                final float textH1 = lbl1.getPosition().getHeight();

                add(lbl1).inTL(0, 0).setSize(LABEL_W, textH1);
                add(lbl2).inTL(0, textH1 + pad).setSize(LABEL_W, lbl2.getPosition().getHeight());
            }

            public CustomPanelAPI getTpParent() {
                return getPanel();
            }

            @Override
            public TooltipMakerAPI createAndAttachTp() {
                final TooltipMakerAPI tooltip = getPanel().createUIElement(460, 0, false);

                tooltip.addPara(
                    "The amount of %s that the sector produced beyond what was demanded. " +
                    "A higher surplus means that, even after all importing markets had their needs filled, some production still remained unused.",
                    pad, highlight, selectedCom.getName()
                );

                getPanel().addUIElement(tooltip);
                WrapUiUtils.mouseCornerPos(tooltip, opad);

                return tooltip;
            }
        };

        add(textPanel).inTL(0, ICON_SIZE + LABEL_H + pad*4);
        }

        { // Total global deficit
        final TextPanel textPanel = new TextPanel(getPanel(), LABEL_W, LABEL_H) {

            public void createPanel() {
                final long value = engine.info.getGlobalDeficit(comID);
                final String txt = "Global deficit";
                String valueTxt = NumFormat.engNotation(value);
                if (value < 1) {
                    valueTxt = "---";
                }

                final LabelAPI lbl1 = settings.createLabel(txt, Fonts.ORBITRON_12);
                lbl1.setColor(base);
                lbl1.setHighlightOnMouseover(true);
                lbl1.setAlignment(Alignment.MID);

                final LabelAPI lbl2 = settings.createLabel(valueTxt, Fonts.INSIGNIA_VERY_LARGE);
                lbl2.setColor(highlight);
                lbl2.setHighlightOnMouseover(true);
                lbl2.setAlignment(Alignment.MID);

                final float textH1 = lbl1.getPosition().getHeight();

                add(lbl1).inTL(0, 0).setSize(LABEL_W, textH1);
                add(lbl2).inTL(0, textH1 + pad).setSize(LABEL_W, lbl2.getPosition().getHeight());
            }

            public CustomPanelAPI getTpParent() {
                return getPanel();
            }

            @Override
            public TooltipMakerAPI createAndAttachTp() {
                final TooltipMakerAPI tooltip = getPanel().createUIElement(460, 0, false);

                tooltip.addPara(
                    "Global deficit represents the total amount of demand that remained unfulfilled on the previous day." +
                    "This value does not track shortages in stockpiles and only measures demand that was not supplied on the previous day." +
                    "A colony may have large reserves and still contribute to the global deficit if trade routes could not deliver enough units in time.",
                    pad
                );

                getPanel().addUIElement(tooltip);
                WrapUiUtils.mouseCornerPos(tooltip, opad);

                return tooltip;
            }
        };

        add(textPanel).inTL(LABEL_W, ICON_SIZE + LABEL_H + pad*4);
        }

        final int largeLabelShift = 30;
        { // Total trade volume (units)
        final TextPanel textPanel = new TextPanel(getPanel(), LABEL_W + largeLabelShift, LABEL_H) {

            public void createPanel() {
                final long value = engine.info.getGlobalTradeVolume(comID);
                final String txt = "Sector-wide trade volume";
                String valueTxt = NumFormat.engNotation(value);
                if (value < 1) {
                    valueTxt = "---";
                }

                final LabelAPI lbl1 = settings.createLabel(txt, Fonts.ORBITRON_12);
                lbl1.setColor(base);
                lbl1.setHighlightOnMouseover(true);
                lbl1.setAlignment(Alignment.MID);

                final LabelAPI lbl2 = settings.createLabel(valueTxt, Fonts.INSIGNIA_VERY_LARGE);
                lbl2.setColor(highlight);
                lbl2.setHighlightOnMouseover(true);
                lbl2.setAlignment(Alignment.MID);

                final float textH1 = lbl1.getPosition().getHeight();

                add(lbl1).inTL(0, 0).setSize(LABEL_W + largeLabelShift, textH1);
                add(lbl2).inTL(0, textH1 + pad).setSize(LABEL_W + largeLabelShift, lbl2.getPosition().getHeight());
            }

            public CustomPanelAPI getTpParent() {
                return getPanel();
            }

            @Override
            public TooltipMakerAPI createAndAttachTp() {
                final TooltipMakerAPI tooltip = getPanel().createUIElement(460, 0, false);

                tooltip.addPara(
                    "The total number of units of %s traded across the sector on the previous day, including both in-faction and out-of-faction transactions. This represents all actual movement of goods between markets, regardless of prices or stockpiles.",
                    pad, highlight, selectedCom.getName()
                );

                getPanel().addUIElement(tooltip);
                WrapUiUtils.mouseCornerPos(tooltip, opad);

                return tooltip;
            }
        };

        add(textPanel).inTL(LEFT_WALL - largeLabelShift/2f, pad);
        }

        { // Total trade value (credits)
        final TextPanel textPanel = new TextPanel(getPanel(), LABEL_W + largeLabelShift, LABEL_H) {

            public void createPanel() {
                final long value = dom.getMarketActivity();
                final String txt = "Sector-wide trade value";
                String valueTxt = NumFormat.formatCredit(value);
                if (value < 1) {
                    valueTxt = "---";
                }

                final LabelAPI lbl1 = settings.createLabel(txt, Fonts.ORBITRON_12);
                lbl1.setColor(base);
                lbl1.setHighlightOnMouseover(true);
                lbl1.setAlignment(Alignment.MID);

                final LabelAPI lbl2 = settings.createLabel(valueTxt, Fonts.INSIGNIA_VERY_LARGE);
                lbl2.setColor(highlight);
                lbl2.setHighlightOnMouseover(true);
                lbl2.setAlignment(Alignment.MID);

                final float textH1 = lbl1.getPosition().getHeight();

                add(lbl1).inTL(0, 0).setSize(LABEL_W + largeLabelShift, textH1);
                add(lbl2).inTL(0, textH1 + pad).setSize(LABEL_W + largeLabelShift, lbl2.getPosition().getHeight());
            }

            public CustomPanelAPI getTpParent() {
                return getPanel();
            }

            @Override
            public TooltipMakerAPI createAndAttachTp() {
                final TooltipMakerAPI tooltip = getPanel().createUIElement(460, 0, false);

                tooltip.addPara(
                    "The total monetary value (in credits) of all %s trades across the entire sector on the previous day. This includes both in-faction and out-of-faction trade, calculated using the prices at which commodities were exchanged.",
                    pad, highlight, selectedCom.getName()
                );

                getPanel().addUIElement(tooltip);
                WrapUiUtils.mouseCornerPos(tooltip, opad);

                return tooltip;
            }
        };

        add(textPanel).inTL(Right_WALL - LABEL_W - largeLabelShift/2f, pad);
        }

        { // Average sector price
        final TextPanel textPanel = new TextPanel(getPanel(), LABEL_W, LABEL_H) {

            public void createPanel() {
                final int value = (int) engine.info.getGlobalAveragePrice(comID, 0);
                final String txt = "Global average price";
                final String valueTxt = NumFormat.formatCredit(value);

                final LabelAPI lbl1 = settings.createLabel(txt, Fonts.ORBITRON_12);
                lbl1.setColor(base);
                lbl1.setHighlightOnMouseover(true);
                lbl1.setAlignment(Alignment.MID);

                final LabelAPI lbl2 = settings.createLabel(valueTxt, Fonts.INSIGNIA_VERY_LARGE);
                lbl2.setColor(highlight);
                lbl2.setHighlightOnMouseover(true);
                lbl2.setAlignment(Alignment.MID);

                final float textH1 = lbl1.getPosition().getHeight();

                add(lbl1).inTL(0, 0).setSize(LABEL_W, textH1);
                add(lbl2).inTL(0, textH1 + pad).setSize(LABEL_W, lbl2.getPosition().getHeight());
            }

            public CustomPanelAPI getTpParent() {
                return getPanel();
            }

            @Override
            public TooltipMakerAPI createAndAttachTp() {
                final TooltipMakerAPI tooltip = getPanel().createUIElement(460, 0, false);

                tooltip.addPara(
                    "The average price of %s across all markets in the sector on the previous day, weighted by the quantities traded. This provides a sector-wide benchmark price, reflecting both in-faction and out-of-faction transactions.",
                    pad, highlight, selectedCom.getName()
                );

                getPanel().addUIElement(tooltip);
                WrapUiUtils.mouseCornerPos(tooltip, opad);

                return tooltip;
            }
        };

        add(textPanel).inTL(LEFT_WALL, pad + LABEL_H);
        }

        { // Trade volatility (month-over-month volume change)
        final TextPanel textPanel = new TextPanel(getPanel(), LABEL_W, LABEL_H) {

            public void createPanel() {
                final float value = dom.getTradeVolatility();
                final String txt = "Trade volatility";
                final String valueTxt = (int) (value * 100f) + "%";

                Color volatilityColor;
                if (value <= 0.1f) volatilityColor = Color.GREEN;
                else if (value <= 0.3f) volatilityColor = Color.YELLOW;
                else if (value <= 0.5f) volatilityColor = Color.ORANGE;
                else volatilityColor = Color.RED;

                final LabelAPI lbl1 = settings.createLabel(txt, Fonts.ORBITRON_12);
                lbl1.setColor(base);
                lbl1.setHighlightOnMouseover(true);
                lbl1.setAlignment(Alignment.MID);

                final LabelAPI lbl2 = settings.createLabel(valueTxt, Fonts.INSIGNIA_VERY_LARGE);
                lbl2.setColor(volatilityColor);
                lbl2.setHighlightOnMouseover(true);
                lbl2.setAlignment(Alignment.MID);

                final float textH1 = lbl1.getPosition().getHeight();

                add(lbl1).inTL(0, 0).setSize(LABEL_W, textH1);
                add(lbl2).inTL(0, textH1 + pad).setSize(LABEL_W, lbl2.getPosition().getHeight());
            }

            public CustomPanelAPI getTpParent() {
                return getPanel();
            }

            @Override
            public TooltipMakerAPI createAndAttachTp() {
                final TooltipMakerAPI tooltip = getPanel().createUIElement(460, 0, false);

                tooltip.addPara(
                    "Trade Volatility for the last %s days. " +
                    "Indicates how much the daily export volume for %s fluctuates relative to its average.",
                    pad,
                    new Color[] {base, highlight},
                    EconomyConfig.VOLATILITY_WINDOW + "",
                    selectedCom.getName()
                );

                getPanel().addUIElement(tooltip);
                WrapUiUtils.mouseCornerPos(tooltip, opad);

                return tooltip;
            }
        };

        add(textPanel).inTL(Right_WALL - LABEL_W, pad + LABEL_H);
        }

        { // Global stockpiles
        final TextPanel textPanel = new TextPanel(getPanel(), LABEL_W, LABEL_H) {

            public void createPanel() {
                final long value = engine.info.getGlobalStockpiles(comID);
                final String valueTxt = NumFormat.engNotation(value);
                final String txt = "Global stockpiles";

                final LabelAPI lbl1 = settings.createLabel(txt, Fonts.ORBITRON_12);
                lbl1.setColor(base);
                lbl1.setHighlightOnMouseover(true);
                lbl1.setAlignment(Alignment.MID);

                final LabelAPI lbl2 = settings.createLabel(valueTxt, Fonts.INSIGNIA_VERY_LARGE);
                lbl2.setColor(base);
                lbl2.setHighlightOnMouseover(true);
                lbl2.setAlignment(Alignment.MID);

                final float textH1 = lbl1.getPosition().getHeight();

                add(lbl1).inTL(0, 0).setSize(LABEL_W, textH1);
                add(lbl2).inTL(0, textH1 + pad).setSize(LABEL_W, lbl2.getPosition().getHeight());
            }

            public CustomPanelAPI getTpParent() {
                return getPanel();
            }

            @Override
            public TooltipMakerAPI createAndAttachTp() {
                final TooltipMakerAPI tooltip = getPanel().createUIElement(460, 0, false);

                tooltip.addPara(
                    "Shows the total amount of %s currently stored across all markets in the sector. " +
                    "This value reflects available stock and does not account for daily production or consumption. " +
                    "High stockpiles indicate abundance, while low stockpiles signal scarcity and potential trade opportunities.",
                    pad, highlight, selectedCom.getName()
                );

                getPanel().addUIElement(tooltip);
                WrapUiUtils.mouseCornerPos(tooltip, opad);

                return tooltip;
            }
        };

        add(textPanel).inTL(LEFT_WALL, pad + LABEL_H*2);
        }

        { // Worker allocation (total workers producing it)
        final TextPanel textPanel = new TextPanel(getPanel(), LABEL_W, LABEL_H) {

            public void createPanel() {
                long value = 0;
                for (WorkerIndustryData data : WorkerRegistry.getInstance().getRegistry()) {
                    value += data.getAssignedForOutput(comID);
                }
                final String txt = "Worker allocation";
                final String valueTxt = NumFormat.engNotation(value);

                final LabelAPI lbl1 = settings.createLabel(txt, Fonts.ORBITRON_12);
                lbl1.setColor(base);
                lbl1.setHighlightOnMouseover(true);
                lbl1.setAlignment(Alignment.MID);

                final LabelAPI lbl2 = settings.createLabel(valueTxt, Fonts.INSIGNIA_VERY_LARGE);
                lbl2.setColor(base);
                lbl2.setHighlightOnMouseover(true);
                lbl2.setAlignment(Alignment.MID);

                final float textH1 = lbl1.getPosition().getHeight();

                add(lbl1).inTL(0, 0).setSize(LABEL_W, textH1);
                add(lbl2).inTL(0, textH1 + pad).setSize(LABEL_W, lbl2.getPosition().getHeight());
            }

            public CustomPanelAPI getTpParent() {
                return getPanel();
            }

            @Override
            public TooltipMakerAPI createAndAttachTp() {
                final TooltipMakerAPI tooltip = getPanel().createUIElement(460, 0, false);

                tooltip.addPara(
                    "Displays the total number of workers currently assigned to producing %s across all markets in the sector ."+
                    "Workers are counted based on the output of industries producing this commodity, not the industry as a whole.",
                    pad, highlight, selectedCom.getName()
                );

                getPanel().addUIElement(tooltip);
                WrapUiUtils.mouseCornerPos(tooltip, opad);

                return tooltip;
            }
        };

        add(textPanel).inTL(Right_WALL - LABEL_W, pad + LABEL_H*2);
        }

        { // Number of exporting markets
        final TextPanel textPanel = new TextPanel(getPanel(), LABEL_W, LABEL_H) {

            public void createPanel() {
                final long value = dom.getExporters().size();
                final String txt = "Global Exporters";
                String valueTxt = NumFormat.engNotation(value);
                if (value < 1) {
                    valueTxt = "---";
                }

                final LabelAPI lbl1 = settings.createLabel(txt, Fonts.ORBITRON_12);
                lbl1.setColor(base);
                lbl1.setHighlightOnMouseover(true);
                lbl1.setAlignment(Alignment.MID);

                final LabelAPI lbl2 = settings.createLabel(valueTxt, Fonts.INSIGNIA_VERY_LARGE);
                lbl2.setColor(highlight);
                lbl2.setHighlightOnMouseover(true);
                lbl2.setAlignment(Alignment.MID);

                final float textH1 = lbl1.getPosition().getHeight();

                add(lbl1).inTL(0, 0).setSize(LABEL_W, textH1);
                add(lbl2).inTL(0, textH1 + pad).setSize(LABEL_W, lbl2.getPosition().getHeight());
            }

            public CustomPanelAPI getTpParent() {
                return getPanel();
            }

            @Override
            public TooltipMakerAPI createAndAttachTp() {
                final TooltipMakerAPI tooltip = getPanel().createUIElement(460, 0, false);

                tooltip.addPara(
                    "The total count of markets in the sector that exported %s on the previous day. Only markets that actually sent units to other markets are included, regardless of faction.",
                    pad, highlight, selectedCom.getName()
                );

                getPanel().addUIElement(tooltip);
                WrapUiUtils.mouseCornerPos(tooltip, opad);

                return tooltip;
            }
        };

        add(textPanel).inTL(Right_WALL + LABEL_W, pad);
        }

        { // Number of importing markets
        final TextPanel textPanel = new TextPanel(getPanel(), LABEL_W, LABEL_H) {

            public void createPanel() {
                final long value = dom.getImporters().size();
                final String txt = "Global Importers";
                String valueTxt = NumFormat.engNotation(value);
                if (value < 1) {
                    valueTxt = "---";
                }

                final LabelAPI lbl1 = settings.createLabel(txt, Fonts.ORBITRON_12);
                lbl1.setColor(base);
                lbl1.setHighlightOnMouseover(true);
                lbl1.setAlignment(Alignment.MID);

                final LabelAPI lbl2 = settings.createLabel(valueTxt, Fonts.INSIGNIA_VERY_LARGE);
                lbl2.setColor(highlight);
                lbl2.setHighlightOnMouseover(true);
                lbl2.setAlignment(Alignment.MID);

                final float textH1 = lbl1.getPosition().getHeight();

                add(lbl1).inTL(0, 0).setSize(LABEL_W, textH1);
                add(lbl2).inTL(0, textH1 + pad).setSize(LABEL_W, lbl2.getPosition().getHeight());
            }

            public CustomPanelAPI getTpParent() {
                return getPanel();
            }

            @Override
            public TooltipMakerAPI createAndAttachTp() {
                final TooltipMakerAPI tooltip = getPanel().createUIElement(460, 0, false);

                tooltip.addPara(
                    "The total count of markets in the sector that imported %s on the previous day. Only markets that actually received units from other markets are included, regardless of faction.",
                    pad, highlight, selectedCom.getName()
                );

                getPanel().addUIElement(tooltip);
                WrapUiUtils.mouseCornerPos(tooltip, opad);

                return tooltip;
            }
        };

        add(textPanel).inTL(Right_WALL + LABEL_W, pad + LABEL_H);
        }

        final PendingTooltip<CustomPanelAPI> tableTp = new PendingTooltip<>();
        tableTp.parentSupplier = () -> { return getPanel(); };
        tableTp.factory = () -> {
            final TooltipMakerAPI tooltip = getPanel().createUIElement(
                400, 0, false
            );
            tooltip.addPara("Ctrl + Click to set course", pad, highlight, new String[]{
                "Ctrl", "Click"
            });

            return tooltip;
        };

        { // Top 5 producers
        final SortableTable table = new SortableTable(getParent(), TABLE_W, TABLE_H);

        table.addHeaders(
            "", 40, null, true, false, 1, // Icon header
            "Colony", 100, "Colony name", true, true, 1,
            "Production", 100, "Daily units of " + selectedCom.getName() + " produced", false, false, -1
        );

        final ArrayList<CommodityCell> producers = dom.getSortedByProduction(5);

        for (CommodityCell cell : producers) {

            final String iconPath = cell.market.getFaction().getCrest();
            final Base iconPanel = new Base(table.getPanel(), 28, 28, iconPath, null, null);
            final Color textColor = cell.market.getFaction().getBaseUIColor();
            final long value = (long) cell.getProduction(true);

            table.addCell(iconPanel, cellAlg.LEFT, null, null);
            table.addCell(cell.market.getName(), cellAlg.LEFT, null, textColor);
            table.addCell(NumFormat.engNotation(value), cellAlg.MID, value, textColor);

            final CallbackRunnable<RowPanel> run = (row) -> {
                if (!Keyboard.isKeyDown(Keyboard.KEY_LCONTROL)&&!Keyboard.isKeyDown(Keyboard.KEY_RCONTROL)) {
                    return;
                }

                final SectorEntityToken target = cell.market.getPrimaryEntity();
                if (target != null) {
                    Global.getSector().layInCourseFor(target);
                }
            };

            table.pushRow(
                null, null, null, null, tableTp, run 
            );
        }

        table.showSortIcon = false;
        table.sortingEnabled = false;
        add(table).inBL(opad*2, TABLE_H + LABEL_H / 2f);
        table.createPanel();

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
            "Demand", 100, "Daily units of " + selectedCom.getName() + " demanded", false, false, -1
        );

        final ArrayList<CommodityCell> consumers = dom.getSortedByDemand(5);

        for (CommodityCell cell : consumers) {

            final String iconPath = cell.market.getFaction().getCrest();
            final Base iconPanel = new Base(table.getPanel(), 28, 28, iconPath, null, null);
            final Color textColor = cell.market.getFaction().getBaseUIColor();
            final long value = (long) cell.getBaseDemand(true);

            table.addCell(iconPanel, cellAlg.LEFT, null, null);
            table.addCell(cell.market.getName(), cellAlg.LEFT, null, textColor);
            table.addCell(NumFormat.engNotation(value), cellAlg.MID, value, textColor);

            final CallbackRunnable<RowPanel> run = (row) -> {
                if (!Keyboard.isKeyDown(Keyboard.KEY_LCONTROL)&&!Keyboard.isKeyDown(Keyboard.KEY_RCONTROL)) {
                    return;
                }

                final SectorEntityToken target = cell.market.getPrimaryEntity();
                if (target != null) {
                    Global.getSector().layInCourseFor(target);
                }
            };

            table.pushRow(
                null, null, null, null, tableTp, run 
            );
        }

        table.showSortIcon = false;
        table.sortingEnabled = false;
        add(table).inBL(opad*2, 0);
        table.createPanel();

        final LabelAPI label = settings.createLabel("Top 5 consumers", Fonts.ORBITRON_16);
        final float labelW = label.computeTextWidth(label.getText());
        label.setColor(base);
        add(label).inBL(opad*2 + (TABLE_W - labelW) / 2f, TABLE_H + pad*2);
        }
      
        { // Global export share per faction (percent)
        final ArrayList<PieSlice> data = new ArrayList<>(); 
        final List<FactionSpecAPI> factionList = settings.getAllFactionSpecs();
        for (Iterator<FactionSpecAPI> iter = factionList.iterator(); iter.hasNext();) {
            final FactionSpecAPI faction = iter.next();
            final float value = engine.info.getFactionTotalExportMarketShare(comID, faction.getId());
            if (value < 0.001f) {
                iter.remove();
                continue;
            }
            data.add(new PieSlice(
                null,
                faction.getBaseUIColor(),
                value
            ));
        }
        final PendingTooltip<CustomPanelAPI> pendingTp = new PendingTooltip<>();

        final PieChart chart = new PieChart(getPanel(), PIECHART_W, PIECHART_H, data);
        add(chart).inBL(360, pad);

        pendingTp.parentSupplier = chart::getPanel;
        pendingTp.factory = () -> {
            final TooltipMakerAPI tp = chart.getPanel().createUIElement(400, 1, false);
            tp.setParaFont(Fonts.ORBITRON_12);
            tp.setParaFontColor(base);
            tp.addPara("Global Export Share by Faction", pad);
            tp.setParaFontDefault();
            tp.setParaFontColor(Misc.getTextColor());
            tp.addPara(
                "Shows the percentage of total exports controlled by each faction. " +
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
                tp.addRow(new Object[] {
                    faction.getBaseUIColor(),
                    faction.getDisplayName(),
                    highlight,
                    (int) (engine.info.getFactionTotalExportMarketShare(comID, faction.getId()) * 100) + "%"
                });
            }

            tp.addTable("", 0, opad);
            return tp;
        };
        chart.pendingTp = pendingTp;

        final LabelAPI label = settings.createLabel("Export share", Fonts.ORBITRON_16);
        final float labelW = label.computeTextWidth(label.getText());
        label.setColor(base);
        add(label).inBL(360 + (PIECHART_W - labelW) / 2f, PIECHART_H + pad*2);
        }

        { // Global import share per faction (percent)
        final ArrayList<PieSlice> data = new ArrayList<>(); 
        final List<FactionSpecAPI> factionList = settings.getAllFactionSpecs();
        for (Iterator<FactionSpecAPI> iter = factionList.iterator(); iter.hasNext();) {
            final FactionSpecAPI faction = iter.next();
            final float value = engine.info.getFactionTotalImportMarketShare(comID, faction.getId());
            if (value < 0.001f) {
                iter.remove();
                continue;
            }
            data.add(new PieSlice(
                null,
                faction.getBaseUIColor(),
                value
            ));
        }
        final PendingTooltip<CustomPanelAPI> pendingTp = new PendingTooltip<>();

        final PieChart chart = new PieChart(getPanel(), PIECHART_W, PIECHART_H, data);
        add(chart).inBL(580, pad);

        pendingTp.parentSupplier = chart::getPanel;
        pendingTp.factory = () -> {
            final TooltipMakerAPI tp = chart.getPanel().createUIElement(400, 1, false);
            tp.setParaFont(Fonts.ORBITRON_12);
            tp.setParaFontColor(base);
            tp.addPara("Global Import Share by Faction", pad);
            tp.setParaFontDefault();
            tp.setParaFontColor(Misc.getTextColor());
            tp.addPara(
                "Shows the percentage of total imports made by each faction. " +
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
                tp.addRow(new Object[] {
                    faction.getBaseUIColor(),
                    faction.getDisplayName(),
                    highlight,
                    (int) (engine.info.getFactionTotalImportMarketShare(comID, faction.getId()) * 100) + "%"
                });
            }

            tp.addTable("", 0, opad);
            return tp;
        };
        chart.pendingTp = pendingTp;

        final LabelAPI label = settings.createLabel("Import share", Fonts.ORBITRON_16);
        final float labelW = label.computeTextWidth(label.getText());
        label.setColor(base);
        add(label).inBL(580 + (PIECHART_W - labelW) / 2f, PIECHART_H + pad*2);
        }

        { // In-faction vs out-of-faction trade share
        final ArrayList<PieSlice> data = new ArrayList<>();
        final double total = engine.info.getTotalGlobalExports(comID) + engine.info.getTotalFactionExports(comID);
        final float globalTradeShare = (float) (engine.info.getTotalGlobalExports(comID) / total);
            
        data.add(new PieSlice(
            null,
            UiUtils.COLOR_IMPORT,
            globalTradeShare
        ));
        data.add(new PieSlice(
            null,
            UiUtils.inFactionColor,
            1f - globalTradeShare
        ));

        final PendingTooltip<CustomPanelAPI> pendingTp = new PendingTooltip<>();

        final PieChart chart = new PieChart(getPanel(), PIECHART_W, PIECHART_H, data);
        add(chart).inBL(800, pad);

        pendingTp.parentSupplier = chart::getPanel;
        pendingTp.factory = () -> {
            final TooltipMakerAPI tp = chart.getPanel().createUIElement(400, 1, false);
            tp.setParaFont(Fonts.ORBITRON_12);
            tp.setParaFontColor(base);
            tp.addPara("Global vs In-Faction Trade Share", pad);
            tp.setParaFontDefault();
            tp.setParaFontColor(Misc.getTextColor());
            tp.addPara(
                "Shows the proportion of this commodity's total exports that are traded outside the producing faction (%s)" +
                " versus exports consumed within the same faction (%s). " +
                "Values are based on the previous day.",
                pad,
                highlight, ((int)(globalTradeShare*100)) + "%", ((int)((1f-globalTradeShare)*100)) + "%"
            );

            return tp;
        };
        chart.pendingTp = pendingTp;

        final LabelAPI label = settings.createLabel("Trade Breakdown", Fonts.ORBITRON_16);
        final float labelW = label.computeTextWidth(label.getText());
        label.setColor(base);
        add(label).inBL(800 + (PIECHART_W - labelW) / 2f, PIECHART_H + pad*2);
        }
    }
}
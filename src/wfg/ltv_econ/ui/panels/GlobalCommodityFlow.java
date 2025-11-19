package wfg.ltv_econ.ui.panels;

import java.awt.Color;
import java.util.ArrayList;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.SettingsAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.econ.CommoditySpecAPI;
import com.fs.starfarer.api.impl.campaign.ids.Commodities;
import com.fs.starfarer.api.ui.CustomPanelAPI;
import com.fs.starfarer.api.ui.Fonts;
import com.fs.starfarer.api.ui.LabelAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.ui.UIPanelAPI;
import com.fs.starfarer.api.util.Misc;

import wfg.ltv_econ.economy.CommodityInfo;
import wfg.ltv_econ.economy.CommodityStats;
import wfg.ltv_econ.economy.EconomyEngine;
import wfg.wrap_ui.ui.UIState.State;
import wfg.wrap_ui.ui.panels.CustomPanel;
import wfg.wrap_ui.ui.panels.SortableTable;
import wfg.wrap_ui.ui.panels.SortableTable.cellAlg;
import wfg.wrap_ui.ui.panels.SpritePanel.Base;
import wfg.wrap_ui.ui.panels.CustomPanel.HasBackground;
import wfg.wrap_ui.ui.panels.TextPanel;
import wfg.wrap_ui.ui.plugins.BasePanelPlugin;
import wfg.wrap_ui.util.NumFormat;
import wfg.wrap_ui.util.WrapUiUtils;

public class GlobalCommodityFlow extends
    CustomPanel<BasePanelPlugin<GlobalCommodityFlow>, GlobalCommodityFlow, CustomPanelAPI> implements
    HasBackground
{

    public static final int pad = 3;
    public static final int opad = 10;
    public static final int TABLE_W = 240;
    public static final int TABLE_H = 28*5 + 20 + opad*3;

    public static CommoditySpecAPI selectedCom = Global.getSettings().getCommoditySpec(Commodities.SUPPLIES);
    public final int PANEL_W;
    public final int PANEL_H;
    public final int ICON_SIZE;

    public GlobalCommodityFlow(UIPanelAPI parent, int width, int height) {
        super(parent, width, height, new BasePanelPlugin<>());

        PANEL_W = width;
        PANEL_H = height;

        ICON_SIZE = width / 7;

        initializePlugin(hasPlugin);
        createPanel();
    }

    public void initializePlugin(boolean hasPlugin) {
        getPlugin().init(this);
    }

    public void createPanel() {
        final SettingsAPI settings = Global.getSettings();
        final SectorAPI sector = Global.getSector();
        final EconomyEngine engine = EconomyEngine.getInstance();
        final String comID = selectedCom.getId();
        final CommodityInfo info = engine.getCommodityInfo(comID);

        final Color baseColor = sector.getPlayerFaction().getBaseUIColor();
        final Color highlight = Misc.getHighlightColor();

        final ComIconPanel comIcon = new ComIconPanel(
            getPanel(), sector.getPlayerFaction(), ICON_SIZE, ICON_SIZE,
            selectedCom.getIconName(), null, null
        );
        comIcon.setCommodity(selectedCom);
        add(comIcon).inTL(opad, opad);

        { // Total global production
        final TextPanel textPanel = new TextPanel(getPanel(), 170, 0) {

            public void createPanel() {
                final long value = engine.getGlobalProduction(comID);
                final String txt = "Global production";
                String valueTxt = NumFormat.engNotation(value);
                if (value < 1) {
                    valueTxt = "---";
                }

                final LabelAPI lbl1 = settings.createLabel(txt, Fonts.ORBITRON_12);
                lbl1.setColor(baseColor);
                lbl1.setHighlightOnMouseover(true);

                final LabelAPI lbl2 = settings.createLabel(valueTxt, Fonts.INSIGNIA_VERY_LARGE);
                lbl2.setColor(highlight);
                lbl2.setHighlightOnMouseover(true);

                final float textH1 = lbl1.getPosition().getHeight();
                final float textW1 = lbl1.computeTextWidth(txt);

                add(lbl1).inTL(0, 0).setSize(textW1, textH1);

                final float textH2 = lbl2.getPosition().getHeight();
                final float textW2 = lbl2.computeTextWidth(valueTxt);
                final float textX2 = (textW1 / 2) - (textW2 / 2);

                add(lbl2).inTL(textX2, textH1 + pad).setSize(textW2, lbl2.getPosition().getHeight());

                m_panel.getPosition().setSize(textW1, textH1 + textH2 + pad);
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
                    pad,
                    Misc.getHighlightColor(),
                    selectedCom.getName()
                );

                getPanel().addUIElement(tooltip);
                WrapUiUtils.mouseCornerPos(tooltip, opad);

                return tooltip;
            }

            @Override
            public void initializePlugin(boolean hasPlugin) {
                super.initializePlugin(hasPlugin);

                getPlugin().setTargetUIState(State.NONE);
            }
        };

        add(textPanel).inTL(ICON_SIZE + opad, pad);
        }

        { // Total global demand
        final TextPanel textPanel = new TextPanel(getPanel(), 170, 0) {

            public void createPanel() {
                final long value = engine.getGlobalDemand(comID);
                final String txt = "Global demand";
                String valueTxt = NumFormat.engNotation(value);
                if (value < 1) {
                    valueTxt = "---";
                }

                final LabelAPI lbl1 = settings.createLabel(txt, Fonts.ORBITRON_12);
                lbl1.setColor(baseColor);
                lbl1.setHighlightOnMouseover(true);

                final LabelAPI lbl2 = settings.createLabel(valueTxt, Fonts.INSIGNIA_VERY_LARGE);
                lbl2.setColor(highlight);
                lbl2.setHighlightOnMouseover(true);

                final float textH1 = lbl1.getPosition().getHeight();
                final float textW1 = lbl1.computeTextWidth(txt);

                add(lbl1).inTL(0, 0).setSize(textW1, textH1);

                final float textH2 = lbl2.getPosition().getHeight();
                final float textW2 = lbl2.computeTextWidth(valueTxt);
                final float textX2 = (textW1 / 2) - (textW2 / 2);

                add(lbl2).inTL(textX2, textH1 + pad).setSize(textW2, lbl2.getPosition().getHeight());

                m_panel.getPosition().setSize(textW1, textH1 + textH2 + pad);
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
                    pad,
                    Misc.getHighlightColor(),
                    selectedCom.getName()
                );

                getPanel().addUIElement(tooltip);
                WrapUiUtils.mouseCornerPos(tooltip, opad);

                return tooltip;
            }

            @Override
            public void initializePlugin(boolean hasPlugin) {
                super.initializePlugin(hasPlugin);

                getPlugin().setTargetUIState(State.NONE);
            }
        };

        add(textPanel).inTL(ICON_SIZE*2 + opad, pad);
        }

        { // Top 5 producers
        final SortableTable table = new SortableTable(getParent(), TABLE_W, TABLE_H);

        table.addHeaders(
            "", 40, null, true, false, 1, // Icon header
            "Colony", 100, "Colony name", true, true, 1,
            "Production", 100, "Daily units of " + selectedCom.getName() + " produced", false, false, -1
        );

        final ArrayList<CommodityStats> producers = info.getSortedByProduction(5);

        for (CommodityStats stats : producers) {

            final String iconPath = stats.market.getFaction().getCrest();
            final Base iconPanel = new Base(
                table.getPanel(), 28, 28, iconPath, null,
                null, false
            );
            final Color textColor = stats.market.getFaction().getBaseUIColor();
            final long value = (long) stats.getLocalProduction(true);

            table.addCell(iconPanel, cellAlg.LEFT, null, null);
            table.addCell(stats.market.getName(), cellAlg.LEFT, null, textColor);
            table.addCell(NumFormat.engNotation(value), cellAlg.MID, value, textColor);

            table.pushRow(
                null, null, null, null, null, null
            );
        }

        add(table).inBL(opad, 0);
        table.createPanel();

        final LabelAPI label = settings.createLabel("Top 5 producers", Fonts.ORBITRON_16);
        final float labelW = label.computeTextWidth(label.getText());
        label.setColor(baseColor);
        add(label).inBL(opad + (TABLE_W - labelW) / 2f, TABLE_H + pad*2);
        }

        { // Top 5 consumers
        final SortableTable table = new SortableTable(getParent(), TABLE_W, TABLE_H);

        table.addHeaders(
            "", 40, null, true, false, 1, // Icon header
            "Colony", 100, "Colony name", true, true, 1,
            "Demand", 100, "Daily units of " + selectedCom.getName() + " demanded", false, false, -1
        );

        final ArrayList<CommodityStats> consumers = info.getSortedByDemand(5);

        for (CommodityStats stats : consumers) {

            final String iconPath = stats.market.getFaction().getCrest();
            final Base iconPanel = new Base(
                table.getPanel(), 28, 28, iconPath, null,
                null, false
            );
            final Color textColor = stats.market.getFaction().getBaseUIColor();
            final long value = (long) stats.getBaseDemand(false);

            table.addCell(iconPanel, cellAlg.LEFT, null, null);
            table.addCell(stats.market.getName(), cellAlg.LEFT, null, textColor);
            table.addCell(NumFormat.engNotation(value), cellAlg.MID, value, textColor);

            table.pushRow(
                null, null, null, null, null, null
            );
        }

        add(table).inBL(opad + TABLE_W + opad, 0);
        table.createPanel();

        final LabelAPI label = settings.createLabel("Top 5 consumers", Fonts.ORBITRON_16);
        final float labelW = label.computeTextWidth(label.getText());
        label.setColor(baseColor);
        add(label).inBL(opad + TABLE_W + opad + (TABLE_W - labelW) / 2f, TABLE_H + pad*2);
        }

        { // Total global surplus
        final TextPanel textPanel = new TextPanel(getPanel(), 170, 0) {

            public void createPanel() {
                final long value = engine.getGlobalSurplus(comID);
                final String txt = "Global surplus";
                String valueTxt = NumFormat.engNotation(value);
                if (value < 1) {
                    valueTxt = "---";
                }

                final LabelAPI lbl1 = settings.createLabel(txt, Fonts.ORBITRON_12);
                lbl1.setColor(baseColor);
                lbl1.setHighlightOnMouseover(true);

                final LabelAPI lbl2 = settings.createLabel(valueTxt, Fonts.INSIGNIA_VERY_LARGE);
                lbl2.setColor(highlight);
                lbl2.setHighlightOnMouseover(true);

                final float textH1 = lbl1.getPosition().getHeight();
                final float textW1 = lbl1.computeTextWidth(txt);

                add(lbl1).inTL(0, 0).setSize(textW1, textH1);

                final float textH2 = lbl2.getPosition().getHeight();
                final float textW2 = lbl2.computeTextWidth(valueTxt);
                final float textX2 = (textW1 / 2) - (textW2 / 2);

                add(lbl2).inTL(textX2, textH1 + pad).setSize(textW2, lbl2.getPosition().getHeight());

                m_panel.getPosition().setSize(textW1, textH1 + textH2 + pad);
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
                    pad,
                    Misc.getHighlightColor(),
                    selectedCom.getName()
                );

                getPanel().addUIElement(tooltip);
                WrapUiUtils.mouseCornerPos(tooltip, opad);

                return tooltip;
            }

            @Override
            public void initializePlugin(boolean hasPlugin) {
                super.initializePlugin(hasPlugin);

                getPlugin().setTargetUIState(State.NONE);
            }
        };

        add(textPanel).inTL(ICON_SIZE*3 + opad, pad);
        }

        { // Total global deficit
        final TextPanel textPanel = new TextPanel(getPanel(), 170, 0) {

            public void createPanel() {
                final long value = engine.getGlobalDeficit(comID);
                final String txt = "Global deficit";
                String valueTxt = NumFormat.engNotation(value);
                if (value < 1) {
                    valueTxt = "---";
                }

                final LabelAPI lbl1 = settings.createLabel(txt, Fonts.ORBITRON_12);
                lbl1.setColor(baseColor);
                lbl1.setHighlightOnMouseover(true);

                final LabelAPI lbl2 = settings.createLabel(valueTxt, Fonts.INSIGNIA_VERY_LARGE);
                lbl2.setColor(highlight);
                lbl2.setHighlightOnMouseover(true);

                final float textH1 = lbl1.getPosition().getHeight();
                final float textW1 = lbl1.computeTextWidth(txt);

                add(lbl1).inTL(0, 0).setSize(textW1, textH1);

                final float textH2 = lbl2.getPosition().getHeight();
                final float textW2 = lbl2.computeTextWidth(valueTxt);
                final float textX2 = (textW1 / 2) - (textW2 / 2);

                add(lbl2).inTL(textX2, textH1 + pad).setSize(textW2, lbl2.getPosition().getHeight());

                m_panel.getPosition().setSize(textW1, textH1 + textH2 + pad);
            }

            public CustomPanelAPI getTpParent() {
                return getPanel();
            }

            @Override
            public TooltipMakerAPI createAndAttachTp() {
                final TooltipMakerAPI tooltip = getPanel().createUIElement(460, 0, false);

                tooltip.addPara(
                    "Global deficit represents the total amount of demand that remained unfulfilled during the last economic cycle." +
                    "This value does not track shortages in stockpiles and only measures demand that was not supplied during the cycle." +
                    "A colony may have large reserves and still contribute to the global deficit if trade routes could not deliver enough units in time.",
                    pad
                );

                getPanel().addUIElement(tooltip);
                WrapUiUtils.mouseCornerPos(tooltip, opad);

                return tooltip;
            }

            @Override
            public void initializePlugin(boolean hasPlugin) {
                super.initializePlugin(hasPlugin);

                getPlugin().setTargetUIState(State.NONE);
            }
        };

        add(textPanel).inTL(ICON_SIZE*4 + opad, pad);
        }

        { // Total trade volume (units)
        final TextPanel textPanel = new TextPanel(getPanel(), 170, 0) {

            public void createPanel() {
                final long value = engine.getGlobalTradeVolume(comID);
                final String txt = "Sector-wide trade volume";
                String valueTxt = NumFormat.engNotation(value);
                if (value < 1) {
                    valueTxt = "---";
                }

                final LabelAPI lbl1 = settings.createLabel(txt, Fonts.ORBITRON_12);
                lbl1.setColor(baseColor);
                lbl1.setHighlightOnMouseover(true);

                final LabelAPI lbl2 = settings.createLabel(valueTxt, Fonts.INSIGNIA_VERY_LARGE);
                lbl2.setColor(highlight);
                lbl2.setHighlightOnMouseover(true);

                final float textH1 = lbl1.getPosition().getHeight();
                final float textW1 = lbl1.computeTextWidth(txt);

                add(lbl1).inTL(0, 0).setSize(textW1, textH1);

                final float textH2 = lbl2.getPosition().getHeight();
                final float textW2 = lbl2.computeTextWidth(valueTxt);
                final float textX2 = (textW1 / 2) - (textW2 / 2);

                add(lbl2).inTL(textX2, textH1 + pad).setSize(textW2, lbl2.getPosition().getHeight());

                m_panel.getPosition().setSize(textW1, textH1 + textH2 + pad);
            }

            public CustomPanelAPI getTpParent() {
                return getPanel();
            }

            @Override
            public TooltipMakerAPI createAndAttachTp() {
                final TooltipMakerAPI tooltip = getPanel().createUIElement(460, 0, false);

                tooltip.addPara(
                    "The total number of units of %s traded across the sector during the current cycle, including both in-faction and out-of-faction transactions. This represents all actual movement of goods between markets, regardless of prices or stockpiles.",
                    pad,
                    Misc.getHighlightColor(),
                    selectedCom.getName()
                );

                getPanel().addUIElement(tooltip);
                WrapUiUtils.mouseCornerPos(tooltip, opad);

                return tooltip;
            }

            @Override
            public void initializePlugin(boolean hasPlugin) {
                super.initializePlugin(hasPlugin);

                getPlugin().setTargetUIState(State.NONE);
            }
        };

        add(textPanel).inTL(ICON_SIZE*5 + opad, pad);
        }

        { // Total trade value (credits)
        final TextPanel textPanel = new TextPanel(getPanel(), 170, 0) {

            public void createPanel() {
                final long value = info.getMarketActivity();
                final String txt = "Sector-wide trade value";
                String valueTxt = NumFormat.formatCredits(value);
                if (value < 1) {
                    valueTxt = "---";
                }

                final LabelAPI lbl1 = settings.createLabel(txt, Fonts.ORBITRON_12);
                lbl1.setColor(baseColor);
                lbl1.setHighlightOnMouseover(true);

                final LabelAPI lbl2 = settings.createLabel(valueTxt, Fonts.INSIGNIA_VERY_LARGE);
                lbl2.setColor(highlight);
                lbl2.setHighlightOnMouseover(true);

                final float textH1 = lbl1.getPosition().getHeight();
                final float textW1 = lbl1.computeTextWidth(txt);

                add(lbl1).inTL(0, 0).setSize(textW1, textH1);

                final float textH2 = lbl2.getPosition().getHeight();
                final float textW2 = lbl2.computeTextWidth(valueTxt);
                final float textX2 = (textW1 / 2) - (textW2 / 2);

                add(lbl2).inTL(textX2, textH1 + pad).setSize(textW2, lbl2.getPosition().getHeight());

                m_panel.getPosition().setSize(textW1, textH1 + textH2 + pad);
            }

            public CustomPanelAPI getTpParent() {
                return getPanel();
            }

            @Override
            public TooltipMakerAPI createAndAttachTp() {
                final TooltipMakerAPI tooltip = getPanel().createUIElement(460, 0, false);

                tooltip.addPara(
                    "The total monetary value (in credits) of all %s trades across the entire sector during the current cycle. This includes both in-faction and out-of-faction trade, calculated using the prices at which commodities were exchanged.",
                    pad,
                    Misc.getHighlightColor(),
                    selectedCom.getName()
                );

                getPanel().addUIElement(tooltip);
                WrapUiUtils.mouseCornerPos(tooltip, opad);

                return tooltip;
            }

            @Override
            public void initializePlugin(boolean hasPlugin) {
                super.initializePlugin(hasPlugin);

                getPlugin().setTargetUIState(State.NONE);
            }
        };

        add(textPanel).inTL(ICON_SIZE + opad, pad + 50);
        }

        { // Average sector price
        final TextPanel textPanel = new TextPanel(getPanel(), 170, 0) {

            public void createPanel() {
                final long value = (long) engine.getGlobalAveragePrice(comID, 1);
                final String txt = "Global average price";
                String valueTxt = NumFormat.formatCredits(value);
                if (value < 1) {
                    valueTxt = "---";
                }

                final LabelAPI lbl1 = settings.createLabel(txt, Fonts.ORBITRON_12);
                lbl1.setColor(baseColor);
                lbl1.setHighlightOnMouseover(true);

                final LabelAPI lbl2 = settings.createLabel(valueTxt, Fonts.INSIGNIA_VERY_LARGE);
                lbl2.setColor(highlight);
                lbl2.setHighlightOnMouseover(true);

                final float textH1 = lbl1.getPosition().getHeight();
                final float textW1 = lbl1.computeTextWidth(txt);

                add(lbl1).inTL(0, 0).setSize(textW1, textH1);

                final float textH2 = lbl2.getPosition().getHeight();
                final float textW2 = lbl2.computeTextWidth(valueTxt);
                final float textX2 = (textW1 / 2) - (textW2 / 2);

                add(lbl2).inTL(textX2, textH1 + pad).setSize(textW2, lbl2.getPosition().getHeight());

                m_panel.getPosition().setSize(textW1, textH1 + textH2 + pad);
            }

            public CustomPanelAPI getTpParent() {
                return getPanel();
            }

            @Override
            public TooltipMakerAPI createAndAttachTp() {
                final TooltipMakerAPI tooltip = getPanel().createUIElement(460, 0, false);

                tooltip.addPara(
                    "The average price of %s across all markets in the sector during the current cycle, weighted by the quantities traded. This provides a sector-wide benchmark price, reflecting both in-faction and out-of-faction transactions.",
                    pad,
                    Misc.getHighlightColor(),
                    selectedCom.getName()
                );

                getPanel().addUIElement(tooltip);
                WrapUiUtils.mouseCornerPos(tooltip, opad);

                return tooltip;
            }

            @Override
            public void initializePlugin(boolean hasPlugin) {
                super.initializePlugin(hasPlugin);

                getPlugin().setTargetUIState(State.NONE);
            }
        };

        add(textPanel).inTL(ICON_SIZE*2 + opad, pad + 50);
        }

        { // Number of exporting markets
        final TextPanel textPanel = new TextPanel(getPanel(), 170, 0) {

            public void createPanel() {
                final long value = info.getExporters().size();
                final String txt = "Global Exporters";
                String valueTxt = NumFormat.engNotation(value);
                if (value < 1) {
                    valueTxt = "---";
                }

                final LabelAPI lbl1 = settings.createLabel(txt, Fonts.ORBITRON_12);
                lbl1.setColor(baseColor);
                lbl1.setHighlightOnMouseover(true);

                final LabelAPI lbl2 = settings.createLabel(valueTxt, Fonts.INSIGNIA_VERY_LARGE);
                lbl2.setColor(highlight);
                lbl2.setHighlightOnMouseover(true);

                final float textH1 = lbl1.getPosition().getHeight();
                final float textW1 = lbl1.computeTextWidth(txt);

                add(lbl1).inTL(0, 0).setSize(textW1, textH1);

                final float textH2 = lbl2.getPosition().getHeight();
                final float textW2 = lbl2.computeTextWidth(valueTxt);
                final float textX2 = (textW1 / 2) - (textW2 / 2);

                add(lbl2).inTL(textX2, textH1 + pad).setSize(textW2, lbl2.getPosition().getHeight());

                m_panel.getPosition().setSize(textW1, textH1 + textH2 + pad);
            }

            public CustomPanelAPI getTpParent() {
                return getPanel();
            }

            @Override
            public TooltipMakerAPI createAndAttachTp() {
                final TooltipMakerAPI tooltip = getPanel().createUIElement(460, 0, false);

                tooltip.addPara(
                    "The total count of markets in the sector that exported %s during the current cycle. Only markets that actually sent units to other markets are included, regardless of faction.",
                    pad,
                    Misc.getHighlightColor(),
                    selectedCom.getName()
                );

                getPanel().addUIElement(tooltip);
                WrapUiUtils.mouseCornerPos(tooltip, opad);

                return tooltip;
            }

            @Override
            public void initializePlugin(boolean hasPlugin) {
                super.initializePlugin(hasPlugin);

                getPlugin().setTargetUIState(State.NONE);
            }
        };

        add(textPanel).inTL(ICON_SIZE*3 + opad, pad + 50);
        }

        { // Number of importing markets
        final TextPanel textPanel = new TextPanel(getPanel(), 170, 0) {

            public void createPanel() {
                final long value = info.getImporters().size();
                final String txt = "Global Importers";
                String valueTxt = NumFormat.engNotation(value);
                if (value < 1) {
                    valueTxt = "---";
                }

                final LabelAPI lbl1 = settings.createLabel(txt, Fonts.ORBITRON_12);
                lbl1.setColor(baseColor);
                lbl1.setHighlightOnMouseover(true);

                final LabelAPI lbl2 = settings.createLabel(valueTxt, Fonts.INSIGNIA_VERY_LARGE);
                lbl2.setColor(highlight);
                lbl2.setHighlightOnMouseover(true);

                final float textH1 = lbl1.getPosition().getHeight();
                final float textW1 = lbl1.computeTextWidth(txt);

                add(lbl1).inTL(0, 0).setSize(textW1, textH1);

                final float textH2 = lbl2.getPosition().getHeight();
                final float textW2 = lbl2.computeTextWidth(valueTxt);
                final float textX2 = (textW1 / 2) - (textW2 / 2);

                add(lbl2).inTL(textX2, textH1 + pad).setSize(textW2, lbl2.getPosition().getHeight());

                m_panel.getPosition().setSize(textW1, textH1 + textH2 + pad);
            }

            public CustomPanelAPI getTpParent() {
                return getPanel();
            }

            @Override
            public TooltipMakerAPI createAndAttachTp() {
                final TooltipMakerAPI tooltip = getPanel().createUIElement(460, 0, false);

                tooltip.addPara(
                    "The total count of markets in the sector that imported %s during the current cycle. Only markets that actually received units from other markets are included, regardless of faction.",
                    pad,
                    Misc.getHighlightColor(),
                    selectedCom.getName()
                );

                getPanel().addUIElement(tooltip);
                WrapUiUtils.mouseCornerPos(tooltip, opad);

                return tooltip;
            }

            @Override
            public void initializePlugin(boolean hasPlugin) {
                super.initializePlugin(hasPlugin);

                getPlugin().setTargetUIState(State.NONE);
            }
        };

        add(textPanel).inTL(ICON_SIZE*4 + opad, pad + 50);
        }

        { // Global export share per factions (percent)

        }

        { // Global import share per factions (percent)

        }

        { // In-faction vs out-of-faction trade share

        }

        { // Trade volatility (month-over-month volume change)

        }

        { // Stockpile ratio (global stored amount / global demand)

        }

        { // Worker allocation (total workers producing it)

        }
    }

    public float getBgAlpha() {
        return 0.1f;
    }

    public Color getBgColor() {
        return Color.BLUE;
    }
}
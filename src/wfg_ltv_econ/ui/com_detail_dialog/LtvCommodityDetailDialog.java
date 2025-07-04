package wfg_ltv_econ.ui.com_detail_dialog;

import java.awt.Color;

import org.lwjgl.input.Keyboard;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CustomDialogDelegate;
import com.fs.starfarer.api.campaign.CustomUIPanelPlugin;
import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.campaign.econ.CommodityOnMarketAPI;
import com.fs.starfarer.api.campaign.econ.Industry;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.impl.campaign.ids.Strings;
import com.fs.starfarer.api.ui.Alignment;
import com.fs.starfarer.api.ui.ButtonAPI;
import com.fs.starfarer.api.ui.CustomPanelAPI;
import com.fs.starfarer.api.ui.Fonts;
import com.fs.starfarer.api.ui.LabelAPI;
import com.fs.starfarer.api.ui.MapParams;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.ui.UIPanelAPI;
import com.fs.starfarer.api.ui.ButtonAPI.UICheckboxSize;
import com.fs.starfarer.api.util.Misc;
import com.fs.starfarer.campaign.ui.marketinfo.CommodityDetailDialog;
import wfg_ltv_econ.plugins.LtvCommodityDetailDialogPlugin;
import wfg_ltv_econ.plugins.LtvSpritePanelPlugin;
import wfg_ltv_econ.plugins.LtvCustomPanelPlugin;
import wfg_ltv_econ.plugins.LtvCustomPanelPlugin.GlowType;
import wfg_ltv_econ.ui.LtvCommodityPanel;
import wfg_ltv_econ.ui.LtvCustomPanel;
import wfg_ltv_econ.ui.LtvIconPanel;
import wfg_ltv_econ.ui.LtvSpritePanel;
import wfg_ltv_econ.ui.LtvTextPanel;
import wfg_ltv_econ.ui.LtvUIState;
import wfg_ltv_econ.ui.LtvUIState.UIStateType;
import wfg_ltv_econ.util.CommodityStats;
import wfg_ltv_econ.util.NumFormat;

public class LtvCommodityDetailDialog implements CustomDialogDelegate {

    public interface CommoditySelectionListener {
        void onCommoditySelected(CommodityOnMarketAPI selectedCommodity);
    }

    // this.PANEL_W = 1206; // Exact width using VisualVM. Includes pad.
    // this.PANEL_H = 728; // Exact height using VisualVM. Includes pad.

    public final int PANEL_W;
    public final int PANEL_H;

    public final int SECT1_WIDTH;
    public final int SECT2_WIDTH;
    public final int SECT3_WIDTH;
    public final int SECT4_WIDTH;

    public final int SECT1_HEIGHT;
    public final int SECT2_HEIGHT;
    public final int SECT3_HEIGHT;
    public final int SECT4_HEIGHT;

    protected CustomPanelAPI section1; // CommodityInfo
    protected CustomPanelAPI section2; // Sector Map
    protected CustomPanelAPI section3; // Prod&Consump Tables
    protected CustomPanelAPI section4; // Commodity Panel

    public final static int pad = 3;
    public final static int opad = 10;
    public final static int iconSize = 28;

    private final LtvCustomPanel m_parentWrapper;
    private final LtvCommodityDetailDialogPlugin m_plugin;
    private final Color highlight = Misc.getHighlightColor();
    private CustomPanelAPI m_dialogPanel;

    public CommodityOnMarketAPI m_com;
    public MarketAPI m_selectedMarket = null; 

    public LtvCommodityDetailDialog(LtvCustomPanel parent, CommodityOnMarketAPI com) {
        // Measured using very precise tools!! (my eyes)
        this(parent, com, 1166, 658 + 20);
    }

    public LtvCommodityDetailDialog(LtvCustomPanel parent, CommodityOnMarketAPI com, int panelW, int panelH) {
        this.PANEL_W = panelW;
        this.PANEL_H = panelH;

        SECT1_WIDTH = (int) (PANEL_W * 0.76f - opad);
        SECT1_HEIGHT = (int) (PANEL_H * 0.28f - opad);

        SECT2_WIDTH = (int) (PANEL_W * 0.24f - opad);
        SECT2_HEIGHT = (int) (PANEL_H * 0.28f - opad);

        SECT3_WIDTH = (int) (PANEL_W * 0.76f - opad);
        SECT3_HEIGHT = (int) (PANEL_H * 0.72f - opad);

        SECT4_WIDTH = (int) (PANEL_W * 0.24f - opad);
        SECT4_HEIGHT = (int) (PANEL_H * 0.72f - opad);

        m_plugin = new LtvCommodityDetailDialogPlugin(parent, this);
        m_parentWrapper = parent;
        m_com = com;
    }

    @Override
    public void createCustomDialog(CustomPanelAPI panel, CustomDialogCallback callback) {
        LtvUIState.setState(UIStateType.DETAIL_DIALOG);
        m_dialogPanel = panel;

        createSections();

        // Footer
        final int footerH = 40;

        LtvCustomPanelPlugin fPlugin = new LtvCustomPanelPlugin() {
            @Override
            public void advance(float amount) {
                super.advance(amount);

                if (hoveredLastFrame && LMBUpLastFrame) {
                    ButtonAPI checkbox = ((LtvTextPanel) m_panel).m_checkbox;

                    if (checkbox != null) {
                        checkbox.setChecked(!checkbox.isChecked());
                    }
                }
            }
        };

        LtvTextPanel footerPanel = new LtvTextPanel(
                m_parentWrapper.getRoot(), panel, m_parentWrapper.m_market, 400, footerH,
                fPlugin, m_parentWrapper.getFaction().getBaseUIColor()) {

            @Override
            public void createPanel() {
                TooltipMakerAPI footer = m_panel.createUIElement(PANEL_W, footerH, false);
                m_checkbox = footer.addCheckbox(20, 20, "", "stockpile_toggle",
                        Fonts.ORBITRON_12, highlight, UICheckboxSize.SMALL, 0);

                m_checkbox.getPosition().inBL(0, 0);
                m_checkbox.setShortcut(Keyboard.KEY_Q, false);

                footer.setParaFont(Fonts.ORBITRON_12);
                footer.setParaFontColor(m_parentWrapper.getFaction().getBaseUIColor());
                LabelAPI txt = footer.addPara("Only show colonies with excess stockpiles or shortages (%s)", 0f,
                        highlight,
                        "Q");
                txt.setHighlightOnMouseover(true);

                int TextY = (int) txt.computeTextHeight(txt.getText());
                txt.getPosition().inBL(20 + pad, (20 - TextY) / 2);

                m_panel.addUIElement(footer);
            }

            @Override
            public TooltipMakerAPI createTooltip() {
                TooltipMakerAPI tooltip = m_panel.createUIElement(getPanelPos().getWidth() * 0.7f, 0, false);

                tooltip.addPara(
                        "Only show colonies that are either suffering from a shortage or have excess stockpiles.\n\nColonies with excess stockpiles have more of the goods available on the open market and have lower prices.\n\nColonies with shortages have less or none available for sale, and have higher prices.",
                        pad);

                m_panel.addUIElement(tooltip).inBL(0, getPanelPos().getHeight());

                return tooltip;
            }

            @Override
            public void removeTooltip(TooltipMakerAPI tooltip) {
                m_panel.removeComponent(tooltip);
            }

            @Override
            public void initializePlugin(boolean hasPlugin) {
                super.initializePlugin(hasPlugin);

                ((LtvCustomPanelPlugin) m_panel.getPlugin()).setTargetUIState(UIStateType.DETAIL_DIALOG);
            }
        };

        panel.addComponent(footerPanel.getPanel()).inBL(pad, -opad * 3.5f);
        m_plugin.init(false, false, false, panel);
    }

    public void createSections() {
        if (m_dialogPanel == null) {
            return;
        }

        updateSection1();
        updateSection2();
        updateSection3();
        updateSection4();
    }

    public void updateSection1() {
        m_dialogPanel.removeComponent(section1);

        section1 = Global.getSettings().createCustom(SECT1_WIDTH, SECT1_HEIGHT, null);

        TooltipMakerAPI tooltip = section1.createUIElement(SECT1_WIDTH, SECT1_HEIGHT, false);
        section1.addUIElement(tooltip).inTL(0, 0);

        createSection1(section1, tooltip, highlight);
        m_dialogPanel.addComponent(section1).inTL(pad, pad);

        // Update anchors
        if (section2 == null || section3 == null) {
            return;
        }
        section2.getPosition().rightOfTop(section1, opad * 1.5f);
        section3.getPosition().rightOfTop(section1, opad * 1.5f);
    }

    public void updateSection2() {
        m_dialogPanel.removeComponent(section2);

        section2 = Global.getSettings().createCustom(SECT2_WIDTH, SECT2_HEIGHT, null);

        TooltipMakerAPI tooltip = section2.createUIElement(SECT2_WIDTH, SECT2_HEIGHT, false);
        section2.addUIElement(tooltip).inTL(0, 0);

        createSection2(section2, tooltip);
        m_dialogPanel.addComponent(section2).rightOfTop(section1, opad * 1.5f);

        // Update anchors
        if (section4 == null) {
            return;
        }
        section4.getPosition().belowLeft(section2, opad);
    }

    public void updateSection3() {
        m_dialogPanel.removeComponent(section3);

        section3 = Global.getSettings().createCustom(SECT3_WIDTH, SECT3_HEIGHT, null);

        TooltipMakerAPI tooltip = section3.createUIElement(SECT3_WIDTH, SECT3_HEIGHT, true);
        section3.addUIElement(tooltip).inTL(0, 0);

        createSection3(section3, tooltip);
        m_dialogPanel.addComponent(section3).belowLeft(section1, opad);
    }

    public void updateSection4() {
        m_dialogPanel.removeComponent(section4);

        section4 = Global.getSettings().createCustom(SECT4_WIDTH, SECT4_HEIGHT, null);

        TooltipMakerAPI tooltip = section4.createUIElement(SECT4_WIDTH, SECT4_HEIGHT, false);
        section4.addUIElement(tooltip).inTL(0, 0);

        createSection4(section4);
        m_dialogPanel.addComponent(section4).belowLeft(section2, opad);
    }

    private void createSection1(CustomPanelAPI section, TooltipMakerAPI tooltip, Color highlight) {
        if (m_com == null) {
            return;
        }
        tooltip.addSectionHeading(m_com.getCommodity().getName(), Alignment.MID, pad);
        final int headerHeight = (int) tooltip.getPrev().getPosition().getHeight();

        // Icons
        final int iconSize = (int) (section.getPosition().getHeight() / 2.2f);

        String comID = m_com.getCommodity().getIconName();

        LtvIconPanel iconLeft = new LtvIconPanel(m_parentWrapper.getRoot(), section, m_parentWrapper.m_market,
                iconSize, iconSize, new LtvSpritePanelPlugin(), comID, null, null, false);
        iconLeft.setCommodity(m_com);

        iconLeft.getPanelPos().inTL(opad * 3,
                (SECT1_HEIGHT - iconSize) / 2 + headerHeight);
        section.addComponent(iconLeft.getPanel());

        LtvIconPanel iconRight = new LtvIconPanel(m_parentWrapper.getRoot(), section, m_parentWrapper.m_market,
                iconSize, iconSize, new LtvSpritePanelPlugin(), comID, null, null, false);
        iconRight.setCommodity(m_com);

        iconRight.getPanelPos().inTL(SECT1_WIDTH - iconSize - opad * 3,
                (SECT1_HEIGHT - iconSize) / 2 + headerHeight);
        section.addComponent(iconRight.getPanel());

        // Text
        final int baseY = (int) (headerHeight + opad * 1.5f);
        Color baseColor = m_parentWrapper.m_faction.getBaseUIColor();
        { // Global market value
            LtvTextPanel textPanel = new LtvTextPanel(
                    m_parentWrapper.getRoot(), section, m_parentWrapper.m_market, 170, 0,
                    new LtvCustomPanelPlugin(), m_parentWrapper.getFaction().getBaseUIColor()) {

                @Override
                public void createPanel() {
                    TooltipMakerAPI tooltip = m_panel.createUIElement(170, 0, false);

                    String txt = "Global market value";
                    String valueTxt = Misc.getWithDGS(m_com.getCommodityMarketData().getMarketValue()) + Strings.C;
                    if (m_com.getCommodityMarketData().getMarketValue() < 1) {
                        valueTxt = "---";
                    }

                    tooltip.setParaFontColor(baseColor);
                    tooltip.setParaFont(Fonts.ORBITRON_12);
                    LabelAPI lbl1 = tooltip.addPara(txt, pad);
                    lbl1.setHighlightOnMouseover(true);

                    tooltip.setParaFontColor(highlight);
                    tooltip.setParaFont(Fonts.INSIGNIA_VERY_LARGE);
                    LabelAPI lbl2 = tooltip.addPara(valueTxt, highlight, pad);
                    lbl2.setHighlightOnMouseover(true);

                    float textH1 = lbl1.getPosition().getHeight();
                    textW1 = lbl1.computeTextWidth(txt);
                    textX1 = (SECT1_WIDTH / 3f) - textW1;

                    lbl1.getPosition().inTL(0, 0).setSize(textW1, lbl1.getPosition().getHeight());

                    float textW2 = lbl2.computeTextWidth(valueTxt);
                    float textX2 = (textW1 / 2) - (textW2 / 2);

                    lbl2.getPosition().inTL(textX2, textH1 + pad).setSize(textW2, lbl2.getPosition().getHeight());

                    m_panel.addUIElement(tooltip).inTL(0, 0);

                    m_panel.getPosition().setSize(tooltip.getWidthSoFar(), tooltip.getHeightSoFar());
                }

                @Override
                public TooltipMakerAPI createTooltip() {
                    TooltipMakerAPI tooltip = ((CustomPanelAPI) getParent()).createUIElement(460, 0, false);

                    tooltip.addPara(
                            "Total profit that can be made fulfilling the demand for " +
                                    m_com.getCommodity().getName() + ". " +
                                    "Will be distributed among all exporters based on their market share.\n\n" +
                                    "More expensive commodities often have lower profit margins due to the high costs "
                                    +
                                    "of manufacturing and transport.\n\n" +
                                    "The value shown here does not include the demand at your colonies, " +
                                    "since there is no profit to be made there.",
                            pad);

                    final float tpX = textX1 + textW1 + opad;
                    final float tpY = baseY;

                    ((CustomPanelAPI) getParent()).addUIElement(tooltip).inTL(tpX, tpY);
                    ((CustomPanelAPI) getParent()).bringComponentToTop(tooltip);

                    return tooltip;
                }

                @Override
                public void removeTooltip(TooltipMakerAPI tooltip) {
                    ((CustomPanelAPI) getParent()).removeComponent(tooltip);
                }

                @Override
                public void initializePlugin(boolean hasPlugin) {
                    super.initializePlugin(hasPlugin);

                    ((LtvCustomPanelPlugin) m_panel.getPlugin()).setTargetUIState(UIStateType.DETAIL_DIALOG);
                }
            };

            tooltip.addComponent(textPanel.getPanel()).inTL(textPanel.textX1, baseY);
        }
        { // Total global exports
            LtvTextPanel textPanel = new LtvTextPanel(
                    m_parentWrapper.getRoot(), section, m_parentWrapper.m_market, 170, 0,
                    new LtvCustomPanelPlugin(), m_parentWrapper.getFaction().getBaseUIColor()) {

                @Override
                public void createPanel() {
                    TooltipMakerAPI tooltip = m_panel.createUIElement(170, 0, false);

                    String txt = "Total global exports";

                    String valueTxt = NumFormat.engNotation(getTotalGlobalExports(m_com.getId()));

                    tooltip.setParaFontColor(baseColor);
                    tooltip.setParaFont(Fonts.ORBITRON_12);
                    LabelAPI lbl1 = tooltip.addPara(txt, pad);
                    lbl1.setHighlightOnMouseover(true);

                    tooltip.setParaFontColor(highlight);
                    tooltip.setParaFont(Fonts.INSIGNIA_VERY_LARGE);
                    LabelAPI lbl2 = tooltip.addPara(valueTxt, highlight, pad);
                    lbl2.setHighlightOnMouseover(true);

                    float textH1 = lbl1.getPosition().getHeight();
                    textW1 = lbl1.computeTextWidth(txt);
                    textX1 = (SECT1_WIDTH / 2f) - (textW1 / 2f);

                    lbl1.getPosition().inTL(0, 0).setSize(textW1, lbl1.getPosition().getHeight());

                    float textW2 = lbl2.computeTextWidth(valueTxt);
                    float textX2 = (textW1 / 2) - (textW2 / 2);

                    lbl2.getPosition().inTL(textX2, textH1 + pad).setSize(textW2, lbl2.getPosition().getHeight());

                    m_panel.addUIElement(tooltip).inTL(0, 0);

                    m_panel.getPosition().setSize(tooltip.getWidthSoFar(), tooltip.getHeightSoFar());
                }

                @Override
                public TooltipMakerAPI createTooltip() {
                    TooltipMakerAPI tooltip = ((CustomPanelAPI) getParent()).createUIElement(460, 0, false);

                    tooltip.addPara(
                            "Maximum number of units capable of being exported by a single producer. Imports can increase the number of units available up to this number. \n\nFor example, if a colony needs 5 units of a commodity, but the maximum export is 4, then that colony will only have 4 units, and a \"global shortage\" of 1.",
                            pad);

                    final float tpX = textX1 + textW1 + opad;
                    final float tpY = baseY;

                    ((CustomPanelAPI) getParent()).addUIElement(tooltip).inTL(tpX, tpY);
                    ((CustomPanelAPI) getParent()).bringComponentToTop(tooltip);

                    return tooltip;
                }

                @Override
                public void removeTooltip(TooltipMakerAPI tooltip) {
                    ((CustomPanelAPI) getParent()).removeComponent(tooltip);
                }

                @Override
                public void initializePlugin(boolean hasPlugin) {
                    super.initializePlugin(hasPlugin);

                    ((LtvCustomPanelPlugin) m_panel.getPlugin()).setTargetUIState(UIStateType.DETAIL_DIALOG);
                }
            };

            tooltip.addComponent(textPanel.getPanel()).inTL(textPanel.textX1, baseY);
        }
        { // Total faction exports
            LtvTextPanel textPanel = new LtvTextPanel(
                    m_parentWrapper.getRoot(), section, m_parentWrapper.m_market, 210, 0,
                    new LtvCustomPanelPlugin(), m_parentWrapper.getFaction().getBaseUIColor()) {

                @Override
                public void createPanel() {
                    TooltipMakerAPI tooltip = m_panel.createUIElement(210, 0, false);

                    String factionName = m_parentWrapper.m_faction.getDisplayNameLong();
                    Color factionColor = baseColor;
                    if (m_selectedMarket != null) {
                        factionName = m_selectedMarket.getFaction().getDisplayNameLong();
                        factionColor = m_selectedMarket.getFaction().getBaseUIColor();
                    }

                    String txt = "Total " + factionName + " exports";

                    String valueTxt = Integer.toString(getTotalFactionImports(m_com.getId(),
                    m_parentWrapper.m_faction));

                    tooltip.setParaFontColor(factionColor);
                    tooltip.setParaFont(Fonts.ORBITRON_12);
                    LabelAPI lbl1 = tooltip.addPara(txt, pad);
                    lbl1.setHighlightOnMouseover(true);

                    tooltip.setParaFontColor(factionColor);
                    tooltip.setParaFont(Fonts.INSIGNIA_VERY_LARGE);
                    LabelAPI lbl2 = tooltip.addPara(valueTxt, pad);
                    lbl2.setHighlightOnMouseover(true);

                    float textH1 = lbl1.getPosition().getHeight();
                    textW1 = lbl1.computeTextWidth(txt);
                    textX1 = SECT1_WIDTH*2 / 3f;

                    lbl1.getPosition().inTL(0, 0).setSize(textW1, lbl1.getPosition().getHeight());

                    float textW2 = lbl2.computeTextWidth(valueTxt);
                    float textX2 = (textW1 / 2) - (textW2 / 2);

                    lbl2.getPosition().inTL(textX2, textH1 + pad).setSize(textW2, lbl2.getPosition().getHeight());

                    m_panel.addUIElement(tooltip).inTL(0, 0);

                    m_panel.getPosition().setSize(tooltip.getWidthSoFar(), tooltip.getHeightSoFar());
                }

                @Override
                public TooltipMakerAPI createTooltip() {
                    TooltipMakerAPI tooltip = ((CustomPanelAPI) getParent()).createUIElement(460, 0, false);

                    tooltip.addPara(
                            "Maximum number of units capable of being exported by a single producer under " + m_parentWrapper.m_faction.getPersonNamePrefix() + " control, to another " + "colony controlled by " + m_parentWrapper.m_faction.getDisplayNameWithArticle() + ".\n\n" + "Either this or the maximum global export will be used, depending on which one " + "is higher given the colony's accessibility-based shipping limits." + "\n\nIn-faction shipping limits are higher.",
                            pad);

                    final float tpX = textX1 - 460 - opad*2;
                    final float tpY = baseY;

                    ((CustomPanelAPI) getParent()).addUIElement(tooltip).inTL(tpX, tpY);
                    ((CustomPanelAPI) getParent()).bringComponentToTop(tooltip);

                    return tooltip;
                }

                @Override
                public void removeTooltip(TooltipMakerAPI tooltip) {
                    ((CustomPanelAPI) getParent()).removeComponent(tooltip);
                }

                @Override
                public void initializePlugin(boolean hasPlugin) {
                    super.initializePlugin(hasPlugin);

                    ((LtvCustomPanelPlugin) m_panel.getPlugin()).setTargetUIState(UIStateType.DETAIL_DIALOG);
                }
            };

            tooltip.addComponent(textPanel.getPanel()).inTL(textPanel.textX1, baseY);
        }

        final int baseRow2Y = baseY * 3 + pad;

        if (m_selectedMarket == null) { // Faction market share
            LtvTextPanel textPanel = new LtvTextPanel(
                    m_parentWrapper.getRoot(), section, m_parentWrapper.m_market, 210, 0,
                    new LtvCustomPanelPlugin(), m_parentWrapper.getFaction().getBaseUIColor()) {

                @Override
                public void createPanel() {
                    TooltipMakerAPI tooltip = m_panel.createUIElement(210, 0, false);

                    String factionName = m_parentWrapper.m_faction.getDisplayNameLong();
                    String txt = factionName + " market share";

                    String valueTxt = m_com.getCommodityMarketData().getMarketSharePercent(m_parentWrapper.m_faction) + "%";

                    tooltip.setParaFontColor(baseColor);
                    tooltip.setParaFont(Fonts.ORBITRON_12);
                    LabelAPI lbl1 = tooltip.addPara(txt, pad);
                    lbl1.setHighlightOnMouseover(true);

                    tooltip.setParaFontColor(baseColor);
                    tooltip.setParaFont(Fonts.INSIGNIA_VERY_LARGE);
                    LabelAPI lbl2 = tooltip.addPara(valueTxt, pad);
                    lbl2.setHighlightOnMouseover(true);

                    float textH1 = lbl1.getPosition().getHeight();
                    textW1 = lbl1.computeTextWidth(txt);
                    textX1 = (SECT1_WIDTH / 2f) - (textW1 / 2f);

                    lbl1.getPosition().inTL(0, 0).setSize(textW1, lbl1.getPosition().getHeight());

                    float textW2 = lbl2.computeTextWidth(valueTxt);
                    float textX2 = (textW1 / 2) - (textW2 / 2);

                    lbl2.getPosition().inTL(textX2, textH1 + pad).setSize(textW2, lbl2.getPosition().getHeight());

                    m_panel.addUIElement(tooltip).inTL(0, 0);

                    m_panel.getPosition().setSize(tooltip.getWidthSoFar(), tooltip.getHeightSoFar());
                }

                @Override
                public TooltipMakerAPI createTooltip() {
                    TooltipMakerAPI tooltip = ((CustomPanelAPI) getParent()).createUIElement(460, 0, false);

                    String marketOwner = m_parentWrapper.m_faction.isPlayerFaction() ?
                        "your" : m_parentWrapper.m_faction.getPersonNamePrefix(); 

                    tooltip.addPara(
                            "Total export market share for " + m_com.getCommodity().getName() + " for all colonies under " + marketOwner + " control.",
                            pad);

                    final float tpX = textX1 + textW1 + opad;
                    final float tpY = baseRow2Y;

                    ((CustomPanelAPI) getParent()).addUIElement(tooltip).inTL(tpX, tpY);
                    ((CustomPanelAPI) getParent()).bringComponentToTop(tooltip);

                    return tooltip;
                }

                @Override
                public void removeTooltip(TooltipMakerAPI tooltip) {
                    ((CustomPanelAPI) getParent()).removeComponent(tooltip);
                }

                @Override
                public void initializePlugin(boolean hasPlugin) {
                    super.initializePlugin(hasPlugin);

                    ((LtvCustomPanelPlugin) m_panel.getPlugin()).setTargetUIState(UIStateType.DETAIL_DIALOG);
                }
            };

            tooltip.addComponent(textPanel.getPanel()).inTL(textPanel.textX1, baseRow2Y);
        }

        else { // Faction market share
            LtvTextPanel textPanelLeft = new LtvTextPanel(
                    m_parentWrapper.getRoot(), section, m_parentWrapper.m_market, 210, 0,
                    new LtvCustomPanelPlugin(), m_parentWrapper.getFaction().getBaseUIColor()) {

                @Override
                public void createPanel() {
                    TooltipMakerAPI tooltip = m_panel.createUIElement(210, 0, false);

                    String factionName = m_selectedMarket.getFaction().getDisplayNameLong();
                    String txt = factionName + " market share";

                    String valueTxt = m_com.getCommodityMarketData().getMarketSharePercent(m_selectedMarket.getFaction()) + "%";

                    tooltip.setParaFontColor(m_selectedMarket.getFaction().getBaseUIColor());
                    tooltip.setParaFont(Fonts.ORBITRON_12);
                    LabelAPI lbl1 = tooltip.addPara(txt, pad);
                    lbl1.setHighlightOnMouseover(true);

                    tooltip.setParaFontColor(m_selectedMarket.getFaction().getBaseUIColor());
                    tooltip.setParaFont(Fonts.INSIGNIA_VERY_LARGE);
                    LabelAPI lbl2 = tooltip.addPara(valueTxt, pad);
                    lbl2.setHighlightOnMouseover(true);

                    float textH1 = lbl1.getPosition().getHeight();
                    textW1 = lbl1.computeTextWidth(txt);
                    textX1 = (SECT1_WIDTH / 3f) - (textW1 / 2f);

                    lbl1.getPosition().inTL(0, 0).setSize(textW1, lbl1.getPosition().getHeight());

                    float textW2 = lbl2.computeTextWidth(valueTxt);
                    float textX2 = (textW1 / 2f) - (textW2 / 2f);

                    lbl2.getPosition().inTL(textX2, textH1 + pad).setSize(textW2, lbl2.getPosition().getHeight());

                    m_panel.addUIElement(tooltip).inTL(0, 0);

                    m_panel.getPosition().setSize(tooltip.getWidthSoFar(), tooltip.getHeightSoFar());
                }

                @Override
                public TooltipMakerAPI createTooltip() {
                    TooltipMakerAPI tooltip = ((CustomPanelAPI) getParent()).createUIElement(460, 0, false);

                    tooltip.addPara(
                        "Total export market share for " + m_com.getCommodity().getName() + " for all colonies under " + m_selectedMarket.getFaction().getDisplayNameLong() + " control.",
                        pad);

                    final float tpX = textX1 + textW1 + opad;
                    final float tpY = baseRow2Y;

                    ((CustomPanelAPI) getParent()).addUIElement(tooltip).inTL(tpX, tpY);
                    ((CustomPanelAPI) getParent()).bringComponentToTop(tooltip);

                    return tooltip;
                }

                @Override
                public void removeTooltip(TooltipMakerAPI tooltip) {
                    ((CustomPanelAPI) getParent()).removeComponent(tooltip);
                }

                @Override
                public void initializePlugin(boolean hasPlugin) {
                    super.initializePlugin(hasPlugin);

                    ((LtvCustomPanelPlugin) m_panel.getPlugin()).setTargetUIState(UIStateType.DETAIL_DIALOG);
                }
            };


            LtvTextPanel textPanelRight = new LtvTextPanel(
                    m_parentWrapper.getRoot(), section, m_parentWrapper.m_market, 210, 0,
                    new LtvCustomPanelPlugin(), m_parentWrapper.getFaction().getBaseUIColor()) {

                @Override
                public void createPanel() {
                    TooltipMakerAPI tooltip = m_panel.createUIElement(210, 0, false);

                    String factionName = m_parentWrapper.m_faction.getDisplayNameLong();
                    String txt = factionName + " market share";

                    String valueTxt = m_com.getCommodityMarketData().getMarketSharePercent(m_parentWrapper.m_faction) + "%";

                    tooltip.setParaFontColor(baseColor);
                    tooltip.setParaFont(Fonts.ORBITRON_12);
                    LabelAPI lbl1 = tooltip.addPara(txt, pad);
                    lbl1.setHighlightOnMouseover(true);

                    tooltip.setParaFontColor(baseColor);
                    tooltip.setParaFont(Fonts.INSIGNIA_VERY_LARGE);
                    LabelAPI lbl2 = tooltip.addPara(valueTxt, pad);
                    lbl2.setHighlightOnMouseover(true);

                    float textH1 = lbl1.getPosition().getHeight();
                    textW1 = lbl1.computeTextWidth(txt);
                    textX1 = (SECT1_WIDTH*2 / 3f) - (textW1 / 2f);

                    lbl1.getPosition().inTL(0, 0).setSize(textW1, lbl1.getPosition().getHeight());

                    float textW2 = lbl2.computeTextWidth(valueTxt);
                    float textX2 = (textW1 / 2) - (textW2 / 2);

                    lbl2.getPosition().inTL(textX2, textH1 + pad).setSize(textW2, lbl2.getPosition().getHeight());

                    m_panel.addUIElement(tooltip).inTL(0, 0);

                    m_panel.getPosition().setSize(tooltip.getWidthSoFar(), tooltip.getHeightSoFar());
                }

                @Override
                public TooltipMakerAPI createTooltip() {
                    TooltipMakerAPI tooltip = ((CustomPanelAPI) getParent()).createUIElement(460, 0, false);

                    String marketOwner = m_parentWrapper.m_faction.isPlayerFaction() ?
                        "your" : m_parentWrapper.m_faction.getPersonNamePrefix(); 

                    tooltip.addPara(
                            "Total export market share for " + m_com.getCommodity().getName() + " for all colonies under " + marketOwner + " control.",
                            pad);

                    final float tpX = textX1 + textW1 + opad;
                    final float tpY = baseRow2Y;

                    ((CustomPanelAPI) getParent()).addUIElement(tooltip).inTL(tpX, tpY);
                    ((CustomPanelAPI) getParent()).bringComponentToTop(tooltip);

                    return tooltip;
                }

                @Override
                public void removeTooltip(TooltipMakerAPI tooltip) {
                    ((CustomPanelAPI) getParent()).removeComponent(tooltip);
                }

                @Override
                public void initializePlugin(boolean hasPlugin) {
                    super.initializePlugin(hasPlugin);

                    ((LtvCustomPanelPlugin) m_panel.getPlugin()).setTargetUIState(UIStateType.DETAIL_DIALOG);
                }
            };

            tooltip.addComponent(textPanelLeft.getPanel()).inTL(textPanelLeft.textX1, baseRow2Y);
            tooltip.addComponent(textPanelRight.getPanel()).inTL(textPanelRight.textX1, baseRow2Y);
        }
    }

    private void createSection2(CustomPanelAPI section, TooltipMakerAPI tooltip) {
        final int mapHeight = (int) section.getPosition().getHeight() - 2 * opad;

        StarSystemAPI starSystem = m_parentWrapper.m_market.getStarSystem();
        String title = m_parentWrapper.m_market.getName();

        MapParams params = new MapParams();
        params.showFilter = false;
        params.showTabs = false;
        params.withLayInCourse = false;
        params.starSelectionRadiusMult = 0f;

        params.showSystem(starSystem);

        if (m_selectedMarket != null) {
            title += " and " + m_selectedMarket.getName();

            params.showSystem(m_selectedMarket.getStarSystem());
            params.positionToShowAllMarkersAndSystems(false, mapHeight);
        }

        UIPanelAPI map = tooltip.createSectorMap(
            section.getPosition().getWidth(),
            mapHeight,
            params,
            title
        );

        tooltip.addCustom(map, 0);
        map.getPosition().inTL(0, 0);
    }

    private void createSection3(CustomPanelAPI section, TooltipMakerAPI tooltip) {
        for (MarketAPI market : Global.getSector().getEconomy().getMarketsCopy()) {

            String iconPath = market.getFaction().getCrest();
            LtvSpritePanel iconPanel = new LtvSpritePanel(m_parentWrapper.getRoot(), section, market, iconSize,
                    iconSize, new LtvSpritePanelPlugin(), iconPath, null, null, false);
            iconPanel.getPlugin().setGlowType(GlowType.NONE);

            String marketName = market.getName();

            int marketSize = market.getSize();

            String factionName = market.getFaction().getDisplayName();

            int production = 0;
            for (Industry industry : market.getIndustries()) {
                production += industry.getSupply(m_com.getId()).getQuantity().getModifiedInt();
            }

            float accessibility = market.getAccessibilityMod().getFlatBonus();

            float marketSharePercent = market.getCommodityData(m_com.getId())
                    .getCommodityMarketData().getMarketValuePercent(market);

            String incomeText = "---";
            if (market.isPlayerOwned()) {
                int exportIncome = market.getCommodityData(m_com.getId()).getExportIncome();

                incomeText = Misc.getDGSCredits(exportIncome) + Strings.C;
            }

        }
    }

    private void createSection4(CustomPanelAPI section) {
        CustomUIPanelPlugin comPanelPlugin = new LtvCustomPanelPlugin();

        LtvCommodityPanel comPanel = new LtvCommodityPanel(
                m_parentWrapper.getRoot(),
                (UIPanelAPI) section,
                (int) section.getPosition().getWidth(),
                (int) section.getPosition().getHeight(),
                m_parentWrapper.m_market,
                comPanelPlugin,
                m_parentWrapper.m_market.getName() + " - Commodities",
                true);
        comPanel.setRowSelectable(true);

        comPanel.setCommoditySelectionListener(new CommoditySelectionListener() {
            @Override
            public void onCommoditySelected(CommodityOnMarketAPI selectedCommodity) {
                m_com = selectedCommodity;

                // Update UI
                updateSection1();
                updateSection3();
            }
        });

        section.addComponent(comPanel.getPanel());
    }

    int getTotalGlobalExports(String comID) {
        int total = 0;

        for (MarketAPI market : Global.getSector().getEconomy().getMarketsCopy()) {
            CommodityOnMarketAPI comOnMarket = market.getCommodityData(comID);

            CommodityStats stats = new CommodityStats(comOnMarket, market);

            total += stats.globalExport;
        }

        return total;
    }

    int getTotalFactionImports(String comID, FactionAPI faction) {
        int total = 0;

        for (MarketAPI market : Global.getSector().getEconomy().getMarketsCopy()) {
            if (!market.getFaction().getId().equals(faction.getId())) {
                continue;
            }
            CommodityOnMarketAPI comOnMarket = market.getCommodityData(comID);

            CommodityStats stats = new CommodityStats(comOnMarket, market);
            total += stats.globalExport;
        }

        return total;
    }

    @Override
    public void customDialogConfirm() {
        LtvUIState.setState(UIStateType.NONE);
    }

    @Override
    public void customDialogCancel() {
        LtvUIState.setState(UIStateType.NONE);
    }

    public float getCustomDialogWidth() {
        return PANEL_W;
    }

    public float getCustomDialogHeight() {
        return PANEL_H;
    }

    public String getCancelText() {
        return "Dismiss";
    }

    public String getConfirmText() {
        return "Dismiss";
    }

    public boolean hasCancelButton() {
        return false;
    }

    public CustomUIPanelPlugin getCustomPanelPlugin() {
        return m_plugin;
    }
}

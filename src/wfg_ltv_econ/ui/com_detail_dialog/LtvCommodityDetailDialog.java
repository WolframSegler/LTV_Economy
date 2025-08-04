package wfg_ltv_econ.ui.com_detail_dialog;

import java.awt.Color;

import org.lwjgl.input.Keyboard;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CustomDialogDelegate;
import com.fs.starfarer.api.campaign.CustomUIPanelPlugin;
import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.campaign.econ.CommodityOnMarketAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.combat.MutableStat.StatMod;
import com.fs.starfarer.api.impl.campaign.ids.Strings;
import com.fs.starfarer.api.impl.codex.CodexDataV2;
import com.fs.starfarer.api.ui.Alignment;
import com.fs.starfarer.api.ui.ButtonAPI;
import com.fs.starfarer.api.ui.CustomPanelAPI;
import com.fs.starfarer.api.ui.CutStyle;
import com.fs.starfarer.api.ui.Fonts;
import com.fs.starfarer.api.ui.LabelAPI;
import com.fs.starfarer.api.ui.MapParams;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI.StatModValueGetter;
import com.fs.starfarer.api.ui.UIPanelAPI;
import com.fs.starfarer.api.ui.ButtonAPI.UICheckboxSize;
import com.fs.starfarer.api.util.Misc;
import com.fs.starfarer.campaign.ui.marketinfo.CommodityDetailDialog;
import wfg_ltv_econ.plugins.LtvCommodityDetailDialogPlugin;
import wfg_ltv_econ.plugins.LtvSpritePanelPlugin;
import wfg_ltv_econ.plugins.LtvCustomPanelPlugin;
import wfg_ltv_econ.plugins.LtvCustomPanelPlugin.Glow;
import wfg_ltv_econ.ui.LtvBasePanel;
import wfg_ltv_econ.ui.LtvCommodityPanel;
import wfg_ltv_econ.ui.LtvCustomPanel;
import wfg_ltv_econ.ui.LtvIconPanel;
import wfg_ltv_econ.ui.LtvSpritePanel;
import wfg_ltv_econ.ui.LtvTextPanel;
import wfg_ltv_econ.ui.LtvUIState;
import wfg_ltv_econ.ui.LtvUIState.UIState;
import wfg_ltv_econ.ui.SortableTable;
import wfg_ltv_econ.util.CommodityStats;
import wfg_ltv_econ.util.NumFormat;
import wfg_ltv_econ.util.ReflectionUtils;
import wfg_ltv_econ.util.TooltipUtils;
import wfg_ltv_econ.util.UiUtils;

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

    public LtvTextPanel footerPanel = null;
    public ButtonAPI producerButton = null;
    public ButtonAPI consumerButton = null;

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
        LtvUIState.setState(UIState.DETAIL_DIALOG);

        // Panel for black BG
        LtvBasePanel BgPanel = new LtvBasePanel(
            m_parentWrapper.getRoot(),
            panel,
            m_parentWrapper.m_market,
            (int) panel.getPosition().getWidth(),
            (int) panel.getPosition().getHeight(),
            new LtvCustomPanelPlugin()
        );
        BgPanel.setBgColor(new Color(0, 0, 0));
        BgPanel.getPlugin().setBackgroundTransparency(1);

        panel.addComponent(BgPanel.getPanel()).inBL(0, 0);
        m_dialogPanel = BgPanel.getPanel();

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

        footerPanel = new LtvTextPanel(
                m_parentWrapper.getRoot(), BgPanel.getPanel(), m_parentWrapper.m_market, 400, footerH,
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
                LabelAPI txt = footer.addPara("Only show colonies with excess stockpiles or shortages (%s)", 0f, highlight, "Q");
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
                m_panel.bringComponentToTop(tooltip);

                return tooltip;
            }

            @Override
            public void removeTooltip(TooltipMakerAPI tooltip) {
                m_panel.removeComponent(tooltip);
            }

            @Override
            public void initializePlugin(boolean hasPlugin) {
                super.initializePlugin(hasPlugin);

                ((LtvCustomPanelPlugin) m_panel.getPlugin()).setTargetUIState(UIState.DETAIL_DIALOG);
            }
        };

        BgPanel.getPanel().addComponent(footerPanel.getPanel()).inBL(pad, -opad * 3.5f);
        m_plugin.init(false, false, BgPanel.getPanel());
    }

    public void createSections() {
        if (m_dialogPanel == null) {
            return;
        }

        updateSection1();
        updateSection2();
        updateSection3(0);
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
        section3.getPosition().belowLeft(section1, opad);
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

    /**
     *  MODE_0: Displays the Producers <br></br>
     *  MODE_1: Displays the Consumers
     */
    public void updateSection3(int mode) {
        if (section3 != null) {
            // Otherwise the anchor of the tooltips gets removed before the tooltips, causing a crash.
            ReflectionUtils.invoke(section3, "clearChildren");
        }
        m_dialogPanel.removeComponent(section3);

        section3 = Global.getSettings().createCustom(SECT3_WIDTH, SECT3_HEIGHT, null);
        TooltipMakerAPI tooltip = section3.createUIElement(SECT3_WIDTH, SECT3_HEIGHT, false);
        section3.addUIElement(tooltip).inTL(0, 0);

        createSection3(section3, tooltip, mode);
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

        String comIconID = m_com.getCommodity().getIconName();

        LtvIconPanel iconLeft = new LtvIconPanel(m_parentWrapper.getRoot(), section, m_parentWrapper.m_market,
                iconSize, iconSize, new LtvSpritePanelPlugin(), comIconID, null, null, false);
        iconLeft.setCommodity(m_com);

        iconLeft.getPanelPos().inTL(opad * 3,
                (SECT1_HEIGHT - iconSize) / 2 + headerHeight);
        section.addComponent(iconLeft.getPanel());

        LtvIconPanel iconRight = new LtvIconPanel(m_parentWrapper.getRoot(), section, m_parentWrapper.m_market,
                iconSize, iconSize, new LtvSpritePanelPlugin(), comIconID, null, null, false);
        iconRight.setCommodity(m_com);

        iconRight.getPanelPos().inTL(SECT1_WIDTH - iconSize - opad * 3,
                (SECT1_HEIGHT - iconSize) / 2 + headerHeight);
        section.addComponent(iconRight.getPanel());

        // Text
        final int baseY = (int) (headerHeight + opad * 1.5f);
        final Color baseColor = m_parentWrapper.getFaction().getBaseUIColor();
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

                    ((LtvCustomPanelPlugin) m_panel.getPlugin()).setTargetUIState(UIState.DETAIL_DIALOG);
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
                        "The total number of " + m_com.getCommodity().getName() + " being exported globally by all producing markets in the sector.\n\n" +
                        "This figure reflects the total global supply that reaches exportable surplus after local and in-faction demand is met. It serves as a measure of how widely and heavily the commodity is being produced relative to consumption, and can indicate its availability or strategic importance.", pad
                    );

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

                    ((LtvCustomPanelPlugin) m_panel.getPlugin()).setTargetUIState(UIState.DETAIL_DIALOG);
                }
            };

            tooltip.addComponent(textPanel.getPanel()).inTL(textPanel.textX1, baseY);
        }
        { // Total faction exports

            final MarketAPI currMarket;

            if (m_selectedMarket != null) {
                currMarket = m_selectedMarket;
            } else {
                currMarket = m_parentWrapper.m_market;
            }

            LtvTextPanel textPanel = new LtvTextPanel(
                    m_parentWrapper.getRoot(), section, currMarket, 210, 0,
                    new LtvCustomPanelPlugin(), m_parentWrapper.getFaction().getBaseUIColor()) {

                @Override
                public void createPanel() {
                    TooltipMakerAPI tooltip = m_panel.createUIElement(210, 0, false);
                    
                    final String factionName = getFaction().getDisplayName();
                    final Color factionColor = getFaction().getBaseUIColor();

                    String txt = "Total " + factionName + " exports";

                    final String globalValue = NumFormat.engNotation(
                        getFactionGlobalExports(m_com.getId(), getFaction()));
                    final String inFactionValue = NumFormat.engNotation(
                        getTotalFactionExports(m_com.getId(), getFaction()));

                    String valueTxt = globalValue + "  |  " + inFactionValue;

                    tooltip.setParaFontColor(factionColor);
                    tooltip.setParaFont(Fonts.ORBITRON_12);
                    LabelAPI lbl1 = tooltip.addPara(txt, pad);
                    lbl1.setHighlightOnMouseover(true);

                    tooltip.setParaFontColor(Misc.getBasePlayerColor());
                    tooltip.setParaFont(Fonts.INSIGNIA_VERY_LARGE);
                    LabelAPI lbl2 = tooltip.addPara(valueTxt, pad, new Color[] {
                        factionColor, UiUtils.getInFactionColor()
                    }, new String[] {
                        globalValue, inFactionValue
                    });
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
                        "The total number of units exported to all consumers globally, as well as the total exported within the faction under " + getFaction().getPersonNamePrefix() + " control.\n\n" +
                        "Global exports are limited by the colony's accessibility and cannot exceed the maximum global export capacity.\n\n" +
                        "In-faction exports benefit from higher shipping limits.",
                        pad, new Color[] {Misc.getBasePlayerColor(), UiUtils.getInFactionColor()},
                        new String[] {"all consumers globally", "within the faction"}
                    );

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

                    ((LtvCustomPanelPlugin) m_panel.getPlugin()).setTargetUIState(UIState.DETAIL_DIALOG);
                }
            };

            tooltip.addComponent(textPanel.getPanel()).inTL(textPanel.textX1, baseY);
        }

        final int baseRow2Y = baseY * 3 + pad;

        if (m_selectedMarket == null || m_selectedMarket.isPlayerOwned()) { // Faction market share
            LtvTextPanel textPanel = new LtvTextPanel(
                    m_parentWrapper.getRoot(), section, m_parentWrapper.m_market, 210, 0,
                    new LtvCustomPanelPlugin(), m_parentWrapper.getFaction().getBaseUIColor()) {

                @Override
                public void createPanel() {
                    TooltipMakerAPI tooltip = m_panel.createUIElement(210, 0, false);

                    String factionName = m_parentWrapper.getFaction().getDisplayName();
                    String txt = factionName + " market share";

                    String valueTxt = m_com.getCommodityMarketData().getMarketSharePercent(m_parentWrapper.getFaction()) + "%";

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

                    String marketOwner = m_parentWrapper.getFaction().isPlayerFaction() ?
                        "your" : m_parentWrapper.getFaction().getPersonNamePrefix(); 

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

                    ((LtvCustomPanelPlugin) m_panel.getPlugin()).setTargetUIState(UIState.DETAIL_DIALOG);
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

                    String factionName = m_selectedMarket.getFaction().getDisplayName();
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
                        "Total export market share for " + m_com.getCommodity().getName() + " for all colonies under " + m_selectedMarket.getFaction().getDisplayName() + " control.",
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

                    ((LtvCustomPanelPlugin) m_panel.getPlugin()).setTargetUIState(UIState.DETAIL_DIALOG);
                }
            };


            LtvTextPanel textPanelRight = new LtvTextPanel(
                    m_parentWrapper.getRoot(), section, m_parentWrapper.m_market, 210, 0,
                    new LtvCustomPanelPlugin(), m_parentWrapper.getFaction().getBaseUIColor()) {

                @Override
                public void createPanel() {
                    TooltipMakerAPI tooltip = m_panel.createUIElement(210, 0, false);

                    String factionName = m_parentWrapper.getFaction().getDisplayName();
                    String txt = factionName + " market share";

                    String valueTxt = m_com.getCommodityMarketData().getMarketSharePercent(m_parentWrapper.getFaction()) + "%";

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

                    String marketOwner = m_parentWrapper.getFaction().isPlayerFaction() ?
                        "your" : m_parentWrapper.getFaction().getPersonNamePrefix(); 

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

                    ((LtvCustomPanelPlugin) m_panel.getPlugin()).setTargetUIState(UIState.DETAIL_DIALOG);
                }
            };

            tooltip.addComponent(textPanelLeft.getPanel()).inTL(textPanelLeft.textX1, baseRow2Y);
            tooltip.addComponent(textPanelRight.getPanel()).inTL(textPanelRight.textX1, baseRow2Y);
        }
    }

    private void createSection2(CustomPanelAPI section, TooltipMakerAPI tooltip) {
        final int mapHeight = (int) section.getPosition().getHeight() - 2 * opad;

        final StarSystemAPI starSystem = m_parentWrapper.m_market.getStarSystem();
        String title = m_parentWrapper.m_market.getName();

        MapParams params = new MapParams();
        params.showFilter = false;
        params.showTabs = false;
        params.withLayInCourse = false;
        params.skipCurrLocMarkerRendering = true;
        params.starSelectionRadiusMult = 0f;

        params.showSystem(starSystem);
        params.showMarket(m_parentWrapper.m_market, 1);

        if (m_selectedMarket != null) {
            title += " and " + m_selectedMarket.getName();

            params.showSystem(m_selectedMarket.getStarSystem());
            params.showMarket(m_selectedMarket, 1);
        }
        
        params.positionToShowAllMarkersAndSystems(false, mapHeight);

        UIPanelAPI map = tooltip.createSectorMap(
            section.getPosition().getWidth(),
            mapHeight,
            params,
            title
        );

        tooltip.addCustom(map, 0);
        map.getPosition().inTL(0, 0);
    }

    private void createSection3(CustomPanelAPI section, TooltipMakerAPI tooltip, int mode) {

        // Table buttons
        final int btnWidth = 200;
        final int btnHeight = 18;

        producerButton = tooltip.addButton(
            "Producers",
            null,
            Misc.getBasePlayerColor(),
            Misc.getDarkPlayerColor(),
            Alignment.MID,
            CutStyle.TOP,
            btnWidth, btnHeight, pad
        );
        consumerButton = tooltip.addButton(
            "Consumers",
            null,
            Misc.getBasePlayerColor(),
            Misc.getDarkPlayerColor(),
            Alignment.MID,
            CutStyle.TOP,
            btnWidth, btnHeight, pad
        );

        producerButton.setShortcut(Keyboard.KEY_1, false);
        consumerButton.setShortcut(Keyboard.KEY_2, false);

        producerButton.getPosition().inTL(0, -btnHeight);
        consumerButton.getPosition().inTL(btnWidth, -btnHeight);
        
        if (mode == 0) {
            producerButton.setChecked(true);
            consumerButton.setChecked(false);

            producerButton.highlight();
        } else if (mode == 1) {
            producerButton.setChecked(false);
            consumerButton.setChecked(true);

            consumerButton.highlight();
        }

        SortableTable table = new SortableTable(
            m_parentWrapper.getRoot(),
            section,
            SECT3_WIDTH,
            SECT3_HEIGHT,
            m_parentWrapper.m_market
        );

        final String marketHeader = mode == 0 ? "Mkt Share" : "Mkt percent";
        final String creditHeader = mode == 0 ? "Income" : "Value";

        final String QuantityDesc = mode == 0 ? "Shows units of the commodity that could be exported."
        :
        "Shows demand for a commodity.";

        final String marketTpDesc = mode == 0 ? "What percentage of the global market value the colony receives as income from its exports of the commodity.\n\nThe market share is affected by the number of units produced and the colony's accessibility." 
        :
        "The portion of the global market value that this colony contributes.\n\nIncludes player-controlled colonies, whose market value does not contribue to the global market value the player can gain export income from.";
        
        final String creditTpDesc = mode == 0 ? "How much income the colony is getting from exporting its production of the commodity. A lack of income means that the export activity is underground, most likely due to the commodity being illegal.\n\nIncome also depends on colony stability, so may not directly correlate with market share." 
        :
        "How much the colony's demand contributes to the global market value for the commodity.";

        table.addHeaders(
            "", (int)(0.04 * SECT3_WIDTH), null, true, false, 1,
            "Colony", (int)(0.18 * SECT3_WIDTH), "Colony name.", true, true, 1,
            "Size", (int)(0.09 * SECT3_WIDTH), "Colony size.", false, false, -1,
            "Faction", (int)(0.17 * SECT3_WIDTH), "Faction that controls this colony.", false, false, -1,
            "Quantity", (int)(0.15 * SECT3_WIDTH), QuantityDesc, false, false, -1,
            "Access", (int)(0.11 * SECT3_WIDTH), "A colony's accessibility. The number in parentheses is the maximum out-of-faction shipping capacity, which limits how many units the colony can import, and how much its demand contributes to the global market value.\n\nIn-faction accessibility and shipping capacity are higher.", false, false, -1,
            marketHeader, (int)(0.15 * SECT3_WIDTH), marketTpDesc, false, false, -1,
            creditHeader, (int)(0.11 * SECT3_WIDTH), creditTpDesc, false, false, -1
        );

        for (MarketAPI market : Global.getSector().getEconomy().getMarketsCopy()) {

            if (market.isHidden()) {
                continue;
            }
            final CommodityStats comStats = new CommodityStats(m_com, market);
            if (comStats.globalExport < 1 && mode == 0 || comStats.localDeficit < 1 && mode == 1) {
                continue;
            }

            if (footerPanel != null && footerPanel.m_checkbox.isChecked() &&
                (comStats.canNotExport > 0 || comStats.localDeficit > 0)) {
                continue;
            }

            String iconPath = market.getFaction().getCrest();
            LtvSpritePanel iconPanel = new LtvSpritePanel(
                m_parentWrapper.getRoot(), section, market, iconSize, iconSize, new LtvSpritePanelPlugin(), 
                iconPath, null, null, comStats.localDeficit > 0
            );
            iconPanel.getPlugin().setGlow(Glow.NONE);

            String marketName = market.getName();

            int marketSize = market.getSize();

            String factionName = market.getFaction().getDisplayName();

            String quantityTxt = NumFormat.engNotation(
                mode == 0 ? comStats.globalExport : comStats.externalImports
            );

            int accessibility = (int) (market.getAccessibilityMod().computeEffective(0) * 100);
            int maxExportCapacity = Global.getSettings().getShippingCapacity(market, false);

            String access = (accessibility) + "% (" + maxExportCapacity + ")";

            int marketShare = market.getCommodityData(m_com.getId())
                    .getCommodityMarketData().getMarketValuePercent(market);
            String marketSharePercent = marketShare + "%";

            String incomeText = "---";
            int exportIncome = 0;

            if (market.isPlayerOwned()) {

                exportIncome = market.getCommodityData(m_com.getId()).getExportIncome();
                incomeText = Misc.getDGSCredits(exportIncome);
            }

            Color textColor = market.getFaction().getBaseUIColor();

            table.addCell(iconPanel, Alignment.LMID, null, null);
            table.addCell(marketName, Alignment.LMID, null, textColor);
            table.addCell(marketSize, Alignment.MID, null, textColor);
            table.addCell(factionName, Alignment.MID, null, textColor);
            table.addCell(quantityTxt, Alignment.MID, comStats.globalExport, null);
            table.addCell(access, Alignment.MID, accessibility, null);
            table.addCell(marketSharePercent, Alignment.MID, marketShare, null);
            table.addCell(incomeText, Alignment.MID, exportIncome, null);

            // Tooltip
            TooltipMakerAPI tp = createSection3RowsTooltip(
                table, market, marketName, textColor
            );

            table.pushRow(
                CodexDataV2.getCommodityEntryId(m_com.getId()),
                market,
                null,
                m_parentWrapper.getFaction().getDarkUIColor(),
                tp
            );

            if (m_parentWrapper.m_market == market) {
                table.selectLastRow();
            }
        }

        section.addComponent(table.getPanel()).inTL(0,0);

        table.sortRows(6);

        table.createPanel();

        table.setRowSelectionListener(selectedRow -> {
            m_selectedMarket = selectedRow.m_market;
            updateSection1();
            updateSection2();
        });
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
        comPanel.selectRow(m_com.getId());

        comPanel.setCommoditySelectionListener(new CommoditySelectionListener() {
            @Override
            public void onCommoditySelected(CommodityOnMarketAPI selectedCommodity) {
                m_com = selectedCommodity;

                // Update UI
                updateSection1();
                
                final int mode = producerButton.isChecked() ? 0 : 1;

                updateSection3(mode);
            }
        });

        section.addComponent(comPanel.getPanel());
    }

    private int getTotalGlobalExports(String comID) {
        int total = 0;

        for (MarketAPI market : Global.getSector().getEconomy().getMarketsCopy()) {
            CommodityOnMarketAPI comOnMarket = market.getCommodityData(comID);

            CommodityStats stats = new CommodityStats(comOnMarket, market);

            total += stats.globalExport;
        }

        return total;
    }

    private int getTotalFactionExports(String comID, FactionAPI faction) {
        int total = 0;

        for (MarketAPI market : Global.getSector().getEconomy().getMarketsCopy()) {
            if (!market.getFaction().getId().equals(faction.getId())) {
                continue;
            }
            CommodityOnMarketAPI comOnMarket = market.getCommodityData(comID);

            CommodityStats stats = new CommodityStats(comOnMarket, market);
            total += stats.inFactionExport;
        }

        return total;
    }

    private int getFactionGlobalExports(String comID, FactionAPI faction) {
        int total = 0;

        for (MarketAPI market : Global.getSector().getEconomy().getMarketsCopy()) {
            if (market.getFaction() != faction) {
                continue;
            }
            CommodityOnMarketAPI comOnMarket = market.getCommodityData(comID);

            CommodityStats stats = new CommodityStats(comOnMarket, market);

            total += stats.globalExport;
        }

        return total;
    }

    private TooltipMakerAPI createSection3RowsTooltip(SortableTable table, MarketAPI market,
        String marketName, Color baseColor) {

        final int tpWidth = 450;
        final CommodityOnMarketAPI com = market.getCommodityData(m_com.getId());

        TooltipMakerAPI tp = table.getPanel().createUIElement(
            tpWidth, 0, false);

        final FactionAPI faction = market.getFaction();
        final CommodityStats comStats = new CommodityStats(com, market);

        final Color highlight = Misc.getHighlightColor();
        final Color negative = Misc.getNegativeHighlightColor();
        final Color darkColor = faction.getDarkUIColor();

        tp.setParaFont(Fonts.ORBITRON_12);
        tp.addPara(marketName + " - " + com.getCommodity().getName(), baseColor, pad);
        tp.setParaFontDefault();

        String locString = "in the " + market.getContainingLocation().getNameWithLowercaseType();
        if (market.getContainingLocation().isHyperspace()) {
           locString = "in hyperspace";
        }

        tp.addPara(market.getName() + " is a size %s %s colony located " + locString +
            ". Its current stability is %s.", opad, new Color[]{
                highlight, baseColor, highlight
            }, new String[]{
                "" + market.getSize(),
                market.getFaction().getPersonNamePrefix(),
                "" + (int)market.getStabilityValue()
            }
        );

        tp.addPara("Income modifiers:", opad);

        tp.addStatModGrid(tpWidth, 50, opad, pad,
            market.getIncomeMult(), null
        );

        tp.addSectionHeading(com.getCommodity().getName() + " production & availability",
        baseColor, darkColor, Alignment.MID, opad);
            
        TooltipUtils.createCommodityProductionBreakdown(
            tp, com, comStats, highlight, negative
        );

        TooltipUtils.createCommodityDemandBreakdown(
            tp, com, comStats, highlight, negative
        );

        final int econUnit = (int)com.getCommodity().getEconUnit();
        final int sellPrice = (int)market.getDemandPrice(
            com.getId(), (double)econUnit, true) / econUnit;
        final int buyPrice = (int)market.getSupplyPrice(
            com.getId(), (double)econUnit, true) / econUnit;

        if (!com.isMeta()) {
            if (comStats.totalExports > 0) {
                tp.addPara("Excess stockpiles: %s units.", opad, Misc.getPositiveHighlightColor(), 
                highlight, NumFormat.engNotation(comStats.totalExports));
            } else if (comStats.localDeficit > 0) {
                tp.addPara("Local deficit: %s units.", opad, negative, 
                highlight, NumFormat.engNotation(comStats.localDeficit));
            }

            tp.addPara("Can be bought for %s and sold for %s per unit, assuming a batch of %s units traded.", opad, highlight, new String[]{
                Misc.getDGSCredits(buyPrice), Misc.getDGSCredits(sellPrice), Misc.getWithDGS(econUnit)
            });
        }

        tp.addSectionHeading("Colony accessibility", baseColor, darkColor, Alignment.MID, opad);

        int stability = (int) (market.getAccessibilityMod().computeEffective(0) * 100);
        Color valueColor = highlight;
        if (stability <= 0) {
            valueColor = negative;
        }

        tp.addPara("Accessibility: %s", opad, valueColor, stability + "%");

        tp.addStatModGrid(tpWidth, 50.0F, opad, pad, market.getAccessibilityMod(),
            new StatModValueGetter() {
            public String getPercentValue(StatMod value) {
                return null;
            }

            public String getMultValue(StatMod value) {
                return null;
            }

            public Color getModColor(StatMod value) {
                return value.value < 0 ? negative : null;
            }

            public String getFlatValue(StatMod value) {
                return value.value >= 0 ? "+" + Math.round(value.value * 100) + "%" : Math.round(value.value * 100) + "%";
            }
        });

        final String seperator = "   - ";
        int shippingGlobal = Global.getSettings().getShippingCapacity(market, false);
        int shippingInFaction = Global.getSettings().getShippingCapacity(market, true);

        tp.addPara(seperator + "Same-faction imports and exports limited to %s units",
            opad, highlight, NumFormat.engNotation(shippingInFaction));
        tp.addPara(seperator + "Other imports and exports limited to %s units",
            0, highlight, NumFormat.engNotation(shippingGlobal));

        stability = Math.max(stability, 0);
        
        tp.addPara(seperator + "Market share of exports multiplied by %s",
            0, highlight, Strings.X + Misc.getRoundedValue(stability / 100.0F));
        tp.addPara(
            "The same-faction export bonus does not increase market share or income from exports.", opad
        );

        tp.addSpacer(opad + pad);

        return tp;
    }

    @Override
    public void customDialogConfirm() {
        LtvUIState.setState(UIState.NONE);
    }

    @Override
    public void customDialogCancel() {
        LtvUIState.setState(UIState.NONE);
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

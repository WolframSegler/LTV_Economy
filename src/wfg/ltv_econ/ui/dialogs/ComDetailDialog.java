package wfg.ltv_econ.ui.dialogs;

import static wfg.wrap_ui.util.UIConstants.*;

import java.util.concurrent.atomic.AtomicInteger;

import org.lwjgl.input.Keyboard;

import java.awt.Color;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.campaign.econ.CommoditySpecAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.combat.MutableStat.StatMod;
import com.fs.starfarer.api.impl.campaign.ids.Strings;
import com.fs.starfarer.api.impl.codex.CodexDataV2;
import com.fs.starfarer.api.ui.Alignment;
import com.fs.starfarer.api.ui.ButtonAPI;
import com.fs.starfarer.api.ui.ButtonAPI.UICheckboxSize;
import com.fs.starfarer.api.ui.CustomPanelAPI;
import com.fs.starfarer.api.ui.Fonts;
import com.fs.starfarer.api.ui.LabelAPI;
import com.fs.starfarer.api.ui.MapParams;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI.ActionListenerDelegate;
import com.fs.starfarer.api.ui.TooltipMakerAPI.StatModValueGetter;
import com.fs.starfarer.api.ui.UIPanelAPI;
import com.fs.starfarer.api.util.Misc;

import wfg.ltv_econ.configs.EconomyConfigLoader.EconomyConfig;
import wfg.ltv_econ.economy.CommodityCell;
import wfg.ltv_econ.economy.EconomyEngine;
import wfg.ltv_econ.ui.panels.LtvCommodityPanel;
import wfg.ltv_econ.ui.panels.CommodityRowPanel;
import wfg.ltv_econ.ui.panels.reusable.ComIconPanel;
import wfg.ltv_econ.util.TooltipUtils;
import wfg.ltv_econ.util.UiUtils;
import wfg.wrap_ui.ui.Attachments;
import wfg.wrap_ui.ui.UIState;
import wfg.wrap_ui.ui.UIState.State;
import wfg.wrap_ui.ui.dialogs.DialogPanel;
import wfg.wrap_ui.ui.panels.Button;
import wfg.wrap_ui.ui.panels.Button.CutStyle;
import wfg.wrap_ui.ui.panels.CustomPanel;
import wfg.wrap_ui.ui.panels.CustomPanel.HasActionListener;
import wfg.wrap_ui.ui.panels.CustomPanel.HasTooltip.PendingTooltip;
import wfg.wrap_ui.ui.panels.SortableTable;
import wfg.wrap_ui.ui.panels.SortableTable.ColumnManager;
import wfg.wrap_ui.ui.panels.SortableTable.HeaderPanelWithTooltip;
import wfg.wrap_ui.ui.panels.SortableTable.RowManager;
import wfg.wrap_ui.ui.panels.SortableTable.cellAlg;
import wfg.wrap_ui.ui.panels.SpritePanel.Base;
import wfg.wrap_ui.ui.panels.TextPanel;
import wfg.wrap_ui.ui.plugins.BasePanelPlugin;
import wfg.wrap_ui.ui.systems.OutlineSystem.Outline;
import wfg.wrap_ui.util.CallbackRunnable;
import wfg.wrap_ui.util.NumFormat;

public class ComDetailDialog extends DialogPanel implements HasActionListener {

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

    public final static int iconSize = 24;

    private final MarketAPI m_market;

    private CommoditySpecAPI m_com;
    public MarketAPI m_selectedMarket = null;

    public TextPanel footerPanel = null;
    public Button producerButton = null;
    public Button consumerButton = null;
    public LtvCommodityPanel section4ComPanel = null;

    public ComDetailDialog(MarketAPI market, CommoditySpecAPI com) {
        // Measured using very precise tools!! (my eyes)
        this(market, com, 1166, 658 + 20);
    }

    public ComDetailDialog(MarketAPI market, CommoditySpecAPI com, int panelW, int panelH) {
        super(Attachments.getScreenPanel(), panelW, panelH, null, null, "Dismiss");

        PANEL_W = panelW;
        PANEL_H = panelH;

        SECT1_WIDTH = (int) (PANEL_W * 0.76f - opad);
        SECT1_HEIGHT = (int) (PANEL_H * 0.28f - opad);

        SECT2_WIDTH = (int) (PANEL_W * 0.24f - opad);
        SECT2_HEIGHT = (int) (PANEL_H * 0.28f - opad);

        SECT3_WIDTH = (int) (PANEL_W * 0.76f - opad);
        SECT3_HEIGHT = (int) (PANEL_H * 0.72f - opad);

        SECT4_WIDTH = (int) (PANEL_W * 0.24f - opad);
        SECT4_HEIGHT = (int) (PANEL_H * 0.72f - opad);

        m_market = market;
        m_com = com;

        setConfirmShortcut();

        getHolo().setBackgroundAlpha(1, 1);
        backgroundDimAmount = 0f;

        createPanel();
    }

    @Override
    public void createPanel() {
        UIState.setState(State.DIALOG);

        createSections();

        // Footer
        final int footerH = 40;

        final BasePanelPlugin<TextPanel> fPlugin = new BasePanelPlugin<>() {
            @Override
            public void advance(float amount) {
                super.advance(amount);

                if (inputSnapshot.hoveredLastFrame && inputSnapshot.LMBUpLastFrame) {
                    final ButtonAPI checkbox = m_panel.m_checkbox;

                    if (checkbox != null) {
                        checkbox.setChecked(!checkbox.isChecked());
                    }
                }
            }
        };

        footerPanel = new TextPanel(innerPanel, 400, footerH, fPlugin) {
            {
                getPlugin().setTargetUIState(State.DIALOG);
            }

            @Override
            public void createPanel() {
                final TooltipMakerAPI footer = m_panel.createUIElement(PANEL_W, footerH, false);
                footer.setActionListenerDelegate(
                    new ActionListenerDelegate() {
                        public void actionPerformed(Object data, Object source) {
                            updateSection3(producerButton.checked ? 0 : 1);
                        }
                    }
                );
                m_checkbox = footer.addCheckbox(20, 20, "", "stockpile_toggle",
                        Fonts.ORBITRON_12, highlight, UICheckboxSize.SMALL, 0);

                m_checkbox.getPosition().inBL(0, 0);
                m_checkbox.setShortcut(Keyboard.KEY_Q, false);

                footer.setParaFont(Fonts.ORBITRON_12);
                footer.setParaFontColor(m_market.getFaction().getBaseUIColor());
                LabelAPI txt = footer.addPara("Only show colonies with excess stockpiles or shortages (%s)", 0f, highlight, "Q");
                txt.setHighlightOnMouseover(true);

                int TextY = (int) txt.computeTextHeight(txt.getText());
                txt.getPosition().inBL(20 + pad, (20 - TextY) / 2);

                m_panel.addUIElement(footer);
            }

            @Override
            public CustomPanelAPI getTpParent() {
                return m_panel;
            }

            @Override
            public TooltipMakerAPI createAndAttachTp() {
                TooltipMakerAPI tooltip = m_panel.createUIElement(getPos().getWidth() * 0.7f, 0, false);

                tooltip.addPara(
                        "Only show colonies that are either suffering from a shortage or have excess stockpiles.\n\nColonies with excess stockpiles have more of the goods available on the open market and have lower prices.\n\nColonies with shortages have less or none available for sale, and have higher prices.",
                        pad);

                add(tooltip).inBL(0, getPos().getHeight());

                return tooltip;
            }
        };

        innerPanel.addComponent(footerPanel.getPanel()).inBL(pad, pad);
    }

    public void createSections() {
        EconomyEngine.getInstance().fakeAdvance();

        updateSection1();
        updateSection2();
        updateSection3(0);
        updateSection4();
    }

    public void updateSection1() {
        innerPanel.removeComponent(section1);

        section1 = Global.getSettings().createCustom(SECT1_WIDTH, SECT1_HEIGHT, null);

        final TooltipMakerAPI tooltip = section1.createUIElement(SECT1_WIDTH, SECT1_HEIGHT, false);
        section1.addUIElement(tooltip).inTL(0, 0);

        createSection1(section1, tooltip);
        innerPanel.addComponent(section1).inTL(pad, pad);

        // Update anchors
        if (section2 == null || section3 == null) {
            return;
        }
        section2.getPosition().rightOfTop(section1, opad * 1.5f);
        section3.getPosition().belowLeft(section1, opad);
    }

    public void updateSection2() {
        innerPanel.removeComponent(section2);

        section2 = Global.getSettings().createCustom(SECT2_WIDTH, SECT2_HEIGHT, null);

        final TooltipMakerAPI tooltip = section2.createUIElement(SECT2_WIDTH, SECT2_HEIGHT, false);
        section2.addUIElement(tooltip).inTL(0, 0);

        createSection2(section2, tooltip);
        innerPanel.addComponent(section2).rightOfTop(section1, opad * 1.5f);

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
        innerPanel.removeComponent(section3);

        section3 = Global.getSettings().createCustom(SECT3_WIDTH, SECT3_HEIGHT, null);

        createSection3(section3, mode);
        innerPanel.addComponent(section3).belowLeft(section1, opad);
    }

    public void updateSection4() {
        innerPanel.removeComponent(section4);

        section4 = Global.getSettings().createCustom(SECT4_WIDTH, SECT4_HEIGHT, null);

        final TooltipMakerAPI tooltip = section4.createUIElement(SECT4_WIDTH, SECT4_HEIGHT, false);
        section4.addUIElement(tooltip).inTL(0, 0);

        createSection4(section4);
        innerPanel.addComponent(section4).belowLeft(section2, opad);
    }

    private void createSection1(CustomPanelAPI section, TooltipMakerAPI tooltip) {
        if (m_com == null) return;
        final EconomyEngine engine = EconomyEngine.getInstance();

        tooltip.addSectionHeading(m_com.getName(), Alignment.MID, pad);
        final int headerHeight = (int) tooltip.getPrev().getPosition().getHeight();

        // Icons
        final int iconSize = (int) (section.getPosition().getHeight() / 2.2f);

        final String comIconID = m_com.getIconName();

        final ComIconPanel iconLeft = new ComIconPanel(section, m_market.getFaction(),
            iconSize, iconSize, comIconID, null, null
        );
        iconLeft.setCommodity(m_com);

        iconLeft.getPos().inTL(opad * 3,
            (SECT1_HEIGHT - iconSize) / 2 + headerHeight);
        section.addComponent(iconLeft.getPanel());

        final ComIconPanel iconRight = new ComIconPanel(section, m_market.getFaction(),
            iconSize, iconSize, comIconID, null, null
        );
        iconRight.setCommodity(m_com);

        iconRight.getPos().inTL(SECT1_WIDTH - iconSize - opad * 3,
            (SECT1_HEIGHT - iconSize) / 2 + headerHeight);
        section.addComponent(iconRight.getPanel());

        // Text
        final String comID = m_com.getId();
        final int baseY = (int) (headerHeight + opad * 1.5f);
        final Color baseColor = m_market.getFaction().getBaseUIColor();
        { // Global market value
            final TextPanel textPanel = new TextPanel(section, 170, 0, new BasePanelPlugin<>()) {
                {
                    getPlugin().setTargetUIState(State.DIALOG);
                }

                @Override
                public void createPanel() {
                    final TooltipMakerAPI tooltip = m_panel.createUIElement(170, 0, false);

                    final long value = engine.getComDomain(comID)
                        .getMarketActivity();
                    final String txt = "Global market value";
                    String valueTxt = NumFormat.formatCredit(value);
                    if (value < 1) {
                        valueTxt = "---";
                    }

                    tooltip.setParaFontColor(baseColor);
                    tooltip.setParaFont(Fonts.ORBITRON_12);
                    final LabelAPI lbl1 = tooltip.addPara(txt, pad);
                    lbl1.setHighlightOnMouseover(true);

                    tooltip.setParaFontColor(highlight);
                    tooltip.setParaFont(Fonts.INSIGNIA_VERY_LARGE);
                    final LabelAPI lbl2 = tooltip.addPara(valueTxt, highlight, pad);
                    lbl2.setHighlightOnMouseover(true);

                    final float textH1 = lbl1.getPosition().getHeight();
                    textW1 = lbl1.computeTextWidth(txt);
                    textX1 = (SECT1_WIDTH / 3f) - textW1;

                    lbl1.getPosition().inTL(0, 0).setSize(textW1, lbl1.getPosition().getHeight());

                    final float textW2 = lbl2.computeTextWidth(valueTxt);
                    final float textX2 = (textW1 / 2) - (textW2 / 2);

                    lbl2.getPosition().inTL(textX2, textH1 + pad).setSize(textW2, lbl2.getPosition().getHeight());

                    m_panel.addUIElement(tooltip).inTL(0, 0);

                    m_panel.getPosition().setSize(tooltip.getWidthSoFar(), tooltip.getHeightSoFar());
                }

                @Override
                public CustomPanelAPI getTpParent() {
                    return getParent();
                }

                @Override
                public TooltipMakerAPI createAndAttachTp() {
                    final TooltipMakerAPI tooltip = getParent().createUIElement(460, 0, false);

                    final int discount = (int)((1f - EconomyConfig.FACTION_EXCHANGE_MULT)*100);

                    tooltip.addPara(
                        "Total credits spent sector-wide for the import of " +
                        m_com.getName() + ". " +
                        "Colonies with higher accessibility, faction relations and a shorter distance will have priority when exporting.\n\n"
                        +
                        "The value shown here includes the demand at your colonies, " +
                        "since they must import goods as well. In-faction imports have a %s discount.",
                        pad,
                        highlight,
                        discount + "%"
                    );

                    final float tpX = textX1 + textW1 + opad;
                    final float tpY = baseY;

                    getParent().addUIElement(tooltip).inTL(tpX, tpY);

                    return tooltip;
                }
            };

            tooltip.addComponent(textPanel.getPanel()).inTL(textPanel.textX1, baseY);
        }
        { // Total global exports
            final TextPanel textPanel = new TextPanel(section, 170, 0, new BasePanelPlugin<>()) {
                {
                    getPlugin().setTargetUIState(State.DIALOG);
                }

                @Override
                public void createPanel() {
                    TooltipMakerAPI tooltip = m_panel.createUIElement(170, 0, false);

                    String txt = "Total global exports";

                    String valueTxt = NumFormat.engNotation(
                        (long) engine.getTotalGlobalExports(comID)
                    );

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
                public CustomPanelAPI getTpParent() {
                    return getParent();
                }

                @Override
                public TooltipMakerAPI createAndAttachTp() {
                    TooltipMakerAPI tooltip = getParent().createUIElement(460, 0, false);

                    tooltip.addPara(
                        "The total number of " + m_com.getName() + " being exported globally by all producing markets in the sector.\n\n" +
                        "This figure reflects the total global supply that reaches exportable surplus after local and in-faction demand is met.", pad
                    );

                    final float tpX = textX1 + textW1 + opad;
                    final float tpY = baseY;

                    getParent().addUIElement(tooltip).inTL(tpX, tpY);

                    return tooltip;
                }
            };

            tooltip.addComponent(textPanel.getPanel()).inTL(textPanel.textX1, baseY);
        }
        { // Total faction exports

            final MarketAPI currMarket;

            if (m_selectedMarket != null) {
                currMarket = m_selectedMarket;
            } else {
                currMarket = m_market;
            }

            final TextPanel textPanel = new TextPanel(section, 210, 0, new BasePanelPlugin<>()) {
                {
                    getPlugin().setTargetUIState(State.DIALOG);
                }

                @Override
                public void createPanel() {
                    TooltipMakerAPI tooltip = m_panel.createUIElement(210, 0, false);
                    
                    final String factionName = currMarket.getFaction().getDisplayName();
                    final Color factionColor = currMarket.getFaction().getBaseUIColor();

                    String txt = "Total " + factionName + " exports";

                    final String globalValue = NumFormat.engNotation(
                        engine.getFactionTotalGlobalExports(
                            comID, currMarket.getFaction())
                    );
                    final String inFactionValue = NumFormat.engNotation(
                        engine.getTotalInFactionExports(
                            comID, currMarket.getFaction())
                    );

                    String valueTxt = globalValue + "  |  " + inFactionValue;

                    tooltip.setParaFontColor(factionColor);
                    tooltip.setParaFont(Fonts.ORBITRON_12);
                    LabelAPI lbl1 = tooltip.addPara(txt, pad);
                    lbl1.setHighlightOnMouseover(true);

                    tooltip.setParaFontColor(base);
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
                public CustomPanelAPI getTpParent() {
                    return getParent();
                }

                @Override
                public TooltipMakerAPI createAndAttachTp() {
                    TooltipMakerAPI tooltip = getParent().createUIElement(460, 0, false);

                    tooltip.addPara(
                        "The total number of units exported to all consumers globally, as well as the total exported within the faction under " + currMarket.getFaction().getPersonNamePrefix() + " control.\n\n" +
                        "Global exports are shaped by the colony's accessibility, its faction relations and other factors.",
                        pad, new Color[] {currMarket.getFaction().getBaseUIColor(), UiUtils.getInFactionColor()},
                        new String[] {"all consumers globally", "within the faction"}
                    );

                    final float tpX = textX1 - 460 - opad*2;
                    final float tpY = baseY;

                    getParent().addUIElement(tooltip).inTL(tpX, tpY);

                    return tooltip;
                }
            };

            tooltip.addComponent(textPanel.getPanel()).inTL(textPanel.textX1, baseY);
        }

        final int baseRow2Y = baseY * 3 + pad;

        if (m_selectedMarket == null || m_selectedMarket.isPlayerOwned()) { // Faction market share
            final TextPanel textPanel = new TextPanel(section, 250, 0, new BasePanelPlugin<>()) {
                {
                    getPlugin().setTargetUIState(State.DIALOG);
                }

                @Override
                public void createPanel() {
                    final TooltipMakerAPI tooltip = m_panel.createUIElement(250, 0, false);

                    final String factionName = m_market.getFaction().getDisplayName();
                    final String txt = factionName + " market share";

                    final String valueTxt = (int)(engine.getFactionTotalExportMarketShare(
                        comID, m_market.getFaction().getId()
                    ) * 100) + "%";

                    tooltip.setParaFontColor(baseColor);
                    tooltip.setParaFont(Fonts.ORBITRON_12);
                    final LabelAPI lbl1 = tooltip.addPara(txt, pad);
                    lbl1.setHighlightOnMouseover(true);

                    tooltip.setParaFontColor(baseColor);
                    tooltip.setParaFont(Fonts.INSIGNIA_VERY_LARGE);
                    final LabelAPI lbl2 = tooltip.addPara(valueTxt, pad);
                    lbl2.setHighlightOnMouseover(true);

                    final float textH1 = lbl1.getPosition().getHeight();
                    textW1 = lbl1.computeTextWidth(txt);
                    textX1 = (SECT1_WIDTH / 2f) - (textW1 / 2f);

                    lbl1.getPosition().inTL(0, 0).setSize(textW1, lbl1.getPosition().getHeight());

                    final float textW2 = lbl2.computeTextWidth(valueTxt);
                    final float textX2 = (textW1 / 2) - (textW2 / 2);

                    lbl2.getPosition().inTL(textX2, textH1 + pad).setSize(textW2, lbl2.getPosition().getHeight());

                    add(tooltip).inTL(0, 0);

                    m_panel.getPosition().setSize(tooltip.getWidthSoFar(), tooltip.getHeightSoFar());
                }

                @Override
                public CustomPanelAPI getTpParent() {
                    return getParent();
                }

                @Override
                public TooltipMakerAPI createAndAttachTp() {
                    TooltipMakerAPI tooltip = getParent().createUIElement(460, 0, false);

                    final String marketOwner = m_market.getFaction().isPlayerFaction() ?
                        "your" : m_market.getFaction().getPersonNamePrefix(); 

                    tooltip.addPara(
                            "Total export market share for " + m_com.getName() + " for all colonies under " + marketOwner + " control.",
                            pad);

                    final float tpX = textX1 + textW1 + opad;
                    final float tpY = baseRow2Y;

                    getParent().addUIElement(tooltip).inTL(tpX, tpY);

                    return tooltip;
                }
            };

            tooltip.addComponent(textPanel.getPanel()).inTL(textPanel.textX1, baseRow2Y);
        }

        else { // Faction market share
            final TextPanel textPanelLeft = new TextPanel(section, 250, 0) {
                {
                    getPlugin().setTargetUIState(State.DIALOG);
                }

                @Override
                public void createPanel() {
                    final TooltipMakerAPI tooltip = m_panel.createUIElement(250, 0, false);

                    final String factionName = m_selectedMarket.getFaction().getDisplayName();
                    final String txt = factionName + " market share";

                    final String valueTxt = (int) (engine.getFactionTotalExportMarketShare(
                        comID, m_selectedMarket.getFactionId()
                    ) * 100) + "%";

                    tooltip.setParaFontColor(m_selectedMarket.getFaction().getBaseUIColor());
                    tooltip.setParaFont(Fonts.ORBITRON_12);
                    final LabelAPI lbl1 = tooltip.addPara(txt, pad);
                    lbl1.setHighlightOnMouseover(true);

                    tooltip.setParaFontColor(m_selectedMarket.getFaction().getBaseUIColor());
                    tooltip.setParaFont(Fonts.INSIGNIA_VERY_LARGE);
                    final LabelAPI lbl2 = tooltip.addPara(valueTxt, pad);
                    lbl2.setHighlightOnMouseover(true);

                    final float textH1 = lbl1.getPosition().getHeight();
                    textW1 = lbl1.computeTextWidth(txt);
                    textX1 = (SECT1_WIDTH / 3f) - (textW1 / 2f);

                    lbl1.getPosition().inTL(0, 0).setSize(textW1, lbl1.getPosition().getHeight());

                    final float textW2 = lbl2.computeTextWidth(valueTxt);
                    final float textX2 = (textW1 / 2f) - (textW2 / 2f);

                    lbl2.getPosition().inTL(textX2, textH1 + pad).setSize(textW2, lbl2.getPosition().getHeight());

                    add(tooltip).inTL(0, 0);

                    m_panel.getPosition().setSize(tooltip.getWidthSoFar(), tooltip.getHeightSoFar());
                }

                @Override
                public CustomPanelAPI getTpParent() {
                    return getParent();
                }
                
                @Override
                public TooltipMakerAPI createAndAttachTp() {
                    TooltipMakerAPI tooltip = getParent().createUIElement(460, 0, false);

                    tooltip.addPara(
                        "Total export market share for " + m_com.getName() + " for all colonies under " + m_selectedMarket.getFaction().getDisplayName() + " control.",
                        pad);

                    final float tpX = textX1 + textW1 + opad;
                    final float tpY = baseRow2Y;

                    getParent().addUIElement(tooltip).inTL(tpX, tpY);

                    return tooltip;
                }
            };


            final TextPanel textPanelRight = new TextPanel(section, 250, 0, new BasePanelPlugin<>()) {
                {
                    getPlugin().setTargetUIState(State.DIALOG);
                }

                @Override
                public void createPanel() {
                    final TooltipMakerAPI tooltip = m_panel.createUIElement(250, 0, false);

                    final String factionName = m_market.getFaction().getDisplayName();
                    final String txt = factionName + " market share";

                    final String valueTxt = (int) (engine.getFactionTotalExportMarketShare(
                        comID, m_market.getFaction().getId()
                    ) * 100) + "%";

                    tooltip.setParaFontColor(baseColor);
                    tooltip.setParaFont(Fonts.ORBITRON_12);
                    final LabelAPI lbl1 = tooltip.addPara(txt, pad);
                    lbl1.setHighlightOnMouseover(true);

                    tooltip.setParaFontColor(baseColor);
                    tooltip.setParaFont(Fonts.INSIGNIA_VERY_LARGE);
                    final LabelAPI lbl2 = tooltip.addPara(valueTxt, pad);
                    lbl2.setHighlightOnMouseover(true);

                    final float textH1 = lbl1.getPosition().getHeight();
                    textW1 = lbl1.computeTextWidth(txt);
                    textX1 = (SECT1_WIDTH*2 / 3f) - (textW1 / 2f);

                    lbl1.getPosition().inTL(0, 0).setSize(textW1, lbl1.getPosition().getHeight());

                    final float textW2 = lbl2.computeTextWidth(valueTxt);
                    final float textX2 = (textW1 / 2) - (textW2 / 2);

                    lbl2.getPosition().inTL(textX2, textH1 + pad).setSize(textW2, lbl2.getPosition().getHeight());

                    m_panel.addUIElement(tooltip).inTL(0, 0);

                    m_panel.getPosition().setSize(tooltip.getWidthSoFar(), tooltip.getHeightSoFar());
                }

                @Override
                public CustomPanelAPI getTpParent() {
                    return getParent();
                }

                @Override
                public TooltipMakerAPI createAndAttachTp() {
                    TooltipMakerAPI tooltip = getParent().createUIElement(460, 0, false);

                    String marketOwner = m_market.getFaction().isPlayerFaction() ?
                        "your" : m_market.getFaction().getPersonNamePrefix(); 

                    tooltip.addPara(
                            "Total export market share for " + m_com.getName() + " for all colonies under " + marketOwner + " control.",
                            pad);

                    final float tpX = textX1 + textW1 + opad;
                    final float tpY = baseRow2Y;

                    getParent().addUIElement(tooltip).inTL(tpX, tpY);

                    return tooltip;
                }
            };

            tooltip.addComponent(textPanelLeft.getPanel()).inTL(textPanelLeft.textX1, baseRow2Y);
            tooltip.addComponent(textPanelRight.getPanel()).inTL(textPanelRight.textX1, baseRow2Y);
        }
    }

    private void createSection2(CustomPanelAPI section, TooltipMakerAPI tooltip) {
        final int mapHeight = (int) section.getPosition().getHeight() - 2 * opad;

        final StarSystemAPI starSystem = m_market.getStarSystem();
        String title = m_market.getName();

        final MapParams params = new MapParams();
        params.showFilter = false;
        params.showTabs = false;
        params.withLayInCourse = false;
        params.skipCurrLocMarkerRendering = true;
        params.starSelectionRadiusMult = 0f;

        params.showSystem(starSystem);
        params.showMarket(m_market, 1);

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

    private void createSection3(CustomPanelAPI section, int mode) {
        final CallbackRunnable<Button> producerRunnable = (btn) -> {
            if (producerButton.checked) return;

            producerButton.checked = true;
            consumerButton.checked = false;
            updateSection3(0);
        };

        final CallbackRunnable<Button> consumerRunnable = (btn) -> {
            if (consumerButton.checked) return;
            
            consumerButton.checked = true;
            producerButton.checked = false;
            updateSection3(1);
        };

        // Table buttons
        final int btnWidth = 200;
        final int btnHeight = 18;

        producerButton = new Button(
            section, btnWidth, btnHeight,
            "Producers", Fonts.ORBITRON_12,
            producerRunnable
        );
        consumerButton = new Button(
            section, btnWidth, btnHeight,
            "Consumers", Fonts.ORBITRON_12,
            consumerRunnable
        );
        producerButton.setLabelColor(base);
        consumerButton.setLabelColor(base);
        producerButton.setCutStyle(CutStyle.TL_TR);
        consumerButton.setCutStyle(CutStyle.TL_TR);
        producerButton.setShortcut(Keyboard.KEY_1);
        consumerButton.setShortcut(Keyboard.KEY_2);
        producerButton.setAppendShortcutToText(true);
        consumerButton.setAppendShortcutToText(true);

        section.addComponent(producerButton.getPanel()).inTL(0, -btnHeight);
        section.addComponent(consumerButton.getPanel()).inTL(btnWidth, -btnHeight);
        
        if (mode == 0) {
            producerButton.checked = true;
            consumerButton.checked = false;
        } else if (mode == 1) {
            producerButton.checked = false;
            consumerButton.checked = true;
        }

        final SortableTable table = new SortableTable(
            section,
            SECT3_WIDTH,
            SECT3_HEIGHT,
            20,
            30
        );

        final String comID = m_com.getId();
        final String marketHeader = mode == 0 ? "Mkt Share" : "Mkt percent";
        final String creditHeader = mode == 0 ? "Income" : "Value";

        PendingTooltip<CustomPanelAPI> quantityTooltip = new PendingTooltip<>();
        createSection3QuantityHeaderTooltip(mode, table, quantityTooltip);

        final String marketTpDesc = mode == 0 ? "What percentage of the global market value the colony receives as income from its exports of the commodity.\n\nThe market share is affected by the number of units produced and the colony's accessibility." 
        :
        "The portion of the global market value that this colony contributes.\n\nIncludes player-controlled colonies.";
        
        final String creditTpDesc = mode == 0 ? "An estimate of how much daily income the colony is getting from exporting its production of the commodity.\n\nIncome also depends on colony stability, so may not directly correlate with market share." 
        :
        "How much the colony's demand contributes to the global market value for the commodity.";

        table.addHeaders(
            "", (int)(0.04 * SECT3_WIDTH), null, true, false, 1,
            "Colony", (int)(0.18 * SECT3_WIDTH), "Colony name.", true, true, 1,
            "Size", (int)(0.09 * SECT3_WIDTH), "Colony size.", false, false, -1,
            "Faction", (int)(0.17 * SECT3_WIDTH), "Faction that controls this colony.", false, false, -1,
            "Quantity", (int)(0.05 * SECT3_WIDTH), quantityTooltip, true, true, 2,
            "", (int)(0.1 * SECT3_WIDTH), null, true, false, 2,
            "Access", (int)(0.11 * SECT3_WIDTH), "A colony's accessibility. In-faction accessibility is higher.", false, false, -1,
            marketHeader, (int)(0.15 * SECT3_WIDTH), marketTpDesc, false, false, -1,
            creditHeader, (int)(0.11 * SECT3_WIDTH), creditTpDesc, false, false, -1
        );

        final EconomyEngine engine = EconomyEngine.getInstance();

        for (MarketAPI market : EconomyEngine.getMarketsCopy()) {

            if (market.isHidden()) continue;

            final String marketID = market.getId();
            final CommodityCell cell = engine.getComCell(comID, marketID);

            if (cell.globalExports < 1 && mode == 0 ||
                cell.getBaseDemand(true) + cell.getImportExclusiveDemand() < 1 && mode == 1
            ) continue;

            if (footerPanel != null && footerPanel.m_checkbox.isChecked() &&
                !(cell.getFlowCanNotExport() > 0 || cell.getFlowDeficit() > 0)) {
                continue;
            }

            final String iconPath = market.getFaction().getCrest();
            final Base iconPanel = new Base(
                section, iconSize, iconSize, iconPath, null,
                null, cell.getFlowDeficit() > 0
            );
            iconPanel.setOutlineColor(Color.RED);
            iconPanel.getPlugin().setOffsets(-1, -1, 2, 2);

            final String factionName = market.getFaction().getDisplayName();

            final float quantityValue = mode == 0 ? cell.globalExports : cell.globalImports;
            final String quantityTxt = NumFormat.engNotation((long) quantityValue);

            final CustomPanelAPI infoBar = UiUtils.CommodityInfoBar(iconSize, 75, cell);

            final int accessibility = (int) (market.getAccessibilityMod().computeEffective(0) * 100);

            final int marketShare = mode == 0 ? engine.getExportMarketShare(comID, marketID) :
                engine.getImportMarketShare(comID, marketID);

            final int incomeValue = (int) (quantityValue * m_com.getBasePrice());

            final Color textColor = market.getFaction().getBaseUIColor();

            table.addCell(iconPanel, cellAlg.LEFTPAD, null, null);
            table.addCell(market.getName(), cellAlg.LEFTPAD, null, textColor);
            table.addCell(market.getSize(), cellAlg.MID, null, textColor);
            table.addCell(factionName, cellAlg.MID, null, textColor);
            table.addCell(quantityTxt, cellAlg.MID, quantityValue, null);
            table.addCell(infoBar, cellAlg.MID, null, null);
            table.addCell(accessibility + "%", cellAlg.MID, accessibility, null);
            table.addCell(marketShare + "%", cellAlg.MID, marketShare, null);
            table.addCell(NumFormat.formatCredit(incomeValue), cellAlg.MID, incomeValue, null);

            // Tooltip
            PendingTooltip<CustomPanelAPI> tp = new PendingTooltip<>();
            createSection3RowsTooltip(
                table, market, market.getName(), textColor, tp
            );

            if (m_market == market) {
                table.getPendingRow().setOutline(Outline.TEX_THIN);
                table.getPendingRow().setOutlineColor(base);
            }

            final CallbackRunnable<RowManager> rowSelectedRunnable = (row) -> {
                m_selectedMarket = (MarketAPI) row.customData;
                updateSection1();
                updateSection2();
            };

            table.pushRow(
                CodexDataV2.getCommodityEntryId(comID), market,
                null, m_market.getFaction().getDarkUIColor(),
                tp, rowSelectedRunnable
            );

            if (m_market == market) {
                table.selectLastRow();
            }
        }

        section.addComponent(table.getPanel()).inTL(0,0);

        table.sortRows(6);

        table.createPanel();
    }

    private void createSection4(CustomPanelAPI section) {
        section4ComPanel = new LtvCommodityPanel(
            section,
            (int) section.getPosition().getWidth(),
            (int) section.getPosition().getHeight(),
            new BasePanelPlugin<>(),
            m_market.getName() + " - Commodities",
            true
        );
        section4ComPanel.setMarket(m_market);
        section4ComPanel.setRowSelectable(true);
        section4ComPanel.selectRow(m_com.getId());

        section4ComPanel.setActionListener(this);
        section4ComPanel.createPanel();

        section.addComponent(section4ComPanel.getPanel());
    }

    @Override
    public void onClicked(CustomPanel<?, ?, ?> source, boolean isLeftClick) {
        if (!isLeftClick) return;

        final CommodityRowPanel panel = ((CommodityRowPanel)source);
        m_com = panel.getCommodity();

        section4ComPanel.selectRow(panel);

        if (section4ComPanel.m_canViewPrices) {
            updateSection1();
            
            final int mode = producerButton.checked ? 0 : 1;

            updateSection3(mode);
        } 
    }

    private void createSection3RowsTooltip(SortableTable table, MarketAPI market,
        String marketName, Color baseColor, PendingTooltip<CustomPanelAPI> wrapper) {

        wrapper.parentSupplier = () -> {
            return table.getPanel();
        };

        wrapper.factory = () -> {
            final int tpWidth = 450;
            final FactionAPI faction = market.getFaction();
            final CommodityCell cell = EconomyEngine.getInstance().getComCell(
                m_com.getId(), market.getId()
            );
    
            final Color darkColor = faction.getDarkUIColor();
            
            final TooltipMakerAPI tp = wrapper.parentSupplier.get().createUIElement(
                tpWidth, 0, false
            );
    
            tp.setParaFont(Fonts.ORBITRON_12);
            tp.addPara(marketName + " - " + m_com.getName(), baseColor, pad);
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
    
            tp.addSectionHeading(m_com.getName() + " production & availability",
            baseColor, darkColor, Alignment.MID, opad);
                
            TooltipUtils.createCommodityProductionBreakdown(tp, cell);
    
            TooltipUtils.createCommodityDemandBreakdown(tp, cell);
    
            final float econUnit = m_com.getEconUnit();
            final int sellPrice = (int) (cell.computeVanillaPrice((int)econUnit, true, true) / econUnit);
            final int buyPrice = (int) (cell.computeVanillaPrice((int)econUnit, false, true) / econUnit);
    
            if (!m_com.isMeta()) {
                if (cell.getFlowCanNotExport() > 0) {
                    tp.addPara("Excess stockpiles: %s units.", opad, positive, 
                    highlight, NumFormat.engNotation((long) cell.getFlowCanNotExport()));
                } else if (cell.getFlowDeficit() > 0) {
                    tp.addPara("Local deficit: %s units.", opad, negative, 
                    highlight, NumFormat.engNotation((long) cell.getFlowDeficit()));
                }
    
                tp.addPara("Can be bought for %s and sold for %s per unit, assuming a batch of %s units traded.", opad, highlight, new String[]{
                    Misc.getDGSCredits(buyPrice), Misc.getDGSCredits(sellPrice), Misc.getWithDGS(econUnit)
                });
            }
    
            tp.addSectionHeading("Colony accessibility", baseColor, darkColor, Alignment.MID, opad);
    
            final int stability = (int) (market.getAccessibilityMod().computeEffective(0) * 100);
            Color valueColor = highlight;
            if (stability <= 0) {
                valueColor = negative;
            }
    
            tp.addPara("Accessibility: %s", opad, valueColor, stability + "%");
    
            tp.addStatModGrid(tpWidth, 50, opad, pad, market.getAccessibilityMod(),
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
                        return value.value >= 0 ? "+" + Math.round(value.value * 100) +
                            "%" : Math.round(value.value * 100) + "%";
                    }
                }
            );
    
            tp.addPara("   - " + "Market share of exports multiplied by %s",
                0, highlight, Strings.X + Misc.getRoundedValue(Math.max(stability, 0) / 100.0F)
            );
            tp.addPara(
                "The same-faction export bonus does not increase market share or income from exports.", opad
            );
    
            tp.addSpacer(opad + pad);
    
            return tp;
        };
    }

    private void createSection3QuantityHeaderTooltip(int mode, SortableTable table,
        PendingTooltip<CustomPanelAPI> wrapper) {
        final String quantityDesc = mode == 0
            ? "Units of this commodity exported globally."
            : "Units of this commodity imported globally to meet demand.";

        wrapper.parentSupplier = () -> {
            for (ColumnManager column : table.getColumns()) {
                if ("Quantity".equals(column.title)) {
                    return ((HeaderPanelWithTooltip) column.getHeaderPanel()).getParent();
                }
            } 

            return null;
        };

        wrapper.factory = () -> {
            final TooltipMakerAPI tp = wrapper.parentSupplier.get().createUIElement(
                SortableTable.headerTooltipWidth*2, 0, false
            );

            tp.addPara(quantityDesc, pad);

            final AtomicInteger y = new AtomicInteger((int) tp.getHeightSoFar() + pad + opad);

            CommodityRowPanel.legendRowCreator(
                1, tp, y, 26, m_market
            );

            return tp;
        };
    }

    public void dismiss(int option) {
        super.dismiss(option);

        UIState.setState(State.NONE);
    }
}
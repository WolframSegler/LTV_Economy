package wfg.ltv_econ.ui.dialogs;

import static wfg.native_ui.util.UIConstants.*;

import org.lwjgl.input.Keyboard;

import java.awt.Color;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.SettingsAPI;
import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.campaign.FactionSpecAPI;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.campaign.econ.CommoditySpecAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.combat.MutableStat.StatMod;
import com.fs.starfarer.api.impl.campaign.ids.Factions;
import com.fs.starfarer.api.impl.campaign.ids.Strings;
import com.fs.starfarer.api.ui.Alignment;
import com.fs.starfarer.api.ui.ButtonAPI.UICheckboxSize;
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
import wfg.ltv_econ.economy.engine.EconomyEngine;
import wfg.ltv_econ.economy.engine.EconomyInfo;
import wfg.ltv_econ.ui.panels.LtvCommodityPanel;
import wfg.ltv_econ.ui.panels.CommodityRowPanel;
import wfg.ltv_econ.ui.panels.reusable.ComIconPanel;
import wfg.ltv_econ.util.TooltipUtils;
import wfg.ltv_econ.util.UiUtils;
import wfg.native_ui.ui.Attachments;
import wfg.native_ui.ui.ComponentFactory;
import wfg.native_ui.ui.UIContext;
import wfg.native_ui.ui.UIContext.Context;
import wfg.native_ui.ui.components.InputSnapshotComp;
import wfg.native_ui.ui.components.NativeComponents;
import wfg.native_ui.ui.components.InteractionComp.ClickHandler;
import wfg.native_ui.ui.components.OutlineComp.OutlineType;
import wfg.native_ui.ui.components.TooltipComp.TooltipBuilder;
import wfg.native_ui.ui.core.UIElementFlags.HasInputSnapshot;
import wfg.native_ui.ui.dialogs.DialogPanel;
import wfg.native_ui.ui.panels.Button;
import wfg.native_ui.ui.panels.Button.CutStyle;
import wfg.native_ui.ui.panels.SortableTable;
import wfg.native_ui.ui.panels.SortableTable.RowPanel;
import wfg.native_ui.ui.panels.SortableTable.cellAlg;
import wfg.native_ui.ui.panels.SpritePanel.Base;
import wfg.native_ui.ui.panels.TextPanel;
import wfg.native_ui.util.CallbackRunnable;
import wfg.native_ui.util.NumFormat;
import wfg.native_ui.util.NativeUiUtils;
import wfg.native_ui.util.NativeUiUtils.AnchorType;

public class ComDetailDialog extends DialogPanel implements HasInputSnapshot {

    protected final InputSnapshotComp input = comp().get(NativeComponents.INPUT_SNAPSHOT);

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

    protected UIPanelAPI section1; // CommodityInfo
    protected UIPanelAPI section2; // Sector Map
    protected UIPanelAPI section3; // Prod&Consump Tables
    protected UIPanelAPI section4; // Commodity Panel

    public final static int iconSize = 24;

    private final MarketAPI m_market;
    private final FactionSpecAPI m_faction;

    private CommoditySpecAPI m_com;
    public MarketAPI m_selectedMarket = null;

    public TextPanel footerPanel = null;
    public Button producerButton = null;
    public Button consumerButton = null;
    public LtvCommodityPanel section4ComPanel = null;

    /** market can be null */
    public ComDetailDialog(MarketAPI market, FactionSpecAPI faction, CommoditySpecAPI com) {
        this(market, faction, com, 1166, 658 + 20);
    }

    /** market can be null */
    public ComDetailDialog(MarketAPI market, FactionSpecAPI faction, CommoditySpecAPI com,
        int panelW, int panelH
    ) {
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
        m_faction = faction;
        m_com = com;

        setConfirmShortcut();

        holo.setBackgroundAlpha(1, 1);
        backgroundDimAmount = 0f;

        createPanel();
    }

    @Override
    public void createPanel() {
        UIContext.setContext(Context.DIALOG);

        createSections();

        // Footer
        final int footerH = 40;

        footerPanel = new TextPanel(innerPanel, 400, footerH) {
            @Override
            public void createPanel() {
                final TooltipMakerAPI footer = ComponentFactory.createTooltip(PANEL_W, false);
                footer.setActionListenerDelegate(
                    new ActionListenerDelegate() {
                        public void actionPerformed(Object data, Object source) {
                            updateSection3(producerButton.isChecked() ? 0 : 1);
                        }
                    }
                );
                m_checkbox = footer.addCheckbox(20, 20, "", "stockpile_toggle",
                    Fonts.ORBITRON_12, highlight, UICheckboxSize.SMALL, 0);

                m_checkbox.getPosition().inBL(0, 0);
                m_checkbox.setShortcut(Keyboard.KEY_Q, false);

                footer.setParaFont(Fonts.ORBITRON_12);
                footer.setParaFontColor(m_faction.getBaseUIColor());
                LabelAPI txt = footer.addPara("Only show colonies with excess stockpiles or shortages (%s)", 0f, highlight, "Q");
                txt.setHighlightOnMouseover(true);

                int TextY = (int) txt.computeTextHeight(txt.getText());
                txt.getPosition().inBL(20 + pad, (20 - TextY) / 2);

                ComponentFactory.addTooltip(footer, footerH, false, m_panel);
            }

            @Override
            public void advance(float delta) {
                super.advance(delta);

                if (input.hoveredLastFrame && input.LMBUpLastFrame) {
                    if (m_checkbox != null) {
                        m_checkbox.setChecked(!m_checkbox.isChecked());
                    }
                }
            }

            {
                context.target = Context.DIALOG;

                tooltip.width = getPos().getWidth() * 0.7f;
                tooltip.parent = ComDetailDialog.this.m_panel;
                tooltip.builder = (tp, exp) -> {
                    tp.addPara(
                        "Only show colonies that are either suffering from a shortage or have excess stockpiles.\n\nColonies with excess stockpiles have more of the goods available on the open market and have lower prices.\n\nColonies with shortages have less or none available for sale, and have higher prices.",
                        pad
                    );
                };
                tooltip.positioner = (tp, exp) -> {
                    NativeUiUtils.anchorPanel(tp, m_panel, AnchorType.TopLeft, pad);
                };
            }
        };

        innerPanel.addComponent(footerPanel.getPanel()).inBL(opad, pad);
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

        final TooltipMakerAPI tp = ComponentFactory.createTooltip(SECT1_WIDTH, false);
        ComponentFactory.addTooltip(tp, SECT1_HEIGHT, false, section1).inTL(0, 0);

        createSection1(section1, tp);
        innerPanel.addComponent(section1).inTL(pad, pad);
    }

    public void updateSection2() {
        innerPanel.removeComponent(section2);

        section2 = Global.getSettings().createCustom(SECT2_WIDTH, SECT2_HEIGHT, null);

        final TooltipMakerAPI tp = ComponentFactory.createTooltip(SECT2_WIDTH, false);
        ComponentFactory.addTooltip(tp, SECT2_HEIGHT, false, section2).inTL(0, 0);

        createSection2(section2, tp);
        innerPanel.addComponent(section2).inTR(pad, pad);
    }

    /**
     *  MODE_0: Displays the Producers <br></br>
     *  MODE_1: Displays the Consumers
     */
    public void updateSection3(int mode) {
        innerPanel.removeComponent(section3);

        section3 = Global.getSettings().createCustom(SECT3_WIDTH, SECT3_HEIGHT, null);

        createSection3(section3, mode);
        innerPanel.addComponent(section3).inBL(pad, BUTTON_H + pad*2 + opad);
    }

    public void updateSection4() {
        innerPanel.removeComponent(section4);

        section4 = Global.getSettings().createCustom(SECT4_WIDTH, SECT4_HEIGHT, null);

        final TooltipMakerAPI tp = ComponentFactory.createTooltip(SECT4_WIDTH, false);
        ComponentFactory.addTooltip(tp, SECT4_HEIGHT, false, section4).inTL(0, 0);

        createSection4(section4);
        innerPanel.addComponent(section4).inBR(pad, BUTTON_H + pad*2 + opad);
    }

    private void createSection1(UIPanelAPI section, TooltipMakerAPI tooltip) {
        if (m_com == null) return;
        final EconomyEngine engine = EconomyEngine.getInstance();
        final SettingsAPI settings = Global.getSettings();

        tooltip.addSectionHeading(m_com.getName(), Alignment.MID, pad);
        final int headerHeight = (int) tooltip.getPrev().getPosition().getHeight();

        // Icons
        final int iconSize = (int) (section.getPosition().getHeight() / 2.2f);

        final ComIconPanel iconLeft = new ComIconPanel(section, iconSize, iconSize,
            null, null, m_com, m_faction
        );

        iconLeft.getPos().inTL(opad * 3,
            (SECT1_HEIGHT - iconSize) / 2 + headerHeight);
        section.addComponent(iconLeft.getPanel());

        final ComIconPanel iconRight = new ComIconPanel(section, iconSize, iconSize,
            null, null, m_com, m_faction
        );

        iconRight.getPos().inTL(SECT1_WIDTH - iconSize - opad * 3,
            (SECT1_HEIGHT - iconSize) / 2 + headerHeight);
        section.addComponent(iconRight.getPanel());

        // Text
        final String comID = m_com.getId();
        final int baseY = (int) (headerHeight + opad * 1.5f);
        final Color baseColor = m_faction.getBaseUIColor();
        { // Global market value
            final TextPanel textPanel = new TextPanel(section, 170, 0) {
                @Override
                public void createPanel() {
                    final long value = engine.getComDomain(comID)
                        .getMarketActivity();
                    final String txt = "Global market value";
                    String valueTxt = NumFormat.formatCredit(value);
                    if (value < 1) valueTxt = "---";

                    ComponentFactory.addCaptionValueBlock(
                        m_panel,
                        txt,
                        valueTxt,
                        baseColor
                    );
                }

                {
                    context.target = Context.DIALOG;

                    tooltip.width = 460f;
                    tooltip.parent = ComDetailDialog.this.m_panel;
                    tooltip.builder = (tp, exp) -> {
                        final int discount = (int)((1f - EconomyConfig.FACTION_EXCHANGE_MULT)*100);

                        tp.addPara(
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
                    };
                    tooltip.positioner = (tp, exp) -> {
                        NativeUiUtils.anchorPanel(
                            tp,
                            m_panel,
                            AnchorType.RightTop,
                            opad
                        );
                    };
                }
            };

            tooltip.addComponent(textPanel.getPanel()).inTL(
                (SECT1_WIDTH / 3f) - textPanel.getPos().getWidth(), baseY
            );
        }
        { // Total global exports
            final TextPanel textPanel = new TextPanel(section, 170, 0) {
                @Override
                public void createPanel() {
                    final String txt = "Total global exports";

                    final String valueTxt = NumFormat.engNotation(
                        engine.info.getTotalGlobalExports(comID)
                    );

                    ComponentFactory.addCaptionValueBlock(
                        m_panel,
                        txt,
                        valueTxt,
                        baseColor
                    );
                }

                {
                    context.target = Context.DIALOG;

                    tooltip.width = 460f;
                    tooltip.parent = ComDetailDialog.this.m_panel;
                    tooltip.builder = (tp, exp) -> {
                        tp.addPara(
                            "The total number of " + m_com.getName() + " being exported globally by all producing markets in the sector.\n\n" +
                            "This figure reflects the total global supply that reaches exportable surplus after local and in-faction demand is met.", pad
                        );
                    };
                    tooltip.positioner = (tp, exp) -> {
                        NativeUiUtils.anchorPanel(tp, m_panel, AnchorType.RightTop, opad);
                    };
                }
            };

            tooltip.addComponent(textPanel.getPanel()).inTL(
                (SECT1_WIDTH - textPanel.getPos().getWidth()) / 2f, baseY
            );
        }
        { // Total faction exports

            final FactionSpecAPI currFaction;

            if (m_selectedMarket != null) currFaction = m_selectedMarket.getFaction().getFactionSpec();
            else currFaction = m_faction;

            final TextPanel textPanel = new TextPanel(section, 210, 0) {
                @Override
                public void createPanel() {
                    final String factionName = currFaction.getDisplayName();
                    final Color factionColor = currFaction.getBaseUIColor();

                    final String txt = "Total " + factionName + " exports";

                    final String globalValue = NumFormat.engNotation(
                        engine.info.getFactionTotalGlobalExports(
                            comID, currFaction)
                    );
                    final String inFactionValue = NumFormat.engNotation(
                        engine.info.getTotalInFactionExports(
                            comID, currFaction)
                    );

                    final String valueTxt = globalValue + "  |  " + inFactionValue;

                    final LabelAPI lbl1 = settings.createLabel(txt, Fonts.ORBITRON_12);
                    lbl1.setColor(factionColor);
                    lbl1.setHighlightOnMouseover(true);

                    final LabelAPI lbl2 = settings.createLabel(valueTxt, Fonts.INSIGNIA_VERY_LARGE);
                    lbl2.setColor(base);
                    lbl2.setHighlight(globalValue, inFactionValue);
                    lbl2.setHighlightColors(factionColor, UiUtils.inFactionColor);
                    lbl2.setHighlightOnMouseover(true);

                    ComponentFactory.layoutCaptionValueLabels(
                        m_panel, lbl1, lbl2
                    );
                }

                {
                    context.target = Context.DIALOG;

                    tooltip.width = 460f;
                    tooltip.parent = ComDetailDialog.this.m_panel;
                    tooltip.builder = (tp, exp) -> {
                        tp.addPara(
                            "The total number of units exported to all consumers globally, as well as the total exported within the faction under " + currFaction.getPersonNamePrefix() + " control.\n\n" +
                            "Global exports are shaped by the colony's accessibility, its faction relations and other factors.",
                            pad, new Color[] {currFaction.getBaseUIColor(), UiUtils.inFactionColor},
                            new String[] {"all consumers globally", "within the faction"}
                        );
                    };
                    tooltip.positioner = (tp, exp) -> {
                        NativeUiUtils.anchorPanel(tp, m_panel, AnchorType.LeftTop, opad);
                    };
                }
            };

            tooltip.addComponent(textPanel.getPanel()).inTL(SECT1_WIDTH*2 / 3f, baseY);
        }

        final int baseRow2Y = baseY * 3 + pad;

        if (m_selectedMarket == null || m_selectedMarket.isPlayerOwned()) { // Faction market share
            final TextPanel textPanel = new TextPanel(section, 250, 0) {
                @Override
                public void createPanel() {
                    final String factionName = m_faction.getDisplayName();
                    final String txt = factionName + " market share";

                    final String valueTxt = (int)(engine.info.getFactionTotalExportMarketShare(
                        comID, m_faction.getId()
                    ) * 100) + "%";

                    ComponentFactory.addCaptionValueBlock(
                        m_panel, txt, valueTxt,
                        baseColor, baseColor
                    );
                }

                {
                    context.target = Context.DIALOG;

                    tooltip.width = 460f;
                    tooltip.parent = ComDetailDialog.this.m_panel;
                    tooltip.builder = (tp, exp) -> {
                        final String marketOwner = m_faction.getId().equals(Factions.PLAYER) ?
                            "your" : m_faction.getPersonNamePrefix(); 

                        tp.addPara(
                            "Total export market share for " + m_com.getName() + " for all colonies under " + marketOwner + " control.",
                            pad
                        );
                    };
                    tooltip.positioner = (tp, exp) -> {
                        NativeUiUtils.anchorPanel(tp, m_panel, AnchorType.RightTop, opad);
                    };
                }
            };

            tooltip.addComponent(textPanel.getPanel()).inTL(
                (SECT1_WIDTH - textPanel.getPos().getWidth()) / 2f, baseRow2Y
            );
        }

        else { // Faction market share
            final TextPanel textPanelLeft = new TextPanel(section, 250, 0) {
                @Override
                public void createPanel() {
                    final String factionName = m_selectedMarket.getFaction().getDisplayName();
                    final String txt = factionName + " market share";

                    final String valueTxt = (int) (engine.info.getFactionTotalExportMarketShare(
                        comID, m_selectedMarket.getFactionId()
                    ) * 100) + "%";

                    ComponentFactory.addCaptionValueBlock(
                        m_panel, txt, valueTxt,
                        m_selectedMarket.getFaction().getBaseUIColor(),
                        m_selectedMarket.getFaction().getBaseUIColor()
                    );
                }

                {
                    context.target = Context.DIALOG;

                    tooltip.width = 460f;
                    tooltip.parent = ComDetailDialog.this.m_panel;
                    tooltip.builder = (tp, exp) -> {
                        tp.addPara(
                            "Total export market share for " + m_com.getName() + " for all colonies under " + m_selectedMarket.getFaction().getDisplayName() + " control.",
                            pad
                        );
                    };
                    tooltip.positioner = (tp, exp) -> {
                        NativeUiUtils.anchorPanel(tp, m_panel, AnchorType.RightTop, opad);
                    };
                }
            };


            final TextPanel textPanelRight = new TextPanel(section, 250, 0) {
                @Override
                public void createPanel() {
                    final String factionName = m_faction.getDisplayName();
                    final String txt = factionName + " market share";

                    final String valueTxt = (int) (engine.info.getFactionTotalExportMarketShare(
                        comID, m_faction.getId()
                    ) * 100) + "%";

                    ComponentFactory.addCaptionValueBlock(
                        m_panel, txt, valueTxt,
                        baseColor, baseColor
                    );
                }

                {
                    context.target = Context.DIALOG;

                    tooltip.width = 460f;
                    tooltip.parent = ComDetailDialog.this.m_panel;
                    tooltip.builder = (tp, exp) -> {
                        String marketOwner = m_faction.getId().equals(Factions.PLAYER) ?
                            "your" : m_faction.getPersonNamePrefix(); 

                        tp.addPara(
                            "Total export market share for " + m_com.getName() + " for all colonies under " + marketOwner + " control.",
                            pad
                        );
                    };
                    tooltip.positioner = (tp, exp) -> {
                        NativeUiUtils.anchorPanel(tp, m_panel, AnchorType.RightTop, opad);
                    };
                }
            };

            tooltip.addComponent(textPanelLeft.getPanel()).inTL(
                (SECT1_WIDTH / 3f) - (textPanelLeft.getPos().getWidth() / 2f), baseRow2Y
            );
            tooltip.addComponent(textPanelRight.getPanel()).inTL(
                (SECT1_WIDTH*2 / 3f) - (textPanelRight.getPos().getWidth() / 2f), baseRow2Y
            );
        }
    }

    private void createSection2(UIPanelAPI section, TooltipMakerAPI tooltip) {
        if (m_market == null) return;

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

    private void createSection3(UIPanelAPI section, int mode) {
        final CallbackRunnable<Button> producerRunnable = (btn) -> {
            if (producerButton.isChecked()) return;

            producerButton.setChecked(true);
            consumerButton.setChecked(false);
            updateSection3(0);
        };

        final CallbackRunnable<Button> consumerRunnable = (btn) -> {
            if (consumerButton.isChecked()) return;
            
            producerButton.setChecked(false);
            consumerButton.setChecked(true);
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
        producerButton.cutStyle = CutStyle.TL_TR;
        consumerButton.cutStyle = CutStyle.TL_TR;
        producerButton.setShortcut(Keyboard.KEY_1);
        consumerButton.setShortcut(Keyboard.KEY_2);
        producerButton.setAppendShortcutToText(true);
        consumerButton.setAppendShortcutToText(true);

        section.addComponent(producerButton.getPanel()).inTL(0, -btnHeight);
        section.addComponent(consumerButton.getPanel()).inTL(btnWidth, -btnHeight);
        
        if (mode == 0) {
            producerButton.setChecked(true);
            consumerButton.setChecked(false);
        } else if (mode == 1) {
            producerButton.setChecked(false);
            consumerButton.setChecked(true);
        }

        final SortableTable table = new SortableTable(
            section, SECT3_WIDTH, SECT3_HEIGHT, 20, 30
        );

        final String comID = m_com.getId();
        final String marketHeader = mode == 0 ? "Mkt Share" : "Mkt percent";
        final String creditHeader = mode == 0 ? "Income" : "Value";

        final TooltipBuilder quantityTooltip = createSection3QuantityHeaderTooltip(mode, table);

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

        for (MarketAPI market : EconomyInfo.getMarketsCopy()) {

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
            final Base iconPanel = new Base(section, iconSize, iconSize,
                iconPath, null, null
            );
            iconPanel.outline.enabled = cell.getFlowDeficit() > 0;
            iconPanel.outline.color = Color.RED;
            iconPanel.offset.setOffset(-1, -1, 2, 2);

            final String factionName = market.getFaction().getDisplayName();

            final float quantityValue = mode == 0 ? cell.globalExports : cell.globalImports;
            final String quantityTxt = NumFormat.engNotation((long) quantityValue);

            final UIPanelAPI infoBar = UiUtils.CommodityInfoBar(iconSize, 75, cell);

            final int accessibility = (int) (market.getAccessibilityMod().computeEffective(0) * 100);

            final int marketShare = mode == 0 ? engine.info.getExportMarketShare(comID, marketID) :
                engine.info.getImportMarketShare(comID, marketID);

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
            table.getPendingRow().tooltip.width = 450f;
            final TooltipBuilder tp = createSection3RowsTooltip(market, market.getName(), textColor);

            if (m_market == market) {
                table.getPendingRow().outline.type = OutlineType.TEX_THIN;
                table.getPendingRow().outline.color = base;
            }

            final ClickHandler<RowPanel> rowSelectedRunnable = (row, isLeftClick) -> {
                m_selectedMarket = (MarketAPI) row.customData;
                updateSection1();
                updateSection2();
            };

            table.pushRow(
                market, tp, rowSelectedRunnable, null,
                null, m_faction.getDarkUIColor()
            );

            if (m_market == market) table.selectLastRow();
        }

        section.addComponent(table.getPanel()).inTL(0,0);

        table.sortRows(6);
    }

    private void createSection4(UIPanelAPI section) {
        if (m_market == null) return;

        section4ComPanel = new LtvCommodityPanel(
            section,
            (int) section.getPosition().getWidth(),
            (int) section.getPosition().getHeight(),
            m_market.getName() + " - Commodities",
            true, m_market
        );
        section4ComPanel.selectRow(m_com.getId());

        section4ComPanel.selectionListener = (source, isLeftClick) -> {
            if (!isLeftClick) return;

            m_com = source.m_com;

            section4ComPanel.selectRow(source);

            if (UiUtils.canViewPrices()) {
                updateSection1();
                
                final int mode = producerButton.isChecked() ? 0 : 1;

                updateSection3(mode);
            } 
        };
        section4ComPanel.createPanel();

        section.addComponent(section4ComPanel.getPanel());
    }

    private TooltipBuilder createSection3RowsTooltip(MarketAPI market, String marketName, Color baseColor) {
        return (tp, exp) -> {
            final FactionAPI faction = market.getFaction();
            final CommodityCell cell = EconomyEngine.getInstance().getComCell(
                m_com.getId(), market.getId()
            );
    
            final Color darkColor = faction.getDarkUIColor();
            
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
    
            tp.addStatModGrid(450f, 50, opad, pad, market.getAccessibilityMod(),
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
        };
    }

    private TooltipBuilder createSection3QuantityHeaderTooltip(
        int mode, SortableTable table
    ) {
        final String quantityDesc = mode == 0
            ? "Units of this commodity exported globally."
            : "Units of this commodity imported globally to meet demand.";

        return (tp, exp) -> {
            tp.addPara(quantityDesc, pad);

            final int y = (int) tp.getHeightSoFar() + pad + opad;

            CommodityRowPanel.legendRowCreator(
                1, tp, y, 26
            );
        };
    }

    public void dismiss(int option) {
        super.dismiss(option);

        UIContext.setContext(Context.NONE);
    }
}
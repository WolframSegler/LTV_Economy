package wfg.ltv_econ.ui.marketInfo.dialogs;

import static wfg.native_ui.util.UIConstants.*;
import static wfg.ltv_econ.constant.strings.Income.TRADE_EXPORT_KEY;
import static wfg.ltv_econ.constant.strings.Income.TRADE_IMPORT_KEY;
import static wfg.ltv_econ.constant.strings.LocalizedStrings.*;
import static wfg.native_ui.util.Globals.settings;

import org.lwjgl.input.Keyboard;

import java.awt.Color;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.campaign.FactionSpecAPI;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.campaign.econ.CommoditySpecAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.combat.MutableStat.StatMod;
import com.fs.starfarer.api.ui.Alignment;
import com.fs.starfarer.api.ui.ButtonAPI.UICheckboxSize;
import com.fs.starfarer.api.ui.Fonts;
import com.fs.starfarer.api.ui.LabelAPI;
import com.fs.starfarer.api.ui.MapParams;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI.StatModValueGetter;
import com.fs.starfarer.api.ui.UIPanelAPI;
import com.fs.starfarer.api.util.Misc;

import wfg.ltv_econ.config.EconConfig;
import wfg.ltv_econ.constant.UIColors;
import wfg.ltv_econ.economy.commodity.CommodityCell;
import wfg.ltv_econ.economy.engine.EconomyEngine;
import wfg.ltv_econ.economy.engine.EconomyInfo;
import wfg.ltv_econ.economy.registry.MarketFinanceRegistry;
import wfg.ltv_econ.ui.marketInfo.CommodityRowPanel;
import wfg.ltv_econ.ui.marketInfo.LtvCommodityPanel;
import wfg.ltv_econ.ui.reusable.ComIconPanel;
import wfg.ltv_econ.ui.reusable.CommodityBarPanel;
import wfg.ltv_econ.util.TooltipUtils;
import wfg.ltv_econ.util.UIUtils;
import wfg.native_ui.ui.ComponentFactory;
import wfg.native_ui.ui.component.InputSnapshotComp;
import wfg.native_ui.ui.component.NativeComponents;
import wfg.native_ui.ui.component.InteractionComp.ClickHandler;
import wfg.native_ui.ui.component.OutlineComp.OutlineType;
import wfg.native_ui.ui.component.TooltipComp.TooltipBuilder;
import wfg.native_ui.ui.core.UIElementFlags.HasInputSnapshot;
import wfg.native_ui.ui.dialog.DialogPanel;
import wfg.native_ui.ui.functional.Button;
import wfg.native_ui.ui.functional.CheckboxButton;
import wfg.native_ui.ui.functional.Button.CutStyle;
import wfg.native_ui.ui.table.SortableTable;
import wfg.native_ui.ui.table.SortableTable.TableRow;
import wfg.native_ui.ui.table.SortableTable.cellAlg;
import wfg.native_ui.ui.visual.SpritePanel.Base;
import wfg.native_ui.ui.visual.TextPanel;
import wfg.native_ui.util.CallbackRunnable;
import wfg.native_ui.util.NumFormat;
import wfg.native_ui.util.NativeUiUtils;
import wfg.native_ui.util.NativeUiUtils.AnchorType;

public class ComDetailDialog extends DialogPanel implements HasInputSnapshot {
    private static final int PANEL_W = 1200;
    private static final int PANEL_H = 678;

    private static final int SECT1_WIDTH = (int) (PANEL_W * 0.75f - opad);
    private static final int SECT2_WIDTH = (int) (PANEL_W * 0.25f - opad);
    private static final int SECT3_WIDTH = (int) (PANEL_W * 0.75f - opad);
    private static final int SECT4_WIDTH = (int) (PANEL_W * 0.25f - opad);

    private static final int SECT1_HEIGHT = (int) (PANEL_H * 0.28f - opad);
    private static final int SECT2_HEIGHT = (int) (PANEL_H * 0.28f - opad);
    private static final int SECT3_HEIGHT = (int) (PANEL_H * 0.72f - opad); // 876
    private static final int SECT4_HEIGHT = (int) (PANEL_H * 0.72f - opad);

    protected final InputSnapshotComp input = comp().get(NativeComponents.INPUT_SNAPSHOT);

    protected UIPanelAPI section1; // CommodityInfo
    protected UIPanelAPI section2; // Sector Map
    protected UIPanelAPI section3; // Prod&Consumption Tables
    protected UIPanelAPI section4; // Commodity Panel

    public final static int iconSize = 24;

    private final MarketAPI m_market;
    private final FactionSpecAPI m_faction;

    private CommoditySpecAPI m_com;
    public MarketAPI selectedMarket = null;

    public CheckboxButton footer = null;
    public Button producerButton = null;
    public Button consumerButton = null;

    /**
     * @param market nullable
     */
    public ComDetailDialog(MarketAPI market, FactionSpecAPI faction, CommoditySpecAPI com) {
        super(PANEL_W, PANEL_H, null, null, str("uiDismiss"));

        m_market = market;
        m_faction = faction;
        m_com = com;

        setConfirmShortcut();

        backgroundDimAmount = 0f;

        buildUI();
    }

    @Override
    public void buildUI() {
        createSections();
        
        footer = new CheckboxButton(m_panel, 20, str("uiShowExcessDeficitTxt"),
            Fonts.ORBITRON_12, (btn) -> {
                btn.setChecked(!btn.isChecked());
                updateSection3(producerButton.isChecked() ? 0 : 1);
            },
            UICheckboxSize.SMALL, false
        );
        footer.setShortcutAndAppendToText(Keyboard.KEY_Q);
        footer.tooltip.width = getPos().getWidth() * 0.7f;
        footer.tooltip.builder = (tp, exp) -> {
            tp.addPara(str("uiTpTxtShowExcessDeficit"), pad);
        };
        footer.tooltip.positioner = (tp, exp) -> {
            NativeUiUtils.anchorPanel(tp, footer.getPanel(), AnchorType.TopLeft, pad);
        };

        add(footer).inBL(pad, pad);
    }

    public void createSections() {
        EconomyEngine.instance().fakeAdvance();

        updateSection1();
        updateSection2();
        updateSection3(0);
        updateSection4();
    }

    public void updateSection1() {
        remove(section1);

        section1 = settings.createCustom(SECT1_WIDTH, SECT1_HEIGHT, null);

        final TooltipMakerAPI tp = ComponentFactory.createTooltip(SECT1_WIDTH, false);
        ComponentFactory.addTooltip(tp, SECT1_HEIGHT, false, section1).inTL(0, 0);

        createSection1(section1, tp);
        add(section1).inTL(pad, pad);
    }

    public void updateSection2() {
        remove(section2);

        section2 = settings.createCustom(SECT2_WIDTH, SECT2_HEIGHT, null);

        final TooltipMakerAPI tp = ComponentFactory.createTooltip(SECT2_WIDTH, false);
        ComponentFactory.addTooltip(tp, SECT2_HEIGHT, false, section2).inTL(0, 0);

        createSection2(section2, tp);
        add(section2).inTR(pad, pad);
    }

    /**
     *  MODE_0: Displays the Producers <br></br>
     *  MODE_1: Displays the Consumers
     */
    public void updateSection3(int mode) {
        remove(section3);

        section3 = settings.createCustom(SECT3_WIDTH, SECT3_HEIGHT, null);

        createSection3(section3, mode);
        add(section3).inBL(pad, BUTTON_H + pad*2 + opad);
    }

    public void updateSection4() {
        remove(section4);

        section4 = settings.createCustom(SECT4_WIDTH, SECT4_HEIGHT, null);

        final TooltipMakerAPI tp = ComponentFactory.createTooltip(SECT4_WIDTH, false);
        ComponentFactory.addTooltip(tp, SECT4_HEIGHT, false, section4).inTL(0, 0);

        createSection4(section4);
        add(section4).inBR(pad, BUTTON_H + pad*2 + opad);
    }

    private void createSection1(UIPanelAPI section, TooltipMakerAPI tooltip) {
        if (m_com == null) return;
        final EconomyEngine engine = EconomyEngine.instance();

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
        final int baseY = (int) (headerHeight + hpad*3);
        final Color baseColor = m_faction.getBaseUIColor();
        { // Global market value
            final TextPanel textPanel = new TextPanel(section, 170, 0) {
                @Override
                public void buildUI() {
                    final long value = engine.getComDomain(comID).getCreditActivityHistory();
                    final String txt = str("uiTitleGlobalMarketValue");
                    final String valueTxt = value < 1l ? "---" : NumFormat.formatCredit(value);

                    ComponentFactory.addCaptionValueBlock(
                        m_panel, txt, valueTxt, baseColor
                    );
                }

                {
                    tooltip.width = 460f;
                    tooltip.builder = (tp, exp) -> {
                        final int discount = (int)((1f - EconConfig.FACTION_EXCHANGE_MULT)*100);

                        tp.addPara(strf("uiTpTxtGlobalMarketValue", m_com.getName()),
                            pad, new Color[] {base, highlight},
                            Integer.toString(EconConfig.HISTORY_LENGTH), discount + "%"
                        );
                    };
                    tooltip.positioner = (tp, exp) -> {
                        NativeUiUtils.anchorPanel(tp, m_panel, AnchorType.RightTop, opad);
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
                public void buildUI() {
                    final String valueTxt = NumFormat.engNotate(
                        engine.info.getGlobalExports(comID)
                    );

                    ComponentFactory.addCaptionValueBlock(
                        m_panel, str("uiTitleGlobalExports"),
                        valueTxt, baseColor
                    );
                }

                {
                    tooltip.width = 460f;
                    tooltip.builder = (tp, exp) -> {
                        tp.addPara(strf("uiTpTxtGlobalExports",  m_com.getName()), pad);
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

            final FactionAPI currFaction;

            if (selectedMarket != null) currFaction = selectedMarket.getFaction();
            else currFaction = Global.getSector().getFaction(m_faction.getId());

            final TextPanel textPanel = new TextPanel(section, 210, 0) {
                @Override
                public void buildUI() {
                    final String factionName = currFaction.getDisplayName();
                    final Color factionColor = currFaction.getBaseUIColor();

                    final String txt = strf("uiTitleTotalFactionExports", factionName);

                    final String globalValue = NumFormat.engNotate(
                        engine.info.getFactionGlobalExports(comID, currFaction.getId())
                    );
                    final String inFactionValue = NumFormat.engNotate(
                        engine.info.getFactionInFactionExports(comID, currFaction.getId())
                    );

                    final String valueTxt = globalValue + "  |  " + inFactionValue;

                    final LabelAPI lbl1 = settings.createLabel(txt, Fonts.ORBITRON_12);
                    lbl1.setColor(factionColor);
                    lbl1.setHighlightOnMouseover(true);

                    final LabelAPI lbl2 = settings.createLabel(valueTxt, Fonts.INSIGNIA_VERY_LARGE);
                    lbl2.setColor(base);
                    lbl2.setHighlight(globalValue, inFactionValue);
                    lbl2.setHighlightColors(factionColor, UIColors.IN_FACTION);
                    lbl2.setHighlightOnMouseover(true);

                    ComponentFactory.layoutCaptionValueLabels(
                        m_panel, lbl1, lbl2
                    );
                }

                {
                    tooltip.width = 460f;
                    tooltip.builder = (tp, exp) -> {
                        tp.addPara(strf("uiTpTxtTotalFactionExports", currFaction.getPersonNamePrefix()),
                            pad, new Color[] {currFaction.getBaseUIColor(), UIColors.IN_FACTION},
                            str("uiTotalFactionExportsTpGlobalConsumersTxt"), str("uiTotalFactionExportsTpInFactionTxt")
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

        if (selectedMarket == null || selectedMarket.isPlayerOwned()) { // Faction market share
            final TextPanel textPanel = new TextPanel(section, 250, 0) {
                @Override
                public void buildUI() {
                    final String factionName = m_faction.getDisplayName();
                    final String txt = strf("uiTitleFactionMarketShare", factionName);

                    final String valueTxt = (int)(engine.info.getFactionExportShare(
                        comID, m_faction.getId()
                    ) * 100) + "%";

                    ComponentFactory.addCaptionValueBlock(
                        m_panel, txt, valueTxt,
                        baseColor, baseColor
                    );
                }

                {
                    tooltip.width = 460f;
                    tooltip.builder = (tp, exp) -> {
                        tp.addPara(strf("uiTpTxtFactionMarketShare", m_com.getName(), m_faction.getPersonNamePrefix()), pad);
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
                public void buildUI() {
                    final String factionName = selectedMarket.getFaction().getDisplayName();
                    final String txt = strf("uiTitleFactionMarketShare", factionName);

                    final String valueTxt = (int) (engine.info.getFactionExportShare(
                        comID, selectedMarket.getFactionId()
                    ) * 100) + "%";

                    ComponentFactory.addCaptionValueBlock(
                        m_panel, txt, valueTxt,
                        selectedMarket.getFaction().getBaseUIColor(),
                        selectedMarket.getFaction().getBaseUIColor()
                    );
                }

                {
                    tooltip.width = 460f;
                    tooltip.builder = (tp, exp) -> {
                        tp.addPara(strf("uiTpTxtFactionMarketShare", m_com.getName(), selectedMarket.getFaction().getDisplayName()), pad);
                    };
                    tooltip.positioner = (tp, exp) -> {
                        NativeUiUtils.anchorPanel(tp, m_panel, AnchorType.RightTop, opad);
                    };
                }
            };


            final TextPanel textPanelRight = new TextPanel(section, 250, 0) {
                @Override
                public void buildUI() {
                    final String factionName = m_faction.getDisplayName();
                    final String txt = strf("uiTitleFactionMarketShare", factionName);

                    final String valueTxt = (int) (engine.info.getFactionExportShare(
                        comID, m_faction.getId()
                    ) * 100) + "%";

                    ComponentFactory.addCaptionValueBlock(
                        m_panel, txt, valueTxt,
                        baseColor, baseColor
                    );
                }

                {
                    tooltip.width = 460f;
                    tooltip.builder = (tp, exp) -> {
                        tp.addPara(strf("uiTpTxtFactionMarketShare", m_com.getName(), m_faction.getPersonNamePrefix()), pad);
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
        final String title = selectedMarket == null ? m_market.getName() :
            strf("uiTwoNameAndSeparator", m_market.getName(), selectedMarket.getName());

        final MapParams params = new MapParams();
        params.showFilter = false;
        params.showTabs = false;
        params.withLayInCourse = false;
        params.skipCurrLocMarkerRendering = true;
        params.starSelectionRadiusMult = 0f;

        params.showSystem(starSystem);
        params.showMarket(m_market, 1);

        if (selectedMarket != null) {
            params.showSystem(selectedMarket.getStarSystem());
            params.showMarket(selectedMarket, 1);
        }
        
        params.positionToShowAllMarkersAndSystems(false, mapHeight);

        final UIPanelAPI map = tooltip.createSectorMap(
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
            str("uiBtnTitleProducers"), Fonts.ORBITRON_12,
            producerRunnable
        );
        consumerButton = new Button(
            section, btnWidth, btnHeight,
            str("uiBtnTitleConsumers"), Fonts.ORBITRON_12,
            consumerRunnable
        );
        producerButton.setLabelColor(base);
        consumerButton.setLabelColor(base);
        producerButton.cutStyle = CutStyle.TL_TR;
        consumerButton.cutStyle = CutStyle.TL_TR;
        producerButton.setShortcutAndAppendToText(Keyboard.KEY_1);
        consumerButton.setShortcutAndAppendToText(Keyboard.KEY_2);
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
        final String creditHeader = mode == 0 ? str("uiTableIncomeTitle") : str("uiTableValue");

        final TooltipBuilder quantityTooltip = createSection3QuantityHeaderTooltip(mode, table);

        final String marketTpDesc = mode == 0 ? str("uiTpTxtMarketShare1") : str("uiTpTxtMarketShare2");
        
        final String creditTpDesc = mode == 0 ? str("uiTpTxtCreditHeader1") : str("uiTpTxtCreditHeader2");

        table.addHeaders( // 876 pixels wide
            "", 0.04 * SECT3_WIDTH, null, true, false, 1,
            str("uiTableColony"), 0.18 * SECT3_WIDTH, str("uiTableTpTxtColony"), true, true, 1,
            str("uiTableSize"), 0.09 * SECT3_WIDTH, str("uiTableSizeTpTxt"), false, false, -1,
            str("uiTableFaction"), 0.17 * SECT3_WIDTH, str("uiTableFactionTpTxt"), false, false, -1,
            str("uiTableQuantity"), 0.05 * SECT3_WIDTH, quantityTooltip, true, true, 2,
            "", 0.1 * SECT3_WIDTH, null, true, false, 2,
            str("uiTableAccessibility"), 0.11 * SECT3_WIDTH, str("uiTableAccessibilityTpTxt"), false, false, -1,
            str("uiTableMarketShare"), 0.15 * SECT3_WIDTH, marketTpDesc, false, false, -1,
            creditHeader, 0.11 * SECT3_WIDTH, creditTpDesc, false, false, -1
        );

        final EconomyEngine engine = EconomyEngine.instance();

        for (MarketAPI market : EconomyInfo.getMarketsCopy()) {
            if (market.isHidden()) continue;

            final String marketID = market.getId();
            final CommodityCell cell = engine.getComCell(comID, marketID);

            if (footer != null && footer.isChecked() && !(cell.getStoredDeficit() + cell.getStoredExcess() > 0f)) {
                continue;
            }

            final double quantity = mode == 0 ? cell.getTotalExports(): cell.getTotalImports();
            if (quantity <= 0d) continue;

            final String iconPath = market.getFaction().getCrest();
            final Base iconPanel = new Base(section, iconSize, iconSize,
                iconPath, null, null
            );
            iconPanel.outline.enabled = cell.getStoredDeficit() > 0;
            iconPanel.outline.color = Color.RED;
            iconPanel.outline.offset.setOffset(-1, -1, 2, 2);

            final String factionName = market.getFaction().getDisplayName();

            final UIPanelAPI infoBar = new CommodityBarPanel(null, 75, iconSize,
                true, cell).getPanel();

            final int accessibility = (int) (market.getAccessibilityMod().computeEffective(0) * 100);

            final int marketShare = mode == 0 ? engine.info.getExportMarketShare(comID, marketID) :
                engine.info.getImportMarketShare(comID, marketID);

            final long incomeValue = MarketFinanceRegistry.instance().getLedger(marketID).getLastMonth(
                (mode == 0 ? TRADE_EXPORT_KEY : TRADE_IMPORT_KEY) + comID
            );

            final Color textColor = market.getFaction().getBaseUIColor();

            table.addCell(iconPanel, cellAlg.LEFTPAD, null, null);
            table.addCell(market.getName(), cellAlg.LEFTPAD, null, textColor);
            table.addCell(market.getSize(), cellAlg.MID, null, textColor);
            table.addCell(factionName, cellAlg.MID, null, textColor);
            table.addCell(NumFormat.engNotate(quantity), cellAlg.MID, quantity, null);
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

            final ClickHandler<TableRow> rowSelectedRunnable = (row, isLeftClick) -> {
                selectedMarket = (MarketAPI) row.customData;
                updateSection1();
                updateSection2();
            };

            table.pushRow(
                market, tp, rowSelectedRunnable, null,
                null, m_faction.getDarkUIColor()
            );

            if (m_market == market) table.selectLastRow();
        }

        section.addComponent(table.getPanel()).inTL(pad, 0f);

        table.sortRows(6);
    }

    private void createSection4(UIPanelAPI section) {
        if (m_market == null) return;

        final LtvCommodityPanel section4ComPanel = new LtvCommodityPanel(
            section,
            (int) section.getPosition().getWidth(),
            (int) section.getPosition().getHeight(),
            m_market.getName() + str("uiTitleCommoditiesSuffix"),
            true, m_market
        );
        section4ComPanel.selectRow(m_com.getId());

        section4ComPanel.selectionListener = (source, isLeftClick) -> {
            if (!isLeftClick) return;

            m_com = source.cell.spec;

            section4ComPanel.selectRow(source);

            if (UIUtils.canViewPrices()) {
                updateSection1();
                
                final int mode = producerButton.isChecked() ? 0 : 1;

                updateSection3(mode);
            } 
        };
        section4ComPanel.buildUI();

        section.addComponent(section4ComPanel.getPanel());
    }

    private TooltipBuilder createSection3RowsTooltip(MarketAPI market, String marketName, Color baseColor) {
        return (tp, exp) -> {
            final FactionAPI faction = market.getFaction();
            final CommodityCell cell = EconomyEngine.instance().getComCell(
                m_com.getId(), market.getId()
            );
    
            final Color darkColor = faction.getDarkUIColor();
            
            tp.setTitleSmallOrbitron();
            tp.addTitle(marketName + " - " + m_com.getName(), baseColor);
    
            String locString = strf("uiLocationPrefix", market.getContainingLocation().getNameWithLowercaseType());
            if (market.getContainingLocation().isHyperspace()) {
               locString = str("uiLocationHyperspace");
            }
    
            tp.addPara(strf("uiMarketStatusSummaryTxt", market.getName(), locString),
                opad, new Color[]{highlight, baseColor, highlight},
                new String[]{
                    Integer.toString(market.getSize()),
                    market.getFaction().getPersonNamePrefix(),
                    Integer.toString((int)market.getStabilityValue())
                }
            );
    
            tp.addSectionHeading(strf("uiProductionConsumptionSuffix", m_com.getName()),
                baseColor, darkColor, Alignment.MID, opad
            );
                
            TooltipUtils.createComProductionBreakdown(tp, cell);
    
            TooltipUtils.createComConsumptionBreakdown(tp, cell);
    
            final float econUnit = m_com.getEconUnit();
            final int sellPrice = (int) (cell.computeVanillaPrice((int)econUnit, 0d, true, true) / econUnit);
            final int buyPrice = (int) (cell.computeVanillaPrice((int)econUnit, 0d, false, true) / econUnit);
    
            if (!m_com.isMeta()) {
                if (cell.getStoredExcess() > 0f) {
                    tp.addPara(str("uiExcessStockSuffix"), opad, positive, 
                    highlight, NumFormat.engNotate(cell.getStoredExcess()));
                } else if (cell.getStoredDeficit() > 0f) {
                    tp.addPara(str("uiDeficitStockSuffix"), opad, negative, 
                    highlight, NumFormat.engNotate(cell.getStoredDeficit()));
                }
    
                tp.addPara(str("uiBoughtAndSoldForTxt"), opad, highlight, new String[]{
                    Misc.getDGSCredits(buyPrice), Misc.getDGSCredits(sellPrice), Misc.getWithDGS(econUnit)
                });
            }
    
            tp.addSectionHeading(str("uiTitleColonyAccessibility"), baseColor, darkColor, Alignment.MID, opad);
    
            final int stability = (int) (market.getAccessibilityMod().computeEffective(0) * 100);
            Color valueColor = highlight;
            if (stability <= 0) {
                valueColor = negative;
            }
    
            tp.addPara(str("uiAccessibilitySuffix"), opad, valueColor, stability + "%");
    
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
        };
    }

    private static final TooltipBuilder createSection3QuantityHeaderTooltip(
        int mode, SortableTable table
    ) {
        final String quantityDesc = mode == 0 ? str("uiTableQuantityTpTxt1") : str("uiTableQuantityTpTxt2");

        return (tp, exp) -> {
            tp.addPara(quantityDesc, pad);

            final int y = (int) tp.getHeightSoFar() + pad + opad;

            CommodityRowPanel.legendRowCreator(
                1, tp, y, 26
            );
        };
    }
}
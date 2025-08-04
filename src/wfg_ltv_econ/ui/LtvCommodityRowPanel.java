package wfg_ltv_econ.ui;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.econ.CommodityMarketDataAPI;
import com.fs.starfarer.api.campaign.econ.CommodityOnMarketAPI;
import com.fs.starfarer.api.campaign.econ.CommoditySourceType;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.ui.Alignment;
import com.fs.starfarer.api.ui.CustomPanelAPI;
import com.fs.starfarer.api.ui.Fonts;
import com.fs.starfarer.api.ui.IconRenderMode;
import com.fs.starfarer.api.ui.LabelAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.ui.UIComponentAPI;
import com.fs.starfarer.api.ui.UIPanelAPI;
import com.fs.starfarer.api.util.Misc;
import com.fs.starfarer.api.impl.campaign.ids.Strings;
import com.fs.starfarer.api.loading.Description.Type;

import wfg_ltv_econ.plugins.LtvSpritePanelPlugin;
import wfg_ltv_econ.plugins.LtvCommodityRowPanelPlugin;
import wfg_ltv_econ.plugins.LtvCustomPanelPlugin.Glow;
import wfg_ltv_econ.plugins.LtvCustomPanelPlugin.Outline;
import wfg_ltv_econ.util.CommodityStats;
import wfg_ltv_econ.util.NumFormat;
import wfg_ltv_econ.util.TooltipUtils;
import wfg_ltv_econ.util.UiUtils;

import java.awt.Color;

public class LtvCommodityRowPanel extends LtvCustomPanel implements LtvCustomPanel.TooltipProvider {

    public static final int pad = 3;
    public static final int opad = 10;

    private static final int iconSize = 24;
    private static final String notExpandedCodexF1 = "F1 show legend";
    private static final String ExpandedCodexF1 = "F1 go back";
    private static final String codexF2 = "F2 open Codex";
    private TooltipMakerAPI codexTooltip;

    private final CommodityOnMarketAPI m_com;
    private final LtvCommodityPanel m_parentWrapper;
    private final CommodityStats m_comStats;

    public boolean isExpanded = false;

    public boolean m_canViewPrices;

    public LtvCommodityRowPanel(UIPanelAPI root, UIPanelAPI parent, MarketAPI market, CommodityOnMarketAPI com,
        LtvCommodityPanel parentWrapper, int width, int height, boolean childrenIgnoreUIState) {
        super(root, parent, width, height, new LtvCommodityRowPanelPlugin(), market);
        m_com = com;
        m_parentWrapper = parentWrapper;
        m_comStats = new CommodityStats(com, market);

        boolean viewAnywhere = Global.getSettings().getBoolean("allowPriceViewAtAnyColony");
        m_canViewPrices = Global.getSector().getIntelManager().isPlayerInRangeOfCommRelay() || viewAnywhere;

        initializePlugin(hasPlugin);
        createPanel();

        getPlugin().setIgnoreUIState(childrenIgnoreUIState);
    }

    public CommodityOnMarketAPI getCommodity() {
        return m_com;
    }

    public LtvCommodityPanel getParentWrapper() {
        return m_parentWrapper;
    }

    public void initializePlugin(boolean hasPlugin) {
        LtvCommodityRowPanelPlugin plugin = ((LtvCommodityRowPanelPlugin) m_panel.getPlugin());
        plugin.init(this, Glow.OVERLAY, true, false, Outline.NONE);
        plugin.setDisplayPrices(m_canViewPrices);
        plugin.setSoundEnabled(true);
    }

    public void createPanel() {
        final int textWidth = 60;
        final Color baseColor = getFaction().getBaseUIColor();
        final TooltipMakerAPI tooltip = m_panel.createUIElement(getPanelPos().getWidth(),
            getPanelPos().getHeight(), false);
        final float rowHeight = getPanelPos().getHeight();  

        // Amount label
        tooltip.setParaSmallInsignia();
        LabelAPI amountTxt = tooltip.addPara(NumFormat.engNotation(m_com.getAvailable()) + Strings.X, pad);
        final int textHeight = (int) amountTxt.computeTextHeight(amountTxt.getText());
        amountTxt.setColor(baseColor);
        amountTxt.getPosition().setSize(textWidth, textHeight);

        final float labelWidth = amountTxt.getPosition().getWidth() + pad;
        final UIComponentAPI lblComp = tooltip.getPrev();
        lblComp.getPosition().inBL(pad, (rowHeight - textHeight) / 2);

        // Icons
		tooltip.beginIconGroup();
		tooltip.setIconSpacingMedium();
		tooltip.addIcons(m_com, 1, IconRenderMode.NORMAL);
		tooltip.addIconGroup(iconSize ,0f);

		float actualIconWidth = iconSize * m_com.getCommodity().getIconWidthMult();
		tooltip.getPrev().getPosition().inBL(labelWidth + ((iconSize - actualIconWidth) * 0.5f),
            (rowHeight - iconSize) / 2);

        // Info Bar
        UiUtils.CommodityInfoBar(tooltip, iconSize, m_comStats);
        tooltip.getPrev().getPosition().inBL(labelWidth + iconSize + opad/2, (rowHeight - iconSize) / 2);

        // Source Icon
        CommodityMarketDataAPI commodityData = m_com.getCommodityMarketData();
        getPanel().addComponent(
            getSourceIcon(baseColor, commodityData, iconSize, m_panel).getPanel())
            .setSize(rowHeight, rowHeight).inBL(0, 0);

        if (commodityData.getExportIncome(m_com) > 0) {
            String iconPath = Global.getSettings().getSpriteName("commodity_markers", "exports");
            LtvSpritePanel iconPanel = new LtvSpritePanel(getRoot(), m_panel, m_market, iconSize, iconSize,
                    new LtvSpritePanelPlugin(), iconPath, null, null, false);
            iconPanel.getPlugin().setHasGlow(Glow.NONE);

            getPanel().addComponent(iconPanel.getPanel()).setSize(rowHeight, rowHeight).inBR(pad, 0.0F);
        }

        getPanel().addUIElement(tooltip).inBL(pad + iconSize, 0);
    }

    private LtvSpritePanel getSourceIcon(Color color, CommodityMarketDataAPI commodityData, int iconSize,
        UIPanelAPI parent) {
        boolean isSourceIllegal = commodityData.getMarketShareData(m_market).isSourceIsIllegal();

        CommoditySourceType source = m_com.getCommodityMarketData().getMarketShareData(m_market).getSource();
        String iconPath = Global.getSettings().getSpriteName("commodity_markers", "imports");
        Color baseColor = color;

        switch (source) {
            case GLOBAL:
                break;
            case IN_FACTION:
                iconPath = m_market.getFaction().getCrest();
                baseColor = null;
                break;
            case LOCAL:
            case NONE:
                iconPath = Global.getSettings().getSpriteName("commodity_markers", "production");
                break;
            default:
        }

        LtvSpritePanel iconPanel = new LtvSpritePanel(getRoot(), parent, m_market, iconSize, iconSize,
                    new LtvSpritePanelPlugin(), iconPath, baseColor, null, isSourceIllegal);
        iconPanel.getPlugin().setHasGlow(Glow.NONE);
        return iconPanel;
    }

    @Override
    public UIPanelAPI getTooltipAttachmentPoint() {
        return getParent();
    }

    @Override
    public TooltipMakerAPI createTooltip() {

        final Color highlight = Misc.getHighlightColor();
        final Color gray = new Color(100, 100, 100);
        final Color positive = Misc.getPositiveHighlightColor();
        final Color negative = Misc.getNegativeHighlightColor();

        TooltipMakerAPI tooltip = ((CustomPanelAPI)getParent()).createUIElement(500f, 0,false);

        final String comDesc = Global.getSettings().getDescription(m_com.getId(), Type.RESOURCE).getText1();

        tooltip.setParaFont(Fonts.ORBITRON_12);
        tooltip.addPara(m_com.getCommodity().getName(), getFaction().getBaseUIColor(), pad);

        tooltip.setParaFontDefault();
        tooltip.addPara(comDesc, opad);

        if (m_canViewPrices) {
            final String text = "Click to view global market info";
            tooltip.addPara(text, opad, positive, text);
        } else {
            final String text = "Must be in range of a comm relay to view global market info";
            tooltip.addPara(text, opad, negative, text);
        }
        if (!isExpanded) {

        tooltip.setParaFont(Fonts.ORBITRON_12);
        tooltip.addSectionHeading("Production, imports and demand", Alignment.MID, opad);

        // // Production
        // {
        TooltipUtils.createCommodityProductionBreakdown(
            tooltip, m_com, m_comStats, highlight, negative
        );
        
        tooltip.addPara("All production sources contribute cumulatively to the commodity's availability. Imports and smuggling add to supply to help meet demand.", gray ,pad);

        // Demand
        TooltipUtils.createCommodityDemandBreakdown(
            tooltip, m_com, m_comStats, highlight, negative
        );

        // Divider
        tooltip.addSectionHeading("Exports", Alignment.MID, opad);

        // Export stats
        int exportIncome = m_com.getCommodityMarketData().getExportIncome(m_com);
        boolean isIllegal = m_market.isIllegal(m_com);
        String commodityName = m_com.getCommodity().getName();

        if (m_comStats.totalExports < 1) {
            tooltip.addPara("No local production to export.", opad);
        } else
        if (isIllegal) {
            tooltip.addPara(
            m_market.getName() + " controls %s of the export market share for " + commodityName + ".This trade brings in no income due to being underground.",
            opad, highlight,
            m_com.getCommodityMarketData().getExportMarketSharePercent(m_market) + "%"
        );
        } else 
        if (exportIncome < 1) {
            tooltip.addPara(
            m_market.getName() + " is exporting %s units of " + commodityName + " and controls %s of the global market share. Exports of " + commodityName + " bring in no income.",
            opad, highlight,
            m_comStats.globalExport + "",
            m_com.getCommodityMarketData().getExportMarketSharePercent(m_market) + "%"
        );
        } else {
            tooltip.addPara(
            m_market.getName() + " is profitably exporting %s units of " + commodityName + " and controls %s of the global market share. Exports bring in %s per month.",
            opad, highlight,
            m_comStats.globalExport + "",
            m_com.getCommodityMarketData().getExportMarketSharePercent(m_market) + "%",
            exportIncome + Strings.C
            );

            if (m_comStats.canNotExport > 0) {
                tooltip.addPara(
                "Exports are reduced by %s due to insufficient accessibility.",
                pad, negative, NumFormat.engNotation(m_comStats.canNotExport)
            );
            }
        }

        // Bottom tip
        tooltip.addPara(
        "Increasing production and colony accessibility will both increase the export market share and income.", gray, opad);

        tooltip.addSpacer(opad*1.5f);

        final int codexW = 240; 

        TooltipUtils.createCustomCodex(tooltip, codexTooltip, this, notExpandedCodexF1, codexF2, codexW);

        } else {
            tooltip.setParaFont(Fonts.ORBITRON_12);
            tooltip.addSectionHeading("Legend", Alignment.MID, opad);
            tooltip.setParaFontDefault();

            final int legendIconSize = iconSize + 2;

            int y = (int)tooltip.getHeightSoFar() + opad + pad;

            legendRowCreator(tooltip, y, legendIconSize);

            tooltip.setHeightSoFar(y + opad*2);

            final int codexW = 200; 
            
            TooltipUtils.createCustomCodex(tooltip, codexTooltip, this, ExpandedCodexF1, codexF2, codexW);  
        }

        ((CustomPanelAPI)getParent()).addUIElement(tooltip);
        ((CustomPanelAPI)getParent()).bringComponentToTop(tooltip);
        tooltip.getPosition().inTL(-tooltip.getPosition().getWidth() - opad, 0);

        ((CustomPanelAPI)getParent()).addUIElement(codexTooltip);
        ((CustomPanelAPI)getParent()).bringComponentToTop(codexTooltip);
        codexTooltip.getPosition().belowLeft(tooltip, opad*1.5f - 1);
        // Idk why I need to do opad*1.5f to begin with. I hate the tooltip

        return tooltip;
    }

    @Override
    public void removeTooltip(TooltipMakerAPI tooltip) {
        if (codexTooltip != null) {
            ((CustomPanelAPI)getParent()).removeComponent(codexTooltip);
        }
        ((CustomPanelAPI)getParent()).removeComponent(tooltip);
    }

    @Override 
    public void attachCodexTooltip(TooltipMakerAPI codex) {
        codexTooltip = codex;
    }

    /**
     * Renders the legend icons and their descriptions in the given tooltip at the specified starting y-position.
     *
     * @param tooltip The TooltipMakerAPI instance to add components to.
     * @param startY The initial vertical position (y-coordinate) to start rendering the icons.
     * @param iconSize The size (width and height) of each legend icon.
     * @param pad The vertical padding between each legend row.
     */
    public void legendRowCreator(TooltipMakerAPI tooltip, int y, int iconSize) {
        String iconPath = Global.getSettings().getSpriteName("commodity_markers", "production");
        String desc = "Demand met through local production.";
        legendRowHelper(tooltip, y, iconPath, desc, iconSize, false, null);

        y += iconSize + pad;

        iconPath = m_market.getFaction().getCrest();
        desc = "Demand met through in-faction imports.";
        legendRowHelper(tooltip, y, iconPath, desc, iconSize, false, null);

        y += iconSize + pad;

        iconPath = Global.getSettings().getSpriteName("commodity_markers", "imports");
        desc = "Demand met through imports from outside the faction.";
        legendRowHelper(tooltip, y, iconPath, desc, iconSize, false, null);
        
        y += iconSize + pad;

        iconPath = Global.getSettings().getSpriteName("commodity_markers", "exports");
        desc = "Excess local production that is exported.";
        legendRowHelper(tooltip, y, iconPath, desc, iconSize, false, null);
        
        y += iconSize + pad;

        iconPath = "";
        desc = "Smuggled or produced by an illegal enterprise. No income from exports.";
        legendRowHelper(tooltip, y, iconPath, desc, iconSize, true, null);
        
        y += iconSize + pad;

        iconPath = "";
        desc = "Local production that could not be exported.";
        legendRowHelper(tooltip, y, iconPath, desc, iconSize, false, UiUtils.COLOR_NOT_EXPORTED);
        
        y += iconSize + pad;

        iconPath = "";
        desc = "Proportion of locally produced goods that were exported globally.";
        legendRowHelper(tooltip, y, iconPath, desc, iconSize, false, UiUtils.COLOR_EXPORT);
        
        y += iconSize + pad;

        iconPath = "";
        desc = "Production used for local demand or exports.";
        legendRowHelper(tooltip, y, iconPath, desc, iconSize, false, UiUtils.COLOR_LOCAL_PROD);
        
        y += iconSize + pad;

        iconPath = "";
        desc = "Proportion of available goods that were imported in-faction.";
        legendRowHelper(tooltip, y, iconPath, desc, iconSize, false, UiUtils.COLOR_FACTION_IMPORT);
        
        y += iconSize + pad;

        iconPath = "";
        desc = "Imported or available through one-time trade or events.";
        legendRowHelper(tooltip, y, iconPath, desc, iconSize, false, UiUtils.COLOR_IMPORT);
        
        y += iconSize + pad;

        iconPath = "";
        desc = "Deficit - in demand, but not available. Higher prices.";
        legendRowHelper(tooltip, y, iconPath, desc, iconSize, false, UiUtils.COLOR_DEFICIT);
    }

    private void legendRowHelper(TooltipMakerAPI tooltip, int y, String iconPath, String desc, int lgdIconSize,
        boolean drawRedBorder, Color drawFilledIcon) {

        LtvSpritePanel iconPanel = new LtvSpritePanel(getRoot(), m_panel, m_market, lgdIconSize, lgdIconSize,
            new LtvSpritePanelPlugin(), iconPath, null, drawFilledIcon, drawRedBorder);
        iconPanel.getPlugin().setHasGlow(Glow.NONE);
            
        tooltip.addComponent(iconPanel.getPanel()).setSize(lgdIconSize, lgdIconSize).inTL(pad + opad/2f, y);

        // Explanation Label
		LabelAPI lbl = tooltip.addPara(desc, pad);

		float textX = opad + lgdIconSize;
		lbl.getPosition().inTL(textX, y);
    }
}
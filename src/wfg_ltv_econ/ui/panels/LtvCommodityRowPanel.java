package wfg_ltv_econ.ui.panels;

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
import com.fs.starfarer.api.util.FaderUtil;
import com.fs.starfarer.api.util.Misc;
import com.fs.starfarer.api.impl.campaign.ids.Strings;
import com.fs.starfarer.api.impl.codex.CodexDataV2;
import com.fs.starfarer.api.loading.Description.Type;

import wfg_ltv_econ.economy.CommodityStats;
import wfg_ltv_econ.economy.EconomyEngine;
import wfg_ltv_econ.ui.components.FaderComponent.Glow;
import wfg_ltv_econ.ui.panels.LtvCustomPanel.AcceptsActionListener;
import wfg_ltv_econ.ui.panels.LtvCustomPanel.HasAudioFeedback;
import wfg_ltv_econ.ui.panels.LtvCustomPanel.HasFader;
import wfg_ltv_econ.ui.panels.LtvCustomPanel.HasTooltip;
import wfg_ltv_econ.ui.panels.LtvSpritePanel.Base;
import wfg_ltv_econ.ui.plugins.BasePanelPlugin;
import wfg_ltv_econ.ui.plugins.LtvSpritePanelPlugin;
import wfg_ltv_econ.util.NumFormat;
import wfg_ltv_econ.util.TooltipUtils;
import wfg_ltv_econ.util.UiUtils;
import wfg_ltv_econ.util.UiUtils.AnchorType;

import java.awt.Color;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

public class LtvCommodityRowPanel extends LtvCustomPanel<BasePanelPlugin<LtvCommodityRowPanel>, LtvCommodityRowPanel, CustomPanelAPI>
    implements HasTooltip, HasFader, HasAudioFeedback, AcceptsActionListener
{
    public static final int pad = 3;
    public static final int opad = 10;

    private static final int iconSize = 24;
    private static final String notExpandedCodexF1 = "F1 show legend";
    private static final String ExpandedCodexF1 = "F1 go back";
    private static final String codexF2 = "F2 open Codex";

    private final CommodityOnMarketAPI m_com;
    private final LtvCommodityPanel m_parent;
    private final FaderUtil m_fader;
    private final CommodityStats m_comStats;
    public TooltipMakerAPI m_tooltip = null;

    public boolean isExpanded = false;
    public boolean persistentGlow = false;
    public boolean m_canViewPrices = false;

    public LtvCommodityRowPanel(UIPanelAPI root, UIPanelAPI parent, MarketAPI market, String comID,
        LtvCommodityPanel parentWrapper, int width, int height, boolean childrenIgnoreUIState, boolean canViewPrices) {

        super(root, parent, width, height, new BasePanelPlugin<>(), market);
        m_comStats = EconomyEngine.getInstance().getComStats(comID, market);
        m_com = m_comStats.m_com;
        m_parent = parentWrapper;
        m_fader = new FaderUtil(0, 0, 0.2f, true, true);

        m_canViewPrices = canViewPrices;

        initializePlugin(hasPlugin);
        createPanel();

        getPlugin().setIgnoreUIState(childrenIgnoreUIState);
    }

    public CommodityOnMarketAPI getCommodity() {
        return m_com;
    }

    public LtvCommodityPanel getParentWrapper() {
        return m_parent;
    }

    public void initializePlugin(boolean hasPlugin) {
        getPlugin().init(this);
    }

    @Override
    public FaderUtil getFader() {
        return m_fader;
    }

    @Override
    public boolean isPersistentGlow() {
        return persistentGlow;
    }

    @Override
    public void setPersistentGlow(boolean a) {
        persistentGlow = a;
    }

    @Override
    public Color getGlowColor() {
        return Misc.getBasePlayerColor();
    }

    @Override
    public Glow getGlowType() {
        return Glow.UNDERLAY;
    }

    public HasActionListener selectionListener;

    public Optional<HasActionListener> getActionListener() {
        return Optional.ofNullable(selectionListener);
    }
    public void setActionListener(HasActionListener listener) {
        selectionListener = listener;
    }

    public void createPanel() {
        final int textWidth = 60;
        final Color baseColor = getFaction().getBaseUIColor();
        final TooltipMakerAPI tooltip = m_panel.createUIElement(getPos().getWidth(),
            getPos().getHeight(), false);
        final int rowHeight = (int) getPos().getHeight(); 

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
		tooltip.addIconGroup(iconSize + pad,0f);

		float actualIconWidth = iconSize * m_com.getCommodity().getIconWidthMult();
		tooltip.getPrev().getPosition().inBL(labelWidth + ((iconSize - actualIconWidth) * 0.5f),
            (rowHeight - iconSize) / 2);

        // Info Bar
        UiUtils.CommodityInfoBar(tooltip, iconSize, 85, m_comStats);
        tooltip.getPrev().getPosition().inBL(labelWidth + iconSize + opad/2, (rowHeight - iconSize) / 2);

        // Source Icon
        CommodityMarketDataAPI commodityData = m_com.getCommodityMarketData();
        getPanel().addComponent(
            getSourceIcon(baseColor, commodityData, rowHeight - 4, m_panel).getPanel())
            .inBL(2, 2);

        if (commodityData.getExportIncome(m_com) > 0) {
            String iconPath = Global.getSettings().getSpriteName("commodity_markers", "exports");
            LtvSpritePanel.Base iconPanel = new Base(getRoot(), m_panel, getMarket(), rowHeight - 4, rowHeight 
            - 4, new LtvSpritePanelPlugin<>(), iconPath, null, null, false);
            iconPanel.getPlugin().setOffsets(-1, -1, 2, 2);

            getPanel().addComponent(iconPanel.getPanel()).inRMid(pad);
        }

        getPanel().addUIElement(tooltip).inBL(pad + iconSize, 0);
    }

    private LtvSpritePanel.Base getSourceIcon(Color color, CommodityMarketDataAPI commodityData, int size,
        UIPanelAPI parent) {
        boolean isSourceIllegal = commodityData.getMarketShareData(getMarket()).isSourceIsIllegal();

        CommoditySourceType source = m_com.getCommodityMarketData().getMarketShareData(getMarket()).getSource();
        String iconPath = Global.getSettings().getSpriteName("commodity_markers", "imports");
        Color baseColor = color;

        switch (source) {
            case GLOBAL:
                break;
            case IN_FACTION:
                iconPath = getFaction().getCrest();
                baseColor = null;
                break;
            case LOCAL:
            case NONE:
                iconPath = Global.getSettings().getSpriteName("commodity_markers", "production");
                break;
            default:
        }

        LtvSpritePanel.Base iconPanel = new Base(getRoot(), parent, getMarket(), size, size,
                    new LtvSpritePanelPlugin<>(), iconPath, baseColor, null, isSourceIllegal);
        if (isSourceIllegal) {
            iconPanel.getPlugin().setOffsets(-1, -1, 2, 2);
            iconPanel.setOutlineColor(Color.RED);
        }
        return iconPanel;
    }

    @Override
    public CustomPanelAPI getTpParent() {
        return getParent();
    }

    @Override
    public TooltipMakerAPI createAndAttachTp() {

        final Color highlight = Misc.getHighlightColor();
        final Color gray = new Color(100, 100, 100);
        final Color positive = Misc.getPositiveHighlightColor();
        final Color negative = Misc.getNegativeHighlightColor();

        TooltipMakerAPI tooltip = getParent().createUIElement(500f, 0,false);

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
        boolean isIllegal = getMarket().isIllegal(m_com);
        String commodityName = m_com.getCommodity().getName();

        if (m_comStats.getTotalExports() < 1) {
            tooltip.addPara("No local production to export.", opad);
        } else
        if (isIllegal) {
            tooltip.addPara(
            getMarket().getName() + " controls %s of the export market share for " + commodityName + ".This trade brings in no income due to being underground.",
            opad, highlight,
            m_com.getCommodityMarketData().getExportMarketSharePercent(getMarket()) + "%"
        );
        } else 
        if (exportIncome < 1) {
            tooltip.addPara(
            getMarket().getName() + " is exporting %s units of " + commodityName + " and controls %s of the global market share. Exports of " + commodityName + " bring in no income.",
            opad, highlight,
            m_comStats.globalExports + "",
            m_com.getCommodityMarketData().getExportMarketSharePercent(getMarket()) + "%"
        );
        } else {
            tooltip.addPara(
            getMarket().getName() + " is profitably exporting %s units of " + commodityName + " and controls %s of the global market share. Exports bring in %s per month.",
            opad, highlight,
            m_comStats.globalExports + "",
            m_com.getCommodityMarketData().getExportMarketSharePercent(getMarket()) + "%",
            exportIncome + Strings.C
            );

            if (m_comStats.getCanNotExport() > 0) {
                tooltip.addPara(
                "Exports are reduced by %s due to insufficient accessibility.",
                pad, negative, NumFormat.engNotation(m_comStats.getCanNotExport())
            );
            }
        }

        // Bottom tip
        tooltip.addPara(
        "Increasing production and colony accessibility will both increase the export market share and income.", gray, opad);

        tooltip.addSpacer(opad*1.5f);

        } else {
            tooltip.setParaFont(Fonts.ORBITRON_12);
            tooltip.addSectionHeading("Legend", Alignment.MID, opad);
            tooltip.setParaFontDefault();

            final int legendIconSize = iconSize + 2;

            AtomicInteger y = new AtomicInteger((int)tooltip.getHeightSoFar() + opad + pad);

            legendRowCreator(0, tooltip, y, legendIconSize, getRoot(), m_panel, getMarket()); 
        }

        getParent().addUIElement(tooltip);
        tooltip.getPosition().inTL(-tooltip.getPosition().getWidth() - opad, 0);

        m_tooltip = tooltip;

        return tooltip;
    }
    @Override
    public Optional<CustomPanelAPI> getCodexParent() {
        return Optional.ofNullable(getParent());
    }

    @Override
    public Optional<String> getCodexID() {;
        return Optional.ofNullable(CodexDataV2.getCommodityEntryId(getCommodity().getId()));
    }

    @Override
    public Optional<TooltipMakerAPI> createAndAttachCodex() {

        TooltipMakerAPI codex = null;

        if(!isExpanded) {
            final int codexW = 240;
            codex = TooltipUtils.createCustomCodex(this, notExpandedCodexF1, codexF2, codexW);

        } else {
            final int codexW = 200; 
            codex = TooltipUtils.createCustomCodex(this, ExpandedCodexF1, codexF2, codexW); 
        }

        getParent().addUIElement(codex);
        UiUtils.anchorPanel(codex, m_tooltip, AnchorType.BottomLeft, opad + pad);

        return Optional.ofNullable(codex);
    }

    @Override
    public boolean isExpanded() {
        return isExpanded;
    }

    @Override
    public void setExpanded(boolean a) {
        isExpanded = a;
    }

    /**
     * Renders the legend icons and their descriptions in the given tooltip at the specified starting y-position.
     * 
     * <br></br> MODE_0: shows everything.
     * <br></br> MODE_1: shows only the CommodityInfoBar relevant info.
     */
    public static void legendRowCreator(int mode, TooltipMakerAPI tooltip, AtomicInteger y, int iconSize,
        UIPanelAPI root, UIPanelAPI panel, MarketAPI market) {

        String iconPath;
        String desc;

        if (mode == 0) {
            iconPath = Global.getSettings().getSpriteName("commodity_markers", "production");
            desc = "Demand met through local production.";
            legendRowHelper(tooltip, y.get(), iconPath, desc, iconSize, false, null, root, panel, market);
    
            y.addAndGet(iconSize + pad);
    
            iconPath = market.getFaction().getCrest();
            desc = "Demand met through in-faction imports.";
            legendRowHelper(tooltip, y.get(), iconPath, desc, iconSize, false, null, root, panel, market);
    
            y.addAndGet(iconSize + pad);
    
            iconPath = Global.getSettings().getSpriteName("commodity_markers", "imports");
            desc = "Demand met through imports from outside the faction.";
            legendRowHelper(tooltip, y.get(), iconPath, desc, iconSize, false, null, root, panel, market);
            
            y.addAndGet(iconSize + pad);
    
            iconPath = Global.getSettings().getSpriteName("commodity_markers", "exports");
            desc = "Excess local production that is exported.";
            legendRowHelper(tooltip, y.get(), iconPath, desc, iconSize, false, null, root, panel, market);
            
            y.addAndGet(iconSize + pad);
    
            iconPath = "";
            desc = "Smuggled or produced by an illegal enterprise. No income from exports.";
            legendRowHelper(tooltip, y.get(), iconPath, desc, iconSize, true, null, root, panel, market);
            
            y.addAndGet(iconSize + pad);
        }

        iconPath = "";
        desc = "Local production that could not be exported.";
        legendRowHelper(tooltip, y.get(), iconPath, desc, iconSize, false, UiUtils.COLOR_NOT_EXPORTED, root, panel, market);
        
        y.addAndGet(iconSize + pad);

        iconPath = "";
        desc = "Proportion of locally produced goods that were exported globally.";
        legendRowHelper(tooltip, y.get(), iconPath, desc, iconSize, false, UiUtils.COLOR_EXPORT, root, panel, market);
        
        y.addAndGet(iconSize + pad);

        iconPath = "";
        desc = "Production used for local demand.";
        legendRowHelper(tooltip, y.get(), iconPath, desc, iconSize, false, UiUtils.COLOR_LOCAL_PROD, root, panel, market);
        
        y.addAndGet(iconSize + pad);

        iconPath = "";
        desc = "Proportion of available goods that were imported in-faction.";
        legendRowHelper(tooltip, y.get(), iconPath, desc, iconSize, false, UiUtils.COLOR_FACTION_IMPORT, root, panel, market);
        
        y.addAndGet(iconSize + pad);

        iconPath = "";
        desc = "Imported or available through one-time trade or events.";
        legendRowHelper(tooltip, y.get(), iconPath, desc, iconSize, false, UiUtils.COLOR_IMPORT, root, panel, market);
        
        y.addAndGet(iconSize + pad);

        iconPath = "";
        desc = "Deficit - in demand, but not available. Higher prices.";
        legendRowHelper(tooltip, y.get(), iconPath, desc, iconSize, false, UiUtils.COLOR_DEFICIT, root, panel, market);
    
        tooltip.setHeightSoFar(y.get() + opad*2);
    }

    private static void legendRowHelper(TooltipMakerAPI tooltip, int y, String iconPath, String desc, int lgdIconSize,
        boolean drawRedBorder, Color drawFilledIcon, UIPanelAPI root, UIPanelAPI panel, MarketAPI market) {

        LtvSpritePanel.Base iconPanel = new Base(root, panel, market, lgdIconSize, lgdIconSize,
            new LtvSpritePanelPlugin<>(), iconPath, null, drawFilledIcon, drawRedBorder);
        if (drawRedBorder) {
            iconPanel.setOutlineColor(Color.RED);
            iconPanel.getPlugin().setOffsets(2, 2, -4, -4);
        }
            
        tooltip.addComponent(iconPanel.getPanel()).setSize(lgdIconSize, lgdIconSize).inTL(pad + opad/2f, y);

        // Explanation Label
		LabelAPI lbl = tooltip.addPara(desc, pad);

		float textX = opad + lgdIconSize;
		lbl.getPosition().inTL(textX, y);
    }
}
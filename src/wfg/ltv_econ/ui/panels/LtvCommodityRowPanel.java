package wfg.ltv_econ.ui.panels;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.econ.CommoditySpecAPI;
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

import wfg.ltv_econ.economy.CommodityInfo;
import wfg.ltv_econ.economy.CommodityStats;
import wfg.ltv_econ.economy.EconomyEngine;
import wfg.ltv_econ.util.TooltipUtils;
import wfg.ltv_econ.util.UiUtils;
import wfg.wrap_ui.util.NumFormat;
import wfg.wrap_ui.util.WrapUiUtils;
import wfg.wrap_ui.util.WrapUiUtils.AnchorType;
import wfg.wrap_ui.ui.panels.CustomPanel;
import wfg.wrap_ui.ui.panels.CustomPanel.AcceptsActionListener;
import wfg.wrap_ui.ui.panels.CustomPanel.HasAudioFeedback;
import wfg.wrap_ui.ui.panels.CustomPanel.HasFader;
import wfg.wrap_ui.ui.panels.CustomPanel.HasTooltip;
import wfg.wrap_ui.ui.panels.SpritePanel.Base;
import wfg.wrap_ui.ui.plugins.BasePanelPlugin;
import wfg.wrap_ui.ui.systems.FaderSystem.Glow;

import com.fs.starfarer.api.impl.campaign.ids.Strings;
import com.fs.starfarer.api.impl.codex.CodexDataV2;
import com.fs.starfarer.api.loading.Description.Type;

import java.awt.Color;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

public class LtvCommodityRowPanel extends CustomPanel<BasePanelPlugin<LtvCommodityRowPanel>, LtvCommodityRowPanel, CustomPanelAPI>
    implements HasTooltip, HasFader, HasAudioFeedback, AcceptsActionListener
{
    public static final int pad = 3;
    public static final int opad = 10;

    private static final int iconSize = 24;
    private static final String notExpandedCodexF1 = "F1 show legend";
    private static final String ExpandedCodexF1 = "F1 go back";
    private static final String codexF2 = "F2 open Codex";

    private final CommoditySpecAPI m_com;
    private final LtvCommodityPanel m_parent;
    private final FaderUtil m_fader;
    private final CommodityStats m_comStats;
    private final MarketAPI m_market;
    public TooltipMakerAPI m_tooltip = null;

    public boolean isExpanded = false;
    public boolean persistentGlow = false;
    public boolean m_canViewPrices = false;

    public LtvCommodityRowPanel(UIPanelAPI parent, MarketAPI market, String comID,
        LtvCommodityPanel parentWrapper, int width, int height, boolean childrenIgnoreUIState, boolean canViewPrices) {

        super(parent, width, height, new BasePanelPlugin<>());
        m_comStats = EconomyEngine.getInstance().getComStats(comID, market.getId());
        m_com = Global.getSettings().getCommoditySpec(comID);
        m_parent = parentWrapper;
        m_fader = new FaderUtil(0, 0, 0.2f, true, true);
        m_market = market;

        m_canViewPrices = canViewPrices;

        getPlugin().init(this);
        getPlugin().setIgnoreUIState(childrenIgnoreUIState);

        createPanel();
    }

    public CommoditySpecAPI getCommodity() {
        return m_com;
    }

    public LtvCommodityPanel getParentWrapper() {
        return m_parent;
    }

    @Override
    public FaderUtil getFader() {
        return m_fader;
    }

    @Override
    public boolean isPersistentGlow() {
        return persistentGlow;
    }

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
        final Color baseColor = m_market.getFaction().getBaseUIColor();
        final TooltipMakerAPI tooltip = m_panel.createUIElement(getPos().getWidth(),
            getPos().getHeight(), false);
        final int rowHeight = (int) getPos().getHeight(); 

        // Amount label
        tooltip.setParaSmallInsignia();
        final LabelAPI amountTxt = tooltip.addPara(NumFormat.engNotation(
            (int)m_comStats.getFlowAvailable()) + Strings.X, pad
        );
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

		float actualIconWidth = iconSize * m_com.getIconWidthMult();
		tooltip.getPrev().getPosition().inBL(labelWidth + ((iconSize - actualIconWidth) * 0.5f),
            (rowHeight - iconSize) / 2);

        // Info Bar
        UiUtils.CommodityInfoBar(tooltip, iconSize, 85, m_comStats);
        tooltip.getPrev().getPosition().inBL(labelWidth + iconSize + opad/2, (rowHeight - iconSize) / 2);

        // Source Icon
        getPanel().addComponent(
            getSourceIcon(baseColor, rowHeight - 4, m_panel).getPanel())
            .inBL(2, 2);

        if (m_comStats.globalExports > 0) {
            final String iconPath = Global.getSettings().getSpriteName("commodity_markers", "exports");
            final Base iconPanel = new Base(m_panel, rowHeight - 4, rowHeight 
                - 4, iconPath, null, null, false
            );
            iconPanel.getPlugin().setOffsets(-1, -1, 2, 2);

            getPanel().addComponent(iconPanel.getPanel()).inRMid(pad);
        }

        getPanel().addUIElement(tooltip).inBL(pad + iconSize, 0);
    }

    private Base getSourceIcon(Color color, int size, UIPanelAPI parent) {
        String iconPath = Global.getSettings().getSpriteName("commodity_markers", "imports");
        Color baseColor = color;

        if (m_comStats.globalImports < 0) {
            if (m_comStats.inFactionImports > 0) {
                iconPath = m_market.getFaction().getCrest();
                baseColor = null;
            } else {
                iconPath = Global.getSettings().getSpriteName("commodity_markers", "production");
            }
        }
        
        return new Base(parent, size, size, iconPath, baseColor, null, false);
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
        final EconomyEngine engine = EconomyEngine.getInstance();
        final TooltipMakerAPI tooltip = getParent().createUIElement(500f, 0,false);
        final String comID = m_com.getId();

        final String comDesc = Global.getSettings().getDescription(comID, Type.RESOURCE).getText1();

        tooltip.setParaFont(Fonts.ORBITRON_12);
        tooltip.addPara(m_com.getName(), m_market.getFaction().getBaseUIColor(), pad);

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
            tooltip, m_comStats, highlight, negative
        );
        
        tooltip.addPara("All production sources contribute cumulatively to the commodity's availability. Imports and smuggling add to supply to help meet demand.", gray ,pad);

        // Demand
        TooltipUtils.createCommodityDemandBreakdown(
            tooltip, m_comStats, highlight, negative
        );

        // Divider
        tooltip.addSectionHeading("Exports", Alignment.MID, opad);

        // Export stats
        final CommodityInfo info = engine.getCommodityInfo(comID);
        
        final long exportIncomeLastMonth = info.hasLedger(m_comStats.marketID) ?
            info.getLedger(m_comStats.marketID).lastMonthExportIncome : 0;
        final long exportIncomeThisMonth = info.hasLedger(m_comStats.marketID) ?
            info.getLedger(m_comStats.marketID).monthlyExportIncome : 0;
        final boolean isIllegal = m_market.isIllegal(comID);
        final String commodityName = m_com.getName();

        if (m_comStats.getTotalExports() < 1) {
            tooltip.addPara("No local production to export.", opad);
        } else if (isIllegal) {
            tooltip.addPara(
            m_market.getName() + " controls %s of the export market share for " + commodityName + ".This trade brings in no income due to being underground.",
            opad, highlight,
            engine.getExportMarketShare(comID, m_comStats.marketID) + "%"
        );
        } else if (exportIncomeLastMonth < 1 && exportIncomeThisMonth < 1) {
            tooltip.addPara(
                m_market.getName() + " is exporting %s units of " + commodityName + " and controls %s of the global market share. Income from exports are not tracked for non-player colonies.",
                opad, highlight,
                NumFormat.engNotation((long) m_comStats.getTotalExports()),
                engine.getExportMarketShare(comID, m_comStats.marketID) + "%"
            );
        } else {
            tooltip.addPara(
                m_market.getName() + " is profitably exporting %s units of " + commodityName + " and controls %s of the global market share. They generated %s last month and %s so far this month.",
                opad, highlight,
                NumFormat.engNotation((long) m_comStats.getTotalExports()),
                engine.getExportMarketShare(comID, m_comStats.marketID) + "%",
                NumFormat.formatCredits(exportIncomeLastMonth),
                NumFormat.formatCredits(exportIncomeThisMonth)
            );

            if (m_comStats.getFlowCanNotExport() > 0) {
                tooltip.addPara(
                    "Exports are reduced by %s due to insufficient importers.",
                    pad, negative, NumFormat.engNotation((int)m_comStats.getFlowCanNotExport())
                );
            }
        }

        // Bottom tip
        tooltip.addPara(
            "Increasing production and colony accessibility will both increase the export market share and income.", gray, opad
        );

        tooltip.addSpacer(opad*1.5f);

        } else {
            tooltip.setParaFont(Fonts.ORBITRON_12);
            tooltip.addSectionHeading("Legend", Alignment.MID, opad);
            tooltip.setParaFontDefault();

            final int legendIconSize = iconSize + 2;

            AtomicInteger y = new AtomicInteger((int)tooltip.getHeightSoFar() + opad + pad);

            legendRowCreator(0, tooltip, y, legendIconSize, m_market); 
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
        WrapUiUtils.anchorPanel(codex, m_tooltip, AnchorType.BottomLeft, opad + pad);

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
        MarketAPI market) {

        String iconPath;
        String desc;

        if (mode == 0) {
            iconPath = Global.getSettings().getSpriteName("commodity_markers", "production");
            desc = "Demand met through local production.";
            legendRowHelper(tooltip, y.get(), iconPath, desc, iconSize, false, null);
    
            y.addAndGet(iconSize + pad);
    
            iconPath = market.getFaction().getCrest();
            desc = "Demand met through in-faction imports.";
            legendRowHelper(tooltip, y.get(), iconPath, desc, iconSize, false, null);
    
            y.addAndGet(iconSize + pad);
    
            iconPath = Global.getSettings().getSpriteName("commodity_markers", "imports");
            desc = "Demand met through imports from outside the faction.";
            legendRowHelper(tooltip, y.get(), iconPath, desc, iconSize, false, null);
            
            y.addAndGet(iconSize + pad);
    
            iconPath = Global.getSettings().getSpriteName("commodity_markers", "exports");
            desc = "Excess local production that is exported.";
            legendRowHelper(tooltip, y.get(), iconPath, desc, iconSize, false, null);
            
            y.addAndGet(iconSize + pad);
    
            iconPath = "";
            desc = "Smuggled or produced by an illegal enterprise. No income from exports.";
            legendRowHelper(tooltip, y.get(), iconPath, desc, iconSize, true, null);
            
            y.addAndGet(iconSize + pad);
        }

        iconPath = "";
        desc = "Local production that could not be exported.";
        legendRowHelper(tooltip, y.get(), iconPath, desc, iconSize, false, UiUtils.COLOR_NOT_EXPORTED);
        
        y.addAndGet(iconSize + pad);

        desc = "Proportion of locally produced goods that were exported globally.";
        legendRowHelper(tooltip, y.get(), iconPath, desc, iconSize, false, UiUtils.COLOR_EXPORT);
        
        y.addAndGet(iconSize + pad);

        desc = "Production used for local demand.";
        legendRowHelper(tooltip, y.get(), iconPath, desc, iconSize, false, UiUtils.COLOR_LOCAL_PROD);
        
        y.addAndGet(iconSize + pad);

        desc = "Proportion of available goods that were imported in-faction.";
        legendRowHelper(tooltip, y.get(), iconPath, desc, iconSize, false, UiUtils.COLOR_FACTION_IMPORT);
        
        y.addAndGet(iconSize + pad);

        desc = "Imported or available through one-time trade or events.";
        legendRowHelper(tooltip, y.get(), iconPath, desc, iconSize, false, UiUtils.COLOR_IMPORT);
        
        y.addAndGet(iconSize + pad);

        desc = "Goods that must be imported regardless of local stockpiles or demand.";
        legendRowHelper(tooltip, y.get(), iconPath, desc, iconSize, false, UiUtils.COLOR_IMPORT_EXCLUSIVE);
        
        y.addAndGet(iconSize + pad);

        desc = "Excess imports beyond current demand stockpiled for future use.";
        legendRowHelper(tooltip, y.get(), iconPath, desc, iconSize, false, UiUtils.COLOR_OVER_IMPORT);
        
        y.addAndGet(iconSize + pad);

        desc = "Deficit - in demand, but not available. Higher prices.";
        legendRowHelper(tooltip, y.get(), iconPath, desc, iconSize, false, UiUtils.COLOR_DEFICIT);
    
        tooltip.setHeightSoFar(y.get() + opad*2);
    }

    private static void legendRowHelper(TooltipMakerAPI tooltip, int y, String iconPath, String desc,
        int lgdIconSize, boolean drawRedBorder, Color drawFilledIcon
    ) {

        final Base iconPanel = new Base(tooltip, lgdIconSize, lgdIconSize,
            iconPath, null, drawFilledIcon, drawRedBorder
        );
        if (drawRedBorder) {
            iconPanel.setOutlineColor(Color.RED);
            iconPanel.getPlugin().setOffsets(2, 2, -4, -4);
        }
            
        tooltip.addComponent(iconPanel.getPanel()).setSize(lgdIconSize, lgdIconSize).inTL(pad + opad/2f, y);

        // Explanation Label
		final LabelAPI lbl = tooltip.addPara(desc, pad);

		final float textX = opad + lgdIconSize;
		lbl.getPosition().inTL(textX, y);
    }
}
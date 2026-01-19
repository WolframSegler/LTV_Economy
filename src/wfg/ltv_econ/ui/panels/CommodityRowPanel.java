package wfg.ltv_econ.ui.panels;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.econ.CommoditySpecAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.ui.Alignment;
import com.fs.starfarer.api.ui.CustomPanelAPI;
import com.fs.starfarer.api.ui.Fonts;
import com.fs.starfarer.api.ui.LabelAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.ui.UIPanelAPI;
import com.fs.starfarer.api.util.FaderUtil;

import wfg.ltv_econ.economy.CommodityDomain;
import wfg.ltv_econ.economy.engine.EconomyEngine;
import wfg.ltv_econ.economy.CommodityCell;
import wfg.ltv_econ.util.TooltipUtils;
import wfg.ltv_econ.util.UiUtils;
import wfg.wrap_ui.util.NumFormat;
import wfg.wrap_ui.util.WrapUiUtils;
import wfg.wrap_ui.util.WrapUiUtils.AnchorType;
import wfg.wrap_ui.ui.ComponentFactory;
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
import static wfg.wrap_ui.util.UIConstants.*;

public class CommodityRowPanel extends CustomPanel<BasePanelPlugin<CommodityRowPanel>, CommodityRowPanel>
    implements HasTooltip, HasFader, HasAudioFeedback, AcceptsActionListener
{
    private static final String EXPORTS_ICON_PATH = Global.getSettings().getSpriteName(
        "commodity_markers", "exports");

    private static final String notExpandedCodexF1 = "F1 show legend";
    private static final String ExpandedCodexF1 = "F1 go back";
    private static final String codexF2 = "F2 open Codex";

    private static final int iconSize = 26;

    private final CommoditySpecAPI m_com;
    private final FaderUtil m_fader = new FaderUtil(0, 0, 0.2f, true, true);
    private final CommodityCell m_cell;
    private final MarketAPI m_market;
    public TooltipMakerAPI m_tooltip = null;

    public boolean isExpanded = false;
    public boolean persistentGlow = false;

    public CommodityRowPanel(UIPanelAPI parent, MarketAPI market, String comID,
        int width, int height, boolean childrenIgnoreUIState
    ) {
        super(parent, width, height, new BasePanelPlugin<>());
        m_cell = EconomyEngine.getInstance().getComCell(comID, market.getId());
        m_com = Global.getSettings().getCommoditySpec(comID);
        m_market = market;

        getPlugin().init(this);
        getPlugin().setIgnoreUIState(childrenIgnoreUIState);

        createPanel();
    }

    public CommoditySpecAPI getCommodity() { return m_com; }

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
        return base;
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
        final int textWidth = 65;
        final int rowHeight = (int) getPos().getHeight();

        final Base comIcon = new Base(m_panel, rowHeight, rowHeight, m_cell.spec.getIconName(),
            null, null);
        add(comIcon).inBL(2f, 0f);

        final LabelAPI amountLbl = Global.getSettings().createLabel(NumFormat.engNotation(
            (int)m_cell.getFlowAvailable()) + Strings.X, Fonts.INSIGNIA_LARGE
        );
        amountLbl.autoSizeToWidth(textWidth);
        final float textHeight = amountLbl.computeTextHeight(amountLbl.getText());
        amountLbl.setColor(m_market.getFaction().getBaseUIColor());
        add(amountLbl).inBL(pad*2 + rowHeight, (rowHeight - textHeight) / 2f);

        final Base stockIcon = TooltipUtils.getStockpilesIcon(m_cell.getDesiredAvailabilityRatio(),
            iconSize, m_panel, base
        );
        add(stockIcon).inBL(pad*3 + rowHeight + textWidth, (rowHeight - iconSize) / 2f);

        final CustomPanelAPI infoBar = UiUtils.CommodityInfoBar(iconSize, 85, m_cell);
        add(infoBar).inBL(pad*4 + rowHeight + textWidth + iconSize, (rowHeight - iconSize) / 2f);

        if (m_cell.globalExports > 0) {
            final Base iconPanel = new Base(m_panel, rowHeight - 4, rowHeight - 4,
                EXPORTS_ICON_PATH, null, null);
            iconPanel.getPlugin().setOffsets(-1, -1, 2, 2);

            add(iconPanel).inRMid(pad);
        }
    }

    @Override
    public UIPanelAPI getTpParent() {
        return m_parent;
    }

    @Override
    public TooltipMakerAPI createAndAttachTp() {
        final EconomyEngine engine = EconomyEngine.getInstance();
        final TooltipMakerAPI tp = ComponentFactory.createTooltip(500f, false);
        final String comID = m_com.getId();

        final String comDesc = Global.getSettings().getDescription(comID, Type.RESOURCE).getText1();

        tp.setParaFont(Fonts.ORBITRON_12);
        tp.addPara(m_com.getName(), m_market.getFaction().getBaseUIColor(), pad);

        tp.setParaFontDefault();
        tp.addPara(comDesc, opad);

        if (UiUtils.canViewPrices()) {
            final String text = "Click to view global market info";
            tp.addPara(text, opad, positive, text);
        } else {
            final String text = "Must be in range of a comm relay to view global market info";
            tp.addPara(text, opad, negative, text);
        }
        if (!isExpanded) {

        tp.setParaFont(Fonts.ORBITRON_12);
        tp.addSectionHeading("Stockpiles", Alignment.MID, opad);

        TooltipUtils.createCommodityStockpilesBreakdown(tp, m_cell);

        tp.setParaFont(Fonts.ORBITRON_12);
        tp.addSectionHeading("Production, imports and demand", Alignment.MID, opad);

        // Production
        TooltipUtils.createCommodityProductionBreakdown(tp, m_cell);
        
        tp.addPara("All production sources contribute to the commodity's availability. Imports and smuggling add to supply to help meet demand.", gray, pad);

        // Demand
        tp.setParaFont(Fonts.ORBITRON_12);
        TooltipUtils.createCommodityDemandBreakdown(tp, m_cell);

        // Divider
        tp.addSectionHeading("Exports", Alignment.MID, opad);

        // Export stats
        final CommodityDomain dom = engine.getComDomain(comID);
        
        final long exportIncomeLastMonth = dom.hasLedger(m_cell.marketID) ?
            dom.getLedger(m_cell.marketID).lastMonthExportIncome : 0;
        final long exportIncomeThisMonth = dom.hasLedger(m_cell.marketID) ?
            dom.getLedger(m_cell.marketID).monthlyExportIncome : 0;
        final boolean isIllegal = m_market.isIllegal(comID);
        final String commodityName = m_com.getName();

        if (exportIncomeLastMonth > 1 || exportIncomeThisMonth > 1) {
            tp.addPara(
                m_market.getName() + " is profitably exporting %s units of " + commodityName + " and controls %s of the global market share. They generated %s last month and %s so far this month.",
                opad, highlight,
                NumFormat.engNotation((long) m_cell.getTotalExports()),
                engine.info.getExportMarketShare(comID, m_cell.marketID) + "%",
                NumFormat.formatCredit(exportIncomeLastMonth),
                NumFormat.formatCredit(exportIncomeThisMonth)
            );
        } else if (m_cell.getTotalExports() < 1) {
            tp.addPara("No recent local production to export.", opad);
        } else if (isIllegal) {
            tp.addPara(
            m_market.getName() + " controls %s of the export market share for " + commodityName + ".This trade brings in no income due to being underground.",
            opad, highlight,
            engine.info.getExportMarketShare(comID, m_cell.marketID) + "%"
        );
        } else if (exportIncomeLastMonth < 1 && exportIncomeThisMonth < 1) {
            tp.addPara(
                m_market.getName() + " is exporting %s units of " + commodityName + " and controls %s of the global market share. Income from exports are not tracked for non-player colonies.",
                opad, highlight,
                NumFormat.engNotation((long) m_cell.getTotalExports()),
                engine.info.getExportMarketShare(comID, m_cell.marketID) + "%"
            );
        }

        if (m_cell.getFlowCanNotExport() > 0) {
            tp.addPara(
                "Exports are reduced by %s due to insufficient importers.",
                pad, negative, NumFormat.engNotation((int)m_cell.getFlowCanNotExport())
            );
        }

        // Bottom tip
        tp.addPara(
            "Markets with higher production and accessibility are prioritized for exports and imports.", gray, opad
        );

        tp.addSpacer(opad*1.5f);

        } else {
            tp.setParaFont(Fonts.ORBITRON_12);
            tp.addSectionHeading("Legend", Alignment.MID, opad);
            tp.setParaFontDefault();

            final int legendIconSize = 26;

            final int y = (int)tp.getHeightSoFar() + opad + pad;

            legendRowCreator(0, tp, y, legendIconSize, m_market); 
        }

        ComponentFactory.addTooltip(tp, 0f, false, m_parent);
        tp.getPosition().inTL(-tp.getPosition().getWidth() - opad, 0);

        m_tooltip = tp;
        return tp;
    }
    @Override
    public Optional<UIPanelAPI> getCodexParent() {
        return Optional.ofNullable(m_parent);
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
    public static void legendRowCreator(int mode, TooltipMakerAPI tooltip, int y, int iconSize,
        MarketAPI market) {

        String iconPath;
        String desc;

        if (mode == 0) {
            desc = "Proportion of stockpiles compared to the desired amount.";
            legendRowHelper(tooltip, y, TooltipUtils.STOCKPILES_FULL_PATH, desc, iconSize, false, null);
            
            y += iconSize + pad;

            desc = "Excess local production that is exported.";
            legendRowHelper(tooltip, y, EXPORTS_ICON_PATH, desc, iconSize, false, null);
            
            y += iconSize + pad;
    
            iconPath = "";
            desc = "Smuggled or produced by an illegal enterprise. No income from exports.";
            legendRowHelper(tooltip, y, iconPath, desc, iconSize, true, null);
            
            y += iconSize + pad;
        }

        iconPath = "";
        desc = "Local production that could not be exported.";
        legendRowHelper(tooltip, y, iconPath, desc, iconSize, false, UiUtils.COLOR_NOT_EXPORTED);
        
        y += iconSize + pad;

        desc = "Local production that was exported.";
        legendRowHelper(tooltip, y, iconPath, desc, iconSize, false, UiUtils.COLOR_EXPORT);
        
        y += iconSize + pad;

        desc = "Production used for local demand.";
        legendRowHelper(tooltip, y, iconPath, desc, iconSize, false, UiUtils.COLOR_LOCAL_PROD);
        
        y += iconSize + pad;

        desc = "Goods that were imported in-faction.";
        legendRowHelper(tooltip, y, iconPath, desc, iconSize, false, UiUtils.COLOR_FACTION_IMPORT);
        
        y += iconSize + pad;

        desc = "Imported or available through one-time trade or events.";
        legendRowHelper(tooltip, y, iconPath, desc, iconSize, false, UiUtils.COLOR_IMPORT);
        
        y += iconSize + pad;

        desc = "Goods that must be imported regardless of local stockpiles or demand.";
        legendRowHelper(tooltip, y, iconPath, desc, iconSize, false, UiUtils.COLOR_IMPORT_EXCLUSIVE);
        
        y += iconSize + pad;

        desc = "Excess imports beyond current demand stockpiled for future use.";
        legendRowHelper(tooltip, y, iconPath, desc, iconSize, false, UiUtils.COLOR_OVER_IMPORT);
        
        y += iconSize + pad;

        desc = "Deficit not covered by production or imports.";
        legendRowHelper(tooltip, y, iconPath, desc, iconSize, false, UiUtils.COLOR_DEFICIT);
    
        tooltip.setHeightSoFar(y + opad*2);
    }

    private static void legendRowHelper(TooltipMakerAPI tooltip, int y, String iconPath, String desc,
        int lgdIconSize, boolean drawRedBorder, Color drawFilledIcon
    ) {
        final Base iconPanel = new Base(tooltip, lgdIconSize, lgdIconSize,
            iconPath, null, drawFilledIcon
        );
        if (drawRedBorder) {
            iconPanel.drawBorder = true;
            iconPanel.outlineColor = Color.RED;
            iconPanel.getPlugin().setOffsets(2, 2, -4, -4);
        }
            
        tooltip.addComponent(iconPanel.getPanel()).setSize(lgdIconSize, lgdIconSize).inTL(pad + opad/2f, y);

		final LabelAPI lbl = tooltip.addPara(desc, pad);
		final float textX = opad + lgdIconSize;
		lbl.getPosition().inTL(textX, y);
    }
}
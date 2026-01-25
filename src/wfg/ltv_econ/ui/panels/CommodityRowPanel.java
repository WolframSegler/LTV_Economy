package wfg.ltv_econ.ui.panels;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.econ.CommoditySpecAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.ui.Alignment;
import com.fs.starfarer.api.ui.Fonts;
import com.fs.starfarer.api.ui.LabelAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.ui.UIPanelAPI;

import wfg.ltv_econ.economy.CommodityDomain;
import wfg.ltv_econ.economy.engine.EconomyEngine;
import wfg.ltv_econ.economy.CommodityCell;
import wfg.ltv_econ.util.TooltipUtils;
import wfg.ltv_econ.util.UiUtils;
import wfg.native_ui.util.NumFormat;
import wfg.native_ui.util.NativeUiUtils;
import wfg.native_ui.util.NativeUiUtils.AnchorType;
import wfg.native_ui.ui.components.AudioFeedbackComp;
import wfg.native_ui.ui.components.HoverGlowComp;
import wfg.native_ui.ui.components.InteractionComp;
import wfg.native_ui.ui.components.NativeComponents;
import wfg.native_ui.ui.components.TooltipComp;
import wfg.native_ui.ui.components.UIContextComp;
import wfg.native_ui.ui.components.HoverGlowComp.GlowType;
import wfg.native_ui.ui.core.UIElementFlags.HasAudioFeedback;
import wfg.native_ui.ui.core.UIElementFlags.HasHoverGlow;
import wfg.native_ui.ui.core.UIElementFlags.HasInteraction;
import wfg.native_ui.ui.core.UIElementFlags.HasTooltip;
import wfg.native_ui.ui.panels.CustomPanel;
import wfg.native_ui.ui.panels.SpritePanel.Base;

import com.fs.starfarer.api.impl.campaign.ids.Strings;
import com.fs.starfarer.api.impl.codex.CodexDataV2;
import com.fs.starfarer.api.loading.Description.Type;

import java.awt.Color;
import static wfg.native_ui.util.UIConstants.*;

public class CommodityRowPanel extends CustomPanel<CommodityRowPanel> implements
    HasTooltip, HasHoverGlow, HasAudioFeedback, HasInteraction
{
    private static final String EXPORTS_ICON_PATH = Global.getSettings().getSpriteName(
        "commodity_markers", "exports");

    private static final int iconSize = 26;

    public final TooltipComp tooltip = comp().get(NativeComponents.TOOLTIP);
    public final HoverGlowComp glow = comp().get(NativeComponents.HOVER_GLOW);
    public final UIContextComp context = comp().get(NativeComponents.UI_CONTEXT);
    public final AudioFeedbackComp audio = comp().get(NativeComponents.AUDIO_FEEDBACK);
    public final InteractionComp<CommodityRowPanel> interaction = comp().get(NativeComponents.INTERACTION);

    public final CommoditySpecAPI m_com;

    private final CommodityCell m_cell;
    private final MarketAPI m_market;

    public CommodityRowPanel(UIPanelAPI parent, MarketAPI market, String comID,
        int width, int height, boolean ignoreUIContext
    ) {
        super(parent, width, height);

        m_cell = EconomyEngine.getInstance().getComCell(comID, market.getId());
        m_com = Global.getSettings().getCommoditySpec(comID);
        m_market = market;

        glow.color = base;
        glow.type = GlowType.UNDERLAY;

        tooltip.width = 500f;
        tooltip.parent = parent;
        tooltip.expandable = true;
        tooltip.builder = (tp, expanded) -> {
            final EconomyEngine engine = EconomyEngine.getInstance();
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
            if (!expanded) {

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

            } else {
            tp.setParaFont(Fonts.ORBITRON_12);
            tp.addSectionHeading("Legend", Alignment.MID, opad);
            tp.setParaFontDefault();

            final int legendIconSize = 26;

            final int y = (int)tp.getHeightSoFar() + pad;

            legendRowCreator(0, tp, y, legendIconSize); 
            tp.addSpacer(pad);
            }
        };
        tooltip.positioner = (tp, expanded) -> {
            NativeUiUtils.anchorPanel(tp, m_parent, AnchorType.LeftTop, opad);
        };
        tooltip.codexID = CodexDataV2.getCommodityEntryId(comID);
        tooltip.expandTxt = "%s show legend";
        tooltip.unexpandTxt = "%s hide";

        context.ignore = ignoreUIContext;

        createPanel();
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

        final UIPanelAPI infoBar = UiUtils.CommodityInfoBar(iconSize, 85, m_cell);
        add(infoBar).inBL(pad*4 + rowHeight + textWidth + iconSize, (rowHeight - iconSize) / 2f);

        if (m_cell.globalExports > 0) {
            final Base iconPanel = new Base(m_panel, rowHeight - 4, rowHeight - 4,
                EXPORTS_ICON_PATH, null, null);
            iconPanel.offset.setOffset(-1, -1, 2, 2);

            add(iconPanel).inRMid(pad);
        }
    }

    /**
     * Renders the legend icons and their descriptions in the given tooltip at the specified starting y-position.
     * 
     * <br></br> MODE_0: shows everything.
     * <br></br> MODE_1: shows only the CommodityInfoBar relevant info.
     */
    public static void legendRowCreator(int mode, TooltipMakerAPI tp, int y, int iconSize) {

        String iconPath;
        String desc;

        if (mode == 0) {
            desc = "Proportion of stockpiles compared to the desired amount.";
            legendRowHelper(tp, y, TooltipUtils.STOCKPILES_FULL_PATH, desc, iconSize, false, null);
            
            y += iconSize + pad;

            desc = "Excess local production that is exported.";
            legendRowHelper(tp, y, EXPORTS_ICON_PATH, desc, iconSize, false, null);
            
            y += iconSize + pad;
    
            iconPath = "";
            desc = "Smuggled or produced by an illegal enterprise. No income from exports.";
            legendRowHelper(tp, y, iconPath, desc, iconSize, true, null);
            
            y += iconSize + pad;
        }

        iconPath = "";
        desc = "Local production that could not be exported.";
        legendRowHelper(tp, y, iconPath, desc, iconSize, false, UiUtils.COLOR_NOT_EXPORTED);
        
        y += iconSize + pad;

        desc = "Local production that was exported.";
        legendRowHelper(tp, y, iconPath, desc, iconSize, false, UiUtils.COLOR_EXPORT);
        
        y += iconSize + pad;

        desc = "Production used for local demand.";
        legendRowHelper(tp, y, iconPath, desc, iconSize, false, UiUtils.COLOR_LOCAL_PROD);
        
        y += iconSize + pad;

        desc = "Goods that were imported in-faction.";
        legendRowHelper(tp, y, iconPath, desc, iconSize, false, UiUtils.COLOR_FACTION_IMPORT);
        
        y += iconSize + pad;

        desc = "Imported or available through one-time trade or events.";
        legendRowHelper(tp, y, iconPath, desc, iconSize, false, UiUtils.COLOR_IMPORT);
        
        y += iconSize + pad;

        desc = "Goods that must be imported regardless of local stockpiles or demand.";
        legendRowHelper(tp, y, iconPath, desc, iconSize, false, UiUtils.COLOR_IMPORT_EXCLUSIVE);
        
        y += iconSize + pad;

        desc = "Excess imports beyond current demand stockpiled for future use.";
        legendRowHelper(tp, y, iconPath, desc, iconSize, false, UiUtils.COLOR_OVER_IMPORT);
        
        y += iconSize + pad;

        desc = "Deficit not covered by production or imports.";
        legendRowHelper(tp, y, iconPath, desc, iconSize, false, UiUtils.COLOR_DEFICIT);
    
        tp.setHeightSoFar(y + opad*2);
    }

    private static void legendRowHelper(TooltipMakerAPI tp, int y, String iconPath, String desc,
        int lgdIconSize, boolean drawRedBorder, Color fillColor
    ) {
        final Base iconPanel = new Base(tp, lgdIconSize, lgdIconSize,
            iconPath, null, fillColor
        );
        if (drawRedBorder) {
            iconPanel.outline.enabled = true;
            iconPanel.outline.color = Color.RED;
            iconPanel.offset.setOffset(2, 2, -4, -4);
        }
            
        tp.addCustom(iconPanel.getPanel(), 0f).getPosition().inTL(pad, y);

		final LabelAPI lbl = tp.addPara(desc, 0f);
		final float textX = opad + lgdIconSize;
		lbl.getPosition().inTL(textX, y + ((lgdIconSize - lbl.computeTextHeight(desc)) / 2f));
    }
}
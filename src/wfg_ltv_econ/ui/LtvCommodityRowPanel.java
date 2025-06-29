package wfg_ltv_econ.ui;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.econ.CommodityMarketDataAPI;
import com.fs.starfarer.api.campaign.econ.CommodityOnMarketAPI;
import com.fs.starfarer.api.campaign.econ.CommoditySourceType;
import com.fs.starfarer.api.campaign.econ.Industry;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.campaign.econ.MarketShareDataAPI;
import com.fs.starfarer.api.combat.MutableStat;
import com.fs.starfarer.api.combat.MutableStat.StatMod;
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
import wfg_ltv_econ.plugins.infobarPlugin;
import wfg_ltv_econ.plugins.LtvCustomPanelPlugin.GlowType;
import wfg_ltv_econ.util.CommodityStats;
import wfg_ltv_econ.util.NumFormat;
import wfg_ltv_econ.util.UiUtils;
import wfg_ltv_econ.util.TooltipUtils;

import java.awt.Color;
import java.util.HashMap;
import java.util.Map;

public class LtvCommodityRowPanel extends LtvCustomPanel implements LtvCustomPanel.TooltipProvider {

    private final CommodityOnMarketAPI m_com;
    private final LtvCommodityPanel m_parentWrapper;

    private static final int iconSize = 24;
    private static final String notExpandedCodexF1 = "F1 show legend";
    private static final String ExpandedCodexF1 = "F1 go back";
    private static final String codexF2 = "F2 open Codex";
    private TooltipMakerAPI codexTooltip;

    public boolean isExpanded = false;

    public boolean m_canViewPrices;

    public LtvCommodityRowPanel(UIPanelAPI root, UIPanelAPI parent, MarketAPI market, CommodityOnMarketAPI com,
        LtvCommodityPanel parentWrapper, int width, int height, boolean childrenIgnoreUIState) {
        super(root, parent, width, height, new LtvCommodityRowPanelPlugin(), market);
        m_com = com;
        m_parentWrapper = parentWrapper;

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
        plugin.init(this, GlowType.OVERLAY, true, false, false);
        plugin.setDisplayPrices(m_canViewPrices);
        plugin.setSoundEnabled(true);
    }

    public void createPanel() {
        final int pad = 3;
        final int opad = 10;
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
        handleInfoBar(tooltip, iconSize);
        tooltip.getPrev().getPosition().inBL(labelWidth + iconSize + opad/2, (rowHeight - iconSize) / 2);

        // Source Icon
        CommodityMarketDataAPI commodityData = m_com.getCommodityMarketData();
        getPanel().addComponent(
            getSourceIcon(baseColor, commodityData, iconSize, m_panel).getPanel())
            .setSize(rowHeight, rowHeight).inBL(0, 0);

        if (commodityData.getExportIncome(m_com) > 0) {
            String iconPath = Global.getSettings().getSpriteName("commodity_markers", "exports");
            LtvIconPanel iconPanel = new LtvIconPanel(getRoot(), m_panel, m_market, iconSize, iconSize,
                    new LtvSpritePanelPlugin(), iconPath, null, false);
            iconPanel.getPlugin().setGlowType(GlowType.NONE);
            iconPanel.getPlugin().setTooltipActive(false);

            getPanel().addComponent(iconPanel.getPanel()).setSize(rowHeight, rowHeight).inBR(pad, 0.0F);
        }

        getPanel().addUIElement(tooltip).inBL(pad + iconSize, 0);
    }

    private LtvIconPanel getSourceIcon(Color color, CommodityMarketDataAPI commodityData, int iconSize,
        UIPanelAPI parent) {
        MarketShareDataAPI marketData = commodityData.getMarketShareData(m_market);
        boolean isSourceIllegal = marketData.isSourceIsIllegal();

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

        LtvIconPanel iconPanel = new LtvIconPanel(getRoot(), parent, m_market, iconSize, iconSize,
                    new LtvSpritePanelPlugin(), iconPath, baseColor, isSourceIllegal);
        iconPanel.getPlugin().setGlowType(GlowType.NONE);
        iconPanel.getPlugin().setTooltipActive(false);
        return iconPanel;
    }

    private void handleInfoBar(TooltipMakerAPI tooltip, int barHeight) {
        CommodityStats comStats = new CommodityStats(m_com, m_market);

        float localProducedRatio = (float)comStats.demandMetWithLocal / (float)comStats.totalActivity;
        float inFactionImportRatio = (float)comStats.inFactionImports / (float)comStats.totalActivity;
        float externalImportRatio = (float)comStats.externalImports / (float)comStats.totalActivity;
        float exportedRatio = (float)comStats.totalExports / (float)comStats.totalActivity;
        float notExportedRatio = (float)comStats.canNotExport / (float)comStats.totalActivity;
        float deficitRatio = (float)comStats.localDeficit / (float)comStats.totalActivity;

        final HashMap<Color, Float> barMap = new HashMap<Color, Float>();
        barMap.put(new Color(170, 46,  46), deficitRatio);
        barMap.put(new Color(225, 170, 76), externalImportRatio);
        barMap.put(new Color(210, 210, 76), inFactionImportRatio);
        barMap.put(new Color(122, 200, 122), localProducedRatio);
        barMap.put(new Color(63,  175, 63), exportedRatio);
        barMap.put(new Color(100, 140, 180), notExportedRatio);

        for (Map.Entry<Color, Float> barPiece : barMap.entrySet()) {
            if (barPiece.getValue() < 0) {
                barPiece.setValue(0f);
            }
        }

        CustomPanelAPI infoBar = Global.getSettings().createCustom(85, barHeight, new infobarPlugin());
        ((infobarPlugin)infoBar.getPlugin()).init(infoBar, true, barMap, m_faction);

        tooltip.addCustom(infoBar, 3);
    }

    @Override
    public TooltipMakerAPI createTooltip() {

        final Color highlight = Misc.getHighlightColor();
        final Color gray = new Color(100, 100, 100);
        final Color positive = Misc.getPositiveHighlightColor();
        final Color negative = Misc.getNegativeHighlightColor();
        final int pad = 3;
        final int opad = 10;

        TooltipMakerAPI tooltip = ((CustomPanelAPI)getParent()).createUIElement(500f, 0,false);
        CommodityStats comStats = new CommodityStats(m_com, m_market);

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

        // Production
        {
        tooltip.setParaFontDefault();
        tooltip.addPara("Available: %s", pad, highlight,
            NumFormat.engNotation(comStats.available));

        final int valueTxtWidth = 45 + pad;
        boolean firstPara = true;
		float y = tooltip.getHeightSoFar() + pad;

        for(Industry industry : m_com.getMarket().getIndustries()) {
            if(industry.getSupply(m_com.getId()).getQuantity().getModifiedInt() < 1) {
                continue;
            }
            
            MutableStat baseProd = industry.getSupply(m_com.getId()).getQuantity();
            MutableStat prodBonus = industry.getSupplyBonus();

            // Flat mods
            for (Map.Entry<String, MutableStat.StatMod> entry : baseProd.getFlatMods().entrySet()) {
                MutableStat.StatMod mod = entry.getValue();

                float value = mod.getValue();
                String desc = mod.getDesc();

                if (desc == null || value < 1) {
                    continue;
                }

                // draw text
                String valueTxt = "+" + NumFormat.engNotation((long)value);

                String industryDesc = industry.getNameForModifier();
                String text =  desc + " (" + industryDesc + ")";

				LabelAPI lbl = tooltip.addPara(valueTxt, pad, highlight, valueTxt);

				UIComponentAPI lblComp = tooltip.getPrev();
				float textH = lbl.computeTextHeight(valueTxt);
				float textX = (valueTxtWidth - lbl.computeTextWidth(valueTxt)) + pad;
                if (firstPara) {
                    firstPara = false;
                } else {
                    y += textH + pad;
                }

				lblComp.getPosition().inTL(textX, y);

                
                lbl = tooltip.addPara(text, pad);
                lblComp = tooltip.getPrev();
				textX = valueTxtWidth + opad; 
				lblComp.getPosition().inTL(textX, y);
            }
            // Flat bonuses
            for (Map.Entry<String, MutableStat.StatMod> entry : prodBonus.getFlatMods().entrySet()) {
                MutableStat.StatMod mod = entry.getValue();

                float value = mod.getValue();
                String desc = mod.getDesc();

                if (desc == null || value < 1) {
                    continue;
                }

                // draw text
                String valueTxt = "+" + NumFormat.engNotation((long)value);

                String industryDesc = industry.getNameForModifier();
                String text =  desc + " (" + industryDesc + ")";

				LabelAPI lbl = tooltip.addPara(valueTxt, pad, highlight, valueTxt);

				UIComponentAPI lblComp = tooltip.getPrev();
				float textH = lbl.computeTextHeight(valueTxt);
				float textX = (valueTxtWidth - lbl.computeTextWidth(valueTxt)) + pad; 
                if (firstPara) {
                    firstPara = false;
                } else {
                    y += textH + pad;
                }

				lblComp.getPosition().inTL(textX, y);

                
                lbl = tooltip.addPara(text, pad);
                lblComp = tooltip.getPrev();
				textX = valueTxtWidth + opad; 
				lblComp.getPosition().inTL(textX, y);
            }
            // Mult bonuses
            for (Map.Entry<String, MutableStat.StatMod> entry : prodBonus.getMultMods().entrySet()) {
                MutableStat.StatMod mod = entry.getValue();

                float value = mod.getValue();
                String desc = mod.getDesc();

                if (desc == null || value < 0) {
                    continue;
                }

                // draw text
                String valueTxt = Strings.X + value;

                String industryDesc = industry.getNameForModifier();
                String text =  desc + " (" + industryDesc + ")";

				LabelAPI lbl = tooltip.addPara(valueTxt, pad, highlight, valueTxt);

				UIComponentAPI lblComp = tooltip.getPrev();
				float textH = lbl.computeTextHeight(valueTxt);
				float textX = (valueTxtWidth - lbl.computeTextWidth(valueTxt)) + pad; 
                if (firstPara) {
                    firstPara = false;
                } else {
                    y += textH + pad;
                }

				lblComp.getPosition().inTL(textX, y);

                
                lbl = tooltip.addPara(text, pad);
                lblComp = tooltip.getPrev();
				textX = valueTxtWidth + opad; 
				lblComp.getPosition().inTL(textX, y);
            }
        }
        // Import mods
        HashMap<String, StatMod> imports = m_com.getAvailableStat().getFlatMods();

        for (Map.Entry<String, MutableStat.StatMod> entry : imports.entrySet()) {
            MutableStat.StatMod mod = entry.getValue();

            float value = mod.getValue();
            String desc = mod.getDesc();

            if (desc == null ||!desc.contains("faction")) {
                continue;
            }

            // draw text
            String valueTxt = "+" + NumFormat.engNotation((long)value);
            Color valueColor = highlight;
            if (value < 0) {
                valueTxt = valueTxt.replace("+", "");
                valueColor = negative;
            }
                
			LabelAPI lbl = tooltip.addPara(valueTxt, pad, valueColor, valueTxt);

			UIComponentAPI lblComp = tooltip.getPrev();
			float textH = lbl.computeTextHeight(valueTxt);
			float textX = (valueTxtWidth - lbl.computeTextWidth(valueTxt)) + pad; 
            if (firstPara) {
                firstPara = false;
            } else {
                y += textH + pad;
            }

			lblComp.getPosition().inTL(textX, y);
  
            lbl = tooltip.addPara(desc, pad);
            lblComp = tooltip.getPrev();
			textX = valueTxtWidth + opad; 
			lblComp.getPosition().inTL(textX, y);
        }

        if (comStats.externalImports > 0) {
            // draw text
            String valueTxt = "+" + NumFormat.engNotation(comStats.externalImports);
			LabelAPI lbl = tooltip.addPara(valueTxt, pad, highlight, valueTxt);

			UIComponentAPI lblComp = tooltip.getPrev();
			float textH = lbl.computeTextHeight(valueTxt);
			float textX = (valueTxtWidth - lbl.computeTextWidth(valueTxt)) + pad; 
            if (firstPara) {
                firstPara = false;
            } else {
                y += textH + pad;
            }
            
			lblComp.getPosition().inTL(textX, y);
            
            String desc = "Desired import volume";
            lbl = tooltip.addPara(desc, pad);
            lblComp = tooltip.getPrev();
			textX = valueTxtWidth + opad; 
			lblComp.getPosition().inTL(textX, y);
        }

        tooltip.setHeightSoFar(y);
        UiUtils.resetFlowLeft(tooltip, opad);
        }
        

        tooltip.addPara("All production sources contribute cumulatively to the commodity's availability. Imports and smuggling add to supply to help meet demand.", gray ,pad);

        
        // Demand
        {
        Color valueColor = highlight;
        if (comStats.available < comStats.localDemand) {
            valueColor = negative;
        }
        
        tooltip.addPara("Total demand: %s", opad, valueColor,
            NumFormat.engNotation(comStats.localDemand));

        final int valueTxtWidth = 45 + pad;
        boolean firstPara = true;
		float y = tooltip.getHeightSoFar() + pad;

        for(Industry industry : m_com.getMarket().getIndustries()) {
            if(industry.getDemand(m_com.getId()).getQuantity().getModifiedInt() < 1) {
                continue;
            }
            
            MutableStat baseProd = industry.getDemand(m_com.getId()).getQuantity();

            // Flat mods
            for (Map.Entry<String, MutableStat.StatMod> entry : baseProd.getFlatMods().entrySet()) {
                MutableStat.StatMod mod = entry.getValue();

                float value = mod.getValue();

                if (value < 1) {
                    continue;
                }

                // draw text
                String valueTxt = NumFormat.engNotation((long)value);

                String industryDesc = industry.getNameForModifier();
                String text =  "Needed by " + industryDesc;

				LabelAPI lbl = tooltip.addPara(valueTxt, pad, valueColor, valueTxt);

				UIComponentAPI lblComp = tooltip.getPrev();
				float textH = lbl.computeTextHeight(valueTxt);
				float textX = (valueTxtWidth - lbl.computeTextWidth(valueTxt)) + pad;
                if (firstPara) {
                    firstPara = false;
                    y += textH;
                } else {
                    y += textH + pad;
                }

				lblComp.getPosition().inTL(textX, y);

                
                lbl = tooltip.addPara(text, pad);
                lblComp = tooltip.getPrev();
				textX = valueTxtWidth + opad; 
				lblComp.getPosition().inTL(textX, y);
            }
        }
        tooltip.setHeightSoFar(y);
        UiUtils.resetFlowLeft(tooltip, opad);
        }

        // Divider
        tooltip.addSectionHeading("Exports", Alignment.MID, opad);

        // Export stats
        int exportIncome = m_com.getCommodityMarketData().getExportIncome(m_com);
        boolean isIllegal = m_market.isIllegal(m_com);
        String commodityName = m_com.getCommodity().getName();

        if (comStats.totalExports < 1) {
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
            comStats.globalExport + "",
            m_com.getCommodityMarketData().getExportMarketSharePercent(m_market) + "%"
        );
        } else {
            tooltip.addPara(
            m_market.getName() + " is profitably exporting %s units of " + commodityName + " and controls %s of the global market share. Exports bring in %s per month.",
            opad, highlight,
            comStats.globalExport + "",
            m_com.getCommodityMarketData().getExportMarketSharePercent(m_market) + "%",
            exportIncome + Strings.C
            );

            if (comStats.canNotExport > 0) {
                tooltip.addPara(
                "Exports are reduced by %s due to insufficient accessibility.",
                pad, negative, NumFormat.engNotation(comStats.canNotExport)
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

            final int lgdIconSize = iconSize + 4;

            int y = (int)tooltip.getHeightSoFar() + opad + pad;

            String iconPath = Global.getSettings().getSpriteName("commodity_markers", "production");
			String desc = "Demand met through local production.";
            legendRowHelper(tooltip, y, iconPath, desc, false, lgdIconSize);

            y += lgdIconSize + pad;

            iconPath = m_market.getFaction().getCrest();
			desc = "Demand met through in-faction imports.";
            legendRowHelper(tooltip, y, iconPath, desc, false, lgdIconSize);

            y += lgdIconSize + pad;

            iconPath = Global.getSettings().getSpriteName("commodity_markers", "imports");
			desc = "Demand met through imports from outside the imports.";
            legendRowHelper(tooltip, y, iconPath, desc, false, lgdIconSize);
            
             y += lgdIconSize + pad;

            iconPath = Global.getSettings().getSpriteName("commodity_markers", "exports");
			desc = "Excess local production that is exported.";
            legendRowHelper(tooltip, y, iconPath, desc, false, lgdIconSize);
            
             y += lgdIconSize + pad;

            iconPath = "";
			desc = "Smuggled or produced by an illegal enterprise. No income from exports.";
            legendRowHelper(tooltip, y, iconPath, desc, true, lgdIconSize);
            
             y += lgdIconSize + pad;

            final int codexW = 200; 
            
            TooltipUtils.createCustomCodex(tooltip, codexTooltip, this, ExpandedCodexF1, codexF2, codexW);  
        }

        ((CustomPanelAPI)getParent()).addUIElement(tooltip);
        tooltip.getPosition().inTL(-tooltip.getPosition().getWidth() - opad, 0);

        ((CustomPanelAPI)getParent()).addUIElement(codexTooltip);
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

    private void legendRowHelper(TooltipMakerAPI tooltip, int y, String iconPath, String desc,
        boolean drawRedBorder, int lgdIconSize) {
        final int pad = 3;
        final int opad = 10;

        LtvIconPanel iconPanel = new LtvIconPanel(getRoot(), m_panel, m_market, iconSize, iconSize,
                    new LtvSpritePanelPlugin(), iconPath, null, drawRedBorder);
        iconPanel.getPlugin().setGlowType(GlowType.NONE);
        iconPanel.getPlugin().setTooltipActive(false);

        tooltip.addComponent(iconPanel.getPanel()).setSize(lgdIconSize, lgdIconSize).inTL(pad + opad/2f, y);

        // Explanation Label
		LabelAPI lbl = tooltip.addPara(desc, pad);

		float textX = opad + lgdIconSize;
		lbl.getPosition().inTL(textX, y);
    }
}
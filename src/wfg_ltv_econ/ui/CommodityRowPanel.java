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
import com.fs.starfarer.settings.StarfarerSettings;
import com.fs.starfarer.api.impl.campaign.econ.CommodityIconCounts;
import com.fs.starfarer.api.impl.campaign.ids.Strings;
import com.fs.starfarer.api.loading.Description.Type;

import wfg_ltv_econ.plugins.CommodityRowIconPlugin;
import wfg_ltv_econ.plugins.LtvCustomPanelPlugin;
import wfg_ltv_econ.plugins.infobarPlugin;
import wfg_ltv_econ.util.LtvNumFormat;
import wfg_ltv_econ.util.LtvUiUtils;
import wfg_ltv_econ.util.ReflectionUtils;

import java.awt.Color;
import java.util.HashMap;
import java.util.Map;

public class CommodityRowPanel extends LtvCustomPanel {

    private final CommodityOnMarketAPI m_com;
    public boolean m_canViewPrices;

    public CommodityRowPanel(CommodityOnMarketAPI com, UIPanelAPI parent, int width, int height, MarketAPI market) {
        super(parent, width, height, new LtvCustomPanelPlugin(), market);
        m_com = com;

        boolean viewAnywhere = Global.getSettings().getBoolean("allowPriceViewAtAnyColony");
        m_canViewPrices = Global.getSector().getIntelManager().isPlayerInRangeOfCommRelay() || viewAnywhere;

        initializePanel(hasPlugin);
        createPanel();
    }

    public CommodityOnMarketAPI getCommodity() {
        return m_com;
    }

    public void initializePanel(boolean hasPlugin) {
        ((LtvCustomPanelPlugin) m_panel.getPlugin()).init(this, true, true, true, false, false);
    }

    public void createPanel() {
        final int pad = 3;
        final int opad = 10;
        final int iconSize = 24;
        final int textWidth = 60;
        final Color baseColor = getFaction().getBaseUIColor();
        final TooltipMakerAPI tooltip = m_panel.createUIElement(getPanelPos().getWidth(), getPanelPos().getHeight(), false);
        final float rowHeight = getPanelPos().getHeight();

        // Amount label
        tooltip.setParaSmallInsignia();
        LabelAPI amountTxt = tooltip.addPara(LtvNumFormat.formatWithMaxDigits(m_com.getAvailable()) + Strings.X, pad);
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
        getPanel().addComponent(getSourceIcon(baseColor, commodityData, iconSize)).setSize(rowHeight, rowHeight).inBL(0,
                0);

        if (commodityData.getExportIncome(m_com) > 0) {
            String iconPath = (String) ReflectionUtils.invoke(StarfarerSettings.class, "new", "commodity_markers", "exports");
            CustomPanelAPI iconPanel = m_panel.createCustomPanel(iconSize, iconSize,
                    new CommodityRowIconPlugin(iconPath, baseColor, false));
            ((CommodityRowIconPlugin) iconPanel.getPlugin()).init(iconPanel);
            getPanel().addComponent(iconPanel).setSize(rowHeight, rowHeight).inBR(pad, 0.0F);
        }

        getPanel().addUIElement(tooltip).inBL(pad + iconSize, 0);
    }

    private CustomPanelAPI getSourceIcon(Color color, CommodityMarketDataAPI commodityData, int iconSize) {
        MarketShareDataAPI marketData = commodityData.getMarketShareData(m_market);
        boolean isSourceIllegal = marketData.isSourceIsIllegal();

        CommoditySourceType source = m_com.getCommodityMarketData().getMarketShareData(m_market).getSource();
        String iconPath = (String) ReflectionUtils.invoke(StarfarerSettings.class, "new", "commodity_markers", "imports");
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
                iconPath = (String) ReflectionUtils.invoke(StarfarerSettings.class, "new", "commodity_markers", "production");
                break;
            default:
        }

        CustomPanelAPI iconPanel = m_panel.createCustomPanel(iconSize, iconSize,
                new CommodityRowIconPlugin(iconPath, baseColor, isSourceIllegal));
        ((CommodityRowIconPlugin) iconPanel.getPlugin()).init(iconPanel);
        return iconPanel;
    }

    private void handleInfoBar(TooltipMakerAPI tooltip, int barHeight) {
        float available = (float) m_com.getAvailableStat().getModifiedValue();
        float totalDemand = m_com.getMaxDemand();
        float totalSupply = m_com.getMaxSupply();

        float totalTarget = Math.max(Math.max(totalDemand, totalSupply), available);

        CommodityIconCounts iconsCount = new CommodityIconCounts(m_com);
        final int demandMetLocal = iconsCount.demandMetWithLocal;
        int demandMetInFactionImports = (int) Math.min(iconsCount.inFactionOnlyExport, totalDemand);

        final int imports = iconsCount.imports;
        final int extra = iconsCount.extra;
        final int deficit = iconsCount.deficit;

        float localProducedRatio = demandMetLocal / totalTarget;
        float inFactionImportRatio = demandMetInFactionImports / totalTarget;
        float externalImportRatio = (imports - demandMetInFactionImports) / totalTarget;
        float exportedRatio = extra / totalTarget;
        float deficitRatio = deficit / totalTarget;

        final HashMap<Color, Float> barMap = new HashMap<Color, Float>();
        barMap.put(new Color(170, 46, 46), deficitRatio);
        barMap.put(new	Color(225, 170, 76), externalImportRatio);
        barMap.put(new	Color(210, 210, 76), inFactionImportRatio);
        barMap.put(new Color(122, 200, 122), localProducedRatio);
        barMap.put(new Color(63, 175, 63), exportedRatio);

        CustomPanelAPI infoBar = Global.getSettings().createCustom(80, barHeight, new infobarPlugin());
        ((infobarPlugin)infoBar.getPlugin()).init(infoBar, true, barMap, m_faction);

        tooltip.addCustom(infoBar, 3);
    }

    public void createTooltip(TooltipMakerAPI tooltip) {
        final Color highlight = Misc.getHighlightColor();
        final Color gray = new Color(100, 100, 100);
        final Color positive = Misc.getPositiveHighlightColor();
        final Color negative = Misc.getNegativeHighlightColor();
        final int pad = 3;
        final int opad = 10;

        tooltip.createRect(BgColor, tooltip.getPosition().getWidth());

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

        tooltip.setParaFont(Fonts.ORBITRON_12);
        tooltip.addSectionHeading("Production, imports and demand", Alignment.MID, opad);

        // Production
        {
        tooltip.setParaFontDefault();
        tooltip.addPara("Available: %s", pad, highlight,
            LtvNumFormat.formatWithMaxDigits(m_com.getMaxSupply()));

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
                String valueTxt = "+" + LtvNumFormat.formatWithMaxDigits((long)value);

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
                String valueTxt = "+" + LtvNumFormat.formatWithMaxDigits((long)value);

                String industryDesc = industry.getNameForModifier();
                String text =  desc + " (" + industryDesc + ")";

				LabelAPI lbl = tooltip.addPara(valueTxt, pad, highlight, valueTxt);

				UIComponentAPI lblComp = tooltip.getPrev();
				float textH = lbl.computeTextHeight(valueTxt);
				float textX = (valueTxtWidth - lbl.computeTextWidth(valueTxt)) + pad; 
                y += textH + pad;

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
                y += textH + pad;

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
            String valueTxt = "+" + LtvNumFormat.formatWithMaxDigits((long)value);
            Color valueColor = highlight;
            if (value < 0) {
                valueTxt = valueTxt.replace("+", "");
                valueColor = negative;
            }
                
			LabelAPI lbl = tooltip.addPara(valueTxt, pad, valueColor, valueTxt);

			UIComponentAPI lblComp = tooltip.getPrev();
			float textH = lbl.computeTextHeight(valueTxt);
			float textX = (valueTxtWidth - lbl.computeTextWidth(valueTxt)) + pad; 
            y += textH + pad;

			lblComp.getPosition().inTL(textX, y);
  
            lbl = tooltip.addPara(desc, pad);
            lblComp = tooltip.getPrev();
			textX = valueTxtWidth + opad; 
			lblComp.getPosition().inTL(textX, y);
        }

        tooltip.setHeightSoFar(y);
        LtvUiUtils.resetFlowLeft(tooltip, opad);
        }
        

        tooltip.addPara("All production sources contribute cumulatively to the commodity's availability. Imports and smuggling add to supply to help meet demand.", gray ,pad);

        
        // Demand
        {
        tooltip.addPara("Total demand: %s", opad, negative,
            LtvNumFormat.formatWithMaxDigits((long)m_com.getMaxDemand()));

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
                String valueTxt = LtvNumFormat.formatWithMaxDigits((long)value);

                String industryDesc = industry.getNameForModifier();
                String text =  "Needed by " + industryDesc;

				LabelAPI lbl = tooltip.addPara(valueTxt, pad, negative, valueTxt);

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
        LtvUiUtils.resetFlowLeft(tooltip, opad);
        }

        // Divider
        tooltip.addSectionHeading("Exports", Alignment.MID, opad);

        // Export stats
        int exportIncome = m_com.getCommodityMarketData().getExportIncome(m_com);
        int exportAmount = m_com.getCommodityMarketData().getMaxExportGlobal();
        boolean isIllegal = m_market.isIllegal(m_com);
        String commodityName = m_com.getCommodity().getName();

        if (exportAmount < 1) {
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
            m_market.getName() + " is profitably exporting %s units of " + commodityName + " and controls %s of the global market share. Exports of" + commodityName + "bring in no income.",
            opad, highlight,
            m_com.getCommodityMarketData().getMaxExportGlobal() + "",
            m_com.getCommodityMarketData().getExportMarketSharePercent(m_market) + "%"
        );
        } else {
            tooltip.addPara(
            m_market.getName() + " is profitably exporting %s units of " + commodityName + " and controls %s of the global market share. Exports bring in %s per month.",
            opad, highlight,
            m_com.getCommodityMarketData().getMaxExportGlobal() + "",
            m_com.getCommodityMarketData().getExportMarketSharePercent(m_market) + "%",
            exportIncome + Strings.C
        );

            int available = new CommodityIconCounts(m_com).extra;
            float accessibility = m_market.getAccessibilityMod().getFlatBonus();
            int exportable = Math.round(available * accessibility);
            int penalty = available - exportable;

            if (penalty > 0) {
                tooltip.addPara(
                "Exports are reduced by %s due to insufficient accessibility.",
                pad, negative, Integer.toString(penalty)
            );
            }
        }

        // Bottom tip
        tooltip.addPara(
        "Increasing production and colony accessibility will both increase the export market share and income.", gray, opad);

        tooltip.addSpacer(opad*1.5f);
    }

}
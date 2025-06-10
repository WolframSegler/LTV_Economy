package wfg_ltv_econ.util;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.econ.CommodityOnMarketAPI;
import com.fs.starfarer.api.campaign.econ.CommoditySourceType;
import com.fs.starfarer.api.ui.Alignment;
import com.fs.starfarer.api.ui.CustomPanelAPI;
import com.fs.starfarer.api.ui.Fonts;
import com.fs.starfarer.api.ui.IconRenderMode;
import com.fs.starfarer.api.ui.LabelAPI;
import com.fs.starfarer.api.ui.PositionAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.ui.UIComponentAPI;
import com.fs.starfarer.api.ui.UIPanelAPI;
import com.fs.starfarer.campaign.ui.marketinfo.i;
import com.fs.starfarer.settings.StarfarerSettings;
import com.fs.starfarer.api.impl.campaign.econ.CommodityIconCounts;
import com.fs.starfarer.api.impl.campaign.ids.Strings;
import com.fs.starfarer.campaign.econ.reach.CommodityMarketData;
import com.fs.starfarer.campaign.econ.reach.MarketShareData;

import java.awt.Color;

public class CommodityRow{

    private CustomPanelAPI m_panel;
    private CommodityOnMarketAPI m_com;
    private i m_sourceIcon;
    final private UIPanelAPI m_parent;

    public CommodityRow(CommodityOnMarketAPI com, UIPanelAPI parent, int width, int height) {
        this.m_com = com;
        this.m_parent = parent;
        m_panel = Global.getSettings().createCustom(width, height, null);
        this.m_parent.addComponent(m_panel);
    }

    public CustomPanelAPI getPanel() {
        return m_panel;
    }
    public PositionAPI getPanelPos() {
        return m_panel.getPosition();
    }

    public void createRow(float var1, float var2) {
        final float pad = 3f;
        final float opad = 10f;
        final int iconSize = 32;
        final Color baseColor = m_com.getMarket().getFaction().getBaseUIColor();
        final TooltipMakerAPI tooltip = m_panel.createUIElement(getPanelPos().getWidth(), getPanelPos().getHeight(), false);
        final float rowHeight = getPanelPos().getHeight();
        
        tooltip.setParaFont(Fonts.ORBITRON_12);
        LabelAPI amountLabel = tooltip.addPara(m_com.getAvailable() + Strings.X, pad);
        amountLabel.setAlignment(Alignment.MID);
        amountLabel.setColor(baseColor);
        amountLabel.getPosition().setSize(iconSize, amountLabel.computeTextHeight(amountLabel.getText()));

        final float labelWidth = amountLabel.getPosition().getWidth() + pad;
        final UIComponentAPI lblComp = tooltip.getPrev();
        lblComp.getPosition().inLMid(rowHeight + pad);

        handleIconGroup(tooltip);
        tooltip.getPrev().getPosition().inLMid(rowHeight + pad + labelWidth);

        CommodityMarketData commodityData = (CommodityMarketData) m_com.getCommodityMarketData();
        getPanel().addComponent(getSourceIcon(tooltip, m_com, baseColor, commodityData)).setSize(rowHeight, rowHeight).inBL(0, 0);

        if (commodityData.getExportIncome(m_com) > 0) {
            m_sourceIcon = new i(
                    (String) ReflectionUtils.invoke(StarfarerSettings.class, "new", "commodity_markers", "exports"),
                    baseColor, false);
            getPanel().addComponent(m_sourceIcon).setSize(rowHeight, rowHeight).inBR(pad, 0.0F);
        }

        getPanel().addUIElement(tooltip);
    }

    private i getSourceIcon(TooltipMakerAPI tooltip, CommodityOnMarketAPI com, Color baseColor, CommodityMarketData commodityData) {
        MarketShareData marketData = commodityData.getMarketShareData(m_com.getMarket());
        boolean isSourceIllegal = marketData.isSourceIsIllegal();

        CommoditySourceType source = com.getCommodityMarketData().getMarketShareData(com.getMarket()).getSource();
        String iconPath = (String) ReflectionUtils.invoke(StarfarerSettings.class, "new", "commodity_markers",
                "imports");

        

        switch (source) {
            case GLOBAL:
                return new i(iconPath, baseColor, isSourceIllegal);
            case IN_FACTION:
                iconPath = com.getMarket().getFaction().getCrest();
                return new i(iconPath, null, isSourceIllegal);
            case LOCAL:
            case NONE:
                iconPath = (String) ReflectionUtils.invoke(StarfarerSettings.class, "new", "commodity_markers",
                        "production");
                return new i(iconPath, baseColor, isSourceIllegal);
            default:
                return new i(iconPath, baseColor, isSourceIllegal);
        }
    }

    private void handleIconGroup(TooltipMakerAPI tooltip) {
        float available = (float)m_com.getAvailableStat().getModifiedValue();
        float totalDemand = m_com.getMaxDemand();
        float totalSupply = m_com.getMaxSupply();

        float totalTarget = Math.max(Math.max(totalDemand, totalSupply), available);
        final int totalIcons = 6;

        CommodityIconCounts iconCount = new CommodityIconCounts(m_com);
        final int demandMetLocal = iconCount.demandMetWithLocal;
        int demandMetInFactionImports = (int)Math.min(iconCount.inFactionOnlyExport, totalDemand);

        final int nonDemandExport = iconCount.nonDemandExport;
        final int imports = iconCount.imports;
        final int extra = iconCount.extra;
        final int deficit = iconCount.deficit;

        float localProducedRatio = demandMetLocal/totalTarget;
        float inFactionImportRatio = demandMetInFactionImports/totalTarget;
        float externalImportRatio = (imports-demandMetInFactionImports)/totalTarget;
        float exportedRatio = extra/totalTarget;
        float deficitRatio = deficit/totalTarget;

        int iconsForLocal = Math.round(totalIcons * localProducedRatio);
        int iconsForInFactionImport = Math.round(totalIcons * inFactionImportRatio);
        int iconsForOutFactionImport = Math.round(totalIcons * externalImportRatio);
        int iconsForExport = Math.round(totalIcons * exportedRatio);
        int iconsForDeficit = Math.round(totalIcons * deficitRatio);

        tooltip.beginIconGroup();
        addIconsToGroup(tooltip, m_com, iconsForExport, IconRenderMode.GREEN);
        addIconsToGroup(tooltip, m_com, iconsForLocal, IconRenderMode.OUTLINE_GREEN);
        addIconsToGroup(tooltip, m_com, iconsForInFactionImport, IconRenderMode.NORMAL);
        addIconsToGroup(tooltip, m_com, iconsForOutFactionImport, IconRenderMode.OUTLINE_CUSTOM);
        addIconsToGroup(tooltip, m_com, iconsForDeficit, IconRenderMode.DIM_RED);

        tooltip.addIconGroup(3);
    }

    private int addIconsToGroup(TooltipMakerAPI tooltip, CommodityOnMarketAPI com, int count, IconRenderMode mode) {
        for (int i = 0; i < count; i++) {
            tooltip.addIcons(com, i, mode);
        }
        return count;
    }
}
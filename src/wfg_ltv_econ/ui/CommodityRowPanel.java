package wfg_ltv_econ.ui;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.econ.CommodityMarketDataAPI;
import com.fs.starfarer.api.campaign.econ.CommodityOnMarketAPI;
import com.fs.starfarer.api.campaign.econ.CommoditySourceType;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.campaign.econ.MarketShareDataAPI;
import com.fs.starfarer.api.ui.CustomPanelAPI;
import com.fs.starfarer.api.ui.IconRenderMode;
import com.fs.starfarer.api.ui.LabelAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.ui.UIComponentAPI;
import com.fs.starfarer.api.ui.UIPanelAPI;
import com.fs.starfarer.settings.StarfarerSettings;
import com.fs.starfarer.api.impl.campaign.econ.CommodityIconCounts;
import com.fs.starfarer.api.impl.campaign.ids.Strings;

import wfg_ltv_econ.plugins.CommodityRowIconPlugin;
import wfg_ltv_econ.plugins.LtvCustomPanelPlugin;
import wfg_ltv_econ.util.LtvNumFormat;
import wfg_ltv_econ.util.ReflectionUtils;

import java.awt.Color;
import java.util.HashMap;
import java.util.Map;

public class CommodityRowPanel extends LtvCustomPanel{

    private final CommodityOnMarketAPI m_com;
    public boolean m_canViewPrices;

    public CommodityRowPanel(CommodityOnMarketAPI com, UIPanelAPI parent, int width, int height, MarketAPI market) {
        super(parent, width, height, new LtvCustomPanelPlugin(), market);
        this.m_com = com;

        boolean viewAnywhere = Global.getSettings().getBoolean("allowPriceViewAtAnyColony");
        this.m_canViewPrices = Global.getSector().getIntelManager().isPlayerInRangeOfCommRelay() || viewAnywhere;

        createPanel();
    }

    public CommodityOnMarketAPI getCommodity() {
        return m_com;
    }

    public void initializePanel(boolean hasPlugin) {
        ((LtvCustomPanelPlugin)m_panel.getPlugin()).init(this, true, true, true, m_com, false, false);
    }

    public void createPanel() {
        final int pad = 3;
        final int iconSize = 24;
        final int textWidth = 55;
        final Color baseColor = m_com.getMarket().getFaction().getBaseUIColor();
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
        lblComp.getPosition().inBL(pad, (rowHeight - textHeight)/2);

        // Icons
        handleIconGroup(tooltip, iconSize);
        tooltip.getPrev().getPosition().inBL(labelWidth, (rowHeight - iconSize)/2);

        // Source Icon
        CommodityMarketDataAPI commodityData = m_com.getCommodityMarketData();
        getPanel().addComponent(getSourceIcon(baseColor, commodityData, iconSize)).setSize(rowHeight, rowHeight).inBL(0, 0);

        if (commodityData.getExportIncome(m_com) > 0) {
            String iconPath = (String) ReflectionUtils.invoke(StarfarerSettings.class, "new", "commodity_markers", "exports");
            CustomPanelAPI iconPanel = m_panel.createCustomPanel(iconSize, iconSize, new CommodityRowIconPlugin(iconPath, baseColor, false));
            ((CommodityRowIconPlugin)iconPanel.getPlugin()).init(iconPanel);
            getPanel().addComponent(iconPanel).setSize(rowHeight, rowHeight).inBR(pad, 0.0F);
        }

        getPanel().addUIElement(tooltip).inBL(pad + iconSize, 0);
    }

    private CustomPanelAPI getSourceIcon(Color color, CommodityMarketDataAPI commodityData, int iconSize) {
        MarketShareDataAPI marketData = commodityData.getMarketShareData(m_com.getMarket());
        boolean isSourceIllegal = marketData.isSourceIsIllegal();

        CommoditySourceType source = m_com.getCommodityMarketData().getMarketShareData(m_com.getMarket()).getSource();
        String iconPath = (String) ReflectionUtils.invoke(StarfarerSettings.class, "new", "commodity_markers",
            "imports");
        Color baseColor = color;

        switch (source) {
            case GLOBAL:
                break;
            case IN_FACTION:
                iconPath = m_com.getMarket().getFaction().getCrest();
                baseColor = null;
                break;
            case LOCAL:
            case NONE:
                iconPath = (String) ReflectionUtils.invoke(StarfarerSettings.class, "new", "commodity_markers",
                    "production");
                break;
            default:
        }

        CustomPanelAPI iconPanel = m_panel.createCustomPanel(iconSize, iconSize, new CommodityRowIconPlugin(iconPath, baseColor, isSourceIllegal));
        ((CommodityRowIconPlugin)iconPanel.getPlugin()).init(iconPanel);
        return iconPanel;
    }

    private void handleIconGroup(TooltipMakerAPI tooltip, int iconSize) {
        float available = (float)m_com.getAvailableStat().getModifiedValue();
        float totalDemand = m_com.getMaxDemand();
        float totalSupply = m_com.getMaxSupply();

        float totalTarget = Math.max(Math.max(totalDemand, totalSupply), available);
        final int totalIcons = 6;

        CommodityIconCounts iconsCount = new CommodityIconCounts(m_com);
        final int demandMetLocal = iconsCount.demandMetWithLocal;
        int demandMetInFactionImports = (int)Math.min(iconsCount.inFactionOnlyExport, totalDemand);

        final int imports = iconsCount.imports;
        final int extra = iconsCount.extra;
        final int deficit = iconsCount.deficit;

        float localProducedRatio = demandMetLocal/totalTarget;
        float inFactionImportRatio = demandMetInFactionImports/totalTarget;
        float externalImportRatio = (imports-demandMetInFactionImports)/totalTarget;
        float exportedRatio = extra/totalTarget;
        float deficitRatio = deficit/totalTarget;

        final HashMap<IconRenderMode, Integer> iconMap = new HashMap<IconRenderMode, Integer>();
        iconMap.put(IconRenderMode.GREEN, Math.round(totalIcons * exportedRatio));
        iconMap.put(IconRenderMode.OUTLINE_GREEN, Math.round(totalIcons * localProducedRatio));
        iconMap.put(IconRenderMode.NORMAL, Math.round(totalIcons * inFactionImportRatio));
        iconMap.put(IconRenderMode.OUTLINE_RED, Math.round(totalIcons * externalImportRatio));
        iconMap.put(IconRenderMode.DIM_RED, Math.round(totalIcons * deficitRatio));

        float totalIconCount = 0;
        IconRenderMode smallestIconMode = null;
        float smallestIconCount = 10f;
        for (Map.Entry<IconRenderMode, Integer> icon : iconMap.entrySet()) {
            totalIconCount += icon.getValue();
            if (icon.getValue() < smallestIconCount) {
                smallestIconCount = icon.getValue();
                smallestIconMode = icon.getKey();
            }
        }
        if (totalIconCount > 6) {
            iconMap.remove(smallestIconMode);
        }

        tooltip.beginIconGroup();
        tooltip.setIconSpacingMedium();
        for (Map.Entry<IconRenderMode, Integer> icon : iconMap.entrySet()) {
            addIconsToGroup(tooltip, m_com, icon.getValue(), icon.getKey());
        }

        tooltip.addIconGroup(iconSize,1, -3);
    }

    private int addIconsToGroup(TooltipMakerAPI tooltip, CommodityOnMarketAPI com, int count, IconRenderMode mode) {
        for (int i = 0; i < count; i++) {
            tooltip.addIcons(com, 1, mode);
        }
        return count;
    }
}
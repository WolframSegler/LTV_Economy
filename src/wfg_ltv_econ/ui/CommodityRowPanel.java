package wfg_ltv_econ.ui;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.campaign.econ.CommodityOnMarketAPI;
import com.fs.starfarer.api.campaign.econ.CommoditySourceType;
import com.fs.starfarer.api.ui.CustomPanelAPI;
import com.fs.starfarer.api.ui.IconRenderMode;
import com.fs.starfarer.api.ui.LabelAPI;
import com.fs.starfarer.api.ui.PositionAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.ui.UIComponentAPI;
import com.fs.starfarer.api.ui.UIPanelAPI;
import com.fs.starfarer.campaign.ui.marketinfo.i;
import com.fs.starfarer.settings.StarfarerSettings;

import wfg_ltv_econ.plugins.CommodityRowIconPlugin;
import wfg_ltv_econ.plugins.LtvCustomPanelPlugin;
import wfg_ltv_econ.util.LtvNumFormat;
import wfg_ltv_econ.util.ReflectionUtils;

import com.fs.starfarer.api.impl.campaign.econ.CommodityIconCounts;
import com.fs.starfarer.api.impl.campaign.ids.Strings;
import com.fs.starfarer.campaign.econ.reach.CommodityMarketData;
import com.fs.starfarer.campaign.econ.reach.MarketShareData;

import java.awt.Color;
import java.util.HashMap;
import java.util.Map;

public class CommodityRowPanel{

    private final CustomPanelAPI m_panel;
    private final CommodityOnMarketAPI m_com;
    private final UIPanelAPI m_parent;
    public final FactionAPI m_faction;
    private i m_sourceIcon;
    public boolean m_canViewPrices;

    public CommodityRowPanel(CommodityOnMarketAPI com, UIPanelAPI parent, int width, int height, FactionAPI faction) {
        this.m_com = com;
        this.m_parent = parent;
        m_panel = Global.getSettings().createCustom(width, height, new LtvCustomPanelPlugin());
        ((LtvCustomPanelPlugin)m_panel.getPlugin()).init(this, true, true, true);
        this.m_parent.addComponent(m_panel);

        this.m_faction = faction;

        boolean viewAnywhere = Global.getSettings().getBoolean("allowPriceViewAtAnyColony");
        this.m_canViewPrices = Global.getSector().getIntelManager().isPlayerInRangeOfCommRelay() || viewAnywhere;

        createRow();
    }

    public CustomPanelAPI getPanel() {
        return m_panel;
    }
    public PositionAPI getPanelPos() {
        return m_panel.getPosition();
    }
    public CommodityOnMarketAPI getCommodity() {
        return m_com;
    }
    public UIPanelAPI getParent() {
        return m_parent;
    }

    public void createRow() {
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
        CommodityMarketData commodityData = (CommodityMarketData) m_com.getCommodityMarketData();
        getPanel().addComponent(getSourceIcon(tooltip, baseColor, commodityData)).setSize(rowHeight, rowHeight).inBL(0, 0);

        if (commodityData.getExportIncome(m_com) > 0) {
            m_sourceIcon = new i(
                    (String) ReflectionUtils.invoke(StarfarerSettings.class, "new", "commodity_markers", "exports"),
                    baseColor, false);
            getPanel().addComponent(m_sourceIcon).setSize(rowHeight, rowHeight).inBR(pad, 0.0F);
        }

        getPanel().addUIElement(tooltip).inBL(pad + iconSize, 0);
    }

    private i getSourceIcon(TooltipMakerAPI tooltip, Color baseColor, CommodityMarketData commodityData) {
        MarketShareData marketData = commodityData.getMarketShareData(m_com.getMarket());
        boolean isSourceIllegal = marketData.isSourceIsIllegal();

        CommoditySourceType source = m_com.getCommodityMarketData().getMarketShareData(m_com.getMarket()).getSource();
        String iconPath = (String) ReflectionUtils.invoke(StarfarerSettings.class, "new", "commodity_markers",
            "imports");

        CustomPanelAPI iconPanel = m_panel.createCustomPanel(32f, 32f, new CommodityRowIconPlugin(iconPath, baseColor, isSourceIllegal));

        switch (source) {
            case GLOBAL:
                return new i(iconPath, baseColor, isSourceIllegal);
            case IN_FACTION:
                iconPath = m_com.getMarket().getFaction().getCrest();
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
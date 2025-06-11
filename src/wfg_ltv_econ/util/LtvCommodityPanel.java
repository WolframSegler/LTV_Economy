package wfg_ltv_econ.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import java.awt.Color;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.campaign.econ.CommodityOnMarketAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.ui.Alignment;
import com.fs.starfarer.api.ui.CustomPanelAPI;
import com.fs.starfarer.api.ui.PositionAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.ui.UIComponentAPI;
import com.fs.starfarer.api.ui.UIPanelAPI;
import com.fs.starfarer.campaign.ui.marketinfo.CommodityPanel;
import com.fs.starfarer.campaign.ui.marketinfo.CommodityTooltipFactory;
import com.fs.starfarer.ui.impl.StandardTooltipV2Expandable;
import com.fs.starfarer.ui.newui.L;
import com.fs.starfarer.ui.n;
import com.fs.starfarer.ui.Q;
import com.fs.starfarer.campaign.ui.marketinfo.ooOo;

public class LtvCommodityPanel{
    final private UIPanelAPI m_parent;
    final private CustomPanelAPI m_panel;
    final public MarketAPI m_market;

    public final FactionAPI faction;
    public final Color baseColor;
    public final Color darkColor;
    public final Color BgColor;
    public final Color gridColor;
    public final Color brightColor;

    public LtvCommodityPanel(UIPanelAPI managementPanel, int width, int height, MarketAPI market, L parentPanel) {
        this.m_parent = managementPanel;
        this.m_market = market;

        this.faction = market.getFaction();
        this.baseColor = this.faction.getBaseUIColor();
        this.darkColor = this.faction.getDarkUIColor();
        this.BgColor = new Color(5, 5, 5, 220);
        this.gridColor = this.faction.getGridUIColor();
        this.brightColor = this.faction.getBrightUIColor();
        
        m_panel = Global.getSettings().createCustom(width, height, null);
        this.m_parent.addComponent(m_panel);

        createPanel();
    }

    public CustomPanelAPI getPanel() {
        return m_panel;
    }
    public PositionAPI getPanelPos() {
        return m_panel.getPosition();
    }

    public static Comparator<CommodityOnMarketAPI> getCommodityOrderComparator() {
        return Comparator.comparingDouble(com -> com.getCommodity().getOrder());
    }

    public void createPanel() {
        final int pad = 3;
        final int opad = 10;
        final TooltipMakerAPI BgTooltip = m_panel.createUIElement(getPanelPos().getWidth(), getPanelPos().getHeight(), false);

        // Grid color
        UIComponentAPI panelGrid = BgTooltip.createRect(gridColor, 1f);
        BgTooltip.addCustom(panelGrid, opad).getPosition().setSize(getPanelPos().getWidth(), getPanelPos().getHeight()).inTL(0, 0);

        // Background Color
        UIComponentAPI BgRect = BgTooltip.createRect(BgColor, getPanelPos().getWidth());
        BgTooltip.addCustom(BgRect, opad).getPosition().setSize(getPanelPos().getWidth(), getPanelPos().getHeight()).inTL(0, 0);
        BgTooltip.sendToBottom(BgRect);
        BgRect.setOpacity(0.1f);
        
        getPanel().addUIElement(BgTooltip);

        // Select relevant commodities
        List<CommodityOnMarketAPI> commodities = m_market.getCommoditiesCopy();
        Collections.sort(commodities, getCommodityOrderComparator());
        for (CommodityOnMarketAPI com : new ArrayList<>(commodities)) {
            if (!com.isNonEcon() && com.getCommodity().isPrimary()) {
                if (com.getAvailableStat().getBaseValue() <= 0.0F && com.getMaxDemand() <= 0 && com.getMaxSupply() <= 0) {
                    commodities.remove(com);
                }
            } else {
                commodities.remove(com);
            }
        }

        final TooltipMakerAPI tooltip = m_panel.createUIElement(getPanelPos().getWidth(), getPanelPos().getHeight(), false);
        tooltip.addSectionHeading("Commodities", Alignment.MID, pad);
        final int headerHeight = (int) tooltip.getPrev().getPosition().getHeight();

        // Determine row height
        float rowHeight = getPanelPos().getHeight() - headerHeight - opad - pad;
        rowHeight = rowHeight / (float)commodities.size();
        rowHeight = (float)((int)rowHeight);
        if (rowHeight % 2.0F == 0.0F) {
           rowHeight--;
        }
        rowHeight -= pad;
        if (rowHeight > 28.0f) {
            rowHeight = 28.0f;
        }

        // Add Rows to the panel
        boolean viewAnywhere = Global.getSettings().getBoolean("allowPriceViewAtAnyColony");
        boolean canViewPrices = Global.getSector().getIntelManager().isPlayerInRangeOfCommRelay() || viewAnywhere;

        CustomPanelAPI previousRow = null;
        // n commodityWrapper;

        for (CommodityOnMarketAPI commodity : commodities) {
            // ooOo commodityRow = new ooOo((CommodityOnMarket)commodity);
            // commodityWrapper = Q.o00000(commodityRow, this);

            // commodityWrapper.setQuickMode(false);
            // commodityWrapper.setSize(getPanelPos().getWidth() - opad * 2.0F, rowHeight);
            CommodityRow comRow = new CommodityRow(commodity, getPanel(), (int)(getPanelPos().getWidth() - opad * 2), (int)rowHeight);
            comRow.getPanelPos().setSize(getPanelPos().getWidth() - opad * 2.0F, rowHeight);

            if (previousRow == null) {
                getPanel().addComponent(comRow.getPanel()).inTL(opad, headerHeight + opad);
            } else {
                getPanel().addComponent(comRow.getPanel()).belowLeft(previousRow, pad);
            }

            // if (previousRow == null) {
            //     add(commodityWrapper).inTL(opad, getTitleHeight() + opad);
            // } else {
            //     add(commodityWrapper).belowLeft(previousRow, pad);
            // }

            // // StandardTooltipV2Expandable comTooltip = CommodityTooltipFactory.super(commodity);
            // StandardTooltipV2Expandable comTooltip = (StandardTooltipV2Expandable) ReflectionUtils.invoke(CommodityTooltipFactory.class, "super", commodity);
            // commodityWrapper.setTooltip(0.0F, comTooltip);

            // // Required for Lambda
            // final n finalRow = commodityWrapper;
            // final StandardTooltipV2Expandable finalTooltip = comTooltip;

            // // comTooltip.setBeforeShowing(new 2(this, commodityWrapper, comTooltip));
            // finalTooltip.setBeforeShowing(() -> {
            //     finalRow.setTooltipPositionRelativeToAnchor(
            //         -finalTooltip.getWidth(),
            //         -(finalTooltip.getHeight() - getPanelPos().getHeight()),
            //         this
            //     );
            // });
            // finalRow.setEnabled(canViewPrices);

            previousRow = comRow.getPanel();
        }
        getPanel().addUIElement(tooltip);
    }
}

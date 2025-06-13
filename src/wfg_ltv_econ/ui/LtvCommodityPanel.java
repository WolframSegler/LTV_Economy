package wfg_ltv_econ.ui;

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

public class LtvCommodityPanel{
    private final UIPanelAPI m_parent;
    private final CustomPanelAPI m_panel;
    public final MarketAPI m_market;

    public final FactionAPI faction;
    public final Color BgColor;
    public final Color gridColor;

    public LtvCommodityPanel(UIPanelAPI managementPanel, int width, int height, MarketAPI market) {
        this.m_parent = managementPanel;
        this.m_market = market;

        this.faction = market.getFaction();
        this.BgColor = new Color(0, 0, 0, 220);
        this.gridColor = this.faction.getGridUIColor();
        
        m_panel = Global.getSettings().createCustom(width, height, null);

        createPanel();
    }

    public CustomPanelAPI getPanel() {
        return m_panel;
    }
    public PositionAPI getPanelPos() {
        return m_panel.getPosition();
    }
    public UIPanelAPI getParent() {
        return m_parent;
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
        BgRect.setOpacity(0.2f);
        
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

        final TooltipMakerAPI FgTooltip = m_panel.createUIElement(getPanelPos().getWidth(), getPanelPos().getHeight(), false);
        FgTooltip.addSectionHeading("Commodities", Alignment.MID, pad);
        final int headerHeight = (int) FgTooltip.getPrev().getPosition().getHeight();

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
        CustomPanelAPI previousRow = null;

        for (CommodityOnMarketAPI commodity : commodities) {
            CommodityRowPanel comRow = new CommodityRowPanel(commodity, getPanel(), (int)(getPanelPos().getWidth() - opad * 2), (int)rowHeight, faction);
            comRow.getPanelPos().setSize(getPanelPos().getWidth() - opad * 2.0F, rowHeight);

            if (previousRow == null) {
                getPanel().addComponent(comRow.getPanel()).inTL(opad, headerHeight + opad);
            } else {
                getPanel().addComponent(comRow.getPanel()).belowLeft(previousRow, pad);
            }

            previousRow = comRow.getPanel();
        }
        getPanel().addUIElement(FgTooltip);
    }
}

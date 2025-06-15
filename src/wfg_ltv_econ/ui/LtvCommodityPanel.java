package wfg_ltv_econ.ui;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import com.fs.starfarer.api.campaign.CustomUIPanelPlugin;
import com.fs.starfarer.api.campaign.econ.CommodityOnMarketAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.ui.Alignment;
import com.fs.starfarer.api.ui.CustomPanelAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.ui.UIPanelAPI;

import wfg_ltv_econ.plugins.LtvCustomPanelPlugin;

public class LtvCommodityPanel extends LtvCustomPanel{

    public LtvCommodityPanel(UIPanelAPI managementPanel, int width, int height, MarketAPI market, CustomUIPanelPlugin plugin) {
        super(managementPanel, width, height, plugin, market);

        initializePanel(hasPlugin);
        createPanel();
    }

    public void initializePanel(boolean hasPlugin) {
        ((LtvCustomPanelPlugin)m_panel.getPlugin()).init(this, false, false, false, null, true, true);
    }

    public static Comparator<CommodityOnMarketAPI> getCommodityOrderComparator() {
        return Comparator.comparingDouble(com -> com.getCommodity().getOrder());
    }

    public void createPanel() {
        final int pad = 3;
        final int opad = 10;

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
        ((LtvCustomPanelPlugin)m_panel.getPlugin()).setOffsets(0, 0, 0, -headerHeight);

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
            CommodityRowPanel comRow = new CommodityRowPanel(commodity, getPanel(), (int)(getPanelPos().getWidth() - opad * 2), (int)rowHeight, m_market);
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

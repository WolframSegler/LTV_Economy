package wfg.ltv_econ.ui.panels;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.econ.CommodityOnMarketAPI;
import com.fs.starfarer.api.campaign.econ.CommoditySpecAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.ui.Alignment;
import com.fs.starfarer.api.ui.CustomPanelAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.ui.UIPanelAPI;

import wfg.ltv_econ.economy.CommodityStats;
import wfg.ltv_econ.economy.EconomyEngine;
import wfg.ltv_econ.ui.panels.LtvCustomPanel.HasBackground;
import wfg.ltv_econ.ui.panels.LtvCustomPanel.HasOutline;
import wfg.ltv_econ.ui.plugins.BasePanelPlugin;

public class LtvCommodityPanel extends LtvCustomPanel<BasePanelPlugin<LtvCommodityPanel>, LtvCommodityPanel, CustomPanelAPI>
    implements HasBackground, HasOutline {

    protected List<LtvCommodityRowPanel> commodityRows = new ArrayList<>();

    public static final int STANDARD_WIDTH = 264;
    public String m_headerTxt;

    public boolean childrenIgnoreUIState = false;
    public boolean isRowSelectable = false;
    public boolean m_canViewPrices = false;

    public LtvCommodityPanel(UIPanelAPI root, UIPanelAPI parent, int width, int height, MarketAPI market,
        BasePanelPlugin<LtvCommodityPanel> plugin, String headerTxt) {
        this(root, parent, width, height, market, plugin, headerTxt, false);
    }

    public LtvCommodityPanel(UIPanelAPI root, UIPanelAPI parent, int width, int height, MarketAPI market,
        BasePanelPlugin<LtvCommodityPanel> plugin) {
        this(root, parent, width, height, market, plugin, "Commodities", false);
    }

    public LtvCommodityPanel(UIPanelAPI root, UIPanelAPI parent, int width, int height, MarketAPI market,
        BasePanelPlugin<LtvCommodityPanel> plugin, boolean childrenIgnoreUIState) {
        this(root, parent, width, height, market, plugin, "Commodities", childrenIgnoreUIState);
    }

    public LtvCommodityPanel(UIPanelAPI root, UIPanelAPI parent, int width, int height, MarketAPI market,
        BasePanelPlugin<LtvCommodityPanel> plugin, String headerTxt, boolean childrenIgnoreUIState) {
        super(root, parent, width, height, plugin, market);

        m_headerTxt = headerTxt;
        this.childrenIgnoreUIState = childrenIgnoreUIState;

        boolean viewAnywhere = Global.getSettings().getBoolean("allowPriceViewAtAnyColony");
        m_canViewPrices = Global.getSector().getIntelManager().isPlayerInRangeOfCommRelay() || viewAnywhere;

        initializePlugin(hasPlugin);
    }

    public void initializePlugin(boolean hasPlugin) {
        getPlugin().init(this);
    }

    public static Comparator<CommoditySpecAPI> getCommodityOrderComparator() {
        return Comparator.comparingDouble(com -> com.getOrder());
    }

    public void setRowSelectable(boolean a) {
        isRowSelectable = a;
    }
    
    public List<LtvCommodityRowPanel> getCommodityRows() {
        return commodityRows;
    }

    public HasActionListener selectionListener;
    public void setActionListener(HasActionListener listener) {
        selectionListener = listener;
    }

    public void createPanel() {
        final int pad = 3;
        final int opad = 10;

        EconomyEngine.getInstance().fakeAdvance();

        // Select relevant commodities
        List<CommoditySpecAPI> commodities = EconomyEngine.getEconCommodities();
        Collections.sort(commodities, getCommodityOrderComparator());
        commodities.removeIf(com -> {
            CommodityStats stats = EconomyEngine.getInstance().getComStats(com.getId(), getMarket().getId());
            return stats.getEconomicFootprint() <= 0;
        });

        final TooltipMakerAPI tooltip = m_panel.createUIElement(
            getPos().getWidth(), 0, false
        );
        tooltip.addSectionHeading(m_headerTxt, Alignment.MID, pad);

        final int headerHeight = (int) tooltip.getPrev().getPosition().getHeight();
        tooltip.setHeightSoFar(headerHeight);
        getPanel().addUIElement(tooltip).inTL(0, 0);
        getPlugin().setOffsets(1, 1, -2, -headerHeight - 2);

        final TooltipMakerAPI rowTp = m_panel.createUIElement(
            getPos().getWidth(), getPos().getHeight() - headerHeight, true
        );
        
        final int rowHeight = 28;
        int cumulativeYOffset = opad;

        for (CommoditySpecAPI com : commodities) {
            LtvCommodityRowPanel comRow = new LtvCommodityRowPanel(
                getRoot(), getPanel(), getMarket(), com.getId(), this, (int)(getPos().getWidth() - opad * 2), 
                rowHeight, childrenIgnoreUIState, m_canViewPrices
            );

            rowTp.addComponent(comRow.getPanel()).inTL(opad, cumulativeYOffset);

            cumulativeYOffset += pad + rowHeight;

            comRow.setActionListener(selectionListener);

            commodityRows.add(comRow);
        }
        rowTp.setHeightSoFar(cumulativeYOffset);
        getPanel().addUIElement(rowTp).inTL(0, headerHeight);
    }

    public void selectRow(String comID) {
        CommodityOnMarketAPI com = getMarket().getCommodityData(comID);
        for (LtvCommodityRowPanel row : commodityRows) {
            row.setPersistentGlow(row.getCommodity() == com);
        }
    }

    public void selectRow(LtvCommodityRowPanel selectedRow) {
        for (LtvCommodityRowPanel row : commodityRows) {
            row.setPersistentGlow(row == selectedRow);
        }
    }
}

package wfg_ltv_econ.ui.panels;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.econ.CommodityOnMarketAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.ui.Alignment;
import com.fs.starfarer.api.ui.CustomPanelAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.ui.UIPanelAPI;

import wfg_ltv_econ.ui.plugins.BasePanelPlugin;
import wfg_ltv_econ.ui.panels.LtvCustomPanel.HasBackground;
import wfg_ltv_econ.ui.panels.LtvCustomPanel.HasOutline;

public class LtvCommodityPanel extends LtvCustomPanel<BasePanelPlugin<LtvCommodityPanel>, LtvCommodityPanel, CustomPanelAPI>
    implements HasBackground, HasOutline {

    protected List<LtvCommodityRowPanel> commodityRows = new ArrayList<>();

    public static int STANDARD_WIDTH = 264;
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

    public static Comparator<CommodityOnMarketAPI> getCommodityOrderComparator() {
        return Comparator.comparingDouble(com -> com.getCommodity().getOrder());
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

        // Select relevant commodities
        List<CommodityOnMarketAPI> commodities = getMarket().getCommoditiesCopy();
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
        final TooltipMakerAPI tooltip = m_panel.createUIElement(getPos().getWidth(), getPos().getHeight(), false);
        tooltip.addSectionHeading(m_headerTxt, Alignment.MID, pad);

        final int headerHeight = (int) tooltip.getPrev().getPosition().getHeight();
        getPlugin().setOffsets(1, 1, -2, -headerHeight - 2);

        // Determine row height
        float rowHeight = getPos().getHeight() - headerHeight - opad - pad;
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
            LtvCommodityRowPanel comRow = new LtvCommodityRowPanel(getRoot(), getPanel(), getMarket(), commodity,
            this, (int)(getPos().getWidth() - opad * 2), (int)rowHeight, childrenIgnoreUIState, m_canViewPrices);

            comRow.getPos().setSize(getPos().getWidth() - opad * 2.0F, rowHeight);

            if (previousRow == null) {
                getPanel().addComponent(comRow.getPanel()).inTL(opad, headerHeight + opad);
            } else {
                getPanel().addComponent(comRow.getPanel()).belowLeft(previousRow, pad);
            }

            comRow.setActionListener(selectionListener);

            previousRow = comRow.getPanel();
            commodityRows.add(comRow);
        }
        getPanel().addUIElement(tooltip).inTL(0, 0);
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

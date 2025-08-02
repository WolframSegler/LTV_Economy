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

import wfg_ltv_econ.plugins.LtvCustomPanelPlugin.Glow;
import wfg_ltv_econ.plugins.LtvCustomPanelPlugin.Outline;
import wfg_ltv_econ.ui.com_detail_dialog.LtvCommodityDetailDialog.CommoditySelectionListener;

public class LtvCommodityPanel extends LtvCustomPanel{

    protected List<LtvCommodityRowPanel> commodityRows = new ArrayList<>();

    public static int STANDARD_WIDTH = 264;
    public String m_headerTxt;

    public boolean childrenIgnoreUIState = false;
    public boolean isRowSelectable = false;

    public LtvCommodityPanel(UIPanelAPI root, UIPanelAPI parent, int width, int height, MarketAPI market,
        CustomUIPanelPlugin plugin, String headerTxt) {
        this(root, parent, width, height, market, plugin, headerTxt, false);
    }

    public LtvCommodityPanel(UIPanelAPI root, UIPanelAPI parent, int width, int height, MarketAPI market,
        CustomUIPanelPlugin plugin) {
        this(root, parent, width, height, market, plugin, "Commodities", false);
    }

    public LtvCommodityPanel(UIPanelAPI root, UIPanelAPI parent, int width, int height, MarketAPI market,
        CustomUIPanelPlugin plugin, boolean childrenIgnoreUIState) {
        this(root, parent, width, height, market, plugin, "Commodities", childrenIgnoreUIState);
    }

    public LtvCommodityPanel(UIPanelAPI root, UIPanelAPI parent, int width, int height, MarketAPI market,
        CustomUIPanelPlugin plugin, String headerTxt, boolean childrenIgnoreUIState) {
        super(root, parent, width, height, plugin, market);

        m_headerTxt = headerTxt;
        this.childrenIgnoreUIState = childrenIgnoreUIState;

        initializePlugin(hasPlugin);
        createPanel();
    }

    public void initializePlugin(boolean hasPlugin) {
        getPlugin().init(this, Glow.NONE, false, true, Outline.LINE);
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
        FgTooltip.addSectionHeading(m_headerTxt, Alignment.MID, pad);

        final int headerHeight = (int) FgTooltip.getPrev().getPosition().getHeight();
        getPlugin().setOffsets(0, 0, 0, -headerHeight);

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
            LtvCommodityRowPanel comRow = new LtvCommodityRowPanel(getRoot(), getPanel(), m_market, commodity,
            this, (int)(getPanelPos().getWidth() - opad * 2), (int)rowHeight, childrenIgnoreUIState);

            comRow.getPanelPos().setSize(getPanelPos().getWidth() - opad * 2.0F, rowHeight);

            if (previousRow == null) {
                getPanel().addComponent(comRow.getPanel()).inTL(opad, headerHeight + opad);
            } else {
                getPanel().addComponent(comRow.getPanel()).belowLeft(previousRow, pad);
            }

            previousRow = comRow.getPanel();
            commodityRows.add(comRow);
        }
        getPanel().addUIElement(FgTooltip).inTL(0, 0);
    }

    public void selectRow(String comID) {
        CommodityOnMarketAPI com = m_market.getCommodityData(comID);
        for (LtvCommodityRowPanel row : commodityRows) {
            row.getPlugin().setPersistentGlow(row.getCommodity() == com);
        }
    }

    public void selectRow(LtvCommodityRowPanel selectedRow) {
        for (LtvCommodityRowPanel row : commodityRows) {
            row.getPlugin().setPersistentGlow(row == selectedRow);
        }
    }

    public CommoditySelectionListener selectionListener;
    public void setCommoditySelectionListener(CommoditySelectionListener listener) {
        this.selectionListener = listener;
    }
}

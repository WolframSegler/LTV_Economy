package wfg.ltv_econ.ui.panels;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import com.fs.starfarer.api.campaign.econ.CommodityOnMarketAPI;
import com.fs.starfarer.api.campaign.econ.CommoditySpecAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.ui.Alignment;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.ui.UIPanelAPI;

import wfg.ltv_econ.economy.engine.EconomyEngine;
import wfg.ltv_econ.economy.engine.EconomyInfo;
import wfg.wrap_ui.ui.ComponentFactory;
import wfg.wrap_ui.ui.panels.CustomPanel;
import wfg.wrap_ui.ui.panels.CustomPanel.HasBackground;
import wfg.wrap_ui.ui.panels.CustomPanel.HasOutline;
import wfg.wrap_ui.ui.plugins.BasePanelPlugin;
import static wfg.wrap_ui.util.UIConstants.*;

public class LtvCommodityPanel extends CustomPanel<BasePanelPlugin<LtvCommodityPanel>, LtvCommodityPanel>
    implements HasBackground, HasOutline {

    public static final int STANDARD_WIDTH = 264;
    public String m_headerTxt;
    public boolean rowsIgnoreUIState = false;

    protected final List<CommodityRowPanel> commodityRows = new ArrayList<>();
    protected final MarketAPI m_market;
    protected HasActionListener selectionListener;

    public LtvCommodityPanel(UIPanelAPI parent, int width, int height, String headerTxt, MarketAPI market) {
        this(parent, width, height, headerTxt, false, market);
    }

    public LtvCommodityPanel(UIPanelAPI parent, int width, int height,
        MarketAPI market) {
        this(parent, width, height, "Commodities", false, market);
    }

    public LtvCommodityPanel(UIPanelAPI parent, int width, int height,
        boolean rowsIgnoreUIState, MarketAPI market
    ) {
        this(parent, width, height, "Commodities", rowsIgnoreUIState, market);
    }

    public LtvCommodityPanel(UIPanelAPI parent, int width, int height,
        String headerTxt, boolean rowsIgnoreUIState, MarketAPI market
    ) {
        super(parent, width, height, new BasePanelPlugin<>());

        m_market = market;
        m_headerTxt = headerTxt;
        this.rowsIgnoreUIState = rowsIgnoreUIState;

        getPlugin().init(this);
    }

    public static Comparator<CommoditySpecAPI> getCommodityOrderComparator() {
        return Comparator.comparingDouble(com -> com.getOrder());
    }
    
    public List<CommodityRowPanel> getCommodityRows() {
        return commodityRows;
    }

    public Optional<HasActionListener> getActionListener() {
        return Optional.ofNullable(selectionListener);
    }
    public void setActionListener(HasActionListener listener) {
        selectionListener = listener;
    }

    public void createPanel() {
        // Select relevant commodities
        final List<CommoditySpecAPI> commodities = EconomyInfo.getEconCommodities();
        Collections.sort(commodities, getCommodityOrderComparator());
        commodities.removeIf(com -> {
            return EconomyEngine.getInstance().getComCell(com.getId(), m_market.getId()).getFlowEconomicFootprint() <= 0;
        });

        final TooltipMakerAPI tooltip = ComponentFactory.createTooltip(
            getPos().getWidth(), true
        );
        tooltip.addSectionHeading(m_headerTxt, Alignment.MID, pad);

        final int headerHeight = (int) tooltip.getPrev().getPosition().getHeight();
        tooltip.setHeightSoFar(headerHeight);
        ComponentFactory.addTooltip(tooltip, headerHeight, true, m_panel).inTL(0, 0);
        getPlugin().setOffsets(1, 1, -2, -headerHeight - 2);

        final TooltipMakerAPI rowTp = ComponentFactory.createTooltip(
            getPos().getWidth(), true
        );
        
        final int rowHeight = 28;
        int cumulativeYOffset = opad;

        for (CommoditySpecAPI com : commodities) {
            final CommodityRowPanel comRow = new CommodityRowPanel(
                getPanel(), m_market, com.getId(), (int)(getPos().getWidth() - opad * 2), 
                rowHeight, rowsIgnoreUIState
            );

            rowTp.addComponent(comRow.getPanel()).inTL(opad, cumulativeYOffset);

            cumulativeYOffset += pad + 2 + rowHeight;

            comRow.setActionListener(selectionListener);

            commodityRows.add(comRow);
        }
        rowTp.setHeightSoFar(cumulativeYOffset);
        ComponentFactory.addTooltip(tooltip, getPos().getHeight() - headerHeight, true, m_panel)
            .inTL(0, headerHeight);
    }

    public void selectRow(String comID) {
        final CommodityOnMarketAPI com = m_market.getCommodityData(comID);
        for (CommodityRowPanel row : commodityRows) {
            row.setPersistentGlow(row.getCommodity() == com);
        }
    }

    public void selectRow(CommodityRowPanel selectedRow) {
        for (CommodityRowPanel row : commodityRows) {
            row.setPersistentGlow(row == selectedRow);
        }
    }

    public float getBgAlpha() { return 0.65f; }
}
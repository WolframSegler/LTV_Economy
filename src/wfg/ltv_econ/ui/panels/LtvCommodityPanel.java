package wfg.ltv_econ.ui.panels;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import com.fs.starfarer.api.campaign.econ.CommodityOnMarketAPI;
import com.fs.starfarer.api.campaign.econ.CommoditySpecAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.ui.Alignment;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.ui.UIPanelAPI;

import wfg.ltv_econ.economy.engine.EconomyEngine;
import wfg.ltv_econ.economy.engine.EconomyInfo;
import wfg.native_ui.ui.ComponentFactory;
import wfg.native_ui.ui.components.BackgroundComp;
import wfg.native_ui.ui.components.LayoutOffsetComp;
import wfg.native_ui.ui.components.NativeComponents;
import wfg.native_ui.ui.components.OutlineComp;
import wfg.native_ui.ui.components.UIContextComp;
import wfg.native_ui.ui.components.InteractionComp.ClickHandler;
import wfg.native_ui.ui.panels.CustomPanel;
import wfg.native_ui.ui.panels.CustomPanel.HasBackground;
import wfg.native_ui.ui.panels.CustomPanel.HasOutline;
import static wfg.native_ui.util.UIConstants.*;

public class LtvCommodityPanel extends CustomPanel<LtvCommodityPanel> implements HasBackground, HasOutline {

    public final BackgroundComp bg = comp().get(NativeComponents.BACKGROUND);
    public final OutlineComp outline = comp().get(NativeComponents.OUTLINE);
    public final UIContextComp context = comp().get(NativeComponents.UI_CONTEXT);
    public final LayoutOffsetComp offset = comp().get(NativeComponents.LAYOUT_OFFSET);

    public static final int STANDARD_WIDTH = 264;
    public String m_headerTxt;
    public boolean rowsIgnoreUIState = false;
    public final List<CommodityRowPanel> commodityRows = new ArrayList<>();
    public final MarketAPI m_market;
    public ClickHandler<CommodityRowPanel> selectionListener;

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
    ) { super(parent, width, height);

        m_market = market;
        m_headerTxt = headerTxt;
        this.rowsIgnoreUIState = rowsIgnoreUIState;

        context.ignore = rowsIgnoreUIState;

        bg.alpha = 0.65f;
    }

    public static Comparator<CommoditySpecAPI> getCommodityOrderComparator() {
        return Comparator.comparingDouble(com -> com.getOrder());
    }

    public void createPanel() {
        // Select relevant commodities
        final List<CommoditySpecAPI> commodities = EconomyInfo.getEconCommodities();
        Collections.sort(commodities, getCommodityOrderComparator());
        commodities.removeIf(com -> {
            return EconomyEngine.getInstance().getComCell(com.getId(), m_market.getId()).getFlowEconomicFootprint() <= 0;
        });

        final TooltipMakerAPI headerTp = ComponentFactory.createTooltip(
            getPos().getWidth(), false
        );
        headerTp.addSectionHeading(m_headerTxt, Alignment.MID, pad);

        final int headerHeight = (int) headerTp.getPrev().getPosition().getHeight();
        headerTp.setHeightSoFar(headerHeight);
        ComponentFactory.addTooltip(headerTp, headerHeight, false, m_panel).inTL(0, 0);
        offset.setOffset(1, 1, -2, -headerHeight - 2);

        final TooltipMakerAPI rowTp = ComponentFactory.createTooltip(
            getPos().getWidth(), true
        );
        
        final int rowHeight = 28;
        int cumulativeYOffset = opad;

        for (CommoditySpecAPI com : commodities) {
            final CommodityRowPanel comRow = new CommodityRowPanel(
                m_panel, m_market, com.getId(), (int)(getPos().getWidth() - opad * 2), 
                rowHeight, rowsIgnoreUIState
            );

            rowTp.addComponent(comRow.getPanel()).inTL(opad, cumulativeYOffset);

            cumulativeYOffset += pad + 2 + rowHeight;

            comRow.interaction.onClicked = selectionListener;

            commodityRows.add(comRow);
        }
        rowTp.setHeightSoFar(cumulativeYOffset);
        ComponentFactory.addTooltip(rowTp, getPos().getHeight() - headerHeight, true, m_panel)
            .inTL(0, headerHeight);
    }

    public void selectRow(String comID) {
        final CommodityOnMarketAPI com = m_market.getCommodityData(comID);
        for (CommodityRowPanel row : commodityRows) {
            row.glow.persistent = row.m_com == com;
        }
    }

    public void selectRow(CommodityRowPanel selectedRow) {
        for (CommodityRowPanel row : commodityRows) {
            row.glow.persistent = row == selectedRow;
        }
    }
}
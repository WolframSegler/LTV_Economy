package wfg_ltv_econ.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.econ.CommodityOnMarketAPI;
import com.fs.starfarer.api.campaign.econ.Industry;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.campaign.econ.CommodityOnMarket;
import com.fs.starfarer.campaign.ui.marketinfo.CommodityPanel;
import com.fs.starfarer.campaign.ui.marketinfo.CommodityTooltipFactory;
import com.fs.starfarer.ui.supersuper;
import com.fs.starfarer.ui.impl.StandardTooltipV2Expandable;
import com.fs.starfarer.ui.newui.L;
import com.fs.starfarer.ui.n;
import com.fs.starfarer.ui.Q;
import com.fs.starfarer.campaign.ui.marketinfo.ooOo;

public class LtvCommodityPanel extends CommodityPanel {
    public LtvCommodityPanel(MarketAPI market, L parentPanel) {
        this(market, parentPanel, "Commodities");
    }

    public LtvCommodityPanel(MarketAPI market, L parentPanel, String panelHeader) {
        super(market, parentPanel, panelHeader);
    }

    public static Comparator<CommodityOnMarketAPI> getCommodityOrderComparator() {
        return Comparator.comparingDouble(com -> com.getCommodity().getOrder());
    }

    @Override
    public void sizeChanged(float panelWidth, float panelHeight) {
        clearChildren();
        //   super.sizeChanged(panelWidth, panelHeight);
        if (title != null) {
            if (!getChildrenNonCopy().contains(title)) {
                add(title).inTL(0.0F, 0.0F);
            }

            title.setSize(panelWidth, title.getHeight());
            title.getPosition().recompute();
        }
        // super.sizeChanged(panelWidth, panelHeight);
        if (!created) {
            afterSizeFirstChanged(panelWidth, panelHeight);
            created = true;
        }
        // Grandparent and Greatgrandparent method ends here

        List<CommodityOnMarketAPI> commodities = market.getCommoditiesCopy();
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

        final float pad = 3.0f;
        final float opad = 10.0f;

        float newHeight = panelHeight - titleHeight - opad - pad;
        float rowHeight = newHeight / (float)commodities.size();
        rowHeight = (float)((int)rowHeight);
        if (rowHeight % 2.0F == 0.0F) {
           rowHeight--;
        }
        rowHeight -= pad;
        if (rowHeight > 28.0f) {
            rowHeight = 28.0f;
        }
        // All this slop to determine the rowHeight

        boolean viewAnywhere = Global.getSettings().getBoolean("allowPriceViewAtAnyColony");
        boolean canViewPrices = Global.getSector().getIntelManager().isPlayerInRangeOfCommRelay() || viewAnywhere;

        n previousRow = null;
        n comRow;

        for (CommodityOnMarketAPI commodity : commodities) {
            ooOo comWrapper = new ooOo((CommodityOnMarket) commodity);
            comRow = Q.o00000(comWrapper, this);

            comRow.setQuickMode(false);
            comRow.setSize(panelWidth - opad * 2.0F, rowHeight);

            if (previousRow == null) {
                add(comRow).inTL(opad, getTitleHeight() + opad);
            } else {
                add(comRow).belowLeft(previousRow, pad);
            }

            // StandardTooltipV2Expandable comTooltip = CommodityTooltipFactory.super(commodity);
            StandardTooltipV2Expandable comTooltip = (StandardTooltipV2Expandable) ReflectionUtils.invoke(CommodityTooltipFactory.class, "super", commodity);
            comRow.setTooltip(0.0F, comTooltip);

            // Required for Lambda
            final n finalRow = comRow;
            final StandardTooltipV2Expandable finalTooltip = comTooltip;

            // comTooltip.setBeforeShowing(new 2(this, comRow, comTooltip));
            finalTooltip.setBeforeShowing(() -> {
                finalRow.setTooltipPositionRelativeToAnchor(
                    -finalTooltip.getWidth(),
                    -(finalTooltip.getHeight() - this.getHeight()),
                    this
                );
            });
            finalRow.setEnabled(canViewPrices);

            previousRow = comRow;
        }
    }
}

package wfg.ltv_econ.ui.scripts;

import com.fs.starfarer.api.campaign.PlayerMarketTransaction;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.campaign.econ.SubmarketAPI;
import com.fs.starfarer.api.campaign.listeners.ColonyInteractionListener;
import com.fs.starfarer.campaign.econ.Market;
import com.fs.starfarer.campaign.econ.Submarket;

import rolflectionlib.util.RolfLectionUtil;
import wfg.ltv_econ.util.wrappers.MarketWrapper;

public class SubmarketMarketRestorer implements ColonyInteractionListener {

    private static final Object marketField = RolfLectionUtil.getAllFields(Submarket.class).stream()
        .filter(f -> Market.class.isAssignableFrom(RolfLectionUtil.getFieldType(f)))
        .findFirst().get();

    @Override public void reportPlayerMarketTransaction(PlayerMarketTransaction transaction) {}
    @Override public void reportPlayerOpenedMarket(MarketAPI market) {}
    @Override public void reportPlayerOpenedMarketAndCargoUpdated(MarketAPI market) {}
    @Override
    public void reportPlayerClosedMarket(MarketAPI market) {
        for (SubmarketAPI submarket : market.getSubmarketsCopy()) {
            final MarketAPI marketRef = submarket.getMarket();
            if (marketRef instanceof MarketWrapper wrapper) {
                setMarketField(submarket, wrapper.original);
            }
        }
    }

    public static final void setMarketField(SubmarketAPI submarket, Market market) {
        RolfLectionUtil.setPrivateVariable(marketField, submarket, market);
    }
}
package wfg.ltv_econ.submarket;

import static wfg.ltv_econ.constant.strings.Income.PLAYER_MARKET_TRANSACTION_KEY;
import static wfg.ltv_econ.constant.strings.Income.getDesc;
import static wfg.ltv_econ.constant.strings.LocalizedStrings.*;

import com.fs.starfarer.api.campaign.CampaignUIAPI.CoreUITradeMode;
import com.fs.starfarer.api.campaign.CoreUIAPI;
import com.fs.starfarer.api.campaign.PlayerMarketTransaction;
import com.fs.starfarer.api.campaign.RepLevel;
import com.fs.starfarer.api.campaign.econ.CommodityOnMarketAPI;

import wfg.ltv_econ.economy.registry.MarketFinanceRegistry;

public class MilitarySubmarketPlugin extends com.fs.starfarer.api.impl.campaign.submarkets.MilitarySubmarketPlugin {
		
	@Override
    public void reportPlayerMarketTransaction(PlayerMarketTransaction transaction) {
		super.reportPlayerMarketTransaction(transaction);

        MarketFinanceRegistry.instance().getLedger(market.getId()).add(
			PLAYER_MARKET_TRANSACTION_KEY, -transaction.getCreditValue(), getDesc(PLAYER_MARKET_TRANSACTION_KEY)
		);
    }
	
	@Override
	public int getStockpileLimit(CommodityOnMarketAPI com) {
        return OpenSubmarketPlugin.getStockpileLimit(com, market, submarket, false);
	}
	
    @Override
	public String getTooltipAppendix(CoreUIAPI ui) {
		if (!isEnabled(ui)) {
			return str("requiredTxtFull") + submarket.getFaction().getDisplayName() + " - " + RepLevel.FAVORABLE.getDisplayName().toLowerCase();
		}
		if (ui.getTradeMode() == CoreUITradeMode.SNEAK) {
			return str("submarketProperDockingTxt");
		}
		return null;
	}
}
package wfg.ltv_econ.submarket;

import static wfg.ltv_econ.constant.strings.LocalizedStrings.*;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignUIAPI.CoreUITradeMode;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.CoreUIAPI;
import com.fs.starfarer.api.campaign.econ.CommodityOnMarketAPI;
import com.fs.starfarer.api.impl.campaign.CoreCampaignPluginImpl;
import com.fs.starfarer.api.impl.campaign.submarkets.BlackMarketPlugin;
import com.fs.starfarer.api.util.Misc;

public class BlackSubmarketPlugin extends BlackMarketPlugin {
	
	@Override
	public int getStockpileLimit(CommodityOnMarketAPI com) {
        return OpenSubmarketPlugin.getStockpileLimit(com, market, submarket, true);
	}
	
	@Override 
	public String getTooltipAppendix(CoreUIAPI ui) {
		if (isEnabled(ui)) {

            final StringBuilder sb = new StringBuilder();
			if (Misc.isExtended()) {
				final CoreUIAPI core = Global.getSector().getCampaignUI().getCore2();
                final CampaignFleetAPI playerFleet = Global.getSector().getPlayerFleet();
				final boolean tradingTOff = core != null && core.getTradeMode() != CoreUITradeMode.NONE &&
					playerFleet != null && !playerFleet.isTransponderOn();

				if (core != null && core.getTradeMode() != CoreUITradeMode.SNEAK && !market.isFreePort() && !tradingTOff) {
                    final boolean hasSneakTariff = Global.getSettings().getFloat("ext_blackMarketTariffSneak") > 0f;
                    sb.append(str(hasSneakTariff ? "blackSubmarketTradeAdvice1" : "blackSubmarketTradeAdvice2"));
				}
			}
			
			final float suspicion = CoreCampaignPluginImpl.computeSmugglingSuspicionLevel(market);
            final String suspicionTxt;
			if (suspicion < 0.05f) {
                suspicionTxt = str("suspicionLevel0Txt");
            } else if (suspicion < 0.1f) {
				suspicionTxt = str("suspicionLevel1Txt");
			} else if (suspicion < 0.2f) {
				suspicionTxt = str("suspicionLevel2Txt");
			} else if (suspicion < 0.3f) {
				suspicionTxt = str("suspicionLevel3Txt");
			} else if (suspicion < 0.5f) {
				suspicionTxt = str("suspicionLevel4Txt");
			} else {
                suspicionTxt = str("suspicionLevel5Txt");
            }

            sb.append(suspicionTxt);
            return sb.toString();
		}

		return null;
	}
}
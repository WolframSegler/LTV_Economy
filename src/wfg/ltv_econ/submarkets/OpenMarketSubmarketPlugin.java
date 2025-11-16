package wfg.ltv_econ.submarkets;

import java.util.Random;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignUIAPI.CoreUITradeMode;
import com.fs.starfarer.api.campaign.CoreUIAPI;
import com.fs.starfarer.api.campaign.FactionAPI.ShipPickMode;
import com.fs.starfarer.api.campaign.PlayerMarketTransaction;
import com.fs.starfarer.api.campaign.econ.CommodityOnMarketAPI;
import com.fs.starfarer.api.campaign.econ.SubmarketAPI;
import com.fs.starfarer.api.impl.campaign.ids.Commodities;
import com.fs.starfarer.api.impl.campaign.ids.Submarkets;
import com.fs.starfarer.api.impl.campaign.submarkets.BaseSubmarketPlugin;
import com.fs.starfarer.api.util.Highlights;
import com.fs.starfarer.api.util.Misc;

import wfg.ltv_econ.economy.CommodityStats;
import wfg.ltv_econ.economy.EconomyEngine;

public class OpenMarketSubmarketPlugin extends BaseSubmarketPlugin {
	
	public void init(SubmarketAPI submarket) {
		super.init(submarket);
	}

	public void updateCargoPrePlayerInteraction() {
		final float seconds = Global.getSector().getClock().convertToSeconds(sinceLastCargoUpdate);
		addAndRemoveStockpiledResources(seconds, false, true, true);
		sinceLastCargoUpdate = 0f;

		if (okToUpdateShipsAndWeapons()) {
			sinceSWUpdate = 0f;

            final boolean isMilitary = Misc.isMilitary(market);

			float extraShips = 0f;
			if (isMilitary && market.isHidden() && !market.hasSubmarket(Submarkets.GENERIC_MILITARY)) {
				extraShips = 150f;
			}
			
			pruneWeapons(0f);
			
			final int weapons = 5 + Math.max(0, market.getSize() - 1) + (isMilitary ? 5 : 0);
			final int fighters = 1 + Math.max(0, (market.getSize() - 3) / 2) + (isMilitary ? 2 : 0);
			
			addWeapons(weapons, weapons + 2, 0, market.getFactionId());
			addFighters(fighters, fighters + 2, 0, market.getFactionId());
			

            final EconomyEngine engine = EconomyEngine.getInstance(); 
            final CommodityStats shipsStats = engine.getComStats(Commodities.SHIPS, market.getId());
            final CommodityStats fuelStats  = engine.getComStats(Commodities.FUEL,  market.getId());

			getCargo().getMothballedShips().clear();

            final float shipProd = (float) Math.log10(Math.max(1f, shipsStats.getAvailable()));
            final float fuelProd = (float) Math.log10(Math.max(1f, fuelStats.getAvailable()));

            final float combatShips = Math.min(10f + 5f * shipProd, 70);
            final float freighters  = Math.min(10f + 10f * shipProd, 40f);
            final float tankers     = Math.min(10f + 10f * fuelProd, 50f);
            final float transports  = 10f + 2f * shipProd + 1f * fuelProd;
            final float liners      = 5f + 2f * shipProd;
            final float utilityPts  = 5f + 1f * shipProd + 1f * fuelProd;
			
			addShips(market.getFactionId(),
                10f + extraShips,
                freighters,
                0f, // tanker
                transports,
                liners,
                utilityPts,
                null, // qualityOverride
                0f, // qualityMod
                ShipPickMode.PRIORITY_THEN_ALL,
                null
            );
			
			addShips(market.getFactionId(),
                combatShips,
                0f, // freighter 
                0f, // tanker
                0f, // transport
                0f, // liner
                0f, // utilityPts
                null, // qualityOverride
                -1f, // qualityMod
                null,
                null,
                4
            );
			
			addShips(market.getFactionId(),
                0f, // combat
                0f, // freighter 
                tankers, // tanker
                0f, // transport
                0f, // liner
                0f, // utilityPts
                null, // qualityOverride
                0f, // qualityMod
                ShipPickMode.PRIORITY_THEN_ALL,
                null
            );
			addHullMods(1, 1 + itemGenRandom.nextInt(3), market.getFactionId());
		}
		
		getCargo().sort();
	}
	
	protected Object writeReplace() {
		if (okToUpdateShipsAndWeapons()) {
			pruneWeapons(0f);
			getCargo().getMothballedShips().clear();
		}
		return this;
	}
	
	
	public boolean shouldHaveCommodity(CommodityOnMarketAPI com) {
		return !market.isIllegal(com);
	}
	
	@Override
	public int getStockpileLimit(CommodityOnMarketAPI com) {
        final Random random = new Random(
            market.getId().hashCode() +
            submarket.getSpecId().hashCode() +
            Global.getSector().getClock().getMonth() * 170000
        );

        float limit = OpenMarketSubmarketPlugin.getBaseStockpileLimit(com);
		
		limit *= 0.9f + 0.2f * random.nextFloat();
		
		final float sm = market.getStabilityValue() / 10f;
		limit *= (0.25f + 0.75f * sm);
		
		return (int) Math.max(0, limit);
	}
	
	public static final int ECON_UNIT_MULT_EXTRA = 9;
	public static final int ECON_UNIT_MULT_PRODUCTION = 4;
	public static final int ECON_UNIT_MULT_IMPORTS = 1;
	public static final int ECON_UNIT_MULT_DEFICIT = 2;
    public static final int ECON_UNIT_MULT_BASE = 3;
	
	public static float getBaseStockpileLimit(CommodityOnMarketAPI com) {
        final CommodityStats stats = EconomyEngine.getInstance().getComStats(
            com.getId(), com.getMarket().getId()
        );
		
		float limit = 0f;
		limit += stats.getTotalImports() * ECON_UNIT_MULT_IMPORTS;
		limit += stats.getLocalProduction(true) * ECON_UNIT_MULT_PRODUCTION;
		limit += stats.getCanNotExport() * ECON_UNIT_MULT_EXTRA;
		limit -= stats.getDeficit() * ECON_UNIT_MULT_DEFICIT;
        limit *= ECON_UNIT_MULT_BASE;

		return (int) Math.max(0f, limit);
	}
	
    @Override
    public void reportPlayerMarketTransaction(PlayerMarketTransaction transaction) {
        if (getTariff() <= 0f) return;

        final float tariffValue = transaction.getCreditValue() * getTariff();

        EconomyEngine.getInstance().addCredits(market.getId(), (int) Math.abs(tariffValue));
    }

	@Override
	public PlayerEconomyImpactMode getPlayerEconomyImpactMode() {
		return PlayerEconomyImpactMode.PLAYER_SELL_ONLY;
	}

	@Override
	public boolean isOpenMarket() {
		return true;
	}

	@Override
	public String getTooltipAppendix(CoreUIAPI ui) {
		if (ui.getTradeMode() == CoreUITradeMode.SNEAK) {
			return "Requires: proper docking authorization (transponder on)";
		}
		return super.getTooltipAppendix(ui);
	}

	@Override
	public Highlights getTooltipAppendixHighlights(CoreUIAPI ui) {
		if (ui.getTradeMode() == CoreUITradeMode.SNEAK) {
			String appendix = getTooltipAppendix(ui);
			if (appendix == null) return null;
			
			Highlights h = new Highlights();
			h.setText(appendix);
			h.setColors(Misc.getNegativeHighlightColor());
			return h;
		}
		return super.getTooltipAppendixHighlights(ui);
	}
}
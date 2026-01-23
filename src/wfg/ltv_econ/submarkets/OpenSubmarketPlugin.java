package wfg.ltv_econ.submarkets;

import static wfg.native_ui.util.UIConstants.negative;

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
import com.fs.starfarer.api.impl.campaign.submarkets.OpenMarketPlugin;
import com.fs.starfarer.api.util.Highlights;
import com.fs.starfarer.api.util.Misc;

import wfg.ltv_econ.economy.CommodityCell;
import wfg.ltv_econ.economy.engine.EconomyEngine;

public class OpenSubmarketPlugin extends BaseSubmarketPlugin {
	
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
            final CommodityCell shipsCell = engine.getComCell(Commodities.SHIPS, market.getId());
            final CommodityCell fuelCell  = engine.getComCell(Commodities.FUEL,  market.getId());

			getCargo().getMothballedShips().clear();

            final float shipProd = (float) Math.log10(Math.max(1f, shipsCell.getFlowAvailable()));
            final float fuelProd = (float) Math.log10(Math.max(1f, fuelCell.getFlowAvailable()));

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

        float limit = OpenSubmarketPlugin.getBaseStockpileLimit(com, market.getId());
		
		limit *= 0.9f + 0.2f * random.nextFloat();
		
		final float sm = market.getStabilityValue() / 10f;
		limit *= (0.25f + 0.75f * sm);
		
		return (int) Math.max(0f, limit);
	}
	
    public static final float ECON_UNIT_MULT_BASE = 0.4f;
	public static final float ECON_UNIT_MULT_EXTRA = 2f;
	public static final float ECON_UNIT_MULT_PRODUCTION = 1.5f;
	public static final float ECON_UNIT_MULT_IMPORTS = 1.2f;
	public static final float ECON_UNIT_MULT_DEFICIT = 1.3f;

	private static final float STOCKPILE_BASELINE = 900f;
	private static final float RATIO_EXP = 0.6f;
	private static final float STOCKPILE_SCALE_MIN = 0.01f;
	private static final float STOCKPILE_SCALE_MAX = 4f;

	public static float getBaseStockpileLimit(CommodityOnMarketAPI com, String marketID) {
		if (!EconomyEngine.isInitialized()) {
			return (int) OpenMarketPlugin.getBaseStockpileLimit(com);
		}
		return getBaseStockpileLimit(com.getId(), marketID);
	}
	
	public static float getBaseStockpileLimit(String comID, String marketID) {
        final CommodityCell cell = EconomyEngine.getInstance().getComCell(
            comID, marketID
        );

		final float base = Math.max(cell.getFlowAvailable(), cell.getBaseDemand(true));

		final float impRatio = cell.getTotalImports(true) / base;
		final float prodRatio = cell.getProduction(true) / base;
		final float extraRatio = cell.getFlowCanNotExport() / base;
		final float defRatio = cell.getFlowDeficit() / base;

		final float mult = 1f
			+ impRatio  * ECON_UNIT_MULT_IMPORTS
			+ prodRatio * ECON_UNIT_MULT_PRODUCTION
			+ extraRatio * ECON_UNIT_MULT_EXTRA
			- defRatio  * ECON_UNIT_MULT_DEFICIT
		;

		final float baseLinear = base * ECON_UNIT_MULT_BASE * mult;

		final float ratio = STOCKPILE_BASELINE / base;
		float scale = (float) Math.pow(ratio, RATIO_EXP);

		if (scale < STOCKPILE_SCALE_MIN) scale = STOCKPILE_SCALE_MIN;
		if (scale > STOCKPILE_SCALE_MAX) scale = STOCKPILE_SCALE_MAX;

		final float finalLimit = Math.max(0f, baseLinear * scale);
		return (int) Math.min(finalLimit, cell.getStored() * CommodityCell.MAX_SUBMARKET_STOCK_MULT);
	}
	
    @Override
    public void reportPlayerMarketTransaction(PlayerMarketTransaction transaction) {
		super.reportPlayerMarketTransaction(transaction);
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
			h.setColors(negative);
			return h;
		}
		return super.getTooltipAppendixHighlights(ui);
	}
}
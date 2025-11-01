package wfg.ltv_econ.submarkets;

import java.util.HashMap;
import java.util.Map;

import com.fs.starfarer.api.campaign.CargoAPI;
import com.fs.starfarer.api.campaign.CargoStackAPI;
import com.fs.starfarer.api.campaign.CoreUIAPI;
import com.fs.starfarer.api.campaign.PlayerMarketTransaction;
import com.fs.starfarer.api.campaign.CargoAPI.CargoItemType;
import com.fs.starfarer.api.campaign.econ.CommodityOnMarketAPI;
import com.fs.starfarer.api.campaign.econ.CommoditySpecAPI;
import com.fs.starfarer.api.campaign.econ.SubmarketAPI;
import com.fs.starfarer.api.combat.MutableStat;
import com.fs.starfarer.api.impl.campaign.ids.Commodities;
import com.fs.starfarer.api.impl.campaign.submarkets.BaseSubmarketPlugin;
import com.fs.starfarer.api.ui.Alignment;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;

import wfg.ltv_econ.economy.CommodityStats;
import wfg.ltv_econ.economy.EconomyEngine;
import wfg.wrap_ui.util.NumFormat;

public class StockpilesSubmarketPlugin extends BaseSubmarketPlugin {

	protected Map<String, MutableStat> stockpilingBonus = new HashMap<String, MutableStat>();

	public StockpilesSubmarketPlugin() {}

	public void init(SubmarketAPI submarket) {
		super.init(submarket);
	}

	public boolean showInFleetScreen() {
		return false;
	}

	public boolean showInCargoScreen() {
		return true;
	}

	public boolean isEnabled(CoreUIAPI ui) {
		return true;
	}

	@Override
	public void advance(float amount) {
		super.advance(amount);
	}

	public boolean shouldHaveCommodity(CommodityOnMarketAPI commodity) {
		if (commodity.isNonEcon() || commodity.isMeta()) return false;
		return true;
	}

	@Override
	public boolean isIllegalOnSubmarket(CargoStackAPI stack, TransferAction action) {
		if (stack.getCommodityId() == null || stack.getType() != CargoItemType.RESOURCES ||
			stack.getResourceIfResource().hasTag(Commodities.TAG_NON_ECONOMIC)
		) {
			return true;
		}

		return false;
	}

	@Override
	public String getIllegalTransferText(CargoStackAPI stack, TransferAction action) {
		return "Can only store commodities used by the economy";
	}

	@Override
	public int getStockpileLimit(CommodityOnMarketAPI com) {
		return 50000;
	}

	@Override
	public float getStockpilingAddRateMult(CommodityOnMarketAPI com) {
		return 0f;
	}

	@Override
	public boolean isParticipatesInEconomy() {
		return false;
	}

	@Override
	public boolean isHidden() {
		return !market.isPlayerOwned();
	}

	public float getTariff() {
		return 0f;
	}

	@Override
	public boolean isFreeTransfer() {
		return true;
	}

	protected transient CargoAPI preTransactionCargoCopy = null;

	public void updateCargoPrePlayerInteraction() {
		final EconomyEngine engine = EconomyEngine.getInstance();
		final String marketId = market.getId();

		getCargo().clear();

		for (CommoditySpecAPI spec : EconomyEngine.getEconCommodities()) {
			if (spec.isMeta()) continue;
			final String comID = spec.getId();

			final CommodityStats stats = engine.getComStats(comID, marketId);
			final float stored = stats.getStored();
			final float limit = getStockpileLimit(null);

			final float displayAmount = Math.min(limit, stored);
			if (displayAmount > 1f) {
				cargo.addCommodity(comID, displayAmount);
			}
		}

		cargo.sort();
	}

	protected Object readResolve() {
		super.readResolve();

		return this;
	}

	@Override
	public void reportPlayerMarketTransaction(PlayerMarketTransaction transaction) {
		final EconomyEngine engine = EconomyEngine.getInstance();
		final String marketId = market.getId();

		for (CargoStackAPI stack : transaction.getSold().getStacksCopy()) {
			String comId = stack.getCommodityId();
			int amount = (int) stack.getSize();

			CommodityStats stats = engine.getComStats(comId, marketId);
			stats.addStoredAmount(amount);
		}

		for (CargoStackAPI stack : transaction.getBought().getStacksCopy()) {
			String comId = stack.getCommodityId();
			int amount = (int) stack.getSize();

			CommodityStats stats = engine.getComStats(comId, marketId);
			stats.addStoredAmount(-amount);
		}
	}

	@Override
	public String getBuyVerb() {
		return "Take";
	}

	@Override
	public String getSellVerb() {
		return "Give";
	}

	public String getTotalTextOverride() {
		return "Transfer";
	}

	public String getTotalValueOverride() {
		return "";
	}

	public boolean isTooltipExpandable() {
		return false;
	}

	public float getTooltipWidth() {
		return 500f;
	}

	protected void createTooltipAfterDescription(TooltipMakerAPI tooltip, boolean expanded) {
		final float opad = 10;

		tooltip.addSectionHeading("Player Interaction", market.getFaction().getBaseUIColor(),
			market.getFaction().getDarkUIColor(), Alignment.MID, opad);

		tooltip.addPara(
			"Commodities can be deposited into or withdrawn from the stockpiles. " +
			"The displayed cargo shows up to the stockpile limit of %s or the amount currently stored, whichever is lower. " +
			"Contributions update the colony's reserves immediately, and the visible cargo reflects the current stockpile limits.",
			opad,
			Misc.getHighlightColor(),
			NumFormat.engNotation(getStockpileLimit(null))
		);
	}
}
package wfg.ltv_econ.submarket;

import com.fs.starfarer.api.campaign.CargoStackAPI;
import com.fs.starfarer.api.campaign.CoreUIAPI;
import com.fs.starfarer.api.campaign.PlayerMarketTransaction;
import com.fs.starfarer.api.campaign.CargoAPI.CargoItemType;
import com.fs.starfarer.api.campaign.econ.CommodityOnMarketAPI;
import com.fs.starfarer.api.campaign.econ.CommoditySpecAPI;
import com.fs.starfarer.api.impl.campaign.DebugFlags;
import com.fs.starfarer.api.impl.campaign.ids.Commodities;
import com.fs.starfarer.api.impl.campaign.submarkets.BaseSubmarketPlugin;
import com.fs.starfarer.api.ui.Alignment;
import com.fs.starfarer.api.ui.TooltipMakerAPI;

import wfg.ltv_econ.constant.EconomyConstants;
import wfg.ltv_econ.economy.commodity.CommodityCell;
import wfg.ltv_econ.economy.engine.EconomyEngine;
import wfg.native_ui.util.NumFormat;

import static wfg.ltv_econ.constant.strings.LocalizedStrings.*;
import static wfg.native_ui.util.UIConstants.*;

public class StockpilesSubmarketPlugin extends BaseSubmarketPlugin {

	public StockpilesSubmarketPlugin() {}

    @Override
	public boolean showInFleetScreen() {
		return false;
	}

    @Override
	public boolean showInCargoScreen() {
		return true;
	}

    @Override
	public boolean isEnabled(CoreUIAPI ui) {
		return true;
	}

    @Override
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
		return str("submarketEconCommodityOnlyTxt");
	}

	@Override
	public int getStockpileLimit(CommodityOnMarketAPI com) {
		return 50000;
	}

	@Override
	public float getStockpilingAddRateMult(CommodityOnMarketAPI com) {
		return 0f;
	}

    @Override public boolean isParticipatesInEconomy() { return false; }
	@Override public boolean isHidden() { return !market.isPlayerOwned() && !DebugFlags.COLONY_DEBUG; }
	@Override public float getTariff() { return 0f; }
	@Override public boolean isFreeTransfer() { return true; }

	public void updateCargoPrePlayerInteraction() {
		final EconomyEngine engine = EconomyEngine.instance();
		final String marketId = market.getId();

		getCargo().clear();

		for (CommoditySpecAPI spec : EconomyConstants.econCommoditySpecs) {
			if (spec.isMeta()) continue;
			final String comID = spec.getId();

			final CommodityCell cell = engine.getComCell(comID, marketId);

			final float displayAmount = Math.min(cell.getRoundedStored(), getStockpileLimit(null));
			if (displayAmount > 1f) {
				cargo.addCommodity(comID, displayAmount);
			}
		}

		cargo.sort();
	}

	@Override
	public void reportPlayerMarketTransaction(PlayerMarketTransaction transaction) {
		final EconomyEngine engine = EconomyEngine.instance();
		final String marketID = market.getId();

		for (CargoStackAPI stack : transaction.getSold().getStacksCopy()) {
			final CommodityCell cell = engine.getComCell(stack.getCommodityId(), marketID);
			cell.addStoredAmount(stack.getSize());
		}

		for (CargoStackAPI stack : transaction.getBought().getStacksCopy()) {
			final CommodityCell cell = engine.getComCell(stack.getCommodityId(), marketID);
			cell.addStoredAmount(-stack.getSize());
		}
	}

	@Override public String getBuyVerb() { return str("submarketTakeTxt"); }
	@Override public String getSellVerb() { return str("submarketGiveTxt"); }
	@Override public String getTotalTextOverride() { return str("submarketTransferTxt"); }
	@Override public String getTotalValueOverride() { return ""; }
	@Override public float getTooltipWidth() { return 500f;}

    @Override
	protected void createTooltipAfterDescription(TooltipMakerAPI tooltip, boolean expanded) {
		tooltip.addSectionHeading(str("uiTpTitlePlayerInteraction"), market.getFaction().getBaseUIColor(),
			market.getFaction().getDarkUIColor(), Alignment.MID, opad);

		tooltip.addPara(str("submarketStockpilesTpTxt1"), opad, highlight, NumFormat.engNotate(getStockpileLimit(null)));
	}
}
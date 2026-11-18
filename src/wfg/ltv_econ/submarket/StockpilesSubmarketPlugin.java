package wfg.ltv_econ.submarket;

import com.fs.starfarer.api.campaign.CargoAPI;
import com.fs.starfarer.api.campaign.CargoStackAPI;
import com.fs.starfarer.api.campaign.CoreUIAPI;
import com.fs.starfarer.api.campaign.PlayerMarketTransaction;
import com.fs.starfarer.api.campaign.CargoAPI.CargoItemType;
import com.fs.starfarer.api.campaign.econ.CommodityOnMarketAPI;
import com.fs.starfarer.api.campaign.econ.CommoditySpecAPI;
import com.fs.starfarer.api.campaign.econ.SubmarketAPI;
import com.fs.starfarer.api.impl.campaign.DebugFlags;
import com.fs.starfarer.api.impl.campaign.ids.Commodities;
import com.fs.starfarer.api.impl.campaign.submarkets.BaseSubmarketPlugin;
import com.fs.starfarer.api.ui.Alignment;
import com.fs.starfarer.api.ui.TooltipMakerAPI;

import wfg.ltv_econ.constant.EconomyConstants;
import wfg.ltv_econ.economy.commodity.CommodityCell;
import wfg.ltv_econ.economy.engine.EconomyEngine;
import wfg.ltv_econ.economy.registry.MarketFinanceRegistry;
import wfg.native_ui.util.NumFormat;

import static wfg.ltv_econ.constant.strings.LocalizedStrings.*;
import static wfg.ltv_econ.constant.strings.Income.*;
import static wfg.native_ui.util.UIConstants.*;

public final class StockpilesSubmarketPlugin extends BaseSubmarketPlugin {

    private static final int STOCKPILE_LIMIT = 50_000;
    private static final int STOCK_RECOVERY_DAYS = 90;
    private static final float DAILY_STOCK_RECOVERED = (float) STOCKPILE_LIMIT / (float) STOCK_RECOVERY_DAYS;

	public StockpilesSubmarketPlugin() {};

    @Override
    public void init(SubmarketAPI submarket) {
        super.init(submarket);
        sinceLastCargoUpdate = 0f;
    }

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
		return shouldHaveCommodity(commodity.getCommodity());
	}

    private static final boolean shouldHaveCommodity(CommoditySpecAPI com) {
        if (com.isNonEcon() || com.isMeta()) return false;
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
		return STOCKPILE_LIMIT;
	}

	@Override
	public float getStockpilingAddRateMult(CommodityOnMarketAPI com) {
		return 0f;
	}

    @Override public boolean isParticipatesInEconomy() { return false; }
	@Override public boolean isHidden() { return !market.isPlayerOwned() && !DebugFlags.COLONY_DEBUG; }
	@Override public float getTariff() { return 0f; }
	@Override public boolean isFreeTransfer() { return true; }

    @Override
	public void updateCargoPrePlayerInteraction() {
		final EconomyEngine engine = EconomyEngine.instance();
		final String marketId = market.getId();
        final CargoAPI cargo = getCargo();

        final float addAmount = DAILY_STOCK_RECOVERED * sinceLastCargoUpdate;
        sinceLastCargoUpdate = 0f;

		for (CommoditySpecAPI spec : EconomyConstants.econCommoditySpecs) {
			if (!shouldHaveCommodity(spec)) continue;
			final String comID = spec.getId();
			final CommodityCell cell = engine.getComCell(comID, marketId);

            final float oldAmount = cargo.getCommodityQuantity(comID);
            final float updatedAmount = Math.min(cell.getRoundedStored(), Math.min(STOCKPILE_LIMIT, oldAmount + addAmount));
            final float inputAmount = updatedAmount - oldAmount;

            if (inputAmount < 0f) {
                cargo.removeCommodity(comID, Math.abs(inputAmount));
            } else {
                cargo.addCommodity(comID, inputAmount);
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

            final float premium = Math.max(0f, cell.mSpec.getBasePrice() - cell.mSpec.getUnmodifiedBasePrice());
            if (premium > 0f) {
                MarketFinanceRegistry.instance().getLedger(marketID).add(
                    STOCKPILE_MARKUP_KEY, premium * stack.getSize(), STOCKPILE_MARKUP_DESC
                );
            }

            cell.addStoredAmount(stack.getSize());
        }

        for (CargoStackAPI stack : transaction.getBought().getStacksCopy()) {
            final CommodityCell cell = engine.getComCell(stack.getCommodityId(), marketID);

            final float premium = Math.max(0f, cell.mSpec.getBasePrice() - cell.mSpec.getUnmodifiedBasePrice());
            if (premium > 0f) {
                MarketFinanceRegistry.instance().getLedger(marketID).add(
                    STOCKPILE_MARKUP_KEY, -premium * stack.getSize(), STOCKPILE_MARKUP_DESC
                );
            }

            cell.addStoredAmount(-stack.getSize());
        }
    }

	@Override public String getBuyVerb() { return str("submarketTakeTxt"); }
	@Override public String getSellVerb() { return str("submarketGiveTxt"); }
	@Override public String getTotalTextOverride() { return str("submarketTransferTxt"); }
	@Override public String getTotalValueOverride() { return ""; }
    @Override public String getTariffTextOverride() { return str("submarketStockpileChargeTxt"); }
	@Override public float getTooltipWidth() { return 500f; }

    @Override
	protected void createTooltipAfterDescription(TooltipMakerAPI tooltip, boolean expanded) {
		tooltip.addSectionHeading(str("uiTpTitlePlayerInteraction"), market.getFaction().getBaseUIColor(),
			market.getFaction().getDarkUIColor(), Alignment.MID, opad);

		tooltip.addPara(str("submarketStockpilesTpTxt1"), opad, highlight, NumFormat.engNotate(STOCKPILE_LIMIT));
	}
}
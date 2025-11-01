package wfg.ltv_econ.submarkets;

import java.util.HashMap;
import java.util.Map;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CargoAPI;
import com.fs.starfarer.api.campaign.CargoStackAPI;
import com.fs.starfarer.api.campaign.CoreUIAPI;
import com.fs.starfarer.api.campaign.PlayerMarketTransaction;
import com.fs.starfarer.api.campaign.CargoAPI.CargoItemType;
import com.fs.starfarer.api.campaign.econ.CommodityOnMarketAPI;
import com.fs.starfarer.api.campaign.econ.SubmarketAPI;
import com.fs.starfarer.api.combat.MutableStat;
import com.fs.starfarer.api.impl.campaign.ids.Commodities;
import com.fs.starfarer.api.impl.campaign.ids.Strings;
import com.fs.starfarer.api.impl.campaign.submarkets.BaseSubmarketPlugin;
import com.fs.starfarer.api.ui.Alignment;
import com.fs.starfarer.api.ui.TooltipMakerAPI;

import wfg.ltv_econ.economy.CommodityStats;
import wfg.ltv_econ.economy.EconomyEngine;

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

	private float daysSinceLastUpdate = -1;

	@Override
	public void advance(float amount) {
		super.advance(amount);
		final int day = Global.getSector().getClock().getDay();

		if (daysSinceLastUpdate == day) return;

		daysSinceLastUpdate = day;
	}

	public boolean shouldHaveCommodity(CommodityOnMarketAPI commodity) {
		if (commodity.isNonEcon()) return false;
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
		return 100000;
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
		preTransactionCargoCopy = getCargo().createCopy();
		preTransactionCargoCopy.sort();
		getCargo().sort();
	}

	public void reportPlayerMarketTransaction(PlayerMarketTransaction transaction) {
		sinceLastCargoUpdate = 0f;

		preTransactionCargoCopy = getCargo().createCopy();
		preTransactionCargoCopy.sort();
	}

	protected Object readResolve() {
		super.readResolve();

		return this;
	}

	public void absorbCargoIntoStockpiles() {
		final EconomyEngine engine = EconomyEngine.getInstance();
		for (String comID : EconomyEngine.getEconCommodityIDs()) {
			final CommodityStats stats = engine.getComStats(comID, market.getId());
			final int amount = (int) cargo.getCommodityQuantity(comID);
			if (amount < 1) continue;
			
			stats.addStoredAmount(amount);
			cargo.removeCommodity(comID, amount);
		}
	}

	@Override
	public String getBuyVerb() {
		return "Take";
	}

	@Override
	public String getSellVerb() {
		return "Leave";
	}

	public String getTotalTextOverride() {
		return "Now AAAAAA";
	}

	public String getTotalValueOverride() {
		return "0" + Strings.C;
	}

	public boolean isTooltipExpandable() {
		return false;
	}

	public float getTooltipWidth() {
		return 500f;
	}

	protected void createTooltipAfterDescription(TooltipMakerAPI tooltip, boolean expanded) {
		final float opad = 10;

		tooltip.addPara(
			"Colony Stockpiles represent the resources held by this colony that are absorbed into the economy at the end of each day. " +
			"Any goods placed into this submarket from your cargo will be added to the colony's stored resources automatically, " +
			"making them available for the industries." +
			"\nResources added to the stockpiles are effectively removed from your personal cargo but remain in the economy for use by the colony." +
			"They can help cover shortages, supply production, and support other colony operations.",
			opad
		);

		tooltip.addSectionHeading("Player Interaction", market.getFaction().getBaseUIColor(),
			market.getFaction().getDarkUIColor(), Alignment.MID, opad);

		tooltip.addPara("You cannot directly take resources back from Colony Stockpiles. Contributions are one-way, intended to strengthen the colony's economy. " +
			"Use this submarket to safely transfer excess goods from your cargo to the colony without risking loss or theft.", opad);
	}
}
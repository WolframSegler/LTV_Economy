package wfg.ltv_econ.submarket;

import static wfg.ltv_econ.constant.strings.Income.PLAYER_MARKET_TRANSACTION_KEY;
import static wfg.ltv_econ.constant.strings.Income.getDesc;
import static wfg.native_ui.util.Globals.settings;

import java.util.Random;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignUIAPI.CoreUITradeMode;
import com.fs.starfarer.api.campaign.CargoStackAPI;
import com.fs.starfarer.api.campaign.CoreUIAPI;
import com.fs.starfarer.api.campaign.PlayerMarketTransaction;
import com.fs.starfarer.api.campaign.econ.CommodityOnMarketAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.campaign.econ.SubmarketAPI;
import com.fs.starfarer.api.impl.campaign.ids.Commodities;
import com.fs.starfarer.api.impl.campaign.submarkets.OpenMarketPlugin;

import wfg.ltv_econ.config.EconConfig;
import wfg.ltv_econ.constant.strings.LocalizedStrings;
import wfg.ltv_econ.economy.commodity.CommodityCell;
import wfg.ltv_econ.economy.engine.EconomyEngine;
import wfg.ltv_econ.economy.registry.MarketFinanceRegistry;
import wfg.ltv_econ.serializable.LtvEconSaveData;
import wfg.native_ui.util.Arithmetic;

public class OpenSubmarketPlugin extends OpenMarketPlugin {

    @Override 
    public void updateCargoPrePlayerInteraction() {
        final EconomyEngine engine = EconomyEngine.instance();
        final CommodityCell shipsCell = engine.getOrCreateComCell(Commodities.SHIPS, market);
        final CommodityCell fuelCell = engine.getOrCreateComCell(Commodities.FUEL, market);
        final CommodityOnMarketAPI shipsCom = market.getCommodityData(Commodities.SHIPS);
        final CommodityOnMarketAPI fuelCom = market.getCommodityData(Commodities.FUEL);

        final int originalShipsMaxSupply = shipsCom.getMaxSupply();
        final int originalFuelMaxSupply = fuelCom.getMaxSupply();
        final int shipsInflow = (int) Math.round(Math.log10(Math.max(1f, getInflowQuantumNormalized(shipsCell))));
        final int fuelInflow = (int) Math.round(Math.log10(Math.max(1f, getInflowQuantumNormalized(fuelCell))));

        shipsCom.setMaxSupply(shipsInflow);
        fuelCom.setMaxSupply(fuelInflow);
        try {
            super.updateCargoPrePlayerInteraction();
        } finally {
            shipsCom.setMaxSupply(originalShipsMaxSupply);
            fuelCom.setMaxSupply(originalFuelMaxSupply);
        }
    }
		
	@Override
	public int getStockpileLimit(CommodityOnMarketAPI com) {
		return getStockpileLimit(com, market, submarket, false);
	}

    public static final int getStockpileLimit(
        CommodityOnMarketAPI com, MarketAPI market,
        SubmarketAPI submarket, boolean invertStability
    ) {
        final Random random = new Random(
            market.getId().hashCode() +
            submarket.getSpecId().hashCode() +
            Global.getSector().getClock().getMonth() * 170000
        );

        final float limit = getBaseStockpileLimit(com, market.getId());
        final float randFactor = 0.9f + 0.2f * random.nextFloat();
        final float stabilityRaw = invertStability ?
            1f - market.getStabilityValue() / 10f :
            market.getStabilityValue() / 10f;
        final float stabilityFactor = 0.25f + 0.75f * stabilityRaw;

        return (int) Math.max(0f, limit * randFactor * stabilityFactor);
    }

	public static final float getBaseStockpileLimit(CommodityOnMarketAPI com, String marketID) {
		if (!LtvEconSaveData.isInitialized() || EconomyEngine.instance().getComCell(com.getId(), marketID) == null) {
			return OpenMarketPlugin.getBaseStockpileLimit(com);
		}
		return getBaseStockpileLimit(com.getId(), marketID);
	}

    public static final float LTV_ECON_UNIT_MULT_BASE = 0.4f;
	public static final float LTV_ECON_UNIT_MULT_EXTRA = 2f;
	public static final float LTV_ECON_UNIT_MULT_PRODUCTION = 1.5f;
	public static final float LTV_ECON_UNIT_MULT_IMPORTS = 1.2f;
	public static final float LTV_ECON_UNIT_MULT_DEFICIT = 0.8f;

	private static final float STOCKPILE_BASELINE = 800f;
	private static final float RATIO_EXP = 0.5f;
	private static final float STOCKPILE_SCALE_MIN = 0.01f;
	private static final float STOCKPILE_SCALE_MAX = 4f;
	
	public static final float getBaseStockpileLimit(String comID, String marketID) {
		final EconomyEngine engine = EconomyEngine.instance();
        final CommodityCell cell = engine.getComCell(
            comID, marketID
        );

		final float prod = cell.getProduction(true);
        final float target = cell.getTargetQuantum(true);
		final float imports = cell.getTotalImports() / EconConfig.TRADE_INTERVAL;

        final float demandMetLocally = Math.min(prod, target);
        final float demandMetViaTrade = (float) Math.min(imports, target - demandMetLocally);
        final float demandUnmet = target - demandMetLocally - demandMetViaTrade;

		final float base = Math.max(1f, Math.max(prod + imports, target));

		final float impRatio = imports / base;
		final float prodRatio = prod / base;
		final float extraRatio = Math.max(0f, prod - target) / base;
		final float defRatio = demandUnmet / base;

		final float mult = 1f
			+ impRatio  * LTV_ECON_UNIT_MULT_IMPORTS
			+ prodRatio * LTV_ECON_UNIT_MULT_PRODUCTION
			+ extraRatio * LTV_ECON_UNIT_MULT_EXTRA
			- defRatio  * LTV_ECON_UNIT_MULT_DEFICIT
		;

		final float baseLinear = Math.max(0f, base * LTV_ECON_UNIT_MULT_BASE * mult);
		if (baseLinear <= 0f) return 0f;

		final float ratio = STOCKPILE_BASELINE / baseLinear;
		final float scale = (float) Arithmetic.clamp(Math.pow(ratio, RATIO_EXP),
			STOCKPILE_SCALE_MIN, STOCKPILE_SCALE_MAX
		);

		return (int) Math.max(0f, baseLinear * scale);
	}
	
    @Override
    public void reportPlayerMarketTransaction(PlayerMarketTransaction transaction) {
		super.reportPlayerMarketTransaction(transaction);
		final EconomyEngine engine = EconomyEngine.instance();
		final String marketID = market.getId();

		if (market.isPlayerOwned()) return;

        if (getTariff() > 0f) {
			final float tariffValue = transaction.getCreditValue() * getTariff();

			MarketFinanceRegistry.instance().getLedger(market.getId()).add(
				PLAYER_MARKET_TRANSACTION_KEY, Math.abs(tariffValue), getDesc(PLAYER_MARKET_TRANSACTION_KEY)
			);
		}

		for (CargoStackAPI stack : transaction.getSold().getStacksCopy()) {
			final var spec = settings.getCommoditySpec(stack.getCommodityId());
			if (spec == null || spec.isNonEcon()) continue;

			final CommodityCell cell = engine.getComCell(stack.getCommodityId(), marketID);
			cell.addStoredAmount(stack.getSize() * EconConfig.OPEN_MARKET_TO_STOCKPILES_RATIO);
		}
    }

	@Override
	public String getTooltipAppendix(CoreUIAPI ui) {
		if (ui.getTradeMode() == CoreUITradeMode.SNEAK) {
			return LocalizedStrings.str("submarketProperDockingTransponderTxt");
		}
		return super.getTooltipAppendix(ui);
	}

	private static final float getInflowQuantumNormalized(final CommodityCell cell) {
		return cell.getProduction(true) + cell.informalImports + (cell.inFactionImports + cell.globalImports) / EconConfig.TRADE_INTERVAL;
	}
}
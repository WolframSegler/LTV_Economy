package wfg.ltv_econ.submarkets;

import java.util.Random;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CargoAPI;
import com.fs.starfarer.api.campaign.CargoStackAPI;
import com.fs.starfarer.api.campaign.CoreUIAPI;
import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.campaign.FactionDoctrineAPI;
import com.fs.starfarer.api.campaign.PlayerMarketTransaction;
import com.fs.starfarer.api.campaign.SpecialItemPlugin;
import com.fs.starfarer.api.campaign.econ.CommodityOnMarketAPI;
import com.fs.starfarer.api.campaign.econ.SubmarketAPI;
import com.fs.starfarer.api.campaign.impl.items.BlueprintProviderItem;
import com.fs.starfarer.api.impl.campaign.CoreCampaignPluginImpl;
import com.fs.starfarer.api.impl.campaign.DelayedBlueprintLearnScript;
import com.fs.starfarer.api.impl.campaign.ids.Factions;
import com.fs.starfarer.api.impl.campaign.submarkets.BaseSubmarketPlugin;
import com.fs.starfarer.api.util.Highlights;
import com.fs.starfarer.api.util.Misc;
import com.fs.starfarer.api.util.WeightedRandomPicker;

public class BlackSubmarketPlugin extends BaseSubmarketPlugin {
	
	public void init(SubmarketAPI submarket) {
		super.init(submarket);
	}

	public void updateCargoPrePlayerInteraction() {
		final float seconds = Global.getSector().getClock().convertToSeconds(sinceLastCargoUpdate);
		addAndRemoveStockpiledResources(seconds, false, true, true);
		sinceLastCargoUpdate = 0f;

		
		if (okToUpdateShipsAndWeapons()) {
			sinceSWUpdate = 0f;
			final float stability = market.getStabilityValue();
			final boolean military = Misc.isMilitary(market);
			
			pruneWeapons(0f);
			
			final WeightedRandomPicker<String> factionPicker = new WeightedRandomPicker<String>();
			factionPicker.add(market.getFactionId(), 15f - stability);
			factionPicker.add(Factions.INDEPENDENT, 4f);
			factionPicker.add(submarket.getFaction().getId(), 6f);
			
			int weapons = 6 + Math.max(0, market.getSize() - 1);
			int fighters = 2 + Math.max(0, (market.getSize() - 3) / 2);
			
			addWeapons(weapons, weapons + 2, 3, factionPicker);
			addFighters(fighters, fighters + 2, 3, factionPicker);
			
			if (military) {
				weapons = market.getSize();
				fighters = Math.max(1, market.getSize() / 3);
				addWeapons(weapons, weapons + 2, 3, market.getFactionId(), false);
				addFighters(fighters, fighters + 2, 3, market.getFactionId());
			}
			
			final float sMult = 0.5f + Math.max(0, (1f - stability / 10f)) * 0.5f;
			final float pOther = 0.1f;

			getCargo().getMothballedShips().clear();
			
			final FactionDoctrineAPI doctrine = market.getFaction().getDoctrine().clone();
			if (doctrine.getWarships() > 0) {
				doctrine.setWarships(Math.max(2, doctrine.getWarships()));
			}
			if (doctrine.getCarriers() > 0) {
				doctrine.setCarriers(Math.max(2, doctrine.getCarriers()));
			}
			if (doctrine.getPhaseShips() > 0) {
				doctrine.setPhaseShips(Math.max(2, doctrine.getPhaseShips()));
			}
			
			addShips(market.getFactionId(),
                70f * sMult, // combat
                itemGenRandom.nextFloat() > pOther ? 0f : 10f, // freighter 
                itemGenRandom.nextFloat() > pOther ? 0f : 10f, // tanker
                itemGenRandom.nextFloat() > pOther ? 0f : 10f, // transport
                itemGenRandom.nextFloat() > pOther ? 0f : 10f, // liner
                itemGenRandom.nextFloat() > pOther ? 0f : 10f, // utilityPts
                null,
                0f, // qualityMod
                null,
                doctrine
            );
			final FactionDoctrineAPI doctrineOverride = submarket.getFaction().getDoctrine().clone();
			doctrineOverride.setWarships(3);
			doctrineOverride.setPhaseShips(2);
			doctrineOverride.setCarriers(2);
			doctrineOverride.setCombatFreighterProbability(1f);
			doctrineOverride.setShipSize(5);
			addShips(submarket.getFaction().getId(),
                70f, // combat
                10f, // freighter 
                itemGenRandom.nextFloat() > pOther ? 0f : 10f, // tanker
                itemGenRandom.nextFloat() > pOther ? 0f : 10f, // transport
                itemGenRandom.nextFloat() > pOther ? 0f : 10f, // liner
                itemGenRandom.nextFloat() > pOther ? 0f : 10f, // utilityPts
                Math.min(1f, Misc.getShipQuality(market, market.getFactionId()) + 0.5f),
                0f, // qualityMod
                null,
                doctrineOverride,
                3 // no capital ships, max size cruiser
			);
			addShips(Factions.INDEPENDENT,
                15f + 15f * sMult, // combat
                itemGenRandom.nextFloat() > pOther ? 0f : 10f, // freighter 
                itemGenRandom.nextFloat() > pOther ? 0f : 10f, // tanker
                itemGenRandom.nextFloat() > pOther ? 0f : 10f, // transport
                itemGenRandom.nextFloat() > pOther ? 0f : 10f, // liner
                itemGenRandom.nextFloat() > pOther ? 0f : 10f, // utilityPts
                Math.min(1f, Misc.getShipQuality(market, market.getFactionId()) + 0.5f),
                0f, // qualityMod
                null,
                null,
                3 // no capital ships, max size cruiser
			); 
			
			addHullMods(4, 1 + itemGenRandom.nextInt(3));
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
	
	@Override
	public int getStockpileLimit(CommodityOnMarketAPI com) {
        final Random random = new Random(
            market.getId().hashCode() +
            submarket.getSpecId().hashCode() +
            Global.getSector().getClock().getMonth() * 170000
        );

		float limit = OpenSubmarketPlugin.getBaseStockpileLimit(com.getId(), market.getId());
		
		limit *= 0.9f + 0.2f * random.nextFloat();
		
		float sm = 1f - market.getStabilityValue() / 10f;
		limit *= (0.25f + 0.75f * sm);
		
		return (int) Math.max(0f, limit);
	}
	
	@Override
	public PlayerEconomyImpactMode getPlayerEconomyImpactMode() {
		return PlayerEconomyImpactMode.PLAYER_SELL_ONLY;
	}

	@Override
	public void reportPlayerMarketTransaction(PlayerMarketTransaction transaction) {
		super.reportPlayerMarketTransaction(transaction);
		
		FactionAPI faction = submarket.getFaction();
		delayedLearnBlueprintsFromTransaction(faction, getCargo(), transaction, 60f + 60 * (float) Math.random());
	}
	
	public static void delayedLearnBlueprintsFromTransaction(FactionAPI faction, CargoAPI cargo, PlayerMarketTransaction transaction) {
		delayedLearnBlueprintsFromTransaction(faction, cargo, transaction, 60f + 60 * (float) Math.random());
	}

	public static void delayedLearnBlueprintsFromTransaction(FactionAPI faction, CargoAPI cargo,    
        PlayerMarketTransaction transaction, float daysDelay
    ) { 
		DelayedBlueprintLearnScript script = new DelayedBlueprintLearnScript(faction.getId(), daysDelay);
		for (CargoStackAPI stack : transaction.getSold().getStacksCopy()) {
			SpecialItemPlugin plugin = stack.getPlugin();
			if (plugin instanceof BlueprintProviderItem) {
				BlueprintProviderItem bpi = (BlueprintProviderItem) plugin;
				
				boolean learnedSomething = false;
				if (bpi.getProvidedFighters() != null) {
					for (String id : bpi.getProvidedFighters()) {
						if (faction.knowsFighter(id)) continue;
						script.getFighters().add(id);
						learnedSomething = true;
					}
				}
				if (bpi.getProvidedWeapons() != null) {
					for (String id : bpi.getProvidedWeapons()) {
						if (faction.knowsWeapon(id)) continue;
						script.getWeapons().add(id);
						learnedSomething = true;
					}
				}
				if (bpi.getProvidedShips() != null) {
					for (String id : bpi.getProvidedShips()) {
						if (faction.knowsShip(id)) continue;
						script.getShips().add(id);
						learnedSomething = true;
					}
				}
				if (bpi.getProvidedIndustries() != null) {
					for (String id : bpi.getProvidedIndustries()) {
						if (faction.knowsIndustry(id)) continue;
						script.getIndustries().add(id);
						learnedSomething = true;
					}
				}
				
				if (learnedSomething) {
					cargo.removeItems(stack.getType(), stack.getData(), 1);
				}
			}
		}
		
		if (!script.getFighters().isEmpty() || !script.getWeapons().isEmpty() ||
				!script.getShips().isEmpty() || !script.getIndustries().isEmpty()) {
			Global.getSector().addScript(script);
			cargo.sort();
		}
	}


	@Override
	public boolean isIllegalOnSubmarket(CargoStackAPI stack, TransferAction action) {
		return false;
	}

	@Override
	public boolean isIllegalOnSubmarket(String commodityId, TransferAction action) {
		return false;
	}
	
	public float getTariff() {
		return 0f;
	}

	@Override
	public boolean isBlackMarket() {
		return true;
	}
	
	
	public String getTooltipAppendix(CoreUIAPI ui) {
		if (isEnabled(ui)) {
			
			float p = CoreCampaignPluginImpl.computeSmugglingSuspicionLevel(market);
			if (p < 0.05f) return "Suspicion level: none";
			
			if (p < 0.1f) {
				return "Suspicion level: minimal";
			}
			if (p < 0.2f) {
				return "Suspicion level: medium";
			}
			if (p < 0.3f) {
				return "Suspicion level: high";
			}
			if (p < 0.5f) {
				return "Suspicion level: very high";
			}
			return "Suspicion level: extreme";
		}

		return null;
	}
	
	public Highlights getTooltipAppendixHighlights(CoreUIAPI ui) {
		String appendix = getTooltipAppendix(ui);
		if (appendix == null) return null;
		
		Highlights h = new Highlights();
		h.setText(appendix);
		h.setColors(Misc.getNegativeHighlightColor());
		return h;
	}
}
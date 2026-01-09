package wfg.ltv_econ.submarkets;

import static wfg.wrap_ui.util.UIConstants.negative;

import java.util.Random;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignUIAPI.CoreUITradeMode;
import com.fs.starfarer.api.campaign.CargoStackAPI;
import com.fs.starfarer.api.campaign.CoreUIAPI;
import com.fs.starfarer.api.campaign.FactionDoctrineAPI;
import com.fs.starfarer.api.campaign.PlayerMarketTransaction;
import com.fs.starfarer.api.campaign.RepLevel;
import com.fs.starfarer.api.campaign.econ.CommodityOnMarketAPI;
import com.fs.starfarer.api.campaign.econ.SubmarketAPI;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.ids.Commodities;
import com.fs.starfarer.api.impl.campaign.ids.Factions;
import com.fs.starfarer.api.impl.campaign.submarkets.BaseSubmarketPlugin;
import com.fs.starfarer.api.loading.FighterWingSpecAPI;
import com.fs.starfarer.api.loading.WeaponSpecAPI;
import com.fs.starfarer.api.util.Highlights;
import com.fs.starfarer.api.util.Misc;

import wfg.ltv_econ.economy.engine.EconomyEngine;

public class MilitarySubmarketPlugin extends BaseSubmarketPlugin {
	
	public void init(SubmarketAPI submarket) {
		super.init(submarket);
	}

	public void updateCargoPrePlayerInteraction() {
		final float seconds = Global.getSector().getClock().convertToSeconds(sinceLastCargoUpdate);
		addAndRemoveStockpiledResources(seconds, false, true, true);
		sinceLastCargoUpdate = 0f;
		
		if (okToUpdateShipsAndWeapons()) {
			sinceSWUpdate = 0f;
			
			pruneWeapons(0f);
			
			int weapons = 7 + Math.max(0, market.getSize() - 1) * 2;
			int fighters = 2 + Math.max(0, market.getSize() - 3);
			
			addWeapons(weapons, weapons + 2, 3, submarket.getFaction().getId());
			addFighters(fighters, fighters + 2, 3, market.getFactionId());

			float stability = market.getStabilityValue();
			float sMult = Math.max(0.1f, stability / 10f);
			getCargo().getMothballedShips().clear();
			
			// larger ships at lower stability to compensate for the reduced number of ships
			// so that low stability doesn't restrict the options to more or less just frigates 
			// and the occasional destroyer
			int size = submarket.getFaction().getDoctrine().getShipSize();
			int add = 0;
			if (stability <= 4) {
				add = 2;
			} else if (stability <= 6) {
				add = 1;
			}
			
			size += add;
			if (size > 5) size = 5;
			
			FactionDoctrineAPI doctrineOverride = submarket.getFaction().getDoctrine().clone();
			doctrineOverride.setShipSize(size);
			
			addShips(submarket.getFaction().getId(),
					//(150f + market.getSize() * 25f) * sMult, // combat
					200f * sMult, // combat
					15f, // freighter 
					10f, // tanker
					20f, // transport
					10f, // liner
					10f, // utilityPts
					null, // qualityOverride
					0f, // qualityMod
					null,
					doctrineOverride);
				
			addHullMods(4, 2 + itemGenRandom.nextInt(4), submarket.getFaction().getId());
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
    public void reportPlayerMarketTransaction(PlayerMarketTransaction transaction) {
		super.reportPlayerMarketTransaction(transaction);

        EconomyEngine.getInstance().addCredits(market.getId(), (int) -transaction.getCreditValue());
    }

	@Override
	public String getName() {
		if (submarket.getFaction().getId().equals(Factions.LUDDIC_CHURCH)) {
			return "Knights of Ludd";
		}
		return Misc.ucFirst(submarket.getFaction().getPersonNamePrefix()) + "\n" + "Military";
	}

	protected boolean requiresCommission(RepLevel req) {
		if (!submarket.getFaction().getCustomBoolean(Factions.CUSTOM_OFFERS_COMMISSIONS)) return false;
		
		if (req.isAtWorst(RepLevel.WELCOMING)) return true;
		return false;
	}
	
	protected boolean hasCommission() {
		return submarket.getFaction().getId().equals(Misc.getCommissionFactionId());
	}
	
	public boolean shouldHaveCommodity(CommodityOnMarketAPI com) {
		if (Commodities.CREW.equals(com.getId())) return true;
		return com.getCommodity().hasTag(Commodities.TAG_MILITARY);
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
		
		final float sm = market.getStabilityValue() / 10f;
		limit *= (0.25f + 0.75f * sm);
		
		return (int) Math.max(0f, limit);
	}
	
	public boolean isIllegalOnSubmarket(String commodityId, TransferAction action) {
		final boolean illegal = market.isIllegal(commodityId);
		RepLevel req = getRequiredLevelAssumingLegal(commodityId, action);
		
		if (req == null) return illegal;
		
		final RepLevel level = submarket.getFaction().getRelationshipLevel(Global.getSector().getFaction(Factions.PLAYER));
		boolean legal = level.isAtWorst(req);
		if (requiresCommission(req)) {
			legal &= hasCommission();
		}
		return !legal;
	}

	public boolean isIllegalOnSubmarket(CargoStackAPI stack, TransferAction action) {
		if (stack.isCommodityStack()) {
			return isIllegalOnSubmarket((String) stack.getData(), action);
		}
		
		final RepLevel req = getRequiredLevelAssumingLegal(stack, action);
		if (req == null) return false;
		
		final RepLevel level = submarket.getFaction().getRelationshipLevel(
            Global.getSector().getFaction(Factions.PLAYER)
        );
		
		boolean legal = level.isAtWorst(req);
		if (requiresCommission(req)) {
			legal &= hasCommission();
		}
		
		return !legal;
	}
	
	public String getIllegalTransferText(CargoStackAPI stack, TransferAction action) {
		final RepLevel req = getRequiredLevelAssumingLegal(stack, action);

		if (req != null) {
			if (requiresCommission(req)) {
				return "Req: " +
						submarket.getFaction().getDisplayName() + " - " + req.getDisplayName().toLowerCase() + ", " +
						" commission";
			}
			return "Req: " + 
					submarket.getFaction().getDisplayName() + " - " + req.getDisplayName().toLowerCase();
		}
		
		return "Illegal to trade in " + stack.getDisplayName() + " here";
	}
	
	public Highlights getIllegalTransferTextHighlights(CargoStackAPI stack, TransferAction action) {
		final RepLevel req = getRequiredLevelAssumingLegal(stack, action);
		if (req != null) {
			Highlights h = new Highlights();
			RepLevel level = submarket.getFaction().getRelationshipLevel(Global.getSector().getFaction(Factions.PLAYER));
			if (!level.isAtWorst(req)) {
				h.append(submarket.getFaction().getDisplayName() + " - " + req.getDisplayName().toLowerCase(), negative);
			}
			if (requiresCommission(req) && !hasCommission()) {
				h.append("commission", negative);
			}
			return h;
		}
		return null;
	}
	
	private RepLevel getRequiredLevelAssumingLegal(CargoStackAPI stack, TransferAction action) {
		int tier = -1;
		if (stack.isWeaponStack()) {
			final WeaponSpecAPI spec = stack.getWeaponSpecIfWeapon();
			tier = spec.getTier();
		} else if (stack.isFighterWingStack()) {
			final FighterWingSpecAPI spec = stack.getFighterWingSpecIfWing();
			tier = spec.getTier();
		}
		
		if (tier >= 0) {
			if (action == TransferAction.PLAYER_BUY) {
				switch (tier) {
				case 0: return RepLevel.FAVORABLE;
				case 1: return RepLevel.WELCOMING;
				case 2: return RepLevel.FRIENDLY;
				case 3: return RepLevel.COOPERATIVE;
				}
			}
			return RepLevel.VENGEFUL;
		}
		
		if (!stack.isCommodityStack()) return null;
		return getRequiredLevelAssumingLegal((String) stack.getData(), action);
	}
	
	private RepLevel getRequiredLevelAssumingLegal(String comID, TransferAction action) {
		if (action == TransferAction.PLAYER_SELL) return RepLevel.VENGEFUL;
		
		final CommodityOnMarketAPI com = market.getCommodityData(comID);
		if (com.getCommodity().getTags().contains(Commodities.TAG_MILITARY)) {
			if (com.isPersonnel()) return RepLevel.COOPERATIVE;
			return RepLevel.FAVORABLE;
		}
		return null;
	}
	
	public boolean isIllegalOnSubmarket(FleetMemberAPI member, TransferAction action) {
		if (action == TransferAction.PLAYER_SELL && Misc.isAutomated(member)) {
			return true;
		}
		
		final RepLevel req = getRequiredLevelAssumingLegal(member, action);
		if (req == null) return false;
		
		final RepLevel level = submarket.getFaction().getRelationshipLevel(
			Global.getSector().getFaction(Factions.PLAYER)
		);
		
		boolean legal = level.isAtWorst(req);
		if (requiresCommission(req)) {
			legal &= hasCommission();
		}
		
		return !legal;
	}
	
	public String getIllegalTransferText(FleetMemberAPI member, TransferAction action) {
		final RepLevel req = getRequiredLevelAssumingLegal(member, action);
		if (req != null) {
			String str = "";
			final RepLevel level = submarket.getFaction().getRelationshipLevel(
				Global.getSector().getFaction(Factions.PLAYER)
			);
			if (!level.isAtWorst(req)) {
				str += "Req: " + submarket.getFaction().getDisplayName() + " - " + req.getDisplayName().toLowerCase();				
			}
			if (requiresCommission(req) && !hasCommission()) {
				if (!str.isEmpty()) str += "\n";
				str += "Req: " + submarket.getFaction().getDisplayName() + " - " + "commission";
			}
			return str;
		}
		
		if (action == TransferAction.PLAYER_BUY) {
			return "Illegal to buy";
		} else {
			return "Illegal to sell";
		}
	}

	public Highlights getIllegalTransferTextHighlights(FleetMemberAPI member, TransferAction action) {
		if (isIllegalOnSubmarket(member, action)) return null;
		
		final RepLevel req = getRequiredLevelAssumingLegal(member, action);
		if (req != null) {
			final Highlights h = new Highlights();
			final RepLevel level = submarket.getFaction().getRelationshipLevel(
				Global.getSector().getFaction(Factions.PLAYER)
			);
			if (!level.isAtWorst(req)) {
				h.append("Req: " + submarket.getFaction().getDisplayName() + " - " + req.getDisplayName().toLowerCase(), negative);
			}
			if (requiresCommission(req) && !hasCommission()) {
				h.append("Req: " + submarket.getFaction().getDisplayName() + " - commission", negative);
			}
			return h;
		}
		return null;
	}
	
	private RepLevel getRequiredLevelAssumingLegal(FleetMemberAPI member, TransferAction action) {
		if (action == TransferAction.PLAYER_BUY) {
			final int fp = member.getFleetPointCost();
			final HullSize size = member.getHullSpec().getHullSize();
			
			if (size == HullSize.CAPITAL_SHIP || fp > 15) return RepLevel.COOPERATIVE;
			if (size == HullSize.CRUISER || fp > 10) return RepLevel.FRIENDLY;
			if (size == HullSize.DESTROYER || fp > 5) return RepLevel.WELCOMING;
			return RepLevel.FAVORABLE;
		}
		return null;
	}
	
	private RepLevel minStanding = RepLevel.FAVORABLE;
	public boolean isEnabled(CoreUIAPI ui) {
		if (ui.getTradeMode() == CoreUITradeMode.SNEAK) return false;
		
		final RepLevel level = submarket.getFaction().getRelationshipLevel(
			Global.getSector().getFaction(Factions.PLAYER)
		);
		return level.isAtWorst(minStanding);
	}
	
	public OnClickAction getOnClickAction(CoreUIAPI ui) {
		return OnClickAction.OPEN_SUBMARKET;
	}
	
	public String getTooltipAppendix(CoreUIAPI ui) {
		if (!isEnabled(ui)) {
			return "Requires: " + submarket.getFaction().getDisplayName() + " - " + minStanding.getDisplayName().toLowerCase();
		}
		if (ui.getTradeMode() == CoreUITradeMode.SNEAK) {
			return "Requires: proper docking authorization";
		}
		return null;
	}
	
	public Highlights getTooltipAppendixHighlights(CoreUIAPI ui) {
		final String appendix = getTooltipAppendix(ui);
		if (appendix == null) return null;
		
		final Highlights h = new Highlights();
		h.setText(appendix);
		h.setColors(negative);
		return h;
	}
	
	@Override
	public PlayerEconomyImpactMode getPlayerEconomyImpactMode() {
		return PlayerEconomyImpactMode.PLAYER_SELL_ONLY;
	}
	
	public boolean isMilitaryMarket() {
		return true;
	}
}
package wfg.ltv_econ.economy.raids;

import static wfg.native_ui.util.UIConstants.*;
import static wfg.native_ui.util.Globals.settings;

import java.util.Random;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CargoAPI;
import com.fs.starfarer.api.campaign.CargoAPI.CargoItemType;
import com.fs.starfarer.api.campaign.CargoStackAPI;
import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.campaign.TextPanelAPI;
import com.fs.starfarer.api.campaign.econ.CommoditySpecAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.combat.WeaponAPI.AIHints;
import com.fs.starfarer.api.impl.campaign.graid.BaseGroundRaidObjectivePluginImpl;
import com.fs.starfarer.api.impl.campaign.graid.ShipWeaponsGroundRaidObjectivePluginImpl;
import com.fs.starfarer.api.impl.campaign.ids.Commodities;
import com.fs.starfarer.api.impl.campaign.ids.Tags;
import com.fs.starfarer.api.impl.campaign.rulecmd.salvage.MarketCMD.RaidDangerLevel;
import com.fs.starfarer.api.loading.FighterWingSpecAPI;
import com.fs.starfarer.api.loading.HullModSpecAPI;
import com.fs.starfarer.api.loading.WeaponSpecAPI;
import com.fs.starfarer.api.ui.IconGroupAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.ui.UIPanelAPI;
import com.fs.starfarer.api.util.Misc;
import com.fs.starfarer.api.util.WeightedRandomPicker;

import wfg.ltv_econ.economy.commodity.CommodityCell;
import wfg.ltv_econ.economy.engine.EconomyEngine;
import wfg.native_ui.util.Arithmetic;
import wfg.native_ui.util.NumFormat;

public class LtvShipWeaponsGroundRaidObjective extends BaseGroundRaidObjectivePluginImpl {
    public final CargoAPI looted = Global.getFactory().createCargo(true);
    public final CommodityCell cell;

	private UIPanelAPI iconPanel = null;

    public LtvShipWeaponsGroundRaidObjective(MarketAPI market) {
        super(market, Commodities.SHIPS);
        cell = EconomyEngine.instance().getComCell(id, market.getId());
        setSource(CommodityCellGroundRaidObjective.computeCommoditySource(cell));
    }

    @Override
    public final int performRaid(CargoAPI loot, Random random, float lootMult, TextPanelAPI text) {
        if (marinesAssigned <= 0) return 0;
		
		final WeightedRandomPicker<WeaponSpecAPI> pickerW = new WeightedRandomPicker<>(random);
		final WeightedRandomPicker<FighterWingSpecAPI> pickerF = new WeightedRandomPicker<>(random);
		final WeightedRandomPicker<HullModSpecAPI> pickerH = new WeightedRandomPicker<>(random);
		
		final WeightedRandomPicker<WeaponSpecAPI> weaponSubset = new WeightedRandomPicker<>(random);
		final WeightedRandomPicker<FighterWingSpecAPI> fighterSubset = new WeightedRandomPicker<>(random);

		final float quality = Misc.getShipQuality(market, market.getFactionId());
		final FactionAPI faction = Global.getSector().getFaction(market.getFactionId());
        final FactionAPI playerFaction = Global.getSector().getPlayerFaction();
		
		int maxTier = 0;
		if (market.getSize() >= 6) maxTier = 1;
		if (Misc.hasHeavyIndustry(market) || Misc.isMilitary(market)) maxTier = 1000;
		
		float numSmall = 0;
		float numMedium = 0;
		float numLarge = 0;
		for (String id : faction.getKnownWeapons()) {
			switch (settings.getWeaponSpec(id).getSize()) {
            case SMALL: numSmall++; break;
            case MEDIUM: numMedium++; break;
			case LARGE: numLarge++; break;
			}
		}
		final float numTotal = numSmall + numMedium + numLarge + 1f;
			
		for (String id : faction.getKnownWeapons()) {
			final WeaponSpecAPI spec = settings.getWeaponSpec(id);
			if (spec.getAIHints().contains(AIHints.SYSTEM)) continue;
			if (spec.getTier() > maxTier) continue;
			
			float p = 1f * spec.getRarity() + quality * getQMult(spec.getTier());
			switch (spec.getSize()) {
			case LARGE:
				p *= 1f - numLarge / numTotal;
				p *= 2f;
				break;
			case MEDIUM:
				p *= 1f - numMedium / numTotal;
				p *= 3f;
				break;
			case SMALL:
				p *= 1f - numSmall / numTotal;
				p *= 4f;
				break;
			}
			pickerW.add(spec, p);
		}
		for (int i = 0; i < 4 + marinesAssigned; i++) {
			final WeaponSpecAPI spec = pickerW.pick();
            if (spec == null) continue;

            weaponSubset.add(spec, pickerW.getWeight(spec));
            pickerW.remove(spec);
		}
		
		for (String id : faction.getKnownFighters()) {
			final FighterWingSpecAPI spec = settings.getFighterWingSpec(id);
			if (spec.getTier() > maxTier) continue;
			
			pickerF.add(spec, spec.getRarity() + quality * getQMult(spec.getTier()));
		}
		for (int i = 0; i < 2 + marinesAssigned/2; i++) {
			final FighterWingSpecAPI spec = pickerF.pick();
				
            fighterSubset.add(spec, pickerF.getWeight(spec));
            pickerF.remove(spec);
		}
		
		for (String id : faction.getKnownHullMods()) {
			final HullModSpecAPI spec = settings.getHullModSpec(id);
			if (spec.isHidden() || spec.isAlwaysUnlocked() || spec.hasTag(Tags.NO_DROP) || spec.getTier() > maxTier) continue;
			
			final float p = 1f * spec.getRarity() * (playerFaction.knowsHullMod(id) ? 0.2f : 1f);
			pickerH.add(spec, p);
		}
		
		lootMult *= 0.9f + random.nextFloat() * 0.2f;
		final float quantity = getQuantity(marinesAssigned, lootMult);

        final float value = quantity * cell.spec.getBasePrice() * 0.2f;
		quantityLooted = (int) quantity;

        cell.addStoredAmount(-quantity);
        looted.clear();
		
		final float weaponWeight = faction.getDoctrine().getWarships() + faction.getDoctrine().getPhaseShips();
		final float fighterWeight = 1f + faction.getDoctrine().getCarriers();
		final float hullmodWeight = 1f + quality * 1f;
		final float totalWeight = weaponWeight + fighterWeight + hullmodWeight;
		
		float weaponValue = value * weaponWeight / totalWeight;
		float fighterValue = value * fighterWeight / totalWeight;
		float hullmodValue = value * hullmodWeight / totalWeight;
		
        float totalValue = 0;
		int tries = 0;
		while (weaponValue > 0 && tries < 100) {
			tries++;
			final WeaponSpecAPI weapon = weaponSubset.pick();

			if (weapon != null) {
				final int min = 1, max = 2;

				final float val = weapon.getBaseValue() * ShipWeaponsGroundRaidObjectivePluginImpl.SELL_MULT;
				int num = (int) Math.min(min + random.nextInt(max - min + 1), weaponValue / val);
				
				if (num == 0 && random.nextFloat() < weaponValue / val) num = 1;
				
				if (num > 0) {
					looted.addWeapons(weapon.getWeaponId(), num);
					weaponValue -= val * num;
					totalValue += val * num;
				} else {
					break;
				}
			} else {
				break;
			}
		}
		
		fighterValue += Math.max(0, weaponValue);
		
		tries = 0;
		while (fighterValue > 0 && tries < 100) {
			tries++;
			final FighterWingSpecAPI fighter = fighterSubset.pick();

			if (fighter != null) {
				int min = 1, max = 2;
				switch (fighter.getRole()) {
                    case ASSAULT, BOMBER, SUPPORT: max = 2; break;
                    case FIGHTER: max = 3; break;
                    case INTERCEPTOR: max = 4; break;
				}
				final float val = fighter.getBaseValue() * ShipWeaponsGroundRaidObjectivePluginImpl.SELL_MULT;
				int num = (int) Math.min(min + random.nextInt(max - min + 1), fighterValue / val);
				if (num == 0 && random.nextFloat() < fighterValue / val) num = 1;
				
				if (num > 0) {
					looted.addFighters(fighter.getId(), num);
					fighterValue -= val * num;
					totalValue += val * num;
				} else {
					break;
				}
			} else {
				break;
			}
		}
		
		hullmodValue += Math.max(0, fighterValue);

		tries = 0;
		while (hullmodValue > 0 && tries < 100) {
			tries++;
			final HullModSpecAPI mod = pickerH.pickAndRemove();

			if (mod != null) {
				final float val = mod.getBaseValue();
				final int num = (random.nextFloat() < hullmodValue / val) ? 1 : 0;

				if (num > 0) {
					looted.addHullmods(mod.getId(), num);
					hullmodValue -= val * num;
					totalValue += val * num;
				} else {
					break;
				}
			} else {
				break;
			}
		}
		
		loot.addAll(looted);
		
		xpGained = (int) (totalValue * XP_GAIN_VALUE_MULT);
		return xpGained;
    }

    @Override
	public final void addIcons(IconGroupAPI iconGroup) {
		iconPanel = (UIPanelAPI) iconGroup;
	}

    public final int getProjectedCreditsValue() {
        return (int) getQuantity(getMarinesAssigned());
    }

    @Override
    public final RaidDangerLevel getDangerLevel() {
        return CommodityCellGroundRaidObjective.getDangerLevel(getWeaponsCommoditySpec(), cell, source);
    }

    @Override
    public final float getQuantitySortValue() {
        return QUANTITY_SORT_TIER_1 + getWeaponsCommoditySpec().getOrder();
    }

    @Override
    public final String getQuantityString(int marines) {
        return "";
    }

    @Override
    public final String getValueString(int marines) {
        return NumFormat.engNotate(getQuantity(Math.max(1, marines)));
    }

    @Override
    public final int getValue(int marines) {
        return (int) getQuantity(marines);
    }

    @Override
    public final float getQuantity(int marines) {
        return getQuantity(marines, 1.0);
    }

    public final float getQuantity(int marines, double mult) {
        return CommodityCellGroundRaidObjective.getQuantity(cell, marines, mult);
    }

    @Override
    public final String getName() {
        return getWeaponsCommoditySpec().getName();
    }

    @Override
    public final CargoStackAPI getStackForIcon() {
        return Global.getFactory().createCargoStack(CargoItemType.RESOURCES, Commodities.SHIP_WEAPONS, null);
    }

    @Override
    public final String getCommodityIdForDeficitIcons() {
		CommodityCellGroundRaidObjective.addInfoBar(iconPanel, cell);

        return cell.comID;
    }

    @Override
    public final void createTooltip(TooltipMakerAPI tp, boolean expanded) {
        tp.addPara("Ship weapons, fighter LPCs, and hullmod specs. Availability is based on the \""
            + cell.spec.getName() + "\" commodity.", 0f
        );
        tp.addPara( "The colony faction's doctrine affects the number of weapons vs fighter LPCs acquired. " +
            "Higher ship quality increases the probability of finding modspecs..", opad
        );
        if (!Misc.hasHeavyIndustry(market) && !Misc.isMilitary(market)) {
            tp.addPara("This colony does not have heavy industry or a military presence and has no access to high-tier ship equipment.",
                negative, opad
            );
        } else if (Misc.hasHeavyIndustry(market)) {
            tp.addPara("This colony has heavy industry and high-tier equipment may be found.", positive, opad);
        } else if (Misc.isMilitary(market)) {
            tp.addPara("This colony has a military presence and high-tier equipment may be found.", positive, opad);
        }

        if (expanded) CommodityCellGroundRaidObjective.addStockpileLegend(tp, expanded);
    }

    @Override
    public final boolean hasTooltip() {
        return true;
    }

    @Override
    public final boolean isTooltipExpandable() {
        return true;
    }

    private final float getQMult(int tier) {
        return Arithmetic.clamp(tier * 0.25f, 0f, 0.75f);
    }

    private static final CommoditySpecAPI getWeaponsCommoditySpec() {
        return settings.getCommoditySpec(Commodities.SHIP_WEAPONS);
    }
}
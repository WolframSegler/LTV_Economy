package wfg.ltv_econ.industry;

import java.util.List;
import java.util.Random;
import java.util.Map.Entry;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.BattleAPI;
import com.fs.starfarer.api.campaign.CampaignEventListener.FleetDespawnReason;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.DebugFlags;
import com.fs.starfarer.api.impl.campaign.econ.impl.MilitaryBase;
import com.fs.starfarer.api.impl.campaign.fleets.FleetFactory.PatrolType;
import com.fs.starfarer.api.impl.campaign.fleets.FleetFactoryV3;
import com.fs.starfarer.api.impl.campaign.fleets.PatrolAssignmentAIV4;
import com.fs.starfarer.api.impl.campaign.fleets.RouteManager;
import com.fs.starfarer.api.impl.campaign.fleets.RouteManager.OptionalFleetData;
import com.fs.starfarer.api.impl.campaign.fleets.RouteManager.RouteData;
import com.fs.starfarer.api.impl.campaign.fleets.RouteManager.RouteSegment;
import com.fs.starfarer.api.impl.campaign.ids.Factions;
import com.fs.starfarer.api.impl.campaign.ids.MemFlags;
import com.fs.starfarer.api.impl.campaign.ids.Ranks;
import com.fs.starfarer.api.impl.campaign.ids.Stats;
import com.fs.starfarer.api.util.Misc;
import com.fs.starfarer.api.util.WeightedRandomPicker;

import wfg.ltv_econ.economy.engine.EconomyEngine;
import wfg.ltv_econ.economy.fleet.FactionShipInventory;
import wfg.ltv_econ.economy.fleet.FleetFactoryHelpers;
import wfg.ltv_econ.economy.fleet.PatrolFleetRouteManager;
import wfg.ltv_econ.economy.fleet.PatrolFleetRouteManager.PatrolMission;
import wfg.ltv_econ.serializable.LtvEconSaveData;

public class LtvMilitaryBase extends MilitaryBase {

	@Override
	public void advance(float amount) {
		final float days = Global.getSector().getClock().convertToDays(amount);

		{ // super super advance
			final boolean disrupted = isDisrupted();
			if (!disrupted && wasDisrupted) disruptionFinished();
			wasDisrupted = disrupted;
			
			if (building && !disrupted) {
				
				buildProgress += days * (DebugFlags.COLONY_DEBUG ? 100f : 1f);
				
				if (buildProgress >= buildTime) finishBuildingOrUpgrading();
			}
		}
		if (Global.getSector().getEconomy().isSimMode()) return;
		if (!isFunctional()) return;
		
		final float baseSpawnRate = market.getStats().getDynamic().getStat(Stats.COMBAT_FLEET_SPAWN_RATE_MULT).getModifiedValue();
		final float spawnRate = baseSpawnRate * (Global.getSector().isInNewGameAdvance() ? 3f : 1f);
		
		final float extraTime = returningPatrolValue <= 0 ? 0f : tracker.getIntervalDuration() * days;
		returningPatrolValue = Math.max(0f, returningPatrolValue - days);
		tracker.advance(days * spawnRate + extraTime);
		
		if (DebugFlags.FAST_PATROL_SPAWN) tracker.advance(days * spawnRate * 100f);
		
		if (tracker.intervalElapsed()) {
			final WeightedRandomPicker<PatrolType> picker = new WeightedRandomPicker<>();

			picker.add(PatrolType.FAST, getMaxPatrols(PatrolType.FAST) - getCount(PatrolType.FAST));
			picker.add(PatrolType.COMBAT, getMaxPatrols(PatrolType.COMBAT) - getCount(PatrolType.COMBAT)); 
			picker.add(PatrolType.HEAVY, getMaxPatrols(PatrolType.HEAVY) - getCount(PatrolType.HEAVY)); 
			
			if (picker.isEmpty()) return;
			
			final PatrolType type = picker.pick();
			final PatrolFleetData custom = new PatrolFleetData(type);
			
			final OptionalFleetData extra = new OptionalFleetData(market);
			extra.fleetType = type.getFleetType();
			
			final RouteData route = RouteManager.getInstance().addRoute(getRouteSourceId(), market, Misc.genRandomSeed(), extra, this, custom);
			extra.strength = Misc.getAdjustedStrength(getPatrolCombatFP(type, route.getRandom()), market);
			
			LtvEconSaveData.instance().patrolRouteManager.registerMission(route);
			
			final float patrolDays = 35f + (float) Math.random() * 10f;
			route.addSegment(new RouteSegment(patrolDays, market.getPrimaryEntity()));
		}
	}

	@Override
	public void reportBattleOccurred(CampaignFleetAPI fleet, CampaignFleetAPI primaryWinner, BattleAPI battle) {
		if (fleet.getFleetData().getNumMembers() < 1) return;

		final RouteData route = RouteManager.getInstance().getRoute(getRouteSourceId(), fleet);
		if (route == null) return;
		final PatrolMission mission = LtvEconSaveData.instance().patrolRouteManager.getMission(route);
		if (mission == null) return;

		final List<FleetMemberAPI> members = fleet.getFleetData().getMembersListCopy();
		final FactionShipInventory inv = EconomyEngine.instance().getFactionShipInventory(mission.factionID);

		for (Entry<String, Integer> entry : mission.allocatedShips.singleEntrySet()) {
			final String hullId = entry.getKey();
			final int amount = entry.getValue();

			int membersLost = amount;
			for (FleetMemberAPI member : members) {
				if (member.getHullId().equals(hullId)) {
					--membersLost;
				}
			}
			
			if (membersLost > 0) {
				inv.registerShipLoss(hullId, membersLost);
				entry.setValue(amount - membersLost);
			}
		}
	}

	@Override
	public void reportFleetDespawnedToListener(CampaignFleetAPI fleet, FleetDespawnReason reason, Object param) {
		final RouteData route = RouteManager.getInstance().getRoute(getRouteSourceId(), fleet);
		if (route == null) return;
		final PatrolFleetRouteManager manager = LtvEconSaveData.instance().patrolRouteManager;

		switch (reason) {
		case REACHED_DESTINATION, PLAYER_FAR_AWAY:
			manager.freePatrol(route);
			break;

		case DESTROYED_BY_BATTLE, NO_MEMBERS:
			manager.registerPatrolLoss(route);
			break;

		case OTHER, NO_REASON_PROVIDED:
			manager.freePatrol(route);
			break;
		}

		if (!isFunctional()) return;

		if (route.getCustom() instanceof PatrolFleetData custom) {
			if (custom.spawnFP > 0) {
				final float fraction  = fleet.getFleetPoints() / custom.spawnFP;
				returningPatrolValue += fraction;
			}
		}
	}
	
	@Override
	public CampaignFleetAPI spawnFleet(RouteData route) {
		final PatrolFleetData custom = (PatrolFleetData) route.getCustom();
		final PatrolType type = custom.type;
		
		final CampaignFleetAPI fleet = createPatrol(type, market.getFactionId(), route, market, route.getRandom());
		if (fleet == null) return null;
		
		fleet.addEventListener(this);
		
		market.getContainingLocation().addEntity(fleet);
		fleet.setFacing((float) Math.random() * 360f);

		// this will get overridden by the patrol assignment AI, depending on route-time elapsed etc
		fleet.setLocation(market.getPrimaryEntity().getLocation().x, market.getPrimaryEntity().getLocation().y);
		
		fleet.addScript(new PatrolAssignmentAIV4(fleet, route));
		
		fleet.getMemoryWithoutUpdate().set(MemFlags.FLEET_IGNORES_OTHER_FLEETS, true, 0.3f);
		
		custom.spawnFP = Math.max(0, fleet.getFleetPoints());
		
		return fleet;
	}

	private static final CampaignFleetAPI createPatrol(PatrolType type,
		String factionId, RouteData route, MarketAPI market, Random random
	) {
		if (random == null) random = new Random();

		final PatrolMission mission = LtvEconSaveData.instance().patrolRouteManager.getMission(route);
		if (mission == null || mission.allocatedShips.isEmpty()) return null;

		final float fleetPoints = route.getExtra().getStrengthModifiedByDamage();
		final boolean banPhaseShips = !Global.getSector().getFaction(factionId).isPlayerFaction()
			&& fleetPoints < FleetFactoryV3.FLEET_POINTS_THRESHOLD_FOR_ANNOYING_SHIPS;

		final CampaignFleetAPI fleet = FleetFactoryV3.createEmptyFleet(factionId, type.getFleetType(), market);
		if (fleet == null) return null;

		FleetFactoryHelpers.populateFleetFromAllocation(fleet, mission.allocatedShips, -1, banPhaseShips, random);

		FleetFactoryHelpers.configureFleetAfterAllocation(
			fleet, factionId,
			route.getQualityOverride() + Misc.getShipQuality(market, factionId),
			route.getTimestamp(), 1.0f, 0,
			random, -1
		);
		if (fleet == null || fleet.isEmpty()) return null;

		if (!fleet.getFaction().getCustomBoolean(Factions.CUSTOM_PATROLS_HAVE_NO_PATROL_MEMORY_KEY)) {
			fleet.getMemoryWithoutUpdate().set(MemFlags.MEMORY_KEY_PATROL_FLEET, true);
			if (type == PatrolType.FAST || type == PatrolType.COMBAT) {
				fleet.getMemoryWithoutUpdate().set(MemFlags.MEMORY_KEY_CUSTOMS_INSPECTOR, true);
			}
		} else if (fleet.getFaction().getCustomBoolean(Factions.CUSTOM_PIRATE_BEHAVIOR)) {
			fleet.getMemoryWithoutUpdate().set(MemFlags.MEMORY_KEY_PIRATE, true);
			
			// make hidden pather and pirate bases raid so there's some consequence to just having a colony in a system with one of those.
			if (market != null && market.isHidden()) {
				fleet.getMemoryWithoutUpdate().set(MemFlags.MEMORY_KEY_RAIDER, true);
			}
		}

		final String rankId = switch (type) {
			case FAST -> Ranks.SPACE_LIEUTENANT;
			case COMBAT -> Ranks.SPACE_COMMANDER;
			case HEAVY -> Ranks.SPACE_CAPTAIN;
			default -> Ranks.SPACE_COMMANDER;
		};

		fleet.getCommander().setPostId(Ranks.POST_PATROL_COMMANDER);
		fleet.getCommander().setRankId(rankId);

		return fleet;
	}
}
package wfg.ltv_econ.economy.fleet;

import static wfg.native_ui.util.Globals.settings;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Set;

import org.apache.log4j.Logger;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.BattleAPI;
import com.fs.starfarer.api.campaign.CampaignEventListener.FleetDespawnReason;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.CargoAPI;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.campaign.listeners.FleetEventListener;
import com.fs.starfarer.api.impl.campaign.command.WarSimScript.LocationDanger;
import com.fs.starfarer.api.impl.campaign.econ.ShippingDisruption;
import com.fs.starfarer.api.impl.campaign.fleets.BaseRouteFleetManager;
import com.fs.starfarer.api.impl.campaign.fleets.EconomyFleetRouteManager;
import com.fs.starfarer.api.impl.campaign.fleets.FleetFactoryV3;
import com.fs.starfarer.api.impl.campaign.fleets.RouteManager;
import com.fs.starfarer.api.impl.campaign.fleets.RouteManager.OptionalFleetData;
import com.fs.starfarer.api.impl.campaign.fleets.RouteManager.RouteData;
import com.fs.starfarer.api.impl.campaign.fleets.RouteManager.RouteSegment;
import com.fs.starfarer.api.impl.campaign.ids.Commodities;
import com.fs.starfarer.api.impl.campaign.ids.Factions;
import com.fs.starfarer.api.impl.campaign.ids.FleetTypes;
import com.fs.starfarer.api.impl.campaign.ids.MemFlags;
import com.fs.starfarer.api.impl.campaign.intel.events.PiracyRespiteScript;
import com.fs.starfarer.api.impl.campaign.rulecmd.KantaCMD;
import com.fs.starfarer.api.impl.campaign.shared.SharedData;
import com.fs.starfarer.api.util.Misc;
import com.fs.starfarer.api.util.TimeoutTracker;
import com.fs.starfarer.api.util.WeightedRandomPicker;

import wfg.ltv_econ.config.EconConfig;
import wfg.ltv_econ.economy.commodity.TradeCom;
import wfg.ltv_econ.economy.engine.EconomyEngine;
import wfg.ltv_econ.economy.fleet.TradeMission.MissionStatus;
import wfg.native_ui.util.Arithmetic;

public class LtvEconFleetRouteManager extends BaseRouteFleetManager implements FleetEventListener {
	private static final Logger log = Global.getLogger(LtvEconFleetRouteManager.class);
	private static final int maxEconFleets = settings.getInt("maxEconFleets");
	private static final int maxShipsInFleet = settings.getInt("maxShipsInAIFleet");
	private static final float minEconSpawnInterval = settings.getFloat("minEconSpawnIntervalPerMarket");
	private static final float SMALL_CARGO_AMOUNT = 1000f;

	private final TimeoutTracker<String> recentlySentTradeFleet = new TimeoutTracker<String>();

	public LtvEconFleetRouteManager() {
		super(0.2f, 0.3f);
	}

	@Override
	public final void advance(float delta) {
		super.advance(delta);

		final float days = Global.getSector().getClock().convertToDays(delta);
		recentlySentTradeFleet.advance(days);
	}

	protected String getRouteSourceId() {
		return EconomyFleetRouteManager.SOURCE_ID;
	}

	protected int getMaxFleets() {
		final int numMarkets = Global.getSector().getEconomy().getNumMarkets();
		final int maxBasedOnMarkets = numMarkets * 2;
		return Math.min(maxBasedOnMarkets, maxEconFleets);
	}

	protected void addRouteFleetIfPossible() {
		final TradeMission mission = pickTradeMission();
		if (mission == null) return;
		final MarketAPI src = mission.src;
		final MarketAPI dest = mission.dest;

		final OptionalFleetData extra = new OptionalFleetData(src);
		final String factionId = getFleetFaction(mission);
		extra.factionId = factionId;

		final RouteData route = RouteManager.getInstance().addRoute(getRouteSourceId(), src, mission.uniqueID,
			extra, this,new LtvEconomyRouteData(mission)
		);

		final StarSystemAPI sysFrom = src.getStarSystem();
		final StarSystemAPI sysTo = dest.getStarSystem();

		EconomyFleetRouteManager.ENEMY_STRENGTH_CHECK_EXCLUDE_PIRATES = (src.getFaction().isPlayerFaction()
			|| dest.getFaction().isPlayerFaction()) &&
			PiracyRespiteScript.playerHasPiracyRespite() ? true : false;

		final LocationDanger danger = (sysFrom != null && sysFrom.isCurrentLocation()) ?
			LocationDanger.NONE : ShipAllocator.getHighestLocationDangerInRoute(factionId, sysFrom, sysTo);

		float pLoss = 0.5f * EconomyFleetRouteManager.DANGER_LOSS_PROB.get(danger);
		if (mission.smuggling) pLoss *= 0.5;
		if (Math.random() < pLoss) {
			onRouteLost(route, mission);
			return;
		}

		route.addSegment(new RouteSegment(MissionStatus.IN_SRC_ORBIT_LOADING.ordinal(), mission.transferDur, src.getPrimaryEntity()));
		route.addSegment(new RouteSegment(MissionStatus.IN_TRANSIT.ordinal(), mission.travelDur, src.getPrimaryEntity(), dest.getPrimaryEntity()));
		route.addSegment(new RouteSegment(MissionStatus.IN_DST_ORBIT_UNLOADING.ordinal(), mission.transferDur, dest.getPrimaryEntity()));

		final int currentDay = EconomyEngine.instance().getCyclesSinceTrade();
		route.setDelay(Math.max(0, mission.startOffset - currentDay));
		mission.spawnedFleetFinishedJob = false;

		recentlySentTradeFleet.add(src.getId(), minEconSpawnInterval);

		log.info("Added trade fleet route from " + src.getName() + " to " + dest.getName());
	}

	private static final String getFleetFaction(TradeMission mission) {
		final MarketAPI src = mission.src;
		final boolean canBeIndependent = !src.getFaction().isHostileTo(Factions.INDEPENDENT) &&
				!mission.dest.getFaction().isHostileTo(Factions.INDEPENDENT);

		String factionId = Factions.INDEPENDENT;
		if (!canBeIndependent) {
			factionId = src.getFactionId();
		} else if (mission.usedFactionFleet) {
			if (!mission.smuggling) {
				factionId = src.getFactionId();
			} else if (Math.random() * 10f < src.getStabilityValue()) {
				factionId = src.getFactionId();
			}
		} else {
			if (!mission.smuggling && Math.random() <= 0.3f) {
				factionId = src.getFactionId();

			} else if (Math.random() <= 0.1f) {
				factionId = src.getFactionId();
			}
		}

		return factionId;
	}

	private final TradeMission pickTradeMission() {
		final Set<String> excludeList = SharedData.getData().getMarketsWithoutTradeFleetSpawn();
		final List<TradeMission> missions = new ArrayList<>(EconomyEngine.instance().getActiveMissions());
		final WeightedRandomPicker<TradeMission> missionPicker = new WeightedRandomPicker<>();

		for (TradeMission m : missions) {
			if (m.status != MissionStatus.SCHEDULED || !m.spawnedFleetFinishedJob) continue;

			if (excludeList.contains(m.src.getId()) || excludeList.contains(m.dest.getId())) continue;

			if (recentlySentTradeFleet.contains(m.src.getId())) continue;

			missionPicker.add(m, m.src.getSize());
		}

		return missionPicker.pick();
	}

	public final boolean shouldCancelRouteAfterDelayCheck(RouteData route) {
		final TradeMission mission = LtvEconomyRouteData.getMission(route);
		if (mission == null) return false;
		if (mission.src == null || mission.dest == null) return true;

		if (!mission.smuggling) {
			if (!mission.src.hasSpaceport() || !mission.dest.hasSpaceport()) {
				mission.status = MissionStatus.CANCELLED;
			} else if (mission.src.getFaction().isHostileTo(mission.dest.getFaction())) {
				mission.status = MissionStatus.CANCELLED;
			}
		}

		return mission.status == MissionStatus.CANCELLED;
	}

	public final CampaignFleetAPI spawnFleet(RouteData route) {
		final Random random = route.getSeed() != null ? new Random(route.getSeed()) : new Random();

		final CampaignFleetAPI fleet = createTradeRouteFleet(route, random);
		if (fleet == null) return null;
		
		final TradeMission mission = LtvEconomyRouteData.getMission(route);
		mission.setSpawnedFleetCapRatios(fleet.getCargo());

		if (KantaCMD.playerHasProtection()) {
			if (mission.src != null && mission.dest != null) {
				if (mission.src.getFaction().isPlayerFaction() || mission.dest.getFaction().isPlayerFaction()) {
					fleet.getMemoryWithoutUpdate().set(MemFlags.FLEET_IGNORED_BY_FACTION, Factions.PIRATES);
				}
			}
		}

		fleet.addEventListener(this);

		fleet.addScript(new LtvEconFleetAssignmentAI(fleet, route));
		return fleet;
	}

	private final CampaignFleetAPI createTradeRouteFleet(RouteData route, Random random) {
		final TradeMission mission = LtvEconomyRouteData.getMission(route);
		if (mission == null || mission.src == null || mission.dest == null) return null;

		final MarketAPI src = mission.src;
		final String factionId = route.getFactionId();

		int totalAllocated = 0;
		for (int count : mission.allocatedShips.values()) totalAllocated += count;
		if (totalAllocated < 1) return null;

		final float usageFraction = (float) Arithmetic.clamp(
			(1d / Math.log10(totalAllocated * 2)) * (mission.smuggling ? 0.5 : 1d), 
			0.1, 1.0
		);
		final int targetShips = Math.min(Math.round(totalAllocated * usageFraction), Math.min(totalAllocated, maxShipsInFleet));

		final float totalAmount = mission.getTotalAmount();
		final String fleetType;
		if (mission.smuggling) {
			fleetType = FleetTypes.TRADE_SMUGGLER;
		} else if ((mission.crewAmount / totalAmount) > 0.5f) {
			fleetType = FleetTypes.TRADE_LINER;
		} else if (totalAmount < SMALL_CARGO_AMOUNT) {
			fleetType = FleetTypes.TRADE_SMALL;
		} else {
			fleetType = FleetTypes.TRADE;
		}

		final float numShipsMult = src.getStats().getDynamic().getMod("combat_fleet_size_mult").computeEffective(0.0F);
		final float combatPts = mission.combatPowerTarget * numShipsMult;

		final CampaignFleetAPI fleet;
		final int selectedAmount;
		{ // Create fleet
			fleet = FleetFactoryV3.createEmptyFleet(factionId, fleetType, src);
			if (fleet == null) return null;

			final boolean banPhaseShips = !fleet.getFaction().isPlayerFaction()
				&& combatPts < FleetFactoryV3.FLEET_POINTS_THRESHOLD_FOR_ANNOYING_SHIPS;

			selectedAmount = FleetFactoryHelpers.populateFleetFromAllocation(
				fleet, mission.allocatedShips, targetShips, banPhaseShips, random
			);

			FleetFactoryHelpers.configureFleetAfterAllocation(
				fleet, factionId,
				route.getQualityOverride() + Misc.getShipQuality(src, factionId),
				route.getTimestamp(), 0.5f, -2,
				random, EconConfig.TRAVEL_SPEED_LY_DAY
			);

			fleet.getMemoryWithoutUpdate().set(mission.smuggling ? MemFlags.MEMORY_KEY_SMUGGLER : MemFlags.MEMORY_KEY_TRADE_FLEET, true);
			if (mission.smuggling) Misc.makeLowRepImpact(fleet, "smuggler");
		}

		log.info("Created trade fleet with " + selectedAmount + " ships for market [" + src.getName() + "]");

		return fleet;
	}

	public final void reportBattleOccurred(CampaignFleetAPI fleet, CampaignFleetAPI primaryWinner, BattleAPI battle) {
		if (fleet.getFleetData().getNumMembers() < 1) return;

		final RouteData route = RouteManager.getInstance().getRoute(getRouteSourceId(), fleet);
		if (route == null) return;
		final TradeMission mission = LtvEconomyRouteData.getMission(route);
		if (mission == null) return;

		final CargoAPI cargo = fleet.getCargo();
		final float cargoRemainingRatio = (cargo.getMaxCapacity() / mission.cargoAmount) / mission.spawnedFleetCargoCapRatio;
		final float fuelRemainingRatio = (cargo.getMaxFuel() / mission.fuelAmount) / mission.spawnedFleetFuelCapRatio;
		final float crewRemainingRatio = (cargo.getFreeCrewSpace() / mission.crewAmount) / mission.spawnedFleetCrewCapRatio;
		mission.setSpawnedFleetCapRatios(cargo);

		for (TradeCom com : mission.cargo) {
			if (com.comID.equals(Commodities.FUEL)) com.amount *= fuelRemainingRatio;
			else if (com.comID.equals(Commodities.CREW) || com.comID.equals(Commodities.MARINES)) com.amount *= crewRemainingRatio;
			else com.amount *= cargoRemainingRatio;
		}

		final float lossFraction = 0.5f;
		if (cargoRemainingRatio < lossFraction && fuelRemainingRatio < lossFraction && crewRemainingRatio < lossFraction) {
			ShippingDisruption.getDisruption(mission.src).addShippingLost(1);
			ShippingDisruption.getDisruption(mission.src).notifyDisrupted(ShippingDisruption.ACCESS_LOSS_DURATION);
		}
	}

	public final void reportFleetDespawnedToListener(CampaignFleetAPI fleet, FleetDespawnReason reason, Object param) {
		final RouteData route = RouteManager.getInstance().getRoute(getRouteSourceId(), fleet);
		if (route == null) return;

		final TradeMission mission = LtvEconomyRouteData.getMission(route);
		if (mission != null) mission.spawnedFleetFinishedJob = true;
		if (route.isExpired()) return;

		switch (reason) {
		case DESTROYED_BY_BATTLE, NO_MEMBERS:
			onRouteLost(route, mission);
			break;
	
		default: break;
		}
	}

	public final boolean shouldRepeat(RouteData route) {
		return false;
	}

	private static final void onRouteLost(RouteData route, TradeMission mission) {
		mission.status = MissionStatus.LOST;
		mission.spawnedFleetFinishedJob = true;
		RouteManager.getInstance().removeRoute(route);
			
		if (mission.src != null) {
			ShippingDisruption.getDisruption(mission.src).addShippingLost(1);
			ShippingDisruption.getDisruption(mission.src).notifyDisrupted(ShippingDisruption.ACCESS_LOSS_DURATION);
		}
	}

	public final void reportAboutToBeDespawnedByRouteManager(RouteData route) {}
}
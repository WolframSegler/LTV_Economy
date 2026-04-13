package wfg.ltv_econ.economy.fleet;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import org.apache.log4j.Logger;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.SettingsAPI;
import com.fs.starfarer.api.campaign.BattleAPI;
import com.fs.starfarer.api.campaign.CampaignEventListener.FleetDespawnReason;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.CargoAPI;
import com.fs.starfarer.api.campaign.FactionAPI.ShipPickMode;
import com.fs.starfarer.api.campaign.FleetDataAPI;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.campaign.listeners.FleetEventListener;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.fleet.FleetMemberType;
import com.fs.starfarer.api.fleet.RepairTrackerAPI;
import com.fs.starfarer.api.impl.campaign.command.WarSimScript;
import com.fs.starfarer.api.impl.campaign.command.WarSimScript.LocationDanger;
import com.fs.starfarer.api.impl.campaign.econ.ShippingDisruption;
import com.fs.starfarer.api.impl.campaign.fleets.BaseRouteFleetManager;
import com.fs.starfarer.api.impl.campaign.fleets.DefaultFleetInflaterParams;
import com.fs.starfarer.api.impl.campaign.fleets.EconomyFleetRouteManager;
import com.fs.starfarer.api.impl.campaign.fleets.FleetFactoryV3;
import com.fs.starfarer.api.impl.campaign.fleets.FleetParamsV3;
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

import wfg.ltv_econ.economy.commodity.ComTradeFlow;
import wfg.ltv_econ.economy.engine.EconomyEngine;
import wfg.ltv_econ.economy.fleet.TradeMission.MissionStatus;
import wfg.ltv_econ.util.Arithmetic;
import wfg.native_ui.util.ArrayMap;

public class LtvEconFleetRouteManager extends BaseRouteFleetManager implements FleetEventListener {
	private static final Logger log = Global.getLogger(LtvEconFleetRouteManager.class);
	private static final SettingsAPI settings = Global.getSettings();
	private static final String SOURCE_ID = "econ";
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
		return SOURCE_ID;
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

		final RouteData route = RouteManager.getInstance().addRoute(
				getRouteSourceId(), src, Misc.genRandomSeed(), extra, this);
		route.setCustom(mission);

		final StarSystemAPI sysFrom = src.getStarSystem();
		final StarSystemAPI sysTo = dest.getStarSystem();

		EconomyFleetRouteManager.ENEMY_STRENGTH_CHECK_EXCLUDE_PIRATES = (src.getFaction().isPlayerFaction()
				|| dest.getFaction().isPlayerFaction()) &&
				PiracyRespiteScript.playerHasPiracyRespite() ? true : false;

		final LocationDanger dFrom = WarSimScript.getDangerFor(factionId, sysFrom);
		final LocationDanger dTo = WarSimScript.getDangerFor(factionId, sysTo);

		LocationDanger danger = dFrom.ordinal() > dTo.ordinal() ? dFrom : dTo;

		if (sysFrom != null && sysFrom.isCurrentLocation()) {
			// the player is in the from location, don't auto-lose the trade fleet.
			// Let it get destroyed by actual fleets, if it does
			danger = LocationDanger.NONE;
		}

		float pLoss = 0.5f * EconomyFleetRouteManager.DANGER_LOSS_PROB.get(danger);
		if (mission.smuggling) pLoss *= 0.5;
		if (Math.random() < pLoss) {
			onRouteLost(route, mission);
			return;
		}

		route.addSegment(new RouteSegment(MissionStatus.IN_SRC_ORBIT_LOADING.ordinal(), mission.transferDur, src.getPrimaryEntity()));
		route.addSegment(new RouteSegment(MissionStatus.IN_TRANSIT.ordinal(), mission.travelDur, src.getPrimaryEntity(), dest.getPrimaryEntity()));
		route.addSegment(new RouteSegment(MissionStatus.IN_DST_ORBIT_UNLOADING.ordinal(), mission.transferDur, dest.getPrimaryEntity()));

		setDelayAndSendMessage(route);
		mission.spawnedFleetFinishedJob = false;

		recentlySentTradeFleet.add(src.getId(), minEconSpawnInterval);

		log.info("Added trade fleet route from " + src.getName() + " to " + dest.getName());
	}

	protected String getFleetFaction(TradeMission mission) {
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

	protected void setDelayAndSendMessage(RouteData route) {
		final TradeMission mission = (TradeMission) route.getCustom();
		final int currentDay = EconomyEngine.instance().getCyclesSinceTrade();
		final int delay = Math.max(0, mission.startOffset - currentDay);
		route.setDelay(delay);

		if (!Factions.PLAYER.equals(route.getFactionId())) {
			// TODO maybe create a custom TradeFleetIntel that does not rely on CustomData
			// new TradeFleetDepartureIntel(route);
		}
	}

	public final TradeMission pickTradeMission() {
		final Set<String> excludeList = SharedData.getData().getMarketsWithoutTradeFleetSpawn();
		final List<TradeMission> missions = new ArrayList<>(EconomyEngine.instance().getActiveMissions());
		final WeightedRandomPicker<TradeMission> missionPicker = new WeightedRandomPicker<>();

		for (TradeMission m : missions) {
			if (m.status != MissionStatus.SCHEDULED || !m.spawnedFleetFinishedJob)
				continue;

			if (excludeList.contains(m.src.getId()) || excludeList.contains(m.dest.getId()))
				continue;

			if (recentlySentTradeFleet.contains(m.src.getId()))
				continue;

			missionPicker.add(m, m.src.getSize());
		}

		return missionPicker.pick();
	}

	public final boolean shouldCancelRouteAfterDelayCheck(RouteData route) {
		if (!(route.getCustom() instanceof TradeMission mission)) return false;

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

		final TradeMission mission = (TradeMission) route.getCustom();
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

	public static final CampaignFleetAPI createTradeRouteFleet(RouteData route, Random random) {
		final TradeMission mission = (TradeMission) route.getCustom();

		final MarketAPI src = mission.src;
		final String factionId = route.getFactionId();

		int totalAllocated = 0;
		for (int count : mission.allocatedShips.values()) totalAllocated += count;
		if (totalAllocated < 1) return null;

		final float usageFraction = (float) Arithmetic.clamp(
			(1.0 / Math.log10(totalAllocated * 2)) * (mission.smuggling ? 0.5 : 1.0), 
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

			final FleetDataAPI fData = fleet.getFleetData();
			fData.setOnlySyncMemberLists(true);
			Misc.getSalvageSeed(fleet);

			final boolean banPhaseShipsEtc = !fleet.getFaction().isPlayerFaction()
				&& combatPts < (float) FleetFactoryV3.FLEET_POINTS_THRESHOLD_FOR_ANNOYING_SHIPS;

			// Ship selection
			final ArrayMap<ShipTypeData, Integer> remaining = new ArrayMap<>(mission.allocatedShips);
			final List<ShipTypeData> selection = new ArrayList<>();
			while (selection.size() < targetShips) {
				double totalWeight = 0;
				for (Map.Entry<ShipTypeData, Integer> e : remaining.singleEntrySet()) {
					final ShipTypeData data = e.getKey();
					if (banPhaseShipsEtc && data.spec.isPhase()) continue;

					totalWeight += e.getValue() * Math.max(1.0, data.spec.getFleetPoints());
				}
				if (totalWeight == 0) break;

				final Iterator<Map.Entry<ShipTypeData, Integer>> it = remaining.entrySet().iterator();
				final double r = random.nextDouble() * totalWeight;
				double accum = 0;
				while (it.hasNext()) {
					final Map.Entry<ShipTypeData, Integer> e = it.next();
					final ShipTypeData data = e.getKey();
					if (banPhaseShipsEtc && data.spec.isPhase()) continue;

					accum += e.getValue() * Math.max(1.0, data.spec.getFleetPoints());
					if (accum >= r) {
						selection.add(data);
						final int newCount = e.getValue() - 1;
						if (newCount == 0) it.remove();
						else e.setValue(newCount);
						break;
					}
				}
			}

			selectedAmount = selection.size();
			for (ShipTypeData data : selection) {
				final List<String> variantIds = settings.getHullIdToVariantListMap().get(data.hullID);

				final ShipVariantAPI variant;
				if (!variantIds.isEmpty()) {
					final int index = random.nextInt(variantIds.size());
					variant = settings.getVariant(variantIds.get(index));
				} else {
					variant = settings.createEmptyVariant("", data.spec);
				}

				final FleetMemberAPI member = settings.createFleetMember(FleetMemberType.SHIP, variant);
				final RepairTrackerAPI repair = member.getRepairTracker();
				member.setShipName(fData.pickShipName(member, random));
				fData.addFleetMember(member);
				repair.setCR(Math.max(repair.getMaxCR(), 0.5f));
			}

			final FleetParamsV3 officerParams = new FleetParamsV3();
			officerParams.officerNumberMult = 0.5f;
			officerParams.officerLevelBonus = -2;
			officerParams.random = random;
			officerParams.timestamp = route.getTimestamp();

			FleetFactoryV3.addCommanderAndOfficers(fleet, officerParams, random);

			if (fleet.getFlagship() != null && fleet.getFlagship().getStatus() != null) {
				fleet.getFlagship().getStatus().updateNumStatusesFromMember();
			}

			for (FleetMemberAPI member : fData.getMembersListCopy()) {
				member.getRepairTracker().setCR(member.getRepairTracker().getMaxCR());
			}

			if (Misc.isPirateFaction(fleet.getFaction())) {
				fleet.getMemoryWithoutUpdate().set(MemFlags.MEMORY_KEY_FORCE_TRANSPONDER_OFF, true);
			}

			if (mission.smuggling) {
				fleet.getMemoryWithoutUpdate().set(MemFlags.MEMORY_KEY_SMUGGLER, true);
				Misc.makeLowRepImpact(fleet, "smuggler");
			} else {
				fleet.getMemoryWithoutUpdate().set(MemFlags.MEMORY_KEY_TRADE_FLEET, true);
			}

			final DefaultFleetInflaterParams p = new DefaultFleetInflaterParams();
			p.quality = route.getQualityOverride() + Misc.getShipQuality(src, factionId);
			p.persistent = true;
			p.seed = random.nextLong();
			p.mode = ShipPickMode.ALL;
			p.timestamp = route.getTimestamp();
			p.factionId = factionId;

			fleet.setInflater(Misc.getInflater(fleet, p));
			fData.setOnlySyncMemberLists(false);
			fData.sort();
			fleet.forceSync();
		}

		log.info("Created trade fleet with " + selectedAmount + " ships for market [" + src.getName() + "]");

		return fleet;
	}

	public final void reportBattleOccurred(CampaignFleetAPI fleet, CampaignFleetAPI primaryWinner, BattleAPI battle) {
		final RouteData route = RouteManager.getInstance().getRoute(getRouteSourceId(), fleet);
		if (route == null || route.isExpired() || !(route.getCustom() instanceof TradeMission mission)) return;

		if (fleet.getFleetData().getNumMembers() < 1) {
			onRouteLost(route, mission);
			return;
		}

		final CargoAPI cargo = fleet.getCargo();
		final float cargoRemainingRatio = (cargo.getMaxCapacity() / mission.cargoAmount) / mission.spawnedFleetCargoCapRatio;
		final float fuelRemainingRatio = (cargo.getMaxFuel() / mission.fuelAmount) / mission.spawnedFleetFuelCapRatio;
		final float crewRemainingRatio = (cargo.getFreeCrewSpace() / mission.crewAmount) / mission.spawnedFleetCrewCapRatio;
		mission.setSpawnedFleetCapRatios(cargo);

		for (ComTradeFlow com : mission.cargo) {
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
		if (route == null || route.isExpired() || !(route.getCustom() instanceof TradeMission mission)) return;

		switch (reason) {
		case REACHED_DESTINATION:
			mission.spawnedFleetFinishedJob = true;
			break;

		case DESTROYED_BY_BATTLE, NO_MEMBERS:
			onRouteLost(route, mission);
			break;
	
		default: break;
		}
	}

	public final boolean shouldRepeat(RouteData route) {
		return false;
	}

	public final void reportAboutToBeDespawnedByRouteManager(RouteData route) {
		if (route.getCustom() instanceof TradeMission mission) {
			mission.spawnedFleetFinishedJob = true;
		}
	}

	private static final void onRouteLost(RouteData route, TradeMission mission) {
		mission.status = MissionStatus.LOST;
		mission.spawnedFleetFinishedJob = true;
		RouteManager.getInstance().removeRoute(route);
			
		ShippingDisruption.getDisruption(mission.src).addShippingLost(1);
		ShippingDisruption.getDisruption(mission.src).notifyDisrupted(ShippingDisruption.ACCESS_LOSS_DURATION);
	}
}
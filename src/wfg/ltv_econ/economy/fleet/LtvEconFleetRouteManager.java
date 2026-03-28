package wfg.ltv_econ.economy.fleet;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.apache.log4j.Logger;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.SettingsAPI;
import com.fs.starfarer.api.campaign.BattleAPI;
import com.fs.starfarer.api.campaign.CampaignEventListener.FleetDespawnReason;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.campaign.econ.CommodityOnMarketAPI;
import com.fs.starfarer.api.campaign.econ.CommoditySpecAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.campaign.listeners.FleetEventListener;
import com.fs.starfarer.api.impl.campaign.command.WarSimScript;
import com.fs.starfarer.api.impl.campaign.command.WarSimScript.LocationDanger;
import com.fs.starfarer.api.impl.campaign.econ.ShippingDisruption;
import com.fs.starfarer.api.impl.campaign.fleets.BaseRouteFleetManager;
import com.fs.starfarer.api.impl.campaign.fleets.EconomyFleetAssignmentAI;
import com.fs.starfarer.api.impl.campaign.fleets.EconomyFleetAssignmentAI.CargoQuantityData;
import com.fs.starfarer.api.impl.campaign.fleets.EconomyFleetAssignmentAI.EconomyRouteData;
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
import com.fs.starfarer.api.impl.campaign.intel.misc.TradeFleetDepartureIntel;
import com.fs.starfarer.api.impl.campaign.rulecmd.KantaCMD;
import com.fs.starfarer.api.impl.campaign.shared.SharedData;
import com.fs.starfarer.api.util.Misc;
import com.fs.starfarer.api.util.TimeoutTracker;
import com.fs.starfarer.api.util.WeightedRandomPicker;

import wfg.ltv_econ.economy.engine.EconomyInfo;
import wfg.native_ui.util.ArrayMap;

public class LtvEconFleetRouteManager extends BaseRouteFleetManager
    implements FleetEventListener
{
    private static final SettingsAPI settings = Global.getSettings();
    private static final int maxEconFleets = settings.getInt("maxEconFleets");
    private static final float minEconSpawnInterval = settings.getFloat("minEconSpawnIntervalPerMarket");
    private static final String SOURCE_ID = "econ";
    private static final int ROUTE_SRC_LOAD = 1;
	private static final int ROUTE_TRAVEL_DST = 2;
	private static final int ROUTE_TRAVEL_WS = 3;
	private static final int ROUTE_RESUPPLY_WS = 4;
	private static final int ROUTE_DST_UNLOAD = 5;
	private static final int ROUTE_DST_LOAD = 6;
	private static final int ROUTE_TRAVEL_BACK_WS = 7;
	private static final int ROUTE_RESUPPLY_BACK_WS = 8;
	private static final int ROUTE_TRAVEL_SRC = 9;
	private static final int ROUTE_SRC_UNLOAD = 10;
	private static final Logger log = Global.getLogger(LtvEconFleetRouteManager.class);
	
	private final TimeoutTracker<String> recentlySentTradeFleet = new TimeoutTracker<String>();
	
	public LtvEconFleetRouteManager() {
		super(0.2f, 0.3f);
	}

	@Override
	public void advance(float delta) {
		super.advance(delta);
		
		final float days = Global.getSector().getClock().convertToDays(delta);
		recentlySentTradeFleet.advance(days);
	}

	protected String getRouteSourceId() {
		return SOURCE_ID;
	}
	
	protected int getMaxFleets() {
        // TODO change later
		final int numMarkets = Global.getSector().getEconomy().getNumMarkets();
		final int maxBasedOnMarkets = numMarkets * 2;
		return Math.min(maxBasedOnMarkets, maxEconFleets);
	}
	
	protected void addRouteFleetIfPossible() {
		final MarketAPI from = pickSourceMarket();
		final MarketAPI to = pickDestMarket(from);
		if (from == null || to == null) return;
			
        final EconomyRouteData data = createData(from, to);
        if (data == null) return;
        
        log.info("Added trade fleet route from " + from.getName() + " to " + to.getName());
        
        // TODO Independent trader change should depend on the newly introduced faction trade fleet mechanics
        final boolean canBeIndependent = !from.getFaction().isHostileTo(Factions.INDEPENDENT) && !to.getFaction().isHostileTo(Factions.INDEPENDENT);
        final OptionalFleetData extra = new OptionalFleetData(from);
        final float tier = data.size;
        final float stability = from.getStabilityValue();
        String factionId = from.getFactionId();
        if (canBeIndependent && (float) Math.random() * 10f > stability + tier) {
            factionId = Factions.INDEPENDENT;
        } else if (data.smuggling) {
            factionId = Factions.INDEPENDENT;
        }
        extra.factionId = factionId;
        
        final RouteData route = RouteManager.getInstance().addRoute(
            getRouteSourceId(), from, Misc.genRandomSeed(), extra, this
        );
        route.setCustom(data);
        
        final StarSystemAPI sysFrom = data.from.getStarSystem();
        final StarSystemAPI sysTo = data.to.getStarSystem();
        
        EconomyFleetRouteManager.ENEMY_STRENGTH_CHECK_EXCLUDE_PIRATES =
            (from.getFaction().isPlayerFaction() || to.getFaction().isPlayerFaction()) &&
            PiracyRespiteScript.playerHasPiracyRespite() ? true : false;
        
        final LocationDanger dFrom = WarSimScript.getDangerFor(factionId, sysFrom);
        final LocationDanger dTo = WarSimScript.getDangerFor(factionId, sysTo);
        
        LocationDanger danger = dFrom.ordinal() > dTo.ordinal() ? dFrom : dTo;
        
        if (sysFrom != null && sysFrom.isCurrentLocation()) {
            // the player is in the from location, don't auto-lose the trade fleet. Let it get destroyed by actual fleets, if it does
            danger = LocationDanger.NONE;
        }
        
        // TODO modify to relfect smuggling changes
        float pLoss = EconomyFleetRouteManager.DANGER_LOSS_PROB.get(danger);
        if (data.smuggling) pLoss *= 0.5;
        if ((float) Math.random() < pLoss) {
            final boolean returning = (float) Math.random() < 0.5f; 
            applyLostShipping(data, returning, true, true, true);
            RouteManager.getInstance().removeRoute(route);
            return;
        }
        
        // TODO modify orbiting dur
        float orbitDays = 2f + (float) Math.random() * 3f;

        // TODO modify stops
        orbitDays = data.size * (0.75f + (float) Math.random() * 0.5f);
        route.addSegment(new RouteSegment(ROUTE_SRC_LOAD, orbitDays, from.getPrimaryEntity()));
        route.addSegment(new RouteSegment(ROUTE_TRAVEL_DST, from.getPrimaryEntity(), to.getPrimaryEntity()));
        route.addSegment(new RouteSegment(ROUTE_DST_UNLOAD, orbitDays * 0.5f, to.getPrimaryEntity()));
        route.addSegment(new RouteSegment(ROUTE_DST_LOAD, orbitDays * 0.5f, to.getPrimaryEntity()));
        route.addSegment(new RouteSegment(ROUTE_TRAVEL_SRC, to.getPrimaryEntity(), from.getPrimaryEntity()));
        route.addSegment(new RouteSegment(ROUTE_SRC_UNLOAD, orbitDays, from.getPrimaryEntity()));
        
        setDelayAndSendMessage(route);
        
        recentlySentTradeFleet.add(from.getId(), minEconSpawnInterval);
	}
	
	protected void setDelayAndSendMessage(RouteData route) {
		final EconomyRouteData data = (EconomyRouteData) route.getCustom();
		
		final float baseDelay;
		if (data.size <= 4f) {
			baseDelay = 5f;
		} else if (data.size <= 6f) {
			baseDelay = 10f;
		} else {
			baseDelay = 15f;
		}
		final float delay = baseDelay * 0.75f + (float) Math.random() * 0.5f;
		route.setDelay((int) delay);

		if (!Factions.PLAYER.equals(route.getFactionId())) {
			// queues itself
			new TradeFleetDepartureIntel(route);
		}
	}
	
	public MarketAPI pickSourceMarket() {
		final var markets = new WeightedRandomPicker<MarketAPI>();
        // TODO modify market qualification constraints later
		for (MarketAPI market : EconomyInfo.getMarketsCopy()) {
			if (market.isHidden() || !market.hasSpaceport()) continue;

			if (SharedData.getData().getMarketsWithoutTradeFleetSpawn().contains(market.getId())) continue;
			
			if (recentlySentTradeFleet.contains(market.getId())) continue;
			
            // TODO Modify weights later
			markets.add(market, market.getSize());
		}
		return markets.pick();
	}
	
	public MarketAPI pickDestMarket(MarketAPI from) {
		if (from == null) return null;
		
		final var markets = new WeightedRandomPicker<MarketAPI>();

        // TODO modify commodity selection logic to use LTV Economy instead
		final var relevant = new ArrayList<CommodityOnMarketAPI>();
		for (CommodityOnMarketAPI com : from.getAllCommodities()) {
			if (com.isNonEcon()) continue;
			
			int exported = Math.min(com.getAvailable(), com.getMaxSupply());
			int imported = Math.max(0, com.getMaxDemand() - exported);
			if (imported > 0 || exported > 0) {
				relevant.add(com);
			}
		}
		
		for (MarketAPI market : Global.getSector().getEconomy().getMarketsInGroup(from.getEconGroup())) {
			if (market.isHidden() || !market.hasSpaceport() || market == from ||
                SharedData.getData().getMarketsWithoutTradeFleetSpawn().contains(market.getId())
            ) continue;
			
            // TODO Modify limits later
			int shipping = Misc.getShippingCapacity(market, market.getFaction() == from.getFaction());
			if (shipping <= 0) continue;
			
            // TODO modify weight logic also use LTV Econ data
			float w = 0f;
			for (CommodityOnMarketAPI com : relevant) {
				int exported = Math.min(com.getAvailable(), com.getMaxSupply());
				exported = Math.min(exported, shipping);
				int imported = Math.max(0, com.getMaxDemand() - exported);
				imported = Math.min(imported, shipping);
				
				CommodityOnMarketAPI other = market.getCommodityData(com.getId());
				exported = Math.min(exported, other.getMaxDemand() - other.getMaxSupply());
				if (exported < 0) exported = 0;
				imported = Math.min(imported, Math.min(other.getAvailable(), other.getMaxSupply()));
				
				w += imported;
				w += exported;
			}
			
			if (from.getFaction().isHostileTo(market.getFaction())) {
				w *= 0.25f;
			}
			markets.add(market, w);
		}
		
		return markets.pick();
	}
	
    // TODO modify completely to use LTV Econ values
	public static EconomyRouteData createData(MarketAPI from, MarketAPI to) {
		final EconomyRouteData smuggling = new EconomyRouteData();
		smuggling.from = from;
		smuggling.to = to;
		smuggling.smuggling = true;
		
		final EconomyRouteData legal = new EconomyRouteData();
		legal.from = from;
		legal.to = to;
		legal.smuggling = false;
		
		float legalTotal = 0;
		float smugglingTotal = 0;
		
		List<CommodityOnMarketAPI> relevant = new ArrayList<CommodityOnMarketAPI>();
		for (CommodityOnMarketAPI com : from.getAllCommodities()) {
			if (com.isNonEcon()) continue;
			CommodityOnMarketAPI orig = com;
			int exported = Math.min(com.getAvailable(), com.getMaxSupply());
			if (!com.getCommodity().isPrimary()) {
				com = from.getCommodityData(com.getCommodity().getDemandClass());
			}
				
			int imported = Math.max(0, com.getMaxDemand() - exported);
			if (imported > 0 || exported > 0) {
				relevant.add(orig);
			}
		}
		
		int shipping = Misc.getShippingCapacity(from, to.getFaction() == from.getFaction());
		for (CommodityOnMarketAPI com : relevant) {
			CommodityOnMarketAPI orig = com;
			int exported = Math.min(com.getAvailable(), com.getMaxSupply());
			exported = Math.min(exported, shipping);
			
			if (!com.getCommodity().isPrimary()) {
				com = from.getCommodityData(com.getCommodity().getDemandClass());
			}
			
			int imported = Math.max(0, com.getMaxDemand() - exported);
			imported = Math.min(imported, shipping);
			if (orig != com) imported = 0;
			
			CommodityOnMarketAPI other = to.getCommodityData(com.getId());
			exported = Math.min(exported, other.getMaxDemand() - other.getMaxSupply());
			if (exported < 0) exported = 0;
			imported = Math.min(imported, Math.min(other.getAvailable(), other.getMaxSupply()));
			
			if (imported < 0) imported = 0;
			
			if (imported <= 0 && exported <= 0) continue;
			
			boolean illegal = com.getCommodityMarketData().getMarketShareData(from).isSourceIsIllegal() ||
							  com.getCommodityMarketData().getMarketShareData(to).isSourceIsIllegal() ||
							  from.getFaction().isHostileTo(to.getFaction());
			
			if (imported > exported) {
				if (illegal) {
					smuggling.addReturn(orig.getId(), imported);
					smugglingTotal += imported;
				} else {
					legal.addReturn(orig.getId(), imported);
					legalTotal += imported;
				}
			} else {
				if (illegal) {
					smuggling.addDeliver(orig.getId(), exported);
					smugglingTotal += exported;
				} else {
					legal.addDeliver(orig.getId(), exported);
					legalTotal += exported;
				}
			}
		}

		Comparator<CargoQuantityData> comp = new Comparator<CargoQuantityData>() {
			public int compare(CargoQuantityData o1, CargoQuantityData o2) {
				if (o1.getCommodity().isPersonnel() && !o2.getCommodity().isPersonnel()) {
					return 1;
				}
				if (o2.getCommodity().isPersonnel() && !o1.getCommodity().isPersonnel()) {
					return -1;
				}
				return o2.units - o1.units;
			}
		};
		Collections.sort(legal.cargoDeliver, comp); 
		Collections.sort(legal.cargoReturn, comp); 
		Collections.sort(smuggling.cargoDeliver, comp); 
		Collections.sort(smuggling.cargoReturn, comp);
		
		if (smugglingTotal <= 0 && legalTotal <= 0) return null;
		
		EconomyRouteData data = null;
		if ((float) Math.random() * (smugglingTotal + legalTotal) < smugglingTotal) {
			data = smuggling;
		} else {
			data = legal;
		}
		
		while (data.cargoDeliver.size() > 4) {
			data.cargoDeliver.remove(4);
		}
		while (data.cargoReturn.size() > 4) {
			data.cargoReturn.remove(4);
		}
		
		float max = 0f;
		for (CargoQuantityData curr : data.cargoDeliver) {
			if (curr.units > max) max = curr.units;
		}
		for (CargoQuantityData curr : data.cargoReturn) {
			if (curr.units > max) max = curr.units;
		}
		
		int types = Math.max(data.cargoDeliver.size(), data.cargoReturn.size());
		if (types >= 3) {
			data.size++;
		}
		if (types >= 4) {
			data.size++;
		}
		
		data.size = max;
		
		
		return data;
	}

    // TODO figure out what this is used for and modify accordingly
	public boolean shouldCancelRouteAfterDelayCheck(RouteData route) {
		final String factionId = route.getFactionId();
		
        if (route.getCustom() instanceof EconomyRouteData data &&
            data.smuggling
        ) { return false;}
		
		if (factionId != null && route.getMarket() != null &&
				route.getMarket().getFaction().isHostileTo(factionId)) {
			return true;
		}
		return false;
	}
	
	public CampaignFleetAPI spawnFleet(RouteData route) {
		Random random = new Random();
		if (route.getSeed() != null) {
			random = new Random(route.getSeed());
		}
		
		CampaignFleetAPI fleet = createTradeRouteFleet(route, random);
		if (fleet == null) return null;;
		
		if (KantaCMD.playerHasProtection() && route.custom instanceof EconomyRouteData) {
			EconomyRouteData data = (EconomyRouteData) route.custom;
			if (data.from != null && data.to != null) {
				if (data.from.getFaction().isPlayerFaction() || data.to.getFaction().isPlayerFaction()) {
					fleet.getMemoryWithoutUpdate().set(MemFlags.FLEET_IGNORED_BY_FACTION, Factions.PIRATES);
				}
			}
		}
		
		fleet.addEventListener(this);
		
		fleet.addScript(new EconomyFleetAssignmentAI(fleet, route));
		return fleet;
	}
	
	
	public static String getFleetTypeIdForTier(float tier, boolean smuggling) {
		String type = FleetTypes.TRADE;
		if (tier <= 3) type = FleetTypes.TRADE_SMALL; 
		if (smuggling) {
			type = FleetTypes.TRADE_SMUGGLER;
		}
		return type;
	}

	public static CampaignFleetAPI createTradeRouteFleet(RouteData route, Random random) {
		EconomyRouteData data = (EconomyRouteData) route.getCustom();

		MarketAPI from = data.from;
		MarketAPI to = data.to;
		
		float tier = data.size;
		
		if (data.smuggling && tier > 4) {
			tier = 4;
		}
		String factionId = route.getFactionId();

		float total = 0f;
		float fuel = 0f;
		float cargo = 0f;
		float personnel = 0f;
		
		List<CargoQuantityData> all = new ArrayList<CargoQuantityData>();
		all.addAll(data.cargoDeliver);
		all.addAll(data.cargoReturn);
		for (CargoQuantityData curr : all) {
			CommoditySpecAPI spec = curr.getCommodity();
			if (spec.isMeta()) continue;
			
			total += curr.units;
			if (spec.hasTag(Commodities.TAG_PERSONNEL)) {
				personnel += curr.units;
			} else if (spec.getId().equals(Commodities.FUEL)) {
				fuel += curr.units;
			} else {
				cargo += curr.units;
			}
		}

		if (total < 1f) total = 1f;
		
		float fuelFraction = fuel / total;
		float personnelFraction = personnel / total;
		float cargoFraction = cargo / total;
		
		if (fuelFraction + personnelFraction + cargoFraction > 0) {
			float mult = 1f / (fuelFraction + personnelFraction + cargoFraction);
			fuelFraction *= mult;
			personnelFraction *= mult;
			cargoFraction *= mult;
		}
		
		log.info("Creating trade fleet of tier " + tier + " for market [" + from.getName() + "]");
		
		float stabilityFactor = 1f + from.getStabilityValue() / 20f;
		
		float combat = Math.max(1f, tier * stabilityFactor * 0.5f) * 10f;
		float freighter = tier * 2f * cargoFraction * 3f;
		float tanker = tier * 2f * fuelFraction * 3f;
		float transport = tier * 2f * personnelFraction * 3f;
		float liner = 0f;
		
		float utility = 0f;
		
		String type = getFleetTypeIdForTier(tier, data.smuggling);
		if (data.smuggling) {
			freighter *= 0.5f;
			tanker *= 0.5f;
			transport *= 0.5f;
			liner *= 0.5f;
		}
		
		FleetParamsV3 params = new FleetParamsV3(
				from,
				null, // locInHyper
				factionId,
				route.getQualityOverride(), // qualityOverride
				type,
				combat, // combatPts
				freighter, // freighterPts 
				tanker, // tankerPts
				transport, // transportPts
				liner, // linerPts
				utility, // utilityPts
				0f //-0.5f // qualityBonus
		);
		params.timestamp = route.getTimestamp();
		params.onlyApplyFleetSizeToCombatShips = true;
		params.maxShipSize = 3;
		params.officerLevelBonus = -2;
		params.officerNumberMult = 0.5f;
		params.random = random;
		CampaignFleetAPI fleet = FleetFactoryV3.createFleet(params);
		
		if (fleet == null || fleet.isEmpty()) return null;
		
		if (Misc.isPirateFaction(fleet.getFaction())) {
			fleet.getMemoryWithoutUpdate().set(MemFlags.MEMORY_KEY_FORCE_TRANSPONDER_OFF, true);
		}
		
		if (data.smuggling) {
			fleet.getMemoryWithoutUpdate().set(MemFlags.MEMORY_KEY_SMUGGLER, true);
			Misc.makeLowRepImpact(fleet, "smuggler");
		} else {
			fleet.getMemoryWithoutUpdate().set(MemFlags.MEMORY_KEY_TRADE_FLEET, true);
		}
		
		data.cargoCap = fleet.getCargo().getMaxCapacity();
		data.fuelCap = fleet.getCargo().getMaxFuel();
		data.personnelCap = fleet.getCargo().getMaxPersonnel();
		
		return fleet;
		
	}

	public void reportBattleOccurred(CampaignFleetAPI fleet, CampaignFleetAPI primaryWinner, BattleAPI battle) {
		// already reduced by losses taken
		//System.out.println("Cargo: " + fleet.getCargo().getMaxCapacity());
		
		RouteData route = RouteManager.getInstance().getRoute(getRouteSourceId(), fleet);
		if (route == null || !(route.getCustom() instanceof EconomyRouteData)) return;
		
		if (route.isExpired()) return;
		
		EconomyRouteData data = (EconomyRouteData) route.getCustom();
		
		float cargoCap = fleet.getCargo().getMaxCapacity();
		float fuelCap = fleet.getCargo().getMaxFuel();
		float personnelCap = fleet.getCargo().getMaxPersonnel();
		
		float lossFraction = 0.34f;
		
		//boolean returning = route.getCurrentIndex() >= 3;
		boolean returning = false;
		if (route.getCurrent() != null && route.getCurrentSegmentId() >= ROUTE_DST_LOAD) {
			returning = true;
		}

		// whether it lost enough carrying capacity to count as an economic loss 
		// of that commodity at destination markets
		boolean lostCargo = data.cargoCap * lossFraction > cargoCap; 
		boolean lostFuel = data.fuelCap * lossFraction > fuelCap; 
		boolean lostPersonnel = data.personnelCap * lossFraction > personnelCap;
		
		// set to 0f so that the loss doesn't happen multiple times for a commodity
		if (lostCargo) data.cargoCap = 0f;
		if (lostFuel) data.fuelCap = 0f;
		if (lostPersonnel) data.personnelCap = 0f;
		
		applyLostShipping(data, returning, lostCargo, lostFuel, lostPersonnel);
		
		// if it's lost all 3 capacities, also trigger a general shipping capacity loss at market
		boolean allThreeLost = true;
		allThreeLost &= data.cargoCap <= 0f || lostCargo;
		allThreeLost &= data.fuelCap <= 0f || lostFuel;
		allThreeLost &= data.personnelCap <= 0f || lostPersonnel;
		
		boolean applyAccessLoss = allThreeLost;
		if (applyAccessLoss) {
			ShippingDisruption.getDisruption(data.from).addShippingLost(data.size);
			ShippingDisruption.getDisruption(data.from).notifyDisrupted(ShippingDisruption.ACCESS_LOSS_DURATION);
		}
		
	}
	
	public static void applyLostShipping(EconomyRouteData data, boolean returning, boolean cargo, boolean fuel, boolean personnel) {
		if (!cargo && !fuel && !personnel) return;
		
		int penalty = 1;
		int penalty2 = 2;
		if (!returning) {
			for (CargoQuantityData curr : data.cargoDeliver) {
				CommodityOnMarketAPI com = data.to.getCommodityData(curr.cargo);
				if (!fuel && com.isFuel()) continue;
				if (!personnel && com.isPersonnel()) continue;
				if (!cargo && !com.isFuel() && !com.isPersonnel()) continue;
				
				com.getAvailableStat().addTemporaryModFlat(
						ShippingDisruption.ACCESS_LOSS_DURATION,
						ShippingDisruption.COMMODITY_LOSS_PREFIX + Misc.genUID(), "Recent incoming shipment lost", -penalty2);
				
				ShippingDisruption.getDisruption(data.to).notifyDisrupted(ShippingDisruption.ACCESS_LOSS_DURATION);
			}
		}
		for (CargoQuantityData curr : data.cargoReturn) {
			CommodityOnMarketAPI com = data.from.getCommodityData(curr.cargo);
			if (!fuel && com.isFuel()) continue;
			if (!personnel && com.isPersonnel()) continue;
			if (!cargo && !com.isFuel() && !com.isPersonnel()) continue;
			
			com.getAvailableStat().addTemporaryModFlat(
					ShippingDisruption.ACCESS_LOSS_DURATION, 
					ShippingDisruption.COMMODITY_LOSS_PREFIX + Misc.genUID(), "Recent incoming shipment lost", -penalty2);
			
			ShippingDisruption.getDisruption(data.from).notifyDisrupted(ShippingDisruption.ACCESS_LOSS_DURATION);
		}
	}
	
	public void reportFleetDespawnedToListener(CampaignFleetAPI fleet, FleetDespawnReason reason, Object param) {
		
	}

	public boolean shouldRepeat(RouteData route) {
		return false;
	}
	
	public void reportAboutToBeDespawnedByRouteManager(RouteData route) {
		
	}
}
package wfg.ltv_econ.economy.fleet;

import static wfg.native_ui.util.Globals.settings;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.CargoAPI;
import com.fs.starfarer.api.campaign.FleetDataAPI;
import com.fs.starfarer.api.campaign.FactionAPI.ShipPickParams;
import com.fs.starfarer.api.campaign.econ.CommoditySpecAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.fleet.ShipRolePick;
import com.fs.starfarer.api.impl.campaign.fleets.RouteManager.RouteData;
import com.fs.starfarer.api.impl.campaign.fleets.RouteManager.RouteSegment;
import com.fs.starfarer.api.impl.campaign.ids.Commodities;
import com.fs.starfarer.api.impl.campaign.ids.Factions;
import com.fs.starfarer.api.impl.campaign.ids.ShipRoles;
import com.fs.starfarer.api.impl.campaign.procgen.themes.RouteFleetAssignmentAI;
import com.fs.starfarer.api.util.IntervalUtil;
import com.fs.starfarer.api.util.Misc;
import com.fs.starfarer.api.util.WeightedRandomPicker;

import wfg.ltv_econ.economy.commodity.TradeCom;
import wfg.ltv_econ.economy.fleet.TradeMission.MissionStatus;

public class LtvEconFleetAssignmentAI extends RouteFleetAssignmentAI {
    private static final int MAX_MOTHBALLED_SHIPS = 15;

    private final String origFaction;
    private final IntervalUtil factionChangeTracker;

    public LtvEconFleetAssignmentAI(CampaignFleetAPI fleet, RouteData route) {
        super(fleet, route);

        if (getMission().smuggling) {
            origFaction = route.getFactionId();
            factionChangeTracker = new IntervalUtil(0.1F, 0.3F);
            factionChangeTracker.forceIntervalElapsed();
            doSmugglingFactionChangeCheck(0.1F);
        } else {
            origFaction = null;
            factionChangeTracker = null;
        }
    }

    @Override
    protected String getStartingActionText(RouteSegment segment) {
        if (getMission().src == null) return super.getStartingActionText(segment);
        return "loading " + getCargoList(segment) + " at " + getMission().src.getName();
    }

    @Override
    protected String getEndingActionText(RouteSegment segment) {
        if (getMission().src == null) return super.getEndingActionText(segment);
        return "unloading " + getCargoList(segment) + " at " + getMission().src.getName();
    }

    @Override
    protected String getTravelActionText(RouteSegment segment) {
        if (getMission().dest == null) return super.getTravelActionText(segment);
        return "delivering " + getCargoList(segment) + " to " + getMission().dest.getName();
    }

    @Override
    protected String getInSystemActionText(RouteSegment segment) {
        if (getMission().dest == null) return super.getInSystemActionText(segment);
        if (segment.getId() == MissionStatus.IN_DST_ORBIT_UNLOADING.ordinal()) {
            return "unloading " + getCargoList(segment) + " at " + getMission().dest.getName();
        }

        return super.getInSystemActionText(segment);
    }

    @Override
    protected void addEndingAssignment(RouteSegment current, boolean justSpawned) {
        super.addEndingAssignment(current, justSpawned);
        updateCargo(current);
    }

    @Override
    protected void addLocalAssignment(RouteSegment current, boolean justSpawned) {
        super.addLocalAssignment(current, justSpawned);
        updateCargo(current);
    }

    @Override
    protected void addStartingAssignment(RouteSegment current, boolean justSpawned) {
        super.addStartingAssignment(current, justSpawned);
        updateCargo(current);
    }

    @Override
    protected void addTravelAssignment(RouteSegment current, boolean justSpawned) {
        super.addTravelAssignment(current, justSpawned);
        updateCargo(current);
    }

    protected TradeMission getMission() {
        return getMission(route);
    }

    public static final TradeMission getMission(RouteData route) {
        return (TradeMission) route.getCustom();
    }

    protected String getCargoList(RouteSegment segment) {
        return getCargoList(route, segment);
    }

    public static final String getCargoList(RouteData route, RouteSegment segment) {
        return getCargoList(getMission(route).cargo);
    }

    @Override
    public void advance(float delta) {
        super.advance(delta);
        doSmugglingFactionChangeCheck(delta);
    }

    private final void doSmugglingFactionChangeCheck(float delta) {
        final TradeMission mission = getMission();
        if (!mission.smuggling) return;
        
        final float days = Global.getSector().getClock().convertToDays(delta);
        factionChangeTracker.advance(days);

        if (fleet.getAI() == null || !factionChangeTracker.intervalElapsed()) return;
        
        final MarketAPI align;
        if (mission.src.getStarSystem() == fleet.getContainingLocation()) {
            align = mission.src;
        } else if (mission.dest.getStarSystem() == fleet.getContainingLocation()) {
            align = mission.dest;
        } else {
            align = null;
        }

        String targetFac;
        if (align != null) {
            targetFac = origFaction;
            boolean hostile = align.getFaction().isHostileTo(targetFac);
            if (hostile) {
                targetFac = Factions.INDEPENDENT;
                hostile = align.getFaction().isHostileTo(targetFac);
            }

            if (hostile) {
                targetFac = align.getFactionId();
            }

            if (!fleet.getFaction().getId().equals(targetFac)) {
                fleet.setFaction(targetFac, true);
            }
        } else {
            targetFac = origFaction;
            if (fleet.isInHyperspace()) targetFac = Factions.INDEPENDENT;

            if (!fleet.getFaction().getId().equals(targetFac)) {
                fleet.setFaction(targetFac, true);
            }
        }
    }

    private static final String getCargoList(List<TradeCom> cargo) {
        if (cargo.isEmpty()) return "";
        final List<TradeCom> sorted = new ArrayList<>(cargo);

        sorted.sort((a, b) -> Double.compare(b.amount, a.amount));
        List<String> strings = new ArrayList<>();
        for (TradeCom flow : sorted) {
            final CommoditySpecAPI spec = settings.getCommoditySpec(flow.comID);

            if (spec.getId().equals(Commodities.SHIPS)) {
                strings.add("ship hulls");
            } else if (!spec.isMeta()) {
                strings.add(spec.getName().toLowerCase());
            }

            if (strings.size() >= 4) break;
        }

        if (cargo.size() > 4 && strings.size() > 2) {
            strings = new ArrayList<>(strings.subList(0, 2));
            strings.add("other commodities");
        }
        return Misc.getAndJoined(strings);
    }

    private final void updateCargo(RouteSegment segment) {
        if (route.isExpired()) return;

        final int id = segment.getId();
        final TradeMission mission = getMission();
        final CargoAPI cargo = fleet.getCargo();
        cargo.clear();

        if (id == MissionStatus.IN_SRC_ORBIT_LOADING.ordinal() || id == MissionStatus.IN_DST_ORBIT_UNLOADING.ordinal()) {
            syncMothballedShips(0f, null);
            return;
        }

        cargo.addFuel(Math.min(mission.fuelAmount * mission.spawnedFleetFuelCapRatio, cargo.getMaxFuel()));

        float ships = 0f;
        for (TradeCom flow : mission.cargo) {
            final String cid = flow.comID;
            final double amount = flow.amount * mission.spawnedFleetCargoCapRatio;

            if (cid.equals(Commodities.FUEL) || cid.equals(Commodities.CREW) || cid.equals(Commodities.MARINES)) continue;
            if (cid.equals(Commodities.SHIPS)) {
                ships += amount;
                continue;
            }

            cargo.addCommodity(cid, (float) amount);
        }

        syncMothballedShips(ships, mission.src);
    }

    private final  void syncMothballedShips(float tons, MarketAPI market) {
        final FleetDataAPI fData = fleet.getFleetData();
        for (FleetMemberAPI member : fData.getMembersListCopy()) {
            if (member.isMothballed()) fData.removeFleetMember(member);
        }

        if (tons <= 0f) return;

        final Random random = route.getRandom();
        final TradeMission mission = getMission();
        final float shipValue = settings.getCommoditySpec(Commodities.SHIPS).getBasePrice();
        final ShipPickParams params = !mission.inFaction ? ShipPickParams.imported() : ShipPickParams.priority();

        final WeightedRandomPicker<String> roles = new WeightedRandomPicker<>(random);
        roles.add(ShipRoles.COMBAT_FREIGHTER_SMALL, 20f);
        roles.add(ShipRoles.FREIGHTER_SMALL, 20f);
        roles.add(ShipRoles.TANKER_SMALL, 10f);

        for (int i = 0; i < MAX_MOTHBALLED_SHIPS; i++) {

            if (i == 2) {
                roles.add(ShipRoles.COMBAT_FREIGHTER_MEDIUM, 20f * (i - 1));
                roles.add(ShipRoles.FREIGHTER_MEDIUM, 20f * (i - 1));
                roles.add(ShipRoles.TANKER_MEDIUM, 10f * (i - 1));
            }

            if (i == 5) {
                roles.add(ShipRoles.COMBAT_FREIGHTER_LARGE, 20f * (i - 2));
                roles.add(ShipRoles.FREIGHTER_LARGE, 20f * (i - 2));
                roles.add(ShipRoles.TANKER_LARGE, 10f * (i - 2));
            }

            for (ShipRolePick pick : market.pickShipsForRole(roles.pick(), params, random, null)) {
                final FleetMemberAPI member = fData.addFleetMember(pick.variantId);
                member.getRepairTracker().setMothballed(true);

                tons -= member.getBaseValue() / shipValue;
            }
            
            if (tons < 1f) break;
        }

        fData.sort();
    }
}
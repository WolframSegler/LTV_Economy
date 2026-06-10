package wfg.ltv_econ.economy.fleet;

import static wfg.native_ui.util.Globals.settings;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;

import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.FactionAPI.ShipPickMode;
import com.fs.starfarer.api.campaign.FleetDataAPI;
import com.fs.starfarer.api.combat.MutableStat;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.fleet.FleetMemberType;
import com.fs.starfarer.api.impl.campaign.fleets.DefaultFleetInflaterParams;
import com.fs.starfarer.api.impl.campaign.fleets.FleetFactoryV3;
import com.fs.starfarer.api.impl.campaign.fleets.FleetParamsV3;
import com.fs.starfarer.api.impl.campaign.ids.MemFlags;
import com.fs.starfarer.api.util.Misc;

import wfg.native_ui.util.ArrayMap;

public class FleetFactoryHelpers {

    /**
     * Populates a fleet with ships chosen from an allocation map, up to {@code targetShips} ships.
     * The allocation map is not modified; a copy is used internally.
     *
     * @param fleet empty fleet to fill (must have its faction already set)
     * @param allocatedShips hullId -> amount
     * @param targetShips maximum number of ships to spawn. -1 for no limit.
     * @param banPhaseShips determines whether if phase ships are to be excluded.
     * @param random random source for variant picking and weighted selection
     * @return number of ships actually added
     */
    public static final int populateFleetFromAllocation(
        CampaignFleetAPI fleet, ArrayMap<String, Integer> allocatedShips,
        int targetShips, boolean banPhaseShips, Random random
    ) {
        final FleetDataAPI fData = fleet.getFleetData();
        fData.setOnlySyncMemberLists(true);
        Misc.getSalvageSeed(fleet);

        final List<ShipHullSpecAPI> selection = new ArrayList<>();
        if (targetShips == -1) {
            for (Map.Entry<String, Integer> e : allocatedShips.singleEntrySet()) {
                final ShipHullSpecAPI spec = settings.getHullSpec(e.getKey());
                if (banPhaseShips && spec.isPhase()) continue;

                for (int i = 0; i < e.getValue(); i++) selection.add(spec);
            }

        } else {
            final ArrayMap<String, Integer> remaining = new ArrayMap<>(allocatedShips);

            while (selection.size() < targetShips) {
                double totalWeight = 0d;
                for (Map.Entry<String, Integer> e : remaining.singleEntrySet()) {
                    final ShipHullSpecAPI spec = settings.getHullSpec(e.getKey());
                    if (banPhaseShips && spec.isPhase()) continue;
    
                    totalWeight += e.getValue() * Math.max(1d, spec.getFleetPoints());
                }
                if (totalWeight <= 0d) break;
    
                final Iterator<Map.Entry<String, Integer>> it = remaining.entrySet().iterator();
                final double r = random.nextDouble() * totalWeight;
                double accum = 0d;
    
                while (it.hasNext()) {
                    final Map.Entry<String, Integer> e = it.next();
                    final ShipHullSpecAPI spec = settings.getHullSpec(e.getKey());
                    if (banPhaseShips && spec.isPhase()) continue;
    
                    accum += e.getValue() * Math.max(1d, spec.getFleetPoints());
                    if (accum >= r) {
                        selection.add(spec);
                        final int newCount = e.getValue() - 1;
                        if (newCount == 0) it.remove();
                        else e.setValue(newCount);
                        break;
                    }
                }
            }
        }

        for (ShipHullSpecAPI spec : selection) {
            final List<String> variantIds = settings.getHullIdToVariantListMap().get(spec.getHullId());
            final ShipVariantAPI variant;
            if (!variantIds.isEmpty()) {
                variant = settings.getVariant(variantIds.get(random.nextInt(variantIds.size())));
            } else {
                variant = settings.createEmptyVariant("", spec);
            }

            final FleetMemberAPI member = settings.createFleetMember(FleetMemberType.SHIP, variant);
            member.setShipName(fData.pickShipName(member, random));
            fData.addFleetMember(member);
            member.getStatus().setHullFraction(1f);
            member.getRepairTracker().setCR(Math.max(member.getRepairTracker().getMaxCR(), 0.5f));
        }

        fData.setOnlySyncMemberLists(false);
        return selection.size();
    }

    /**
     * Finalises a fleet after it has been populated with ships via
     * {@link #populateFleetFromAllocation} and before assigning mission‑specific
     * memory keys or AI. Handles officers, crew quality, CR, inflater, pirate transponders, and travel speed.
     *
     * @param fleet the fleet to configure
     * @param factionId fleet faction ID
     * @param quality ship quality value for the inflater (from route override + market quality)
     * @param timestamp fleet creation timestamp
     * @param officerNumberMult multiplier for number of officers. <b>default: </b> {@code 1f}
     * @param officerLevelBonus flat level bonus/penalty for officers.  <b>default: </b> {@code 0f}
     * @param random deterministic random source.
     * @param forcedMaxBurn sets the speed of all fleet members. -1 to disable it. 
     */
    public static void configureFleetAfterAllocation(
        CampaignFleetAPI fleet, String factionId, float quality,
        long timestamp, float officerNumberMult, int officerLevelBonus,
        Random random, int forcedMaxBurn
    ) {
        final FleetDataAPI fData = fleet.getFleetData();

        final FleetParamsV3 officerParams = new FleetParamsV3();
        officerParams.officerNumberMult = officerNumberMult;
        officerParams.officerLevelBonus = officerLevelBonus;
        officerParams.random = random;
        officerParams.timestamp = timestamp;
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

        final DefaultFleetInflaterParams p = new DefaultFleetInflaterParams();
        p.quality = quality;
        p.persistent = true;
        p.seed = random.nextLong();
        p.mode = ShipPickMode.ALL;
        p.timestamp = timestamp;
        p.factionId = factionId;
        fleet.setInflater(Misc.getInflater(fleet, p));

        if (forcedMaxBurn != -1) {
            for (FleetMemberAPI member : fData.getMembersListCopy()) {
                final MutableStat stat = member.getStats().getMaxBurnLevel();
                stat.unmodify();
                stat.setBaseValue(forcedMaxBurn);
            }
        }

        fData.setOnlySyncMemberLists(false);
        fData.sort();
        fleet.forceSync();
    }
}
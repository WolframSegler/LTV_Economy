package wfg.ltv_econ.util;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.impl.campaign.ids.Planets;
import com.fs.starfarer.api.impl.campaign.ids.Tags;

public class ConfigUtils {
    private static final String HIDDEN_SYSTEM_ID = "ltv_dynamic_ind_test_system";
    private static final String HIDDEN_PLANET_ID = "ltv_dynamic_ind_test_planet";
    
    public static final String TEST_FACTION_ID = "neutral";
    public static final String TEST_MARKET_1_ID = "ltv_dynamic_ind_test_market1";
    public static final String TEST_MARKET_2_ID = "ltv_dynamic_ind_test_market2";
    public static final int TEST_MARKET_SIZE = 6;

    public static final StarSystemAPI getOrCreateHiddenSystem() {
        final SectorAPI sector = Global.getSector();

        final StarSystemAPI old_system = sector.getStarSystem(HIDDEN_SYSTEM_ID);
        if (old_system != null) return old_system;

        final StarSystemAPI system = sector.createStarSystem(HIDDEN_SYSTEM_ID);
        system.setMaxRadiusInHyperspace(0f);
        system.addTag(Tags.THEME_HIDDEN);
        system.addTag(Tags.INVISIBLE_IN_CODEX);
        system.addTag(Tags.STAR_HIDDEN_ON_MAP);
        system.addTag(Tags.SYSTEM_CUT_OFF_FROM_HYPER);
        system.addTag(Tags.NON_CLICKABLE);
        system.addTag(Tags.NOT_RANDOM_MISSION_TARGET);
        system.addTag(Tags.NO_DROP);
        system.addTag(Tags.NO_ARMOR_SCHEMATIC);
        system.addTag(Tags.NO_BP_DROP);
        system.addTag(Tags.NO_ENTITY_TOOLTIP);
        system.addTag(Tags.NO_TOPOGRAPHY_SCANS);
        system.addTag(Tags.DO_NOT_RESPAWN_PLAYER_IN);

        system.getLocation().set(1_000_000f, 1_000_000f);

        if (system.getHyperspaceAnchor() != null) {
            sector.getHyperspace().removeEntity(system.getHyperspaceAnchor());
        }

        system.setEnteredByPlayer(false);
        system.setDoNotShowIntelFromThisLocationOnMap(true);

        return system;
    }

    public static final SectorEntityToken getOrCreatePlanetInHiddenSystem() {
        final StarSystemAPI system = getOrCreateHiddenSystem();
        final SectorEntityToken old_planet = system.getEntityById(HIDDEN_PLANET_ID);
        if (old_planet != null) return old_planet;

        return system.addPlanet(
            HIDDEN_PLANET_ID, system.getCenter(), HIDDEN_PLANET_ID, Planets.ARID, 0f, 1f, 1f, 10000000f
        );
    }

    /** Creates a new market each time */
    public static final MarketAPI createMarketOnPlanetInHiddenSystem(String marketID, int size) {
        final MarketAPI market = Global.getFactory().createMarket(marketID, marketID, size);
        final SectorEntityToken testPlanet = ConfigUtils.getOrCreatePlanetInHiddenSystem();
        market.setFactionId(TEST_FACTION_ID);
        market.setPrimaryEntity(testPlanet);
        market.setHidden(true);

        return market;
    }

    public static final MarketAPI getTestMarket1() {
        return createMarketOnPlanetInHiddenSystem(TEST_MARKET_1_ID, TEST_MARKET_SIZE);
    }

    /**
     * Size is one smaller than test market 1
     */
    public static final MarketAPI getTestMarket2() {
        return createMarketOnPlanetInHiddenSystem(TEST_MARKET_2_ID, TEST_MARKET_SIZE - 1);
    }
}
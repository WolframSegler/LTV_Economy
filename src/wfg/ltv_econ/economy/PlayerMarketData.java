package wfg.ltv_econ.economy;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Map;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.campaign.econ.MarketImmigrationModifier;
import com.fs.starfarer.api.combat.StatBonus;
import com.fs.starfarer.api.impl.campaign.ids.Commodities;
import com.fs.starfarer.api.impl.campaign.ids.Conditions;
import com.fs.starfarer.api.impl.campaign.ids.Stats;
import com.fs.starfarer.api.impl.campaign.population.PopulationComposition;

import rolflectionlib.util.RolfLectionUtil;
import wfg.ltv_econ.config.LaborConfig;
import wfg.ltv_econ.config.PolicyConfigLoader.PolicyConfig;
import wfg.ltv_econ.config.PolicyConfigLoader.PolicySpec;
import wfg.ltv_econ.config.EconConfig;
import wfg.ltv_econ.config.EventConfig;
import wfg.ltv_econ.config.EventConfig.EventSpec;
import wfg.ltv_econ.economy.commodity.CommodityCell;
import wfg.ltv_econ.economy.commodity.CommodityDomain;
import wfg.ltv_econ.economy.engine.EconomyEngine;
import wfg.ltv_econ.economy.registry.MarketFinanceRegistry;
import wfg.ltv_econ.intel.market.events.MarketEvent;
import wfg.ltv_econ.intel.market.policies.MarketPolicy;
import wfg.native_ui.util.Arithmetic;

public class PlayerMarketData implements Serializable, MarketImmigrationModifier {
    public final String marketID;
    public transient MarketAPI market;

    /**
     * Value must be between 0 and 1.
     */
    public float playerProfitRatio = 0f;

    public final StatBonus healthDelta = new StatBonus();
    public final StatBonus happinessDelta = new StatBonus();
    public final StatBonus socialCohesionDelta = new StatBonus();
    public final StatBonus classConsciousnessDelta = new StatBonus();

    public static final float BASELINE_VALUE = 50f;
    public static final float RoSV_Equalibrium = 1.3f;
    public static final String healthID = "health";
    public static final String happinessID = "happiness";
    public static final String socialCohesionID = "cohesion";
    public static final String classConscID = "classConsc";

    private float RoSV = LaborConfig.RoSV;
    private float popHealth = BASELINE_VALUE;
    private float popHappiness = BASELINE_VALUE;
    private float popSocialCohesion = BASELINE_VALUE;
    private float popClassConsciousness = 0f;

    private final ArrayList<MarketPolicy> policies = new ArrayList<>();
    private final ArrayList<MarketEvent> events = new ArrayList<>();

    public PlayerMarketData(String marketID) {
        this.marketID = marketID;
        readResolve();

        try {
            for (PolicySpec spec : PolicyConfig.map.values()) {
                final MarketPolicy policy = (MarketPolicy) RolfLectionUtil.instantiateClass(
                    RolfLectionUtil.getConstructor(spec.marketPolicyClass, null)
                );
                policies.add(policy);
                policy.spec = spec;
                policy.id = spec.id;
            }

            for (EventSpec spec : EventConfig.map.values()) {
                final MarketEvent event = (MarketEvent) RolfLectionUtil.instantiateClass(
                    RolfLectionUtil.getConstructor(spec.marketEventClass, null)
                );
                events.add(event);
                event.spec = spec;
                event.id = spec.id;
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to assign policies to "+marketID+". ", e);
        }
    }

    public Object readResolve() {
        market = Global.getSector().getEconomy().getMarket(marketID);

        return this;
    }

    public final float getRoSV() { return RoSV; }
    public final float getHealth() { return popHealth; }
    public final float getHappiness() { return popHappiness; }
    public final float getSocialCohesion() { return popSocialCohesion; }
    public final float getClassConsciousness() { return popClassConsciousness; }

    public final void setRoSV(float rate) { RoSV = rate; }
    public final void setHealth(float health) { popHealth = clamp(health); }
    public final void setHappiness(float happiness) { popHappiness = clamp(happiness); }
    public final void setSocialCohesion(float cohesion) { popSocialCohesion = clamp(cohesion); }
    public final void setClassConsciousness(float consciousness) { popClassConsciousness = clamp(consciousness); }
    public final ArrayList<MarketPolicy> getPolicies() { return policies; }
    public final ArrayList<MarketEvent> getEvents() { return events; }
    public final MarketPolicy getPolicy(String policyID) {
        for (MarketPolicy policy : policies) if (policy.id.equals(policyID)) return policy;
        return null;
    }
    public final MarketEvent getEvent(String eventID) {
        for (MarketEvent event : events) if (event.id.equals(eventID)) return event;
        return null;
    }
    public final long getWithdrawLimit() {
        return withdrewCreditsThisMonth ? 0l : Math.min(EconomyEngine.instance().getCredits(marketID),
            EconConfig.CREDIT_WITHDRAWAL_LIMIT
        );
    }

    public final void advance(int days) {
        for (MarketPolicy policy : policies) {
            if (policy.isActive(this)) policy.preAdvance(this);
        }
        for (MarketEvent event : events) {
            event.preAdvance(this);
        }

        advanceMarket(days);

        for (MarketPolicy policy : policies) {
            if (policy.isActive(this))  policy.postAdvance(this);
        }
        for (MarketEvent event : events) {
            event.postAdvance(this);
        }

        for (MarketPolicy policy : policies) {
            policy.advanceTime(this, days);
        }
    }
    public final void advance() {
        advance(1);
    }

    public boolean withdrewCreditsThisMonth = false;

    public final void endMonth() {
        withdrewCreditsThisMonth = false;
    }

    /**
     * Assumes end-of-month
     */
    public final float getEffectiveProfitRatio() {
        final long net = MarketFinanceRegistry.instance().getLedger(marketID).getNetLastMonth();
        if (net <= 0l) return 0f;

        final long endCredits = EconomyEngine.instance().getCredits(marketID);
        if (endCredits <= 0l) return 0f;

        return (float) Math.min(playerProfitRatio, endCredits / (double) net);
    }

    public final void apply() {
        for (MarketPolicy policy : policies) {
            if (policy.isActive(this)) policy.apply(this);
        }

        market.addTransientImmigrationModifier(this);
    }

    // PRIVATE METHODS

    private final void advanceMarket(int days) {
        updateHealthDelta();
        updateHappinessDelta();
        updateSocialCohesionDelta();
        updateClassConsciousnessDelta();

        setHealth(healthDelta.computeEffective(popHealth) * days);
        setHappiness(happinessDelta.computeEffective(popHappiness) * days);
        setSocialCohesion(socialCohesionDelta.computeEffective(popSocialCohesion) * days);
        setClassConsciousness(classConsciousnessDelta.computeEffective(popClassConsciousness) * days);

        applyHealthModifiers();
        applyHappinessModifiers();
        applySocialCohesionModifiers();
        applyClassConsciousnessModifiers();
    }

    private final void updateHealthDelta() {
        final EconomyEngine engine = EconomyEngine.instance();
        final CommodityCell foodCell = engine.getComCell(Commodities.FOOD, market.getId());
        final CommodityCell organicsCell = engine.getComCell(Commodities.ORGANICS, market.getId());
        final float fulfillmentRatio = market.hasCondition(Conditions.HABITABLE) ?
            foodCell.getStoredAvailabilityRatio() :
            Math.min(foodCell.getStoredAvailabilityRatio(), organicsCell.getStoredAvailabilityRatio());
        final float modifier = 
            fulfillmentRatio < 0.1 ? -0.2f :
            fulfillmentRatio < 0.4 ? -0.1f :
            fulfillmentRatio < 0.7 ? -0.05f : 0;

        healthDelta.modifyFlat("food_organic_deficit", modifier, "Food or organics deficit");

        healthDelta.modifyFlat("hazard", (1f - market.getHazardValue()) * 0.16f, "Hazard rating");

        healthDelta.modifyFlat("wage", (LaborConfig.LPV_month / RoSV - 1f) * 0.08f, "Wages");
    }

    private final void updateHappinessDelta() {
        happinessDelta.modifyFlat("health", (popHealth - BASELINE_VALUE) * 0.003f, "Health");

        happinessDelta.modifyFlat(
            "stability", (market.getStability().getModifiedValue() - 5f) * 0.025f, "Stability"
        );

        happinessDelta.modifyFlat("wage", (LaborConfig.RoSV - RoSV) * 0.04f, "Wages");

        happinessDelta.modifyFlat(
            "cohesion", (popSocialCohesion - BASELINE_VALUE) * 0.0008f, "Social Cohesion"
        );
    }

    private final void updateSocialCohesionDelta() {
        Map<String, Float> comp = market.getPopulation().getComp();
        float sum = 0f;
        for (float v : comp.values()) sum += v;

        float homogeneity = 0f;
        if (sum > 0f) {
            for (float v : comp.values()) {
                final float frac = v / sum;
                homogeneity += frac * frac;
            }
        }

        final float baseline = 0.6f;
        socialCohesionDelta.modifyFlat(
            "composition", (homogeneity - baseline) * 0.0002f, "Cultural Composition"
        );

        socialCohesionDelta.modifyFlat(
            "consciousness", popClassConsciousness * 0.0007f, "Class Consciousness"
        );

        socialCohesionDelta.modifyFlat(
            "random", (float) (Math.random() * 0.006 - 0.003), "Natural drift"
        );
    }

    private final void updateClassConsciousnessDelta() {
        classConsciousnessDelta.modifyFlat("base", -0.003f, "Base change");

        classConsciousnessDelta.modifyFlat("wage", 0.02f * (RoSV - RoSV_Equalibrium) / RoSV, "Wages");

        classConsciousnessDelta.modifyFlat("health", (BASELINE_VALUE - popHealth) * 0.0002f, "Health");

        classConsciousnessDelta.modifyFlat(
            "happiness", (BASELINE_VALUE - popHappiness) * 0.00016f, "Happiness"
        );
    }

    public final void modifyIncoming(MarketAPI market, PopulationComposition incoming) {
        final String desc = "Colony health";
        final int baseValue = (int) ((popHealth + 5f - BASELINE_VALUE) / 10f);

        incoming.getWeight().modifyFlat(healthID, baseValue * 2f, desc);
    }

    private final void applyHealthModifiers() {
        final String desc = "Colony health";

        final int baseValue = (int) ((popHealth + 5f - BASELINE_VALUE) / 10f);

        if (baseValue != 0) {
            market.getStats().getDynamic().getMod(Stats.GROUND_DEFENSES_MOD).modifyMult(
                healthID, 1f + baseValue * 0.05f, desc
            );
        } else {
            market.getStats().getDynamic().getMod(Stats.GROUND_DEFENSES_MOD).unmodifyMult(healthID);
        }
    }

    private final void applyHappinessModifiers() {
        final String desc = "Colony happiness";

        final int baseValue = popHappiness < 20f ? -1 : (popHappiness > 80f ? 1 : 0);

        if (baseValue != 0) {
            market.getStability().modifyFlat(happinessID, baseValue, desc);
            for (CommodityDomain dom : EconomyEngine.instance().getComDomains()) {
                dom.getCell(marketID).getProductionStat().modifyMult(happinessID, 1f + baseValue * 0.2f, desc);
            }
        } else {
            market.getStability().unmodifyFlat(happinessID);
            for (CommodityDomain dom : EconomyEngine.instance().getComDomains()) {
                dom.getCell(marketID).getProductionStat().unmodifyMult(happinessID);
            }
        }
    }

    private final void applySocialCohesionModifiers() {
        // TODO create apply social cohesion modifiers
        // final String desc = "Social cohesion";

        /*
        Crisis response speed
        Effect:
            high cohesion → shortages, raids, instability resolve faster
            low cohesion → problems linger

        Expedition / event resistance
        Effect:
            low cohesion → hostile events advance faster
            high cohesion → events stall or fail
        */
    }

    private final void applyClassConsciousnessModifiers() {
        
    }

    private static final float clamp(float value) {
        return Arithmetic.clamp(value, 0f, 100f);
    }

    @Override
    public String toString() {
        return " [" + marketID + "; name: " + market.getName() + "] ";
    }
}
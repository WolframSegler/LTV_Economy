package wfg.ltv_econ.economy;

import java.util.ArrayList;
import java.util.Map;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.combat.StatBonus;
import com.fs.starfarer.api.impl.campaign.ids.Commodities;
import com.fs.starfarer.api.impl.campaign.ids.Conditions;

import rolflectionlib.util.RolfLectionUtil;
import wfg.ltv_econ.configs.LaborConfigLoader.LaborConfig;
import wfg.ltv_econ.configs.PolicyConfigLoader.PolicyConfig;
import wfg.ltv_econ.configs.PolicyConfigLoader.PolicySpec;
import wfg.ltv_econ.economy.engine.EconomyEngine;
import wfg.ltv_econ.economy.policies.MarketPolicy;

public class PlayerMarketData {
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
    private float RoSV = LaborConfig.RoSV;
    private float popHealth = BASELINE_VALUE;
    private float popHappiness = BASELINE_VALUE;
    private float popSocialCohesion = BASELINE_VALUE;
    private float popClassConsciousness = 0f;

    private final ArrayList<MarketPolicy> policies = new ArrayList<>();

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
    public final ArrayList<MarketPolicy> getPolicies() { return new ArrayList<>(policies); }
    public final void addPolicy(MarketPolicy policy) { policies.add(policy); }
    public final void removePolicy(MarketPolicy policy) { policies.remove(policy); }
    public final MarketPolicy getPolicy(String policyID) {
        for (MarketPolicy policy : policies) if (policy.id.equals(policyID)) return policy;
        return null;
    }

    /**
     * Advances the statistics concerning the Market by one day.
     */
    public void advance(int days) {
        for (MarketPolicy policy : policies) {
            if (policy.isActive()) policy.preAdvance(this);
        }

        advanceMarket(days);

        for (MarketPolicy policy : policies) {
            if (policy.isActive())  policy.postAdvance(this);
        }

        for (MarketPolicy policy : policies) {
            policy.advanceTime(this, days);
        }
    }
    public final void advance() {
        advance(1);
    }

    /**
     * Assumes end-of-month
     */
    public final float getEffectiveProfitRatio() {
        final EconomyEngine engine = EconomyEngine.getInstance();
        final double net = engine.info.getNetIncome(market, true);
        if (net <= 0) return 0f;

        final double endCredits = engine.getCredits(marketID) + net;
        if (endCredits <= 0) return 0f;

        return (float) Math.min(playerProfitRatio, endCredits / net);
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
    }

    private final void updateHealthDelta() {
        final EconomyEngine engine = EconomyEngine.getInstance();
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

        healthDelta.modifyFlat("wage", (LaborConfig.LPV_month / RoSV - 1f) * 0.1f, "Wages");
    }

    private final void updateHappinessDelta() {
        happinessDelta.modifyFlat("health", (popHealth - BASELINE_VALUE) * 0.02f, "Health");

        happinessDelta.modifyFlat(
            "stability", (market.getStability().getModifiedValue() - 5f) * 0.03f, "Stability"
        );

        happinessDelta.modifyFlat("wage", (LaborConfig.RoSV - RoSV) * 0.05f, "Wages");

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
        classConsciousnessDelta.modifyFlat("base", -0.005f, "Base change");

        classConsciousnessDelta.modifyFlat("wage", 0.03f * (RoSV - 1f) / RoSV, "Wages");

        classConsciousnessDelta.modifyFlat("health", (BASELINE_VALUE - popHealth) * 0.0002f, "Health");

        classConsciousnessDelta.modifyFlat(
            "happiness", (BASELINE_VALUE - popHappiness) * 0.00016f, "Happiness"
        );
    }

    private static final float clamp(float value) {
        return clamp(value, 0f, 100f);
    }

    private static final float clamp(float value, float min, float max) {
        return value < min ? min : (value > max ? max : value);
    }

    @Override
    public String toString() {
        return "[" + marketID + "; name: " + market.getName() + "]";
    }
}
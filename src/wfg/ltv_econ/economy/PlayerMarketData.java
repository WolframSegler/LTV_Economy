package wfg.ltv_econ.economy;

import java.util.ArrayList;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.impl.campaign.ids.Commodities;
import com.fs.starfarer.api.impl.campaign.ids.Conditions;

import wfg.ltv_econ.configs.LaborConfigLoader.LaborConfig;
import wfg.ltv_econ.economy.policies.MarketPolicy;

public class PlayerMarketData {
    public final String marketID;
    public transient MarketAPI market;

    public static final float BASELINE_VALUE = 50f;
    private float RoSV = LaborConfig.RoSV;
    private float popHealth = BASELINE_VALUE;
    private float popHappiness = BASELINE_VALUE;
    private float popCulturalCohesion = BASELINE_VALUE;
    private float popClassConsciousness = 0f;

    private final ArrayList<MarketPolicy> policies = new ArrayList<>();

    public PlayerMarketData(String marketID) {
        this.marketID = marketID;
        readResolve();
    }

    public Object readResolve() {
        market = Global.getSector().getEconomy().getMarket(marketID);

        return this;
    }

    public float getRoSV() { return RoSV; }
    public float getHealth() { return popHealth; }
    public float getHappiness() { return popHappiness; }
    public float getCulturalCohesion() { return popCulturalCohesion; }
    public float getClassConsciousness() { return popClassConsciousness; }

    public void setRoSV(float rate) { RoSV = rate; }
    public void setHealth(float health) { popHealth = clamp(health); }
    public void setHappiness(float happiness) { popHappiness = clamp(happiness); }
    public void setCulturalCohesion(float cohesion) { popCulturalCohesion = clamp(cohesion); }
    public void setClassConsciousness(float consciousness) { popClassConsciousness = clamp(consciousness); }

    public void addPolicy(MarketPolicy policy) { policies.add(policy); }
    public void removePolicy(MarketPolicy policy) { policies.remove(policy); }

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
            policy.advanceTime(days);
        }
    }
    public void advance() {
        advance(1);
    }


    // PRIVATE METHODS
    private void advanceMarket(int days) {
        updateHealth(days);

        updateHappiness(days);

        updateCulturalCohesion(days);
        
        updateClassConsciousness(days);
    }

    private void updateHealth(int days) {
        float foodRatio = EconomyEngine.getMaxDeficit(market, Commodities.FOOD).two;
        if (!market.hasCondition(Conditions.HABITABLE)) {
            foodRatio = EconomyEngine.getMaxDeficit(market, Commodities.FOOD, Commodities.ORGANICS).two;
        }
        float modifier = 0f;
        if (foodRatio < 0.1f) modifier = -0.2f;
        else if (foodRatio < 0.4f) modifier = -0.1f;
        else if (foodRatio < 0.7f) modifier = -0.05f;

        modifier += (1f - market.getHazardValue()) * 0.16f;

        modifier += (LaborConfig.LPV_month / RoSV - 1f) * 0.1f;

        popHealth = clamp(popHealth + modifier*days);
    }

    private void updateHappiness(int days) {
        float modifier = (popHealth - BASELINE_VALUE) * 0.2f;

        modifier += (market.getStability().getModifiedValue() - 5f) * 0.03f;

        modifier += (LaborConfig.RoSV - RoSV) * 0.06f;

        modifier += (popCulturalCohesion - BASELINE_VALUE) * 0.0008f;

        popHappiness = clamp(popHappiness + modifier*days);
    }

    private void updateCulturalCohesion(int days) {
        float diversityScore = 0f;
        for (Float pct : market.getPopulation().getComp().values()) {
            diversityScore += pct * pct;
        }
        float modifier = (1f - diversityScore) * -0.05f;

        modifier += +popClassConsciousness * 0.0007f;

        modifier += (float) (Math.random() * 0.006 - 0.003);

        popCulturalCohesion = clamp(popCulturalCohesion + modifier*days);
    }

    private void updateClassConsciousness(int days) {
        float modifier = -0.005f;

        modifier += 0.05f * (RoSV - 1f) / RoSV;

        modifier += (BASELINE_VALUE - popHealth) * 0.0002f;

        modifier += (BASELINE_VALUE - popHappiness) * 0.00016f;

        popClassConsciousness = clamp(popClassConsciousness + modifier * days);
    }

    private static final float clamp(float value) {
        return clamp(value, 0f, 100f);
    }

    private static final float clamp(float value, float min, float max) {
        return value < min ? min : (value > max ? max : value);
    }
}
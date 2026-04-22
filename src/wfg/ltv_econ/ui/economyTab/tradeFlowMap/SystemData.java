package wfg.ltv_econ.ui.economyTab.tradeFlowMap;

import java.awt.Color;

import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;

import wfg.native_ui.util.ArrayMap;

public class SystemData {
    private final ArrayMap<Color, Double> colorWeights = new ArrayMap<>(2);
    private float totalColorWeight = 0f;

    public final ArrayMap<MarketAPI, Double> marketAmounts = new ArrayMap<>(2);
    public boolean isSource = false;
    public boolean isDest = false;
    public StarSystemAPI system = null;
    public float nodeSize = 0f;

    public final void addColorWeight(Color color, double weight) {
        final Double prev = colorWeights.get(color);
        if (prev == null) {
            colorWeights.put(color, weight);
        } else {
            colorWeights.put(color, prev + weight);
        }

        totalColorWeight += weight;
    }

    public final ArrayMap<Color, Double> getColorWeights() {
        return colorWeights;
    }

    public final double getWeightSum() {
        return totalColorWeight;
    }

    public final void clearColorWeights() {
        colorWeights.clear();
        totalColorWeight = 0f;
    }
}
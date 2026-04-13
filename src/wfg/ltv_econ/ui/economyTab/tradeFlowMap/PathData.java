package wfg.ltv_econ.ui.economyTab.tradeFlowMap;

import java.awt.Color;

import com.fs.starfarer.api.campaign.FactionSpecAPI;
import com.fs.starfarer.api.campaign.StarSystemAPI;

import wfg.native_ui.util.ArrayMap;

public class PathData {
    private final ArrayMap<Color, Double> colorWeights = new ArrayMap<>(2);
    private double totalColorWeight = 0f;

    public final ArrayMap<FactionSpecAPI, Double> factionAmounts = new ArrayMap<>(2);
    public StarSystemAPI source;
    public StarSystemAPI destination;
    public float pathWidth;
    public float nodeSize;
    public float travelDuration;
    public float pauseDuration;
    public float pulseOffset;

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
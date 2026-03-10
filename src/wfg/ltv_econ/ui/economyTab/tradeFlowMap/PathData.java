package wfg.ltv_econ.ui.economyTab.tradeFlowMap;

import java.awt.Color;

import com.fs.starfarer.api.campaign.FactionSpecAPI;
import com.fs.starfarer.api.campaign.StarSystemAPI;

import wfg.native_ui.util.ArrayMap;

public class PathData {
    private final ArrayMap<Color, Float> colorWeights = new ArrayMap<>();
    private float totalColorWeight = 0f;

    public final ArrayMap<FactionSpecAPI, Float> factionAmounts = new ArrayMap<>();
    public StarSystemAPI source;
    public StarSystemAPI destination;
    public float pathWidth;
    public float nodeSize;
    public float travelDuration;
    public float pauseDuration;
    public float pulseOffset;

    public final void addColorWeight(Color color, float weight) {
        final Float prev = colorWeights.get(color);
        if (prev == null) {
            colorWeights.put(color, weight);
        } else {
            colorWeights.put(color, prev + weight);
        }

        totalColorWeight += weight;
    }

    public final ArrayMap<Color, Float> getColorWeights() {
        return colorWeights;
    }

    public final float getWeightSum() {
        return totalColorWeight;
    }

    public final void clearColorWeights() {
        colorWeights.clear();
        totalColorWeight = 0f;
    }
}
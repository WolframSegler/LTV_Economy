package wfg.ltv_econ.ui.economyTab.tradeFlowMap;

import java.awt.Color;

import com.fs.starfarer.api.campaign.StarSystemAPI;

import wfg.native_ui.util.ArrayMap;

public class SystemData {
    private final ArrayMap<Color, Float> colorWeights = new ArrayMap<>(2);
    private float totalColorWeight = 0f;

    public boolean isSource = false;
    public boolean isDest = false;
    public StarSystemAPI system = null;
    public float nodeSize = 0f;

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
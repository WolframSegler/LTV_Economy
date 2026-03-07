package wfg.ltv_econ.ui.economyTab.tradeFlowMap;

import java.awt.Color;

import com.fs.starfarer.api.campaign.StarSystemAPI;

import wfg.ltv_econ.util.ArrayMap;

public class PathData {
    public final ArrayMap<Color, Float> colorWeights = new ArrayMap<>();
    public StarSystemAPI source;
    public StarSystemAPI destination;
    public float pathWidth;
    public float nodeSize;
    public float travelDuration;
    public float pauseDuration;
    public float pulseOffset;
}
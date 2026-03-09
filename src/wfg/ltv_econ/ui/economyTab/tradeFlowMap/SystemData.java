package wfg.ltv_econ.ui.economyTab.tradeFlowMap;

import java.awt.Color;

import com.fs.starfarer.api.campaign.StarSystemAPI;

import wfg.native_ui.util.ArrayMap;

public class SystemData {
    public final ArrayMap<Color, Float> colorWeights = new ArrayMap<>();
    public boolean isSource = false;
    public boolean isDest = false;
    public StarSystemAPI system = null;
    public float nodeSize = 0f;
}
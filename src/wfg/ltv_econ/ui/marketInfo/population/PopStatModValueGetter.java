package wfg.ltv_econ.ui.marketInfo.population;

import static wfg.native_ui.util.UIConstants.highlight;

import java.awt.Color;

import com.fs.starfarer.api.combat.MutableStat.StatMod;
import com.fs.starfarer.api.impl.campaign.ids.Strings;
import com.fs.starfarer.api.ui.TooltipMakerAPI.StatModValueGetter;

public class PopStatModValueGetter implements StatModValueGetter {
    public static final int GRID_W = 350;
    public static final int VALUE_W = 60;

    public String getFlatValue(StatMod mod) {
        return String.format("%.3f", mod.value);
    }
    public String getPercentValue(StatMod mod) {
        return mod.value + "%";
    }
    public String getMultValue(StatMod mod) {
        return Strings.X + String.format("%.1f", mod.value);
    }
    public Color getModColor(StatMod mod) {
        return highlight;
    }
}
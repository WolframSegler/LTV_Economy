package wfg_ltv_econ.util;

import com.fs.starfarer.api.ui.LabelAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;

public class UiUtils {
    public static final void resetFlowLeft(TooltipMakerAPI tooltip, float opad) {
		float prevHeight = tooltip.getHeightSoFar();
		LabelAPI alignReset = tooltip.addPara("", 0);
		alignReset.getPosition().inTL(opad / 2, prevHeight);
		tooltip.setHeightSoFar(prevHeight);
	}
}

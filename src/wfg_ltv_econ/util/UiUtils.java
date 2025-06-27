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

	public static final void positionCodexLabel(TooltipMakerAPI tooltip, int opad, int pad) {
		// LabelAPI F2Label = ((StandardTooltipV2Expandable) tooltip).expandLabel;
        LabelAPI F2Label = (LabelAPI)ReflectionUtils.get(tooltip, "expandLabel", LabelAPI.class, true);
        if (F2Label != null) {
            F2Label.getPosition().inBL(opad + pad, -pad*6);
        }
	}
}

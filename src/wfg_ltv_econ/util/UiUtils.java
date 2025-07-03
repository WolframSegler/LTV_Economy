package wfg_ltv_econ.util;

import org.lwjgl.util.vector.Vector2f;

import com.fs.starfarer.api.ui.LabelAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.codex2.CodexDialog;

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

	/**
     * This function assumes that the sprite is pointing right.
     * In other words, it's directed towards the positive x-axis in Hyperspace.
     */
    public static float rotateSprite(Vector2f origin, Vector2f target) {
        Vector2f delta = Vector2f.sub(target, origin, null);

        float angleDegrees = (float) Math.toDegrees(Math.atan2(delta.y, delta.x));

        return angleDegrees;
    }

	public static void openCodexPage(String codexID) {
        CodexDialog.show(codexID);
    }
}

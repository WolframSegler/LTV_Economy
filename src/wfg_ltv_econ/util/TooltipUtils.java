package wfg_ltv_econ.util;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.ui.CustomPanelAPI;
import com.fs.starfarer.api.ui.PositionAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;

public class TooltipUtils {

    public static void dynamicPos(TooltipMakerAPI tooltip, CustomPanelAPI anchor, int opad) {
        PositionAPI pos = tooltip.getPosition();

        final float tooltipWidth = pos.getWidth();
        final float tooltipHeight = pos.getHeight();

        final float screenW = Global.getSettings().getScreenWidth();
        final float screenH = Global.getSettings().getScreenHeight();

        pos.rightOfTop(anchor, opad);

        // If it overflows the screen to the right, move it to the left
        if (pos.getX() + tooltipWidth > screenW) {
            pos.leftOfTop(anchor, opad);
        }

        float y = pos.getY();
        float yOverflowTop = y + tooltipHeight - screenH;
        float yUnderflowBottom = y < 0 ? -y : 0;

        // If it overflows the top, clamp it to top
        if (yUnderflowBottom > 0) {
            pos.setYAlignOffset(-yOverflowTop - opad);
        }

        // If it overflows the bottom, clamp it to bottom
        if (yUnderflowBottom > 0) {
            pos.setYAlignOffset(yUnderflowBottom + opad);
        }
    }

    public static void mouseCornerPos(TooltipMakerAPI tooltip, int opad) {
        PositionAPI pos = tooltip.getPosition();

        final int mouseSize = 40;

        final float tooltipW = pos.getWidth();
        final float tooltipH = pos.getHeight();

        float mouseX = Global.getSettings().getMouseX();
        float mouseY = Global.getSettings().getMouseY();

        final float screenW = Global.getSettings().getScreenWidth();

        pos.inBL(0, 0);

        float tooltipX = pos.getX();
        float tooltipY = pos.getY();

        float offsetX = (mouseX - tooltipX) + (mouseSize / 2f);
        float offsetY = (mouseY - tooltipY) - (tooltipH + mouseSize);

        // If overflow to the right
        if (tooltipX + offsetX + tooltipW > screenW - opad) {
            offsetX += -(tooltipW + mouseSize - 8);
        }

        // If overflow to the bottom
        if (tooltipY + offsetY < opad) {
            offsetY += (tooltipH + mouseSize + 5);
        }

        pos.setXAlignOffset(offsetX);
        pos.setYAlignOffset(offsetY);
    }
}

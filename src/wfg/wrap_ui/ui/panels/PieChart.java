package wfg.wrap_ui.ui.panels;

import java.awt.Color;
import java.util.ArrayList;

import org.lwjgl.opengl.GL11;

import com.fs.starfarer.api.ui.CustomPanelAPI;
import com.fs.starfarer.api.ui.PositionAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.ui.UIPanelAPI;

import wfg.wrap_ui.ui.UIState.State;
import wfg.wrap_ui.ui.panels.CustomPanel.HasTooltip;
import wfg.wrap_ui.ui.plugins.PieChartPlugin;
import wfg.wrap_ui.util.RenderUtils;
import wfg.wrap_ui.util.WrapUiUtils;

public class PieChart extends CustomPanel<PieChartPlugin, PieChart, UIPanelAPI> implements
    HasTooltip
{

    /**
     * Does not require manual positioning or parent attachment for this instance.
     */
    public PendingTooltip<CustomPanelAPI> pendingTp = null;
    public float anglePerSegment = 3f;

    private static final int opad = 10;
    private final ArrayList<PieSlice> data;

    public PieChart(UIPanelAPI parent, int width, int height, ArrayList<PieSlice> data) {
        super(parent, width, height, new PieChartPlugin());

        this.data = data;

        initializePlugin(hasPlugin);
    }

    public void initializePlugin(boolean hasPlugin) {
        getPlugin().init(this);
        getPlugin().setTargetUIState(State.NONE);
    }

    public void createPanel() {}

    public void renderImpl(float alpha) {
        float startDeg = 0f;
        for (PieSlice slice : data) {
            final float sweepDeg = slice.fraction * 360f;
            final int segments = Math.max(1, (int) Math.ceil(sweepDeg / anglePerSegment));

            final PositionAPI pos = getPos();
            final float radiusX = pos.getWidth() / 2f;
            final float radiusY = pos.getHeight() / 2f;
            final float cx = pos.getX() +radiusX;
            final float cy = pos.getY() + radiusY;

            GL11.glDisable(GL11.GL_TEXTURE_2D);
            GL11.glEnable(GL11.GL_BLEND);
            GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
            RenderUtils.setGlColor(slice.color, alpha);
            GL11.glBegin(GL11.GL_TRIANGLE_FAN);
            GL11.glVertex2f(cx, cy);

            for (int i = 0; i <= segments; i++) {
                final float angle = (float) Math.toRadians(startDeg + i * sweepDeg / segments);
                GL11.glVertex2f(
                    cx + (float) Math.cos(angle) * radiusX,
                    cy + (float) Math.sin(angle) * radiusY
                );
            }
            GL11.glEnd();
            startDeg += sweepDeg;
        }
    }

    public boolean isTooltipEnabled() {
        return pendingTp != null;
    }

    public CustomPanelAPI getTpParent() {
        return pendingTp.parentSupplier.get();
    }

    public TooltipMakerAPI createAndAttachTp() {
        final TooltipMakerAPI tp = pendingTp.factory.get();

        pendingTp.parentSupplier.get().addComponent(tp);
        WrapUiUtils.mouseCornerPos(tp, opad);
        return tp;
    }

    public static class PieSlice {
        public final String uniqueID;
        public final Color color;
        public final float fraction;

        public PieSlice(String uniqueID, Color color, float fraction) {
            this.uniqueID = uniqueID;
            this.color = color;
            this.fraction = fraction;
        }
    }
}
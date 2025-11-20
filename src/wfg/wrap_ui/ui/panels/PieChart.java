package wfg.wrap_ui.ui.panels;

import java.awt.Color;
import java.util.ArrayList;

import org.lwjgl.opengl.GL11;

import com.fs.starfarer.api.ui.CustomPanelAPI;
import com.fs.starfarer.api.ui.PositionAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.ui.UIPanelAPI;
import com.fs.starfarer.api.util.Misc;

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

        final PositionAPI pos = getPos();
        final float radiusX = pos.getWidth() / 2f;
        final float radiusY = pos.getHeight() / 2f;
        final float cx = pos.getX() +radiusX;
        final float cy = pos.getY() + radiusY;

        GL11.glEnable(GL11.GL_BLEND);
        GL11.glEnable(GL11.GL_LINE_SMOOTH);
        GL11.glHint(GL11.GL_LINE_SMOOTH_HINT, GL11.GL_NICEST);
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);

        final int haloRings = 64;
        final int haloSegments = 30;
        final float haloMaxAlpha = 0.4f;
        final float minRadiusFactor = 0.9f;

        final int startRing = (int) Math.ceil(haloRings * minRadiusFactor);

        for (int r = haloRings; r > startRing; r--) {
            final float ringRadiusX = (radiusX*1.1f) * r / haloRings;
            final float ringRadiusY = (radiusY*1.1f) * r / haloRings;

            final float alphaFactor = haloMaxAlpha * (1f - (r / (float) haloRings) * (r / (float) haloRings));

            RenderUtils.setGlColor(Misc.getBasePlayerColor(), alphaFactor);
            GL11.glBegin(GL11.GL_TRIANGLE_FAN);
            GL11.glVertex2f(cx, cy);
            for (int i = 0; i <= haloSegments; i++) {
                float angle = (float) (2 * Math.PI * i / haloSegments);
                GL11.glVertex2f(
                    cx + (float) Math.cos(angle) * ringRadiusX,
                    cy + (float) Math.sin(angle) * ringRadiusY
                );
            }
            GL11.glEnd();
        }

        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);

        for (PieSlice slice : data) {
            final float sweepDeg = slice.fraction * 360f;
            final int segments = Math.max(1, (int) Math.ceil(sweepDeg / anglePerSegment));

            RenderUtils.setGlColor(slice.color, alpha*0.95f);
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

            GL11.glColor4f(0f, 0f, 0f, 1f);
            GL11.glLineWidth(1.5f);
            GL11.glBegin(GL11.GL_LINE_STRIP);

            for (int i = 0; i <= segments; i++) {
                float angle = (float)Math.toRadians(startDeg + i * sweepDeg / segments);
                GL11.glVertex2f(
                    cx + (float)Math.cos(angle) * radiusX,
                    cy + (float)Math.sin(angle) * radiusY
                );
            }
            GL11.glEnd();

            GL11.glColor4f(0f, 0f, 0f, 0.8f);
            GL11.glLineWidth(1f);
            GL11.glBegin(GL11.GL_LINES);
            float startRad = (float)Math.toRadians(startDeg);
            GL11.glVertex2f(cx, cy);
            GL11.glVertex2f(
                cx + (float)Math.cos(startRad) * radiusX,
                cy + (float)Math.sin(startRad) * radiusY
            );
            GL11.glEnd();

            startDeg += sweepDeg;
        }
    
        { // Radial Highlight
            final float centerHighlightAlpha = 0.2f;
            final float edgeAlpha = 0.0f;
            final int segments = 56;

            GL11.glBegin(GL11.GL_TRIANGLE_FAN);

            GL11.glColor4f(1f, 1f, 1f, centerHighlightAlpha);
            GL11.glVertex2f(cx, cy + radiusY * 0.1f);

            for (int i = 0; i <= segments; i++) {
                float rad = (float)(2 * Math.PI * (i / (float)segments));
                float x = cx + (float)Math.cos(rad) * radiusX;
                float y = cy + (float)Math.sin(rad) * radiusY;

                GL11.glColor4f(1f, 1f, 1f, edgeAlpha);
                GL11.glVertex2f(x, y);
            }

            GL11.glEnd();
        }

        { // Border Vignette
            final float edgeDarkAlpha = 0.08f;
            final int segments = 64;

            GL11.glBegin(GL11.GL_TRIANGLE_FAN);

            GL11.glColor4f(0f, 0f, 0f, 0f);
            GL11.glVertex2f(cx, cy);

            for (int i = 0; i <= segments; i++) {
                float rad = (float)(2 * Math.PI * (i / (float)segments));
                float x = cx + (float)Math.cos(rad) * radiusX;
                float y = cy + (float)Math.sin(rad) * radiusY;

                GL11.glColor4f(0f, 0f, 0f, edgeDarkAlpha);
                GL11.glVertex2f(x, y);
            }

            GL11.glEnd();
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

        pendingTp.parentSupplier.get().addUIElement(tp);
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
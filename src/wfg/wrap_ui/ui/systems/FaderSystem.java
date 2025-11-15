package wfg.wrap_ui.ui.systems;

import com.fs.starfarer.api.ui.PositionAPI;
import com.fs.starfarer.api.util.FaderUtil;
import com.fs.starfarer.api.util.FaderUtil.State;

import wfg.wrap_ui.ui.panels.CustomPanel;
import wfg.wrap_ui.ui.panels.CustomPanel.HasFader;
import wfg.wrap_ui.ui.plugins.CustomPanelPlugin;
import wfg.wrap_ui.ui.plugins.CustomPanelPlugin.InputSnapshot;
import wfg.wrap_ui.util.RenderUtils;

public final class FaderSystem<
    PluginType extends CustomPanelPlugin<PanelType, PluginType>,
    PanelType extends CustomPanel<PluginType, PanelType, ?> & HasFader
> extends BaseSystem<PluginType, PanelType> {

    public static enum Glow {
        NONE,
        ADDITIVE,
        OVERLAY,
        UNDERLAY
    }

    public FaderSystem(PluginType plugin) {
        super(plugin);
    }

    @Override
    public final void advance(float amount, InputSnapshot input) {
        if (getPanel().getGlowType() == Glow.NONE || !getPanel().isFaderOwner()) {
            return;
        }

        State target = input.hoveredLastFrame ? State.IN : State.OUT;

        if (!getPlugin().isValidUIContext()) {
            target = State.OUT;
        }
        if (getPanel().isPersistentGlow()) {
            target = State.IN;
        }

        getPanel().getFader().setState(target);
        getPanel().getFader().advance(amount);
    }

    @Override
    public final void renderBelow(float alphaMult, InputSnapshot input) {
        if (getPanel().getGlowType() != Glow.UNDERLAY || getPanel().getFader().getBrightness() <= 0) return;

        drawGlowLayer(alphaMult, input);
    }

    @Override
    public final void render(float alphaMult, InputSnapshot input) {
        final FaderUtil fader = getPanel().getFader();
        if (fader.getBrightness() <= 0) return;

        if (getPanel().getGlowType() == Glow.OVERLAY) {
            drawGlowLayer(alphaMult, input);
        }

        if (getPanel().getGlowType() == Glow.ADDITIVE) {
            final float glowAmount = getPanel().getAdditiveBrightness() * fader.getBrightness() * alphaMult;
            getPanel().getSprite().ifPresentOrElse(
                sprite -> RenderUtils.drawAdditiveGlow(
                    sprite,
                    getPanel().getPos().getX(),
                    getPanel().getPos().getY(),
                    getPanel().getGlowColor(),
                    glowAmount
                ),
                () -> drawGlowLayer(alphaMult, input)
            );
        }
    }

    private final void drawGlowLayer(float alphaMult, InputSnapshot input) {
        final PanelType panel = getPanel();
        final float glowAmount = panel.getOverlayBrightness() * getPanel().getFader().getBrightness() * alphaMult;
        final float[] verts = panel.getFaderMaskVertices();

        if (verts != null) {
            for (int i = 0; i < verts.length; i += 2) {
                verts[i] = verts[i] + getPlugin().offsetX;
                verts[i + 1] = verts[i + 1] + getPlugin().offsetY;
            }
        }   

        if (verts != null) {
            RenderUtils.drawPolygon(verts, panel.getGlowColor(), glowAmount);
        } else {
            final PositionAPI pos = panel.getPos();
            RenderUtils.drawQuad(
                pos.getX() + getPlugin().offsetX,
                pos.getY() + getPlugin().offsetY,
                pos.getWidth() + getPlugin().offsetW,
                pos.getHeight() + getPlugin().offsetH,
                panel.getGlowColor(), glowAmount, getPanel().getGlowType() == Glow.ADDITIVE
            );
        }

        if (!input.hasLMBClickedBefore) return;
        
        if (verts != null) {
            RenderUtils.drawPolygon(verts, panel.getGlowColor(), glowAmount / 2f);
        } else {
            final PositionAPI pos = panel.getPos();
            RenderUtils.drawQuad(
                pos.getX() + getPlugin().offsetX,
                pos.getY() + getPlugin().offsetY,
                pos.getWidth() + getPlugin().offsetW,
                pos.getHeight() + getPlugin().offsetH,
                panel.getGlowColor(),
                glowAmount / 2f, getPanel().getGlowType() == Glow.ADDITIVE
            );
        }
    }
}
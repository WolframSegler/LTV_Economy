package wfg_ltv_econ.ui.panels.components;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.ui.PositionAPI;
import com.fs.starfarer.api.util.FaderUtil;
import com.fs.starfarer.api.util.FaderUtil.State;

import wfg_ltv_econ.ui.panels.LtvCustomPanel;
import wfg_ltv_econ.ui.panels.LtvCustomPanel.HasFader;
import wfg_ltv_econ.ui.plugins.LtvCustomPanelPlugin;
import wfg_ltv_econ.ui.plugins.LtvCustomPanelPlugin.InputSnapshot;
import wfg_ltv_econ.util.RenderUtils;

public final class FaderComponent<
    PluginType extends LtvCustomPanelPlugin<PanelType, PluginType>,
    PanelType extends LtvCustomPanel<PluginType, PanelType> & HasFader
> extends BaseComponent<PluginType, PanelType> {

    public static enum Glow {
        NONE,
        ADDITIVE,
        OVERLAY,
        UNDERLAY
    }

    public FaderComponent(PluginType plugin) {
        super(plugin);
    }

    private final float additiveBrightness = 0.6f;

    @Override
    public void advance(float amount, InputSnapshot input) {
        if (getPanel().getGlowType() == Glow.NONE) return;

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

    private void drawGlowLayer(float alphaMult, InputSnapshot input) {
        final PanelType panel = getPanel();
        final PositionAPI pos = panel.getPos();

        final float glowAmount = panel.getOverlayBrightness() * getPanel().getFader().getBrightness() * alphaMult;

        RenderUtils.drawGlowOverlay(
            pos.getX(), pos.getY(), pos.getWidth(), pos.getHeight(),
            panel.getGlowColor(), glowAmount
        );

        if (input.hasClickedBefore) {
            RenderUtils.drawGlowOverlay(
                pos.getX(), pos.getY(), pos.getWidth(), pos.getHeight(),
                panel.getGlowColor(), glowAmount / 2
            );
        }
    }

    @Override
    public void renderBelow(float alphaMult, InputSnapshot input) {
        if (getPanel().getGlowType() != Glow.UNDERLAY || getPanel().getFader().getBrightness() <= 0) return;

        drawGlowLayer(alphaMult, input);
    }

    @Override
    public void render(float alphaMult, InputSnapshot input) {
        final FaderUtil fader = getPanel().getFader();

        if (getPanel().getGlowType() == Glow.OVERLAY && fader.getBrightness() > 0) {
            drawGlowLayer(alphaMult, input);
        }

        if (getPanel().getGlowType() == Glow.ADDITIVE && fader.getBrightness() > 0) {
            float glowAmount = additiveBrightness * fader.getBrightness() * alphaMult;

            if (getPanel().getSpriteID() != null) {
                SpriteAPI sprite = Global.getSettings().getSprite(getPanel().getSpriteID());
                
                RenderUtils.drawAdditiveGlow(
                    sprite,
                    getPanel().getPos().getX(),
                    getPanel().getPos().getY(), 
                    getPanel().getFaction().getBaseUIColor(),
                    glowAmount
                );
            }
        }
    }
}
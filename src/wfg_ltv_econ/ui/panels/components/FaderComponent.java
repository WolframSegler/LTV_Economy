package wfg_ltv_econ.ui.panels.components;

import com.fs.starfarer.api.ui.PositionAPI;
import com.fs.starfarer.api.util.FaderUtil.State;

import wfg_ltv_econ.ui.panels.LtvCustomPanel;
import wfg_ltv_econ.ui.panels.LtvCustomPanel.HasFader;
import wfg_ltv_econ.ui.plugins.LtvCustomPanelPlugin;
import wfg_ltv_econ.ui.plugins.LtvCustomPanelPlugin.InputSnapshot;
import wfg_ltv_econ.util.RenderUtils;

public final class FaderComponent<
    PluginType extends LtvCustomPanelPlugin<PanelType, ?>,
    PanelType extends LtvCustomPanel<PluginType, ?> & HasFader
> extends BaseComponent<PluginType, PanelType> {

    public static enum Glow {
        NONE,
        ADDITIVE,
        OVERLAY
    }

    public FaderComponent(PluginType plugin) {
        super(plugin);
    }

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

    @Override
    public void renderBelow(float alphaMult, InputSnapshot input) {
        if (getPanel().getGlowType() != Glow.OVERLAY || getPanel().getFader().getBrightness() <= 0) return;

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
}
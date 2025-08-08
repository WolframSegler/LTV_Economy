package wfg_ltv_econ.ui.plugins;

import java.util.ArrayList;
import java.util.List;

import com.fs.starfarer.api.campaign.CustomUIPanelPlugin;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.ui.PositionAPI;

import wfg_ltv_econ.ui.LtvUIState;
import wfg_ltv_econ.ui.LtvUIState.UIState;
import wfg_ltv_econ.ui.panels.LtvCustomPanel;
import wfg_ltv_econ.ui.panels.LtvCustomPanel.HasAudioFeedback;
import wfg_ltv_econ.ui.panels.LtvCustomPanel.HasBackground;
import wfg_ltv_econ.ui.panels.LtvCustomPanel.HasFader;
import wfg_ltv_econ.ui.panels.LtvCustomPanel.HasOutline;
import wfg_ltv_econ.ui.panels.LtvCustomPanel.HasTooltip;
import wfg_ltv_econ.ui.panels.components.AudioFeedbackComponent;
import wfg_ltv_econ.ui.panels.components.BackgroundComponent;
import wfg_ltv_econ.ui.panels.components.BaseComponent;
import wfg_ltv_econ.ui.panels.components.FaderComponent;
import wfg_ltv_econ.ui.panels.components.OutlineComponent;
import wfg_ltv_econ.ui.panels.components.TooltipComponent;

/**
 * The plugin serves as the central coordinator for its associated {@link LtvCustomPanel} and components.
 *
 * <p><strong>Design principles:</strong></p>
 * <ul>
 *   <li>The plugin manages and owns <em>plugin-specific</em> state — flags, toggles, configuration values —
 *       that control component behavior but are not part of the panel's intrinsic UI data.</li>
 *   <li>All <em>panel-specific</em> state (such as background color, dimensions, position) is stored exclusively
 *       in the panel itself. The plugin may query this data via {@link #getPanel()}.</li>
 *   <li>Components never store their own global state; they query the plugin, which may in turn query the panel.
 *       This keeps components stateless or minimally stateful, focused only on behavior.</li>
 *   <li>Generic recursive typing ensures that each plugin and panel pair is tightly coupled in type,
 *       allowing components to interact with the exact plugin type without unsafe casting.</li>
 * </ul>
 *
 * <p>Example data flow:</p>
 * <pre>
 * // Plugin accessing panel data:
 * float width = m_panel.getPos().getWidth();
 *
 * // Component accessing panel data via plugin:
 * var panel = getPlugin().getPanel();
 * Color bg = panel.getBgColor();
 * </pre>
 */
public abstract class LtvCustomPanelPlugin<
    PanelType extends LtvCustomPanel<PluginType, ?>,
    PluginType extends LtvCustomPanelPlugin<PanelType, ?>
> implements CustomUIPanelPlugin {

    public static class InputSnapshot {
        public boolean LMBDownLastFrame = false;
        public boolean LMBUpLastFrame = false;
        public boolean hoveredLastFrame = false;
        public boolean playedUIHoverSound = false;
        public boolean hasClickedBefore = false;

        public void resetFrameFlags() {
            LMBDownLastFrame = false;
            LMBUpLastFrame = false;
        }
    }

    protected PanelType m_panel;

    public PanelType getPanel() {
        return m_panel;
    }

    private final List<BaseComponent<PluginType, PanelType>> components = new ArrayList<>();
    private final InputSnapshot inputSnapshot = new InputSnapshot();
    
    protected UIState targetUIState = UIState.NONE;
    protected boolean ignoreUIState = false;

    public int offsetX = 0;
    public int offsetY = 0;
    public int offsetW = 0;
    public int offsetH = 0;

    @SuppressWarnings({"rawtypes", "unchecked"})
    public void init(PanelType panel) {
        m_panel = panel;

        if (getPanel() instanceof HasTooltip provider) {
            addComponent(new TooltipComponent((PluginType)this, provider));
        }

        if (getPanel() instanceof HasBackground) {
            addComponent(new BackgroundComponent((PluginType)this));
        }

        if (getPanel() instanceof HasAudioFeedback) {
            addComponent(new AudioFeedbackComponent((PluginType)this));
        }

        if (getPanel() instanceof HasOutline) {
            addComponent(new OutlineComponent((PluginType)this));
        }

        if (getPanel() instanceof HasFader) {
            addComponent(new FaderComponent((PluginType)this));
        }
    }

    public void addComponent(BaseComponent<PluginType, PanelType> comp) {
        components.add(comp);
    }

    public void removeComponent(BaseComponent<PluginType, PanelType> comp) {
        components.remove(comp);
        comp.onRemove(inputSnapshot);
    }

    public void setTargetUIState(UIState a) {
        targetUIState = a;
    }

    public void setIgnoreUIState(boolean a) {
        ignoreUIState = a;
    }

    public boolean isValidUIContext() {
        return LtvUIState.is(targetUIState) || ignoreUIState; 
    }

    public void positionChanged(PositionAPI position) {

    }

    /**
     * Effects the background and outline position
     */
    public void setOffsets(int x, int y, int width, int height) {
        offsetX = x;
        offsetY = y;
        offsetW = width;
        offsetH = height;
    }

    public void renderBelow(float alphaMult) {
        for (BaseComponent<PluginType, PanelType> comp : components) {
            comp.renderBelow(alphaMult, inputSnapshot);
        }
    }

    public void render(float alphaMult) {
        for (BaseComponent<PluginType, PanelType> comp : components) {
            comp.render(alphaMult, inputSnapshot);
        }
    }

    public void advance(float amount) {
        for (BaseComponent<PluginType, PanelType> comp : components) {
            comp.advance(amount, inputSnapshot);
        }
    }

    public void processInput(List<InputEventAPI> events) {
        inputSnapshot.resetFrameFlags();

        for (InputEventAPI event : events) {

            if (event.isMouseMoveEvent()) {
                final float mouseX = event.getX();
                final float mouseY = event.getY();

                final PositionAPI pos = m_panel.getPos();
                final float x = pos.getX();
                final float y = pos.getY();
                final float w = pos.getWidth();
                final float h = pos.getHeight();

                // Check for mouse over panel
                inputSnapshot.hoveredLastFrame = mouseX >= x && mouseX <= x + w && mouseY >= y && mouseY <= y + h;
            }

            if (!inputSnapshot.hoveredLastFrame) {
                inputSnapshot.playedUIHoverSound = false;
                inputSnapshot.hasClickedBefore = false;
            }

            if (inputSnapshot.hoveredLastFrame && inputSnapshot.hasClickedBefore && event.isLMBUpEvent()) {
                inputSnapshot.LMBUpLastFrame = true;
            }

            if (inputSnapshot.hoveredLastFrame && event.isLMBDownEvent()) {
                inputSnapshot.LMBDownLastFrame = true;
                inputSnapshot.hasClickedBefore = true;
            }
        }
    }

    public void buttonPressed(Object buttonId) {

    }
}

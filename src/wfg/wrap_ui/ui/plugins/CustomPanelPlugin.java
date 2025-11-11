package wfg.wrap_ui.ui.plugins;

import java.util.ArrayList;
import java.util.List;

import org.lwjgl.input.Mouse;

import com.fs.starfarer.api.campaign.CustomUIPanelPlugin;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.ui.PositionAPI;
import com.fs.starfarer.api.ui.UIPanelAPI;

import wfg.wrap_ui.ui.UIState;
import wfg.wrap_ui.ui.UIState.State;
import wfg.wrap_ui.ui.panels.CustomPanel;
import wfg.wrap_ui.ui.panels.CustomPanel.AcceptsActionListener;
import wfg.wrap_ui.ui.panels.CustomPanel.HasAudioFeedback;
import wfg.wrap_ui.ui.panels.CustomPanel.HasBackground;
import wfg.wrap_ui.ui.panels.CustomPanel.HasFader;
import wfg.wrap_ui.ui.panels.CustomPanel.HasOutline;
import wfg.wrap_ui.ui.panels.CustomPanel.HasTooltip;
import wfg.wrap_ui.ui.systems.ActionListenerSystem;
import wfg.wrap_ui.ui.systems.AudioFeedbackSystem;
import wfg.wrap_ui.ui.systems.BackgroundSystem;
import wfg.wrap_ui.ui.systems.BaseSystem;
import wfg.wrap_ui.ui.systems.FaderSystem;
import wfg.wrap_ui.ui.systems.OutlineSystem;
import wfg.wrap_ui.ui.systems.TooltipSystem;

/**
 * The plugin serves as the central coordinator for its associated {@link CustomPanel} and components.
 *
 * <p><strong>Design principles:</strong></p>
 * <ul>
 *   <li>The plugin manages and owns <em>plugin-specific</em> state — flags, toggles, configuration values —
 *       that control system behavior but are not part of the panel's intrinsic UI data.</li>
 *   <li>All <em>panel-specific</em> state (such as background color, dimensions, position) is stored exclusively
 *       in the panel itself. The plugin may query this data via {@link #getPanel()}.</li>
 *   <li>Systems never store their own global state; they query the plugin, which may in turn query the panel.
 *       This keeps systems stateless or minimally stateful, focused only on behavior.</li>
 *   <li>Recursive generics bind a plugin to a compatible panel type while permitting that plugin to be reused 
 *       across panel subclasses, preserving compile-time type safety without casts.</li>
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
public abstract class CustomPanelPlugin<
    PanelType extends CustomPanel<? extends CustomPanelPlugin<?, ? extends PluginType>, PanelType, ? extends UIPanelAPI>,
    PluginType extends CustomPanelPlugin<? extends CustomPanel<?, ?, ? extends UIPanelAPI>, ? extends CustomPanelPlugin<?, ? extends PluginType>>
> implements CustomUIPanelPlugin {

    public static class InputSnapshot {
        public boolean LMBDownLastFrame = false;
        public boolean LMBUpLastFrame = false;
        public boolean hasLMBClickedBefore = false;

        public boolean RMBDownLastFrame = false;
        public boolean RMBUpLastFrame = false;
        public boolean hasRMBClickedBefore = false;

        public boolean hoveredLastFrame = false;
        public boolean hoverStarted = false;
        public boolean hoverEnded = false;
        public boolean isActive = false;

        public InputEventAPI mouseEvent = null;

        public void resetFrameFlags() {
            LMBDownLastFrame = false;
            LMBUpLastFrame = false;

            RMBDownLastFrame = false;
            RMBUpLastFrame = false;
        }
    }

    protected PanelType m_panel;

    public PanelType getPanel() {
        return m_panel;
    }

    public final List<BaseSystem<?, PanelType>> components = new ArrayList<>();
    protected final InputSnapshot inputSnapshot = new InputSnapshot();
    
    protected State targetUIState = State.NONE;
    protected boolean ignoreUIState = false;

    public int offsetX = 0;
    public int offsetY = 0;
    public int offsetW = 0;
    public int offsetH = 0;

    @SuppressWarnings({"rawtypes", "unchecked"})
    public void init(PanelType panel) {
        m_panel = panel;

        if (panel instanceof HasTooltip provider) {
            addComponent(new TooltipSystem(this, provider));
        }

        if (panel instanceof HasBackground) {
            addComponent(new BackgroundSystem(this));
        }

        if (panel instanceof HasAudioFeedback) {
            addComponent(new AudioFeedbackSystem(this));
        }

        if (panel instanceof HasOutline) {
            addComponent(new OutlineSystem(this));
        }

        if (panel instanceof HasFader) {
            addComponent(new FaderSystem(this));
        }

        if (panel instanceof AcceptsActionListener) {
            addComponent(new ActionListenerSystem(this));
        }
    }

    protected final <C extends BaseSystem<?, PanelType>> void addComponent(C comp) {
        components.add(comp);
    }

    public void removeComponent(BaseSystem<?, PanelType> comp) {
        components.remove(comp);
        comp.onRemove(inputSnapshot);
    }

    public void setTargetUIState(State a) {
        targetUIState = a;
    }

    public void setIgnoreUIState(boolean a) {
        ignoreUIState = a;
    }

    public boolean isValidUIContext() {
        return UIState.is(targetUIState) || ignoreUIState; 
    }

    /**
     * Effects the background, the fader and the outline position
     */
    public void setOffsets(int x, int y, int width, int height) {
        offsetX = x;
        offsetY = y;
        offsetW = width;
        offsetH = height;
    }

    public void renderBelow(float alphaMult) {
        for (BaseSystem<?, PanelType> comp : components) {
            comp.renderBelow(alphaMult, inputSnapshot);
        }
    }

    public void render(float alphaMult) {
        for (BaseSystem<?, PanelType> comp : components) {
            comp.render(alphaMult, inputSnapshot);
        }
    }

    public void advance(float amount) {
        for (BaseSystem<?, PanelType> comp : components) {
            comp.advance(amount, inputSnapshot);
        }
    }

    public void processInput(List<InputEventAPI> events) {
        inputSnapshot.resetFrameFlags();

        // General events used by most components
        for (InputEventAPI event : events) {
            
            if (event.isMouseMoveEvent()) {
                inputSnapshot.mouseEvent = event;

                final float mouseX = event.getX();
                final float mouseY = event.getY();

                final PositionAPI pos = m_panel.getPos();
                final float x = pos.getX();
                final float y = pos.getY();
                final float w = pos.getWidth();
                final float h = pos.getHeight();

                // Check for mouse over panel
                final boolean hoveredBefore = inputSnapshot.hoveredLastFrame;
                inputSnapshot.hoveredLastFrame = mouseX >= x && mouseX <= x + w && mouseY >= y && mouseY <= y + h;

                inputSnapshot.hoverStarted = inputSnapshot.hoveredLastFrame && !hoveredBefore;
                inputSnapshot.hoverEnded   = !inputSnapshot.hoveredLastFrame && hoveredBefore;
            }

            if (event.isLMBDownEvent() && inputSnapshot.hoveredLastFrame) {
                inputSnapshot.LMBDownLastFrame = true;
                inputSnapshot.hasLMBClickedBefore = true;
                inputSnapshot.isActive = true;
            }

            if (event.isLMBUpEvent() || !Mouse.isButtonDown(0)) {
                if (inputSnapshot.hasLMBClickedBefore) inputSnapshot.LMBUpLastFrame = true;
                inputSnapshot.isActive = false;
                inputSnapshot.hasLMBClickedBefore = false;
            }

            if (event.isRMBDownEvent() && inputSnapshot.hoveredLastFrame) {
                inputSnapshot.RMBDownLastFrame = true;
                inputSnapshot.hasRMBClickedBefore = true;
            }

            if (event.isRMBUpEvent() || !Mouse.isButtonDown(1)) {
                if (inputSnapshot.hasRMBClickedBefore) inputSnapshot.RMBUpLastFrame = true;
                inputSnapshot.hasRMBClickedBefore = false;
            }
        }

        // Component specific
        for (BaseSystem<?, PanelType> comp : components) {
            comp.processInput(events, inputSnapshot);
        }
    }

    public void buttonPressed(Object buttonId) {}

    public void positionChanged(PositionAPI position) {}
}

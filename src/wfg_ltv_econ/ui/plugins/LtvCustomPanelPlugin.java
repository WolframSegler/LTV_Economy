package wfg_ltv_econ.ui.plugins;

import java.util.ArrayList;
import java.util.List;

import com.fs.starfarer.api.campaign.CustomUIPanelPlugin;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.ui.PositionAPI;
import com.fs.starfarer.api.ui.UIPanelAPI;

import wfg_ltv_econ.ui.LtvUIState;
import wfg_ltv_econ.ui.LtvUIState.UIState;
import wfg_ltv_econ.ui.components.ActionListenerComponent;
import wfg_ltv_econ.ui.components.AudioFeedbackComponent;
import wfg_ltv_econ.ui.components.BackgroundComponent;
import wfg_ltv_econ.ui.components.BaseComponent;
import wfg_ltv_econ.ui.components.FaderComponent;
import wfg_ltv_econ.ui.components.OutlineComponent;
import wfg_ltv_econ.ui.components.TooltipComponent;
import wfg_ltv_econ.ui.panels.LtvCustomPanel;
import wfg_ltv_econ.ui.panels.LtvCustomPanel.AcceptsActionListener;
import wfg_ltv_econ.ui.panels.LtvCustomPanel.HasAudioFeedback;
import wfg_ltv_econ.ui.panels.LtvCustomPanel.HasBackground;
import wfg_ltv_econ.ui.panels.LtvCustomPanel.HasFader;
import wfg_ltv_econ.ui.panels.LtvCustomPanel.HasOutline;
import wfg_ltv_econ.ui.panels.LtvCustomPanel.HasTooltip;

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
public abstract class LtvCustomPanelPlugin<
    PanelType extends LtvCustomPanel<? extends LtvCustomPanelPlugin<?, ? extends PluginType>, PanelType, ? extends UIPanelAPI>,
    PluginType extends LtvCustomPanelPlugin<? extends LtvCustomPanel<?, ?, ? extends UIPanelAPI>, ? extends LtvCustomPanelPlugin<?, ? extends PluginType>>
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

    public final List<BaseComponent<?, PanelType>> components = new ArrayList<>();
    protected final InputSnapshot inputSnapshot = new InputSnapshot();
    
    protected UIState targetUIState = UIState.NONE;
    protected boolean ignoreUIState = false;

    public int offsetX = 0;
    public int offsetY = 0;
    public int offsetW = 0;
    public int offsetH = 0;

    @SuppressWarnings({"rawtypes", "unchecked"})
    public void init(PanelType panel) {
        m_panel = panel;

        if (panel instanceof HasTooltip provider) {
            addComponent(new TooltipComponent(this, provider));
        }

        if (panel instanceof HasBackground) {
            addComponent(new BackgroundComponent(this));
        }

        if (panel instanceof HasAudioFeedback) {
            addComponent(new AudioFeedbackComponent(this));
        }

        if (panel instanceof HasOutline) {
            addComponent(new OutlineComponent(this));
        }

        if (panel instanceof HasFader) {
            addComponent(new FaderComponent(this));
        }

        if (panel instanceof AcceptsActionListener) {
            addComponent(new ActionListenerComponent(this));
        }
    }

    protected final <C extends BaseComponent<?, PanelType>> void addComponent(C comp) {
        components.add(comp);
    }

    public void removeComponent(BaseComponent<?, PanelType> comp) {
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
        for (BaseComponent<?, PanelType> comp : components) {
            comp.renderBelow(alphaMult, inputSnapshot);
        }
    }

    public void render(float alphaMult) {
        for (BaseComponent<?, PanelType> comp : components) {
            comp.render(alphaMult, inputSnapshot);
        }
    }

    public void advance(float amount) {
        for (BaseComponent<?, PanelType> comp : components) {
            comp.advance(amount, inputSnapshot);
        }
    }

    public void processInput(List<InputEventAPI> events) {
        inputSnapshot.resetFrameFlags();

        // General events used by most components
        for (InputEventAPI event : events) {

            if (event.isMouseEvent()) {

                if (event.isMouseMoveEvent()) {
                    final float mouseX = event.getX();
                    final float mouseY = event.getY();
    
                    final PositionAPI pos = m_panel.getPos();
                    final float x = pos.getX();
                    final float y = pos.getY();
                    final float w = pos.getWidth();
                    final float h = pos.getHeight();
    
                    // Check for mouse over panel
                    boolean hoveredBefore = inputSnapshot.hoveredLastFrame;
                    inputSnapshot.hoveredLastFrame = mouseX >= x && mouseX <= x + w && mouseY >= y && mouseY <= y + h;
    
                    inputSnapshot.hoverStarted = inputSnapshot.hoveredLastFrame && !hoveredBefore;
                    inputSnapshot.hoverEnded   = !inputSnapshot.hoveredLastFrame && hoveredBefore;
                }

                if (!inputSnapshot.hoveredLastFrame) {
                    inputSnapshot.hasLMBClickedBefore = false;
                    inputSnapshot.hasRMBClickedBefore = false;
                }

                if (inputSnapshot.hoveredLastFrame) {
                    if (inputSnapshot.hasLMBClickedBefore && event.isLMBUpEvent()) {
                        inputSnapshot.LMBUpLastFrame = true;
                    }

                    if (inputSnapshot.hasRMBClickedBefore && event.isRMBUpEvent()) {
                        inputSnapshot.RMBUpLastFrame = true;
                    }

                    if (event.isLMBDownEvent()) {
                        inputSnapshot.LMBDownLastFrame = true;
                        inputSnapshot.hasLMBClickedBefore = true;
                    }

                    if (event.isRMBDownEvent()) {
                        inputSnapshot.RMBDownLastFrame = true;
                        inputSnapshot.hasRMBClickedBefore = true;
                    }
                }
            }
        }

        // Component specific
        for (BaseComponent<?, PanelType> comp : components) {
            comp.processInput(events, inputSnapshot);
        }
    }

    public void buttonPressed(Object buttonId) {}

    public void positionChanged(PositionAPI position) {}
}

package wfg.wrap_ui.ui.panels;

import com.fs.starfarer.api.ui.ButtonAPI;
import com.fs.starfarer.api.ui.CustomPanelAPI;
import com.fs.starfarer.api.ui.LabelAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.ui.UIPanelAPI;

import wfg.wrap_ui.ui.panels.CustomPanel.HasAudioFeedback;
import wfg.wrap_ui.ui.panels.CustomPanel.HasTooltip;
import wfg.wrap_ui.ui.plugins.BasePanelPlugin;

/**
 * A base implementation of a text-based UI panel with tooltip support, 
 * intended to be subclassed anonymously for ad-hoc UI creation.
 * It acts as a configurable template, with public fields provided to expose 
 * key UI state to external consumers. This allows external code to inspect 
 * or interact with panel elements created by the anonymous subclass.
 * <p>
 * This class provides common state and lifecycle hooks for panels that 
 * render text and optionally have tooltips or codex entries. 
 * Anonymous subclasses are expected to override methods such as 
 * {@code createPanel()} and {@code createAndAttachTooltip()} 
 * to define their own UI behavior.
 * <p>
 * Typical usage:
 * <pre>
 * TextPanel panel = new TextPanel(root, parent, market, 300, 50, plugin) {
 * </pre>
 * <pre>
 *      @Override 
 *      public void createPanel() {
 *          // Custom panel creation logic here
 *      }
 * </pre>
 * <pre>
 * 
 *      @Override
 *      public CustomPanelAPI getTpParent() {
 *          return null; // return tooltip parent
 *      }
 * </pre>
 * <pre>
 *      @Override 
 *      public TooltipMakerAPI createAndAttachTp() {
 *          // Custom tooltip logic here
 *      }
 * };
 * </pre>
 *
 * <h4>Public API Fields</h4>
 * <ul>
 *   <li>{@code m_checkbox} – Reference to the checkbox UI element, if created.</li>
 *   <li>{@code textX1}, {@code textX2}, {@code textY1}, {@code textY2} – Positional coordinates.</li>
 *   <li>{@code textW1}, {@code textW2}, {@code textH1}, {@code textH2} – Dimension values.</li>
 * </ul>
 * These values are intentionally exposed as part of the public API to enable 
 * direct querying and manipulation of panel state from outside the class.
 */
public class TextPanel extends CustomPanel<BasePanelPlugin<TextPanel>, TextPanel, CustomPanelAPI>
    implements HasTooltip, HasAudioFeedback {

    // Shared state for anonymous subclasses to modify.
    public ButtonAPI m_checkbox;
    public float textX1 = 0;
    public float textX2 = 0;
    public float textY1 = 0;
    public float textY2 = 0;
    public float textW1 = 0;
    public float textW2 = 0;
    public float textH1 = 0;
    public float textH2 = 0;
    public LabelAPI label1 = null;
    public LabelAPI label2 = null;

    private boolean isSoundEnabled = true;

    public TextPanel(UIPanelAPI parent, int width, int height) {
        this(parent, width, height, new BasePanelPlugin<>());
    }

    public TextPanel(UIPanelAPI parent, int width, int height, BasePanelPlugin<TextPanel> plugin) {
        super(parent, width, height, plugin);

        initializePlugin(hasPlugin);
        createPanel();
    }

    public void setSoundEnabled(boolean a) {
        isSoundEnabled = a;
    }

    public boolean isSoundEnabled() {
        return isSoundEnabled;
    }

    public void initializePlugin(boolean hasPlugin) {
        getPlugin().init(this);
    }

    public void createPanel() {}

    public CustomPanelAPI getTpParent() {
        return null;
    }

    public TooltipMakerAPI createAndAttachTp() {
        return null;
    }
}

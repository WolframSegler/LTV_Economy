package wfg.wrap_ui.ui.systems;

import java.util.List;

import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.ui.UIPanelAPI;

import wfg.wrap_ui.ui.panels.CustomPanel;
import wfg.wrap_ui.ui.plugins.CustomPanelPlugin;
import wfg.wrap_ui.ui.plugins.CustomPanelPlugin.InputSnapshot;

/**
 * Try to make the systems final so that JIT can inline them.
 * Implement the plugin methods as needed.
 * 
 * <br></br>Design note:
 * All mutable state or configuration variables (e.g. toggles, transparency, 
 * flags) should be stored inside the plugin, not inside the systems.
 * Systems should access plugin state via getPlugin() to remain stateless
 * or minimally stateful, focusing on behavior and logic only.
 * Variables that the plugin is not going access should be stored in the system.
 * 
 * <br></br>Technical note:
 * Recursive generics e.g.
 * {@code PluginType extends CustomPanelPlugin<? extends CustomPanel<PluginType, ?>, ?>}
 * enable systems to know the exact concrete plugin type at compile time,
 * allowing type-safe access to plugin members without unchecked casts.
 */
public abstract class BaseSystem<
    PluginType extends CustomPanelPlugin<PanelType, PluginType>,
    PanelType extends CustomPanel<PluginType, PanelType, ? extends UIPanelAPI>
> {

    private PluginType plugin;

    public BaseSystem(PluginType a) {
        plugin = a;
    }

    public final PluginType getPlugin() {
        return plugin;
    }

    public final PanelType getPanel() {
        return plugin.getPanel();
    }

    /**
     * Runs before the game itself handles the inputs.
     */
    public void processInput(List<InputEventAPI> events, InputSnapshot input) {}
    public void advance(float amount, InputSnapshot input) {}
    public void renderBelow(float alphaMult, InputSnapshot input) {}
    public void render(float alphaMult, InputSnapshot input) {}
    public void onRemove(InputSnapshot input) {}
}

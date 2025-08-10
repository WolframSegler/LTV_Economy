package wfg_ltv_econ.ui.panels.components;

import java.util.List;

import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.ui.UIPanelAPI;

import wfg_ltv_econ.ui.panels.LtvCustomPanel;
import wfg_ltv_econ.ui.plugins.LtvCustomPanelPlugin;
import wfg_ltv_econ.ui.plugins.LtvCustomPanelPlugin.InputSnapshot;

/**
 * Try to make the components final so that JIT can inline them.
 * Implement the plugin methods as needed.
 * 
 * <br></br>Design note:
 * All mutable state or configuration variables (e.g. toggles, transparency, 
 * flags) should be stored inside the plugin, not inside components.
 * Components should access plugin state via getPlugin() to remain stateless
 * or minimally stateful, focusing on behavior and logic only.
 * Variables that the plugin is not going access should be stored in the component.
 * 
 * <br></br>Technical note:
 * Recursive generics e.g.
 * {@code PluginType extends LtvCustomPanelPlugin<? extends LtvCustomPanel<PluginType, ?>, ?>}
 * enable components to know the exact concrete plugin type at compile time,
 * allowing type-safe access to plugin members without unchecked casts.
 */
public abstract class BaseComponent<
    PluginType extends LtvCustomPanelPlugin<PanelType, PluginType>,
    PanelType extends LtvCustomPanel<PluginType, PanelType, ? extends UIPanelAPI>
> {

    private PluginType plugin;

    public BaseComponent(PluginType a) {
        plugin = a;
    }

    public final PluginType getPlugin() {
        return plugin;
    }

    public final PanelType getPanel() {
        return plugin.getPanel();
    }

    public void advance(float amount, InputSnapshot input) {}
    public void renderBelow(float alphaMult, InputSnapshot input) {}
    public void render(float alphaMult, InputSnapshot input) {}
    public void processInput(List<InputEventAPI> events) {}
    public void onRemove(InputSnapshot input) {}
}

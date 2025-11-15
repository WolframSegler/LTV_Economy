package wfg.wrap_ui.ui.dialogs;

import java.awt.Color;

import com.fs.starfarer.api.ui.CustomPanelAPI;
import com.fs.starfarer.api.ui.UIPanelAPI;

import wfg.wrap_ui.ui.panels.CustomPanel;
import wfg.wrap_ui.ui.panels.CustomPanel.HasBackground;
import wfg.wrap_ui.ui.plugins.CustomPanelPlugin;

/**
 * A wrapper panel that bridges the game-provided detail dialog panel with
 * the plugin logic managing that dialog.
 *
 * <p>Unlike creating a standalone panel, this class wraps the existing panel
 * supplied by the game’s detail dialog system, avoiding redundant panel creation.</p>
 *
 * <p>This design allows the dialog’s plugin logic to integrate cleanly into the
 * component system, enabling consistent behavior, background handling,
 * and rendering within the larger UI framework.</p>
 */
public class CustomDetailDialogPanel<
    PluginType extends CustomPanelPlugin<
        CustomDetailDialogPanel<PluginType>,
        PluginType
    >
> extends CustomPanel<PluginType, CustomDetailDialogPanel<PluginType>, CustomPanelAPI> implements HasBackground{

    public Color BgColor = Color.BLACK;
    public boolean isBgEnabled = true;

    public CustomDetailDialogPanel(UIPanelAPI parent, int width, int height,
        PluginType plugin) {
        super(parent, width, height, plugin);

        initializePlugin(hasPlugin);
        createPanel();
    }

    public void initializePlugin(boolean hasPlugin) {
        if (!hasPlugin) return;
        
        getPlugin().init(this);
    }

    public void createPanel() {}

    public void setBgColor(Color color) {
        BgColor = color;

        isBgEnabled = true;
    }

    @Override
    public boolean isBgEnabled() {
        return true;
    }

    @Override
    public float getBgAlpha() {
        return 1f;
    }

    @Override
    public Color getBgColor() {
        return new Color(0, 0, 0, 255);
    }
}
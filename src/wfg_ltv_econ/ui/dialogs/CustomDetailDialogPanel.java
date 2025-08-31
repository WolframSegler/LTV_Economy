package wfg_ltv_econ.ui.dialogs;

import java.awt.Color;

import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.ui.CustomPanelAPI;
import com.fs.starfarer.api.ui.UIPanelAPI;

import wfg_ltv_econ.ui.panels.LtvCustomPanel;
import wfg_ltv_econ.ui.panels.LtvCustomPanel.HasBackground;
import wfg_ltv_econ.ui.plugins.LtvCustomPanelPlugin;

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
    PluginType extends LtvCustomPanelPlugin<
        CustomDetailDialogPanel<PluginType>,
        PluginType
    >
> extends LtvCustomPanel<PluginType, CustomDetailDialogPanel<PluginType>, CustomPanelAPI> implements HasBackground{

    public Color BgColor = Color.BLACK;
    public boolean isBgEnabled = true;

    public CustomDetailDialogPanel(UIPanelAPI root, UIPanelAPI parent, MarketAPI market, int width, int height,
        PluginType plugin) {
        super(root, parent, width, height, plugin, market);

        initializePlugin(hasPlugin);
        createPanel();
    }

    public void initializePlugin(boolean hasPlugin) {
        if (!hasPlugin) return;
        
        getPlugin().init(this);
    }

    public void createPanel() {}

    @Override
    public void setBgColor(Color color) {
        BgColor = color;

        isBgEnabled = true;
    }

    @Override
    public boolean isBgEnabled() {
        return true;
    }

    @Override
    public float getBgTransparency() {
        return 1f;
    }

    @Override
    public Color getBgColor() {
        return new Color(0, 0, 0, 255);
    }
}
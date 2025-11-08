package wfg.wrap_ui.ui.panels;

import java.awt.Color;

import com.fs.starfarer.api.ui.CustomPanelAPI;
import com.fs.starfarer.api.ui.UIPanelAPI;

import wfg.wrap_ui.ui.panels.CustomPanel.HasBackground;
import wfg.wrap_ui.ui.plugins.BasePanelPlugin;

/**
 * An empty implementation of {@link CustomPanel}
 */
public class BasePanel extends CustomPanel<
    BasePanelPlugin<BasePanel>,
    BasePanel,
    CustomPanelAPI
> implements HasBackground{

    public Color BgColor = Color.BLACK;
    public boolean isBgEnabled = true;

    public BasePanel(UIPanelAPI parent, int width, int height,
        BasePanelPlugin<BasePanel> plugin) {
        super(parent, width, height, plugin);

        initializePlugin(hasPlugin);
        createPanel();
    }

    public void initializePlugin(boolean hasPlugin) {
        getPlugin().init(this);
    }

    public void createPanel() {

    }

    public void setBgColor(Color color) {
        BgColor = color;

        isBgEnabled = true;
    }
    public boolean isBgEnabled() {
        return true;
    }
}

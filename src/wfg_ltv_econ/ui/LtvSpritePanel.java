package wfg_ltv_econ.ui;

import java.awt.Color;

import com.fs.starfarer.api.campaign.CustomUIPanelPlugin;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.ui.UIPanelAPI;

import wfg_ltv_econ.plugins.LtvCustomPanelPlugin.Glow;
import wfg_ltv_econ.plugins.LtvCustomPanelPlugin.Outline;
import wfg_ltv_econ.plugins.LtvSpritePanelPlugin;

public class LtvSpritePanel extends LtvCustomPanel {

    public final String m_spriteID;
    public Color color;
    public Color fillColor;
    public boolean drawBorder = false;

    public LtvSpritePanel(UIPanelAPI root, UIPanelAPI parent, MarketAPI market, int width, int height,
        CustomUIPanelPlugin plugin, String spriteID, Color color, Color fillColor, boolean drawBorder) {
        super(root, parent, width, height, plugin, market);

        m_spriteID = spriteID;
        this.color = color;
        this.fillColor = fillColor;
        this.drawBorder = drawBorder;

        initializePlugin(hasPlugin);
        createPanel();
    }

    public void initializePlugin(boolean hasPlugin) {
        LtvSpritePanelPlugin plugin = ((LtvSpritePanelPlugin) m_panel.getPlugin());
        plugin.init(this, Glow.NONE, false, false, Outline.NONE);
        plugin.init(m_spriteID, color, fillColor);
        plugin.setIgnoreUIState(true);
        plugin.setOutline(drawBorder ? Outline.VERY_THIN : Outline.NONE);
    }

    public void setColor(Color a) {
        color = a;
    }

    public void createPanel() {}
}

package wfg_ltv_econ.ui.panels;

import java.awt.Color;

import com.fs.graphics.Sprite;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CustomUIPanelPlugin;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.ui.UIPanelAPI;

import wfg_ltv_econ.ui.plugins.LtvSpritePanelPlugin;
import wfg_ltv_econ.ui.plugins.LtvCustomPanelPlugin.Glow;
import wfg_ltv_econ.ui.plugins.LtvCustomPanelPlugin.Outline;

public class LtvSpritePanel extends LtvCustomPanel {

    public SpriteAPI m_sprite;
    public Color color;
    public Color fillColor;
    public boolean drawBorder = false;

    public LtvSpritePanel(UIPanelAPI root, UIPanelAPI parent, MarketAPI market, int width, int height,
        CustomUIPanelPlugin plugin, String spriteID, Color color, Color fillColor, boolean drawBorder) {
        super(root, parent, width, height, plugin, market);

        m_sprite = Global.getSettings().getSprite(spriteID);
        this.color = color;
        this.fillColor = fillColor;
        this.drawBorder = drawBorder;

        initializePlugin(hasPlugin);
        createPanel();
    }

    public void initializePlugin(boolean hasPlugin) {
        LtvSpritePanelPlugin plugin = ((LtvSpritePanelPlugin) m_panel.getPlugin());
        plugin.init(this, Glow.NONE, false, false, Outline.NONE);
        plugin.setIgnoreUIState(true);
        plugin.setOutline(drawBorder ? Outline.VERY_THIN : Outline.NONE);
    }

    public void setColor(Color a) {
        color = a;
    }

    public void setSprite(String spriteID) {
        m_sprite = Global.getSettings().getSprite(spriteID);
    }
    public void setSprite(SpriteAPI sprite) {
        m_sprite = sprite;
    }

    public void createPanel() {}
}

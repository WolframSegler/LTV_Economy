package wfg_ltv_econ.ui.panels;

import java.awt.Color;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.ui.UIPanelAPI;

import wfg_ltv_econ.ui.plugins.LtvSpritePanelPlugin;
import wfg_ltv_econ.ui.panels.LtvCustomPanel.HasOutline;
import wfg_ltv_econ.ui.panels.components.OutlineComponent.Outline;

public class LtvSpritePanel extends LtvCustomPanel<LtvSpritePanelPlugin<LtvSpritePanel>, LtvSpritePanel> 
    implements HasOutline{

    public SpriteAPI m_sprite;
    public Color color;
    public Color outlineColor;
    public Color fillColor;
    public boolean drawBorder = false;

    public LtvSpritePanel(UIPanelAPI root, UIPanelAPI parent, MarketAPI market, int width, int height,
        LtvSpritePanelPlugin<LtvSpritePanel> plugin, String spriteID, Color color, Color fillColor, boolean drawBorder) {
        super(root, parent, width, height, plugin, market);

        m_sprite = Global.getSettings().getSprite(spriteID);
        this.color = color;
        this.fillColor = fillColor;
        this.drawBorder = drawBorder;

        initializePlugin(hasPlugin);
        createPanel();
    }

    public void initializePlugin(boolean hasPlugin) {
        getPlugin().init(this);
        getPlugin().setIgnoreUIState(true);
    }

    public Outline getOutline() {
        return drawBorder ? Outline.VERY_THIN : Outline.NONE;
    }

    public Color getOutlineColor() {
        return outlineColor;
    }

    public void setOutlineColor(Color a) {
        outlineColor = a;
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

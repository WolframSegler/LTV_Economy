package wfg_ltv_econ.ui.panels;

import java.awt.Color;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.ui.CustomPanelAPI;
import com.fs.starfarer.api.ui.UIPanelAPI;

import wfg_ltv_econ.ui.plugins.LtvSpritePanelPlugin;
import wfg_ltv_econ.ui.panels.LtvCustomPanel.HasOutline;
import wfg_ltv_econ.ui.panels.components.OutlineComponent.Outline;


/**
 * LtvSpritePanel is a UI panel specialized for displaying a single sprite with optional coloring
 * and border outline. It extends {@link LtvCustomPanel} and implements {@link HasOutline} to
 * provide configurable visual appearance with minimal setup.
 * 
 * <p><b>Key features:</b>
 * <ul>
 *   <li>Holds and displays a sprite loaded from a sprite ID or directly assigned {@link SpriteAPI}.</li>
 *   <li>Supports setting primary color, fill color, and toggle for drawing a very thin border outline.</li>
 *   <li>Provides {@link HasOutline} implementation for integration with outline-aware UI components.</li>
 *   <li>Allows dynamic sprite and color updates via setter methods.</li>
 * </ul>
 * 
 * <p><b>Usage example:</b>
 * <pre>
 * LtvSpritePanel sprite = new LtvSpritePanel(root, parent, market, 64, 64, plugin, "ui/icons/sprite", Color.WHITE, null, true);
 * sprite.setOutlineColor(Color.RED);
 * 
 * panel.addComponent(sprite.getPanel());
 * </pre>
 */
public class LtvSpritePanel extends LtvCustomPanel<LtvSpritePanelPlugin, LtvSpritePanel, CustomPanelAPI> 
    implements HasOutline{

    public SpriteAPI m_sprite;
    public Color color;
    public Color outlineColor;
    public Color fillColor;
    public boolean drawBorder = false;

    public LtvSpritePanel(UIPanelAPI root, UIPanelAPI parent, MarketAPI market, int width, int height,
        LtvSpritePanelPlugin plugin, String spriteID, Color color, Color fillColor, boolean drawBorder) {
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
        getPlugin().init();
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

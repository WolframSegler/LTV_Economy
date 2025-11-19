package wfg.wrap_ui.ui.panels;

import java.awt.Color;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.ui.CustomPanelAPI;
import com.fs.starfarer.api.ui.UIPanelAPI;

import wfg.wrap_ui.ui.panels.CustomPanel.HasOutline;
import wfg.wrap_ui.ui.plugins.SpritePanelPlugin;
import wfg.wrap_ui.ui.systems.OutlineSystem.Outline;


/**
 * {@link SpritePanel} is a UI panel specialized for displaying a single sprite with optional coloring
 * and border outline. It extends {@link CustomPanel} and implements {@link HasOutline} to
 * provide configurable visual appearance with minimal setup.
 * 
 * <p><b>Key features:</b>
 * <ul>
 *   <li>Holds and displays a sprite loaded from a sprite ID or directly assigned {@link SpriteAPI}.</li>
 *   <li>Supports setting primary color, fill color, and toggle for drawing a very thin border outline.</li>
 *   <li>Allows dynamic sprite and color updates via setter methods.</li>
 * </ul>
 * 
 * <p><b>Usage:</b>
 * <ul>
 *   <li>To subclass and customize, extend {@link SpritePanel} with your own {@code PanelType}.</li>
 *   <li>To directly instantiate a generic panel without subclassing, use the inner static {@link Base} class.</li>
 * </ul>
 * 
 * <p><b>Example:</b>
 * <pre>
 * SpritePanel.Base sprite = new SpritePanel.Base(root, parent, market, 64, 64, plugin, "ui/icons/sprite", Color.WHITE, null, true);
 * sprite.setOutlineColor(Color.RED);
 * 
 * panel.addComponent(sprite.getPanel());
 * </pre>
 */
public class SpritePanel<
    PanelType extends SpritePanel<PanelType>
> extends CustomPanel<SpritePanelPlugin<PanelType>, PanelType, CustomPanelAPI> 
    implements HasOutline{

    public static class Base extends SpritePanel<Base> {
        public Base(UIPanelAPI parent, int width, int height, String spriteID, Color color,
            Color fillColor, boolean drawBorder
        ) {
            super(parent, width, height, new SpritePanelPlugin<Base>(), spriteID, color, fillColor, drawBorder);
        }
    }

    public SpriteAPI m_sprite;
    public Color color;
    public Color outlineColor;
    public Color fillColor;
    public Color texOutlineColor = Color.GREEN;
    public boolean drawBorder = false;
    public boolean drawTexOutline = false;

    public SpritePanel(UIPanelAPI parent, int width, int height,
        SpritePanelPlugin<PanelType> plugin, String spriteID, Color color, Color fillColor, boolean drawBorder) {
        super(parent, width, height, plugin);

        m_sprite = Global.getSettings().getSprite(spriteID);
        this.color = color;
        this.fillColor = fillColor;
        this.drawBorder = drawBorder;

        initializePlugin(hasPlugin);
    }

    @SuppressWarnings("unchecked")
    public void initializePlugin(boolean hasPlugin) {
        // PanelType is a subclass of LtvSpritePanel. This cast is safe.
        getPlugin().init((PanelType)this); 
        getPlugin().init();
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

    public void setDrawTexOutline(boolean a) {
        drawTexOutline = a;
    }

    public void setTexOutlineColor(Color a) {
        texOutlineColor = a;
    }
}

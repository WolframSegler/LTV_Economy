package wfg.wrap_ui.ui.panels;

import java.awt.Color;

import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.ui.CustomPanelAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.ui.UIPanelAPI;

import wfg.wrap_ui.ui.panels.CustomPanel.HasFader;
import wfg.wrap_ui.ui.panels.CustomPanel.HasTooltip;
import wfg.wrap_ui.ui.plugins.SpritePanelPlugin;

/**
 * A generic sprite panel with tooltip and fading support.
 *
 * <p>This class is designed to be subclassed or instantiated anonymously. The default implementation
 * provides a template, but the actual functionality should be defined by the subclass or anonymous
 * implementation. Users must override {@link #getTpParent()}, and {@link #createAndAttachTp()} to
 * have a working tooltip.</p>
 *
 * <p>Intended usage example:
 * <pre>{@code
 * SpritePanelWithTp panel = new SpritePanelWithTp(root, parent, market, width, height, plugin,
 *                                               "iconPath", color, fillColor, drawBorder) {
 *      @Override
 *      public CustomPanelAPI getTpParent() {
 *          return getPanel();
 *      }
 * }</pre>
 * <pre>{@code
 *      
 *      @Override
 *      public TooltipMakerAPI createAndAttachTp() {
 *          TooltipMakerAPI tp = getPanel().createUIElement(300, 20, false);
 *          tp.addPara("Example text", 3);
 *          
 *          add(tp);
 *          return tp; 
 *      }
 * };
 * }</pre></p>
 *
 * <p>By default, the glow color is white and tooltip methods return null</p>
 */
public class SpritePanelWithTp extends SpritePanel<SpritePanelWithTp> implements HasTooltip, HasFader {

    public SpritePanelWithTp(UIPanelAPI root, UIPanelAPI parent, MarketAPI market, int width, int height,
        SpritePanelPlugin<SpritePanelWithTp> plugin, String spriteID, Color color, Color fillColor, boolean drawBorder) {
        super(root, parent, market, width, height, plugin, spriteID, color, fillColor, drawBorder);

        initializePlugin(hasPlugin);
    }

    @Override
    public Color getGlowColor() {
        return Color.WHITE;
    }

    @Override
    public CustomPanelAPI getTpParent() {
        return null;
    }

    @Override
    public TooltipMakerAPI createAndAttachTp() {
        return null;
    }
}

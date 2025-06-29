package wfg_ltv_econ.ui;

import java.awt.Color;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CustomUIPanelPlugin;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.ui.UIPanelAPI;

import wfg_ltv_econ.plugins.LtvCustomPanelPlugin.GlowType;
import wfg_ltv_econ.plugins.LtvSpritePanelPlugin;

public class LtvSpritePanel extends LtvCustomPanel {

    public final SpriteAPI m_sprite;
    public final String m_spriteID;
    public Color color;
    public boolean drawBorder;

    public LtvSpritePanel(UIPanelAPI root, UIPanelAPI parent, MarketAPI market, int width, int height,
        CustomUIPanelPlugin plugin, String spriteID, Color color, boolean drawBorder) {
        this(root, parent, market, width, height, plugin, spriteID,
        Global.getSettings().getSprite(spriteID), 
        color, drawBorder);
    }

    public LtvSpritePanel(UIPanelAPI root, UIPanelAPI parent, MarketAPI market, int width, int height,
        CustomUIPanelPlugin plugin, String spriteID, SpriteAPI sprite, Color color, boolean drawBorder) {
        super(root, parent, width, height, plugin, market);

        m_spriteID = spriteID;
        m_sprite = sprite;
        this.color = color;
        this.drawBorder = drawBorder;

        initializePlugin(hasPlugin);
        createPanel();
    }

    public void initializePlugin(boolean hasPlugin) {
        LtvSpritePanelPlugin plugin = ((LtvSpritePanelPlugin) m_panel.getPlugin());
        plugin.init(this, GlowType.NONE, false, false, false);
        plugin.init(m_spriteID, color, drawBorder);
        plugin.setIgnoreUIState(true);
    }

    public void createPanel() {}
}

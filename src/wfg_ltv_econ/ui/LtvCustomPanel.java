package wfg_ltv_econ.ui;

import java.awt.Color;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CustomUIPanelPlugin;
import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.ui.CustomPanelAPI;
import com.fs.starfarer.api.ui.PositionAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.ui.UIPanelAPI;

public abstract class LtvCustomPanel{
    protected final UIPanelAPI m_parent;
    protected final CustomPanelAPI m_panel;
    protected MarketAPI m_market = null;
    protected FactionAPI m_faction = null;
    public Color BgColor = new Color(0, 0, 0, 255);

    protected boolean hasPlugin = false;

    /**
     * The child SHALL NOT add himself to the parent. The parent UIPanelAPI will add the child.
     * The parent SHALL NOT call createPanel(). Only the children may call it.
     * The parent SHALL NOT call initializePanel(). It may use members only the child has.
     */
    public LtvCustomPanel(UIPanelAPI parent, int width, int height, CustomUIPanelPlugin plugin, MarketAPI market) {
        m_parent = parent;
        m_market = market;
        if (market != null) {
            m_faction = market.getFaction();
        }

        hasPlugin = plugin != null;
        
        if (hasPlugin) {
            m_panel = Global.getSettings().createCustom(width, height, plugin);
        } else {
            m_panel = Global.getSettings().createCustom(width, height, null);
        }
    }

    public CustomPanelAPI getPanel() {
        return m_panel;
    }
    public PositionAPI getPanelPos() {
        return m_panel.getPosition();
    }
    public UIPanelAPI getParent() {
        return m_parent;
    }

    /**
     * Has a default value for null faction.
     */
    public FactionAPI getFaction() {
        if (m_faction == null) {
            return Global.getSettings().createBaseFaction("LtvCustomPanelWrapperFaction");
        }
        else {
            return m_faction;
        }
    }

    /**
     * The child must initialize the Plugin.
     * Leave it empty for no Plugin.
     */
    public abstract void initializePanel(boolean hasPlugin);

    /**
     * The method for populating the main panel.
     */
    public abstract void createPanel();

    /**
     * The Custom Plugin will call this method to create a tooltip.
     * The method can be left empty for cases where a tooltip is not needed.
     */
    public abstract void createTooltip(TooltipMakerAPI tooltip);
}

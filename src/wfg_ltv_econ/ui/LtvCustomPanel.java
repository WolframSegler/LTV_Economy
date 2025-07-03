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

import wfg_ltv_econ.plugins.LtvCustomPanelPlugin;
import wfg_ltv_econ.util.ReflectionUtils;
import wfg_ltv_econ.util.ReflectionUtils.ReflectedField;

public abstract class LtvCustomPanel{
    private final UIPanelAPI m_root;
    protected final UIPanelAPI m_parent;
    protected final CustomPanelAPI m_panel;
    protected final CustomUIPanelPlugin m_plugin;
    public MarketAPI m_market = null;
    public FactionAPI m_faction = null;
    public Color BgColor = new Color(0, 0, 0, 255);

    protected boolean hasPlugin = false;

    /**
     * The child SHALL NOT add himself to the parent. The parent UIPanelAPI will add the child.
     * The parent SHALL NOT call createPanel(). Only the children may call it.
     * The parent SHALL NOT call initializePanel(). It may use members only the child has.
     */
    public LtvCustomPanel(UIPanelAPI root, UIPanelAPI parent, int width, int height, CustomUIPanelPlugin plugin,
        MarketAPI market) {
        m_root = root;
        m_parent = parent;
        m_plugin = plugin;
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

    public UIPanelAPI getRoot() {
        return m_root == null ? m_panel : m_root;
    }

    public LtvCustomPanelPlugin getPlugin() {
        return (LtvCustomPanelPlugin)m_plugin;
    }

    /**
     * This is the realest way to do this. Do not judge.
     */
    public void setPlugin(CustomUIPanelPlugin newPlugin) {
        ReflectedField plugin = ReflectionUtils.getFieldsMatching(m_panel, null, CustomUIPanelPlugin.class).get(0);

        plugin.set(m_panel, newPlugin);
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
    public abstract void initializePlugin(boolean hasPlugin);

    /**
     * The method for populating the main panel.
     */
    public abstract void createPanel();

    public static interface TooltipProvider {
        /**
        * The LtvCustomPanelPlugin will call this.
        * Can be left empty for no tooltip.
        * Must create its own tooltip, attach it and position it.
        */
        TooltipMakerAPI createTooltip();

        /**
         * Remove the provided Tooltip and codexTooltip from its owner.
         * The custom Plugin does not know its owner.
         */
        void removeTooltip(TooltipMakerAPI tooltip);

        /**
         * Set any codexTooltip variables to the parameter here.
         * Used by TooltipUtils.
         * May also be used to attach Codex Tooltip to the primary tooltip depending on circumstance.
         * This is because variables in Java cannot be passed by reference.
         */
        void attachCodexTooltip(TooltipMakerAPI codexTooltip);
    }
}

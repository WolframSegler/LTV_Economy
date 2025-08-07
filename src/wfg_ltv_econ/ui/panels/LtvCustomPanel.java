package wfg_ltv_econ.ui.panels;

import java.awt.Color;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CustomUIPanelPlugin;
import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.ui.CustomPanelAPI;
import com.fs.starfarer.api.ui.LabelAPI;
import com.fs.starfarer.api.ui.PositionAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.ui.UIComponentAPI;
import com.fs.starfarer.api.ui.UIPanelAPI;
import com.fs.starfarer.campaign.CampaignEngine;

import wfg_ltv_econ.ui.ui_plugins.LtvCustomPanelPlugin;
import wfg_ltv_econ.util.ReflectionUtils;
import wfg_ltv_econ.util.ReflectionUtils.ReflectedField;

public abstract class LtvCustomPanel<PluginType extends LtvCustomPanelPlugin<? extends LtvCustomPanel<PluginType>>>{
    protected final UIPanelAPI m_parent;
    protected final CustomPanelAPI m_panel;
    protected final PluginType m_plugin;
    private final UIPanelAPI m_root;
    private FactionAPI m_faction = null;
    private MarketAPI m_market = null;

    protected boolean hasPlugin = false;

    /**
     * The child SHALL NOT add himself to the parent. The parent UIPanelAPI will add the child.
     * The parent SHALL NOT call createPanel(). Only the children may call it.
     * The parent SHALL NOT call initializePanel(). It may be using members only the child has.
     */
    public LtvCustomPanel(UIPanelAPI root, UIPanelAPI parent, int width, int height, PluginType plugin,
        MarketAPI market) {
        m_root = root;
        m_parent = parent;
        m_plugin = plugin;
        m_market = market;
        if (market != null) {
            m_faction = market.getFaction();
        }

        hasPlugin = plugin != null;
        
        m_panel = Global.getSettings().createCustom(width, height, hasPlugin ? plugin : null);
    }

    public CustomPanelAPI getPanel() {
        return m_panel;
    }

    public PositionAPI getPos() {
        return m_panel.getPosition();
    }

    public UIPanelAPI getParent() {
        return m_parent;
    }

    public UIPanelAPI getRoot() {
        final UIPanelAPI defaultRoot = CampaignEngine.getInstance().getCampaignUI().getDialogParent();
        return m_root == null ? defaultRoot : m_root;
    }

    public PluginType getPlugin() {
        return m_plugin;
    }

    /**
     * This is the realest way to do this. Do not judge.
     */
    public void setPlugin(CustomUIPanelPlugin newPlugin) {
        ReflectedField plugin = ReflectionUtils.getFieldsMatching(m_panel, null, CustomUIPanelPlugin.class).get(0);

        plugin.set(m_panel, newPlugin);
    }

    /**
     * Has a default value for no faction.
     */
    public FactionAPI getFaction() {
        if (m_faction == null) {
            final String factionID = "player";

            if (Global.getSector().getFaction(factionID) == null) {
                return Global.getSettings().createBaseFaction(factionID);
            }

            return Global.getSector().getFaction(factionID);
        }
        else {
            return m_faction;
        }
    }

    public MarketAPI getMarket() {
        return m_market;
    }

    public void setMarket(MarketAPI market) {
        m_market = market;
        m_faction = market.getFaction();
    }

    public PositionAPI add(LabelAPI a) {
        return add((UIComponentAPI) a);
    }

    public PositionAPI add(UIComponentAPI a) {
        getPanel().addComponent(a);

        return (a).getPosition();
    }

    public void remove(LabelAPI a) {
        remove((UIComponentAPI) a);
    }

    public void remove(UIComponentAPI a) {
        getPanel().removeComponent(a);
    }

    /**
     * The child must initialize the Plugin.
     * The plugin should not hold a copy the panel's members.
     * Leave it empty for no Plugin.
     */
    public abstract void initializePlugin(boolean hasPlugin);

    /**
     * The method for populating the main panel.
     */
    public abstract void createPanel();

    public static interface ColoredPanel {
        Color getBgColor();
        Color getOutlineColor();
        Color getGlowColor();

        void setBgColor(Color color);
        void setOutlineColor(Color color);
        void setGlowColor(Color color);
    }

    public static interface TooltipProvider {

        /**
        * Return the panel the tooltip should be attached to.
        * Must return a non-null UIPanelAPI.
        */
        UIPanelAPI getTooltipAttachmentPoint();

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

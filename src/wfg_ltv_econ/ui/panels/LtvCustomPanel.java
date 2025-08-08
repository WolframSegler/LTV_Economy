package wfg_ltv_econ.ui.panels;

import java.awt.Color;
import java.util.function.Supplier;

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
import com.fs.starfarer.api.util.FaderUtil;
import com.fs.starfarer.api.util.Misc;
import com.fs.starfarer.campaign.CampaignEngine;

import wfg_ltv_econ.ui.panels.components.FaderComponent.Glow;
import wfg_ltv_econ.ui.panels.components.OutlineComponent.Outline;
import wfg_ltv_econ.ui.plugins.LtvCustomPanelPlugin;
import wfg_ltv_econ.util.ReflectionUtils;
import wfg_ltv_econ.util.ReflectionUtils.ReflectedField;

/**
 * Represents the visual and layout container for a set of components managed by a matching {@link LtvCustomPanelPlugin}.
 *
 * <p><strong>Design principles:</strong></p>
 * <ul>
 *   <li>The panel is responsible for all <em>UI-specific</em> state — such as background color, position,
 *       dimensions, and any interface-specific properties (e.g. implementing {@link ColoredPanel}).</li>
 *   <li>The panel does not store or manage plugin-specific logic or toggles; those belong in the plugin.</li>
 *   <li>By implementing capability interfaces (like {@code ColoredPanel}), the panel exposes relevant data
 *       to both the plugin and components in a type-safe way.</li>
 *   <li>The panel type is bound to its plugin type via recursive generics to ensure compile-time type safety.</li>
 * </ul>
 *
 * <p>Example usage:</p>
 * <pre>
 * // Panel implementing ColoredPanel
 * public class MyPanel extends LtvCustomPanel<MyPlugin, MyPanel> implements ColoredPanel {
 *     private final Color bgColor;
 *
 *     public Color getBgColor() { return bgColor; }
 * }
 * </pre>
 */
public abstract class LtvCustomPanel<
    PluginType extends LtvCustomPanelPlugin<? extends LtvCustomPanel<PluginType, PanelType>, ?>, PanelType> {
    protected final UIPanelAPI m_parent;
    protected final CustomPanelAPI m_panel;
    protected final PluginType m_plugin;
    private final UIPanelAPI m_root;
    private FactionAPI m_faction = null;
    private MarketAPI m_market = null;

    protected boolean hasPlugin = false;

    /**
     * Ownership and lifecycle rules for child panels:
     * <ul>
     *   <li>The child <b>MUST NOT</b> add itself to the parent.
     *      This prevents the child from being responsible for its own positioning,
     *       since each panel handles positioning its children separately.</li>
     *   <li>The parent <b>MUST NOT</b> call <code>createPanel()</code>.
     *      This ensures that the child’s members are fully initialized before panel creation.</li>
     *   <li>The parent <b>MUST NOT</b> call <code>initializePanel()</code>.
     *      Initialization may depend on child-specific members. 
     *      The child <b>must</b> call this in the Constructor.</li>
     * </ul>
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
     * The child is responsible for initializing the Plugin.
     * The plugin must not keep copies of the panel’s internal members.
     * If no Plugin is needed, this method should be left empty.
     *
     * <ul>
     *  <li>Custom components should be added to the plugin by the child within this method. Since component management belongs to the plugin, doing this in the constructor is discouraged. The constructor calls this method, keeping related logic together.</li>
     *  <li>Components provided by the UI library are added automatically by the base plugin.</li>
     *  <li>The panel must implement any required interfaces defined in LtvCustomPanel to support the relevant components.</li>
     *  <li>All state management for components should be handled within the plugin.</li>
     * </ul>
     */
    public abstract void initializePlugin(boolean hasPlugin);

    /**
     * The method for populating the main panel.
     */
    public abstract void createPanel();

    public static interface ColoredPanel {
        Color getGlowColor();

        void setGlowColor(Color color);
    }

    public static interface HasFader {

        default FaderUtil getFader() {
            return new FaderUtil(0, 0, 0.2f, true, true);
        }

        default Glow getGlowType() {
            return Glow.OVERLAY;
        }

        default boolean isPersistentGlow() {
            return false;
        }

        void setPersistentGlow();

        default float getOverlayBrightness() {
            return 1.2f;
        }

        Color getGlowColor();
    }

    public static interface HasOutline {
        void setOutline(Outline a);

        default Outline getOutline() {
            return Outline.LINE;
        }

        default Color getOutlineColor() {
            return Misc.getDarkPlayerColor();
        }
        void setOutlineColor(Color color);
    }

    public static interface HasAudioFeedback {
        default boolean isSoundEnabled() {
            return true;
        }

        void setSoundEnabled();
    }

    public static interface HasBackground {
        default Color getBgColor() {
            return new Color(0, 0, 0, 255);
        }

        void setBgColor(Color color);

        default boolean isBgEnabled() {
            return true;
        }

        /**
         * default is 0.65f which looks vanilla-like.
         */
        default float getBgTransparency() {
            return 0.65f;
        }
    }

    public static interface HasTooltip {

        /**
        * Return the panel the tooltip is attached to.
        * Must return a non-null UIPanelAPI.
        */
        UIPanelAPI getTooltipAttachmentPoint();

        /**
        * Return the panel the codex is attached to, ideally the tooltip itself.
        * Must return a non-null UIPanelAPI.
        */
        UIPanelAPI getCodexAttachmentPoint();

        /**
        * The TooltipComponent will call this.
        * Can be left empty for no tooltip.
        * Must create its own tooltip, attach it, position it and return it.
        */
        TooltipMakerAPI createTooltip();

        /**
        * The TooltipComponent will call this.
        * Can be left empty for no codex.
        * Must create its own codex, attach it, position it and return it.
        */
        TooltipMakerAPI createCodex();

        default boolean isTooltipEnabled() {
            return true;
        }

        float getTooltipDelay();

        /**
         * A tooltip interface that acts as a mutable shell.
         * Used primarily to pass null checks during UI construction and allow
         * tooltip creation to be deferred until the actual content is ready.
         *
         * This class should be returned from {@code createTooltip()} when the real tooltip
         * is not yet available. The {@code factory} Supplier is used to supply the actual
         * TooltipMakerAPI instance later, enabling lazy or dynamic creation.
         *
         * The real power lies in the ability to assign the {@code factory} field a Supplier
         * from any scope, allowing flexible tooltip-building logic that can depend on
         * runtime state or user input instead of fixed static data.
         *
         * For example, a table component can accept a factory from its user and call it
         * to create tooltips for headers or rows on demand, vastly improving flexibility.
         *
         * <p><b>Example usage:</b>
         * <pre>
         * public TooltipMakerAPI createTooltip() {
         *     PendingTooltip pending = new PendingTooltip();
         *     pending.factory = () -> {
         *         // Create and return the real tooltip here
         *         TooltipMakerAPI tooltip = somePanel.createTooltipInstance();
         *         // Configure tooltip as needed
         *         return tooltip;
         *     };
         *     return pending.factory.get();
         * }
         * </pre>
         */
        public static class PendingTooltip {
            public Supplier<TooltipMakerAPI> factory = null;
        }
    }
}

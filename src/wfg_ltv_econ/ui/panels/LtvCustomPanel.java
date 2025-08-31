package wfg_ltv_econ.ui.panels;

import java.awt.Color;
import java.util.Optional;
import java.util.function.Supplier;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CustomUIPanelPlugin;
import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.ui.CustomPanelAPI;
import com.fs.starfarer.api.ui.LabelAPI;
import com.fs.starfarer.api.ui.PositionAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI.ActionListenerDelegate;
import com.fs.starfarer.api.ui.UIComponentAPI;
import com.fs.starfarer.api.ui.UIPanelAPI;
import com.fs.starfarer.api.util.FaderUtil;
import com.fs.starfarer.api.util.Misc;
import com.fs.starfarer.campaign.CampaignEngine;

import wfg_ltv_econ.ui.plugins.LtvCustomPanelPlugin;
import wfg_ltv_econ.ui.systems.FaderSystem.Glow;
import wfg_ltv_econ.ui.systems.OutlineSystem.Outline;
import wfg_ltv_econ.util.ReflectionUtils;
import wfg_ltv_econ.util.ReflectionUtils.ReflectedField;

/**
 * Represents the visual and layout container for a set of components managed by a matching {@link LtvCustomPanelPlugin}.
 *
 * <p><strong>Design principles:</strong></p>
 * <ul>
 *   <li>The panel is responsible for all <em>UI-specific</em> state — such as background color, position,
 *       dimensions, and any interface-specific properties (e.g. implementing {@link HasBackground}).</li>
 *   <li>The panel does not store or manage plugin-specific logic or toggles; those belong in the plugin.</li>
 *   <li>By implementing capability interfaces (like {@link HasBackground}), the panel exposes relevant data
 *       to both the plugin and components in a type-safe way.</li>
 *   <li>Recursive bounds (with selective wildcards) ensure methods like <code>getPlugin()</code> / <code>getPanel()</code> resolve to the concrete types at compile time, while still allowing a single plugin implementation to be reused by multiple panel subclasses.</li>
 * </ul>
 *
 * <p>Example usage:</p>
 * <pre>
 * // Panel implementing {@link HasBackground}
 * public class MyPanel extends LtvCustomPanel< MyPanelPlugin<MyPanel>, MyPanel, CustomPanelAPI> implements HasBackground {
 *     private final Color bgColor;
 *
 *     public Color getBgColor() { return bgColor; }
 * }
 * </pre>
 */
public abstract class LtvCustomPanel<
    PluginType extends LtvCustomPanelPlugin<? extends LtvCustomPanel<?, ?, ParentType>, PluginType>, 
    PanelType extends LtvCustomPanel<PluginType, ? extends LtvCustomPanel<?, ? extends PanelType, ParentType>, ParentType>,
    ParentType extends UIPanelAPI
> {
    protected final ParentType m_parent;
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
    @SuppressWarnings("unchecked")
    public LtvCustomPanel(UIPanelAPI root, UIPanelAPI parent, int width, int height, PluginType plugin,
        MarketAPI market) {
        m_root = root;
        m_parent = (ParentType) parent;
        m_plugin = plugin;
        m_market = market;
        if (market != null) {
            m_faction = market.getFaction();
        }

        hasPlugin = plugin != null;
        
        m_panel = Global.getSettings().createCustom(width, height, hasPlugin ? plugin : null);
    }

    public final CustomPanelAPI getPanel() {
        return m_panel;
    }

    public final PositionAPI getPos() {
        return m_panel.getPosition();
    }

    /**
     * Returns the parent panel cast to the expected type.
     * <p>
     * This cast is unchecked and does not guarantee type safety at compile time.
     * It is provided for convenience based on the assumption that the parent
     * is of the expected type (usually {@code CustomPanelAPI}).
     * <p>
     * Use with caution: if the actual parent type differs, a {@code ClassCastException}
     * may occur at runtime.
     */
    public final ParentType getParent() {
        return m_parent;
    }

    public final UIPanelAPI getRoot() {
        return m_root == null ? getDialogParent() : m_root;
    }

    public static final UIPanelAPI getDialogParent() {
        return CampaignEngine.getInstance().getCampaignUI().getDialogParent();
    }

    public final PluginType getPlugin() {
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
    public final FactionAPI getFaction() {
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

    public final MarketAPI getMarket() {
        return m_market;
    }

    public final void setMarket(MarketAPI market) {
        m_market = market;
        m_faction = market.getFaction();
    }

    public final PositionAPI add(LabelAPI a) {
        return add((UIComponentAPI) a);
    }

    public final PositionAPI add(TooltipMakerAPI a) {
        return m_panel.addUIElement(a);
    }

    public final PositionAPI add(UIComponentAPI a) {
        m_panel.addComponent(a);

        return (a).getPosition();
    }

    public final void remove(LabelAPI a) {
        remove((UIComponentAPI) a);
    }

    public final void remove(UIComponentAPI a) {
        m_panel.removeComponent(a);
    }

    public void clearChildren() {
        ReflectionUtils.invoke(m_panel, "clearChildren");
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

    /**
     * Marks a panel as being able to accept and store a {@link HasActionListener}.
     * <p>
     * This interface is primarily intended for panels that work with the explicit
     * interaction methods defined in {@link HasActionListener}, allowing {@code ActionListenerComponent} 
     * to automatically invoke those callbacks.
     * </p>
     *
     * <p>
     * This design keeps {@code AcceptsActionListener} compatible with both {@link HasActionListener}
     * and {@link TooltipMakerAPI.ActionListenerDelegate} while encouraging use of the more explicit,
     * strongly-typed {@link HasActionListener} methods for clarity and composability.
     * </p>
     */
    public interface AcceptsActionListener {
        default Optional<HasActionListener> getActionListener() {
            return Optional.empty();
        }
        default void setActionListener(HasActionListener listener) {}

        /**
         * Optional support for the vanilla Starsector ActionListenerDelegate.
         * This listener is not invoked automatically by the custom ActionListenerComponent.
         * If you want to use it, you must call actionPerformed() manually from your plugin.
         */
        default Optional<ActionListenerDelegate> getVanillaActionListener() {
            return Optional.empty();
        }
        default void setVanillaActionListener(ActionListenerDelegate listener) {}
    }

    /**
     * A strongly-typed, explicit alternative to {@link com.fs.starfarer.api.ui.TooltipMakerAPI.ActionListenerDelegate}.
     * <p>
     * While the built-in {@code ActionListenerDelegate} reports a single, catch-all {@code actionPerformed} event,
     * this listener clearly differentiates between interaction types.
     * </p>
     * 
     * <p>
     * Implement this interface in any {@code LtvCustomPanel} (or compatible type) to handle specific user interactions.
     * The {@code source} parameter passed to each method is always the panel where the event originated.
     * </p>
     */
    public static interface HasActionListener {

        default boolean isListenerEnabled() {
            return true;
        }

        /**
         * Called once per frame while the cursor is over the panel.
         */
        default void onHover(LtvCustomPanel<?, ?, ?> source) {}

        /**
         * Called once when the cursor first enters the panel.
         */
        default void onHoverStarted(LtvCustomPanel<?, ?, ?> source) {}

        /**
         * Called once when the cursor leaves the panel.
         */
        default void onHoverEnded(LtvCustomPanel<?, ?, ?> source) {}

        default void onClicked(LtvCustomPanel<?, ?, ?> source, boolean isLeftClick) {}

        default void onShortcutPressed(LtvCustomPanel<?, ?, ?> source) {}

        /**
         * Use org.lwjgl.input.Keyboard
         */
        default Optional<Integer> getShortcut() {
            return Optional.empty();
        } 
    }

    public static interface HasFader {

        /**
         * Indicates whether this panel controls its own {@code faderUtil} instance.
         * <p>
         * Some panels may instead synchronize their fading behavior with another panel's fader.
         * In such cases, this should return {@code false}.
         *
         * @return {@code true} if this panel manages its own fader instance,
         *         {@code false} if it follows another panel's fader.
         */
        default boolean isFaderOwner() {
            return true;
        }

        default FaderUtil getFader() {
            return new FaderUtil(0, 0, 0.2f, true, true);
        }

        default Glow getGlowType() {
            return Glow.OVERLAY;
        }

        default boolean isPersistentGlow() {
            return false;
        }

        default void setPersistentGlow(boolean a) {}

        default float getOverlayBrightness() {
            return 1.2f;
        }

        default float getAdditiveBrightness() {
            return 0.6f;
        }

        Color getGlowColor();

        default void setGlowColor(Color a) {}

        /**
         * Used for additive Glow. Leave as null if not using it.
         */
        default Optional<SpriteAPI> getSprite() {
            return Optional.empty();
        }
    }

    public static interface HasOutline {
        default void setOutline(Outline a) {

        }

        default Outline getOutline() {
            return Outline.LINE;
        }

        default Color getOutlineColor() {
            return Misc.getDarkPlayerColor();
        }
        default void setOutlineColor(Color color) {

        }
    }

    public static interface HasAudioFeedback {
        default boolean isSoundEnabled() {
            return true;
        }

        default void setSoundEnabled(boolean a) {

        }
    }

    public static interface HasBackground {
        default Color getBgColor() {
            return new Color(0, 0, 0, 255);
        }

        default void setBgColor(Color color) {

        }

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
        * Return the parent panel of the tooltip.
        * Must return a non-null CustomPanelAPI. Otherwise the tooltip will not be removed.
        * Never attach the tooltip to the codex. It WILL crash the game.
        */
        CustomPanelAPI getTpParent();

        /**
        * Return the parent panel of the codex is, ideally the same as the tooltip.
        * Must return a non-null CustomPanelAPI. Otherwise the codex will not be removed.
        */
        default Optional<CustomPanelAPI> getCodexParent() {
            return Optional.empty();
        }

        /**
        * The TooltipComponent will call this.
        * Must create its own tooltip, attach it, position it and return it.
        * A new tooltip will be created instead of an update.
        * Therefore, conditional changes to the tooltip should happen during creation.
        */
        TooltipMakerAPI createAndAttachTp();

        /**
        * The TooltipComponent will call this.
        * Must create its own codex, attach it, position it and return it.
        */
        default Optional<TooltipMakerAPI> createAndAttachCodex() {
            return Optional.empty();
        }

        /**
         * The component uses this ID to open the codex.
         * Therefore it must be provided independent of {@code createCodex()} for codex behaviour.
         */ 
        default Optional<String> getCodexID() {
            return Optional.empty();
        }

        /**
         * Use this toggle to conditionally disable the tooltip.
         */
        default boolean isTooltipEnabled() {
            return true;
        }

        default float getTooltipDelay() {
            return 0.3f;
        }

        default boolean isExpanded() {
            return false;
        }

        default void setExpanded(boolean a) {}

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
        public static class PendingTooltip<ParentType extends CustomPanelAPI> {
            /**
             * Factory method to create the tooltip.
             * Must be set by subclasses or instances.
             */
            public Supplier<TooltipMakerAPI> factory;

            /**
             * Factory method to return the parent panel of the tooltip created by the factory.
             * Must return a non-null UIPanelAPI.
             * Classes that support PendingTooltip will use this factory.
             */
            public  Supplier<ParentType> getParent;
        }
    }
}

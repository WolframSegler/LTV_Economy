package wfg.ltv_econ.ui.reusable;

import static wfg.native_ui.util.Globals.settings;

import java.util.List;

import com.fs.starfarer.api.campaign.BaseCustomUIPanelPlugin;
import com.fs.starfarer.api.ui.CustomPanelAPI;
import com.fs.starfarer.api.ui.UIComponentAPI;
import com.fs.starfarer.api.ui.UIPanelAPI;

import rolflectionlib.util.RolfLectionUtil;
import wfg.native_ui.ui.panel.CustomPanel;

/**
 * Gets injected to vanilla UI hierarchies. Its presence indicates that the UI has yet to be refreshed.
 * Used to prevent constant UI-replacement.
 */
public class IdentityMarker extends BaseCustomUIPanelPlugin {
    private IdentityMarker() {}
    private static final UIComponentAPI element = settings.createCustom(0f, 0f, new IdentityMarker());

    public static final void attach(UIPanelAPI parent) {
        parent.addComponent(element);
    }

    public static final boolean isMarker(Object obj) {
        return obj instanceof CustomPanelAPI custom && custom.getPlugin() instanceof IdentityMarker;
    }

    public static final boolean isPresent(UIPanelAPI parent) {
        final List<?> children = (List<?>) RolfLectionUtil.invokeMethodDirectly(
            CustomPanel.getChildrenNonCopyMethod, parent);
        
        for (Object child : children) {
            if (isMarker(child)) return true;
        }
        return false;
    }
}
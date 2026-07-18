package wfg.ltv_econ.ui.reusable;

import java.util.List;

import com.fs.starfarer.api.ui.UIComponentAPI;
import com.fs.starfarer.api.ui.UIPanelAPI;

import wfg.native_ui.internal.ui.core.UIElement;
import wfg.native_ui.ui.MethodFields;
import wfg.native_ui.ui.core.UIElementAPI;

/**
 * Gets injected to vanilla UI hierarchies. Its presence indicates that the UI has yet to be refreshed.
 * Used to prevent constant UI-replacement.
 */
public class IdentityMarker extends UIElement {
    private IdentityMarker() {}
    private static final UIElementAPI element = new UIElement(0f, 0f);

    public static final void attach(UIPanelAPI parent) {
        parent.addComponent(element);
    }

    public static final boolean isMarker(Object obj) {
        return obj instanceof IdentityMarker;
    }

    public static final boolean isPresent(UIPanelAPI parent) {
        final List<UIComponentAPI> children = MethodFields.getChildrenNonCopy(parent);
        
        for (UIComponentAPI child : children) {
            if (isMarker(child)) return true;
        }
        return false;
    }
}
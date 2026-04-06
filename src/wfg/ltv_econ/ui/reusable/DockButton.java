package wfg.ltv_econ.ui.reusable;

import java.util.function.Supplier;

import com.fs.starfarer.api.ui.UIPanelAPI;

import wfg.native_ui.ui.panels.Button;
import wfg.native_ui.ui.panels.DockPanel;

// TODO move to NativeUI
public class DockButton<T extends DockPanel> extends Button {
    private T dock;
    private final Supplier<T> dockFactory;

    public DockButton(UIPanelAPI parent, int width, int height, String text, String font, Supplier<T> dockFactory) {
        super(parent, width, height, text, font, null);
        this.dockFactory = dockFactory;
        onClicked = btn -> {
            if (dock == null) createDock();
            if (dock.isOpen()) dock.close();
            else dock.open(true);
        };
        
        setQuickMode(true);
    }

    private void createDock() {
        dock = dockFactory.get();
        dock.onRemoved = d -> dock = null;
    }
}
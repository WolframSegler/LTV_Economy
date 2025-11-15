package wfg.wrap_ui.ui.plugins;

import wfg.wrap_ui.ui.panels.Button;

public class ButtonPlugin extends CustomPanelPlugin<Button, ButtonPlugin> {
    @Override
    public void renderBelow(float alphaMult) {
        super.renderBelow(alphaMult);
        getPanel().renderImpl(alphaMult);
    }
}

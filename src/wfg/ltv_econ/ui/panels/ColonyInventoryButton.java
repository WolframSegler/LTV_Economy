package wfg.ltv_econ.ui.panels;

import java.awt.Color;

import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.ui.Alignment;
import com.fs.starfarer.api.ui.ButtonAPI;
import com.fs.starfarer.api.ui.CustomPanelAPI;
import com.fs.starfarer.api.ui.CutStyle;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.ui.UIPanelAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI.ActionListenerDelegate;
import com.fs.starfarer.api.util.Misc;

import wfg.ltv_econ.ui.dialogs.ColonyInvDialog;
import wfg.wrap_ui.util.WrapUiUtils;
import wfg.wrap_ui.ui.panels.CustomPanel;
import wfg.wrap_ui.ui.panels.CustomPanel.HasBackground;
import wfg.wrap_ui.ui.panels.CustomPanel.HasOutline;
import wfg.wrap_ui.ui.plugins.CustomPanelPlugin;

public class ColonyInventoryButton extends CustomPanel<
    ColonyInventoryButton.ColonyInvButtonPlugin, ColonyInventoryButton, CustomPanelAPI
> implements HasOutline, ActionListenerDelegate, HasBackground{

    public static final int ButtonH = 20;

    private final MarketAPI m_market;

    public ColonyInventoryButton(UIPanelAPI parent, MarketAPI market) {
        super(parent, LtvCommodityPanel.STANDARD_WIDTH, ButtonH, new ColonyInvButtonPlugin());

        m_market = market;

        initializePlugin(hasPlugin);
        createPanel();
    }

    @Override
    public void initializePlugin(boolean hasPlugin) {
        getPlugin().init(this);
        getPlugin().setOffsets(-1, -1, 2, 2);
    }

    @Override
    public void createPanel() {
        TooltipMakerAPI buttonWrapper = getPanel().createUIElement(
            getPos().getWidth(), getPos().getHeight(), false
        );

        buttonWrapper.setActionListenerDelegate(this);

        final ButtonAPI buildButton = buttonWrapper.addButton(
            "Colony Inventory",
            null,
            Misc.getBasePlayerColor(),
            new Color(0, 0, 0, 0),
            Alignment.MID,
            CutStyle.NONE,
            getPos().getWidth(),
            getPos().getHeight(),
            0
        );

        buildButton.setQuickMode(true);
        buildButton.setHighlightBrightness(2f);

        buttonWrapper.addComponent(buildButton).inBL(0, 0);
        add(buttonWrapper).inBL(0, 0);
    }

    public void actionPerformed(Object data, Object source) {
        final ColonyInvDialog dialogPanel = new ColonyInvDialog(m_market);

        WrapUiUtils.CustomDialogViewer(
            dialogPanel, ColonyInvDialog.PANEL_W, ColonyInvDialog.PANEL_H
        );
    }

    public float getBgTransparency() {
        return 0.85f;
    }

    public static class ColonyInvButtonPlugin extends CustomPanelPlugin<
        ColonyInventoryButton, ColonyInvButtonPlugin
    > {}
}

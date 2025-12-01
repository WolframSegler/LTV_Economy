package wfg.ltv_econ.ui.panels;

import java.util.ArrayList;
import java.util.List;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.SettingsAPI;
import com.fs.starfarer.api.ui.CustomPanelAPI;
import com.fs.starfarer.api.ui.Fonts;
import com.fs.starfarer.api.ui.LabelAPI;
import com.fs.starfarer.api.ui.UIPanelAPI;

import wfg.reflection.ReflectionUtils;
import wfg.wrap_ui.ui.panels.Button;
import wfg.wrap_ui.ui.panels.CustomPanel;
import wfg.wrap_ui.ui.panels.Button.CutStyle;
import wfg.wrap_ui.ui.plugins.BasePanelPlugin;
import wfg.wrap_ui.util.CallbackRunnable;

public class EconomyOverviewPanel extends CustomPanel<
    BasePanelPlugin<EconomyOverviewPanel>, EconomyOverviewPanel, UIPanelAPI>
{

    public static final int opad = 10;
    public static final int pad = 3;
    public static final int MAIN_PANEL_W = 1250;
    public static final int MAIN_PANEL_H = 700;
    public static final int NAVBAR_W = 200;
    public static final int NAV_BUTTON_W = 180;
    public static final int NAV_BUTTON_H = 28;
    public static final int CONTENT_PANEL_W = 1045;
    public static final int CONTENT_PANEL_H = 700;
    public static final int OPTIONS_PANEL_W = 200;
    public static final int OPTIONS_PANEL_H = 450;

    private static LabelAPI title = null;
    private static LabelAPI subtitle = null;
    private static CustomPanelAPI contentPanel = null;
    private static CustomPanelAPI optionsPanel = null;
    private static List<Button> navButtons = new ArrayList<>();
    private static Button firstButton = null;

    public EconomyOverviewPanel(UIPanelAPI parent) {
        super(parent, MAIN_PANEL_W, MAIN_PANEL_H, new BasePanelPlugin<>());

        getPlugin().init(this);
        createPanel();
    }

    public void createPanel() {
        final SettingsAPI settings = Global.getSettings();
        title = settings.createLabel("Economy Overview", Fonts.INSIGNIA_LARGE);
        final float titleH = title.computeTextHeight(title.getText());
        final float titleY = opad;
        add(title).inTL(pad, titleY);

        subtitle = settings.createLabel("Sector-wide activity summary", Fonts.ORBITRON_12);
        final float subtitleH = subtitle.computeTextHeight(title.getText());
        final float subtitleY = titleY + titleH + pad*2;
        add(subtitle).inTL(pad, subtitleY);

        createNavButtons();

        final CustomPanelAPI navbar = settings.createCustom(NAVBAR_W, 0, null);
        int currentY = opad*2; 
        for (Button btn : navButtons) {
            navbar.addComponent(btn.getPanel()).inTL(opad, currentY);
            currentY += pad*2 + NAV_BUTTON_H;
        }
        currentY += opad*2;

        navbar.getPosition().setSize(NAVBAR_W, currentY);
        final float navbarY = subtitleY + subtitleH + pad*2;
        add(navbar).inTL(pad, navbarY);

        contentPanel = settings.createCustom(CONTENT_PANEL_W, CONTENT_PANEL_H, null);
        optionsPanel = settings.createCustom(OPTIONS_PANEL_W, OPTIONS_PANEL_H, null);
        add(contentPanel).inTL(pad + NAVBAR_W + opad, titleY);
        add(optionsPanel).inBL(pad, 0);

        firstButton.click(false);
    }

    private final void createNavButtons() {
        navButtons.clear();
        CallbackRunnable<Button> buttonRunnable = (btn) -> {
            clearPanelAndButtonState(btn);
            final GlobalCommodityFlow content = new GlobalCommodityFlow(
                contentPanel, CONTENT_PANEL_W, CONTENT_PANEL_H
            );
            contentPanel.addComponent(content.getPanel()).inBL(0, 0);

            final CommoditySelectionPanel options = new CommoditySelectionPanel(
                optionsPanel, OPTIONS_PANEL_W, OPTIONS_PANEL_H, content
            );
            optionsPanel.addComponent(options.getPanel()).inBL(0, 0);
        };
        Button button = new Button(
            getPanel(), NAV_BUTTON_W, NAV_BUTTON_H, "Global Commodity Flow",
            Fonts.ORBITRON_12, buttonRunnable
        );
        button.setCutStyle(CutStyle.TL_BR);
        navButtons.add(button);
        firstButton = button;

        buttonRunnable = (btn) -> {
            clearPanelAndButtonState(btn);
        };
        button = new Button(
            getPanel(), NAV_BUTTON_W, NAV_BUTTON_H, "BUTTON B", Fonts.ORBITRON_12, buttonRunnable
        );
        button.setCutStyle(CutStyle.TL_BR);
        navButtons.add(button);

        buttonRunnable = (btn) -> {
            clearPanelAndButtonState(btn);
        };
        button = new Button(
            getPanel(), NAV_BUTTON_W, NAV_BUTTON_H, "BUTTON C", Fonts.ORBITRON_12, buttonRunnable
        );
        button.setCutStyle(CutStyle.TL_BR);
        navButtons.add(button);
        
        buttonRunnable = (btn) -> {
            clearPanelAndButtonState(btn);
        };
        button = new Button(
            getPanel(), NAV_BUTTON_W, NAV_BUTTON_H, "BUTTON D", Fonts.ORBITRON_12, buttonRunnable
        );
        button.setCutStyle(CutStyle.TL_BR);
        navButtons.add(button);
    }

    private static final void clearPanelAndButtonState(Button caller) {
        navButtons.forEach(b -> b.checked = false);
        caller.checked = true;
        ReflectionUtils.invoke(contentPanel, "clearChildren");
        ReflectionUtils.invoke(optionsPanel, "clearChildren");
    }
}
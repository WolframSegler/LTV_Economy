package wfg.ltv_econ.ui.panels;

import java.util.ArrayList;
import java.util.List;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.SettingsAPI;
import com.fs.starfarer.api.ui.CustomPanelAPI;
import com.fs.starfarer.api.ui.Fonts;
import com.fs.starfarer.api.ui.LabelAPI;
import com.fs.starfarer.api.ui.UIPanelAPI;

import wfg.wrap_ui.ui.panels.Button;
import wfg.wrap_ui.ui.panels.CustomPanel;
import wfg.wrap_ui.ui.panels.Button.CutStyle;
import wfg.wrap_ui.ui.panels.CustomPanel.HasBackground;
import wfg.wrap_ui.ui.plugins.BasePanelPlugin;
import wfg.wrap_ui.util.CallbackRunnable;

public class EconomyOverviewPanel extends CustomPanel<
    BasePanelPlugin<EconomyOverviewPanel>, EconomyOverviewPanel, UIPanelAPI> implements
    HasBackground
{

    public static final int opad = 10;
    public static final int pad = 3;
    public static final int MAIN_PANEL_W = 1200;
    public static final int MAIN_PANEL_H = 700;
    public static final int NAV_BUTTON_W = 160;
    public static final int NAV_BUTTON_H = 22;
    public static final int NAVBAR_W = 200;

    private static LabelAPI title = null;
    private static LabelAPI subtitle = null;
    private static List<Button> navButtons = new ArrayList<>();

    public EconomyOverviewPanel(UIPanelAPI parent) {
        super(parent, MAIN_PANEL_W, MAIN_PANEL_H, new BasePanelPlugin<>());

        initializePlugin(hasPlugin);
        createPanel();
    }

    public void initializePlugin(boolean hasPlugin) {
        getPlugin().init(this);
    }

    public void createPanel() {
        final SettingsAPI settings = Global.getSettings();
        title = settings.createLabel("Economy Overview", Fonts.INSIGNIA_LARGE);
        final float titleH = title.computeTextHeight(title.getText());
        final float titleY = opad;
        add(title).inTL(pad, titleY);

        subtitle = settings.createLabel("Sector-wide activity summary", Fonts.ORBITRON_20AA);
        final float subtitleH = subtitle.computeTextHeight(title.getText());
        final float subtitleY = titleY + titleH + pad*2;
        add(subtitle).inTL(pad, subtitleY);

        createNavButtons();

        final CustomPanelAPI navbar = settings.createCustom(NAVBAR_W, 0, null);
        int currentY = opad*2; 
        for (Button btn : navButtons) {
            navbar.addComponent(btn.getPanel()).inTL(opad, currentY);
            currentY += pad + NAV_BUTTON_H;
        }
        currentY += opad*2;

        navbar.getPosition().setSize(NAVBAR_W, currentY);

        add(navbar).inTL(pad, subtitleY + subtitleH + pad*2);
    }

    private final void createNavButtons() {
        navButtons.clear();
        CallbackRunnable<Button> buttonRunnable = (btn) -> {
            navButtons.forEach(b -> b.checked = false);
            btn.checked = true;
        };
        Button button = new Button(
            getPanel(), NAV_BUTTON_W, NAV_BUTTON_H, "BUTTON A", Fonts.ORBITRON_12, buttonRunnable
        );
        button.setCutStyle(CutStyle.TL_BR);
        navButtons.add(button);

        buttonRunnable = (btn) -> {
            navButtons.forEach(b -> b.checked = false);
            btn.checked = true;
        };
        button = new Button(
            getPanel(), NAV_BUTTON_W, NAV_BUTTON_H, "BUTTON B", Fonts.ORBITRON_12, buttonRunnable
        );
        button.setCutStyle(CutStyle.TL_BR);
        navButtons.add(button);

        buttonRunnable = (btn) -> {
            navButtons.forEach(b -> b.checked = false);
            btn.checked = true;
        };
        button = new Button(
            getPanel(), NAV_BUTTON_W, NAV_BUTTON_H, "BUTTON C", Fonts.ORBITRON_12, buttonRunnable
        );
        button.setCutStyle(CutStyle.TL_BR);
        navButtons.add(button);
        
        buttonRunnable = (btn) -> {
            navButtons.forEach(b -> b.checked = false);
            btn.checked = true;
        };
        button = new Button(
            getPanel(), NAV_BUTTON_W, NAV_BUTTON_H, "BUTTON D", Fonts.ORBITRON_12, buttonRunnable
        );
        button.setCutStyle(CutStyle.TL_BR);
        navButtons.add(button);
    }

    public float getBgAlpha() {
        return 0.8f;
    }
}
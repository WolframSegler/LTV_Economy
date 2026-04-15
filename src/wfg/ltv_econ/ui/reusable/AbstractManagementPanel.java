package wfg.ltv_econ.ui.reusable;

import static wfg.native_ui.util.UIConstants.*;

import java.util.ArrayList;
import java.util.List;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.SettingsAPI;
import com.fs.starfarer.api.ui.Fonts;
import com.fs.starfarer.api.ui.LabelAPI;
import com.fs.starfarer.api.ui.UIPanelAPI;

import rolflectionlib.util.RolfLectionUtil;
import wfg.native_ui.ui.core.UIBuildableAPI;
import wfg.native_ui.ui.functional.Button;
import wfg.native_ui.ui.functional.Button.CutStyle;
import wfg.native_ui.ui.panel.CustomPanel;
import wfg.native_ui.util.CallbackRunnable;

public abstract class AbstractManagementPanel<T extends AbstractManagementPanel<T>>
    extends CustomPanel<T> implements UIBuildableAPI
{
    protected static final int MAIN_PANEL_W = 1250;
    protected static final int MAIN_PANEL_H = 700;
    protected static final int NAVBAR_W = 200;
    protected static final int NAV_BUTTON_W = 180;
    protected static final int NAV_BUTTON_H = 28;
    protected static final int CONTENT_PANEL_W = 1045;
    protected static final int CONTENT_PANEL_H = 700;
    protected static final int OPTIONS_PANEL_W = 200;
    protected static final int OPTIONS_PANEL_H = 450;

    protected LabelAPI titleLabel;
    protected LabelAPI subtitleLabel;
    protected UIPanelAPI contentPanel;
    protected UIPanelAPI optionsPanel;
    protected List<Button> navButtons = new ArrayList<>();
    protected Button firstButton;

    protected AbstractManagementPanel(UIPanelAPI parent, int width, int height) {
        super(parent, width, height);
    }

    protected AbstractManagementPanel(UIPanelAPI parent) {
        this(parent, MAIN_PANEL_W, MAIN_PANEL_H);
    }

    protected abstract String getTitle();
    protected abstract String getSubtitle();
    protected abstract List<NavButtonDef> getNavButtonDefs();

    @Override
    public void buildUI() {
        final SettingsAPI settings = Global.getSettings();

        titleLabel = settings.createLabel(getTitle(), Fonts.INSIGNIA_LARGE);
        final float titleH = titleLabel.computeTextHeight(titleLabel.getText());
        final float titleY = opad;
        add(titleLabel).inTL(pad, titleY);

        subtitleLabel = settings.createLabel(getSubtitle(), Fonts.VICTOR_10);
        final float subtitleH = subtitleLabel.computeTextHeight(titleLabel.getText());
        final float subtitleY = titleY + titleH + pad * 2;
        add(subtitleLabel).inTL(pad, subtitleY);

        createNavButtons();

        final UIPanelAPI navbar = settings.createCustom(NAVBAR_W, 0, null);
        int currentY = opad * 2;
        for (Button btn : navButtons) {
            navbar.addComponent(btn.getPanel()).inTL(opad, currentY);
            currentY += pad * 2 + NAV_BUTTON_H;
        }
        currentY += opad * 2;
        navbar.getPosition().setSize(NAVBAR_W, currentY);

        final float navbarY = subtitleY + subtitleH + pad * 2;
        add(navbar).inTL(pad, navbarY);

        contentPanel = settings.createCustom(CONTENT_PANEL_W, CONTENT_PANEL_H, null);
        optionsPanel = settings.createCustom(OPTIONS_PANEL_W, OPTIONS_PANEL_H, null);
        add(contentPanel).inTL(pad + NAVBAR_W + opad, titleY);
        add(optionsPanel).inBL(pad, 0);

        firstButton.click(false);
    }

    private final void createNavButtons() {
        navButtons.clear();
        final List<NavButtonDef> defs = getNavButtonDefs();
        if (defs.isEmpty()) throw new IllegalStateException("Empty button definition list");

        for (NavButtonDef def : defs) {
            final CallbackRunnable<Button> runnable = (btn) -> {
                clearPanelAndButtonState(btn);
                def.contentSupplier.run();
            };
            final Button button = new Button(
                m_panel, NAV_BUTTON_W, NAV_BUTTON_H, def.label,
                Fonts.ORBITRON_12, runnable
            );
            button.cutStyle = CutStyle.TL_BR;
            button.bgAlpha = 1f;
            navButtons.add(button);
            if (firstButton == null) firstButton = button;
        }
    }

    private final void clearPanelAndButtonState(Button caller) {
        navButtons.forEach(b -> b.setChecked(false));
        caller.setChecked(true);
        RolfLectionUtil.invokeMethodDirectly(CustomPanel.clearChildrenMethod, contentPanel);
        RolfLectionUtil.invokeMethodDirectly(CustomPanel.clearChildrenMethod, optionsPanel);
    }

    protected static class NavButtonDef {
        public final String label;
        public final Runnable contentSupplier;

        public NavButtonDef(String label, Runnable contentSupplier) {
            this.label = label;
            this.contentSupplier = contentSupplier;
        }
    }
}
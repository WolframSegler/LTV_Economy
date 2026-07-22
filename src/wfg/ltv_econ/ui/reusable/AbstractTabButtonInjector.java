package wfg.ltv_econ.ui.reusable;

import java.util.ArrayList;
import java.util.List;

import org.lwjgl.input.Keyboard;

import com.fs.starfarer.api.GameState;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignUIAPI;
import com.fs.starfarer.api.campaign.CoreUITabId;
import com.fs.starfarer.api.ui.ButtonAPI;
import com.fs.starfarer.api.ui.Fonts;
import com.fs.starfarer.api.ui.UIComponentAPI;
import com.fs.starfarer.api.ui.UIPanelAPI;

import wfg.ltv_econ.ui.scripts.CoreTabUIBuilder;
import wfg.native_ui.ui.Attachments;
import wfg.native_ui.ui.MethodFields;
import wfg.native_ui.ui.widget.Button;
import wfg.native_ui.ui.widget.Button.CutStyle;
import wfg.native_ui.ui.functional.CallbackRunnable;

public abstract class AbstractTabButtonInjector implements CoreTabUIBuilder, CallbackRunnable<Button> {
    protected static final int BUTTON_HEIGHT = 18;
    protected static final int BUTTON_WIDTH = 130;

    private int buttonTabId = 0;
    private boolean uiInjected = false;

    protected Button injectedBtn = null;
    protected UIComponentAPI injectedComp = null;

    protected UIPanelAPI targetTab = null;
    protected List<UIComponentAPI> targetChildren = null;
    protected List<UIComponentAPI> hiddenPanels = new ArrayList<>(6);

    protected abstract int getCurrentTabIndex();
    protected abstract void setCurrentTabIndex(int index);
    protected abstract CoreUITabId getTargetCoreTabId();
    protected abstract String getButtonLabel();
    protected abstract UIComponentAPI createCustomComponent(UIPanelAPI parent);

    /**
     * Hook for subclasses to modify the button once it is added.
     */
    protected void onPostBtnInject(Button btn) {}

    /**
     * Hook for subclasses to perform additional injection steps after button creation.
     * Called once during injection, after the button and panel are created.
     */
    protected void onPostInject() {}

    @Override
    public void advance(float amount) {
        if (Global.getCurrentState() != GameState.CAMPAIGN) return;
        final CampaignUIAPI campaignUI = Global.getSector().getCampaignUI();
        if (!campaignUI.isShowingDialog()) return;

        for (UIComponentAPI panel : hiddenPanels) {
            if (panel.getOpacity() > 0f && getCurrentTabIndex() == buttonTabId) panel.setOpacity(0f);
        }

        final int index = getCurrentTabIndex();
        if (uiInjected) {
            if (index == buttonTabId) return;
            if (index != buttonTabId && (injectedBtn == null || !injectedBtn.isChecked())) return;
        }

        targetTab = Attachments.getCurrentTab();
        if (targetTab == null || campaignUI.getCurrentCoreTab() != getTargetCoreTabId()) return;

        targetChildren = MethodFields.getChildrenNonCopy(targetTab);

        if (!uiInjected) {
            createButtonAndComponent();
            onPostInject();
            uiInjected = true;
        }

        updateButtonAndPanels();
    }

    private final void createButtonAndComponent() {
        UIComponentAPI lastBtn = null;
        buttonTabId = 0;
        for (UIComponentAPI child : targetChildren) {
            if (child instanceof ButtonAPI button && injectedBtn != button) {
                buttonTabId++;
                if (lastBtn == null || lastBtn.getPosition().getX() < button.getPosition().getX()) {
                    lastBtn = button;
                }
            }
        }
        if (lastBtn == null) return;

        injectedBtn = new Button(BUTTON_WIDTH, BUTTON_HEIGHT, getButtonLabel(), Fonts.ORBITRON_12, this);
        injectedBtn.setShortcutAndAppendToText(getKeyFromIndex(buttonTabId));
        injectedBtn.setCutStyle(CutStyle.TL_TR);
        injectedBtn.overrideCutSize = 6;
        injectedBtn.setHighlightBrightness(0.3f);
        injectedBtn.tooltip.enabled = false;

        targetTab.addComponent(injectedBtn);
        injectedBtn.pos().rightOfTop(lastBtn, 1f);

        injectedComp = createCustomComponent(targetTab);
        targetTab.addComponent(injectedComp).inTL(0f, 20f);

        onPostBtnInject(injectedBtn);
    }

    private final void updateButtonAndPanels() {
        if (getCurrentTabIndex() != buttonTabId) {
            if (injectedBtn != null) injectedBtn.setChecked(false);
            if (injectedComp != null) injectedComp.setOpacity(0f);

            hiddenPanels.forEach(p -> p.setOpacity(1f));
            hiddenPanels.clear();
        }
    }

    @Override
    public final void run(Button btn) {
        if (injectedBtn != null) injectedBtn.setChecked(true);
        if (getCurrentTabIndex() == buttonTabId) return;

        setCurrentTabIndex(buttonTabId);
        if (injectedComp != null) injectedComp.setOpacity(1f);

        for (UIComponentAPI child : targetChildren) {
            if (child instanceof ButtonAPI button && injectedBtn != button) {
                button.setChecked(false);
                button.unhighlight();
            } else if (child != injectedComp) {
                child.setOpacity(0f);
                hiddenPanels.add(child);
            }
        }
    }

    public static final int getKeyFromIndex(int index) {
        return switch (index) {
            default -> Keyboard.KEY_NONE;
            case 0 -> Keyboard.KEY_1;
            case 1 -> Keyboard.KEY_2;
            case 2 -> Keyboard.KEY_3;
            case 3 -> Keyboard.KEY_4;
            case 4 -> Keyboard.KEY_5;
            case 5 -> Keyboard.KEY_6;
            case 6 -> Keyboard.KEY_7;
            case 7 -> Keyboard.KEY_8;
            case 8 -> Keyboard.KEY_9;
            case 9 -> Keyboard.KEY_0;
        };
    }
}
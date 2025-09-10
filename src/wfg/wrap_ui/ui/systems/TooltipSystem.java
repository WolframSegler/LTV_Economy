package wfg.wrap_ui.ui.systems;

import java.util.List;

import org.lwjgl.input.Keyboard;

import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.ui.impl.StandardTooltipV2Expandable;

import wfg.wrap_ui.ui.panels.CustomPanel;
import wfg.wrap_ui.ui.panels.CustomPanel.HasTooltip;
import wfg.wrap_ui.ui.plugins.CustomPanelPlugin;
import wfg.wrap_ui.ui.plugins.CustomPanelPlugin.InputSnapshot;
import wfg.wrap_ui.util.WrapUiUtils;

public final class TooltipSystem<
    PluginType extends CustomPanelPlugin<PanelType, PluginType>,
    PanelType extends CustomPanel<PluginType, PanelType, ?> & HasTooltip
> extends BaseSystem<PluginType, PanelType>{

    private final HasTooltip provider;
    private TooltipMakerAPI tooltip;
    private TooltipMakerAPI codex;

    private float hoverTime = 0f;
    private boolean wasF1Down = false;
    private boolean wasF2Down = false;

    public TooltipSystem(PluginType a, HasTooltip b) {
        super(a);
        provider = b;
    }

    @Override
    public final void advance(float amount, InputSnapshot input) {

        if (provider.isTooltipEnabled() && input.hoveredLastFrame && !input.hasLMBClickedBefore && getPlugin().isValidUIContext()) {
            hoverTime += amount;
            if (hoverTime > provider.getTooltipDelay()) {
                showTooltip();
            }
        } else {
            hoverTime = 0f;
            hideTooltip();
        }
    }

    @Override
    public void processInput(List<InputEventAPI> events, InputSnapshot input) {
        if (tooltip == null || !input.hoveredLastFrame) {
            getPanel().setExpanded(false);

        } else {
            final boolean isF1Down = Keyboard.isKeyDown(Keyboard.KEY_F1);
            final boolean isF2Down = Keyboard.isKeyDown(Keyboard.KEY_F2);

            if (isF1Down && !wasF1Down) {
                getPanel().setExpanded(!getPanel().isExpanded());
                hideTooltip();
            }

            if (isF2Down && !wasF2Down) {
                getPanel().getCodexID().ifPresent(codexID -> {
                    WrapUiUtils.openCodexPage(codexID);
                });
                hideTooltip();
            }

            wasF1Down = isF1Down;
            wasF2Down = isF2Down;
        }
    }

    public final void showTooltip() {
        if (tooltip == null) {
            tooltip = provider.createAndAttachTp();
            if (tooltip instanceof StandardTooltipV2Expandable standard) {
                standard.setShowBorder(true);
                standard.setShowBackground(true);
                standard.setBgAlpha(1f);
            }
            provider.getTpParent().bringComponentToTop(tooltip);
        }

        if (codex == null) {
            codex = provider.createAndAttachCodex().orElse(null);
            if (codex instanceof StandardTooltipV2Expandable standard) {
                standard.setShowBorder(true);
                standard.setShowBackground(true);
                standard.setBgAlpha(1f);
            }
            provider.getCodexParent().ifPresent(attachment -> {
                attachment.bringComponentToTop(codex);
            });
        }
    }

    public final void hideTooltip() {
        // The codex needs to be deleted first in-case it is anchored to the tooltip
        provider.getCodexParent().ifPresent(attachment -> {
            attachment.removeComponent(codex);
            codex = null;
        });

        if (tooltip != null) {
            provider.getTpParent().removeComponent(tooltip);
            tooltip = null;
        }
    }
}
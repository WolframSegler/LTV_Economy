package wfg_ltv_econ.ui.panels.components;

import org.lwjgl.input.Keyboard;

import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.ui.impl.StandardTooltipV2Expandable;

import wfg_ltv_econ.ui.panels.LtvCustomPanel;
import wfg_ltv_econ.ui.panels.LtvCustomPanel.HasTooltip;
import wfg_ltv_econ.ui.plugins.LtvCustomPanelPlugin;
import wfg_ltv_econ.ui.plugins.LtvCustomPanelPlugin.InputSnapshot;
import wfg_ltv_econ.util.UiUtils;

public final class TooltipComponent<
    PluginType extends LtvCustomPanelPlugin<PanelType, PluginType>,
    PanelType extends LtvCustomPanel<PluginType, PanelType, ?> & HasTooltip
> extends BaseComponent<PluginType, PanelType>{

    private final HasTooltip provider;
    private TooltipMakerAPI tooltip;
    private TooltipMakerAPI codex;

    private float hoverTime = 0f;

    public TooltipComponent(PluginType a, HasTooltip b) {
        super(a);
        provider = b;
    }

    @Override
    public final void advance(float amount, InputSnapshot input) {
        if (tooltip == null) {
            return;
        }

        if (provider.isTooltipEnabled() && input.hoveredLastFrame && !input.hasClickedBefore && getPlugin().isValidUIContext()) {
            hoverTime += amount;
            if (hoverTime > provider.getTooltipDelay()) {
                showTooltip();
            }
        } else {
            hoverTime = 0f;
            hideTooltip();
        }

        if (tooltip == null || input.hoveredLastFrame) {
            getPanel().setExpanded(false);

        } else {
            for (InputEventAPI event : input.events) {
                if (event.isMouseEvent()) {
                    continue;
                }
    
                if (event.isKeyDownEvent() && event.getEventValue() == Keyboard.KEY_F1) {
                    getPanel().setExpanded(!getPanel().isExpanded());
                    hideTooltip();
    
                    event.consume();
    
                    break;
                }
    
                if (event.isKeyDownEvent() && event.getEventValue() == Keyboard.KEY_F2 ) {
                    getPanel().getCodexID().ifPresent(codexID -> {
                        UiUtils.openCodexPage(codexID);
                    });
                    hideTooltip();
    
                    event.consume();

                    break;
                }
            }
        }
    }

    public final void showTooltip() {
        if (tooltip == null) {
            tooltip = provider.createAndAttachTooltip();
            if (tooltip instanceof StandardTooltipV2Expandable standard) {
                standard.setShowBackground(true);
                standard.setShowBorder(true);
            }
        }

        if (codex == null) {
            codex = provider.createAndAttachCodex().orElse(null);
            if (codex instanceof StandardTooltipV2Expandable standard) {
                standard.setShowBackground(true);
                standard.setShowBorder(true);
            }
        }
    }

    public final void hideTooltip() {
        if (tooltip != null) {
            provider.getTooltipAttachmentPoint().removeComponent(tooltip);
            tooltip = null;
        }

        provider.getCodexAttachmentPoint().ifPresent(attachment -> {
            attachment.removeComponent(codex);
            codex = null;
        });
    }
}
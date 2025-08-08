package wfg_ltv_econ.ui.panels.components;

import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.ui.impl.StandardTooltipV2Expandable;

import wfg_ltv_econ.ui.panels.LtvCustomPanel;
import wfg_ltv_econ.ui.panels.LtvCustomPanel.HasTooltip;
import wfg_ltv_econ.ui.plugins.LtvCustomPanelPlugin;
import wfg_ltv_econ.ui.plugins.LtvCustomPanelPlugin.InputSnapshot;

public final class TooltipComponent<
    PluginType extends LtvCustomPanelPlugin<PanelType, ?>,
    PanelType extends LtvCustomPanel<PluginType, ?> & HasTooltip
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
    final public void advance(float amount, InputSnapshot input) {
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
    }

    final public void showTooltip() {
        if (tooltip == null) {
            tooltip = provider.createTooltip();

            if (tooltip instanceof StandardTooltipV2Expandable standard) {
                standard.setShowBackground(true);
                standard.setShowBorder(true);
            }
        }
        if (codex == null) {
            codex = provider.createCodex();
            if (tooltip instanceof StandardTooltipV2Expandable standard) {
                standard.setShowBackground(true);
                standard.setShowBorder(true);
            }
        }
    }

    final public void hideTooltip() {
        if (tooltip != null) {
            provider.getTooltipAttachmentPoint().removeComponent(tooltip);
            tooltip = null;
        }
        if (codex != null) {
            provider.getCodexAttachmentPoint().removeComponent(codex);
            codex = null;
        }
    }
}
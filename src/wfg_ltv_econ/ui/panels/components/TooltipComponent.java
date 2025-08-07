package wfg_ltv_econ.ui.panels.components;

import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.ui.impl.StandardTooltipV2Expandable;

import wfg_ltv_econ.ui.panels.LtvCustomPanel.TooltipProvider;

public class TooltipComponent {
    private final TooltipProvider provider;
    private TooltipMakerAPI tooltip;
    private boolean tooltipActive = false;
    private float tooltipDelay = 0.3f;

    public TooltipComponent(TooltipProvider a) {
        provider = a;
    }

    public TooltipMakerAPI showTooltip() {
        if (tooltip == null) {
            tooltip = provider.createTooltip();

            if (tooltip instanceof StandardTooltipV2Expandable standard) {
                standard.setShowBackground(true);
                standard.setShowBorder(true);
            }
        }
        return tooltip;
    }

    public void hideTooltip() {
        if (tooltip != null) {
            provider.removeTooltip(tooltip);
            tooltip = null;
        }
    }

    public void setTooltipActive(boolean active) {
        this.tooltipActive = active;
    }

    public void setTooltipDelay(float delay) {
        this.tooltipDelay = delay;
    }

    public boolean isTooltipActive() {
        return tooltipActive;
    }

    public float getDelay() {
        return tooltipDelay;
    }
}
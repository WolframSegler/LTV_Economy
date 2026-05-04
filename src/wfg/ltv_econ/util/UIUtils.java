package wfg.ltv_econ.util;

import static wfg.native_ui.util.Globals.settings;
import static wfg.native_ui.util.UIConstants.*;

import java.awt.Color;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.FactionSpecAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.impl.campaign.ids.Strings;
import com.fs.starfarer.api.ui.Alignment;
import com.fs.starfarer.api.ui.LabelAPI;
import com.fs.starfarer.api.ui.UIPanelAPI;
import com.fs.starfarer.api.util.Misc;
import com.fs.starfarer.api.util.MutableValue;

import wfg.ltv_econ.constants.UIColors;
import wfg.ltv_econ.economy.commodity.CommodityCell;
import wfg.ltv_econ.economy.engine.EconomyEngine;
import wfg.native_ui.ui.visual.SpritePanel.Base;

public class UIUtils {
    public static final SpriteAPI STOCKPILES_FULL = settings.getSprite("icons", "stockpiles_full");
    public static final SpriteAPI STOCKPILES_MEDIUM = settings.getSprite("icons", "stockpiles_medium");
    public static final SpriteAPI STOCKPILES_LOW = settings.getSprite("icons", "stockpiles_low");
    public static final SpriteAPI STOCKPILES_EMPTY = settings.getSprite("icons", "stockpiles_empty");
    public static final SpriteAPI STOCKPILES_NO_DEMAND = settings.getSprite("icons", "stockpiles_no_demand");

    /**
     * I copied and cleaned this from the obfuscated code.
     * Because this is not available through the API for some reason.
     */
    public static final LabelAPI createPlayerCreditsLabel(String font, int height) {
        final MutableValue credits = Global.getSector().getPlayerFleet().getCargo().getCredits();
        final String valueStr = Misc.getWithDGS(credits.get()) + Strings.C;

        final LabelAPI label = settings.createLabel(
            "Player Credits: " + valueStr, font
        );
        if (font == "small_insignia") label.setAlignment(Alignment.LMID);
        
        label.setColor(gray);
        label.autoSizeToWidth(label.computeTextWidth(label.getText()));
        if (height > 0) {
            label.getPosition().setSize(label.getPosition().getWidth(), height);
        }

        label.setHighlightColor(highlight);
        label.highlightLast(valueStr);
        return label;
    }

    public static final LabelAPI createColonyCreditsLabel(String font, int height, String marketID) {
        final long credits = EconomyEngine.instance().getCredits(marketID);
        final String valueStr = Misc.getWithDGS(credits) + Strings.C;

        final LabelAPI label = settings.createLabel(
            "Colony Credits: " + valueStr, font
        );
        if (font == "small_insignia") label.setAlignment(Alignment.LMID);
        
        label.setColor(gray);
        label.autoSizeToWidth(label.computeTextWidth(label.getText()));
        if (height > 0) label.getPosition().setSize(label.getPosition().getWidth(), height);

        label.setHighlightColor(credits < 0 ? negative : highlight);
        label.highlightLast(valueStr);
        return label;
    }

    /**
     * I copied and cleaned this from the obfuscated code.
     * Because this is not available through the API for some reason.
     */
    public static final LabelAPI createMaxIndustriesLabel(String font, int height, MarketAPI market) {
        final int numInd = Misc.getNumIndustries(market);
        final int maxInd = Misc.getMaxIndustries(market);

        String text = numInd + " / " + maxInd;

        LabelAPI label = settings.createLabel("Industries: " + text, font);
        if (font == "small_insignia") {
            label.setAlignment(Alignment.LMID);
        }

        label.setColor(gray);
        label.autoSizeToWidth(label.computeTextWidth(label.getText()));
        if (height > 0) {
            label.getPosition().setSize(label.getPosition().getWidth(), height);
        }
        if (numInd > maxInd) {
            label.setHighlightColor(negative);
        } else {
            label.setHighlightColor(highlight);
        }

        label.highlightLast(text);
        return label;
    }

    public static final boolean canViewPrices() {
        return Global.getSector().getIntelManager().isPlayerInRangeOfCommRelay() ||
            settings.getBoolean("allowPriceViewAtAnyColony");
    }

    public static final Base getStockpilesIcon(final float ratio, final int size,
        final UIPanelAPI parent, final FactionSpecAPI faction, final boolean addRatioColors
    ) {
        final Color color = getStockpileColor(ratio, faction, addRatioColors);
        return getStockpilesIcon(ratio, size, parent, color, null, false, false);
    }

    public static final Base getStockpilesIcon(final CommodityCell cell, final int size,
        final UIPanelAPI parent, final Color iconColor
    ) {
        return getStockpilesIcon(cell.getDesiredAvailabilityRatio(), size, parent, iconColor,
            null, false, cell.getTargetQuantum(true) < 0.1f
        );
    }

    public static final Base getStockpilesIcon(final float ratio, final int size,
        final UIPanelAPI parent, final Color iconColor, final Color bgColor, final boolean drawBorder,
        final boolean useNoDemandIcon
    ) {
        final SpriteAPI iconPath;
        if (useNoDemandIcon) {
            iconPath = STOCKPILES_NO_DEMAND;
        } else if (ratio <= 0.25f) {
            iconPath = STOCKPILES_EMPTY;
        } else if (ratio <= 0.5f) {
            iconPath = STOCKPILES_LOW;
        } else if (ratio <= 0.75f) {
            iconPath = STOCKPILES_MEDIUM;
        } else {
            iconPath = STOCKPILES_FULL;
        }
        final Base icon = new Base(parent, size, size, iconPath, iconColor, bgColor);
        icon.outline.enabled = drawBorder;
        return icon;
    }

    public static final String getDayOrDays(int val) {
        return getDayOrDays(val, false);
    }

    public static final String getDayOrDays(long val) {
        return getDayOrDays(val, false);
    }

    public static final String getDayOrDays(float val) {
        return getDayOrDays(val, false);
    }

    public static final String getDayOrDays(double val) {
        return getDayOrDays(val, false);
    }

    public static final String getDayOrDays(int val, boolean capitalized) {
        return dayOrDays(val == 1, capitalized);
    }

    public static final String getDayOrDays(long val, boolean capitalized) {
        return dayOrDays(val == 1l, capitalized);
    }

    public static final String getDayOrDays(float val, boolean capitalized) {
        return dayOrDays(Math.abs(val - 1f) < 1e-4f, capitalized);
    }

    public static final String getDayOrDays(double val, boolean capitalized) {
        return dayOrDays(Math.abs(val - 1d) < 1e-4d, capitalized);
    }

    private static final Color getStockpileColor(final float ratio, final FactionSpecAPI faction,
        final boolean addRatioColors
    ) {
        if (!addRatioColors) return faction.getBaseUIColor();
        if (ratio <= 0.25f) return UIColors.COM_DEFICIT;
        if (ratio <= 0.5f) return UIColors.COM_IMPORT;
        if (ratio <= 0.75f) return UIColors.COM_LOCAL_PROD;
        return UIColors.COM_NOT_EXPORTED;
    }

    private static final String dayOrDays(boolean isSingular, boolean capitalized) {
        return capitalized ?
            isSingular ? "Day" : "Days":
            isSingular ? "day" : "days";
    }
}
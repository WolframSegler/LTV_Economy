package wfg.ltv_econ.util;

import static wfg.native_ui.util.Globals.settings;
import static wfg.native_ui.util.UIConstants.*;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.impl.campaign.ids.Strings;
import com.fs.starfarer.api.ui.Alignment;
import com.fs.starfarer.api.ui.LabelAPI;
import com.fs.starfarer.api.util.Misc;
import com.fs.starfarer.api.util.MutableValue;

import wfg.ltv_econ.economy.engine.EconomyEngine;

public class UIUtils {

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
}
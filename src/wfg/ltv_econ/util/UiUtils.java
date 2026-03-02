package wfg.ltv_econ.util;

import static wfg.native_ui.util.UIConstants.*;

import java.awt.Color;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.ui.Alignment;
import com.fs.starfarer.api.ui.LabelAPI;
import com.fs.starfarer.api.util.Misc;
import com.fs.starfarer.api.util.MutableValue;

import wfg.ltv_econ.economy.engine.EconomyEngine;

public class UiUtils {

    public static final Color inFactionColor = new Color(35, 70, 130, 255);
    public static final Color COLOR_DEFICIT = new Color(140, 15, 15);
    public static final Color COLOR_OVER_IMPORT = new Color(180, 90, 180);
    public static final Color COLOR_IMPORT_EXCLUSIVE = new Color(0, 180, 200);
    public static final Color COLOR_IMPORT = new Color(200, 140, 60);
    public static final Color COLOR_FACTION_IMPORT = new Color(240, 240, 100);
    public static final Color COLOR_LOCAL_PROD = new Color(122, 200, 122);
    public static final Color COLOR_EXPORT = new Color(63,  175, 63);
    public static final Color COLOR_NOT_EXPORTED = new Color(100, 140, 180);

    /**
     * I copied and cleaned this from the obfuscated code.
     * Because this is not available through the API for some reason.
     */
    public static final LabelAPI createPlayerCreditsLabel(String font, int height) {
        final MutableValue credits = Global.getSector().getPlayerFleet().getCargo().getCredits();

        final LabelAPI label = Global.getSettings().createLabel(
            "Player Credits: " + Misc.getWithDGS(credits.get()), font
        );
        if (font == "small_insignia") label.setAlignment(Alignment.LMID);
        
        label.setColor(gray);
        label.autoSizeToWidth(label.computeTextWidth(label.getText()));
        if (height > 0) {
            label.getPosition().setSize(label.getPosition().getWidth(), height);
        }

        label.setHighlightColor(highlight);
        label.highlightLast(Misc.getWithDGS(credits.get()));
        return label;
    }

    public static final LabelAPI createColonyCreditsLabel(String font, int height, String marketID) {
        final long credits = EconomyEngine.getInstance().getCredits(marketID);

        final LabelAPI label = Global.getSettings().createLabel(
            "Colony Credits: " + String.format("%,d", credits), font
        );
        if (font == "small_insignia") label.setAlignment(Alignment.LMID);
        
        label.setColor(gray);
        label.autoSizeToWidth(label.computeTextWidth(label.getText()));
        if (height > 0) label.getPosition().setSize(label.getPosition().getWidth(), height);

        label.setHighlightColor(credits < 0 ? negative : highlight);
        label.highlightLast(String.format("%,d", credits));
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

        LabelAPI label = Global.getSettings().createLabel("Industries: " + text, font);
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
            Global.getSettings().getBoolean("allowPriceViewAtAnyColony");
    }
}
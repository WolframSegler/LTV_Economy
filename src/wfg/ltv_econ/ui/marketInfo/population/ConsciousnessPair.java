package wfg.ltv_econ.ui.marketInfo.population;

import static wfg.native_ui.util.Globals.settings;
import static wfg.native_ui.util.UIConstants.*;
import static wfg.ltv_econ.constants.strings.LocalizedStrings.*;

import java.awt.Color;

import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.ui.Alignment;
import com.fs.starfarer.api.ui.LabelAPI;
import com.fs.starfarer.api.ui.UIPanelAPI;

import wfg.ltv_econ.economy.MarketPopulationData;
import wfg.native_ui.ui.visual.IconValuePairTp;
import wfg.native_ui.util.NativeUiUtils;
import wfg.native_ui.util.NativeUiUtils.AnchorType;

public class ConsciousnessPair extends IconValuePairTp {
    private static final SpriteAPI SOLIDARITY_ICON = settings.getSprite("ui", "solidarity_colored");

    public ConsciousnessPair(UIPanelAPI parent, int w, int h, MarketPopulationData data, Color color, String font) {
        super(parent, w, h, SOLIDARITY_ICON, data.getClassConsciousness(), false, color, font);
        final LabelAPI lbl = label();
        lbl.setHighlightOnMouseover(true);
        lbl.setAlignment(Alignment.MID);
        tooltip.builder = (tp, exp) -> {
            tp.addTitle(str("marketPopDataConsciousness"), base);

            tp.addPara(str("uiClassConsciousnessTpTxt"), opad);

            final float value = data.classConsciousnessDelta.computeEffective(
                data.getClassConsciousness()) - data.getClassConsciousness();
            tp.addPara(str("uiDailyChangePrefix"), 3, highlight, String.format("%.2f", value));
            tp.addStatModGrid(PopStatModValueGetter.GRID_W, PopStatModValueGetter.VALUE_W,
                pad, pad, data.classConsciousnessDelta, new PopStatModValueGetter()
            );
        };
        tooltip.positioner = (tp, exp) -> {
            NativeUiUtils.anchorPanelWithBounds(tp, getPanel(), AnchorType.RightTop, 0);
        };
    }
}
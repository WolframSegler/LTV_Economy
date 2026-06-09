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

public class HappinessPair extends IconValuePairTp {
    private static final SpriteAPI SMILING_ICON = settings.getSprite("ui", "smiling_face");

    public HappinessPair(UIPanelAPI parent, int w, int h, MarketPopulationData data, Color color, String font) {
        super(parent, w, h, SMILING_ICON, data.getHappiness(), false, color, font);
        final LabelAPI lbl = label();
        lbl.setHighlightOnMouseover(true);
        lbl.setAlignment(Alignment.MID);
        tooltip.builder = (tp, exp) -> {
            tp.addTitle(str("marketPopDataHappinessTxt"), base);

            tp.addPara(str("uiHappinessTpTxt"), opad);

            final float value = data.happinessDelta
                .computeEffective(data.getHappiness()) - data.getHappiness();
            tp.addPara(str("uiDailyChangePrefix"), 3, highlight, String.format("%.2f", value));
            tp.addStatModGrid(PopStatModValueGetter.GRID_W, PopStatModValueGetter.VALUE_W,
                pad, pad, data.happinessDelta, new PopStatModValueGetter()
            );
        };
        tooltip.positioner = (tp, exp) -> {
            NativeUiUtils.anchorPanelWithBounds(tp, m_panel, AnchorType.RightTop, opad);
        };
    }
}
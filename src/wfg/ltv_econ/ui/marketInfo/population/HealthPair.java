package wfg.ltv_econ.ui.marketInfo.population;

import static wfg.native_ui.util.Globals.settings;
import static wfg.native_ui.util.UIConstants.*;

import java.awt.Color;

import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.ui.Alignment;
import com.fs.starfarer.api.ui.LabelAPI;
import com.fs.starfarer.api.ui.UIPanelAPI;

import wfg.ltv_econ.economy.PlayerMarketData;
import wfg.native_ui.ui.visual.IconValuePairTp;
import wfg.native_ui.util.NativeUiUtils;
import wfg.native_ui.util.NativeUiUtils.AnchorType;

public class HealthPair extends IconValuePairTp {
    private static final SpriteAPI HEALTH_ICON = settings.getSprite("ui", "health");

    public HealthPair(UIPanelAPI parent, int w, int h, PlayerMarketData data, Color color, String font) {
        super(parent, w, h, HEALTH_ICON, data.getHealth(), false, color, font);
        final LabelAPI lbl = label();
        lbl.setHighlightOnMouseover(true);
        lbl.setAlignment(Alignment.MID);
        tooltip.builder = (tp, exp) -> {
            tp.addTitle("Health", base);

            tp.addPara("Overall health of the population. A higher value indicates better living conditions, food availability, and lower hazard exposure.", pad);
        
            final float value = data.healthDelta
                .computeEffective(data.getHealth()) - data.getHealth();
            tp.addPara("Daily Change: %s", 3, highlight, String.format("%.2f", value));
            tp.addStatModGrid(PopStatModValueGetter.GRID_W, PopStatModValueGetter.VALUE_W,
                pad, pad, data.healthDelta, new PopStatModValueGetter()
            );
        };
        tooltip.positioner = (tp, exp) -> {
            NativeUiUtils.anchorPanelWithBounds(tp, m_panel, AnchorType.RightTop, opad);
        };
    }
}
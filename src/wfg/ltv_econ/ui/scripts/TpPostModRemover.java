package wfg.ltv_econ.ui.scripts;

import static wfg.native_ui.util.UIConstants.opad;

import java.util.List;

import com.fs.starfarer.api.EveryFrameScript;
import com.fs.starfarer.api.ui.LabelAPI;
import com.fs.starfarer.api.ui.PositionAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.ui.UIComponentAPI;
import com.fs.starfarer.api.ui.UIPanelAPI;
import com.fs.starfarer.ui.impl.StandardTooltipV2Expandable;

import rolflectionlib.util.RolfLectionUtil;
import wfg.native_ui.ui.panels.CustomPanel;

public class TpPostModRemover implements EveryFrameScript {
    private boolean isDone = false;
    private final TooltipMakerAPI tp;
    private final UIComponentAPI lastComp;
    
    private final int prevHeightSoFar;
    private final int prevY;

    public TpPostModRemover(final TooltipMakerAPI tooltip) {
        tp = tooltip;
        lastComp = tooltip.getPrev();
        prevHeightSoFar = (int) tooltip.getHeightSoFar() + opad;

        final PositionAPI pos = tooltip.getPosition();
        prevY = (int) (pos.getY() + pos.getHeight());
    }

    @SuppressWarnings("unchecked")
    public void advance(float var1) {
        if (lastComp == null) { isDone = true; return;}

        if(tp.getPrev() instanceof LabelAPI label){
        if(!label.getText().contains("Per unit prices assume")) return;

        final var panel = (UIPanelAPI) RolfLectionUtil.getMethodAndInvokeDirectly(
            "getPanel", tp
        );
        final var children = (List<UIComponentAPI>) RolfLectionUtil.invokeMethodDirectly(
            CustomPanel.getChildrenNonCopyMethod, panel
        );


        final int index = children.size() - 1;
        for (int i = index; i >= 0; i--) {
            if (lastComp == children.get(i)) break;
            panel.removeComponent(children.get(i));
        };

        final PositionAPI pos = tp.getPosition();
        final int prevX = (int) pos.getX();

        tp.setHeightSoFar(prevHeightSoFar);
        StandardTooltipV2Expandable.updateSizeAsUIElement((StandardTooltipV2Expandable) tp);
        pos.inBL(0f, 0f);

        final float currX = pos.getX();
        final float currY = pos.getY() + pos.getHeight();

        pos.inBL(prevX - currX, Math.max(prevY - currY, 30));
        
        isDone = true;
        }
    }

    public boolean isDone() { return isDone;}
    public boolean runWhilePaused() { return true;}
}
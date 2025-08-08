package wfg_ltv_econ.ui.plugins;

import com.fs.starfarer.api.ui.PositionAPI;

import wfg_ltv_econ.ui.panels.LtvCustomPanel;
import wfg_ltv_econ.ui.panels.LtvSpritePanel;
import wfg_ltv_econ.util.RenderUtils;

public class LtvSpritePanelPlugin<
    PanelType extends LtvCustomPanel<?, LtvSpritePanel>> 
    extends LtvCustomPanelPlugin<LtvSpritePanel, LtvSpritePanelPlugin<PanelType>
> {

    private boolean isDrawFilledQuad = false;

    public void init() {
        if (getPanel().fillColor != null) {
            isDrawFilledQuad = true;
        }
    }

    public void setDrawFilledQuad(boolean a) {
        isDrawFilledQuad = a;
    }

    @Override
    public void render(float alphaMult) {
        super.render(alphaMult);
        if (getPanel().m_sprite == null) {
            return;
        }

        if (getPanel().color != null) {
            getPanel().setColor(getPanel().color);
        }

        final PositionAPI pos = getPanel().getPos();
        final float x = pos.getX();
        final float y = pos.getY();
        final float w = pos.getWidth();
        final float h = pos.getHeight();

        getPanel().m_sprite.setSize(w, h);
        getPanel().m_sprite.render(x, y);

        if (isDrawFilledQuad && getPanel().fillColor != null) {
            getPanel().m_sprite.setColor(getPanel().fillColor);
            RenderUtils.drawQuad(x, y, w, h, getPanel().fillColor, alphaMult, false);
        }
    }
}
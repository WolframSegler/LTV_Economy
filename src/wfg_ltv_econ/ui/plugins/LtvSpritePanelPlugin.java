package wfg_ltv_econ.ui.plugins;

import com.fs.starfarer.api.ui.PositionAPI;

import wfg_ltv_econ.ui.panels.LtvSpritePanel;
import wfg_ltv_econ.util.RenderUtils;

public class LtvSpritePanelPlugin<
    PanelType extends LtvSpritePanel<PanelType>
> extends LtvCustomPanelPlugin<PanelType, LtvSpritePanelPlugin<PanelType>> {

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
    public void renderBelow(float alphaMult) {
        super.renderBelow(alphaMult);
        if (getPanel().m_sprite == null) {
            return;
        }

        if (getPanel().color != null) {
            getPanel().m_sprite.setColor(getPanel().color);
        }

        final PositionAPI pos = getPanel().getPos();
        final float x = pos.getX();
        final float y = pos.getY();
        final float w = pos.getWidth();
        final float h = pos.getHeight();

        if (isDrawFilledQuad && getPanel().fillColor != null) {
            getPanel().m_sprite.setColor(getPanel().fillColor);
            RenderUtils.drawQuad(x, y, w, h, getPanel().fillColor, alphaMult, false);
        }

        if (getPanel().drawTexOutline && getPanel().texOutlineColor != null) {
            RenderUtils.drawSpriteOutline(
                getPanel().m_sprite, x, y, w, h, getPanel().texOutlineColor, alphaMult,1
            );
        }

        getPanel().m_sprite.setSize(w, h);
        getPanel().m_sprite.render(x, y);
    }
}
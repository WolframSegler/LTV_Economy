package wfg.wrap_ui.ui.plugins;

import com.fs.starfarer.api.ui.PositionAPI;

import wfg.wrap_ui.ui.panels.SpritePanel;
import wfg.wrap_ui.util.RenderUtils;

public class SpritePanelPlugin<
    PanelType extends SpritePanel<PanelType>
> extends CustomPanelPlugin<PanelType, SpritePanelPlugin<PanelType>> {

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
                getPanel().m_sprite, getPanel().texOutlineColor, x, y, w, h, alphaMult, 2
            );
        }

        getPanel().m_sprite.setSize(w, h);
        getPanel().m_sprite.render(x, y);
    }
}
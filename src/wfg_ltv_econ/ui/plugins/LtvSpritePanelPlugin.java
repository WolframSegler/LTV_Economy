package wfg_ltv_econ.ui.plugins;

import java.util.List;

import org.lwjgl.input.Keyboard;

import com.fs.starfarer.api.campaign.econ.CommodityOnMarketAPI;
import com.fs.starfarer.api.impl.codex.CodexDataV2;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.ui.PositionAPI;

import wfg_ltv_econ.ui.panels.LtvIconPanel;
import wfg_ltv_econ.ui.panels.LtvSpritePanel;
import wfg_ltv_econ.ui.plugins.LtvCustomPanelPlugin.Glow;
import wfg_ltv_econ.util.RenderUtils;
import wfg_ltv_econ.util.UiUtils;

public class LtvSpritePanelPlugin extends LtvCustomPanelPlugin {

    @Override
    public LtvSpritePanel getPanel() {
        return (LtvSpritePanel) m_panel;
    }
    
    private final float additiveBrightness = 0.6f;

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

        if (glowType == Glow.ADDITIVE && m_fader.getBrightness() > 0) {
            float glowAmount = additiveBrightness * m_fader.getBrightness() * alphaMult;

            RenderUtils.drawAdditiveGlow(getPanel().m_sprite, x, y, m_panel.getFaction().getBaseUIColor(),
                    glowAmount);
        }
    }

    @Override
    public void positionChanged(PositionAPI position) {
    }

    @Override
    public void advance(float amount) {
        super.advance(amount);
    }

    @Override
    public void processInput(List<InputEventAPI> events) {
        super.processInput(events);

        if (m_panel instanceof LtvIconPanel) {
            if (!m_hasTooltip || !hoveredLastFrame) {
                ((LtvIconPanel) m_panel).isExpanded = false;

                return;
            }

            for (InputEventAPI event : events) {
                if (event.isMouseEvent()) {
                    continue;
                }

                if (event.isKeyDownEvent() && event.getEventValue() == Keyboard.KEY_F1) {
                    ((LtvIconPanel) m_panel).isExpanded = !((LtvIconPanel) m_panel).isExpanded;
                    hideTooltip();

                    event.consume();

                    continue;
                }

                if (event.isKeyDownEvent() && event.getEventValue() == Keyboard.KEY_F2) {
                    CommodityOnMarketAPI com = ((LtvIconPanel) m_panel).m_com;
                    String codexID = CodexDataV2.getCommodityEntryId(com.getId());
                    UiUtils.openCodexPage(codexID);
                    hideTooltip();

                    event.consume();

                    continue;
                }
            }
        }

    }

    @Override
    public void buttonPressed(Object buttonId) {
    }
}
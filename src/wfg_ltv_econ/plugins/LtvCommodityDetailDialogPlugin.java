package wfg_ltv_econ.plugins;

import java.util.List;

import com.fs.starfarer.api.campaign.CustomUIPanelPlugin;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.ui.CustomPanelAPI;
import com.fs.starfarer.api.ui.PositionAPI;
import com.fs.starfarer.api.util.FaderUtil;
import com.fs.starfarer.api.util.FaderUtil.State;

import wfg_ltv_econ.ui.LtvCustomPanel;
import wfg_ltv_econ.ui.LtvUIState;
import wfg_ltv_econ.ui.LtvUIState.UIStateType;
import wfg_ltv_econ.ui.com_detail_dialog.LtvCommodityDetailDialog;
import wfg_ltv_econ.util.RenderUtils;

public class LtvCommodityDetailDialogPlugin implements CustomUIPanelPlugin {
    protected CustomPanelAPI m_panel;
    protected LtvCustomPanel m_parent;
    protected FaderUtil m_fader;
    protected LtvCommodityDetailDialog m_dialog;

    final protected float highlightBrightness = 1.2f;
    protected boolean glowEnabled = false;
    protected boolean hoveredLastFrame = false;
    protected boolean clickedThisFrame = false;
    protected boolean hasBackground = false;
    protected boolean hasOutline = false;

    protected float hoverTime = 0f;
    protected int offsetX = 0;
    protected int offsetY = 0;
    protected int offsetW = 0;
    protected int offsetH = 0;

    public LtvCommodityDetailDialogPlugin(LtvCustomPanel parent, LtvCommodityDetailDialog dialog) {
        m_parent = parent;
        m_dialog = dialog;
    }

    public void init(boolean glowEnabled, boolean hasBackground, boolean hasOutline, CustomPanelAPI panel) {
        m_panel = panel;
        this.glowEnabled = glowEnabled;
        this.hasBackground = hasBackground;
        this.hasOutline = hasOutline;

        if (glowEnabled) {
            m_fader = new FaderUtil(0, 0, 0.2f, true, true);
        }
    }

    public boolean getGlowEnabled() {
        return glowEnabled;
    }

    public FaderUtil getFader() {
        return m_fader;
    }

    public void setGlowEnabled(boolean a) {
        glowEnabled = a;
    }

    public void positionChanged(PositionAPI position) {

    }

    public void setOffsets(int x, int y, int width, int height) {
        offsetX = x;
        offsetY = y;
        offsetW = width;
        offsetH = height;
    }

    public void renderBelow(float alphaMult) {
        PositionAPI pos = m_panel.getPosition();

        if (hasBackground) {
            int x = (int)pos.getX() + offsetX;
            int y = (int)pos.getY() + offsetY;
            int w = (int)pos.getWidth() + offsetW;
            int h = (int)pos.getHeight() + offsetH;
            RenderUtils.drawQuad(x, y, w, h, m_parent.BgColor, alphaMult*0.65f);
            // Looks vanilla like with 0.65f
        }
        if (hasOutline) {
            RenderUtils.drawOutline(pos.getX(), pos.getY(), pos.getWidth(), pos.getHeight(), m_parent.getFaction().getGridUIColor(), alphaMult);
        }

        if (glowEnabled && m_fader.getBrightness() > 0) {
            float glowAmount = highlightBrightness * m_fader.getBrightness() * alphaMult;

            RenderUtils.drawGlowOverlay(pos.getX(), pos.getY(), pos.getWidth(), pos.getHeight(),
            m_parent.getFaction().getBaseUIColor(), glowAmount);

            if (clickedThisFrame) {
                RenderUtils.drawGlowOverlay(pos.getX(), pos.getY(), pos.getWidth(), pos.getHeight(),
                m_parent.getFaction().getBaseUIColor(), glowAmount / 2);
            }
        }
    }

    public void render(float alphaMult) {

    }

    public void advance(float amount) {
        if (glowEnabled) {
            State target = hoveredLastFrame ? State.IN : State.OUT;
            
            if (!LtvUIState.is(UIStateType.NONE)) {
                target = State.OUT;
            }

            m_fader.setState(target);
            m_fader.advance(amount);
        }
    }

    public void processInput(List<InputEventAPI> events) {
        for (InputEventAPI event : events) {
            if (event.isMouseMoveEvent()) {
                float mouseX = event.getX();
                float mouseY = event.getY();

                PositionAPI pos = m_panel.getPosition();
                float x = pos.getX();
                float y = pos.getY();
                float w = pos.getWidth();
                float h = pos.getHeight();

                // Check for mouse over panel
                hoveredLastFrame = mouseX >= x && mouseX <= x + w && mouseY >= y && mouseY <= y + h;

                if (hoveredLastFrame && event.isLMBDownEvent()) {
                    clickedThisFrame = true;
                }

                if (event.isLMBUpEvent()) {
                    clickedThisFrame = false;
                }
                continue;
            }            
        }
    }

    public void buttonPressed(Object buttonId) {

    }
}

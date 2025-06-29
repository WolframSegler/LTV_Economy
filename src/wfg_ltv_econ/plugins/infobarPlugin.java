package wfg_ltv_econ.plugins;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.awt.Color;

import com.fs.starfarer.api.campaign.CustomUIPanelPlugin;
import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.ui.CustomPanelAPI;
import com.fs.starfarer.api.ui.PositionAPI;

import wfg_ltv_econ.util.RenderUtils;


public class infobarPlugin implements CustomUIPanelPlugin {

    private CustomPanelAPI m_panel;
    private HashMap<Color, Float> m_barMap;
    private FactionAPI m_faction;
    private boolean hasOutline = false;

    public void init(CustomPanelAPI panel, boolean hasOutline, HashMap<Color, Float> barMap, FactionAPI faction) {
        m_panel = panel;
        m_barMap = barMap;
        m_faction = faction;
        this.hasOutline = hasOutline;
    }
   
	public void positionChanged(PositionAPI position) {}
	
	public void renderBelow(float alphaMult) {}

	public void render(float alphaMult) {
        PositionAPI pos = m_panel.getPosition();
        float x = pos.getX();
        float y = pos.getY();
        float w = pos.getWidth();
        float h = pos.getHeight();

        for (Map.Entry<Color, Float> mapEntry : m_barMap.entrySet()) {
            RenderUtils.drawQuad(x, y, w*mapEntry.getValue(), h, mapEntry.getKey(), alphaMult*0.65f);
            x += w*mapEntry.getValue();
        }

        if (hasOutline) {
            RenderUtils.drawOutline(pos.getX(), pos.getY(), pos.getWidth(), pos.getHeight(), m_faction.getGridUIColor(), alphaMult);
        }
    }
	
	public void advance(float amount) {}

	public void processInput(List<InputEventAPI> events) {}
	
	public void buttonPressed(Object buttonId) {}
}

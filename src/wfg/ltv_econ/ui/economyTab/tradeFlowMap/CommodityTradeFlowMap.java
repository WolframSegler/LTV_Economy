package wfg.ltv_econ.ui.economyTab.tradeFlowMap;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.awt.Color;

import org.lwjgl.opengl.GL11;
import org.lwjgl.util.vector.Vector2f;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.SettingsAPI;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.campaign.econ.CommoditySpecAPI;
import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.ui.UIPanelAPI;

import wfg.ltv_econ.economy.commodity.CommodityTradeFlow;
import wfg.ltv_econ.economy.engine.EconomyEngine;
import wfg.ltv_econ.ui.economyTab.CommoditySelectionPanel;
import wfg.ltv_econ.util.ArrayMap;
import wfg.native_ui.ui.components.BackgroundComp;
import wfg.native_ui.ui.components.NativeComponents;
import wfg.native_ui.ui.core.UIBuildableAPI;
import wfg.native_ui.ui.core.UIElementFlags.HasBackground;
import wfg.native_ui.ui.core.UIElementFlags.HasOutline;
import wfg.native_ui.ui.panels.CustomPanel;
import wfg.native_ui.util.NativeUiUtils;

public class CommodityTradeFlowMap extends CustomPanel<CommodityTradeFlowMap> implements
    HasOutline, HasBackground, UIBuildableAPI
{
    private static final SettingsAPI settings = Global.getSettings();
    private static final SpriteAPI NODE_SPRITE = settings.getSprite("planets", "default_sun_halo");
    private static final float BOUNDS_PAD = 100f;
    private static final float ZOOM_MAX = 5f;
    private static final float ZOOM_MIN = 0.25f;
    private static final float ZOOM_SENSITIVITY = 0.002f;

    private final BackgroundComp bg = comp().get(NativeComponents.BACKGROUND);

    private final Map<StarSystemAPI, List<CommodityTradeFlow>> systems = new ArrayMap<>();
    private List<CommodityTradeFlow> data;

    private Vector2f lastMouse = null;
    private float panOffsetX = 0f;
    private float panOffsetY = 0f;
    private float zoom = 1f;

    private float sectorMinXCoord = 0f;
    private float sectorMaxXCoord = 0f;
    private float sectorMinYCoord = 0f;
    private float sectorMaxYCoord = 0f;

    public CommodityTradeFlowMap(UIPanelAPI parent, int width, int height) {
        super(parent, width, height);

        bg.alpha = 1f;

        buildUI();
    }

    @Override
    public void buildUI() {
        panOffsetX = 0f;
        panOffsetY = 0f;
        zoom = 1f;

        final CommoditySpecAPI com = CommoditySelectionPanel.selectedCom;
        data = EconomyEngine.getInstance().getComDomain(com.getId()).getTradeFlows();
        
        systems.clear();
        for (CommodityTradeFlow flow : data) {
            final List<CommodityTradeFlow> expList = systems.computeIfAbsent(
                flow.exporter.getStarSystem(), k -> new ArrayList<>()
            );
            expList.add(flow);
            final List<CommodityTradeFlow> impList = systems.computeIfAbsent(
                flow.importer.getStarSystem(), k -> new ArrayList<>()
            );
            impList.add(flow);
        }

        { // Calculate sector bounds
            sectorMinXCoord = Float.POSITIVE_INFINITY;
            sectorMaxXCoord = Float.NEGATIVE_INFINITY;
            sectorMinYCoord = Float.POSITIVE_INFINITY;
            sectorMaxYCoord = Float.NEGATIVE_INFINITY;
    
            for (StarSystemAPI system : systems.keySet()) {
                Vector2f loc = system.getLocation();
                if (loc.x < sectorMinXCoord) sectorMinXCoord = loc.x;
                if (loc.x > sectorMaxXCoord) sectorMaxXCoord = loc.x;
                if (loc.y < sectorMinYCoord) sectorMinYCoord = loc.y;
                if (loc.y > sectorMaxYCoord) sectorMaxYCoord = loc.y;
            }
        }
    }

    @Override
    public void renderBelow(float alpha) {
        super.renderBelow(alpha);

        GL11.glEnable(GL11.GL_SCISSOR_TEST);
        GL11.glScissor((int) pos.getX(), (int) pos.getY(), (int) pos.getWidth(), (int) pos.getHeight());
    }

    @Override
    public void render(float alpha) {
        super.render(alpha);

        renderNodes(alpha);

        GL11.glDisable(GL11.GL_SCISSOR_TEST);
    }

    @Override
    public void processInput(List<InputEventAPI> events) {
        for (InputEventAPI event : events) {
            if (event.isMouseScrollEvent()) {
                // Zoom
                final float scrollAmount = event.getEventValue();
                final float oldZoom = zoom;
                final float zoomFactor = 1f + scrollAmount * ZOOM_SENSITIVITY;
                zoom *= zoomFactor;
                zoom = Math.max(ZOOM_MIN, Math.min(zoom, ZOOM_MAX));

                final float r = zoom / oldZoom;
                final float localMouseX = event.getX() - pos.getX();
                final float localMouseY = event.getY() - pos.getY();

                final float centerX = pos.getWidth() * 0.5f;
                final float centerY = pos.getHeight() * 0.5f;

                panOffsetX = (1f - r) * (localMouseX - centerX) + r * panOffsetX;
                panOffsetY = (1f - r) * (localMouseY - centerY) + r * panOffsetY;

            } else if (event.isMouseEvent()) {
                if (event.isRMBDownEvent()) {
                    lastMouse = new Vector2f(event.getX(), event.getY());

                } else if (event.isRMBUpEvent()) {
                    lastMouse = null;

                } else if (event.isMouseMoveEvent() && lastMouse != null) {
                    final float dx = event.getX() - lastMouse.x;
                    final float dy = event.getY() - lastMouse.y;

                    panOffsetX += dx;
                    panOffsetY += dy;
                    lastMouse.set(event.getX(), event.getY());
                }
            }
        }
    }

    private final void renderNodes(float alpha) {
        final float nodeRadius = 3f;

        for (Map.Entry<StarSystemAPI, List<CommodityTradeFlow>> entry : systems.entrySet()) {
            final StarSystemAPI system = entry.getKey();
            final List<CommodityTradeFlow> flows = entry.getValue();
            final Vector2f sysPos = project(system.getLocation());

            if (sysPos.x < -nodeRadius || sysPos.x > pos.getWidth() + nodeRadius ||
                sysPos.y < -nodeRadius || sysPos.y > pos.getHeight() + nodeRadius
            ) { continue; }

            float totalAmount = 0f;
            for (CommodityTradeFlow flow : flows) {
                totalAmount += flow.amount;
            }

            Color mixed = flows.get(0).exporter.getFaction().getBaseUIColor();
            float accumulated = flows.get(0).amount / totalAmount;

            for (int i = 1; i < flows.size(); i++) {
                final CommodityTradeFlow flow = flows.get(i);
                final float weight = flow.amount / totalAmount;
                final float t = weight / (1f - accumulated); 
                mixed = NativeUiUtils.lerpColor(mixed, flow.exporter.getFaction().getBaseUIColor(), t);
                accumulated += weight;
            }

            NODE_SPRITE.setSize(nodeRadius * 2f, nodeRadius * 2f);
            NODE_SPRITE.setColor(mixed);
            NODE_SPRITE.setAlphaMult(alpha);
            NODE_SPRITE.render(pos.getX() + sysPos.getX(), pos.getY() + sysPos.getY());
        }
    }

    private final Vector2f project(Vector2f starCoord) {
        final float sectorW = BOUNDS_PAD + sectorMaxXCoord - sectorMinXCoord;
        final float sectorH = BOUNDS_PAD + sectorMaxYCoord - sectorMinYCoord;
        final float panelSize = Math.min(pos.getWidth(), pos.getHeight());

        final float scale = panelSize / Math.max(sectorW, sectorH) * zoom;

        final float sectorCenterX = (sectorMinXCoord + sectorMaxXCoord) / 2f;
        final float sectorCenterY = (sectorMinYCoord + sectorMaxYCoord) / 2f;

        final float normX = (starCoord.x - sectorCenterX) * scale;
        final float normY = (starCoord.y - sectorCenterY) * scale;

        final float px = normX + pos.getWidth() / 2f + panOffsetX;
        final float py = normY + pos.getHeight() / 2f + panOffsetY;

        return new Vector2f(px, py);
    }
}
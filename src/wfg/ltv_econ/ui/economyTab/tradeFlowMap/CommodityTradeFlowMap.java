package wfg.ltv_econ.ui.economyTab.tradeFlowMap;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static wfg.native_ui.util.UIConstants.grid;

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
import wfg.native_ui.util.RenderUtils;

public class CommodityTradeFlowMap extends CustomPanel<CommodityTradeFlowMap> implements
    HasOutline, HasBackground, UIBuildableAPI
{
    private static final SettingsAPI settings = Global.getSettings();
    // private static final SpriteAPI NODE_SPRITE = settings.getSprite("planets", "default_sun_halo");
    private static final SpriteAPI NODE_SPRITE = settings.getSprite("map", "star");
    private static final SpriteAPI NODE_OVERLAY = settings.getSprite("backgrounds", "star1");
    private static final SpriteAPI NODE_UNDERLAY = settings.getSprite("map", "star_underlay");
    private static final Color NODE_UNDERLAY_COLOR = new Color(32, 12, 64);
    private static final float BOUNDS_PAD = 100f;
    private static final float ZOOM_MAX = 5f;
    private static final float ZOOM_MIN = 0.25f;
    private static final float ZOOM_SENSITIVITY = 0.001f;

    private static final float BASE_NODE_RADIUS = 20f;
    private static final float NODE_RADIUS_SCALE = 0.8f;

    private static final float BASE_FLOW_PATH_W = 4f;
    private static final float FLOW_PATH_SCALE = 0.5f;
    private static final float PATH_GAP_MULT = 0.6f;

    private final BackgroundComp bg = comp().get(NativeComponents.BACKGROUND);

    private final Set<StarSystemAPI> systems = new HashSet<>();
    private final List<FlowPathData> flowData = new ArrayList<>();

    private Vector2f lastMouse = null;
    private float panOffsetX = 0f;
    private float panOffsetY = 0f;
    private float zoom = 1f;

    private float sectorMinXCoord = 0f;
    private float sectorMaxXCoord = 0f;
    private float sectorMinYCoord = 0f;
    private float sectorMaxYCoord = 0f;

    private float time = 0f;

    public CommodityTradeFlowMap(UIPanelAPI parent, int width, int height) {
        super(parent, width, height);

        bg.alpha = 1f;

        buildUI();
    }

    @Override
    public void buildUI() {
        { // Clear data
            systems.clear();
            flowData.clear();
            panOffsetX = 0f;
            panOffsetY = 0f;
            zoom = 1f;
        }

        { // Create render data
            final CommoditySpecAPI com = CommoditySelectionPanel.selectedCom;
            final var tradeFlows = EconomyEngine.getInstance().getComDomain(com.getId()).getTradeFlows();
            
            final ArrayMap<SystemPair, List<CommodityTradeFlow>> uniqueFlows = new ArrayMap<>();
            for (CommodityTradeFlow flow : tradeFlows) {
                final SystemPair pair = new SystemPair(
                    flow.exporter.getStarSystem(), flow.importer.getStarSystem()
                );
                final List<CommodityTradeFlow> list = uniqueFlows.computeIfAbsent(
                    pair, k -> new ArrayList<>()
                );
                list.add(flow);
            }

            for (List<CommodityTradeFlow> flows : uniqueFlows.values()) {
                final FlowPathData pathData = new FlowPathData();
                final CommodityTradeFlow first = flows.get(0);
                pathData.source = first.exporter.getStarSystem();
                pathData.destination = first.importer.getStarSystem();

                systems.add(pathData.source);
                systems.add(pathData.destination);

                float totalAmount = 0f;
                for (CommodityTradeFlow flow : flows) totalAmount += flow.amount;

                pathData.pathWidth = calculatePathWidth(totalAmount);
                pathData.nodeSize = calculateNodeSize(totalAmount);

                for (CommodityTradeFlow flow : flows) {
                    final float weight = flow.amount / totalAmount;
                    final Color c = flow.exporter.getFaction().getBrightUIColor();
                    final Float prev = pathData.colorWeights.get(c);
                    pathData.colorWeights.put(c, (prev == null ? 0f : prev) + weight);
                }

                flowData.add(pathData);
            }
        }

        { // Calculate sector bounds
            sectorMinXCoord = Float.POSITIVE_INFINITY;
            sectorMaxXCoord = Float.NEGATIVE_INFINITY;
            sectorMinYCoord = Float.POSITIVE_INFINITY;
            sectorMaxYCoord = Float.NEGATIVE_INFINITY;
    
            for (StarSystemAPI system : systems) {
                final Vector2f loc = system.getLocation();
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

        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);

        renderGrid(alpha);
        renderPaths(alpha);
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

    @Override
    public void advance(float amount) {
        time += amount;
    }

    private final void renderGrid(float alpha) {
        final float GRID_WORLD = 6000f;
        final int MAJOR_EVERY = 3;
        final float MINOR_LINE_WIDTH = 1f;
        final float MAJOR_LINE_WIDTH = 1.8f;

        final Color minorColor = grid;
        final Color majorColor = NativeUiUtils.adjustBrightness(grid, 1.3f);

        final float panelX = pos.getX();
        final float panelY = pos.getY();
        final float panelW = pos.getWidth();
        final float panelH = pos.getHeight();

        final Vector2f worldBL = reverseProject(0f, 0f);
        final Vector2f worldTR = reverseProject(panelW, panelH);

        final float visibleMinX = worldBL.x - BOUNDS_PAD;
        final float visibleMaxX = worldTR.x + BOUNDS_PAD;
        final float visibleMinY = worldBL.y - BOUNDS_PAD;
        final float visibleMaxY = worldTR.y + BOUNDS_PAD;

        final int startXI = (int)Math.floor(visibleMinX / GRID_WORLD);
        final int endXI = (int)Math.ceil( visibleMaxX / GRID_WORLD);
        final int startYI = (int)Math.floor(visibleMinY / GRID_WORLD);
        final int endYI = (int)Math.ceil( visibleMaxY / GRID_WORLD);

        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glEnable(GL11.GL_LINE_SMOOTH);
        GL11.glHint(GL11.GL_LINE_SMOOTH_HINT, GL11.GL_NICEST);

        // vertical lines
        for (int xi = startXI; xi <= endXI; xi++) {
            final float worldX = xi * GRID_WORLD;
            Vector2f topLocal = project(new Vector2f(worldX, visibleMaxY));
            Vector2f botLocal = project(new Vector2f(worldX, visibleMinY));

            final float sx0 = panelX + topLocal.x;
            final float sy0 = panelY + topLocal.y;
            final float sx1 = panelX + botLocal.x;
            final float sy1 = panelY + botLocal.y;

            if ((sx0 < panelX - 2f && sx1 < panelX - 2f) || (sx0 > panelX + panelW + 2f && sx1 > panelX + panelW + 2f)) {
                continue;
            }

            final boolean isMajor = Math.floorMod(xi, MAJOR_EVERY) == 0;
            RenderUtils.setGlColor(isMajor ? majorColor : minorColor, alpha);
            GL11.glLineWidth(isMajor ? MAJOR_LINE_WIDTH : MINOR_LINE_WIDTH);

            GL11.glBegin(GL11.GL_LINES);
            GL11.glVertex2f(sx0, sy0);
            GL11.glVertex2f(sx1, sy1);
            GL11.glEnd();
        }

        // horizontal lines
        for (int yi = startYI; yi <= endYI; yi++) {
            final float worldY = yi * GRID_WORLD;
            final Vector2f leftLocal = project(new Vector2f(visibleMinX, worldY));
            final Vector2f rightLocal = project(new Vector2f(visibleMaxX, worldY));

            final float sx0 = panelX + leftLocal.x;
            final float sy0 = panelY + leftLocal.y;
            final float sx1 = panelX + rightLocal.x;
            final float sy1 = panelY + rightLocal.y;

            if ((sy0 < panelY - 2f && sy1 < panelY - 2f) || (sy0 > panelY + panelH + 2f && sy1 > panelY + panelH + 2f)) {
                continue;
            }

            final boolean isMajor = Math.floorMod(yi, MAJOR_EVERY) == 0;
            RenderUtils.setGlColor(isMajor ? majorColor : minorColor, alpha);
            GL11.glLineWidth(isMajor ? MAJOR_LINE_WIDTH : MINOR_LINE_WIDTH);

            GL11.glBegin(GL11.GL_LINES);
            GL11.glVertex2f(sx0, sy0);
            GL11.glVertex2f(sx1, sy1);
            GL11.glEnd();
        }

        GL11.glLineWidth(1f);
        GL11.glDisable(GL11.GL_LINE_SMOOTH);
        GL11.glEnable(GL11.GL_TEXTURE_2D);
    }

    private final void renderNodes(float alpha) {
        final float glow = 0.5f + 0.5f * (float)Math.sin(time * 4f);
        final float overlaySizeMult = 1f + (float)Math.sin(time * 4f) * 0.4f;

        GL11.glEnable(GL11.GL_TEXTURE_2D);

        // Destinations
        for (FlowPathData data : flowData) {
            final Vector2f sysPos = project(data.destination.getLocation());

            drawNodeSprite(alpha, data, sysPos, Color.WHITE, glow, overlaySizeMult);
        }

        // Sources
        for (FlowPathData data : flowData) {
            final Vector2f sysPos = project(data.source.getLocation());
            Color mixed = data.colorWeights.keyAt(0);
            float accumulated = data.colorWeights.valueAt(0);

            for (int i = 1; i < data.colorWeights.size(); i++) {
                final Color key = data.colorWeights.keyAt(i);
                final float value = data.colorWeights.valueAt(i);
                final float t = value / (1f - accumulated); 
                mixed = NativeUiUtils.lerpColor(mixed, key, t);
                accumulated += value;
            }

            drawNodeSprite(alpha, data, sysPos, mixed, glow, overlaySizeMult);
        }
    }

    private final void renderPaths(float alpha) {
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glEnable(GL11.GL_LINE_SMOOTH);
        GL11.glEnable(GL11.GL_POLYGON_SMOOTH);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);

        for (FlowPathData data : flowData) {
            final Vector2f aLocal = project(data.source.getLocation());
            final Vector2f bLocal = project(data.destination.getLocation());

            // convert to SCREEN coords by adding panel origin
            final float srcX = pos.getX() + aLocal.x;
            final float srcY = pos.getY() + aLocal.y;
            final float destX = pos.getX() + bLocal.x;
            final float destY = pos.getY() + bLocal.y;

            final float dx = destX - srcX;
            final float dy = destY - srcY;
            final float len = (float)Math.sqrt(dx*dx + dy*dy);
            if (len == 0f) continue;

            final float gap = data.nodeSize * PATH_GAP_MULT;

            // move endpoints toward each other
            final float ax = srcX + dx / len * gap;
            final float ay = srcY + dy / len * gap;
            final float bx = destX - dx / len * gap;
            final float by = destY - dy / len * gap;

            Color mixed = data.colorWeights.keyAt(0);
            float accumulated = data.colorWeights.valueAt(0);
            for (int c = 1; c < data.colorWeights.size(); c++) {
                final Color key = data.colorWeights.keyAt(c);
                final float value = data.colorWeights.valueAt(c);
                final float tt = value / (1f - accumulated);
                mixed = NativeUiUtils.lerpColor(mixed, key, tt);
                accumulated += value;
            }

            // outer halo
            RenderUtils.drawGradientSprite(ax, ay, bx, by, data.pathWidth * 1.8f, mixed, true,
                0.25f * alpha, 0.25f * alpha, 0.25f * alpha);

            // inner core (bright, constant)
            RenderUtils.drawGradientSprite(ax, ay, bx, by, data.pathWidth * 0.7f, mixed, true,
                alpha, alpha, alpha);

            // thin white specular (center)
            RenderUtils.drawGradientSprite(ax, ay, bx, by, Math.max(1f, data.pathWidth * 0.13f),
                Color.WHITE, true, 0.9f * alpha, 0.9f * alpha, 0.9f * alpha);
        }

        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glDisable(GL11.GL_LINE_SMOOTH);
        GL11.glDisable(GL11.GL_POLYGON_SMOOTH);
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

    private final Vector2f reverseProject(float panelLocalX, float panelLocalY) {
        final float sectorW = BOUNDS_PAD + sectorMaxXCoord - sectorMinXCoord;
        final float sectorH = BOUNDS_PAD + sectorMaxYCoord - sectorMinYCoord;
        final float panelSize = Math.min(pos.getWidth(), pos.getHeight());

        final float scale = panelSize / Math.max(sectorW, sectorH) * zoom;

        final float sectorCenterX = (sectorMinXCoord + sectorMaxXCoord) / 2f;
        final float sectorCenterY = (sectorMinYCoord + sectorMaxYCoord) / 2f;

        final float normX = panelLocalX - pos.getWidth() / 2f - panOffsetX;
        final float normY = panelLocalY - pos.getHeight() / 2f - panOffsetY;

        final float worldOffsetX = normX / scale;
        final float worldOffsetY = normY / scale;

        final float worldX = worldOffsetX + sectorCenterX;
        final float worldY = worldOffsetY + sectorCenterY;

        return new Vector2f(worldX, worldY);
    }

    private final void drawNodeSprite(float alpha, FlowPathData data, Vector2f sysPos, Color color,
        float glow, float overlaySizeMult
    ) {
        final float nodeSize = data.nodeSize;

        if (sysPos.x < -nodeSize * 2f || sysPos.x > pos.getWidth() + nodeSize * 2f ||
            sysPos.y < -nodeSize * 2f || sysPos.y > pos.getHeight() + nodeSize * 2f
        ) { return; }

        NODE_UNDERLAY.setSize(nodeSize * 1.2f, nodeSize * 1.2f);
        NODE_UNDERLAY.setColor(NODE_UNDERLAY_COLOR);
        NODE_UNDERLAY.setAlphaMult(alpha * 0.7f);
        NODE_UNDERLAY.renderAtCenter(pos.getX() + sysPos.x, pos.getY() + sysPos.y);

        NODE_SPRITE.setSize(nodeSize, nodeSize);
        NODE_SPRITE.setColor(color);
        NODE_SPRITE.setAlphaMult(alpha * 0.5f);
        NODE_SPRITE.renderAtCenter(pos.getX() + sysPos.x, pos.getY() + sysPos.y);

        final float overlaySize = overlaySizeMult * nodeSize;
        NODE_OVERLAY.setSize(overlaySize, overlaySize);
        NODE_OVERLAY.setColor(color);
        NODE_OVERLAY.setAlphaMult(alpha * 0.5f * glow);
        NODE_OVERLAY.renderAtCenter(pos.getX() + sysPos.x, pos.getY() + sysPos.y);
    }

    private final float calculatePathWidth(float amount) {
        return (BASE_FLOW_PATH_W + (float) Math.log10(amount) * FLOW_PATH_SCALE) * visualZoom();
    }

    private final float calculateNodeSize(float amount) {
        return (BASE_NODE_RADIUS + (float) Math.log10(amount) * NODE_RADIUS_SCALE) * visualZoom();
    }

    private final float visualZoom() {
        return (float) Math.pow(zoom, 0.6f);
    }

    private static record SystemPair(StarSystemAPI source, StarSystemAPI dest) {}
}
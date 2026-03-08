package wfg.ltv_econ.ui.economyTab.tradeFlowMap;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import static wfg.native_ui.util.UIConstants.*;

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
import wfg.ltv_econ.util.Arithmetic;
import wfg.native_ui.util.ArrayMap;
import wfg.native_ui.ui.core.UIBuildableAPI;
import wfg.native_ui.ui.core.UIElementFlags.HasBackground;
import wfg.native_ui.ui.core.UIElementFlags.HasOutline;
import wfg.native_ui.ui.panels.CustomPanel;
import wfg.native_ui.util.NativeUiUtils;
import wfg.native_ui.util.RenderUtils;

public class ComTradeFlowMap extends CustomPanel<ComTradeFlowMap> implements
    HasOutline, HasBackground, UIBuildableAPI
{
    private static final SettingsAPI settings = Global.getSettings();
    private static final Random random = new Random();

    private static final SpriteAPI NODE_SPRITE = settings.getSprite("map", "star");
    private static final SpriteAPI NODE_OVERLAY = settings.getSprite("backgrounds", "star1");
    private static final SpriteAPI NODE_UNDERLAY = settings.getSprite("map", "star_underlay");
    private static final SpriteAPI SHIP_ARROW = settings.getSprite("map", "ship_arrow");
    private static final SpriteAPI BG_1 = settings.getSprite("map", "bg1");
    private static final SpriteAPI BG_2 = settings.getSprite("map", "bg2");
    private static final Color NODE_UNDERLAY_COLOR = new Color(38, 12, 64);

    private static final float BOUNDS_PAD = 100f;
    private static final float ZOOM_MAX = 4f;
    private static final float ZOOM_MIN = 0.25f;
    private static final float ZOOM_SENSITIVITY = 0.001f;

    private static final float BASE_NODE_RADIUS = 28f;
    private static final float NODE_RADIUS_SCALE = 0.8f;

    private static final float BASE_FLOW_PATH_W = 4f;
    private static final float FLOW_PATH_SCALE = 0.6f;
    private static final float PATH_GAP_MULT = 0.4f;

    private static final float COM_ICON_SIZE = 48f;

    private static final Set<String> exporterFactionBlacklist = new HashSet<>(12);
    private static final Set<String> importerFactionBlacklist = new HashSet<>(12);
    private static int directionMode = 0;
    private static float minTradeAmount = 0f;

    private final SpriteAPI bgImg = switch (random.nextInt(2)) {
        default -> BG_1;
        case 1 -> BG_2;
    };
    private SpriteAPI comSprite;

    private final Set<StarSystemAPI> systems = new HashSet<>(24);
    private final List<SystemData> systemData = new ArrayList<>(24);
    private final List<PathData> flowData = new ArrayList<>(32);

    private Vector2f lastMouse = null;
    private float panOffsetX = 0f;
    private float panOffsetY = 0f;
    private float zoom = 1f;

    private float sectorMinXCoord = 0f;
    private float sectorMaxXCoord = 0f;
    private float sectorMinYCoord = 0f;
    private float sectorMaxYCoord = 0f;

    private float time = 0f;

    public ComTradeFlowMap(UIPanelAPI parent, int width, int height) {
        super(parent, width, height);

        buildUI();
    }

    @Override
    public void buildUI() {
        { // Clear data
            systems.clear();
            systemData.clear();
            flowData.clear();
            panOffsetX = 0f;
            panOffsetY = 0f;
            zoom = 1f;
        }

        final CommoditySpecAPI com = CommoditySelectionPanel.selectedCom;

        { // Create render data
            final List<CommodityTradeFlow> tradeFlows = new ArrayList<>(
                EconomyEngine.getInstance().getComDomain(com.getId()).getTradeFlows()
            );
            tradeFlows.removeIf(t -> t.exporter.isHidden() ||
                t.importer.isHidden() ||
                exporterFactionBlacklist.contains(t.exporter.getFactionId()) ||
                importerFactionBlacklist.contains(t.importer.getFactionId())
            );

            for (CommodityTradeFlow flow : tradeFlows) {
                systems.add(flow.exporter.getStarSystem());
                systems.add(flow.importer.getStarSystem());
            }

            final List<StarSystemAPI> systemsToRemove = new ArrayList<>(systems.size());
            for (StarSystemAPI system : systems) {
                final SystemData data = new SystemData();
                data.system = system;

                float totalAmount = 0f;
                final List<CommodityTradeFlow> relevantFlows = new ArrayList<>(4);
                for (CommodityTradeFlow flow : tradeFlows) {
                    if (flow.exporter.getStarSystem().equals(system)) {
                        data.isSource = true;
                        totalAmount += flow.amount;
                        relevantFlows.add(flow);
                    }
                }

                if (totalAmount < minTradeAmount ||
                    directionMode == 1 && !data.isSource ||
                    directionMode == 2 && data.isSource && totalAmount <= 0f
                ) {
                    systemsToRemove.add(system);
                    continue;
                }

                data.nodeSize = calculateNodeSize(totalAmount);

                for (CommodityTradeFlow flow : relevantFlows) {
                    final float weight = flow.amount / totalAmount;
                    final Color c = flow.exporter.getFaction().getBrightUIColor();
                    final Float prev = data.colorWeights.get(c);
                    data.colorWeights.put(c, (prev == null ? 0f : prev) + weight);
                }

                systemData.add(data);
            }
            systems.removeAll(systemsToRemove);
            
            final ArrayMap<SystemPair, List<CommodityTradeFlow>> uniqueFlows = new ArrayMap<>();
            for (CommodityTradeFlow flow : tradeFlows) {
                if (flow.exporter.getStarSystem().equals(flow.importer.getStarSystem())) continue;

                final SystemPair pair = new SystemPair(
                    flow.exporter.getStarSystem(), flow.importer.getStarSystem()
                );
                final List<CommodityTradeFlow> list = uniqueFlows.computeIfAbsent(
                    pair, k -> new ArrayList<>()
                );
                list.add(flow);
            }

            for (List<CommodityTradeFlow> flows : uniqueFlows.values()) {
                final CommodityTradeFlow first = flows.get(0);
                final PathData pathData = new PathData();
                final StarSystemAPI source = first.exporter.getStarSystem();
                final StarSystemAPI dest = first.importer.getStarSystem();

                if (!systems.contains(source) || !systems.contains(dest)) continue;
                
                float totalAmount = 0f;
                for (CommodityTradeFlow flow : flows) {
                    totalAmount += flow.amount;
                    final Color c = flow.exporter.getFaction().getBrightUIColor();
                    final Float prev = pathData.colorWeights.get(c);
                    pathData.colorWeights.put(c, (prev == null ? 0f : prev) + flow.amount);
                }

                if (totalAmount < minTradeAmount) continue;

                for (Color c : pathData.colorWeights.keySet()) {
                    pathData.colorWeights.put(c, pathData.colorWeights.get(c) / totalAmount);
                }

                pathData.source = source;
                pathData.destination = dest;
                pathData.pathWidth = calculatePathWidth(totalAmount);
                pathData.nodeSize = calculateNodeSize(totalAmount);

                final float len = Arithmetic.dist(source.getLocation(), dest.getLocation());
                pathData.travelDuration = len * 0.002f;
                pathData.pauseDuration = 10f + pathData.travelDuration;
                pathData.pulseOffset = random.nextFloat() * pathData.travelDuration;
                flowData.add(pathData);
            }
        }

        { // Find commodity data
            comSprite = settings.getSprite(com.getIconName());
            comSprite.setSize(COM_ICON_SIZE, COM_ICON_SIZE);
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

        final float x = pos.getX();
        final float y = pos.getY();
        final float w = pos.getWidth();
        final float h = pos.getHeight();

        GL11.glEnable(GL11.GL_SCISSOR_TEST);
        GL11.glScissor((int) x, (int) y, (int) w, (int) h);

        RenderUtils.drawQuad(x, y, w, h, Color.BLACK, alpha, false);
    }

    @Override
    public void render(float alpha) {
        super.render(alpha);

        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);

        renderBg(alpha);
        renderGrid(alpha);
        renderPaths(alpha);
        renderNodes(alpha);
        renderIcon(alpha);

        GL11.glDisable(GL11.GL_BLEND);
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

    private final void renderBg(float alpha) {
        final float panelW = pos.getWidth();
        final float panelH = pos.getHeight();
        final float imgW = bgImg.getWidth();
        final float imgH = bgImg.getHeight();

        final float scale = Math.max(panelW / imgW, panelH / imgH);

        final float drawW = imgW * scale;
        final float drawH = imgH * scale;

        final float x = (panelW - drawW) / 2f + pos.getX();
        final float y = (panelH - drawH) / 2f + pos.getY();

        bgImg.setSize(drawW, drawH);
        bgImg.setAlphaMult(alpha * 0.2f);
        bgImg.renderAtCenter(x + drawW / 2f, y + drawH / 2f);
    }

    private final void renderGrid(float alpha) {
        final float GRID_WORLD = 6000f;
        final int MAJOR_EVERY = 3;

        final Color minorColor = dark;
        final Color majorColor = NativeUiUtils.adjustBrightness(dark, 1.2f);

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
        GL11.glDisable(GL11.GL_BLEND);
        GL11.glLineWidth(1f);

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

            GL11.glBegin(GL11.GL_LINES);
            GL11.glVertex2f(sx0, sy0);
            GL11.glVertex2f(sx1, sy1);
            GL11.glEnd();
        }
    }

    private final void renderPaths(float alpha) {
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glEnable(GL11.GL_LINE_SMOOTH);
        GL11.glEnable(GL11.GL_POLYGON_SMOOTH);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);
        GL11.glHint(GL11.GL_LINE_SMOOTH_HINT, GL11.GL_NICEST);

        for (PathData data : flowData) {
            final Vector2f aLocal = project(data.source.getLocation());
            final Vector2f bLocal = project(data.destination.getLocation());

            // convert to SCREEN coords by adding panel origin
            final float srcX = pos.getX() + aLocal.x;
            final float srcY = pos.getY() + aLocal.y;
            final float destX = pos.getX() + bLocal.x;
            final float destY = pos.getY() + bLocal.y;

            final float dx = destX - srcX;
            final float dy = destY - srcY;
            final float len = Arithmetic.dist(srcX, srcY, destX, destY);
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
            RenderUtils.drawGradientSprite(ax, ay, bx, by, data.pathWidth * 1.7f, mixed, true,
                0.3f * alpha, 0.2f * alpha, 0.3f * alpha);

            // inner core (bright, constant)
            RenderUtils.drawGradientSprite(ax, ay, bx, by, data.pathWidth * 0.5f, mixed, true,
                alpha, alpha, alpha);

            // thin white specular (center)
            RenderUtils.drawGradientSprite(ax, ay, bx, by, Math.max(1f, data.pathWidth * 0.05f),
                Color.WHITE, true, 0.8f * alpha, 0.5f * alpha, 0.8f * alpha);

            final float totalPeriod = data.travelDuration + data.pauseDuration;
            if (totalPeriod > 0f) {

                final float localT = (time + data.pulseOffset) % totalPeriod;
                if (localT <= data.travelDuration) {
                    final float travelFrac = localT / data.travelDuration;

                    float arrowAlpha = alpha;
                    if (travelFrac < 0.03f) {
                        arrowAlpha *= travelFrac / 0.05f;
                    } else if (travelFrac > 0.97f) {
                        arrowAlpha *= (1f - travelFrac) / 0.05f;
                    }

                    final float cx = ax + (bx - ax) * travelFrac;
                    final float cy = ay + (by - ay) * travelFrac;

                    final float arrowSize = data.pathWidth * 2.5f;
                    SHIP_ARROW.setAngle(NativeUiUtils.rotateSprite(aLocal, bLocal));
                    SHIP_ARROW.setSize(arrowSize, arrowSize);
                    SHIP_ARROW.setAlphaMult(arrowAlpha);
                    SHIP_ARROW.renderAtCenter(cx, cy);
                }
            }
        }

        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glDisable(GL11.GL_LINE_SMOOTH);
        GL11.glDisable(GL11.GL_POLYGON_SMOOTH);
        GL11.glHint(GL11.GL_LINE_SMOOTH_HINT, GL11.GL_DONT_CARE);
    }

    private final void renderNodes(float alpha) {
        final float freq = 0.7f;
        final float glow = 0.5f + 0.5f * (float)Math.sin(time * freq);
        final float overlaySizeMult = 1f + (float)Math.sin(time * freq) * 0.3f;

        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);

        for (SystemData data : systemData) {
            final Vector2f sysPos = project(data.system.getLocation());
            final float nodeSize = data.nodeSize;

            final float x = pos.getX() + sysPos.x;
            final float y = pos.getY() + sysPos.y;

            if (x < pos.getX() - nodeSize * 2f ||
                x > pos.getX() + pos.getWidth() + nodeSize * 2f ||
                y < pos.getY() - nodeSize * 2f ||
                y > pos.getY() + pos.getHeight() + nodeSize * 2f)
            { continue; }

            final Color color;
            if (data.isSource && !data.colorWeights.isEmpty()) {
                Color mixed = data.colorWeights.keyAt(0);
                float accumulated = data.colorWeights.valueAt(0);

                for (int i = 1; i < data.colorWeights.size(); i++) {
                    final Color key = data.colorWeights.keyAt(i);
                    final float value = data.colorWeights.valueAt(i);
                    final float t = value / (1f - accumulated); 
                    mixed = NativeUiUtils.lerpColor(mixed, key, t);
                    accumulated += value;
                }
                color = mixed;
            } else {
                color = Color.WHITE;
            }

            NODE_UNDERLAY.setSize(nodeSize * 1.2f, nodeSize * 1.2f);
            NODE_UNDERLAY.setColor(NODE_UNDERLAY_COLOR);
            NODE_UNDERLAY.setAlphaMult(alpha * 0.5f);
            NODE_UNDERLAY.renderAtCenter(x, y);

            NODE_SPRITE.setSize(nodeSize, nodeSize);
            NODE_SPRITE.setColor(color);
            NODE_SPRITE.setAlphaMult(alpha * 0.5f);
            NODE_SPRITE.renderAtCenter(x, y);

            final float overlaySize = overlaySizeMult * nodeSize;
            NODE_OVERLAY.setSize(overlaySize, overlaySize);
            NODE_OVERLAY.setColor(color);
            NODE_OVERLAY.setAlphaMult(alpha * 0.5f * glow);
            NODE_OVERLAY.renderAtCenter(x, y);
        }
    }

    private final void renderIcon(float alpha) {
        comSprite.render(pos.getX() + opad, pos.getY() + pos.getHeight() - opad - COM_ICON_SIZE);
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

    private final float calculatePathWidth(float amount) {
        return (BASE_FLOW_PATH_W + (float) Math.log10(1f + amount) * FLOW_PATH_SCALE) * visualZoom();
    }

    private final float calculateNodeSize(float amount) {
        return (BASE_NODE_RADIUS + (float) Math.log10(1f + amount) * NODE_RADIUS_SCALE) * visualZoom();
    }

    private final float visualZoom() {
        return (float) Math.pow(zoom, 0.6f);
    }

    private static record SystemPair(StarSystemAPI source, StarSystemAPI dest) {}
}
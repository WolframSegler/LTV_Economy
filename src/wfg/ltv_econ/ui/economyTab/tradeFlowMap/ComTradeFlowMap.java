package wfg.ltv_econ.ui.economyTab.tradeFlowMap;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import static wfg.native_ui.util.UIConstants.*;
import static wfg.native_ui.util.Globals.settings;

import java.awt.Color;

import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;
import org.lwjgl.util.vector.Vector2f;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.campaign.FactionSpecAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.campaign.econ.CommoditySpecAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.ui.UIPanelAPI;

import wfg.ltv_econ.economy.commodity.TradeCom;
import wfg.ltv_econ.economy.engine.EconomyEngine;
import wfg.ltv_econ.economy.fleet.TradeMission;
import wfg.ltv_econ.economy.fleet.TradeMission.MissionStatus;
import wfg.ltv_econ.ui.economyTab.CommoditySelectionPanel;
import wfg.ltv_econ.ui.fleet.TradeFilters;
import wfg.ltv_econ.ui.fleet.TradeMissionWidget;
import wfg.native_ui.util.Arithmetic;
import wfg.native_ui.util.ArrayMap;
import wfg.native_ui.ui.component.NativeComponents;
import wfg.native_ui.ui.component.TooltipComp;
import wfg.native_ui.ui.component.TooltipComp.TooltipBuilder;
import wfg.native_ui.ui.core.UIBuildableAPI;
import wfg.native_ui.ui.core.UIElementFlags.HasOutline;
import wfg.native_ui.ui.core.UIElementFlags.HasTooltip;
import wfg.native_ui.ui.panel.CustomPanel;
import wfg.native_ui.ui.system.NativeSystems;
import wfg.native_ui.ui.system.TooltipSystem;
import wfg.native_ui.util.NativeUiUtils;
import wfg.native_ui.util.NumFormat;
import wfg.native_ui.util.RenderUtils;

public class ComTradeFlowMap extends CustomPanel implements
    HasOutline, UIBuildableAPI, HasTooltip
{
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
    private static final float ZOOM_MIN = 0.1f;
    private static final float ZOOM_SENSITIVITY = 0.001f;

    private static final float BASE_NODE_RADIUS = 28f;
    private static final float NODE_RADIUS_SCALE = 0.8f;

    private static final float BASE_FLOW_PATH_W = 4f;
    private static final float FLOW_PATH_SCALE = 0.6f;
    private static final float PATH_GAP_MULT = 0.4f;

    private static final float COLOR_CHANGE_DUR = 15f;
    private static final float COLOR_TRANSITION_FRAC = 0.15f;
    private static final float MIN_COLOR_DUR = 0.1f;

    private static final float COM_ICON_SIZE = 48f;

    private final TooltipComp toolitp = comp().get(NativeComponents.TOOLTIP);
    private final TooltipSystem tooltipSys = system().get(NativeSystems.TOOLTIP);
    private Object hoveredElement = null;

    private final SpriteAPI bgImg = switch (random.nextInt(2)) {
        default -> BG_1;
        case 1 -> BG_2;
    };
    private SpriteAPI comSprite;
    private BloomEffect bloom;

    private List<TradeMission> missions = new ArrayList<>(0);
    private final Set<StarSystemAPI> systems = new HashSet<>(24);
    private final ArrayMap<StarSystemAPI, SystemData> systemData = new ArrayMap<>(24);
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
    private int hoverRegistered = 0;

    public ComTradeFlowMap(UIPanelAPI parent, int width, int height) {
        super(parent, width, height);

        toolitp.bgAlpha = 0.85f;

        buildUI();
    }

    @Override
    public void buildUI() {
        final CommoditySpecAPI com = CommoditySelectionPanel.selectedCom;
        final String comID = com.getId();

        { // Clear data
            clearChildren();
            missions = getFilteredMissions(comID);
            systems.clear();
            systemData.clear();
            flowData.clear();
            panOffsetX = 0f;
            panOffsetY = 0f;
            zoom = 1f;

            sectorMinXCoord = Float.POSITIVE_INFINITY;
            sectorMaxXCoord = Float.NEGATIVE_INFINITY;
            sectorMinYCoord = Float.POSITIVE_INFINITY;
            sectorMaxYCoord = Float.NEGATIVE_INFINITY;

            if (bloom != null)  bloom.cleanup();
        }

        { // Create render data
            for (TradeMission m : missions) {
                systems.add(m.src.getStarSystem());
                systems.add(m.dest.getStarSystem());
            }

            final List<StarSystemAPI> systemsToRemove = new ArrayList<>(systems.size());
            for (StarSystemAPI system : systems) {
                final SystemData data = new SystemData();
                data.system = system;

                double totalAmount = 0f;
                for (TradeMission m : missions) {
                    double amount = 0.0;
                    for (TradeCom tradeCom : m.cargo) {
                        if (tradeCom.comID.equals(comID)) {
                            amount = tradeCom.amount;
                            break;
                        }
                    }
                    totalAmount += amount;

                    if (m.src.getStarSystem().equals(system)) {
                        data.isSource = true;
                        data.marketAmounts.merge(m.src, amount, Double::sum);

                        data.addColorWeight(m.src.getFaction().getBrightUIColor(), amount);
                    } else if (m.dest.getStarSystem().equals(system)) {
                        data.isDest = true;
                        data.marketAmounts.merge(m.dest, -amount, Double::sum);
                    }
                }

                if (TradeFilters.directionMode == 1 && !data.isSource || TradeFilters.directionMode == 2 && !data.isDest) {
                    systemsToRemove.add(system);
                    continue;
                }

                data.nodeSize = calculateNodeSize(totalAmount);
                systemData.put(system, data);
            }
            systems.removeAll(systemsToRemove);
            
            final ArrayMap<SystemPair, List<TradeMission>> uniqueFlows = new ArrayMap<>(missions.size() / 4);
            for (TradeMission m : missions) {
                if (m.src.getStarSystem().equals(m.dest.getStarSystem())) continue;

                final SystemPair pair = new SystemPair(
                    m.src.getStarSystem(), m.dest.getStarSystem()
                );
                uniqueFlows.computeIfAbsent(pair, k -> new ArrayList<>()).add(m);
            }

            for (List<TradeMission> flows : uniqueFlows.values()) {
                final PathData data = new PathData();
                final TradeMission first = flows.get(0);
                final StarSystemAPI source = first.src.getStarSystem();
                final StarSystemAPI dest = first.dest.getStarSystem();

                if (!systems.contains(source) || !systems.contains(dest)) continue;
                
                double totalAmount = 0.0;
                for (TradeMission m : flows) {
                    final FactionSpecAPI faction = m.src.getFaction().getFactionSpec();
                    double amount = 0.0;
                    for (TradeCom tradeCom : m.cargo) {
                        if (tradeCom.comID.equals(comID)) {
                            amount = tradeCom.amount;
                            break;
                        }
                    }
                    totalAmount += amount;

                    data.factionAmounts.merge(faction, amount, Double::sum);
                    data.addColorWeight(faction.getBrightUIColor(), amount);
                }

                data.source = source;
                data.destination = dest;
                data.pathWidth = calculatePathWidth(totalAmount);
                flowData.add(data);
            }
        }

        { // Find commodity data
            comSprite = settings.getSprite(com.getIconName());
            comSprite.setSize(COM_ICON_SIZE, COM_ICON_SIZE);
        }

        { // Calculate sector bounds
            for (StarSystemAPI system : systems) {
                final Vector2f loc = system.getLocation();
                if (loc.x < sectorMinXCoord) sectorMinXCoord = loc.x;
                if (loc.x > sectorMaxXCoord) sectorMaxXCoord = loc.x;
                if (loc.y < sectorMinYCoord) sectorMinYCoord = loc.y;
                if (loc.y > sectorMaxYCoord) sectorMaxYCoord = loc.y;
            }
        }
    
        { // Bloom
            bloom = new BloomEffect();
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

        hoverRegistered = 0;
        toolitp.builder = null;

        // TODO add a bloom pass to the map.
        { // Render Calls
            // if (!bloom.isInitialized()) bloom.init(pos);
            // bloom.beginSceneCapture();

            renderBg(alpha);
            renderGrid(alpha);
            renderPaths(alpha);
            renderNodes(alpha);
            renderShips(alpha);
            renderIcon(alpha);

            // bloom.endSceneCapture();
            
            // bloom.render();
        }

        GL11.glDisable(GL11.GL_BLEND);
        GL11.glDisable(GL11.GL_SCISSOR_TEST);
    }

    @Override
    public void processInput(List<InputEventAPI> events) {
        super.processInput(events);
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
        super.advance(amount);
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

        final int mx = Mouse.getX();
        final int my = Mouse.getY();

        for (PathData data : flowData) {
            final Vector2f aLocal = project(data.source.getLocation());
            final Vector2f bLocal = project(data.destination.getLocation());

            // convert to SCREEN coords by adding panel origin
            final float srcX = pos.getX() + aLocal.x;
            final float srcY = pos.getY() + aLocal.y;
            final float destX = pos.getX() + bLocal.x;
            final float destY = pos.getY() + bLocal.y;

            final float diffX = destX - srcX;
            final float diffY = destY - srcY;
            final float len = Arithmetic.dist(srcX, srcY, destX, destY);
            if (len == 0f) continue;

            final float gapSrc = systemData.get(data.source).nodeSize * PATH_GAP_MULT;
            final float gapDest = systemData.get(data.destination).nodeSize * PATH_GAP_MULT;

            // move endpoints toward each other
            final float ax = srcX + diffX / len * gapSrc;
            final float ay = srcY + diffY / len * gapSrc;
            final float bx = destX - diffX / len * gapDest;
            final float by = destY - diffY / len * gapDest;

            final boolean isHovering;
            {
                final float dx = bx - ax;
                final float dy = by - ay;
                final float len2 = dx*dx + dy*dy;
                if (len2 == 0f) {
                    isHovering = false;
                } else {
                    final float hw = data.pathWidth * 0.6f;
                    final float t = Arithmetic.clamp(((mx - ax) * dx + (my - ay) * dy) / len2, 0f, 1f);
                    final float cx = ax + t * dx;
                    final float cy = ay + t * dy;
                    isHovering = Arithmetic.dist(mx, my, cx, cy) <= hw && hoverRegistered < 1;
                    if (isHovering) hoverRegistered = 1;
                }
            }

            final Color baseColor = getWeightedCycleColor(data.getColorWeights(), data.getWeightSum());
            final Color color = isHovering ? NativeUiUtils.adjustBrightness(baseColor, 1.7f) : baseColor;

            // outer halo
            final float haloAlpha = alpha * (isHovering ? 0.7f : 0.3f);
            RenderUtils.drawGradientSprite(ax, ay, bx, by, data.pathWidth * 1.7f, color, true,
                haloAlpha, haloAlpha * 0.7f, haloAlpha);

            // inner core (bright, constant)
            RenderUtils.drawGradientSprite(ax, ay, bx, by, data.pathWidth * 0.5f, color, true,
                alpha, alpha, alpha);

            // thin white specular (center)
            RenderUtils.drawGradientSprite(ax, ay, bx, by, Math.max(1f, data.pathWidth * 0.05f),
                Color.WHITE, true, 0.8f * alpha, 0.5f * alpha, 0.8f * alpha);
        
            if (isHovering) updateTpState(data);
        }

        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glDisable(GL11.GL_LINE_SMOOTH);
        GL11.glDisable(GL11.GL_POLYGON_SMOOTH);
        GL11.glHint(GL11.GL_LINE_SMOOTH_HINT, GL11.GL_DONT_CARE);
    }

    private final void renderNodes(float alpha) {
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);

        final float freq = 0.7f;
        final float glow = 0.5f + 0.5f * (float)Math.sin(time * freq);
        final float overlaySizeMult = 1f + (float)Math.sin(time * freq) * 0.3f;

        final int mx = Mouse.getX();
        final int my = Mouse.getY();

        for (SystemData data : systemData.values()) {
            final Vector2f sysPos = project(data.system.getLocation());
            final float nodeSize = data.nodeSize;

            final float x = pos.getX() + sysPos.x;
            final float y = pos.getY() + sysPos.y;

            if (x < pos.getX() - nodeSize * 2f ||
                x > pos.getX() + pos.getWidth() + nodeSize * 2f ||
                y < pos.getY() - nodeSize * 2f ||
                y > pos.getY() + pos.getHeight() + nodeSize * 2f)
            { continue; }

            final boolean isHovering = mx >= x - nodeSize/2 && mx <= x + nodeSize/2 &&
                my >= y - nodeSize/2 && my <= y + nodeSize/2 && hoverRegistered < 2;
            if (isHovering) hoverRegistered = 2;

            final Color color = getWeightedCycleColor(data.getColorWeights(), data.getWeightSum());

            NODE_UNDERLAY.setSize(nodeSize * 1.2f, nodeSize * 1.2f);
            NODE_UNDERLAY.setColor(NODE_UNDERLAY_COLOR);
            NODE_UNDERLAY.setAlphaMult(alpha * 0.5f);
            NODE_UNDERLAY.renderAtCenter(x, y);

            NODE_SPRITE.setSize(nodeSize, nodeSize);
            NODE_SPRITE.setColor(color);
            NODE_SPRITE.setAlphaMult(alpha * 0.5f);
            NODE_SPRITE.renderAtCenter(x, y);

            final float overlaySize = overlaySizeMult * nodeSize * (isHovering ? 1.5f : 1f);
            NODE_OVERLAY.setSize(overlaySize, overlaySize);
            NODE_OVERLAY.setColor(isHovering ? NativeUiUtils.adjustBrightness(color, 1.5f) : color);
            NODE_OVERLAY.setAlphaMult(alpha * 0.5f * (isHovering ? 2f : glow));
            NODE_OVERLAY.renderAtCenter(x, y);
        
            if (isHovering) updateTpState(data);
        }
    }

    private final void renderShips(float alpha) {
        final int mx = Mouse.getX();
        final int my = Mouse.getY();

        final String comID = CommoditySelectionPanel.selectedCom.getId();
        for (TradeMission m : missions) {
            if (m.status != MissionStatus.IN_TRANSIT || m.travelDur <= 0f) continue;
            final SystemData srcData = systemData.get(m.src.getStarSystem());
            final SystemData dstData = systemData.get(m.dest.getStarSystem());
            if (srcData == null || dstData == null) continue;

            final Vector2f aLocal = project(m.src.getLocation());
            final Vector2f bLocal = project(m.dest.getLocation());

            final float srcX = pos.getX() + aLocal.x;
            final float srcY = pos.getY() + aLocal.y;
            final float destX = pos.getX() + bLocal.x;
            final float destY = pos.getY() + bLocal.y;

            final float diffX = destX - srcX;
            final float diffY = destY - srcY;
            final float len = Arithmetic.dist(srcX, srcY, destX, destY);

            // move endpoints toward each other
            final float gapSrc = srcData.nodeSize * PATH_GAP_MULT;
            final float gapDest = dstData.nodeSize * PATH_GAP_MULT;

            final float ax = srcX + diffX / len * gapSrc;
            final float ay = srcY + diffY / len * gapSrc;
            final float bx = destX - diffX / len * gapDest;
            final float by = destY - diffY / len * gapDest;

            final float travelFrac = Arithmetic.clamp(1f - (m.durRemaining - m.transferDur) / m.travelDur, 0f, 1f);
            final float cx = ax + (bx - ax) * travelFrac;
            final float cy = ay + (by - ay) * travelFrac;

            float arrowAlpha = alpha;
            if (travelFrac < 0.03f) {
                arrowAlpha *= travelFrac / 0.03f;
            } else if (travelFrac > 0.97f) {
                arrowAlpha *= (1f - travelFrac) / 0.03f;
            }
            double amount = 0.0;
            for (TradeCom c : m.cargo) {
                if (c.comID.equals(comID)) {
                    amount = c.amount;
                    break;
                }
            }
            final float arrowSize = calculatePathWidth(amount) * 2f * (0.4f + 0.6f*arrowAlpha);

            final boolean isHovering = mx >= cx - arrowSize/2 && mx <= cx + arrowSize/2 &&
                my >= cy - arrowSize/2 && my <= cy + arrowSize/2 && hoverRegistered < 3;
            if (isHovering) hoverRegistered = 3;

            final Color baseColor = m.src.getFaction().getBaseUIColor();
            final Color color = isHovering ? NativeUiUtils.adjustBrightness(baseColor, 1.7f) : baseColor;

            NativeUiUtils.rotateSprite(aLocal, bLocal, SHIP_ARROW);
            SHIP_ARROW.setSize(arrowSize, arrowSize);
            SHIP_ARROW.setAlphaMult(arrowAlpha);
            SHIP_ARROW.setColor(color);
            SHIP_ARROW.renderAtCenter(cx, cy);

            if (isHovering) updateTpState(m);
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

    private final float calculatePathWidth(double amount) {
        return (BASE_FLOW_PATH_W + (float) Math.log10(1f + amount) * FLOW_PATH_SCALE) * visualZoom();
    }

    private final float calculateNodeSize(double amount) {
        return (BASE_NODE_RADIUS + (float) Math.log10(1f + amount) * NODE_RADIUS_SCALE) * visualZoom();
    }

    private final float visualZoom() {
        return (float) Math.pow(zoom, 0.6f);
    }

    public final Color getWeightedCycleColor(final ArrayMap<Color, Double> weights, double totalWeight) {
        final int count = weights.size();
        if (count == 0) return Color.WHITE;
        if (count == 1) return weights.keyAt(0);

        final float[] segDur = new float[count];
        float totalSeg = 0f;
        for (int i = 0; i < count; i++) {
            segDur[i] = (float) (COLOR_CHANGE_DUR * weights.valueAt(i) / totalWeight);
            totalSeg += segDur[i];
        }

        float extraTime = 0f;
        for (int i = 0; i < count; i++) {
            if (segDur[i] < MIN_COLOR_DUR) {
                extraTime += MIN_COLOR_DUR - segDur[i];
                segDur[i] = MIN_COLOR_DUR;
            }
        }

        final float overTime = (float) (totalSeg + extraTime - COLOR_CHANGE_DUR);
        if (overTime > 0f) {
            float totalLarge = 0f;
            for (int i = 0; i < count; i++) {
                if (segDur[i] > MIN_COLOR_DUR) totalLarge += segDur[i] - MIN_COLOR_DUR;
            }
            if (totalLarge > 0f) {
                for (int i = 0; i < count; i++) {
                    if (segDur[i] > MIN_COLOR_DUR) {
                        segDur[i] -= ((segDur[i] - MIN_COLOR_DUR) / totalLarge) * overTime;
                    }
                }
            }
        }

        final float tCycle = time % COLOR_CHANGE_DUR;
        float accumulated = 0f;
        int idx = 0;
        for (int i = 0; i < count; i++) {
            if (tCycle < accumulated + segDur[i]) {
                idx = i;
                break;
            }
            accumulated += segDur[i];
        }

        final int nextIdx = (idx + 1) % count;
        final float localT = Arithmetic.clamp((tCycle - accumulated) / segDur[idx], 0f, 1f);

        if (localT < 1f - COLOR_TRANSITION_FRAC) {
            return weights.keyAt(idx);
        } else {
            final float blendT = (localT - (1f - COLOR_TRANSITION_FRAC)) / COLOR_TRANSITION_FRAC;
            return NativeUiUtils.lerpColor(weights.keyAt(idx), weights.keyAt(nextIdx), blendT);
        }
    }

    private static final TooltipBuilder createPathTp(PathData data) {
        return (tp, expanded) -> {
            final var entries = new ArrayList<>(data.factionAmounts.entrySet());
            entries.sort((a, b) -> Double.compare(b.getValue(), a.getValue()));

            final SectorAPI sector = Global.getSector();

            tp.addPara("Trade Route", base, 0f);

            tp.beginTable(Global.getSector().getPlayerFaction(), 20, new Object[] {
                "Faction", 140, "Volume", 70
            });
            for (var entry : entries) {
                final double value = entry.getValue();
                if (Math.abs(value) < 0.01f) continue;

                final FactionAPI faction = sector.getFaction(entry.getKey().getId());
                tp.addRow(
                    faction.getBaseUIColor(),
                    faction.getDisplayName(),
                    highlight,
                    value < 1f ? "<1" : NumFormat.engNotate(value)
                );
            }
            tp.addTable("", 0, opad);
            tp.addSpacer(pad);
        };
    }

    private static final TooltipBuilder createNodeTp(SystemData data) {
        return (tp, expanded) -> {
            final var entries = new ArrayList<>(data.marketAmounts.entrySet());
            entries.sort((a, b) -> Double.compare(b.getValue(), a.getValue()));

            tp.addPara(data.system.getName(), base, 0f);

            tp.beginTable(Global.getSector().getPlayerFaction(), 20, new Object[] {
                "Colony", 140, "Net Trade", 70
            });
            for (var entry : entries) {
                final MarketAPI market = entry.getKey();
                final double amount = entry.getValue();
                if (Math.abs(amount) < 0.01) continue;

                tp.addRow(
                    market.getFaction().getBaseUIColor(),
                    market.getName(),
                    amount < 0.0 ? negative : highlight,
                    Math.abs(amount) < 1f ? "<1" : NumFormat.engNotate(amount)
                );
            }
            tp.addTable("", 0, opad);
            tp.addSpacer(pad);
        };
    }

    private final void updateTpState(Object dataObj) {
        if (hoveredElement == null || hoveredElement != dataObj) {
            tooltipSys.hideTooltip(toolitp);
            hoveredElement = dataObj;
        }
        if (dataObj instanceof PathData data) {
            toolitp.width = 220f;
            toolitp.builder = createPathTp(data);
        } else if (dataObj instanceof SystemData data) {
            toolitp.width = 220f;
            toolitp.builder = createNodeTp(data);
        } else if (dataObj instanceof TradeMission mission) {
            toolitp.width = 380f;
            toolitp.builder = TradeMissionWidget.createMissionTp(mission, false);
        }
    }

    private static final List<TradeMission> getFilteredMissions(String comID) {
        final List<TradeMission> missions = new ArrayList<>(EconomyEngine.instance().getActiveMissions());

        missions.removeIf(m -> !m.cargo.stream().anyMatch(c -> c.comID.equals(comID)) || m.src == null || m.dest == null);
        missions.removeIf(m -> TradeFilters.exporterFactionBlacklist.contains(m.src.getFactionId()));
        missions.removeIf(m -> TradeFilters.importerFactionBlacklist.contains(m.dest.getFactionId()));
        missions.removeIf(m -> m.src.isHidden() || m.dest.isHidden());
        
        if (TradeFilters.hideVirtualFleets) {
            missions.removeIf(m -> m.spawnedFleetFinishedJob);
        }

        if (TradeFilters.directionMode == 3) {
            missions.removeIf(m -> !m.inFaction);
        }

        missions.removeIf(m -> {
            double amount = 0.0;
            for (TradeCom c : m.cargo) {
                if (c.comID.equals(comID)) {
                    amount = c.amount;
                    break;
                }
            }
            return amount < TradeFilters.minTradeAmount;
        });

        return missions;
    }

    private static record SystemPair(StarSystemAPI source, StarSystemAPI dest) {}
}
package wfg_ltv_econ.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import org.lwjgl.util.vector.Vector2f;

import java.awt.Color;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.PlanetAPI;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.campaign.econ.CommodityOnMarketAPI;
import com.fs.starfarer.api.campaign.econ.CommoditySpecAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.impl.campaign.ids.Submarkets;
import com.fs.starfarer.api.impl.campaign.submarkets.OpenMarketPlugin;
import com.fs.starfarer.api.ui.Alignment;
import com.fs.starfarer.api.ui.CustomPanelAPI;
import com.fs.starfarer.api.ui.Fonts;
import com.fs.starfarer.api.ui.LabelAPI;
import com.fs.starfarer.api.ui.PositionAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.CountingMap;
import com.fs.starfarer.api.util.Misc;
import com.fs.starfarer.codex2.CodexDialog;
import com.fs.starfarer.ui.impl.CargoTooltipFactory;
import com.fs.starfarer.ui.impl.StandardTooltipV2Expandable;

import wfg_ltv_econ.plugins.LtvSpritePanelPlugin;
import wfg_ltv_econ.ui.LtvCustomPanel;
import wfg_ltv_econ.ui.LtvSpritePanel;

public class TooltipUtils {

    private final static String cargoTooltipArrow_PATH;

    static {
        cargoTooltipArrow_PATH = Global.getSettings().getSpriteName("ui", "cargoTooltipArrow");
    }

    public static void dynamicPos(TooltipMakerAPI tooltip, CustomPanelAPI anchor, int opad) {
        PositionAPI pos = tooltip.getPosition();

        final float tooltipWidth = pos.getWidth();
        final float tooltipHeight = pos.getHeight();

        final float screenW = Global.getSettings().getScreenWidth();
        final float screenH = Global.getSettings().getScreenHeight();

        pos.rightOfTop(anchor, opad);

        // If it overflows the screen to the right, move it to the left
        if (pos.getX() + tooltipWidth > screenW) {
            pos.leftOfTop(anchor, opad);
        }

        float y = pos.getY();
        float yOverflowTop = y + tooltipHeight - screenH;
        float yUnderflowBottom = y < 0 ? -y : 0;

        // If it overflows the top, clamp it to top
        if (yUnderflowBottom > 0) {
            pos.setYAlignOffset(-yOverflowTop - opad);
        }

        // If it overflows the bottom, clamp it to bottom
        if (yUnderflowBottom > 0) {
            pos.setYAlignOffset(yUnderflowBottom + opad);
        }
    }

    public static void mouseCornerPos(TooltipMakerAPI tooltip, int opad) {
        final int mouseSize = 40;
        final float correction = 8f;

        PositionAPI pos = tooltip.getPosition();

        float tooltipW = pos.getWidth();
        float tooltipH = pos.getHeight();
        float mouseX = Global.getSettings().getMouseX();
        float mouseY = Global.getSettings().getMouseY();
        float screenW = Global.getSettings().getScreenWidth();

        pos.inBL(0, 0);

        float tooltipX = pos.getX();
        float tooltipY = pos.getY();

        // Bottom-left of mouse
        float offsetX = (mouseX - tooltipX) + mouseSize / 2f;
        float offsetY = (mouseY - tooltipY) - tooltipH - mouseSize;

        // If right-side overflow
        if (tooltipX + offsetX + tooltipW > screenW - opad) {
            offsetX -= tooltipW + mouseSize - correction;
        }

        // If bottom overflow
        if (tooltipY + offsetY < opad) {
            offsetY += tooltipH + mouseSize + correction;
        }

        pos.setXAlignOffset(offsetX);
        pos.setYAlignOffset(offsetY);
    }
    /**
     * Reflectively calls the original factory method
     */
    public static void cargoTooltipFactory(TooltipMakerAPI tooltip, float pad, CommoditySpecAPI com,
            int rowsPerTable, boolean showExplanation, boolean showBestSell, boolean showBestBuy) {

        ReflectionUtils.invoke(CargoTooltipFactory.class, "super", tooltip, pad, com, rowsPerTable, showExplanation, showBestSell, showBestBuy);
        tooltip.getPosition().setSize(1000, 0);
    }

    /**
     * Literally copied this from com.fs.starfarer.ui.impl.CargoTooltipFactory.
     * Only modified the parts that concern me. All hail Alex, the Lion of Sindria.
     * @param showExplanation
     * Displays explanation paragraphs
     * 
     * @param showBestSell
     * Shows the best places to make a profit selling the commodity.
     * 
     * @param showBestBuy
     * Shows the best places to buy the commodity at a discount.
     */
    public static void cargoComTooltip (TooltipMakerAPI tooltip, int pad, int opad, CommoditySpecAPI comSpec,
        int rowsPerTable, boolean showExplanation, boolean showBestSell, boolean showBestBuy) {

        final Color gray = Misc.getGrayColor();
        final Color highlight = Misc.getHighlightColor();
        final int rowH = 20;

        if (!Global.getSector().getIntelManager().isPlayerInRangeOfCommRelay()) {
            if (showExplanation) {
                tooltip.addPara("Seeing remote price data for various colonies requires being within range of a functional comm relay.", gray, pad);
            }
            return;
        }

        final CountingMap<String> countingMap = new CountingMap<>();
        final String comID = comSpec.getId();
        final int econUnit = (int)comSpec.getEconUnit();
        
        if (showBestSell) {
            ArrayList<MarketAPI> marketList = new ArrayList<>();
            for (MarketAPI market : Global.getSector().getEconomy().getMarketsCopy()) {
                if (!market.isHidden() && market.getEconGroup() == null &&
                market.hasSubmarket(Submarkets.SUBMARKET_OPEN)) {
                    CommodityOnMarketAPI comData = market.getCommodityData(comID);
                    int demandScore = (int)(comData.getDemand().getDemand().getModifiedValue() + comData.getGreedValue());
                    if (demandScore > 0 && demandScore >= econUnit) {
                        marketList.add(market);
                    }
                }
            }

            Collections.sort(marketList, createSellComparator(comID, econUnit));
            if (!marketList.isEmpty()) {
                tooltip.addPara("Best places to sell:", pad);
                int relativeY = (int) tooltip.getPosition().getY() - (int) tooltip.getPrev().getPosition().getY();
                tooltip.beginTable(Global.getSector().getPlayerFaction(), rowH, new java.lang.Object[]{"Price", 100, "Demand", 70, "Deficit", 70, "Location", 230, "Star system", 140, "Dist (ly)", 80});
                countingMap.clear();
                
                int rowCount = 0;
                for (MarketAPI market : marketList) {
                    if (countingMap.getCount(market.getFactionId()) < 3) {
                        countingMap.add(market.getFactionId());
                        CommodityOnMarketAPI com = market.getCommodityData(comID);
                        CommodityStats comStat = new CommodityStats(com, market);
                        long marketDemand = com.getMaxDemand() - com.getPlayerTradeNetQuantity();
                        if (marketDemand < 0) {
                            marketDemand = 0;
                        }

                        int unitPrice = (int)market.getDemandPrice(comID, 1, true);
                        long deficit = comStat.localDeficit;
                        Color labelColor = highlight;
                        Color deficitColor = gray;
                        String quantityLabel = "---";
                        if (deficit > 0) {
                            quantityLabel = NumFormat.engNotation(deficit);
                            deficitColor = Misc.getNegativeHighlightColor();
                        }

                        if (marketDemand > 0) {
                            String lessThanSymbol = "";
                            marketDemand = marketDemand / 100 * 100;
                            if (marketDemand < 100) {
                                marketDemand = 100;
                                lessThanSymbol = "<";
                                labelColor = gray;
                            }

                            String factionName = market.getFaction().getDisplayName();
                            String location = "In hyperspace";
                            Color locationColor = gray;
                            if (market.getStarSystem() != null) {
                                StarSystemAPI starSystem = market.getStarSystem();
                                location = starSystem.getBaseName();
                                PlanetAPI star = starSystem.getStar();
                                locationColor = star.getSpec().getIconColor();
                                locationColor = Misc.setBrightness(locationColor, 235);
                            }

                            float distanceToPlayer = Misc.getDistanceToPlayerLY(market.getPrimaryEntity());
                            
                            tooltip.addRow(new java.lang.Object[]{
                                highlight, 
                                Misc.getDGSCredits(unitPrice), 
                                labelColor, 
                                lessThanSymbol + NumFormat.engNotation(marketDemand), 
                                deficitColor, 
                                quantityLabel, 
                                Alignment.LMID, 
                                market.getFaction().getBaseUIColor(), 
                                market.getName() + " - " + factionName, 
                                locationColor, 
                                location, 
                                highlight, 
                                Misc.getRoundedValueMaxOneAfterDecimal(distanceToPlayer)
                            });

                            // Arrow Sprite
                            SpriteAPI arrow = Global.getSettings().getSprite(cargoTooltipArrow_PATH);

                            LtvCustomPanel arrowPanel = new LtvSpritePanel(null, tooltip, null,
                            20, 20, new LtvSpritePanelPlugin(), "", null, false);
                            LtvSpritePanelPlugin plugin = ((LtvSpritePanelPlugin)arrowPanel.getPlugin());

                            plugin.setSprite(arrow);

                            Vector2f playerLoc = Global.getSector().getPlayerFleet().getLocationInHyperspace();
                            Vector2f targetLoc = market.getStarSystem().getLocation();

                            arrow.setAngle(rotateSprite(playerLoc, targetLoc));

                            int arrowY = relativeY + rowH * (2 + rowCount);
                            tooltip.addComponent(arrowPanel.getPanel()).inTL(610, arrowY);

                            ++rowCount;
                            if (rowCount >= rowsPerTable) {
                               break;
                            }
                        }
                    }
                }

                tooltip.addTable("", 0, opad);
            }
        }

        if (showBestBuy) {
            ArrayList<MarketAPI> marketList = new ArrayList<>();
            for (MarketAPI market : Global.getSector().getEconomy().getMarketsCopy()) {
                if (!market.isHidden() && market.getEconGroup() == null &&
                market.hasSubmarket(Submarkets.SUBMARKET_OPEN)) {
                    CommodityOnMarketAPI comData = market.getCommodityData(comID);
                    int stockpileLimit = OpenMarketPlugin.getApproximateStockpileLimit(comData);
                    if (stockpileLimit > 0 && stockpileLimit >= econUnit) {
                        marketList.add(market);
                    }
                }
            }

            Collections.sort(marketList, createBuyComparator(comID, econUnit));
            if (!marketList.isEmpty()) {
                int dynaOpad = showBestSell ? opad*2 : opad;

                tooltip.addPara("Best places to buy:", dynaOpad);
                int relativeY = (int) tooltip.getPosition().getY() - (int) tooltip.getPrev().getPosition().getY();
                tooltip.beginTable(Global.getSector().getPlayerFaction(), 20, new java.lang.Object[]{"Price", 100, "Available", 70, "Excess", 70, "Location", 230, "Star system", 140, "Dist (ly)", 80});
                countingMap.clear();

                int rowCount = 0;
                for (MarketAPI market : marketList) {
                    CommodityOnMarketAPI com = market.getCommodityData(comID);

                    if (countingMap.getCount(market.getFactionId()) < 3) {
                        countingMap.add(market.getFactionId());
                        long stockpileLimit = OpenMarketPlugin.getApproximateStockpileLimit(com);
                        int unitPrice = (int)market.getSupplyPrice(comID, 1, true);
                        stockpileLimit += com.getPlayerTradeNetQuantity();
                        if (stockpileLimit < 0) {
                            stockpileLimit = 0;
                        }

                        int excess = com.getExcessQuantity();
                        Color excessColor = gray;
                        String excessStr = "---";
                        if (excess > 0) {
                            excessStr = NumFormat.engNotation(excess);
                            excessColor = Misc.getPositiveHighlightColor();
                        }

                        String availableStr = "";
                        stockpileLimit = stockpileLimit / 100 * 100;
                        if (stockpileLimit < 100) {
                            stockpileLimit = 100;
                            availableStr = "<";
                        }

                        String factionName = market.getFaction().getDisplayName();
                        String location = "In hyperspace";
                        Color locationColor = gray;
                        if (market.getStarSystem() != null) {
                            StarSystemAPI StarSystem = market.getStarSystem();
                            location = StarSystem.getBaseName();
                            PlanetAPI star = StarSystem.getStar();
                            locationColor = star.getSpec().getIconColor();
                            locationColor = Misc.setBrightness(locationColor, 235);
                        }

                        float distance = Misc.getDistanceToPlayerLY(market.getPrimaryEntity());

                        tooltip.addRow(new java.lang.Object[]{
                            highlight,
                            Misc.getDGSCredits(unitPrice),
                            highlight,
                            availableStr + NumFormat.engNotation(stockpileLimit),
                            excessColor,
                            excessStr,
                            Alignment.LMID,
                            market.getFaction().getBaseUIColor(),
                            market.getName() + " - " + factionName,
                            locationColor,
                            location,
                            highlight,
                            Misc.getRoundedValueMaxOneAfterDecimal(distance)
                        });

                        // Arrow Sprite
                        SpriteAPI arrow = Global.getSettings().getSprite(cargoTooltipArrow_PATH);

                        LtvCustomPanel arrowPanel = new LtvSpritePanel(null, tooltip, null,
                        20, 20, new LtvSpritePanelPlugin(), "", null, false);
                        LtvSpritePanelPlugin plugin = ((LtvSpritePanelPlugin)arrowPanel.getPlugin());

                        plugin.setSprite(arrow);

                        Vector2f playerLoc = Global.getSector().getPlayerFleet().getLocationInHyperspace();
                        Vector2f targetLoc = market.getStarSystem().getLocation();

                        arrow.setAngle(rotateSprite(playerLoc, targetLoc));

                        int arrowY = relativeY + rowH * (2 + rowCount);
                        tooltip.addComponent(arrowPanel.getPanel()).inTL(610, arrowY);

                        rowCount++;
                        if (rowCount >= rowsPerTable) {
                            break;
                        }
                    }
                }

                tooltip.addTable("", 0, opad);
            }
         }

        if (showExplanation) {
            tooltip.addPara("All values approximate. Prices do not include tariffs, which can be avoided through black market trade.", Misc.getGrayColor(), opad);

            // final Color txtColor = Misc.setAlpha(highlight, 155);
            // tooltip.addPara("*Per unit prices assume buying or selling a batch of %s units. Each unit bought costs more as the market's supply is reduced, and each unit sold brings in less as demand is fulfilled.", opad, gray, txtColor, new String[]{"" + econUnit});
        }
    }

    /**
     * Creates a static Codex footer with no functionality. 
     * The Codex is static and its labels must be updated manually.
     * The Codex must be attached manually.
     * The F1 and F2 events must be handled using the Plugin.
     */
    public static void createCustomCodex(TooltipMakerAPI tooltip, TooltipMakerAPI codexTooltip,
        LtvCustomPanel panel, String codexF1, String codexF2, int codexW) {

        final int pad = 3;
        final int opad = 10;
        final Color gray = new Color(100, 100, 100);
        final Color highlight = Misc.getHighlightColor();
        
        // Create the custom Footer
        codexTooltip = ((CustomPanelAPI)panel.getParent()).createUIElement(codexW, 0, false);

        codexTooltip.setParaFont(Fonts.ORBITRON_12);
        ((StandardTooltipV2Expandable)codexTooltip).setShowBackground(true);
        ((StandardTooltipV2Expandable)codexTooltip).setShowBorder(true);

        codexTooltip.setParaFontColor(gray);
        LabelAPI lbl1 = codexTooltip.addPara(codexF1, 0, highlight, "F1");
        LabelAPI lbl2 = codexTooltip.addPara(codexF2, 0, highlight, "F2");

        lbl1.getPosition().inTL(opad/2f, -2);
        int lbl2X = (int) (lbl1.computeTextWidth(lbl1.getText()) + opad + pad);
        lbl2.getPosition().inTL(lbl2X, -2);

        int tooltipH = (int) lbl1.computeTextHeight(lbl1.getText()) - opad/2;

        codexTooltip.setHeightSoFar(tooltipH);

        if (panel instanceof LtvCustomPanel.TooltipProvider) {
            ((LtvCustomPanel.TooltipProvider)panel).attachCodexTooltip(codexTooltip);
        }
    }

    /**
     * This function assumes that the sprite is pointing right.
     * In other words, it's directed towards the positive x-axis in Hyperspace.
     */
    public static float rotateSprite(Vector2f origin, Vector2f target) {
        Vector2f delta = Vector2f.sub(target, origin, null);

        float angleDegrees = (float) Math.toDegrees(Math.atan2(delta.y, delta.x));

        return angleDegrees;
    }

    public static void openCodexPage(String codexID) {
        CodexDialog.show(codexID);
    }

    private static Comparator<MarketAPI> createSellComparator(String comID, int econUnit) {
        return (m1, m2) -> {
            int price1 = (int) (m1.getDemandPrice(comID, (double)econUnit, true) / econUnit);
            int price2 = (int) (m2.getDemandPrice(comID, (double)econUnit, true) / econUnit);
            return Integer.compare(price2, price1);
        };
    }

    private static Comparator<MarketAPI> createBuyComparator(String comID, int econUnit) {
    return (m1, m2) -> {
        int price1 = (int) (m1.getSupplyPrice(comID, (double)econUnit, true) / econUnit);
        int price2 = (int) (m2.getSupplyPrice(comID, (double)econUnit, true) / econUnit);
        return Integer.compare(price1, price2);
        };
    }
}

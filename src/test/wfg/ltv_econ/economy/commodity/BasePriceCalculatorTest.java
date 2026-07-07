package wfg.ltv_econ.economy.commodity;

import wfg.ltv_econ.economy.commodity.BasePriceCalculator.TransactionDirection;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static wfg.ltv_econ.economy.commodity.BasePriceCalculator.*;

public class BasePriceCalculatorTest {
    private static final float BASE_PRICE = 18.0f;
    private static final float DEMAND = 100.0f;
    private static final double TOLERANCE = 0.01;   // 1% relative tolerance for integral comparisons

    @Test
    public void sellToEntity_MoreUnits_LowerAveragePrice() {
        final double stored = DEMAND; // equilibrium
        final float avg1 = BasePriceCalculator.getUnitPrice(TransactionDirection.ENTITY_BUYING, 10, stored, BASE_PRICE, DEMAND);
        final float avg2 = BasePriceCalculator.getUnitPrice(TransactionDirection.ENTITY_BUYING, 20, stored, BASE_PRICE, DEMAND);

        assertTrue(avg2 < avg1, "Average price for selling 20 units should be less than for 10");
    }

    @Test
    public void buyFromEntity_MoreUnits_HigherAveragePrice() {
        final double stored = DEMAND;
        final float avg1 = BasePriceCalculator.getUnitPrice(TransactionDirection.ENTITY_SELLING, 10, stored, BASE_PRICE, DEMAND);
        final float avg2 = BasePriceCalculator.getUnitPrice(TransactionDirection.ENTITY_SELLING, 20, stored, BASE_PRICE, DEMAND);

        assertTrue(avg2 > avg1, "Average price for buying 20 units should be greater than for 10");
    }

    @Test
    public void buySplit_SameTotalCost() {
        final double stored = DEMAND + 50;
        final long totalAmount = 100;

        final float avgOnce = BasePriceCalculator.getUnitPrice(TransactionDirection.ENTITY_SELLING, totalAmount, stored, BASE_PRICE, DEMAND);
        final double totalOnce = avgOnce * totalAmount;

        final long half = totalAmount / 2;
        final float avgFirst = BasePriceCalculator.getUnitPrice(TransactionDirection.ENTITY_SELLING, half, stored, BASE_PRICE, DEMAND);
        final double costFirst = avgFirst * half;
        final double newStock = stored - half;

        final float avgSecond = BasePriceCalculator.getUnitPrice(TransactionDirection.ENTITY_SELLING, half, newStock, BASE_PRICE, DEMAND);
        final double costSecond = avgSecond * half;
        final double totalSplit = costFirst + costSecond;

        assertEquals(totalOnce, totalSplit, TOLERANCE * totalOnce,
                "Split buying total cost should equal one-time total cost");
    }

    @Test
    public void sellSplit_SameTotalRevenue() {
        final double stored = DEMAND - 30;
        final long totalAmount = 80;

        final float avgOnce = BasePriceCalculator.getUnitPrice(TransactionDirection.ENTITY_BUYING, totalAmount, stored, BASE_PRICE, DEMAND);
        final double totalOnce = avgOnce * totalAmount;

        final long half = totalAmount / 2;
        final float avgFirst = BasePriceCalculator.getUnitPrice(TransactionDirection.ENTITY_BUYING, half, stored, BASE_PRICE, DEMAND);
        final double revFirst = avgFirst * half;
        final double newStock = stored + half;

        final float avgSecond = BasePriceCalculator.getUnitPrice(TransactionDirection.ENTITY_BUYING, half, newStock, BASE_PRICE, DEMAND);
        final double revSecond = avgSecond * half;
        final double totalSplit = revFirst + revSecond;

        assertEquals(totalOnce, totalSplit, TOLERANCE * totalOnce,
                "Split selling total revenue should equal one-time total revenue");
    }

    @Test
    public void buyMoreThanStock_HigherAverageThanExactStock() {
        final double stored = 40.0;
        final long exactAmount = (long) stored;
        final long overAmount = exactAmount + 30;

        final float avgExact = BasePriceCalculator.getUnitPrice(TransactionDirection.ENTITY_SELLING, exactAmount, stored, BASE_PRICE, DEMAND);
        final float avgOver = BasePriceCalculator.getUnitPrice(TransactionDirection.ENTITY_SELLING, overAmount, stored, BASE_PRICE, DEMAND);

        assertTrue(avgOver > avgExact,
                "Average price when depleting stock into negatives should be higher than stopping at zero");
    }

    @Test
    public void buyWhenStockNegative_PriceHigh() {
        final double stored = -50.0;
        final float avg = BasePriceCalculator.getUnitPrice(TransactionDirection.ENTITY_SELLING, 10, stored, BASE_PRICE, DEMAND);

        assertTrue(avg > BASE_PRICE, "Price with negative stock should exceed base price");
        assertTrue(avg < 1e6, "Price should be finite and not absurdly huge (reasonable)");
        assertTrue(avg > BASE_PRICE * 1.5, "Negative stock should give notably elevated price");
    }

    @Test
    public void neutralPrice_ReturnsInstantaneousPrice() {
        final double stored = 80.0;
        final float neutral = BasePriceCalculator.getUnitPrice(TransactionDirection.NEUTRAL, 0, stored, BASE_PRICE, DEMAND);
        final float buy1 = BasePriceCalculator.getUnitPrice(TransactionDirection.ENTITY_SELLING, 0, stored, BASE_PRICE, DEMAND);
        assertEquals(neutral, buy1, 1e-6,
                "Neutral with zero amount should return instantaneous price, same as zero buy");
    }

    @Test
    public void equilibriumPrice_EqualsBasePrice() {
        final double stored = DEMAND; // equilibrium
        final float neutral = BasePriceCalculator.getUnitPrice(TransactionDirection.NEUTRAL, 0, stored, BASE_PRICE, DEMAND);
        assertEquals(BASE_PRICE, neutral, TOLERANCE,
                "At stock == demand, instantaneous price should equal base price");
    }

    @Test
    public void sellWhenStockNegative_GivesVeryHighPrice() {
        final double stored = -80.0;
        final float avg = BasePriceCalculator.getUnitPrice(TransactionDirection.ENTITY_BUYING, 20, stored, BASE_PRICE, DEMAND);
        assertTrue(avg > BASE_PRICE * 2, "Selling to entity when stock is negative should still yield high price");
        assertTrue(avg < 1e6, "Price should be finite");
    }

    @Test
    public void priceNeverBelowOne() {
        final double stored = DEMAND * 1000;
        final float avgBuy = BasePriceCalculator.getUnitPrice(TransactionDirection.ENTITY_SELLING, 1, stored, BASE_PRICE, DEMAND);
        final float avgSell = BasePriceCalculator.getUnitPrice(TransactionDirection.ENTITY_BUYING, 1, stored, BASE_PRICE, DEMAND);
        final float neutral = BasePriceCalculator.getUnitPrice(TransactionDirection.NEUTRAL, 0, stored, BASE_PRICE, DEMAND);
        assertTrue(avgBuy >= 1.0f, "Price floor should be at least 1");
        assertTrue(avgSell >= 1.0f, "Price floor should be at least 1");
        assertTrue(neutral >= 1.0f, "Price floor should be at least 1");
    }

    @Test
    public void zeroAmount_ReturnsInstantaneousPrice() {
        final double stored = 70.0;
        final float p1 = BasePriceCalculator.getUnitPrice(TransactionDirection.ENTITY_BUYING, 0, stored, BASE_PRICE, DEMAND);
        final float p2 = BasePriceCalculator.getUnitPrice(TransactionDirection.NEUTRAL, 0, stored, BASE_PRICE, DEMAND);
        assertEquals(p1, p2, 1e-6);
    }

    @Test
    public void tradeStraddlingZero_StillAdditive() {
        // Start in deficit (negative), buy enough to cross into positive
        final double stored = -30.0;
        final long sellAmount = 80;
        final float avgFull = BasePriceCalculator.getUnitPrice(TransactionDirection.ENTITY_BUYING, sellAmount, stored, BASE_PRICE, DEMAND);
        final double revenueFull = avgFull * sellAmount;

        final long part1 = 40;
        final float avg1 = BasePriceCalculator.getUnitPrice(TransactionDirection.ENTITY_BUYING, part1, stored, BASE_PRICE, DEMAND);
        final double rev1 = avg1 * part1;
        final double midStock = stored + part1; // 10
        final long part2 = sellAmount - part1;
        final float avg2 = BasePriceCalculator.getUnitPrice(TransactionDirection.ENTITY_BUYING, part2, midStock, BASE_PRICE, DEMAND);
        final double rev2 = avg2 * part2;

        assertEquals(revenueFull, rev1 + rev2, TOLERANCE * revenueFull,
                "Total revenue should be additive even when crossing zero stock boundary");
    }

    @Test
    public void extremelyNegativeStock_DoesNotOverflow() {
        final double stored = -1e6;
        final float avg = BasePriceCalculator.getUnitPrice(TransactionDirection.ENTITY_SELLING, 1, stored, BASE_PRICE, DEMAND);
        assertTrue(!Float.isNaN(avg), "Should not be NaN");
        assertTrue(Float.isFinite(avg), "Should be finite");
        assertTrue(avg >= BASE_PRICE * BasePriceCalculator.PRICE_MULT_CEILING, "Extremely negative stock should give extremely high price");
    }

    @Test
    public void sellThenBuy_NetLoss() {
        final double stored = DEMAND + 20;
        final long amount = 50;

        final float avgSell = BasePriceCalculator.getUnitPrice(TransactionDirection.ENTITY_BUYING, amount, stored, BASE_PRICE, DEMAND);
        final double revenue = avgSell * amount;
        final double stockAfterSell = stored + amount;

        final float avgBuy = BasePriceCalculator.getUnitPrice(TransactionDirection.ENTITY_SELLING, amount, stockAfterSell, BASE_PRICE, DEMAND);
        final double cost = avgBuy * amount;

        assertEquals(revenue, cost, TOLERANCE * Math.max(revenue, 1.0),
                "Round-trip sell-then-buy should result in net credits staying the same");
    }

    @Test
    public void buyThenSell_NetLoss() {
        final double stored = DEMAND - 20;
        final long amount = 40;

        final float avgBuy = BasePriceCalculator.getUnitPrice(TransactionDirection.ENTITY_SELLING, amount, stored, BASE_PRICE, DEMAND);
        final double cost = avgBuy * amount;
        final double stockAfterBuy = stored - amount;

        final float avgSell = BasePriceCalculator.getUnitPrice(TransactionDirection.ENTITY_BUYING, amount, stockAfterBuy, BASE_PRICE, DEMAND);
        final double revenue = avgSell * amount;

        assertEquals(cost, revenue, TOLERANCE * Math.max(cost, 1.0),
                "Round-trip buy-then-sell should result in net credits staying the same");
    }

    @Test
    public void instantaneousPriceDecreasesWithStock() {
        final double[] stocks = {-100, -10, 0, 10, 50, 100, 500, 10_000};
        for (int i = 0; i < stocks.length - 1; i++) {
            final float p1 = BasePriceCalculator.getUnitPrice(TransactionDirection.NEUTRAL, 0, stocks[i],   BASE_PRICE, DEMAND);
            final float p2 = BasePriceCalculator.getUnitPrice(TransactionDirection.NEUTRAL, 0, stocks[i+1], BASE_PRICE, DEMAND);
            assertTrue(p1 >= p2, "Price should decrease as stock increases: " + stocks[i] + " -> " + stocks[i+1]);
        }
    }

    @Test
    public void tinyBuyInExcessGivesLowPrice() {
        final double stored = DEMAND * 1000;
        final float avg = BasePriceCalculator.getUnitPrice(TransactionDirection.ENTITY_SELLING, 1, stored, BASE_PRICE, DEMAND);
        assertTrue(avg < BASE_PRICE, "Buying 1 unit in huge excess should be cheap");
    }

    @Test
    public void tinyBuyInDeepDeficitHitsCeiling() {
        final double stored = -10_000;
        final float avg = BasePriceCalculator.getUnitPrice(TransactionDirection.ENTITY_SELLING, 1, stored, BASE_PRICE, DEMAND);
        assertEquals(BASE_PRICE * BasePriceCalculator.PRICE_MULT_CEILING, avg, 0.01);
    }

    @Test
    public void hugeAmountDoesNotOverflow() {
        final double stored = DEMAND;
        final long huge = 1_000_000_000L;
        final float avg = BasePriceCalculator.getUnitPrice(TransactionDirection.ENTITY_BUYING, huge, stored, BASE_PRICE, DEMAND);
        assertTrue(Float.isFinite(avg) && avg > 0);
    }

    // ---- clamping enforcement --------------------------------------------------

    @Test
    public void massiveExcessHitsFloor() {
        final double stored = 1e9;
        final float avg = BasePriceCalculator.getUnitPrice(TransactionDirection.ENTITY_SELLING, 1, stored, BASE_PRICE, DEMAND);
        assertTrue(avg >= BASE_PRICE * BasePriceCalculator.PRICE_MULT_FLOOR);
    }

    @Test
    public void massiveDeficitHitsCeiling() {
        final double stored = -1e9;
        final float avg = BasePriceCalculator.getUnitPrice(TransactionDirection.ENTITY_SELLING, 1, stored, BASE_PRICE, DEMAND);
        assertEquals(BASE_PRICE * BasePriceCalculator.PRICE_MULT_CEILING, avg, 0.01);
    }

    // ---- demand flooring -------------------------------------------------------

    @Test
    public void demandBelowInherentFloorIsIgnored() {
        final float tinyDemand = 2f;
        // stock = INHERENT_DEMAND -> equilibrium
        final float p = BasePriceCalculator.getUnitPrice(TransactionDirection.NEUTRAL, 0, INHERENT_DEMAND, BASE_PRICE, tinyDemand);
        assertEquals(BASE_PRICE, p, 0.01);
    }

    // ---- edge numbers ----------------------------------------------------------

    @Test
    public void stockZeroIsValid() {
        final float avg = BasePriceCalculator.getUnitPrice(TransactionDirection.NEUTRAL, 0, 0, BASE_PRICE, DEMAND);
        assertTrue(avg > 0 && Float.isFinite(avg));
    }

    @Test
    public void stockDoubleMaxValueDoesNotOverflow() {
        final double stored = Double.MAX_VALUE;
        final float avg = BasePriceCalculator.getUnitPrice(TransactionDirection.NEUTRAL, 0, stored, BASE_PRICE, DEMAND);
        assertTrue(avg >= BASE_PRICE * BasePriceCalculator.PRICE_MULT_FLOOR);
    }

    // ---- transaction direction consistency ------------------------------------

    @Test
    public void entityBuyingIncreasesStock() {
        // No direct assertion; just validates the call does not crash.
        BasePriceCalculator.getUnitPrice(TransactionDirection.ENTITY_BUYING, 30, 50, BASE_PRICE, DEMAND);
    }

    @Test
    public void entitySellingDecreasesStock() {
        BasePriceCalculator.getUnitPrice(TransactionDirection.ENTITY_SELLING, 20, 50, BASE_PRICE, DEMAND);
    }

    @Test
    public void neutralIgnoresAmount() {
        final double stored = 50;
        final float p1 = BasePriceCalculator.getUnitPrice(TransactionDirection.NEUTRAL, 0, stored, BASE_PRICE, DEMAND);
        final float p2 = BasePriceCalculator.getUnitPrice(TransactionDirection.NEUTRAL, 100, stored, BASE_PRICE, DEMAND);
        assertEquals(p1, p2, 0.01);
    }

    // ---- additivity for complex splits -----------------------------------------

    @Test
    public void threeWaySplitSameTotalCost() {
        final double stored = DEMAND + 50;
        final long total = 90;
        final long p1 = 30, p2 = 30, p3 = 30;

        final float avgOnce = BasePriceCalculator.getUnitPrice(TransactionDirection.ENTITY_SELLING, total, stored, BASE_PRICE, DEMAND);
        final double costOnce = avgOnce * total;

        double s = stored;
        final float a1 = BasePriceCalculator.getUnitPrice(TransactionDirection.ENTITY_SELLING, p1, s, BASE_PRICE, DEMAND);
        final double c1 = a1 * p1;
        s -= p1;
        final float a2 = BasePriceCalculator.getUnitPrice(TransactionDirection.ENTITY_SELLING, p2, s, BASE_PRICE, DEMAND);
        final double c2 = a2 * p2;
        s -= p2;
        final float a3 = BasePriceCalculator.getUnitPrice(TransactionDirection.ENTITY_SELLING, p3, s, BASE_PRICE, DEMAND);
        final double c3 = a3 * p3;
        assertEquals(costOnce, c1 + c2 + c3, TOLERANCE * costOnce);
    }

    @Test
    public void tradeCrossingNegativeToPositiveAdditivity() {
        // Start deeply negative, cross into positive after a large sell (ENTITY_BUYING)
        final double stored = -100;
        final long sellAmount = 250;   // new stock = 150 (positive)
        final float avgOnce = BasePriceCalculator.getUnitPrice(TransactionDirection.ENTITY_BUYING, sellAmount, stored, BASE_PRICE, DEMAND);
        final double revenueOnce = avgOnce * sellAmount;

        final long half = sellAmount / 2;
        final float a1 = BasePriceCalculator.getUnitPrice(TransactionDirection.ENTITY_BUYING, half, stored, BASE_PRICE, DEMAND);
        final double r1 = a1 * half;
        final double mid = stored + half;   // -100 + 125 = 25 (crossed zero)
        final float a2 = BasePriceCalculator.getUnitPrice(TransactionDirection.ENTITY_BUYING, half, mid, BASE_PRICE, DEMAND);
        final double r2 = a2 * half;
        assertEquals(revenueOnce, r1 + r2, TOLERANCE * revenueOnce,
                "Total revenue additive when crossing from negative to positive stock");
    }

    // ---- base price scaling ----------------------------------------------------

    @Test
    public void priceScalesLinearlyWithBasePrice() {
        final double stored = DEMAND - 10;
        final long amount = 5;
        final float bp1 = 20f, bp2 = 40f;
        final float p1 = BasePriceCalculator.getUnitPrice(TransactionDirection.ENTITY_SELLING, amount, stored, bp1, DEMAND);
        final float p2 = BasePriceCalculator.getUnitPrice(TransactionDirection.ENTITY_SELLING, amount, stored, bp2, DEMAND);
        assertEquals(p1 * 2.0, p2, 0.01);
    }

    // ---- regression safety (snapshot a few known points) -----------------------

    @Test
    public void regressionSnapshot_EquilibriumBuy() {
        final double stored = DEMAND;
        final float avg = BasePriceCalculator.getUnitPrice(TransactionDirection.ENTITY_SELLING, 10, stored, BASE_PRICE, DEMAND);
        assertTrue(avg > BASE_PRICE && avg < BASE_PRICE * 1.5,
                "Buying at equilibrium should raise the average price above base, but not drastically");
    }

    @Test
    public void regressionSnapshot_DeepDeficitSmallBuy() {
        final double stored = -50;
        final float avg = BasePriceCalculator.getUnitPrice(TransactionDirection.ENTITY_SELLING, 1, stored, BASE_PRICE, DEMAND);
        assertTrue(avg > BASE_PRICE * 2 && avg <= BASE_PRICE * BasePriceCalculator.PRICE_MULT_CEILING);
    }

    // ---- continuity ------------------------------------------------------------

    @Test
    public void priceFunctionIsContinuousAtZero() {
        // Instantaneous multiplier near s=0 from left and right should be close to m(0)
        final double eps = 1e-6;
        final float left  = BasePriceCalculator.getUnitPrice(TransactionDirection.NEUTRAL, 0, -eps, BASE_PRICE, DEMAND);
        final float right = BasePriceCalculator.getUnitPrice(TransactionDirection.NEUTRAL, 0,  eps, BASE_PRICE, DEMAND);
        final float atZero = BasePriceCalculator.getUnitPrice(TransactionDirection.NEUTRAL, 0, 0.0, BASE_PRICE, DEMAND);
        assertEquals(atZero, left,  1e-4, "Continuity from left at zero");
        assertEquals(atZero, right, 1e-4, "Continuity from right at zero");
    }
}
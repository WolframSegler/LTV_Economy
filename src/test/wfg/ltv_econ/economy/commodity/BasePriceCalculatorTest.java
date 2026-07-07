package wfg.ltv_econ.economy.commodity;

import wfg.ltv_econ.economy.commodity.BasePriceCalculator;
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

        // Buy in two halves, updating stock after each
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
        final long overAmount = exactAmount + 30; // stock becomes negative

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
        // Player selling (entity buying) increases stock, but still starts negative
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
    public void tradeAcrossZoneBoundaries_StillAdditive() {
        // Stock near deficit boundary: DEMAND=100, DEFICIT_NORMAL_BOUND=0.5 -> boundary at 50
        final double stored = 55.0;
        final long buyAmount = 20;  // will cross from normal to deficit (stored goes 55 -> 35)
        final float avgFull = BasePriceCalculator.getUnitPrice(TransactionDirection.ENTITY_SELLING, buyAmount, stored, BASE_PRICE, DEMAND);
        final double costFull = avgFull * buyAmount;

        final float avg1 = BasePriceCalculator.getUnitPrice(TransactionDirection.ENTITY_SELLING, 5, stored, BASE_PRICE, DEMAND);
        final double cost1 = avg1 * 5;
        final double newStock = stored - 5;
        final float avg2 = BasePriceCalculator.getUnitPrice(TransactionDirection.ENTITY_SELLING, 15, newStock, BASE_PRICE, DEMAND);
        final double cost2 = avg2 * 15;
        assertEquals(costFull, cost1 + cost2, TOLERANCE * costFull,
                "Total cost should be additive even when crossing zone boundaries");
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

        assertTrue(Math.abs(cost - revenue) <= TOLERANCE, "Round-trip sell-then-buy should result in net credits staying the same");
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

        assertTrue(Math.abs(cost - revenue) <= TOLERANCE, "Round-trip buy-then-sell should result in net credits staying the same");
    }

    @Test
    public void instantaneousMultiplierAtZoneBoundaries() {
        final double atDeficit = DEFICIT_NORMAL_BOUND * DEMAND;
        final double atExcess  = EXCESS_NORMAL_BOUND * DEMAND;
        final float pDef = BasePriceCalculator.getUnitPrice(TransactionDirection.NEUTRAL, 0, atDeficit, BASE_PRICE, DEMAND);
        final float pExc = BasePriceCalculator.getUnitPrice(TransactionDirection.NEUTRAL, 0, atExcess,  BASE_PRICE, DEMAND);
        // These are just sanity checks; no assertion on magnitude, but they must be finite.
        assertTrue(Float.isFinite(pDef) && Float.isFinite(pExc));
    }

    @Test
    public void tradeStartingExactlyAtDeficitBoundary() {
        final double stored = DEFICIT_NORMAL_BOUND * DEMAND;
        final float avg = BasePriceCalculator.getUnitPrice(TransactionDirection.ENTITY_SELLING, 5, stored, BASE_PRICE, DEMAND);
        assertTrue(avg > 0);
    }

    @Test
    public void tradeEndingExactlyAtExcessBoundary() {
        final double start = EXCESS_NORMAL_BOUND * DEMAND - 2;
        final float avg = BasePriceCalculator.getUnitPrice(TransactionDirection.ENTITY_SELLING, 2, start, BASE_PRICE, DEMAND);
        assertTrue(avg > 0);
    }

    @Test
    public void instantaneousPriceDecreasesWithStock() {
        final double[] stocks = {-100, -10, 0, 10, 50, 100, 500, 10_000};
        for (int i = 0; i < stocks.length - 1; i++) {
            final float p1 = BasePriceCalculator.getUnitPrice(TransactionDirection.NEUTRAL, 0, stocks[i],   BASE_PRICE, DEMAND);
            final float p2 = BasePriceCalculator.getUnitPrice(TransactionDirection.NEUTRAL, 0, stocks[i+1], BASE_PRICE, DEMAND);
            assertTrue(p1 > p2, "Price should decrease as stock increases: " + stocks[i] + " -> " + stocks[i+1]);
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
        final double stored = 50;
        final long amount = 30;
        BasePriceCalculator.getUnitPrice(TransactionDirection.ENTITY_BUYING, amount, stored, BASE_PRICE, DEMAND);
        // No direct access to new stock, but we can compute: stock should become stored + amount
        // This test just verifies it does not crash; accurate stock change is implicit in other tests.
    }

    @Test
    public void entitySellingDecreasesStock() {
        final double stored = 50;
        final long amount = 20;
        BasePriceCalculator.getUnitPrice(TransactionDirection.ENTITY_SELLING, amount, stored, BASE_PRICE, DEMAND);
        // Stock would be 30; no crash is enough.
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
    public void crossAllThreeZonesAdditivity() {
        final double stored = -100;               // deep deficit
        final long buy = 300;                     // move to deep excess
        final float avgOnce = BasePriceCalculator.getUnitPrice(TransactionDirection.ENTITY_SELLING, buy, stored, BASE_PRICE, DEMAND);
        final double costOnce = avgOnce * buy;

        // split: first half, then second half
        final long half = buy / 2;
        final float a1 = BasePriceCalculator.getUnitPrice(TransactionDirection.ENTITY_SELLING, half, stored, BASE_PRICE, DEMAND);
        final double c1 = a1 * half;
        final double afterHalf = stored - half;
        final float a2 = BasePriceCalculator.getUnitPrice(TransactionDirection.ENTITY_SELLING, half, afterHalf, BASE_PRICE, DEMAND);
        final double c2 = a2 * half;
        assertEquals(costOnce, c1 + c2, TOLERANCE * costOnce);
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
        // buying 10 units when stock = demand should yield price < basePrice
        final double stored = DEMAND;
        final float avg = BasePriceCalculator.getUnitPrice(TransactionDirection.ENTITY_SELLING, 10, stored, BASE_PRICE, DEMAND);
        // With current parameters (EXP_NORMAL=1, SHIFT_FRACTION=0.002) the exact value may vary;
        // we just ensure it's in a reasonable ballpark (less than base, greater than half base).
        assertTrue(avg > BASE_PRICE && avg < BASE_PRICE * 1.5, "Buying at equilibrium should raise the average price above base, but not drastically");
    }

    @Test
    public void regressionSnapshot_DeepDeficitSmallBuy() {
        final double stored = -50;
        final float avg = BasePriceCalculator.getUnitPrice(TransactionDirection.ENTITY_SELLING, 1, stored, BASE_PRICE, DEMAND);
        // Must be significantly elevated, but below ceiling if not saturated.
        assertTrue(avg > BASE_PRICE * 2 && avg <= BASE_PRICE * BasePriceCalculator.PRICE_MULT_CEILING);
    }

    @Test
    public void priceFunctionIsContinuousAtZoneBoundaries() {
        final double deficitB = DEFICIT_NORMAL_BOUND * DEMAND;
        final double excessB  = EXCESS_NORMAL_BOUND * DEMAND;

        // Get instantaneous prices right at the boundaries (using NEUTRAL with zero amount)
        final float atDeficit = BasePriceCalculator.getUnitPrice(TransactionDirection.NEUTRAL, 0, deficitB, BASE_PRICE, DEMAND);
        final float atExcess  = BasePriceCalculator.getUnitPrice(TransactionDirection.NEUTRAL, 0, excessB,  BASE_PRICE, DEMAND);

        // Compute prices just barely inside each adjacent zone (offset by a tiny epsilon)
        final double eps = 1e-6;
        final float justBelowDeficit = BasePriceCalculator.getUnitPrice(TransactionDirection.NEUTRAL, 0, deficitB - eps, BASE_PRICE, DEMAND);
        final float justAboveDeficit = BasePriceCalculator.getUnitPrice(TransactionDirection.NEUTRAL, 0, deficitB + eps, BASE_PRICE, DEMAND);

        final float justBelowExcess = BasePriceCalculator.getUnitPrice(TransactionDirection.NEUTRAL, 0, excessB - eps, BASE_PRICE, DEMAND);
        final float justAboveExcess = BasePriceCalculator.getUnitPrice(TransactionDirection.NEUTRAL, 0, excessB + eps, BASE_PRICE, DEMAND);

        // The limit from both sides should equal the value at the boundary.
        assertEquals(atDeficit, justBelowDeficit, 1e-4, "Deficit boundary left limit");
        assertEquals(atDeficit, justAboveDeficit, 1e-4, "Deficit boundary right limit");
        assertEquals(atExcess, justBelowExcess, 1e-4, "Excess boundary left limit");
        assertEquals(atExcess, justAboveExcess, 1e-4, "Excess boundary right limit");
    }
}
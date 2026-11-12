package wfg.ltv_econ.economy.commodity;

/**
 * Price calculator for trade with tunable parameters. The price function m(s) is
 * piecewise-defined, continuous, monotonically decreasing, and bounded by
 * {@link #PRICE_MULT_FLOOR} < m(s) < {@link #PRICE_MULT_CEILING} for all real s, where s = stock/demand.
 * 
 * The function passes through three anchors:
 * <ul>
 *   <li>m({@link #DEFICIT_ANCHOR_RATIO} × {@code demand}) = {@link #DEFICIT_ANCHOR_MULT}</li>
 *   <li>m({@code demand}) = {@code 1} (stock equals demand -> base price)</li>
 *   <li>m({@link #EXCESS_ANCHOR_RATIO} × {@code demand}) = {@link #EXCESS_ANCHOR_MULT}</li>
 * </ul>
 * <p>
 *
 *   <p>
 *   <p>
 *   <p>
 * <p>
 * For s ≥ {@link #DEFICIT_ANCHOR_RATIO} the multiplier is a shifted power law blended with an additive floor,
 * asymptotically approaching {@link #PRICE_MULT_FLOOR} as s -> ∞. For s < {@link #DEFICIT_ANCHOR_RATIO} it is a
 * saturating exponential, asymptotically approaching {@link #PRICE_MULT_CEILING} as s -> -∞.
 * Neither asymptote is ever reached.
 * <p>
 * The integral over any [a, b] is closed-form (see {@link #integrateMultiplier}), which
 * makes the average price for a transaction path-independent: splitting a trade
 * into N sub-trades yields the same total cost as a single trade.
 */
public final class BasePriceCalculator {
    private BasePriceCalculator() {};

    /* ------------------------ TUNABLE START ------------------------ */

    /** Stock/demand ratio at the deficit anchor (m = {@link #DEFICIT_ANCHOR_MULT}). */
    static final double DEFICIT_ANCHOR_RATIO = 0d;
    /** Stock/demand ratio at the excess anchor (m = {@link #EXCESS_ANCHOR_MULT}). */
    static final double EXCESS_ANCHOR_RATIO = 6d;

    /** Multiplier when stock is at {@link #DEFICIT_ANCHOR_RATIO}. */
    static final double DEFICIT_ANCHOR_MULT = 2.5d;
    /** Multiplier when stock is at {@link #EXCESS_ANCHOR_RATIO}. */
    static final double EXCESS_ANCHOR_MULT = 0.35d;

    /** Maximum price multiplier used by {@link #multiplier}. Never reached exactly. */
    static final double PRICE_MULT_CEILING = 3.5d;
    /** Minimum price multiplier used by {@link #multiplier}. Never reached exactly. */
    static final double PRICE_MULT_FLOOR = 0.2d;

    /** Minimum demand used for price calculations. Must be > {@code 0d}. */
    static final double INHERENT_DEMAND = 100d;

    /* ------------------------ TUNABLE END ------------------------ */

    static final double EXCESS_EXPONENT;
    static final double NEGATIVE_TAIL_FACTOR;
    static final double BLEND_SHIFT_FRACTION;

    private static final boolean TUNABLES_CORRECT =
        DEFICIT_ANCHOR_RATIO < 1d &&
        EXCESS_ANCHOR_RATIO > 1d &&
        DEFICIT_ANCHOR_MULT > 1d &&
        EXCESS_ANCHOR_MULT < 1d &&
        DEFICIT_ANCHOR_MULT > EXCESS_ANCHOR_MULT &&
        DEFICIT_ANCHOR_MULT < PRICE_MULT_CEILING &&
        EXCESS_ANCHOR_MULT > PRICE_MULT_FLOOR &&
        PRICE_MULT_FLOOR > 0d &&
        INHERENT_DEMAND >= 1d;

    static {
        if (!TUNABLES_CORRECT) throw new IllegalArgumentException("Parameters incorrectly tuned");

        final double F = PRICE_MULT_FLOOR;
        final double C = PRICE_MULT_CEILING;
        final double m0 = DEFICIT_ANCHOR_MULT;
        final double m1 = EXCESS_ANCHOR_MULT;
        final double r0 = DEFICIT_ANCHOR_RATIO;
        final double r1 = EXCESS_ANCHOR_RATIO;

        final double P1 = (1d - F) / (m0 - F);
        final double P2 = (m1 - F) / (m0 - F);
        final double target = Math.log(P1) / Math.log(P2);

        final double gInf = (1d - r0) / (r1 - r0);
        if (target <= gInf) {
            throw new IllegalArgumentException(
                "No solution for blend shift: target=" + target + " must exceed limit=" + gInf
                + " (adjust DEFICIT_BOUND/EXCESS_BOUND/MULT constants)");
        }

        double lo = 1e-6, hi = 1e6;
        for (int i = 0; i < 100; i++) {
            final double mid = Math.sqrt(lo * hi);
            final double g = Math.log((r0 + mid) / (1d + mid))
                / Math.log((r0 + mid) / (r1 + mid));
            if (g > target) lo = mid; else hi = mid;
        }

        BLEND_SHIFT_FRACTION = Math.sqrt(lo * hi);
        EXCESS_EXPONENT = Math.log(P1) / Math.log((r0 + BLEND_SHIFT_FRACTION) / (1d + BLEND_SHIFT_FRACTION));
        NEGATIVE_TAIL_FACTOR = (m0 - F) * EXCESS_EXPONENT / ((C - m0) * (r0 + BLEND_SHIFT_FRACTION));
    }

    /**
     * Computes the per‑unit price for a transaction of {@code amount} units. This function is symmetric and directionless.
     *
     * @param type Direction of the trade
     * @param amount Number of units to transact (positive)
     * @param stored Current stockpile (may be negative)
     * @param basePrice Base price when stockpile equals demand
     * @param preferred The demand value.
     * @return the average unit price for this transaction.
     */
    public static final float getUnitPrice(TransactionDirection type, long amount, double stored, float basePrice, double preferred) {
        if (amount < 0l) throw new IllegalArgumentException("Amount cannot be negative: " + amount);

        final double d = Math.max(preferred, INHERENT_DEMAND);

        if (amount == 0l || type == TransactionDirection.NEUTRAL) {
            final double mult = multiplier(stored, d);
            return Math.max(1f, basePrice * (float) mult);
        }

        final double deltaStock = (type == TransactionDirection.ENTITY_BUYING) ? amount : -amount;
        final double newStock = stored + deltaStock;

        final double lower = Math.min(stored, newStock);
        final double upper = Math.max(stored, newStock);

        final double integralMult = integrateMultiplier(lower, upper, d);
        final double avgMult = integralMult / Math.abs(deltaStock);

        return Math.max(1f, basePrice * (float) avgMult);
    }

    private static final double multiplier(double s, double demand) {
        final double F = PRICE_MULT_FLOOR;
        final double m0 = DEFICIT_ANCHOR_MULT;
        final double r0 = DEFICIT_ANCHOR_RATIO;
        final double r0d = r0 * demand;

        if (s < r0d) {
            final double C = PRICE_MULT_CEILING;
            return C - (C - m0) * Math.exp(NEGATIVE_TAIL_FACTOR * (s - r0d) / demand);
        }
        final double sh = BLEND_SHIFT_FRACTION * demand;
        return F + (m0 - F) * Math.pow((r0d + sh) / (s + sh), EXCESS_EXPONENT);
    }

    private static final double integrateMultiplier(double lo, double hi, double demand) {
        final double r0d = DEFICIT_ANCHOR_RATIO * demand;
        if (hi <= r0d) return expIntegral(lo, hi, demand);
        if (lo >= r0d) return excessIntegral(lo, hi, demand);
        return expIntegral(lo, r0d, demand) + excessIntegral(r0d, hi, demand);
    }

    private static final double expIntegral(double lo, double hi, double demand) {
        final double C = PRICE_MULT_CEILING;
        final double m0 = DEFICIT_ANCHOR_MULT;
        final double NTF = NEGATIVE_TAIL_FACTOR;
        final double r0d = DEFICIT_ANCHOR_RATIO * demand;
        return C * (hi - lo)
            - (C - m0) * (demand / NTF)
            * (Math.exp(NTF * (hi - r0d) / demand) - Math.exp(NTF * (lo - r0d) / demand));
    }

    private static final double excessIntegral(double lo, double hi, double demand) {
        final double F = PRICE_MULT_FLOOR;
        final double m0 = DEFICIT_ANCHOR_MULT;
        final double E = EXCESS_EXPONENT;
        final double sh = BLEND_SHIFT_FRACTION * demand;
        final double shift = DEFICIT_ANCHOR_RATIO * demand + sh;

        final double linearPart = F * (hi - lo);
        final double powerPart;
        if (Math.abs(E - 1d) < 1e-9) {
            powerPart = (m0 - F) * shift * Math.log((hi + sh) / (lo + sh));
        } else {
            powerPart = (m0 - F) * Math.pow(shift, E) / (1d - E)
                * (Math.pow(hi + sh, 1d - E) - Math.pow(lo + sh, 1d - E));
        }
        return linearPart + powerPart;
    }

    public enum TransactionDirection {
        /** Buying from the player. Stock increases. */
        ENTITY_BUYING,
        /** Selling to the player. Stock decreases. */
        ENTITY_SELLING,
        /** Internal baseline */
        NEUTRAL 
    }
}
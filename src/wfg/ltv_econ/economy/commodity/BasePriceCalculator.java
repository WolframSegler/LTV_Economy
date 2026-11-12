package wfg.ltv_econ.economy.commodity;

/**
 * Price calculator for trade with tunable parameters. The price function is piecewise‑defined,
 * continuous, monotonic, decreasing, symmetric and positive for all real s.
 * <p>
 * For s >= 0 the multiplier follows a shifted power law that passes through the two anchor points.
 * For s < 0 the multiplier extends smoothly using a decaying exponential.
 */
public final class BasePriceCalculator {
    private BasePriceCalculator() {};

    // ------------------------ TUNABLE START ------------------------

    /** Stock/demand ratio with maximum deficit. */
    static final double ABSOLUTE_DEFICIT_BOUND = 0d;
    /** Stock/demand ratio with maximum excess. */
    static final double ABSOLUTE_EXCESS_BOUND = 6d;

    /** Multiplier when stock is at {@link #ABSOLUTE_DEFICIT_BOUND}. */
    static final double ABSOLUTE_DEFICIT_MULT = 2.5d;
    /** Multiplier when stock is at {@link #ABSOLUTE_EXCESS_BOUND}. */
    static final double ABSOLUTE_EXCESS_MULT = 0.35d;

    // ------------------------ TUNABLE END ------------------------

    static final double EXPONENT;
    static final double SHIFT_FRACTION;
    static final double NEGATIVE_TAIL_FACTOR;

    static final float INHERENT_DEMAND = 100f;
    static final double PRICE_MULT_FLOOR = 0.2d;
    static final double PRICE_MULT_CEILING = 5d;

    static {
        final double m0 = ABSOLUTE_DEFICIT_MULT;
        final double m1 = ABSOLUTE_EXCESS_MULT;
        final double r0 = ABSOLUTE_DEFICIT_BOUND;
        final double r1 = ABSOLUTE_EXCESS_BOUND;

        // solve: ln(m0)/ln((1+f)/(r0+f)) == ln(m1)/ln((1+f)/(r1+f))
        // bisection on f ∈ [0.0001, 10]
        double lo = 0.0001, hi = 10d;
        for (int i = 0; i < 80; i++) {
            final double mid = (lo + hi) * 0.5;
            final double L0 = Math.log((1 + mid) / (r0 + mid));
            final double L4 = Math.log((1 + mid) / (r1 + mid));
            final double left = Math.log(m0) / L0;
            final double right = Math.log(m1) / L4;
            if (left < right) {
                lo = mid;
            } else {
                hi = mid;
            }
        }
        SHIFT_FRACTION = (lo + hi) * 0.5;

        final double L0 = Math.log((1d + SHIFT_FRACTION) / (r0 + SHIFT_FRACTION));
        EXPONENT = Math.log(m0) / L0;

        NEGATIVE_TAIL_FACTOR = EXPONENT * ABSOLUTE_DEFICIT_MULT
            / (SHIFT_FRACTION * (PRICE_MULT_CEILING - ABSOLUTE_DEFICIT_MULT));
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
    public static final float getUnitPrice(TransactionDirection type, long amount, double stored, float basePrice, float preferred) {
        if (amount < 0l) throw new IllegalArgumentException("Amount cannot be negative: " + amount);

        final float d = Math.max(preferred, INHERENT_DEMAND);
        final double shift = SHIFT_FRACTION * d;

        if (amount == 0l || type == TransactionDirection.NEUTRAL) {
            final double mult = multiplier(stored, d, shift);
            return Math.max(1f, basePrice * (float) clampMult(mult));
        }

        final double deltaStock = (type == TransactionDirection.ENTITY_BUYING) ? amount : -amount;
        final double newStock = stored + deltaStock;

        final double lower = Math.min(stored, newStock);
        final double upper = Math.max(stored, newStock);

        final double integralMult = integrateMultiplier(lower, upper, d, shift);
        final double avgMult = integralMult / Math.abs(deltaStock);
        final double clamped = clampMult(avgMult);

        return Math.max(1f, basePrice * (float) clamped);
    }

    private static final double clampMult(double mult) {
        return Math.max(PRICE_MULT_FLOOR, mult);
        // return Math.max(PRICE_MULT_FLOOR, Math.min(PRICE_MULT_CEILING, mult));
    }

    private static final double multiplier(double s, float demand, double shift) {
        if (s < 0) {
            final double C = PRICE_MULT_CEILING;
            final double m0 = ABSOLUTE_DEFICIT_MULT;
            // Bounded below C for all s < 0; no min() needed.
            return C - (C - m0) * Math.exp(NEGATIVE_TAIL_FACTOR * s / demand);
        }
        final double ratio = (demand + shift) / (s + shift);
        return Math.pow(ratio, EXPONENT);
    }

    /**
     * Integrates the multiplier over [a, b], splitting at 0 if necessary
     * so that the exponential and power‑law branches are handled correctly.
     */
    private static final double integrateMultiplier(double a, double b, float demand, double shift) {
        if (b <= 0) {
            return expIntegral(a, b, demand);
        } else {
            final double K = Math.pow(demand + shift, EXPONENT);
            return a >= 0d ? powerIntegral(a, b, shift, EXPONENT, K) :
                expIntegral(a, 0d, demand) + powerIntegral(0d, b, shift, EXPONENT, Math.pow(demand + shift, EXPONENT));
        }
    }

    private static final double expIntegral(double a, double b, float demand) {
        final double C = PRICE_MULT_CEILING;
        final double m0 = ABSOLUTE_DEFICIT_MULT;
        final double NTF = NEGATIVE_TAIL_FACTOR;
        return C * (b - a)
            - (C - m0) * (demand / NTF)
            * (Math.exp(NTF * b / demand) - Math.exp(NTF * a / demand));
    }

    /** ∫_a^b K * (s + shift)^{-exp} ds for exp != 1 (and exp == 1 handled). */
    private static final double powerIntegral(double a, double b, double shift, double exp, double K) {
        if (exp == 1d) {
            return K * Math.log((b + shift) / (a + shift));
        } else {
            final double powA = Math.pow(a + shift, 1d - exp);
            final double powB = Math.pow(b + shift, 1d - exp);
            return K / (1d - exp) * (powB - powA);
        }
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
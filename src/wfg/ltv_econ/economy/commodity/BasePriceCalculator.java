package wfg.ltv_econ.economy.commodity;

import java.util.ArrayList;
import java.util.Collections;

/**
 * Price calculator for trade with tunable parameters. The price function is piecewise‑defined,
 * continuous, monotonic, decreasing, symmetric and positive for all real s.
 * <p>
 */
public class BasePriceCalculator {
    private BasePriceCalculator() {}

    // ------------------------ TUNABLE START ------------------------

    /** Stock/demand ratio below which the deficit zone starts. */
    static final double DEFICIT_NORMAL_BOUND = 0.7;
    /** Stock/demand ratio above which the excess zone starts. */
    static final double EXCESS_NORMAL_BOUND  = 1.3;
    /** Stock/demand ratio where the deficit zone ends. */
    static final double ABSOLUTE_DEFICIT_BOUND = 0.0;
    /** Stock/demand ratio where the excess zone ends. */
    static final double ABSOLUTE_EXCESS_BOUND = 4.0;

    /** Multiplier when stock is at {@link #DEFICIT_NORMAL_BOUND}. */
    static final double DEFICIT_NORMAL_MULT = 1.2;
    /** Multiplier when stock is at {@link #EXCESS_NORMAL_BOUND}. */
    static final double EXCESS_NORMAL_MULT = 0.8;
    /** Multiplier when stock is at {@link #ABSOLUTE_DEFICIT_BOUND}. */
    static final double ABSOLUTE_DEFICIT_MULT = 2.2;
    /** Multiplier when stock is at {@link #ABSOLUTE_EXCESS_BOUND}. */
    static final double ABSOLUTE_EXCESS_MULT = 0.3;

    // ------------------------ TUNABLE END ------------------------

    static final double EXP_NORMAL;
    static final double LAMBDA_FACTOR;
    static final double EXP_EXCESS;
    static final double SHIFT_FRACTION;

    static final float INHERENT_DEMAND = 100f;
    static final float PRICE_MULT_FLOOR = 0.01f;
    static final float PRICE_MULT_CEILING = 100.0f;
    static final double MAX_MULT = Double.MAX_VALUE;
    static final double MAX_EXP_ARG = 6.0;

    static {
        final double r1 = DEFICIT_NORMAL_BOUND;
        final double r2 = EXCESS_NORMAL_BOUND;
        final double m1 = DEFICIT_NORMAL_MULT;
        final double m2 = EXCESS_NORMAL_MULT;

        // solve: ln(m1)/ln((1+f)/(r1+f)) == ln(m2)/ln((1+f)/(r2+f))
        // bisection on f ∈ [0.0001, 10.0]
        double lo = 0.0001, hi = 10.0;
        for (int i = 0; i < 60; i++) {
            double mid = (lo + hi) * 0.5;
            double L1 = Math.log((1 + mid) / (r1 + mid));
            double L2 = Math.log((1 + mid) / (r2 + mid));
            double left  = Math.log(m1) / L1;
            double right = Math.log(m2) / L2;
            if (left < right) {
                hi = mid;
            } else {
                lo = mid;
            }
        }
        SHIFT_FRACTION = (lo + hi) * 0.5;

        final double L1 = Math.log((1 + SHIFT_FRACTION) / (r1 + SHIFT_FRACTION));
        EXP_NORMAL = Math.log(m1) / L1;

        final double rDefAbs = ABSOLUTE_DEFICIT_BOUND;
        LAMBDA_FACTOR = -Math.log(ABSOLUTE_DEFICIT_MULT / m1) / (rDefAbs - r1);

        final double rExcAbs = ABSOLUTE_EXCESS_BOUND;
        final double ratioExc = (r2 + SHIFT_FRACTION) / (rExcAbs + SHIFT_FRACTION);
        EXP_EXCESS = Math.log(ABSOLUTE_EXCESS_MULT / m2) / Math.log(ratioExc);
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

        if (amount == 0l || type == TransactionDirection.NEUTRAL) {
            return (float) Math.max(1.0, basePrice * p(stored, d));
        }

        final double deltaStock = (type == TransactionDirection.ENTITY_BUYING) ? amount : -amount;
        final double newStock = stored + deltaStock;

        final double lower = Math.min(stored, newStock);
        final double upper = Math.max(stored, newStock);

        // integral of the multiplier function over the stock change
        final double integralMult = integrate(lower, upper, d);

        final double avgMult = integralMult / Math.abs(deltaStock);
        final float clampedMult = (float) clampMult(avgMult);

        return Math.max(1f, basePrice * clampedMult);
    }

    private static final double clampMult(double mult) {
        return Math.max(PRICE_MULT_FLOOR, Math.min(PRICE_MULT_CEILING, mult));
    }

    /** Piecewise multiplier function m(s). */
    private static final double p(double stock, float demand) {
        final double deficitBound = DEFICIT_NORMAL_BOUND * demand;
        final double excessBound = EXCESS_NORMAL_BOUND * demand;

        final double raw;
        if (stock <= deficitBound) {
            final double mAtBoundary = normalMultiplier(deficitBound, demand);
            final double arg = -LAMBDA_FACTOR * (stock - deficitBound);

            raw = mAtBoundary * Math.exp(arg);
            // raw = mAtBoundary * Math.exp(arg > MAX_EXP_ARG ? MAX_EXP_ARG : arg);
        } else if (stock >= excessBound) {
            raw = excessMultiplier(stock, demand);
        } else {
            raw = normalMultiplier(stock, demand);
        }

        return Math.min(raw, MAX_MULT);
    }

    /** Normal zone multiplier: m(s) = ((d+shift)/(s+shift))^EXP_NORMAL */
    private static final double normalMultiplier(double s, float demand) {
        final double shift = SHIFT_FRACTION * demand;
        final double ratio = (demand + shift) / (s + shift);
        return Math.pow(ratio, EXP_NORMAL);
    }

    /** Excess zone multiplier, continuous with normal at excessBound. */
    private static final double excessMultiplier(double s, float demand) {
        final double excessBound = EXCESS_NORMAL_BOUND * demand;
        final double shift = SHIFT_FRACTION * demand;

        final double ratio = (excessBound + shift) / (s + shift);
        return normalMultiplier(excessBound, demand) * Math.pow(ratio, EXP_EXCESS);
    }

    /** Integral of the multiplier function over [a, b]. */
    private static final double integrate(double a, double b, float demand) {
        if (a == b) return 0.0;

        final double deficitBound = DEFICIT_NORMAL_BOUND * demand;
        final double excessBound = EXCESS_NORMAL_BOUND * demand;

        final ArrayList<Double> points = new ArrayList<>(4);
        points.add(a);
        points.add(b);
        if (deficitBound > a && deficitBound < b) points.add(deficitBound);
        if (excessBound  > a && excessBound  < b) points.add(excessBound);
        Collections.sort(points);

        double total = 0.0;
        for (int i = 0; i < points.size() - 1; i++) {
            final double x0 = points.get(i);
            final double x1 = points.get(i + 1);
            if (x0 == x1) continue;

            final double midpoint = (x0 + x1) * 0.5;
            final IntegralRegion zone = midpoint <= deficitBound ? IntegralRegion.DEFICIT :
                    midpoint >= excessBound ? IntegralRegion.EXCESS : IntegralRegion.NORMAL;

            total += integrateMultiplierBranch(x0, x1, zone, demand);
        }
        return total;
    }

    /** Integral of the multiplier over [a, b] assuming one zone. */
    private static final double integrateMultiplierBranch(double a, double b, IntegralRegion zone, float demand) {
        final double shift = SHIFT_FRACTION * demand;
        switch (zone) {
            case NORMAL: {
                // ∫ (d+shift)^EXP_NORMAL * (s+shift)^{-EXP_NORMAL} ds
                final double K = Math.pow(demand + shift, EXP_NORMAL);
                return powerIntegral(a, b, shift, EXP_NORMAL, K);
            }
            case EXCESS: {
                final double excessBound = EXCESS_NORMAL_BOUND * demand;
                final double mAtBoundary = normalMultiplier(excessBound, demand);
                final double K = mAtBoundary * Math.pow(excessBound + shift, EXP_EXCESS);
                return powerIntegral(a, b, shift, EXP_EXCESS, K);
            }
            case DEFICIT: {
                final double deficitBound = DEFICIT_NORMAL_BOUND * demand;
                final double mDef = normalMultiplier(deficitBound, demand);
                final double lambdaOverD = LAMBDA_FACTOR / demand;

                // stock where raw exponential reaches MAX_MULT
                final double satStock = deficitBound - Math.log(MAX_MULT / mDef) / lambdaOverD;

                double total = 0.0;
                double lower = a, upper = b;

                // capped region
                if (lower < satStock) {
                    final double end = Math.min(upper, satStock);
                    total += MAX_MULT * (end - lower);
                    lower = end;
                }

                // exponential region
                if (lower < upper) {
                    final double argLower = -lambdaOverD * (lower - deficitBound);
                    final double argUpper = -lambdaOverD * (upper - deficitBound);
                    total += (mDef / lambdaOverD) * (Math.exp(argLower) - Math.exp(argUpper));
                }

                return total;
            }
            default:
                throw new IllegalArgumentException("Unhandled: " + zone.name());
        }
    }

    /** ∫_a^b K * (s + shift)^{-exp} ds for exp != 1. */
    private static final double powerIntegral(double a, double b, double shift, double exp, double K) {
        if (exp == 1.0) {
            return K * Math.log((b + shift) / (a + shift));
        } else {
            final double powA = Math.pow(a + shift, 1.0 - exp);
            final double powB = Math.pow(b + shift, 1.0 - exp);
            return K / (1.0 - exp) * (powB - powA);
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

    public enum IntegralRegion {
        DEFICIT,
        EXCESS,
        NORMAL
    }
}
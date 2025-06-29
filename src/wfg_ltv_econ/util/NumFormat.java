package wfg_ltv_econ.util;

import java.text.DecimalFormat;

public class NumFormat {

    private static final String[] SUFFIXES = {"", "K", "M", "B", "T", "P", "E"};

    /**
     * Format a positive number so that:
     *  - If it has less than 4 digits, it’s printed in full.
     *  - Otherwise it’s printed in engineer notation with 3 significant digits.
     *
     * Examples:
     *   924 -> "924"
     *   9,245 -> "9.25K"
     *   79,245 -> "79.2K"
     *   1,000,000,000 -> "1.00B"
     */
    public static final String engNotation(long input) {

        long value = Math.abs(input);

        if (value < 1000) {
            return Long.toString(value);
        }

        int suffix = (int)(Math.log10(value) / 3);
        suffix = Math.min(suffix, SUFFIXES.length - 1);
        double scaled = value / Math.pow(1000, suffix);

        int intDigits = (int)Math.floor(Math.log10(scaled)) + 1;
        intDigits = Math.max(1, Math.min(3, intDigits));
        int decimals = 3 - intDigits;

        StringBuilder pattern = new StringBuilder("#");
        if (decimals > 0) {
            pattern.append(".");
            pattern.append("#".repeat(decimals));
        }

        DecimalFormat df = new DecimalFormat(pattern.toString());
        if (input < 0) {
            return "\u2212" + df.format(scaled) + SUFFIXES[suffix]; // large minus sign
        }
        return df.format(scaled) + SUFFIXES[suffix];
    }

    public static final int firstDigit(int x) {
		while (x > 9) {
			x /= 10;
		}
		return x;
	}

    public static int min(int... values) {
        if (values == null || values.length == 0) {
            throw new IllegalArgumentException("At least one value must be provided");
        }
        int min = values[0];
        for (int i = 1; i < values.length; i++) {
            if (values[i] < min) {
                min = values[i];
            }
        }
        return min;
    }

    public static float min(float... values) {
        if (values == null || values.length == 0) {
            throw new IllegalArgumentException("At least one value must be provided");
        }
        float min = values[0];
        for (int i = 1; i < values.length; i++) {
            if (values[i] < min) {
                min = values[i];
            }
        }
        return min;
    }
}

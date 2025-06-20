package wfg_ltv_econ.util;

import java.text.DecimalFormat;

public class LtvNumFormat {

    private static final String[] SUFFIXES = {"", "K", "M", "B", "T", "P", "E"};

    /**
     * Format a positive number so that:
     *  - If it has less than 4 digits, it’s printed in full.
     *  - Otherwise it’s printed in engineer notation with 3 significant digits.
     *
     * Examples:
     *   924 -> "924"
     *   9245 -> "9.25K"
     *   79245 -> "79.2K"
     *   1_000_000_000 -> "1.00B"
     */
    public static final String formatWithMaxDigits(long input) {

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
}

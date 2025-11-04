package wfg.wrap_ui.util;

import java.text.DecimalFormat;

import com.fs.starfarer.api.impl.campaign.ids.Strings;

public class NumFormat {

    private static final String[] LARGE_SUFFIXES = {"", "K", "M", "B", "T", "P", "E"};
    private static final String[] SMALL_SUFFIXES = {"", "m", "μ", "n", "p", "f", "a"};

    /**
     * <p>Formats a positive number according to the following rules:</p>
     * 
     * <ul>
     *     <li>If the number has less than 4 digits, it is printed in full.</li>
     *     <li>Otherwise, it is printed in <b>engineer notation</b> with 3 significant digits.</li>
     * </ul>
     * 
     * <p>Examples:</p>
     * <ul>
     *     <li><code>924</code> → <code>"924"</code></li>
     *     <li><code>9,245</code> → <code>"9.25K"</code></li>
     *     <li><code>79,245</code> → <code>"79.2K"</code></li>
     *     <li><code>1,024,000,000</code> → <code>"1.02B"</code></li>
     * </ul>
     * @return a <code>String</code> containing the formatted number.
     */
    public static final String engNotation(long input) {

        long value = Math.abs(input);

        if (value < 1000) {
            final String prefix = input < 0 ? "\u2212" : "";
            return prefix + Long.toString(value);
        }

        int suffix = (int)(Math.log10(value) / 3);
        suffix = Math.min(suffix, LARGE_SUFFIXES.length - 1);
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
            return "\u2212" + df.format(scaled) + LARGE_SUFFIXES[suffix]; // large minus sign
        }
        return df.format(scaled) + LARGE_SUFFIXES[suffix];
    }

    /**
     * <p>Formats a multiplier into readable notation.</p>
     * 
     * <ul>
     *     <li><b>Large multipliers:</b> displayed normally (e.g., <code>×1.3</code>, <code>×2.5</code>).</li>
     *     <li><b>Tiny multipliers near 1:</b> displayed using reverse-engineering notation (e.g., <code>1.0m</code> represents <code>1 + 0.001</code>).</li>
     * </ul>
     *
     * <p>This method ensures that very small deviations from 1 are readable, while larger multipliers retain standard formatting.</p>
     *
     * @return a <code>String</code> containing the formatted multiplier.
     */
    public static final String reverseEngNotation(float multiplier) {
        if (multiplier <= 0 || multiplier >= 1.01) return "" + multiplier;

        float delta = multiplier - 1f;

        int exp = 0;
        while (Math.abs(delta) < 0.001 && exp < SMALL_SUFFIXES.length - 1) {
            delta *= 1000.0;
            exp++;
        }

        return String.format("1.%.0f%s", Math.round(delta * 10) / 10.0, SMALL_SUFFIXES[exp]);
    }

    public static final String formatSmart(double value) {
        double rounded = Math.round(value * 100.0) / 100.0;
        String formatted;

        // Check if it's basically 1.00 but not exactly
        if (Math.abs(value - 1.0) < 0.01 && Math.abs(value - 1.0) > 1e-6) {
            formatted = "1.00..";
        }
        // Normal formatting: drop second decimal if it's 0
        else if (Math.abs(rounded * 10 - Math.round(rounded * 10)) < 1e-9) {
            formatted = String.format("%.1f", rounded);
        } else {
            formatted = String.format("%.2f", rounded);
        }

        return formatted;
    }

    public static final int firstDigit(int x) {
		while (x > 9) {
			x /= 10;
		}
		return x;
	}

    public static final String formatCredits(long number) {
        return String.format("%,d", number) + Strings.C;
    }
}
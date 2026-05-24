package wfg.ltv_econ.economy.planning.custom;

import java.util.LinkedHashMap;

public class PiecewiseSegments {
    public final LinkedHashMap<String, PiecewiseSegment> segments = new LinkedHashMap<>(4);
    public final double[] getAsArray() {
        final double[] costs = new double[segments.size()];

        int idx = 0;
        for (PiecewiseSegment segment : segments.values()) {
            costs[idx] = segment.cost;
        }

        return costs;
    }
    public final int size() { return segments.size(); }
    public final PiecewiseSegment get(String label) { return segments.get(label); }

    public final int indexOf(String label) {
        int i = 0;
        for (String key : segments.keySet()) {
            if (key.equals(label)) return i;
            i++;
        }
        return -1;
    }

    public static class PiecewiseSegment {
        public final double cost;
        public final String id;

        public PiecewiseSegment(double cost, String label) {
            this.cost = cost;
            this.id = label;
        }
    }
}
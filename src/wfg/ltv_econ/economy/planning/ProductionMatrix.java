package wfg.ltv_econ.economy.planning;

import static wfg.native_ui.util.Globals.settings;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.loading.IndustrySpecAPI;

import wfg.ltv_econ.config.IndustryConfigManager;
import wfg.ltv_econ.config.IndustryConfigManager.IndustryConfig;
import wfg.ltv_econ.config.IndustryConfigManager.OutputConfig;
import wfg.ltv_econ.constant.EconomyConstants;
import wfg.ltv_econ.economy.engine.EconomyLoop;
import wfg.ltv_econ.industry.IndustryIOs;
import wfg.ltv_econ.industry.IndustryIOs.BaseIO;
import wfg.native_ui.util.ArrayMap;

public final class ProductionMatrix {
    private ProductionMatrix() {};
    
    private static double[][] STATIC_MATRIX;
    private static ArrayList<String> STATIC_WORKER_COMMODITIES;
    private static ArrayList<String> STATIC_WORKER_INDUSTRIES;
    private static ArrayList<String> STATIC_INDUSTRY_OUTPUT_PAIRS;
    private static Map<String, Integer> INDUSTRY_OUTPUT_PAIR_TO_COLUMN;

    public static final synchronized double[][] getMatrix() {
        if (STATIC_MATRIX == null) buildMatrix();
        return STATIC_MATRIX;
    }

    public static final synchronized ArrayList<String> getIndustryOutputPairs() {
        if (STATIC_INDUSTRY_OUTPUT_PAIRS == null) buildMatrix();
        return STATIC_INDUSTRY_OUTPUT_PAIRS;
    }

    public static final synchronized ArrayList<String> getWorkerRelatedCommodityIDs() {
        if (STATIC_WORKER_COMMODITIES == null) buildWorkerRelatedCommodityIDs();
        return STATIC_WORKER_COMMODITIES;
    }

    public static final synchronized ArrayList<String> getWorkerIndustries() {
        if (STATIC_WORKER_INDUSTRIES == null) buildWorkerDependentIndustries();
        return STATIC_WORKER_INDUSTRIES;
    }

    public static final synchronized Map<String, Integer> getIndOutputPairToColumnMap() {
        if (INDUSTRY_OUTPUT_PAIR_TO_COLUMN == null) buildMatrix();
        return INDUSTRY_OUTPUT_PAIR_TO_COLUMN;
    }

    public static final void invalidate() {
        STATIC_MATRIX = null;
        STATIC_INDUSTRY_OUTPUT_PAIRS = null;
        STATIC_WORKER_COMMODITIES = null;
        INDUSTRY_OUTPUT_PAIR_TO_COLUMN = null;
        STATIC_WORKER_INDUSTRIES = null;
    }

    public static void logProductionMatrix() {
        final double[][] A = getMatrix();
        final List<String> industryOutputs = getIndustryOutputPairs();
        final List<String> coms = getWorkerRelatedCommodityIDs();
        final int O = industryOutputs.size();
        final int C = coms.size();

        final StringBuilder csv = new StringBuilder(64 + (O + 1) * (C + 1) * 12);

        csv.append("Production Matrix");
        for (String indOut : industryOutputs) {
            csv.append(',').append(csvField(indOut));
        }
        csv.append('\n');

        for (int i = 0; i < C; i++) {
            csv.append(csvField(coms.get(i)));
            for (int o = 0; o < O; o++) {
                csv.append(',').append(String.format(Locale.ROOT, "%.4f", A[i][o]));
            }
            csv.append('\n');
        }

        Global.getLogger(ProductionMatrix.class).info("=== Input Matrix A (Industry_Output Columns) ===\n" + csv);
    }

    private static final String csvField(String s) {
        if (s.indexOf(',') >= 0 || s.indexOf('"') >= 0 ||
            s.indexOf('\n') >= 0 || s.indexOf('\r') >= 0) {
            return '"' + s.replace("\"", "\"\"") + '"';
        }
        return s;
    }

    /**
     * Each method has exactly one output, so the output row is the column's only
     * possible positive entry; this comparison is therefore equivalent to the
     * strict all-rows-<=0 prune.
     */
    static final boolean isNetProductive(final String indID, final String outputID) {
        final float outputVal = IndustryIOs.getBaseOutput(indID, outputID);
        final float inputVal = IndustryIOs.getBaseInput(indID, outputID, outputID);
        return outputVal > inputVal;
    }

    private static final void buildMatrix() {
        final ArrayList<String> commodities = getWorkerRelatedCommodityIDs();
        final ArrayList<String> industries = getWorkerIndustries();

        STATIC_INDUSTRY_OUTPUT_PAIRS = new ArrayList<>();
        for (final String indID : industries) {
            final Map<String, BaseIO> outputs = IndustryIOs.getBaseIOs(indID);
            final IndustryConfig indCfg = IndustryConfigManager.getIndConfig(indID);
            
            for (final String outputID : outputs.keySet()) {
                if (!indCfg.outputs.get(outputID).usesWorkers) continue;
                if (!isNetProductive(indID, outputID)) continue;
                STATIC_INDUSTRY_OUTPUT_PAIRS.add(indID + EconomyLoop.KEY + outputID);
            }
        }

        final double[][] A = new double[commodities.size()][STATIC_INDUSTRY_OUTPUT_PAIRS.size()];

        int colIndex = 0;
        for (final String indID : industries) {
            final Map<String, BaseIO> outputs = IndustryIOs.getBaseIOs(indID);
            final IndustryConfig indCfg = IndustryConfigManager.getIndConfig(indID);

            for (final Map.Entry<String, BaseIO> out : outputs.entrySet()) {
                final String outputID = out.getKey();
                if (!indCfg.outputs.get(outputID).usesWorkers) continue;
                if (!isNetProductive(indID, outputID)) continue;

                // Inputs
                for (var in : IndustryIOs.getBaseInputs(indID, outputID).singleEntrySet()) {
                    final int row = commodities.indexOf(in.getKey());
                    if (row >= 0) A[row][colIndex] -= in.getValue();
                }

                // Output
                final int row = commodities.indexOf(outputID);
                if (row >= 0) A[row][colIndex] += out.getValue().outputValue;

                colIndex++;
            }
        }

        final int size = STATIC_INDUSTRY_OUTPUT_PAIRS.size();
        INDUSTRY_OUTPUT_PAIR_TO_COLUMN = new ArrayMap<>(size);
        for (int i = 0; i < size; i++) {
            INDUSTRY_OUTPUT_PAIR_TO_COLUMN.put(STATIC_INDUSTRY_OUTPUT_PAIRS.get(i), i);
        }

        STATIC_MATRIX = A;
    }

    private static final void buildWorkerRelatedCommodityIDs() {
        STATIC_WORKER_COMMODITIES = new ArrayList<>(EconomyConstants.econCommodityIDs);

        final Iterator<String> commodities = STATIC_WORKER_COMMODITIES.iterator();
        while (commodities.hasNext()) {
            final String com = commodities.next();
            boolean relevant = false;
            for (final IndustrySpecAPI spec : settings.getAllIndustrySpecs()) {
                final IndustryConfig cfg = IndustryConfigManager.getIndConfig(spec);
                if (!cfg.workerAssignable) continue;

                for (final OutputConfig output : cfg.outputs.values()) {
                    if (!output.usesWorkers) continue;

                    if (com.equals(output.comID)) { relevant = true; break; }

                    if (IndustryIOs.getBaseInputs(spec.getId(), output.comID).containsKey(com)) {
                        relevant = true; break;
                    }
                }
            }
            if (!relevant) commodities.remove();
        }
    }

    private static final void buildWorkerDependentIndustries() {
        final Set<String> industrySet = new LinkedHashSet<>();
        for (IndustrySpecAPI spec : settings.getAllIndustrySpecs()) {
            if (IndustryConfigManager.hasConfig(spec)) {
                if (IndustryConfigManager.getIndConfig(spec).workerAssignable) {
                    industrySet.add(IndustryConfigManager.getBaseIndIDifNoConfig(spec));
                }
            }
        }
        STATIC_WORKER_INDUSTRIES = new ArrayList<>(industrySet);
    }
}
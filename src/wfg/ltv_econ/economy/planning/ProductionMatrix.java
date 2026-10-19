package wfg.ltv_econ.economy.planning;

import static wfg.native_ui.util.Globals.settings;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import com.fs.starfarer.api.loading.IndustrySpecAPI;

import wfg.ltv_econ.config.IndustryConfigManager;
import wfg.ltv_econ.config.IndustryConfigManager.IndustryConfig;
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

    private static final void buildMatrix() {
        final ArrayList<String> commodities = getWorkerRelatedCommodityIDs();
        final ArrayList<String> industries = getWorkerIndustries();

        STATIC_INDUSTRY_OUTPUT_PAIRS = new ArrayList<>();
        for (String indID : industries) {
            final Map<String, BaseIO> outputs = IndustryIOs.getBaseIOs(indID);
            
            for (String outputID : outputs.keySet()) {
                STATIC_INDUSTRY_OUTPUT_PAIRS.add(indID + EconomyLoop.KEY + outputID);
            }
        }

        final double[][] A = new double[commodities.size()][STATIC_INDUSTRY_OUTPUT_PAIRS.size()];

        int colIndex = 0;
        for (String indID : industries) {
            final Map<String, BaseIO> outputs = IndustryIOs.getBaseIOs(indID);

            for (Map.Entry<String, BaseIO> out : outputs.entrySet()) {
                final String outputID = out.getKey();

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

        Iterator<String> it = STATIC_WORKER_COMMODITIES.iterator();
        while (it.hasNext()) {
            final String com = it.next();
            boolean remove = true;
            for (String key : EconomyConstants.industryIDs) {
                final IndustrySpecAPI spec = settings.getIndustrySpec(key);
                final IndustryConfig cfg = IndustryConfigManager.getIndConfig(spec);

                if (!cfg.workerAssignable || !cfg.outputs.containsKey(com) ||
                    !cfg.outputs.get(com).usesWorkers
                ) continue;

                remove = false;
                break;
            }
            if (remove) it.remove();
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
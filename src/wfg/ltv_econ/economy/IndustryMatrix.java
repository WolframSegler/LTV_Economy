package wfg.ltv_econ.economy;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.SettingsAPI;
import com.fs.starfarer.api.loading.IndustrySpecAPI;

import wfg.ltv_econ.configs.IndustryConfigManager.IndustryConfig;
import wfg.ltv_econ.economy.engine.EconomyInfo;
import wfg.ltv_econ.economy.engine.EconomyLoop;
import wfg.ltv_econ.industry.IndustryIOs;

public class IndustryMatrix {
    private static double[][] STATIC_MATRIX;
    private static List<String> STATIC_WORKER_COMMODITIES;
    private static List<String> STATIC_INDUSTRY_OUTPUT_PAIRS;
    private static Map<String, Integer> INDUSTRY_OUTPUT_PAIR_TO_COLUMN;

    public static final synchronized double[][] getMatrix() {
        if (STATIC_MATRIX == null) buildMatrix();
        return STATIC_MATRIX;
    }

    public static final synchronized List<String> getIndustryOutputPairs() {
        if (STATIC_INDUSTRY_OUTPUT_PAIRS == null) buildMatrix();
        return STATIC_INDUSTRY_OUTPUT_PAIRS;
    }

    public static final synchronized List<String> getWorkerRelatedCommodityIDs() {
        if (STATIC_WORKER_COMMODITIES == null) buildWorkerRelatedCommodityIDs();
        return STATIC_WORKER_COMMODITIES;
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
    }

    private static final void buildMatrix() {
        final Map<String, Map<String, Float>> baseOutputs = IndustryIOs.getBaseOutputsMap();
        final Map<String, Map<String, Map<String, Float>>> baseInputs = IndustryIOs.getBaseInputsMap();
        final SettingsAPI settings = Global.getSettings();

        final List<String> commodities = getWorkerRelatedCommodityIDs();

        final List<String> industries;
        {
            Set<String> industrySet = new LinkedHashSet<>();
            for (IndustrySpecAPI spec : settings.getAllIndustrySpecs()) {
                if (IndustryIOs.hasConfig(spec)) {
                    if (IndustryIOs.getIndConfig(spec).workerAssignable) {
                        industrySet.add(IndustryIOs.getBaseIndIDifNoConfig(spec));
                    }
                }
            }
            industries = new ArrayList<>(industrySet);
        }

        // Flatten industries into industry-output pairs
        final List<String> industryOutputPairs = new ArrayList<>();
        for (String indID : industries) {
            Map<String, Float> outputs = baseOutputs.get(indID);
            if (outputs != null) {
                for (String outputID : outputs.keySet()) {
                    industryOutputPairs.add(indID + EconomyLoop.KEY + outputID);
                }
            }
        }

        STATIC_INDUSTRY_OUTPUT_PAIRS = new ArrayList<>();
        double[][] A = new double[commodities.size()][industryOutputPairs.size()];

        int colIndex = 0;
        for (String indID : industries) {
            final Map<String, Float> outputs = baseOutputs.get(indID);
            final Map<String, Map<String, Float>> inputs = baseInputs.get(indID);
            if (outputs == null) continue;

            for (Map.Entry<String, Float> out : outputs.entrySet()) {
                final String outputID = out.getKey();
                STATIC_INDUSTRY_OUTPUT_PAIRS.add(indID + EconomyLoop.KEY + outputID);

                // Inputs
                if (inputs != null && inputs.containsKey(outputID)) {
                    for (Map.Entry<String, Float> in : inputs.get(outputID).entrySet()) {
                        int row = commodities.indexOf(in.getKey());
                        if (row >= 0) A[row][colIndex] -= in.getValue();
                    }
                }

                // Output
                int row = commodities.indexOf(outputID);
                if (row >= 0) A[row][colIndex] += out.getValue();

                colIndex++;
            }
        }

        INDUSTRY_OUTPUT_PAIR_TO_COLUMN = new HashMap<>();
        for (int i = 0; i < STATIC_INDUSTRY_OUTPUT_PAIRS.size(); i++) {
            INDUSTRY_OUTPUT_PAIR_TO_COLUMN.put(STATIC_INDUSTRY_OUTPUT_PAIRS.get(i), i);
        }

        STATIC_MATRIX = A;
    }

    private static final void buildWorkerRelatedCommodityIDs() {
        final SettingsAPI settings = Global.getSettings();
        final Map<String, Map<String, Float>> baseOutputs = IndustryIOs.getBaseOutputsMap();

        STATIC_WORKER_COMMODITIES = EconomyInfo.getEconCommodityIDs();

        Iterator<String> it = STATIC_WORKER_COMMODITIES.iterator();
        while (it.hasNext()) {
            final String com = it.next();
            boolean remove = true;
            for (Map.Entry<String, Map<String, Float>> entry : baseOutputs.entrySet()) {
                final IndustrySpecAPI spec = settings.getIndustrySpec(entry.getKey());
                final IndustryConfig cfg = IndustryIOs.getIndConfig(spec);

                if (!cfg.workerAssignable || !cfg.outputs.containsKey(com) ||
                    !cfg.outputs.get(com).usesWorkers
                ) continue;

                remove = false;
                break;
            }
            if (remove) it.remove();
        }
    }
}
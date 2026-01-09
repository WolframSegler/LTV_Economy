package wfg.ltv_econ.economy;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.SettingsAPI;
import com.fs.starfarer.api.loading.IndustrySpecAPI;

import wfg.ltv_econ.configs.IndustryConfigManager.IndustryConfig;
import wfg.ltv_econ.economy.engine.EconomyEngine;
import wfg.ltv_econ.economy.engine.EconomyInfo;
import wfg.ltv_econ.industry.IndustryIOs;

public class IndustryMatrix {
    private static double[][] STATIC_MATRIX;
    private static List<String> STATIC_INDUSTRY_OUTPUT_PAIRS;

    public static final synchronized double[][] getMatrix() {
        if (STATIC_MATRIX == null) buildMatrix();
        return STATIC_MATRIX;
    }

    public static final synchronized List<String> getIndustryOutputPairs() {
        if (STATIC_INDUSTRY_OUTPUT_PAIRS == null) buildMatrix();
        return STATIC_INDUSTRY_OUTPUT_PAIRS;
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
                    industryOutputPairs.add(indID + EconomyEngine.KEY + outputID);
                }
            }
        }

        List<String> pairs = new ArrayList<>();
        double[][] A = new double[commodities.size()][industryOutputPairs.size()];

        int colIndex = 0;
        for (String indID : industries) {
            final Map<String, Float> outputs = baseOutputs.get(indID);
            final Map<String, Map<String, Float>> inputs = baseInputs.get(indID);
            if (outputs == null) continue;

            for (Map.Entry<String, Float> out : outputs.entrySet()) {
                final String outputID = out.getKey();
                pairs.add(indID + EconomyEngine.KEY + outputID);

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

        STATIC_MATRIX = A;
        STATIC_INDUSTRY_OUTPUT_PAIRS = pairs;
    }

    public static final void invalidate() {
        STATIC_MATRIX = null;
        STATIC_INDUSTRY_OUTPUT_PAIRS = null;
    }

    public static final List<String> getWorkerRelatedCommodityIDs() {
        final SettingsAPI settings = Global.getSettings();
        final Map<String, Map<String, Float>> baseOutputs = IndustryIOs.getBaseOutputsMap();

        final List<String> commodities = EconomyInfo.getEconCommodityIDs();

        Iterator<String> it = commodities.iterator();
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

        return commodities;
    }
}
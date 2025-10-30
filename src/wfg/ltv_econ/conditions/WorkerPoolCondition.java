package wfg.ltv_econ.conditions;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.econ.Industry;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.impl.campaign.econ.BaseMarketConditionPlugin;
import com.fs.starfarer.api.impl.campaign.ids.Factions;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;

import wfg.ltv_econ.configs.LaborConfigLoader.LaborConfig;
import wfg.ltv_econ.economy.WorkerRegistry;
import wfg.ltv_econ.economy.WorkerRegistry.WorkerIndustryData;
import wfg.wrap_ui.util.NumFormat;

public class WorkerPoolCondition extends BaseMarketConditionPlugin {

    public static final String ConditionID = "worker_pool";

    private long workerPool = 0;
    private float freeWorkerRatio = 1f;

    @Override
    public void apply(String id) {
        if (market == null)
            return;

        recalculateWorkerPool();
    }

    public void recalculateWorkerPool() {
        setWorkerPool((long)(
            LaborConfig.populationRatioThatAreWorkers * Math.pow(10, market.getSize())
        ));

        float totalAssigned = 0;
        final WorkerRegistry registry = WorkerRegistry.getInstance();
        if (registry == null) return;

        for (Industry ind : market.getIndustries()) {
            WorkerIndustryData data = registry.getData(market.getId(), ind.getId());
            if (data != null) {
                totalAssigned += data.getWorkerAssignedRatio(false);
            }
        }
        setFreeWorkerRatio(Math.max(0f, 1f - totalAssigned));
    }

    public long getWorkerPool() {
        return workerPool;
    }

    public float getFreeWorkerRatio() {
        return freeWorkerRatio;
    }

    public void setWorkerPool(long workers) {
        if (workers < 0) {
            return;
        }
        workerPool = workers;
    }

    public boolean setFreeWorkerRatio(float workers) {
        if (0f > workers || workers > 1f) {
            return false;
        }
        freeWorkerRatio = workers;
        return true;
    }

    public boolean isWorkerRatioAssignable(float amount) {
        return freeWorkerRatio >= amount;
    }

    public boolean assignFreeWorkers(float assignedWorkers) {
        if (freeWorkerRatio < assignedWorkers)
            return false;

        freeWorkerRatio -= assignedWorkers;
        return true;
    }

    public void releaseWorkers(float releasedWorkers) {
        freeWorkerRatio += releasedWorkers;
        if (freeWorkerRatio > 1f) {
            freeWorkerRatio = 1f;
        }
    }

    @Override
    protected void createTooltipAfterDescription(TooltipMakerAPI tooltip, boolean expanded) {
        final float pad = 10f;

        tooltip.addPara("Total Workers: %s", pad, Misc.getHighlightColor(),
            NumFormat.engNotation(getWorkerPool())
        );
        tooltip.addPara(
            "Free Workers: %s (%s%%)", pad, Misc.getHighlightColor(),
            NumFormat.engNotation((long)(freeWorkerRatio * getWorkerPool())),
            String.format("%.1f", freeWorkerRatio * 100)
        );
    }

    @Override
    public boolean showIcon() {
        return true;
    }

    public static final void initialize() {
        // All existing markets
        for (MarketAPI market : Global.getSector().getEconomy().getMarketsCopy()) {
            addConditionToMarket(market);
        }
    }

    public static final void addConditionToMarket(MarketAPI market) {
        if (market.hasCondition(ConditionID) ||
            market.getFactionId().equals(Factions.NEUTRAL) ||
            !market.isInEconomy()
        ) return;

        market.addCondition(ConditionID);
    }
}
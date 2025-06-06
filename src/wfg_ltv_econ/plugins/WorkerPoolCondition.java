package wfg_ltv_econ.plugins;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.econ.Industry;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.impl.campaign.econ.BaseMarketConditionPlugin;
import com.fs.starfarer.api.impl.campaign.ids.Factions;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import wfg_ltv_econ.util.LtvNumFormat;
import wfg_ltv_econ.industry.LtvBaseIndustry;

public class WorkerPoolCondition extends BaseMarketConditionPlugin {

    private long workerPool = 0;
    private float freeWorkerRatio = 1f;

    @Override
    public void apply(String id) {
        if (market == null)
            return;

        recalculateWorkerPool();
    }

    public void recalculateWorkerPool() {
        setWorkerPool((long)(0.64 * Math.pow(10, market.getSize())));

        float totalAssigned = 0;
        for (Industry ind : market.getIndustries()) {
            if (ind instanceof LtvBaseIndustry) {
                totalAssigned += ((LtvBaseIndustry) ind).workersAssigned;
            }
        }
        if (setFreeWorkerRatio(1 - totalAssigned)) {

        }
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

    @Override
    protected void createTooltipAfterDescription(TooltipMakerAPI tooltip, boolean expanded) {
        final float pad = 10f;

        tooltip.addPara("Total Workers: %s", pad, Misc.getHighlightColor(),
                LtvNumFormat.formatWithMaxDigits(getWorkerPool()));
        tooltip.addPara("Free Workers: %s", pad, Misc.getHighlightColor(),
                LtvNumFormat.formatWithMaxDigits((long)(freeWorkerRatio*getWorkerPool())));
    }

    @Override
    public boolean showIcon() {
        return true;
    }

    public final static void initialize() {
        // All existing markets
        for (MarketAPI market : Global.getSector().getEconomy().getMarketsCopy()) {
            if (market.hasCondition("worker_pool") || market.getFactionId().equals(Factions.NEUTRAL)) {
                continue;
            }
            market.addCondition("worker_pool");
        }
    }
}
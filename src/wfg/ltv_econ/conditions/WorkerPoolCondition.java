package wfg.ltv_econ.conditions;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.econ.Industry;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.campaign.econ.MarketConditionAPI;
import com.fs.starfarer.api.impl.campaign.econ.BaseMarketConditionPlugin;
import com.fs.starfarer.api.impl.campaign.ids.Factions;
import com.fs.starfarer.api.ui.TooltipMakerAPI;

import wfg.ltv_econ.economy.WorkerRegistry;
import wfg.ltv_econ.economy.WorkerRegistry.WorkerIndustryData;
import wfg.wrap_ui.util.NumFormat;
import static wfg.wrap_ui.util.UIConstants.*;

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

    public final void recalculateWorkerPool() {
        setWorkerPool((long)(
            getWorkerRatio(market.getSize()) * Math.pow(10, market.getSize())
        ));

        float totalAssigned = 0;
        final WorkerRegistry reg = WorkerRegistry.getInstance();
        if (reg == null) return;

        for (Industry ind : market.getIndustries()) {
            final WorkerIndustryData data = reg.getData(ind);
            if (data != null) {
                totalAssigned += data.getWorkerAssignedRatio(false);
            }
        }
        setFreeWorkerRatio(Math.max(0f, 1f - totalAssigned));
    }

    public final long getWorkerPool() {
        return workerPool;
    }

    public final float getFreeWorkerRatio() {
        return freeWorkerRatio;
    }

    public final void setWorkerPool(long workers) {
        if (workers < 0) {
            return;
        }
        workerPool = workers;
    }

    public final boolean setFreeWorkerRatio(float workers) {
        if (0f > workers || workers > 1f) {
            return false;
        }
        freeWorkerRatio = workers;
        return true;
    }

    public final boolean isWorkerRatioAssignable(float amount) {
        return freeWorkerRatio >= amount;
    }

    public final boolean assignFreeWorkers(float assignedWorkers) {
        if (freeWorkerRatio < assignedWorkers)
            return false;

        freeWorkerRatio -= assignedWorkers;
        return true;
    }

    public final void releaseWorkers(float releasedWorkers) {
        freeWorkerRatio += releasedWorkers;
        if (freeWorkerRatio > 1f) {
            freeWorkerRatio = 1f;
        }
    }

    @Override
    protected void createTooltipAfterDescription(TooltipMakerAPI tooltip, boolean expanded) {
        tooltip.addPara(
            "Smaller colonies have mostly workers, while larger colonies house more families and independent residents, limiting the labor controlled by the government.", opad
        );

        tooltip.addPara("Total Workers: %s", opad, highlight,
            NumFormat.engNotation(getWorkerPool())
        );
        tooltip.addPara(
            "Unemployed Workers: %s (%s%%)", opad, highlight,
            NumFormat.engNotation((long)(freeWorkerRatio * getWorkerPool())),
            String.format("%.1f", freeWorkerRatio * 100)
        );
    }

    @Override
    public boolean showIcon() {
        return true;
    }

    public static final float getWorkerRatio(int size) {
        switch (size) {
            case 1, 2: return 1f;
            case 3: return 0.9f;
            case 4: return 0.8f;
            case 5: return 0.68f;
            case 6: return 0.52f;
            case 7: return 0.4f;
            case 8: return 0.26f;
            case 9: return 0.15f;
            case 10: return 0.08f;
            default: return 0.05f;
        }
    }

    public static final void initialize() {
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

    public static final WorkerPoolCondition getPoolCondition(MarketAPI market) {
        if (!market.hasCondition(ConditionID)) addConditionToMarket(market);
        final MarketConditionAPI cond = market.getCondition(ConditionID);
        return (WorkerPoolCondition) cond.getPlugin();
    }
}
package wfg.ltv_econ.condition;

import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.impl.campaign.DebugFlags;
import com.fs.starfarer.api.impl.campaign.econ.BaseMarketConditionPlugin;
import com.fs.starfarer.api.impl.campaign.ids.Factions;
import com.fs.starfarer.api.ui.TooltipMakerAPI;

import wfg.ltv_econ.config.LaborConfig;
import wfg.ltv_econ.economy.engine.EconomyInfo;
import wfg.ltv_econ.economy.registry.WorkerPoolRegistry;
import wfg.ltv_econ.economy.registry.WorkerPoolRegistry.WorkerPool;
import wfg.native_ui.util.NumFormat;

import static wfg.ltv_econ.constant.strings.LocalizedStrings.*;
import static wfg.native_ui.util.UIConstants.*;

public class WorkerPoolCondition extends BaseMarketConditionPlugin {
    private static final String ConditionID = "worker_pool";

    @Override
    protected void createTooltipAfterDescription(TooltipMakerAPI tooltip, boolean expanded) {
        final WorkerPool pool = WorkerPoolRegistry.get(market);

        tooltip.addPara(str("workerPoolConditionDesc"), opad);

        tooltip.addPara(str("localWorkersWithValue"), opad, highlight,
            NumFormat.engNotate(pool.getWorkerPool())
        );
        tooltip.addPara(
            str("unemployedWorkersWithValue"), opad, highlight,
            NumFormat.engNotate(pool.getFreeWorkerRatio() * pool.getWorkerPool()),
            String.format("%.1f", pool.getFreeWorkerRatio() * 100f)
        );
    }

    @Override
    public boolean showIcon() {
        return DebugFlags.COLONY_DEBUG || LaborConfig.NPC_WORKER_POOL_VISIBLE || market.isPlayerOwned();
    }

    public static final void addConditionToMarket(MarketAPI market, Object param) {
        if (market.hasCondition(ConditionID) ||
            market.getFactionId().equals(Factions.NEUTRAL)
        ) return;

        market.addCondition(ConditionID, param);
    }

    public static final void initialize() {
        for (MarketAPI market : EconomyInfo.getMarketsCopy()) {
            addConditionToMarket(market, Boolean.valueOf(true));
        }
    }
}
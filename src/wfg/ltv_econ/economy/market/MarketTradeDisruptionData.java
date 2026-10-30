package wfg.ltv_econ.economy.market;

import java.io.Serializable;
import java.util.ArrayList;

import com.fs.starfarer.api.campaign.econ.MarketAPI;

import wfg.ltv_econ.config.EconConfig;
import wfg.ltv_econ.economy.engine.EconomyEngine;
import wfg.ltv_econ.serializable.LtvEconSaveData;

public final class MarketTradeDisruptionData implements Serializable {
    private static final long serialVersionUID = 1L;

    private final ArrayList<Integer> mLossRemainingDays = new ArrayList<>(6);
    private int mTradeSuspendedDays = 0;

    public final void recordLoss() {
        if (mLossRemainingDays.size() + 1 >= EconConfig.CONVOY_LOSS_SUSPENSION_THRESHOLD) {
            mTradeSuspendedDays = EconConfig.CONVOY_LOSS_SUSPENSION_DAYS;
            mLossRemainingDays.clear();
        } else {
            mLossRemainingDays.add(EconConfig.CONVOY_LOSS_WINDOW_DAYS);
        }
    }

    public final void advance(int days) {
        for (int i = 0; i < mLossRemainingDays.size(); i++) {
            mLossRemainingDays.set(i, mLossRemainingDays.get(i) - days);
        }
        mLossRemainingDays.removeIf(remaining -> remaining <= 0);

        mTradeSuspendedDays = Math.max(0, mTradeSuspendedDays - days);
    }

    public final boolean isSuspended() {
        return mTradeSuspendedDays > 0;
    }

    public final int getSuspendedDays() {
        return mTradeSuspendedDays;
    }

    public final void resetLosses() {
        mLossRemainingDays.clear();
        mTradeSuspendedDays = 0;
    }

    public static synchronized final MarketTradeDisruptionData get(String marketID) {
        final ConcurrentMarketData data = EconomyEngine.instance().getMarketData(marketID);
        return data.mTradeDisruptionData;
    }

    public static final MarketTradeDisruptionData get(MarketAPI market) {
        return get(market.getId());
    }

    public static final boolean shouldSuspendTrade(MarketAPI market) {
        if (market.getFaction().isPlayerFaction() && !LtvEconSaveData.instance().playerFactionSettings.tradeSuspensionWhenConvoysRaided) {
            return false;
        }
        return get(market).isSuspended();
    }
}
package wfg.ltv_econ.economy.commodity;

import java.util.ArrayList;

import com.fs.starfarer.api.campaign.econ.MarketAPI;

import wfg.ltv_econ.config.EconConfig;
import wfg.ltv_econ.serializable.LtvEconSaveData;

public final class MarketTradeDisruptionData implements java.io.Serializable {
    private static final long serialVersionUID = 1L;
    private static final String DISRUPTION_KEY = "$ltv_trade_disruption";

    private final ArrayList<Integer> mLossRemainingDays = new ArrayList<>();
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

    public static final MarketTradeDisruptionData get(MarketAPI market) {
        final Object obj = market.getMemory().get(DISRUPTION_KEY);
        if (obj instanceof MarketTradeDisruptionData) {
            return (MarketTradeDisruptionData) obj;
        }

        final MarketTradeDisruptionData data = new MarketTradeDisruptionData();
        market.getMemory().set(DISRUPTION_KEY, data);
        return data;
    }

    public static final boolean shouldSuspendTrade(MarketAPI market) {
        if (market.getFaction().isPlayerFaction() && !LtvEconSaveData.instance().playerFactionSettings.tradeSuspensionWhenConvoysRaided) {
            return false;
        }
        return get(market).isSuspended();
    }
}
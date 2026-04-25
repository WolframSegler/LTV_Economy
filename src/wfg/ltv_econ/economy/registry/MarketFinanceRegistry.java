package wfg.ltv_econ.economy.registry;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;

import com.fs.starfarer.api.campaign.econ.MarketAPI;

import wfg.ltv_econ.economy.engine.EconomyEngine;
import wfg.ltv_econ.economy.engine.EconomyInfo;
import wfg.ltv_econ.serializable.LtvEconSaveData;
import wfg.native_ui.util.ArrayMap;

public class MarketFinanceRegistry implements Serializable {
    private final ArrayMap<String, MarketLedger> registry = new ArrayMap<>(EconomyInfo.getMarketsCount());

    public static final MarketFinanceRegistry instance() {
        return LtvEconSaveData.instance().financeRegistry;
    }

    public final synchronized void register(String marketID) {
        registry.putIfAbsent(marketID, new MarketLedger(marketID));
    }

    public final void register(MarketAPI market) {
        register(market.getId());
    }

    public final synchronized void remove(String marketID) {
        registry.remove(marketID);
    }

    public final void remove(MarketAPI market) {
        remove(market.getId());
    }

    public final boolean has(String marketID) {
        return registry.containsKey(marketID);
    }

    public final boolean has(MarketAPI market) {
        return has(market.getId());
    }

    public final MarketLedger getLedger(String marketID) {
        return registry.computeIfAbsent(marketID, m -> new MarketLedger(marketID));
    }

    public final MarketLedger getLedger(MarketAPI market) {
        return getLedger(market.getId());
    }

    public final void endMonth() {
        registry.values().forEach(MarketLedger::endMonth);
    }

    public final synchronized MarketLedger overrideLedger(String marketID, MarketLedger ledger) {
        return registry.put(marketID, ledger);
    }

    public final MarketLedger overrideLedger(MarketAPI market, MarketLedger ledger) {
        return overrideLedger(market.getId(), ledger);
    }

    public final Collection<MarketLedger> getRegistry() {
        return registry.values();
    }

    public final ArrayList<MarketLedger> getRegistryCopy() {
        return new ArrayList<>(registry.values());
    }

    public static class MarketLedger implements Serializable {
        private final ArrayMap<String, Long> currentMonth = new ArrayMap<>(8);
        private final ArrayMap<String, Long> lastMonth = new ArrayMap<>(8);
        private final ArrayMap<String, String> currentMonthDesc = new ArrayMap<>(8);
        private final ArrayMap<String, String> lastMonthDesc = new ArrayMap<>(8);
        public final String marketID;

        public MarketLedger(String marketID) {
            this.marketID = marketID;
        }

        public final synchronized void add(String key, double amount, String desc) {
            add(key, (long) amount, desc);
        }

        public final synchronized void add(String key, long amount, String desc) {
            currentMonth.merge(key, amount, Long::sum);
            currentMonthDesc.putIfAbsent(key, desc);
        }
        
        public final synchronized void replace(String key, double amount, String desc) {
            replace(key, (long) amount, desc);
        }

        public final synchronized void replace(String key, long amount, String desc) {
            currentMonth.put(key, amount);
            currentMonthDesc.put(key, desc);
        }

        public final long getCurrentMonth(String key) {
            return currentMonth.getOrDefault(key, 0l);
        }

        public final long getLastMonth(String key) {
            return lastMonth.getOrDefault(key, 0l);
        }

        public final String getDescCurrentMonth(String key) {
            return currentMonthDesc.getOrDefault(key, "");
        }

        public final String getDescLastMonth(String key) {
            return lastMonthDesc.getOrDefault(key, "");
        }

        public final String getDesc(String key) {
            return currentMonthDesc.getOrDefault(key, "");
        }

        public final long getNetLastMonth() {
            long total = 0l;
            for (Long v : lastMonth.values()) {
                total += v;
            }
            return total;
        }

        public final long getIncomeLastMonth() {
            long total = 0;
            for (long v : lastMonth.values()) {
                if (v > 0l) total += v;
            }
            return total;
        }

        /** returns a positive value */
        public final long getExpenseLastMonth() {
            long total = 0l;
            for (long v : lastMonth.values()) {
                if (v < 0l) total += v;
            }
            return Math.abs(total);
        }

        public final long getNetCurrentMonth() {
            long total = 0l;
            for (long v : currentMonth.values()) total += v;
            return total;
        }

        public final long getIncomeCurrentMonth() {
            long total = 0;
            for (long v : currentMonth.values()) {
                if (v > 0l) total += v;
            }
            return total;
        }

        /** returns a positive value */
        public final long getExpenseCurrentMonth() {
            long total = 0l;
            for (long v : currentMonth.values()) {
                if (v < 0l) total += v;
            }
            return Math.abs(total);
        }

        public final Map<String, Long> getAllCurrent() {
            return Collections.unmodifiableMap(currentMonth);
        }

        public final Map<String, Long> getAllLast() {
            return Collections.unmodifiableMap(lastMonth);
        }

        public final synchronized void endMonth() {
            lastMonth.clear();
            lastMonthDesc.clear();
            lastMonth.putAll(currentMonth);
            lastMonthDesc.putAll(currentMonthDesc);
            currentMonth.clear();
            currentMonthDesc.clear();

            EconomyEngine.instance().addCredits(marketID, getNetLastMonth());
        }
    }
}
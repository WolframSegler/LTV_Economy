package wfg_ltv_econ.economy;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.econ.CommoditySpecAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;

public class CommodityInfo {
    private final CommoditySpecAPI m_spec;
    private final Map<MarketAPI, CommodityStats> m_comStats = new HashMap<>();

    public CommodityInfo(
        CommoditySpecAPI spec
    ) {
        m_spec = spec;

        for (MarketAPI market : Global.getSector().getEconomy().getMarketsCopy()) {
            m_comStats.put(market, new CommodityStats(m_spec.getId(), market));
        }
    }

    public void advance() {
        for (Map.Entry<MarketAPI, CommodityStats> com : m_comStats.entrySet()) {
            com.getValue().advance();
        }
    }

    public CommodityStats getStats(MarketAPI market) {
        return m_comStats.get(market);
    }

    public Collection<CommodityStats> getAllStats() {
        return m_comStats.values();
    }
}
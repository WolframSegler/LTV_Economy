package wfg_ltv_econ.economy;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.econ.CommoditySpecAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;

public class CommodityInfo {
    private final CommoditySpecAPI m_spec;
    private final Map<String, CommodityStats> m_comStats = new HashMap<>();

    public CommodityInfo(
        CommoditySpecAPI spec
    ) {
        m_spec = spec;

        for (MarketAPI market : Global.getSector().getEconomy().getMarketsCopy()) {
            if (market.isHidden()) continue;
            
            m_comStats.put(market.getId(), new CommodityStats(m_spec.getId(), market));
        }
    }

    public void advance() {
        for (Map.Entry<String, CommodityStats> stats : m_comStats.entrySet()) {
            stats.getValue().advance();
        }
    }

    public void addMarket(MarketAPI market) {
        if (m_comStats.containsKey(market.getId())) return;
        
        m_comStats.put(market.getId(), new CommodityStats(m_spec.getId(), market));
    }

    public CommodityStats getStats(MarketAPI market) {

        return m_comStats.get(market.getId());
    }

    public Collection<CommodityStats> getAllStats() {
        return m_comStats.values();
    }
}
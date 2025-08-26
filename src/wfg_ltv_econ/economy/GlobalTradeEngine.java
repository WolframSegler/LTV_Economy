package wfg_ltv_econ.economy;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.econ.CommodityOnMarketAPI;
import com.fs.starfarer.api.campaign.econ.CommoditySpecAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;

/**
 * Handles the trade, consumption, production and all related logic
 */
public class GlobalTradeEngine {
    private static GlobalTradeEngine instance;

    private Map<String, CommoditySpecAPI> m_commoditySpecs;
    private Map<String, CommodityInfo> m_commoditInfo;
    private List<MarketAPI> m_markets;

    public static GlobalTradeEngine getInstance() {
        if (instance == null) instance = new GlobalTradeEngine();
        return instance;
    }

    private GlobalTradeEngine() {
        this.m_markets = new ArrayList<>();
        this.m_commoditInfo = new HashMap<>();
        this.m_commoditySpecs = new HashMap<>();

        for (CommoditySpecAPI spec : Global.getSettings().getAllCommoditySpecs()) {
            m_commoditySpecs.put(spec.getId(), spec);
            m_commoditInfo.put(spec.getId(), new CommodityInfo(spec));
        }
    }

    protected int dayTracker = -1;

    public void advance(float delta) {
        final int day = Global.getSector().getClock().getDay();

		if (dayTracker != day) {

            for (Map.Entry<String, CommodityInfo> com : m_commoditInfo.entrySet()) {
                com.getValue().advance();
            }

			dayTracker = day;
		}
    }

    public void registerMarket(MarketAPI market) {
        m_markets.add(market);
    }

    public List<MarketAPI> getMarkets() {
        return m_markets;
    }

    public final List<MarketAPI> getMarketsImportingCom(
        CommoditySpecAPI spec, MarketAPI exporter, boolean onlyInFaction
    ) {
        List <MarketAPI> importers = new ArrayList<>();

        for (CommodityStats stats : m_commoditInfo.get(spec.getId()).getAllStats()) {
            if (onlyInFaction && !stats.market.getFaction().equals(exporter.getFaction())) {
                continue;
            }

            final CommodityOnMarketAPI com = stats.m_com;

            if (com != null && stats.getDeficitPreTrade() > 0) {
                importers.add(stats.market);
            }
        }

        return importers;
    }
}

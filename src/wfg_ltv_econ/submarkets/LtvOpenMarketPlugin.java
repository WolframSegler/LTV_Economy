package wfg_ltv_econ.submarkets;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CoreUIAPI;
import com.fs.starfarer.api.campaign.FactionDoctrineAPI;
import com.fs.starfarer.api.campaign.SubmarketPlugin;
import com.fs.starfarer.api.campaign.CampaignUIAPI.CoreUITradeMode;
import com.fs.starfarer.api.campaign.FactionAPI.ShipPickMode;
import com.fs.starfarer.api.campaign.econ.CommodityOnMarketAPI;
import com.fs.starfarer.api.campaign.econ.SubmarketAPI;
import com.fs.starfarer.api.impl.campaign.ids.Commodities;
import com.fs.starfarer.api.impl.campaign.ids.MemFlags;
import com.fs.starfarer.api.impl.campaign.ids.Submarkets;
import com.fs.starfarer.api.util.Highlights;
import com.fs.starfarer.api.util.Misc;
import java.awt.Color;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;

public class LtvOpenMarketPlugin extends LtvBaseSubmarketPlugin {
    public static float ECON_UNIT_MULT_EXTRA = 1.0F;
    public static float ECON_UNIT_MULT_PRODUCTION = 0.4F;
    public static float ECON_UNIT_MULT_IMPORTS = 0.1F;
    public static float ECON_UNIT_MULT_DEFICIT = -0.2F;
    public static Set<String> SPECIAL_COMMODITIES = new HashSet<>();

    static {
        SPECIAL_COMMODITIES.add(Commodities.SUPPLIES);
        SPECIAL_COMMODITIES.add(Commodities.FUEL);
        SPECIAL_COMMODITIES.add(Commodities.CREW);
        SPECIAL_COMMODITIES.add(Commodities.MARINES);
        SPECIAL_COMMODITIES.add(Commodities.HEAVY_MACHINERY);
    }

    public static float EXTRA_SHIPS = 150f;

    public LtvOpenMarketPlugin() {
    }

    public void init(SubmarketAPI submarket) {
        super.init(submarket);
    }

    public void updateCargoPrePlayerInteraction() {
        float seconds = Global.getSector().getClock().convertToSeconds(sinceLastCargoUpdate);
        float days = Global.getSector().getClock().convertToDays(seconds);

        addAndRemoveStockpiledResources(days, false, true, true);
        sinceLastCargoUpdate = 0.0F;
        if (okToUpdateShipsAndWeapons()) {
            sinceSWUpdate = 0.0F;
            boolean military = Misc.isMilitary(market);
            boolean hiddenBase = market.getMemoryWithoutUpdate().getBoolean(MemFlags.HIDDEN_BASE_MEM_FLAG);
            float extraShips = 0.0F;

            if (military && hiddenBase && !market.hasSubmarket(Submarkets.GENERIC_MILITARY)) {
                extraShips = EXTRA_SHIPS;
            }

            pruneWeapons(0.0F);
            int weapons = 5 + Math.max(0, market.getSize() - 1) + (Misc.isMilitary(market) ? 5 : 0);
            int fighters = 1 + Math.max(0, (market.getSize() - 3) / 2) + (Misc.isMilitary(market) ? 2 : 0);
            addWeapons(weapons, weapons + 2, 0, market.getFactionId());
            addFighters(fighters, fighters + 2, 0, market.getFactionId());
            getCargo().getMothballedShips().clear();
            float freighters = 10.0F;
            CommodityOnMarketAPI commodity = market.getCommodityData(Commodities.SHIPS);
            freighters += (float) commodity.getMaxSupply() * 2.0F;
            if (freighters > 30.0F) {
                freighters = 30.0F;
            }

            addShips(market.getFactionId(), 10.0F + extraShips, freighters, 0.0F, 10.0F, 10.0F, 5.0F,
                    null, 0.0F, ShipPickMode.PRIORITY_THEN_ALL, null);
            addShips(market.getFactionId(), 40.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, null, -1.0F,
                    null, null, 4);
            commodity = market.getCommodityData(Commodities.FUEL);
            float tankers = Math.min(20 + commodity.getMaxSupply()*3, 40);

            addShips(market.getFactionId(), 0.0F, 0.0F, tankers, 0.0F, 0.0F, 0.0F, null, 0.0F,
                    ShipPickMode.PRIORITY_THEN_ALL, (FactionDoctrineAPI) null);
            addHullMods(1, 1 + itemGenRandom.nextInt(3), market.getFactionId());
        }

        getCargo().sort();
    }

    protected Object writeReplace() {
        if (okToUpdateShipsAndWeapons()) {
            pruneWeapons(0.0F);
            getCargo().getMothballedShips().clear();
        }

        return this;
    }

    @Override
    public boolean shouldHaveCommodity(CommodityOnMarketAPI commodity) {
        return !market.isIllegal(commodity);
    }

    @Override
    public int getStockpileLimit(CommodityOnMarketAPI commodity) {
        float limit = getBaseStockpileLimit(commodity);
        Random random = new Random((long) (market.getId().hashCode() + submarket.getSpecId().hashCode()
                + Global.getSector().getClock().getMonth() * 170000));
        limit *= 0.9F + 0.2F * random.nextFloat();
        float sm = market.getStabilityValue() / 10;
        limit *= 0.25F + 0.75F * sm;

        return (int)Math.max(limit, 0);
    }
    
    public static int getBaseStockpileLimit(CommodityOnMarketAPI commodity) {
        int shippingGlobal = Global.getSettings().getShippingCapacity(commodity.getMarket(), false);
        int available = commodity.getAvailable();
        int production = Math.min(commodity.getMaxSupply(), available);

        int demand = commodity.getMaxDemand();
        int export = Math.min(production, shippingGlobal);
        int extra = Math.max(available - Math.max(export, demand), 0);

        int deficit = Math.max(0, demand - available);
        float unit = commodity.getCommodity().getEconUnit();
        int imports = Math.max(available - production, 0);

        float limit = 0.0F;
        limit += (float) imports * unit * ECON_UNIT_MULT_IMPORTS;
        limit += (float) production * unit * ECON_UNIT_MULT_PRODUCTION;
        limit += (float) extra * unit * ECON_UNIT_MULT_EXTRA;
        limit -= (float) deficit * unit * ECON_UNIT_MULT_DEFICIT;
        if (limit < 0.0F) {
            limit = 0.0F;
        }

        return Math.round(limit);
    }

    public static int getApproximateStockpileLimit(CommodityOnMarketAPI commodity) {
        return getBaseStockpileLimit(commodity);
    }

    @Override
    public SubmarketPlugin.PlayerEconomyImpactMode getPlayerEconomyImpactMode() {
        return PlayerEconomyImpactMode.BOTH;
    }

    public boolean isOpenMarket() {
        return true;
    }

    public String getTooltipAppendix(CoreUIAPI ui) {
        return ui.getTradeMode() == CoreUITradeMode.SNEAK ? "Requires: proper docking authorization (transponder on)"
                : super.getTooltipAppendix(ui);
    }

    public Highlights getTooltipAppendixHighlights(CoreUIAPI ui) {
        if (ui.getTradeMode() == CoreUITradeMode.SNEAK) {
            String appendix = getTooltipAppendix(ui);
            if (appendix == null) {
                return null;
            } else {
                Highlights h = new Highlights();
                h.setText(new String[] { appendix });
                h.setColors(new Color[] { Misc.getNegativeHighlightColor() });
                return h;
            }
        } else {
            return super.getTooltipAppendixHighlights(ui);
        }
    }
}
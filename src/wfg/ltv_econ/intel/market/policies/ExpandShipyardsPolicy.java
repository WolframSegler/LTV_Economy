package wfg.ltv_econ.intel.market.policies;

import static wfg.native_ui.util.Globals.settings;
import static wfg.native_ui.util.UIConstants.*;
import static wfg.ltv_econ.constants.strings.LocalizedStrings.*;

import java.awt.Color;

import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.impl.campaign.ids.Commodities;
import com.fs.starfarer.api.impl.campaign.ids.Submarkets;
import com.fs.starfarer.api.ui.TooltipMakerAPI;

import wfg.ltv_econ.constants.CommoditiesID;
import wfg.ltv_econ.economy.MarketPopulationData;
import wfg.ltv_econ.economy.commodity.CommodityCell;
import wfg.ltv_econ.economy.engine.EconomyEngine;

public class ExpandShipyardsPolicy extends MarketPolicy {
    private static final int METALS_COST = 5000;
    private static final int RARE_METALS_COST = 500;
    private static final int SUPPLIES_COST = 1000;
    private static final int STRUCTURAL_COMPONENTS_COST = 1000;
    private static final int HEAVY_MACHINERY_COST = 250;

    public void apply(MarketPopulationData data) {
        removeStored(data.market, Commodities.METALS, METALS_COST);
        removeStored(data.market, Commodities.RARE_METALS, RARE_METALS_COST);
        removeStored(data.market, Commodities.SUPPLIES, SUPPLIES_COST);
        removeStored(data.market, Commodities.HEAVY_MACHINERY, HEAVY_MACHINERY_COST);
        removeStored(data.market, CommoditiesID.STRUCTURAL_COMPONENTS, STRUCTURAL_COMPONENTS_COST);
    }

    public void unapply(MarketPopulationData data) {
        EconomyEngine.instance().getFactionShipInventory(data.market.getFactionId()).addAssemblyLines(1);
    }

    @Override
    public boolean isEnabled(MarketPopulationData data) {
        return EconomyEngine.instance().getFactionShipInventory(data.market.getFactionId()).getCapital().equals(data.market);
    }

    @Override
    public boolean isAvailable(MarketPopulationData data) {
        if (getStored(data.market, Commodities.METALS) < METALS_COST) return false;
        if (getStored(data.market, Commodities.RARE_METALS) < RARE_METALS_COST) return false;
        if (getStored(data.market, Commodities.SUPPLIES) < SUPPLIES_COST) return false;
        if (getStored(data.market, CommoditiesID.STRUCTURAL_COMPONENTS) < STRUCTURAL_COMPONENTS_COST) return false;
        if (getStored(data.market, Commodities.HEAVY_MACHINERY) < HEAVY_MACHINERY_COST) return false;

        return super.isAvailable(data);
    }

    @Override
    public void createTooltip(MarketPopulationData data, TooltipMakerAPI tp) {
        super.createTooltip(data, tp);

        final Color metalsColor = getStored(data.market, Commodities.METALS) >= METALS_COST ? highlight : negative;
        final Color rareMetalsColor = getStored(data.market, Commodities.RARE_METALS) >= RARE_METALS_COST ? highlight : negative;
        final Color suppliesColor = getStored(data.market, Commodities.SUPPLIES) >= SUPPLIES_COST ? highlight : negative;
        final Color structuralColor = getStored(data.market, CommoditiesID.STRUCTURAL_COMPONENTS) >= STRUCTURAL_COMPONENTS_COST ? highlight : negative;
        final Color heavyMachineryColor = getStored(data.market, Commodities.HEAVY_MACHINERY) >= HEAVY_MACHINERY_COST ? highlight : negative;
        
        final int cols = 2;
        tp.addPara(str("marketPolicyRequiredResourcesTxt"), pad);
        tp.beginGridFlipped(250f, cols, 60f, pad);
        tp.addToGrid(0, 0, settings.getCommoditySpec(Commodities.METALS).getName(), Integer.toString(METALS_COST), metalsColor);
        tp.addToGrid(0, 1, settings.getCommoditySpec(Commodities.RARE_METALS).getName(), Integer.toString(RARE_METALS_COST), rareMetalsColor);
        tp.addToGrid(0, 2, settings.getCommoditySpec(Commodities.SUPPLIES).getName(), Integer.toString(SUPPLIES_COST), suppliesColor);
        tp.addToGrid(0, 3, settings.getCommoditySpec(CommoditiesID.STRUCTURAL_COMPONENTS).getName(), Integer.toString(STRUCTURAL_COMPONENTS_COST), structuralColor);
        tp.addToGrid(0, 4, settings.getCommoditySpec(Commodities.HEAVY_MACHINERY).getName(), Integer.toString(HEAVY_MACHINERY_COST), heavyMachineryColor);

        tp.addGrid(0);

        tp.addPara(str("marketPolicyExpandShipyardsDisclaimer"), gray, opad);
    }

    private static final double getStored(MarketAPI market, String comID) {
        return EconomyEngine.instance().getComCell(comID, market.getId()).getStored() + market.getSubmarket(Submarkets.SUBMARKET_STORAGE).getCargo().getCommodityQuantity(comID);
    }

    private static final void removeStored(MarketAPI market, String comID, int amount) {
        final CommodityCell cell = EconomyEngine.instance().getComCell(comID, market.getId());
        final double removedFromCell = Math.min(amount, cell.getStored());
        cell.addStoredAmount(-removedFromCell);

        market.getSubmarket(Submarkets.SUBMARKET_STORAGE).getCargo().removeCommodity(comID, (float) -(amount - removedFromCell));
    }
}
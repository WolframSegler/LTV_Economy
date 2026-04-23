package wfg.ltv_econ.intel.market.policies;

import static wfg.native_ui.util.UIConstants.*;

import java.awt.Color;

import com.fs.starfarer.api.impl.campaign.ids.Commodities;
import com.fs.starfarer.api.ui.TooltipMakerAPI;

import wfg.ltv_econ.constants.CommoditiesID;
import wfg.ltv_econ.economy.PlayerMarketData;
import wfg.ltv_econ.economy.commodity.CommodityCell;
import wfg.ltv_econ.economy.engine.EconomyEngine;

public class ExpandShipyardsPolicy extends MarketPolicy {
    private static final int METALS_COST = 5000;
    private static final int RARE_METALS_COST = 500;
    private static final int SUPPLIES_COST = 1000;
    private static final int STRUCTURAL_COMPONENTS_COST = 1000;
    private static final int SUBASSEMBLY_COMPONENTS_COST = 250;

    public void apply(PlayerMarketData data) {
        final EconomyEngine engine = EconomyEngine.instance();
        engine.getComCell(Commodities.METALS, data.marketID).addStoredAmount(-METALS_COST);
        engine.getComCell(Commodities.RARE_METALS, data.marketID).addStoredAmount(-RARE_METALS_COST);
        engine.getComCell(Commodities.SUPPLIES, data.marketID).addStoredAmount(-SUPPLIES_COST);
        engine.getComCell(CommoditiesID.STRUCTURAL_COMPONENTS, data.marketID).addStoredAmount(-STRUCTURAL_COMPONENTS_COST);
        engine.getComCell(CommoditiesID.SUBASSEMBLY_COMPONENTS, data.marketID).addStoredAmount(-SUBASSEMBLY_COMPONENTS_COST);
    }

    public void unapply(PlayerMarketData data) {
        EconomyEngine.instance().getFactionShipInventory(data.market.getFactionId()).addAssemblyLines(1);
    }

    @Override
    public boolean isEnabled(PlayerMarketData data) {
        return EconomyEngine.instance().getFactionShipInventory(data.market.getFactionId()).getCapital().equals(data.market);
    }

    @Override
    public boolean isAvailable(PlayerMarketData data) {
        final EconomyEngine engine = EconomyEngine.instance();
        if (engine.getComCell(Commodities.METALS, data.marketID).getStored() < METALS_COST) return false;
        if (engine.getComCell(Commodities.RARE_METALS, data.marketID).getStored() < RARE_METALS_COST) return false;
        if (engine.getComCell(Commodities.SUPPLIES, data.marketID).getStored() < SUPPLIES_COST) return false;
        if (engine.getComCell(CommoditiesID.STRUCTURAL_COMPONENTS, data.marketID).getStored() < STRUCTURAL_COMPONENTS_COST) return false;
        if (engine.getComCell(CommoditiesID.SUBASSEMBLY_COMPONENTS, data.marketID).getStored() < SUBASSEMBLY_COMPONENTS_COST) return false;

        return super.isAvailable(data);
    }

    @Override
    public void createTooltip(PlayerMarketData data, TooltipMakerAPI tp) {
        super.createTooltip(data, tp);

        final EconomyEngine engine = EconomyEngine.instance();
        final CommodityCell metals = engine.getComCell(Commodities.METALS, data.marketID);
        final CommodityCell rare_metals = engine.getComCell(Commodities.RARE_METALS, data.marketID);
        final CommodityCell supplies = engine.getComCell(Commodities.SUPPLIES, data.marketID);
        final CommodityCell structural = engine.getComCell(CommoditiesID.STRUCTURAL_COMPONENTS, data.marketID);
        final CommodityCell subassembly = engine.getComCell(CommoditiesID.SUBASSEMBLY_COMPONENTS, data.marketID);
        final Color metalsColor = metals.getStored() >= METALS_COST ? highlight : negative;
        final Color rareMetalsColor = rare_metals.getStored() >= RARE_METALS_COST ? highlight : negative;
        final Color suppliesColor = supplies.getStored() >= SUPPLIES_COST ? highlight : negative;
        final Color structuralColor = structural.getStored() >= STRUCTURAL_COMPONENTS_COST ? highlight : negative;
        final Color subassemblyColor = subassembly.getStored() >= SUBASSEMBLY_COMPONENTS_COST ? highlight : negative;
        
        final int cols = 2;
        tp.addPara("Required resources to start", pad);
        tp.beginGridFlipped(250f, cols, 60f, pad);
        tp.addToGrid(0, 0, metals.spec.getName(), Integer.toString(METALS_COST), metalsColor);
        tp.addToGrid(0, 1, rare_metals.spec.getName(), Integer.toString(RARE_METALS_COST), rareMetalsColor);
        tp.addToGrid(0, 2, supplies.spec.getName(), Integer.toString(SUPPLIES_COST), suppliesColor);
        tp.addToGrid(0, 3, structural.spec.getName(), Integer.toString(STRUCTURAL_COMPONENTS_COST), structuralColor);
        tp.addToGrid(0, 4, subassembly.spec.getName(), Integer.toString(SUBASSEMBLY_COMPONENTS_COST), subassemblyColor);

        tp.addGrid(0);

        tp.addPara("This policy can only be activated at the faction capital.", gray, opad);
    }
}
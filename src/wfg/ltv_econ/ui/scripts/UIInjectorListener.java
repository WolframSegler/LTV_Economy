package wfg.ltv_econ.ui.scripts;

import static wfg.native_ui.util.Globals.settings;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CargoStackAPI;
import com.fs.starfarer.api.campaign.CoreUITabId;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.econ.CommoditySpecAPI;
import com.fs.starfarer.api.campaign.listeners.CommodityTooltipModifier;
import com.fs.starfarer.api.campaign.listeners.CoreUITabListener;
import com.fs.starfarer.api.ui.TooltipMakerAPI;

import wfg.ltv_econ.util.TooltipUtils;

public class UIInjectorListener implements CoreUITabListener, CommodityTooltipModifier {
    
    @Override
    public void reportAboutToOpenCoreTab(CoreUITabId tabID, Object param) {
        final SectorAPI sector = Global.getSector();

        sector.removeTransientScriptsOfClass(CoreTabUIBuilder.class);

        switch (tabID) {
        case CARGO:
            sector.addTransientScript(new MarketUIReplacer());
            break;
            
        case OUTPOSTS:
            sector.addTransientScript(new OutpostsTabUIBuilder());
            break;

        case INTEL:
            sector.addTransientScript(new IntelTabUIBuilder());
            break;

        case FLEET:
            sector.addTransientScript(new FleetTabUIBuilder());
            break;
    
        default: break;
        }
    }

    @Override
    public void addSectionAfterPrice(TooltipMakerAPI tp, float width, boolean expanded, CargoStackAPI stack) {
        if (!expanded ||!stack.isCommodityStack()) return;
        final CommoditySpecAPI spec = settings.getCommoditySpec(stack.getCommodityId());
        if (spec.isNonEcon()) return;

        TooltipUtils.cargoComTooltip(tp, spec, 5,
            true, true, true
        );
        Global.getSector().addTransientScript(new TpPostModRemover(tp));
    }
}
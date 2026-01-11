package wfg.ltv_econ.ui.scripts;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CoreUITabId;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.listeners.CoreUITabListener;

public class UIInjectorListener implements CoreUITabListener {
    public void reportAboutToOpenCoreTab(CoreUITabId tabID, Object param) {
        final SectorAPI sector = Global.getSector();

        { // Clear all listeners
        sector.removeTransientScriptsOfClass(MarketUIReplacer.class);
        sector.removeTransientScriptsOfClass(OutpostsTabUIBuilder.class);
        }
        
        switch (tabID) {
        case CARGO:
            sector.addTransientScript(new MarketUIReplacer());
            break;
            
        case OUTPOSTS:
            sector.addTransientScript(new OutpostsTabUIBuilder());
            break;
    
        default: break;
        }
    }
}
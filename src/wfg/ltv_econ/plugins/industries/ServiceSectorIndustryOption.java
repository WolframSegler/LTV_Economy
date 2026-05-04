package wfg.ltv_econ.plugins.industries;

import static wfg.native_ui.util.UIConstants.*;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

import com.fs.starfarer.api.campaign.econ.Industry;
import com.fs.starfarer.api.campaign.listeners.DialogCreatorUI;
import com.fs.starfarer.api.campaign.listeners.IndustryOptionProvider;
import com.fs.starfarer.api.impl.campaign.DebugFlags;
import com.fs.starfarer.api.impl.campaign.ids.Industries;
import com.fs.starfarer.api.ui.TooltipMakerAPI;

import wfg.ltv_econ.economy.registry.WorkerRegistry;
import wfg.ltv_econ.ui.marketInfo.dialogs.ServiceSectorDialog;
import wfg.native_ui.util.NumFormat;

public class ServiceSectorIndustryOption implements IndustryOptionProvider {
    public static final Object pluginID = new Object();

    private static final boolean isSuitable(Industry ind, boolean allowUnderConstruction){
        if (ind == null || ind.getMarket() == null) return false;
        if (!DebugFlags.COLONY_DEBUG && !ind.getMarket().isPlayerOwned()) return false;

        return ind.getId().equals(Industries.POPULATION);
    }

    @Override
    public List<IndustryOptionData> getIndustryOptions(Industry ind) {
        if (!isSuitable(ind, false)) return null;

        final List<IndustryOptionData> result = new ArrayList<>();

        final IndustryOptionData opt = new IndustryOptionData("Manage Service Sector...", pluginID, ind, this);
        opt.color = ind.getMarket().getFaction().getBrightUIColor();
        result.add(opt);

        return result;
    }

    @Override
    public void createTooltip(IndustryOptionData opt, TooltipMakerAPI tooltip, float width) {
        if (opt.id != pluginID) return;

        tooltip.addPara(
            "Assign workers to public services that improve stability, growth, trade efficiency, and planetary defense.\n",
            0f);

        final long assignedWorkers = WorkerRegistry.instance().getRegisterData(opt.ind).getWorkersAssigned();
        tooltip.addPara(
            "The number of workers available for public services depends on colony size " +
            "and how many are already employed in other industries. Currently, %s workers " +
            "serve in civil departments across the colony.", 0f, new Color[] {highlight},
            NumFormat.engNotate(assignedWorkers)
        );
    }

    @Override
    public void addToIndustryTooltip(Industry ind, Industry.IndustryTooltipMode mode, TooltipMakerAPI tooltip, float width, boolean expanded) {

    }

    @Override
    public void optionSelected(IndustryOptionData opt, DialogCreatorUI ui) {
        if (opt.id != pluginID) return;

        new ServiceSectorDialog(opt.ind).show(0.3f, 0.3f);
    }
}
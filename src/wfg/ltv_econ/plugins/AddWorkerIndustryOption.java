package wfg.ltv_econ.plugins;

import com.fs.starfarer.api.campaign.econ.Industry;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.campaign.listeners.DialogCreatorUI;
import com.fs.starfarer.api.campaign.listeners.IndustryOptionProvider;
import com.fs.starfarer.api.impl.campaign.DebugFlags;

import wfg.ltv_econ.economy.WorkerRegistry;
import wfg.ltv_econ.industry.IndustryIOs;
import wfg.ltv_econ.ui.dialogs.AssignWorkersDialog;
import wfg.native_ui.util.NumFormat;

import java.util.ArrayList;
import java.util.List;

import static wfg.native_ui.util.UIConstants.*;

import java.awt.Color;

public class AddWorkerIndustryOption implements IndustryOptionProvider {

    public static Object PluginID = new Object();
    public Industry industry = null;

    public boolean isSuitable(Industry ind, boolean allowUnderConstruction){
        if (ind == null || ind.getMarket() == null ||
            (!allowUnderConstruction && (ind.isBuilding() || ind.isUpgrading()))
        ) return false;
        if (!DebugFlags.COLONY_DEBUG && !ind.getMarket().isPlayerOwned()) return false;

        return IndustryIOs.getIndConfig(ind).workerAssignable;
    }

    @Override
    public List<IndustryOptionData> getIndustryOptions(Industry ind) {
        if (!isSuitable(ind, false)) return null;

        final List<IndustryOptionData> result = new ArrayList<IndustryOptionData>();
        industry = ind;

        final IndustryOptionData opt = new IndustryOptionData("Assign Workers...", PluginID, ind, this);
        opt.color = ind.getMarket().getFaction().getBrightUIColor();
        result.add(opt);

        return result;
    }

    @Override
    public void createTooltip(IndustryOptionData opt, TooltipMakerAPI tooltip, float width) {
        if (opt.id != PluginID) return;

        tooltip.addPara("Assign idle workers to increase this industry's output.", 0f);

        final WorkerRegistry reg = WorkerRegistry.getInstance();
        if (reg.getData(opt.ind) == null) {
            reg.register(opt.ind);
        }

        tooltip.addPara(null, 0f);
        tooltip.addPara(
        "The number of workers that can be assigned to an industry is determined by the colony size, and certain industries have a natural limit on how many workers they can employ. Currently, there are %s workers employed in %s.",
        0f,
        new Color[] {
            highlight,
            base
        },
        NumFormat.engNotation(WorkerRegistry.getInstance().getData(industry).getWorkersAssigned()),
        industry.getCurrentName()
        );
    }

    @Override
    public void addToIndustryTooltip(Industry ind, Industry.IndustryTooltipMode mode, TooltipMakerAPI tooltip, float width, boolean expanded) {

    }

    @Override
    public void optionSelected(IndustryOptionData opt, DialogCreatorUI ui) {
        if (opt.id != PluginID) return;
        
        final WorkerRegistry reg = WorkerRegistry.getInstance();
        if (reg.getData(opt.ind) == null) {
            reg.register(opt.ind);
        }

        final AssignWorkersDialog dialog = new AssignWorkersDialog(opt.ind);
        dialog.show(0.3f, 0.3f);
    }
}
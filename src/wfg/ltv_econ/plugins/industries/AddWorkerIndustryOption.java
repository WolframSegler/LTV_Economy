package wfg.ltv_econ.plugins.industries;

import com.fs.starfarer.api.campaign.econ.Industry;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.campaign.listeners.DialogCreatorUI;
import com.fs.starfarer.api.campaign.listeners.IndustryOptionProvider;
import com.fs.starfarer.api.impl.campaign.DebugFlags;

import wfg.ltv_econ.config.IndustryConfigManager;
import wfg.ltv_econ.economy.registry.WorkerRegistry;
import wfg.ltv_econ.ui.marketInfo.dialogs.AssignWorkersDialog;
import wfg.native_ui.util.NumFormat;

import java.util.ArrayList;
import java.util.List;

import static wfg.native_ui.util.UIConstants.*;

import java.awt.Color;

public class AddWorkerIndustryOption implements IndustryOptionProvider {

    public static final Object pluginID = new Object();

    private static final boolean isSuitable(Industry ind){
        if (ind == null || ind.getMarket() == null || (ind.isBuilding() || ind.isUpgrading())) return false;
        if (!DebugFlags.COLONY_DEBUG && !ind.getMarket().isPlayerOwned()) return false;

        return IndustryConfigManager.getIndConfig(ind).workerAssignable;
    }

    @Override
    public List<IndustryOptionData> getIndustryOptions(Industry ind) {
        if (!isSuitable(ind)) return null;

        final List<IndustryOptionData> result = new ArrayList<>();

        final IndustryOptionData opt = new IndustryOptionData("Assign Workers...", pluginID, ind, this);
        opt.color = ind.getMarket().getFaction().getBrightUIColor();
        result.add(opt);

        return result;
    }

    @Override
    public void createTooltip(IndustryOptionData opt, TooltipMakerAPI tooltip, float width) {
        if (opt.id != pluginID) return;

        tooltip.addPara("Assign idle workers to increase this industry's output.\n", 0f);

        tooltip.addPara(
        "The number of workers that can be assigned to an industry is determined by the colony size, and certain industries have a natural limit on how many workers they can employ. Currently, there are %s workers employed in %s.",
        0f,
        new Color[] {
            highlight,
            base
        },
        NumFormat.engNotate(WorkerRegistry.instance().getRegisterData(opt.ind).getWorkersAssigned()),
        opt.ind.getCurrentName()
        );
    }

    @Override
    public void addToIndustryTooltip(Industry ind, Industry.IndustryTooltipMode mode, TooltipMakerAPI tp, float w, boolean exp) {}

    @Override
    public void optionSelected(IndustryOptionData opt, DialogCreatorUI ui) {
        if (opt.id != pluginID) return;

        new AssignWorkersDialog(opt.ind).show(0.3f, 0.3f);
    }
}
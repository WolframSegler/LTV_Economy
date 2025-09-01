package wfg_ltv_econ.plugins;

import wfg_ltv_econ.economy.EconomyEngine;
import wfg_ltv_econ.economy.WorkerRegistry;
import wfg_ltv_econ.ui.dialogs.AssignWorkersDialog;
import wfg_ltv_econ.util.NumFormat;

import com.fs.starfarer.api.campaign.econ.Industry;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.campaign.listeners.DialogCreatorUI;
import com.fs.starfarer.api.campaign.listeners.IndustryOptionProvider;
import com.fs.starfarer.api.util.Misc;

import java.util.ArrayList;
import java.util.List;
import java.awt.Color;

public class AddWorkerIndustryOption implements IndustryOptionProvider {

    public static Object PluginID = new Object();
    public Industry industry = null;

    public boolean isSuitable(Industry ind, boolean allowUnderConstruction){
        if (ind == null ||
            ind.getMarket() == null ||
            (!allowUnderConstruction && (ind.isBuilding() || ind.isUpgrading()))
        ) {
            return false;
        }

        if (!EconomyEngine.getInstance().ind_config.get(ind.getId()).workerAssignable) {
            return false;
        }
        return true;
    }

    @Override
    public List<IndustryOptionData> getIndustryOptions(Industry ind) {
        if (!isSuitable(ind, false)) return null;

        List<IndustryOptionData> result = new ArrayList<IndustryOptionData>();
        industry = ind;

        IndustryOptionData opt = new IndustryOptionData("Assign Workers...", PluginID, ind, this);
        opt.color = ind.getMarket().getFaction().getBaseUIColor();
        result.add(opt);

        return result;
    }

    @Override
    public void createTooltip(IndustryOptionData opt, TooltipMakerAPI tooltip, float width) {
        if (opt.id != PluginID) {
            return;
        }

        tooltip.addPara("Assign idle workers to increase this industry's output.", 0f);

        if (industry == null) {
            return;
        }
        tooltip.addPara(null, 0f);
        tooltip.addPara(
        "The number of workers that can be assigned to an industry is determined by the colony size, and certain industries have a natural limit on how many workers they can employ. Currently, there are %s workers employed in %s.",
        0f,
        new Color[] {
            Misc.getHighlightColor(),
            Misc.getBasePlayerColor()
        },
        NumFormat.engNotation(WorkerRegistry.getInstance().get(
            industry.getMarket().getId(), industry.getId()).getWorkersAssigned()),
        industry.getCurrentName()
        );
    }

    @Override
    public void addToIndustryTooltip(Industry ind, Industry.IndustryTooltipMode mode, TooltipMakerAPI tooltip, float width, boolean expanded) {

    }

    @Override
    public void optionSelected(IndustryOptionData opt, DialogCreatorUI ui) {
        if (opt.id != PluginID) {
            return;
        }

        // On Click
        final int panelWidth = 540;
        final int panelHeight = 400;
        ui.showDialog(panelWidth, panelHeight, new AssignWorkersDialog(opt.ind, panelWidth, panelHeight));
    }
}
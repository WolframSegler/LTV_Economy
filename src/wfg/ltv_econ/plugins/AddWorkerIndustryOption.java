package wfg.ltv_econ.plugins;

import com.fs.starfarer.api.campaign.econ.Industry;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.campaign.listeners.DialogCreatorUI;
import com.fs.starfarer.api.campaign.listeners.IndustryOptionProvider;
import com.fs.starfarer.api.impl.campaign.DebugFlags;

import wfg.ltv_econ.economy.EconomyEngine;
import wfg.ltv_econ.economy.WorkerRegistry;
import wfg.ltv_econ.ui.dialogs.AssignWorkersDialog;
import wfg.wrap_ui.util.NumFormat;

import java.util.ArrayList;
import java.util.List;

import static wfg.wrap_ui.util.UIConstants.*;

import java.awt.Color;

public class AddWorkerIndustryOption implements IndustryOptionProvider {

    public static Object PluginID = new Object();
    public Industry industry = null;

    public boolean isSuitable(Industry ind, boolean allowUnderConstruction){
        if (ind == null || ind.getMarket() == null ||
            (!allowUnderConstruction && (ind.isBuilding() || ind.isUpgrading()))
        ) return false;
        if (!DebugFlags.COLONY_DEBUG && !ind.getMarket().isPlayerOwned()) return false;

        return EconomyEngine.isWorkerAssignable(ind);
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
        if (reg.getData(opt.ind.getMarket().getId(), opt.ind.getId()) == null) {
            reg.register(opt.ind.getMarket(), opt.ind);
        }

        tooltip.addPara(null, 0f);
        tooltip.addPara(
        "The number of workers that can be assigned to an industry is determined by the colony size, and certain industries have a natural limit on how many workers they can employ. Currently, there are %s workers employed in %s.",
        0f,
        new Color[] {
            highlight,
            base
        },
        NumFormat.engNotation(WorkerRegistry.getInstance().getData(
            industry.getMarket().getId(), industry.getSpec()).getWorkersAssigned()),
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
        final WorkerRegistry reg = WorkerRegistry.getInstance();
        if (reg.getData(opt.ind.getMarket().getId(), opt.ind.getId()) == null) {
            reg.register(opt.ind.getMarket(), opt.ind);
        }

        // On Click
        final int panelWidth = 540;
        final int panelHeight = 400;
        ui.showDialog(panelWidth, panelHeight, new AssignWorkersDialog(opt.ind, panelWidth, panelHeight));
    }
}
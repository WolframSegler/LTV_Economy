package wfg_ltv_econ.industry;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CargoStackAPI;
import com.fs.starfarer.api.campaign.SpecialItemData;
import com.fs.starfarer.api.campaign.SpecialItemSpecAPI;
import com.fs.starfarer.api.campaign.econ.Industry;
import com.fs.starfarer.api.campaign.impl.items.GenericSpecialItemPlugin;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.impl.campaign.econ.impl.GenericInstallableItemPlugin;
import com.fs.starfarer.api.impl.campaign.econ.impl.InstallableItemEffect;

public class LtvGenericInstallableItemPlugin extends GenericInstallableItemPlugin {
	
	public LtvGenericInstallableItemPlugin(Industry industry) {
		super(industry);
	}

	@Override
	public boolean isInstallableItem(CargoStackAPI stack) {
		if (!stack.isSpecialStack()) return false;
		
		String [] industries = stack.getPlugin().getSpec().getParams().split(",");
		Set<String> all = new HashSet<String>();
		for (String ind: industries) all.add(ind.trim());
		if (!all.contains(industry.getId())) return false;
		
		return LtvItemEffectsRepo.ITEM_EFFECTS.containsKey(stack.getSpecialDataIfSpecial().getId());
	}
	@Override
	public boolean canBeInstalled(SpecialItemData data) {
		InstallableItemEffect effect = LtvItemEffectsRepo.ITEM_EFFECTS.get(data.getId());
		if (effect != null) {
			//return effect.canBeInstalledIn(industry);
			List<String> unmet = effect.getUnmetRequirements(industry);
			return unmet == null || unmet.isEmpty();
		}
		return true;
	}


	@Override
	public void addItemDescription(TooltipMakerAPI text, SpecialItemData data, InstallableItemDescriptionMode mode) {
		InstallableItemEffect effect = LtvItemEffectsRepo.ITEM_EFFECTS.get(data.getId());
		if (effect != null) {
			List<String> unmet = effect.getUnmetRequirements(industry);
			boolean canInstall = unmet == null || unmet.isEmpty();
			if (!canInstall) {
				GenericSpecialItemPlugin.addReqsSection(industry, effect, text, true, 0f);
			} else {
				effect.addItemDescription(industry, text, data, mode);
			}
		}
	}

	@Override
	public void createMenuItemTooltip(TooltipMakerAPI tooltip, boolean expanded) {
		float opad = 10f;

		tooltip.addPara("Certain Domain-era artifacts might be installed here to improve the colony. " +
						"Only one such artifact may be installed at an industry at a time.", 0f);

		SpecialItemData data = industry.getSpecialItem();
		if (data == null) {
			tooltip.addPara(getNoItemCurrentlyInstalledText() + ".", opad);
		} else {
			InstallableItemEffect effect = LtvItemEffectsRepo.ITEM_EFFECTS.get(data.getId());
			
			SpecialItemSpecAPI spec = Global.getSettings().getSpecialItemSpec(data.getId());
			TooltipMakerAPI text = tooltip.beginImageWithText(spec.getIconName(), 48);
			effect.addItemDescription(industry, text, data, InstallableItemDescriptionMode.INDUSTRY_MENU_TOOLTIP);
			tooltip.addImageWithText(opad);
		}
	}
}
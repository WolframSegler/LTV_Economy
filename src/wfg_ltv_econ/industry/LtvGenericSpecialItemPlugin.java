package wfg_ltv_econ.industry;

import java.awt.Color;
import com.fs.starfarer.api.campaign.impl.items.GenericSpecialItemPlugin;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CargoTransferHandlerAPI;
import com.fs.starfarer.api.campaign.SpecialItemData;
import com.fs.starfarer.api.campaign.econ.InstallableIndustryItemPlugin.InstallableItemDescriptionMode;
import com.fs.starfarer.api.impl.campaign.econ.impl.InstallableItemEffect;
import com.fs.starfarer.api.ui.LabelAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;

public class LtvGenericSpecialItemPlugin extends GenericSpecialItemPlugin {

	@Override
	public void createTooltip(TooltipMakerAPI tooltip, boolean expanded, CargoTransferHandlerAPI transferHandler, Object stackSource) {
		
		float pad = 0f;
		float opad = 10f;
		
		if (!Global.CODEX_TOOLTIP_MODE) {
			tooltip.addTitle(getName());
		} else {
			tooltip.addSpacer(-opad);
		}
		
		LabelAPI design = null;
		
		if (!tooltipIsForPlanetSearch) {
			design = Misc.addDesignTypePara(tooltip, getDesignType(), opad);
		}
		
		float bulletWidth = 86f;
		if (design != null) {
			bulletWidth = design.computeTextWidth("Design type: ");
		}
		
		InstallableItemEffect effect = LtvItemEffectsRepo.ITEM_EFFECTS.get(getId());
		if (effect != null) {
			tooltip.setBulletWidth(bulletWidth);
			tooltip.setBulletColor(Misc.getGrayColor());
			
			tooltip.setBulletedListMode("Installed in:");
			addInstalledInSection(tooltip, opad);
			tooltip.setBulletedListMode("Requires:");
			addReqsSection(null, effect, tooltip, false, pad);
			if (effect.getSpecialNotesName() != null) {
				tooltip.setBulletedListMode(effect.getSpecialNotesName() + ":");
				addSpecialNotesSection(null, effect, tooltip, false, pad);
			}
			
			tooltip.setBulletedListMode(null);
			
			if (Global.CODEX_TOOLTIP_MODE) {
				tooltip.setParaSmallInsignia();
			}
			
			if (!tooltipIsForPlanetSearch) {
				if (!spec.getDesc().isEmpty()) {
					Color c = Misc.getTextColor();
					tooltip.addPara(spec.getDesc(), c, opad);
				}
			}
			
			if (!tooltipIsForPlanetSearch) {
				effect.addItemDescription(null, tooltip, new SpecialItemData(getId(), null), InstallableItemDescriptionMode.CARGO_TOOLTIP);
			}
		} else {
			if (!spec.getDesc().isEmpty() && !tooltipIsForPlanetSearch) {
				Color c = Misc.getTextColor();
				if (Global.CODEX_TOOLTIP_MODE) {
					tooltip.setParaSmallInsignia();
				}
				tooltip.addPara(spec.getDesc(), c, opad);
			}
		}
			
		addCostLabel(tooltip, opad, transferHandler, stackSource);
	}	
}
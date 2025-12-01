package wfg.ltv_econ.industry;

import java.awt.Color;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.econ.CommoditySpecAPI;
import com.fs.starfarer.api.combat.MutableStat;
import com.fs.starfarer.api.impl.campaign.econ.impl.BaseIndustry;
import com.fs.starfarer.api.impl.campaign.ids.Stats;
import com.fs.starfarer.api.impl.campaign.intel.events.ht.HyperspaceTopographyEventIntel;
import com.fs.starfarer.api.impl.campaign.intel.events.ht.HyperspaceTopographyEventIntel.Stage;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;

public class LtvWaystation extends BaseIndustry {

	public static final float opad = 10;
	
	public static float UPKEEP_MULT_PER_DEFICIT = 0.1f;
	public static float BASE_ACCESSIBILITY = 0.1f;
	
	public static float IMPROVE_ACCESSIBILITY = 0.2f;
	
	public static float ALPHA_CORE_ACCESSIBILITY = 0.2f;
	
	
	public void apply() {
		super.apply(true);
		
		market.setHasWaystation(true);
		if (BASE_ACCESSIBILITY > 0) {
			market.getAccessibilityMod().modifyFlat(getModId(0), BASE_ACCESSIBILITY, getNameForModifier());
		}
		
		final HyperspaceTopographyEventIntel intel = HyperspaceTopographyEventIntel.get();
		if (intel != null && intel.isStageActive(Stage.SLIPSTREAM_DETECTION)) {
			market.getStats().getDynamic().getMod(Stats.SLIPSTREAM_REVEAL_RANGE_LY_MOD).modifyFlat(
				getModId(0), HyperspaceTopographyEventIntel.WAYSTATION_BONUS, getNameForModifier()
			);
		}
		
		if (!isFunctional()) {
			unapply();
		}
	}

	@Override
	public void unapply() {
		super.unapply();
		
		market.setHasWaystation(false);
		market.getAccessibilityMod().unmodifyFlat(getModId(0));
		market.getAccessibilityMod().unmodifyFlat(getModId(1));
		market.getAccessibilityMod().unmodifyFlat(getModId(2));

		getSupplyBonus().unmodifyMult(getModId(1));
		
		market.getStats().getDynamic().getMod(Stats.SLIPSTREAM_REVEAL_RANGE_LY_MOD).unmodifyFlat(getModId(0));
	}

	@Override
	protected void addPostDescriptionSection(TooltipMakerAPI tooltip, IndustryTooltipMode mode) {
		if (!market.isPlayerOwned()) return;
		
		tooltip.addPara("Increases the range at which slipstreams are detected around the colony by %s, once "
			+ "the capability to do so is available.", opad, Misc.getHighlightColor(),
			"" + (int)HyperspaceTopographyEventIntel.WAYSTATION_BONUS);
	}

	protected boolean hasPostDemandSection(boolean hasDemand, IndustryTooltipMode mode) {
		return mode != IndustryTooltipMode.NORMAL || isFunctional();
	}
	
	@Override
	protected void addPostDemandSection(TooltipMakerAPI tooltip, boolean hasDemand, IndustryTooltipMode mode) {
		if (mode != IndustryTooltipMode.NORMAL || isFunctional()) {			
			MutableStat fake = new MutableStat(0);
			
			if (BASE_ACCESSIBILITY > 0) {
				fake.modifyFlat(getModId(0), BASE_ACCESSIBILITY, getNameForModifier());
			}

			String totalStr = "+" + (int)Math.round(BASE_ACCESSIBILITY * 100f) + "%";
			Color h = Misc.getHighlightColor();
			if (BASE_ACCESSIBILITY < 0) {
				h = Misc.getNegativeHighlightColor();
				totalStr = "" + (int)Math.round(BASE_ACCESSIBILITY * 100f) + "%";
			}
			if (BASE_ACCESSIBILITY >= 0) {
				tooltip.addPara("Accessibility bonus: %s", opad, h, totalStr);
			} else {
				tooltip.addPara("Accessibility penalty: %s", opad, h, totalStr);
			}
			
			tooltip.addPara("As long as demand is met, allows the colony to stockpile fuel, supplies, and crew, even " +
				"if it does not produce them locally.", opad);
		}
	}

	@Override
	protected void applyAlphaCoreModifiers() {
		if (market.isPlayerOwned()) {
			getSupplyBonus().modifyMult(getModId(1), 2);
		}
	}
	
	@Override
	protected void applyNoAICoreModifiers() {
		if (market.isPlayerOwned()) {
			getSupplyBonus().unmodifyMult(getModId(1));
		}
	}
	
	@Override
	protected void applyAlphaCoreSupplyAndDemandModifiers() {
		demandReduction.modifyFlat(getModId(0), DEMAND_REDUCTION, "Alpha core");
	}
	
	@Override
	protected void addAlphaCoreDescription(TooltipMakerAPI tooltip, AICoreDescriptionMode mode) {
		final Color highlight = Misc.getHighlightColor();
		
		String pre = "Alpha-level AI core currently assigned. ";
		if (mode == AICoreDescriptionMode.MANAGE_CORE_DIALOG_LIST || mode == AICoreDescriptionMode.INDUSTRY_TOOLTIP) {
			pre = "Alpha-level AI core. ";
		}
		final String aStr = (int)Math.round(ALPHA_CORE_ACCESSIBILITY * 100f) + "%";
		
		if (mode == AICoreDescriptionMode.INDUSTRY_TOOLTIP) {
			final CommoditySpecAPI coreSpec = Global.getSettings().getCommoditySpec(aiCoreId);
			final TooltipMakerAPI text = tooltip.beginImageWithText(coreSpec.getIconName(), 48);
			text.addPara(pre + "Reduces upkeep cost by %s. Reduces demand by %s unit. " +
				"Greatly increases stockpiles.", 0f, highlight,
				"" + (int)((1f - UPKEEP_MULT) * 100f) + "%", "" + DEMAND_REDUCTION,
				aStr);
			tooltip.addImageWithText(opad);
			return;
		}
		
		tooltip.addPara(pre + "Reduces upkeep cost by %s. Reduces demand by %s unit. " +
			"Greatly increases stockpiles.", opad, highlight,
			"" + (int)((1f - UPKEEP_MULT) * 100f) + "%", "" + DEMAND_REDUCTION,
			aStr);
		
	}
	
	@Override
	public boolean isAvailableToBuild() {
		return market.hasSpaceport();
	}
	
	@Override
	public String getUnavailableReason() {
		return "Requires a functional spaceport";
	}

	@Override
	public boolean canImprove() {
		return true;
	}
	
	@Override
	protected void applyImproveModifiers() {
		if (isImproved()) {
			market.getAccessibilityMod().modifyFlat(getModId(3), IMPROVE_ACCESSIBILITY,
				getImprovementsDescForModifiers() + " (" + getNameForModifier() + ")"
			);
		} else {
			market.getAccessibilityMod().unmodifyFlat(getModId(3));
		}
	}
	
	@Override
	public void addImproveDesc(TooltipMakerAPI info, ImprovementDescriptionMode mode) {
		final Color highlight = Misc.getHighlightColor();
		
		final String aStr = (int)Math.round(IMPROVE_ACCESSIBILITY * 100f) + "%";
		
		if (mode == ImprovementDescriptionMode.INDUSTRY_TOOLTIP) {
			info.addPara("Accessibility increased by %s.", 0f, highlight, aStr);
		} else {
			info.addPara("Increases accessibility by %s.", 0f, highlight, aStr);
		}

		info.addSpacer(opad);
		super.addImproveDesc(info, mode);
	}
}
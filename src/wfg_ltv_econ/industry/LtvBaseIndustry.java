package wfg_ltv_econ.industry;

import java.util.ArrayList;
import java.util.List;
import java.awt.Color;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.campaign.econ.CommodityOnMarketAPI;
import com.fs.starfarer.api.campaign.econ.CommoditySpecAPI;
import com.fs.starfarer.api.campaign.econ.Industry;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.campaign.econ.MarketImmigrationModifier;
import com.fs.starfarer.api.campaign.econ.MutableCommodityQuantity;
import com.fs.starfarer.api.campaign.listeners.ListenerUtil;
import com.fs.starfarer.api.combat.MutableStat;
import com.fs.starfarer.api.combat.MutableStat.StatMod;
import com.fs.starfarer.api.impl.SharedUnlockData;
import com.fs.starfarer.api.impl.campaign.DebugFlags;
import com.fs.starfarer.api.impl.campaign.econ.impl.BaseIndustry;
import com.fs.starfarer.api.impl.campaign.econ.impl.InstallableItemEffect;
import com.fs.starfarer.api.impl.campaign.ids.Industries;
import com.fs.starfarer.api.impl.campaign.ids.Strings;
import com.fs.starfarer.api.impl.campaign.ids.Tags;
import com.fs.starfarer.api.impl.codex.CodexDataV2;
import com.fs.starfarer.api.loading.IndustrySpecAPI;
import com.fs.starfarer.api.ui.Alignment;
import com.fs.starfarer.api.ui.IconRenderMode;
import com.fs.starfarer.api.ui.LabelAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.ui.UIComponentAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI.StatModValueGetter;
import com.fs.starfarer.api.util.Misc;
import com.fs.starfarer.api.util.Pair;

import wfg_ltv_econ.economy.CommodityStats;
import wfg_ltv_econ.economy.EconomyEngine;
import wfg_ltv_econ.economy.LtvCompatibilityLayer;
import wfg_ltv_econ.util.NumFormat;
import wfg_ltv_econ.util.UiUtils;

public abstract class LtvBaseIndustry extends BaseIndustry {

	public static final float DEFAULT_IMPROVE_PRODUCTION_BONUS = 1.3f; // +30% output

	public static final float ALPHA_CORE_PRODUCTION_BOOST = 1.3f; // +30% output

	public static final float ALPHA_CORE_UPKEEP_REDUCTION_MULT = 0.75f; // -20% upkeep
	public static final float BETA_CORE_UPKEEP_REDUCTION_MULT = 0.75f; // -20% upkeep

	public static final float ALPHA_CORE_INPUT_REDUCTION = 0.75f; // -20% input
	public static final float BETA_CORE_INPUT_REDUCTION = 0.75f; // -20% input
	public static final float GAMMA_CORE_INPUT_REDUCTION = 0.75f; // -20% input

	@Override
	protected LtvBaseIndustry clone() {
		LtvBaseIndustry copy = null;

		copy = (LtvBaseIndustry) super.clone();

		return copy;
	}

	public LtvBaseIndustry() {}

	@Override
	public void apply(boolean updateIncomeAndUpkeep) {
		updateSupplyAndDemandModifiers();

		if (updateIncomeAndUpkeep) {
			updateIncomeAndUpkeep();
		}
		
		applyAICoreModifiers();
		applyImproveModifiers();

		if (this instanceof MarketImmigrationModifier) {
			market.addTransientImmigrationModifier((MarketImmigrationModifier) this);
		}

		if (special != null) {
			InstallableItemEffect effect = LtvItemEffectsRepo.ITEM_EFFECTS.get(special.getId());
			if (effect != null) {
				List<String> unmet = effect.getUnmetRequirements(this);
				if (unmet == null || unmet.isEmpty()) {
					effect.apply(this);
				} else {
					effect.unapply(this);
				}
			}
		}
	}

	@Override
	public void updateIncomeAndUpkeep() {
		int sizeMult = market.getSize() - 2;
		float stabilityMult = market.getIncomeMult().getModifiedValue();
		float upkeepMult = market.getUpkeepMult().getModifiedValue();

		int income = (int) (getSpec().getIncome() * sizeMult);
		if (income != 0) {
			getIncome().modifyFlatAlways("ind_base", income, "Base value");
			getIncome().modifyMultAlways("ind_stability", stabilityMult, "Market income multiplier");
		} else {
			getIncome().unmodifyFlat("ind_base");
			getIncome().unmodifyMult("ind_stability");
		}

		int upkeep = (int) (getSpec().getUpkeep() * sizeMult);
		if (upkeep != 0) {
			getUpkeep().modifyFlatAlways("ind_base", upkeep, "Base value");
			getUpkeep().modifyMultAlways("ind_hazard", upkeepMult, "Market upkeep multiplier");
		} else {
			getUpkeep().unmodifyFlat("ind_base");
			getUpkeep().unmodifyMult("ind_hazard");
		}

		applyAICoreToIncomeAndUpkeep();

		if (!isFunctional()) {
			getIncome().unmodifyFlat("ind_base");
			getIncome().unmodifyMult("ind_stability");
		}
	}

	@Override
	public float getBaseUpkeep() {
		float sizeMult = market.getSize();
		sizeMult = Math.max(1, sizeMult - 2);
		return getSpec().getUpkeep() * sizeMult;
	}

	protected float dayTracker = -1;

	@Override
	public void advance(float day) {
		if (dayTracker == -1) {
			dayTracker = day;
		}
		if (dayTracker != day) {
			boolean disrupted = isDisrupted();
			if (!disrupted && wasDisrupted) {
				disruptionFinished();
			}
			wasDisrupted = disrupted;

			if (building && !disrupted) {
				buildProgress++;

				if ((buildProgress >= buildTime) || DebugFlags.COLONY_DEBUG) {
					finishBuildingOrUpgrading();
				}
			}
		}
	}

	@Override
	public MutableStat getIncome() {
		return income;
	}

	@Override
	public MutableStat getUpkeep() {
		return upkeep;
	}

	public Pair<String, Float> ltv_getMaxDeficit(String... commodityIds) {
		// 1 is no deficit and 0 is 100% deficit
		Pair<String, Float> result = new Pair<String, Float>();
		result.two = 1f;
		if (Global.CODEX_TOOLTIP_MODE || !EconomyEngine.isInitialized()) return result;

		for (String id : commodityIds) {
			final CommodityStats stats = EconomyEngine.getInstance().getComStats(id, market.getId());
			if (stats == null) {
				return result;
			}

			float available = stats.getStoredCoverageRatio();

			if (available < result.two) {
				result.one = id;
				result.two = available;
			}
		}
		return result;
	}

	@Override
	public void createTooltip(IndustryTooltipMode mode, TooltipMakerAPI tooltip, boolean expanded) {

		if (getSpec() != null && getSpec().hasTag(Tags.CODEX_UNLOCKABLE)) {
			SharedUnlockData.get().reportPlayerAwareOfIndustry(getSpec().getId(), true);
		}
		tooltip.setCodexEntryId(CodexDataV2.getIndustryEntryId(getSpec().getId()));

		currTooltipMode = mode;

		final float pad = 3f;
		final float opad = 10f;

		FactionAPI faction = market.getFaction();
		Color color = faction.getBaseUIColor();
		Color dark = faction.getDarkUIColor();

		Color gray = Misc.getGrayColor();
		Color highlight = Misc.getHighlightColor();
		Color bad = Misc.getNegativeHighlightColor();

		MarketAPI copy = market.clone();
		// the copy is a shallow copy and its conditions point to the original market
		// so, make it share the suppressed conditions list, too, otherwise
		// e.g. SolarArray will suppress conditions in the original market and the copy
		// will still apply them
		copy.setSuppressedConditions(market.getSuppressedConditions());
		copy.setRetainSuppressedConditionsSetWhenEmpty(true);
		market.setRetainSuppressedConditionsSetWhenEmpty(true);
		MarketAPI orig = market;

		market = copy;
		boolean needToAddIndustry = !market.hasIndustry(getId());

		if (needToAddIndustry)
			market.getIndustries().add(this);

		if (mode != IndustryTooltipMode.NORMAL) {
			market.clearCommodities();
			for (CommodityOnMarketAPI curr : market.getAllCommodities()) {
				curr.getAvailableStat().setBaseValue(100);
			}
		}

		market.reapplyConditions();
		reapply();

		String type = "";
		if (isIndustry())
			type = " - Industry";
		if (isStructure())
			type = " - Structure";

		tooltip.addTitle(getCurrentName() + type, color);

		String desc = spec.getDesc();
		String override = getDescriptionOverride();
		if (override != null) {
			desc = override;
		}
		desc = Global.getSector().getRules().performTokenReplacement(null, desc, market.getPrimaryEntity(), null);

		tooltip.addPara(desc, opad);

		if (isIndustry() && (mode == IndustryTooltipMode.ADD_INDUSTRY ||
				mode == IndustryTooltipMode.UPGRADE ||
				mode == IndustryTooltipMode.DOWNGRADE)) {

			int num = Misc.getNumIndustries(market);
			int max = Misc.getMaxIndustries(market);

			// during the creation of the tooltip, the market has both the current industry
			// and the upgrade/downgrade. So if this upgrade/downgrade counts as an
			// industry, it'd count double if
			// the current one is also an industry. Thus reduce num by 1 if that's the case.
			if (isIndustry()) {
				if (mode == IndustryTooltipMode.UPGRADE) {
					for (Industry curr : market.getIndustries()) {
						if (getSpec().getId().equals(curr.getSpec().getUpgrade())) {
							if (curr.isIndustry()) {
								num--;
							}
							break;
						}
					}
				} else if (mode == IndustryTooltipMode.DOWNGRADE) {
					for (Industry curr : market.getIndustries()) {
						if (getSpec().getId().equals(curr.getSpec().getDowngrade())) {
							if (curr.isIndustry()) {
								num--;
							}
							break;
						}
					}
				}
			}

			if (num > max) {

				num--;
				tooltip.addPara("Maximum number of industries reached", bad, opad);
			}
		}

		addRightAfterDescriptionSection(tooltip, mode);

		if (isDisrupted()) {
			int left = (int) getDisruptedDays();
			if (left < 1)
				left = 1;
			String days = "days";
			if (left == 1)
				days = "day";

			tooltip.addPara("Operations disrupted! %s " + days + " until return to normal function.",
					opad, Misc.getNegativeHighlightColor(), highlight, Integer.toString(left));
		}

		if (DebugFlags.COLONY_DEBUG || market.isPlayerOwned()) {
			if (mode == IndustryTooltipMode.NORMAL) {
				if (getSpec().getUpgrade() != null && !isBuilding()) {
					tooltip.addPara("Click to manage or upgrade", Misc.getPositiveHighlightColor(), opad);
				} else {
					tooltip.addPara("Click to manage", Misc.getPositiveHighlightColor(), opad);
				}
			}
		}

		if (mode == IndustryTooltipMode.QUEUED) {
			tooltip.addPara("Click to remove or adjust position in queue", Misc.getPositiveHighlightColor(), opad);
			tooltip.addPara("Currently queued for construction. Does not have any impact on the colony.", opad);

			int left = Math.round((getSpec().getBuildTime()));
			String days = "days";
			if (left < 2)
				days = "day";
			tooltip.addPara("Requires %s " + days + " to build.", opad, highlight, "" + left);

			// return;
		} else if (!isFunctional() && mode == IndustryTooltipMode.NORMAL && !isDisrupted()) {
			tooltip.addPara("Currently under construction and not producing anything or providing other benefits.",
					opad);

			int left = Math.round((buildTime - buildProgress));
			String days = "days";
			if (left < 2)
				days = "day";
			tooltip.addPara("Requires %s more " + days + " to finish building.", opad, highlight, "" + left);
		}

		if (!isAvailableToBuild() &&
				(mode == IndustryTooltipMode.ADD_INDUSTRY ||
						mode == IndustryTooltipMode.UPGRADE ||
						mode == IndustryTooltipMode.DOWNGRADE)) {
			String reason = getUnavailableReason();
			if (reason != null) {
				tooltip.addPara(reason, bad, opad);
			}
		}

		boolean category = getSpec().hasTag(Industries.TAG_PARENT);

		if (!category) {
			int credits = (int) Global.getSector().getPlayerFleet().getCargo().getCredits().get();
			String creditsStr = Misc.getDGSCredits(credits);
			if (mode == IndustryTooltipMode.UPGRADE || mode == IndustryTooltipMode.ADD_INDUSTRY) {
				int cost = (int) getBuildCost();
				String costStr = Misc.getDGSCredits(cost);

				int days = (int) getBuildTime();
				String daysStr = "days";
				if (days == 1)
					daysStr = "day";

				LabelAPI label = null;
				if (mode == IndustryTooltipMode.UPGRADE) {
					label = tooltip.addPara("%s and %s " + daysStr + " to upgrade. You have %s.", opad,
							highlight, costStr, Integer.toString(days), creditsStr);
				} else {
					label = tooltip.addPara("%s and %s " + daysStr + " to build. You have %s.", opad,
							highlight, costStr, Integer.toString(days), creditsStr);
				}
				label.setHighlight(costStr, Integer.toString(days), creditsStr);
				if (credits >= cost) {
					label.setHighlightColors(highlight, highlight, highlight);
				} else {
					label.setHighlightColors(bad, highlight, highlight);
				}
			} else if (mode == IndustryTooltipMode.DOWNGRADE) {
				if (getSpec().getUpgrade() != null) {
					float refundFraction = Global.getSettings().getFloat("industryRefundFraction");

					IndustrySpecAPI spec = Global.getSettings().getIndustrySpec(getSpec().getUpgrade());
					int cost = (int) (spec.getCost() * refundFraction);
					String refundStr = Misc.getDGSCredits(cost);

					tooltip.addPara("%s refunded for downgrade.", opad, highlight, refundStr);
				}
			}

			addPostDescriptionSection(tooltip, mode);

			if (!getIncome().isUnmodified()) {
				int income = getIncome().getModifiedInt();
				tooltip.addPara("Monthly income: %s", opad, highlight, Misc.getDGSCredits(income));
				tooltip.addStatModGrid(300, 65, 10, pad, getIncome(), true, new StatModValueGetter() {
					public String getPercentValue(StatMod mod) {
						return null;
					}

					public String getMultValue(StatMod mod) {
						return null;
					}

					public Color getModColor(StatMod mod) {
						return null;
					}

					public String getFlatValue(StatMod mod) {
						return Misc.getWithDGS(mod.value) + Strings.C;
					}
				});
			}

			if (!getUpkeep().isUnmodified()) {
				int upkeep = getUpkeep().getModifiedInt();
				tooltip.addPara("Monthly upkeep: %s", opad, highlight, Misc.getDGSCredits(upkeep));
				tooltip.addStatModGrid(300, 65, 10, pad, getUpkeep(), true, new StatModValueGetter() {

					public String getPercentValue(StatMod mod) {
						return null;
					}

					public String getMultValue(StatMod mod) {
						return null;
					}

					public Color getModColor(StatMod mod) {
						return null;
					}

					public String getFlatValue(StatMod mod) {
						return Misc.getWithDGS(mod.value) + Strings.C;
					}
				});
			}

			addPostUpkeepSection(tooltip, mode);

			List<MutableStat> outputs = new ArrayList<>(7);
			List<MutableStat> inputs = new ArrayList<>(7);

			boolean hasSupply = false;
			for (MutableCommodityQuantity curr : supply.values()) {
				MutableStat stat = LtvCompatibilityLayer.convertIndSupplyStat(this, curr.getCommodityId());
				if (stat.getModifiedValue() <= 0)
					continue;

				outputs.add(stat);
				hasSupply = true;
			}
			boolean hasDemand = false;
			for (MutableCommodityQuantity curr : demand.values()) {
				MutableStat stat = LtvCompatibilityLayer.convertIndDemandStat(this, curr.getCommodityId());
				if (stat.getModifiedValue() <= 0)
					continue;
				
				inputs.add(stat);
				hasDemand = true;
			}

			final float iconSize = 32f;
			final int itemsPerRow = 3;

			if (hasSupply) {
				tooltip.addSectionHeading("Production", color, dark, Alignment.MID, opad);

				float startY = tooltip.getHeightSoFar() + opad;

				float x = opad;
				float y = startY;
				float sectionWidth = (getTooltipWidth() / itemsPerRow) - opad;
				int count = -1;

				for (MutableCommodityQuantity curr : supply.values()) {
					CommoditySpecAPI commodity = market.getCommodityData(curr.getCommodityId()).getCommodity();
					int pAmount = curr.getQuantity().getModifiedInt();

					if (pAmount < 1) {
						continue;
					}

					// wrap to next line if needed
					count++;
					if (count % itemsPerRow == 0 && count != 0) {
						x = opad;
						y += iconSize + 5f; // line height + padding between rows
					}

					// draw icon
					tooltip.beginIconGroup();
					tooltip.setIconSpacingMedium();
					tooltip.addIcons(commodity, 1, IconRenderMode.NORMAL);
					tooltip.addIconGroup(0f);
					UIComponentAPI iconComp = tooltip.getPrev();

					// Add extra padding for thinner icons
					float actualIconWidth = iconSize * commodity.getIconWidthMult();
					iconComp.getPosition().inTL(x + ((iconSize - actualIconWidth) * 0.5f), y);

					// draw text
					String txt = Strings.X + NumFormat.engNotation(pAmount);
					LabelAPI lbl = tooltip.addPara(txt + " / Day", 0f, highlight, txt);

					float textH = lbl.computeTextHeight(txt);
					float textX = x + iconSize + pad;
					float textY = y + (iconSize - textH) * 0.5f;
					lbl.getPosition().inTL(textX, textY);

					// advance X
					x += sectionWidth + 5f;
				}
				tooltip.setHeightSoFar(y + opad*1.5f);
				UiUtils.resetFlowLeft(tooltip, opad);
			}

			addPostSupplySection(tooltip, hasSupply, mode);

			float headerHeight = 0;
			if (hasDemand || hasPostDemandSection(hasDemand, mode)) {
				tooltip.addSectionHeading("Demand & effects", color, dark, Alignment.MID, opad);
				headerHeight = tooltip.getPrev().getPosition().getHeight();
			}

			if (hasDemand) {
				float startY = tooltip.getHeightSoFar() + opad;
				if (hasSupply) {
					startY += headerHeight;
				}

				float x = opad;
				float y = startY;
				float sectionWidth = (getTooltipWidth() / itemsPerRow) - opad;
				int count = -1;

				for (MutableCommodityQuantity curr : demand.values()) {
					CommodityOnMarketAPI commodity = market.getCommodityData(curr.getCommodityId());
					CommoditySpecAPI commoditySpec = Global.getSettings().getCommoditySpec(curr.getCommodityId());
					int dAmount = curr.getQuantity().getModifiedInt();
					int allDeficit = commodity.getDeficitQuantity();

					if (dAmount < 1) {
						continue;
					}

					// wrap to next line if needed
					count++;
					if (count % itemsPerRow == 0 && count != 0) {
						x = opad;
						y += iconSize + 5f; // line height + padding between rows
					}

					// draw icon
					tooltip.beginIconGroup();
					tooltip.setIconSpacingMedium();
					if (allDeficit > 0) {
						tooltip.addIcons(commodity, 1, IconRenderMode.DIM_RED);
					} else {
						tooltip.addIcons(commodity, 1, IconRenderMode.NORMAL);
					}
					tooltip.addIconGroup(0f);
					UIComponentAPI iconComp = tooltip.getPrev();

					// Add extra padding for thinner icons
					float actualIconWidth = iconSize * commoditySpec.getIconWidthMult();
					iconComp.getPosition().inTL(x + ((iconSize - actualIconWidth) * 0.5f), y);

					// draw text
					String txt = Strings.X + NumFormat.engNotation(dAmount);
					LabelAPI lbl = tooltip.addPara(txt + " / Day", 0f, highlight, txt);

					float textH = lbl.computeTextHeight(txt);
					float textX = x + iconSize + pad;
					float textY = y + (iconSize - textH) * 0.5f;
					lbl.getPosition().inTL(textX, textY);

					// advance X
					x += sectionWidth + 5f;
				}
				tooltip.setHeightSoFar(y + opad*1.5f);
				UiUtils.resetFlowLeft(tooltip, opad);
			}

			addPostDemandSection(tooltip, hasDemand, mode);

			if (!needToAddIndustry) {
				addInstalledItemsSection(mode, tooltip, expanded);
				addImprovedSection(mode, tooltip, expanded);

				ListenerUtil.addToIndustryTooltip(this, mode, tooltip, getTooltipWidth(), expanded);
			}

			tooltip.addPara(
					"*Shown production and demand values are already adjusted based on current market size and local conditions.",
					gray, opad);
			tooltip.addSpacer(opad + pad);
		}

		if (needToAddIndustry) {
			unapply();
			market.getIndustries().remove(this);
		}
		market = orig;
		market.setRetainSuppressedConditionsSetWhenEmpty(null);
		if (!needToAddIndustry) {
			reapply();
		}
		market.reapplyConditions();
	}


	@Override
	protected void addAlphaCoreDescription(TooltipMakerAPI tooltip, AICoreDescriptionMode mode) {
		final float opad = 10f;
		final Color highlight = Misc.getHighlightColor();

		String pre = "Alpha-level AI core currently assigned. ";
		if (mode == AICoreDescriptionMode.MANAGE_CORE_DIALOG_LIST || mode == AICoreDescriptionMode.INDUSTRY_TOOLTIP) {
			pre = "Alpha-level AI core. ";
		}
		if (mode == AICoreDescriptionMode.INDUSTRY_TOOLTIP || mode == AICoreDescriptionMode.MANAGE_CORE_TOOLTIP) {
			CommoditySpecAPI coreSpec = Global.getSettings().getCommoditySpec(aiCoreId);
			TooltipMakerAPI text = tooltip.beginImageWithText(coreSpec.getIconName(), 48);
			text.addPara(pre + "Reduces demand by %s. Reduces upkeep cost by %s. " +
					"Increases production by %s. All modifiers are multiplicative", 0f, highlight,
					String.valueOf(Math.round((1f - ALPHA_CORE_INPUT_REDUCTION) * 100f)) + "%",
					String.valueOf(Math.round((1f - ALPHA_CORE_UPKEEP_REDUCTION_MULT) * 100f)) + "%",
					String.valueOf(Math.round((ALPHA_CORE_PRODUCTION_BOOST - 1f) * 100f)) + "%");
			tooltip.addImageWithText(opad);
			return;
		}

		tooltip.addPara(pre + "Reduces demand by %s. Reduces upkeep cost by %s. " +
				"Increases production by %s. All modifiers are multiplicative", 0f, highlight,
				String.valueOf(Math.round((1f - ALPHA_CORE_INPUT_REDUCTION) * 100f)) + "%",
				String.valueOf(Math.round((1f - ALPHA_CORE_UPKEEP_REDUCTION_MULT) * 100f)) + "%",
				String.valueOf(Math.round((ALPHA_CORE_PRODUCTION_BOOST - 1f) * 100f)) + "%");
	}

	@Override
	protected void addBetaCoreDescription(TooltipMakerAPI tooltip, AICoreDescriptionMode mode) {
		final float opad = 10f;
		final Color highlight = Misc.getHighlightColor();

		String pre = "Beta-level AI core currently assigned. ";
		if (mode == AICoreDescriptionMode.MANAGE_CORE_DIALOG_LIST || mode == AICoreDescriptionMode.INDUSTRY_TOOLTIP) {
			pre = "Beta-level AI core. ";
		}
		if (mode == AICoreDescriptionMode.INDUSTRY_TOOLTIP || mode == AICoreDescriptionMode.MANAGE_CORE_TOOLTIP) {
			CommoditySpecAPI coreSpec = Global.getSettings().getCommoditySpec(aiCoreId);
			TooltipMakerAPI text = tooltip.beginImageWithText(coreSpec.getIconName(), 48);
			text.addPara(pre + "Reduces demand by %s.Reduces upkeep cost by %s. All modifiers are multiplicative", opad,
					highlight,
					String.valueOf(Math.round((1f - BETA_CORE_INPUT_REDUCTION) * 100f)) + "%",
					String.valueOf(Math.round((1f - BETA_CORE_UPKEEP_REDUCTION_MULT) * 100f)) + "%");
			tooltip.addImageWithText(opad);
			return;
		}

		tooltip.addPara(pre + "Reduces demand by %s. Reduces upkeep cost by %s. All modifiers are multiplicative", opad,
				highlight,
				String.valueOf(Math.round((1f - BETA_CORE_INPUT_REDUCTION) * 100f)) + "%",
				String.valueOf(Math.round((1f - BETA_CORE_UPKEEP_REDUCTION_MULT) * 100f)) + "%");
	}

	@Override
	protected void addGammaCoreDescription(TooltipMakerAPI tooltip, AICoreDescriptionMode mode) {
		final float opad = 10f;
		final Color highlight = Misc.getHighlightColor();

		String pre = "Gamma-level AI core currently assigned. ";
		if (mode == AICoreDescriptionMode.MANAGE_CORE_DIALOG_LIST || mode == AICoreDescriptionMode.INDUSTRY_TOOLTIP) {
			pre = "Gamma-level AI core. ";
		}
		if (mode == AICoreDescriptionMode.INDUSTRY_TOOLTIP || mode == AICoreDescriptionMode.MANAGE_CORE_TOOLTIP) {
			CommoditySpecAPI coreSpec = Global.getSettings().getCommoditySpec(aiCoreId);
			TooltipMakerAPI text = tooltip.beginImageWithText(coreSpec.getIconName(), 48);

			text.addPara(pre + "Reduces demand by %s. All modifiers are multiplicative", opad, highlight,
					String.valueOf(Math.round((1f - GAMMA_CORE_INPUT_REDUCTION) * 100f)) + "%");
			tooltip.addImageWithText(opad);
			return;
		}

		tooltip.addPara(pre + "Reduces demand by %s. All modifiers are multiplicative", opad, highlight,
				String.valueOf(Math.round((1f - GAMMA_CORE_INPUT_REDUCTION) * 100f)) + "%");
	}

	@Override
	@Deprecated
	protected int getStabilityPenalty() {
		return super.getStabilityPenalty();
	}

	@Override
	public int getImproveStoryPoints() {
		int base = Global.getSettings().getInt("industryImproveBase");
		return base * (int) Math.round(Math.pow(2, Misc.getNumImprovedIndustries(market)));
	}

	@Override
	public void addImproveDesc(TooltipMakerAPI info, ImprovementDescriptionMode mode) {
		float initPad = 0f;
		float opad = 10f;
		boolean addedSomething = false;
		if (canImproveToIncreaseProduction()) {
			if (mode == ImprovementDescriptionMode.INDUSTRY_TOOLTIP) {
				info.addPara("Production increased by %s.", initPad, Misc.getHighlightColor(),
						Strings.X + DEFAULT_IMPROVE_PRODUCTION_BONUS);
			} else {
				info.addPara("Increases production by %s.", initPad, Misc.getHighlightColor(),
						Strings.X + DEFAULT_IMPROVE_PRODUCTION_BONUS);
			}
			initPad = opad;
			addedSomething = true;
		}

		if (mode != ImprovementDescriptionMode.INDUSTRY_TOOLTIP) {

			info.addPara("Each improvement made at a colony doubles the number of " +
					"" + Misc.STORY + " points required to make an additional improvement.", initPad,
					Misc.getStoryOptionColor(), Misc.STORY + " points");
			addedSomething = true;
		}
		if (!addedSomething) {
			info.addSpacer(-opad);
		}
	}
}
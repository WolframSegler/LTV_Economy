package wfg.ltv_econ.industry;

import java.awt.Color;
import java.util.List;
import java.util.ArrayList;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.campaign.econ.CommodityOnMarketAPI;
import com.fs.starfarer.api.campaign.econ.CommoditySpecAPI;
import com.fs.starfarer.api.campaign.econ.Industry;
import com.fs.starfarer.api.campaign.econ.Industry.AICoreDescriptionMode;
import com.fs.starfarer.api.campaign.econ.Industry.ImprovementDescriptionMode;
import com.fs.starfarer.api.campaign.econ.Industry.IndustryTooltipMode;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.campaign.listeners.ListenerUtil;
import com.fs.starfarer.api.combat.MutableStat.StatMod;
import com.fs.starfarer.api.impl.SharedUnlockData;
import com.fs.starfarer.api.impl.campaign.DebugFlags;
import com.fs.starfarer.api.impl.campaign.econ.impl.BaseIndustry;
import com.fs.starfarer.api.impl.campaign.ids.Commodities;
import com.fs.starfarer.api.impl.campaign.ids.Industries;
import com.fs.starfarer.api.impl.campaign.ids.Strings;
import com.fs.starfarer.api.impl.campaign.ids.Tags;
import com.fs.starfarer.api.impl.codex.CodexDataV2;
import com.fs.starfarer.api.loading.IndustrySpecAPI;
import com.fs.starfarer.api.ui.Alignment;
import com.fs.starfarer.api.ui.IconRenderMode;
import com.fs.starfarer.api.ui.LabelAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI.StatModValueGetter;
import com.fs.starfarer.api.ui.UIComponentAPI;
import com.fs.starfarer.api.util.Misc;

import wfg.ltv_econ.economy.CommodityStats;
import wfg.ltv_econ.economy.EconomyEngine;
import wfg.wrap_ui.util.NumFormat;
import wfg.wrap_ui.util.WrapUiUtils;
import wfg.reflection.ReflectionUtils;

public class IndustryTooltips {
    public static final int pad = 3;
	public static final int opad = 10;

    public static final float ALPHA_CORE_PRODUCTION_BOOST = 1.3f;

	public static final float ALPHA_CORE_UPKEEP_REDUCTION_MULT = 0.75f;
	public static final float BETA_CORE_UPKEEP_REDUCTION_MULT = 0.75f;

	public static final float ALPHA_CORE_INPUT_REDUCTION = 0.75f;
	public static final float BETA_CORE_INPUT_REDUCTION = 0.75f;
	public static final float GAMMA_CORE_INPUT_REDUCTION = 0.75f;

    public static final float DEFAULT_IMPROVE_PRODUCTION_BONUS = 1.3f;

    public static void createIndustryTooltip(
		IndustryTooltipMode mode, TooltipMakerAPI tp, boolean expanded, Industry ind
	) {
		if (ind.getSpec() != null && ind.getSpec().hasTag(Tags.CODEX_UNLOCKABLE)) {
			SharedUnlockData.get().reportPlayerAwareOfIndustry(ind.getSpec().getId(), true);
		}
		tp.setCodexEntryId(CodexDataV2.getIndustryEntryId(ind.getSpec().getId()));

		// This line does not do anything
		// ind.currTooltipMode = mode;

		final MarketAPI orgMarket = ind.getMarket();
		final FactionAPI faction = orgMarket.getFaction();
		final Color color = faction.getBaseUIColor();
		final Color dark = faction.getDarkUIColor();

		final Color gray = Misc.getGrayColor();
		final Color highlight = Misc.getHighlightColor();
		final Color bad = Misc.getNegativeHighlightColor();

		MarketAPI copy = ind.getMarket().clone(); // shallow

		copy.setSuppressedConditions(orgMarket.getSuppressedConditions());
		copy.setRetainSuppressedConditionsSetWhenEmpty(true);
		orgMarket.setRetainSuppressedConditionsSetWhenEmpty(true);

		MarketAPI market = copy;
		boolean needToAddIndustry = !market.hasIndustry(ind.getId());

		if (needToAddIndustry)
			market.getIndustries().add(ind);

		if (mode != IndustryTooltipMode.NORMAL) {
			market.clearCommodities();
			for (CommodityOnMarketAPI curr : market.getAllCommodities()) {
				curr.getAvailableStat().setBaseValue(100);
			}
		}

		market.reapplyConditions();
		ind.reapply();

		String type = "";
		if (ind.isIndustry())
			type = " - Industry";
		if (ind.isStructure())
			type = " - Structure";

		tp.addTitle(ind.getCurrentName() + type, color);

		String desc = ind.getSpec().getDesc();
        String override = (String) ReflectionUtils.getMethodsMatching(
            BaseIndustry.class, "getDescriptionOverride", String.class, 0).get(0
        ).invoke(ind);
		if (override != null) {
			desc = override;
		}
		desc = Global.getSector().getRules().performTokenReplacement(
			null, desc, market.getPrimaryEntity(), null
		);

		tp.addPara(desc, opad);

		if (ind.isIndustry() && (mode == IndustryTooltipMode.ADD_INDUSTRY ||
				mode == IndustryTooltipMode.UPGRADE ||
				mode == IndustryTooltipMode.DOWNGRADE)) {

			int num = Misc.getNumIndustries(market);
			int max = Misc.getMaxIndustries(market);

			// during the creation of the tooltip, the market has both the current industry
			// and the upgrade/downgrade. So if this upgrade/downgrade counts as an
			// industry, it'd count double if
			// the current one is also an industry. Thus reduce num by 1 if that's the case.
			if (ind.isIndustry()) {
				if (mode == IndustryTooltipMode.UPGRADE) {
					for (Industry curr : market.getIndustries()) {
						if (ind.getSpec().getId().equals(curr.getSpec().getUpgrade())) {
							if (curr.isIndustry()) {
								num--;
							}
							break;
						}
					}
				} else if (mode == IndustryTooltipMode.DOWNGRADE) {
					for (Industry curr : market.getIndustries()) {
						if (ind.getSpec().getId().equals(curr.getSpec().getDowngrade())) {
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
				tp.addPara("Maximum number of industries reached", bad, opad);
			}
		}

        ReflectionUtils.getMethodsMatching(
            BaseIndustry.class, "addRightAfterDescriptionSection", void.class, 2).get(0
        ).invoke(ind, tp, mode);

		if (ind.isDisrupted()) {
			int left = (int) ind.getDisruptedDays();
			if (left < 1)
				left = 1;
			String days = "days";
			if (left == 1)
				days = "day";

			tp.addPara("Operations disrupted! %s " + days + " until return to normal function.",
					opad, Misc.getNegativeHighlightColor(), highlight, Integer.toString(left));
		}

		if (DebugFlags.COLONY_DEBUG || market.isPlayerOwned()) {
			if (mode == IndustryTooltipMode.NORMAL) {
				if (ind.getSpec().getUpgrade() != null && !ind.isBuilding()) {
					tp.addPara("Click to manage or upgrade", Misc.getPositiveHighlightColor(), opad);
				} else {
					tp.addPara("Click to manage", Misc.getPositiveHighlightColor(), opad);
				}
			}
		}

		if (mode == IndustryTooltipMode.QUEUED) {
			tp.addPara("Click to remove or adjust position in queue", Misc.getPositiveHighlightColor(), opad);
			tp.addPara("Currently queued for construction. Does not have any impact on the colony.", opad);

			int left = Math.round((ind.getSpec().getBuildTime()));
			String days = "days";
			if (left < 2)
				days = "day";
			tp.addPara("Requires %s " + days + " to build.", opad, highlight, "" + left);

		} else if (!ind.isFunctional() && mode == IndustryTooltipMode.NORMAL && !ind.isDisrupted()) {
			tp.addPara(
				"Currently under construction and not producing anything or providing other benefits.",
				opad
			);

			float buildTime = (float) ReflectionUtils.get(ind, "buildTime", float.class, true);
			int left = Math.round((buildTime - ((BaseIndustry)ind).getBuildProgress()));
			String days = "days";
			if (left < 2)
				days = "day";
			tp.addPara("Requires %s more " + days + " to finish building.", opad, highlight, "" + left);
		}

		if (!ind.isAvailableToBuild() &&
				(mode == IndustryTooltipMode.ADD_INDUSTRY ||
						mode == IndustryTooltipMode.UPGRADE ||
						mode == IndustryTooltipMode.DOWNGRADE)) {
			String reason = ind.getUnavailableReason();
			if (reason != null) {
				tp.addPara(reason, bad, opad);
			}
		}

		boolean category = ind.getSpec().hasTag(Industries.TAG_PARENT);

		if (!category) {
			int credits = (int) Global.getSector().getPlayerFleet().getCargo().getCredits().get();
			String creditsStr = Misc.getDGSCredits(credits);
			if (mode == IndustryTooltipMode.UPGRADE || mode == IndustryTooltipMode.ADD_INDUSTRY) {
				int cost = (int) ind.getBuildCost();
				String costStr = Misc.getDGSCredits(cost);

				int days = (int) ind.getBuildTime();
				String daysStr = "days";
				if (days == 1)
					daysStr = "day";

				LabelAPI label = null;
				if (mode == IndustryTooltipMode.UPGRADE) {
					label = tp.addPara("%s and %s " + daysStr + " to upgrade. You have %s.", opad,
							highlight, costStr, Integer.toString(days), creditsStr);
				} else {
					label = tp.addPara("%s and %s " + daysStr + " to build. You have %s.", opad,
							highlight, costStr, Integer.toString(days), creditsStr);
				}
				label.setHighlight(costStr, Integer.toString(days), creditsStr);
				if (credits >= cost) {
					label.setHighlightColors(highlight, highlight, highlight);
				} else {
					label.setHighlightColors(bad, highlight, highlight);
				}
			} else if (mode == IndustryTooltipMode.DOWNGRADE) {
				if (ind.getSpec().getUpgrade() != null) {
					float refundFraction = Global.getSettings().getFloat("industryRefundFraction");

					IndustrySpecAPI spec = Global.getSettings().getIndustrySpec(ind.getSpec().getUpgrade());
					int cost = (int) (spec.getCost() * refundFraction);
					String refundStr = Misc.getDGSCredits(cost);

					tp.addPara("%s refunded for downgrade.", opad, highlight, refundStr);
				}
			}

            ReflectionUtils.getMethodsMatching(
                BaseIndustry.class, "addPostDescriptionSection", void.class, 2).get(0
            ).invoke(ind, tp, mode);

			if (!ind.getIncome().isUnmodified()) {
				int income = ind.getIncome().getModifiedInt();
				tp.addPara("Monthly income: %s", opad, highlight, Misc.getDGSCredits(income));
				tp.addStatModGrid(300, 65, 10, pad, ind.getIncome(), true, new StatModValueGetter() {
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

			if (!ind.getUpkeep().isUnmodified()) {
				int upkeep = ind.getUpkeep().getModifiedInt();
				tp.addPara("Monthly upkeep: %s", opad, highlight, Misc.getDGSCredits(upkeep));
				tp.addStatModGrid(300, 65, 10, pad, ind.getUpkeep(), true, new StatModValueGetter() {

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

            ReflectionUtils.getMethodsMatching(
                BaseIndustry.class, "addPostUpkeepSection", void.class, 2).get(0
            ).invoke(ind, tp, mode);

			List<CommodityStats> supplyList = new ArrayList<>();
			List<CommodityStats> demandList = new ArrayList<>();

			final EconomyEngine engine = EconomyEngine.getInstance();

			for (CommoditySpecAPI spec : EconomyEngine.getEconCommodities()) {
				CommodityStats stats = engine.getComStats(spec.getId(), ind.getMarket().getId());

				if (stats.getLocalProductionStat(ind.getId()).getModifiedInt() > 0) {
					supplyList.add(stats);
				}
				if (stats.getBaseDemandStat(ind.getId()).getModifiedInt() > 0) {
					demandList.add(stats);
				}
			}

			final int iconSize = 32;
			final int itemsPerRow = 3;

			if (!supplyList.isEmpty()) {
				tp.addSectionHeading("Production", color, dark, Alignment.MID, opad);

				final float startY = tp.getHeightSoFar() + opad;

				float x = opad;
				float y = startY;
				float sectionWidth = (ind.getTooltipWidth() / itemsPerRow) - opad;
				int count = -1;

				for (CommodityStats stats : supplyList) {
					CommoditySpecAPI commodity = market.getCommodityData(stats.comID).getCommodity();

					int pAmount = stats.getLocalProductionStat(ind.getId()).getModifiedInt();

					if (pAmount < 1) continue;

					// wrap to next line if needed
					count++;
					if (count % itemsPerRow == 0 && count != 0) {
						x = opad;
						y += iconSize + 5f; // line height + padding between rows
					}

					// draw icon
					tp.beginIconGroup();
					tp.setIconSpacingMedium();
					tp.addIcons(commodity, 1, IconRenderMode.NORMAL);
					tp.addIconGroup(0f);
					UIComponentAPI iconComp = tp.getPrev();

					// Add extra padding for thinner icons
					float actualIconWidth = iconSize * commodity.getIconWidthMult();
					iconComp.getPosition().inTL(x + ((iconSize - actualIconWidth) * 0.5f), y);

					// draw text
					String txt = Strings.X + NumFormat.engNotation(pAmount);
					LabelAPI lbl = tp.addPara(txt + " / Day", 0f, highlight, txt);

					float textH = lbl.computeTextHeight(txt);
					float textX = x + iconSize + pad;
					float textY = y + (iconSize - textH) * 0.5f;
					lbl.getPosition().inTL(textX, textY);

					// advance X
					x += sectionWidth + 5f;
				}
				tp.setHeightSoFar(y + opad*1.5f);
				WrapUiUtils.resetFlowLeft(tp, opad);
			}

            ReflectionUtils.getMethodsMatching(
                BaseIndustry.class, "addPostSupplySection", void.class, 3).get(0
            ).invoke(ind, tp, !supplyList.isEmpty(), mode);

			float headerHeight = 0;
            boolean hasPostDemandSection = (boolean) ReflectionUtils.getMethodsMatching(
                BaseIndustry.class, "hasPostDemandSection", boolean.class, 2).get(0
            ).invoke(ind, !demandList.isEmpty(), mode);
			if (!demandList.isEmpty() || hasPostDemandSection) {
				tp.addSectionHeading("Demand & effects", color, dark, Alignment.MID, opad);
				headerHeight = tp.getPrev().getPosition().getHeight();
			}

			if (!demandList.isEmpty()) {
				float startY = tp.getHeightSoFar() + opad;
				if (!supplyList.isEmpty()) {
					startY += headerHeight;
				}

				float x = opad;
				float y = startY;
				float sectionWidth = (ind.getTooltipWidth() / itemsPerRow) - opad;
				int count = -1;

				for (CommodityStats stats : demandList) {
					CommoditySpecAPI commodity = market.getCommodityData(stats.comID).getCommodity();

					int dAmount = stats.getBaseDemandStat(ind.getId()).getModifiedInt();

					if (dAmount < 1) continue;

					// wrap to next line if needed
					count++;
					if (count % itemsPerRow == 0 && count != 0) {
						x = opad;
						y += iconSize + 5f; // line height + padding between rows
					}

					// draw icon
					tp.beginIconGroup();
					tp.setIconSpacingMedium();
					if (dAmount > 0) {
						tp.addIcons(commodity, 1, IconRenderMode.DIM_RED);
					} else {
						tp.addIcons(commodity, 1, IconRenderMode.NORMAL);
					}
					tp.addIconGroup(0f);
					UIComponentAPI iconComp = tp.getPrev();

					// Add extra padding for thinner icons
					float actualIconWidth = iconSize * commodity.getIconWidthMult();
					iconComp.getPosition().inTL(x + ((iconSize - actualIconWidth) * 0.5f), y);

					// draw text
					String txt = Strings.X + NumFormat.engNotation(dAmount);
					LabelAPI lbl = tp.addPara(txt + " / Day", 0f, highlight, txt);

					float textH = lbl.computeTextHeight(txt);
					float textX = x + iconSize + pad;
					float textY = y + (iconSize - textH) * 0.5f;
					lbl.getPosition().inTL(textX, textY);

					x += sectionWidth + 5f;
				}
				tp.setHeightSoFar(y + opad*1.5f);
				WrapUiUtils.resetFlowLeft(tp, opad);
			}

            ReflectionUtils.getMethodsMatching(
                BaseIndustry.class, "addPostDemandSection", void.class, 3).get(0
            ).invoke(ind, tp, !demandList.isEmpty(), mode);

			if (!needToAddIndustry) {
				addInstalledItemsSection(mode, tp, expanded, ind);
				addImprovedSection(mode, tp, expanded, ind);

				ListenerUtil.addToIndustryTooltip(ind, mode, tp, ind.getTooltipWidth(), expanded);
			}

			tp.addPara("*Shown production and demand values are already adjusted based on current market"
				+ "size and local conditions.",
				gray, opad);
			tp.addSpacer(opad + pad);
		}

		if (needToAddIndustry) {
			ind.unapply();
			market.getIndustries().remove(ind);
		}
		orgMarket.setRetainSuppressedConditionsSetWhenEmpty(null);
		if (!needToAddIndustry) {
			ind.reapply();
		}
		orgMarket.reapplyConditions();
	}

    public static void addInstalledItemsSection(
        IndustryTooltipMode mode, TooltipMakerAPI tooltip, boolean expanded, Industry ind
    ) {
		final FactionAPI faction = ind.getMarket().getFaction();
		final Color color = faction.getBaseUIColor();
		final Color dark = faction.getDarkUIColor();
		
		final LabelAPI heading = tooltip.addSectionHeading("Items", color, dark, Alignment.MID, opad);

		boolean addedSomething = false;
		if (ind.getAICoreId() != null) {
			AICoreDescriptionMode aiCoreDescMode = AICoreDescriptionMode.INDUSTRY_TOOLTIP;
			addAICoreSection(tooltip, ind.getAICoreId(), aiCoreDescMode, ind);
			addedSomething = true;
		}
        boolean r = (boolean) ReflectionUtils.getMethodsMatching(
            BaseIndustry.class, "addNonAICoreInstalledItems", boolean.class, 3).get(0
        ).invoke(ind, mode, tooltip, expanded);
		addedSomething |= r;
		
		if (!addedSomething) {
			heading.setText("No items installed");
			//tooltip.addPara("None.", opad);
		}
	}

    public static void addAICoreSection(
        TooltipMakerAPI tooltip, String coreId, AICoreDescriptionMode mode, Industry ind
    ) {
		if (mode == AICoreDescriptionMode.MANAGE_CORE_TOOLTIP) {
			if (coreId == null) {
				tooltip.addPara("No AI core currently assigned. Click to assign an AI core from your cargo.", opad);
				return;
			}
		}
		
		boolean alpha = coreId.equals(Commodities.ALPHA_CORE); 
		boolean beta = coreId.equals(Commodities.BETA_CORE); 
		boolean gamma = coreId.equals(Commodities.GAMMA_CORE);
		
		if (alpha) {
			addAlphaCoreDescription(tooltip, mode, ind);
		} else if (beta) {
			addBetaCoreDescription(tooltip, mode, ind);
		} else if (gamma) {
			addGammaCoreDescription(tooltip, mode, ind);
		} else {
            ReflectionUtils.getMethodsMatching(
                BaseIndustry.class, "addUnknownCoreDescription", void.class, 3).get(0
            ).invoke(ind, coreId, tooltip, mode);
		}
	}

	public static void addAlphaCoreDescription(
        TooltipMakerAPI tp, AICoreDescriptionMode mode, Industry ind
    ) {
		final float opad = 10f;
		final Color highlight = Misc.getHighlightColor();

		String pre = "Alpha-level AI core currently assigned. ";
		if (mode == AICoreDescriptionMode.MANAGE_CORE_DIALOG_LIST || mode == AICoreDescriptionMode.INDUSTRY_TOOLTIP) {
			pre = "Alpha-level AI core. ";
		}
		if (mode == AICoreDescriptionMode.INDUSTRY_TOOLTIP || mode == AICoreDescriptionMode.MANAGE_CORE_TOOLTIP) {
			CommoditySpecAPI coreSpec = Global.getSettings().getCommoditySpec(ind.getAICoreId());
			TooltipMakerAPI text = tp.beginImageWithText(coreSpec.getIconName(), 48);
			text.addPara(pre + "Reduces demand by %s. Reduces upkeep cost by %s. " +
					"Increases production by %s. All modifiers are multiplicative", 0f, highlight,
					String.valueOf(Math.round((1f - ALPHA_CORE_INPUT_REDUCTION) * 100f)) + "%",
					String.valueOf(Math.round((1f - ALPHA_CORE_UPKEEP_REDUCTION_MULT) * 100f)) + "%",
					String.valueOf(Math.round((ALPHA_CORE_PRODUCTION_BOOST - 1f) * 100f)) + "%");
			tp.addImageWithText(opad);
			return;
		}

		tp.addPara(pre + "Reduces demand by %s. Reduces upkeep cost by %s. " +
				"Increases production by %s. All modifiers are multiplicative", 0f, highlight,
				String.valueOf(Math.round((1f - ALPHA_CORE_INPUT_REDUCTION) * 100f)) + "%",
				String.valueOf(Math.round((1f - ALPHA_CORE_UPKEEP_REDUCTION_MULT) * 100f)) + "%",
				String.valueOf(Math.round((ALPHA_CORE_PRODUCTION_BOOST - 1f) * 100f)) + "%");
	}

	public static void addBetaCoreDescription(
        TooltipMakerAPI tp, AICoreDescriptionMode mode, Industry ind
    ) {
		final float opad = 10f;
		final Color highlight = Misc.getHighlightColor();

		String pre = "Beta-level AI core currently assigned. ";
		if (mode == AICoreDescriptionMode.MANAGE_CORE_DIALOG_LIST || mode == AICoreDescriptionMode.INDUSTRY_TOOLTIP) {
			pre = "Beta-level AI core. ";
		}
		if (mode == AICoreDescriptionMode.INDUSTRY_TOOLTIP || mode == AICoreDescriptionMode.MANAGE_CORE_TOOLTIP) {
			CommoditySpecAPI coreSpec = Global.getSettings().getCommoditySpec(ind.getAICoreId());
			TooltipMakerAPI text = tp.beginImageWithText(coreSpec.getIconName(), 48);
			text.addPara(pre + "Reduces demand by %s.Reduces upkeep cost by %s. All modifiers are multiplicative", opad,
					highlight,
					String.valueOf(Math.round((1f - BETA_CORE_INPUT_REDUCTION) * 100f)) + "%",
					String.valueOf(Math.round((1f - BETA_CORE_UPKEEP_REDUCTION_MULT) * 100f)) + "%");
			tp.addImageWithText(opad);
			return;
		}

		tp.addPara(pre + "Reduces demand by %s. Reduces upkeep cost by %s. All modifiers are multiplicative", opad,
				highlight,
				String.valueOf(Math.round((1f - BETA_CORE_INPUT_REDUCTION) * 100f)) + "%",
				String.valueOf(Math.round((1f - BETA_CORE_UPKEEP_REDUCTION_MULT) * 100f)) + "%");
	}

	public static void addGammaCoreDescription(
        TooltipMakerAPI tp, AICoreDescriptionMode mode, Industry ind
    ) {
		final float opad = 10f;
		final Color highlight = Misc.getHighlightColor();

		String pre = "Gamma-level AI core currently assigned. ";
		if (mode == AICoreDescriptionMode.MANAGE_CORE_DIALOG_LIST || mode == AICoreDescriptionMode.INDUSTRY_TOOLTIP) {
			pre = "Gamma-level AI core. ";
		}
		if (mode == AICoreDescriptionMode.INDUSTRY_TOOLTIP || mode == AICoreDescriptionMode.MANAGE_CORE_TOOLTIP) {
			CommoditySpecAPI coreSpec = Global.getSettings().getCommoditySpec(ind.getAICoreId());
			TooltipMakerAPI text = tp.beginImageWithText(coreSpec.getIconName(), 48);

			text.addPara(pre + "Reduces demand by %s. All modifiers are multiplicative", opad, highlight,
					String.valueOf(Math.round((1f - GAMMA_CORE_INPUT_REDUCTION) * 100f)) + "%");
			tp.addImageWithText(opad);
			return;
		}

		tp.addPara(pre + "Reduces demand by %s. All modifiers are multiplicative", opad, highlight,
				String.valueOf(Math.round((1f - GAMMA_CORE_INPUT_REDUCTION) * 100f)) + "%");
	}

    public static void addImprovedSection(
        IndustryTooltipMode mode, TooltipMakerAPI tooltip, boolean expanded, Industry ind
    ) {
        if (!ind.isImproved()) return;

		tooltip.addSectionHeading(
            "Improvements made", Misc.getStoryOptionColor(), Misc.getStoryDarkColor(), Alignment.MID, opad
        );
		
		tooltip.addSpacer(opad);
		ind.addImproveDesc(tooltip, ImprovementDescriptionMode.INDUSTRY_TOOLTIP);
	}

    public static void addImproveDesc(
        TooltipMakerAPI info, ImprovementDescriptionMode mode, Industry ind
    ) {
		float initPad = 0f;

		boolean addedSomething = false;
        boolean canImprove = (boolean) ReflectionUtils.invoke(ind, "canImproveToIncreaseProduction");
		if (canImprove) {
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

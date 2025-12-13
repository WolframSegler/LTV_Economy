package wfg.ltv_econ.ui.panels;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Collections;
import java.util.List;

import org.lwjgl.input.Keyboard;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.econ.Industry;
import com.fs.starfarer.api.campaign.econ.Industry.IndustryTooltipMode;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI.MarketInteractionMode;
import com.fs.starfarer.api.impl.campaign.DebugFlags;
import com.fs.starfarer.api.impl.campaign.econ.impl.ConstructionQueue.ConstructionQueueItem;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.loading.IndustrySpecAPI;
import com.fs.starfarer.api.ui.Fonts;
import com.fs.starfarer.api.ui.LabelAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.ui.UIPanelAPI;
import com.fs.starfarer.api.util.Misc;
import com.fs.starfarer.api.util.MutableValue;
import com.fs.starfarer.api.ui.Alignment;
import com.fs.starfarer.api.ui.CustomPanelAPI;
import com.fs.starfarer.campaign.ui.marketinfo.IndustryListPanel;
import com.fs.starfarer.campaign.ui.marketinfo.IndustryPickerDialog;

import wfg.ltv_econ.economy.CommodityStats;
import wfg.ltv_econ.industry.IndustryTooltips;
import wfg.ltv_econ.industry.LtvPopulationAndInfrastructure;
import wfg.ltv_econ.ui.panels.LtvIndustryWidget.ConstructionMode;
import wfg.ltv_econ.ui.plugins.IndustryListPanelPlugin;
import wfg.ltv_econ.ui.plugins.IndustryWidgetPlugin;
import wfg.ltv_econ.util.ListenerFactory;
import wfg.ltv_econ.util.UiUtils;
import wfg.wrap_ui.util.CallbackRunnable;
import wfg.wrap_ui.util.WrapUiUtils;
import wfg.wrap_ui.util.WrapUiUtils.AnchorType;
import wfg.wrap_ui.ui.Attachments;
import wfg.wrap_ui.ui.UIState;
import wfg.wrap_ui.ui.UIState.State;
import wfg.wrap_ui.ui.panels.Button;
import wfg.wrap_ui.ui.panels.CustomPanel;
import wfg.wrap_ui.ui.panels.TextPanel;
import wfg.wrap_ui.ui.panels.Button.CutStyle;
import wfg.wrap_ui.ui.panels.CustomPanel.HasTooltip.PendingTooltip;
import wfg.wrap_ui.ui.plugins.BasePanelPlugin;
import wfg.reflection.ReflectionUtils;
import wfg.reflection.ReflectionUtils.ReflectedConstructor;
import static wfg.wrap_ui.util.UIConstants.*;

public class LtvIndustryListPanel extends CustomPanel<
	IndustryListPanelPlugin, LtvIndustryListPanel, UIPanelAPI
> {

	public static final int BUTTON_SECTION_HEIGHT = 45;

	public static final ReflectedConstructor indPickCtor = ReflectionUtils.getConstructorsMatching(IndustryPickerDialog.class, 3).get(0);
	public static ReflectedConstructor indOptCtor = null;
	
	private final List<Object> widgets = new ArrayList<>();
	public final UIPanelAPI dummyWidget;
	private final MarketAPI m_market;

	public List<Object> getWidgets() {
		return widgets;
	}

	public static Comparator<Industry> getIndustryOrderComparator() {
		return Comparator.comparingInt(ind -> ind.getSpec().getOrder());
	}

	private Button buildButton;

	public LtvIndustryListPanel(UIPanelAPI parent, int width, int height, MarketAPI market, 
		UIPanelAPI industryPanel) {
		super(parent, width, height, new IndustryListPanelPlugin());

		m_market = market;

		dummyWidget = ((IndustryListPanel)industryPanel).getWidgets().get(0);
		dummyWidget.setOpacity(0);

		getPlugin().init(this);
		createPanel();
   	}

	public static void setindustryOptionsPanelConstructor(ReflectedConstructor a) {
		indOptCtor = a;
	}

	public void createPanel() {
		clearChildren();
		widgets.clear();

		List<Industry> industries = CommodityStats.getVisibleIndustries(m_market);
		Collections.sort(industries, getIndustryOrderComparator());
		List<ConstructionQueueItem> queuedIndustries = m_market.getConstructionQueue().getItems();
	
		final byte columnAmount = 4;

		CustomPanelAPI widgetWrapper = Global.getSettings().createCustom(
            getPos().getWidth(),
            getPos().getHeight() - BUTTON_SECTION_HEIGHT*1.5f,
            null
        );

		TooltipMakerAPI wrappertp = widgetWrapper.createUIElement(
            getPos().getWidth(),
			getPos().getHeight() - BUTTON_SECTION_HEIGHT*1.4f,
			true
        );

		int wrapperTpHeight = 0;

		// Normal industries
		for (int index = 0; index < industries.size(); index++) {
			int i = index % columnAmount;
			int j = index / columnAmount;
			Industry ind = industries.get(index);

			final LtvIndustryWidget widget = new LtvIndustryWidget(
				wrappertp,
				new IndustryWidgetPlugin(),
				m_market,
				ind,
				this
			);

			wrappertp.addComponent(widget.getPanel()).inTL(
				i * (LtvIndustryWidget.PANEL_WIDTH + opad) + pad,
				wrapperTpHeight = j * (LtvIndustryWidget.TOTAL_HEIGHT + opad)
			);

			addWidgetTooltip(IndustryTooltipMode.NORMAL, ind, widget);

			widgets.add(widget);
		}

		// Queued industries
		for (int index = 0; index < queuedIndustries.size(); index++) {
			int i = (index + industries.size()) % columnAmount;
			int j = (index + industries.size()) / columnAmount;
			Industry ind = m_market.instantiateIndustry(queuedIndustries.get(index).id);

			final LtvIndustryWidget widget = new LtvIndustryWidget(
				wrappertp,
				new IndustryWidgetPlugin(),
				m_market,
				ind,
				this,
				index
			);

			wrappertp.addComponent(widget.getPanel()).inTL(
				i * (LtvIndustryWidget.PANEL_WIDTH + opad) + pad,
				wrapperTpHeight = j * (LtvIndustryWidget.TOTAL_HEIGHT + opad)
			);

			addWidgetTooltip(IndustryTooltipMode.QUEUED, ind, widget);

			widgets.add(widget);
		}
		wrappertp.setHeightSoFar(wrapperTpHeight + LtvIndustryWidget.TOTAL_HEIGHT + opad*1.5f);

		widgetWrapper.addUIElement(wrappertp).inTL(-pad, 0);
		add(widgetWrapper).inTL(0, 0);
		
		TextPanel creditLblPanel = null;
		TextPanel maxIndLblPanel = null;

		{ // creditLbl
			creditLblPanel = new TextPanel(getPanel(), 200, 25, new BasePanelPlugin<>()) {

                @Override
                public void createPanel() {
                    LabelAPI creditLbl = UiUtils.createCreditsLabel(Fonts.INSIGNIA_LARGE, 25);
					creditLbl.setHighlightOnMouseover(true);

					getPos().setSize(
						creditLbl.computeTextWidth(creditLbl.getText()),
						getPos().getHeight()
					);

					add(creditLbl).inBL(0, 0);
                }

                @Override
                public CustomPanelAPI getTpParent() {
                    return getParent();
                }

                @Override
                public TooltipMakerAPI createAndAttachTp() {
					final int tpWidth = 400;

                    TooltipMakerAPI tooltip = getParent().createUIElement(tpWidth, 0, false);

					tooltip.addPara("Credits available.", 0);

					getParent().addUIElement(tooltip);

					WrapUiUtils.anchorPanel(tooltip, getPanel(), AnchorType.TopLeft, 0);

                    return tooltip;
                }
            };
        }

		{ // maxIndLbl
			maxIndLblPanel = new TextPanel(getPanel(), 200, 25, new BasePanelPlugin<>()) {

                @Override
                public void createPanel() {
                    LabelAPI maxIndLbl = UiUtils.createMaxIndustriesLabel(Fonts.INSIGNIA_LARGE, 25, m_market);
					maxIndLbl.setHighlightOnMouseover(true);

					getPos().setSize(
						maxIndLbl.computeTextWidth(maxIndLbl.getText()),
						getPos().getHeight()
					);

					add(maxIndLbl).inBL(0, 0);
                }

                @Override
                public CustomPanelAPI getTpParent() {
                    return getParent();
                }

                @Override
                public TooltipMakerAPI createAndAttachTp() {
					final int tpWidth = 400;

                    TooltipMakerAPI tooltip = getParent().createUIElement(tpWidth, 0, false);

					tooltip.addPara(
						"Maximum number of industries, based on the size of a colony and other factors.", 0
					);
					tooltip.beginTable(
						m_market.getFaction(), 20, new Object[]{"Colony size", 120, "Base industries", 120}
					);

					for(int i = 3; i <= Misc.getMaxMarketSize(m_market); i++) {
						tooltip.addRow(new Object[]{highlight, "" + i, highlight,
						"" + LtvPopulationAndInfrastructure.getMaxIndustries(i)});
					}

					tooltip.addTable("", 0, 10);
					tooltip.addPara(
						"Structures such as spaceports or orbital stations do not count against this limit." + 
						"Colonies that exceed this limit for any reason have their stability reduced by %s.", 20,
						highlight, new String[]{"" + Misc.OVER_MAX_INDUSTRIES_PENALTY}
					);
					tooltip.addPara("Industries on %s:", 10, m_market.getFaction().getBaseUIColor(),
						new String[]{m_market.getName()}
					);

					List<Industry> industries = CommodityStats.getVisibleIndustries(m_market);
					Collections.sort(industries, getIndustryOrderComparator());

					final String indent = "    ";
					boolean anyIndustryAdded = false;
					int paragraphSpacing = 5;

					for (Industry industry : industries) {
						if (industry.isIndustry()) {
							tooltip.addPara(indent + industry.getCurrentName(), paragraphSpacing);
							paragraphSpacing = 3;
							anyIndustryAdded = true;
						} else if (industry.isUpgrading()) {
							String upgradeId = industry.getSpec().getUpgrade();
							if (upgradeId != null) {
								Industry upgradedIndustry = m_market.instantiateIndustry(upgradeId);
								if (upgradedIndustry.isIndustry()) {
									tooltip.addPara(
										indent + industry.getCurrentName() + " (upgrading to " + 
										upgradedIndustry.getCurrentName() + ")", paragraphSpacing
									);
									paragraphSpacing = 3;
									anyIndustryAdded = true;
								}
							}
						}
					}

					for (ConstructionQueueItem item : m_market.getConstructionQueue().getItems()) {
						final IndustrySpecAPI spec = Global.getSettings().getIndustrySpec(item.id);
						if (spec.hasTag("industry")) {
							Industry ind = m_market.instantiateIndustry(item.id);
							tooltip.addPara(indent + ind.getCurrentName() + " (queued)", paragraphSpacing);
							paragraphSpacing = 3;
							anyIndustryAdded = true;
						}
					}

					if (!anyIndustryAdded) {
						tooltip.addPara(indent + "None", paragraphSpacing);
					}

					getParent().addUIElement(tooltip);

					WrapUiUtils.anchorPanel(tooltip, getPanel(), AnchorType.TopLeft, 0);

                    return tooltip;
                }
            };
        }

		final CallbackRunnable<Button> buildBtnRunnable = (btn) -> {
			UIState.setState(State.DIALOG);

			final Object listener = new ListenerFactory.DialogDismissedListener() {
				@Override
				public void trigger(Object... args) {
					dialogDismissed(args);
				}
			}.getProxy();

			UIPanelAPI coreUI = Attachments.getInteractionCoreUI();
			if (coreUI == null) Attachments.getCoreUI();
			IndustryPickerDialog dialog = (IndustryPickerDialog) indPickCtor.newInstance(
				m_market,
				coreUI,
				listener
			);
			dialog.show(0.3F, 0.2F);
		};

		final int buildBtnWidth = 350;
		buildButton = new Button(
			getPanel(), buildBtnWidth, 25,
			"      Add industry or structure...", Fonts.ORBITRON_20AABOLD,
			buildBtnRunnable
		);
		buildButton.setCutStyle(CutStyle.TL_BR);
		buildButton.setAlignment(Alignment.LMID);
		buildButton.setLabelColor(base);
		buildButton.setShortcut(Keyboard.KEY_A);
		buildButton.getPlugin().setIgnoreUIState(false);
		buildButton.getPlugin().setTargetUIState(State.NONE);
		
		add(buildButton).inBL(0, BUTTON_SECTION_HEIGHT);
		add(creditLblPanel.getPanel()).inBL(buildBtnWidth + 70, BUTTON_SECTION_HEIGHT);
		add(maxIndLblPanel.getPanel()).inBR(40, BUTTON_SECTION_HEIGHT);

		if (!DebugFlags.COLONY_DEBUG && !m_market.isPlayerOwned()) {
			buildButton.disabled = true;
			if (DebugFlags.HIDE_COLONY_CONTROLS) {
				creditLblPanel.getPanel().setOpacity(0);
				buildButton.getPanel().setOpacity(0);
			}
		}
	}

	public final void addWidgetTooltip(IndustryTooltipMode mode, Industry ind, LtvIndustryWidget widget) {

		PendingTooltip<CustomPanelAPI> wrapper = widget.m_tooltip;

		wrapper.parentSupplier = () -> {
			return LtvIndustryListPanel.this.getPanel();
		};

		wrapper.factory = () -> {
			TooltipMakerAPI tp = wrapper.parentSupplier.get().createUIElement(ind.getTooltipWidth(), 0, false);

			// ind.createTooltip(mode, tp, false);
			IndustryTooltips.createIndustryTooltip(mode, tp, false, ind);

			wrapper.parentSupplier.get().addUIElement(tp);
			
			WrapUiUtils.anchorPanelWithBounds(tp, widget.getPanel(), AnchorType.RightTop, pad*2);

			return tp;
		};
	}

	public void processInputImpl(List<InputEventAPI> events) {
		boolean anyWidgetNotNormal = false;

		for (Object widgetObj : widgets) {
			if (widgetObj instanceof LtvIndustryWidget widget) {
				if (widget.getMode() != ConstructionMode.NORMAL) {
					anyWidgetNotNormal = true;
					break;
				}
			}

		}

		if (anyWidgetNotNormal) {
			boolean mouseOverWidget = events.stream()
				.filter(InputEventAPI::isMouseMoveEvent)
				.anyMatch(event -> widgets.stream()
				.anyMatch(widget -> ((LtvIndustryWidget) widget).getIndustryIcon().getPos()
				.containsEvent(event)));

			if (mouseOverWidget) {
				anyWidgetNotNormal = false;
			}
		}

		if (anyWidgetNotNormal) {
			InputEventAPI targetEvent = null;

			for (InputEventAPI event : events) {
				if (event.isConsumed()) continue;

				if (event.isLMBDownEvent() || event.isRMBDownEvent()) {
					targetEvent = event;
					break;
				}
			}

			if (targetEvent != null) {
				for (Object widgetObj : widgets) {
					if (widgetObj instanceof LtvIndustryWidget widget && widget.getQueueIndex() >= 0) {
						widget.setNormalMode();
					}
				}
				targetEvent.consume();
			}
		}
	}

	public void dialogDismissed(Object... args) {
		UIState.setState(State.NONE);

		final IndustryPickerDialog buildDialog = (IndustryPickerDialog) args[0];
		if (buildDialog.getSelected() == null) return;

		Industry selectedIndustry = buildDialog.getSelected().getIndustry();
		final int buildCost = (int) selectedIndustry.getBuildCost();

		Misc.getCurrentlyBeingConstructed(m_market);
		m_market.getConstructionQueue().addToEnd(selectedIndustry.getId(), buildCost);

		final MutableValue playerCredits = Global.getSector().getPlayerFleet().getCargo().getCredits();
		playerCredits.subtract(buildCost);
		if (playerCredits.get() <= 0) playerCredits.set(0);

		Global.getSector().getCampaignUI().getMessageDisplay().addMessage(
			String.format("Spent %s", Misc.getDGSCredits(buildCost)),
			Misc.getTooltipTitleAndLightHighlightColor(),
			Misc.getDGSCredits(buildCost),
			highlight
		);

		createPanel();
	}

	public static MarketInteractionMode getMarketInteractionMode(MarketAPI market) {
		final InteractionDialogAPI dialog = Global.getSector().getCampaignUI().getCurrentInteractionDialog();
		if (dialog == null) {
			return MarketInteractionMode.REMOTE;
		}

		final SectorEntityToken interactingTarget = dialog.getInteractionTarget();
		if (interactingTarget != null && interactingTarget.getMarket() == market) {
			return MarketInteractionMode.LOCAL;
		} else {
			return MarketInteractionMode.REMOTE;
		}
	}
}
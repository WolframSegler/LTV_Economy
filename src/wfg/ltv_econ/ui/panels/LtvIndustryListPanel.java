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
import com.fs.starfarer.api.impl.campaign.econ.impl.PopulationAndInfrastructure;
import com.fs.starfarer.api.impl.campaign.econ.impl.ConstructionQueue.ConstructionQueueItem;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.loading.IndustrySpecAPI;
import com.fs.starfarer.api.ui.Fonts;
import com.fs.starfarer.api.ui.LabelAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.ui.UIPanelAPI;
import com.fs.starfarer.api.util.Misc;
import com.fs.starfarer.api.util.MutableValue;
import com.fs.starfarer.campaign.ui.marketinfo.IndustryPickerDialog;

import rolflectionlib.util.ListenerFactory;
import rolflectionlib.util.RolfLectionUtil;
import wfg.ltv_econ.economy.WorkerRegistry;
import wfg.ltv_econ.industry.IndustryTooltips;
import wfg.ltv_econ.ui.panels.IndustryWidget.ConstructionMode;
import wfg.ltv_econ.util.UiUtils;
import wfg.native_ui.util.CallbackRunnable;
import wfg.native_ui.util.NativeUiUtils;
import wfg.native_ui.util.NativeUiUtils.AnchorType;
import wfg.native_ui.ui.Attachments;
import wfg.native_ui.ui.ComponentFactory;
import wfg.native_ui.ui.UIContext;
import wfg.native_ui.ui.UIContext.Context;
import wfg.native_ui.ui.panels.Button;
import wfg.native_ui.ui.panels.CustomPanel;
import wfg.native_ui.ui.panels.TextPanel;
import wfg.native_ui.ui.panels.Button.CutStyle;
import static wfg.native_ui.util.UIConstants.*;

public class LtvIndustryListPanel extends CustomPanel<LtvIndustryListPanel> {

	public static final int BUTTON_SECTION_HEIGHT = 45;

	public static final Object indPickCtor = RolfLectionUtil.getConstructor(
		IndustryPickerDialog.class, 
		RolfLectionUtil.getConstructorParamTypesSingleConstructor(IndustryPickerDialog.class)
	);

	private static LtvIndustryListPanel instance;
	public static Object indOptCtor = null;
	
	public final List<Object> widgets = new ArrayList<>();
	public final MarketAPI m_market;
	public final UIPanelAPI dummyWidget;

	private Button buildButton;

	public LtvIndustryListPanel(UIPanelAPI parent, int width, int height, MarketAPI market, 
		UIPanelAPI industryPanel) {
		super(parent, width, height);

		m_market = market;

		final List<?> widgets = (List<?>) RolfLectionUtil.getMethodAndInvokeDirectly(
            "getWidgets", industryPanel);
		if (!widgets.isEmpty()) {
			dummyWidget = (UIPanelAPI) widgets.get(0);
			dummyWidget.setOpacity(0);
		} else { dummyWidget = null; }

		createPanel();

		instance = this;
   	}

	public static final void refreshPanel() {
		if (instance == null) return;
		instance.createPanel();
	}

	public void createPanel() {
		clearChildren();
		widgets.clear();

		final List<Industry> industries = WorkerRegistry.getVisibleIndustries(m_market);
		Collections.sort(industries, getIndustryOrderComparator());
		List<ConstructionQueueItem> queuedIndustries = m_market.getConstructionQueue().getItems();
	
		final byte columnAmount = 4;

		final TooltipMakerAPI wrappertp = ComponentFactory.createTooltip(
            getPos().getWidth(), true
        );

		int wrapperTpHeight = 0;

		// Normal industries
		for (int index = 0; index < industries.size(); index++) {
			int i = index % columnAmount;
			int j = index / columnAmount;
			Industry ind = industries.get(index);

			final IndustryWidget widget = new IndustryWidget(
				m_panel,
				m_market,
				ind,
				this
			);

			wrappertp.addComponent(widget.getPanel()).inTL(
				i * (IndustryWidget.PANEL_WIDTH + opad) + pad,
				wrapperTpHeight = j * (IndustryWidget.TOTAL_HEIGHT + pad*5)
			);

			addWidgetTooltip(IndustryTooltipMode.NORMAL, ind, widget);

			widgets.add(widget);
		}

		// Queued industries
		for (int index = 0; index < queuedIndustries.size(); index++) {
			int i = (index + industries.size()) % columnAmount;
			int j = (index + industries.size()) / columnAmount;
			Industry ind = m_market.instantiateIndustry(queuedIndustries.get(index).id);

			final IndustryWidget widget = new IndustryWidget(
				m_panel,
				m_market,
				ind,
				this,
				index
			);

			wrappertp.addComponent(widget.getPanel()).inTL(
				i * (IndustryWidget.PANEL_WIDTH + opad) + pad,
				wrapperTpHeight = j * (IndustryWidget.TOTAL_HEIGHT + pad*5)
			);

			addWidgetTooltip(IndustryTooltipMode.QUEUED, ind, widget);

			widgets.add(widget);
		}
		wrappertp.setHeightSoFar(wrapperTpHeight + IndustryWidget.TOTAL_HEIGHT + opad*1.5f);

		ComponentFactory.addTooltip(wrappertp, getPos().getHeight() - BUTTON_SECTION_HEIGHT*1.4f,
			true, m_panel
		).inTL(-pad, 0);
		
		TextPanel playerCreditLblPanel = null;
		TextPanel colonyCreditLblPanel = null;
		TextPanel maxIndLblPanel = null;

		{ // player creditLbl
		playerCreditLblPanel = new TextPanel(m_panel, 200, 25) {

			@Override
			public void createPanel() {
				LabelAPI creditLbl = UiUtils.createPlayerCreditsLabel(Fonts.INSIGNIA_LARGE, 25);
				creditLbl.setHighlightOnMouseover(true);

				getPos().setSize(
					creditLbl.computeTextWidth(creditLbl.getText()),
					getPos().getHeight()
				);

				add(creditLbl).inBL(0, 0);
			}

			{
				tooltip.parent = LtvIndustryListPanel.this.m_panel;
				tooltip.builder = (tp, exp) -> {
					tp.addPara("Player credits available.", 0);
				};
				tooltip.positioner = (tp, exp) -> {
					NativeUiUtils.anchorPanel(tp, m_panel, AnchorType.TopLeft, 0);
				};
			}
		};
        }

		{ // colony creditLbl
		colonyCreditLblPanel = new TextPanel(getPanel(), 200, 25) {

			@Override
			public void createPanel() {
				LabelAPI creditLbl = UiUtils.createColonyCreditsLabel(
					Fonts.INSIGNIA_LARGE, 25, m_market.getId()
				);
				creditLbl.setHighlightOnMouseover(true);

				getPos().setSize(
					creditLbl.computeTextWidth(creditLbl.getText()),
					getPos().getHeight()
				);

				add(creditLbl).inBL(0, 0);
			}

			{
				tooltip.parent = LtvIndustryListPanel.this.m_panel;
				tooltip.builder = (tp, exp) -> {
					tp.addPara("Colony credits available.", 0);
				};
				tooltip.positioner = (tp, exp) -> {
					NativeUiUtils.anchorPanel(tp, m_panel, AnchorType.TopLeft, 0);
				};
			}
		};
        }

		{ // maxIndLbl
		maxIndLblPanel = new TextPanel(getPanel(), 200, 25) {

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

			{
				tooltip.parent = LtvIndustryListPanel.this.m_panel;
				tooltip.builder = (tp, exp) -> {
					tp.addPara(
						"Maximum number of industries, based on the size of a colony and other factors.", 0
					);
					tp.beginTable(
						m_market.getFaction(), 20, new Object[]{"Colony size", 120, "Base industries", 120}
					);

					for(int i = 3; i <= Misc.getMaxMarketSize(m_market); i++) {
						tp.addRow(new Object[]{highlight, "" + i, highlight,
						"" + PopulationAndInfrastructure.getMaxIndustries(i)});
					}

					tp.addTable("", 0, 10);
					tp.addPara(
						"Structures such as spaceports or orbital stations do not count against this limit." + 
						"Colonies that exceed this limit for any reason have their stability reduced by %s.", 20,
						highlight, new String[]{"" + Misc.OVER_MAX_INDUSTRIES_PENALTY}
					);
					tp.addPara("Industries on %s:", 10, m_market.getFaction().getBaseUIColor(),
						new String[]{m_market.getName()}
					);

					final List<Industry> industries = WorkerRegistry.getVisibleIndustries(m_market);
					Collections.sort(industries, getIndustryOrderComparator());

					final String indent = "    ";
					boolean anyIndustryAdded = false;
					int paragraphSpacing = 5;

					for (Industry industry : industries) {
						if (industry.isIndustry()) {
							tp.addPara(indent + industry.getCurrentName(), paragraphSpacing);
							paragraphSpacing = 3;
							anyIndustryAdded = true;
						} else if (industry.isUpgrading()) {
							String upgradeId = industry.getSpec().getUpgrade();
							if (upgradeId != null) {
								Industry upgradedIndustry = m_market.instantiateIndustry(upgradeId);
								if (upgradedIndustry.isIndustry()) {
									tp.addPara(
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
							tp.addPara(indent + ind.getCurrentName() + " (queued)", paragraphSpacing);
							paragraphSpacing = 3;
							anyIndustryAdded = true;
						}
					}

					if (!anyIndustryAdded) {
						tp.addPara(indent + "None", paragraphSpacing);
					}
				};
				tooltip.positioner = (tp, exp) -> {
					NativeUiUtils.anchorPanel(tp, m_panel, AnchorType.TopLeft, 0);
				};
			}
		};
        }

		final CallbackRunnable<Button> buildBtnRunnable = (btn) -> {
			UIContext.setContext(Context.DIALOG);

			final Object listener = new ListenerFactory.DialogDismissedListener() {
				@Override
				public void trigger(Object... args) {
					dialogDismissed(args);
				}
			}.getProxy();

			final UIPanelAPI coreUI = Attachments.getCoreUI();
			final IndustryPickerDialog dialog = (IndustryPickerDialog) RolfLectionUtil.instantiateClass(
				indPickCtor,
				m_market,
				coreUI,
				listener
			);
			dialog.show(0.3f, 0.2f);
		};

		final int buildBtnWidth = 350;
		buildButton = new Button(
			getPanel(), buildBtnWidth, 25,
			"Add industry or structure...", Fonts.ORBITRON_20AABOLD,
			buildBtnRunnable
		);
		buildButton.cutStyle = CutStyle.TL_BR;
		buildButton.setLabelColor(base);
		buildButton.setShortcut(Keyboard.KEY_A);
		buildButton.context.ignore = false;
		buildButton.context.target = Context.NONE;
		
		add(buildButton).inBL(0, BUTTON_SECTION_HEIGHT);
		add(playerCreditLblPanel).inBL(buildBtnWidth + 40, BUTTON_SECTION_HEIGHT + 18);
		add(colonyCreditLblPanel).inBL(buildBtnWidth + 40, BUTTON_SECTION_HEIGHT - 18);
		add(maxIndLblPanel).inBR(40, BUTTON_SECTION_HEIGHT);

		if (!m_market.isPlayerOwned() && !DebugFlags.COLONY_DEBUG) {
			buildButton.setEnabled(false);
			playerCreditLblPanel.getPanel().setOpacity(0f);
			colonyCreditLblPanel.getPanel().setOpacity(0f);
			if (DebugFlags.HIDE_COLONY_CONTROLS) buildButton.getPanel().setOpacity(0f);
		}
	}

	public static final void addWidgetTooltip(IndustryTooltipMode mode, Industry ind, IndustryWidget widget) {
		widget.industryIcon.tooltip.parent = widget.getParent();
		widget.industryIcon.tooltip.builder = (tp, exp) -> {
			IndustryTooltips.createIndustryTooltip(mode, tp, false, ind);
		};
	}

	@Override
	public final void processInput(List<InputEventAPI> events) {
		super.processInput(events);

		boolean anyWidgetNotNormal = false;
		for (Object widgetObj : widgets) {
			if (widgetObj instanceof IndustryWidget widget) {
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
				.anyMatch(widget -> ((IndustryWidget) widget).industryIcon.getPos()
				.containsEvent(event))
			);

			if (mouseOverWidget) anyWidgetNotNormal = false;
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
					if (widgetObj instanceof IndustryWidget widget && widget.getQueueIndex() >= 0) {
						widget.setNormalMode();
					}
				}
				targetEvent.consume();
			}
		}
	}

	public final void dialogDismissed(Object... args) {
		UIContext.setContext(Context.NONE);

		if (((int) args[1]) != 0) return; // 0 means confirm
		final IndustryPickerDialog buildDialog = (IndustryPickerDialog) args[0];
		final Object selectedObj = RolfLectionUtil.getMethodAndInvokeDirectly(
			"getSelected", buildDialog);
		if (selectedObj == null) return;

		Industry selectedIndustry = buildDialog.getSelected().getIndustry();
		final int buildCost = (int) selectedIndustry.getBuildCost();

		Misc.getCurrentlyBeingConstructed(m_market);
		m_market.getConstructionQueue().addToEnd(selectedIndustry.getId(), buildCost);

		final MutableValue playerCredits = Global.getSector().getPlayerFleet().getCargo().getCredits();
		playerCredits.subtract(buildCost);
		if (playerCredits.get() <= 0) playerCredits.set(0);

		Global.getSector().getCampaignUI().getMessageDisplay().addMessage(
			String.format("Spent %s", Misc.getDGSCredits(buildCost)),
			glowHighlight, Misc.getDGSCredits(buildCost), highlight
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

	private static final Comparator<Industry> getIndustryOrderComparator() {
		return Comparator.comparingInt(ind -> ind.getSpec().getOrder());
	}
}
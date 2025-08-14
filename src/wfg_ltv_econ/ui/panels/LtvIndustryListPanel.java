package wfg_ltv_econ.ui.panels;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Collections;
import java.util.List;

import org.lwjgl.input.Keyboard;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignUIAPI.DismissDialogDelegate;
import com.fs.starfarer.api.campaign.econ.Industry;
import com.fs.starfarer.api.campaign.econ.Industry.IndustryTooltipMode;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.impl.campaign.DebugFlags;
import com.fs.starfarer.api.impl.campaign.econ.impl.ConstructionQueue.ConstructionQueueItem;
import com.fs.starfarer.api.impl.campaign.econ.impl.PopulationAndInfrastructure;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.loading.IndustrySpecAPI;
import com.fs.starfarer.api.ui.CutStyle;
import com.fs.starfarer.api.ui.Fonts;
import com.fs.starfarer.api.ui.LabelAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.ui.UIComponentAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI.ActionListenerDelegate;
import com.fs.starfarer.api.ui.UIPanelAPI;
import com.fs.starfarer.api.util.Misc;
import com.fs.starfarer.api.ui.Alignment;
import com.fs.starfarer.api.ui.ButtonAPI;
import com.fs.starfarer.campaign.ui.marketinfo.IndustryListPanel;
import com.fs.starfarer.campaign.ui.marketinfo.IndustryPickerDialog;

import wfg_ltv_econ.ui.panels.LtvIndustryWidget.ConstructionMode;
import wfg_ltv_econ.ui.plugins.IndustryListPanelPlugin;
import wfg_ltv_econ.ui.plugins.IndustryPanelPlugin;
import wfg_ltv_econ.util.CommodityStats;
import wfg_ltv_econ.util.ReflectionUtils;
import wfg_ltv_econ.util.TooltipUtils;
import wfg_ltv_econ.util.UiUtils;
import wfg_ltv_econ.util.ReflectionUtils.ReflectedConstructor;
import wfg_ltv_econ.util.UiUtils.AnchorType;

public class LtvIndustryListPanel
	extends LtvCustomPanel<IndustryListPanelPlugin, LtvIndustryListPanel, UIPanelAPI>
	implements ActionListenerDelegate, DismissDialogDelegate {

	public static final int BUTTON_SECTION_HEIGHT = 50;

	public static final ReflectedConstructor indPickCtor = ReflectionUtils.getConstructorsMatching(IndustryPickerDialog.class, 3).get(0);
	public static ReflectedConstructor indOptCtor = null;
	private final IndustryListPanel originalIndustryPanel;

	private List<Object> widgets = new ArrayList<>();
	public List<Object> getWidgets() {
		return widgets;
	}

	public static Comparator<Industry> getIndustryOrderComparator() {
		return Comparator.comparingInt(ind -> ind.getSpec().getOrder());
	}

	private final UIPanelAPI m_coreUI;

	private ButtonAPI buildButton;
	private TooltipMakerAPI buildButtonTp;
	private boolean buildDialogOpen = false;

	public LtvIndustryListPanel(UIPanelAPI root, UIPanelAPI parent, int width, int height, MarketAPI market, 
		UIPanelAPI industryPanel, UIPanelAPI coreUI) {
		super(root, parent, width, height, new IndustryListPanelPlugin(), market);

		originalIndustryPanel = (IndustryListPanel) industryPanel;

		m_coreUI = coreUI;

		initializePlugin(hasPlugin);
		createPanel();
   	}

	@Override
	public void initializePlugin(boolean hasPlugin) {
		getPlugin().init(this);
	}

	public static void setindustryOptionsPanelConstructor(ReflectedConstructor a) {
		indOptCtor = a;
	}

	public UIPanelAPI getOverview() {
		return originalIndustryPanel.getOverview();
	}

	@Override
	public void createPanel() {
		clearChildren();
		widgets.clear();

		List<Industry> industries = CommodityStats.getVisibleIndustries(getMarket());
		Collections.sort(industries, getIndustryOrderComparator());
		List<ConstructionQueueItem> queuedIndustries = getMarket().getConstructionQueue().getItems();
		
		final int pad = 3;
		final int opad = 20;
		final byte columnAmount = 4;

		TooltipMakerAPI widgetWrapper = getPanel().createUIElement(
            getPos().getWidth(), getPos().getHeight() - BUTTON_SECTION_HEIGHT, true
        );

		// Normal industries
		for (int index = 0; index < industries.size(); index++) {
			int i = index % columnAmount;
			int j = index / columnAmount;
			Industry ind = industries.get(index);

			Object originalWidget = originalIndustryPanel.getWidgets().get(index);
			LtvIndustryWidget widget = new LtvIndustryWidget(
				getRoot(),
				widgetWrapper,
				new IndustryPanelPlugin(),
				getMarket(),
				ind,
				this,
				originalWidget
			);

			widgetWrapper.addComponent(widget.getPanel()).inTL(
				i * (LtvIndustryWidget.PANEL_WIDTH + opad),
				j * (LtvIndustryWidget.TOTAL_HEIGHT + opad)
			);
		}

		// Queued industries
		for (int index = 0; index < queuedIndustries.size(); index++) {
			int i = (index + industries.size()) % columnAmount;
			int j = (index + industries.size()) / columnAmount;
			Industry ind = getMarket().instantiateIndustry(queuedIndustries.get(index).id);

			Object originalWidget = originalIndustryPanel.getWidgets().get(index);
			LtvIndustryWidget widget = new LtvIndustryWidget(
				getRoot(),
				widgetWrapper,
				new IndustryPanelPlugin(),
				getMarket(),
				ind,
				this,
				originalWidget,
				index
			);

			widgetWrapper.addComponent(widget.getPanel()).inTL(
				i * (LtvIndustryWidget.PANEL_WIDTH + opad),
				j * (LtvIndustryWidget.TOTAL_HEIGHT + opad)
			);
		}

		add(widgetWrapper).inTL(0, 0);

		TooltipMakerAPI buttonWrapper = getPanel().createUIElement(
            getPos().getWidth(), getPos().getHeight(), false
        );
		
		LabelAPI creditLbl = UiUtils.createCreditsLabel(Fonts.INSIGNIA_LARGE, 25);
		LabelAPI maxIndLbl = UiUtils.createMaxIndustriesLabel(Fonts.INSIGNIA_LARGE, 25, getMarket());

		buttonWrapper.setButtonFontOrbitron20Bold();
		buttonWrapper.setActionListenerDelegate(this);
		buildButton = buttonWrapper.addButton(
            "   Add industry or structure...",
            null,
            Misc.getBasePlayerColor(),
            Misc.getDarkPlayerColor(),
            Alignment.LMID,
            CutStyle.TL_BR,
            350,
            25,
            pad
        );
		buildButton.setShortcut(Keyboard.KEY_Q, false);

		addTooltips(creditLbl, maxIndLbl, getMarket());
		
		buttonWrapper.addComponent(buildButton).inBL(0, 50);
		add(creditLbl);
		UiUtils.anchorPanel((UIComponentAPI) creditLbl, buildButton, AnchorType.RightMid, 70);
		add(maxIndLbl).inBR(40, 50);

		if (!DebugFlags.COLONY_DEBUG && !getMarket().isPlayerOwned()) {
			buildButton.setEnabled(false);
			if (DebugFlags.HIDE_COLONY_CONTROLS) {
				buildButton.setOpacity(0);
				creditLbl.setOpacity(0);
			}
		}

		CommodityStats.recalculateMaxDemandAndSupplyForAll(getMarket());

		add(buttonWrapper).inBL(0, 0);
	}

	public void recreateOverview() {
		Global.getSector().getEconomy().tripleStep();

		createPanel();
		ReflectionUtils.invoke(getPanel(), "fakeAdvance", 10f);
	}

	public final void addTooltips(LabelAPI label1, LabelAPI label2, MarketAPI var2) {
		label1.setHighlightOnMouseover(true);
		label2.setHighlightOnMouseover(true);
		final int tpWidth = 400;

		TooltipMakerAPI tp1 = getPanel().createUIElement(tpWidth, 0, false);
		TooltipMakerAPI tp2 = getPanel().createUIElement(tpWidth, 0, false);

		// Tooltip 1
		tp1.addPara("Credits available.", 0);

		// Tooltip 2
		tp2.addPara("Maximum number of industries, based on the size of a colony and other factors.", 0);
		tp2.beginTable(getFaction(), 20, new Object[]{"Colony size", 120, "Base industries", 120});

		for(int i = 3; i <= Misc.getMaxMarketSize(getMarket()); i++) {
			tp2.addRow(new Object[]{Misc.getHighlightColor(), "" + i, Misc.getHighlightColor(), "" + PopulationAndInfrastructure.getMaxIndustries(i)});
		}

		tp2.addTable("", 0, 10);
		tp2.addPara("Structures such as spaceports or orbital stations do not count against this limit." + 
			"Colonies that exceed this limit for any reason have their stability reduced by %s.", 20,
			Misc.getHighlightColor(), new String[]{"" + Misc.OVER_MAX_INDUSTRIES_PENALTY}
		);
		tp2.addPara("Industries on %s:", 10, getFaction().getBaseUIColor(),
			new String[]{getMarket().getName()}
		);

		List<Industry> industries = CommodityStats.getVisibleIndustries(getMarket());
		Collections.sort(industries, getIndustryOrderComparator());

		final String indent = "    ";
		boolean anyIndustryAdded = false;
		int paragraphSpacing = 5;

		for (Industry industry : industries) {
			if (industry.isIndustry()) {
				tp2.addPara(indent + industry.getCurrentName(), paragraphSpacing);
				paragraphSpacing = 3;
				anyIndustryAdded = true;
			} else if (industry.isUpgrading()) {
				String upgradeId = industry.getSpec().getUpgrade();
				if (upgradeId != null) {
					Industry upgradedIndustry = getMarket().instantiateIndustry(upgradeId);
					if (upgradedIndustry.isIndustry()) {
						tp2.addPara(indent + industry.getCurrentName() + " (upgrading to " + upgradedIndustry.getCurrentName() + ")", paragraphSpacing);
						paragraphSpacing = 3;
						anyIndustryAdded = true;
					}
				}
			}
		}

		for (ConstructionQueueItem item : getMarket().getConstructionQueue().getItems()) {
			final IndustrySpecAPI spec = Global.getSettings().getIndustrySpec(item.id);
			if (spec.hasTag("industry")) {
				Industry ind = getMarket().instantiateIndustry(item.id);
				tp2.addPara(indent + ind.getCurrentName() + " (queued)", paragraphSpacing);
				paragraphSpacing = 3;
				anyIndustryAdded = true;
			}
		}

		if (!anyIndustryAdded) {
			tp2.addPara(indent + "None", paragraphSpacing);
		}

		UiUtils.anchorPanel(tp1, (UIComponentAPI) label1, AnchorType.LeftTop, 0);
		UiUtils.anchorPanel(tp2, (UIComponentAPI) label2, AnchorType.LeftTop, 0);
   	}

	public void advanceImpl(float amount) {
		boolean shouldEnableButton = DebugFlags.COLONY_DEBUG || getMarket().isPlayerOwned();
		
		if (shouldEnableButton != buildButton.isEnabled()) {
			buildButton.setEnabled(shouldEnableButton);
			if (!shouldEnableButton) {
				buildButtonTp = getPanel().createUIElement(300, 0, false);

				buildButtonTp.addPara("Maximum number of industries reached.", 0);

				add(buildButtonTp);
				UiUtils.anchorPanel(buildButtonTp, buildButton, AnchorType.LeftBottom, 0);

			} else {
				remove(buildButtonTp);

				buildButtonTp = null;
			}
		}

		/*
		 * Call dialogDismissed for this class when the original listPanel's method runs. 
		 */
		if (buildDialogOpen) {
			int currentSize = getMarket().getConstructionQueue().getItems().size();
			if (currentSize > previousQueueSize) {
				buildDialogOpen = false;
				dialogDismissed();
			}
		}
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
				if (event.isConsumed()) {
					continue;
				}
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


	private int previousQueueSize = 0;

	public void actionPerformed(Object data, Object source) {
		if (source == buildButton) {
			buildDialogOpen = true;

			/*
			 * Poll for queue size change to detect when the
			 * dialogDismissed method for originalIndustryPanel is called.
			 */
			previousQueueSize = getMarket().getConstructionQueue().getItems().size();

			// Get the button of the original widget to call its actionPerformed succesfully
			Object originalBtn = ReflectionUtils.get(originalIndustryPanel, "build", ButtonAPI.class);

			// Call the original actionPerformed to set up the isDialogOpen enum. First parameter not used.
			originalIndustryPanel.actionPerformed(null, originalBtn);

			// The original method call handles this
			// IndustryPickerDialog dialog = (IndustryPickerDialog) indPickCtor.newInstance(
			// 	getMarket(),
			// 	m_coreUI,
			// 	originalIndustryPanel
			// );
			// dialog.show(0.3F, 0.2F);
		}
	}

	public void dialogDismissed() {

		if (buildDialogOpen) {
			// // The dialogDismissed method of originalIndustryPanel already does the following.
			// List<?> chldr = CampaignEngine.getInstance().getCampaignUI().getDialogParent().getChildrenNonCopy();
    
			// IndustryPickerDialog buildDialog = chldr.stream()
			// 	.filter(child -> child instanceof IndustryPickerDialog && child instanceof UIPanelAPI)
			// 	.map(child -> (IndustryPickerDialog) child)
			// 	.findFirst().orElse(null);

			// if (buildDialog.getSelected() != null) {
			// 	Industry selectedIndustry = buildDialog.getSelected().getIndustry();
			// 	final int buildCost = (int) selectedIndustry.getBuildCost();

			// 	Misc.getCurrentlyBeingConstructed(getMarket());
			// 	getMarket().getConstructionQueue().addToEnd(selectedIndustry.getId(), buildCost);

			// 	MutableValue playerCredits = Global.getSector().getPlayerFleet().getCargo().getCredits();
			// 	playerCredits.subtract(buildCost);
			// 	if (playerCredits.get() <= 0) {
			// 		playerCredits.set(0);
			// 	}

			// 	CampaignEngine.getInstance().getCampaignUI().getMessageDisplay().addMessage(
			// 		String.format(
			// 			"Spent %s",
			// 			Misc.getDGSCredits(buildCost)
			// 		),
			// 		Misc.getTooltipTitleAndLightHighlightColor(),
			// 		Misc.getDGSCredits(buildCost),
			// 		Misc.getHighlightColor()
			// 	);
			// }

			recreateOverview();
		} else {
			buildDialogOpen = false;
		}
	}
}

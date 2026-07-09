package wfg.ltv_econ.ui.scripts;

import static wfg.native_ui.util.Globals.settings;
import static com.fs.starfarer.api.ui.TooltipMakerAPI.TooltipLocation.BELOW;

import java.util.List;

import com.fs.starfarer.api.Global;

import wfg.ltv_econ.economy.engine.EconomyEngine;
import wfg.ltv_econ.ui.marketInfo.CommodityRowPanel;
import wfg.ltv_econ.ui.marketInfo.LtvCommodityPanel;
import wfg.ltv_econ.ui.marketInfo.LtvIndustryListPanel;
import wfg.ltv_econ.ui.marketInfo.buttons.ColonyStockpilesButton;
import wfg.ltv_econ.ui.marketInfo.buttons.IncomeLabel;
import wfg.ltv_econ.ui.marketInfo.buttons.ManagePopButton;
import wfg.ltv_econ.ui.marketInfo.buttons.MarketEventsButton;
import wfg.ltv_econ.ui.marketInfo.dialogs.ComDetailDialog;
import wfg.ltv_econ.ui.reusable.IdentityMarker;
import wfg.ltv_econ.util.TooltipUtils;
import wfg.ltv_econ.util.UIUtils;
import wfg.ltv_econ.util.wrappers.MarketWrapper;
import wfg.native_ui.util.NativeUiUtils;
import wfg.native_ui.util.NativeUiUtils.AnchorType;
import wfg.native_ui.ui.Attachments;
import wfg.native_ui.ui.component.InteractionComp.ClickHandler;
import wfg.native_ui.ui.panel.CustomPanel;

import com.fs.starfarer.campaign.econ.Market;
import com.fs.starfarer.campaign.ui.marketinfo.IndustryListPanel;
import com.fs.starfarer.campaign.ui.marketinfo.ShippingPanel;

import rolflectionlib.util.RolfLectionUtil;

import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.campaign.listeners.DialogCreatorUI;
import com.fs.starfarer.api.impl.campaign.DebugFlags;
import com.fs.starfarer.api.ui.ButtonAPI;
import com.fs.starfarer.api.ui.PositionAPI;
import com.fs.starfarer.api.ui.UIPanelAPI;
import com.fs.starfarer.campaign.ui.MarketConditionsWidget;
import com.fs.starfarer.campaign.ui.marketinfo.CommodityPanel;

public class MarketUIReplacer implements CoreTabUIBuilder {

    private static final Class<?> knownClass1 = IndustryListPanel.class;
    private static final Class<?> knownClass2 = LtvIndustryListPanel.class;
    private static final Class<?> knownClass3 = CommodityPanel.class;
    private static final Class<?> knownClass4 = LtvCommodityPanel.class;

    private static Object marketAPIField = null;
    private static Object marketField = null;

    private static MarketAPI market = null;
    @Override
    public void advance(float amount) {
        final UIPanelAPI masterTab = Attachments.getCurrentTab();
        if (masterTab == null) return;

        final UIPanelAPI tradePanel = (UIPanelAPI) RolfLectionUtil.getMethodAndInvokeDirectly(
            "getTradePanel", masterTab);
        if (tradePanel == null) return;

        if (marketAPIField == null) {
            marketAPIField = RolfLectionUtil.getAllFields(tradePanel.getClass())
                .stream().filter(f -> MarketAPI.class.isAssignableFrom(
                    RolfLectionUtil.getFieldType(f)
                )).findFirst().get();
        }
        market = (MarketAPI) RolfLectionUtil.getPrivateVariable(marketAPIField, tradePanel);
        if (!market.isInEconomy()) return;

        final List<?> outpostChildren = (List<?>) RolfLectionUtil.invokeMethodDirectly(
            CustomPanel.getChildrenNonCopyMethod, tradePanel);
        final UIPanelAPI overviewPanel = outpostChildren.stream()
            .filter(c -> RolfLectionUtil.hasMethodOfName("showOverview", c))
            .map(child -> (UIPanelAPI) child)
            .findFirst().orElse(null);
        if (overviewPanel == null) return;

        final UIPanelAPI marketStatsPanel = outpostChildren.stream()
            .filter(c -> RolfLectionUtil.getAllFields(c.getClass()).stream()
                .anyMatch(f -> RolfLectionUtil.getFieldType(f).equals(MarketConditionsWidget.class)))
            .map(child -> (UIPanelAPI) child)
            .findFirst().orElse(null);
        if (marketStatsPanel == null) return;

        final List<?> overviewChildren = (List<?>) RolfLectionUtil.invokeMethodDirectly(
            CustomPanel.getChildrenNonCopyMethod, overviewPanel);
        final UIPanelAPI managementPanel = overviewChildren.stream()
            .filter(c -> RolfLectionUtil.hasMethodOfName("recreateWithEconUpdate", c))
            .map(child -> (UIPanelAPI) child)
            .findFirst().orElse(null);
        if (managementPanel == null) return;

        if (IdentityMarker.isPresent(managementPanel)) return;
        IdentityMarker.attach(managementPanel);

        final List<?> managementChildren = (List<?>) RolfLectionUtil.invokeMethodDirectly(
            CustomPanel.getChildrenNonCopyMethod, managementPanel);

        UIPanelAPI anchorChild = null;

        for (Object child : managementChildren) {
            Class<?> childClass = child.getClass();
            if (!childClass.equals(knownClass1) &&
                    !childClass.equals(knownClass2) &&
                    !childClass.equals(knownClass3) &&
                    !childClass.equals(knownClass4)) {
                anchorChild = (UIPanelAPI) child;
                break;
            }
        }
        if (anchorChild == null) return;

        EconomyEngine.instance().fakeAdvance();

        addManagementButtons(managementPanel, managementChildren, anchorChild);

        replaceMarketCreditsLabel(managementPanel, managementChildren, anchorChild);

        replaceIndustryListPanel(managementPanel, managementChildren, anchorChild);

        replaceCommodityPanel(managementPanel, managementChildren, anchorChild);

        replaceMarketInstanceForPriceControl(masterTab);

        replaceAccessLabelTooltip(marketStatsPanel);
    }

    private static final void addManagementButtons(
        UIPanelAPI managementPanel, List<?> managementChildren, UIPanelAPI colonyInfoPanel
    ) {
        if (!RolfLectionUtil.hasMethodOfName("getShipping", colonyInfoPanel)) return;
        final ShippingPanel shipPanel = (ShippingPanel) RolfLectionUtil.invokeMethod(
            "getShipping", colonyInfoPanel);

        final ButtonAPI useStockpilesBtn = (ButtonAPI) RolfLectionUtil.invokeMethod(
            "getUseStockpiles", shipPanel);

        if (!useStockpilesBtn.isEnabled()) return;
        useStockpilesBtn.setOpacity(0f);
        useStockpilesBtn.setEnabled(false);

        if (!market.isPlayerOwned() &&
            (!DebugFlags.COLONY_DEBUG || DebugFlags.HIDE_COLONY_CONTROLS)
        ) return;

        final int buttonWidth = (int) (LtvCommodityPanel.STANDARD_WIDTH / 3f - 4f);
        final int buttonHeight = (int) (buttonWidth / 1.63f);

        final MarketEventsButton eventsBtn = new MarketEventsButton(managementPanel, buttonWidth, buttonHeight, market);
        final ColonyStockpilesButton stockpilesBtn = new ColonyStockpilesButton(managementPanel, buttonWidth, buttonHeight, market);
        final ManagePopButton popBtn = new ManagePopButton(managementPanel, buttonWidth, buttonHeight, market);

        final int gap = (LtvCommodityPanel.STANDARD_WIDTH - buttonWidth * 3) / 2;

        colonyInfoPanel.addComponent(eventsBtn.getPanel());
        colonyInfoPanel.addComponent(stockpilesBtn.getPanel());
        colonyInfoPanel.addComponent(popBtn.getPanel());

        NativeUiUtils.anchorPanel(popBtn.getPanel(), colonyInfoPanel, AnchorType.BottomRight, -100);
        NativeUiUtils.anchorPanel(stockpilesBtn.getPanel(), popBtn.getPanel(), AnchorType.LeftBottom, gap);
        NativeUiUtils.anchorPanel(eventsBtn.getPanel(), stockpilesBtn.getPanel(), AnchorType.LeftBottom, gap);

        if (settings.isDevMode()) {
            Global.getLogger(MarketUIReplacer.class).info("Added Market Buttons");
        }
    }

    private static final void replaceMarketCreditsLabel(
        UIPanelAPI managementPanel, List<?> managementChildren, UIPanelAPI colonyInfoPanel
    ) {
        if (!DebugFlags.COLONY_DEBUG && !market.isPlayerOwned()) return;
        if (!RolfLectionUtil.hasMethodOfName("getIncome", colonyInfoPanel)) return;
        
        final UIPanelAPI incomePanel = (UIPanelAPI) RolfLectionUtil.getMethodAndInvokeDirectly(
            "getIncome", colonyInfoPanel);
        final List<?> incomePanelChildren = (List<?>) RolfLectionUtil.invokeMethodDirectly(
            CustomPanel.getChildrenNonCopyMethod, incomePanel);
        
        final var Buttons = (List<ButtonAPI>) incomePanelChildren.stream()
            .filter(c -> c instanceof ButtonAPI).map(c -> (ButtonAPI) c).toList();
        final ButtonAPI creditBtn = Buttons.get(0);
        final ButtonAPI hazardBtn = Buttons.get(1);
        incomePanel.removeComponent(creditBtn);

        final IncomeLabel colonyCreditLabel = new IncomeLabel(colonyInfoPanel, 150, 50, market);

        final PositionAPI posS = colonyCreditLabel.getPos();
        final PositionAPI posA = hazardBtn.getPosition();
        colonyInfoPanel.addComponent(colonyCreditLabel.getPanel()).inTL(
            (posA.getX() - posS.getX()) + (posA.getWidth() - posS.getWidth()) / 2f, 0
        );
    }

    private static final void replaceIndustryListPanel(UIPanelAPI managementPanel,
        List<?> managementChildren, UIPanelAPI anchor
    ) {
        UIPanelAPI industryPanel = null;
        for (Object child : managementChildren) {
            if (child instanceof IndustryListPanel) {
                industryPanel = (UIPanelAPI) child;
                break;
            }
        }
        if (industryPanel == null) return;

        // Steal the members for the constructor
        final int width = (int) industryPanel.getPosition().getWidth();
        final int height = (int) industryPanel.getPosition().getHeight();

        final LtvIndustryListPanel replacement = new LtvIndustryListPanel(
            managementPanel, width, height,
            market, industryPanel
        );

        managementPanel.addComponent(replacement.getPanel());
        NativeUiUtils.anchorPanel(replacement.getPanel(), anchor, AnchorType.BottomLeft, 25);

        if (LtvIndustryListPanel.indOptCtor == null) {
            // Acquire the popup class from one of the widgets
            final List<?> widgets = (List<?>) RolfLectionUtil.getMethodAndInvokeDirectly(
                "getWidgets", industryPanel);

            if (!widgets.isEmpty()) {
            final Object widget0 = widgets.get(0);

            // Attach the popup;
            RolfLectionUtil.getMethodAndInvokeDirectly("actionPerformed", widget0,
                null, null
            );

            // Now the popup class is a child of:
            final UIPanelAPI dialogParent = Attachments.getCampaignScreenPanel();
            final List<?> children = (List<?>) RolfLectionUtil.invokeMethodDirectly(
                CustomPanel.getChildrenNonCopyMethod, dialogParent);

            final UIPanelAPI indOps = children.stream()
                .filter(child -> child instanceof DialogCreatorUI && child instanceof UIPanelAPI)
                .map(child -> (UIPanelAPI) child)
                .findFirst().orElse(null);

            LtvIndustryListPanel.indOptCtor = RolfLectionUtil.getConstructor(indOps.getClass(),
                RolfLectionUtil.getConstructorParamTypesSingleConstructor(indOps.getClass())
            );

            // Dismiss the indOpsPanel after getting its constructor
            RolfLectionUtil.getMethodAndInvokeDirectly(
                "dismiss", indOps, 0
            );
            }
        }

        // No need for the old panel
        managementPanel.removeComponent(industryPanel);

        if (settings.isDevMode()) {
            Global.getLogger(MarketUIReplacer.class).info("Replaced IndustryListPanel");
        }
    }

    private static final void replaceCommodityPanel(UIPanelAPI managementPanel,
        List<?> managementChildren,UIPanelAPI anchor
    ) {
        UIPanelAPI commodityPanel = null;
        for (Object child : managementChildren) {
            if (child instanceof CommodityPanel) {
                commodityPanel = (UIPanelAPI) child;
                break;
            }
        }
        if (commodityPanel == null) return;

        final int height = (int) commodityPanel.getPosition().getHeight();

        final LtvCommodityPanel replacement = new LtvCommodityPanel(
            managementPanel, LtvCommodityPanel.STANDARD_WIDTH, height, market
        );

        final ClickHandler<CommodityRowPanel> listener = (source, isLeftClick) -> {
            if (!isLeftClick) return;

            CommodityRowPanel panel = ((CommodityRowPanel) source);

            replacement.selectRow(panel);

            if (UIUtils.canViewPrices()) {
                final ComDetailDialog dialogPanel = new ComDetailDialog(
                    market, market.getFaction().getFactionSpec(), panel.cell.spec
                );
                dialogPanel.show(0.3f, 0.3f);
            }
        };

        replacement.selectionListener = listener;
        replacement.buildUI();

        // Got the Y offset by looking at the getY() difference of replacement and
        // commodityPanel
        // Might automate the getY() difference later
        managementPanel.addComponent(replacement.getPanel());
        NativeUiUtils.anchorPanel(replacement.getPanel(), anchor, AnchorType.BottomRight, -43);

        managementPanel.removeComponent(commodityPanel);

        if (settings.isDevMode()) {
            Global.getLogger(MarketUIReplacer.class).info("Replaced CommodityPanel");
        }
    }

    private static final void replaceMarketInstanceForPriceControl(UIPanelAPI masterTab) {
        final UIPanelAPI handler = (UIPanelAPI) RolfLectionUtil.invokeMethod(
            "getTransferHandler", masterTab);

        if (marketField == null) {
            marketField = RolfLectionUtil.getAllFields(handler.getClass())
                .stream().filter(f -> Market.class.isAssignableFrom(
                    RolfLectionUtil.getFieldType(f)
                )).findFirst().get();
        }

        final Market original = (Market) RolfLectionUtil.getPrivateVariable(marketField, handler);
        if (original instanceof MarketWrapper) return;

        RolfLectionUtil.setPrivateVariable(marketField, handler, new MarketWrapper(original));
    }

    private static final void replaceAccessLabelTooltip(UIPanelAPI statsPanel) {
        final List<?> statsChilren = (List<?>) RolfLectionUtil.invokeMethodDirectly(
            CustomPanel.getChildrenNonCopyMethod, statsPanel);
        final List<ButtonAPI> buttons = statsChilren.stream()
            .filter(c -> c instanceof ButtonAPI)
            .map(child -> (ButtonAPI) child)
            .toList();

        ButtonAPI accessBtn = null;
        for (ButtonAPI candidate : buttons) {
            if (accessBtn == null) {
                accessBtn = candidate;
            } else if (candidate.getPosition().getX() < accessBtn.getPosition().getX()) {
                accessBtn = candidate;
            }
        }
        if (accessBtn == null) return;

        UIUtils.getTpForStaticAccess().addTooltipTo(TooltipUtils.createAccessTp(market), accessBtn, BELOW, false);
    }
}
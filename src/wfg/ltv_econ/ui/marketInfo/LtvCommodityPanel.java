package wfg.ltv_econ.ui.marketInfo;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import com.fs.starfarer.api.campaign.econ.CommodityOnMarketAPI;
import com.fs.starfarer.api.campaign.econ.CommoditySpecAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.ui.Alignment;
import com.fs.starfarer.api.ui.TooltipMakerAPI;

import wfg.ltv_econ.constant.EconomyConstants;
import wfg.ltv_econ.constant.Sprites;
import wfg.ltv_econ.constant.UIColors;
import wfg.ltv_econ.economy.commodity.MarketTradeDisruptionData;
import wfg.ltv_econ.economy.engine.EconomyEngine;
import wfg.ltv_econ.util.UIUtils;
import wfg.native_ui.internal.ui.core.UIContainer;
import wfg.native_ui.ui.ComponentFactory;
import wfg.native_ui.ui.component.BackgroundComp;
import wfg.native_ui.ui.component.InteractionComp;
import wfg.native_ui.ui.component.NativeComponents;
import wfg.native_ui.ui.component.OutlineComp;
import wfg.native_ui.ui.functional.ClickHandler;
import wfg.native_ui.ui.system.InteractionSystem;
import wfg.native_ui.ui.system.NativeSystems;
import wfg.native_ui.ui.visual.InteractiveSprite;
import wfg.native_ui.util.NativeUiUtils;
import wfg.native_ui.util.NativeUiUtils.AnchorType;
import wfg.native_ui.ui.core.UIBuildableAPI;
import wfg.native_ui.ui.core.UIElementFlags.HasBackground;
import wfg.native_ui.ui.core.UIElementFlags.HasOutline;
import wfg.native_ui.ui.dialog.DialogPanel;

import static wfg.ltv_econ.constant.strings.LocalizedStrings.*;
import static wfg.native_ui.util.UIConstants.*;

public final class LtvCommodityPanel extends UIContainer implements HasBackground, HasOutline,
    UIBuildableAPI
{
    public final BackgroundComp bg = comp().get(NativeComponents.BACKGROUND);
    public final OutlineComp outline = comp().get(NativeComponents.OUTLINE);

    public static final int STANDARD_WIDTH = 284;
    public String m_headerTxt;
    public boolean rowsIgnoreUIState = false;
    public final List<CommodityRowPanel> commodityRows = new ArrayList<>();
    public final MarketAPI mMarket;
    public ClickHandler<CommodityRowPanel> selectionListener;

    public LtvCommodityPanel(int width, int height, String headerTxt, MarketAPI market) {
        this(width, height, headerTxt, false, market);
    }

    public LtvCommodityPanel(int width, int height,
        MarketAPI market) {
        this(width, height, str("uiTitleCommodities"), false, market);
    }

    public LtvCommodityPanel(int width, int height,
        boolean rowsIgnoreUIState, MarketAPI market
    ) {
        this(width, height, str("uiTitleCommodities"), rowsIgnoreUIState, market);
    }

    public LtvCommodityPanel(int width, int height,
        String headerTxt, boolean rowsIgnoreUIState, MarketAPI market
    ) { super(width, height);

        mMarket = market;
        m_headerTxt = headerTxt;
        this.rowsIgnoreUIState = rowsIgnoreUIState;

        bg.alpha = 0.65f;
    }

    public static Comparator<CommoditySpecAPI> getCommodityOrderComparator() {
        return Comparator.comparingDouble(com -> com.getOrder());
    }

    public void buildUI() {
        clearChildren();

        final List<CommoditySpecAPI> commodities = new ArrayList<>(EconomyConstants.econCommoditySpecs);
        Collections.sort(commodities, getCommodityOrderComparator());
        commodities.removeIf(com -> {
            return EconomyEngine.instance().getComCell(com.getId(), mMarket.getId()).getActivityIndicator() <= 0f;
        });

        final TooltipMakerAPI headerTp = ComponentFactory.createTooltip(
            getWidth(), false
        );
        headerTp.addSectionHeading(m_headerTxt, Alignment.MID, pad);

        final int headerHeight = (int) headerTp.getPrev().getPosition().getHeight();
        headerTp.setHeightSoFar(headerHeight);
        ComponentFactory.addTooltip(headerTp, headerHeight, false, this).inTL(0, 0);
        bg.offset.setOffset(1, 1, -2, -headerHeight - 2);
        outline.offset.setOffset(1, 1, -2, -headerHeight - 2);

        final TooltipMakerAPI rowTp = ComponentFactory.createTooltip(
            getWidth(), true
        );
        
        final int rowWidth = (int) getWidth() - opad * 2;
        final int rowHeight = 28;
        int cumulativeYOffset = opad;

        for (CommoditySpecAPI com : commodities) {
            final CommodityRowPanel comRow = new CommodityRowPanel(
                mMarket, com.getId(), rowWidth, 
                rowHeight, rowsIgnoreUIState
            );

            rowTp.addComponent(comRow).inTL(opad, cumulativeYOffset);

            cumulativeYOffset += pad + 2 + rowHeight;

            comRow.interaction.onClicked = selectionListener;

            commodityRows.add(comRow);
        }
        rowTp.setHeightSoFar(cumulativeYOffset);
        ComponentFactory.addTooltip(rowTp, getHeight() - headerHeight, true, this)
            .inTL(0, headerHeight);
  
        if (MarketTradeDisruptionData.shouldSuspendTrade(mMarket)) {
            // TODO test this.
            final InteractiveSprite tradeSuspensionIcon = new InteractiveSprite(20, 20, Sprites.WARNING, UIColors.CARGO_COLOR, null);
            add(tradeSuspensionIcon).inTL(-20 - opad, pad);
            tradeSuspensionIcon.tooltip.positioner = (tp, isExpanded) -> {
                NativeUiUtils.anchorPanel(tp, tradeSuspensionIcon, AnchorType.LeftTop, opad);
            };
            tradeSuspensionIcon.tooltip.builder = (tp, isExpanded) -> {
                final var data = MarketTradeDisruptionData.get(mMarket);
                final String daysStr = Integer.toString(data.getSuspendedDays());
                final String dayDays = UIUtils.getDayOrDays(data.getSuspendedDays());
                tp.addPara(str("uiTpTxtTradeSuspensionIconInfo"), 0f, new Color[]{highlight, text_color}, daysStr, dayDays);

                tp.addPara(str("uiTpTxtTradeSuspensionIconClick"), pad);
            };
            tradeSuspensionIcon.system().setIfNotPresent(NativeSystems.INTERACTION, InteractionSystem.get(), tradeSuspensionIcon);
            final InteractionComp<InteractiveSprite> suspensionComp = tradeSuspensionIcon.comp().get(NativeComponents.INTERACTION);
            suspensionComp.onClicked = (icon, isLeftClick) -> {
                new ClearSuspensionDialog().show(0.3f, 0.3f);
            };
        }
    }

    public void selectRow(String comID) {
        final CommodityOnMarketAPI com = mMarket.getCommodityData(comID);
        for (CommodityRowPanel row : commodityRows) {
            row.glow.persistent = row.cell.spec == com;
        }
    }

    public void selectRow(CommodityRowPanel selectedRow) {
        for (CommodityRowPanel row : commodityRows) {
            row.glow.persistent = row == selectedRow;
        }
    }

    public final class ClearSuspensionDialog extends DialogPanel {
        
        public ClearSuspensionDialog() {
            super(500, 100, null, str("uiTxtResetTradeSuspension"), str("uiConfirm"), str("uiCancel"));

            backgroundDimAmount = 0.1f;
            holo.borderAlpha = 0.66f;

            setConfirmShortcut();
        }

        @Override
        public void dismiss(int option) {
            super.dismiss(option);

            if (option == 0) {
                MarketTradeDisruptionData.get(mMarket).resetLosses();
                LtvCommodityPanel.this.buildUI();
            }
        }
    }
}
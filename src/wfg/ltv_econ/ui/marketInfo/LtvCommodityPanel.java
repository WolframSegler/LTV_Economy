package wfg.ltv_econ.ui.marketInfo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import com.fs.starfarer.api.campaign.econ.CommodityOnMarketAPI;
import com.fs.starfarer.api.campaign.econ.CommoditySpecAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.ui.Alignment;
import com.fs.starfarer.api.ui.Fonts;
import com.fs.starfarer.api.ui.LabelAPI;

import rolflectionlib.util.RolfLectionUtil;
import wfg.ltv_econ.constant.EconomyConstants;
import wfg.ltv_econ.economy.engine.EconomyEngine;
import wfg.native_ui.internal.ui.core.UIContainer;
import wfg.native_ui.ui.ComponentFactory;
import wfg.native_ui.ui.component.BackgroundComp;
import wfg.native_ui.ui.component.NativeComponents;
import wfg.native_ui.ui.component.OutlineComp;
import wfg.native_ui.ui.functional.ClickHandler;
import wfg.native_ui.ui.core.UIBuildableAPI;
import wfg.native_ui.ui.core.UIContainerAPI;
import wfg.native_ui.ui.core.UIElementFlags.HasBackground;
import wfg.native_ui.ui.core.UIElementFlags.HasOutline;

import static wfg.ltv_econ.constant.strings.LocalizedStrings.*;
import static wfg.native_ui.util.Globals.settings;
import static wfg.native_ui.util.UIConstants.*;

public final class LtvCommodityPanel extends UIContainer implements HasBackground, HasOutline,
    UIBuildableAPI
{
    public static final int STANDARD_WIDTH = 284;

    public final BackgroundComp bg = comp().get(NativeComponents.BACKGROUND);
    public final OutlineComp outline = comp().get(NativeComponents.OUTLINE);

    public String mHeaderTxt;
    public final List<CommodityRowPanel> mCommodityRows = new ArrayList<>();
    public final MarketAPI mMarket;
    public ClickHandler<CommodityRowPanel> mSelectionListener;

    public LtvCommodityPanel(int width, int height, MarketAPI market) {
        this(width, height, str("uiTitleCommodities"), market);
    }

    public LtvCommodityPanel(int width, int height, String headerTxt, MarketAPI market) {
        super(width, height);

        mMarket = market;
        mHeaderTxt = headerTxt;

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

        final LabelAPI headerLbl = settings.createLabel(mHeaderTxt, Fonts.ORBITRON_12);
        RolfLectionUtil.getMethodAndInvokeDirectly("setBackgroundColor", headerLbl, dark);
        headerLbl.setAlignment(Alignment.MID);
        headerLbl.setColor(base);
        headerLbl.autoSizeToWidth(getWidth());
        final int headerH = (int) headerLbl.getPosition().getHeight() + 3;
        headerLbl.getPosition().setSize(getWidth(), headerH);
        add(headerLbl).inTL(0f, 0f);

        bg.offset.setOffset(1, 1, -2, -headerH - 2);
        outline.offset.setOffset(1, 1, -2, -headerH - 2);

        final int contentW = (int) getWidth() - 5;
        final UIContainerAPI rowContainer = new UIContainer(contentW, 0f);
        
        final int rowWidth = (int) contentW - opad * 2;
        final int rowHeight = 28;
        int cumulativeYOffset = opad;

        for (CommoditySpecAPI com : commodities) {
            final CommodityRowPanel comRow = new CommodityRowPanel(
                mMarket, com.getId(), rowWidth, 
                rowHeight, this
            );

            rowContainer.add(comRow).inTL(opad, cumulativeYOffset);

            cumulativeYOffset += pad + 2 + rowHeight;

            comRow.interaction.onClicked = mSelectionListener;

            mCommodityRows.add(comRow);
        }
        rowContainer.setHeight(cumulativeYOffset);
        add(ComponentFactory.wrapWithScrollPanel(rowContainer, contentW, getHeight() - headerH)).inTL(0f, headerH);
    }

    public void selectRow(String comID) {
        final CommodityOnMarketAPI com = mMarket.getCommodityData(comID);
        for (CommodityRowPanel row : mCommodityRows) {
            row.glow.persistent = row.cell.spec == com;
        }
    }

    public void selectRow(CommodityRowPanel selectedRow) {
        for (CommodityRowPanel row : mCommodityRows) {
            row.glow.persistent = row == selectedRow;
        }
    }
}
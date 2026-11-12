package wfg.ltv_econ.ui.marketInfo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import com.fs.starfarer.api.campaign.econ.CommoditySpecAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.ui.Alignment;
import com.fs.starfarer.api.ui.Fonts;
import com.fs.starfarer.api.ui.LabelAPI;

import wfg.ltv_econ.constant.EconomyConstants;
import wfg.ltv_econ.economy.engine.EconomyEngine;
import wfg.native_ui.internal.ui.core.UIContainer;
import wfg.native_ui.ui.component.BackgroundComp;
import wfg.native_ui.ui.component.NativeComponents;
import wfg.native_ui.ui.component.OutlineComp;
import wfg.native_ui.ui.functional.ClickHandler;
import wfg.native_ui.ui.table.ListTable;
import wfg.native_ui.ui.core.UIBuildableAPI;
import wfg.native_ui.ui.core.UIElementFlags.HasBackground;
import wfg.native_ui.ui.core.UIElementFlags.HasOutline;

import static wfg.ltv_econ.constant.strings.LocalizedStrings.*;
import static wfg.native_ui.util.Globals.settings;
import static wfg.native_ui.util.UIConstants.*;

public final class LtvCommodityPanel extends UIContainer implements HasBackground, HasOutline, UIBuildableAPI {
    public static final int STANDARD_WIDTH = 284;
    private static final int ROW_H = 28;

    public final BackgroundComp bg = comp().get(NativeComponents.BACKGROUND);
    public final OutlineComp outline = comp().get(NativeComponents.OUTLINE);

    public final String mHeaderTxt;
    public final MarketAPI mMarket;
    public ClickHandler<CommodityRowPanel> mSelectionListener;

    private CommodityList mCommodityList;

    public LtvCommodityPanel(int width, int height, MarketAPI market) {
        this(width, height, str("uiTitleCommodities"), market);
    }

    public LtvCommodityPanel(int width, int height, String headerTxt, MarketAPI market) {
        super(width, height);
        mMarket = market;
        mHeaderTxt = headerTxt;

        bg.alpha = 0.65f;

        buildUI();
    }

    public void buildUI() {
        clearChildren();

        final LabelAPI headerLbl = settings.createLabel(mHeaderTxt, Fonts.ORBITRON_12);
        headerLbl.setBackgroundColor(dark);
        headerLbl.setAlignment(Alignment.MID);
        headerLbl.setColor(base);
        headerLbl.autoSizeToWidth(getWidth());
        final int headerH = (int) headerLbl.getPosition().getHeight() + 3;
        headerLbl.getPosition().setSize(getWidth(), headerH);
        add(headerLbl).inTL(0f, 0f);

        bg.offset.setOffset(1, 1, -2, -headerH - 2);
        outline.offset.setOffset(1, 1, -2, -headerH - 2);

        final float contentH = getHeight() - headerH;

        mCommodityList = new CommodityList(getWidth(), contentH);
        add(mCommodityList).inTL(0f, headerH);
    }

    public void selectRow(String comID) {
        for (CommodityRowPanel row : mCommodityList.rows()) {
            row.glow.setHighlighted(row.mCell.comID.equals(comID));
        }
    }

    public void selectRow(CommodityRowPanel selectedRow) {
        for (CommodityRowPanel row : mCommodityList.rows()) {
            row.glow.setHighlighted(row == selectedRow);
        }
    }

    private final class CommodityList extends ListTable<CommoditySpecAPI, CommodityRowPanel> {
        CommodityList(float width, float height) {
            super(width, height, ROW_H, pad);
            mAddRowGap = true;
            buildUI();
        }

        protected List<CommoditySpecAPI> getDataList() {
            final List<CommoditySpecAPI> commodities = new ArrayList<>(EconomyConstants.econCommoditySpecs);
            Collections.sort(commodities, Comparator.comparingDouble(com -> com.getOrder()));
            commodities.removeIf(com ->
                EconomyEngine.instance().getComCell(com.getId(), mMarket.getId()).getActivityIndicator() <= 0f
            );
            return commodities;
        }

        protected CommodityRowPanel createWidget(CommoditySpecAPI com, int index) {
            final CommodityRowPanel row = new CommodityRowPanel(mMarket, com.getId(), LtvCommodityPanel.this);
            row.interaction.onClicked = mSelectionListener;
            return row;
        }

        protected void onWidgetClicked(CommodityRowPanel source) {}

        protected String getEmptyMessage() {
            return "";
        }

        final List<CommodityRowPanel> rows() {
            return mWidgets;
        }
    }
}
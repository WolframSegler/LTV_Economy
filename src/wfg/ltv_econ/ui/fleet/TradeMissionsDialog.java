package wfg.ltv_econ.ui.fleet;

import static wfg.native_ui.util.UIConstants.*;
import static wfg.ltv_econ.constant.strings.LocalizedStrings.*;
import static wfg.native_ui.util.Globals.settings;

import java.util.ArrayList;
import java.util.List;

import org.lwjgl.input.Keyboard;

import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.ui.Fonts;
import com.fs.starfarer.api.ui.LabelAPI;

import wfg.ltv_econ.economy.engine.EconomyEngine;
import wfg.ltv_econ.economy.fleet.TradeMission;
import wfg.native_ui.internal.ui.Side;
import wfg.native_ui.ui.Attachments;
import wfg.native_ui.ui.container.DockPanel;
import wfg.native_ui.ui.table.ListTable;
import wfg.native_ui.ui.widget.DockButton;
import wfg.native_ui.ui.widget.Button.CutStyle;
import wfg.native_ui.ui.widget.RadioPanel;
import wfg.native_ui.ui.widget.RadioPanel.LayoutMode;

public final class TradeMissionsDialog extends DockPanel {
    private static final int WIDTH = 420;
    private static final int GAP = 100;
    private static final int ROW_H = 200;

    private final MarketAPI market;
    private boolean marketRelevantOnly;
    private boolean activeMissions = true;
    private float scrollLen = 0f;

    public TradeMissionsDialog(MarketAPI market, boolean marketRelevantOnly) {
        super(Attachments.getCoreUI(), WIDTH, screenH - GAP * 2, Side.LEFT, 8);

        this.market = market;
        this.marketRelevantOnly = marketRelevantOnly;

        buildUI();
    }

    @Override
    public void buildUI() {
        clearChildren();

        final LabelAPI title = settings.createLabel(str("tradeMissionsTitle"), Fonts.INSIGNIA_LARGE);
        add(title).inTL(0f, opad);

        final RadioPanel monthSwitch = new RadioPanel(110, 18, LayoutMode.HORIZONTAL)
            .addOption(str("activeTitle"), activeMissions)
            .addOption(str("pastTitle"), !activeMissions);
        monthSwitch.buildUI();
        add(monthSwitch).inTR(opad - pad, opad);

        final DockButton<FiltersDialog> filterBtn = new DockButton<>(90, 18, str("filtersTitle"),
            Fonts.DEFAULT_SMALL, () -> new FiltersDialog(this)
        );
        filterBtn.setCutStyle(CutStyle.ALL);
        filterBtn.bgAlpha = 1f;
        filterBtn.setShortcutAndAppendToText(Keyboard.KEY_Q);
        add(filterBtn).inTR(opad + 110 + hpad - pad, opad);

        final int offset = opad + 30;
        final float listHeight = mContentContainer.getHeight() - offset - opad;
        final MissionList list = new MissionList(mContentContainer.getWidth(), listHeight, scrollLen);
        add(list).inTL(0f, offset);

        monthSwitch.optionSelected = code -> {
            activeMissions = code == 0;
            scrollLen = list.getScrollOffset();
            buildUI();
        };
    }

    private final class MissionList extends ListTable<TradeMission, TradeMissionWidget> {
        MissionList(float width, float height, float initialScroll) {
            super(width, height, ROW_H, pad);
            mAddRowGap = true;
            mScrollOffset = initialScroll;
            buildUI();
        }

        @Override
        protected List<TradeMission> getDataList() {
            final EconomyEngine engine = EconomyEngine.instance();
            final List<TradeMission> missions = activeMissions
                ? engine.getActiveMissions()
                : engine.getPastMissions();

            final List<TradeMission> filtered = new ArrayList<>();
            for (TradeMission m : missions) {
                if (marketRelevantOnly && market != null && market != m.src && market != m.dest) continue;
                if (TradeFilters.hideVirtualFleets && m.spawnedFleetFinishedJob) continue;
                if (TradeFilters.directionMode == 1 && market != m.src) continue;
                if (TradeFilters.directionMode == 2 && market != m.dest) continue;
                if (TradeFilters.directionMode == 3 && !m.inFaction) continue;
                if (TradeFilters.minTradeAmount > m.getTotalAmount()) continue;
                if (TradeFilters.exporterFactionBlacklist.contains(m.src.getFactionId())) continue;
                if (TradeFilters.importerFactionBlacklist.contains(m.dest.getFactionId())) continue;
                filtered.add(m);
            }
            return filtered;
        }

        protected TradeMissionWidget createWidget(TradeMission m, int index) {
            final boolean isSrcMarket = market != null && market == m.src;
            return new TradeMissionWidget(computeContentWidth(), ROW_H, m, isSrcMarket, TradeMissionsDialog.this);
        }

        protected void onWidgetClicked(TradeMissionWidget source) {}

        protected String getEmptyMessage() {
            return activeMissions ? str("noActiveMissions") : str("noPastMissions");
        }

        float getScrollOffset() {
            return mScrollContainer != null ? mScrollContainer.getYOffset() : mScrollOffset;
        }
    }
}
package wfg.ltv_econ.ui.fleet;

import static wfg.native_ui.util.UIConstants.*;

import java.util.List;

import org.lwjgl.input.Keyboard;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.SettingsAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.ui.Fonts;
import com.fs.starfarer.api.ui.LabelAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;

import wfg.ltv_econ.economy.engine.EconomyEngine;
import wfg.ltv_econ.economy.fleet.TradeMission;
import wfg.ltv_econ.util.Arithmetic;
import wfg.native_ui.internal.ui.Side;
import wfg.native_ui.ui.Attachments;
import wfg.native_ui.ui.ComponentFactory;
import wfg.native_ui.ui.container.DockPanel;
import wfg.native_ui.ui.functional.DockButton;
import wfg.native_ui.ui.functional.Button.CutStyle;
import wfg.native_ui.ui.widget.RadioPanel;
import wfg.native_ui.ui.widget.RadioPanel.LayoutMode;

public class TradeMissionsDialog extends DockPanel {
    private static final SettingsAPI settings = Global.getSettings();

    private static final int WIDTH = 440;
    private static final int GAP = 100;
    private static final int I_WIDTH = WIDTH - hpad * 3;
    private static final int ROW_H = 200;

    private final MarketAPI market;
    private boolean marketRelevantOnly;
    private boolean activeMissions = true;
    private float scrollLen = 0f;

    public TradeMissionsDialog(MarketAPI market, boolean marketRelevantOnly) {
        super(Attachments.getCoreUI(), WIDTH, screenH - GAP * 2, Side.LEFT);

        this.market = market;
        this.marketRelevantOnly = marketRelevantOnly;

        buildUI();
    }

    @Override
    public void buildUI() {
        clearChildren();
        final LabelAPI title = settings.createLabel("Trade Missions", Fonts.INSIGNIA_LARGE);
        add(title).inTL(opad, opad * 2);

        EconomyEngine engine = EconomyEngine.instance();
        final List<TradeMission> missions = activeMissions ? engine.getActiveMissions() : engine.getPastMissions();

        final TooltipMakerAPI scrollPanel = ComponentFactory.createTooltip(I_WIDTH, true);

        final RadioPanel monthSwitch = new RadioPanel(m_panel, 110, 18, LayoutMode.HORIZONTAL)
            .addOption("Active", activeMissions)
            .addOption("Past", !activeMissions);
        monthSwitch.optionSelected = code -> {
            activeMissions = code == 0;
            scrollLen = scrollPanel.getExternalScroller().getYOffset();
            buildUI();
        };
        monthSwitch.buildUI();
        add(monthSwitch).inTR(opad*2, opad*2);

        final DockButton<FiltersDialog> filterBtn = new DockButton<>(m_panel, 90, 18, "Filters",
            Fonts.DEFAULT_SMALL, () -> new FiltersDialog(this)
        );
        filterBtn.cutStyle = CutStyle.ALL;
        filterBtn.bgAlpha = 1f;
        filterBtn.setShortcutAndAppendToText(Keyboard.KEY_Q);
        add(filterBtn).inTR(opad*2 + 110 + hpad, opad*2 + pad);

        float yCoord = pad;
        for (TradeMission m : missions) {
            if (marketRelevantOnly && market != null && market != m.src && market != m.dest) continue;

            if (TradeFilters.directionMode == 1 && market != m.src) continue;
            if (TradeFilters.directionMode == 2 && market != m.dest) continue;
            if (TradeFilters.directionMode == 3 && !m.inFaction) continue;
            if (TradeFilters.minTradeAmount > m.getTotalAmount()) continue;
            if (TradeFilters.exporterFactionBlacklist.contains(m.src.getFactionId())) continue;
            if (TradeFilters.importerFactionBlacklist.contains(m.dest.getFactionId())) continue;

            final boolean isSrcMarket = market != null && market == m.src;
            final TradeMissionWidget row = new TradeMissionWidget(
                scrollPanel, I_WIDTH - opad, ROW_H - opad, m, isSrcMarket, this
            );

            scrollPanel.addCustom(row.getPanel(), 0).getPosition().inTL(hpad, yCoord);

            yCoord += ROW_H + pad;
        }

        if (missions.isEmpty()) {
            final LabelAPI lbl = settings.createLabel(activeMissions ?
                "No active missions" : "Past missions are not saved", Fonts.DEFAULT_SMALL
            );
            lbl.setColor(gray);
            add(lbl).inMid();
        }

        final int offset = opad * 2 + 30;
        scrollPanel.setHeightSoFar(yCoord);
        final float scrollPanelH = pos.getHeight() - offset - opad;
        ComponentFactory.addTooltip(scrollPanel, scrollPanelH, true, m_panel).inTL(pad, offset);

        scrollPanel.getExternalScroller().setYOffset(Arithmetic.clamp(
            scrollLen, 0f, scrollPanel.getHeightSoFar() - scrollPanelH
        ));
    }
}
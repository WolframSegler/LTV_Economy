package wfg.ltv_econ.ui.fleet;

import static wfg.native_ui.util.UIConstants.*;

import java.util.List;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.SettingsAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.ui.Fonts;
import com.fs.starfarer.api.ui.LabelAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;

import wfg.ltv_econ.economy.engine.EconomyEngine;
import wfg.ltv_econ.economy.fleet.TradeMission;
import wfg.native_ui.internal.ui.Side;
import wfg.native_ui.ui.Attachments;
import wfg.native_ui.ui.ComponentFactory;
import wfg.native_ui.ui.container.DockPanel;

public class TradeMissionsDialog extends DockPanel {
    private static final SettingsAPI settings = Global.getSettings();

    private static final int WIDTH = 440;
    private static final int GAP = 100;
    private static final int I_WIDTH = WIDTH - hpad * 3;
    private static final int ROW_H = 200;

    private final MarketAPI market;
    private boolean marketRelevantOnly;

    public TradeMissionsDialog(MarketAPI market, boolean marketRelevantOnly) {
        super(Attachments.getCoreUI(), WIDTH,
            (int) settings.getScreenHeight() - GAP * 2,
            Side.LEFT
        );

        this.market = market;
        this.marketRelevantOnly = marketRelevantOnly;

        buildUI();
    }

    @Override
    public void buildUI() {
        clearChildren();
        final LabelAPI title = settings.createLabel("Trade Missions", Fonts.INSIGNIA_LARGE);
        add(title).inTL(opad, opad * 2);

        final TooltipMakerAPI scrollPanel = ComponentFactory.createTooltip(I_WIDTH, true);

        EconomyEngine engine = EconomyEngine.instance();
        final List<TradeMission> active = engine.getActiveMissions();
        final List<TradeMission> past = engine.getPastMissions();

        float yCoord = pad;
        for (TradeMission m : active) {
            if (marketRelevantOnly && market != null && market != m.src && market != m.dest) continue;

            final boolean isSrcMarket = market != null && market == m.src;
            final TradeMissionWidget row = new TradeMissionWidget(
                scrollPanel, I_WIDTH - opad, ROW_H - opad, m, isSrcMarket, this
            );

            scrollPanel.addCustom(row.getPanel(), 0).getPosition().inTL(hpad, yCoord);

            yCoord += ROW_H + pad;
        }

        yCoord += 100;

        for (TradeMission m : past) {
            if (marketRelevantOnly && market != null && market != m.src && market != m.dest) continue;

            final boolean isSrcMarket = market != null && market == m.src;
            final TradeMissionWidget row = new TradeMissionWidget(
                scrollPanel, I_WIDTH - opad, ROW_H - opad, m, isSrcMarket, this
            );

            scrollPanel.addCustom(row.getPanel(), 0).getPosition().inTL(hpad, yCoord);

            yCoord += ROW_H + pad;
        }

        final int offset = opad * 2 + 30;
        scrollPanel.setHeightSoFar(yCoord);
        final float scrollPanelH = pos.getHeight() - offset - opad;
        ComponentFactory.addTooltip(scrollPanel, scrollPanelH, true, m_panel).inTL(pad, offset);
    }
}
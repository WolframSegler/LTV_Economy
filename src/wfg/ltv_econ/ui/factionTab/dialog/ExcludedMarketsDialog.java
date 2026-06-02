package wfg.ltv_econ.ui.factionTab.dialog;

import static wfg.ltv_econ.constants.strings.LocalizedStrings.*;
import static wfg.native_ui.util.Globals.settings;
import static wfg.native_ui.util.UIConstants.*;

import java.awt.Color;
import java.util.List;
import java.util.Set;

import com.fs.starfarer.api.campaign.PlanetAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.ui.Alignment;
import com.fs.starfarer.api.ui.Fonts;
import com.fs.starfarer.api.ui.LabelAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI.PlanetInfoParams;
import com.fs.starfarer.api.ui.UIComponentAPI;
import com.fs.starfarer.api.ui.UIPanelAPI;

import wfg.ltv_econ.economy.engine.EconomyInfo;
import wfg.ltv_econ.serializable.LtvEconSaveData;
import wfg.native_ui.ui.ComponentFactory;
import wfg.native_ui.ui.component.HoverGlowComp;
import wfg.native_ui.ui.component.InteractionComp;
import wfg.native_ui.ui.component.NativeComponents;
import wfg.native_ui.ui.component.HoverGlowComp.GlowType;
import wfg.native_ui.ui.core.UIElementFlags.HasHoverGlow;
import wfg.native_ui.ui.dialog.DialogPanel;
import wfg.native_ui.ui.functional.UIClickable;
import wfg.native_ui.ui.table.GridTable;
import wfg.native_ui.ui.table.WidgetAPI;
import wfg.native_ui.ui.visual.SpritePanel.Base;

public class ExcludedMarketsDialog extends DialogPanel {
    private static final SpriteAPI FORBIDDEN = settings.getSprite("ui", "forbidden");
    private static final int GRID_W = 400;
    private static final int WIDGET_W = GRID_W - opad;
    private static final int WIDGET_H = 50;

    private static final PlanetInfoParams params = new PlanetInfoParams();
    static {
        params.showName = true;
        params.showConditions = false;
        params.showHazardRating = false;
        params.scaleEvenWhenShowingName = true;
    }
    
    public ExcludedMarketsDialog() {
        super(GRID_W, 600, null, null, str("dismissTxt"));

        backgroundDimAmount = 0.1f;
        holo.borderAlpha = 0.66f;

        setConfirmShortcut();

        final LabelAPI title = settings.createLabel(str("uiDialogTitleMarketsToExclude"), Fonts.INSIGNIA_LARGE);
        title.setAlignment(Alignment.TMID);
        title.autoSizeToWidth(400);
        add(title).inTL(0f, 0f);

        final PlayerMarketsGrid grid = new PlayerMarketsGrid(m_panel);
        add(grid).inTMid(30f);
    }

    public static class PlayerMarketsGrid extends GridTable<MarketAPI, PlayerMarketWidget> {

        public PlayerMarketsGrid(UIPanelAPI parent) {
            super(parent, GRID_W, 500, WIDGET_W, WIDGET_H, hpad);

            uniformOuterGap = true;
            isSelectionEnabled = true;
            buildUI();
        }

        protected final List<MarketAPI> getDataList() {
            final List<MarketAPI> markets = EconomyInfo.getMarketsCopy();
            markets.removeIf(m -> !m.isPlayerOwned());
            return markets;
        }

        protected final PlayerMarketWidget createWidget(MarketAPI market, int index) {
            return new PlayerMarketWidget(m_panel, market);
        }

        protected final void onWidgetClicked(PlayerMarketWidget source) {
            final Set<String> marketIDs = LtvEconSaveData.instance().playerFactionSettings.excludedMarketsFromWorkerAllocation;
            final String marketID = source.market.getId();
            if (marketIDs.contains(marketID)) {
                marketIDs.remove(marketID);
            } else {
                marketIDs.add(marketID);
            }
            source.buildUI();
        }

        protected final String getEmptyMessage() {
            return str("uiNoStaticAssets");
        }
    }

    public static class PlayerMarketWidget extends UIClickable<PlayerMarketWidget> implements WidgetAPI<PlayerMarketWidget>, HasHoverGlow {
        private final MarketAPI market;

        private final HoverGlowComp glow = comp().get(NativeComponents.HOVER_GLOW);

        public PlayerMarketWidget(UIPanelAPI parent, MarketAPI market) {
            super(parent, WIDGET_W, WIDGET_H, null);

            this.market = market;

            glow.type = GlowType.UNDERLAY;
            glow.color = base;

            buildUI();
        }

        public void buildUI() {
            clearChildren();

            final float nameW = 150f;

            final TooltipMakerAPI nameTp = ComponentFactory.createTooltip(100, false);
            final SectorEntityToken entity = market.getPrimaryEntity();

            if (entity instanceof PlanetAPI) {
                nameTp.showPlanetInfo(market.getPlanetEntity(), nameW, WIDGET_H, params, 0f);
            } else {
                nameTp.addImage(entity.getCustomEntitySpec().getIconName(), nameW, WIDGET_H, 0f);
                final LabelAPI lbl = nameTp.addPara(market.getName(), 0f);
                lbl.autoSizeToWidth(nameW).inBL(0f, pad);
                lbl.setAlignment(Alignment.MID);
            }
            ComponentFactory.addTooltip(nameTp, WIDGET_H, false, m_panel).inBL(0f, 0f);

            if (LtvEconSaveData.instance().playerFactionSettings.excludedMarketsFromWorkerAllocation.contains(market.getId())) {
                final Base icon = new Base(m_panel, 36, 36, FORBIDDEN, Color.RED, null);
                add(icon).inRMid(pad);
            }
        }

        public InteractionComp<PlayerMarketWidget> getInteraction() {
            return interaction;
        }

        public UIComponentAPI getElement() {
            return m_panel;
        }
    }
}
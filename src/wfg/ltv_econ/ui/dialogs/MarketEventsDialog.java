package wfg.ltv_econ.ui.dialogs;

import static wfg.native_ui.util.UIConstants.*;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.SettingsAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.ui.Fonts;
import com.fs.starfarer.api.ui.LabelAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.ui.UIPanelAPI;

import wfg.ltv_econ.economy.PlayerMarketData;
import wfg.ltv_econ.economy.engine.EconomyEngine;
import wfg.ltv_econ.intel.market.events.MarketEvent;
import wfg.native_ui.ui.Attachments;
import wfg.native_ui.ui.ComponentFactory;
import wfg.native_ui.ui.components.AudioFeedbackComp;
import wfg.native_ui.ui.components.BackgroundComp;
import wfg.native_ui.ui.components.HoverGlowComp;
import wfg.native_ui.ui.components.HoverGlowComp.GlowType;
import wfg.native_ui.ui.components.NativeComponents;
import wfg.native_ui.ui.components.TooltipComp;
import wfg.native_ui.ui.core.UIElementFlags.HasAudioFeedback;
import wfg.native_ui.ui.core.UIElementFlags.HasBackground;
import wfg.native_ui.ui.core.UIElementFlags.HasHoverGlow;
import wfg.native_ui.ui.core.UIElementFlags.HasTooltip;
import wfg.native_ui.ui.panels.CustomPanel;
import wfg.native_ui.ui.panels.DockPanel;
import wfg.native_ui.ui.panels.SpritePanel.Base;

public class MarketEventsDialog extends DockPanel {
    private static final SettingsAPI settings = Global.getSettings();
    private static final int ROW_H = 48;
    private final PlayerMarketData data;

    public MarketEventsDialog(final MarketAPI market) {
        super(Attachments.getCoreUI(), 500,
            (int) settings.getScreenHeight() - 200,
            DockDirection.LEFT
        );
        data = EconomyEngine.getInstance().getPlayerMarketData(market.getId());

        offsetY = 100f;
        bgAlpha = 0.9f;

        createPanel();
    }

    @Override
    public void createPanel() {
        final int width = (int) (pos.getWidth() - opad*2);

        final LabelAPI title = settings.createLabel("Current Events", Fonts.INSIGNIA_LARGE);
        add(title).inTL(opad, opad*2);

        final TooltipMakerAPI eventsList = ComponentFactory.createTooltip(width, true);

        float yCoord = pad;
        for (MarketEvent event : data.getEvents()) {
            if (!event.isVisible(data)) return;

            final RowPanel row = new RowPanel(
                eventsList, width - opad, ROW_H, event
            );
            eventsList.addCustom(row.getPanel(), 0).getPosition().inTL(pad, yCoord);

            yCoord += ROW_H + pad;
        }

        eventsList.setHeightSoFar(yCoord);
        final int offset = opad*2 + 30;
        ComponentFactory.addTooltip(eventsList, pos.getHeight() - offset - opad, true, m_panel).inTL(opad, offset);
    }

    public class RowPanel extends CustomPanel<RowPanel> 
        implements HasHoverGlow, HasAudioFeedback, HasTooltip, HasBackground
    {
        public final BackgroundComp bg = comp().get(NativeComponents.BACKGROUND);
        public final HoverGlowComp glow = comp().get(NativeComponents.HOVER_GLOW);
        public final AudioFeedbackComp audio = comp().get(NativeComponents.AUDIO_FEEDBACK);
        public final TooltipComp tooltip = comp().get(NativeComponents.TOOLTIP);

        private final MarketEvent event;

        public RowPanel(UIPanelAPI parent, int width, int height, MarketEvent event) {
            super(parent, width, height);

            this.event = event;

            glow.type = GlowType.UNDERLAY;
            bg.color = event.spec.tags.contains("negative") ? negative : positive;
            bg.alpha = 0.1f;

            tooltip.width = 500f;
            tooltip.builder = (tp, exp) -> {
                event.createTooltip(data, tp);
            };

            createPanel();
        }

        public void createPanel() {
            final int iconSize = ROW_H - 4;

            try {
                settings.loadTexture(event.spec.iconPath);
            } catch (Exception e) {
                Global.getLogger(getClass()).warn(e);
            }

            final Base comIcon = new Base(
                m_panel, iconSize, iconSize, event.spec.iconPath,
                null, null
            );
            RowPanel.this.add(comIcon).inBL(pad, (ROW_H - iconSize) / 2f);

            final LabelAPI comNameLabel = settings.createLabel(event.spec.name, Fonts.ORBITRON_16);
            comNameLabel.setColor(event.spec.tags.contains("negative") ? negative : base);
            final float labelW = comNameLabel.computeTextHeight(event.spec.name);
            RowPanel.this.add(comNameLabel).inBL(iconSize + opad, (ROW_H - labelW) / 2f);
        }
    }
}
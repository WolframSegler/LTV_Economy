package wfg.ltv_econ.ui.marketInfo.dialogs;

import static wfg.native_ui.util.UIConstants.*;

import java.util.ArrayList;

import static wfg.ltv_econ.constant.strings.LocalizedStrings.str;
import static wfg.native_ui.util.Globals.settings;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.impl.campaign.DebugFlags;
import com.fs.starfarer.api.ui.Fonts;
import com.fs.starfarer.api.ui.LabelAPI;

import wfg.ltv_econ.constant.strings.LocalizedStrings;
import wfg.ltv_econ.economy.engine.EconomyEngine;
import wfg.ltv_econ.economy.market.MarketPopulationData;
import wfg.ltv_econ.intel.market.events.MarketEvent;
import wfg.native_ui.internal.ui.Side;
import wfg.native_ui.internal.ui.core.UIContainer;
import wfg.native_ui.ui.Attachments;
import wfg.native_ui.ui.component.AudioFeedbackComp;
import wfg.native_ui.ui.component.BackgroundComp;
import wfg.native_ui.ui.component.HoverGlowComp;
import wfg.native_ui.ui.component.InteractionComp;
import wfg.native_ui.ui.component.HoverGlowComp.GlowType;
import wfg.native_ui.ui.component.NativeComponents;
import wfg.native_ui.ui.component.TooltipComp;
import wfg.native_ui.ui.core.UIElementFlags.HasAudioFeedback;
import wfg.native_ui.ui.core.UIElementFlags.HasBackground;
import wfg.native_ui.ui.core.UIElementFlags.HasHoverGlow;
import wfg.native_ui.ui.core.UIElementFlags.HasTooltip;
import wfg.native_ui.ui.table.ListTable;
import wfg.native_ui.ui.table.WidgetAPI;
import wfg.native_ui.ui.container.DockPanel;
import wfg.native_ui.ui.visual.AbstractSpriteElement.SpriteElement;

public final class MarketEventsDialog extends DockPanel {
    private static final int ROW_H = 48;
    private final MarketPopulationData data;

    public MarketEventsDialog(final MarketAPI market) {
        super(Attachments.getCoreUI(), 400, screenH - 200, Side.LEFT, 8);
        data = EconomyEngine.instance().getMarketPopulationData(market.getId());

        mOffsetY = 100f;
        mBgAlpha = 0.9f;

        buildUI();
    }

    public void buildUI() {
        clearChildren();

        final LabelAPI title = settings.createLabel(
            LocalizedStrings.str("uiTitleCurrentEvents"), Fonts.INSIGNIA_LARGE);
        add(title).inTL(0f, opad);

        final float listH = mContentContainer.getHeight() - 30 - opad * 2;
        final EventList list = new EventList(mContentContainer.getWidth(), listH);
        add(list).inBL(0f, 0f);
    }

    private final class EventList extends ListTable<MarketEvent, RowPanel> {
        EventList(float width, float height) {
            super(width, height, ROW_H, pad);
            mAddRowGap = true;
            buildUI();
        }

        protected ArrayList<MarketEvent> getDataList() {
            final ArrayList<MarketEvent> events = new ArrayList<>();
            for (MarketEvent event : data.getEvents()) {
                if (!event.isVisible(data) && !DebugFlags.COLONY_DEBUG) continue;
                events.add(event);
            }
            return events;
        }

        protected RowPanel createWidget(MarketEvent event, int index) {
            return new RowPanel(event);
        }

        protected void onWidgetClicked(RowPanel source) {}

        protected String getEmptyMessage() {
            return str("uiDialogTxtMarketEventEmpty");
        }
    }

    public final class RowPanel extends UIContainer implements
        WidgetAPI<RowPanel>, HasHoverGlow, HasAudioFeedback, HasTooltip, HasBackground
    {
        public final BackgroundComp bg = comp().get(NativeComponents.BACKGROUND);
        public final HoverGlowComp glow = comp().get(NativeComponents.HOVER_GLOW);
        public final AudioFeedbackComp audio = comp().get(NativeComponents.AUDIO_FEEDBACK);
        public final TooltipComp tooltip = comp().get(NativeComponents.TOOLTIP);

        private final MarketEvent event;

        public RowPanel(MarketEvent event) {
            this.event = event;

            glow.type = GlowType.UNDERLAY;
            bg.color = event.spec.tags.contains("negative") ? negative : positive;
            bg.alpha = 0.1f;

            tooltip.width = 500f;
            tooltip.builder = (tp, exp) -> event.createTooltip(data, tp);
        }

        public InteractionComp<RowPanel> getInteraction() {
            return null;
        }

        public void buildUI() {
            clearChildren();

            final int iconSize = ROW_H - 4;

            try {
                settings.loadTexture(event.spec.iconPath);
            } catch (Exception e) {
                Global.getLogger(getClass()).warn(e);
            }

            final SpriteElement comIcon = new SpriteElement(
                iconSize, iconSize, event.spec.iconPath, null, null
            );
            add(comIcon).inBL(pad, (getHeight() - iconSize) / 2f);

            final LabelAPI comNameLabel = settings.createLabel(
                event.spec.name, Fonts.INSIGNIA_LARGE);
            comNameLabel.setColor(event.spec.tags.contains("negative") ? negative : base);
            add(comNameLabel).inLMid(iconSize + opad);
        }
    }
}
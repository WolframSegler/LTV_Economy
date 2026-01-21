package wfg.ltv_econ.ui.panels;

import java.util.List;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.econ.CommoditySpecAPI;
import com.fs.starfarer.api.ui.Fonts;
import com.fs.starfarer.api.ui.LabelAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.ui.UIPanelAPI;

import wfg.ltv_econ.economy.engine.EconomyInfo;
import wfg.wrap_ui.ui.ComponentFactory;
import wfg.wrap_ui.ui.components.AudioFeedbackComp;
import wfg.wrap_ui.ui.components.BackgroundComp;
import wfg.wrap_ui.ui.components.HoverGlowComp;
import wfg.wrap_ui.ui.components.InteractionComp;
import wfg.wrap_ui.ui.components.NativeComponents;
import wfg.wrap_ui.ui.components.OutlineComp;
import wfg.wrap_ui.ui.components.HoverGlowComp.GlowType;
import wfg.wrap_ui.ui.components.OutlineComp.OutlineType;
import wfg.wrap_ui.ui.panels.CustomPanel;
import wfg.wrap_ui.ui.panels.CustomPanel.HasBackground;
import wfg.wrap_ui.ui.panels.CustomPanel.HasOutline;
import wfg.wrap_ui.ui.panels.SpritePanel.Base;
import static wfg.wrap_ui.util.UIConstants.*;

public class CommoditySelectionPanel extends CustomPanel<CommoditySelectionPanel> implements
    HasOutline, HasBackground
{
    private static final int ROW_H = 32;
    private static GlobalCommodityFlow contentPanel = null;

    public final OutlineComp outline = comp().get(NativeComponents.OUTLINE);
    public final BackgroundComp bg = comp().get(NativeComponents.BACKGROUND);

    public CommoditySelectionPanel(UIPanelAPI parent, int width, int height, GlobalCommodityFlow content) {
        super(parent, width, height);

        contentPanel = content;

        outline.type = OutlineType.TEX_THIN;
        outline.color = dark;

        createPanel();
    }

    public void createPanel() {
        final int width = (int) pos.getWidth();
        final TooltipMakerAPI container = ComponentFactory.createTooltip(width, true);
        final List<CommoditySpecAPI> commodities = EconomyInfo.getEconCommodities();

        float yCoord = pad;
        for (CommoditySpecAPI spec : commodities) {
            final RowPanel row = new RowPanel(
                container, width - opad, ROW_H, spec
            );
            container.addCustom(row.getPanel(), 0).getPosition().inTL(pad, yCoord);

            yCoord += ROW_H + pad;
        }
        container.setHeightSoFar(yCoord);
        ComponentFactory.addTooltip(container, pos.getHeight(), true, m_panel).inTL(-pad, 0);
    }

    public static class RowPanel extends CustomPanel<RowPanel> 
        implements HasInteraction, HasHoverGlow, HasAudioFeedback
    {
        public final HoverGlowComp glow = comp().get(NativeComponents.HOVER_GLOW);
        public final AudioFeedbackComp audio = comp().get(NativeComponents.AUDIO_FEEDBACK);
        public final InteractionComp<RowPanel> interaction = comp().get(NativeComponents.INTERACTION);

        private final CommoditySpecAPI spec;

        public RowPanel(UIPanelAPI parent, int width, int height, CommoditySpecAPI com) {
            super(parent, width, height);

            spec = com;

            glow.type = GlowType.UNDERLAY;

            interaction.onClicked = (source, isLeftClick) -> {
                GlobalCommodityFlow.selectedCom = spec;
                contentPanel.createPanel();
            };

            createPanel();
        }

        public void createPanel() {
            final int iconSize = 28;

            final Base comIcon = new Base(
                getPanel(), iconSize, iconSize, spec.getIconName(),
                null, null
            );
            RowPanel.this.add(comIcon).inBL(pad, (ROW_H - iconSize) / 2f);

            final LabelAPI comNameLabel = Global.getSettings().createLabel(spec.getName(), Fonts.ORBITRON_12);
            comNameLabel.setColor(base);
            final float labelW = comNameLabel.computeTextHeight(spec.getName());
            RowPanel.this.add(comNameLabel).inBL(iconSize + opad, (ROW_H - labelW) / 2f);
        }
    }
}
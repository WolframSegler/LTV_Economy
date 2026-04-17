package wfg.ltv_econ.ui.economyTab;

import static wfg.ltv_econ.constants.EconomyConstants.visibleFactions;
import static wfg.native_ui.util.Globals.settings;
import static wfg.native_ui.util.UIConstants.*;

import java.awt.Color;

import com.fs.starfarer.api.campaign.FactionSpecAPI;
import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.ui.Fonts;
import com.fs.starfarer.api.ui.LabelAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.ui.UIPanelAPI;

import wfg.ltv_econ.serializable.LtvEconSaveData;
import wfg.native_ui.ui.ComponentFactory;
import wfg.native_ui.ui.component.AudioFeedbackComp;
import wfg.native_ui.ui.component.BackgroundComp;
import wfg.native_ui.ui.component.HoverGlowComp;
import wfg.native_ui.ui.component.InteractionComp;
import wfg.native_ui.ui.component.NativeComponents;
import wfg.native_ui.ui.component.OutlineComp;
import wfg.native_ui.ui.component.OutlineComp.OutlineType;
import wfg.native_ui.ui.core.UIBuildableAPI;
import wfg.native_ui.ui.core.UIElementFlags.HasAudioFeedback;
import wfg.native_ui.ui.core.UIElementFlags.HasBackground;
import wfg.native_ui.ui.core.UIElementFlags.HasHoverGlow;
import wfg.native_ui.ui.core.UIElementFlags.HasInteraction;
import wfg.native_ui.ui.core.UIElementFlags.HasOutline;
import wfg.native_ui.ui.core.UIElementFlags.HasTooltip;
import wfg.native_ui.ui.core.UIElementFlags.HasUIContext;
import wfg.native_ui.ui.component.TooltipComp;
import wfg.native_ui.ui.component.UIContextComp;
import wfg.native_ui.ui.component.HoverGlowComp.GlowType;
import wfg.native_ui.ui.panel.CustomPanel;
import wfg.native_ui.ui.visual.SpritePanel.Base;

public class FactionSelectionPanel extends CustomPanel implements
    HasOutline, HasBackground, HasUIContext, UIBuildableAPI
{
    public static final SpriteAPI restrictedPath = settings.getSprite("ui", "restricted");
    private static final int ROW_H = 32;

    public final OutlineComp outline = comp().get(NativeComponents.OUTLINE);
    public final BackgroundComp bg = comp().get(NativeComponents.BACKGROUND);
    public final UIContextComp context = comp().get(NativeComponents.UI_CONTEXT);

    public FactionSelectionPanel(UIPanelAPI parent, int width, int height) {
        super(parent, width, height);

        outline.type = OutlineType.TEX_THIN;
        outline.color = dark;
        context.ignore = true;

        buildUI();
    }

    public void buildUI() {
        final int width = (int) pos.getWidth();
        final TooltipMakerAPI container = ComponentFactory.createTooltip(width, true);

        float yCoord = pad;
        for (FactionSpecAPI faction : visibleFactions) {
            final RowPanel row = new RowPanel(
                container, width - pad*2, ROW_H, faction
            );
            container.addCustom(row.getPanel(), 0).getPosition().inTL(pad, yCoord);

            yCoord += ROW_H + pad;
        }
        container.setHeightSoFar(yCoord);
        ComponentFactory.addTooltip(container, getPos().getHeight(), true, m_panel).inTL(0f, 0f);
    }

    public class RowPanel extends CustomPanel implements UIBuildableAPI,
        HasInteraction, HasHoverGlow, HasAudioFeedback, HasBackground, HasTooltip
    {
        public final TooltipComp tooltip = comp().get(NativeComponents.TOOLTIP);
        public final BackgroundComp bg = comp().get(NativeComponents.BACKGROUND);
        public final HoverGlowComp glow = comp().get(NativeComponents.HOVER_GLOW);
        public final AudioFeedbackComp audio = comp().get(NativeComponents.AUDIO_FEEDBACK);
        public final InteractionComp<RowPanel> interaction = comp().get(NativeComponents.INTERACTION);
        
        private final FactionSpecAPI faction;
        public boolean alreadyEmbargoed;

        public RowPanel(UIPanelAPI parent, int width, int height, FactionSpecAPI faction) {
            super(parent, width, height);

            this.faction = faction;
            alreadyEmbargoed = LtvEconSaveData.instance().playerFactionSettings
                .embargoedFactions.contains(faction.getId());

            interaction.onClicked = (row, isLeftClick) -> {
                new ConfirmEmbargoDialog(faction, this, alreadyEmbargoed).show(0.3f, 0.3f);
            };

            tooltip.builder = (tp, exp) -> {
                if (alreadyEmbargoed) {
                    tp.addPara("Click to lift the embargo", pad);
                } else {
                    tp.addPara("Click to impose an embargo", pad);
                }
            };

            glow.type = GlowType.UNDERLAY;
            glow.color = base;

            bg.color = Color.RED;
            bg.alpha = 0.15f;

            buildUI();
        }

        public void buildUI() {
            clearChildren();
            final int iconSize = 28;

            bg.enabled = alreadyEmbargoed;

            final Base comIcon = new Base(m_panel, iconSize, iconSize, faction.getCrest(),
                null, null
            );
            add(comIcon).inBL(pad, (ROW_H - iconSize) / 2f);

            if (alreadyEmbargoed) {
                final Base restrictedIcon = new Base(m_panel, iconSize, iconSize, restrictedPath,
                    null, null
                );
                add(restrictedIcon).inBR(pad, (ROW_H - iconSize) / 2f);
            }

            final LabelAPI nameLabel = settings.createLabel(faction.getDisplayName(), Fonts.ORBITRON_12);
            nameLabel.setColor(faction.getBaseUIColor());
            if (alreadyEmbargoed) nameLabel.setOpacity(0.6f);
            final float labelW = nameLabel.computeTextHeight(faction.getDisplayName());
            add(nameLabel).inBL(iconSize + opad, (ROW_H - labelW) / 2f);
        }
    }
}
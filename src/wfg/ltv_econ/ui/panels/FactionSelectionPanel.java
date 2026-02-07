package wfg.ltv_econ.ui.panels;

import static wfg.native_ui.util.UIConstants.*;

import java.util.List;
import java.awt.Color;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.FactionSpecAPI;
import com.fs.starfarer.api.impl.campaign.ids.Factions;
import com.fs.starfarer.api.ui.Fonts;
import com.fs.starfarer.api.ui.LabelAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.ui.UIPanelAPI;

import wfg.ltv_econ.economy.engine.EconomyEngine;
import wfg.ltv_econ.ui.dialogs.ConfirmEmbargoDialog;
import wfg.native_ui.ui.ComponentFactory;
import wfg.native_ui.ui.components.AudioFeedbackComp;
import wfg.native_ui.ui.components.BackgroundComp;
import wfg.native_ui.ui.components.HoverGlowComp;
import wfg.native_ui.ui.components.InteractionComp;
import wfg.native_ui.ui.components.NativeComponents;
import wfg.native_ui.ui.components.OutlineComp;
import wfg.native_ui.ui.components.OutlineComp.OutlineType;
import wfg.native_ui.ui.core.UIElementFlags.HasAudioFeedback;
import wfg.native_ui.ui.core.UIElementFlags.HasBackground;
import wfg.native_ui.ui.core.UIElementFlags.HasHoverGlow;
import wfg.native_ui.ui.core.UIElementFlags.HasInteraction;
import wfg.native_ui.ui.core.UIElementFlags.HasOutline;
import wfg.native_ui.ui.core.UIElementFlags.HasTooltip;
import wfg.native_ui.ui.core.UIElementFlags.HasUIContext;
import wfg.native_ui.ui.components.TooltipComp;
import wfg.native_ui.ui.components.UIContextComp;
import wfg.native_ui.ui.components.HoverGlowComp.GlowType;
import wfg.native_ui.ui.panels.CustomPanel;
import wfg.native_ui.ui.panels.SpritePanel.Base;

public class FactionSelectionPanel extends CustomPanel<FactionSelectionPanel> implements
    HasOutline, HasBackground, HasUIContext
{
    public static final String restrictedPath = Global.getSettings().getSpriteName("ui", "restricted");
    private static final int ROW_H = 32;

    public final OutlineComp outline = comp().get(NativeComponents.OUTLINE);
    public final BackgroundComp bg = comp().get(NativeComponents.BACKGROUND);
    public final UIContextComp context = comp().get(NativeComponents.UI_CONTEXT);

    public FactionSelectionPanel(UIPanelAPI parent, int width, int height) {
        super(parent, width, height);

        outline.type = OutlineType.TEX_THIN;
        outline.color = dark;
        context.ignore = true;

        createPanel();
    }

    public void createPanel() {
        final int width = (int) pos.getWidth();
        final TooltipMakerAPI container = ComponentFactory.createTooltip(width, true);
        final List<FactionSpecAPI> factions = Global.getSettings().getAllFactionSpecs();
        factions.removeIf(f -> f.getId().equals(Factions.PLAYER));
        factions.removeIf(f -> !f.isShowInIntelTab());

        float yCoord = pad;
        for (FactionSpecAPI faction : factions) {
            final RowPanel row = new RowPanel(
                container, width - opad, ROW_H, faction
            );
            container.addCustom(row.getPanel(), 0).getPosition().inTL(0, yCoord);

            yCoord += ROW_H + pad;
        }
        container.setHeightSoFar(yCoord);
        ComponentFactory.addTooltip(container, getPos().getHeight(), true, m_panel).inTL(0f, 0f);
    }

    public class RowPanel extends CustomPanel<RowPanel> implements
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
            alreadyEmbargoed = EconomyEngine.getInstance().playerFactionSettings
                .embargoedFactions.contains(faction.getId());

            interaction.onClicked = (row, isLeftClick) -> {
                new ConfirmEmbargoDialog(faction, this, alreadyEmbargoed).show(0.3f, 0.3f);
            };

            tooltip.parent = FactionSelectionPanel.this.m_parent;
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

            createPanel();
        }

        public void createPanel() {
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

            final LabelAPI nameLabel = Global.getSettings().createLabel(faction.getDisplayName(), Fonts.ORBITRON_12);
            nameLabel.setColor(faction.getBaseUIColor());
            if (alreadyEmbargoed) nameLabel.setOpacity(0.6f);
            final float labelW = nameLabel.computeTextHeight(faction.getDisplayName());
            add(nameLabel).inBL(iconSize + opad, (ROW_H - labelW) / 2f);
        }
    }
}
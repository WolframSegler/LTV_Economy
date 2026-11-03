package wfg.ltv_econ.ui.economyTab;

import static wfg.ltv_econ.constant.EconomyConstants.visibleFactions;
import static wfg.ltv_econ.constant.strings.LocalizedStrings.*;
import static wfg.native_ui.util.Globals.settings;
import static wfg.native_ui.util.UIConstants.*;

import java.awt.Color;
import java.util.List;

import com.fs.starfarer.api.campaign.FactionSpecAPI;
import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.ui.Fonts;
import com.fs.starfarer.api.ui.LabelAPI;

import wfg.ltv_econ.serializable.LtvEconSaveData;
import wfg.native_ui.internal.ui.core.UIContainer;
import wfg.native_ui.ui.component.AudioFeedbackComp;
import wfg.native_ui.ui.component.BackgroundComp;
import wfg.native_ui.ui.component.HoverGlowComp;
import wfg.native_ui.ui.component.InteractionComp;
import wfg.native_ui.ui.component.NativeComponents;
import wfg.native_ui.ui.component.OutlineComp;
import wfg.native_ui.ui.component.OutlineComp.OutlineType;
import wfg.native_ui.ui.core.UIElementFlags.HasAudioFeedback;
import wfg.native_ui.ui.core.UIElementFlags.HasBackground;
import wfg.native_ui.ui.core.UIElementFlags.HasHoverGlow;
import wfg.native_ui.ui.core.UIElementFlags.HasInteraction;
import wfg.native_ui.ui.core.UIElementFlags.HasOutline;
import wfg.native_ui.ui.core.UIElementFlags.HasTooltip;
import wfg.native_ui.ui.table.ListTable;
import wfg.native_ui.ui.table.WidgetAPI;
import wfg.native_ui.ui.component.TooltipComp;
import wfg.native_ui.ui.component.HoverGlowComp.GlowType;
import wfg.native_ui.ui.visual.AbstractSpriteElement.SpriteElement;

public final class FactionSelectionPanel extends
    ListTable<FactionSpecAPI, FactionSelectionPanel.RowPanel>
    implements HasOutline, HasBackground
{
    public static final SpriteAPI restrictedPath = settings.getSprite("ui", "restricted");
    private static final int ROW_H = 32;

    public final OutlineComp outline = comp().get(NativeComponents.OUTLINE);
    public final BackgroundComp bg = comp().get(NativeComponents.BACKGROUND);

    public FactionSelectionPanel(int width, int height) {
        super(width, height, ROW_H, pad);

        outline.type = OutlineType.TEX_THIN;
        outline.color = dark;
        mAddRowGap = true;

        buildUI();
    }

    protected List<FactionSpecAPI> getDataList() {
        return visibleFactions;
    }

    protected RowPanel createWidget(FactionSpecAPI faction, int index) {
        return new RowPanel(faction);
    }

    protected void onWidgetClicked(RowPanel source) {}

    protected String getEmptyMessage() {
        return "";
    }

    public class RowPanel extends UIContainer implements WidgetAPI<RowPanel>,
        HasInteraction, HasHoverGlow, HasAudioFeedback, HasBackground, HasTooltip
    {
        public final TooltipComp tooltip = comp().get(NativeComponents.TOOLTIP);
        public final BackgroundComp bg = comp().get(NativeComponents.BACKGROUND);
        public final HoverGlowComp glow = comp().get(NativeComponents.HOVER_GLOW);
        public final AudioFeedbackComp audio = comp().get(NativeComponents.AUDIO_FEEDBACK);
        public final InteractionComp<RowPanel> interaction = comp().get(NativeComponents.INTERACTION);

        private final FactionSpecAPI faction;
        public boolean alreadyEmbargoed;

        public RowPanel(FactionSpecAPI faction) {
            this.faction = faction;
            alreadyEmbargoed = LtvEconSaveData.instance().playerFactionSettings
                .embargoedFactions.contains(faction.getId());

            interaction.onClicked = (row, isLeftClick) -> {
                new ConfirmEmbargoDialog(faction, this, alreadyEmbargoed).show(0.3f, 0.3f);
            };

            tooltip.builder = (tp, exp) -> {
                if (alreadyEmbargoed) {
                    tp.addPara(str("uiBtnTpTxtEmbargo1"), pad);
                } else {
                    tp.addPara(str("uiBtnTpTxtEmbargo2"), pad);
                }
            };

            glow.type = GlowType.UNDERLAY;
            glow.color = base;

            bg.color = Color.RED;
            bg.alpha = 0.15f;
        }

        @Override
        public InteractionComp<RowPanel> getInteraction() {
            return interaction;
        }

        @Override
        public void buildUI() {
            clearChildren();
            final int iconSize = 28;

            bg.enabled = alreadyEmbargoed;

            final SpriteElement comIcon = new SpriteElement(
                iconSize, iconSize, faction.getCrest(), null, null
            );
            add(comIcon).inBL(pad, (getHeight() - iconSize) / 2f);

            if (alreadyEmbargoed) {
                final SpriteElement restrictedIcon = new SpriteElement(
                    iconSize, iconSize, restrictedPath, null, null
                );
                add(restrictedIcon).inBR(pad, (getHeight() - iconSize) / 2f);
            }

            final LabelAPI nameLabel = settings.createLabel(
                faction.getDisplayName(), Fonts.ORBITRON_12);
            nameLabel.setColor(faction.getBaseUIColor());
            if (alreadyEmbargoed) nameLabel.setOpacity(0.6f);
            add(nameLabel).inLMid(iconSize + opad);
        }
    }
}
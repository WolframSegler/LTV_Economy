package wfg.ltv_econ.ui.panels;

import static wfg.wrap_ui.util.UIConstants.*;

import java.util.List;
import java.util.Optional;
import java.awt.Color;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.FactionSpecAPI;
import com.fs.starfarer.api.impl.campaign.ids.Factions;
import com.fs.starfarer.api.ui.CustomPanelAPI;
import com.fs.starfarer.api.ui.Fonts;
import com.fs.starfarer.api.ui.LabelAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.ui.UIPanelAPI;
import com.fs.starfarer.api.util.FaderUtil;

import wfg.ltv_econ.economy.EconomyEngine;
import wfg.ltv_econ.ui.dialogs.ConfirmEmbargoDialog;
import wfg.wrap_ui.ui.panels.CustomPanel;
import wfg.wrap_ui.ui.panels.CustomPanel.HasBackground;
import wfg.wrap_ui.ui.panels.CustomPanel.HasOutline;
import wfg.wrap_ui.ui.panels.SpritePanel.Base;
import wfg.wrap_ui.ui.plugins.BasePanelPlugin;
import wfg.wrap_ui.ui.systems.FaderSystem.Glow;
import wfg.wrap_ui.ui.systems.OutlineSystem.Outline;
import wfg.wrap_ui.util.WrapUiUtils;
import wfg.wrap_ui.util.WrapUiUtils.AnchorType;

public class FactionSelectionPanel extends
    CustomPanel<BasePanelPlugin<FactionSelectionPanel>, FactionSelectionPanel, CustomPanelAPI>
    implements HasOutline, HasBackground
{
    public static final String restrictedPath = Global.getSettings().getSpriteName("ui", "restricted");
    private static final int ROW_H = 32;

    public FactionSelectionPanel(UIPanelAPI parent, int width, int height) {
        super(parent, width, height, new BasePanelPlugin<>());

        getPlugin().init(this);
        createPanel();
    }

    public void createPanel() {
        final int width = (int) getPos().getWidth();
        final TooltipMakerAPI container = getPanel().createUIElement(
            width, getPos().getHeight(), true
        );
        final List<FactionSpecAPI> factions = Global.getSettings().getAllFactionSpecs();
        factions.removeIf(f -> f.getId().equals(Factions.PLAYER));
        factions.removeIf(f -> !f.isShowInIntelTab());

        float yCoord = pad;
        for (FactionSpecAPI faction : factions) {
            final RowPanel row = new RowPanel(
                container, width - opad, ROW_H, faction, m_panel
            );
            container.addCustom(row.getPanel(), 0).getPosition().inTL(pad, yCoord);

            yCoord += ROW_H + pad;
        }
        container.setHeightSoFar(yCoord);
        add(container).inTL(-pad, 0);
    }

    public Outline getOutline() {
        return Outline.TEX_THIN;
    }

    public Color getOutlineColor() {
        return dark;
    }

    public static class RowPanel extends CustomPanel<BasePanelPlugin<RowPanel>, RowPanel, CustomPanelAPI> 
        implements HasActionListener, AcceptsActionListener, HasFader, HasAudioFeedback, HasBackground,
        HasTooltip
    {
        private final FaderUtil fader = new FaderUtil(0, 0, 0.2f, true, true);
        private final FactionSpecAPI faction;
        private final CustomPanelAPI tpPanel;
        public boolean alreadyEmbargoed;

        public RowPanel(UIPanelAPI parent, int width, int height, FactionSpecAPI faction,
            CustomPanelAPI tpPanel
        ) {
            super(parent, width, height, new BasePanelPlugin<>());

            this.faction = faction;
            this.tpPanel = tpPanel;
            alreadyEmbargoed = EconomyEngine.getInstance().playerFactionSettings
                .embargoedFactions.contains(faction.getId());

            getPlugin().init(this);
            createPanel();
        }

        public void createPanel() {
            clearChildren();
            final int iconSize = 28;

            final Base comIcon = new Base(
                getPanel(), iconSize, iconSize, faction.getCrest(),
                null, null, false
            );
            add(comIcon).inBL(pad, (ROW_H - iconSize) / 2f);

            if (alreadyEmbargoed) {
                final Base restrictedIcon = new Base(
                    getPanel(), iconSize, iconSize, restrictedPath,
                    null, null, false
                );
                add(restrictedIcon).inBR(pad, (ROW_H - iconSize) / 2f);
            }

            final LabelAPI nameLabel = Global.getSettings().createLabel(faction.getDisplayName(), Fonts.ORBITRON_12);
            nameLabel.setColor(faction.getBaseUIColor());
            if (alreadyEmbargoed) nameLabel.setOpacity(0.6f);
            final float labelW = nameLabel.computeTextHeight(faction.getDisplayName());
            add(nameLabel).inBL(iconSize + opad, (ROW_H - labelW) / 2f);
        }

        public void onClicked(CustomPanel<?, ?, ?> source, boolean isLeftClick) {
            final ConfirmEmbargoDialog dialog = new ConfirmEmbargoDialog(faction, this, alreadyEmbargoed);
            dialog.show(0.3f, 0.3f);
        }

        public CustomPanelAPI getTpParent() { return tpPanel;}

        public TooltipMakerAPI createAndAttachTp() {
            final TooltipMakerAPI tp = tpPanel.createUIElement(400, 0, false);

            if (alreadyEmbargoed) {
                tp.addPara("Click to lift the embargo", pad);
            } else {
                tp.addPara("Click to impose an embargo", pad);
            }

            tpPanel.addUIElement(tp);
            WrapUiUtils.anchorPanel(tp, m_panel, AnchorType.RightTop, opad);
            return tp;
        }

        public Optional<HasActionListener> getActionListener() { return Optional.of(this);}
        public FaderUtil getFader() { return fader;}
        public Glow getGlowType() { return Glow.UNDERLAY;}

        public Color getBgColor() { return Color.RED;}
        public boolean isBgEnabled() { return alreadyEmbargoed;}
        public float getBgAlpha() { return 0.15f;}
    }
}
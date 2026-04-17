package wfg.ltv_econ.ui.factionTab;

import static wfg.native_ui.util.Globals.settings;
import static wfg.native_ui.util.UIConstants.*;

import java.util.function.Consumer;

import com.fs.starfarer.api.campaign.FactionSpecAPI;
import com.fs.starfarer.api.ui.Fonts;
import com.fs.starfarer.api.ui.LabelAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.ui.UIPanelAPI;

import wfg.ltv_econ.constants.EconomyConstants;
import wfg.ltv_econ.economy.engine.EconomyEngine;
import wfg.native_ui.internal.ui.Side;
import wfg.native_ui.ui.ComponentFactory;
import wfg.native_ui.ui.component.HoverGlowComp;
import wfg.native_ui.ui.component.HoverGlowComp.GlowType;
import wfg.native_ui.ui.component.InteractionComp;
import wfg.native_ui.ui.component.NativeComponents;
import wfg.native_ui.ui.container.DockPanel;
import wfg.native_ui.ui.core.UIBuildableAPI;
import wfg.native_ui.ui.core.UIElementFlags.HasAudioFeedback;
import wfg.native_ui.ui.core.UIElementFlags.HasHoverGlow;
import wfg.native_ui.ui.core.UIElementFlags.HasInteraction;
import wfg.native_ui.ui.panel.CustomPanel;
import wfg.native_ui.ui.visual.SpritePanel.Base;

public class FactionSelectionDialog extends DockPanel {
    private static final int ROW_H = 32;
    
    private final ShipInventoryPanel content;

    public FactionSelectionDialog(ShipInventoryPanel content) {
        super(300, 400, Side.BOTTOM);

        this.content = content;

        buildUI();
    }

    @Override
    public void buildUI() {
        clearChildren();

        final int width = (int) pos.getWidth();
        final TooltipMakerAPI container = ComponentFactory.createTooltip(width, true);

        float yCoord = opad + 1;
        for (FactionSpecAPI faction : EconomyConstants.visibleFactions) {
            final DebugFactionRow row = new DebugFactionRow(
                container, width - hpad * 2, ROW_H, faction, this::onFactionSelected
            );
            container.addCustom(row.getPanel(), 0f).getPosition().inTL(hpad, yCoord);
            yCoord += ROW_H + pad;
        }
        container.setHeightSoFar(yCoord);
        ComponentFactory.addTooltip(container, getPos().getHeight(), true, m_panel).inTL(0f, 0f);
    }

    private void onFactionSelected(FactionSpecAPI faction) {
        content.inv = EconomyEngine.instance().getFactionShipInventory(faction.getId());
        content.buildUI();
    }

    private static class DebugFactionRow extends CustomPanel implements UIBuildableAPI,
        HasInteraction, HasHoverGlow, HasAudioFeedback
    {
        public final InteractionComp<DebugFactionRow> interaction = comp().get(NativeComponents.INTERACTION);
        public final HoverGlowComp glow = comp().get(NativeComponents.HOVER_GLOW);

        private final FactionSpecAPI faction;

        public DebugFactionRow(UIPanelAPI parent, int width, int height, FactionSpecAPI faction,
            Consumer<FactionSpecAPI> onSelect
        ) {
            super(parent, width, height);
            this.faction = faction;

            interaction.onClicked = (row, isLeftClick) -> onSelect.accept(faction);

            glow.type = GlowType.UNDERLAY;
            glow.color = base;

            buildUI();
        }

        @Override
        public void buildUI() {
            clearChildren();
            final int iconSize = 28;

            final Base crestIcon = new Base(m_panel, iconSize, iconSize, faction.getCrest(), null, null);
            add(crestIcon).inBL(pad, (ROW_H - iconSize) / 2f);

            final LabelAPI nameLabel = settings.createLabel(faction.getDisplayName(), Fonts.ORBITRON_12);
            nameLabel.setColor(faction.getBaseUIColor());
            add(nameLabel).inLMid(iconSize + opad);
        }
    }
}
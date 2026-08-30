package wfg.ltv_econ.ui.factionTab.dialog;

import static wfg.native_ui.util.Globals.settings;
import static wfg.native_ui.util.UIConstants.*;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import com.fs.starfarer.api.campaign.FactionSpecAPI;
import com.fs.starfarer.api.impl.campaign.ids.Factions;
import com.fs.starfarer.api.ui.Fonts;
import com.fs.starfarer.api.ui.LabelAPI;

import wfg.ltv_econ.constant.EconomyConstants;
import wfg.ltv_econ.economy.engine.EconomyEngine;
import wfg.ltv_econ.serializable.StaticData;
import wfg.native_ui.internal.ui.Side;
import wfg.native_ui.internal.ui.core.UIContainer;
import wfg.native_ui.ui.ComponentFactory;
import wfg.native_ui.ui.component.HoverGlowComp;
import wfg.native_ui.ui.component.HoverGlowComp.GlowType;
import wfg.native_ui.ui.component.InteractionComp;
import wfg.native_ui.ui.component.NativeComponents;
import wfg.native_ui.ui.container.DockPanel;
import wfg.native_ui.ui.core.UIBuildableAPI;
import wfg.native_ui.ui.core.UIContainerAPI;
import wfg.native_ui.ui.core.UIElementFlags.HasAudioFeedback;
import wfg.native_ui.ui.core.UIElementFlags.HasHoverGlow;
import wfg.native_ui.ui.core.UIElementFlags.HasInteraction;
import wfg.native_ui.ui.visual.AbstractSpriteElement.SpriteElement;

public final class FactionSelectionDialog extends DockPanel {
    private static final int ROW_H = 32;
    
    private final UIBuildableAPI content;

    public FactionSelectionDialog(UIBuildableAPI content) {
        super(300, 400, Side.BOTTOM);

        this.content = content;

        buildUI();
    }

    @Override
    public void buildUI() {
        clearChildren();

        final int width = (int) contentContainer.getWidth();
        final UIContainerAPI content = new UIContainer(width, 0f);
        final List<FactionSpecAPI> factions = new ArrayList<>(EconomyConstants.visibleFactions);
        factions.add(settings.getFactionSpec(Factions.PLAYER));

        float yCoord = 0f;
        for (FactionSpecAPI faction : factions) {
            final DebugFactionRow row = new DebugFactionRow(
                width, ROW_H, faction, this::onFactionSelected
            );
            content.add(row).inTL(0f, yCoord);
            yCoord += ROW_H + pad;
        }
        content.setHeight(yCoord);
        add(ComponentFactory.wrapWithScrollPanel(content, width, contentContainer.getHeight())).inTL(0f, 0f);
    }

    private void onFactionSelected(FactionSpecAPI faction) {
        StaticData.inv = EconomyEngine.instance().getFactionShipInventory(faction.getId());
        content.buildUI();
    }

    private static class DebugFactionRow extends UIContainer implements UIBuildableAPI,
        HasInteraction, HasHoverGlow, HasAudioFeedback
    {
        public final InteractionComp<DebugFactionRow> interaction = comp().get(NativeComponents.INTERACTION);
        public final HoverGlowComp glow = comp().get(NativeComponents.HOVER_GLOW);

        private final FactionSpecAPI faction;

        public DebugFactionRow(int width, int height, FactionSpecAPI faction,
            Consumer<FactionSpecAPI> onSelect
        ) {
            super(width, height);
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

            final SpriteElement crestIcon = new SpriteElement(iconSize, iconSize, faction.getCrest(), null, null);
            add(crestIcon).inBL(pad, (ROW_H - iconSize) / 2f);

            final LabelAPI nameLabel = settings.createLabel(faction.getDisplayName(), Fonts.ORBITRON_12);
            nameLabel.setColor(faction.getBaseUIColor());
            add(nameLabel).inLMid(iconSize + opad);
        }
    }
}
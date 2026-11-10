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
import wfg.native_ui.ui.component.GlowComp;
import wfg.native_ui.ui.component.GlowComp.GlowType;
import wfg.native_ui.ui.component.InteractionComp;
import wfg.native_ui.ui.component.NativeComponents;
import wfg.native_ui.ui.container.DockPanel;
import wfg.native_ui.ui.core.UIBuildableAPI;
import wfg.native_ui.ui.core.UIElementFlags.HasAudioFeedback;
import wfg.native_ui.ui.core.UIElementFlags.HasGlow;
import wfg.native_ui.ui.core.UIElementFlags.HasInteraction;
import wfg.native_ui.ui.table.ListTable;
import wfg.native_ui.ui.table.WidgetAPI;
import wfg.native_ui.ui.visual.AbstractSpriteElement.SpriteElement;

public final class FactionSelectionDialog extends DockPanel {
    private static final int ROW_H = 32;

    private final UIBuildableAPI mContent;

    public FactionSelectionDialog(UIBuildableAPI content) {
        super(300, 400, Side.BOTTOM);
        mContent = content;
        buildUI();
    }

    @Override
    public void buildUI() {
        clearChildren();
        add(new FactionList()).inTL(0f, 0f);
    }

    private void onFactionSelected(FactionSpecAPI faction) {
        StaticData.inv = EconomyEngine.instance().getFactionShipInventory(faction.getId());
        mContent.buildUI();
    }

    private final class FactionList extends ListTable<FactionSpecAPI, DebugFactionRow> {
        FactionList() {
            super(mContentContainer.getWidth(), mContentContainer.getHeight(), ROW_H, 0);
            mAddRowGap = false;
            buildUI();
        }

        protected List<FactionSpecAPI> getDataList() {
            final List<FactionSpecAPI> factions = new ArrayList<>(EconomyConstants.visibleFactions);
            factions.add(settings.getFactionSpec(Factions.PLAYER));
            return factions;
        }

        protected DebugFactionRow createWidget(FactionSpecAPI faction, int index) {
            return new DebugFactionRow(faction, f -> onFactionSelected(f));
        }

        protected void onWidgetClicked(DebugFactionRow source) {}

        protected String getEmptyMessage() {
            return "";
        }
    }

    private static final class DebugFactionRow extends UIContainer
        implements WidgetAPI<DebugFactionRow>, HasInteraction, HasGlow, HasAudioFeedback
    {
        public final InteractionComp<DebugFactionRow> interaction = comp().get(NativeComponents.INTERACTION);
        public final GlowComp glow = comp().get(NativeComponents.GLOW);

        private final FactionSpecAPI faction;

        DebugFactionRow(FactionSpecAPI faction, Consumer<FactionSpecAPI> onSelect) {
            this.faction = faction;

            interaction.onClicked = (row, isLeftClick) -> onSelect.accept(faction);

            glow.type = GlowType.UNDERLAY;
            glow.color = base;
        }

        public InteractionComp<DebugFactionRow> getInteraction() {
            return interaction;
        }

        @Override
        public void buildUI() {
            clearChildren();
            final int iconSize = 28;

            final SpriteElement crestIcon = new SpriteElement(iconSize, iconSize, faction.getCrest(), null, null);
            add(crestIcon).inBL(pad, (getHeight() - iconSize) / 2f);

            final LabelAPI nameLabel = settings.createLabel(faction.getDisplayName(), Fonts.ORBITRON_12);
            nameLabel.setColor(faction.getBaseUIColor());
            add(nameLabel).inLMid(iconSize + opad);
        }
    }
}
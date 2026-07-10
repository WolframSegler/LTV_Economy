package wfg.ltv_econ.ui.factionTab.dialog;

import static wfg.ltv_econ.constant.Sprites.STOPWATCH;
import static wfg.ltv_econ.constant.Sprites.WAGES;
import static wfg.ltv_econ.constant.strings.LocalizedStrings.*;
import static wfg.native_ui.util.Globals.settings;
import static wfg.native_ui.util.UIConstants.*;

import java.util.List;

import org.lwjgl.input.Keyboard;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.campaign.econ.CommoditySpecAPI;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.impl.campaign.ids.Strings;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.ui.Fonts;
import com.fs.starfarer.api.ui.LabelAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.ui.UIPanelAPI;

import wfg.ltv_econ.economy.fleet.FactionShipInventory;
import wfg.ltv_econ.economy.fleet.PlannedOrder;
import wfg.ltv_econ.economy.fleet.ShipProductionManager;
import wfg.ltv_econ.serializable.StaticData;
import wfg.ltv_econ.ui.factionTab.PlannedOrdersPanel;
import wfg.native_ui.internal.ui.Side;
import wfg.native_ui.ui.ComponentFactory;
import wfg.native_ui.ui.component.HoverGlowComp;
import wfg.native_ui.ui.component.HoverGlowComp.GlowType;
import wfg.native_ui.ui.component.InteractionComp;
import wfg.native_ui.ui.component.NativeComponents;
import wfg.native_ui.ui.component.TooltipComp;
import wfg.native_ui.ui.container.DockPanel;
import wfg.native_ui.ui.core.UIBuildableAPI;
import wfg.native_ui.ui.core.UIElementFlags.HasAudioFeedback;
import wfg.native_ui.ui.core.UIElementFlags.HasHoverGlow;
import wfg.native_ui.ui.core.UIElementFlags.HasInteraction;
import wfg.native_ui.ui.core.UIElementFlags.HasTooltip;
import wfg.native_ui.ui.panel.CustomPanel;
import wfg.native_ui.ui.visual.IconValuePair;
import wfg.native_ui.ui.visual.SpritePanel.Base;
import wfg.native_ui.util.NativeUiUtils;
import wfg.native_ui.util.NumFormat;

public class ShipCommissionDialog extends DockPanel {
    private static final int ROW_H = 36;

    private final PlannedOrdersPanel content;

    public ShipCommissionDialog(PlannedOrdersPanel content) {
        super(450, screenH - 200, Side.LEFT, 8);
        this.content = content;
        buildUI();
    }

    @Override
    public void buildUI() {
        clearChildren();

        final FactionShipInventory inv = StaticData.inv;

        final FactionAPI faction = Global.getSector().getFaction(inv.factionID);

        final int width = (int) contentContainer.getPosition().getWidth();
        final TooltipMakerAPI container = ComponentFactory.createTooltip(width, true);

        float yCoord = 0f;
        for (String hullId : faction.getKnownShips()) {
            final HullRow row = new HullRow(container, width, ROW_H, settings.getHullSpec(hullId));
            container.addCustom(row.getPanel(), 0f).getPosition().inTL(0f, yCoord);
            yCoord += ROW_H + pad;
        }
        container.setHeightSoFar(yCoord);
        ComponentFactory.addTooltip(container, contentContainer.getPosition().getHeight() - hpad*4, true, contentContainer).inTL(0f, hpad);
    }

    private final void addOrder(String hullId, int count) {
        final PlannedOrder cost = ShipProductionManager.getProductionCost(settings.getHullSpec(hullId));
        for (int i = 0; i < count; i++) {
            StaticData.inv.addPlannedOrder(new PlannedOrder(hullId, cost.credits, cost.commodities, cost.days));
        }

        content.buildUI();
        content.grid.scrollToBottom();
    }

    private class HullRow extends CustomPanel implements UIBuildableAPI,
        HasInteraction, HasHoverGlow, HasTooltip, HasAudioFeedback
    {
        public final InteractionComp<HullRow> interaction = comp().get(NativeComponents.INTERACTION);
        public final HoverGlowComp glow = comp().get(NativeComponents.HOVER_GLOW);
        public final TooltipComp tooltip = comp().get(NativeComponents.TOOLTIP);

        private final ShipHullSpecAPI spec;
        private final PlannedOrder order;

        public HullRow(UIPanelAPI parent, int width, int height, ShipHullSpecAPI spec) {
            super(parent, width, height);
            this.spec = spec;
            this.order =  ShipProductionManager.getProductionCost(spec);

            interaction.onClicked = (row, isLeftClick) -> {
                int count = 1;
                if (NativeUiUtils.isCtrlDown()) count = 5;
                if (NativeUiUtils.isShiftDown()) count = 10;
                if (NativeUiUtils.isShiftDown() && NativeUiUtils.isCtrlDown()) count = 50;
                ShipCommissionDialog.this.addOrder(order.hullId, count);
            };

            glow.type = GlowType.UNDERLAY;
            glow.color = base;

            tooltip.builder = (tp, expanded) -> {
                tp.addTitle(spec.getHullNameWithDashClass(), base);
                tp.addPara(str("uiHullsCostBuildTimeTxt"), pad, highlight,
                    NumFormat.formatCreditAbs(order.credits), String.valueOf(order.days)
                );

                int row = 0;
                tp.addPara(str("uiRequiredResourcesTxt"), base, opad);
                tp.beginGridFlipped(300, 2, 50, hpad);
                for (var e : order.commodities.singleEntrySet()) {
                    final CommoditySpecAPI com = settings.getCommoditySpec(e.getKey());
                    final String amount = NumFormat.engNotate(e.getValue());
                    tp.addToGrid(0, row++, com.getName(), amount);
                }
                tp.addGrid(0);

                tp.addPara(str("uiHullsCommissionShortcutsTxt"),
                    opad*2, highlight, str("uiCtrlTxt"), str("uiShift"), str("uiClickTxt")
                );
            };

            buildUI();
        }

        @Override
        public void buildUI() {
            clearChildren();
            final int nameW = 150;
            final int iconSize = 28;
            final int pairW = 90;

            final SpriteAPI sprite = settings.getSprite(spec.getSpriteName());
            final float scale = Math.min(iconSize / sprite.getWidth(), iconSize / sprite.getHeight());
            final int scaledW = (int) (sprite.getWidth() * scale);
            final int scaledH = (int) (sprite.getHeight() * scale);
            final Base shipIcon = new Base(m_panel, scaledW, scaledH, sprite, null, null);
            add(shipIcon).inLMid(pad + (iconSize - scaledW) / 2);

            final String name = spec.getHullNameWithDashClass();
            final LabelAPI nameLabel = settings.createLabel(name, Fonts.DEFAULT_SMALL);
            nameLabel.setColor(base);
            add(nameLabel).inLMid(iconSize + opad);

            final IconValuePair costPair = new IconValuePair(m_panel, pairW, iconSize, WAGES, order.credits, false, null);
            final IconValuePair timePair = new IconValuePair(m_panel, pairW, iconSize, STOPWATCH, order.days, false, null);
            add(costPair).inLMid(iconSize + opad + nameW);
            add(timePair).inLMid(iconSize + opad + nameW + pairW);

            costPair.label().setText(costPair.label().getText() + Strings.C);
        }

        @Override
        public void processInput(List<InputEventAPI> events) {
            super.processInput(events);
            if (!NativeUiUtils.containsMouse(pos)) return;
            
            for (InputEventAPI e : events) {
                if (!e.isConsumed() && e.isKeyboardEvent() && e.getEventValue() == Keyboard.KEY_F2) {
                    settings.showCodex(spec.getCodexVariantId());
                }
            }
        }
    }
}
package wfg.ltv_econ.ui.economyTab;

import org.lwjgl.input.Keyboard;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.SettingsAPI;
import com.fs.starfarer.api.campaign.econ.CommoditySpecAPI;
import com.fs.starfarer.api.impl.campaign.ids.Commodities;
import com.fs.starfarer.api.impl.campaign.ids.Factions;
import com.fs.starfarer.api.ui.Fonts;
import com.fs.starfarer.api.ui.LabelAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.ui.UIPanelAPI;

import wfg.ltv_econ.constants.EconomyConstants;
import wfg.ltv_econ.ui.marketInfo.dialogs.ComDetailDialog;
import wfg.ltv_econ.util.UIUtils;
import wfg.native_ui.ui.ComponentFactory;
import wfg.native_ui.ui.component.AudioFeedbackComp;
import wfg.native_ui.ui.component.BackgroundComp;
import wfg.native_ui.ui.component.HoverGlowComp;
import wfg.native_ui.ui.component.InteractionComp;
import wfg.native_ui.ui.component.NativeComponents;
import wfg.native_ui.ui.component.OutlineComp;
import wfg.native_ui.ui.component.TooltipComp;
import wfg.native_ui.ui.component.HoverGlowComp.GlowType;
import wfg.native_ui.ui.component.OutlineComp.OutlineType;
import wfg.native_ui.ui.core.UIBuildableAPI;
import wfg.native_ui.ui.core.UIElementFlags.HasAudioFeedback;
import wfg.native_ui.ui.core.UIElementFlags.HasBackground;
import wfg.native_ui.ui.core.UIElementFlags.HasHoverGlow;
import wfg.native_ui.ui.core.UIElementFlags.HasInteraction;
import wfg.native_ui.ui.core.UIElementFlags.HasOutline;
import wfg.native_ui.ui.core.UIElementFlags.HasTooltip;
import wfg.native_ui.ui.panel.CustomPanel;
import wfg.native_ui.ui.visual.SpritePanel.Base;
import static wfg.native_ui.util.UIConstants.*;

public class CommoditySelectionPanel extends CustomPanel<CommoditySelectionPanel> implements
    HasOutline, HasBackground, UIBuildableAPI
{
    private static final SettingsAPI settings = Global.getSettings();
    private static final int ROW_H = 32;
    private static UIBuildableAPI targetPanel = null;

    public static CommoditySpecAPI selectedCom = settings.getCommoditySpec(Commodities.SUPPLIES);

    public final OutlineComp outline = comp().get(NativeComponents.OUTLINE);
    public final BackgroundComp bg = comp().get(NativeComponents.BACKGROUND);

    public CommoditySelectionPanel(UIPanelAPI parent, int width, int height, UIBuildableAPI content) {
        super(parent, width, height);

        targetPanel = content;

        outline.type = OutlineType.TEX_THIN;
        outline.color = dark;

        buildUI();
    }

    public void buildUI() {
        final int width = (int) pos.getWidth();
        final TooltipMakerAPI container = ComponentFactory.createTooltip(width, true);

        float yCoord = pad;
        for (CommoditySpecAPI spec : EconomyConstants.econCommoditySpecs) {
            final RowPanel row = new RowPanel(
                container, width - pad*2, ROW_H, spec
            );
            container.addCustom(row.getPanel(), 0).getPosition().inTL(pad, yCoord);

            yCoord += ROW_H + pad;
        }
        container.setHeightSoFar(yCoord);
        ComponentFactory.addTooltip(container, pos.getHeight(), true, m_panel).inTL(0f, 0f);
    }

    public class RowPanel extends CustomPanel<RowPanel> 
        implements HasInteraction, HasHoverGlow, HasAudioFeedback, HasTooltip
    {
        public final HoverGlowComp glow = comp().get(NativeComponents.HOVER_GLOW);
        public final AudioFeedbackComp audio = comp().get(NativeComponents.AUDIO_FEEDBACK);
        public final InteractionComp<RowPanel> interaction = comp().get(NativeComponents.INTERACTION);
        public final TooltipComp tooltip = comp().get(NativeComponents.TOOLTIP);

        private final CommoditySpecAPI spec;

        public RowPanel(UIPanelAPI parent, int width, int height, CommoditySpecAPI com) {
            super(parent, width, height);

            spec = com;

            glow.type = GlowType.UNDERLAY;

            interaction.onClicked = (source, isLeftClick) -> {
                selectedCom = spec;
                targetPanel.buildUI();

                if (UIUtils.canViewPrices() && (Keyboard.isKeyDown(Keyboard.KEY_LCONTROL) ||
                    Keyboard.isKeyDown(Keyboard.KEY_RCONTROL)
                )) {
                    final ComDetailDialog dialogPanel = new ComDetailDialog(
                        null, settings.getFactionSpec(Factions.PLAYER), com
                    );
                    dialogPanel.show(0.3f, 0.3f);
                }
            };

            tooltip.builder = (tp, exp) -> {
                if (UIUtils.canViewPrices()) {
                    tp.addPara("Ctrl + Click to view global market info", pad, highlight, new String[]{
                        "Ctrl", "Click"
                    });
                } else {
                    final String text = "Must be in range of a comm relay to view global market info";
                    tp.addPara(text, pad, negative, text);
                }
            };

            buildUI();
        }

        public void buildUI() {
            final int iconSize = 28;

            final Base comIcon = new Base(
                m_panel, iconSize, iconSize, spec.getIconName(),
                null, null
            );
            RowPanel.this.add(comIcon).inBL(pad, (ROW_H - iconSize) / 2f);

            final LabelAPI comNameLabel = settings.createLabel(spec.getName(), Fonts.ORBITRON_12);
            comNameLabel.setColor(base);
            final float labelW = comNameLabel.computeTextHeight(spec.getName());
            RowPanel.this.add(comNameLabel).inBL(iconSize + opad, (ROW_H - labelW) / 2f);
        }
    }
}
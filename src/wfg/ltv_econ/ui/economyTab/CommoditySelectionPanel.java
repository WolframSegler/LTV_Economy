package wfg.ltv_econ.ui.economyTab;

import static wfg.ltv_econ.constant.strings.LocalizedStrings.*;
import static wfg.native_ui.util.Globals.settings;

import com.fs.starfarer.api.campaign.econ.CommoditySpecAPI;
import com.fs.starfarer.api.impl.campaign.ids.Commodities;
import com.fs.starfarer.api.impl.campaign.ids.Factions;
import com.fs.starfarer.api.ui.Fonts;
import com.fs.starfarer.api.ui.LabelAPI;

import wfg.ltv_econ.constant.EconomyConstants;
import wfg.ltv_econ.ui.marketInfo.dialogs.ComDetailDialog;
import wfg.ltv_econ.util.UIUtils;
import wfg.native_ui.internal.ui.core.UIContainer;
import wfg.native_ui.ui.component.AudioFeedbackComp;
import wfg.native_ui.ui.component.BackgroundComp;
import wfg.native_ui.ui.component.GlowComp;
import wfg.native_ui.ui.component.InteractionComp;
import wfg.native_ui.ui.component.NativeComponents;
import wfg.native_ui.ui.component.OutlineComp;
import wfg.native_ui.ui.component.TooltipComp;
import wfg.native_ui.ui.component.GlowComp.GlowType;
import wfg.native_ui.ui.core.UIBuildableAPI;
import wfg.native_ui.ui.core.UIElementFlags.HasAudioFeedback;
import wfg.native_ui.ui.core.UIElementFlags.HasBackground;
import wfg.native_ui.ui.core.UIElementFlags.HasGlow;
import wfg.native_ui.ui.core.UIElementFlags.HasInteraction;
import wfg.native_ui.ui.core.UIElementFlags.HasOutline;
import wfg.native_ui.ui.core.UIElementFlags.HasTooltip;
import wfg.native_ui.ui.table.ListTable;
import wfg.native_ui.ui.table.WidgetAPI;
import wfg.native_ui.ui.visual.AbstractSpriteElement.SpriteElement;
import wfg.native_ui.util.NativeUiUtils;

import static wfg.native_ui.util.UIConstants.*;

import java.util.List;

public final class CommoditySelectionPanel
    extends ListTable<CommoditySpecAPI, CommoditySelectionPanel.RowPanel>
    implements HasOutline, HasBackground
{
    private static final int ROW_H = 32;
    private static UIBuildableAPI targetPanel = null;

    public static CommoditySpecAPI selectedCom = settings.getCommoditySpec(Commodities.SUPPLIES);

    public final OutlineComp outline = comp().get(NativeComponents.OUTLINE);
    public final BackgroundComp bg = comp().get(NativeComponents.BACKGROUND);

    public CommoditySelectionPanel(int width, int height, UIBuildableAPI content) {
        super(width, height, ROW_H, pad);
        targetPanel = content;
        mAddRowGap = true;
        buildUI();
    }

    protected List<CommoditySpecAPI> getDataList() {
        return EconomyConstants.econCommoditySpecs;
    }

    protected RowPanel createWidget(CommoditySpecAPI item, int index) {
        return new RowPanel(item);
    }

    protected void onWidgetClicked(RowPanel source) {
        // Never invoked: mIsSelectionEnabled is false; RowPanel manages its own click handler.
    }

    protected String getEmptyMessage() {
        return "";
    }

    public class RowPanel extends UIContainer implements
        WidgetAPI<RowPanel>, HasInteraction, HasGlow, HasAudioFeedback, HasTooltip
    {
        public final GlowComp glow = comp().get(NativeComponents.GLOW);
        public final AudioFeedbackComp audio = comp().get(NativeComponents.AUDIO_FEEDBACK);
        public final InteractionComp<RowPanel> interaction = comp().get(NativeComponents.INTERACTION);
        public final TooltipComp tooltip = comp().get(NativeComponents.TOOLTIP);

        private final CommoditySpecAPI spec;

        public RowPanel(CommoditySpecAPI com) {
            spec = com;

            glow.type = GlowType.UNDERLAY;

            interaction.onClicked = (source, isLeftClick) -> {
                selectedCom = spec;
                targetPanel.buildUI();

                if (UIUtils.canViewPrices() && NativeUiUtils.isCtrlDown()) {
                    final ComDetailDialog dialogPanel = new ComDetailDialog(
                        null, settings.getFactionSpec(Factions.PLAYER), com
                    );
                    dialogPanel.setConfirmShortcut();
                    dialogPanel.show(0.3f, 0.3f);
                }
            };

            tooltip.builder = (tp, exp) -> {
                if (UIUtils.canViewPrices()) {
                    tp.addPara(str("uiComSelectionTxt1"), pad, highlight, new String[]{
                        str("uiCtrlTxt"), str("uiClickTxt")
                    });
                } else {
                    final String text = str("uiComSelectionTxt2");
                    tp.addPara(text, pad, negative, text);
                }
            };
        }

        public InteractionComp<RowPanel> getInteraction() { return interaction; }

        public void buildUI() {
            clearChildren();

            final int iconSize = 28;

            final SpriteElement comIcon = new SpriteElement(
                iconSize, iconSize, spec.getIconName(), null, null
            );
            add(comIcon).inLMid(pad);

            final LabelAPI comNameLabel = settings.createLabel(spec.getName(), Fonts.ORBITRON_12);
            comNameLabel.setColor(base);
            add(comNameLabel).inLMid(iconSize + opad);
        }
    }
}
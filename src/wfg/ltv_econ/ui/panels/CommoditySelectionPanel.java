package wfg.ltv_econ.ui.panels;

import java.awt.Color;
import java.util.List;
import java.util.Optional;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.econ.CommoditySpecAPI;
import com.fs.starfarer.api.ui.CustomPanelAPI;
import com.fs.starfarer.api.ui.Fonts;
import com.fs.starfarer.api.ui.LabelAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.ui.UIPanelAPI;
import com.fs.starfarer.api.util.FaderUtil;

import wfg.ltv_econ.economy.engine.EconomyInfo;
import wfg.wrap_ui.ui.panels.CustomPanel;
import wfg.wrap_ui.ui.panels.CustomPanel.HasBackground;
import wfg.wrap_ui.ui.panels.CustomPanel.HasOutline;
import wfg.wrap_ui.ui.panels.SpritePanel.Base;
import wfg.wrap_ui.ui.plugins.BasePanelPlugin;
import wfg.wrap_ui.ui.systems.FaderSystem.Glow;
import wfg.wrap_ui.ui.systems.OutlineSystem.Outline;
import static wfg.wrap_ui.util.UIConstants.*;

public class CommoditySelectionPanel extends
    CustomPanel<BasePanelPlugin<CommoditySelectionPanel>, CommoditySelectionPanel, CustomPanelAPI> implements
    HasOutline, HasBackground
{
    private static final int ROW_H = 32;
    private static GlobalCommodityFlow contentPanel = null;

    public CommoditySelectionPanel(UIPanelAPI parent, int width, int height, GlobalCommodityFlow content) {
        super(parent, width, height, new BasePanelPlugin<>());

        contentPanel = content;

        getPlugin().init(this);
        createPanel();
    }

    public void createPanel() {
        final int width = (int) getPos().getWidth();
        final TooltipMakerAPI container = getPanel().createUIElement(
            width, getPos().getHeight(), true
        );
        final List<CommoditySpecAPI> commodities = EconomyInfo.getEconCommodities();

        float yCoord = pad;
        for (CommoditySpecAPI spec : commodities) {
            final RowPanel row = new RowPanel(
                container, width - opad, ROW_H, spec
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
        implements HasActionListener, AcceptsActionListener, HasFader, HasAudioFeedback
    {
        private final FaderUtil fader = new FaderUtil(0, 0, 0.2f, true, true);
        private final CommoditySpecAPI spec;

        public RowPanel(UIPanelAPI parent, int width, int height, CommoditySpecAPI com) {
            super(parent, width, height, new BasePanelPlugin<>());

            spec = com;

            getPlugin().init(this);
            createPanel();
        }

        public void createPanel() {
            final int iconSize = 28;

            final Base comIcon = new Base(
                getPanel(), iconSize, iconSize, spec.getIconName(),
                null, null
            );
            RowPanel.this.add(comIcon).inBL(pad, (ROW_H - iconSize) / 2f);

            final LabelAPI comNameLabel = Global.getSettings().createLabel(spec.getName(), Fonts.ORBITRON_12);
            comNameLabel.setColor(base);
            final float labelW = comNameLabel.computeTextHeight(spec.getName());
             RowPanel.this.add(comNameLabel).inBL(iconSize + opad, (ROW_H - labelW) / 2f);
        }

        public void onClicked(CustomPanel<?, ?, ?> source, boolean isLeftClick) {
            GlobalCommodityFlow.selectedCom = spec;
            contentPanel.createPanel();
        }

        public Optional<HasActionListener> getActionListener() {
            return Optional.of(this);
        }

        public FaderUtil getFader() {
            return fader;
        }

        public Glow getGlowType() {
            return Glow.UNDERLAY;
        }
    }
}
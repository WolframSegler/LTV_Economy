package wfg_ltv_econ.ui.com_detail_dialog;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CustomUIPanelPlugin;
import com.fs.starfarer.api.campaign.econ.CommodityOnMarketAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.loading.Description.Type;
import com.fs.starfarer.api.ui.Fonts;
import com.fs.starfarer.api.ui.IconRenderMode;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.ui.UIPanelAPI;

import wfg_ltv_econ.plugins.LtvCustomPanelPlugin;
import wfg_ltv_econ.ui.LtvCustomPanel;

public class Section1Icons extends LtvCustomPanel {

    private final TooltipMakerAPI m_tooltip;
    private final UIPanelAPI m_section;
    private final CommodityOnMarketAPI m_com;
    private final int headerHeight;

    public Section1Icons(UIPanelAPI parent, TooltipMakerAPI tooltip, int headerHeight, MarketAPI  
        market, int width, int height, CustomUIPanelPlugin plugin, CommodityOnMarketAPI com) {
        super(parent, width, height, plugin, market);

        m_tooltip = tooltip;
        m_section = parent;
        m_com = com;
        this.headerHeight = headerHeight;

        initializePanel(hasPlugin);
        createPanel();
    }

    public void initializePanel(boolean hasPlugin) {
        ((LtvCustomPanelPlugin) m_panel.getPlugin()).init(this, true, true, false, false);
    }

    public void createPanel() {
        final int opad = 10;
        final float width = getPanelPos().getWidth();
        final float height = getPanelPos().getHeight();

        final int iconSize = (int) (m_section.getPosition().getHeight() / 2.2f);
        float actualIconWidth = iconSize * m_com.getCommodity().getIconWidthMult();

        m_tooltip.beginIconGroup();
        m_tooltip.addIcons(m_com, 1, IconRenderMode.NORMAL);
        m_tooltip.addIconGroup(iconSize, 1, 0);
        m_tooltip.getPrev().getPosition().inTL(opad * 3 + ((iconSize - actualIconWidth) * 0.5f),
                (height - iconSize) / 2 + headerHeight);

        m_tooltip.beginIconGroup();
        m_tooltip.addIcons(m_com, 1, IconRenderMode.NORMAL);
        m_tooltip.addIconGroup(iconSize, 1, 0);
        m_tooltip.getPrev().getPosition().inTL(width - 0.5f * (iconSize + actualIconWidth) - opad * 3,
                (height - iconSize) / 2 + headerHeight);
    }

    public void createTooltip(TooltipMakerAPI tooltip) {
        final int pad = 3;
        final int opad = 10;
        tooltip.createRect(BgColor, tooltip.getPosition().getWidth());

        final String comDesc = Global.getSettings().getDescription(m_com.getId(), Type.RESOURCE).getText1();

        tooltip.setParaFont(Fonts.ORBITRON_12);
        tooltip.addPara(m_com.getCommodity().getName(), getFaction().getBaseUIColor(), pad);

        tooltip.setParaFontDefault();
        tooltip.addPara(comDesc, opad);
    }
}

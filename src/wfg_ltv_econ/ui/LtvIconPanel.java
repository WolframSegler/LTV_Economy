package wfg_ltv_econ.ui;

import java.awt.Color;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CustomUIPanelPlugin;
import com.fs.starfarer.api.campaign.econ.CommodityOnMarketAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.impl.campaign.ids.Strings;
import com.fs.starfarer.api.loading.Description.Type;
import com.fs.starfarer.api.ui.CustomPanelAPI;
import com.fs.starfarer.api.ui.Fonts;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.ui.UIPanelAPI;
import com.fs.starfarer.api.util.Misc;

import wfg_ltv_econ.plugins.LtvIconPanelPlugin;
import wfg_ltv_econ.plugins.LtvCustomPanelPlugin.GlowType;
import wfg_ltv_econ.util.TooltipUtils;

public class LtvIconPanel extends LtvCustomPanel implements LtvCustomPanel.TooltipProvider {

    public final String m_spriteID;
    public Color color;
    public boolean drawBorder;
    public CommodityOnMarketAPI m_com;

    public LtvIconPanel(UIPanelAPI parent, MarketAPI market, int width, int height,
        CustomUIPanelPlugin plugin, String spriteID, Color color, boolean drawBorder) {
        super(parent, width, height, plugin, market);

        m_spriteID = spriteID;
        this.color = color;
        this.drawBorder = drawBorder;

        initializePlugin(hasPlugin);
        createPanel();
    }

    public void setCommodity(CommodityOnMarketAPI a) {
        m_com = a;
    }

    public void initializePlugin(boolean hasPlugin) {
        LtvIconPanelPlugin plugin = ((LtvIconPanelPlugin) m_panel.getPlugin());
        plugin.init(this, GlowType.ADDITIVE, true, false, false);
        plugin.init(m_spriteID, color, drawBorder);
        plugin.setIgnoreUIState(true);
    }

    public void createPanel() {
        final float iconSize = getPanelPos().getWidth();

        TooltipMakerAPI tooltip = m_panel.createUIElement(iconSize, iconSize, false);



        m_panel.addUIElement(tooltip).inTL(0, 0);
    }

    @Override
    public TooltipMakerAPI createTooltip() {
        if (m_com == null) {
            return null;
        }

        final int pad = 3;
        final int opad = 10;
        final Color gray = new Color(100, 100, 100);

        TooltipMakerAPI tooltip = m_panel.createUIElement(500, 0, false);

        tooltip.createRect(BgColor, tooltip.getPosition().getWidth());

        final String comDesc = Global.getSettings().getDescription(m_com.getId(), Type.RESOURCE).getText1();

        tooltip.setParaFont(Fonts.ORBITRON_12);
        tooltip.addPara(m_com.getCommodity().getName(), getFaction().getBaseUIColor(), pad);

        tooltip.setParaFontDefault();
        tooltip.addPara(comDesc, opad);

        String basePrice = ((int)m_com.getCommodity().getBasePrice()) + Strings.C;
        tooltip.addPara("Base value: %s per unit.", pad, Misc.getHighlightColor(), basePrice);
        
        tooltip.addPara("Expand to see remote price data.", gray, pad);

        m_panel.addUIElement(tooltip);
        TooltipUtils.mouseCornerPos(tooltip, opad);

        return tooltip;
    }

    @Override
    public void removeTooltip(TooltipMakerAPI tooltip) {
        m_panel.removeComponent(tooltip);
    }
}

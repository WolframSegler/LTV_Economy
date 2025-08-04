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

import wfg_ltv_econ.plugins.LtvSpritePanelPlugin;
import wfg_ltv_econ.plugins.LtvCustomPanelPlugin.Glow;
import wfg_ltv_econ.plugins.LtvCustomPanelPlugin.Outline;
import wfg_ltv_econ.util.TooltipUtils;

public class LtvIconPanel extends LtvSpritePanel implements LtvCustomPanel.TooltipProvider {

    private static final String notExpandedCodexF1 = "F1 more info";
    private static final String ExpandedCodexF1 = "F1 hide";
    private static final String codexF2 = "F2 open Codex";
    private TooltipMakerAPI codexTooltip;

    public boolean isExpanded = false;
    public CommodityOnMarketAPI m_com;

    public LtvIconPanel(UIPanelAPI root, UIPanelAPI parent, MarketAPI market, int width, int height,
        CustomUIPanelPlugin plugin, String spriteID, Color color, Color fillColor, boolean drawBorder) {
        super(root, parent, market, width, height, plugin, spriteID, color, fillColor, drawBorder);
    }

    public void setCommodity(CommodityOnMarketAPI a) {
        m_com = a;
    }

    @Override
    public void initializePlugin(boolean hasPlugin) {
        LtvSpritePanelPlugin plugin = ((LtvSpritePanelPlugin) m_panel.getPlugin());
        plugin.init(this, Glow.ADDITIVE, true, false, Outline.NONE);
        plugin.init(m_spriteID, color, fillColor, drawBorder);
        plugin.setIgnoreUIState(true);
    }

    @Override
    public void createPanel() {}

    @Override
    public UIPanelAPI getTooltipAttachmentPoint() {
        return getParent();
    }

    @Override
    public TooltipMakerAPI createTooltip() {
        if (m_com == null) {
            return null;
        }

        final int pad = 3;
        final int opad = 10;
        final Color gray = new Color(100, 100, 100);

        TooltipMakerAPI tooltip = ((CustomPanelAPI)getParent()).createUIElement(720, 0, false);

        tooltip.createRect(BgColor, tooltip.getPosition().getWidth());

        final String comDesc = Global.getSettings().getDescription(m_com.getId(), Type.RESOURCE).getText1();

        tooltip.setParaFont(Fonts.ORBITRON_12);
        tooltip.addPara(m_com.getCommodity().getName(), getFaction().getBaseUIColor(), pad);

        tooltip.setParaFontDefault();
        tooltip.addPara(comDesc, opad);

        String basePrice = ((int)m_com.getCommodity().getBasePrice()) + Strings.C;
        tooltip.addPara("Base value: %s per unit.", opad, Misc.getHighlightColor(), basePrice);
        if (!isExpanded) {
            tooltip.addPara("Expand to see remote price data.", gray, opad);

            final int codexW = 210;

            TooltipUtils.createCustomCodex(tooltip, codexTooltip, this,
                notExpandedCodexF1, codexF2, codexW);
        } else {
            tooltip.addSpacer(opad);

            final int codexW = 180;

            TooltipUtils.cargoComTooltip(tooltip, pad, opad, m_com.getCommodity(), 5,
                true, true, true);

            TooltipUtils.createCustomCodex(tooltip, codexTooltip, this,
                ExpandedCodexF1, codexF2, codexW);  
        }
        
        ((CustomPanelAPI)getParent()).addUIElement(tooltip);
        ((CustomPanelAPI)getParent()).bringComponentToTop(tooltip);
        TooltipUtils.mouseCornerPos(tooltip, opad);

        ((CustomPanelAPI)getParent()).addUIElement(codexTooltip);
        ((CustomPanelAPI)getParent()).bringComponentToTop(codexTooltip);
        codexTooltip.getPosition().belowLeft(tooltip, 0);

        return tooltip;
    }

    @Override
    public void removeTooltip(TooltipMakerAPI tooltip) {
        if (codexTooltip != null) {
            ((CustomPanelAPI)getParent()).removeComponent(codexTooltip);
        }
        ((CustomPanelAPI)getParent()).removeComponent(tooltip);
    }

    @Override 
    public void attachCodexTooltip(TooltipMakerAPI codex) {
        codexTooltip = codex;
    }
}

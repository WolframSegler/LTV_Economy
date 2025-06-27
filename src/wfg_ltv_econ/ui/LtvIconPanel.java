package wfg_ltv_econ.ui;

import java.awt.Color;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CargoAPI;
import com.fs.starfarer.api.campaign.CustomUIPanelPlugin;
import com.fs.starfarer.api.campaign.econ.CommodityOnMarketAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.impl.campaign.ids.Strings;
import com.fs.starfarer.api.impl.codex.CodexDataV2;
import com.fs.starfarer.api.impl.codex.CodexEntryV2;
import com.fs.starfarer.ui.impl.CargoTooltipFactory;
import com.fs.starfarer.api.loading.Description.Type;
import com.fs.starfarer.api.ui.CustomPanelAPI;
import com.fs.starfarer.api.ui.Fonts;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.ui.UIPanelAPI;
import com.fs.starfarer.api.util.Misc;

import wfg_ltv_econ.plugins.LtvIconPanelPlugin;
import wfg_ltv_econ.plugins.LtvCustomPanelPlugin.GlowType;
import wfg_ltv_econ.util.TooltipUtils;
import wfg_ltv_econ.util.UiUtils;

public class LtvIconPanel extends LtvCustomPanel implements LtvCustomPanel.TooltipProvider {

    public final String m_spriteID;
    public Color color;
    public boolean drawBorder;
    public CommodityOnMarketAPI m_com;

    public LtvIconPanel(UIPanelAPI root, UIPanelAPI parent, MarketAPI market, int width, int height,
        CustomUIPanelPlugin plugin, String spriteID, Color color, boolean drawBorder) {
        super(root, parent, width, height, plugin, market);

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

    public void createPanel() {}

    @Override
    public TooltipMakerAPI createTooltip() {
        if (m_com == null) {
            return null;
        }

        final int pad = 3;
        final int opad = 10;
        final Color gray = new Color(100, 100, 100);

        TooltipMakerAPI tooltip = ((CustomPanelAPI)getParent()).createUIElement(500, 0, false);

        tooltip.createRect(BgColor, tooltip.getPosition().getWidth());

        final String comDesc = Global.getSettings().getDescription(m_com.getId(), Type.RESOURCE).getText1();

        tooltip.setParaFont(Fonts.ORBITRON_12);
        tooltip.addPara(m_com.getCommodity().getName(), getFaction().getBaseUIColor(), pad);

        tooltip.setParaFontDefault();
        tooltip.addPara(comDesc, opad);

        String basePrice = ((int)m_com.getCommodity().getBasePrice()) + Strings.C;
        tooltip.addPara("Base value: %s per unit.", opad, Misc.getHighlightColor(), basePrice);
        
        tooltip.addPara("Expand to see remote price data.", gray, opad);

        ((CustomPanelAPI)getParent()).addUIElement(tooltip);
        TooltipUtils.mouseCornerPos(tooltip, opad);

        // Codex and F1
        tooltip.setCodexEntryId(CodexDataV2.getCommodityEntryId(m_com.getId()));

        // tooltip.setCodexTempEntry(new CodexEntryV2("IconPanelCodex", "", "megaIcon"));
        CargoTooltipFactory.super(tooltip, pad, m_com.getCommodity(), );

        UiUtils.positionCodexLabel(tooltip, opad, pad);

        return tooltip;
    }

    @Override
    public void removeTooltip(TooltipMakerAPI tooltip) {
        ((CustomPanelAPI)getParent()).removeComponent(tooltip);
    }
}

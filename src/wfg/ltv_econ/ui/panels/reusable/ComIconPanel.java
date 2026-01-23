package wfg.ltv_econ.ui.panels.reusable;

import java.awt.Color;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.campaign.econ.CommoditySpecAPI;
import com.fs.starfarer.api.impl.campaign.ids.Strings;
import com.fs.starfarer.api.impl.codex.CodexDataV2;
import com.fs.starfarer.api.loading.Description.Type;
import com.fs.starfarer.api.ui.Fonts;
import com.fs.starfarer.api.ui.UIPanelAPI;

import wfg.ltv_econ.util.TooltipUtils;
import wfg.native_ui.ui.components.HoverGlowComp;
import wfg.native_ui.ui.components.NativeComponents;
import wfg.native_ui.ui.components.TooltipComp;
import wfg.native_ui.ui.components.HoverGlowComp.GlowType;
import wfg.native_ui.ui.panels.SpritePanel;
import wfg.native_ui.ui.panels.CustomPanel.HasHoverGlow;
import wfg.native_ui.ui.panels.CustomPanel.HasTooltip;

import static wfg.native_ui.util.UIConstants.*;

public class ComIconPanel extends SpritePanel<ComIconPanel> implements
    HasTooltip, HasHoverGlow
{
    public final TooltipComp tooltip = comp().get(NativeComponents.TOOLTIP);
    public final HoverGlowComp glow = comp().get(NativeComponents.HOVER_GLOW);

    public FactionAPI m_faction;
    protected CommoditySpecAPI m_com;

    public ComIconPanel(UIPanelAPI parent, int width, int height, Color color,
        Color fillColor, CommoditySpecAPI spec,FactionAPI faction
    ) {
        super(parent, width, height, spec.getIconName(), color, fillColor);
        m_faction = faction;
        m_com = spec;
        
        context.ignore = true;

        glow.type = GlowType.ADDITIVE;
        glow.color = Color.WHITE;
        glow.additiveSprite = m_sprite;
        tooltip.width = 720f;
        tooltip.expandable = true;
        tooltip.codexID = CodexDataV2.getCommodityEntryId(spec.getId());
        tooltip.parent = m_parent;
        tooltip.builder = (tooltip, expanded) -> {
            if (m_com == null) return;

            final String comDesc = Global.getSettings().getDescription(
                m_com.getId(), Type.RESOURCE).getText1();

            tooltip.setParaFont(Fonts.ORBITRON_12);
            tooltip.addPara(m_com.getName(), m_faction.getBaseUIColor(), pad);

            tooltip.setParaFontDefault();
            tooltip.addPara(comDesc, opad);

            String basePrice = ((int)m_com.getBasePrice()) + Strings.C;
            tooltip.addPara("Base value: %s per unit.", opad, highlight, basePrice);

            if (!expanded) {
                tooltip.addPara("Expand to see remote price data.", gray, opad);

            } else {
                tooltip.addSpacer(opad);

                TooltipUtils.cargoComTooltip(tooltip, m_com, 5,
                    true, true, true
                ); 
            }
        };
    }
    public void createPanel() {}

    public void setCommodity(CommoditySpecAPI spec) {
        m_com = spec;
        tooltip.codexID = CodexDataV2.getCommodityEntryId(spec.getId());
    }
}
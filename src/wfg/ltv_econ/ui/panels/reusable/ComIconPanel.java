package wfg.ltv_econ.ui.panels.reusable;

import java.awt.Color;
import java.util.Optional;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.campaign.econ.CommoditySpecAPI;
import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.impl.campaign.ids.Strings;
import com.fs.starfarer.api.impl.codex.CodexDataV2;
import com.fs.starfarer.api.loading.Description.Type;
import com.fs.starfarer.api.ui.Fonts;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.ui.UIPanelAPI;
import com.fs.starfarer.api.util.FaderUtil;

import wfg.ltv_econ.util.TooltipUtils;
import wfg.wrap_ui.util.WrapUiUtils;
import wfg.wrap_ui.util.WrapUiUtils.AnchorType;
import wfg.wrap_ui.ui.Attachments;
import wfg.wrap_ui.ui.ComponentFactory;
import wfg.wrap_ui.ui.panels.SpritePanel;
import wfg.wrap_ui.ui.panels.CustomPanel.HasFader;
import wfg.wrap_ui.ui.panels.CustomPanel.HasTooltip;
import wfg.wrap_ui.ui.systems.FaderSystem.Glow;
import static wfg.wrap_ui.util.UIConstants.*;

public class ComIconPanel extends SpritePanel<ComIconPanel> implements HasTooltip, HasFader {

    private static final String notExpandedCodexF1 = "F1 more info";
    private static final String ExpandedCodexF1 = "F1 hide";
    private static final String codexF2 = "F2 open Codex";
    private final FaderUtil m_fader = new FaderUtil(0, 0, 0.2f, true, true);;
    private TooltipMakerAPI m_tooltip;
    private FactionAPI m_faction = null;

    public boolean isExpanded = false;
    public CommoditySpecAPI m_com;

    public ComIconPanel(UIPanelAPI parent, FactionAPI faction, int width, int height,
        String iconSpriteID, Color color, Color fillColor
    ) {
        super(parent, width, height, iconSpriteID, color, fillColor);
        getPlugin().setIgnoreUIState(true);

        m_faction = faction;
    }

    public void setCommodity(CommoditySpecAPI a) {
        m_com = a;
    }

    @Override
    public void createPanel() {}

    public FaderUtil getFader() {
        return m_fader;
    }

    @Override
    public Glow getGlowType() {
        return Glow.ADDITIVE;
    }

    public void setPersistentGlow(boolean a) {}

    public Color getGlowColor() {
        return Color.WHITE;
    }

    @Override
    public Optional<SpriteAPI> getAdditiveSprite() {
        return Optional.ofNullable(m_sprite);
    }

    @Override
    public void setExpanded(boolean a) {
        isExpanded = a;
    }

    @Override
    public boolean isExpanded() {
        return isExpanded;
    }

    @Override
    public UIPanelAPI getTpParent() {
        return Attachments.getScreenPanel();
    }

    @Override
    public TooltipMakerAPI createAndAttachTp() {
        if (m_com == null) return null;

        m_tooltip = ComponentFactory.createTooltip(720f, false);

        final String comDesc = Global.getSettings().getDescription(m_com.getId(), Type.RESOURCE).getText1();

        m_tooltip.setParaFont(Fonts.ORBITRON_12);
        m_tooltip.addPara(m_com.getName(), m_faction.getBaseUIColor(), pad);

        m_tooltip.setParaFontDefault();
        m_tooltip.addPara(comDesc, opad);

        String basePrice = ((int)m_com.getBasePrice()) + Strings.C;
        m_tooltip.addPara("Base value: %s per unit.", opad, highlight, basePrice);

        if (!isExpanded) {
            m_tooltip.addPara("Expand to see remote price data.", gray, opad);

        } else {
            m_tooltip.addSpacer(opad);

            TooltipUtils.cargoComTooltip(m_tooltip, pad, opad, m_com, 5,
                true, true, true); 
        }
        
        ComponentFactory.addTooltip(m_tooltip, 0f, false);
        WrapUiUtils.mouseCornerPos(m_tooltip, opad);

        return m_tooltip;
    }

    public Optional<UIPanelAPI> getCodexParent() {
        return Optional.ofNullable(getParent());
    }

    @Override
    public Optional<TooltipMakerAPI> createAndAttachCodex() {
        TooltipMakerAPI codex;

        if (!isExpanded) {
            final int codexW = 210;

            codex = TooltipUtils.createCustomCodex(this, notExpandedCodexF1, codexF2, codexW);
        } else {
            final int codexW = 180;

            codex = TooltipUtils.createCustomCodex(this, ExpandedCodexF1, codexF2, codexW);  
        }

        WrapUiUtils.anchorPanel(codex, m_tooltip, AnchorType.BottomLeft, opad + pad);

        return Optional.ofNullable(codex);
    }

    @Override
    public Optional<String> getCodexID() {
        return Optional.ofNullable(CodexDataV2.getCommodityEntryId(m_com.getId()));
    }
}
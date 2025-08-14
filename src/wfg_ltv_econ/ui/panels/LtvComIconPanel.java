package wfg_ltv_econ.ui.panels;

import java.awt.Color;
import java.util.Optional;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.econ.CommodityOnMarketAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.impl.campaign.ids.Strings;
import com.fs.starfarer.api.impl.codex.CodexDataV2;
import com.fs.starfarer.api.loading.Description.Type;
import com.fs.starfarer.api.ui.Fonts;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.ui.UIPanelAPI;
import com.fs.starfarer.api.util.FaderUtil;
import com.fs.starfarer.api.util.Misc;

import wfg_ltv_econ.ui.plugins.LtvSpritePanelPlugin;
import wfg_ltv_econ.util.TooltipUtils;
import wfg_ltv_econ.util.UiUtils;
import wfg_ltv_econ.util.UiUtils.AnchorType;
import wfg_ltv_econ.ui.components.FaderComponent.Glow;
import wfg_ltv_econ.ui.panels.LtvCustomPanel.HasFader;
import wfg_ltv_econ.ui.panels.LtvCustomPanel.HasTooltip;

public class LtvComIconPanel extends LtvSpritePanel<LtvComIconPanel> implements HasTooltip, HasFader {

    private static final int pad = 3;
    private static final int opad = 10;

    private static final String notExpandedCodexF1 = "F1 more info";
    private static final String ExpandedCodexF1 = "F1 hide";
    private static final String codexF2 = "F2 open Codex";
    private TooltipMakerAPI m_tooltip;
    private FaderUtil m_fader = null;

    public boolean isExpanded = false;
    public CommodityOnMarketAPI m_com;

    public LtvComIconPanel(UIPanelAPI root, UIPanelAPI parent, MarketAPI market, int width, int height,
        LtvSpritePanelPlugin<LtvComIconPanel> plugin, String iconSpriteID, Color color, Color fillColor) {
        super(root, parent, market, width, height, plugin, iconSpriteID, color, fillColor, false);

        m_fader = new FaderUtil(0, 0, 0.2f, true, true);
    }

    public void setCommodity(CommodityOnMarketAPI a) {
        m_com = a;
    }

    @Override
    public void initializePlugin(boolean hasPlugin) {
        getPlugin().init(this);
        getPlugin().init();
        getPlugin().setIgnoreUIState(true);
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

    public float getOverlayBrightness() {
        return 1.2f;
    }

    public Color getGlowColor() {
        return Color.WHITE;
    }

    @Override
    public Optional<SpriteAPI> getSprite() {
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
    public UIPanelAPI getTooltipParent() {
        return getParent();
    }

    @Override
    public TooltipMakerAPI createAndAttachTooltip() {
        if (m_com == null) {
            return null;
        }

        final Color gray = new Color(100, 100, 100);

        m_tooltip = getParent().createUIElement(720, 0, false);

        final String comDesc = Global.getSettings().getDescription(m_com.getId(), Type.RESOURCE).getText1();

        m_tooltip.setParaFont(Fonts.ORBITRON_12);
        m_tooltip.addPara(m_com.getCommodity().getName(), getFaction().getBaseUIColor(), pad);

        m_tooltip.setParaFontDefault();
        m_tooltip.addPara(comDesc, opad);

        String basePrice = ((int)m_com.getCommodity().getBasePrice()) + Strings.C;
        m_tooltip.addPara("Base value: %s per unit.", opad, Misc.getHighlightColor(), basePrice);

        if (!isExpanded) {
            m_tooltip.addPara("Expand to see remote price data.", gray, opad);

        } else {
            m_tooltip.addSpacer(opad);

            TooltipUtils.cargoComTooltip(m_tooltip, pad, opad, m_com.getCommodity(), 5,
                true, true, true); 
        }
        
        getParent().addUIElement(m_tooltip);
        getParent().bringComponentToTop(m_tooltip);
        TooltipUtils.mouseCornerPos(m_tooltip, opad);

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

        getParent().addUIElement(codex);
        getParent().bringComponentToTop(codex);
        UiUtils.anchorPanel(codex, m_tooltip, AnchorType.BottomLeft, opad + pad);

        return Optional.ofNullable(codex);
    }

    @Override
    public Optional<String> getCodexID() {
        return Optional.ofNullable(CodexDataV2.getCommodityEntryId(m_com.getId()));
    }
}

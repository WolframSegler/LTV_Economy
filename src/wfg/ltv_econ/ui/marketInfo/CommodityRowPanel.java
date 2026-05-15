package wfg.ltv_econ.ui.marketInfo;

import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.ui.Alignment;
import com.fs.starfarer.api.ui.Fonts;
import com.fs.starfarer.api.ui.LabelAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.ui.UIPanelAPI;

import wfg.ltv_econ.economy.commodity.CommodityCell;
import wfg.ltv_econ.economy.engine.EconomyEngine;
import wfg.ltv_econ.ui.reusable.CommodityInfoBar;
import wfg.ltv_econ.constants.UIColors;
import wfg.ltv_econ.util.TooltipUtils;
import wfg.ltv_econ.util.UIUtils;
import wfg.native_ui.util.NumFormat;
import wfg.native_ui.util.NativeUiUtils;
import wfg.native_ui.util.NativeUiUtils.AnchorType;
import wfg.native_ui.ui.component.AudioFeedbackComp;
import wfg.native_ui.ui.component.HoverGlowComp;
import wfg.native_ui.ui.component.InteractionComp;
import wfg.native_ui.ui.component.NativeComponents;
import wfg.native_ui.ui.component.TooltipComp;
import wfg.native_ui.ui.component.HoverGlowComp.GlowType;
import wfg.native_ui.ui.core.UIBuildableAPI;
import wfg.native_ui.ui.core.UIElementFlags.HasAudioFeedback;
import wfg.native_ui.ui.core.UIElementFlags.HasHoverGlow;
import wfg.native_ui.ui.core.UIElementFlags.HasInteraction;
import wfg.native_ui.ui.core.UIElementFlags.HasTooltip;
import wfg.native_ui.ui.panel.CustomPanel;
import wfg.native_ui.ui.visual.SpritePanel.Base;

import com.fs.starfarer.api.impl.campaign.ids.Strings;
import com.fs.starfarer.api.impl.codex.CodexDataV2;
import com.fs.starfarer.api.loading.Description.Type;

import java.awt.Color;
import static wfg.native_ui.util.UIConstants.*;
import static wfg.native_ui.util.Globals.settings;
import static wfg.ltv_econ.constants.strings.LocalizedStrings.*;

public class CommodityRowPanel extends CustomPanel implements
    HasTooltip, HasHoverGlow, HasAudioFeedback, HasInteraction, UIBuildableAPI
{
    private static final SpriteAPI EXPORTS_ICON = settings.getSprite("commodity_markers", "exports");

    public static final int iconSize = 26;

    public final TooltipComp tooltip = comp().get(NativeComponents.TOOLTIP);
    public final HoverGlowComp glow = comp().get(NativeComponents.HOVER_GLOW);
    public final AudioFeedbackComp audio = comp().get(NativeComponents.AUDIO_FEEDBACK);
    public final InteractionComp<CommodityRowPanel> interaction = comp().get(NativeComponents.INTERACTION);

    public final CommodityCell cell;
    private final MarketAPI market;

    public CommodityRowPanel(UIPanelAPI parent, MarketAPI market, String comID,
        int width, int height, boolean ignoreUIContext
    ) {
        super(parent, width, height);

        final EconomyEngine engine = EconomyEngine.instance();
        cell = engine.getComCell(comID, market.getId());
        this.market = market;

        glow.color = base;
        glow.type = GlowType.UNDERLAY;

        tooltip.width = 500f;
        tooltip.expandable = true;
        tooltip.positioner = (tp, expanded) -> {
            NativeUiUtils.anchorPanel(tp, m_parent, AnchorType.LeftTop, pad*4);
        };
        tooltip.codexID = CodexDataV2.getCommodityEntryId(comID);
        tooltip.expandTxt = str("uiTpShowLegend");
        tooltip.unexpandTxt = str("uiHide");

        buildUI();
    }

    public void buildUI() {
        final int textW = 75;
        final int rowH = (int) getPos().getHeight();

        final Base comIcon = new Base(m_panel, rowH, rowH, cell.spec.getIconName(), null, null);
        add(comIcon).inBL(2f, 0f);

        final float consumption = cell.getConsumption(true);
        final String amountStr;
        if (consumption <= 0f) {
            amountStr = str("notApplicable");
        } else {
            amountStr = NumFormat.engNotate(cell.getStored() / consumption) + str("uiSingleLetterDay");
        }
        final LabelAPI amountLbl = settings.createLabel(Strings.X + amountStr, Fonts.INSIGNIA_LARGE);
        amountLbl.autoSizeToWidth(textW);
        final float textHeight = amountLbl.computeTextHeight(amountLbl.getText());
        amountLbl.setColor(market.getFaction().getBaseUIColor());
        add(amountLbl).inBL(pad*2 + rowH, (rowH - textHeight) / 2f);

        final Base stockIcon = UIUtils.getStockpilesIcon(cell,
            iconSize, m_panel, base
        );
        add(stockIcon).inBL(pad*3 + rowH + textW, (rowH - iconSize) / 2f);

        final UIPanelAPI infoBar = new CommodityInfoBar(null, 85, iconSize,
            true, cell).getPanel();
        add(infoBar).inBL(pad*4 + rowH + textW + iconSize, (rowH - iconSize) / 2f);

        if (EconomyEngine.instance().info.getExportAmount(cell.comID, cell.marketID) > 0.0) {
            final Base iconPanel = new Base(m_panel, rowH - 4, rowH - 4,
                EXPORTS_ICON, null, null);

            add(iconPanel).inRMid(pad);
        }

        tooltip.builder = (tp, expanded) -> {
            final String comDesc = settings.getDescription(cell.comID, Type.RESOURCE).getText1();
            final String timerStr = "    -   " + strf("uiDaysOfStockSuffix", amountStr);

            tp.setParaFont(Fonts.ORBITRON_12);
            final LabelAPI title = tp.addPara(cell.spec.getName(), market.getFaction().getBaseUIColor(), pad);
            title.setText(title.getText() + timerStr);

            tp.setParaFontDefault();
            tp.addPara(comDesc, opad);

            if (UIUtils.canViewPrices()) {
                tp.addPara(str("uiViewGlobalMarketInfo"), opad, positive);
            } else {
                tp.addPara(str("uiCannotViewGlobalMarketInfo"), opad, negative);
            }
            
            if (!expanded) {
                tp.setParaFont(Fonts.ORBITRON_12);
                tp.addSectionHeading(str("uiTitleStockpilesTradeDemand"), Alignment.MID, opad);
                TooltipUtils.createComStockpilesChangeBreakdown(tp, cell);
                tp.addSpacer(opad);
                TooltipUtils.createComTargetBreakdown(tp, cell);

                tp.setParaFont(Fonts.ORBITRON_12);
                tp.addSectionHeading(str("uiProductionConsumptionTitle"), Alignment.MID, opad);
                TooltipUtils.createComProductionBreakdown(tp, cell);
                
                tp.addSpacer(hpad);
                tp.setParaFont(Fonts.ORBITRON_12);
                TooltipUtils.createComConsumptionBreakdown(tp, cell);

                tp.addSectionHeading(str("uiTradeLedgerTitle"), Alignment.MID, opad);
                TooltipUtils.createComTradeLedgerSection(tp, cell);
                
                tp.addPara(str("uiCommodityWidgetTpTxt1"), gray, opad);

            } else {
                tp.setParaFont(Fonts.ORBITRON_12);
                tp.addSectionHeading(str("uiInfoLegendTitle"), Alignment.MID, opad);
                tp.setParaFontDefault();

                final int y = (int)tp.getHeightSoFar() + pad;

                legendRowCreator(0, tp, y, iconSize); 
                tp.addSpacer(pad);
            }
        };
    }

    /**
     * Renders the legend icons and their descriptions in the given tooltip at the specified starting y-position.
     * 
     * <br></br> MODE_0: shows everything.
     * <br></br> MODE_1: shows only the CommodityInfoBar relevant info.
     */
    public static final void legendRowCreator(int mode, TooltipMakerAPI tp, int y, int iconSize) {

        if (mode == 0) {
            legendRowHelper(tp, y, UIUtils.STOCKPILES_FULL, str("uiLegendRowDesc1"), iconSize, false, null);
            
            y += iconSize + pad;

            legendRowHelper(tp, y, EXPORTS_ICON, str("uiLegendRowDesc2"), iconSize, false, null);
            
            y += iconSize + pad;
    
            legendRowHelper(tp, y, null, str("uiLegendRowDesc3"), iconSize, true, null);
            
            y += iconSize + pad;
        }

        legendRowHelper(tp, y, null, str("uiLegendRowDesc4"), iconSize, false, UIColors.COM_NOT_EXPORTED);
        
        y += iconSize + pad;

        legendRowHelper(tp, y, null, str("uiLegendRowDesc5"), iconSize, false, UIColors.COM_EXPORT);
        
        y += iconSize + pad;

        legendRowHelper(tp, y, null, str("uiLegendRowDesc6"), iconSize, false, UIColors.COM_LOCAL_PROD);
        
        y += iconSize + pad;

        legendRowHelper(tp, y, null, str("uiLegendRowDesc7"), iconSize, false, UIColors.COM_FACTION_IMPORT);
        
        y += iconSize + pad;

        legendRowHelper(tp, y, null, str("uiLegendRowDesc8"), iconSize, false, UIColors.COM_IMPORT);
        
        y += iconSize + pad;

        legendRowHelper(tp, y, null, str("uiLegendRowDesc9"), iconSize, false, UIColors.COM_OVER_IMPORT);
        
        y += iconSize + pad;

        legendRowHelper(tp, y, null, str("uiLegendRowDesc10"), iconSize, false, UIColors.COM_DEFICIT);
    
        tp.setHeightSoFar(y + opad*2);
    }

    public static final void legendRowHelper(TooltipMakerAPI tp, int y, SpriteAPI icon, String desc,
        int lgdIconSize, boolean drawRedBorder, Color fillColor
    ) {
        final Base iconPanel = new Base(tp, lgdIconSize, lgdIconSize,
            icon, null, fillColor
        );
        if (drawRedBorder) {
            iconPanel.outline.enabled = true;
            iconPanel.outline.color = Color.RED;
            iconPanel.outline.offset.setOffset(2, 2, -4, -4);
        }
            
        tp.addCustom(iconPanel.getPanel(), 0f).getPosition().inTL(pad, y);

		final LabelAPI lbl = tp.addPara(desc, 0f);
		final float textX = opad + lgdIconSize;
		lbl.getPosition().inTL(textX, y + ((lgdIconSize - lbl.computeTextHeight(desc)) / 2f));
    }
}
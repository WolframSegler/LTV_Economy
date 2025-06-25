package wfg_ltv_econ.ui.com_detail_dialog;

import java.awt.Color;

import org.lwjgl.input.Keyboard;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CustomDialogDelegate;
import com.fs.starfarer.api.campaign.CustomUIPanelPlugin;
import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.campaign.econ.CommodityOnMarketAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.impl.campaign.ids.Strings;
import com.fs.starfarer.api.ui.Alignment;
import com.fs.starfarer.api.ui.ButtonAPI;
import com.fs.starfarer.api.ui.CustomPanelAPI;
import com.fs.starfarer.api.ui.Fonts;
import com.fs.starfarer.api.ui.LabelAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.ui.UIComponentAPI;
import com.fs.starfarer.api.ui.UIPanelAPI;
import com.fs.starfarer.api.ui.ButtonAPI.UICheckboxSize;
import com.fs.starfarer.api.util.Misc;
import com.fs.starfarer.campaign.ui.marketinfo.CommodityDetailDialog;

import wfg_ltv_econ.plugins.LtvCommodityDetailDialogPlugin;
import wfg_ltv_econ.plugins.LtvIconPanelPlugin;
import wfg_ltv_econ.plugins.LtvCustomPanelPlugin;
import wfg_ltv_econ.ui.LtvCommodityPanel;
import wfg_ltv_econ.ui.LtvCustomPanel;
import wfg_ltv_econ.ui.LtvIconPanel;
import wfg_ltv_econ.ui.LtvUIState;
import wfg_ltv_econ.ui.LtvUIState.UIStateType;
import wfg_ltv_econ.util.CommodityStats;

public class LtvCommodityDetailDialog implements CustomDialogDelegate {

    public interface CommoditySelectionListener {
        void onCommoditySelected(CommodityOnMarketAPI selectedCommodity);
    }

    // Notes about the original window. Using VisualVM
    // new Color(181, 230, 255, 255)
    // new Color(70, 200, 255, 255)
    // new Color(31, 94, 112, 255)
    // new Color(170, 222, 255, 255)

    // this.PANEL_W = 1206; // Exact width acquired using VisualVM. Includes
    // padding.
    // this.PANEL_H = 728; // Exact height acquired using VisualVM. Includes
    // padding.

    public final int PANEL_W;
    public final int PANEL_H;

    public final int SECT1_WIDTH;
    public final int SECT2_WIDTH;
    public final int SECT3_WIDTH;
    public final int SECT4_WIDTH;

    public final int SECT1_HEIGHT;
    public final int SECT2_HEIGHT;
    public final int SECT3_HEIGHT;
    public final int SECT4_HEIGHT;

    protected CustomPanelAPI section1; // CommodityInfo
    protected CustomPanelAPI section2; // Sector Map
    protected CustomPanelAPI section3; // Prod&Consump Tables
    protected CustomPanelAPI section4; // Commodity Panel

    public final static int pad = 3;
    public final static int opad = 10;

    private final LtvCustomPanel m_parentWrapper;
    private final LtvCommodityDetailDialogPlugin m_plugin;
    private final Color highlight = Misc.getHighlightColor();
    private CustomPanelAPI m_dialogPanel;
    public CommodityOnMarketAPI m_com;

    public LtvCommodityDetailDialog(LtvCustomPanel parent, CommodityOnMarketAPI com) {
        // Measured using very precise tools!! (my eyes)
        this(parent, com, 1166, 658 + 20);
    }

    public LtvCommodityDetailDialog(LtvCustomPanel parent, CommodityOnMarketAPI com, int panelW, int panelH) {
        this.PANEL_W = panelW;
        this.PANEL_H = panelH;

        SECT1_WIDTH = (int) (PANEL_W * 0.76f - opad);
        SECT1_HEIGHT = (int) (PANEL_H * 0.28f - opad);

        SECT2_WIDTH = (int) (PANEL_W * 0.24f - opad);
        SECT2_HEIGHT = (int) (PANEL_H * 0.28f - opad);

        SECT3_WIDTH = (int) (PANEL_W * 0.76f - opad);
        SECT3_HEIGHT = (int) (PANEL_H * 0.72f - opad);

        SECT4_WIDTH = (int) (PANEL_W * 0.24f - opad);
        SECT4_HEIGHT = (int) (PANEL_H * 0.72f - opad);

        m_plugin = new LtvCommodityDetailDialogPlugin(parent, this);
        m_parentWrapper = parent;
        m_com = com;
    }

    @Override
    public void createCustomDialog(CustomPanelAPI panel, CustomDialogCallback callback) {
        LtvUIState.setState(UIStateType.DETAIL_DIALOG);
        m_dialogPanel = panel;

        createSections();

        // Footer
        final int footerH = 40;

        TooltipMakerAPI footer = panel.createUIElement(PANEL_W, footerH, false);
        ButtonAPI m_checkbox = footer.addCheckbox(20, 20, "", "stockpile_toggle",
            Fonts.ORBITRON_12, highlight, UICheckboxSize.SMALL, 0);

        m_checkbox.getPosition().inBL(0, 0);
        m_checkbox.setShortcut(Keyboard.KEY_Q, false);

        footer.setParaFont(Fonts.ORBITRON_12);
        LabelAPI txt = footer.addPara("Only show colonies with excess stockpiles or shortages (%s)", 0f, highlight,
                "Q");
        int TextY = (int) txt.computeTextHeight(txt.getText());
        footer.getPrev().getPosition().inBL(20 + pad, (20 - TextY) / 2);

        panel.addUIElement(footer).inBL(pad, -opad * 3.5f);
        m_plugin.init(false, false, false, panel);
    }

    public void createSections() {
        if (m_dialogPanel == null) {
            return;
        }

        updateSection1();
        updateSection2();
        updateSection3();
        updateSection4();
    }

    public void updateSection1() {
        m_dialogPanel.removeComponent(section1);

        section1 = Global.getSettings().createCustom(SECT1_WIDTH, SECT1_HEIGHT, null);

        TooltipMakerAPI tooltip = section1.createUIElement(SECT1_WIDTH, SECT1_HEIGHT, false);
        section1.addUIElement(tooltip).inTL(0, 0);

        createSection1(section1, tooltip, highlight);
        m_dialogPanel.addComponent(section1).inTL(pad, pad);

        // Update anchors
        if (section2 == null || section3 == null) {
            return;
        }
        section2.getPosition().rightOfTop(section1, opad * 1.5f);
        section3.getPosition().rightOfTop(section1, opad * 1.5f);
    }

    public void updateSection2() {
        m_dialogPanel.removeComponent(section2);

        section2 = Global.getSettings().createCustom(SECT2_WIDTH, SECT2_HEIGHT, null);

        TooltipMakerAPI tooltip = section2.createUIElement(SECT2_WIDTH, SECT2_HEIGHT, false);
        section2.addUIElement(tooltip).inTL(0, 0);

        createSection2(section2, tooltip);
        m_dialogPanel.addComponent(section2).rightOfTop(section1, opad * 1.5f);

        // Update anchors
        if (section4 == null) {
            return;
        }
        section4.getPosition().belowLeft(section2, opad);
    }

    public void updateSection3() {
        m_dialogPanel.removeComponent(section3);

        section3 = Global.getSettings().createCustom(SECT3_WIDTH, SECT3_HEIGHT, null);

        TooltipMakerAPI tooltip = section3.createUIElement(SECT3_WIDTH, SECT3_HEIGHT, true);
        section3.addUIElement(tooltip).inTL(0, 0);

        createSection3(section3, tooltip);
        m_dialogPanel.addComponent(section3).belowLeft(section1, opad);
    }

    public void updateSection4() {
        m_dialogPanel.removeComponent(section4);

        section4 = Global.getSettings().createCustom(SECT4_WIDTH, SECT4_HEIGHT, null);

        TooltipMakerAPI tooltip = section4.createUIElement(SECT4_WIDTH, SECT4_HEIGHT, false);
        section4.addUIElement(tooltip).inTL(0, 0);

        createSection4(section4);
        m_dialogPanel.addComponent(section4).belowLeft(section2, opad);
    }

    private void createSection1(CustomPanelAPI section, TooltipMakerAPI tooltip, Color highlight) {
        if (m_com == null) {
            return;
        }
        tooltip.addSectionHeading(m_com.getCommodity().getName(), Alignment.MID, pad);
        final int headerHeight = (int) tooltip.getPrev().getPosition().getHeight();

        // Icons
        final int iconSize = (int) (section.getPosition().getHeight() / 2.2f);
        final float actualIconWidth = iconSize * m_com.getCommodity().getIconWidthMult();

        String comID = m_com.getCommodity().getIconName();

        LtvIconPanel iconLeft = new LtvIconPanel(section, m_parentWrapper.m_market, iconSize, iconSize,
                new LtvIconPanelPlugin(), comID, null, false);
        iconLeft.setCommodity(m_com);

        iconLeft.getPanelPos().inTL(opad * 3 + ((iconSize - actualIconWidth) * 0.5f),
                (SECT1_HEIGHT - iconSize) / 2 + headerHeight);
        section.addComponent(iconLeft.getPanel());

        LtvIconPanel iconRight = new LtvIconPanel(section, m_parentWrapper.m_market, iconSize, iconSize,
                new LtvIconPanelPlugin(), comID, null, false);
        iconRight.setCommodity(m_com);

        iconRight.getPanelPos().inTL(SECT1_WIDTH - 0.5f * (iconSize + actualIconWidth) - opad * 3,
                (SECT1_HEIGHT - iconSize) / 2 + headerHeight);
        section.addComponent(iconRight.getPanel());

        // Text
        final int baseY = (int) (headerHeight + opad * 1.5f);
        Color baseColor = m_parentWrapper.m_faction.getBaseUIColor();
        { // Global market value
            int y = baseY;
            String txt = "Global market value";
            String valueTxt = Misc.getWithDGS(m_com.getCommodityMarketData().getMarketValue()) + Strings.C;
            if (m_com.getCommodityMarketData().getMarketValue() < 1) {
                valueTxt = "---";
            }

            tooltip.setParaFontColor(baseColor);
            tooltip.setParaFont(Fonts.ORBITRON_12);
            LabelAPI lbl1 = tooltip.addPara(txt, pad);
            UIComponentAPI lblComp1 = tooltip.getPrev();

            tooltip.setParaFontColor(highlight);
            tooltip.setParaFont(Fonts.INSIGNIA_VERY_LARGE);
            LabelAPI lbl2 = tooltip.addPara(valueTxt, highlight, pad);
            UIComponentAPI lblComp2 = tooltip.getPrev();

            float textH1 = lblComp1.getPosition().getHeight();
            float textW1 = lbl1.computeTextWidth(txt);
            float textX1 = (SECT1_WIDTH / 3f) - textW1;

            lblComp1.getPosition().inTL(textX1, y);

            y += textH1 + pad;
            float textW2 = lbl2.computeTextWidth(valueTxt);
            float textX2 = (textX1 + (textW1 / 2)) - (textW2 / 2);

            lblComp2.getPosition().inTL(textX2, y);
        }
        { // Total global exports
            int y = baseY;
            String txt = "Total global exports";

            String valueTxt = Integer.toString(getTotalGlobalExports(m_com.getId()));

            tooltip.setParaFontColor(baseColor);
            tooltip.setParaFont(Fonts.ORBITRON_12);
            LabelAPI lbl1 = tooltip.addPara(txt, pad);
            UIComponentAPI lblComp1 = tooltip.getPrev();

            tooltip.setParaFontColor(highlight);
            tooltip.setParaFont(Fonts.INSIGNIA_VERY_LARGE);
            LabelAPI lbl2 = tooltip.addPara(valueTxt, highlight, pad);
            UIComponentAPI lblComp2 = tooltip.getPrev();

            float textH1 = lblComp1.getPosition().getHeight();
            float textW1 = lbl1.computeTextWidth(txt);
            float textX1 = (SECT1_WIDTH / 2f) - (textW1 / 2);

            lblComp1.getPosition().inTL(textX1, y);

            y += textH1 + pad;
            float textW2 = lbl2.computeTextWidth(valueTxt);
            float textX2 = (textX1 + (textW1 / 2)) - (textW2 / 2);

            lblComp2.getPosition().inTL(textX2, y);
        }
        { // Total faction exports
            int y = baseY;
            String factionName = m_parentWrapper.m_faction.getDisplayNameLong();
            String txt = "Total " + factionName + " exports";

            String valueTxt = Integer.toString(getTotalFactionImports(m_com.getId(),
                m_parentWrapper.m_faction));

            tooltip.setParaFontColor(baseColor);
            tooltip.setParaFont(Fonts.ORBITRON_12);
            LabelAPI lbl1 = tooltip.addPara(txt, pad);
            UIComponentAPI lblComp1 = tooltip.getPrev();

            tooltip.setParaFont(Fonts.INSIGNIA_VERY_LARGE);
            LabelAPI lbl2 = tooltip.addPara(valueTxt, pad);
            UIComponentAPI lblComp2 = tooltip.getPrev();

            float textH1 = lblComp1.getPosition().getHeight();
            float textW1 = lbl1.computeTextWidth(txt);
            float textX1 = SECT1_WIDTH * 2 / 3f;

            lblComp1.getPosition().inTL(textX1, y);

            y += textH1 + pad;
            float textW2 = lbl2.computeTextWidth(valueTxt);
            float textX2 = (textX1 + (textW1 / 2)) - (textW2 / 2);

            lblComp2.getPosition().inTL(textX2, y);
        }
        { // Faction market share
            int y = baseY*3 + pad;
            String factionName = m_parentWrapper.m_faction.getDisplayNameLong();
            String txt = factionName + " market share";

            String valueTxt = m_com.getCommodityMarketData().getMarketSharePercent(m_parentWrapper.m_faction)  
                            + "%";

            tooltip.setParaFontColor(baseColor);
            tooltip.setParaFont(Fonts.ORBITRON_12);
            LabelAPI lbl1 = tooltip.addPara(txt, pad);
            UIComponentAPI lblComp1 = tooltip.getPrev();

            tooltip.setParaFont(Fonts.INSIGNIA_VERY_LARGE);
            LabelAPI lbl2 = tooltip.addPara(valueTxt, pad);
            UIComponentAPI lblComp2 = tooltip.getPrev();

            float textH1 = lblComp1.getPosition().getHeight();
            float textW1 = lbl1.computeTextWidth(txt);
            float textX1 = (SECT1_WIDTH / 2f) - (textW1 / 2);

            lblComp1.getPosition().inTL(textX1, y);

            y += textH1 + pad;
            float textW2 = lbl2.computeTextWidth(valueTxt);
            float textX2 = (textX1 + (textW1 / 2)) - (textW2 / 2);

            lblComp2.getPosition().inTL(textX2, y);
        }

    }

    private void createSection2(CustomPanelAPI section, TooltipMakerAPI tooltip) {
        StarSystemAPI starSystem = m_parentWrapper.m_market.getStarSystem();
        starSystem.setName(m_parentWrapper.m_market.getName());

        UIPanelAPI map = tooltip.addSectorMap(section.getPosition().getWidth(),
                section.getPosition().getHeight() - 2 * opad, starSystem, 0);

        map.getPosition().inTL(0, 0);
    }

    private void createSection3(CustomPanelAPI section, TooltipMakerAPI tooltip) {

    }

    private void createSection4(CustomPanelAPI section) {
        CustomUIPanelPlugin comPanelPlugin = new LtvCustomPanelPlugin();

        LtvCommodityPanel comPanel = new LtvCommodityPanel(
                (UIPanelAPI) section,
                (int) section.getPosition().getWidth(),
                (int) section.getPosition().getHeight(),
                m_parentWrapper.m_market,
                comPanelPlugin,
                m_parentWrapper.m_market.getName() + " - Commodities",
                true);
        comPanel.setRowSelectable(true);

        comPanel.setCommoditySelectionListener(new CommoditySelectionListener() {
            @Override
            public void onCommoditySelected(CommodityOnMarketAPI selectedCommodity) {
                m_com = selectedCommodity;

                // Update UI
                updateSection1();
                updateSection3();
            }
        });

        section.addComponent(comPanel.getPanel());
    }

    int getTotalGlobalExports(String comID) {
        int total = 0;

        for (MarketAPI market : Global.getSector().getEconomy().getMarketsCopy()) {
            CommodityOnMarketAPI comOnMarket = market.getCommodityData(comID);

            CommodityStats stats = new CommodityStats(comOnMarket, market);

            total += stats.globalExport;
        }

        return total;
    }

    int getTotalFactionImports(String comID, FactionAPI faction) {
        int total = 0;

        for (MarketAPI market : Global.getSector().getEconomy().getMarketsCopy()) {
            if (!market.getFaction().getId().equals(faction.getId())) {
                continue;
            }
            CommodityOnMarketAPI comOnMarket = market.getCommodityData(comID);

            CommodityStats stats = new CommodityStats(comOnMarket, market);
            total += stats.globalExport;
        }

        return total;
    }

    @Override
    public void customDialogConfirm() {
        LtvUIState.setState(UIStateType.NONE);
    }

    @Override
    public void customDialogCancel() {
        LtvUIState.setState(UIStateType.NONE);
    }

    public float getCustomDialogWidth() {
        return PANEL_W;
    }

    public float getCustomDialogHeight() {
        return PANEL_H;
    }

    public String getCancelText() {
        return "Dismiss";
    }

    public String getConfirmText() {
        return "Dismiss";
    }

    public boolean hasCancelButton() {
        return false;
    }

    public CustomUIPanelPlugin getCustomPanelPlugin() {
        return m_plugin;
    }
}

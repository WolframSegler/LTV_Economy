package wfg_ltv_econ.ui;

import java.util.Map;

import java.awt.Color;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CustomDialogDelegate;
import com.fs.starfarer.api.campaign.CustomUIPanelPlugin;
import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.campaign.econ.CommodityOnMarketAPI;
import com.fs.starfarer.api.campaign.econ.CommoditySpecAPI;
import com.fs.starfarer.api.campaign.econ.MarketConditionAPI;
import com.fs.starfarer.api.campaign.econ.MutableCommodityQuantity;
import com.fs.starfarer.api.impl.campaign.ids.Strings;
import com.fs.starfarer.api.ui.Alignment;
import com.fs.starfarer.api.ui.CustomPanelAPI;
import com.fs.starfarer.api.ui.Fonts;
import com.fs.starfarer.api.ui.IconRenderMode;
import com.fs.starfarer.api.ui.LabelAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.ui.UIComponentAPI;
import com.fs.starfarer.api.ui.UIPanelAPI;
import com.fs.starfarer.api.ui.ButtonAPI.UICheckboxSize;
import com.fs.starfarer.api.util.Misc;
import com.fs.starfarer.campaign.ui.marketinfo.CommodityDetailDialog;

import wfg_ltv_econ.conditions.WorkerPoolCondition;
import wfg_ltv_econ.industry.LtvBaseIndustry;
import wfg_ltv_econ.util.NumFormat;
import wfg_ltv_econ.ui.LtvUIState;
import wfg_ltv_econ.ui.LtvUIState.UIStateType;

public class LtvCommodityDetailDialog implements CustomDialogDelegate {
    // The window that opens when you click on a commodity while inside the range
    // of a comms network

    // Notes about the original window. Using VisualVM
    // new Color(181, 230, 255, 255)
    // new Color(70, 200, 255, 255)
    // new Color(31, 94, 112, 255)
    // new Color(170, 222, 255, 255)

    public final static int pad = 3;
    public final static int opad = 10;

    // this.PANEL_W = 1206; // Exact width acquired using VisualVM. Includes padding.
    // this.PANEL_H = 728;  // Exact height acquired using VisualVM. Includes padding.

    public final int PANEL_W;
    public final int PANEL_H; 

    public final float SECT1_WIDTH;
    public final float SECT2_WIDTH;
    public final float SECT3_WIDTH;
    public final float SECT4_WIDTH;

    public final float SECT1_HEIGHT;
    public final float SECT2_HEIGHT;
    public final float SECT3_HEIGHT;
    public final float SECT4_HEIGHT;

    public LtvCommodityDetailDialog() {
        // Measured using very precise tools!! (my eyes)
        this.PANEL_W = 1166;
        this.PANEL_H = 658;

        SECT1_WIDTH = PANEL_W * 0.75f;
        SECT1_HEIGHT = PANEL_H * 0.25f;

        SECT2_WIDTH = PANEL_W * 0.25f;
        SECT2_HEIGHT = PANEL_H * 0.25f;

        SECT3_WIDTH = PANEL_W * 0.75f;
        SECT3_HEIGHT = PANEL_H * 0.7f;

        SECT4_WIDTH = PANEL_W * 0.25f;
        SECT4_HEIGHT = PANEL_H * 0.7f;
    }

    public LtvCommodityDetailDialog(int panelW, int panelH) {
        this.PANEL_W = panelW;
        this.PANEL_H = panelH;

        SECT1_WIDTH = PANEL_W * 0.75f;
        SECT1_HEIGHT = PANEL_H * 0.25f;

        SECT2_WIDTH = PANEL_W * 0.25f;
        SECT2_HEIGHT = PANEL_H * 0.25f;

        SECT3_WIDTH = PANEL_W * 0.75f;
        SECT3_HEIGHT = PANEL_H * 0.7f;

        SECT4_WIDTH = PANEL_W * 0.25f;
        SECT4_HEIGHT = PANEL_H * 0.7f;
    }

    @Override
    public void createCustomDialog(CustomPanelAPI panel, CustomDialogCallback callback) {
        LtvUIState.setState(UIStateType.DETAIL_DIALOG);

        final int footerH = 40;
        final Color highlight = Misc.getHighlightColor();

        CustomPanelAPI section1 = Global.getSettings().createCustom(SECT1_WIDTH, SECT1_HEIGHT, null);
        CustomPanelAPI section2 = Global.getSettings().createCustom(SECT2_WIDTH, SECT2_HEIGHT, null);
        CustomPanelAPI section3 = Global.getSettings().createCustom(SECT3_WIDTH, SECT3_HEIGHT, null);
        CustomPanelAPI section4 = Global.getSettings().createCustom(SECT4_WIDTH, SECT4_HEIGHT, null);

        TooltipMakerAPI tooltip1 = section1.createUIElement(SECT1_WIDTH, SECT1_HEIGHT, false);
        TooltipMakerAPI tooltip2 = section2.createUIElement(SECT2_WIDTH, SECT2_HEIGHT, false);
        TooltipMakerAPI tooltip3 = section3.createUIElement(SECT3_WIDTH, SECT3_HEIGHT, true);
        TooltipMakerAPI tooltip4 = section4.createUIElement(SECT4_WIDTH, SECT4_HEIGHT, false);

        createSection1(section1, tooltip1, highlight);  // CommodityInfo
        createSection2(section2, tooltip2);             // Sector Map
        createSection3(section3, tooltip3);             // Prod&Consump Tables
        createSection4(section4, tooltip4);             // Commodity Panel

        section1.addUIElement(tooltip1).inTL(0, 0);
        section2.addUIElement(tooltip2).inTL(0, 0);
        section3.addUIElement(tooltip3).inTL(0, 0);
        section4.addUIElement(tooltip4).inTL(0, 0);

        panel.addComponent(section1).inTL(pad, pad);
        panel.addComponent(section2).aboveRight(section1, pad);
        panel.addComponent(section3).belowLeft(section1, pad);
        panel.addComponent(section4).belowLeft(section2, pad);

        // Footer
        // "Only show colonies with excess stockpiles or shortages (Q)"
        TooltipMakerAPI footer = panel.createUIElement(PANEL_W, footerH, false);
        footer.addCheckbox(20, 20, "", "stockpile_toggle", Fonts.ORBITRON_12, highlight, 
        UICheckboxSize.SMALL, 0);
        footer.getPrev().getPosition().inBL(0, 0);

        footer.setParaFont(Fonts.ORBITRON_12);
        LabelAPI txt = footer.addPara("Only show colonies with excess stockpiles or shortages (%s)", 0f, highlight, "Q");
        int TextY = (int) txt.computeTextHeight(txt.getText()); 
        footer.getPrev().getPosition().inBL(20 + pad, (20-TextY)/2);

        panel.addUIElement(footer).inBL(pad, -opad*3.5f);
    }

    private void createSection1(CustomPanelAPI section, TooltipMakerAPI tooltip, Color highlight) {

    }

    private void createSection2(CustomPanelAPI section, TooltipMakerAPI tooltip) {
        
    }

    private void createSection3(CustomPanelAPI section, TooltipMakerAPI tooltip) {
        
    }

    private void createSection4(CustomPanelAPI section, TooltipMakerAPI tooltip) {
        
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
        return null;
    }
}

package wfg_ltv_econ.util;

import com.fs.starfarer.api.campaign.econ.CommodityOnMarketAPI;
import com.fs.starfarer.api.campaign.econ.CommoditySourceType;
import com.fs.starfarer.api.ui.Alignment;
import com.fs.starfarer.api.ui.CustomPanelAPI;
import com.fs.starfarer.api.ui.Fonts;
import com.fs.starfarer.api.ui.IconRenderMode;
import com.fs.starfarer.api.ui.UIComponentAPI;
import com.fs.starfarer.campaign.ui.marketinfo.i;
import com.fs.starfarer.campaign.ui.marketinfo.ooO0;
import com.fs.starfarer.campaign.ui.marketinfo.ooOo;
import com.fs.starfarer.settings.StarfarerSettings;
import com.fs.starfarer.ui.d;
import com.fs.starfarer.api.impl.campaign.econ.CommodityIconCounts;
import com.fs.starfarer.campaign.econ.CommodityOnMarket;
import com.fs.starfarer.campaign.econ.reach.CommodityMarketData;
import com.fs.starfarer.campaign.econ.reach.MarketShareData;

import java.awt.Color;

public class CommodityRow extends ooOo {

    private CustomPanelAPI m_panel;
    private CommodityOnMarketAPI m_com; // øøÓO00
    private ooO0 m_iconGroup; // super.for$null
    private i m_sourceIcon; // O0ÔO00

    public CommodityRow(CommodityOnMarketAPI com) {
        super((CommodityOnMarket) com);
        this.m_com = com;
    }

    public UIComponentAPI getPanel() {
        return m_panel;
    }

    public void sizeChanged(float var1, float var2) {
        this.clearChildren();
        // super.sizeChanged(var1, var2);
        if (!this.created) {
            this.afterSizeFirstChanged(var1, var2);
            this.created = true;
        }
        // End of grandparent method

        final float pad = 3f;
        final float opad = 10f;
        float rowHeight = this.getHeight();
        m_iconGroup = new ooO0(null);
        m_iconGroup.setMediumSpacing(true);
        m_iconGroup.autoSizeWithAdjust(this.getHeight(),
                this.getWidth() - rowHeight * 2.0F - pad * 2.0F - rowHeight - pad - opad, this.getHeight(),
                this.getHeight());
        float iconSize = 32.0F;
        Color baseColor = m_com.getMarket().getFaction().getBaseUIColor();
        d amountLabel = d.createSmallInsigniaLabel(m_com.getAvailable() + "\u00d7", Alignment.MID);
        boolean isShortRow = rowHeight < 24.0F;
        if (isShortRow) {
            amountLabel = new d(m_com.getAvailable() + "\u00d7", Fonts.DEFAULT_SMALL, baseColor, true, Alignment.MID);
            iconSize = 24.0F;
        }

        amountLabel.setColor(baseColor);
        ReflectionUtils.invoke(amountLabel.getRenderer(), "int", true);
        amountLabel.setSize(iconSize, amountLabel.getLineHeight());
        float labelWidth = amountLabel.getWidth() + pad;
        this.add(amountLabel).inLMid(rowHeight + pad);
        if (isShortRow) {
            amountLabel.getPosition().setYAlignOffset(1.0F);
        }
        handleIconGroup();

        this.add(m_iconGroup).inLMid(rowHeight + pad + labelWidth);

        CommodityMarketData commodityData = (CommodityMarketData) m_com.getCommodityMarketData();
        this.add(getSourceIcon(m_com, baseColor, commodityData)).setSize(rowHeight, rowHeight).inBL(0, 0);

        if (commodityData.getExportIncome(m_com) > 0) {
            m_sourceIcon = new i(
                    (String) ReflectionUtils.invoke(StarfarerSettings.class, "new", "commodity_markers", "exports"),
                    baseColor, false);
            this.add(this.m_sourceIcon).setSize(rowHeight, rowHeight).inBR(pad, 0.0F);
        }

        this.bringToTop(amountLabel);
    }

    private i getSourceIcon(CommodityOnMarketAPI com, Color baseColor, CommodityMarketData commodityData) {
        MarketShareData marketData = commodityData.getMarketShareData(m_com.getMarket());
        boolean isSourceIllegal = marketData.isSourceIsIllegal();

        CommoditySourceType source = com.getCommodityMarketData().getMarketShareData(com.getMarket()).getSource();
        String iconPath = (String) ReflectionUtils.invoke(StarfarerSettings.class, "new", "commodity_markers",
                "imports");

        switch (source) {
            case GLOBAL:
                return new i(iconPath, baseColor, isSourceIllegal);
            case IN_FACTION:
                iconPath = com.getMarket().getFaction().getCrest();
                return new i(iconPath, null, isSourceIllegal);
            case LOCAL:
            case NONE:
                iconPath = (String) ReflectionUtils.invoke(StarfarerSettings.class, "new", "commodity_markers",
                        "production");
                return new i(iconPath, baseColor, isSourceIllegal);
            default:
                return new i(iconPath, baseColor, isSourceIllegal);
        }
    }

    private void handleIconGroup() {
        float available = (float)m_com.getAvailableStat().getModifiedValue();
        float totalDemand = m_com.getMaxDemand();
        float totalSupply = m_com.getMaxSupply();

        float totalTarget = Math.max(Math.max(totalDemand, totalSupply), available);
        final int totalIcons = 6;

        CommodityIconCounts iconCount = new CommodityIconCounts(m_com);
        final int demandMetLocal = iconCount.demandMetWithLocal;
        int demandMetInFactionImports = (int)Math.min(iconCount.inFactionOnlyExport, totalDemand);

        final int nonDemandExport = iconCount.nonDemandExport;
        final int imports = iconCount.imports;
        final int extra = iconCount.extra;
        final int deficit = iconCount.deficit;

        float localProducedRatio = demandMetLocal/totalTarget;
        float inFactionImportRatio = demandMetInFactionImports/totalTarget;
        float externalImportRatio = (imports-demandMetInFactionImports)/totalTarget;
        float exportedRatio = extra/totalTarget;
        float deficitRatio = deficit/totalTarget;

        int iconsForLocal = Math.round(totalIcons * localProducedRatio);
        int iconsForInFactionImport = Math.round(totalIcons * inFactionImportRatio);
        int iconsForOutFactionImport = Math.round(totalIcons * externalImportRatio);
        int iconsForExport = Math.round(totalIcons * exportedRatio);
        int iconsForDeficit = Math.round(totalIcons * deficitRatio);

        // Clamp to not exceed totalIcons:
        int usedIcons = 0;

        usedIcons += addIconsToGroup(m_com, iconsForExport, IconRenderMode.GREEN);
        usedIcons += addIconsToGroup(m_com, iconsForLocal, IconRenderMode.OUTLINE_GREEN);
        usedIcons += addIconsToGroup(m_com, iconsForInFactionImport, IconRenderMode.NORMAL);
        usedIcons += addIconsToGroup(m_com, iconsForOutFactionImport, IconRenderMode.OUTLINE_CUSTOM);
        usedIcons += addIconsToGroup(m_com, iconsForDeficit, IconRenderMode.DIM_RED);

        // Fill with blanks to make exactly totalIcons
        while (usedIcons < totalIcons) {
            addIconsToGroup(m_com, 1, IconRenderMode.BLACK);
            usedIcons++;
        }

        // Position group as before
        float var16 = this.getHeight();
        float varSpacing = 3.0F;
        float var17 = 10.0F;
        m_iconGroup.autoSizeWithAdjust(this.getHeight(), this.getWidth() - var16 * 2.0F - varSpacing * 2.0F - var16 - varSpacing - var17, this.getHeight(), this.getHeight());
        this.add(m_iconGroup).inLMid(var16 + varSpacing);
    }

    private int addIconsToGroup(CommodityOnMarketAPI com, int count, IconRenderMode mode) {
        for (int i = 0; i < count; i++) {
            m_iconGroup.addGroup(com, 1, 1.0f, convertIconRenderMode(mode), null);
        }
        return count;
    }

    private com.fs.starfarer.campaign.ui.marketinfo.f.o convertIconRenderMode(IconRenderMode mode) {
        com.fs.starfarer.campaign.ui.marketinfo.f.o[] values = com.fs.starfarer.campaign.ui.marketinfo.f.o.values();
        if (mode.ordinal() < values.length) {
            return values[mode.ordinal()];
        } else {
            return values[0];
        }
    }
}
package wfg.ltv_econ.intel;

import java.awt.Color;

import static wfg.ltv_econ.constants.EconomyConstants.MONTH;
import static wfg.native_ui.util.Globals.settings;
import static wfg.native_ui.util.UIConstants.*;

import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.impl.campaign.intel.BaseIntelPlugin;
import com.fs.starfarer.api.ui.SectorMapAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;

import wfg.ltv_econ.economy.PlayerMarketData;

public class PlayerMarketBombardedIntel extends BaseIntelPlugin {
    private static final String ICON = settings.getSpriteName("icons", "bombardment");

    private final PlayerMarketData data;
    private final double stockpileReduction;
    private final float statsReduction;
    private final boolean isSaturation;

    public PlayerMarketBombardedIntel(PlayerMarketData data, double stockpileReduction, float statsReduction, boolean isSaturation) {
        this.data = data;
        this.stockpileReduction = stockpileReduction;
        this.statsReduction = statsReduction;
        this.isSaturation = isSaturation;

        endingTimeRemaining = (float) MONTH;
    }

    @Override
    public final String getSmallDescriptionTitle() {
        return isSaturation
            ? "Saturation Bombardment - " + data.market.getName()
            : "Tactical Bombardment - " + data.market.getName();
    }
    
    @Override
    public final SectorEntityToken getMapLocation(SectorMapAPI map) {
		return data.market.getPrimaryEntity();
	}

    @Override
    public final void createSmallDescription(TooltipMakerAPI tp, float width, float height) {
        tp.addPara("%s was " + (isSaturation ? "saturation" : "tactically") + " bombarded. Destroyed %s of local stockpiles and " +
            "reduced population health and happiness by %s.", 0f,
            new Color[] {highlight, negative, negative},
            data.market.getName(), Integer.toString((int) stockpileReduction) + "%", Integer.toString((int) statsReduction)
        );
    }

    public final String getIcon() { return ICON;}
    public final boolean isImportant() { return true;}
    public final IntelSortTier getSortTier() { return IntelSortTier.TIER_1;}
    public final boolean isEnding() { return true;}
    protected final String getName() { return getSmallDescriptionTitle();}
}
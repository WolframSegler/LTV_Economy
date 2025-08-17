package wfg_ltv_econ.commodities;

import java.util.Set;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.ModSpecAPI;
import com.fs.starfarer.api.campaign.SpecialItemData;
import com.fs.starfarer.api.campaign.SpecialItemSpecAPI;
import com.fs.starfarer.api.campaign.econ.CommodityMarketDataAPI;
import com.fs.starfarer.api.campaign.econ.CommodityOnMarketAPI;
import com.fs.starfarer.api.campaign.econ.CommoditySpecAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.campaign.econ.MarketDemandAPI;
import com.fs.starfarer.api.campaign.econ.PriceVariability;
import com.fs.starfarer.api.combat.MutableStat;
import com.fs.starfarer.api.combat.MutableStatWithTempMods;
import com.fs.starfarer.api.combat.StatBonus;
import com.fs.starfarer.api.impl.campaign.rulecmd.salvage.MarketCMD.RaidDangerLevel;
import com.fs.starfarer.campaign.econ.CommodityOnMarket;

/**
 * <p>This class exists as a thin wrapper around <code>SpecialItemData</code> to make it
 * compatible with the game's <code>CommodityOnMarketAPI</code> interface. 
 * <code>TooltipMakerAPI</code> expects commodities to render icons with a neat border
 * using <code>addIcons</code>/<code>addIconGroup</code>. However, <code>SpecialItemData</code>
 * objects are <strong>not</strong> real commodities and cannot be directly passed to
 * <code>addIcons</code>.</p>
 *
 * <p>Instead of using reflection or dealing with obfuscation to access the original wrapper,
 * this class replicates the essential behavior of the original.
 * It implements <code>CommodityOnMarketAPI</code> but only provides meaningful data for
 * rendering the icon (via <code>getCommodity()</code>).</p>
 *
 * <h4>Usage</h4>
 * <pre>
 * tp.addIcons(new SpecialItemCommodityWrapper(specialItemData), 1, IconRenderMode.NORMAL);
 * </pre>
 */
public class SpecialItemCommodityWrapper implements CommodityOnMarketAPI {
    private CommoditySpec m_Itemspec;

    public class CommoditySpec implements CommoditySpecAPI {
        private SpecialItemSpecAPI m_spec;

        public CommoditySpec(SpecialItemData item) {
            m_spec = Global.getSettings().getSpecialItemSpec(item.getId());
        }

        public float getBasePrice() {
            return 0.0F;
        }

        public float getCargoSpace() {
            return 0.0F;
        }

        public String getDemandClass() {
            return null;
        }

        public float getEconUnit() {
            return 0.0F;
        }

        public float getEconomyTier() {
            return 0.0F;
        }

        public String getIconLargeName() {
            return null;
        }

        public String getIconName() {
            return m_spec.getIconName();
        }

        public String getId() {
            return null;
        }

        public String getName() {
            return null;
        }

        public float getOrder() {
            return 0.0F;
        }

        public String getOrigin() {
            return null;
        }

        public PriceVariability getPriceVariability() {
            return null;
        }

        public int getStackSize() {
            return 0;
        }

        public Set<String> getTags() {
            return null;
        }

        public float getUtility() {
            return 0.0F;
        }

        public boolean hasTag(String var1) {
            return false;
        }

        public boolean isExotic() {
            return false;
        }

        public boolean isFuel() {
            return false;
        }

        public boolean isMeta() {
            return false;
        }

        public boolean isPersonnel() {
            return false;
        }

        public boolean isPrimary() {
            return false;
        }

        public boolean isSupplies() {
            return false;
        }

        public float getIconWidthMult() {
            return 1.0F;
        }

        public String getSoundIdDrop() {
            return m_spec.getSoundIdDrop();
        }

        public String getSoundId() {
            return m_spec.getSoundId();
        }

        public String getLowerCaseName() {
            return null;
        }

        public float getExportValue() {
            return 0.0F;
        }

        public void setExportValue(float var1) {
        }

        public void setBasePrice(float var1) {
        }

        public RaidDangerLevel getBaseDanger() {
            return RaidDangerLevel.MEDIUM;
        }

        public void setBaseDanger(RaidDangerLevel var1) {
        }

        public boolean isNonEcon() {
            return false;
        }

        public void setName(String var1) {
        }

        public void setIconName(String var1) {
        }

        public void setOrder(float var1) {
        }

        public void setDemandClass(String var1) {
        }

        public ModSpecAPI getSourceMod() {
            return null;
        }
    }

    public SpecialItemCommodityWrapper(SpecialItemData item) {
        this.m_Itemspec = new CommoditySpec(item);
    }

    public void addTradeMod(String var1, float var2, float var3, String var4) {
    }

    public void addTradeMod(String var1, float var2, float var3) {
    }

    public void addToStockpile(float var1) {
    }

    public int getAvailable() {
        return 0;
    }

    public MutableStatWithTempMods getAvailableStat() {
        return null;
    }

    public CommoditySpecAPI getCommodity() {
        return this.m_Itemspec;
    }

    public MarketDemandAPI getDemand() {
        return null;
    }

    public String getDemandClass() {
        return null;
    }

    public MutableStatWithTempMods getTradeMod() {
        return null;
    }

    public int getExportIncome() {
        return 0;
    }

    public MutableStat getGreed() {
        return null;
    }

    public float getGreedValue() {
        return 0.0F;
    }

    public String getId() {
        return null;
    }

    public MarketAPI getMarket() {
        return null;
    }

    public int getMaxDemand() {
        return 0;
    }

    public int getMaxSupply() {
        return 0;
    }

    public float getModValueForQuantity(float var1) {
        return 0.0F;
    }

    public StatBonus getPlayerPriceMod() {
        return null;
    }

    public float getQuantityForModValue(float var1) {
        return 0.0F;
    }

    public float getStockpile() {
        return 0.0F;
    }

    public float getUtilityOnMarket() {
        return 0.0F;
    }

    public boolean isDemandLegal() {
        return false;
    }

    public boolean isExporting() {
        return false;
    }

    public boolean isFuel() {
        return false;
    }

    public boolean isIllegal() {
        return false;
    }

    public boolean isIllegalAssumePrimary() {
        return false;
    }

    public boolean isImporting() {
        return false;
    }

    public boolean isNonEcon() {
        return false;
    }

    public boolean isPersonnel() {
        return false;
    }

    public boolean isSupplyLegal() {
        return false;
    }

    public void reapplyEventMod() {
    }

    public void removeFromStockpile(float var1) {
    }

    public void setDemandLegal(boolean var1) {
    }

    public void setMaxDemand(int var1) {
    }

    public void setMaxSupply(int var1) {
    }

    public void setStockpile(float var1) {
    }

    public void setSupplyLegal(boolean var1) {
    }

    public void updateMaxSupplyAndDemand() {
    }

    public void addTradeModMinus(String var1, float var2, float var3) {
    }

    public void addTradeModPlus(String var1, float var2, float var3) {
    }

    public MutableStatWithTempMods getTradeModMinus() {
        return null;
    }

    public MutableStatWithTempMods getTradeModPlus() {
        return null;
    }

    public float getCombinedTradeModQuantity() {
        return 0.0F;
    }

    public boolean isMeta() {
        return false;
    }

    public int getDemandValue() {
        return 0;
    }

    public CommodityMarketDataAPI getCommodityMarketData() {
        return null;
    }

    public int getDeficitQuantity() {
        return 0;
    }

    public int getExcessQuantity() {
        return 0;
    }

    public int getPlayerTradeNetQuantity() {
        return 0;
    }

    public StatBonus getPlayerDemandPriceMod() {
        return null;
    }

    public StatBonus getPlayerSupplyPriceMod() {
        return null;
    }
}

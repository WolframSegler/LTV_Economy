package wfg.ltv_econ.plugins;

import com.fs.starfarer.api.combat.MutableStat;
import com.fs.starfarer.api.combat.MutableStatWithTempMods;
import com.fs.starfarer.api.combat.StatBonus;
import com.fs.starfarer.campaign.econ.CommodityOnMarket;
import com.fs.starfarer.campaign.econ.Market;
import com.fs.starfarer.campaign.econ.MarketDemand;
import com.fs.starfarer.campaign.econ.PriceCalculator;
import com.fs.starfarer.campaign.econ.reach.CommodityMarketData;

import rolflectionlib.util.RolfLectionUtil;
import wfg.ltv_econ.economy.CommodityCell;
import wfg.ltv_econ.economy.engine.EconomyEngine;

public class CommodityOnMarketWrapper extends CommodityOnMarket {

    final CommodityOnMarket org;

    public CommodityOnMarketWrapper(CommodityOnMarket org) {
        super(org.getMarket(), org.getId());
        this.org = org;
    }

    // MODIFIED FUNCTIONS
    public int getDeficitQuantity() {
        final CommodityCell cell = EconomyEngine.getInstance().getComCell(getId(), getMarket().getId());
        if (cell == null) return org.getDeficitQuantity();

        return (int) cell.getStoredDeficit();
    }

    public int getExcessQuantity() {
        final CommodityCell cell = EconomyEngine.getInstance().getComCell(getId(), getMarket().getId());
        if (cell == null) return org.getExcessQuantity();

        return (int) cell.getStoredExcess();
    }


    // WRAPPER DELEGATORS
    @SuppressWarnings("all")
    Object readResolve() {
        return RolfLectionUtil.getMethodDeclaredAndInvokeDirectly(
            "readResolve", org);
    }

    public CommodityMarketData getCommodityMarketData() {
        return org.getCommodityMarketData();
    }

    public void setCommodityMarketData(CommodityMarketData var1) {
        org.setCommodityMarketData(var1);
    }

    public boolean isSupplyLegal() {
        return org.isSupplyLegal();
    }

    public void setSupplyLegal(boolean var1) {
        org.setSupplyLegal(var1);
    }

    public boolean isDemandLegal() {
        return org.isDemandLegal();
    }

    public void setDemandLegal(boolean var1) {
        org.setDemandLegal(var1);
    }

    public int getExportIncome() {
        return org.getExportIncome();
    }

    public int getDemandValue() {
        return org.getDemandValue();
    }

    public boolean isNonEcon() {
        return org.isNonEcon();
    }

    public StatBonus getPlayerDemandPriceMod() {
        return org.getPlayerDemandPriceMod();
    }

    public StatBonus getPlayerSupplyPriceMod() {
        return org.getPlayerSupplyPriceMod();
    }

    public String getId() {
        return org.getId();
    }

    public String toString() {
        return org.toString();
    }

    public void updateCalc() {
        org.updateCalc();
    }

    public PriceCalculator getDemandPrice() {
        return org.getDemandPrice();
    }

    public PriceCalculator getSupplyPrice() {
        return org.getSupplyPrice();
    }

    public MutableStat getGreed() {
        return org.getGreed();
    }

    public float getGreedValue() {
        return org.getGreedValue();
    }

    public MarketDemand getDemand() {
        return org.getDemand();
    }

    public String getDemandClass() {
        return org.getDemandClass();
    }

    public float getStockpile() {
        return org.getStockpile();
    }

    public void setStockpile(float var1) {
        org.setStockpile(var1);
    }

    public void addToStockpile(float var1) {
        org.addToStockpile(var1);
    }

    public void removeFromStockpile(float var1) {
        org.removeFromStockpile(var1);
    }

    public Market getMarket() {
        return org.getMarket();
    }

    public float getUtilityOnMarket() {
        return org.getUtilityOnMarket();
    }

    public boolean isPersonnel() {
        return org.isPersonnel();
    }

    public boolean isFuel() {
        return org.isFuel();
    }

    public int getAvailable() {
        return org.getAvailable();
    }

    public MutableStatWithTempMods getAvailableStat() {
        return org.getAvailableStat();
    }

    public MutableStatWithTempMods getTradeMod() {
        return org.getTradeMod();
    }

    public MutableStatWithTempMods getTradeModPlus() {
        return org.getTradeModPlus();
    }

    public MutableStatWithTempMods getTradeModMinus() {
        return org.getTradeModMinus();
    }

    public void addTradeMod(String var1, float var2, float var3) {
        org.addTradeMod(var1, var2, var3);
    }

    public void addTradeModPlus(String var1, float var2, float var3) {
        org.addTradeModPlus(var1, var2, var3);
    }

    public void addTradeModMinus(String var1, float var2, float var3) {
        org.addTradeModMinus(var1, var2, var3);
    }

    public float getCombinedTradeModQuantity() {
        return org.getCombinedTradeModQuantity();
    }

    public void reapplyEventMod() {
        org.reapplyEventMod();
    }

    public float getModValueForQuantity(float var1) {
        return org.getModValueForQuantity(var1);
    }

    public float getQuantityForModValue(float var1) {
        return org.getModValueForQuantity(var1);
    }

    public int getMaxSupply() {
        return org.getMaxSupply();
    }

    public void setMaxSupply(int var1) {
        org.setMaxSupply(var1);
    }

    public int getMaxDemand() {
        return org.getMaxDemand();
    }

    public void setMaxDemand(int var1) {
        org.setMaxDemand(var1);
    }

    public boolean isMeta() {
        return org.isMeta();
    }

    public void updateMaxSupplyAndDemand() {
        org.updateMaxSupplyAndDemand();
    }

    public boolean isIllegal() {
        return org.isIllegal();
    }

    public boolean isIllegalAssumePrimary() {
        return org.isIllegalAssumePrimary();
    }

    public int getPlayerTradeNetQuantity() {
        return org.getPlayerTradeNetQuantity();
    }
}
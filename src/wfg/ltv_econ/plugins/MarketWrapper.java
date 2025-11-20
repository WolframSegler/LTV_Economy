package wfg.ltv_econ.plugins;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.awt.Color;

import org.lwjgl.util.vector.Vector2f;

import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.campaign.FactionAPI.ShipPickParams;
import com.fs.starfarer.api.campaign.LocationAPI;
import com.fs.starfarer.api.campaign.PlanetAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.campaign.econ.CommodityOnMarketAPI;
import com.fs.starfarer.api.campaign.econ.CommoditySpecAPI;
import com.fs.starfarer.api.campaign.econ.Industry;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.campaign.econ.MarketConditionAPI;
import com.fs.starfarer.api.campaign.econ.MarketDemandAPI;
import com.fs.starfarer.api.campaign.econ.MarketImmigrationModifier;
import com.fs.starfarer.api.campaign.econ.SubmarketAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.combat.MutableStat;
import com.fs.starfarer.api.combat.MutableStatWithTempMods;
import com.fs.starfarer.api.combat.StatBonus;
import com.fs.starfarer.api.fleet.ShipFilter;
import com.fs.starfarer.api.fleet.ShipRolePick;
import com.fs.starfarer.api.impl.campaign.econ.impl.ConstructionQueue;
import com.fs.starfarer.api.impl.campaign.population.PopulationComposition;
import com.fs.starfarer.campaign.CommDirectory;
import com.fs.starfarer.campaign.Faction;
import com.fs.starfarer.campaign.econ.CommodityOnMarket;
import com.fs.starfarer.campaign.econ.Economy;
import com.fs.starfarer.campaign.econ.Market;
import com.fs.starfarer.campaign.econ.MarketDemand;
import com.fs.starfarer.campaign.econ.MarketDemandData;
import com.fs.starfarer.campaign.econ.PlanetConditionMarket;
import com.fs.starfarer.campaign.econ.Submarket;
import com.fs.starfarer.campaign.fleet.MutableMarketStats;
import com.fs.starfarer.rpg.Person;

import wfg.reflection.ReflectionUtils;

public class MarketWrapper extends Market {
    public final Market original;

    public MarketWrapper(Market original) {
        super(original.getId(), original.getName(), original.getSize(), original.getEconomy());

        this.original = original;
    }

    public int hashCode() {
        return original.hashCode();
    }

    public boolean equals(Object var1) {
        return original.equals(var1);
    }

    public String getEconGroup() {
        return original.getEconGroup();
    }

    public void setEconGroup(String var1) {
        original.setEconGroup(var1);
    }

    public ConstructionQueue getConstructionQueue() {
        return original.getConstructionQueue();
    }

    public boolean isPlayerOwned() {
        return original.isPlayerOwned();
    }

    public boolean hasSpaceport() {
        return original.hasSpaceport();
    }

    public void setHasSpaceport(boolean var1) {
        original.setHasSpaceport(var1);
    }

    public void setPlayerOwned(boolean var1) {
        original.setPlayerOwned(var1);
    }

    public List<Industry> getIndustries() {
        return original.getIndustries();
    }

    public Industry getIndustry(String var1) {
        return original.getIndustry(var1);
    }

    public boolean hasIndustry(String var1) {
        return original.hasIndustry(var1);
    }

    public boolean hasFunctionalIndustry(String var1) {
        return original.hasFunctionalIndustry(var1);
    }

    public void addIndustry(String var1) {
        original.addIndustry(var1);
    }

    public void addIndustry(String var1, List<String> var2) {
        original.addIndustry(var1, var2);
    }

    public Industry instantiateIndustry(String var1) {
        return original.instantiateIndustry(var1);
    }

    public void removeIndustry(String var1, MarketInteractionMode var2, boolean var3) {
        original.removeIndustry(var1, var2, var3);
    }

    public void reapplyIndustries() {
        original.reapplyIndustries();
    }

    public boolean isInEconomy() {
        return original.isInEconomy();
    }

    public CommDirectory getCommDirectory() {
        return original.getCommDirectory();
    }

    public Person getAdmin() {
        return original.getAdmin();
    }

    public void setAdmin(PersonAPI var1) {
        original.setAdmin(var1);
    }

    public Set<Person> getPeople() {
        return original.getPeople();
    }

    public void addPerson(PersonAPI var1) {
        original.addPerson(var1);
    }

    public void removePerson(PersonAPI var1) {
        original.removePerson(var1);
    }

    public List<PersonAPI> getPeopleCopy() {
        return original.getPeopleCopy();
    }

    public void setMemory(MemoryAPI var1) {
        original.setMemory(var1);
    }

    public MemoryAPI getMemory() {
        return original.getMemory();
    }

    public MemoryAPI getMemoryWithoutUpdate() {
        return original.getMemoryWithoutUpdate();
    }

    public float getPrevStability() {
        return original.getPrevStability();
    }

    public void updatePrevStability() {
        original.updatePrevStability();
    }

    public float getDaysInExistence() {
        return original.getDaysInExistence();
    }

    public void setDaysInExistence(float var1) {
        original.setDaysInExistence(var1);
    }

    public void advance(float var1) {
        original.advance(var1);
    }

    public SectorEntityToken getPrimaryEntity() {
        return original.getPrimaryEntity();
    }

    public PlanetAPI getPlanetEntity() {
        return original.getPlanetEntity();
    }

    public void setPrimaryEntity(SectorEntityToken var1) {
        original.setPrimaryEntity(var1);
    }

    public Set<SectorEntityToken> getConnectedEntities() {
        return original.getConnectedEntities();
    }

    public void addSubmarket(SubmarketAPI var1) {
        original.addSubmarket(var1);
    }

    public void addSubmarket(String var1) {
        original.addSubmarket(var1);
    }

    public Submarket getSubmarket(String var1) {
        return original.getSubmarket(var1);
    }

    public boolean hasSubmarket(String var1) {
        return hasSubmarket(var1);
    }

    public List<Submarket> getSubmarkets() {
        return original.getSubmarkets();
    }

    public List<SubmarketAPI> getSubmarketsCopy() {
        return original.getSubmarketsCopy();
    }

    public void removeSubmarket(String var1) {
        original.removeSubmarket(var1);
    }

    public MutableStat getIncomeMult() {
        return original.getIncomeMult();
    }

    public MutableStat getUpkeepMult() {
        return original.getUpkeepMult();
    }

    public MutableStat getHazard() {
        return original.getHazard();
    }

    public float getHazardValue() {
        return original.getHazardValue();
    }

    public MutableStatWithTempMods getStability() {
        return original.getStability();
    }

    public float getStabilityValue() {
        return getStabilityValue();
    }

    public String getFactionId() {
        return getFactionId();
    }

    public void setCachedFaction(FactionAPI var1) {
        original.setCachedFaction(var1);
    }

    public Faction getFaction() {
        return original.getFaction();
    }

    public void setFactionId(String var1) {
        original.setFactionId(var1);
    }

    public MutableMarketStats getStats() {
        return original.getStats();
    }

    public StatBonus getAccessibilityMod() {
        return original.getAccessibilityMod();
    }

    private Object readResolve() {
        return ReflectionUtils.invoke(original, "readResolve");
    }

    public boolean isUseStockpilesForShortages() {
        return original.isUseStockpilesForShortages();
    }

    public void setUseStockpilesForShortages(boolean var1) {
        original.setUseStockpilesForShortages(var1);
    }

    public void clearCommodities() {
        original.clearCommodities();
    }

    private Object writeReplace() {
        return ReflectionUtils.invoke(original, "writeReplace");
    }

    public Market clone() {
        return original.clone();
    }

    public MutableStat getTariff() {
        return original.getTariff();
    }

    public StatBonus getDemandPriceMod() {
        return original.getDemandPriceMod();
    }

    public StatBonus getSupplyPriceMod() {
        return original.getSupplyPriceMod();
    }

    public Economy getEconomy() {
        return original.getEconomy();
    }

    public String getId() {
        return original.getId();
    }

    public void setId(String var1) {
        original.setId(var1);
    }

    public String getName() {
        return original.getName();
    }

    public void setName(String var1) {
        original.setName(var1);
    }

    public List<CommodityOnMarket> getCommodities() {
        return original.getCommodities();
    }

    public List<CommodityOnMarketAPI> getCommoditiesCopy() {
        return original.getCommoditiesCopy();
    }

    public void updatePrices() {
        original.updatePrices();
    }

    public List<CommodityOnMarket> getCommoditiesWithClass(String var1) {
        return original.getCommoditiesWithClass(var1);
    }

    public CommodityOnMarket getCommodityData(CommoditySpecAPI var1) {
        return getCommodityData(var1);
    }

    public CommodityOnMarket getCommodityData(String var1) {
        return original.getCommodityData(var1);
    }

    public float getDemandPrice(String var1, double var2, boolean var4) {
        return this.getDemandPriceAssumingExistingTransaction(var1, var2, 0.0D, var4);
    }

    public float getDemandPriceAssumingExistingTransaction(String var1, double var2, double var4, boolean var6) {
        return 0;
    }

    public float getDemandPriceAssumingStockpileUtility(
        CommodityOnMarket var1, double var2, double var4, boolean var6
    ) {
        return 0;
    }

    public float getSupplyPrice(String var1, double var2, boolean var4) {
        return this.getSupplyPriceAssumingExistingTransaction(var1, var2, 0.0D, var4);
    }

    public float getSupplyPriceAssumingExistingTransaction(String var1, double var2, double var4, boolean var6) {
        return 0;
    }

    public float getSupplyPriceAssumingStockpileUtility(
        CommodityOnMarket var1, double var2, double var4, boolean var6
    ) {
        return 0;
    }

    public MarketDemandData getDemandData() {
        return original.getDemandData();
    }

    public String toString() {
        return original.toString();
    }

    public Vector2f getLocation() {
        return original.getLocation();
    }

    public Vector2f getSimDisplayLocation() {
        return original.getSimDisplayLocation();
    }

    public int getSize() {
        return original.getSize();
    }

    public void setSize(int var1) {
        original.setSize(var1);
    }

    public List<CommodityOnMarketAPI> getCommoditiesWithTags(String... var1) {
        return original.getCommoditiesWithTags(var1);
    }

    public List<CommodityOnMarketAPI> getCommoditiesWithTag(String var1) {
        return original.getCommoditiesWithTag(var1);
    }

    public List<CommodityOnMarketAPI> getAllCommodities() {
        return original.getAllCommodities();
    }

    public MarketDemand getDemand(String var1) {
        return original.getDemand(var1);
    }

    public List<MarketDemandAPI> getDemandWithTag(String var1) {
        return original.getDemandWithTag(var1);
    }

    public SurveyLevel getSurveyLevel() {
        return original.getSurveyLevel();
    }

    public void setSurveyLevel(SurveyLevel var1) {
        original.setSurveyLevel(var1);
    }

    public List<MarketConditionAPI> getConditions() {
        return original.getConditions();
    }

    public MarketConditionAPI getCondition(String var1) {
        return original.getCondition(var1);
    }

    public void setRetainSuppressedConditionsSetWhenEmpty(Boolean var1) {
        original.setRetainSuppressedConditionsSetWhenEmpty(var1);
    }

    public LinkedHashSet<String> getSuppressedConditions() {
        return original.getSuppressedConditions();
    }

    public boolean isConditionSuppressed(String var1) {
        return original.isConditionSuppressed(var1);
    }

    public void suppressCondition(String var1) {
        original.suppressCondition(var1);
    }

    public void unsuppressCondition(String var1) {
        original.unsuppressCondition(var1);
    }

    public void setSuppressedConditions(LinkedHashSet<String> var1) {
        original.setSuppressedConditions(var1);
    }

    public String addCondition(String var1) {
        return addCondition(var1);
    }

    public String addCondition(String var1, Object var2) {
        return original.addCondition(var1, var2);
    }

    public void addCondition(MarketConditionAPI var1) {
        original.addCondition(var1);
    }

    public boolean hasCondition(String var1) {
        return original.hasCondition(var1);
    }

    public boolean hasSpecificCondition(String var1) {
        return original.hasSpecificCondition(var1);
    }

    public MarketConditionAPI getFirstCondition(String var1) {
        return original.getFirstCondition(var1);
    }

    public MarketConditionAPI getSpecificCondition(String var1) {
        return original.getSpecificCondition(var1);
    }

    public void reapplyConditions() {
        original.reapplyConditions();
    }

    public void reapplyCondition(String var1) {
        original.reapplyCondition(var1);
    }

    public void removeCondition(String var1) {
        original.removeCondition(var1);
    }

    public void removeSpecificCondition(String var1) {
        original.removeSpecificCondition(var1);
    }

    public boolean isIllegal(String var1) {
        return original.isIllegal(var1);
    }

    public boolean isIllegal(CommodityOnMarketAPI var1) {
        return original.isIllegal(var1);
    }

    public void updatePriceMult() {
        original.updatePriceMult();
    }

    public float pickShipAndAddToFleet(String var1, ShipPickParams var2, CampaignFleetAPI var3) {
        return original.pickShipAndAddToFleet(var1, var2, var3);
    }

    public float pickShipAndAddToFleet(String var1, String var2, ShipPickParams var3, CampaignFleetAPI var4) {
        return original.pickShipAndAddToFleet(var1, var2, var3, var4);
    }

    public List<ShipRolePick> pickShipsForRole(String var1, ShipPickParams var2, Random var3, ShipFilter var4) {
        return original.pickShipsForRole(var1, var2, var3, var4);
    }

    public List<ShipRolePick> pickShipsForRole(String var1, String var2, ShipPickParams var3, Random var4,
            ShipFilter var5) {
        return original.pickShipsForRole(var1, var2, var3, var4, var5);
    }

    public float getShipQualityFactor() {
        return original.getShipQualityFactor();
    }

    public StarSystemAPI getStarSystem() {
        return original.getStarSystem();
    }

    public LocationAPI getContainingLocation() {
        return original.getContainingLocation();
    }

    public Vector2f getLocationInHyperspace() {
        return original.getLocationInHyperspace();
    }

    public boolean isPlanetConditionMarketOnly() {
        return original.isPlanetConditionMarketOnly();
    }

    public void setPlanetConditionMarketOnly(boolean var1) {
        original.setPlanetConditionMarketOnly(var1);
    }

    public PlanetConditionMarket convertToCondition() {
        return original.convertToCondition();
    }

    public boolean isForceNoConvertOnSave() {
        return original.isForceNoConvertOnSave();
    }

    public void setForceNoConvertOnSave(boolean var1) {
        original.setForceNoConvertOnSave(var1);
    }

    public boolean isHostile(MarketAPI var1) {
        return original.isHostile(var1);
    }

    public void clearHostileCache() {
        original.clearHostileCache();
    }

    public float getIndustryUpkeep() {
        return original.getIndustryUpkeep();
    }

    public float getIndustryIncome() {
        return original.getIndustryIncome();
    }

    public float getExportIncome(boolean var1) {
        return original.getExportIncome(var1);
    }

    public float getNetIncome() {
        return original.getNetIncome();
    }

    public float getGrossIncome() {
        return original.getGrossIncome();
    }

    public float getImmigrationIncentivesCost() {
        return original.getImmigrationIncentivesCost();
    }

    public float getShortageCounteringCost() {
        return original.getShortageCounteringCost();
    }

    public boolean hasWaystation() {
        return original.hasWaystation();
    }

    public void setHasWaystation(boolean var1) {
        original.setHasWaystation(var1);
    }

    public PopulationComposition getPopulation() {
        return original.getPopulation();
    }

    public boolean wasIncomingSetBefore() {
        return original.wasIncomingSetBefore();
    }

    public PopulationComposition getIncoming() {
        return original.getIncoming();
    }

    public void setPopulation(PopulationComposition var1) {
        original.setPopulation(var1);
    }

    public void setIncoming(PopulationComposition var1) {
        original.setIncoming(var1);
    }

    public LinkedHashSet<MarketImmigrationModifier> getImmigrationModifiers() {
        return original.getImmigrationModifiers();
    }

    public LinkedHashSet<MarketImmigrationModifier> getTransientImmigrationModifiers() {
        return original.getTransientImmigrationModifiers();
    }

    public void addImmigrationModifier(MarketImmigrationModifier var1) {
        original.addImmigrationModifier(var1);
    }

    public void removeImmigrationModifier(MarketImmigrationModifier var1) {
        original.removeImmigrationModifier(var1);
    }

    public void addTransientImmigrationModifier(MarketImmigrationModifier var1) {
        original.addTransientImmigrationModifier(var1);
    }

    public void removeTransientImmigrationModifier(MarketImmigrationModifier var1) {
        original.removeTransientImmigrationModifier(var1);
    }

    public List<MarketImmigrationModifier> getAllImmigrationModifiers() {
        return original.getAllImmigrationModifiers();
    }

    public boolean isAllowImport() {
        return original.isAllowImport();
    }

    public void setAllowImport(boolean var1) {
        original.setAllowImport(var1);
    }

    public boolean isAllowExport() {
        return original.isAllowExport();
    }

    public void setAllowExport(boolean var1) {
        original.setAllowExport(var1);
    }

    public float getIncentiveCredits() {
        return original.getIncentiveCredits();
    }

    public void setIncentiveCredits(float var1) {
        original.setIncentiveCredits(var1);
    }

    public boolean isFreePort() {
        return original.isFreePort();
    }

    public void setFreePort(boolean var1) {
        original.setFreePort(var1);
    }

    public boolean isImmigrationClosed() {
        return original.isImmigrationClosed();
    }

    public void setImmigrationClosed(boolean var1) {
        original.setImmigrationClosed(var1);
    }

    public boolean hasTag(String var1) {
        return original.hasTag(var1);
    }

    public void addTag(String var1) {
        original.addTag(var1);
    }

    public void removeTag(String var1) {
        original.removeTag(var1);
    }

    public Collection<String> getTags() {
        return original.getTags();
    }

    public void clearTags() {
        original.clearTags();
    }

    public String getOnOrAt() {
        return original.getOnOrAt();
    }

    public Color getTextColorForFactionOrPlanet() {
        return original.getTextColorForFactionOrPlanet();
    }

    public Color getDarkColorForFactionOrPlanet() {
        return original.getDarkColorForFactionOrPlanet();
    }

    public boolean isHidden() {
        return original.isHidden();
    }

    public void setHidden(Boolean var1) {
        original.setHidden(var1);
    }

    public boolean isInvalidMissionTarget() {
        return original.isInvalidMissionTarget();
    }

    public void setInvalidMissionTarget(Boolean var1) {
        original.setInvalidMissionTarget(var1);
    }

    public boolean isImmigrationIncentivesOn() {
        return original.isImmigrationIncentivesOn();
    }

    public void setImmigrationIncentivesOn(Boolean var1) {
        original.setImmigrationIncentivesOn(var1);
    }
}
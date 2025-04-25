package wfg_ltv_econ.submarkets;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.CargoAPI;
import com.fs.starfarer.api.campaign.CargoStackAPI;
import com.fs.starfarer.api.campaign.CoreUIAPI;
import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.campaign.FactionDoctrineAPI;
import com.fs.starfarer.api.campaign.FleetDataAPI;
import com.fs.starfarer.api.campaign.PlayerMarketTransaction;
import com.fs.starfarer.api.campaign.SpecialItemData;
import com.fs.starfarer.api.campaign.SubmarketPlugin;
import com.fs.starfarer.api.campaign.CampaignUIAPI.CoreUITradeMode;
import com.fs.starfarer.api.campaign.CargoAPI.CargoItemType;
import com.fs.starfarer.api.campaign.econ.CommodityOnMarketAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.campaign.econ.SubmarketAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.combat.WeaponAPI.AIHints;
import com.fs.starfarer.api.combat.WeaponAPI.WeaponSize;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.fleet.FleetMemberType;
import com.fs.starfarer.api.impl.campaign.DModManager;
import com.fs.starfarer.api.impl.campaign.fleets.DefaultFleetInflater;
import com.fs.starfarer.api.impl.campaign.fleets.FleetFactoryV3;
import com.fs.starfarer.api.impl.campaign.fleets.FleetParamsV3;
import com.fs.starfarer.api.impl.campaign.shared.SharedData;
import com.fs.starfarer.api.loading.FighterWingSpecAPI;
import com.fs.starfarer.api.loading.HullModSpecAPI;
import com.fs.starfarer.api.loading.WeaponSpecAPI;
import com.fs.starfarer.api.plugins.impl.CoreAutofitPlugin;
import com.fs.starfarer.api.ui.LabelAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Highlights;
import com.fs.starfarer.api.util.Misc;
import com.fs.starfarer.api.util.WeightedRandomPicker;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class LtvBaseSubmarketPlugin implements SubmarketPlugin {
   public static float TRADE_IMPACT_DAYS = 120.0F;
   protected MarketAPI market;
   protected SubmarketAPI submarket;
   protected CargoAPI cargo;
   protected float minSWUpdateInterval = 30.0F;
   protected float sinceSWUpdate = 31.0F;
   protected float sinceLastCargoUpdate = 31.0F;
   protected Random itemGenRandom = new Random();

   public LtvBaseSubmarketPlugin() {
   }

   public void init(SubmarketAPI submarket) {
      this.submarket = submarket;
      this.market = submarket.getMarket();
   }

   protected Object readResolve() {
      return this;
   }

   public String getName() {
      return null;
   }

   public CargoAPI getCargo() {
      if (this.cargo == null) {
         this.cargo = Global.getFactory().createCargo(true);
         this.cargo.initMothballedShips(this.submarket.getFaction().getId());
      }

      return this.cargo;
   }

   public CargoAPI getCargoNullOk() {
      return this.cargo;
   }

   public void setCargo(CargoAPI cargo) {
      this.cargo = cargo;
   }

   public void updateCargoPrePlayerInteraction() {
   }

   public void advance(float amount) {
      float days = Global.getSector().getClock().convertToDays(amount);
      this.sinceLastCargoUpdate += days;
      this.sinceSWUpdate += days;
   }

   public boolean okToUpdateShipsAndWeapons() {
      return this.sinceSWUpdate >= this.minSWUpdateInterval;
   }

   public void addAllCargo(CargoAPI otherCargo) {
      Iterator<CargoStackAPI> var3 = otherCargo.getStacksCopy().iterator();

      while(var3.hasNext()) {
         CargoStackAPI stack = (CargoStackAPI)var3.next();
         if (!stack.isNull()) {
            this.getCargo().addItems(stack.getType(), stack.getData(), stack.getSize());
         }
      }

   }

   public float getTariff() {
      return this.market.getTariff().getModifiedValue();
   }

   public String getBuyVerb() {
      return "Buy";
   }

   public String getSellVerb() {
      return "Sell";
   }

   public boolean isFreeTransfer() {
      return false;
   }

   public boolean isEnabled(CoreUIAPI ui) {
      return ui.getTradeMode() == CoreUITradeMode.OPEN || this.isBlackMarket();
   }

   public SubmarketPlugin.OnClickAction getOnClickAction(CoreUIAPI ui) {
      return OnClickAction.OPEN_SUBMARKET;
   }

   public String getDialogText(CoreUIAPI ui) {
      return null;
   }

   public Highlights getDialogTextHighlights(CoreUIAPI ui) {
      return null;
   }

   public SubmarketPlugin.DialogOption[] getDialogOptions(CoreUIAPI ui) {
      return null;
   }

   public String getTooltipAppendix(CoreUIAPI ui) {
      return null;
   }

   public Highlights getTooltipAppendixHighlights(CoreUIAPI ui) {
      return null;
   }

   public SubmarketPlugin.PlayerEconomyImpactMode getPlayerEconomyImpactMode() {
      return PlayerEconomyImpactMode.NONE;
   }

   public float getPlayerTradeImpactMult() {
      return 1.0F;
   }

   public void reportPlayerMarketTransaction(PlayerMarketTransaction transaction) {
      if (this.isParticipatesInEconomy()) {
         SubmarketPlugin.PlayerEconomyImpactMode mode = this.getPlayerEconomyImpactMode();
         SharedData.getData().getPlayerActivityTracker().getPlayerTradeData(this.submarket).addTransaction(transaction);
         Iterator<CargoStackAPI> var4 = transaction.getSold().getStacksCopy().iterator();

         while(true) {
            while(true) {
               CargoStackAPI stack;
               float qty;
               CommodityOnMarketAPI com;
               do {
                  do {
                     if (!var4.hasNext()) {
                        var4 = transaction.getBought().getStacksCopy().iterator();

                        while(true) {
                           while(true) {
                              do {
                                 do {
                                    if (!var4.hasNext()) {
                                       return;
                                    }

                                    stack = (CargoStackAPI)var4.next();
                                 } while(!stack.isCommodityStack());

                                 qty = stack.getSize() * this.getPlayerTradeImpactMult();
                              } while(qty <= 0.0F);

                              com = this.market.getCommodityData(stack.getCommodityId());
                              if (mode == PlayerEconomyImpactMode.BOTH) {
                                 com.addTradeMod("buy_" + Misc.genUID(), -qty, TRADE_IMPACT_DAYS);
                              } else if (mode != PlayerEconomyImpactMode.PLAYER_SELL_ONLY && mode != PlayerEconomyImpactMode.NONE) {
                                 if (mode == PlayerEconomyImpactMode.PLAYER_BUY_ONLY) {
                                    com.addTradeModMinus("buy_" + Misc.genUID(), -qty, TRADE_IMPACT_DAYS);
                                 }
                              } else {
                                 com.addTradeModPlus("buy_" + Misc.genUID(), -qty, TRADE_IMPACT_DAYS);
                              }
                           }
                        }
                     }

                     stack = (CargoStackAPI)var4.next();
                  } while(!stack.isCommodityStack());

                  qty = stack.getSize() * this.getPlayerTradeImpactMult();
               } while(qty <= 0.0F);

               com = this.market.getCommodityData(stack.getCommodityId());
               if (mode == PlayerEconomyImpactMode.BOTH) {
                  com.addTradeMod("sell_" + Misc.genUID(), qty, TRADE_IMPACT_DAYS);
               } else if (mode == PlayerEconomyImpactMode.PLAYER_SELL_ONLY) {
                  com.addTradeModPlus("sell_" + Misc.genUID(), qty, TRADE_IMPACT_DAYS);
               } else if (mode == PlayerEconomyImpactMode.PLAYER_BUY_ONLY || mode == PlayerEconomyImpactMode.NONE) {
                  com.addTradeModMinus("sell_" + Misc.genUID(), qty, TRADE_IMPACT_DAYS);
               }
            }
         }
      }
   }

   public boolean isMilitaryMarket() {
      return false;
   }

   public boolean isBlackMarket() {
      return this.market.getFaction().isHostileTo(this.submarket.getFaction());
   }

   public boolean isOpenMarket() {
      return false;
   }

   public boolean isParticipatesInEconomy() {
      return true;
   }

   public boolean isIllegalOnSubmarket(String commodityId, SubmarketPlugin.TransferAction action) {
      return this.market.isIllegal(commodityId);
   }

   public boolean isIllegalOnSubmarket(CargoStackAPI stack, SubmarketPlugin.TransferAction action) {
      return !stack.isCommodityStack() ? false : this.isIllegalOnSubmarket((String)stack.getData(), action);
   }

   public String getIllegalTransferText(CargoStackAPI stack, SubmarketPlugin.TransferAction action) {
      return "Illegal to trade on the " + this.submarket.getNameOneLine().toLowerCase() + " here";
   }

   public boolean isIllegalOnSubmarket(FleetMemberAPI member, SubmarketPlugin.TransferAction action) {
      return action == TransferAction.PLAYER_SELL && !this.isBlackMarket() && Misc.isAutomated(member);
   }

   public String getIllegalTransferText(FleetMemberAPI member, SubmarketPlugin.TransferAction action) {
      if (action == TransferAction.PLAYER_BUY) {
         return "Illegal to buy";
      } else {
         return this.isFreeTransfer() ? "Illegal to store" : "Illegal to sell";
      }
   }

   protected void addFighters(int min, int max, int maxTier, WeightedRandomPicker<String> factionPicker) {
      int num = min + this.itemGenRandom.nextInt(max - min + 1);

      for(int i = 0; i < num; ++i) {
         String factionId = (String)factionPicker.pick();
         this.addFighters(1, 1, maxTier, (String)factionId);
      }

   }

   protected void addWeapons(int min, int max, int maxTier, String factionId) {
      this.addWeapons(min, max, maxTier, factionId, true);
   }

   protected void addWeapons(int min, int max, int maxTier, String factionId, boolean withCategories) {
      WeightedRandomPicker<String> picker = new WeightedRandomPicker<>(this.itemGenRandom);
      picker.add(factionId);
      this.addWeapons(min, max, maxTier, picker, withCategories);
   }

   protected void addWeapons(int min, int max, int maxTier, WeightedRandomPicker<String> factionPicker) {
      this.addWeapons(min, max, maxTier, factionPicker, true);
   }

   protected void addWeapons(int min, int max, int maxTier, WeightedRandomPicker<String> factionPicker, boolean withCategories) {
      WeightedRandomPicker<WeaponSpecAPI> picker = new WeightedRandomPicker<>(this.itemGenRandom);
      WeightedRandomPicker<WeaponSpecAPI> pd = new WeightedRandomPicker<>(this.itemGenRandom);
      WeightedRandomPicker<WeaponSpecAPI> kinetic = new WeightedRandomPicker<>(this.itemGenRandom);
      WeightedRandomPicker<WeaponSpecAPI> nonKinetic = new WeightedRandomPicker<>(this.itemGenRandom);
      WeightedRandomPicker<WeaponSpecAPI> missile = new WeightedRandomPicker<>(this.itemGenRandom);
      WeightedRandomPicker<WeaponSpecAPI> strike = new WeightedRandomPicker<>(this.itemGenRandom);

      int num;
      label125:
      for(num = 0; num < factionPicker.getItems().size(); ++num) {
         String factionId = (String)factionPicker.getItems().get(num);
         float w = factionPicker.getWeight(num);
         if (factionId == null) {
            factionId = this.market.getFactionId();
         }

         float quality = Misc.getShipQuality(this.market, factionId);
         FactionAPI faction = Global.getSector().getFaction(factionId);
         Iterator<String> var18 = faction.getKnownWeapons().iterator();

         while(true) {
            while(true) {
               WeaponSpecAPI spec;
               float p;
               String cat;
               do {
                  do {
                     String id;
                     do {
                        do {
                           do {
                              if (!var18.hasNext()) {
                                 continue label125;
                              }

                              id = (String)var18.next();
                              spec = Global.getSettings().getWeaponSpec(id);
                           } while(spec.getTier() > maxTier);
                        } while(spec.getAIHints().contains(AIHints.SYSTEM));
                     } while(spec.hasTag("no_sell"));

                     p = DefaultFleetInflater.getTierProbability(spec.getTier(), quality);
                     p = 1.0F;
                     p *= w;
                     if (faction.getWeaponSellFrequency().containsKey(id)) {
                        p *= (Float)faction.getWeaponSellFrequency().get(id);
                     }

                     picker.add(spec, p);
                     cat = spec.getAutofitCategory();
                  } while(cat == null);
               } while(spec.getSize() == WeaponSize.LARGE);

               if (CoreAutofitPlugin.PD.equals(cat)) {
                  pd.add(spec, p);
               } else if (CoreAutofitPlugin.STRIKE.equals(cat)) {
                  strike.add(spec, p);
               } else if (CoreAutofitPlugin.KINETIC.equals(cat)) {
                  kinetic.add(spec, p);
               } else if (!CoreAutofitPlugin.MISSILE.equals(cat) && !CoreAutofitPlugin.ROCKET.equals(cat)) {
                  if (CoreAutofitPlugin.HE.equals(cat) || CoreAutofitPlugin.ENERGY.equals(cat)) {
                     nonKinetic.add(spec, p);
                  }
               } else {
                  missile.add(spec, p);
               }
            }
         }
      }

      num = min + this.itemGenRandom.nextInt(max - min + 1);
      if (withCategories) {
         if (num > 0 && !pd.isEmpty()) {
            this.pickAndAddWeapons(pd);
            --num;
         }

         if (num > 0 && !kinetic.isEmpty()) {
            this.pickAndAddWeapons(kinetic);
            --num;
         }

         if (num > 0 && !missile.isEmpty()) {
            this.pickAndAddWeapons(missile);
            --num;
         }

         if (num > 0 && !nonKinetic.isEmpty()) {
            this.pickAndAddWeapons(nonKinetic);
            --num;
         }

         if (num > 0 && !strike.isEmpty()) {
            this.pickAndAddWeapons(strike);
            --num;
         }
      }

      for(int i = 0; i < num && !picker.isEmpty(); ++i) {
         this.pickAndAddWeapons(picker);
      }

   }

   protected void pickAndAddWeapons(WeightedRandomPicker<WeaponSpecAPI> picker) {
      WeaponSpecAPI spec = (WeaponSpecAPI)picker.pick();
      if (spec != null) {
         int count = 1;
         switch (spec.getSize()) {
            case SMALL:
               count = 3;
               break;
            case MEDIUM:
               count = 2;
               break;
            case LARGE:
               count = 1;
         }

         count = count + this.itemGenRandom.nextInt(count + 2) - this.itemGenRandom.nextInt(count + 1);
         if (count < 1) {
            count = 1;
         }

         this.cargo.addWeapons(spec.getWeaponId(), count);
      }
   }

   protected void addFighters(int min, int max, int maxTier, String factionId) {
      if (factionId == null) {
         factionId = this.market.getFactionId();
      }

      int num = min + this.itemGenRandom.nextInt(max - min + 1);
      float quality = Misc.getShipQuality(this.market, factionId);
      FactionAPI faction = Global.getSector().getFaction(factionId);
      WeightedRandomPicker<FighterWingSpecAPI> picker = new WeightedRandomPicker<>(this.itemGenRandom);
      Iterator<String> var10 = faction.getKnownFighters().iterator();

      while(var10.hasNext()) {
         String id = (String)var10.next();
         FighterWingSpecAPI spec = Global.getSettings().getFighterWingSpec(id);
         if (spec == null) {
            throw new RuntimeException("Fighter wing spec with id [" + id + "] not found");
         }

         if (spec.getTier() <= maxTier && !spec.hasTag("no_sell")) {
            float p = DefaultFleetInflater.getTierProbability(spec.getTier(), quality);
            p = 1.0F;
            if (faction.getFighterSellFrequency().containsKey(id)) {
               p *= (Float)faction.getFighterSellFrequency().get(id);
            }

            picker.add(spec, p);
         }
      }

      for(int i = 0; i < num && !picker.isEmpty(); ++i) {
         FighterWingSpecAPI spec = (FighterWingSpecAPI)picker.pick();
         int count = 2;
         switch (spec.getRole()) {
            case BOMBER:
               count = 2;
               break;
            case FIGHTER:
               count = 3;
               break;
            case INTERCEPTOR:
               count = 4;
               break;
            case ASSAULT:
               count = 2;
               break;
            case SUPPORT:
               count = 2;
         }

         count = count + this.itemGenRandom.nextInt(count + 1) - count / 2;
         this.cargo.addItems(CargoItemType.FIGHTER_CHIP, spec.getId(), (float)count);
      }

   }

   protected void pruneWeapons(float keepFraction) {
      CargoAPI cargo = this.getCargo();
      Iterator<CargoStackAPI> var4 = cargo.getStacksCopy().iterator();

      while(true) {
         CargoStackAPI stack;
         do {
            if (!var4.hasNext()) {
               return;
            }

            stack = (CargoStackAPI)var4.next();
         } while(!stack.isWeaponStack() && !stack.isFighterWingStack());

         float qty = stack.getSize();
         if (qty <= 1.0F) {
            if (this.itemGenRandom.nextFloat() > keepFraction) {
               cargo.removeItems(stack.getType(), stack.getData(), 1.0F);
            }
         } else {
            cargo.removeItems(stack.getType(), stack.getData(), (float)Math.round(qty * (1.0F - keepFraction)));
         }
      }
   }

   public void addShips(String factionId, float combat, float freighter, float tanker, float transport, float liner, float utility, Float qualityOverride, float qualityMod, FactionAPI.ShipPickMode modeOverride, FactionDoctrineAPI doctrineOverride) {
      this.addShips(factionId, combat, freighter, tanker, transport, liner, utility, qualityOverride, qualityMod, modeOverride, doctrineOverride, 1000);
   }

   public void addShips(String factionId, float combat, float freighter, float tanker, float transport, float liner, float utility, Float qualityOverride, float qualityMod, FactionAPI.ShipPickMode modeOverride, FactionDoctrineAPI doctrineOverride, int maxShipSize) {
      FleetParamsV3 params = new FleetParamsV3(this.market, Global.getSector().getPlayerFleet().getLocationInHyperspace(), factionId, (Float)null, "patrolLarge", combat, freighter, tanker, transport, liner, utility, 0.0F);
      params.maxShipSize = maxShipSize;
      params.random = new Random(this.itemGenRandom.nextLong());
      params.qualityOverride = Misc.getShipQuality(this.market, factionId) + qualityMod;
      if (qualityOverride != null) {
         params.qualityOverride = qualityOverride + qualityMod;
      }

      params.withOfficers = false;
      params.forceAllowPhaseShipsEtc = true;
      params.treatCombatFreighterSettingAsFraction = true;
      params.modeOverride = Misc.getShipPickMode(this.market, factionId);
      if (modeOverride != null) {
         params.modeOverride = modeOverride;
      }

      params.doctrineOverride = doctrineOverride;
      CampaignFleetAPI fleet = FleetFactoryV3.createFleet(params);
      if (fleet != null) {
         WeightedRandomPicker<FleetMemberAPI> picker = new WeightedRandomPicker<>(this.itemGenRandom);
         FactionAPI faction = Global.getSector().getFaction(factionId);
         Iterator<FleetMemberAPI> var18 = fleet.getFleetData().getMembersListCopy().iterator();

         label61:
         while(true) {
            FleetMemberAPI member;
            float f;
            do {
               do {
                  do {
                     if (!var18.hasNext()) {
                        List<FleetMemberAPI> members = new ArrayList<>();

                        while(!picker.isEmpty()) {
                           members.add((FleetMemberAPI)picker.pickAndRemove());
                        }

                        Iterator<FleetMemberAPI> var23 = members.iterator();

                        while(var23.hasNext()) {
                           member = (FleetMemberAPI)var23.next();
                           String emptyVariantId = member.getHullId() + "_Hull";
                           this.addShip(emptyVariantId, true, params.qualityOverride);
                        }
                        break label61;
                     }

                     member = (FleetMemberAPI)var18.next();
                     f = 1.0F;
                     if (faction != null) {
                        Float mult = (Float)faction.getFactionSpec().getShipSellFrequency().get(member.getHullId());
                        if (mult != null) {
                           f *= mult;
                        }
                     }
                  } while(this.itemGenRandom.nextFloat() > f * 0.5F);
               } while(member.getHullSpec().hasTag("no_sell"));
            } while(!this.isMilitaryMarket() && member.getHullSpec().hasTag("req_military"));

            picker.add(member, f);
         }
      }

   }

   protected FleetMemberAPI addShip(String variantOrWingId, boolean withDmods, float quality) {
      FleetMemberAPI member = null;
      if (variantOrWingId.endsWith("_wing")) {
         member = Global.getFactory().createFleetMember(FleetMemberType.FIGHTER_WING, variantOrWingId);
      } else {
         member = Global.getFactory().createFleetMember(FleetMemberType.SHIP, variantOrWingId);
      }

      if (withDmods) {
         float averageDmods = DefaultFleetInflater.getAverageDmodsForQuality(quality);
         int addDmods = DefaultFleetInflater.getNumDModsToAdd(member.getVariant(), averageDmods, this.itemGenRandom);
         if (addDmods > 0) {
            DModManager.setDHull(member.getVariant());
            DModManager.addDMods(member, true, addDmods, this.itemGenRandom);
         }
      }

      member.getRepairTracker().setMothballed(true);
      member.getRepairTracker().setCR(0.5F);
      this.getCargo().getMothballedShips().addFleetMember(member);
      return member;
   }

   protected void pruneShips(float mult) {
      CargoAPI cargo = this.getCargo();
      FleetDataAPI data = cargo.getMothballedShips();
      Iterator<FleetMemberAPI> var5 = data.getMembersListCopy().iterator();

      while(var5.hasNext()) {
         FleetMemberAPI member = (FleetMemberAPI)var5.next();
         if (this.itemGenRandom.nextFloat() > mult) {
            data.removeFleetMember(member);
         }
      }

   }

   protected void addHullMods(int maxTier, int num) {
      this.addHullMods(maxTier, num, (String)null);
   }

   @SuppressWarnings("deprecation")
   protected void addHullMods(int maxTier, int num, String factionId) {
      CargoAPI cargo = this.getCargo();
      Iterator<CargoStackAPI> var6 = cargo.getStacksCopy().iterator();

      while(var6.hasNext()) {
         CargoStackAPI stack = (CargoStackAPI)var6.next();
         if (stack.isModSpecStack()) {
            cargo.removeStack(stack);
         }
      }

      FactionAPI faction = null;
      if (factionId != null) {
         faction = Global.getSector().getFaction(factionId);
      }

      WeightedRandomPicker<HullModSpecAPI> picker = new WeightedRandomPicker<>(this.itemGenRandom);
      Iterator<String> var8 = this.submarket.getFaction().getKnownHullMods().iterator();

      while(var8.hasNext()) {
         String id = (String)var8.next();
         HullModSpecAPI spec = Global.getSettings().getHullModSpec(id);
         if (!spec.isHidden() && !spec.isAlwaysUnlocked() && spec.getTier() <= maxTier) {
            float p = spec.getRarity();
            if (faction != null && faction.getHullmodSellFrequency().containsKey(id) && !Global.getSector().getPlayerFaction().knowsHullMod(id)) {
               p *= (Float)faction.getHullmodSellFrequency().get(id);
            }

            picker.add(spec, p);
         }
      }

      for(int i = 0; i < num; ++i) {
         HullModSpecAPI pick = (HullModSpecAPI)picker.pickAndRemove();
         if (pick != null) {
            String id = pick.getId();
            if (!this.cargoAlreadyHasMod(id) && !Global.getSector().getPlayerFaction().knowsHullMod(id)) {
               cargo.addItems(CargoItemType.SPECIAL, new SpecialItemData("modspec", id), 1.0F);
            }
         }
      }

   }

   @SuppressWarnings("deprecation")
   protected boolean removeModFromCargo(String id) {
      CargoAPI cargo = this.getCargo();
      Iterator<CargoStackAPI> var4 = cargo.getStacksCopy().iterator();

      while(var4.hasNext()) {
         CargoStackAPI stack = (CargoStackAPI)var4.next();
         if (stack.isModSpecStack() && stack.getData().equals(id)) {
            cargo.removeStack(stack);
         }
      }

      return false;
   }

   protected boolean cargoAlreadyHasMod(String id) {
      CargoAPI cargo = this.getCargo();
      Iterator<CargoStackAPI> var4 = cargo.getStacksCopy().iterator();

      CargoStackAPI stack;
      do {
         if (!var4.hasNext()) {
            return false;
         }

         stack = (CargoStackAPI)var4.next();
      } while(!stack.isSpecialStack() || !stack.getSpecialDataIfSpecial().getId().equals("modspec") || !stack.getSpecialDataIfSpecial().getData().equals(id));

      return true;
   }

   public Highlights getIllegalTransferTextHighlights(CargoStackAPI stack, SubmarketPlugin.TransferAction action) {
      return null;
   }

   public Highlights getIllegalTransferTextHighlights(FleetMemberAPI member, SubmarketPlugin.TransferAction action) {
      return null;
   }

   public float getMinSWUpdateInterval() {
      return this.minSWUpdateInterval;
   }

   public void setMinSWUpdateInterval(float minCargoUpdateInterval) {
      this.minSWUpdateInterval = minCargoUpdateInterval;
   }

   public float getSinceLastCargoUpdate() {
      return this.sinceLastCargoUpdate;
   }

   public void setSinceLastCargoUpdate(float sinceLastCargoUpdate) {
      this.sinceLastCargoUpdate = sinceLastCargoUpdate;
   }

   public float getSinceSWUpdate() {
      return this.sinceSWUpdate;
   }

   public void setSinceSWUpdate(float sinceSWUpdate) {
      this.sinceSWUpdate = sinceSWUpdate;
   }

   public boolean hasCustomTooltip() {
      return true;
   }

   public void createTooltip(CoreUIAPI ui, TooltipMakerAPI tooltip, boolean expanded) {
      float opad = 10.0F;
      tooltip.addTitle(this.submarket.getNameOneLine());
      String desc = this.submarket.getSpec().getDesc();
      desc = Global.getSector().getRules().performTokenReplacement((String)null, desc, this.market.getPrimaryEntity(), (Map<String, MemoryAPI>)null);
      String appendix = this.submarket.getPlugin().getTooltipAppendix(ui);
      if (appendix != null) {
         desc = desc + "\n\n" + appendix;
      }

      if (desc != null && !desc.isEmpty()) {
         LabelAPI body = tooltip.addPara(desc, opad);
         if (this.getTooltipAppendixHighlights(ui) != null) {
            Highlights h = this.submarket.getPlugin().getTooltipAppendixHighlights(ui);
            if (h != null) {
               body.setHighlightColors(h.getColors());
               body.setHighlight(h.getText());
            }
         }
      }

      this.createTooltipAfterDescription(tooltip, expanded);
   }

   protected void createTooltipAfterDescription(TooltipMakerAPI tooltip, boolean expanded) {
   }

   public boolean isTooltipExpandable() {
      return false;
   }

   public float getTooltipWidth() {
      return 400.0F;
   }

   public boolean isHidden() {
      return false;
   }

   public boolean showInFleetScreen() {
      return true;
   }

   public boolean showInCargoScreen() {
      return true;
   }

   public MarketAPI getMarket() {
      return this.market;
   }

   public SubmarketAPI getSubmarket() {
      return this.submarket;
   }

   public int getStockpileLimit(CommodityOnMarketAPI com) {
      return 0;
   }

   public float getStockpilingAddRateMult(CommodityOnMarketAPI com) {
      return 1.0F;
   }

   public boolean shouldHaveCommodity(CommodityOnMarketAPI com) {
      return true;
   }

   public void addAndRemoveStockpiledResources(float amount, boolean withShortageCountering, boolean withDecreaseToLimit, boolean withCargoUpdate) {
      Iterator<CommodityOnMarketAPI> var6 = this.market.getCommoditiesCopy().iterator();

      while(var6.hasNext()) {
         CommodityOnMarketAPI com = (CommodityOnMarketAPI)var6.next();
         if (!com.isNonEcon() && !com.getCommodity().isMeta()) {
            this.addAndRemoveStockpiledResources(com, amount, withShortageCountering, withDecreaseToLimit, withCargoUpdate);
         }
      }

   }

   protected boolean doShortageCountering(CommodityOnMarketAPI com, float amount, boolean withShortageCountering) {
      return false;
   }

   public void addAndRemoveStockpiledResources(CommodityOnMarketAPI com, float amount, boolean withShortageCountering, boolean withDecreaseToLimit, boolean withCargoUpdate) {
      float days = Global.getSector().getClock().convertToDays(amount);
      if (!com.isNonEcon()) {
         if (!com.getCommodity().isMeta()) {
            CargoAPI cargo = this.getCargo();
            if (withShortageCountering) {
               withShortageCountering = this.market.isUseStockpilesForShortages();
            }

            if (!this.doShortageCountering(com, amount, withShortageCountering)) {
               float limit;
               float curr;
               float addRate;
               float addAmount;
               if (!this.shouldHaveCommodity(com)) {
                  if (withDecreaseToLimit) {
                     limit = (float)this.getStockpileLimit(com);
                     curr = cargo.getCommodityQuantity(com.getId());
                     if (curr > limit && withDecreaseToLimit) {
                        addRate = (curr - limit) * 2.0F / 30.0F;
                        addAmount = addRate * days;
                        if (curr - addAmount < limit) {
                           addAmount = curr - limit;
                        }

                        if (addAmount > 0.0F && curr <= 1.0F) {
                           addAmount = 1.0F;
                        }

                        if (addAmount > 0.0F) {
                           cargo.removeCommodity(com.getId(), addAmount);
                        }
                     }
                  }

               } else {
                  limit = (float)this.getStockpileLimit(com);
                  curr = cargo.getCommodityQuantity(com.getId());
                  if (curr < limit && withCargoUpdate) {
                     if (!(limit <= 0.0F)) {
                        addRate = limit / 30.0F * this.getStockpilingAddRateMult(com);
                        if (!(this.sinceLastCargoUpdate * addRate + curr < 1.0F)) {
                           addAmount = addRate * days;
                           if (curr + addAmount > limit) {
                              addAmount = limit - curr;
                           }

                           if (addAmount > 0.0F) {
                              float q = cargo.getCommodityQuantity(com.getId()) + addAmount;
                              if (q < 1.0F) {
                                 addAmount = 1.0F;
                              }

                              cargo.addCommodity(com.getId(), addAmount);
                           }

                        }
                     }
                  } else if (curr > limit && withDecreaseToLimit) {
                     addRate = (curr - limit) * 2.0F / 30.0F;
                     addAmount = addRate * days;
                     if (curr - addAmount < limit) {
                        addAmount = curr - limit;
                     }

                     if (addAmount > 0.0F && curr <= 1.0F) {
                        addAmount = 1.0F;
                     }

                     if (addAmount > 0.0F) {
                        cargo.removeCommodity(com.getId(), addAmount);
                     }

                  }
               }
            }
         }
      }
   }

   public String getTariffTextOverride() {
      return null;
   }

   public String getTariffValueOverride() {
      return null;
   }

   public String getTotalTextOverride() {
      return null;
   }

   public String getTotalValueOverride() {
      return null;
   }
}

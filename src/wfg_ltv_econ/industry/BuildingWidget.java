package wfg_ltv_econ.industry;

import com.fs.graphics.A.D;
import com.fs.graphics.util.B;
import com.fs.graphics.util.Fader;
import com.fs.starfarer.O0OO;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.SpecialItemData;
import com.fs.starfarer.api.campaign.econ.CommodityOnMarketAPI;
import com.fs.starfarer.api.campaign.econ.Industry;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI.MarketInteractionMode;
import com.fs.starfarer.api.impl.campaign.DebugFlags;
import com.fs.starfarer.api.impl.campaign.econ.impl.ConstructionQueue;
import com.fs.starfarer.api.impl.campaign.econ.impl.ConstructionQueue.ConstructionQueueItem;
import com.fs.starfarer.api.ui.Alignment;
import com.fs.starfarer.api.util.Misc;
import com.fs.starfarer.api.util.Pair;
import com.fs.starfarer.campaign.CampaignEngine;
import com.fs.starfarer.campaign.econ.CommodityOnMarket;
import com.fs.starfarer.campaign.ui.N;
import com.fs.starfarer.renderers.O;
import com.fs.starfarer.ui.OO0O;
import com.fs.starfarer.ui.Q;
import com.fs.starfarer.ui.U;
import com.fs.starfarer.ui.d;
import com.fs.starfarer.ui.n;
import com.fs.starfarer.ui.oo0O;
import com.fs.starfarer.ui.x;
import com.fs.starfarer.util.A.C;

import wfg_ltv_econ.util.LtvMarketWidgetReplacer;

import java.awt.Color;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import com.fs.starfarer.campaign.ui.marketinfo.IndustryListPanel;
import com.fs.starfarer.campaign.ui.marketinfo.ooO0;
import com.fs.starfarer.campaign.ui.marketinfo.S;
import com.fs.starfarer.campaign.ui.marketinfo.b;
import com.fs.starfarer.api.ui.IconRenderMode;
import com.fs.starfarer.campaign.ui.marketinfo.intnew;

public class BuildingWidget extends intnew/* interfacenew implements U, oo0O.o */ {
   private Industry currentIndustry;
   private x industryIcon;
   private n constructionActionButton;
   private ooO0 commodityDeficitIconGroup;
   private ooO0 specialItemGroup;
   private intnew.o tradeInfoPanel;
   private IndustryListPanel IndustryPanel;
   private int constructionQueueIndex;
   protected final List<d> labels = new ArrayList<>();
   private Fader glowFader;
   private d buildingTitleHeader;
   private d constructionStatusText;
   private Oo constructionMode;

   private static final String DEFAULT_FONT = "defaultFont";

   static {
      HEIGHT = IMAGE_HEIGHT + FONT_HEIGHT + PAD;
   }

   public BuildingWidget(MarketAPI market, Industry currentIndustry, IndustryListPanel IndustryPanel) {
      this(market, currentIndustry, IndustryPanel, -1);
   }

   public BuildingWidget(MarketAPI market, Industry currentIndustry, IndustryListPanel IndustryPanel, int queue) {
      super(market, currentIndustry, IndustryPanel, queue);
      this.glowFader = new Fader(0.0F, 0.2F, 0.2F, false, true);
      this.constructionMode = com.fs.starfarer.campaign.ui.marketinfo.intnew.Oo.Ó00000;
      this.currentIndustry = currentIndustry;
      this.IndustryPanel = IndustryPanel;
      this.constructionQueueIndex = queue;
   }

   public void sizeChanged(float var1, float var2) {
      clearChildren();
      super.sizeChanged(var1, var2);
      buildingTitleHeader = d.createSmallInsigniaLabel(currentIndustry.getCurrentName(), Alignment.LMID);
      buildingTitleHeader = new d(" " + currentIndustry.getCurrentName(), DEFAULT_FONT, color, true, Alignment.LMID); // "defaultFont might change in the future"
      buildingTitleHeader.setColor(color);
      if (currentIndustry.isImproved()) {
         buildingTitleHeader.setColor(Misc.getStoryOptionColor());
      }
      try {
         // buildingTitleHeader.getRenderer().int(true);
         Method intMethod = buildingTitleHeader.getRenderer().getClass().getDeclaredMethod("int", boolean.class);
         intMethod.setAccessible(true);
         intMethod.invoke(buildingTitleHeader.getRenderer(), true);
      } catch (Exception e) {
         Global.getLogger(LtvMarketWidgetReplacer.class).error("Custom Widget failed", e);
      }

      buildingTitleHeader.autoSize();
      industryIcon = new x(currentIndustry.getCurrentImage(), color, true);
      constructionActionButton = Q.o00000(industryIcon, this);
      constructionActionButton.setQuickMode(true);

      try {
         // constructionActionButton.getLogic().o00000(com.fs.starfarer.ui.int.o.String);
         Class<?> enumClass = Class.forName("com.fs.starfarer.ui.int$o");
         Object enumConstant = Enum.class.getMethod("valueOf", Class.class, String.class) .invoke(null, enumClass, "String");
         Method getLogicMethod = constructionActionButton.getClass().getMethod("getLogic");
         Object logic = getLogicMethod.invoke(constructionActionButton);
         Method method = logic.getClass().getMethod("o00000", enumClass);
         method.invoke(logic, enumConstant);
      } catch (Exception e) {
         Global.getLogger(LtvMarketWidgetReplacer.class).error("Custom Widget failed", e);
      }

      if (!currentIndustry.isFunctional() || constructionQueueIndex >= 0) {
         industryIcon.setImageColor(dark);
      }

      if (!DebugFlags.COLONY_DEBUG && !market.isPlayerOwned()) {
         industryIcon.setDisabledColor(Color.white);
         constructionActionButton.setEnabled(false);
      }

      commodityDeficitIconGroup = new ooO0((U) null);
      commodityDeficitIconGroup.setWideSpacing(true);
      commodityDeficitIconGroup.setMediumSpacing(true);
      int var3 = 0;
      for (Pair<String, Integer> deficitEntry : currentIndustry.getAllDeficit()) {
         CommodityOnMarketAPI var6 = market.getCommodityData(deficitEntry.one);
         commodityDeficitIconGroup.addGroup((CommodityOnMarket) var6, deficitEntry.two, 1.0F,
               com.fs.starfarer.campaign.ui.marketinfo.f.o.õ00000, null);
         if (deficitEntry.two > var3) {
            var3 = deficitEntry.two;
         }
      }

      float var17 = 24.0F;
      float var18 = 150.0F;
      commodityDeficitIconGroup.autoSizeWithAdjust(var17, var18, var17, var17);
      d var19 = d.createSmallInsigniaLabel("-" + var3, Alignment.LMID);
      var19 = new d("-" + var3, "graphics/fonts/insignia21LTaa.fnt", color, true, Alignment.LMID);
      var19.setColor(O0OO.ÒÓ0000);

      try {
         // var19.getRenderer().int(true);
         Method intMethod = buildingTitleHeader.getRenderer().getClass().getDeclaredMethod("int", boolean.class);
         intMethod.setAccessible(true);
         intMethod.invoke(var19.getRenderer(), true);
      } catch (Exception e) {
         Global.getLogger(LtvMarketWidgetReplacer.class).error("Custom Widget failed", e);
      }
      

      var19.autoSize();
      var19.setOpacity(0.0F);
      specialItemGroup = new ooO0((U) null);
      specialItemGroup.setWideSpacing(true);
      int var7 = 0;
      List<SpecialItemData> visibleItems = currentIndustry.getVisibleInstalledItems();
      for (SpecialItemData item : visibleItems) {
         String comIdItem = new S(item).getCommodity().getId();
         specialItemGroup.addIconGroup(comIdItem, IconRenderMode.NORMAL, var7, color);
         var7++;
      }

      if (currentIndustry.getAICoreId() != null) {
         String comIdAICore = market.getCommodityData(currentIndustry.getAICoreId()).getId();
         specialItemGroup.addIconGroup(comIdAICore, IconRenderMode.NORMAL, var7, color);
         ++var7;
      }

      float var22;
      if (var7 > 0) {
         var22 = 32.0F;
         specialItemGroup.autoSizeWithAdjust(var22, var22 * (float) var7, var22, var22);
      }

      var22 = 3.0F;
      float var13 = IMAGE_HEIGHT;
      add(constructionActionButton).setSize(var1, var13).inBL(0.0F, 0.0F);
      add(buildingTitleHeader).inTL(0.0F, 0.0F);
      add(commodityDeficitIconGroup).inBL(var22 + 2.0F, var22);
      add(specialItemGroup).inTR(var22 + 2.0F, var22 + buildingTitleHeader.getHeight() + var22);
      if (var3 != 0) {
         add(var19).inBR(var22 + 2.0F, var22 + (var17 - var19.getHeight()) / 2.0F);
      }

      float var14 = 18.0F;
      var14 = 12.0F;
      boolean var15 = currentIndustry.isBuilding() || currentIndustry.isDisrupted();
      if (var15) {
         if (currentIndustry.isBuilding() && !currentIndustry.isUpgrading() && !currentIndustry.isDisrupted()) {
            String var16 = "graphics/fonts/insignia25LTaa.fnt";
            constructionStatusText = d.create("Building", var16, color);
            constructionStatusText.autoSize();
            add(constructionStatusText).inMid().setYAlignOffset(-buildingTitleHeader.getHeight() / 2.0F);
         }

         N var24 = new N((String) null, 0.0F, 100.0F);
         var24.getValue().getRenderer().o00000(D.Ò00000("graphics/fonts/victor10.fnt"));
         var24.getValue().getPosition().setYAlignOffset(1.0F);
         var24.setBarColor(Misc.interpolateColor(color, dark, 0.5F));
         var24.setTextColor(color);
         var24.setProgress(currentIndustry.getBuildOrUpgradeProgress() * 100.0F);
         var24.setText(currentIndustry.getBuildOrUpgradeProgressText());
         var24.setShowLabelOnly(true);
         add(var24).setSize(var1, var14).inBL(0.0F, -var14 - 2.0F);
      }

      if (constructionQueueIndex >= 0) {
         setNormalMode();
      }

   }

   public Fader getGlow() {
      return glowFader;
   }

   public int getQueueIndex() {
      return constructionQueueIndex;
   }

   public void clearLabels() {
      for (d label : labels) {
         remove(label);
      }
      labels.clear();
   }

   public Oo getMode() {
      return constructionMode;
   }

   @Override
   public void setRemoveMode() {
      ConstructionQueue.ConstructionQueueItem var1 = null;
      if (market.getConstructionQueue().getItems().size() > constructionQueueIndex && constructionQueueIndex >= 0) {
         var1 = (ConstructionQueue.ConstructionQueueItem) market.getConstructionQueue().getItems()
               .get(constructionQueueIndex);
      }

      if (var1 != null) {
         clearLabels();
         d var2 = d.create("Click to remove", DEFAULT_FONT, color);
         d var3 = d.create(Misc.getDGSCredits((float) var1.cost) + " refund", DEFAULT_FONT, color);
         var3.highlightFirst(Misc.getDGSCredits((float) var1.cost));
         var3.setHighlightColor(Misc.getHighlightColor());
         labels.add(var2);
         labels.add(var3);
         add(var2).aboveMid(constructionStatusText, 0.0F);
         add(var3).belowMid(constructionStatusText, 0.0F);
         constructionActionButton.highlight();
         constructionMode = com.fs.starfarer.campaign.ui.marketinfo.intnew.Oo.String;
         addCostTimeLabels();
      }
   }

   public void setSwapMode() {
      clearLabels();
      d var1 = d.create("Click to swap", DEFAULT_FONT, color);
      labels.add(var1);
      add(var1).aboveMid(constructionStatusText, 0.0F);
      constructionActionButton.highlight();
      constructionMode = com.fs.starfarer.campaign.ui.marketinfo.intnew.Oo.Ò00000;
      addCostTimeLabels();
   }

   @Override
   public void setNormalMode() {
      clearLabels();
      String var1 = "Queued";
      if (Misc.getCurrentlyBeingConstructed(market) == null && constructionQueueIndex == 0) {
         var1 = "Building";
      }

      remove(constructionStatusText);
      constructionStatusText = d.create(var1, "graphics/fonts/insignia25LTaa.fnt", color);
      constructionStatusText.autoSize();
      add(constructionStatusText).inMid().setYAlignOffset(-buildingTitleHeader.getHeight() / 2.0F);
      constructionActionButton.unhighlight();
      constructionMode = com.fs.starfarer.campaign.ui.marketinfo.intnew.Oo.Ó00000;
      addCostTimeLabels();
   }

   protected void addCostTimeLabels() {
      if (market.getConstructionQueue().getItems().size() > constructionQueueIndex && constructionQueueIndex >= 0) {
         ConstructionQueue.ConstructionQueueItem var1 = (ConstructionQueue.ConstructionQueueItem) market
               .getConstructionQueue().getItems().get(constructionQueueIndex);
         if (var1 != null) {
            int var2 = (int) currentIndustry.getSpec().getBuildTime();
            String var3 = "days";
            if (var2 == 1) {
               var3 = "day";
            }

            Color var4 = Misc.interpolateColor(Misc.getHighlightColor(), Color.black, 0.5F);
            Color var5 = Misc.interpolateColor(color, dark, 0.5F);
            d var6 = d.create(var2 + " " + var3, DEFAULT_FONT, var5);
            var6.highlightFirst("" + var2);
            var6.setHighlightColor(var4);
            d var7 = d.create(Misc.getDGSCredits((float) var1.cost), DEFAULT_FONT, var5);
            var7.highlightFirst(Misc.getDGSCredits((float) var1.cost));
            var7.setHighlightColor(var4);
            labels.add(var6);
            labels.add(var7);
            float var8 = 7.0F;
            float var9 = 3.0F;
            add(var6).inBL(var8, var9);
            add(var7).inBR(var8, var9);
         }
      }

   }

   @Override
   protected void renderImpl(float var1) {
      float var2 = 0.0F;
      var2 = FONT_HEIGHT + PAD;
      OO0O var3 = getPosition();
      float var4 = var3.getX();
      float var5 = var3.getY();
      float var6 = var3.getWidth();
      float var7 = var3.getHeight() - var2;
      float var8 = 1.0F;
      var4 -= var8;
      var5 -= var8;
      var6 += var8 * 2.0F;
      var7 += var8 * 2.0F;
      Color var9 = dark;
      if (currentIndustry.isImproved()) {
         var9 = Misc.getStoryDarkColor();
      }

      if (currentIndustry.isIndustry()) {
         O.Ó00000(var4, var5 + var7, var6, buildingTitleHeader.getHeight() + 3.0F, var9, var1);
      }

      O.o00000(var4, var5, var6, var7, 1.0F, true, true, true, true, var9, var1);
      float var10 = glowFader.getBrightness();
      var10 = constructionActionButton.getGlowAmount();
      if (var10 > 0.0F) {
         Color aColor = B.Ô00000(Color.white, var10 * 0.33F);
         buildingTitleHeader.setAdditiveColor(aColor);
         if (constructionStatusText != null) {
            constructionStatusText.setAdditiveColor(aColor);
         }
         for (d label : labels) {
            label.setAdditiveColor(aColor);
         }
      } else {
         buildingTitleHeader.setAdditiveColor((Color) null);
         if (constructionStatusText != null) {
            constructionStatusText.setAdditiveColor((Color) null);
         }
         for (d label : labels) {
            label.setAdditiveColor((Color) null);
         }
      }

      super.renderImpl(var1);
      if (currentIndustry.isIndustry() && var10 > 0.0F) {
         O.Ô00000(var4, var5 + var7, var6, buildingTitleHeader.getHeight() + 3.0F, dark, var1 * var10 * 0.33F);
      }

   }

   protected void advanceImpl(float var1) {
      glowFader.advance(var1);
      super.advanceImpl(var1);
   }

   @Override
   public void actionPerformed(Object var1, Object var2) {
      if (tradeInfoPanel == null) {
         BuildingWidget var9;
         if (constructionQueueIndex >= 0) {
            Iterator var5;
            BuildingWidget var10;
            if (var1 instanceof C) {
               C var3 = (C) var1;
               if (var3.isRMBEvent()) {
                  var5 = IndustryPanel.getWidgets().iterator();

                  while (var5.hasNext()) {
                     var10 = (BuildingWidget) var5.next();
                     if (var10.getQueueIndex() >= 0) {
                        var10.setNormalMode();
                     }
                  }

                  return;
               }
            }

            if (constructionMode == com.fs.starfarer.campaign.ui.marketinfo.intnew.Oo.Ó00000) {
               Iterator var4 = IndustryPanel.getWidgets().iterator();

               while (var4.hasNext()) {
                  var9 = (BuildingWidget) var4.next();
                  if (var9.getQueueIndex() >= 0) {
                     if (var9 == this) {
                        var9.setRemoveMode();
                     } else {
                        var9.setSwapMode();
                     }
                  }
               }
            } else if (constructionMode == com.fs.starfarer.campaign.ui.marketinfo.intnew.Oo.Ò00000) {
               var9 = null;
               var5 = IndustryPanel.getWidgets().iterator();

               while (var5.hasNext()) {
                  var10 = (BuildingWidget) var5.next();
                  if (var10.getQueueIndex() >= 0
                        && var10.constructionMode == com.fs.starfarer.campaign.ui.marketinfo.intnew.Oo.String) {
                     var9 = var10;
                     break;
                  }
               }

               List<ConstructionQueueItem> var12 = market.getConstructionQueue().getItems();
               if (var9 != null && var9.constructionQueueIndex >= 0 && var9.constructionQueueIndex < var12.size()
                     && constructionQueueIndex < var12.size() && constructionQueueIndex >= 0) {
                  ConstructionQueue.ConstructionQueueItem var15 = (ConstructionQueue.ConstructionQueueItem) var12
                        .get(constructionQueueIndex);
                  ConstructionQueue.ConstructionQueueItem var6 = (ConstructionQueue.ConstructionQueueItem) var12
                        .get(var9.constructionQueueIndex);
                  String var7 = var15.id;
                  int var8 = var15.cost;
                  var15.id = var6.id;
                  var15.cost = var6.cost;
                  var6.id = var7;
                  var6.cost = var8;
                  if (var12.indexOf(var15) != 0 && var12.indexOf(var6) != 0) {
                     IndustryPanel.recreateOverviewNoEconStep();
                  } else {
                     IndustryPanel.recreateOverview();
                  }
               }
            } else if (constructionMode == com.fs.starfarer.campaign.ui.marketinfo.intnew.Oo.String) {
               List<ConstructionQueueItem> var11 = market.getConstructionQueue().getItems();
               if (constructionQueueIndex < var11.size() && constructionQueueIndex >= 0) {
                  ConstructionQueue.ConstructionQueueItem var13 = (ConstructionQueue.ConstructionQueueItem) var11
                        .get(constructionQueueIndex);
                  market.getConstructionQueue().removeItem(var13.id);
                  int var16 = var13.cost;
                  if (var16 > 0) {
                     Global.getSector().getPlayerFleet().getCargo().getCredits().add((float) var16);
                     Misc.addCreditsMessage("Received %s", var16);
                  }

                  if (constructionQueueIndex == 0) {
                     IndustryPanel.recreateOverview();
                  } else {
                     IndustryPanel.recreateOverviewNoEconStep();
                  }
               }
            }
         } else {
            Iterator var4 = IndustryPanel.getWidgets().iterator();

            while (var4.hasNext()) {
               var9 = (BuildingWidget) var4.next();
               if (var9.getQueueIndex() >= 0) {
                  var9.setNormalMode();
               }
            }

            MarketAPI.MarketInteractionMode var14 = MarketInteractionMode.LOCAL;
            if (IndustryPanel.getOverview().getInfoPanel().getTradePanel().getOutpostPanelParams() != null) {
               var14 = IndustryPanel.getOverview().getInfoPanel().getTradePanel().getOutpostPanelParams().Õ00000;
            }

            b var17 = new b(currentIndustry, (intnew) this, var14,
                  CampaignEngine.getInstance().getCampaignUI().getDialogParent(), this);
            // b(com.fs.starfarer.ui.oo0O.o var5) {
            var17.show(0.0F, 0.0F);
            tradeInfoPanel = com.fs.starfarer.campaign.ui.marketinfo.intnew.o.o00000;
         }

      }
   }

   public void dialogDismissed(oo0O var1, int var2) {
      tradeInfoPanel = null;
   }

   public n getButton() {
      return constructionActionButton;
   }

   public IndustryListPanel getIndustryPanel() {
      return IndustryPanel;
   }
}

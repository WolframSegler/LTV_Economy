package wfg_ltv_econ.util;

import com.fs.graphics.A.D;
import com.fs.graphics.util.B;
import com.fs.graphics.util.Fader;
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
import com.fs.starfarer.api.ui.Fonts;
import com.fs.starfarer.api.ui.LabelAPI;
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

import wfg_ltv_econ.industry.LtvBaseIndustry;
import wfg_ltv_econ.util.ReflectionUtils;
import com.fs.starfarer.campaign.ui.marketinfo.T;
import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import com.fs.starfarer.campaign.ui.marketinfo.IndustryListPanel;
import com.fs.starfarer.campaign.ui.marketinfo.ooO0;
import com.fs.starfarer.campaign.ui.marketinfo.S;
import com.fs.starfarer.campaign.ui.marketinfo.b;
import com.fs.starfarer.campaign.ui.marketinfo.intnew;
import com.fs.starfarer.util.K;
// import com.fs.starfarer.util.A.C; //Cannot import. Because there is also a .class file named A.clas 

// A replacement for com.fs.starfarer.campaign.ui.marketinfo.intnew
// The widget class inside of List<intnew> widgets, which is a member of IndustryListItem
public class BuildingWidget extends intnew {
   public final static float ICON_SIZE = 32.0F;
   private Industry currentIndustry;
   private x industryIcon; // private x ÓõöO00;
   private n constructionActionButton; // private n ôôöO00;
   private ooO0 commodityDeficitIconGroup; // private ooO0 void.private$float;
   private ooO0 specialItemGroup; // private ooO0 ÕõöO00;
   private intnew.o tradeInfoPanel; // com.fs.starfarer.campaign.ui.marketinfo.intnew.o ÒõöO00;
   private IndustryListPanel IndustryPanel; // IndustryListPanel if.private$float;
   private int constructionQueueIndex; // private int ÖõöO00;
   protected final List<d> labels = new ArrayList<>(); // protected List<d> labels;
   private Fader glowFader; // private Fader OõöO00;
   private d buildingTitleHeader; // private d do.private$float;
   private d constructionStatusText; // private d õôöO00;
   private Oo constructionMode; // com.fs.starfarer.campaign.ui.marketinfo.intnew.Oo private.private$float;

   public BuildingWidget(MarketAPI market, Industry currentIndustry, IndustryListPanel IndustryPanel) {
      this(market, currentIndustry, IndustryPanel, -1);
   }

   public BuildingWidget(MarketAPI market, Industry currentIndustry, IndustryListPanel IndustryPanel, int queue) {
      super(market, currentIndustry, IndustryPanel, queue);
      this.glowFader = new Fader(0.0F, 0.2F, 0.2F, false, true);
      this.constructionMode = intnew.Oo.values()[0]; // Ó00000
      this.currentIndustry = currentIndustry;
      this.IndustryPanel = IndustryPanel;
      this.constructionQueueIndex = queue;
      this.glowFader = new Fader(0.0F, 0.2F, 0.2F, false, true);
   }

   @Override
   public void sizeChanged(float var1, float var2) {
      clearChildren();
      // super.sizeChanged(var1, var2);
      if (!this.created) {
         this.afterSizeFirstChanged(var1, var2);
         this.created = true;
      }
      /* function copy */

      buildingTitleHeader = new d(" " + currentIndustry.getCurrentName(), Fonts.DEFAULT_SMALL, color, true,
            Alignment.LMID);
      buildingTitleHeader.setColor(color);
      if (currentIndustry.isImproved()) {
         buildingTitleHeader.setColor(Misc.getStoryOptionColor());
      }
      try {
         // buildingTitleHeader.getRenderer().int(true);
         ReflectionUtils.invoke(buildingTitleHeader.getRenderer(), "int", true);
      } catch (Exception e) {
         Global.getLogger(LtvMarketWidgetReplacer.class).error("Custom Widget failed", e);
      }

      buildingTitleHeader.autoSize();
      industryIcon = new x(currentIndustry.getCurrentImage(), color, true);
      constructionActionButton = Q.o00000(industryIcon, this);
      constructionActionButton.setQuickMode(true);

      try {
         // constructionActionButton.getLogic().o00000(com.fs.starfarer.ui.int.o.String);
         Class<?> clazz = Class.forName("com.fs.starfarer.ui.int$o");
         Object stringFieldValue = ReflectionUtils.getFieldsMatching(clazz, "String", null, null)
               .get(0)
               .get(null); // null because it's a static field

         Object logicInstance = ReflectionUtils.invoke(constructionActionButton, "getLogic");
         ReflectionUtils.invoke(logicInstance, "o00000", stringFieldValue);
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

      LabelAPI workerCountLabel = Global.getSettings().createLabel("", Fonts.DEFAULT_SMALL);
      if (currentIndustry instanceof LtvBaseIndustry) {
         int assigned = ((LtvBaseIndustry) currentIndustry).getWorkerAssigned();
         if (((LtvBaseIndustry)currentIndustry).isWorkerAssignable()) {
            String assignedStr = LtvNumFormat.formatWithMaxDigits(assigned);

            workerCountLabel.setText(assignedStr);
            workerCountLabel.setHighlight(0, 5);
            workerCountLabel.setHighlightColor(Misc.getHighlightColor());
            workerCountLabel.highlightFirst(assignedStr);
            workerCountLabel.setOpacity(0.9f);
            ((d)workerCountLabel).autoSizeToWidth(100f);
         }
      }

      commodityDeficitIconGroup = new ooO0((U) null);
      commodityDeficitIconGroup.setMediumSpacing(true);
      int var3 = 0;
      for (Pair<String, Integer> deficitEntry : currentIndustry.getAllDeficit()) {
         CommodityOnMarketAPI commodity = market.getCommodityData(deficitEntry.one);

         if (deficitEntry.two < 1) { continue;}
         commodityDeficitIconGroup.addGroup((CommodityOnMarket)commodity,
               2, 1.0F, com.fs.starfarer.campaign.ui.marketinfo.f.o.values()[3], // õ00000
               null);

         if (deficitEntry.two > var3) {
            var3 = deficitEntry.two;
         }
      }

      float var17 = 24.0F;
      float var18 = 150.0F;
      commodityDeficitIconGroup.autoSizeWithAdjust(var17, var18, var17, var17);

      specialItemGroup = new ooO0((U) null);
      specialItemGroup.setWideSpacing(true);
      int var7 = 0;
      List<SpecialItemData> visibleItems = currentIndustry.getVisibleInstalledItems();
      for (SpecialItemData item : visibleItems) {
         S commodity = new S(item);
         specialItemGroup.addGroup(commodity, 1, 1f, com.fs.starfarer.campaign.ui.marketinfo.f.o.values()[7]/*if*/, color);
         var7++;
      }

      if (currentIndustry.getAICoreId() != null) {
         CommodityOnMarketAPI AICore = market.getCommodityData(currentIndustry.getAICoreId());
         specialItemGroup.addGroup((CommodityOnMarket)AICore, 1, 1f, com.fs.starfarer.campaign.ui.marketinfo.f.o.values()[7]/*if*/, color);
         ++var7;
      }

      if (var7 > 0) {
         specialItemGroup.autoSizeWithAdjust(ICON_SIZE, ICON_SIZE * (float) var7, ICON_SIZE, ICON_SIZE);
      }

      add(constructionActionButton).setSize(var1, IMAGE_HEIGHT).inBL(0.0F, 0.0F);
      add(buildingTitleHeader).inTL(0.0F, 0.0F);
      add(commodityDeficitIconGroup).inBL(PAD + 2.0F, PAD);
      add(specialItemGroup).inTR(PAD + 2.0F, PAD + buildingTitleHeader.getHeight() + PAD);
      add(((d)workerCountLabel)).inTL(PAD + 4f, PAD + 20f);

      boolean var15 = currentIndustry.isBuilding() || currentIndustry.isDisrupted();
      if (var15) {
         if (currentIndustry.isBuilding() && !currentIndustry.isUpgrading() && !currentIndustry.isDisrupted()) {
            String var16 = "graphics/fonts/insignia25LTaa.fnt";
            constructionStatusText = d.create("Building", var16, color);
            constructionStatusText.autoSize();
            add(constructionStatusText).inMid().setYAlignOffset(-buildingTitleHeader.getHeight() / 2.0F);
         }

         N var24 = new N((String) null, 0.0F, 100.0F);
         // Thanks to UTF-8, no need for reflection
         var24.getValue().getRenderer().o00000(D.Ò00000("graphics/fonts/victor10.fnt"));
         var24.getValue().getPosition().setYAlignOffset(1.0F);
         var24.setBarColor(Misc.interpolateColor(color, dark, 0.5F));
         var24.setTextColor(color);
         var24.setProgress(currentIndustry.getBuildOrUpgradeProgress() * 100.0F);
         var24.setText(currentIndustry.getBuildOrUpgradeProgressText());
         var24.setShowLabelOnly(true);
         float var14 = 12.0F;
         add(var24).setSize(var1, var14).inBL(0.0F, -var14 - 2.0F);
      }

      if (constructionQueueIndex >= 0) {
         setNormalMode();
      }

   }

   @Override
   public Fader getGlow() {
      return glowFader;
   }

   @Override
   public int getQueueIndex() {
      return constructionQueueIndex;
   }

   @Override
   public void clearLabels() {
      for (d label : labels) {
         remove(label);
      }
      labels.clear();
   }

   @Override
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
         d var2 = d.create("Click to remove", Fonts.DEFAULT_SMALL, color);
         d var3 = d.create(Misc.getDGSCredits((float) var1.cost) + " refund", Fonts.DEFAULT_SMALL, color);
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

   @Override
   public void setSwapMode() {
      clearLabels();
      d var1 = d.create("Click to swap", Fonts.DEFAULT_SMALL, color);
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

   @Override
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
            d var6 = d.create(var2 + " " + var3, Fonts.DEFAULT_SMALL, var5);
            var6.highlightFirst("" + var2);
            var6.setHighlightColor(var4);
            d var7 = d.create(Misc.getDGSCredits((float) var1.cost), Fonts.DEFAULT_SMALL, var5);
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
         try {
            O.o00000(var4, var5 + var7, var6, buildingTitleHeader.getHeight() + 3.0F, var9, var1, false);
         } catch (Exception e) {
            Global.getLogger(LtvMarketWidgetReplacer.class).error("Custom Widget failed", e);
         }
      }

      O.o00000(var4, var5, var6, var7, 1.0F, true, true, true, true, var9, var1);
      float var10 = glowFader.getBrightness();
      var10 = constructionActionButton.getGlowAmount();

      if (var10 > 0.0F) {
         // Color aColor = B.Ô00000(Color.white, var10 * 0.33F);
         Color aColor = (Color) ReflectionUtils.invoke(B.class, "Ô00000", Color.white, var10 * 0.33F);
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

      // super.renderImpl(var1);
      if (!(var1 <= 0.0F)) {
         this.updateCopyIfNeeded();
         if (this.isClipping()) {
            OO0O position = this.getPosition();
            float posX = position.getX();
            float posY = position.getY();
            float width = position.getWidth();
            float height = position.getHeight();
            K.o00000((int) posX, (int) posY, (int) width, (int) height);
         }

         for (com.fs.starfarer.ui.c instance : this.copy) {
            instance.render(var1);
         }

         if (this.isClipping()) {
            K.o00000();
         }
         /* function copy */
      }

      if (currentIndustry.isIndustry() && var10 > 0.0F) {
         // O.Ô00000(var4, var5 + var7, var6, buildingTitleHeader.getHeight() + 3.0F,
         // dark, var1 * var10 * 0.33F);
         ReflectionUtils.invoke(O.class, "Ô00000", var4, var5 + var7, var6,
         buildingTitleHeader.getHeight() + 3.0F, dark, var1 * var10 * 0.33F);
      }

   }

   @Override
   protected void advanceImpl(float var1) {
      glowFader.advance(var1);
      super.advanceImpl(var1);
   }

   @Override
   public void actionPerformed(Object objEventHandler, Object var2) {
      if (tradeInfoPanel != null) {
         return;
      }
      BuildingWidget var9;
      if (constructionQueueIndex >= 0) {
         try {
            Class<?> cClass = Class.forName("com.fs.starfarer.util.A.C");

            if (cClass.isInstance(objEventHandler)) {
               Object eventHandler = cClass.cast(objEventHandler);
               Object a = ReflectionUtils.invoke(eventHandler, "isRMBEvent");

               if ((boolean) a) {
                  for (intnew widget : IndustryPanel.getWidgets()) {
                     if (((BuildingWidget) widget).getQueueIndex() >= 0) {
                        ((BuildingWidget) widget).setNormalMode();
                     }
                  }

                  return;
               }
            }
         } catch (Exception e) {
            Global.getLogger(LtvMarketWidgetReplacer.class).error("Custom Widget failed", e);
         }

         if (constructionMode == intnew.Oo.Ó00000) {
            for (intnew widget : IndustryPanel.getWidgets()) {
               if (((BuildingWidget) widget).getQueueIndex() >= 0) {
                  if (((BuildingWidget) widget) == this) {
                     ((BuildingWidget) widget).setRemoveMode();
                  } else {
                     ((BuildingWidget) widget).setSwapMode();
                  }
               }
            }
         } else if (constructionMode == intnew.Oo.Ò00000) {
            var9 = null;

            for (intnew widget : IndustryPanel.getWidgets()) {
               if (((BuildingWidget) widget).getQueueIndex() >= 0
                     && ((BuildingWidget) widget).constructionMode == com.fs.starfarer.campaign.ui.marketinfo.intnew.Oo.String) {
                  var9 = (BuildingWidget) widget;
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
         } else if (constructionMode == intnew.Oo.String) {
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
         for (intnew widget : IndustryPanel.getWidgets()) {
            if (widget instanceof BuildingWidget && ((BuildingWidget) widget).getQueueIndex() >= 0) {
               ((BuildingWidget) widget).setNormalMode();
            }
         }

         try {
            T infoPanel = IndustryPanel.getOverview().getInfoPanel();
            Object a = ReflectionUtils.invoke(infoPanel, "getTradePanel");
            Object outpostPanelParams = ReflectionUtils.invoke(a, "getOutpostPanelParams");

            MarketAPI.MarketInteractionMode var14 = MarketInteractionMode.LOCAL;
            if (outpostPanelParams != null) {
               var14 = (MarketInteractionMode) ReflectionUtils.get(outpostPanelParams, "Õ00000");
            }

            b var17 = new b(currentIndustry, (intnew) ((Object) this), var14,
                  CampaignEngine.getInstance().getCampaignUI().getDialogParent(), this);
            var17.show(0.0F, 0.0F);
            tradeInfoPanel = com.fs.starfarer.campaign.ui.marketinfo.intnew.o.o00000;
         } catch (Exception e) {
            Global.getLogger(LtvMarketWidgetReplacer.class).error("Custom Widget failed", e);
         }
      }
   }

   @Override
   public void dialogDismissed(oo0O var1, int var2) {
      tradeInfoPanel = null;
   }

   @Override
   public n getButton() {
      return constructionActionButton;
   }

   public Industry getIndustry() {
      return currentIndustry;
   }

   @Override
   public IndustryListPanel getIndustryPanel() {
      return IndustryPanel;
   }
}

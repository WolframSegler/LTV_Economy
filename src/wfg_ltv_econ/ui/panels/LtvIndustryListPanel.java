package wfg_ltv_econ.ui.panels;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.econ.CommodityOnMarketAPI;
import com.fs.starfarer.api.campaign.econ.Industry;
import com.fs.starfarer.api.campaign.econ.Industry.IndustryTooltipMode;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.impl.campaign.DebugFlags;
import com.fs.starfarer.api.impl.campaign.econ.impl.ConstructionQueue.ConstructionQueueItem;
import com.fs.starfarer.api.ui.CutStyle;
import com.fs.starfarer.api.ui.UIComponentAPI;
import com.fs.starfarer.campaign.ui.marketinfo.IndustryListPanel;
import com.fs.starfarer.campaign.ui.marketinfo.s;
import com.fs.starfarer.ui.impl.StandardTooltipV2Expandable;
import com.fs.starfarer.ui.newui.L;

import wfg_ltv_econ.ui.plugins.IndustryPanelPlugin;
import wfg_ltv_econ.util.CommodityStats;
import wfg_ltv_econ.util.ReflectionUtils;
import wfg_ltv_econ.util.ReflectionUtils.ReflectedConstructor;

import com.fs.starfarer.ui.d;
import com.fs.starfarer.ui.Q;
import com.fs.starfarer.ui.c;
import com.fs.starfarer.api.ui.Alignment;
import com.fs.starfarer.api.campaign.econ.MutableCommodityQuantity;

import java.util.Collections;

public class LtvIndustryListPanel extends IndustryListPanel {
   static ReflectedConstructor indOptConstr = null;

   public LtvIndustryListPanel(MarketAPI var1, L var2, s var3) {
      super(var1, var2, var3);
   }

   public static Comparator<Industry> getIndustryOrderComparator() {
      return Comparator.comparingInt(ind -> ind.getSpec().getOrder());
   }

   @Override
   public void recreate() {
      this.sizeChanged(this.getWidth(), this.getHeight());
   }

   public List<LtvIndustryWidget> getWidgetsNew() {
      return new ArrayList<LtvIndustryWidget>();
   }

   public static void setindustryOptionsPanelConstructor(ReflectedConstructor a) {
      indOptConstr = a;

      Global.getLogger(LtvIndustryListPanel.class).error(a.getClass());
   }

   @Override
   public void sizeChanged(float var1, float var2) {
      clearChildren();
      // super.sizeChanged(var1, var2);
      if (!this.created) {
         this.afterSizeFirstChanged(var1, var2);
         this.created = true;
      }
      /* Grandparent Code Block */
      widgets.clear();
      List<Industry> industries = CommodityStats.getVisibleIndustries(market);
      Collections.sort(industries, getIndustryOrderComparator());
      List<ConstructionQueueItem> queuedIndustries = market.getConstructionQueue().getItems();
      float opad = 20.0F;
      byte rowAmount = 3;
      byte colunmAmount = 4;

      for (int i = 0; i < colunmAmount; i++) {
         for (int j = 0; j < rowAmount; j++) {
            int var14 = j * colunmAmount + i;
            if (var14 >= (industries.size() + queuedIndustries.size())) {
               continue;
            }
            if (var14 < industries.size()) {
               Industry var15 = (Industry) industries.get(var14);
               BuildingWidgetPanel var16 = new BuildingWidgetPanel(market, var15, this);
               LtvIndustryWidget panelo = new LtvIndustryWidget(
                  this, this, new IndustryPanelPlugin(), market, var15, this
               );
               add((c)panelo.getPanel()).inTL((float) i * (BuildingWidgetPanel.WIDTH + opad), (float) j * (BuildingWidgetPanel.HEIGHT + opad) - 130);
               widgets.add(var16);
               add(var16).setSize(BuildingWidgetPanel.WIDTH, BuildingWidgetPanel.HEIGHT).inTL((float) i * (BuildingWidgetPanel.WIDTH + opad), (float) j * (BuildingWidgetPanel.HEIGHT + opad));

               StandardTooltipV2Expandable.addTooltipRight(var16,
                     BuildingWidgetPanel.createIndustryTooltip(IndustryTooltipMode.NORMAL, var15));
            } else {
               var14 -= industries.size();
               ConstructionQueueItem var21 = (ConstructionQueueItem) queuedIndustries.get(var14);
               Industry var23 = market.instantiateIndustry(var21.id);
               BuildingWidgetPanel var17 = new BuildingWidgetPanel(market, var23, this, var14);
               widgets.add(var17);
               add(var17).setSize(BuildingWidgetPanel.WIDTH, BuildingWidgetPanel.HEIGHT).inTL((float) i * (BuildingWidgetPanel.WIDTH + opad), (float) j * (BuildingWidgetPanel.HEIGHT + opad));
               StandardTooltipV2Expandable.addTooltipRight(var17, BuildingWidgetPanel.createIndustryTooltip(IndustryTooltipMode.QUEUED, var23));
            }
         }
      }

      d var18 = d.createCreditsLabel("graphics/fonts/insignia21LTaa.fnt", 25.0F);
      d var19 = d.createMaxIndustriesLabel("graphics/fonts/insignia21LTaa.fnt", 25.0F, market);
      addTooltips(var18, var19, market);
      build = Q.o00000("   Add industry or structure...", "graphics/fonts/orbitron20aabold.fnt", dark, color,
      Alignment.LMID, CutStyle.TL_BR, this);

      try {
         // build.setShortcut(oo.øô0000, true);
         Class<?> clazz = Class.forName("com.fs.starfarer.title.OoOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOO.OoOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOO$oo");
         Object enumValue = ReflectionUtils.getFieldsMatching(clazz, "øô0000", null, null)
               .get(0)
               .get(null); // null because it's a static field
         ReflectionUtils.invoke(build, "setShortcut", enumValue, true);
      } catch (Exception e) {
         Global.getLogger(getClass()).error("Failed to replace IndustryListPanel", e);
      }

      float var20 = 350.0F;
      float var22 = 25.0F;
      add(build).setSize(var20, var22).inBL(0.0F, 50.0F);
      add(var18).rightOfMid(build, 70.0F);
      add(var19).inBR(40.0F, 50.0F);
      if (!DebugFlags.COLONY_DEBUG && !market.isPlayerOwned()) {
         build.setEnabled(false);
         if (DebugFlags.HIDE_COLONY_CONTROLS) {
            build.setOpacity(0.0F);
            var18.setOpacity(0.0F);
         }
      }

      CommodityStats.recalculateMaxDemandAndSupplyForAll(market);
   }
}

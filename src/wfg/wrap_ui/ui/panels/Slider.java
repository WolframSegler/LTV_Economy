package wfg.wrap_ui.ui.panels;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.SettingsAPI;
import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.graphics.Object;
import com.fs.graphics.oOoO;
import com.fs.graphics.util.B;
import com.fs.graphics.util.Fader;
import com.fs.graphics.util.GLListManager;
import com.fs.graphics.util.GLListManager.GLListToken;
import com.fs.starfarer.O0OO;
import com.fs.starfarer.api.ui.Alignment;
import com.fs.starfarer.api.ui.Fonts;
import com.fs.starfarer.api.ui.LabelAPI;
import com.fs.starfarer.api.ui.UIPanelAPI;
import com.fs.starfarer.prototype.Utils;
import com.fs.starfarer.renderers.E;
import com.fs.starfarer.renderers.O;
import com.fs.starfarer.settings.StarfarerSettings;
import com.fs.starfarer.ui.OO0O;
import com.fs.starfarer.ui.W;
import com.fs.starfarer.ui.d;
import com.fs.starfarer.ui.interfacenew;
import com.fs.starfarer.util.A.C;

import wfg.wrap_ui.ui.plugins.BasePanelPlugin;

import java.awt.Color;
import java.util.Iterator;
import org.lwjgl.opengl.GL11;

public class Slider extends CustomPanel<BasePanelPlugin<Slider>, Slider, UIPanelAPI>{
	private float minRange = 0f;
	private float maxRange = 1f;
	private float cachedMaxValue = 1f;
	private float progressValue = 0f;
	private float cachedProgressValue = 0f;
	private float minValue = 0f;
	private float maxValue = Float.MAX_VALUE;
	private float cachedMax = Float.MAX_VALUE;
	private float cachedMin = 0f;
	private float potentialDecreaseAmount = 0f;
	private float cachedPotentialDecreaseAmount = 0f;
	private float CachedShowNotchOnIfBelowProgress = -3.4028235E38f;
	private float showNotchOnIfBelowProgress = -3.4028235E38f;
	private LabelAPI label;
	private Object lineTexture;
	private String labelText = null;
	private Fader barHighlightFader = null;
	private boolean userAdjustable = false;
	private Color barColor;
	private Color barColorOverflow;
	private Color labelColor;
	private Color labelValueColor;
	private Color bonusColor;
	private float bonusAmount = 0f;
	private boolean shouldInterpolateCachedValues = false;
	private float flashOnOverflowFraction = Float.MAX_VALUE;
	private Fader flashOnOverflowFader = null;
	private SpriteAPI bgGlowTexture;
	private boolean showNoText = false;
	private boolean showValueOnly = false;
	private boolean clampCurrToMax = false;
	private boolean showDecimalForValueOnlyMode = false;
	private GLListToken GLListToken;
	private int numSubivisions = 0;
	private boolean lineUpTextOnCenter = false;
	private float lineUpTextOnCenterWidth = 0f;
	private boolean showPercent = false;
	private boolean showPercentAndTitle = false;
	private float scrollSpeed = 100f;
	private boolean showLabelOnly = false;
	private boolean roundBarValue = false;
	private int roundingIncrement = 1;
	private Color widgetColor;
	private boolean showAdjustableIndicator = false;
	private boolean highlightBrightnessOverride = false;
	private float highlightBrightnessOverrideValue = -1f;
	private float cachedAlphaMult = -1f;
	private float cachedHighlightBrightness = -1f;

   	public Slider(String initialText, float minRange, float maxRange) {
		super(m_panel, m_parent, roundingIncrement, numSubivisions, m_plugin, getMarket());
		final SettingsAPI settings = Global.getSettings();
		barColor = new Color(107, 175, 0, 255);
		barColorOverflow = settings.getColor("progressBarOverflowColor");
		labelColor = settings.getColor("standardTextColor");
		labelValueColor = settings.getColor("standardTextColor");
		bonusColor = settings.getColor("textFriendColor");
		widgetColor = settings.getColor("widgetBorderColorBright");
		labelText = initialText;
		this.minRange = minRange;
		this.maxRange = maxRange;
		bgGlowTexture = settings.getSprite("ui", "scanline11");
		bgGlowTexture.setSize(10f, 10f);
		label = settings.createLabel("", Fonts.DEFAULT_SMALL);
		label.setColor(settings.getColor("standardTextColor"));
		label.setHighlightOnMouseover(true);
		label.setAlignment(Alignment.MID);
		add(label).inMid();
		setProgress(progressValue);
		lineTexture = oOoO.Ò00000("graphics/hud/line4x4.png");
		cachedMaxValue = maxRange;
	}

	public void initializePlugin(boolean hasPlugin) {

	}

	public void createPanel() {

	}

	public Color getBonusColor() {
		return bonusColor;
	}

	public void setBonusColor(Color color) {
		bonusColor = color;
	}

	public float getBonusAmount() {
		return bonusAmount;
	}

	public void setBonusAmount(float amount) {
		bonusAmount = amount;
	}

	public void setShowDecimalForValueOnlyMode(boolean bool) {
		showDecimalForValueOnlyMode = bool;
	}

	public int getNumSubivisions() {
		return numSubivisions;
	}

	public void setNumSubivisions(int subdivisions) {
		numSubivisions = subdivisions;
	}

	public void setFlashOnOverflowFraction(float fraction) {
		flashOnOverflowFraction = fraction;
	}

	public boolean isShowNoText() {
		return showNoText;
	}

	public void setClampCurrToMax(boolean bool) {
		clampCurrToMax = bool;
	}

	public void setShowNoText(boolean bool) {
		showNoText = bool;
	}

	public void setShowValueOnly(boolean bool) {
		showValueOnly = bool;
	}

	public float getRangeMin() {
		return minRange;
	}

   public void setPotentialDecreaseAmount(float var1) {
      if (potentialDecreaseAmount != var1) {
         GLListManager.invalidateList(GLListToken);
      }

      potentialDecreaseAmount = var1;
   }

   public float getShowNotchOnIfBelowProgress() {
      return showNotchOnIfBelowProgress;
   }

   public void setShowNotchOnIfBelowProgress(float var1) {
      if (showNotchOnIfBelowProgress != var1) {
         GLListManager.invalidateList(GLListToken);
      }

      showNotchOnIfBelowProgress = var1;
   }

   public void setHighlightOnMouseover(boolean var1) {
      if (var1) {
         barHighlightFader = new Fader(0.05F, 0.25F);
      } else {
         barHighlightFader = null;
      }

   }

   public d getValue() {
      return label;
   }

   public void setUserAdjustable(boolean var1) {
      userAdjustable = var1;
      setHighlightOnMouseover(var1);
   }

   public float getRangeMax() {
      return maxRange;
   }

   public float getProgress() {
      return progressValue;
   }

   public void setText(String var1) {
      labelText = var1;
   }

   public void sizeChanged(float var1, float var2) {
      super.sizeChanged(var1, var2);
   }

   public void setLineUpTextOnCenter(boolean var1, float var2) {
      lineUpTextOnCenter = var1;
      lineUpTextOnCenterWidth = var2;
   }

   public void setShowPercent(boolean var1) {
      showPercent = var1;
   }

   public void setShowPercentAndTitle(boolean var1) {
      showPercentAndTitle = var1;
   }

   public void setScrollSpeed(float var1) {
      scrollSpeed = var1;
   }

   public boolean isShowLabelOnly() {
      return showLabelOnly;
   }

   public void setShowLabelOnly(boolean var1) {
      showLabelOnly = var1;
   }

   public Fader getBarHighlightFader() {
      return barHighlightFader;
   }

   protected void advanceImpl(float var1) {
      super.advanceImpl(var1);
      if (roundBarValue) {
         cachedProgressValue = (float)(Math.round(cachedProgressValue / (float)roundingIncrement) * roundingIncrement);
         if (roundingIncrement != 1) {
            cachedProgressValue = (float)(Math.round(cachedProgressValue / (float)roundingIncrement) * roundingIncrement);
            progressValue = (float)(Math.round(progressValue / (float)roundingIncrement) * roundingIncrement);
         }
      }

      if (cachedProgressValue != progressValue || cachedMin != minValue || cachedMax != maxValue || cachedMaxValue != maxRange || showNotchOnIfBelowProgress != CachedShowNotchOnIfBelowProgress) {
         GLListManager.invalidateList(GLListToken);
      }

      if (barHighlightFader != null) {
         barHighlightFader.advance(var1);
      }

      if (flashOnOverflowFader != null) {
         flashOnOverflowFader.advance(var1);
      }

      if (clampCurrToMax && progressValue > maxValue) {
         progressValue = maxValue;
      }

      float var2 = 0.0F;
      if (maxRange <= 0.0F) {
         var2 = 10.0F;
      } else {
         var2 = (progressValue - minRange) / maxRange - 1.0F;
         if (var2 < 0.0F) {
            var2 = 0.0F;
         }
      }

      if (var2 > this.flashOnOverflowFraction) {
         if (this.flashOnOverflowFader == null) {
            this.flashOnOverflowFader = new Fader(0.25F, 0.25F);
            this.flashOnOverflowFader.setBounce(true, true);
            this.flashOnOverflowFader.fadeIn();
         }
      } else if (this.flashOnOverflowFader != null) {
         this.flashOnOverflowFader.setBounceUp(false);
         this.flashOnOverflowFader.fadeOut();
         if (this.flashOnOverflowFader.isFadedOut()) {
            this.flashOnOverflowFader = null;
         }
      }

      float var3;
      if (this.shouldInterpolateCachedValues) {
         this.scrollSpeed = 100.0F;
         var3 = this.getPosition().getWidth() / Math.maxValue(this.progressValue - this.minRange, this.maxRange - this.minRange);
         float var4 = this.cachedMaxValue - this.minRange;
         if (this.cachedProgressValue - this.minRange > var4) {
            var4 = this.cachedProgressValue - this.minRange;
         }

         if (1.0F > var4) {
            var4 = 1.0F;
         }

         var3 = getPos().getWidth() / var4;
         this.cachedProgressValue = Utils.o00000(this.cachedProgressValue, this.progressValue, this.scrollSpeed / var3, 0.02F * Math.abs(this.cachedProgressValue - this.progressValue) * var3, var1);
         this.cachedMin = Utils.o00000(this.cachedMin, this.minValue, this.scrollSpeed / var3, 0.02F * Math.abs(this.cachedMin - this.minValue) * var3, var1);
         this.cachedMax = Utils.o00000(this.cachedMax, this.maxValue, this.scrollSpeed / var3, 0.05F * Math.abs(this.cachedMax - this.maxValue) * var3, var1);
         this.cachedPotentialDecreaseAmount = Utils.o00000(this.cachedPotentialDecreaseAmount, this.potentialDecreaseAmount, this.scrollSpeed / var3, 0.05F * Math.abs(this.cachedPotentialDecreaseAmount - this.potentialDecreaseAmount) * var3, var1);
         this.cachedMaxValue = Utils.o00000(this.cachedMaxValue, this.maxRange, this.scrollSpeed / var3, 0.02F * Math.abs(this.cachedMaxValue - this.maxRange) * var3, var1);
         this.CachedShowNotchOnIfBelowProgress = Utils.o00000(this.CachedShowNotchOnIfBelowProgress, this.showNotchOnIfBelowProgress, this.scrollSpeed / var3, 0.05F * Math.abs(this.CachedShowNotchOnIfBelowProgress - this.showNotchOnIfBelowProgress) * var3, var1);
      } else {
         this.cachedProgressValue = this.progressValue;
         this.cachedMin = this.minValue;
         this.cachedMax = this.maxValue;
         this.cachedMaxValue = this.maxRange;
         this.cachedPotentialDecreaseAmount = this.potentialDecreaseAmount;
         this.CachedShowNotchOnIfBelowProgress = this.showNotchOnIfBelowProgress;
      }

      this.shouldInterpolateCachedValues = false;
      var3 = 0.0F;
      String var5 = "";
      if (this.showLabelOnly) {
         this.label.setText(this.labelText);
      } else if (this.showPercent) {
         var5 = (long)Math.round(this.cachedProgressValue) + "%";
         this.label.setText(String.format("%s", var5));
      } else if (this.showPercentAndTitle) {
         var5 = (long)Math.round(this.cachedProgressValue) + "%";
         this.label.setText(String.format("%s: %s", this.labelText, var5));
      } else if (this.showValueOnly) {
         if (this.showDecimalForValueOnlyMode) {
            var5 = String.format("%2.2f", this.cachedProgressValue);
         } else {
            var5 = "" + (long)this.cachedProgressValue;
         }

         this.label.setText(var5);
      } else if (this.labelText != null) {
         var5 = String.format("% 1d / % 1d", (long)Math.round(this.cachedProgressValue), (long)this.cachedMaxValue);
         this.label.setText(String.format("%s: %s", this.labelText, var5));
         var3 = this.label.getRenderer().Õ00000(this.labelText);
      } else {
         var5 = String.format("%d / %d", (long)Math.round(this.cachedProgressValue), (long)this.cachedMaxValue);
         this.label.setText(String.format("%s", var5));
      }

      if (this.showNoText) {
         this.label.setText("");
      }

      this.label.autoSize();
      this.label.setColor(this.labelColor);
      this.label.getRenderer().Object(this.labelValueColor);
      this.label.getRenderer().Ô00000(var5);
      if (var3 != 0.0F && this.lineUpTextOnCenter) {
         this.label.getPosition().setXAlignOffset(this.label.getWidth() / 2.0F - var3 + this.lineUpTextOnCenterWidth);
      }

      this.label.getPosition().recompute();
      if (this.cachedProgressValue < this.cachedMin) {
         this.cachedProgressValue = this.cachedMin;
      }

   }

   public void setBarColor(Color var1) {
      if (!this.barColor.equals(var1)) {
         GLListManager.invalidateList(this.GLListToken);
      }

      this.barColor = var1;
   }

   public Color getBarColor() {
      return this.barColor;
   }

   public void setBarColorOverflow(Color var1) {
      if (!this.barColorOverflow.equals(var1)) {
         GLListManager.invalidateList(this.GLListToken);
      }

      this.barColorOverflow = var1;
   }

   public Fader getHighlight() {
      return this.barHighlightFader;
   }

   protected void processInputImpl(new var1) {
      super.processInputImpl(var1);
      if (this.isEnabled() && (this.userAdjustable || this.barHighlightFader != null)) {
         Iterator var3 = var1.iterator();

         while(true) {
            while(true) {
               C var2;
               do {
                  do {
                     if (!var3.hasNext()) {
                        return;
                     }

                     var2 = (C)var3.next();
                  } while(var2.isConsumed());

                  if ((!var2.isMouseMoveEvent() || !this.getPosition().containsEvent(var2)) && W.Ó00000() != this) {
                     this.barHighlightFader.fadeOut();
                  } else {
                     this.barHighlightFader.isFadedOut();
                     this.barHighlightFader.fadeIn();
                  }
               } while(!this.userAdjustable);

               if (var2.isLMBDownEvent() && this.getPosition().containsEvent(var2) && W.Ó00000() == null) {
                  W.class(this);
                  this.Ö00000(var2);
                  var2.consume();
                  var1.Ò00000();
               } else if (var2.isMouseMoveEvent() && W.Ó00000() == this) {
                  this.Ö00000(var2);
                  var2.consume();
                  var1.Ò00000();
               } else if (var2.isLMBUpEvent() && W.Ó00000() == this) {
                  this.Ö00000(var2);
                  var2.consume();
                  W.o00000(this);
                  var1.Ò00000();
               }
            }
         }
      }
   }

   private float Ö00000(C var1) {
      float var2 = (float)var1.getX() - this.getX() - 6.0F;
      if (var2 > this.getWidth() - 6.0F) {
         var2 = this.getWidth() - 6.0F;
      }

      float var3 = this.getWidth() - 6.0F;
      float var4 = var3 * this.cachedMin / (this.cachedMaxValue - this.minRange);
      if (var2 < var4) {
         var2 = var4;
      }

      float var5 = var2 / (this.getWidth() - 6.0F) * (this.getRangeMax() - this.minRange) + this.minRange;
      if (this.clampCurrToMax && var5 > this.maxValue) {
         var5 = this.maxValue;
      }

      this.setProgress(var5);
      this.forceSync();
      return var5;
   }

   public float getXCoordinateForProgressValue(float var1) {
      OO0O var2 = this.getPosition();
      float var3 = 0.0F;
      float var4 = var2.getWidth();
      float var5 = var3 + 3.0F;
      float var6 = var4 - 8.0F;
      float var7 = var6 * (this.cachedProgressValue - this.minRange) / (this.maxRange - this.minRange);
      float var8 = var7 - 2.0F - (var5 - var3 - 6.0F);
      float var9 = var6 * (var1 - this.minRange) / (this.maxRange - this.minRange);
      float var10 = var9 - var8;
      return var5 + var8 + 2.0F + var10 + 1.5F;
   }

   public void setHighlightBrightnessOverride(float var1) {
      this.highlightBrightnessOverrideValue = var1;
      this.highlightBrightnessOverride = true;
   }

   public void setRoundBarValue(boolean var1) {
      this.roundBarValue = var1;
   }

   public int getRoundingIncrement() {
      return this.roundingIncrement;
   }

   public void setRoundingIncrement(int var1) {
      this.roundingIncrement = var1;
   }

   public boolean isShowAdjustableIndicator() {
      return this.showAdjustableIndicator;
   }

   public void setShowAdjustableIndicator(boolean var1) {
      this.showAdjustableIndicator = var1;
   }

   public Color getWidgetColor() {
      return this.widgetColor;
   }

   public void setWidgetColor(Color var1) {
      this.widgetColor = var1;
   }

   protected void renderImpl(float alphaMult) {
      float var2 = this.cachedProgressValue;
      if (this.roundBarValue) {
         var2 = (float)(Math.round(var2 / (float)this.roundingIncrement) * this.roundingIncrement);
         if (this.roundingIncrement != 1) {
            this.cachedProgressValue = (float)(Math.round(var2 / (float)this.roundingIncrement) * this.roundingIncrement);
         }
      }

      float var3 = -1.0F;
      if (this.barHighlightFader != null) {
         var3 = this.barHighlightFader.getBrightness();
      }

      if (this.highlightBrightnessOverride) {
         var3 = this.highlightBrightnessOverrideValue;
      }

      if (alphaMult != this.cachedAlphaMult || var3 != this.cachedHighlightBrightness) {
         GLListManager.invalidateList(this.GLListToken);
      }

      this.cachedAlphaMult = alphaMult;
      this.cachedHighlightBrightness = var3;
      this.shouldInterpolateCachedValues = true;
      OO0O var4 = this.getPosition();
      float var5 = var4.getX();
      float var6 = var4.getY();
      float var7 = var4.getWidth();
      float var8 = var4.getHeight();
      GL11.glPushMatrix();
      GL11.glTranslatef(var5, var6, 0.0F);
      var6 = 0.0F;
      var5 = 0.0F;
      if (!GLListManager.callList(this.GLListToken)) {
         this.GLListToken = GLListManager.beginList();
         O.Ó00000(var5, var6, var7, var8, Color.black, alphaMult);
         Color var9 = this.widgetColor;
         float var10 = var5 + 3.0F;
         float var11 = var7 - 8.0F;
         if (var8 <= 5.0F) {
            --var10;
            var11 += 2.0F;
         }

         float var12 = var11 * this.cachedMin / (this.cachedMaxValue - this.minRange);
         float var13 = var11 * (this.cachedMaxValue - var2) / (this.cachedMaxValue - this.minRange);
         float var10000 = var10 + var12;
         float var15 = var11 * (var2 - this.minRange) / (this.cachedMaxValue - this.minRange);
         var10000 = var10 + var15;
         float var17 = 0.0F;
         float var18;
         if (var2 > this.cachedMaxValue) {
            var12 = var11 * this.cachedMin / (var2 - this.minRange);
            var10000 = var10 + var12;
            var15 = var11 * this.cachedMaxValue / (var2 - this.minRange);
            var10000 = var10 + var15;
            var18 = var2 - this.cachedMaxValue;
            var17 = var11 * var18 / (var2 - this.minRange);
            var13 = 0.0F;
         }

         if (var8 <= 5.0F) {
            E.o00000(var5, var6, var5, var6 + var8, 2.0F, var9, 0.5F * alphaMult, 0.5F * alphaMult, 0.5F * alphaMult, false);
            E.o00000(var5 + 1.0F, var6, var5 + 1.0F, var6 + var8, 2.0F, var9, 1.0F * alphaMult, 1.0F * alphaMult, alphaMult, true);
            E.o00000(var5 + var7, var6, var5 + var7, var6 + var8, 2.0F, var9, 0.5F * alphaMult, 0.5F * alphaMult, 0.5F * alphaMult, false);
            E.o00000(var5 + var7 - 1.0F, var6, var5 + var7 - 1.0F, var6 + var8, 2.0F, var9, 1.0F * alphaMult, 1.0F * alphaMult, alphaMult, true);
         } else {
            E.o00000(var5, var6, var5, var6 + var8, 2.0F, var9, 0.5F * alphaMult, 0.5F * alphaMult, 0.5F * alphaMult, false);
            E.o00000(var5 + 1.0F, var6, var5 + 1.0F, var6 + var8, 2.0F, var9, 1.0F * alphaMult, 1.0F * alphaMult, alphaMult, true);
            E.o00000(var5 + 1.0F, var6, var5 + 15.0F, var6, 1.0F, var9, 1.0F * alphaMult, 0.5F * alphaMult, 0.0F * alphaMult, false);
            E.o00000(var5 + 1.0F, var6 + var8 - 1.0F, var5 + 15.0F, var6 + var8, 1.0F, var9, 1.0F * alphaMult, 0.5F * alphaMult, 0.0F * alphaMult, false);
            E.o00000(var5 + var7, var6, var5 + var7, var6 + var8, 2.0F, var9, 0.5F * alphaMult, 0.5F * alphaMult, 0.5F * alphaMult, false);
            E.o00000(var5 + var7 - 1.0F, var6, var5 + var7 - 1.0F, var6 + var8, 2.0F, var9, 1.0F * alphaMult, 1.0F * alphaMult, alphaMult, true);
            E.o00000(var5 + var7 - 1.0F, var6, var5 + var7 - 15.0F, var6, 1.0F, var9, 1.0F * alphaMult, 0.5F * alphaMult, 0.0F * alphaMult, false);
            E.o00000(var5 + var7 - 1.0F, var6 + var8 - 1.0F, var5 + var7 - 15.0F, var6 + var8, 1.0F, var9, 1.0F * alphaMult, 0.5F * alphaMult, 0.0F * alphaMult, false);
         }

         var9 = this.widgetColor;
         if (var12 > 0.0F) {
            var18 = Math.minValue(10.0F, var12);
            E.o00000(this.lineTexture, var10 - 2.0F, var6 + 1.0F, var10 - 2.0F + var18, var6 + 1.0F, 1.0F, var9, 0.0F, 0.5F * alphaMult, alphaMult, false);
            E.o00000(this.lineTexture, var10 - 2.0F + var18, var6 + 1.0F, var10 + var12 - 1.5F, var6 + 1.0F, 1.0F, var9, alphaMult, alphaMult, alphaMult, false);
            E.o00000(this.lineTexture, var10 - 2.0F, var6 + var8 - 2.0F, var10 - 2.0F + var18, var6 + var8 - 2.0F, 1.0F, var9, 0.0F, 0.5F * alphaMult, alphaMult, false);
            E.o00000(this.lineTexture, var10 - 2.0F + var18, var6 + var8 - 2.0F, var10 + var12 - 1.5F, var6 + var8 - 2.0F, 1.0F, var9, alphaMult, alphaMult, alphaMult, false);
            E.o00000(this.lineTexture, var10 + var12 - 1.5F, var6 + 1.0F, var10 + var12 - 1.5F, var6 + var8 - 1.0F, 1.0F, var9, alphaMult, alphaMult, alphaMult, false);
            E.o00000(this.lineTexture, var10 + var12 - 1.5F, var6 + 1.0F, var10 + var12 - 1.5F, var6 + var8 - 1.0F, 1.0F, Color.white, 0.0F, alphaMult, 0.0F, true);
            var10 += var12;
         }

         float var19;
         if (var13 > 0.0F) {
            var18 = Math.minValue(10.0F, var13);
            var19 = var5 + 6.0F + var11;
            if (var8 <= 5.0F) {
               E.o00000(this.lineTexture, var19 + 0.0F, var6 + 1.0F, var19 - var13 - 1.0F, var6 + 1.0F, 1.0F, var9, alphaMult, alphaMult, alphaMult, false);
               E.o00000(this.lineTexture, var19 + 0.0F, var6 + var8 - 2.0F, var19 - var13 - 1.0F, var6 + var8 - 2.0F, 1.0F, var9, alphaMult, alphaMult, alphaMult, false);
            } else if (this.numSubivisions <= 0) {
               E.o00000(this.lineTexture, var19 + 2.0F, var6 + 1.0F, var19 + 2.0F - var18, var6 + 1.0F, 1.0F, var9, 0.0F, 0.5F * alphaMult, alphaMult, false);
               E.o00000(this.lineTexture, var19 + 2.0F - var18, var6 + 1.0F, var19 - var13 + 1.5F, var6 + 1.0F, 1.0F, var9, alphaMult, alphaMult, alphaMult, false);
               E.o00000(this.lineTexture, var19 + 2.0F, var6 + var8 - 2.0F, var19 + 2.0F - var18, var6 + var8 - 2.0F, 1.0F, var9, 0.0F, 0.5F * alphaMult, alphaMult, false);
               E.o00000(this.lineTexture, var19 + 2.0F - var18, var6 + var8 - 2.0F, var19 - var13 + 1.5F, var6 + var8 - 2.0F, 1.0F, var9, alphaMult, alphaMult, alphaMult, false);
               E.o00000(this.lineTexture, var19 - var13 + 1.5F, var6 + 1.0F, var19 - var13 + 1.5F, var6 + var8 - 1.0F, 1.0F, var9, alphaMult, alphaMult, alphaMult, false);
               E.o00000(this.lineTexture, var19 - var13 + 1.5F, var6 + 1.0F, var19 - var13 + 1.5F, var6 + var8 - 1.0F, 1.0F, Color.white, 0.0F, alphaMult, 0.0F, true);
            }
         }

         if (!(var15 - var12 > 0.0F) && this.cachedMax < this.cachedMaxValue) {
         }

         var18 = var15 - 2.0F - (var10 - var5 - 6.0F);
         var19 = 0.0F;
         if (this.barHighlightFader != null || this.highlightBrightnessOverride) {
            var19 += var3 * 1.0F;
            this.highlightBrightnessOverride = false;
         }

         float var20;
         float var21;
         if (this.cachedMax < this.cachedMaxValue && this.cachedMax > var2) {
            if (var18 > 0.0F) {
               if (this.CachedShowNotchOnIfBelowProgress < var2 && this.CachedShowNotchOnIfBelowProgress >= this.minRange) {
                  var20 = var11 * (this.CachedShowNotchOnIfBelowProgress - this.minRange) / (this.cachedMaxValue - this.minRange);
                  var21 = var20 - var18 + 3.5F - 2.0F;
                  var10000 = var20 - var18;
                  var21 = this.getXCoordinateForProgressValue(this.CachedShowNotchOnIfBelowProgress) - var10 - var18;
                  if (-var21 <= 2.0F) {
                     O.o00000(var10, var6 + 1.0F, var18, var8 - 2.0F, this.barColor, alphaMult, var19);
                  } else {
                     O.o00000(var10, var6 + 1.0F, var18 + var21, var8 - 2.0F, this.barColor, alphaMult, var19);
                     if (-var21 > 0.0F) {
                        O.o00000(var10 + var18 + var21, var6 + 1.0F, -var21, var8 - 2.0F, this.barColor, alphaMult * 0.7F, var19, false);
                     }
                  }
               } else {
                  O.o00000(var10, var6 + 1.0F, var18, var8 - 2.0F, this.barColor, alphaMult, var19);
               }
            }

            var20 = var11 * (this.cachedMax - this.minRange) / (this.cachedMaxValue - this.minRange);
            var21 = var20 - var18;
            if (var21 > 0.0F) {
               O.o00000(var10 + var18 + 4.0F, var6 + 3.0F, var21, var8 - 6.0F, this.barColor, alphaMult * 0.65F, var19 * 1.0F, true);
            }

            E.o00000(this.lineTexture, var10 + var18 + 4.0F + var21 + 1.5F, var6 + 2.0F, var10 + var18 + 4.0F + var21 + 1.5F, var6 + var8 - 2.0F, 1.0F, var9, alphaMult, alphaMult, alphaMult, false);
         } else {
            if (var18 > 0.0F) {
               if (this.cachedMax <= var2 && var2 < this.maxRange - 1.0F && this.cachedMax >= var2 - 1.0F) {
                  var20 = var11 * (this.cachedMax - this.minRange) / (this.cachedMaxValue - this.minRange);
                  var21 = var20 - var18;
                  E.o00000(this.lineTexture, var10 + var18 + 4.0F + var21 + 1.5F, var6 + 2.0F, var10 + var18 + 4.0F + var21 + 1.5F, var6 + var8 - 2.0F, 1.0F, var9, alphaMult, alphaMult, alphaMult, false);
               }

               if (this.CachedShowNotchOnIfBelowProgress < var2 && this.CachedShowNotchOnIfBelowProgress >= this.minRange) {
                  var20 = var11 * (this.CachedShowNotchOnIfBelowProgress - this.minRange) / (this.cachedMaxValue - this.minRange);
                  var21 = var20 - var18 + 3.5F - 2.0F;
                  var10000 = var20 - var18;
                  var21 = this.getXCoordinateForProgressValue(this.CachedShowNotchOnIfBelowProgress) - var10 - var18;
                  if (-var21 <= 2.0F) {
                     O.o00000(var10, var6 + 1.0F, var18, var8 - 2.0F, this.barColor, alphaMult, var19);
                  } else {
                     O.o00000(var10, var6 + 1.0F, var18 + var21, var8 - 2.0F, this.barColor, alphaMult, var19);
                     if (-var21 > 0.0F) {
                        O.o00000(var10 + var18 + var21, var6 + 1.0F, -var21, var8 - 2.0F, this.barColor, alphaMult * 0.7F, var19, false);
                     }
                  }
               } else if (this.cachedMax < this.cachedMaxValue) {
                  var20 = var11 * (this.cachedMax - this.minRange) / (this.cachedMaxValue - this.minRange);
                  var21 = var20 - var18 + 3.5F - 2.0F;
                  var10000 = var20 - var18;
                  var21 = this.getXCoordinateForProgressValue(this.cachedMax) - var10 - var18;
                  if (-var21 <= 2.0F) {
                     O.o00000(var10, var6 + 1.0F, var18, var8 - 2.0F, this.barColor, alphaMult, var19);
                  } else {
                     O.o00000(var10, var6 + 1.0F, var18 + var21 + 2.0F, var8 - 2.0F, this.barColor, alphaMult, var19);
                     if (-var21 > 0.0F) {
                        O.o00000(var10 + var18 + var21 + 2.0F, var6 + 1.0F, -var21 - 2.0F, var8 - 2.0F, this.barColor, alphaMult * 0.7F, var19, false);
                     }
                  }
               } else {
                  O.o00000(var10, var6 + 1.0F, var18, var8 - 2.0F, this.barColor, alphaMult, var19);
               }
            }

            if (!(this.cachedMax < this.cachedMaxValue)) {
               var20 = var11 * (this.cachedMaxValue - this.minRange) / (this.cachedMaxValue - this.minRange);
               var21 = var20 - var18 - 4.0F - var17;
               if (var21 > 0.0F) {
                  O.o00000(var10 + var18 + 5.0F, var6 + 3.0F, var21, var8 - 6.0F, this.barColor, alphaMult * 0.65F, var19 * 1.0F, true);
               }
            }
         }

         if (this.showNotchOnIfBelowProgress < this.progressValue) {
         }

         if (this.cachedPotentialDecreaseAmount > 0.0F) {
            var20 = Math.maxValue(0.0F, var2 - this.cachedPotentialDecreaseAmount);
            var21 = var11 * (var2 - this.cachedPotentialDecreaseAmount - this.minRange) / (this.cachedMaxValue - this.minRange);
            float var22 = var11 * (var20 - this.minRange) / (this.cachedMaxValue - this.minRange);
            E.o00000(this.lineTexture, var5 + var22 + 5.5F, var6, var5 + var22 + 5.5F, var6 + var8, 1.0F, var9, 0.5F * alphaMult, alphaMult, 0.5F * alphaMult, false);
            E.o00000(this.lineTexture, var5 + var22 + 5.5F, var6, var5 + var22 + 5.5F, var6 + var8, 1.0F, Color.white, 0.0F * alphaMult, alphaMult, 0.0F * alphaMult, true);
         }

         if (var17 > 2.0F) {
            var18 = var17 - 1.0F;
            var19 = 0.0F;
            if (this.flashOnOverflowFader != null) {
               var19 += this.flashOnOverflowFader.getBrightness() * 1.0F;
            }

            O.o00000(var10 + var15 - (var10 - var5 - 6.0F), var6 + 1.0F, var18, var8 - 2.0F, this.barColorOverflow, alphaMult, var19);
         }

         E.o00000(this.lineTexture, var5 + var15 + 5.5F, var6, var5 + var15 + 5.5F, var6 + var8, 1.0F, var9, 0.5F * alphaMult, alphaMult, 0.5F * alphaMult, false);
         if (this.userAdjustable && this.showAdjustableIndicator) {
            E.o00000(this.lineTexture, var5 + var15 + 5.5F, var6, var5 + var15 + 5.5F, var6 + var8, 2.0F, Color.white, 0.0F * alphaMult, alphaMult, 0.0F * alphaMult, false);
            GL11.glDisable(3553);
            GL11.glEnable(3042);
            GL11.glBlendFunc(770, 771);

            for(var18 = -3.0F; var18 <= 3.0F; ++var18) {
               var19 = var8 * 0.5F;
               if (var19 < 6.0F) {
                  var19 = 6.0F;
               }

               if (var19 > 12.0F) {
                  var19 = 12.0F;
               }

               var20 = 1.62F;
               var20 = 0.61728394F;
               var21 = var5 + var15 + 5.5F;
               float var23 = var6 + var8;
               float var24 = alphaMult * 0.2F;
               var24 = alphaMult * 0.5F;
               float var25 = 0.25F;
               var21 += var18 * var25;
               GL11.glBegin(4);
               B.Ò00000(var9, var24);
               GL11.glVertex2f(var21, var6);
               B.Ò00000(var9, var24 * 0.0F);
               GL11.glVertex2f(var21 - var19, var6 - var19 * var20);
               GL11.glVertex2f(var21 + var19, var6 - var19 * var20);
               B.Ò00000(var9, var24);
               GL11.glVertex2f(var21, var23);
               B.Ò00000(var9, var24 * 0.0F);
               GL11.glVertex2f(var21 - var19, var23 + var19 * var20);
               GL11.glVertex2f(var21 + var19, var23 + var19 * var20);
               GL11.glEnd();
            }
         }

         if (this.numSubivisions > 0) {
            for(int var26 = 0; var26 < this.numSubivisions - 1; ++var26) {
               var19 = (float)((int)(var11 / (float)this.numSubivisions) * (var26 + 1));
               var19 += 2.0F;
               var20 = 3.0F;
               if (this.getXCoordinateForProgressValue(this.progressValue) > var10 + var19) {
                  var20 = 1.0F;
               }

               E.o00000(this.lineTexture, var10 + var19 - 1.0F, var6 + var20, var10 + var19 - 1.0F, var6 + var8 - var20, 1.0F, Color.black, alphaMult * 1.0F, alphaMult, alphaMult * 1.0F, false);
               E.o00000(this.lineTexture, var10 + var19, var6 + var20, var10 + var19, var6 + var8 - var20, 1.0F, var9, alphaMult * 0.5F, alphaMult, alphaMult * 0.5F, false);
               E.o00000(this.lineTexture, var10 + var19 + 1.0F, var6 + var20, var10 + var19 + 1.0F, var6 + var8 - var20, 1.0F, Color.black, alphaMult * 1.0F, alphaMult, alphaMult * 1.0F, false);
               var21 = 3.0F;
               E.o00000(this.lineTexture, var10 + var19 - var21, var6 - 1.0F, var10 + var19 + var21 + 1.0F, var6 - 1.0F, 1.0F, var9, alphaMult, alphaMult, alphaMult, false);
               E.o00000(this.lineTexture, var10 + var19 - var21, var6, var10 + var19 + var21 + 1.0F, var6, 1.0F, Color.black, alphaMult, alphaMult, alphaMult, false);
               E.o00000(this.lineTexture, var10 + var19 - var21, var6 + var8, var10 + var19 + var21 + 1.0F, var6 + var8, 1.0F, var9, alphaMult, alphaMult, alphaMult, false);
               E.o00000(this.lineTexture, var10 + var19 - var21, var6 + var8 - 1.0F, var10 + var19 + var21 + 1.0F, var6 + var8 - 1.0F, 1.0F, Color.black, alphaMult, alphaMult, alphaMult, false);
            }

            var18 = Math.minValue(10.0F, var13);
            var19 = var5 + 6.0F + var11;
            E.o00000(this.lineTexture, var19 + 2.0F, var6 + 1.0F, var19 + 2.0F - var18, var6 + 1.0F, 1.0F, var9, 0.0F, 0.5F * alphaMult, alphaMult, false);
            E.o00000(this.lineTexture, var19 + 2.0F - var18, var6 + 1.0F, var19 - var13 + 1.5F, var6 + 1.0F, 1.0F, var9, alphaMult, alphaMult, alphaMult, false);
            E.o00000(this.lineTexture, var19 + 2.0F, var6 + var8 - 2.0F, var19 + 2.0F - var18, var6 + var8 - 2.0F, 1.0F, var9, 0.0F, 0.5F * alphaMult, alphaMult, false);
            E.o00000(this.lineTexture, var19 + 2.0F - var18, var6 + var8 - 2.0F, var19 - var13 + 1.5F, var6 + var8 - 2.0F, 1.0F, var9, alphaMult, alphaMult, alphaMult, false);
            E.o00000(this.lineTexture, var19 - var13 + 1.5F, var6 + 1.0F, var19 - var13 + 1.5F, var6 + var8 - 1.0F, 1.0F, var9, alphaMult, alphaMult, alphaMult, false);
            E.o00000(this.lineTexture, var19 - var13 + 1.5F, var6 + 1.0F, var19 - var13 + 1.5F, var6 + var8 - 1.0F, 1.0F, Color.white, 0.0F, alphaMult, 0.0F, true);
         }

         GLListManager.endList();
      }

      GL11.glPopMatrix();
      super.renderImpl(alphaMult);
   }

   	public void forceSync() {
      	boolean needsRefresh = false;
      	if (cachedProgressValue != progressValue || cachedMin != minValue ||
			this.cachedMaxValue != maxRange || cachedMax != maxValue || cachedPotentialDecreaseAmount != potentialDecreaseAmount || CachedShowNotchOnIfBelowProgress != showNotchOnIfBelowProgress
		) needsRefresh = true;

		cachedProgressValue = progressValue;
		cachedMin = minValue;
		cachedMaxValue = maxRange;
		cachedMax = maxValue;
		cachedPotentialDecreaseAmount = potentialDecreaseAmount;
		CachedShowNotchOnIfBelowProgress = showNotchOnIfBelowProgress;
		if (needsRefresh) {
			GLListManager.invalidateList(GLListToken);
		}
	}

   public void setRangeMin(float var1) {
      this.minRange = var1;
   }

   public void setRangeMax(float var1) {
      this.maxRange = var1;
   }

   public void setProgress(float var1) {
      if (var1 < this.minRange) {
         var1 = this.minRange;
      }

      if (this.progressValue != var1) {
         GLListManager.invalidateList(this.GLListToken);
      }

      this.progressValue = var1;
   }

   public void setMin(float var1) {
      this.minValue = var1;
   }

   public void setTextColor(Color var1) {
      this.labelColor = var1;
   }

   public void setTextValueColor(Color var1) {
      this.labelValueColor = var1;
   }

   public float getMax() {
      return this.maxValue;
   }

   public void setMax(float var1) {
      this.maxValue = var1;
   }
}
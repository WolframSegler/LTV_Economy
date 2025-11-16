package wfg.wrap_ui.ui.panels;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.SettingsAPI;
import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.graphics.util.GLListManager;
import com.fs.graphics.util.GLListManager.GLListToken;
import com.fs.starfarer.api.ui.Alignment;
import com.fs.starfarer.api.ui.Fonts;
import com.fs.starfarer.api.ui.LabelAPI;
import com.fs.starfarer.api.ui.PositionAPI;
import com.fs.starfarer.api.ui.UIPanelAPI;
import com.fs.starfarer.api.util.FaderUtil;
import com.fs.starfarer.api.util.Misc;

import wfg.wrap_ui.ui.plugins.SliderPlugin;
import wfg.wrap_ui.ui.plugins.CustomPanelPlugin.InputSnapshot;
import wfg.wrap_ui.util.NumUtils;
import wfg.wrap_ui.util.RenderUtils;

import java.awt.Color;
import java.util.function.Supplier;

import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.GL11;

public class Slider extends CustomPanel<SliderPlugin, Slider, UIPanelAPI> {

    public float minRange = 0f;
    public float maxRange = 1f;
    public float minValue = 0f;
    public float maxValue = Float.MAX_VALUE;
    public LabelAPI label;
    public String labelText = null;
    public Color labelColor = Misc.getTextColor();
    public Color labelValueColor = Misc.getTextColor();
    public float flashOnOverflowFraction = Float.MAX_VALUE;
    public boolean showNoText = false;
    public boolean showValueOnly = false;
    public boolean clampCurrToMax = false;
    public boolean showDecimalForValueOnlyMode = false;
    public int numSubdivisions = 0;
    public boolean showPercent = false;
    public boolean showPercentAndTitle = false;
    public float scrollSpeed = 100f;
    public boolean showLabelOnly = false;
    public boolean roundBarValue = false;
    public int roundingIncrement = 1;
    public Color widgetColor;
    public boolean showAdjustableIndicator = false;
    /**
     * Optional supplier that dynamically provides the label's text.
     *
     * Example:
     * <pre>
     * slider.customText = () -> Misc.getDGSCredits(slider.getProgress());
     * </pre>
     */
    public Supplier<String> customText = null;

    private final int windowWidth = Display.getWidth();
    private final int windowHeight = Display.getWidth();

    private float cachedMaxValue = 1f;
    private float progressValue = 0f;
    private float cachedProgressValue = 0f;
    private float cachedMax = Float.MAX_VALUE;
    private float cachedMin = 0f;
    private float potentialDecreaseAmount = 0f;
    private float cachedPotentialDecreaseAmount = 0f;
    private float CachedShowNotchOnIfBelowProgress = -3.4028235E38f;
    private float showNotchOnIfBelowProgress = -3.4028235E38f;
    private SpriteAPI lineTexture;
    private FaderUtil barHighlightFader = null;
    private boolean userAdjustable = false;
    private Color barColor = new Color(107, 175, 0, 255);
    private Color barColorOverflow;
    private boolean shouldInterpolateCachedValues = false;
    private FaderUtil flashOnOverflowFader = null;
    private GLListToken GLListToken;
    private boolean lineUpTextOnCenter = false;
    private float lineUpTextOnCenterWidth = 0f;
    private boolean highlightBrightnessOverride = false;
    private float highlightBrightnessOverrideValue = -1f;
    private float cachedAlphaMult = -1f;
    private float cachedHighlightBrightness = -1f;

    public Slider(UIPanelAPI parent, String initialText, float minRange, float maxRange, int width, int height) {
        super(parent, width, height, new SliderPlugin());
        final SettingsAPI settings = Global.getSettings();

        barColorOverflow = settings.getColor("progressBarOverflowColor");
        widgetColor = settings.getColor("widgetBorderColorBright");
        lineTexture = settings.getSprite("graphics/hud/line4x4.png");

        this.labelText = initialText;
        this.minRange = minRange;
        this.maxRange = maxRange;
        cachedMaxValue = maxRange;

        initializePlugin(hasPlugin);
        createLabel(null);
    }

    public void initializePlugin(boolean hasPlugin) {
        getPlugin().init(this);
    }
    public void createPanel() {}

    private void createLabel(String fontInput) {
        final String font = fontInput == null ? Fonts.DEFAULT_SMALL : fontInput;
        label = Global.getSettings().createLabel("", font);
        label.setColor(labelColor);
        label.setHighlightOnMouseover(true);
        label.setAlignment(Alignment.MID);
        add(label).inMid();
        label.autoSizeToWidth(label.computeTextWidth(label.getText()));
    }

    public void setPotentialDecreaseAmount(float amount) {
        if (potentialDecreaseAmount != amount) {
            GLListManager.invalidateList(GLListToken);
        }

        potentialDecreaseAmount = amount;
    }

    public float getShowNotchOnIfBelowProgress() {
        return showNotchOnIfBelowProgress;
    }

    public void setShowNotchOnIfBelowProgress(float progress) {
        if (showNotchOnIfBelowProgress != progress) {
            GLListManager.invalidateList(GLListToken);
        }

        showNotchOnIfBelowProgress = progress;
    }

    public void setHighlightOnMouseover(boolean bool) {
        if (bool) {
            barHighlightFader = new FaderUtil(0.05f, 0.25f);
        } else {
            barHighlightFader = null;
        }

    }

    public void setUserAdjustable(boolean bool) {
        userAdjustable = bool;
        setHighlightOnMouseover(bool);
    }

    public float getProgress() {
        return progressValue;
    }

    public float getProgressInterpolated() {
        return cachedProgressValue;
    }

    public void setLineUpTextOnCenter(boolean bool, float width) {
        lineUpTextOnCenter = bool;
        lineUpTextOnCenterWidth = width;
    }

    public FaderUtil getBarHighlightFader() {
        return barHighlightFader;
    }

    public void setProgress(float progress) {
        if (progress < minRange) {
            progress = minRange;
        }

        if (progressValue != progress) {
            GLListManager.invalidateList(GLListToken);
        }

        progressValue = progress;
    }

    public void setBarColor(Color color) {
        if (!barColor.equals(color)) {
            GLListManager.invalidateList(GLListToken);
        }

        barColor = color;
    }

    public Color getBarColor() {
        return barColor;
    }

    public void setLabelFont(String font) {
        final String text = label.getText();
        createLabel(font);
        label.setText(text);
    }

    public void setBarColorOverflow(Color color) {
        if (!barColorOverflow.equals(color)) {
            GLListManager.invalidateList(GLListToken);
        }

        barColorOverflow = color;
    }

    public FaderUtil getHighlight() {
        return barHighlightFader;
    }

    public float getXCoordinateForProgressValue(float progress) {
        final float w = getPos().getWidth() - 8f;
        return w * (cachedProgressValue - minRange) / (maxRange - minRange) +
                w * (progress - minRange) / (maxRange - minRange) + 4.5f;
    }

    public void setHighlightBrightnessOverride(float brightness) {
        highlightBrightnessOverrideValue = brightness;
        highlightBrightnessOverride = true;
    }

    public void forceSync() {
        boolean needsRefresh = false;
        if (cachedProgressValue != progressValue || cachedMin != minValue ||
                cachedMaxValue != maxRange || cachedMax != maxValue
                || cachedPotentialDecreaseAmount != potentialDecreaseAmount
                || CachedShowNotchOnIfBelowProgress != showNotchOnIfBelowProgress)
            needsRefresh = true;

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

    public void renderImpl(float alphaMult) {
        float roundedProgress = cachedProgressValue;
        if (roundBarValue) {
            roundedProgress = Math.round(roundedProgress / roundingIncrement) * roundingIncrement;
            if (roundingIncrement != 1) {
                cachedProgressValue = Math.round(roundedProgress / roundingIncrement) * roundingIncrement;
            }
        }

        float highlightBrightness = (barHighlightFader != null)
            ? barHighlightFader.getBrightness() : -1f;

        if (highlightBrightnessOverride) {
            highlightBrightness = highlightBrightnessOverrideValue;
        }

        if (alphaMult != cachedAlphaMult || highlightBrightness != cachedHighlightBrightness) {
            GLListManager.invalidateList(GLListToken);
        }

        cachedAlphaMult = alphaMult;
        cachedHighlightBrightness = highlightBrightness;
        shouldInterpolateCachedValues = true;
        final PositionAPI pos = getPos();
        final float x = 0;
        final float y = 0;
        final float w = pos.getWidth();
        final float h = pos.getHeight();
        GL11.glPushMatrix();
        GL11.glTranslatef(pos.getX(), pos.getY(), 0f);

        if (!GLListManager.callList(GLListToken)) {
            GLListToken = GLListManager.beginList();

            // Background
            RenderUtils.drawQuad(x, y, w, h, Color.BLACK, alphaMult, false);

            // Layout offsets
            float leftMargin = x + 3f;
            float usableWidth = w - 8f;

            // Adjust for very thin sliders
            if (h <= 5f) {
                leftMargin -= 1f;
                usableWidth += 2f;
            }

            // Precompute ratios
            float minBarWidth = usableWidth * cachedMin / (cachedMaxValue - minRange);
            float progressBarWidth = usableWidth * (roundedProgress - minRange) / (cachedMaxValue - minRange);
            float underfillWidth = usableWidth * (cachedMaxValue - roundedProgress) / (cachedMaxValue - minRange);
            float overflowWidth = 0f;
            float overAmount;

            if (roundedProgress > cachedMaxValue) {
                minBarWidth = usableWidth * cachedMin / (roundedProgress - minRange);
                progressBarWidth = usableWidth * cachedMaxValue / (roundedProgress - minRange);
                overflowWidth = usableWidth * (roundedProgress - cachedMaxValue) / (roundedProgress - minRange);
                underfillWidth = 0f;
                overAmount = roundedProgress - cachedMaxValue;
            }

            final boolean thin = h <= 5f;

            { // Gradient Border Glow
                
                // Left edge
                RenderUtils.drawGradientSprite(x, y, x, y + h, 2f, widgetColor, false, 0.5f * alphaMult, 0.5f * alphaMult, 0.5f * alphaMult);
                RenderUtils.drawGradientSprite(x + 1f, y, x + 1f, y + h, 2f, widgetColor, true, 1f * alphaMult, 1f * alphaMult, alphaMult);
    
                // Right edge
                RenderUtils.drawGradientSprite(x + w, y, x + w, y + h, 2f, widgetColor, false, 0.5f * alphaMult, 0.5f * alphaMult, 0.5f * alphaMult);
                RenderUtils.drawGradientSprite(x + w - 1f, y, x + w - 1f, y + h, 2f, widgetColor, true, 1f * alphaMult, 1f * alphaMult, alphaMult);
    
                if (!thin) {
                    // Left edge horizontal glow at top/bottom
                    RenderUtils.drawGradientSprite(x + 1f, y, x + 15f, y, 1f, widgetColor, false, 1f * alphaMult, 0.5f * alphaMult, 0f);
                    RenderUtils.drawGradientSprite(x + 1f, y + h - 1f, x + 15f, y + h, 1f, widgetColor, false, 1f * alphaMult, 0.5f * alphaMult, 0f);
    
                    // Right edge horizontal glow at top/bottom
                    RenderUtils.drawGradientSprite(x + w - 1f, y, x + w - 15f, y, 1f, widgetColor, false, 1f * alphaMult, 0.5f * alphaMult, 0f);
                    RenderUtils.drawGradientSprite(x + w - 1f, y + h - 1f, x + w - 15f, y + h, 1f, widgetColor, false, 1f * alphaMult, 0.5f * alphaMult, 0f);
                }
            }

            if (minBarWidth > 0f) {  // Progress bar underfill Segment
                overAmount = Math.min(10f, minBarWidth);

                // Top underfill segment
                RenderUtils.drawGradientSprite(
                    lineTexture, leftMargin - 2f, y + 1f,
                    leftMargin - 2f + overAmount, y + 1f,
                    1f,
                    widgetColor, false,
                    0f, 0.5f * alphaMult, alphaMult
                );

                // Top main bar segment
                RenderUtils.drawGradientSprite(
                    lineTexture, leftMargin - 2f + overAmount, y + 1f,
                    leftMargin + minBarWidth - 1.5f, y + 1f,
                    1f,
                    widgetColor, false,
                    alphaMult, alphaMult, alphaMult
                );

                // Bottom underfill segment
                RenderUtils.drawGradientSprite(
                    lineTexture, leftMargin - 2f, y + h - 2f,
                    leftMargin - 2f + overAmount, y + h - 2f,
                    1f,
                    widgetColor, false,
                    0f, 0.5f * alphaMult, alphaMult
                );

                // Bottom main bar segment
                RenderUtils.drawGradientSprite(
                    lineTexture, leftMargin - 2f + overAmount, y + h - 2f,
                    leftMargin + minBarWidth - 1.5f, y + h - 2f,
                    1f,
                    widgetColor, false,
                    alphaMult, alphaMult, alphaMult
                );

                // Right vertical bar
                RenderUtils.drawGradientSprite(
                    lineTexture, leftMargin + minBarWidth - 1.5f, y + 1f,
                    leftMargin + minBarWidth - 1.5f, y + h - 1f,
                    1f,
                    widgetColor, false,
                    alphaMult, alphaMult, alphaMult
                );

                // Right white edge
                RenderUtils.drawGradientSprite(
                    lineTexture, leftMargin + minBarWidth - 1.5f, y + 1f,
                    leftMargin + minBarWidth - 1.5f, y + h - 1f,
                    1f,
                    Color.WHITE, true,
                    0f, alphaMult, 0f
                );

                leftMargin += minBarWidth;
            }

            if (underfillWidth > 0f) {  // Underfill / Right-side Bar Segment
                overAmount = Math.min(10f, underfillWidth);
                float rightEdgeX = x + 6f + usableWidth;

                if (thin) {
                    // Small slider: single horizontal lines
                    RenderUtils.drawGradientSprite(rightEdgeX, y + 1f, rightEdgeX - underfillWidth - 1f, y + 1f,
                            1f, widgetColor, false, alphaMult, alphaMult, alphaMult);
                    RenderUtils.drawGradientSprite(rightEdgeX, y + h - 2f, rightEdgeX - underfillWidth - 1f, y + h - 2f,
                            1f, widgetColor, false, alphaMult, alphaMult, alphaMult);
                } else if (numSubdivisions <= 0) {
                    // Regular slider: split horizontal + vertical segments
                    RenderUtils.drawGradientSprite(rightEdgeX + 2f, y + 1f, rightEdgeX + 2f - overAmount, y + 1f,
                        1f, widgetColor, false, 0f, 0.5f * alphaMult, alphaMult);
                    RenderUtils.drawGradientSprite(rightEdgeX + 2f - overAmount, y + 1f, rightEdgeX - underfillWidth + 1.5f, y + 1f,
                        1f, widgetColor, false, alphaMult, alphaMult, alphaMult);
                    RenderUtils.drawGradientSprite(rightEdgeX + 2f, y + h - 2f, rightEdgeX + 2f - overAmount, y + h - 2f,
                        1f, widgetColor, false, 0f, 0.5f * alphaMult, alphaMult);
                    RenderUtils.drawGradientSprite(rightEdgeX + 2f - overAmount, y + h - 2f, rightEdgeX - underfillWidth + 1.5f, y + h - 2f,
                        1f, widgetColor, false, alphaMult, alphaMult, alphaMult);
                    
                    // Vertical edge lines
                    RenderUtils.drawGradientSprite(rightEdgeX - underfillWidth + 1.5f, y + 1f, rightEdgeX - underfillWidth + 1.5f, y + h - 1f,
                        1f, widgetColor, false, alphaMult, alphaMult, alphaMult);
                    RenderUtils.drawGradientSprite(rightEdgeX - underfillWidth + 1.5f, y + 1f, rightEdgeX - underfillWidth + 1.5f, y + h - 1f,
                        1f, Color.WHITE, false, 0f, alphaMult, 0f);
                }
            }

            overAmount = progressBarWidth - 2f - (leftMargin - x - 6f);
            float highlightIntensity = 0f;
            if (barHighlightFader != null || highlightBrightnessOverride) {
                highlightIntensity += highlightBrightness;
                highlightBrightnessOverride = false;
            }

            float maxNotchPos;
            float maxNotchOffset;
            if (cachedMax < cachedMaxValue && cachedMax > roundedProgress) { // Highlight Notch Rendering
                if (overAmount > 0f) {
                    if (CachedShowNotchOnIfBelowProgress < roundedProgress
                            && CachedShowNotchOnIfBelowProgress >= minRange) {

                        // Compute notch position
                        maxNotchPos = usableWidth * (CachedShowNotchOnIfBelowProgress - minRange) / (cachedMaxValue - minRange);
                        maxNotchOffset = getXCoordinateForProgressValue(CachedShowNotchOnIfBelowProgress) - leftMargin - overAmount;

                        if (-maxNotchOffset <= 2f) {
                            // Simple notch rectangle
                            RenderUtils.drawHighlightBar(leftMargin, y + 1f, overAmount, h - 2f, barColor,
                                alphaMult, highlightIntensity, false
                            );
                        } else {
                            // Split the highlight bar for the notch
                            RenderUtils.drawHighlightBar(leftMargin, y + 1f, overAmount + maxNotchOffset,
                                h - 2f, barColor, alphaMult, highlightIntensity, false
                            );

                            if (-maxNotchOffset > 0f) {
                                RenderUtils.drawHighlightBar(
                                    leftMargin + overAmount + maxNotchOffset, y + 1f, -maxNotchOffset, h - 2f,
                                    barColor, alphaMult * 0.7f, highlightIntensity, false
                                );
                            }
                        }
                    } else {
                        // CachedShowNotchOnIfBelowProgress is outside range
                        RenderUtils.drawHighlightBar(
                            leftMargin, y + 1f, overAmount, h - 2f,
                            barColor, alphaMult, highlightIntensity, false
                        );
                    }
                }

                // Highlight remaining section of the bar
                maxNotchPos = usableWidth * (cachedMax - minRange) / (cachedMaxValue - minRange);
                maxNotchOffset = maxNotchPos - overAmount;
                if (maxNotchOffset > 0f) {
                    RenderUtils.drawHighlightBar(
                        leftMargin + overAmount + 4f,
                        y + 3f,
                        maxNotchOffset,
                        h - 6f,
                        barColor,
                        alphaMult * 0.65f,
                        highlightIntensity,
                        true
                    );
                }

                // Draw the vertical end line
                RenderUtils.drawGradientSprite(
                    lineTexture,
                    leftMargin + overAmount + 4f + maxNotchOffset + 1.5f, y + 2f,
                    leftMargin + overAmount + 4f + maxNotchOffset + 1.5f, y + h - 2f,
                    1f, widgetColor, false,
                    alphaMult, alphaMult, alphaMult
                );
            }
            else { // drawOverflowAndNotches
                if (overAmount > 0f) {

                    // Draw max progress notch if current progress is near the cached max
                    if (cachedMax <= roundedProgress && roundedProgress < maxRange - 1f
                            && cachedMax >= roundedProgress - 1f) {
                        maxNotchPos = usableWidth * (cachedMax - minRange) / (cachedMaxValue - minRange);
                        maxNotchOffset = maxNotchPos - overAmount;
                        RenderUtils.drawGradientSprite(lineTexture, 
                            leftMargin + overAmount + 4f + maxNotchOffset + 1.5f, y + 2f, 
                            leftMargin + overAmount + 4f + maxNotchOffset + 1.5f, y + h - 2f, 
                            1f, widgetColor, false, alphaMult, alphaMult, alphaMult
                        );
                    }

                    // Draw CachedShowNotch highlight
                    if (CachedShowNotchOnIfBelowProgress < roundedProgress &&
                        CachedShowNotchOnIfBelowProgress >= minRange
                    ) {
                        float notchRelativeOffset = getXCoordinateForProgressValue(CachedShowNotchOnIfBelowProgress) - leftMargin - overAmount;

                        if (-notchRelativeOffset <= 2f) {
                            RenderUtils.drawHighlightBar(leftMargin, y + 1f, overAmount, h - 2f, barColor, alphaMult, highlightIntensity, false);
                        } else {
                            RenderUtils.drawHighlightBar(leftMargin, y + 1f, overAmount + notchRelativeOffset,
                                h - 2f, barColor, alphaMult, highlightIntensity, false
                            );

                            if (-notchRelativeOffset > 0f) {
                                RenderUtils.drawHighlightBar(leftMargin + overAmount + notchRelativeOffset,
                                    y + 1f, -notchRelativeOffset, h - 2f, barColor, alphaMult * 0.7f, 
                                    highlightIntensity, false
                                );
                            }
                        }
                    } 
                    // Draw cachedMax notch if CachedShowNotch is not in range
                    else if (cachedMax < cachedMaxValue) {
                        maxNotchPos = usableWidth * (cachedMax - minRange) / (cachedMaxValue - minRange);
                        maxNotchOffset = getXCoordinateForProgressValue(cachedMax) - leftMargin - overAmount;

                        if (-maxNotchOffset <= 2f) {
                            RenderUtils.drawHighlightBar(leftMargin, y + 1f, overAmount, h - 2f, barColor, alphaMult, highlightIntensity, false);
                        } else {
                            RenderUtils.drawHighlightBar(leftMargin, y + 1f, overAmount + maxNotchOffset + 2f, h - 2f, barColor, alphaMult, highlightIntensity, false);

                            if (-maxNotchOffset > 0f) {
                                RenderUtils.drawHighlightBar(leftMargin + overAmount + maxNotchOffset + 2f, y + 1f,
                                    -maxNotchOffset - 2f, h - 2f, barColor, alphaMult * 0.7f, highlightIntensity, false
                                );
                            }
                        }
                    } 
                    // Default highlight
                    else {
                        RenderUtils.drawHighlightBar(leftMargin, y + 1f, overAmount, h - 2f, barColor, alphaMult, highlightIntensity, false);
                    }
                }

                // Draw overflow highlight if progress exceeds cached max
                if (!(cachedMax < cachedMaxValue)) {
                    float overflowNotchPos = usableWidth * (cachedMaxValue - minRange) / (cachedMaxValue - minRange);
                    float overflowNotchOffset = overflowNotchPos - overAmount - 4f - overflowWidth;

                    if (overflowNotchOffset > 0f) {
                        RenderUtils.drawHighlightBar(
                            leftMargin + overAmount + 5f, y + 3f, overflowNotchOffset, h - 6f,
                            barColor, alphaMult * 0.65f, highlightIntensity * 1f, true
                        );
                    }
                }
            }
            
            if (cachedPotentialDecreaseAmount > 0f) { // Decrease Notch Indicator
                maxNotchPos = Math.max(0f, roundedProgress - cachedPotentialDecreaseAmount);
                maxNotchOffset = usableWidth * (roundedProgress - cachedPotentialDecreaseAmount - minRange)
                    / (cachedMaxValue - minRange);
                final float notchX = usableWidth * (maxNotchPos - minRange) / (cachedMaxValue - minRange);

                // Draw the gradient bar for the notch
                RenderUtils.drawGradientSprite(
                    lineTexture,
                    x + notchX + 5.5f, y,
                    x + notchX + 5.5f, y + h,
                    1f, widgetColor, false,
                    0.5f * alphaMult, alphaMult, 0.5f * alphaMult
                );

                // Draw the additive white highlight on top
                RenderUtils.drawGradientSprite(
                    lineTexture,
                    x + notchX + 5.5f, y,
                    x + notchX + 5.5f, y + h,
                    1f, Color.white, true,
                    0f, alphaMult, 0f
                );
            }

            if (overflowWidth > 2f) { // Overflow Highlight Bar
                overAmount = overflowWidth - 1f;
                highlightIntensity = 0.0f;
                if (flashOnOverflowFader != null) {
                    highlightIntensity += flashOnOverflowFader.getBrightness();
                }

                RenderUtils.drawHighlightBar(
                    leftMargin + progressBarWidth - (leftMargin - x - 6f),
                    y + 1f,
                    overAmount,
                    h - 2f,
                    barColorOverflow,
                    alphaMult,
                    highlightIntensity,
                    false
                );
            }

            RenderUtils.drawGradientSprite(lineTexture, x + progressBarWidth + 5.5f, y,
                x + progressBarWidth + 5.5f, y + h, 1.0f, widgetColor, false,
                0.5f * alphaMult, alphaMult, 0.5f * alphaMult
            );

            if (userAdjustable && showAdjustableIndicator) { // User Adjustable Indicator
                RenderUtils.drawGradientSprite(lineTexture, x + progressBarWidth + 5.5f, y,
                    x + progressBarWidth + 5.5f, y + h, 2.0f, Color.white, false, 
                    0f, alphaMult, 0f
                );
                GL11.glDisable(GL11.GL_TEXTURE_2D);
                GL11.glEnable(GL11.GL_BLEND);
                GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);

                for (overAmount = -3f; overAmount <= 3f; ++overAmount) {
                    highlightIntensity = Math.min(Math.max(h * 0.5f, 6f), 12f);
                    maxNotchPos = 0.61728394f;
                    maxNotchOffset = x + progressBarWidth + 5.5f + overAmount * 0.25f;
                    float bottomY = y + h;
                    float alpha = alphaMult * 0.5f;

                    GL11.glBegin(GL11.GL_TRIANGLES);
                    RenderUtils.setGlColor(widgetColor, alpha);
                    GL11.glVertex2f(maxNotchOffset, y);
                    RenderUtils.setGlColor(widgetColor, alpha * 0f);
                    GL11.glVertex2f(maxNotchOffset - highlightIntensity, y - highlightIntensity * maxNotchPos);
                    GL11.glVertex2f(maxNotchOffset + highlightIntensity, y - highlightIntensity * maxNotchPos);

                    RenderUtils.setGlColor(widgetColor, alpha);
                    GL11.glVertex2f(maxNotchOffset, bottomY);
                    RenderUtils.setGlColor(widgetColor, alpha * 0f);
                    GL11.glVertex2f(maxNotchOffset - highlightIntensity, bottomY + highlightIntensity * maxNotchPos);
                    GL11.glVertex2f(maxNotchOffset + highlightIntensity, bottomY + highlightIntensity * maxNotchPos);
                    GL11.glEnd();
                }
            }

            if (numSubdivisions > 0) { // Subdivision Notches & Underfill Highlight
                for (int i = 0; i < numSubdivisions - 1; ++i) {
                    final float subdivisionX = (int)(usableWidth / (float) numSubdivisions) * (i + 1) + 2;
                    final float notchPadding = 3.0f;
                    final float topOffset = getXCoordinateForProgressValue(progressValue) >
                        leftMargin + subdivisionX ? 1.0f : 3.0f;

                    RenderUtils.drawGradientSprite(lineTexture, leftMargin + subdivisionX - 1.0f, y + topOffset,
                        leftMargin + subdivisionX - 1.0f, y + h - topOffset,
                        1.0f, Color.black, false, alphaMult, alphaMult, alphaMult
                    );

                    RenderUtils.drawGradientSprite(lineTexture, leftMargin + subdivisionX, y + topOffset,
                        leftMargin + subdivisionX, y + h - topOffset,
                        1.0f, widgetColor, false, alphaMult * 0.5f, alphaMult, alphaMult * 0.5f
                    );

                    RenderUtils.drawGradientSprite(lineTexture, leftMargin + subdivisionX + 1.0f, y + topOffset,
                        leftMargin + subdivisionX + 1.0f, y + h - topOffset,
                        1.0f, Color.black, false, alphaMult, alphaMult, alphaMult
                    );

                    RenderUtils.drawGradientSprite(lineTexture, leftMargin + subdivisionX - notchPadding,
                        y - 1.0f, leftMargin + subdivisionX + notchPadding + 1.0f, y - 1.0f,
                        1.0f, widgetColor, false, alphaMult, alphaMult, alphaMult
                    );

                    RenderUtils.drawGradientSprite(lineTexture, leftMargin + subdivisionX - notchPadding, y,
                        leftMargin + subdivisionX + notchPadding + 1.0f, y,
                        1.0f, Color.black, false, alphaMult, alphaMult, alphaMult
                    );

                    RenderUtils.drawGradientSprite(lineTexture, leftMargin + subdivisionX - notchPadding, y + h,
                        leftMargin + subdivisionX + notchPadding + 1.0f, y + h,
                        1.0f, widgetColor, false, alphaMult, alphaMult, alphaMult
                    );

                    RenderUtils.drawGradientSprite(lineTexture, leftMargin + subdivisionX - notchPadding,
                        y + h - 1.0f, leftMargin + subdivisionX + notchPadding + 1.0f, y + h - 1.0f,
                        1.0f, Color.black, false, alphaMult, alphaMult, alphaMult
                    );
                }

                overAmount = Math.min(10.0f, underfillWidth);
                final float baseX = x + 6.0f + usableWidth;

                RenderUtils.drawGradientSprite(lineTexture, baseX + 2.0f, y + 1.0f, baseX + 2.0f - overAmount,
                    y + 1.0f, 1.0f, widgetColor, false, 0.0f,
                    0.5f * alphaMult, alphaMult
                );

                RenderUtils.drawGradientSprite(lineTexture, baseX + 2.0f - overAmount, y + 1.0f,
                    baseX - underfillWidth + 1.5f, y + 1.0f, 1.0f, widgetColor, false,
                    alphaMult, alphaMult, alphaMult
                );

                RenderUtils.drawGradientSprite(lineTexture, baseX + 2.0f, y + h - 2.0f,
                    baseX + 2.0f - overAmount, y + h - 2.0f, 1.0f, widgetColor,
                    false, 0.0f, 0.5f * alphaMult, alphaMult
                );

                RenderUtils.drawGradientSprite(lineTexture, baseX + 2.0f - overAmount, y + h - 2.0f,
                    baseX - underfillWidth + 1.5f, y + h - 2.0f, 1.0f,
                    widgetColor, false, alphaMult, alphaMult, alphaMult
                );

                RenderUtils.drawGradientSprite(lineTexture, baseX - underfillWidth + 1.5f, y + 1.0f,
                    baseX - underfillWidth + 1.5f, y + h - 1.0f, 1.0f, widgetColor, 
                    false, alphaMult, alphaMult, alphaMult
                );

                RenderUtils.drawGradientSprite(lineTexture, baseX - underfillWidth + 1.5f, y + 1.0f,
                    baseX - underfillWidth + 1.5f, y + h - 1.0f, 1.0f, Color.white, 
                    false, 0.0f, alphaMult, 0.0f
                );
            }

            GLListManager.endList();
        }

        GL11.glPopMatrix();
    }

    public void processInputImpl(InputSnapshot snapshot) {
        if (!userAdjustable && barHighlightFader == null) return;

        final InputEventAPI event = snapshot.mouseEvent;
        if (event == null) return;

        if (barHighlightFader != null) {
            if (snapshot.isActive || snapshot.hoveredLastFrame) {
                barHighlightFader.fadeIn();
            } else {
                barHighlightFader.fadeOut();
            }
        }

        if (!userAdjustable || event.isConsumed()) return;

        if ((snapshot.isActive)) {
            mapInputToProgress(event);
            event.consume();
        }
    }

    public void advanceImpl(float delta) {
        if (roundBarValue && roundingIncrement > 0) {
            cachedProgressValue = Math.round(cachedProgressValue / roundingIncrement) * roundingIncrement;
            if (roundingIncrement != 1) {
                progressValue = Math.round(progressValue / roundingIncrement) * roundingIncrement;
            }
        }

        if (cachedProgressValue != progressValue || cachedMin != minValue || cachedMax != maxValue || 
            cachedMaxValue != maxRange || showNotchOnIfBelowProgress != CachedShowNotchOnIfBelowProgress
        ) {
            GLListManager.invalidateList(GLListToken);
        }

        if (barHighlightFader != null) barHighlightFader.advance(delta);
        if (flashOnOverflowFader != null) flashOnOverflowFader.advance(delta);

        if (clampCurrToMax && progressValue > maxValue) progressValue = maxValue;

        // Overflow flash: detect when progress exceeds the configured range and toggle the flash fader
        float overflowRatio;
        if (maxRange <= 0f) {
            overflowRatio = 10f;
        } else {
            overflowRatio = (progressValue - minRange) / maxRange - 1.0f;
            if (overflowRatio < 0f) {
                overflowRatio = 0f;
            }
        }

        if (overflowRatio > flashOnOverflowFraction) {
            if (flashOnOverflowFader == null) {
                flashOnOverflowFader = new FaderUtil(0.25f, 0.25f);
                flashOnOverflowFader.setBounce(true, true);
                flashOnOverflowFader.fadeIn();
            }
        } else if (flashOnOverflowFader != null) {
            flashOnOverflowFader.setBounceUp(false);
            flashOnOverflowFader.fadeOut();
            if (flashOnOverflowFader.isFadedOut()) {
                flashOnOverflowFader = null;
            }
        }

        float interpolationScale;
        if (shouldInterpolateCachedValues) {
            scrollSpeed = 100f;

            // Compute interpolation scale based on the visible width vs the effective progress range
            interpolationScale = getPos().getWidth() / Math.max(progressValue - minRange, maxRange - minRange);

            float effectiveRange = cachedMaxValue - minRange;
            if (cachedProgressValue - minRange > effectiveRange) {
                effectiveRange = cachedProgressValue - minRange;
            }
            if (effectiveRange < 1f) {
                effectiveRange = 1f;
            }

            interpolationScale = getPos().getWidth() / effectiveRange;

            cachedProgressValue = NumUtils.smoothApproach(
                cachedProgressValue, progressValue,
                scrollSpeed / interpolationScale,
                0.02f * Math.abs(cachedProgressValue - progressValue) * interpolationScale,
                delta
            );

            cachedMin = NumUtils.smoothApproach(
                cachedMin, minValue,
                scrollSpeed / interpolationScale,
                0.02f * Math.abs(cachedMin - minValue) * interpolationScale,
                delta
            );

            cachedMax = NumUtils.smoothApproach(
                cachedMax, maxValue,
                scrollSpeed / interpolationScale,
                0.05f * Math.abs(cachedMax - maxValue) * interpolationScale,
                delta
            );

            cachedPotentialDecreaseAmount = NumUtils.smoothApproach(
                cachedPotentialDecreaseAmount, potentialDecreaseAmount,
                scrollSpeed / interpolationScale,
                0.05f * Math.abs(cachedPotentialDecreaseAmount - potentialDecreaseAmount) * interpolationScale,
                delta
            );

            cachedMaxValue = NumUtils.smoothApproach(
                cachedMaxValue, maxRange,
                scrollSpeed / interpolationScale,
                0.02f * Math.abs(cachedMaxValue - maxRange) * interpolationScale,
                delta
            );

            CachedShowNotchOnIfBelowProgress = NumUtils.smoothApproach(
                CachedShowNotchOnIfBelowProgress, showNotchOnIfBelowProgress,
                scrollSpeed / interpolationScale,
                0.05f * Math.abs(CachedShowNotchOnIfBelowProgress - showNotchOnIfBelowProgress) * interpolationScale,
                delta
            );
        } else {
            cachedProgressValue = progressValue;
            cachedMin = minValue;
            cachedMax = maxValue;
            cachedMaxValue = maxRange;
            cachedPotentialDecreaseAmount = potentialDecreaseAmount;
            CachedShowNotchOnIfBelowProgress = showNotchOnIfBelowProgress;
        }

        shouldInterpolateCachedValues = false;
        if (customText != null) {
            label.setText(customText.get());

        } else if (showLabelOnly) {
            label.setText(labelText);

        } else if (showPercent) {
            final String displayText = String.format("%d%%", Math.round(cachedProgressValue));
            label.setText(displayText);

        } else if (showPercentAndTitle) {
            final String displayText = String.format("%d%%", Math.round(cachedProgressValue));
            label.setText(String.format("%s: %s", labelText, displayText));
        } else if (showValueOnly) {
            final String displayText = showDecimalForValueOnlyMode ? 
                String.format("%.2f", cachedProgressValue) :
                String.format("%d", Math.round(cachedProgressValue));

            label.setText(displayText);

        } else if (labelText != null) {
            final String displayText = String.format(
                "%d / %d", Math.round(cachedProgressValue), Math.round(cachedMaxValue)
            );
            label.setText(String.format("%s: %s", labelText, displayText));

        } else {
            final String displayText = String.format(
                "%d / %d", Math.round(cachedProgressValue), Math.round(cachedMaxValue)
            );
            label.setText(displayText);
        }

        if (showNoText) label.setText("");

        float labelWidth = label.computeTextWidth(label.getText());
        label.autoSizeToWidth(labelWidth);
        label.setColor(labelColor);
        label.setHighlightColor(labelValueColor);
        if (labelWidth != 0f && lineUpTextOnCenter) {
            final float offset = (label.getPosition().getWidth() / 2f) - labelWidth + lineUpTextOnCenterWidth;
            label.getPosition().setXAlignOffset(offset);
        }

        if (cachedProgressValue < cachedMin) {
            cachedProgressValue = cachedMin;
        }
    }

    protected float mapInputToProgress(InputEventAPI inputEvent) {
        final float mouseX = inputEvent.getX();
        final float mouseY = inputEvent.getY();
        if (mouseX < 0 || mouseX > windowWidth || mouseY < 0 || mouseY > windowHeight) {
            return progressValue;
        }

        final float offset = 6f;
        final float maxBarWidth = getPos().getWidth() - offset;
        float relativeX = mouseX - getPos().getX() - offset;

        if (relativeX > maxBarWidth) relativeX = maxBarWidth;

        float minBarWidth = maxBarWidth * cachedMin / (cachedMaxValue - minRange);
        if (relativeX < minBarWidth) relativeX = minBarWidth;

        float progressValue = relativeX / maxBarWidth * (maxRange - minRange) + minRange;
        if (clampCurrToMax && progressValue > maxValue) progressValue = maxValue;

        setProgress(progressValue);
        forceSync();

        return progressValue;
    }
}
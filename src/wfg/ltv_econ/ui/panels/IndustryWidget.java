package wfg.ltv_econ.ui.panels;

import java.util.ArrayList;
import java.util.List;
import java.awt.Color;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.SpecialItemData;
import com.fs.starfarer.api.campaign.SpecialItemSpecAPI;
import com.fs.starfarer.api.campaign.econ.CommoditySpecAPI;
import com.fs.starfarer.api.campaign.econ.Industry;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.campaign.listeners.DialogCreatorUI;
import com.fs.starfarer.api.impl.campaign.DebugFlags;
import com.fs.starfarer.api.impl.campaign.econ.impl.ConstructionQueue.ConstructionQueueItem;
import com.fs.starfarer.api.ui.Alignment;
import com.fs.starfarer.api.ui.Fonts;
import com.fs.starfarer.api.ui.IconRenderMode;
import com.fs.starfarer.api.ui.LabelAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.ui.UIComponentAPI;
import com.fs.starfarer.api.ui.UIPanelAPI;
import com.fs.starfarer.api.util.FaderUtil;
import com.fs.starfarer.api.util.Misc;

import rolflectionlib.util.ListenerFactory;
import rolflectionlib.util.RolfLectionUtil;
import wfg.ltv_econ.economy.CommodityCell;
import wfg.ltv_econ.economy.WorkerRegistry;
import wfg.ltv_econ.economy.WorkerRegistry.WorkerIndustryData;
import wfg.ltv_econ.economy.engine.EconomyEngine;
import wfg.ltv_econ.industry.IndustryIOs;
import wfg.native_ui.util.NativeUiUtils.AnchorType;
import wfg.native_ui.ui.Attachments;
import wfg.native_ui.ui.ComponentFactory;
import wfg.native_ui.ui.UIContext;
import wfg.native_ui.ui.UIContext.Context;
import wfg.native_ui.ui.components.AudioFeedbackComp;
import wfg.native_ui.ui.components.BackgroundComp;
import wfg.native_ui.ui.components.HoverGlowComp;
import wfg.native_ui.ui.components.InteractionComp;
import wfg.native_ui.ui.components.LayoutOffsetComp;
import wfg.native_ui.ui.components.NativeComponents;
import wfg.native_ui.ui.components.TooltipComp;
import wfg.native_ui.ui.components.HoverGlowComp.GlowType;
import wfg.native_ui.ui.panels.CustomPanel;
import wfg.native_ui.ui.panels.Slider;
import wfg.native_ui.ui.panels.SpritePanel;
import wfg.native_ui.ui.panels.CustomPanel.HasBackground;
import wfg.native_ui.ui.panels.CustomPanel.HasHoverGlow;
import wfg.native_ui.ui.panels.CustomPanel.HasLayoutOffset;
import wfg.native_ui.ui.panels.SpritePanel.Base;
import wfg.native_ui.util.NumFormat;
import wfg.native_ui.util.NativeUiUtils;
import static wfg.native_ui.util.UIConstants.*;

public class IndustryWidget extends CustomPanel<IndustryWidget> implements
    HasBackground, HasHoverGlow, HasLayoutOffset
{
    public final static int PANEL_WIDTH = 190;
    public final static int TITLE_HEIGHT = 15 + pad;
    public final static int IMAGE_HEIGHT = 95;
    public final static int TOTAL_HEIGHT = TITLE_HEIGHT + IMAGE_HEIGHT;
    public final static int ICON_SIZE = 32;

    public final BackgroundComp bg = comp().get(NativeComponents.BACKGROUND);
    public final HoverGlowComp glow = comp().get(NativeComponents.HOVER_GLOW);
    public final LayoutOffsetComp offset = comp().get(NativeComponents.LAYOUT_OFFSET);

    public final Color baseColor;
    public final Color darkColor;

    public final Industry m_industry;
    public IndustryImagePanel industryIcon;
    public LtvIndustryListPanel industryPanel;

    private boolean tradeInfoPanel;
    private int constructionQueueIndex;
    private LabelAPI buildingTitleHeader;
    private LabelAPI constructionStatusText;
    private ConstructionMode constructionMode;
    private final MarketAPI m_market;
    protected final List<LabelAPI> labels = new ArrayList<>();

    public IndustryWidget(UIPanelAPI parent, MarketAPI market, Industry ind,
        LtvIndustryListPanel indPanel
    ) { this(parent, market, ind, indPanel, -1); }

    public IndustryWidget(UIPanelAPI parent, MarketAPI market, Industry ind,
        LtvIndustryListPanel indPanel, int queue
    ) { super(parent, PANEL_WIDTH, IMAGE_HEIGHT + TITLE_HEIGHT);

        m_market = market;
        m_industry = ind;

        constructionMode = ConstructionMode.NORMAL;
        industryPanel = indPanel;
        constructionQueueIndex = queue;

        baseColor = market.getFaction().getBaseUIColor();
        darkColor = market.getFaction().getDarkUIColor();

        bg.color = m_industry.isImproved() ? Misc.getStoryDarkColor() : darkColor;
        bg.alpha = 1f;

        glow.fader = new FaderUtil(0.3f, 0.1f, 0.4f, true, false);
        glow.isFaderOwner = false;
        glow.type = GlowType.UNDERLAY;
        glow.color = baseColor;

        final int hOffset = m_industry.isStructure() ? TITLE_HEIGHT : 0;
        offset.setOffset(-1, -1, 2, 2 - hOffset);

        createPanel();
    }

    public void createPanel() {
        buildingTitleHeader = Global.getSettings().createLabel(
            m_industry.getCurrentName(), Fonts.DEFAULT_SMALL
        );
        buildingTitleHeader.setColor(
            m_industry.isImproved() ? Misc.getStoryOptionColor() : baseColor
        );
        buildingTitleHeader.setHighlightColor(
            NativeUiUtils.adjustBrightness(buildingTitleHeader.getColor(), 1.33f)
        );
        buildingTitleHeader.setAlignment(Alignment.LMID);
        buildingTitleHeader.getPosition().setSize(PANEL_WIDTH + 50, TITLE_HEIGHT);
        add(buildingTitleHeader).inTL(pad, 0f);


        industryIcon = new IndustryImagePanel(
            m_panel, PANEL_WIDTH, IMAGE_HEIGHT,
            m_industry.getCurrentImage(),
            Color.WHITE, null
        );

        if (!m_industry.isFunctional() || constructionQueueIndex >= 0) {
            industryIcon.setColor(darkColor);
        }

        if (!DebugFlags.COLONY_DEBUG && !m_market.isPlayerOwned()) {
            industryIcon.setColor(Color.white);
            industryIcon.interaction.enabled = false;
        }
        add(industryIcon).inBL(0, 0);

        industryIcon.interaction.onClicked = (indIcon, isLeftClick) -> {
            if (tradeInfoPanel) return;
            IndustryWidget targetInd;

            if (constructionQueueIndex >= 0) {
                if (!isLeftClick) {
                    for (Object widgetObj : industryPanel.widgets) {
                        if (widgetObj instanceof IndustryWidget widget && widget.getQueueIndex() >= 0) {
                            widget.setNormalMode();
                        }
                    }

                    return;
                }

                if (constructionMode == ConstructionMode.NORMAL) {
                    for (Object widgetObj : industryPanel.widgets) {
                    if (widgetObj instanceof IndustryWidget widget && widget.getQueueIndex() >= 0) {
                        if (widget == this) {
                            widget.setRemoveMode();
                        } else {
                            widget.setSwapMode();
                        }
                    }
                    }

                } else if (constructionMode == ConstructionMode.SWAP) {
                    targetInd = null;

                    for (Object widgetObj : industryPanel.widgets) {
                        if (widgetObj instanceof IndustryWidget widget &&
                            widget.getQueueIndex() >= 0 &&
                            widget.constructionMode == ConstructionMode.REMOVE
                        ) {
                            targetInd = widget;
                            break;
                        }
                    }

                    // Swap industries

                    List<ConstructionQueueItem> queueItems = m_market.getConstructionQueue().getItems();
                    if (targetInd != null && targetInd.constructionQueueIndex >= 0 && targetInd.constructionQueueIndex < queueItems.size()
                        && constructionQueueIndex < queueItems.size() && constructionQueueIndex >= 0) {

                        ConstructionQueueItem sourceItem = queueItems.get(constructionQueueIndex);
                        ConstructionQueueItem targetItem = queueItems.get(targetInd.constructionQueueIndex);

                        String tempID = sourceItem.id;
                        int tempCost  = sourceItem.cost;

                        sourceItem.id = targetItem.id;
                        sourceItem.cost = targetItem.cost;
                        targetItem.id = tempID;
                        targetItem.cost = tempCost;

                        industryPanel.createPanel();
                    }
                } else if (constructionMode == ConstructionMode.REMOVE) {
                    List<ConstructionQueueItem> queueItems = m_market.getConstructionQueue().getItems();
                    if (constructionQueueIndex < queueItems.size() && constructionQueueIndex >= 0) {

                        final ConstructionQueueItem item = queueItems.get(constructionQueueIndex);
                        m_market.getConstructionQueue().removeItem(item.id);

                        int itemCost = item.cost;
                        if (itemCost > 0) {
                            Global.getSector().getPlayerFleet().getCargo().getCredits().add(itemCost);
                            Misc.addCreditsMessage("Received %s", itemCost);
                        }

                        industryPanel.createPanel();
                    }
                }
            } else {
                for (Object widgetObj : industryPanel.widgets) {
                    if (widgetObj instanceof IndustryWidget widget && widget.getQueueIndex() >= 0) {
                        widget.setNormalMode();
                    }
                }

                if (LtvIndustryListPanel.indOptCtor != null && industryPanel.dummyWidget != null) {
                    final Object listener = new ListenerFactory.DialogDismissedListener() {
                        @Override
                        public void trigger(Object... args) {
                            dialogDismissed();
                        }
                    }.getProxy();

                    // c var17 = new c(
                    //     this.øôöO00,
                    //     this, var14,
                    //     CampaignEngine.getInstance().getCampaignUI().getDialogParent(),
                    //     this
                    // );
                    final DialogCreatorUI dialog = (DialogCreatorUI) RolfLectionUtil.instantiateClass(
                        LtvIndustryListPanel.indOptCtor,
                        m_industry,
                        industryPanel.dummyWidget,
                        LtvIndustryListPanel.getMarketInteractionMode(m_market),
                        Attachments.getCampaignScreenPanel(),
                        listener
                    );
                    RolfLectionUtil.getMethodAndInvokeDirectly(
                        "show", dialog, 0f, 0f);

                    NativeUiUtils.anchorPanel(
                        ((UIPanelAPI)dialog), industryIcon.getPanel(), AnchorType.MidTopLeft, 0
                    );

                }
                tradeInfoPanel = true;

                UIContext.setContext(Context.DIALOG);
            }
        };

        final WorkerIndustryData data = WorkerRegistry.getInstance().getData(m_industry);
        final LabelAPI workerCountLabel = Global.getSettings().createLabel("", Fonts.DEFAULT_SMALL);
        workerCountLabel.setColor(highlight);
        workerCountLabel.setHighlightColor(
            NativeUiUtils.adjustBrightness(workerCountLabel.getColor(), 1.33f)
        );
        if (data != null) {
            final String assignedStr = NumFormat.engNotation(data.getWorkersAssigned());

            workerCountLabel.setText(assignedStr);
            workerCountLabel.setOpacity(0.9f);
            workerCountLabel.autoSizeToWidth(PANEL_WIDTH - pad*4);
        }

        add(workerCountLabel).inTL(pad*2, TITLE_HEIGHT + pad*2);

        final TooltipMakerAPI tp = ComponentFactory.createTooltip(PANEL_WIDTH, false);

        tp.beginIconGroup();
        tp.setIconSpacingMedium();

        final EconomyEngine engine = EconomyEngine.getInstance();

        if (m_industry.isFunctional() && !m_industry.isBuilding()) {
        for (String comID : IndustryIOs.getRealInputs(m_industry, false)) {
            final CommodityCell cell = engine.getComCell(comID, m_market.getId());
            if (cell == null || cell.getStoredAvailabilityRatio() > 0.9f) continue;

            tp.addIcons(cell.spec, 1, IconRenderMode.RED);
        }
        }
        tp.addIconGroup(24, 1, pad);
        tp.getPrev().getPosition().inBL(pad + 2, pad);


        tp.beginIconGroup();
        tp.setIconSpacingWide();

        int totalW = 0;
        for (SpecialItemData item : m_industry.getVisibleInstalledItems()) {

            final SpecialItemSpecAPI spec = Global.getSettings().getSpecialItemSpec(item.getId());

            final Base itemPanel = new Base(m_panel, 28, 28, spec.getIconName(), Color.WHITE, null);
            itemPanel.drawTextureHalo = true;
            itemPanel.texHaloColor = baseColor;

            add(itemPanel).inTR(pad*2 + totalW, TITLE_HEIGHT + pad*2);
            
            totalW += itemPanel.getPos().getWidth() + pad*2;
        }

        if (m_industry.getAICoreId() != null) {

            final CommoditySpecAPI spec = Global.getSettings().getCommoditySpec(m_industry.getAICoreId());

            final Base aiCorePanel = new Base(m_panel, 28, 28, spec.getIconName(), Color.WHITE, null);
            aiCorePanel.drawTextureHalo = true;
            aiCorePanel.texHaloColor = baseColor;

            add(aiCorePanel).inTR(pad + 2 + totalW, TITLE_HEIGHT + pad*2);
        }

        
        final boolean isIndNotFunctional = m_industry.isBuilding() || m_industry.isDisrupted();
        if (isIndNotFunctional) {
            if (m_industry.isBuilding() && !m_industry.isUpgrading() && !m_industry.isDisrupted()) {

                tp.setParaFont(Fonts.INSIGNIA_VERY_LARGE);
                constructionStatusText = tp.createLabel("Building", baseColor);
                constructionStatusText.setHighlightColor(
                    NativeUiUtils.adjustBrightness(constructionStatusText.getColor(), 1.33f)
                );

                constructionStatusText.autoSizeToWidth(PANEL_WIDTH);
                constructionStatusText.getPosition().inMid().setYAlignOffset(-TITLE_HEIGHT / 2);
            }

            final int sliderHeight = 12;
            final Slider slider = new Slider(
                tp, null, 0, 100, PANEL_WIDTH, sliderHeight
            );

            slider.setLabelFont(Fonts.VICTOR_10);
            slider.label.getPosition().setYAlignOffset(1);
            slider.setBarColor(Misc.interpolateColor(baseColor, darkColor, 0.5f));
            slider.labelColor = baseColor;
            slider.setProgress(m_industry.getBuildOrUpgradeProgress() * 100);
            slider.labelText = m_industry.getBuildOrUpgradeProgressText();
            slider.showLabelOnly = true;
            
            tp.addComponent(slider.getPanel()).inBL(0, -sliderHeight - 2);
        }

        tp.setHeightSoFar(IMAGE_HEIGHT);
        ComponentFactory.addTooltip(tp, IMAGE_HEIGHT, false, m_panel).inBL(0, 0);

        if (constructionQueueIndex >= 0) setNormalMode();
    }

    public void clearLabels() {
        for (LabelAPI label : labels) {
            remove(label);
        }
        labels.clear();
    }

    public void setNormalMode() {
        clearLabels();
        String txt = "Queued";
        if (Misc.getCurrentlyBeingConstructed(m_market) == null && constructionQueueIndex == 0) {
            txt = "Building";
        }

        remove(constructionStatusText); 
        constructionStatusText = Global.getSettings().createLabel(txt, Fonts.INSIGNIA_VERY_LARGE);
        constructionStatusText.setColor(baseColor);
        constructionStatusText.setHighlightColor(
            NativeUiUtils.adjustBrightness(constructionStatusText.getColor(), 1.33f)
        );
        constructionStatusText.autoSizeToWidth(constructionStatusText.computeTextWidth(txt));

        add(constructionStatusText).inMid().setYAlignOffset(-TITLE_HEIGHT / 2f);

        constructionMode = ConstructionMode.NORMAL;
        addCostTimeLabels();
    }

    public void setRemoveMode() {
        ConstructionQueueItem queueItem = null;
        if (m_market.getConstructionQueue().getItems().size() > constructionQueueIndex && constructionQueueIndex >= 0) {
            queueItem = m_market.getConstructionQueue().getItems().get(constructionQueueIndex);
        }

        if (queueItem != null) {
            clearLabels();
            LabelAPI removeLabel = Global.getSettings().createLabel(
                "Click to remove", Fonts.DEFAULT_SMALL
            );
            removeLabel.setColor(baseColor);
            removeLabel.setHighlightColor(
                NativeUiUtils.adjustBrightness(removeLabel.getColor(), 1.33f)
            );

            LabelAPI refundLabel = Global.getSettings().createLabel(
                Misc.getDGSCredits(queueItem.cost), Fonts.DEFAULT_SMALL
            );
            refundLabel.setColor(highlight);
            refundLabel.setHighlightColor(
                NativeUiUtils.adjustBrightness(refundLabel.getColor(), 1.33f)
            );

            LabelAPI refundLabelAppendix = Global.getSettings().createLabel(
                " refund", Fonts.DEFAULT_SMALL
            );
            refundLabelAppendix.setColor(baseColor);
            refundLabelAppendix.setHighlightColor(
                NativeUiUtils.adjustBrightness(refundLabelAppendix.getColor(), 1.33f)
            );

            labels.add(removeLabel);
            labels.add(refundLabel);
            labels.add(refundLabelAppendix);

            final float offset = refundLabelAppendix.computeTextWidth(refundLabelAppendix.getText()) / 2f;

            add(removeLabel).aboveMid((UIComponentAPI)constructionStatusText, 0);
            add(refundLabel).belowMid((UIComponentAPI)constructionStatusText, 0).setXAlignOffset(-offset);
            add(refundLabelAppendix).rightOfBottom((UIComponentAPI)refundLabel, 0);
            constructionMode = ConstructionMode.REMOVE;
            addCostTimeLabels();
        }
    }

    public void setSwapMode() {
        clearLabels();
        LabelAPI swapLabel = Global.getSettings().createLabel("Click to swap", Fonts.DEFAULT_SMALL);
        swapLabel.setColor(baseColor);
        swapLabel.setHighlightColor(
            NativeUiUtils.adjustBrightness(swapLabel.getColor(), 1.33f)
        );
        
        labels.add(swapLabel);
        add(swapLabel).aboveMid((UIComponentAPI)constructionStatusText, 0);
        constructionMode = ConstructionMode.SWAP;
        addCostTimeLabels();
    }

    protected void addCostTimeLabels() {
        if (m_market.getConstructionQueue().getItems().size() > constructionQueueIndex && constructionQueueIndex >= 0) {
            final ConstructionQueueItem queueItem = (ConstructionQueueItem) m_market
                .getConstructionQueue().getItems().get(constructionQueueIndex);
            if (queueItem != null) {
                final int buildTime = (int) m_industry.getSpec().getBuildTime();
                String buildText = "days";
                if (buildTime == 1) {
                    buildText = "day";
                }

                LabelAPI buildTimeLabel = Global.getSettings().createLabel(
                    "" + buildTime, Fonts.DEFAULT_SMALL
                );
                buildTimeLabel.setColor(highlight);
                buildTimeLabel.setHighlightColor(
                    NativeUiUtils.adjustBrightness(buildTimeLabel.getColor(), 1.33f)
                );

                LabelAPI buildTimeAppendix = Global.getSettings().createLabel(
                    " " + buildText, Fonts.DEFAULT_SMALL
                );
                buildTimeAppendix.setColor(baseColor);
                buildTimeAppendix.setHighlightColor(
                    NativeUiUtils.adjustBrightness(buildTimeAppendix.getColor(), 1.33f)
                );
                
                LabelAPI costLabel = Global.getSettings().createLabel(
                    Misc.getDGSCredits(queueItem.cost), Fonts.DEFAULT_SMALL
                );
                costLabel.setColor(highlight);
                costLabel.setHighlightColor(
                    NativeUiUtils.adjustBrightness(costLabel.getColor(), 1.33f)
                );

                labels.add(buildTimeLabel);
                labels.add(buildTimeAppendix);
                labels.add(costLabel);

                final int labelOpad = 7;
                final float timeLblW = buildTimeLabel.computeTextWidth("" + buildTime);
                add(buildTimeLabel).inBL(labelOpad, pad);
                add(buildTimeAppendix).inBL(labelOpad + timeLblW, pad);
                add(costLabel).inBR(labelOpad, pad);
            }
        }

    }

    public int getQueueIndex() {
        return constructionQueueIndex;
    }

    public ConstructionMode getMode() {
      return constructionMode;
   }

    @Override
    public void renderBelow(float alpha) {
        super.renderBelow(alpha);
        if (glow.fader.getBrightness() > 0f) {
            buildingTitleHeader.setHighlight(buildingTitleHeader.getText());

            if (constructionStatusText != null) {
                constructionStatusText.setHighlight(constructionStatusText.getText());
            }

            for (LabelAPI label : labels) {
                label.setHighlight(label.getText());
            }

        } else {
            buildingTitleHeader.setHighlight("");
            if (constructionStatusText != null) {
                constructionStatusText.setHighlight("");
            }
            for (LabelAPI label : labels) {
                label.setHighlight("");
            }
        }
    }

    public void dialogDismissed() {
        tradeInfoPanel = false;
        RolfLectionUtil.getMethodAndInvokeDirectly("dialogDismissed",
            industryPanel.dummyWidget, null, 0);

        UIContext.setContext(Context.NONE);
    }

    public enum ConstructionMode {
        NORMAL,
        SWAP,
        REMOVE,
    }

    public class IndustryImagePanel extends SpritePanel<IndustryImagePanel> implements
        HasHoverGlow, HasInteraction, HasTooltip, HasAudioFeedback
    {
        public final TooltipComp tooltip = comp().get(NativeComponents.TOOLTIP);
        public final AudioFeedbackComp audio = comp().get(NativeComponents.AUDIO_FEEDBACK);
        public final HoverGlowComp ImgGlow = comp().get(NativeComponents.HOVER_GLOW);
        public final InteractionComp<IndustryImagePanel> interaction = comp().get(
            NativeComponents.INTERACTION
        ); 

        public IndustryImagePanel(UIPanelAPI parent, int width, int height,
            String spriteID, Color color, Color fillColor
        ) {
            super(parent, width, height, spriteID, color, fillColor);

            final Color gColor = constructionMode == ConstructionMode.NORMAL ? Color.WHITE : Color.BLACK;

            ImgGlow.fader = glow.fader;
            ImgGlow.type = GlowType.ADDITIVE;
            ImgGlow.additiveBrightness = 1f;
            ImgGlow.additiveSprite = m_sprite;
            ImgGlow.color = NativeUiUtils.adjustBrightness(gColor, 0.33f);

            audio.useDisabledSound = (!DebugFlags.COLONY_DEBUG && !m_industry.getMarket().isPlayerOwned());
        }
    }
}
package wfg_ltv_econ.ui.panels;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.awt.Color;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.SpecialItemData;
import com.fs.starfarer.api.campaign.SpecialItemSpecAPI;
import com.fs.starfarer.api.campaign.econ.CommoditySpecAPI;
import com.fs.starfarer.api.campaign.econ.Industry;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.campaign.listeners.DialogCreatorUI;
import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.impl.campaign.DebugFlags;
import com.fs.starfarer.api.impl.campaign.econ.impl.ConstructionQueue.ConstructionQueueItem;
import com.fs.starfarer.api.ui.Alignment;
import com.fs.starfarer.api.ui.CustomPanelAPI;
import com.fs.starfarer.api.ui.Fonts;
import com.fs.starfarer.api.ui.IconRenderMode;
import com.fs.starfarer.api.ui.LabelAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.ui.UIComponentAPI;
import com.fs.starfarer.api.ui.UIPanelAPI;
import com.fs.starfarer.api.util.FaderUtil;
import com.fs.starfarer.api.util.Misc;
import com.fs.starfarer.api.util.Pair;
import com.fs.starfarer.campaign.CampaignEngine;

// import com.fs.starfarer.campaign.ui.marketinfo.b;
// import com.fs.starfarer.campaign.ui.marketinfo.intnew;
// import com.fs.starfarer.ui.impl.o0OO;
import com.fs.starfarer.campaign.ui.N;
import com.fs.graphics.A.D;

import wfg_ltv_econ.economy.CommodityStats;
import wfg_ltv_econ.economy.EconomyEngine;
import wfg_ltv_econ.industry.LtvBaseIndustry;
import wfg_ltv_econ.ui.LtvUIState;
import wfg_ltv_econ.ui.LtvUIState.UIState;
import wfg_ltv_econ.ui.panels.LtvCustomPanel.HasActionListener;
import wfg_ltv_econ.ui.panels.LtvCustomPanel.HasBackground;
import wfg_ltv_econ.ui.panels.LtvCustomPanel.HasFader;
import wfg_ltv_econ.ui.panels.LtvCustomPanel.HasTooltip.PendingTooltip;
import wfg_ltv_econ.ui.plugins.BasePanelPlugin;
import wfg_ltv_econ.ui.plugins.IndustryWidgetPlugin;
import wfg_ltv_econ.ui.plugins.LtvSpritePanelPlugin;
import wfg_ltv_econ.ui.systems.FaderSystem.Glow;
import wfg_ltv_econ.util.ListenerFactory;
import wfg_ltv_econ.util.NumFormat;
import wfg_ltv_econ.util.ReflectionUtils;
import wfg_ltv_econ.util.UiUtils;
import wfg_ltv_econ.util.UiUtils.AnchorType;

public class LtvIndustryWidget extends LtvCustomPanel<IndustryWidgetPlugin, LtvIndustryWidget, TooltipMakerAPI>
    implements HasBackground, HasFader, HasActionListener {

    public final static int pad = 3;
    public final static int opad = 10;
    public final static int PANEL_WIDTH = 190;
    public final static int TITLE_HEIGHT = 15 + pad;
    public final static int IMAGE_HEIGHT = 95;
    public final static int TOTAL_HEIGHT = TITLE_HEIGHT + IMAGE_HEIGHT;
    public final static int ICON_SIZE = 32;

    public final Color baseColor;
    public final Color darkColor;
    public final Color gridColor;
    public final Color brightColor;

    private Industry m_industry;
    private IndustryImagePanel industryIcon;
    private boolean isListenerEnabled = true;
    private boolean tradeInfoPanel;
    private LtvIndustryListPanel IndustryPanel;
    private int constructionQueueIndex;
    private LabelAPI buildingTitleHeader;
    private LabelAPI constructionStatusText;
    private ConstructionMode constructionMode;
    private Color BgColor = null;
    protected final List<LabelAPI> labels = new ArrayList<>();

    /*
     * Shared both by the Widget and IndustryImagePanel
     */
    private FaderUtil m_fader = null;
    public PendingTooltip<CustomPanelAPI> m_tooltip = null;

    public LtvIndustryWidget(UIPanelAPI root, UIPanelAPI parent, IndustryWidgetPlugin plugin,
        MarketAPI market, Industry ind, LtvIndustryListPanel indPanel) {

        this(root, parent, plugin, market, ind, indPanel, -1);
    }

    public LtvIndustryWidget(UIPanelAPI root, UIPanelAPI parent, IndustryWidgetPlugin plugin,
            MarketAPI market, Industry ind, LtvIndustryListPanel indPanel, int queue) {
        super(root, parent, PANEL_WIDTH, IMAGE_HEIGHT + TITLE_HEIGHT, plugin, market);

        constructionMode = ConstructionMode.NORMAL;
        m_industry = ind;
        IndustryPanel = indPanel;
        constructionQueueIndex = queue;

        baseColor = getFaction().getBaseUIColor();
        darkColor = getFaction().getDarkUIColor();
        gridColor = getFaction().getGridUIColor();
        brightColor = getFaction().getBrightUIColor();

        BgColor = m_industry.isImproved() ? Misc.getStoryDarkColor() : darkColor;

        m_fader = new FaderUtil(0.3f, 0.1f, 0.4f, true, false);
        m_tooltip = new PendingTooltip<>();

        initializePlugin(hasPlugin);
        createPanel();
    }

    public void initializePlugin(boolean hasPlugin) {
        getPlugin().init(this);
        getPlugin().setOffsets(-1, -1, 2, 2);
    }

    @Override
    public Color getBgColor() {
        return BgColor;
    }

    @Override
    public void setBgColor(Color color) {
        BgColor = color;
    }

    @Override
    public boolean isBgEnabled() {
        return true;
    }

    @Override
    public float getBgTransparency() {
        return 1;
    }

    @Override
    public FaderUtil getFader() {
        return m_fader;
    }

    @Override
    public boolean isFaderOwner() {
        return false;
    }

    @Override
    public Glow getGlowType() {
        return Glow.UNDERLAY;
    }

    @Override
    public Color getGlowColor() {
        return getFaction().getBaseUIColor();
    }

    @Override
    public boolean isListenerEnabled() {
        return isListenerEnabled;
    }

    public void createPanel() {

        LtvBasePanel titlePanel = new LtvBasePanel(
            getRoot(), getPanel(), getMarket(), PANEL_WIDTH, TITLE_HEIGHT, new BasePanelPlugin<>()
        ) {
            @Override
            public void createPanel() {
                buildingTitleHeader = Global.getSettings().createLabel(
                    m_industry.getCurrentName(), Fonts.DEFAULT_SMALL
                );
                buildingTitleHeader.setColor(
                    m_industry.isImproved() ? Misc.getStoryOptionColor() : baseColor
                );
                buildingTitleHeader.setHighlightColor(
                    UiUtils.adjustBrightness(buildingTitleHeader.getColor(), 1.33f)
                );
                buildingTitleHeader.setAlignment(Alignment.LMID);
                buildingTitleHeader.autoSizeToWidth(PANEL_WIDTH + 50);

                add(buildingTitleHeader).inLMid(pad);
            }

            public boolean isBgEnabled() {
                return false;
            }
        };

        add(titlePanel.getPanel()).inTL(0, 0);


        industryIcon = new IndustryImagePanel(
            getRoot(),
            m_panel,
            getMarket(),
            PANEL_WIDTH,
            IMAGE_HEIGHT,
            new LtvSpritePanelPlugin<>(),
            m_industry.getCurrentImage(),
            Color.WHITE,
            null,
            false
        );
        industryIcon.setActionListener(this);

        if (!m_industry.isFunctional() || constructionQueueIndex >= 0) {
            industryIcon.setColor(darkColor);
        }

        if (!DebugFlags.COLONY_DEBUG && !getMarket().isPlayerOwned()) {
            industryIcon.setColor(Color.white);
            isListenerEnabled = false;
        }

        add(industryIcon.getPanel()).inBL(0, 0);


        LabelAPI workerCountLabel = Global.getSettings().createLabel("", Fonts.DEFAULT_SMALL);
        workerCountLabel.setColor(Misc.getHighlightColor());
        workerCountLabel.setHighlightColor(
            UiUtils.adjustBrightness(workerCountLabel.getColor(), 1.33f)
        );
        if (m_industry instanceof LtvBaseIndustry baseIndustry) {
            int assigned = baseIndustry.getWorkerAssigned();
            if (baseIndustry.isWorkerAssignable()) {
                String assignedStr = NumFormat.engNotation(assigned);

                workerCountLabel.setText(assignedStr);
                workerCountLabel.setOpacity(0.9f);
                workerCountLabel.autoSizeToWidth(100f);
            }
        }

        add(workerCountLabel).inTL(pad*2, TITLE_HEIGHT + pad*2);

        TooltipMakerAPI tp = getPanel().createUIElement(PANEL_WIDTH, IMAGE_HEIGHT, false);

        tp.beginIconGroup();
        tp.setIconSpacingMedium();

        for (Pair<String, Integer> deficitEntry : m_industry.getAllDeficit()) {
            CommodityStats stats = EconomyEngine.getInstance().getComStats(deficitEntry.one, getMarket());

            if (stats.getDeficit() < 1) {
                continue;
            }

            int iconCount = 1;

            if (stats.getAvailabilityRatio() < 0.67f) iconCount = 2;
            if (stats.getAvailabilityRatio() < 0.33f) iconCount = 3;

            tp.addIcons(stats.m_com, iconCount, IconRenderMode.RED);
        }
        tp.addIconGroup(24, 1, pad);
        tp.getPrev().getPosition().inBL(pad + 2, pad);


        tp.beginIconGroup();
        tp.setIconSpacingWide();

        int totalW = 0;
        List<SpecialItemData> visibleItems = m_industry.getVisibleInstalledItems();
        for (SpecialItemData item : visibleItems) {

            SpecialItemSpecAPI spec = Global.getSettings().getSpecialItemSpec(item.getId());

            LtvSpritePanel.Base itemPanel = new LtvSpritePanel.Base(
                getRoot(),
                m_panel,
                getMarket(),
                28, 28,
                new LtvSpritePanelPlugin<>(),
                spec.getIconName(),
                Color.WHITE, null, false
            );
            itemPanel.setDrawTexOutline(true);
            itemPanel.setTexOutlineColor(baseColor);

            add(itemPanel.getPanel()).inTR(pad*2 + totalW, TITLE_HEIGHT + pad*2);
            
            totalW += itemPanel.getPos().getWidth() + pad*2;
        }

        if (m_industry.getAICoreId() != null) {

            CommoditySpecAPI spec = Global.getSettings().getCommoditySpec(m_industry.getAICoreId());

            LtvSpritePanel.Base aiCorePanel = new LtvSpritePanel.Base(
                getRoot(),
                m_panel,
                getMarket(),
                28, 28,
                new LtvSpritePanelPlugin<>(),
                spec.getIconName(),
                Color.WHITE, null, false
            );
            aiCorePanel.setDrawTexOutline(true);
            aiCorePanel.setTexOutlineColor(baseColor);

            add(aiCorePanel.getPanel()).inTR(pad + 2 + totalW, TITLE_HEIGHT + pad*2);
        }

        
        boolean isIndNotFunctional = m_industry.isBuilding() || m_industry.isDisrupted();
        if (isIndNotFunctional) {
            if (m_industry.isBuilding() && !m_industry.isUpgrading() && !m_industry.isDisrupted()) {

                tp.setParaFont(Fonts.INSIGNIA_VERY_LARGE);
                constructionStatusText = tp.createLabel("Building", baseColor);
                constructionStatusText.setHighlightColor(
                    UiUtils.adjustBrightness(constructionStatusText.getColor(), 1.33f)
                );

                constructionStatusText.autoSizeToWidth(PANEL_WIDTH);
                constructionStatusText.getPosition().inMid().setYAlignOffset(-TITLE_HEIGHT / 2);
            }

            N slider = new N((String) null, 0, 100);

            slider.getValue().getRenderer().o00000(D.Ò00000(Fonts.VICTOR_10));
            slider.getValue().getPosition().setYAlignOffset(1);
            slider.setBarColor(Misc.interpolateColor(baseColor, darkColor, 0.5f));
            slider.setTextColor(baseColor);
            slider.setProgress(m_industry.getBuildOrUpgradeProgress() * 100);
            slider.setText(m_industry.getBuildOrUpgradeProgressText());
            slider.setShowLabelOnly(true);
            final int sliderHeight = 12;
            tp.addComponent(slider).setSize(PANEL_WIDTH, sliderHeight).inBL(0, -sliderHeight - 2);
        }

        tp.setHeightSoFar(IMAGE_HEIGHT);
        add(tp).inBL(0, 0);

        if (constructionQueueIndex >= 0) {
            setNormalMode();
        }
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
        if (Misc.getCurrentlyBeingConstructed(getMarket()) == null && constructionQueueIndex == 0) {
            txt = "Building";
        }

        remove(constructionStatusText); 
        constructionStatusText = Global.getSettings().createLabel(txt, Fonts.INSIGNIA_VERY_LARGE);
        constructionStatusText.setColor(baseColor);
        constructionStatusText.setHighlightColor(
            UiUtils.adjustBrightness(constructionStatusText.getColor(), 1.33f)
        );
        constructionStatusText.autoSizeToWidth(constructionStatusText.computeTextWidth(txt));

        add(constructionStatusText).inMid().setYAlignOffset(-TITLE_HEIGHT / 2f);

        constructionMode = ConstructionMode.NORMAL;
        addCostTimeLabels();
    }

    public void setRemoveMode() {
        ConstructionQueueItem queueItem = null;
        if (getMarket().getConstructionQueue().getItems().size() > constructionQueueIndex && constructionQueueIndex >= 0) {
            queueItem = getMarket().getConstructionQueue().getItems().get(constructionQueueIndex);
        }

        if (queueItem != null) {
            clearLabels();
            LabelAPI removeLabel = Global.getSettings().createLabel(
                "Click to remove", Fonts.DEFAULT_SMALL
            );
            removeLabel.setColor(baseColor);
            removeLabel.setHighlightColor(
                UiUtils.adjustBrightness(removeLabel.getColor(), 1.33f)
            );

            LabelAPI refundLabel = Global.getSettings().createLabel(
                Misc.getDGSCredits(queueItem.cost), Fonts.DEFAULT_SMALL
            );
            refundLabel.setColor(Misc.getHighlightColor());
            refundLabel.setHighlightColor(
                UiUtils.adjustBrightness(refundLabel.getColor(), 1.33f)
            );

            LabelAPI refundLabelAppendix = Global.getSettings().createLabel(
                " refund", Fonts.DEFAULT_SMALL
            );
            refundLabelAppendix.setColor(baseColor);
            refundLabelAppendix.setHighlightColor(
                UiUtils.adjustBrightness(refundLabelAppendix.getColor(), 1.33f)
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
            UiUtils.adjustBrightness(swapLabel.getColor(), 1.33f)
        );
        
        labels.add(swapLabel);
        add(swapLabel).aboveMid((UIComponentAPI)constructionStatusText, 0);
        constructionMode = ConstructionMode.SWAP;
        addCostTimeLabels();
    }

    protected void addCostTimeLabels() {
        if (getMarket().getConstructionQueue().getItems().size() > constructionQueueIndex && constructionQueueIndex >= 0) {
            ConstructionQueueItem queueItem = (ConstructionQueueItem) getMarket()
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
                buildTimeLabel.setColor(Misc.getHighlightColor());
                buildTimeLabel.setHighlightColor(
                    UiUtils.adjustBrightness(buildTimeLabel.getColor(), 1.33f)
                );

                LabelAPI buildTimeAppendix = Global.getSettings().createLabel(
                    " " + buildText, Fonts.DEFAULT_SMALL
                );
                buildTimeAppendix.setColor(baseColor);
                buildTimeAppendix.setHighlightColor(
                    UiUtils.adjustBrightness(buildTimeAppendix.getColor(), 1.33f)
                );
                
                LabelAPI costLabel = Global.getSettings().createLabel(
                    Misc.getDGSCredits(queueItem.cost), Fonts.DEFAULT_SMALL
                );
                costLabel.setColor(Misc.getHighlightColor());
                costLabel.setHighlightColor(
                    UiUtils.adjustBrightness(costLabel.getColor(), 1.33f)
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
    public void onClicked(LtvCustomPanel<?, ?, ?> source, boolean isLeftClick) {
        if (tradeInfoPanel) {
            return;
        }
        LtvIndustryWidget targetInd;

        if (constructionQueueIndex >= 0) {

            if (!isLeftClick) {
                for (Object widgetObj : IndustryPanel.getWidgets()) {
                    if (widgetObj instanceof LtvIndustryWidget widget && widget.getQueueIndex() >= 0) {
                        widget.setNormalMode();
                    }
                }

                return;
            }

            if (constructionMode == ConstructionMode.NORMAL) {
                for (Object widgetObj : IndustryPanel.getWidgets()) {
                if (widgetObj instanceof LtvIndustryWidget widget && widget.getQueueIndex() >= 0) {
                    if (widget == this) {
                        widget.setRemoveMode();
                    } else {
                        widget.setSwapMode();
                    }
                }
                }

            } else if (constructionMode == ConstructionMode.SWAP) {
                targetInd = null;

                for (Object widgetObj : IndustryPanel.getWidgets()) {
                    if (widgetObj instanceof LtvIndustryWidget widget &&
                        widget.getQueueIndex() >= 0 &&
                        widget.constructionMode == ConstructionMode.REMOVE
                    ) {
                        targetInd = widget;
                        break;
                    }
                }

                // Swap industries

                List<ConstructionQueueItem> queueItems = getMarket().getConstructionQueue().getItems();
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

                    if (queueItems.indexOf(sourceItem) != 0 && queueItems.indexOf(targetItem) != 0) {
                        IndustryPanel.createPanel();
                    } else {
                        IndustryPanel.recreateOverview();
                    }
                }
            } else if (constructionMode == ConstructionMode.REMOVE) {
                List<ConstructionQueueItem> queueItems = getMarket().getConstructionQueue().getItems();
                if (constructionQueueIndex < queueItems.size() && constructionQueueIndex >= 0) {

                    final ConstructionQueueItem item = queueItems.get(constructionQueueIndex);
                    getMarket().getConstructionQueue().removeItem(item.id);

                    int itemCost = item.cost;
                    if (itemCost > 0) {
                        Global.getSector().getPlayerFleet().getCargo().getCredits().add(itemCost);
                        Misc.addCreditsMessage("Received %s", itemCost);
                    }

                    if (constructionQueueIndex == 0) {
                        IndustryPanel.recreateOverview();
                    } else {
                        IndustryPanel.createPanel();
                    }
                }
            }
        } else {
            for (Object widgetObj : IndustryPanel.getWidgets()) {
                if (widgetObj instanceof LtvIndustryWidget widget && widget.getQueueIndex() >= 0) {
                    widget.setNormalMode();
                }
            }

            if (LtvIndustryListPanel.indOptCtor != null && getIndustryPanel().dummyWidget != null) {
                // b var17 = new b(this.øôöO00, this, var14, CampaignEngine.getInstance().getCampaignUI().getDialogParent(), this);

                Object listener = new ListenerFactory.DialogDismissedListener() {
                    @Override
                    public void trigger(Object... args) {
                        dialogDismissed();
                    }
                }.getProxy();

                DialogCreatorUI dialog = (DialogCreatorUI) LtvIndustryListPanel.indOptCtor.newInstance(
                    m_industry,
                    getIndustryPanel().dummyWidget,
                    getIndustryPanel().getMarketInteractionMode(),
                    CampaignEngine.getInstance().getCampaignUI().getDialogParent(),
                    listener
                );
                ReflectionUtils.invoke(dialog, "show", 0f, 0f);

                UiUtils.anchorPanel(
                    ((UIPanelAPI)dialog), industryIcon.getPanel(), AnchorType.MidTopLeft, 0
                );

            }
            tradeInfoPanel = true;

            LtvUIState.setState(UIState.DETAIL_DIALOG);
        }
    }

    public void renderImpl(float alphaMult) {
        if (industryIcon.getFader().getBrightness() > 0) {
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
        ReflectionUtils.getMethodsMatching(getIndustryPanel().dummyWidget, "dialogDismissed", void.class, 2)
        .get(0).invoke(getIndustryPanel().dummyWidget, null, 0);

        LtvUIState.setState(UIState.NONE);
    }

    public Industry getIndustry() {
        return m_industry;
    }

    public IndustryImagePanel getIndustryIcon() {
        return industryIcon;
    }

    public LtvIndustryListPanel getIndustryPanel() {
        return IndustryPanel;
    }

    public enum ConstructionMode {
        NORMAL,
        SWAP,
        REMOVE,
    }

    public class IndustryImagePanel extends LtvSpritePanel<IndustryImagePanel> 
        implements HasFader, AcceptsActionListener, HasTooltip, HasAudioFeedback {

        HasActionListener m_listener = null;

        public IndustryImagePanel(UIPanelAPI root, UIPanelAPI parent, MarketAPI market, int width, int height,
            LtvSpritePanelPlugin<IndustryImagePanel> plugin, String spriteID, Color color, Color fillColor, boolean drawBorder) {
            super(root, parent, market, width, height, plugin, spriteID, color, fillColor, drawBorder);
        }

        @Override
        public FaderUtil getFader() {
            return m_fader;
        }

        @Override
        public Glow getGlowType() {
            return Glow.ADDITIVE;
        }

        @Override
        public float getAdditiveBrightness() {
            return 1;
        }

        @Override
        public Optional<SpriteAPI> getSprite() {
            return Optional.ofNullable(m_sprite);
        }

        @Override
        public Color getGlowColor() {
            Color gColor = constructionMode == ConstructionMode.NORMAL ? Color.WHITE : Color.BLACK; 
            return UiUtils.adjustBrightness(gColor, 0.33f);
        }

        @Override
        public Optional<HasActionListener> getActionListener() {
            return Optional.ofNullable(m_listener);
        }

        @Override
        public void setActionListener(HasActionListener a) {
            m_listener = a;
        }

        @Override
        public CustomPanelAPI getTpParent() {
            return m_tooltip.getParent.get();
        }

        @Override
        public TooltipMakerAPI createAndAttachTp() {
            return m_tooltip.factory.get();
        }
    }
}

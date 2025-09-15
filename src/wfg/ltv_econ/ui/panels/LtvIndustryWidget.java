package wfg.ltv_econ.ui.panels;

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

import wfg.ltv_econ.economy.CommodityStats;
import wfg.ltv_econ.economy.EconomyEngine;
import wfg.ltv_econ.economy.WorkerRegistry;
import wfg.ltv_econ.economy.WorkerRegistry.WorkerIndustryData;
import wfg.ltv_econ.industry.IndustryIOs;
import wfg.ltv_econ.ui.plugins.IndustryWidgetPlugin;
import wfg.ltv_econ.util.ListenerFactory;
import wfg.wrap_ui.util.WrapUiUtils.AnchorType;
import wfg.wrap_ui.ui.UIState;
import wfg.wrap_ui.ui.UIState.State;
import wfg.wrap_ui.ui.panels.BasePanel;
import wfg.wrap_ui.ui.panels.CustomPanel;
import wfg.wrap_ui.ui.panels.SpritePanel;
import wfg.wrap_ui.ui.panels.CustomPanel.HasActionListener;
import wfg.wrap_ui.ui.panels.CustomPanel.HasBackground;
import wfg.wrap_ui.ui.panels.CustomPanel.HasFader;
import wfg.wrap_ui.ui.panels.CustomPanel.HasTooltip.PendingTooltip;
import wfg.wrap_ui.ui.plugins.BasePanelPlugin;
import wfg.wrap_ui.ui.plugins.SpritePanelPlugin;
import wfg.wrap_ui.ui.systems.FaderSystem.Glow;
import wfg.wrap_ui.util.NumFormat;
import wfg.wrap_ui.util.WrapUiUtils;
import wfg.reflection.ReflectionUtils;

// import com.fs.starfarer.campaign.ui.marketinfo.b;
// import com.fs.starfarer.campaign.ui.marketinfo.intnew;
// import com.fs.starfarer.ui.impl.o0OO;
import com.fs.graphics.A.D;
import com.fs.starfarer.campaign.ui.N;


public class LtvIndustryWidget extends CustomPanel<IndustryWidgetPlugin, LtvIndustryWidget, TooltipMakerAPI>
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

        final int hOffset = m_industry.isStructure() ? TITLE_HEIGHT : 0;
        getPlugin().setOffsets(-1, -1, 2, 2 - hOffset);
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

        BasePanel titlePanel = new BasePanel(
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
                    WrapUiUtils.adjustBrightness(buildingTitleHeader.getColor(), 1.33f)
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
            new SpritePanelPlugin<>(),
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


        final WorkerIndustryData data = WorkerRegistry.getInstance().getData(
            getMarket().getId(), m_industry.getId()
        );
        LabelAPI workerCountLabel = Global.getSettings().createLabel("", Fonts.DEFAULT_SMALL);
        workerCountLabel.setColor(Misc.getHighlightColor());
        workerCountLabel.setHighlightColor(
            WrapUiUtils.adjustBrightness(workerCountLabel.getColor(), 1.33f)
        );
        if (data != null) {

            final String assignedStr = NumFormat.engNotation(data.getWorkersAssigned());

            workerCountLabel.setText(assignedStr);
            workerCountLabel.setOpacity(0.9f);
            workerCountLabel.autoSizeToWidth(100f);
        }

        add(workerCountLabel).inTL(pad*2, TITLE_HEIGHT + pad*2);

        TooltipMakerAPI tp = getPanel().createUIElement(PANEL_WIDTH, IMAGE_HEIGHT, false);

        tp.beginIconGroup();
        tp.setIconSpacingMedium();

        boolean hasConfig = IndustryIOs.hasConfig(m_industry);

        if (hasConfig && m_industry.isFunctional() && !m_industry.isBuilding()) {
            for (String comID : IndustryIOs.getInputs(m_industry, false)) {
                CommoditySpecAPI spec = Global.getSettings().getCommoditySpec(comID);
                CommodityStats stats = EconomyEngine.getInstance().getComStats(comID, getMarket().getId());

                if (stats == null || stats.getDeficit() < 1) continue;

                int iconCount = 1;
                if (stats.getAvailabilityRatio() < 0.67f) iconCount = 2;
                if (stats.getAvailabilityRatio() < 0.33f) iconCount = 3;

                tp.addIcons(spec, iconCount, IconRenderMode.RED);
            }
        } else if (m_industry.isFunctional() && !m_industry.isBuilding()) {
            for (Pair<String, Integer> pair : m_industry.getAllDeficit()) {
                CommoditySpecAPI spec = Global.getSettings().getCommoditySpec(pair.one);
                CommodityStats stats = EconomyEngine.getInstance().getComStats(pair.one, getMarket().getId());

                if (stats == null || stats.getDeficit() < 1) continue;

                int iconCount = 1;

                if (stats.getAvailabilityRatio() < 0.67f) iconCount = 2;
                if (stats.getAvailabilityRatio() < 0.33f) iconCount = 3;

                tp.addIcons(spec, iconCount, IconRenderMode.RED);
            }
        }
        tp.addIconGroup(24, 1, pad);
        tp.getPrev().getPosition().inBL(pad + 2, pad);


        tp.beginIconGroup();
        tp.setIconSpacingWide();

        int totalW = 0;
        List<SpecialItemData> visibleItems = m_industry.getVisibleInstalledItems();
        for (SpecialItemData item : visibleItems) {

            SpecialItemSpecAPI spec = Global.getSettings().getSpecialItemSpec(item.getId());

            SpritePanel.Base itemPanel = new SpritePanel.Base(
                getRoot(),
                m_panel,
                getMarket(),
                28, 28,
                new SpritePanelPlugin<>(),
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

            SpritePanel.Base aiCorePanel = new SpritePanel.Base(
                getRoot(),
                m_panel,
                getMarket(),
                28, 28,
                new SpritePanelPlugin<>(),
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
                    WrapUiUtils.adjustBrightness(constructionStatusText.getColor(), 1.33f)
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
            WrapUiUtils.adjustBrightness(constructionStatusText.getColor(), 1.33f)
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
                WrapUiUtils.adjustBrightness(removeLabel.getColor(), 1.33f)
            );

            LabelAPI refundLabel = Global.getSettings().createLabel(
                Misc.getDGSCredits(queueItem.cost), Fonts.DEFAULT_SMALL
            );
            refundLabel.setColor(Misc.getHighlightColor());
            refundLabel.setHighlightColor(
                WrapUiUtils.adjustBrightness(refundLabel.getColor(), 1.33f)
            );

            LabelAPI refundLabelAppendix = Global.getSettings().createLabel(
                " refund", Fonts.DEFAULT_SMALL
            );
            refundLabelAppendix.setColor(baseColor);
            refundLabelAppendix.setHighlightColor(
                WrapUiUtils.adjustBrightness(refundLabelAppendix.getColor(), 1.33f)
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
            WrapUiUtils.adjustBrightness(swapLabel.getColor(), 1.33f)
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
                    WrapUiUtils.adjustBrightness(buildTimeLabel.getColor(), 1.33f)
                );

                LabelAPI buildTimeAppendix = Global.getSettings().createLabel(
                    " " + buildText, Fonts.DEFAULT_SMALL
                );
                buildTimeAppendix.setColor(baseColor);
                buildTimeAppendix.setHighlightColor(
                    WrapUiUtils.adjustBrightness(buildTimeAppendix.getColor(), 1.33f)
                );
                
                LabelAPI costLabel = Global.getSettings().createLabel(
                    Misc.getDGSCredits(queueItem.cost), Fonts.DEFAULT_SMALL
                );
                costLabel.setColor(Misc.getHighlightColor());
                costLabel.setHighlightColor(
                    WrapUiUtils.adjustBrightness(costLabel.getColor(), 1.33f)
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
    public void onClicked(CustomPanel<?, ?, ?> source, boolean isLeftClick) {
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

                    IndustryPanel.createPanel();
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

                    IndustryPanel.createPanel();
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
                    LtvIndustryListPanel.getMarketInteractionMode(getMarket()),
                    CampaignEngine.getInstance().getCampaignUI().getDialogParent(),
                    listener
                );
                ReflectionUtils.invoke(dialog, "show", 0f, 0f);

                WrapUiUtils.anchorPanel(
                    ((UIPanelAPI)dialog), industryIcon.getPanel(), AnchorType.MidTopLeft, 0
                );

            }
            tradeInfoPanel = true;

            UIState.setState(State.DETAIL_DIALOG);
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

        UIState.setState(State.NONE);
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

    public class IndustryImagePanel extends SpritePanel<IndustryImagePanel> 
        implements HasFader, AcceptsActionListener, HasTooltip, HasAudioFeedback {

        HasActionListener m_listener = null;

        public IndustryImagePanel(UIPanelAPI root, UIPanelAPI parent, MarketAPI market, int width, int height,
            SpritePanelPlugin<IndustryImagePanel> plugin, String spriteID, Color color, Color fillColor, boolean drawBorder) {
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
            return WrapUiUtils.adjustBrightness(gColor, 0.33f);
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

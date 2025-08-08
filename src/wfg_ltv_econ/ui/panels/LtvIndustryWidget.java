package wfg_ltv_econ.ui.panels;

import java.util.ArrayList;
import java.util.List;

import java.awt.Color;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignUIAPI.DismissDialogDelegate;
import com.fs.starfarer.api.campaign.CustomUIPanelPlugin;
import com.fs.starfarer.api.campaign.SpecialItemData;
import com.fs.starfarer.api.campaign.econ.CommodityOnMarketAPI;
import com.fs.starfarer.api.campaign.econ.Industry;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI.MarketInteractionMode;
import com.fs.starfarer.api.impl.campaign.DebugFlags;
import com.fs.starfarer.api.impl.campaign.econ.impl.ConstructionQueue.ConstructionQueueItem;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.ui.Alignment;
import com.fs.starfarer.api.ui.ButtonAPI;
import com.fs.starfarer.api.ui.CutStyle;
import com.fs.starfarer.api.ui.Fonts;
import com.fs.starfarer.api.ui.IconRenderMode;
import com.fs.starfarer.api.ui.LabelAPI;
import com.fs.starfarer.api.ui.PositionAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.ui.UIComponentAPI;
import com.fs.starfarer.api.ui.UIPanelAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI.ActionListenerDelegate;
import com.fs.starfarer.api.util.FaderUtil;
import com.fs.starfarer.api.util.Misc;
import com.fs.starfarer.api.util.Pair;
import com.fs.starfarer.campaign.ui.marketinfo.IndustryListPanel;

import com.fs.starfarer.campaign.ui.N;
import com.fs.starfarer.campaign.ui.marketinfo.b;
import com.fs.graphics.A.D;

import wfg_ltv_econ.industry.LtvBaseIndustry;
import wfg_ltv_econ.ui.plugins.IndustryPanelPlugin;
import wfg_ltv_econ.ui.plugins.LtvCustomPanelPlugin;
import wfg_ltv_econ.ui.plugins.LtvSpritePanelPlugin;
import wfg_ltv_econ.ui.plugins.LtvCustomPanelPlugin.Glow;
import wfg_ltv_econ.ui.plugins.LtvCustomPanelPlugin.Outline;
import wfg_ltv_econ.util.LtvMarketReplacer;
import wfg_ltv_econ.util.NumFormat;
import wfg_ltv_econ.util.ReflectionUtils;
import wfg_ltv_econ.util.RenderUtils;
import wfg_ltv_econ.util.UiUtils;

public class LtvIndustryWidget extends LtvCustomPanel implements ActionListenerDelegate, DismissDialogDelegate {

    public final static int pad = 3;
    public final static int opad = 10;
    public final static int PANEL_WIDTH = 190;
    public final static int TITLE_HEIGHT = 15 + pad;
    public final static int IMAGE_HEIGHT = 95;
    public final static int ICON_SIZE = 32;

    public final Color baseColor;
    public final Color darkColor;
    public final Color gridColor;
    public final Color brightColor;

    private Industry m_industry;
    private LtvSpritePanel industryIcon;
    private ButtonAPI constructionActionButton;
    private boolean tradeInfoPanel;
    private LtvIndustryListPanel IndustryPanel;
    private int constructionQueueIndex;
    private LabelAPI buildingTitleHeader;
    private LabelAPI constructionStatusText;
    private ConstructionMode constructionMode;
    protected final List<LabelAPI> labels = new ArrayList<>();

    public LtvIndustryWidget(UIPanelAPI root, UIPanelAPI parent, CustomUIPanelPlugin plugin,
            MarketAPI market, Industry ind, LtvIndustryListPanel indPanel) {

        this(root, parent, plugin, market, ind, indPanel, -1);
    }

    public LtvIndustryWidget(UIPanelAPI root, UIPanelAPI parent, CustomUIPanelPlugin plugin,
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

        initializePlugin(hasPlugin);
        createPanel();
    }

    public void initializePlugin(boolean hasPlugin) {
        IndustryPanelPlugin plugin = ((IndustryPanelPlugin) getPlugin());
        plugin.init(this, Glow.NONE, false, true, Outline.NONE);

        getPlugin().setFader(new FaderUtil(0.2f, 0.2f, 0.2f, false, true));
        setBgColor(m_industry.isImproved() ? Misc.getStoryOptionColor() : darkColor);
        getPlugin().setBackgroundTransparency(1.0f);
    }

    public void createPanel() {

        // Button Listener
        TooltipMakerAPI tp = getPanel().createUIElement(
            PANEL_WIDTH, TITLE_HEIGHT + IMAGE_HEIGHT, false
        );

        tp.setActionListenerDelegate(this);

        LtvBasePanel titlePanel = new LtvBasePanel(
            getRoot(), getPanel(), m_market, PANEL_WIDTH, TITLE_HEIGHT, new LtvCustomPanelPlugin()
        ) {
            @Override
            public void createPanel() {
                buildingTitleHeader = Global.getSettings().createLabel(
                    m_industry.getCurrentName(), Fonts.DEFAULT_SMALL
                );
                buildingTitleHeader.setColor(
                    m_industry.isImproved() ? Misc.getStoryOptionColor() : baseColor
                );
                buildingTitleHeader.setAlignment(Alignment.LMID);
                buildingTitleHeader.autoSizeToWidth(PANEL_WIDTH);

                add(buildingTitleHeader).inLMid(pad);
            }
        };

        add(titlePanel.getPanel()).inTL(0, 0);


        constructionActionButton = tp.addButton(
            "",
            null,
            new Color(0, 0, 0, 0),
            new Color(0, 0, 0, 0),
            Alignment.MID,
            CutStyle.NONE,
            PANEL_WIDTH,
            IMAGE_HEIGHT,
            pad
        );
        constructionActionButton.setQuickMode(true);
        constructionActionButton.setOpacity(0.00001f);

        tp.addComponent(constructionActionButton).inBL(0, 0);


        industryIcon = new LtvSpritePanel(
            getRoot(),
            m_panel,
            m_market,
            PANEL_WIDTH,
            IMAGE_HEIGHT,
            new LtvSpritePanelPlugin(),
            m_industry.getCurrentImage(),
            Color.white,
            null,
            false
        );

        if (!m_industry.isFunctional() || constructionQueueIndex >= 0) {
            industryIcon.setColor(darkColor);
        }

        if (!DebugFlags.COLONY_DEBUG && !m_market.isPlayerOwned()) {
            industryIcon.setColor(Color.white);
            constructionActionButton.setEnabled(false);
        }

        add(industryIcon.getPanel()).inBL(0, 0);


        LabelAPI workerCountLabel = tp.createLabel("", Misc.getHighlightColor());
        if (m_industry instanceof LtvBaseIndustry) {
            int assigned = ((LtvBaseIndustry) m_industry).getWorkerAssigned();
            if (((LtvBaseIndustry) m_industry).isWorkerAssignable()) {
                String assignedStr = NumFormat.engNotation(assigned);

                workerCountLabel.setText(assignedStr);
                workerCountLabel.setOpacity(0.9f);
                workerCountLabel.autoSizeToWidth(100f);
            }
        }

        workerCountLabel.getPosition().inTL(pad*2, TITLE_HEIGHT + pad*2);


        tp.beginIconGroup();
        tp.setIconSpacingMedium();

        for (Pair<String, Integer> deficitEntry : m_industry.getAllDeficit()) {
            CommodityOnMarketAPI commodity = m_market.getCommodityData(deficitEntry.one);

            if (deficitEntry.two < 1) {
                continue;
            }

            tp.addIcons(commodity, 2, IconRenderMode.RED);
        }
        tp.addIconGroup(24, 1, pad);
        tp.getPrev().getPosition().inBL(pad + 2, pad);


        tp.beginIconGroup();
        tp.setIconSpacingWide();
        List<SpecialItemData> visibleItems = m_industry.getVisibleInstalledItems();
        for (SpecialItemData item : visibleItems) {
            CommodityOnMarketAPI com = m_market.getCommodityData(item.getId());
            
            tp.addIcons(com, 1, IconRenderMode.BLACK);

            // Global.getSettings().getSpecialItemSpec(item.getId())
        }

        if (m_industry.getAICoreId() != null) {
            CommodityOnMarketAPI AICore = m_market.getCommodityData(m_industry.getAICoreId());
            tp.addIcons(AICore, 1, IconRenderMode.BLACK);
        }

        tp.addIconGroup(ICON_SIZE, 1, pad);
        tp.getPrev().getPosition().inTR(pad + 2, TITLE_HEIGHT + pad*2);

        
        boolean isIndNotFunctional = m_industry.isBuilding() || m_industry.isDisrupted();
        if (isIndNotFunctional) {
            if (m_industry.isBuilding() && !m_industry.isUpgrading() && !m_industry.isDisrupted()) {

                tp.setParaFont(Fonts.INSIGNIA_VERY_LARGE);
                constructionStatusText = tp.createLabel("Building", baseColor);

                constructionStatusText.autoSizeToWidth(PANEL_WIDTH);
                constructionStatusText.getPosition().inMid().setYAlignOffset(-TITLE_HEIGHT / 2);
            }

            N slider = new N((String) null, 0, 100);
            slider.getValue().getRenderer().o00000(D.Ò00000(Fonts.VICTOR_10));
            slider.getValue().getPosition().setYAlignOffset(1);
            slider.setBarColor(Misc.interpolateColor(baseColor, darkColor, 0.5F));
            slider.setTextColor(baseColor);
            slider.setProgress(m_industry.getBuildOrUpgradeProgress() * 100);
            slider.setText(m_industry.getBuildOrUpgradeProgressText());
            slider.setShowLabelOnly(true);
            final int sliderHeight = 12;
            tp.addComponent(slider).setSize(PANEL_WIDTH, sliderHeight).inBL(0, -sliderHeight - 2);
        }

        getPanel().addUIElement(tp).inBL(0, 0);

        if (constructionQueueIndex >= 0) {
            setNormalMode();
        }
    }

    public void clearLabels() {
        for (LabelAPI label : labels) {
            getPanel().removeComponent((UIComponentAPI) label);
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
        constructionStatusText = Global.getSettings().createLabel(txt, "graphics/fonts/insignia25LTaa.fnt");
        constructionStatusText.setColor(baseColor);
        constructionStatusText.autoSizeToWidth(PANEL_WIDTH);

        add(constructionStatusText).inMid().setYAlignOffset(-TITLE_HEIGHT / 2);
        constructionActionButton.unhighlight();

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
            LabelAPI refundLabel = Global.getSettings().createLabel(
                Misc.getDGSCredits(queueItem.cost) + " refund", Fonts.DEFAULT_SMALL
            );
            removeLabel.setColor(baseColor);

            refundLabel.highlightFirst(Misc.getDGSCredits(queueItem.cost));
            refundLabel.setHighlightColor(Misc.getHighlightColor());

            labels.add(removeLabel);
            labels.add(refundLabel);

            add(removeLabel).aboveMid((UIComponentAPI)constructionStatusText, 0);
            add(refundLabel).belowMid((UIComponentAPI)constructionStatusText, 0);
            constructionActionButton.highlight();
            constructionMode = ConstructionMode.REMOVE;
            addCostTimeLabels();
        }
    }

    public void setSwapMode() {
        clearLabels();
        LabelAPI swapLabel = Global.getSettings().createLabel("Click to swap", Fonts.DEFAULT_SMALL);
        swapLabel.setColor(baseColor);
        
        labels.add(swapLabel);
        add(swapLabel).aboveMid((UIComponentAPI)constructionStatusText, 0);
        constructionActionButton.highlight();
        constructionMode = ConstructionMode.SWAP;
        addCostTimeLabels();
    }

    protected void addCostTimeLabels() {
        if (m_market.getConstructionQueue().getItems().size() > constructionQueueIndex && constructionQueueIndex >= 0) {
            ConstructionQueueItem queueItem = (ConstructionQueueItem) m_market
                .getConstructionQueue().getItems().get(constructionQueueIndex);
            if (queueItem != null) {
                final int buildTime = (int) m_industry.getSpec().getBuildTime();
                String buildText = "days";
                if (buildTime == 1) {
                    buildText = "day";
                }

                final Color highlight = Misc.interpolateColor(Misc.getHighlightColor(), Color.black, 0.5f);
                final Color base = Misc.interpolateColor(baseColor, darkColor, 0.5f);

                LabelAPI buildTimeLabel = Global.getSettings().createLabel(
                    buildTime + " " + buildText, Fonts.DEFAULT_SMALL
                );
                buildTimeLabel.setColor(base);
                buildTimeLabel.highlightFirst("" + buildTime);
                buildTimeLabel.setHighlightColor(highlight);

                LabelAPI costLabel = Global.getSettings().createLabel(
                    Misc.getDGSCredits(queueItem.cost), Fonts.DEFAULT_SMALL
                );
                buildTimeLabel.setColor(base);
                costLabel.highlightFirst(Misc.getDGSCredits(queueItem.cost));
                costLabel.setHighlightColor(highlight);

                labels.add(buildTimeLabel);
                labels.add(costLabel);

                final int labelOpad = 7;
                add(buildTimeLabel).inBL(labelOpad, pad);
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
    public void actionPerformed(Object data, Object source) {
        if (tradeInfoPanel || !(data instanceof InputEventAPI)) {
            return;
        }

        final InputEventAPI event = (InputEventAPI) data;
        LtvIndustryWidget targetInd;

        if (constructionQueueIndex >= 0) {

            if (event.isRMBEvent()) {
                for (LtvIndustryWidget widget : IndustryPanel.getWidgetsNew()) {
                    if (widget.getQueueIndex() >= 0) {
                        widget.setNormalMode();
                    }
                }

                return;
            }

            if (constructionMode == ConstructionMode.NORMAL) {
                for (LtvIndustryWidget widget : IndustryPanel.getWidgetsNew()) {
                if (widget.getQueueIndex() >= 0) {
                    if (widget == this) {
                        widget.setRemoveMode();
                    } else {
                        widget.setSwapMode();
                    }
                }
                }
            } else if (constructionMode == ConstructionMode.SWAP) {
                targetInd = null;

                for (LtvIndustryWidget widget : IndustryPanel.getWidgetsNew()) {
                    if (widget.getQueueIndex() >= 0
                            && widget.constructionMode == ConstructionMode.REMOVE) {
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

                    if (queueItems.indexOf(sourceItem) != 0 && queueItems.indexOf(targetItem) != 0) {
                        IndustryPanel.recreateOverviewNoEconStep();
                    } else {
                        IndustryPanel.recreateOverview();
                    }
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

                    if (constructionQueueIndex == 0) {
                        IndustryPanel.recreateOverview();
                    } else {
                        IndustryPanel.recreateOverviewNoEconStep();
                    }
                }
            }
        } else {
            for (LtvIndustryWidget widget : IndustryPanel.getWidgetsNew()) {
                if (widget.getQueueIndex() >= 0) {
                    widget.setNormalMode();
                }
            }

            try {
                UIPanelAPI infoPanel = IndustryPanel.getOverview().getInfoPanel();
                Object a = ReflectionUtils.invoke(infoPanel, "getTradePanel");
                Object outpostPanelParams = ReflectionUtils.invoke(a, "getOutpostPanelParams");

                MarketInteractionMode interactionMode = MarketInteractionMode.LOCAL;
                if (outpostPanelParams != null) {
                    interactionMode = MarketInteractionMode.REMOTE;
                }

                // No way to get the constructor of b for now
                // DialogCreatorUI dialog = new b(
                //     m_industry,
                //     (intnew) ((Object) this),
                //     interactionMode,
                //     getPanel(),
                //     this
                // );
                // dialog.showDialog(0,0,null);
                tradeInfoPanel = true;
            } catch (Exception e) {
                Global.getLogger(LtvMarketReplacer.class).error("Custom Widget failed", e);
            }
        }
    }

    public void renderImpl(float alphaMult) {
        PositionAPI pos = getPanel().getPosition();

        int x = (int) pos.getX();
        int y = (int) pos.getY();
        int w = (int) pos.getWidth();
        int h = IMAGE_HEIGHT - pad;
        int gap = 1;
        x -= gap;
        y -= gap;
        w += gap * 2;
        h += gap * 2;

        Color quadColor = m_industry.isImproved() ? Misc.getStoryDarkColor() : darkColor;

        if (m_industry.isIndustry()) {
            RenderUtils.drawQuad(x, y + h, w, TITLE_HEIGHT, quadColor, 1.0f, false);
        }

        RenderUtils.drawQuad(x, y, w, h, quadColor, alphaMult, false);

        float btnGlow = constructionActionButton.getGlowBrightness();

        if (btnGlow > 0) {
            Color glowColor = UiUtils.adjustAlpha(Color.white, btnGlow * 0.33f);

            buildingTitleHeader.setHighlightColor(glowColor);
            buildingTitleHeader.setHighlight(buildingTitleHeader.getText());

            if (constructionStatusText != null) {
                constructionStatusText.setHighlightColor(glowColor);
                constructionStatusText.setHighlight(constructionStatusText.getText());
            }

            for (LabelAPI label : labels) {
                label.setHighlightColor(glowColor);
                label.setHighlight(label.getText());
            }

        } else {
            buildingTitleHeader.setHighlightColor(null);
            if (constructionStatusText != null) {
                constructionStatusText.setHighlightColor(null);
            }
            for (LabelAPI label : labels) {
                label.setHighlightColor(null);
            }
        }

        if (m_industry.isIndustry() && btnGlow > 0) {
            RenderUtils.drawQuad(
                x, y + h, w, TITLE_HEIGHT, quadColor, alphaMult * btnGlow * 0.33F, true
            );
        }

    }

    @Override
    public void dialogDismissed() {
        tradeInfoPanel = false;
    }

    public ButtonAPI getButton() {
        return constructionActionButton;
    }

    public FaderUtil getGlow() {
        return getPlugin().getFader();
    }

    public Industry getIndustry() {
        return m_industry;
    }

    public IndustryListPanel getIndustryPanel() {
        return IndustryPanel;
    }

    public enum ConstructionMode {
        NORMAL,
        SWAP,
        REMOVE
    }
}

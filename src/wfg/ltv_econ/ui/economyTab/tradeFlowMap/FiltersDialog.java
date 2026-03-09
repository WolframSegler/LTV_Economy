package wfg.ltv_econ.ui.economyTab.tradeFlowMap;

import static wfg.ltv_econ.constants.EconomyConstants.visibleFactions;
import static wfg.native_ui.util.UIConstants.*;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.SettingsAPI;
import com.fs.starfarer.api.campaign.FactionSpecAPI;
import com.fs.starfarer.api.ui.ButtonAPI.UICheckboxSize;
import com.fs.starfarer.api.ui.Fonts;
import com.fs.starfarer.api.ui.LabelAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;

import wfg.native_ui.internal.ui.Side;
import wfg.native_ui.ui.ComponentFactory;
import wfg.native_ui.ui.components.NativeComponents;
import wfg.native_ui.ui.components.TooltipComp;
import wfg.native_ui.ui.core.UIBuildableAPI;
import wfg.native_ui.ui.panels.Button;
import wfg.native_ui.ui.panels.CheckboxButton;
import wfg.native_ui.ui.panels.DockPanel;
import wfg.native_ui.ui.panels.RadioPanel;
import wfg.native_ui.ui.panels.Slider;
import wfg.native_ui.ui.panels.Button.CutStyle;
import wfg.native_ui.ui.systems.NativeSystems;
import wfg.native_ui.ui.systems.TooltipSystem;
import wfg.native_ui.ui.panels.RadioPanel.LayoutMode;

public class FiltersDialog extends DockPanel {
    private static final SettingsAPI settings = Global.getSettings();

    private final UIBuildableAPI content;
    
    private Slider minAmountSlider;
    private float minAmountSliderValueCache = ComTradeFlowMap.minTradeAmount;

    public FiltersDialog(int width, int height, UIBuildableAPI content) {
        super(width, height, Side.RIGHT);

        this.content = content;

        buildUI();
    }

    @Override
    public void buildUI() {
        clearChildren();
        
        final LabelAPI title = settings.createLabel("Map Filters", Fonts.INSIGNIA_VERY_LARGE);
        add(title).inTL(opad, opad + pad*2);
        final int SECT_I_H = 36;
        final int LABEL_H = 16;
        final int BTN_H = 32;
        final int S_BTN_H = 18;

        final LabelAPI modeLblb = settings.createLabel("Direction Mode", Fonts.DEFAULT_SMALL);
        add(modeLblb).inTL(opad, SECT_I_H + opad*2);

        final RadioPanel modeRadio = new RadioPanel(m_panel, (int) pos.getWidth() - opad*2, BTN_H, LayoutMode.HORIZONTAL)
            .addOption("All")
            .addOption("Exporters")
            .addOption("Importers");
        modeRadio.optionSelected = (index) -> {
            ComTradeFlowMap.directionMode = index;
            content.buildUI();
        };
        modeRadio.setSelectedIndex(ComTradeFlowMap.directionMode);
        modeRadio.buildUI();
        add(modeRadio).inTL(opad, SECT_I_H + LABEL_H + opad*2);

        final LabelAPI minAmountLbl = settings.createLabel("Min Volume", Fonts.INSIGNIA_LARGE);
        final float lblW = minAmountLbl.getPosition().getWidth();
        add(minAmountLbl).inTL(opad, SECT_I_H + LABEL_H + BTN_H + opad*4);
        minAmountLbl.getPosition().setSize(lblW, BTN_H);
        final int sliderW = (int) (pos.getWidth() - opad*3 - lblW);

        minAmountSlider = new Slider(m_panel, null, 0f, 1000f, sliderW, BTN_H);
        minAmountSlider.setProgress(ComTradeFlowMap.minTradeAmount);
        add(minAmountSlider).inTL(opad + lblW + pad, SECT_I_H + LABEL_H + BTN_H + opad*4);
        minAmountSlider.system().setIfNotPresent(
            NativeSystems.TOOLTIP, TooltipSystem.get(), minAmountSlider
        );
        final TooltipComp sliderTp = minAmountSlider.comp().get(NativeComponents.TOOLTIP);
        sliderTp.builder = (tp, expanded) -> {
            tp.addPara("Filter trade routes by daily volume (%s tonnes)", pad, highlight,
                Integer.toString((int) minAmountSlider.getProgress())
            );
        };

        final int LIST_H = 200;
        final int SECT_II_H = SECT_I_H + LABEL_H + BTN_H*2 + opad*5;

        final LabelAPI exportersLbl = settings.createLabel("Exporters", Fonts.INSIGNIA_LARGE);
        add(exportersLbl).inTL(opad, SECT_II_H + opad);

        final LabelAPI importersLbl = settings.createLabel("Importers", Fonts.INSIGNIA_LARGE);
        final float halfW = (pos.getWidth() - opad*3f) * 0.5f;
        add(importersLbl).inTL(opad + halfW + opad, SECT_II_H + opad);

        final TooltipMakerAPI exportersContainer = ComponentFactory.createTooltip((int)halfW, true);
        final TooltipMakerAPI importersContainer = ComponentFactory.createTooltip((int)halfW, true);

        float yLeft = pad;
        float yRight = pad;

        for (FactionSpecAPI spec : visibleFactions) {
            final String factionName = spec.getDisplayName();
            final String factionId = spec.getId();

            final boolean initiallyAllowedExport = !ComTradeFlowMap.exporterFactionBlacklist.contains(factionId);
            final CheckboxButton cbExp = new CheckboxButton(m_panel, 20, factionName, Fonts.DEFAULT_SMALL, 
                (btn) -> {
                    btn.setChecked(!btn.isChecked());
                    if (btn.isChecked()) ComTradeFlowMap.exporterFactionBlacklist.remove(factionId);
                    else ComTradeFlowMap.exporterFactionBlacklist.add(factionId);
                    content.buildUI();
                }, UICheckboxSize.SMALL, false
            );
            cbExp.setChecked(initiallyAllowedExport);
            cbExp.setLabelColor(spec.getBaseUIColor());

            exportersContainer.addCustom(cbExp.getPanel(), 0).getPosition().inTL(pad, yLeft);
            yLeft += cbExp.getPanel().getPosition().getHeight() + pad;

            final boolean initiallyAllowedImport = !ComTradeFlowMap.importerFactionBlacklist.contains(factionId);
            final CheckboxButton cbImp = new CheckboxButton(m_panel, 20, factionName, Fonts.DEFAULT_SMALL,
                (btn) -> {
                    btn.setChecked(!btn.isChecked());
                    if (btn.isChecked()) ComTradeFlowMap.importerFactionBlacklist.remove(factionId);
                    else ComTradeFlowMap.importerFactionBlacklist.add(factionId);
                    content.buildUI();
                }, UICheckboxSize.SMALL, false
            );
            cbImp.setChecked(initiallyAllowedImport);
            cbImp.setLabelColor(spec.getBaseUIColor());

            importersContainer.addCustom(cbImp.getPanel(), 0).getPosition().inTL(pad, yRight);
            yRight += cbImp.getPanel().getPosition().getHeight() + pad;
        }

        exportersContainer.setHeightSoFar(yLeft);
        importersContainer.setHeightSoFar(yRight);

        ComponentFactory.addTooltip(exportersContainer, LIST_H, true, m_panel).inTL(opad, SECT_II_H + LABEL_H + S_BTN_H + opad*2);
        ComponentFactory.addTooltip(importersContainer, LIST_H, true, m_panel).inTL(opad + halfW + opad, SECT_II_H + LABEL_H + S_BTN_H + opad*2);

        final float btnW = 80;
        final Button enableAllExporters = new Button(m_panel, (int)btnW, S_BTN_H, "Enable All", Fonts.DEFAULT_SMALL, (b) -> {
            for (FactionSpecAPI s : visibleFactions) ComTradeFlowMap.exporterFactionBlacklist.remove(s.getId());
            content.buildUI();
            buildUI();
        });
        
        final Button disableAllExporters = new Button(m_panel, (int)btnW, S_BTN_H, "Disable All", Fonts.DEFAULT_SMALL, (b) -> {
            for (FactionSpecAPI s : visibleFactions) ComTradeFlowMap.exporterFactionBlacklist.add(s.getId());
            content.buildUI();
            buildUI();
        });
        
        final Button enableAllImporters = new Button(m_panel, (int)btnW, S_BTN_H, "Enable All", Fonts.DEFAULT_SMALL, (b) -> {
            for (FactionSpecAPI s : visibleFactions) ComTradeFlowMap.importerFactionBlacklist.remove(s.getId());
            content.buildUI();
            buildUI();
        });
        
        final Button disableAllImporters = new Button(m_panel, (int)btnW, S_BTN_H, "Disable All", Fonts.DEFAULT_SMALL, (b) -> {
            for (FactionSpecAPI s : visibleFactions) ComTradeFlowMap.importerFactionBlacklist.add(s.getId());
            content.buildUI();
            buildUI();
        });

        enableAllExporters.cutStyle = CutStyle.TL_BL;
        disableAllExporters.cutStyle = CutStyle.TR_BR;
        enableAllImporters.cutStyle = CutStyle.TL_BL;
        disableAllImporters.cutStyle = CutStyle.TR_BR;

        add(enableAllExporters).inTL(opad, SECT_II_H + LABEL_H + opad + pad*2);
        add(disableAllExporters).inTL(opad*1.5f + btnW, SECT_II_H + LABEL_H + opad + pad*2);
        add(enableAllImporters).inTL(opad*2 + halfW, SECT_II_H + LABEL_H + opad + pad*2);
        add(disableAllImporters).inTL(opad*2.5f + halfW + btnW, SECT_II_H + LABEL_H + opad + pad*2);
    }

    @Override
    public void advance(float delta) {
        super.advance(delta);

        if (minAmountSlider != null && minAmountSliderValueCache != minAmountSlider.getProgress()) {
            ComTradeFlowMap.minTradeAmount = minAmountSlider.getProgress();
            minAmountSliderValueCache = minAmountSlider.getProgress();
            content.buildUI();
        }
    }
}
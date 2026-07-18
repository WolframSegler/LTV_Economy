package wfg.ltv_econ.ui.factionTab;

import static wfg.ltv_econ.constant.strings.LocalizedStrings.*;
import static wfg.native_ui.util.UIConstants.*;

import java.awt.Color;
import java.util.List;

import org.lwjgl.input.Keyboard;

import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.ui.Fonts;
import com.fs.starfarer.api.ui.TextFieldAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;

import wfg.ltv_econ.ui.fleet.ShipFilters;
import wfg.native_ui.internal.ui.core.UIContainer;
import wfg.native_ui.ui.ComponentFactory;
import wfg.native_ui.ui.core.UIBuildableAPI;
import wfg.native_ui.ui.functional.Button;
import wfg.native_ui.ui.functional.Button.CutStyle;
import wfg.native_ui.util.NativeUiUtils;

public final class ShipFiltersPanel extends UIContainer {
    private static final String emptyNameFieldTxt = "Ctrl-F to search";
    private static final int btnH = 24;
    private static final Color nearBlack = new Color(20, 20, 25);

    private final UIBuildableAPI target;
    private final TextFieldAPI nameField;
    
    public ShipFiltersPanel(int w, UIBuildableAPI target) {
        super(w, btnH);

        this.target = target;

        final TooltipMakerAPI uiBuilder = ComponentFactory.createTooltip(w, false);
        final int btnW = 100;
        

        nameField = uiBuilder.addTextField(192, btnH, Fonts.DEFAULT_SMALL, pad);
        nameField.setText(ShipFilters.searchQuery);
        nameField.setMaxChars(30);
        nameField.setLimitByStringWidth(true);
        nameField.setUndoOnEscape(true);
        nameField.getPosition().inBL(hpad, 0f);

        float btnX = 200 + hpad*3;

        final Button civilianBtn = new Button(btnW, btnH, str("uiTitleCivilianShipType"), Fonts.DEFAULT_SMALL, (btn) -> {
            ShipFilters.showCivilian = !ShipFilters.showCivilian;
            btn.setChecked(ShipFilters.showCivilian);
            target.buildUI();
        });
        civilianBtn.setCutStyle(CutStyle.TL_BL);
        civilianBtn.setChecked(ShipFilters.showCivilian);
        add(civilianBtn).inBL(btnX, 0f);
        btnX += btnW + pad;

        final Button combatBtn = new Button(btnW, btnH, str("uiTitleCombatShipType"), Fonts.DEFAULT_SMALL, (btn) -> {
            ShipFilters.showCombat = !ShipFilters.showCombat;
            btn.setChecked(ShipFilters.showCombat);
            target.buildUI();
        });
        combatBtn.setChecked(ShipFilters.showCombat);
        add(combatBtn).inBL(btnX, 0f);
        btnX += btnW + pad;

        final Button idleBtn = new Button(btnW, btnH, str("uiTitleIdleOnlyShipType"), Fonts.DEFAULT_SMALL, (btn) -> {
            ShipFilters.showOnlyIdle = !ShipFilters.showOnlyIdle;
            btn.setChecked(ShipFilters.showOnlyIdle);
            target.buildUI();
        });
        idleBtn.setCutStyle(CutStyle.TR_BR);
        idleBtn.setChecked(ShipFilters.showOnlyIdle);
        add(idleBtn).inBL(btnX, 0f);
        btnX += btnW + opad*2;

        final Button frigateBtn = new Button(btnW, btnH, str("uiTitleFrigatesShipType"), Fonts.DEFAULT_SMALL, (btn) -> {
            ShipFilters.showFrigates = !ShipFilters.showFrigates;
            btn.setChecked(ShipFilters.showFrigates);
            target.buildUI();
        });
        frigateBtn.setCutStyle(CutStyle.TL_BL);
        frigateBtn.setChecked(ShipFilters.showFrigates);
        add(frigateBtn).inBL(btnX, 0f);
        btnX += btnW + pad;

        final Button destroyerBtn = new Button(btnW, btnH, str("uiTitleDestroyersShipType"), Fonts.DEFAULT_SMALL, (btn) -> {
            ShipFilters.showDestroyers = !ShipFilters.showDestroyers;
            btn.setChecked(ShipFilters.showDestroyers);
            target.buildUI();
        });
        destroyerBtn.setChecked(ShipFilters.showDestroyers);
        add(destroyerBtn).inBL(btnX, 0f);
        btnX += btnW + pad;

        final Button cruiserBtn = new Button(btnW, btnH, str("uiTitleCruisersShipType"), Fonts.DEFAULT_SMALL, (btn) -> {
            ShipFilters.showCruisers = !ShipFilters.showCruisers;
            btn.setChecked(ShipFilters.showCruisers);
            target.buildUI();
        });
        cruiserBtn.setChecked(ShipFilters.showCruisers);
        add(cruiserBtn).inBL(btnX, 0f);
        btnX += btnW + pad;

        final Button capitalBtn = new Button(btnW, btnH, str("uiTitleCapitalsShipType"), Fonts.DEFAULT_SMALL, (btn) -> {
            ShipFilters.showCapitals = !ShipFilters.showCapitals;
            btn.setChecked(ShipFilters.showCapitals);
            target.buildUI();
        });
        capitalBtn.setCutStyle(CutStyle.TR_BR);
        capitalBtn.setChecked(ShipFilters.showCapitals);
        add(capitalBtn).inBL(btnX, 0f);

        ComponentFactory.addTooltip(uiBuilder, btnH, false, this);

        civilianBtn.bgColor = nearBlack;
        combatBtn.bgColor = nearBlack;
        idleBtn.bgColor = nearBlack;
        frigateBtn.bgColor = nearBlack;
        destroyerBtn.bgColor = nearBlack;
        cruiserBtn.bgColor = nearBlack;
        capitalBtn.bgColor = nearBlack;

        civilianBtn.setHighlightBrightness(0f);
        combatBtn.setHighlightBrightness(0f);
        idleBtn.setHighlightBrightness(0f);
        frigateBtn.setHighlightBrightness(0f);
        destroyerBtn.setHighlightBrightness(0f);
        cruiserBtn.setHighlightBrightness(0f);
        capitalBtn.setHighlightBrightness(0f);
    }

    @Override
    public void advanceImpl(float delta) {
        super.advanceImpl(delta);

        if (nameField.hasFocus()) {
            final String current = nameField.getText();
            final boolean equalsEmptyFieldTxt = current.equals(emptyNameFieldTxt);

            if (!current.equals(ShipFilters.searchQuery) && !equalsEmptyFieldTxt) {
                ShipFilters.searchQuery = current;
                target.buildUI();
            } else if (equalsEmptyFieldTxt) {
                nameField.setText("");
                nameField.setColor(base);
            }
        } else {
            nameField.setColor(gray);
            if (nameField.getText().isBlank()) {
                nameField.setText(emptyNameFieldTxt);
            }
        }
    }

    @Override
    public void processInputImpl(List<InputEventAPI> events) {
        super.processInputImpl(events);

        for (InputEventAPI event : events) {
            if (event.isConsumed()) continue;
            if (event.isKeyDownEvent() && event.getEventValue() == Keyboard.KEY_F) {
                if (NativeUiUtils.isCtrlDown()) nameField.grabFocus(true);
            }
        }
    }
}
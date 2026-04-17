package wfg.ltv_econ.ui.factionTab;

import static wfg.native_ui.util.UIConstants.*;

import java.awt.Color;

import com.fs.starfarer.api.ui.Fonts;
import com.fs.starfarer.api.ui.TextFieldAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.ui.UIPanelAPI;

import wfg.ltv_econ.ui.fleet.ShipFilters;
import wfg.native_ui.ui.ComponentFactory;
import wfg.native_ui.ui.core.UIBuildableAPI;
import wfg.native_ui.ui.functional.Button;
import wfg.native_ui.ui.functional.Button.CutStyle;
import wfg.native_ui.ui.panel.CustomPanel;

public class ShipFiltersPanel extends CustomPanel {
    private static final int btnH = 24;
    private static final Color nearBlack = new Color(20, 20, 25);

    private final UIBuildableAPI target;
    private final TextFieldAPI nameField;
    
    public ShipFiltersPanel(UIPanelAPI parent, int w, UIBuildableAPI target) {
        super(parent, w, btnH);

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

        final Button civilianBtn = new Button(m_panel, btnW, btnH, "Civilian", Fonts.DEFAULT_SMALL, (btn) -> {
            ShipFilters.showCivilian = !ShipFilters.showCivilian;
            btn.setChecked(ShipFilters.showCivilian);
            target.buildUI();
        });
        civilianBtn.cutStyle = CutStyle.TL_BL;
        civilianBtn.setChecked(ShipFilters.showCivilian);
        add(civilianBtn.getPanel()).inBL(btnX, 0f);
        btnX += btnW + pad;

        final Button combatBtn = new Button(m_panel, btnW, btnH, "Combat", Fonts.DEFAULT_SMALL, (btn) -> {
            ShipFilters.showCombat = !ShipFilters.showCombat;
            btn.setChecked(ShipFilters.showCombat);
            target.buildUI();
        });
        combatBtn.setChecked(ShipFilters.showCombat);
        add(combatBtn.getPanel()).inBL(btnX, 0f);
        btnX += btnW + pad;

        final Button idleBtn = new Button(m_panel, btnW, btnH, "Idle Only", Fonts.DEFAULT_SMALL, (btn) -> {
            ShipFilters.showOnlyIdle = !ShipFilters.showOnlyIdle;
            btn.setChecked(ShipFilters.showOnlyIdle);
            target.buildUI();
        });
        idleBtn.cutStyle = CutStyle.TR_BR;
        idleBtn.setChecked(ShipFilters.showOnlyIdle);
        add(idleBtn.getPanel()).inBL(btnX, 0f);
        btnX += btnW + opad*2;

        final Button frigateBtn = new Button(m_panel, btnW, btnH, "Frigates", Fonts.DEFAULT_SMALL, (btn) -> {
            ShipFilters.showFrigates = !ShipFilters.showFrigates;
            btn.setChecked(ShipFilters.showFrigates);
            target.buildUI();
        });
        frigateBtn.cutStyle = CutStyle.TL_BL;
        frigateBtn.setChecked(ShipFilters.showFrigates);
        add(frigateBtn.getPanel()).inBL(btnX, 0f);
        btnX += btnW + pad;

        final Button destroyerBtn = new Button(m_panel, btnW, btnH, "Destroyers", Fonts.DEFAULT_SMALL, (btn) -> {
            ShipFilters.showDestroyers = !ShipFilters.showDestroyers;
            btn.setChecked(ShipFilters.showDestroyers);
            target.buildUI();
        });
        destroyerBtn.setChecked(ShipFilters.showDestroyers);
        add(destroyerBtn.getPanel()).inBL(btnX, 0f);
        btnX += btnW + pad;

        final Button cruiserBtn = new Button(m_panel, btnW, btnH, "Cruisers", Fonts.DEFAULT_SMALL, (btn) -> {
            ShipFilters.showCruisers = !ShipFilters.showCruisers;
            btn.setChecked(ShipFilters.showCruisers);
            target.buildUI();
        });
        cruiserBtn.setChecked(ShipFilters.showCruisers);
        add(cruiserBtn.getPanel()).inBL(btnX, 0f);
        btnX += btnW + pad;

        final Button capitalBtn = new Button(m_panel, btnW, btnH, "Capitals", Fonts.DEFAULT_SMALL, (btn) -> {
            ShipFilters.showCapitals = !ShipFilters.showCapitals;
            btn.setChecked(ShipFilters.showCapitals);
            target.buildUI();
        });
        capitalBtn.cutStyle = CutStyle.TR_BR;
        capitalBtn.setChecked(ShipFilters.showCapitals);
        add(capitalBtn.getPanel()).inBL(btnX, 0f);

        ComponentFactory.addTooltip(uiBuilder, btnH, false, m_panel);

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
    public void advance(float delta) {
        super.advance(delta);

        if (nameField.hasFocus()) {
            final String current = nameField.getText();
            if (!current.equals(ShipFilters.searchQuery)) {
                ShipFilters.searchQuery = current;
                target.buildUI();
            }
        }
    }
}
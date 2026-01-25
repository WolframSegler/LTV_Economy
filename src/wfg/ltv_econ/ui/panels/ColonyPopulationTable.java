package wfg.ltv_econ.ui.panels;

import static wfg.native_ui.util.UIConstants.pad;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.SettingsAPI;
import com.fs.starfarer.api.campaign.PlanetAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.ui.Alignment;
import com.fs.starfarer.api.ui.LabelAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.ui.UIPanelAPI;

import wfg.ltv_econ.economy.PlayerMarketData;
import wfg.ltv_econ.economy.engine.EconomyEngine;
import wfg.native_ui.ui.ComponentFactory;
import wfg.native_ui.ui.components.BackgroundComp;
import wfg.native_ui.ui.components.NativeComponents;
import wfg.native_ui.ui.core.UIElementFlags.HasBackground;
import wfg.native_ui.ui.panels.CustomPanel;
import wfg.native_ui.ui.panels.SortableTable;
import wfg.native_ui.ui.panels.SortableTable.cellAlg;
public class ColonyPopulationTable extends CustomPanel<ColonyPopulationTable> implements HasBackground {
    public static final int PANEL_W = 815;

    protected final BackgroundComp bg = comp().get(NativeComponents.BACKGROUND);

    public ColonyPopulationTable(UIPanelAPI parent, int height) {
        super(parent, PANEL_W, height);

        bg.alpha = 1f;

        createPanel();
    }

    public void createPanel() {
        final SettingsAPI settings = Global.getSettings();
        final int rowH = 65;
        final int nameW = 167;
        
        clearChildren();
        final SortableTable table = new SortableTable(m_panel, (int) pos.getWidth(),
            (int) pos.getHeight(), 18, rowH
        );

        table.addHeaders(
            "Name", nameW + pad, "Colony name.\nSorts colonies by date established.", false, false, -1,
            "Size", 80, "Colony size.", false, false, -1,
            "Health", 140, "Overall health of the colony's population.", false, false, -1,
            "Happiness", 140, "Overall happiness and morale of the colony's population.", false, false, -1,
            "Cohesion", 140, "Degree of social cohesion within the colony's population.", false, false, -1,
            "Consciousness", 140, "The colony population's awareness of exploitation and social hierarchy.", false, false, -1
        );

        for (PlayerMarketData data : EconomyEngine.getInstance().getPlayerMarketData().values()) {
            final UIPanelAPI namePanel = settings.createCustom(nameW, rowH, null);
            final TooltipMakerAPI nameTp = ComponentFactory.createTooltip(nameW, false);
            final SectorEntityToken entity = data.market.getPrimaryEntity();

            if (entity instanceof PlanetAPI) {
                nameTp.showPlanetInfo(data.market.getPlanetEntity(), nameW, rowH, true, 0f);
            } else {
                nameTp.addImage(entity.getCustomEntitySpec().getIconName(), nameW, rowH, 0f);
                final LabelAPI lbl = nameTp.addPara(data.market.getName(), 0f);
                lbl.autoSizeToWidth(nameW).inBL(0f, pad);
                lbl.setAlignment(Alignment.MID);
            }
            ComponentFactory.addTooltip(nameTp, rowH, false, namePanel);

            table.addCell(namePanel, cellAlg.LEFT, data.market.getDaysInExistence(), null);
            table.addCell(data.market.getSize(), cellAlg.MID, null, null);
            table.addCell((int) data.getHealth(), cellAlg.MID, null, null);
            table.addCell((int) data.getHappiness(), cellAlg.MID, null, null);
            table.addCell((int) data.getSocialCohesion(), cellAlg.MID, null, null);
            table.addCell((int) data.getClassConsciousness(), cellAlg.MID, null, null);

            table.pushRow(null, null, null, null, null, null);
        }

        table.outline.enabled = true;
        add(table).inTL(0f, 0f);
        table.sortRows(0, false);
    }
}
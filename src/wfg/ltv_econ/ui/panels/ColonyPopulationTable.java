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
import com.fs.starfarer.api.ui.TooltipMakerAPI.PlanetInfoParams;

import wfg.ltv_econ.economy.PlayerMarketData;
import wfg.ltv_econ.economy.engine.EconomyEngine;
import wfg.ltv_econ.ui.dialogs.ManagePopulationDialog;
import wfg.native_ui.ui.ComponentFactory;
import wfg.native_ui.ui.components.BackgroundComp;
import wfg.native_ui.ui.components.NativeComponents;
import wfg.native_ui.ui.core.UIElementFlags.HasBackground;
import wfg.native_ui.ui.panels.CustomPanel;
import wfg.native_ui.ui.panels.SortableTable;
import wfg.native_ui.ui.panels.SortableTable.cellAlg;
import wfg.native_ui.ui.panels.SpritePanel.Base;
public class ColonyPopulationTable extends CustomPanel<ColonyPopulationTable> implements HasBackground {
    public static final int PANEL_W = 935;

    protected final BackgroundComp bg = comp().get(NativeComponents.BACKGROUND);

    public ColonyPopulationTable(UIPanelAPI parent, int height) {
        super(parent, PANEL_W, height);

        bg.alpha = 1f;

        createPanel();
    }

    public void createPanel() {
        final SettingsAPI settings = Global.getSettings();
        final int rowH = 68;
        final int nameW = 170;
        
        clearChildren();
        final SortableTable table = new SortableTable(m_panel, (int) pos.getWidth(),
            (int) pos.getHeight(), 18, rowH
        );

        table.addHeaders(
            "Name", nameW + pad, "Colony name.\nSorts colonies by date established.", false, false, -1,
            "Size", 70, "Colony size.", false, false, -1,

            "Health Icon", 30, null, true, false, 1,
            "Health", 90, "Overall health of the colony's population.", true, true, 1,
            
            "Happiness Icon", 30, null, true, false, 2,
            "Happiness", 90, "Overall happiness and morale of the colony's population.", true, true, 2,

            "Cohesion Icon", 30, null, true, false, 3,
            "Cohesion", 90, "Degree of social cohesion within the colony's population.", true, true, 3,

            "Consciousness Icon", 30, null, true, false, 4,
            "Conscious..", 90, "The colony population's awareness of exploitation and social hierarchy.", true, true, 4
        );

        final PlanetInfoParams params = new PlanetInfoParams();
        params.showName = true;
        params.showConditions = false;
        params.showHazardRating = false;
        params.scaleEvenWhenShowingName = true;

        for (PlayerMarketData data : EconomyEngine.getInstance().getPlayerMarketData().values()) {
            final UIPanelAPI namePanel = settings.createCustom(nameW, rowH, null);
            final TooltipMakerAPI nameTp = ComponentFactory.createTooltip(nameW, false);
            final SectorEntityToken entity = data.market.getPrimaryEntity();

            if (entity instanceof PlanetAPI) {
                nameTp.showPlanetInfo(data.market.getPlanetEntity(), nameW, rowH, params, 0f);
            } else {
                nameTp.addImage(entity.getCustomEntitySpec().getIconName(), nameW, rowH, 0f);
                final LabelAPI lbl = nameTp.addPara(data.market.getName(), 0f);
                lbl.autoSizeToWidth(nameW).inBL(0f, pad);
                lbl.setAlignment(Alignment.MID);
            }
            ComponentFactory.addTooltip(nameTp, rowH, false, namePanel);

            final int iconS = rowH/3;
            final Base health = new Base(table.getPanel(), iconS, iconS, ManagePopulationDialog.HEALTH_ICON, null, null);
            final Base happiness = new Base(table.getPanel(), iconS, iconS, ManagePopulationDialog.SMILING_ICON, null, null);
            final Base cohesion = new Base(table.getPanel(), iconS, iconS, ManagePopulationDialog.SOCIETY_ICON, null, null);
            final Base consciousness = new Base(table.getPanel(), iconS, iconS, ManagePopulationDialog.SOLIDARITY_ICON, null, null);

            table.addCell(namePanel, cellAlg.LEFT, data.market.getDaysInExistence(), null);
            table.addCell(data.market.getSize(), cellAlg.MID, null, null);
            table.addCell(health, cellAlg.LEFTOPAD, null, null);
            table.addCell((int) data.getHealth(), cellAlg.MID, null, null);
            table.addCell(happiness, cellAlg.LEFTOPAD, null, null);
            table.addCell((int) data.getHappiness(), cellAlg.MID, null, null);
            table.addCell(cohesion, cellAlg.LEFTOPAD, null, null);
            table.addCell((int) data.getSocialCohesion(), cellAlg.MID, null, null);
            table.addCell(consciousness, cellAlg.LEFTOPAD, null, null);
            table.addCell((int) data.getClassConsciousness(), cellAlg.MID, null, null);

            table.pushRow(null, null, null, null, null, null);
        }

        table.outline.enabled = true;
        add(table).inTL(0f, 0f);
        table.sortRows(0, false);
    }
}
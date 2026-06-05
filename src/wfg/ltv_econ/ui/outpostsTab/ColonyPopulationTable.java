package wfg.ltv_econ.ui.outpostsTab;

import static wfg.native_ui.util.Globals.settings;
import static wfg.native_ui.util.UIConstants.*;
import static wfg.ltv_econ.constants.strings.LocalizedStrings.*;

import java.awt.Color;

import com.fs.starfarer.api.campaign.PlanetAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.ui.Alignment;
import com.fs.starfarer.api.ui.Fonts;
import com.fs.starfarer.api.ui.LabelAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.ui.UIPanelAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI.PlanetInfoParams;

import wfg.ltv_econ.conditions.WorkerPoolCondition;
import wfg.ltv_econ.economy.PlayerMarketData;
import wfg.ltv_econ.economy.engine.EconomyEngine;
import wfg.ltv_econ.ui.marketInfo.dialogs.ManagePopulationDialog;
import wfg.ltv_econ.ui.marketInfo.population.CohesionPair;
import wfg.ltv_econ.ui.marketInfo.population.ConsciousnessPair;
import wfg.ltv_econ.ui.marketInfo.population.HappinessPair;
import wfg.ltv_econ.ui.marketInfo.population.HealthPair;
import wfg.native_ui.ui.ComponentFactory;
import wfg.native_ui.ui.component.BackgroundComp;
import wfg.native_ui.ui.component.NativeComponents;
import wfg.native_ui.ui.component.InteractionComp.ClickHandler;
import wfg.native_ui.ui.core.UIElementFlags.HasBackground;
import wfg.native_ui.ui.panel.CustomPanel;
import wfg.native_ui.ui.table.SortableTable;
import wfg.native_ui.ui.table.SortableTable.TableRow;
import wfg.native_ui.ui.table.SortableTable.cellAlg;
import wfg.native_ui.util.NumFormat;
public class ColonyPopulationTable extends CustomPanel implements HasBackground {
    static final int PANEL_W = 935;

    private static final PlanetInfoParams params = new PlanetInfoParams();
    static {
        params.showName = true;
        params.showConditions = false;
        params.showHazardRating = false;
        params.scaleEvenWhenShowingName = true;
    }

    private final BackgroundComp bg = comp().get(NativeComponents.BACKGROUND);

    public ColonyPopulationTable(UIPanelAPI parent, int height) {
        super(parent, PANEL_W, height);

        bg.alpha = 1f;

        buildUI();
    }

    public void buildUI() {
        final EconomyEngine engine = EconomyEngine.instance();
        final int rowH = 68;
        final int nameW = 170;
        final int iconW = 120;
        
        clearChildren();
        final SortableTable table = new SortableTable(m_panel, (int) pos.getWidth(),
            (int) pos.getHeight(), 18, rowH
        );

        table.addHeaders(
            str("uiTableColonyNameTitle"), nameW + pad, null, false, false, -1,
            str("uiTableSize"), 70, null, false, false, -1,
            str("marketPopDataHealthTxt"), iconW, str("uiHealthTpTxtShort"), false, false, -1,
            str("marketPopDataHappinessTxt"), iconW, str("uiHappinessTpTxtShort"), false, false, -1,
            str("marketPopDataCohesionShortTxt"), iconW, str("uiCohesionTpTxtShort"), false, false, -1,
            str("marketPopDataConsciousnessShort"), iconW, str("uiClassConsciousnessTpTxtShort"), false, false, -1,
            str("uiTableCreditReserves"), 100, str("uiTableCreditReservesTpTxt"), false, false, -1,
            str("uiTableEmployment"), 112, str("uiTableEmploymentTpTxt"), false, false, -1
        );

        if (engine.getMarketPopulationData().size() > 0) {
            for (PlayerMarketData data : engine.getMarketPopulationData().values()) {
                final UIPanelAPI namePanel = settings.createCustom(nameW, rowH, null);
                final TooltipMakerAPI nameTp = ComponentFactory.createTooltip(nameW, false);
                final MarketAPI market = data.market;
                final SectorEntityToken entity = market.getPrimaryEntity();
    
                if (entity instanceof PlanetAPI) {
                    nameTp.showPlanetInfo(market.getPlanetEntity(), nameW, rowH, params, 0f);
                } else {
                    nameTp.addImage(entity.getCustomEntitySpec().getIconName(), nameW, rowH, 0f);
                    final LabelAPI lbl = nameTp.addPara(market.getName(), 0f);
                    lbl.autoSizeToWidth(nameW).inBL(0f, pad);
                    lbl.setAlignment(Alignment.MID);
                }
                ComponentFactory.addTooltip(nameTp, rowH, false, namePanel);
    
                final int iconS = rowH/3;
                final int PairW = iconW - opad;
                final HealthPair healthPair = new HealthPair(m_panel, PairW, iconS, data, base, null);
                final HappinessPair happinessPair = new HappinessPair(m_panel, PairW, iconS, data, base, null);
                final CohesionPair cohesionPair = new CohesionPair(m_panel, PairW, iconS, data, base, null);
                final ConsciousnessPair consciousnessPair = new ConsciousnessPair(m_panel, PairW, iconS, data, base, null);

                final var cond = WorkerPoolCondition.getPoolCondition(market);
                final int employment = Math.round(100f - cond.getFreeWorkerRatio()*100f);

                final long credits = engine.getCredits(data.marketID);
                final Color creditColor = credits < 0l ? negative : highlight;

                table.addCell(namePanel, cellAlg.LEFT, market.getDaysInExistence(), null);
                table.addCell(market.getSize(), cellAlg.MID, null, null);
                table.addCell(healthPair.getPanel(), cellAlg.LEFTOPAD, data.getHealth(), null);
                table.addCell(happinessPair.getPanel(), cellAlg.LEFTOPAD, data.getHappiness(), null);
                table.addCell(cohesionPair.getPanel(), cellAlg.LEFTOPAD, data.getSocialCohesion(), null);
                table.addCell(consciousnessPair.getPanel(), cellAlg.LEFTOPAD, data.getClassConsciousness(), null);
                table.addCell(NumFormat.formatCredit(credits), cellAlg.MID, credits, creditColor);
                table.addCell(employment + "%", cellAlg.MID, employment, null);

                final ClickHandler<TableRow> run = (row, isLeftClick) -> {
                    final ManagePopulationDialog dialogPanel = new ManagePopulationDialog(market);
                    dialogPanel.show(0.3f, 0.3f);
                };
    
                table.pushRow(null, null, run, null, null, null);
            }
        } else {
            final LabelAPI lbl = settings.createLabel(str("uiNoStaticAssets"), Fonts.DEFAULT_SMALL);
            lbl.setColor(base);
            table.add(lbl).inMid();
        }

        table.outline.enabled = true;
        add(table).inTL(0f, 0f);
        table.sortRows(0, false);
    }
}
package wfg.ltv_econ.ui.outpostsTab;

import static wfg.ltv_econ.constant.strings.LocalizedStrings.*;
import static wfg.native_ui.util.Globals.settings;
import static wfg.native_ui.util.UIConstants.*;

import java.awt.Color;

import com.fs.starfarer.api.campaign.PlanetAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.ui.Alignment;
import com.fs.starfarer.api.ui.Fonts;
import com.fs.starfarer.api.ui.LabelAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI.PlanetInfoParams;

import wfg.ltv_econ.economy.engine.EconomyEngine;
import wfg.ltv_econ.economy.market.MarketPopulationData;
import wfg.ltv_econ.economy.registry.WorkerPoolRegistry;
import wfg.ltv_econ.ui.marketInfo.dialogs.ManagePopulationDialog;
import wfg.ltv_econ.ui.marketInfo.population.CohesionPair;
import wfg.ltv_econ.ui.marketInfo.population.ConsciousnessPair;
import wfg.ltv_econ.ui.marketInfo.population.HappinessPair;
import wfg.ltv_econ.ui.marketInfo.population.HealthPair;
import wfg.native_ui.internal.ui.core.UIContainer;
import wfg.native_ui.ui.ComponentFactory;
import wfg.native_ui.ui.component.BackgroundComp;
import wfg.native_ui.ui.component.NativeComponents;
import wfg.native_ui.ui.core.UIContainerAPI;
import wfg.native_ui.ui.core.UIElementFlags.HasBackground;
import wfg.native_ui.ui.table.SortableTable;
import wfg.native_ui.ui.table.SortableTable.CellAlg;
import wfg.native_ui.ui.table.SortableTable.ColumnData;
import wfg.native_ui.ui.table.SortableTable.RowData;
import wfg.native_ui.util.NumFormat;

public final class ColonyPopulationTable extends UIContainer implements HasBackground {
    static final int PANEL_W = 935;

    private static final PlanetInfoParams params = new PlanetInfoParams() {
        {
            showName = true;
            showConditions = false;
            showHazardRating = false;
            scaleEvenWhenShowingName = true;
        }
    };

    private final BackgroundComp bg = comp().get(NativeComponents.BACKGROUND);

    public ColonyPopulationTable(int height) {
        super(PANEL_W, height);

        bg.alpha = 1f;

        buildUI();
    }

    public void buildUI() {
        final EconomyEngine engine = EconomyEngine.instance();
        final int rowH = 68;
        final int nameW = 170;
        final int iconW = 120;
        
        clearChildren();
        final SortableTable table = new SortableTable(getWidth(), getHeight(), 18, rowH) {
            @Override
            protected String getEmptyMessage() {
                return str("uiNoStaticAssets");
            }
        };

        table.addHeaders(
            new ColumnData(str("uiTableColonyNameTitle"), nameW + pad, null, true, false, false, -1),
            new ColumnData(str("uiTableSize"), 70, null, true, false, false, -1),
            new ColumnData(str("marketPopDataHealthTxt"), iconW, str("uiTpTxtShortHealth"), true, false, false, -1),
            new ColumnData(str("marketPopDataHappinessTxt"), iconW, str("uiTpTxtShortHappiness"), true, false, false, -1),
            new ColumnData(str("marketPopDataCohesionShortTxt"), iconW, str("uiTpTxtShortCohesion"), true, false, false, -1),
            new ColumnData(str("marketPopDataConsciousnessShort"), iconW, str("uiTpTxtShortClassConsciousness"), true, false, false, -1),
            new ColumnData(str("uiTableCreditReserves"), 100, str("uiTableTpTxtCreditReserves"), true, false, false, -1),
            new ColumnData(str("uiTableEmployment"), 112, str("uiTableTpTxtEmployment"), true, false, false, -1)
        );

        for (MarketPopulationData data : engine.getMarketPopulationData().values()) {
            final UIContainerAPI colonyPanel = new UIContainer(nameW, rowH);
            final MarketAPI market = data.market;

            colonyPanel.add(getPlanetInfoContainer(market, nameW, rowH));

            final int iconS = rowH/3;
            final int PairW = iconW - opad;
            final HealthPair healthPair = new HealthPair(PairW, iconS, data, base, null);
            final HappinessPair happinessPair = new HappinessPair(PairW, iconS, data, base, null);
            final CohesionPair cohesionPair = new CohesionPair(PairW, iconS, data, base, null);
            final ConsciousnessPair consciousnessPair = new ConsciousnessPair(PairW, iconS, data, base, null);

            final var cond = WorkerPoolRegistry.get(market);
            final int employment = Math.round(100f - cond.getFreeWorkerRatio()*100f);

            final long credits = engine.getCredits(data.marketID);
            final Color creditColor = credits < 0l ? negative : highlight;

            table.addRow(new RowData()
                .cell(colonyPanel, CellAlg.LEFT, market.getDaysInExistence(), null)
                .cell(market.getSize(), CellAlg.MID, null, null)
                .cell(healthPair, CellAlg.LEFTOPAD, data.getHealth(), null)
                .cell(happinessPair, CellAlg.LEFTOPAD, data.getHappiness(), null)
                .cell(cohesionPair, CellAlg.LEFTOPAD, data.getSocialCohesion(), null)
                .cell(consciousnessPair, CellAlg.LEFTOPAD, data.getClassConsciousness(), null)
                .cell(NumFormat.formatCredit(credits), CellAlg.MID, credits, creditColor)
                .cell(employment + "%", CellAlg.MID, employment, null)
                .onClicked((row, isLeftClick) -> new ManagePopulationDialog(market).show(0.3f, 0.3f))
            );
        }

        table.outline.enabled = true;
        add(table).inTL(0f, 0f);
        table.sortRows(0, false);
    }

    public static final TooltipMakerAPI getPlanetInfoContainer(final MarketAPI market, float width, float height) {
        final TooltipMakerAPI nameTp = ComponentFactory.createTooltip(width, false);
        final SectorEntityToken entity = market.getPrimaryEntity();

        if (entity instanceof PlanetAPI planet) {
            nameTp.showPlanetInfo(planet, width, height, params, 0f);
            nameTp.getPrev().getPosition().inTMid(0f);
        } else {
            nameTp.setParaFont(Fonts.ORBITRON_20AABOLD);
            final LabelAPI lbl = nameTp.addPara(market.getName(), 0f);
            lbl.autoSizeToWidth(width).inBMid(pad);
            lbl.setAlignment(Alignment.MID);
            lbl.setColor(settings.getColor("textNeutralColor")); // standardUIIconColor
            final float lblH = lbl.getPosition().getHeight();

            final float spriteH = Math.max(0f, height - lblH - pad*2);
            final SpriteAPI sprite = settings.getSprite(entity.getCustomEntitySpec().getSpriteName());
            final float sh = sprite.getHeight();
            final float sw = sprite.getWidth();
            final float fitHeight = Math.min(spriteH, sh * width / sw);

            nameTp.addImage(entity.getCustomEntitySpec().getSpriteName(), width, fitHeight, 0f);
            nameTp.getPrev().getPosition().inTMid(0f);
        }

        return nameTp;
    }
}
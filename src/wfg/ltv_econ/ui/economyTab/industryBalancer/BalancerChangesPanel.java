package wfg.ltv_econ.ui.economyTab.industryBalancer;

import static wfg.ltv_econ.constant.strings.LocalizedStrings.str;
import static wfg.native_ui.util.Globals.settings;
import static wfg.native_ui.util.UIConstants.*;

import org.lwjgl.opengl.GL11;
import java.awt.Color;
import java.util.List;

import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.impl.codex.CodexDataV2;
import com.fs.starfarer.api.loading.IndustrySpecAPI;
import com.fs.starfarer.api.ui.Fonts;
import com.fs.starfarer.api.ui.UIComponentAPI;

import wfg.ltv_econ.economy.planning.IndustryBalancer.BalancerState;
import wfg.ltv_econ.economy.planning.IndustryBalancer.IndustryChange;
import wfg.ltv_econ.ui.outpostsTab.ColonyPopulationTable;
import wfg.native_ui.internal.ui.core.UIContainer;
import wfg.native_ui.internal.ui.core.UIElement;
import wfg.native_ui.ui.core.UIBuildableAPI;
import wfg.native_ui.ui.core.UIContainerAPI;
import wfg.native_ui.ui.table.SortableTable;
import wfg.native_ui.ui.table.SortableTable.CellAlg;
import wfg.native_ui.ui.visual.AbstractSpriteElement.SpriteElement;
import wfg.native_ui.ui.widget.Button;
import wfg.native_ui.ui.widget.Button.CutStyle;

public final class BalancerChangesPanel extends UIContainer implements UIBuildableAPI {
    private static final Color ADDED_IND_COLOR = new Color(115, 217, 115);
    private static final Color REMOVED_IND_COLOR = new Color(230, 115, 115);
    private static final Color UPGRADED_IND_COLOR = new Color(115, 191, 255);

    private final IndustryBalancerHolo mHolo;
    private final BalancerState mState;

    public BalancerChangesPanel(IndustryBalancerHolo holo, BalancerState state) {
        super(IndustryBalancerHolo.CONTENT_PANEL_W, IndustryBalancerHolo.CONTENT_PANEL_H);
        mHolo = holo;
        mState = state;

        buildUI();
    }

    @Override
    public void buildUI() {
        clearChildren();

        final int panelW = IndustryBalancerHolo.CONTENT_PANEL_W;
        final int panelH = IndustryBalancerHolo.CONTENT_PANEL_H;

        final int tableW = panelW - 20;
        final int tableH = panelH - 55;
        final int headerH = 26;
        final int rowH = 75;

        final int indW = (int) (tableW * 0.25f) - pad;
        final int actionW = (int) (tableW * 0.3f) - pad;
        final int nameW = (int) (tableW * 0.45f);

        final SortableTable table = new SortableTable(
            tableW, tableH, headerH, rowH
        );

        table.addHeaders(
            str("uiTableIndustryTitle"), indW + pad, null, false, false, -1,
            str("uiTableIndustryBalancerMove"), actionW + pad, null, false, false, -1,
            str("uiTableColony"), nameW, null, false, false, -1
        );

        final List<IndustryChange> changes = mState.changes();
        for (int changeIdx = 0; changeIdx < changes.size(); changeIdx++) {
            final IndustryChange change = changes.get(changeIdx);

            final UIContainerAPI colonyPanel = new UIContainer(nameW, rowH);
            final MarketAPI market = change.market();
            final IndustrySpecAPI sourceSpec = settings.getIndustrySpec(change.sourceIndustryId());
            final IndustrySpecAPI targetSpec = settings.getIndustrySpec(change.targetIndustryId());

            colonyPanel.add(ColonyPopulationTable.getPlanetInfoContainer(market, nameW, rowH));

            final UIComponentAPI spriteRenderer = (sourceSpec != null && targetSpec != null) ?
                new DoubleSpriteRenderer(sourceSpec.getImageName(), targetSpec.getImageName()) :
                new SpriteElement(0f, 0f, sourceSpec != null ? sourceSpec.getImageName() : targetSpec.getImageName(), null, null);
            spriteRenderer.getPosition().setSize(indW, rowH);

            final String actionStr = str((sourceSpec != null && targetSpec != null) ?
                "uiDialogTxtIndustryBalancerIndustryUpgraded" :
                sourceSpec != null ? "uiDialogTxtIndustryBalancerIndustryRemoved" :
                "uiDialogTxtIndustryBalancerIndustryAdded"
            );
            final Color actionColor = (sourceSpec != null && targetSpec != null) ?
                UPGRADED_IND_COLOR : sourceSpec != null ? REMOVED_IND_COLOR : ADDED_IND_COLOR;

            table.addCell(spriteRenderer, CellAlg.MID, changeIdx, null);
            table.addCell(actionStr, CellAlg.MID, actionStr, actionColor);
            table.addCell(colonyPanel, CellAlg.MID, market.getName(), null);

            table.pushRow(
                null, null, null, CodexDataV2.getIndustryEntryId(change.targetIndustryId()), null, null
            );
        }

        add(table).inTL((panelW - tableW) / 2, opad);

        table.sortRows(0);

        final Button dismissBtn = new Button(BUTTON_W, BUTTON_H, str("uiDismiss"), Fonts.ORBITRON_20AA, (btn) -> {
            mHolo.dismiss();
        });
        dismissBtn.setCutStyle(CutStyle.TL_BR);
        add(dismissBtn).inBR(hpad, hpad);
    }

    private static final class DoubleSpriteRenderer extends UIElement {

        private final SpriteAPI mFrom;
        private final SpriteAPI mTo;

        private DoubleSpriteRenderer(String from, String to) {
            super();
            mFrom = settings.getSprite(from);
            mTo = settings.getSprite(to);
        }

        @Override
        public void renderImpl(float alpha) {
            final float x = getX();
            final float y = getY();
            final float w = getWidth();
            final float h = getHeight();

            mFrom.setAlphaMult(alpha);
            mTo.setAlphaMult(alpha);
            mFrom.setSize(w, h);
            mTo.setSize(w, h);
    
            GL11.glEnable(GL11.GL_STENCIL_TEST);
    
            GL11.glColorMask(false, false, false, false);
            GL11.glStencilFunc(GL11.GL_ALWAYS, 1, 0xFF);
            GL11.glStencilOp(GL11.GL_KEEP, GL11.GL_KEEP, GL11.GL_REPLACE);
            stampTriangle(x, y, x, y + h, x + w, y + h);
            GL11.glColorMask(true, true, true, true);
    
            GL11.glStencilFunc(GL11.GL_EQUAL, 1, 0xFF);
            GL11.glStencilOp(GL11.GL_KEEP, GL11.GL_KEEP, GL11.GL_KEEP);
            mFrom.render(x, y);
    
            GL11.glStencilFunc(GL11.GL_NOTEQUAL, 1, 0xFF);
            mTo.render(x, y);
    
            GL11.glColorMask(false, false, false, false);
            GL11.glStencilFunc(GL11.GL_ALWAYS, 0, 0xFF);
            GL11.glStencilOp(GL11.GL_KEEP, GL11.GL_KEEP, GL11.GL_REPLACE);
            stampTriangle(x, y, x, y + h, x + w, y + h);
            GL11.glColorMask(true, true, true, true);

            GL11.glStencilFunc(GL11.GL_ALWAYS, 0, 0xFF);
            GL11.glStencilOp(GL11.GL_KEEP, GL11.GL_KEEP, GL11.GL_KEEP);
            GL11.glDisable(GL11.GL_STENCIL_TEST);
        }
    
        private static final void stampTriangle(float x1, float y1, float x2, float y2, float x3, float y3) {
            GL11.glBegin(GL11.GL_TRIANGLES);
            GL11.glVertex2f(x1, y1);
            GL11.glVertex2f(x2, y2);
            GL11.glVertex2f(x3, y3);
            GL11.glEnd();
        }
    }
}
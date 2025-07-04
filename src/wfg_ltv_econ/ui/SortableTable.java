package wfg_ltv_econ.ui;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import java.awt.Color;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.ui.Alignment;
import com.fs.starfarer.api.ui.CustomPanelAPI;
import com.fs.starfarer.api.ui.Fonts;
import com.fs.starfarer.api.ui.LabelAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.ui.UIComponentAPI;
import com.fs.starfarer.api.ui.UIPanelAPI;

import wfg_ltv_econ.plugins.LtvCustomPanelPlugin;
import wfg_ltv_econ.plugins.LtvCustomPanelPlugin.GlowType;
import wfg_ltv_econ.util.TooltipUtils;
import wfg_ltv_econ.plugins.LtvSpritePanelPlugin;

/**
 * Supports the following types:
 * String, LabelAPI, LtvSpritePanel
 */
public class SortableTable extends LtvCustomPanel {
    private final List<ColumnManager> m_columns = new ArrayList<>();
    private final List<RowManager> m_rows = new ArrayList<>();

    private RowManager rowInStack = null;

    private final int m_headerHeight;
    private final int m_rowHeight;

    private int prevSelectedSortColumnIndex = -1;
    private int selectedSortColumnIndex = -1;
    private boolean ascending = true;

    RowSelectionListener selectionListener;
    public interface RowSelectionListener {
        void onRowSelected(RowManager selectedRow);
    }

    public void setRowSelectionListener(RowSelectionListener a) {
        selectionListener = a;
    }

    private RowManager m_selectedRow;

    public final static int pad = 3;
    public final static int opad = 10;

    public SortableTable(UIPanelAPI root, UIPanelAPI parent, int width, int height, MarketAPI market) {
        this(root, parent, width, height, market, 40, 20);
    }

    public SortableTable(UIPanelAPI root, UIPanelAPI parent, int width, int height, MarketAPI market,
            int headerHeight, int rowHeight) {
        super(root, parent, width, height, null, market);
        m_headerHeight = headerHeight;
        m_rowHeight = rowHeight;
    }

    public void initializePlugin(boolean hasPlugin) {
    }

    public void createPanel() {
        // Create headers
        int cumulativeXOffset = 0;
        for (ColumnManager column : m_columns) {

            LtvCustomPanel columnPanel = null;
            if (column.tooltipText == null) {
                columnPanel = new HeaderPanel(
                        getRoot(), getPanel(), column.width, m_headerHeight, m_market, column);
            } else {
                columnPanel = new HeaderPanelWithTooltip(
                        getRoot(), getPanel(), column.width, m_headerHeight, m_market, column);
            }

            getPanel().addComponent(columnPanel.getPanel()).inTL(
                    cumulativeXOffset, pad);

            cumulativeXOffset += column.width + pad;
        }

        // Create rows
        TooltipMakerAPI rowContainer = getPanel().createUIElement(
                getPanelPos().getWidth(),
                getPanelPos().getHeight() - m_headerHeight - pad,
                true);

        int cumulativeYOffset = 0;
        for (RowManager row : m_rows) {
            rowContainer.addComponent(row.getPanel()).inTL(
                    0, pad + cumulativeYOffset);

            cumulativeYOffset += pad + m_rowHeight;
        }
    }

    private class HeaderPanel extends LtvCustomPanel {
        protected final ColumnManager column;

        public HeaderPanel(UIPanelAPI root, UIPanelAPI parent, int width, int height, MarketAPI market,
                ColumnManager column) {
            super(root, parent, width, height, new LtvCustomPanelPlugin(), market);
            this.column = column;
        }

        @Override
        public void initializePlugin(boolean hasPlugin) {
            getPlugin().init(this, GlowType.OVERLAY, false, true, true);
        }

        @Override
        public void createPanel() {
            TooltipMakerAPI tooltip = getPanel().createUIElement(
                    getPanelPos().getWidth(), getPanelPos().getHeight(), false);

            LabelAPI lbl = tooltip.addPara(column.title, pad);
            final int lblWidth = (int) lbl.computeTextWidth(lbl.getText());

            lbl.getPosition().inBL((getPanelPos().getWidth() / 2f) - (lblWidth / 2f), pad);

            LtvSpritePanel sortIcon = new LtvSpritePanel(
                    getRoot(),
                    getPanel(),
                    m_market,
                    m_headerHeight - pad, m_headerHeight - pad,
                    new LtvSpritePanelPlugin(),
                    ColumnManager.sortIconPath,
                    getFaction().getBaseUIColor(),
                    Color.WHITE,
                    false);
            sortIcon.getPlugin().setGlowType(GlowType.NONE);

            tooltip.addComponent(sortIcon.getPanel()).inBR(pad, pad);

            getPanel().addUIElement(tooltip).inBL(0, 0);
        }
    }

    private class HeaderPanelWithTooltip extends HeaderPanel implements LtvCustomPanel.TooltipProvider {
        public HeaderPanelWithTooltip(UIPanelAPI root, UIPanelAPI parent, int width, int height,
                MarketAPI market, ColumnManager column) {
            super(root, parent, width, height, market, column);
        }

        public TooltipMakerAPI createTooltip() {
            TooltipMakerAPI tooltip = ((CustomPanelAPI) getParent()).createUIElement(
                    getPanelPos().getWidth(), getPanelPos().getHeight(), false);

            tooltip.addPara(column.tooltipText, pad);

            ((CustomPanelAPI) getParent()).addUIElement(tooltip);
            ((CustomPanelAPI) getParent()).bringComponentToTop(tooltip);

            return tooltip;
        }

        public void removeTooltip(TooltipMakerAPI tooltip) {
            ((CustomPanelAPI) getParent()).removeComponent(tooltip);
        }

        public void attachCodexTooltip(TooltipMakerAPI codex) {
        }
    }

    public List<ColumnManager> getColumns() {
        return m_columns;
    }

    public List<RowManager> getRows() {
        return m_rows;
    }

    public RowManager getM_selectedRow() {
        return m_selectedRow;
    }

    private class ColumnManager {
        public static String sortIconPath;
        {
            sortIconPath = Global.getSettings().getSpriteName("ui", "sortIcon");
        }

        public String title;
        public int width;
        public String tooltipText;

        public ColumnManager(String title, int width, String tooltipText) {
            this.title = title;
            this.width = width;
            this.tooltipText = tooltipText;
        }
    }

    private class RowManager extends LtvCustomPanel implements LtvCustomPanel.TooltipProvider {
        protected final List<Object> m_cellData = new ArrayList<>();
        protected final List<Alignment> m_cellAlg = new ArrayList<>();
        protected String codexID = null;

        public RowManager(UIPanelAPI root, UIPanelAPI parent, int width, int height, MarketAPI market,
            RowSelectionListener listener) {
            super(root, parent, width, height, new LtvCustomPanelPlugin() {
                @Override
                public void advance(float amount) {
                    super.advance(amount);

                    RowManager panel = (RowManager) m_panel;

                    if (LMBDownLastFrame) {
                        setPersistentGlow(!persistentGlow);
                        panel.getParentWrapper().selectRow(panel);

                        if (panel.getParentWrapper().selectionListener != null) {
                            panel.getParentWrapper().selectionListener.onRowSelected(panel);
                        }
                    }
                }
            }, market);
        }

        public void initializePlugin(boolean hasPlugin) {
            getPlugin().init(this, GlowType.OVERLAY, true, false, false);
        }

        public SortableTable getParentWrapper() {
            return (SortableTable)m_parent;
        }

        public void createPanel() {
            int cumulativeXOffset = 0;
            for (int i = 0; i < m_cellData.size(); i++) {
                Object cell = m_cellData.get(i);
                Alignment alignment = m_cellAlg.get(i);
                float colWidth = getColumns().get(i).width;

                UIComponentAPI comp;
                float compWidth;

                if (cell instanceof String) {
                    LabelAPI label = Global.getSettings().createLabel((String) cell, Fonts.DEFAULT_SMALL);
                    comp = (UIComponentAPI) label;
                    compWidth = label.computeTextWidth(label.getText());
                } else if (cell instanceof LtvSpritePanel) {
                    comp = (UIComponentAPI) cell;
                    compWidth = ((LtvSpritePanel) cell).getPanelPos().getWidth();
                } else if (cell instanceof LabelAPI) {
                    LabelAPI label = (LabelAPI) cell;
                    comp = (UIComponentAPI) label;
                    compWidth = label.computeTextWidth(label.getText());
                } else {
                    throw new IllegalArgumentException("Unsupported cell type: " + cell.getClass());
                }

                float xOffset = calcXOffset(cumulativeXOffset, colWidth, compWidth, alignment);
                getPanel().addComponent(comp).inBL(xOffset, 0f);

                cumulativeXOffset += colWidth;
            }
        }

        private float calcXOffset(float baseX, float colWidth, float compWidth, Alignment alignment) {
            final int pad = 3; // define pad somewhere appropriate

            switch (alignment) {
                case BL, TL, LMID:
                    return baseX;
                case BR, TR, RMID:
                    return baseX + colWidth - compWidth - pad;
                default:
                    return baseX + (colWidth / 2f) - (compWidth / 2f);
            }
        }

        public Object getSortValue(int columnIndex) {
            return m_cellData.get(columnIndex);
        }

        public void setCodexId(String codex) {
            codexID = codex;
        }

        public TooltipMakerAPI createTooltip() {
            TooltipMakerAPI tooltip = ((CustomPanelAPI) getParent()).createUIElement(
                    getPanelPos().getWidth(), getPanelPos().getHeight(), false);

            ((CustomPanelAPI) getParent()).addUIElement(tooltip);
            ((CustomPanelAPI) getParent()).bringComponentToTop(tooltip);
            TooltipUtils.mouseCornerPos(tooltip, opad);

            if (codexID != null) {
                tooltip.setCodexEntryId(codexID);
            }

            return tooltip;
        }

        public void removeTooltip(TooltipMakerAPI tooltip) {
            ((CustomPanelAPI) getParent()).removeComponent(tooltip);
        }

        public void attachCodexTooltip(TooltipMakerAPI codex) {
        }

        public void addCell(Object cell, Alignment alg) {
            m_cellData.add(cell);
            m_cellAlg.add(alg);
        }
    }

    /**
     * Each set must contain the title of the header, its width and the text of the
     * tooltip
     * The expected input is {String, int, String}. The tooltip can be left empty:
     * {String, int, null}.
     */
    public void addHeaders(Object... headerDatas) {
        m_columns.clear();
        if (headerDatas.length % 3 != 0) {
            throw new IllegalArgumentException("headerDatas must be triplets of {String, int, String}");
        }
        for (int i = 0; i < headerDatas.length; i += 3) {
            m_columns.add(
                    new ColumnManager(
                            (String) headerDatas[i],
                            ((Number) headerDatas[i + 1]).intValue(),
                            (String) headerDatas[i + 2]));
        }
    }

    /**
     * CodexID is optional, but nice to have
     */
    public void addCell(Object cell, Alignment alg, String codexID) {
        if (rowInStack == null) {
            rowInStack = new RowManager(
                    getRoot(),
                    getParent(),
                    (int) getPanelPos().getWidth(),
                    m_rowHeight,
                    m_market,
                    new RowSelectionListener() {
                        @Override
                        public void onRowSelected(RowManager row) {
                            m_selectedRow = row;
                        }
                    });
        }

        rowInStack.addCell(cell, alg);
        rowInStack.setCodexId(codexID);
    }

    /**
     * Uses the added cells to create a row and clears the rowInStack.
     * The amount of cells must match the column amount.
     */
    public void pushRow() {
        if (rowInStack == null || rowInStack.m_cellData.isEmpty()) {
            throw new IllegalStateException("Cannot push row: no cells have been added yet. "
                    + "Call addCell() before pushRow().");

        } else if (rowInStack.m_cellData.size() != m_columns.size()) {
            throw new IllegalStateException("Cannot push row: cell count mismatch. "
                    + "The number of cells must match the number of columns.");

        } else {
            rowInStack.createPanel();
        }

        rowInStack = null;
    }

    public void sortRows(int index) {
        if (m_rows.isEmpty()) {
            return;
        }

        selectedSortColumnIndex = index;
        if (index == prevSelectedSortColumnIndex) {
            ascending = !ascending;
        }

        Object value = m_rows.get(0).getSortValue(index);

        if (value instanceof String) {
            Collections.sort(m_rows, stringComparator);

        } else if (value instanceof Integer ||
                value instanceof Long ||
                value instanceof Float ||
                value instanceof Double) {
            Collections.sort(m_rows, numberComparator);

        } else {
            throw new IllegalArgumentException(
                    "Cannot sort rows: unsupported sort value type '"
                            + value.getClass().getSimpleName()
                            + "'. Supported types are String, Integer, Long, Float and Double.");
        }

        prevSelectedSortColumnIndex = index;
    }

    private Comparator<RowManager> stringComparator = (a, b) -> {
        String valA = (String) a.getSortValue(selectedSortColumnIndex);
        String valB = (String) b.getSortValue(selectedSortColumnIndex);

        int cmp = valA.compareTo(valB);
        return ascending ? cmp : -cmp;
    };

    private Comparator<RowManager> numberComparator = (a, b) -> {
        Number valA = (Number) a.getSortValue(selectedSortColumnIndex);
        Number valB = (Number) b.getSortValue(selectedSortColumnIndex);

        int cmp = Double.compare(valA.doubleValue(), valB.doubleValue());
        return ascending ? cmp : -cmp;
    };

    public void selectRow(RowManager selectedRow) {
        for (RowManager row : m_rows) {
            row.getPlugin().setPersistentGlow(row == selectedRow);
        }
    }
}

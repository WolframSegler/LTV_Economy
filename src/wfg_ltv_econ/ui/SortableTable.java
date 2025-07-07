package wfg_ltv_econ.ui;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

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
import wfg_ltv_econ.ui.LtvUIState.UIStateType;
import wfg_ltv_econ.util.TooltipUtils;
import wfg_ltv_econ.util.UiUtils;
import wfg_ltv_econ.plugins.LtvSpritePanelPlugin;

/**
 * Supports the following types:
 * String, LabelAPI, LtvSpritePanel
 */
public class SortableTable extends LtvCustomPanel {
    private final List<ColumnManager> m_columns = new ArrayList<>();
    private final List<RowManager> m_rows = new ArrayList<>();

    private RowManager pendingRow = null;

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

    private CustomPanelAPI m_headerContainer = null;
    private CustomPanelAPI m_rowContainer = null;

    public final static int pad = 3;
    public final static int opad = 10;

    public final static String sortIconPath;
    static {
        sortIconPath = Global.getSettings().getSpriteName("ui", "sortIcon");
    }

    public SortableTable(UIPanelAPI root, UIPanelAPI parent, int width, int height, MarketAPI market) {
        this(root, parent, width, height, market, 18, 28);
    }

    public SortableTable(UIPanelAPI root, UIPanelAPI parent, int width, int height, MarketAPI market,
            int headerHeight, int rowHeight) {
        super(root, parent, width, height, null, market);
        m_headerHeight = headerHeight;
        m_rowHeight = rowHeight;

        // The Table itself needs to be created after the rows are ready
        initializePlugin(hasPlugin);
    }

    public void initializePlugin(boolean hasPlugin) {
    }

    public void createPanel() {
        // Clear previous Panels
        getPanel().removeComponent(m_headerContainer);
        getPanel().removeComponent(m_rowContainer);

        // Create headers
        m_headerContainer = Global.getSettings().createCustom(
            getPanelPos().getWidth(),
            m_headerHeight,
            null
        );

        int cumulativeXOffset = 0;
        for (int i = 0; i < m_columns.size(); i++) {
            ColumnManager column = m_columns.get(i);

            if (column.tooltipText == null) {
                m_headerContainer.addComponent((new HeaderPanel(getRoot(), getPanel(), column.width, 
                    m_headerHeight, m_market, column, i).getPanel())).inTL(cumulativeXOffset, 0);
            } else {
                m_headerContainer.addComponent((new HeaderPanelWithTooltip(getRoot(), getPanel(), column.width, 
                    m_headerHeight, m_market, column, i).getPanel())).inTL(cumulativeXOffset, 0);
            }

            cumulativeXOffset += column.width + pad;

        }
        getPanel().addComponent(m_headerContainer).inTL(0,0);

        // Create rows
        m_rowContainer = Global.getSettings().createCustom(
            getPanelPos().getWidth(),
            getPanelPos().getHeight() - (m_headerHeight + pad),
            null
        );

        TooltipMakerAPI tp = m_rowContainer.createUIElement(
            getPanelPos().getWidth(),
            getPanelPos().getHeight() - (m_headerHeight + pad),
            true
        );

        int cumulativeYOffset = 0;
        for (RowManager row : m_rows) {
            tp.addComponent(row.getPanel()).inTL(
                    0, cumulativeYOffset);

            cumulativeYOffset += pad + m_rowHeight;
        }

        tp.setHeightSoFar(cumulativeYOffset);

        m_rowContainer.addUIElement(tp).inTL(0, 0);
        getPanel().addComponent(m_rowContainer).inTL(0, m_headerHeight + pad);
    }

    private class HeaderPanel extends LtvCustomPanel {
        protected final ColumnManager column;
        public int listIndex = -1;

        public HeaderPanel(UIPanelAPI root, UIPanelAPI parent, int width, int height, MarketAPI market,
                ColumnManager column, int listIndex) {
            super(root, parent, width, height, new LtvCustomPanelPlugin() {
                @Override
                public void advance(float amount) {
                    super.advance(amount);

                    if (LMBUpLastFrame && hasClickedBefore) {
                        SortableTable.this.sortRows(((HeaderPanel) m_panel).listIndex);
                    }
                }
            }, market);
            this.column = column;
            this.listIndex = listIndex;

            createPanel();
            initializePlugin(hasPlugin);
        }

        @Override
        public void initializePlugin(boolean hasPlugin) {
            getPlugin().init(this, GlowType.OVERLAY, false, true, true);
            getPlugin().setTargetUIState(UIStateType.DETAIL_DIALOG);
        }

        @Override
        public void createPanel() {
            CustomPanelAPI panel = SortableTable.HeaderPanel.this.getPanel();
            TooltipMakerAPI tooltip = panel.createUIElement(
                    getPanelPos().getWidth(), getPanelPos().getHeight(), false);

            tooltip.setParaFontColor(m_market.getFaction().getBaseUIColor());

            LabelAPI lbl = tooltip.addPara(column.title, pad);
            final int lblWidth = (int) lbl.computeTextWidth(lbl.getText());

            lbl.getPosition().inBL((getPanelPos().getWidth() / 2f) - (lblWidth / 2f), pad);

            LtvSpritePanel sortIcon = new LtvSpritePanel(
                    getRoot(),
                    panel,
                    m_market,
                    m_headerHeight + 2, m_headerHeight + 2,
                    new LtvSpritePanelPlugin(),
                    sortIconPath,
                    getFaction().getBaseUIColor(),
                    null,
                    false);
            sortIcon.getPlugin().setGlowType(GlowType.NONE);

            tooltip.addComponent(sortIcon.getPanel()).inBR(1, 0);

            panel.addUIElement(tooltip).inBL(0, 0);
        }
    }

    private class HeaderPanelWithTooltip extends HeaderPanel implements LtvCustomPanel.TooltipProvider {
        public HeaderPanelWithTooltip(UIPanelAPI root, UIPanelAPI parent, int width, int height,
                MarketAPI market, ColumnManager column, int listIndex) {
            super(root, parent, width, height, market, column, listIndex);
        }

        @Override
        public void initializePlugin(boolean hasPlugin) {
            getPlugin().init(this, GlowType.OVERLAY, true, true, true);
            getPlugin().setTargetUIState(UIStateType.DETAIL_DIALOG);
        }

        public TooltipMakerAPI createTooltip() {
            TooltipMakerAPI tooltip = ((CustomPanelAPI) getParent()).createUIElement(
                    400, 0, false);

            tooltip.addPara(column.tooltipText, pad);

            ((CustomPanelAPI) getParent()).addUIElement(tooltip);
            ((CustomPanelAPI) getParent()).bringComponentToTop(tooltip);
            tooltip.getPosition().aboveLeft(getPanel(), pad*2);

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

    public RowManager getSelectedRow() {
        return m_selectedRow;
    }

    private class ColumnManager {

        public String title;
        public int width;
        public String tooltipText;

        public ColumnManager(String title, int width, String tooltipText) {
            this.title = title;
            this.width = width;
            this.tooltipText = tooltipText;
        }
    }

    public class RowManager extends LtvCustomPanel implements LtvCustomPanel.TooltipProvider {
        protected final List<Object> m_cellData = new ArrayList<>();
        protected final List<Alignment> m_cellAlignment = new ArrayList<>();
        protected final List<Object> m_sortValues = new ArrayList<>();
        protected final List<Boolean> m_useColor = new ArrayList<>();
        protected String codexID = null;

        public Color textColor = getFaction().getBaseUIColor();

        public RowManager(UIPanelAPI root, UIPanelAPI parent, int width, int height, MarketAPI market,
                RowSelectionListener listener) {
            super(root, parent, width, height, new LtvCustomPanelPlugin() {
                @Override
                public void advance(float amount) {
                    super.advance(amount);

                    if (LMBUpLastFrame && hasClickedBefore) {
                        SortableTable table = SortableTable.this;
                        table.selectRow((RowManager) m_panel);

                        if (table.selectionListener != null) {
                            table.selectionListener.onRowSelected((RowManager) m_panel);
                        }

                        hasClickedBefore = false;
                    }
                }
            }, market);

            initializePlugin(hasPlugin);
        }

        public void initializePlugin(boolean hasPlugin) {
            getPlugin().init(this, GlowType.OVERLAY, true, false, false);
            getPlugin().setTargetUIState(UIStateType.DETAIL_DIALOG);
        }

        public void createPanel() {

            CustomPanelAPI panel = SortableTable.RowManager.this.getPanel();
            int cumulativeXOffset = 0;

            for (int i = 0; i < m_cellData.size(); i++) {
                Object cell = m_cellData.get(i);
                Alignment alignment = m_cellAlignment.get(i);
                boolean useColor = m_useColor.get(i);
                float colWidth = getColumns().get(i).width;

                UIComponentAPI comp;
                float compWidth;
                float compHeight;

                if (cell instanceof Number) {
                    LabelAPI label = Global.getSettings().createLabel(String.valueOf(cell), Fonts.DEFAULT_SMALL);
                    comp = (UIComponentAPI) label;
                    compWidth = label.computeTextWidth(label.getText());
                    compHeight = label.computeTextHeight(label.getText());

                    if (useColor) {
                        label.setColor(textColor);
                    } else {
                        label.setColor(SortableTable.this.getFaction().getBaseUIColor());
                    }

                } else if (cell instanceof String) {
                    LabelAPI label = Global.getSettings().createLabel((String) cell, Fonts.DEFAULT_SMALL);
                    comp = (UIComponentAPI) label;
                    compWidth = label.computeTextWidth(label.getText());
                    compHeight = label.computeTextHeight(label.getText());

                    if (useColor) {
                        label.setColor(textColor);
                    } else {
                        label.setColor(SortableTable.this.getFaction().getBaseUIColor());
                    }

                } else if (cell instanceof LtvSpritePanel) {
                    comp = (UIComponentAPI) ((LtvSpritePanel)cell).getPanel();
                    compWidth = ((LtvSpritePanel) cell).getPanelPos().getWidth();
                    compHeight = ((LtvSpritePanel) cell).getPanelPos().getHeight();

                } else if (cell instanceof LabelAPI) {
                    LabelAPI label = (LabelAPI) cell;
                    comp = (UIComponentAPI) label;
                    compWidth = label.computeTextWidth(label.getText());
                    compHeight = label.computeTextHeight(label.getText());

                    if (useColor) {
                        label.setColor(textColor);
                    } else {
                        label.setColor(SortableTable.this.getFaction().getBaseUIColor());
                    }

                } else {
                    throw new IllegalArgumentException("Unsupported cell type: " + cell.getClass());
                }

                float xOffset = calcXOffset(cumulativeXOffset, colWidth, compWidth, alignment);
                float yOffset = (m_rowHeight/2) - (compHeight/2);
                panel.addComponent(comp).inBL(xOffset, yOffset);

                cumulativeXOffset += colWidth + pad;
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
            if (m_sortValues.get(columnIndex) == null) {
                return m_cellData.get(columnIndex);
            }

            return m_sortValues.get(columnIndex);
        }

        public void setCodexId(String codex) {
            codexID = codex;
        }

        public void setTextColor(Color color) {
            textColor = color;
        }

        public TooltipMakerAPI createTooltip() {
            TooltipMakerAPI tooltip = ((CustomPanelAPI) getParent()).createUIElement(
                    400, 0, false);

            ((CustomPanelAPI) getParent()).addUIElement(tooltip);
            ((CustomPanelAPI) getParent()).bringComponentToTop(tooltip);
            TooltipUtils.mouseCornerPos(tooltip, opad);

            if (codexID != null) {
                tooltip.setCodexEntryId(codexID);
                UiUtils.positionCodexLabel(tooltip, opad, pad);
            }

            return tooltip;
        }

        public void removeTooltip(TooltipMakerAPI tooltip) {
            ((CustomPanelAPI) getParent()).removeComponent(tooltip);
        }

        public void attachCodexTooltip(TooltipMakerAPI codex) {
        }

        public void addCell(Object cell, Alignment alg, Object sort, boolean useColor) {
            m_cellData.add(cell);
            m_cellAlignment.add(alg);
            m_sortValues.add(sort);
            m_useColor.add(useColor);
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
            Object titleObj = headerDatas[i];
            Object widthObj = headerDatas[i + 1];
            Object tooltipObj = headerDatas[i + 2];

            if (!(titleObj instanceof String)) {
                throw new IllegalArgumentException("Header title must be String.");
            }
            if (!(widthObj instanceof Number)) {
                throw new IllegalArgumentException("Header width must be int.");
            }
            if (tooltipObj != null && !(tooltipObj instanceof String)) {
                throw new IllegalArgumentException("Tooltip text must be String or null.");
            }
            m_columns.add(
                    new ColumnManager(
                            (String) titleObj,
                            ((Number) widthObj).intValue(),
                            (String) tooltipObj));
        }
    }

    /**
     * The call order of addCell must match the order of Columns.
     * CodexID is optional.
     */
    public void addCell(Object cell, Alignment alg, Object sort, boolean useColor) {
        if (pendingRow == null) {
            pendingRow = new RowManager(
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

        pendingRow.addCell(cell, alg, sort, useColor);
    }

    /**
     * Uses the added cells to create a row and clears the rowInStack.
     * The amount of cells must match the column amount.
     * The market is used for certain colors and location info.
     */
    public void pushRow(String codexID, MarketAPI market, Color textColor) {
        if (pendingRow == null || pendingRow.m_cellData.isEmpty()) {
            throw new IllegalStateException("Cannot push row: no cells have been added yet. "
                    + "Call addCell() before pushRow().");

        } else if (pendingRow.m_cellData.size() != m_columns.size()) {
            throw new IllegalStateException("Cannot push row: cell count mismatch. "
                    + "The number of cells must match the number of columns.");

        }
        pendingRow.setCodexId(codexID);
        pendingRow.setMarket(market);
        pendingRow.setTextColor(textColor);
        
        pendingRow.createPanel();
        m_rows.add(pendingRow);

        pendingRow = null;
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

        createPanel();
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

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
import com.fs.starfarer.api.util.Misc;

import wfg_ltv_econ.plugins.LtvCustomPanelPlugin;
import wfg_ltv_econ.plugins.LtvCustomPanelPlugin.Glow;
import wfg_ltv_econ.plugins.LtvCustomPanelPlugin.Outline;
import wfg_ltv_econ.ui.LtvUIState.UIState;
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

    public RowManager getPendingRow() {
        return pendingRow;
    }

    private RowManager m_selectedRow;

    private CustomPanelAPI m_headerContainer = null;
    private CustomPanelAPI m_rowContainer = null;

    public final static int pad = 3;
    public final static int opad = 10;
    public final static int headerTooltipWidth = 450;

    public final static String sortIconPath;
    static {
        sortIconPath = Global.getSettings().getSpriteName("ui", "sortIcon");
    }

    public SortableTable(UIPanelAPI root, UIPanelAPI parent, int width, int height, MarketAPI market) {
        this(root, parent, width, height, market, 20, 28);
    }

    public SortableTable(UIPanelAPI root, UIPanelAPI parent, int width, int height, MarketAPI market,
            int headerHeight, int rowHeight) {
        super(root, parent, width, height, null, market);
        m_headerHeight = headerHeight;
        m_rowHeight = rowHeight;

        // The Table itself needs to be created after the rows are ready
        initializePlugin(hasPlugin);
    }

    public void initializePlugin(boolean hasPlugin) {}

    public void createPanel() {
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

            int lastHeaderPad = i + 1 == m_columns.size() ? pad*3 : 0;

            UIPanelAPI panel = null;

            // Merged headers
            if (column.isMerged()) {
                int setID = column.getSetID();

                if (!column.isParent()) {
                    continue;
                }
                // Calculate total width of all merged columns in this set
                int mergedWidth = lastHeaderPad;
                for (ColumnManager col : m_columns) {
                    if (col.isMerged() && col.getSetID() == setID) {
                        mergedWidth += col.width;
                    }
                }

                if (column.tooltip == null) {
                    panel = new HeaderPanel(
                        getRoot(), getPanel(), mergedWidth - pad, m_headerHeight, m_market, column, i
                    ).getPanel();
                } else {
                    panel = new HeaderPanelWithTooltip(
                        getRoot(), getPanel(), mergedWidth - pad, m_headerHeight, m_market, column, i
                    ).getPanel();
                }

                m_headerContainer.addComponent(panel).inTL(cumulativeXOffset, 0);

                cumulativeXOffset += mergedWidth;

            // Standalone header
            } else {
                if (column.tooltip == null) {
                    panel = new HeaderPanel(
                        getRoot(), getPanel(), column.width + lastHeaderPad - pad, m_headerHeight,
                        m_market, column, i
                    ).getPanel();
                } else {
                    panel = new HeaderPanelWithTooltip(
                        getRoot(), getPanel(), column.width + lastHeaderPad - pad, m_headerHeight,
                        m_market, column, i
                    ).getPanel();
                }

                m_headerContainer.addComponent(panel).inTL(cumulativeXOffset, 0);

                cumulativeXOffset += column.width;
            }

            column.setHeaderPanel(panel);
        }

        getPanel().addComponent(m_headerContainer).inTL(0,0);

        // Create rows
        m_rowContainer = Global.getSettings().createCustom(
            getPanelPos().getWidth(),
            getPanelPos().getHeight() - (m_headerHeight + pad),
            null
        );

        TooltipMakerAPI tp = m_rowContainer.createUIElement(
            getPanelPos().getWidth() + pad,
            getPanelPos().getHeight() - (m_headerHeight + pad),
            true
        );

        int cumulativeYOffset = pad; // The first row should still have a gap
        for (RowManager row : m_rows) {
            tp.addComponent(row.getPanel()).inTL(
                    pad, cumulativeYOffset);

            cumulativeYOffset += pad + m_rowHeight;
        }

        tp.setHeightSoFar(cumulativeYOffset);

        m_rowContainer.addUIElement(tp).inTL(-pad, 0);
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
            getPlugin().init(this, Glow.OVERLAY, false, true, Outline.LINE);
            getPlugin().setTargetUIState(UIState.DETAIL_DIALOG);

            glowColor = Misc.getTooltipTitleAndLightHighlightColor();
        }

        @Override
        public void createPanel() {
            CustomPanelAPI panel = SortableTable.HeaderPanel.this.getPanel();
            TooltipMakerAPI tooltip = panel.createUIElement(
                    getPanelPos().getWidth(), getPanelPos().getHeight(), false);

            tooltip.setParaFontColor(m_market.getFaction().getBaseUIColor());
            tooltip.setParaFont(Fonts.ORBITRON_12);

            LabelAPI lbl = tooltip.addPara(column.title, pad);
            final float lblWidth = lbl.computeTextWidth(lbl.getText());
            final float lblHeight = lbl.computeTextHeight(lbl.getText());

            lbl.getPosition().inTL(
                (getPanelPos().getWidth() / 2f) - (lblWidth / 2f),
                (getPanelPos().getHeight() / 2f) - (lblHeight / 2f) 
            );

            LtvSpritePanel sortIcon = new LtvSpritePanel(
                    getRoot(),
                    panel,
                    m_market,
                    m_headerHeight, m_headerHeight + 2,
                    new LtvSpritePanelPlugin(),
                    sortIconPath,
                    getFaction().getBaseUIColor(),
                    null,
                    false);
            sortIcon.getPlugin().setHasGlow(Glow.NONE);

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
            getPlugin().init(this, Glow.OVERLAY, true, true, Outline.LINE);
            getPlugin().setTargetUIState(UIState.DETAIL_DIALOG);
        }

        public UIPanelAPI getTooltipAttachmentPoint() {
            return getParent();
        }

        public TooltipMakerAPI createTooltip() {
            final TooltipMakerAPI tooltip;

            if (column.getTooltipType() == String.class) {
                tooltip = ((CustomPanelAPI) getParent()).createUIElement(
                        headerTooltipWidth, 0, false);
    
                tooltip.addPara((String) column.tooltip, pad);
            } else if (column.getTooltipType() == PendingTooltip.class) {
                tooltip = ((PendingTooltip) column.tooltip).tooltip;
            } else {
                throw new IllegalArgumentException(
                    "Tooltip for header '" + column.title + "' has an illegal type."
                );
            }

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

    public class ColumnManager {

        public String title;
        public int width;
        public Object tooltip;
        
        private UIPanelAPI panel = null;
        private boolean isMerged = false;
        private boolean isParent = false;
        private int mergeSetID = 0;

        public ColumnManager(String title, int width, Object tooltip,
            boolean isMerged, boolean isParent, int mergeSetID) {
            this.title = title;
            this.width = width;
            this.tooltip = tooltip;

            this.isMerged = isMerged;
            this.isParent = isParent;
            this.mergeSetID = mergeSetID;
        }

        public Class<?> getTooltipType() {
            if (tooltip instanceof String) {
                return String.class;
            }
            if (tooltip instanceof PendingTooltip) {
                return PendingTooltip.class;
            }

            return Object.class;
        }

        public boolean isMerged() {
            return isMerged;
        }

        public boolean isParent() {
            return isParent;
        }

        public int getSetID() {
            return mergeSetID;
        }

        public UIPanelAPI getHeaderPanel() {
            return panel;
        }

        public void setHeaderPanel(UIPanelAPI a) {
            panel = a;
        }
    }

    public class RowManager extends LtvCustomPanel implements LtvCustomPanel.TooltipProvider {
        protected final List<Object> m_cellData = new ArrayList<>();
        protected final List<Alignment> m_cellAlignment = new ArrayList<>();
        protected final List<Object> m_sortValues = new ArrayList<>();
        protected final List<Color> m_useColor = new ArrayList<>();
        protected String codexID = null;
        
        public TooltipMakerAPI m_tooltip = null;
        public Color textColor = Misc.getBasePlayerColor();

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
            getPlugin().init(this, Glow.OVERLAY, true, false, Outline.NONE);
            getPlugin().setTargetUIState(UIState.DETAIL_DIALOG);
        }

        public void createPanel() {

            CustomPanelAPI panel = SortableTable.RowManager.this.getPanel();
            int cumulativeXOffset = 0;

            for (int i = 0; i < m_cellData.size(); i++) {
                Object cell = m_cellData.get(i);
                Alignment alignment = m_cellAlignment.get(i);
                Color useColor = m_useColor.get(i);
                float colWidth = getColumns().get(i).width;

                UIComponentAPI comp;
                float compWidth;
                float compHeight;

                if (cell instanceof Number) {
                    LabelAPI label = Global.getSettings().createLabel(String.valueOf(cell), Fonts.DEFAULT_SMALL);
                    comp = (UIComponentAPI) label;
                    compWidth = label.computeTextWidth(label.getText());
                    compHeight = label.computeTextHeight(label.getText());

                    if (useColor != null) {
                        label.setColor(useColor);
                    } else {
                        label.setColor(textColor);
                    }

                } else if (cell instanceof String) {
                    LabelAPI label = Global.getSettings().createLabel((String) cell, Fonts.DEFAULT_SMALL);
                    comp = (UIComponentAPI) label;
                    compWidth = label.computeTextWidth(label.getText());
                    compHeight = label.computeTextHeight(label.getText());

                    if (useColor != null) {
                        label.setColor(useColor);
                    } else {
                        label.setColor(textColor);
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

                    if (useColor != null) {
                        label.setColor(useColor);
                    } else {
                        label.setColor(textColor);
                    }

                } else {
                    throw new IllegalArgumentException("Unsupported cell type: " + cell.getClass());
                }

                float xOffset = calcXOffset(cumulativeXOffset, colWidth, compWidth, alignment);
                float yOffset = (m_rowHeight/2) - (compHeight/2);
                panel.addComponent(comp).inBL(xOffset, yOffset);

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
            if (m_sortValues.get(columnIndex) == null) {
                return m_cellData.get(columnIndex);
            }

            return m_sortValues.get(columnIndex);
        }

        public void setCodexId(String codex) {
            codexID = codex;
        }

        public void setTextColor(Color color) {
            if (color != null) {
                textColor = color;
            }
        }

        public UIPanelAPI getTooltipAttachmentPoint() {
            return getParent();
        }
        
        public TooltipMakerAPI createTooltip() {
            if (m_tooltip == null) {
                m_tooltip = ((CustomPanelAPI) getParent()).createUIElement(
                    headerTooltipWidth, 0, false);
            }

            (SortableTable.this.getPanel()).addUIElement(m_tooltip);
            (SortableTable.this.getPanel()).bringComponentToTop(m_tooltip);
            TooltipUtils.mouseCornerPos(m_tooltip, opad);

            if (codexID != null) {
                m_tooltip.setCodexEntryId(codexID);
                UiUtils.positionCodexLabel(m_tooltip, opad, pad);
            }

            return m_tooltip;
        }

        public void removeTooltip(TooltipMakerAPI tooltip) {
            (SortableTable.this.getPanel()).removeComponent(tooltip);
        }

        public void attachCodexTooltip(TooltipMakerAPI codex) {
        }

        public void addCell(Object cell, Alignment alg, Object sort, Color textColor) {
            m_cellData.add(cell);
            m_cellAlignment.add(alg);
            m_sortValues.add(sort);
            m_useColor.add(textColor);
        }

        public List<Object> getCellData() {
            return m_cellData;
        }
    }

    /**
     * Each set must contain the title of the header, its width, the text of the
     * tooltip or a PendingTooltip, whether if it is merged, if it is the parent and the ID of the mergeSet.
     * A merged non-parent header will not display a tooltip.
     * <br></br> The expected input is {String, int, String, Bool, Bool, int}.
     * Or alternatively {String, int, PendingTooltip, Bool, Bool, int}.
     * The tooltip and mergeSetID can be left empty:
     * {String, int, null, Bool, Bool, null}.
     */
    public void addHeaders(Object... headerDatas) {
        m_columns.clear();
        if (headerDatas.length % 6 != 0) {
            throw new IllegalArgumentException(
                "headerDatas must be sextuplets of {String, int, String, Bool, Bool, int}"
            );
        }
        for (int i = 0; i < headerDatas.length; i += 6) {
            Object titleObj = headerDatas[i];
            Object widthObj = headerDatas[i + 1];
            Object tooltipObj = headerDatas[i + 2];
            Object isMergedObj = headerDatas[i + 3];
            Object isParentObj = headerDatas[i + 4];
            Object mergeSetIdObj = headerDatas[i + 5];

            if (!(titleObj instanceof String)) {
                throw new IllegalArgumentException("Header title must be String.");
            }
            if (!(widthObj instanceof Number)) {
                throw new IllegalArgumentException("Header width must be int.");
            }
            if (tooltipObj != null && !(tooltipObj instanceof String || tooltipObj instanceof PendingTooltip)) {
                throw new IllegalArgumentException("Tooltip text must be String, PendingTooltip or null.");
            }
            if (!(isMergedObj instanceof Boolean)) {
                throw new IllegalArgumentException("isMerged must be Boolean.");
            }
            if (!(isParentObj instanceof Boolean)) {
                throw new IllegalArgumentException("isParent must be Boolean.");
            }
            if (mergeSetIdObj != null && !(mergeSetIdObj instanceof Number)) {
                throw new IllegalArgumentException("mergeSetID must be int or null.");
            }
            final int mergeSetID = mergeSetIdObj != null ? ((Number) mergeSetIdObj).intValue() : -1;

            m_columns.add(
                    new ColumnManager(
                            (String) titleObj,
                            ((Number) widthObj).intValue(),
                            (Object) tooltipObj,
                            (Boolean) isMergedObj,
                            (Boolean) isParentObj,
                            mergeSetID));
        }
    }

    /**
     * The call order of addCell must match the order of Columns.
     * CodexID is optional.
     */
    public void addCell(Object cell, Alignment alg, Object sort, Color textColor) {
        if (pendingRow == null) {
            pendingRow = new RowManager(
                    getRoot(),
                    getParent(),
                    (int) getPanelPos().getWidth() - 2,
                    m_rowHeight,
                    m_market,
                    new RowSelectionListener() {
                        @Override
                        public void onRowSelected(RowManager row) {
                            m_selectedRow = row;
                        }
                    });
        }

        pendingRow.addCell(cell, alg, sort, textColor);
    }

    /**
     * Uses the added cells to create a row and clears the {@code pendingRow}.
     * The amount of cells must match the column amount.
     * The {@code textColor} sets all the cells to that color.
     * The {@code market} is used for certain colors and location info.
     * {@code glowClr} can be null.
     * The {@code tp} can be null. It must be attached to the SortableTable instance due to a design limitation.
     */
    public void pushRow(String codexID, MarketAPI market, Color textColor, Color glowClr, TooltipMakerAPI tp) {
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
        if (glowClr != null) {
            pendingRow.setGlowColor(glowClr);
        }

        // Selected Colony has an outline
        if (m_market == market) {
            pendingRow.getPlugin().setOutline(Outline.THIN);
        }

        pendingRow.m_tooltip = tp;
        
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

        createPanel(); // Refresh the table
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

    public void selectLastRow() {
        for (RowManager row : m_rows) {
            row.getPlugin().setPersistentGlow(row == m_rows.get(m_rows.size() - 1));
        }
    }

    public void selectRow(RowManager selectedRow) {
        for (RowManager row : m_rows) {
            row.getPlugin().setPersistentGlow(row == selectedRow);
        }
    }

    /**
     * A TooltipMakerAPI implementation that acts as a mutable shell.
     * Used to pass null checks during UI construction. Actual content is added after
     * panel instantiation.
     *
     * Used internally by SortableTable. Use this when creating a custom tooltip for a header.
     */
    public static class PendingTooltip {
        public TooltipMakerAPI tooltip = null;
    }
}

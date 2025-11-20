package wfg.wrap_ui.ui.panels;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.ui.CustomPanelAPI;
import com.fs.starfarer.api.ui.Fonts;
import com.fs.starfarer.api.ui.LabelAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.ui.UIComponentAPI;
import com.fs.starfarer.api.ui.UIPanelAPI;
import com.fs.starfarer.api.util.FaderUtil;
import com.fs.starfarer.api.util.Misc;

import wfg.wrap_ui.util.CallbackRunnable;
import wfg.wrap_ui.util.WrapUiUtils;
import wfg.wrap_ui.util.WrapUiUtils.AnchorType;
import wfg.wrap_ui.ui.UIState.State;
import wfg.wrap_ui.ui.panels.CustomPanel.HasTooltip.PendingTooltip;
import wfg.wrap_ui.ui.panels.SpritePanel.Base;
import wfg.wrap_ui.ui.plugins.BasePanelPlugin;
import wfg.wrap_ui.ui.systems.FaderSystem.Glow;
import wfg.wrap_ui.ui.systems.OutlineSystem.Outline;

/**
 * SortableTable is a customizable, sortable UI table component designed to display
 * tabular data within a custom panel environment. It extends {@link CustomPanel}
 * to integrate smoothly with the Wrao UI framework.
 * <p>
 * <b>This table supports:</b>
 * <ul>
 *   <li>Dynamic column headers with customizable width, tooltip, alignment, and sorting behavior.</li>
 *   <li>Adding rows with multiple typed cells (text, icons, custom components, numeric values).</li>
 *   <li>Sorting rows by any sortable column (e.g., numeric or text columns).</li>
 *   <li>Row selection events via a listener callback for interactive behavior.</li>
 * </ul>
 * <p>
 * <b>Accepted row cell types:</b>
 * <ul>
 *   <li>{@link Integer} — displayed as a small-font label with optional color.</li>
 *   <li>{@link String} — displayed as a small-font label with optional color.</li>
 *   <li>{@link SpritePanel} — displayed as a sprite panel UI component.</li>
 *   <li>{@link UIPanelAPI} — displayed directly as a UI panel component.</li>
 *   <li>{@link LabelAPI} — displayed as a label UI component with optional color.</li>
 * </ul>
 * </p>
 * <b>Typical usage example:</b>
 * <pre>{@code
 * SortableTable table = new SortableTable(...);
 *
 * // Setup headers with labels, widths, tooltips, merge flags and merge group
 * table.addHeaders(
 *     "", 40, null, true, false, 1, // Icon header
 *     "Colony", 200, "Colony name", true, true, 1,
 *     "Size", 100, "Colony size", false, false, -1,
 *     "Faction", 150, "Controlling faction", false, false, -1,
 *     // etc.
 * );
 *
 * // Add rows with multiple typed cells and associated tooltips
 * table.addCell(iconPanel, cellAlg.LEFT, null, null);
 * table.addCell(colonyName, cellAlg.LEFT, null, textColor);
 * table.addCell(5, cellAlg.MID, null, textColor);
 * // ...
 * 
 * // Register a listener for row selection
 * final CallbackRunnable<RowManager> rowSelectedRunnable = (row) -> {
 *      ...
 * };
 *
 * table.pushRow(codexID, customData, null, highlightColor, tooltip, rowSelectedRunnable);
 *
 * // Add table to UI panel and initialize it
 * parentPanel.addComponent(table.getPanel()).inTL(0, 0);
 * table.createPanel();
 *
 * // Enable sorting by a particular column index. Calls createPanel() internally
 * table.sortRows(columnIndex);
 * }</pre>
 * <p>
 * This component supports tooltips both for headers and rows via {@link PendingTooltip}.
 * <p>
 */
public class SortableTable extends CustomPanel<BasePanelPlugin<SortableTable>, SortableTable, CustomPanelAPI> {

    public boolean showSortIcon = true;
    public boolean sortingEnabled = true;

    private final List<ColumnManager> m_columns = new ArrayList<>();
    private final List<RowManager> m_rows = new ArrayList<>();
    private RowManager pendingRow = null;

    private final int HEADER_HEIGHT;
    private final int ROW_HEIGHT;

    private int prevSelectedSortColumnIndex = -1;
    private int selectedSortColumnIndex = -1;
    private boolean ascending = true;

    private RowManager m_selectedRow;
    private CustomPanelAPI m_headerContainer = null;
    private CustomPanelAPI m_rowContainer = null;

    public RowManager getPendingRow() {
        return pendingRow;
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

    public final static int pad = 3;
    public final static int opad = 10;
    public final static int headerTooltipWidth = 250;

    public final static String sortIconPath;
    static {
        sortIconPath = Global.getSettings().getSpriteName("ui", "sortIcon");
    }

    public SortableTable(UIPanelAPI parent, int width, int height) {
        this(parent, width, height, 20, 28);
    }

    public SortableTable(UIPanelAPI parent, int width, int height, int headerHeight, int rowHeight) {
        super(parent, width, height, null);
        HEADER_HEIGHT = headerHeight;
        ROW_HEIGHT = rowHeight;

        // The Table itself needs to be created after the rows are ready
        initializePlugin(hasPlugin);
    }

    public void initializePlugin(boolean hasPlugin) {}

    public void createPanel() {
        getPanel().removeComponent(m_headerContainer);
        getPanel().removeComponent(m_rowContainer);

        // Create headers
        m_headerContainer = Global.getSettings().createCustom(
            getPos().getWidth(),
            HEADER_HEIGHT,
            null
        );

        int cumulativeXOffset = 0;
        for (int i = 0; i < m_columns.size(); i++) {
            ColumnManager column = m_columns.get(i);

            int lastHeaderPad = i + 1 == m_columns.size() ? pad*3 : 0;

            HeaderPanel panel = null;

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
                        getPanel(), mergedWidth - pad, HEADER_HEIGHT, column, i
                    );
                } else {
                    panel = new HeaderPanelWithTooltip(
                        getPanel(), mergedWidth - pad, HEADER_HEIGHT, column, i
                    );
                }

                m_headerContainer.addComponent(panel.getPanel()).inBL(cumulativeXOffset, 0);

                cumulativeXOffset += mergedWidth;

            // Standalone header
            } else {
                if (column.tooltip == null) {
                    panel = new HeaderPanel(
                        getPanel(), column.width + lastHeaderPad - pad, HEADER_HEIGHT,
                        column, i
                    );
                } else {
                    panel = new HeaderPanelWithTooltip(
                        getPanel(), column.width + lastHeaderPad - pad, HEADER_HEIGHT,
                        column, i
                    );
                }

                m_headerContainer.addComponent(panel.getPanel()).inBL(cumulativeXOffset, 0);

                cumulativeXOffset += column.width;
            }

            column.setHeaderPanel(panel);
        }

        getPanel().addComponent(m_headerContainer).inTL(0,0);

        // Create rows
        m_rowContainer = Global.getSettings().createCustom(
            getPos().getWidth(),
            getPos().getHeight() - (HEADER_HEIGHT + pad),
            null
        );

        TooltipMakerAPI tp = m_rowContainer.createUIElement(
            getPos().getWidth() + pad,
            getPos().getHeight() - (HEADER_HEIGHT + pad),
            true
        );

        int cumulativeYOffset = pad;
        for (RowManager row : m_rows) {
            tp.addComponent(row.getPanel()).inTL(pad, cumulativeYOffset);

            cumulativeYOffset += pad + ROW_HEIGHT;
        }

        tp.setHeightSoFar(cumulativeYOffset);

        m_rowContainer.addUIElement(tp).inTL(-pad, 0);
        add(m_rowContainer).inTL(0, HEADER_HEIGHT + pad);
    }

    private class HeaderPanel extends CustomPanel<BasePanelPlugin<HeaderPanel>, HeaderPanel, CustomPanelAPI> 
        implements HasOutline, HasBackground, HasFader, HasAudioFeedback, HasFaction, HasActionListener,
        AcceptsActionListener
    {
        protected final ColumnManager column;
        public int listIndex = -1;

        private boolean isPersistentGlow = false;
        private final FaderUtil m_fader = new FaderUtil(0, 0, 0.2f, true, true);

        public HeaderPanel(UIPanelAPI parent, int width, int height, ColumnManager column, int listIndex) {
            super(parent, width, height, new BasePanelPlugin<>());
            this.column = column;
            this.listIndex = listIndex;

            createPanel();
            initializePlugin(hasPlugin);
        }

        @Override
        public void initializePlugin(boolean hasPlugin) {
            getPlugin().init(this);
            getPlugin().setTargetUIState(State.DIALOG);
        }

        @Override
        public void createPanel() {
            final LabelAPI lbl = Global.getSettings().createLabel(column.title, Fonts.ORBITRON_12);
            lbl.setColor(getFaction().getBaseUIColor());
            final float lblWidth = lbl.computeTextWidth(lbl.getText());
            final float lblHeight = lbl.computeTextHeight(lbl.getText());

            lbl.getPosition().inTL(
                (getPos().getWidth() / 2f) - (lblWidth / 2f),
                (getPos().getHeight() / 2f) - (lblHeight / 2f) 
            );
            add(lbl);

            if (showSortIcon) {
                final Base sortIcon = new Base(
                    getPanel(),
                    HEADER_HEIGHT - 2, HEADER_HEIGHT,
                    sortIconPath,
                    getFaction().getBaseUIColor(),
                    null,
                    false
                );
    
                add(sortIcon).inBR(1, 0);
            }
        }

        public Optional<HasActionListener> getActionListener() {
            return Optional.of(this);
        }

        public void onClicked(CustomPanel<?, ?, ?> source, boolean isLeftClick) {
            if (!sortingEnabled) return;
            
            SortableTable.this.sortRows(listIndex);
        }

        public FaderUtil getFader() {
            return m_fader;
        }

        public boolean isPersistentGlow() {
            return isPersistentGlow;
        }

        public void setPersistentGlow(boolean a) {
            isPersistentGlow = a;
        }

        public Color getGlowColor() {
            return Misc.getTooltipTitleAndLightHighlightColor();
        }

        public Color getOutlineColor() {
            return getFaction().getGridUIColor();
        }

        public Color getBgColor() {
            return new Color(0, 0, 0, 255);
        }

        public float getBgAlpha() {
            return 0.65f;
        }
    }

    public class HeaderPanelWithTooltip extends HeaderPanel implements HasTooltip {
        public HeaderPanelWithTooltip(UIPanelAPI parent, int width, int height,
            ColumnManager column, int listIndex) {
            super(parent, width, height, column, listIndex);
        }

        private boolean isExpanded = false;

        @Override
        public void initializePlugin(boolean hasPlugin) {
            super.initializePlugin(hasPlugin);
        }

        @Override
        public CustomPanelAPI getTpParent() {
            if (column.getTooltipType() == String.class) {
                return getParent();
            } else if (column.getTooltipType() == PendingTooltip.class) {
                return ((PendingTooltip<? extends CustomPanelAPI>) column.tooltip).parentSupplier.get();
            } else {
                throw new IllegalArgumentException(
                    "Tooltip for header '" + column.title + "' has an illegal type."
                );
            }
        }

        @Override
        public TooltipMakerAPI createAndAttachTp() {
            final TooltipMakerAPI tooltip;

            if (column.getTooltipType() == String.class) {
                tooltip = getParent().createUIElement(headerTooltipWidth, 0, false);
    
                tooltip.addPara((String) column.tooltip, pad);

            } else if (column.getTooltipType() == PendingTooltip.class) {
                tooltip = ((PendingTooltip<? extends UIPanelAPI>) column.tooltip).factory.get();

            } else {
                throw new IllegalArgumentException(
                    "Tooltip for header '" + column.title + "' has an illegal type."
                );
            }

            getParent().addUIElement(tooltip);
            WrapUiUtils.anchorPanelWithBounds(tooltip, getPanel(), AnchorType.TopLeft, 0);

            return tooltip;
        }

        @Override
        public boolean isExpanded() {
            return isExpanded;
        }

        @Override
        public void setExpanded(boolean a) {
            isExpanded = a;
        }
    }

    public class ColumnManager {

        public String title;
        public int width;
        public Object tooltip;
        
        private HeaderPanel headerPanel = null;
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

        public HeaderPanel getHeaderPanel() {
            return headerPanel;
        }

        public void setHeaderPanel(HeaderPanel a) {
            headerPanel = a;
        }
    }

    public class RowManager extends CustomPanel<BasePanelPlugin<RowManager>, RowManager, CustomPanelAPI> 
        implements HasTooltip, HasFader, HasOutline, HasAudioFeedback, HasActionListener, AcceptsActionListener
    {
        public Color textColor = Misc.getBasePlayerColor();
        public PendingTooltip<? extends UIPanelAPI> m_tooltip = null;
        public CallbackRunnable<RowManager> onRowClicked = null;
        public Object customData = null;
        
        protected final List<Object> m_cellData = new ArrayList<>();
        protected final List<cellAlg> m_cellAlignment = new ArrayList<>();
        protected final List<Object> m_sortValues = new ArrayList<>();
        protected final List<Color> m_useColor = new ArrayList<>();
        protected String codexID = null;

        private final FaderUtil m_fader;
        private boolean isPersistentGlow = false;
        private Color glowColor = Misc.getDarkPlayerColor();
        private Outline outline = Outline.NONE;
        private Color outlineColor = Misc.getDarkPlayerColor();

        public RowManager(UIPanelAPI parent, int width, int height) {
            super(parent, width, height, new BasePanelPlugin<>());

            m_fader = new FaderUtil(0, 0, 0.2f, true, true);

            initializePlugin(hasPlugin);
        }

        public void initializePlugin(boolean hasPlugin) {
            getPlugin().init(this);
            getPlugin().setTargetUIState(State.DIALOG);
        }

        public void createPanel() {

            CustomPanelAPI rowPanel = SortableTable.RowManager.this.getPanel();
            int cumulativeXOffset = 0;

            for (int i = 0; i < m_cellData.size(); i++) {
                Object cell = m_cellData.get(i);
                cellAlg alignment = m_cellAlignment.get(i);
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

                } else if (cell instanceof String txt) {
                    LabelAPI label = Global.getSettings().createLabel(txt, Fonts.DEFAULT_SMALL);
                    comp = (UIComponentAPI) label;
                    compWidth = label.computeTextWidth(label.getText());
                    compHeight = label.computeTextHeight(label.getText());

                    if (useColor != null) {
                        label.setColor(useColor);
                    } else {
                        label.setColor(textColor);
                    }

                } else if (cell instanceof SpritePanel sprite) {
                    comp = (UIComponentAPI) sprite.getPanel();
                    compWidth = sprite.getPos().getWidth();
                    compHeight = sprite.getPos().getHeight();

                } else if (cell instanceof UIPanelAPI panel) {
                    comp = panel;
                    compWidth = panel.getPosition().getWidth();
                    compHeight = panel.getPosition().getHeight();

                } else if (cell instanceof LabelAPI label) {
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
                float yOffset = (ROW_HEIGHT/2) - (compHeight/2);
                rowPanel.addComponent(comp).inBL(xOffset, yOffset);

                cumulativeXOffset += colWidth;
            }
        }

        private float calcXOffset(float baseX, float colWidth, float compWidth, cellAlg alignment) {
            final int pad = 3; // define pad somewhere appropriate

            switch (alignment) {
                case LEFT:
                    return baseX;
                case RIGHT:
                    return baseX + colWidth - compWidth;
                case LEFTPAD:
                    return baseX + pad;
                case LEFTOPAD:
                    return baseX + opad;
                case RIGHTPAD:
                    return baseX + colWidth - compWidth - pad;
                case RIGHTOPAD:
                    return baseX + colWidth - compWidth - opad;
                case MID:
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

        public FaderUtil getFader() {
            return m_fader;
        }

        public Optional<HasActionListener> getActionListener() {
            return Optional.of(this);
        }

        public void onClicked(CustomPanel<?, ?, ?> source, boolean isLeftClick) {
            SortableTable table = SortableTable.this;
            table.selectRow(this);
            m_selectedRow = this;
            if (onRowClicked != null) onRowClicked.run(this);
        }

        public Glow getGlowType() {
            return Glow.UNDERLAY;
        }

        public boolean isPersistentGlow() {
            return isPersistentGlow;
        }

        public void setPersistentGlow(boolean a) {
            isPersistentGlow = a;
        }

        public Color getGlowColor() {
            return glowColor;
        }

        public void setGlowColor(Color a) {
            glowColor = a;
        }

        public void setOutline(Outline a) {
            outline = a;
        }

        public Outline getOutline() {
            return outline;
        }

        public Color getOutlineColor() {
            return outlineColor;
        }

        public void setOutlineColor(Color color) {
            outlineColor = color;
        }

        public CustomPanelAPI getTpParent() {
            if (m_tooltip == null) {
                return getParent();
            } else {
                return m_tooltip.parentSupplier.get();
            }
        }
        
        public TooltipMakerAPI createAndAttachTp() {
            if (m_tooltip == null) {
                // Invisible header
                return getParent().createUIElement(0, 0, false);
            }

            TooltipMakerAPI tooltip = m_tooltip.factory.get();

            (SortableTable.this.getPanel()).addUIElement(tooltip);
            WrapUiUtils.mouseCornerPos(tooltip, opad);

            if (codexID != null) {
                tooltip.setCodexEntryId(codexID);
                WrapUiUtils.positionCodexLabel(tooltip, opad, pad);
            }

            return tooltip;
        }

        public void addCell(Object cell, cellAlg alg, Object sort, Color textColor) {
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
     * tooltip or a {@link PendingTooltip}, whether if it is merged, if it is the parent and the ID of the mergeSet.
     * A merged non-parent header will not display a tooltip.
     * <br></br> The expected input is {String, int, String, bool, bool, int}.
     * Or alternatively {String, int, {@link PendingTooltip}, bool, bool, int}.
     * The tooltip and mergeSetID can be left empty:
     * {String, int, null, bool, bool, null}.
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
     * Supports the following types:
     * String, LabelAPI, LtvSpritePanel, UIPanelAPI, CustomPanelAPI
     */
    public void addCell(Object cell, cellAlg alg, Object sortValue, Color textColor) {
        if (pendingRow == null) {
            pendingRow = new RowManager(
                getParent(),
                (int) getPos().getWidth() - 2,
                ROW_HEIGHT
            );
        }

        pendingRow.addCell(cell, alg, sortValue, textColor);
    }

    /**
     * Uses the added cells to create a row and clears the {@code pendingRow}.
     * The amount of cells must match the column amount.
     * @param codexID optional.
     * @param customData stored by RowManager instance.
     * @param textColor sets all the cells to that color.
     * @param highlight optional.
     * @param tp optional.
     * @param onRowClicked gets called when the row is clicked.
     */
    public <TpType extends CustomPanelAPI> void pushRow(String codexID, Object customData, Color textColor,
        Color highlight, PendingTooltip<TpType> tp, CallbackRunnable<RowManager> onRowClicked
    ) {
        if (pendingRow == null || pendingRow.m_cellData.isEmpty()) {
            throw new IllegalStateException("Cannot push row: no cells have been added yet. "
                    + "Call addCell() before pushRow().");

        } else if (pendingRow.m_cellData.size() != m_columns.size()) {
            throw new IllegalStateException("Cannot push row: cell count mismatch. "
                    + "The number of cells must match the number of columns.");

        }
        pendingRow.setCodexId(codexID);
        pendingRow.setTextColor(textColor);
        pendingRow.customData = customData;
        if (highlight != null) pendingRow.setGlowColor(highlight);
        if (onRowClicked != null) pendingRow.onRowClicked = onRowClicked;

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

    public RowManager selectLastRow() {
        RowManager target = null;
        for (RowManager row : m_rows) {
            boolean result = row == m_rows.get(m_rows.size() - 1);
            row.setPersistentGlow(result);

            if (result) {
                target = row;
            }
        }

        return target;
    }

    public void selectRow(RowManager selectedRow) {
        for (RowManager row : m_rows) {
            row.setPersistentGlow(row == selectedRow);
        }
    }

    /**
     * Determines how the panels inside a cell are positioned
     */
    public enum cellAlg {
        LEFT,
        MID,
        RIGHT,
        LEFTPAD,
        LEFTOPAD,
        RIGHTPAD,
        RIGHTOPAD
    }
}

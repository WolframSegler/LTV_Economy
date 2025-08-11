package wfg_ltv_econ.ui.panels;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.ui.CustomPanelAPI;
import com.fs.starfarer.api.ui.Fonts;
import com.fs.starfarer.api.ui.LabelAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.ui.UIComponentAPI;
import com.fs.starfarer.api.ui.UIPanelAPI;
import com.fs.starfarer.api.util.FaderUtil;
import com.fs.starfarer.api.util.Misc;

import wfg_ltv_econ.ui.LtvUIState.UIState;
import wfg_ltv_econ.ui.panels.LtvCustomPanel.HasTooltip.PendingTooltip;
import wfg_ltv_econ.ui.panels.components.FaderComponent.Glow;
import wfg_ltv_econ.ui.panels.components.OutlineComponent.Outline;
import wfg_ltv_econ.ui.panels.LtvSpritePanel.Base;
import wfg_ltv_econ.ui.plugins.BasePanelPlugin;
import wfg_ltv_econ.ui.plugins.LtvSpritePanelPlugin;
import wfg_ltv_econ.util.TooltipUtils;
import wfg_ltv_econ.util.UiUtils;

/**
 * SortableTable is a customizable, sortable UI table component designed to display
 * complex tabular data within a custom panel environment. It extends {@link LtvCustomPanel}
 * to integrate smoothly with the overall UI framework.
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
 *   <li>{@link LtvSpritePanel} — displayed as a sprite panel UI component.</li>
 *   <li>{@link UIPanelAPI} — displayed directly as a UI panel component.</li>
 *   <li>{@link LabelAPI} — displayed as a label UI component with optional color.</li>
 * </ul>
 * Passing any other cell type will result in an {@link IllegalArgumentException}.
 * </p>
 * <b>Typical usage example:</b>
 * <pre>{@code
 * SortableTable table = new SortableTable(root, parentPanel, width, height, market, headerH, rowH);
 *
 * // Setup headers with labels, widths, tooltips, merge flags and merge group
 * table.addHeaders(
 *     "", 40, null, true, false, 1,
 *     "Colony", 200, "Colony name", true, true, 1,
 *     "Size", 100, "Colony size", false, false, -1,
 *     "Faction", 150, "Controlling faction", false, false, -1,
 *     // etc.
 * );
 *
 * // Add rows with multiple typed cells and associated tooltips
 * table.addCell(iconPanel, Alignment.LEFT, null, null);
 * table.addCell("Colony Name", Alignment.LEFT, null, textColor);
 * table.addCell(5, Alignment.CENTER, null, textColor);
 * // ...
 * table.pushRow(codexID, market, null, highlightColor, tooltip);
 *
 * // Add table to UI panel and initialize it
 * parentPanel.addComponent(table.getPanel()).inTL(0, 0);
 * table.createPanel();
 *
 * // Enable sorting by a particular column index
 * table.sortRows(columnIndex);
 *
 * // Register a listener for row selection
 * table.setRowSelectionListener(selectedRow -> {
 *     // Handle selection change
 * });
 * }</pre>
 * <p>
 * This component is designed to be flexible for various data types and dynamic updates.
 * It supports tooltips both for headers and rows via {@link PendingTooltip}.
 * <p>
 * The class is tightly integrated with the {@code LtvCustomPanel} and the broader UI framework,
 * allowing custom rendering, plugin integration, and seamless UI event handling.
 */
public class SortableTable extends LtvCustomPanel<BasePanelPlugin<SortableTable>, SortableTable, CustomPanelAPI> {
    private final List<ColumnManager> m_columns = new ArrayList<>();
    private final List<RowManager> m_rows = new ArrayList<>();

    private RowManager pendingRow = null;

    private final int m_headerHeight;
    private final int m_rowHeight;

    private int prevSelectedSortColumnIndex = -1;
    private int selectedSortColumnIndex = -1;
    private boolean ascending = true;

    RowSelectionListener selectionListener;

    private RowManager m_selectedRow;

    private CustomPanelAPI m_headerContainer = null;
    private CustomPanelAPI m_rowContainer = null;

    public interface RowSelectionListener {
        void onRowSelected(RowManager selectedRow);
    }

    public void setRowSelectionListener(RowSelectionListener a) {
        selectionListener = a;
    }

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
            getPos().getWidth(),
            m_headerHeight,
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
                        getRoot(), getPanel(), mergedWidth - pad, m_headerHeight, getMarket(), column, i
                    );
                } else {
                    panel = new HeaderPanelWithTooltip(
                        getRoot(), getPanel(), mergedWidth - pad, m_headerHeight, getMarket(), column, i
                    );
                }

                m_headerContainer.addComponent(panel.getPanel()).inTL(cumulativeXOffset, 0);

                cumulativeXOffset += mergedWidth;

            // Standalone header
            } else {
                if (column.tooltip == null) {
                    panel = new HeaderPanel(
                        getRoot(), getPanel(), column.width + lastHeaderPad - pad, m_headerHeight,
                        getMarket(), column, i
                    );
                } else {
                    panel = new HeaderPanelWithTooltip(
                        getRoot(), getPanel(), column.width + lastHeaderPad - pad, m_headerHeight,
                        getMarket(), column, i
                    );
                }

                m_headerContainer.addComponent(panel.getPanel()).inTL(cumulativeXOffset, 0);

                cumulativeXOffset += column.width;
            }

            column.setHeaderPanel(panel);
        }

        getPanel().addComponent(m_headerContainer).inTL(0,0);

        // Create rows
        m_rowContainer = Global.getSettings().createCustom(
            getPos().getWidth(),
            getPos().getHeight() - (m_headerHeight + pad),
            null
        );

        TooltipMakerAPI tp = m_rowContainer.createUIElement(
            getPos().getWidth() + pad,
            getPos().getHeight() - (m_headerHeight + pad),
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

    private class HeaderPanel extends LtvCustomPanel<BasePanelPlugin<HeaderPanel>, HeaderPanel, CustomPanelAPI> 
        implements HasOutline, HasBackground, HasFader, HasAudioFeedback {
        protected final ColumnManager column;
        public int listIndex = -1;

        private boolean isPersistentGlow = false;
        private FaderUtil m_fader = null;

        public HeaderPanel(UIPanelAPI root, UIPanelAPI parent, int width, int height, MarketAPI market,
                ColumnManager column, int listIndex) {
            super(root, parent, width, height, new BasePanelPlugin<>() {
                @Override
                public void advance(float amount) {
                    super.advance(amount);

                    if (inputSnapshot.LMBUpLastFrame && inputSnapshot.hasClickedBefore) {
                        SortableTable.this.sortRows(getPanel().listIndex);
                    }
                }
            }, market);
            this.column = column;
            this.listIndex = listIndex;
            m_fader = new FaderUtil(0, 0, 0.2f, true, true);

            createPanel();
            initializePlugin(hasPlugin);
        }

        @Override
        public void initializePlugin(boolean hasPlugin) {
            getPlugin().init(this);
            getPlugin().setTargetUIState(UIState.DETAIL_DIALOG);
        }

        @Override
        public void createPanel() {
            CustomPanelAPI panel = SortableTable.HeaderPanel.this.getPanel();
            TooltipMakerAPI tooltip = panel.createUIElement(
                    getPos().getWidth(), getPos().getHeight(), false);

            tooltip.setParaFontColor(getMarket().getFaction().getBaseUIColor());
            tooltip.setParaFont(Fonts.ORBITRON_12);

            LabelAPI lbl = tooltip.addPara(column.title, pad);
            final float lblWidth = lbl.computeTextWidth(lbl.getText());
            final float lblHeight = lbl.computeTextHeight(lbl.getText());

            lbl.getPosition().inTL(
                (getPos().getWidth() / 2f) - (lblWidth / 2f),
                (getPos().getHeight() / 2f) - (lblHeight / 2f) 
            );

            LtvSpritePanel.Base sortIcon = new Base(
                    getRoot(),
                    panel,
                    getMarket(),
                    m_headerHeight - 2, m_headerHeight,
                    new LtvSpritePanelPlugin<>(),
                    sortIconPath,
                    getFaction().getBaseUIColor(),
                    null,
                    false);

            tooltip.addComponent(sortIcon.getPanel()).inBR(1, 0);

            panel.addUIElement(tooltip).inBL(0, 0);
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

        public float getBgTransparency() {
            return 0.65f;
        }
    }

    public class HeaderPanelWithTooltip extends HeaderPanel implements HasTooltip {
        public HeaderPanelWithTooltip(UIPanelAPI root, UIPanelAPI parent, int width, int height,
                MarketAPI market, ColumnManager column, int listIndex) {
            super(root, parent, width, height, market, column, listIndex);
        }

        private boolean isExpanded = false;

        @Override
        public void initializePlugin(boolean hasPlugin) {
            super.initializePlugin(hasPlugin);
        }

        @Override
        public UIPanelAPI getTooltipParent() {
            if (column.getTooltipType() == String.class) {
                return getParent();
            } else if (column.getTooltipType() == PendingTooltip.class) {
                return ((PendingTooltip) column.tooltip).getTooltipParent();
            } else {
                throw new IllegalArgumentException(
                    "Tooltip for header '" + column.title + "' has an illegal type."
                );
            }
        }

        @Override
        public TooltipMakerAPI createAndAttachTooltip() {
            final TooltipMakerAPI tooltip;

            if (column.getTooltipType() == String.class) {
                tooltip = getParent().createUIElement(
                        headerTooltipWidth, 0, false);
    
                tooltip.addPara((String) column.tooltip, pad);

            } else if (column.getTooltipType() == PendingTooltip.class) {
                tooltip = ((PendingTooltip) column.tooltip).factory.get();

            } else {
                throw new IllegalArgumentException(
                    "Tooltip for header '" + column.title + "' has an illegal type."
                );
            }

            getParent().addUIElement(tooltip);
            getParent().bringComponentToTop(tooltip);
            TooltipUtils.dynamicPos(tooltip, getPanel(), opad);

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

    public class RowManager extends LtvCustomPanel<BasePanelPlugin<RowManager>, RowManager, CustomPanelAPI> 
        implements HasTooltip, HasFader, HasOutline, HasAudioFeedback {

        protected final List<Object> m_cellData = new ArrayList<>();
        protected final List<cellAlg> m_cellAlignment = new ArrayList<>();
        protected final List<Object> m_sortValues = new ArrayList<>();
        protected final List<Color> m_useColor = new ArrayList<>();
        protected String codexID = null;
        
        public Color textColor = Misc.getBasePlayerColor();
        public PendingTooltip m_tooltip = null;
        
        private FaderUtil m_fader = null;
        private boolean isPersistentGlow = false;
        private Color glowColor = Misc.getDarkPlayerColor();
        private Outline outline = Outline.NONE;
        private Color outlineColor = Misc.getDarkPlayerColor();

        public RowManager(UIPanelAPI root, UIPanelAPI parent, int width, int height, MarketAPI market,
                RowSelectionListener listener) {
            super(root, parent, width, height, new BasePanelPlugin<>() {
                
                @Override
                public void advance(float amount) {
                    super.advance(amount);

                    if (inputSnapshot.LMBUpLastFrame && inputSnapshot.hasClickedBefore) {
                        SortableTable table = SortableTable.this;
                        table.selectRow(m_panel);

                        if (table.selectionListener != null) {
                            table.selectionListener.onRowSelected(m_panel);
                        }

                        inputSnapshot.hasClickedBefore = false;
                    }
                }
            }, market);

            m_fader = new FaderUtil(0, 0, 0.2f, true, true);

            initializePlugin(hasPlugin);
        }

        public void initializePlugin(boolean hasPlugin) {
            getPlugin().init(this);
            getPlugin().setTargetUIState(UIState.DETAIL_DIALOG);
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

                } else if (cell instanceof LtvSpritePanel sprite) {
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
                float yOffset = (m_rowHeight/2) - (compHeight/2);
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
                case RIGHTPAD:
                    return baseX + colWidth - compWidth - pad;
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

        @Override
        public FaderUtil getFader() {
            return m_fader;
        }

        @Override
        public Glow getGlowType() {
            return Glow.UNDERLAY;
        }

        @Override
        public boolean isPersistentGlow() {
            return isPersistentGlow;
        }

        @Override
        public void setPersistentGlow(boolean a) {
            isPersistentGlow = a;
        }

        @Override
        public Color getGlowColor() {
            return glowColor;
        }

        @Override
        public void setGlowColor(Color a) {
            glowColor = a;
        }

        @Override
        public void setOutline(Outline a) {
            outline = a;
        }

        @Override
        public Outline getOutline() {
            return outline;
        }

        @Override
        public Color getOutlineColor() {
            return outlineColor;
        }

        @Override
        public void setOutlineColor(Color color) {
            outlineColor = color;
        }

        @Override
        public UIPanelAPI getTooltipParent() {
            if (m_tooltip == null) {
                return getParent();
            } else {
                return m_tooltip.getTooltipParent();
            }
        }
        
        @Override
        public TooltipMakerAPI createAndAttachTooltip() {
            if (m_tooltip == null) {
                // Invisible header
                return getParent().createUIElement(headerTooltipWidth, 0, false);
            }

            TooltipMakerAPI tooltip = m_tooltip.factory.get();

            (SortableTable.this.getPanel()).addUIElement(tooltip);
            (SortableTable.this.getPanel()).bringComponentToTop(tooltip);
            TooltipUtils.mouseCornerPos(tooltip, opad);

            if (codexID != null) {
                tooltip.setCodexEntryId(codexID);
                UiUtils.positionCodexLabel(tooltip, opad, pad);
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
     * tooltip or a PendingTooltip, whether if it is merged, if it is the parent and the ID of the mergeSet.
     * A merged non-parent header will not display a tooltip.
     * <br></br> The expected input is {String, int, String, bool, bool, int}.
     * Or alternatively {String, int, PendingTooltip, bool, bool, int}.
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
                    getRoot(),
                    getParent(),
                    (int) getPos().getWidth() - 2,
                    m_rowHeight,
                    getMarket(),
                    new RowSelectionListener() {
                        @Override
                        public void onRowSelected(RowManager row) {
                            m_selectedRow = row;
                        }
                    });
        }

        pendingRow.addCell(cell, alg, sortValue, textColor);
    }

    /**
     * Uses the added cells to create a row and clears the {@code pendingRow}.
     * The amount of cells must match the column amount.
     * The {@code textColor} sets all the cells to that color.
     * The {@code market} is used for certain colors and location info.
     * {@code glowClr} can be null.
     * {@code codexID} is optional.
     * The {@code PendingTooltip} can be null.
     */
    public void pushRow(String codexID, MarketAPI market, Color textColor, Color glowClr, PendingTooltip tp) {
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
        if (getMarket() == market) {
            pendingRow.setOutline(Outline.TEX_THIN);
            pendingRow.setOutlineColor(getFaction().getBaseUIColor());
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
        RIGHTPAD,
    }
}

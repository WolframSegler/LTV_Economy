package wfg_ltv_econ.ui;

import java.util.ArrayList;
import java.util.List;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.graphics.SpriteAPI;

public class SortableTable {
    private static SpriteAPI sortIcon;
    {
        sortIcon = Global.getSettings().getSprite("ui", "sortIcon");
    }

    private final int m_width;
    private final int m_height;

    private List<ColumnManager> columns = new ArrayList<>();
    private List<RowManager> rows = new ArrayList<>();

    private int m_headerHeight = 0;
    private int m_rowHeight = 0;

    private String currentSortColumn = null;
    private boolean ascending = true;

    private String selectedRowID = null;

    public SortableTable(int width, int height) {
        this(width, height, 50, 25);
    }

    public SortableTable(int width, int height, int headerHeight, int rowHeight) {
        m_width = width;
        m_height = height;
        m_headerHeight = headerHeight;
        m_rowHeight = rowHeight;
    }

    public List<ColumnManager> getColumns() {
        return columns;
    }

    public List<RowManager> getRows() {
        return rows;
    }

    public static class ColumnManager {
        public String name;
        public float width;

        public ColumnManager(String name, float width) {
            this.name = name;
            this.width = width;
        }
    }

    public static class RowManager {
        private static int NEXT_ID = 0;

        public String id;
        public LtvCustomPanel m_row;

        public RowManager(LtvCustomPanel row) {
            m_row = row;
            id = "row_" + (NEXT_ID++);
        }

        public void setRow(LtvCustomPanel a) {
            m_row = a;
        }
    }

    /**
     * Each set must contain the title of the header and its width
     * The types are String and int
     */
    public void addHeaders(Object... headers) {
        columns.clear();
        if (headers.length % 2 != 0) {
            throw new IllegalArgumentException("Headers must be pairs of {String, int}");
        }
        for (int i = 0; i < headers.length; i += 2) {
            columns.add(new ColumnManager((String) headers[i], ((Number) headers[i + 1]).intValue()));
        }
    }

    public void addRow(LtvCustomPanel row) {
        rows.add(row);
    }

    // public void sortByColumn(String columnName) {
    //     if (columnName.equals(currentSortColumn)) {
    //         ascending = !ascending; // toggle
    //     } else {
    //         currentSortColumn = columnName;
    //         ascending = true;
    //     }
    //     rows.sort((r1, r2) -> {
    //         Comparable v1 = r1.getSortValue(currentSortColumn);
    //         Comparable v2 = r2.getSortValue(currentSortColumn);
    //         int cmp = v1.compareTo(v2);
    //         return ascending ? cmp : -cmp;
    //     });
    // }

}

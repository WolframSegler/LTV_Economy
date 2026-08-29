package wfg.ltv_econ.ui;

import java.awt.Color;
import java.util.List;

import com.fs.starfarer.api.combat.MutableStat;
import com.fs.starfarer.api.combat.StatBonus;
import com.fs.starfarer.api.ui.LabelAPI;
import com.fs.starfarer.api.ui.UIPanelAPI;

// TODO delete this
public interface ModGridAPI extends UIPanelAPI {

    public interface GridRowAPI extends UIPanelAPI {
        LabelAPI getNameLabel();
        LabelAPI getValueLabel();
    }

    /** The font used by the value {@link LabelAPI}. */
    String getValueFont();
    /** The font used by the value {@link LabelAPI}. Must be set before adding rows. */
    void setValueFont(String font);
    /** The font used by the name {@link LabelAPI}. */
    String getNameFont();
    /** The font used by the name {@link LabelAPI}. Must be set before adding rows. */
    void setNameFont(String font);

    /**
     * Flips the value and name labels such that the value label is on the left.
     * @param valueWidth the width of the value label.
     * @param gap the gap between the value and the name label.
     * @implNote the width of the name label is <code>itemWidth - valueWidth - gap</code>. 
     */
    void setFlipped(float valueWidth, float gap);

    /** Uses row height and gap height to compute the final height. */
    float computeHeight(int rowCount);
    /** Uses {@link #computeHeight} to update height. */
    void autoSizeToRows(int rowCount);

    /** An item is made up of name label and value label. */
    float getItemWidth();
    /** The unit label width. The unit label is not used if its width is {@code 0f}.  */
    float getUnitsWidth();
    float getRowHeight();

    /** Updates the value label text of the specified row.  */
    GridRowAPI updateRowText(int x, int y, String text);
    /** Gets the row at the specified coordinates, null if it doesn't exist. */
    GridRowAPI getRow(int x, int y);

    /** Horizontal padding. */
    float getHpad();
    /** Horizontal padding. */
    void setHpad(float pad);

    /** The number of Anti Aliasing passes. */
    void setNumAA(int count);
    /** The rendered text scale increases by this amount each pass for AA. */
    void setAaIncr(float increment);
    /** All labels use this font size. Default is <code>18f</code>. */
    void setFontSize(float size);

    /** <code>setNumAA(3), setAaIncr(0.125f), setFontSize(18f)</code>.  */
    void setFontConfigForBarFont();
    /** <code>setNumAA(2), setAaIncr(0.125f), setFontSize(18f)</code>.  */
    void setFontConfigForSmallInsignia();

    void setNameColor(Color color);
    void setValueColor(Color color);
    void setUnitsColor(Color color);

    /**
     * @param x the horizontal position starting from 0. This value is multiplied by <code>itemWidth + HPAD</code>.
     * @param y the vertical position starting from 0. This value is multiplied by <code>rowHeight + VPAD</code>.
     * @param nameText the text of the name label.
     * @param valueText the text of the value label.
     */
    GridRowAPI add(int x, int y, String nameText, String valueText);
    /**
     * @param x the horizontal position starting from 0. This value is multiplied by <code>itemWidth + HPAD</code>.
     * @param y the vertical position starting from 0. This value is multiplied by <code>rowHeight + VPAD</code>.
     * @param nameText the text of the name label.
     * @param valueText the text of the value label.
     * @param unitText the text of the unit label. It is only shown when <code>unitsWidth</code> is not 0f.
     */
    GridRowAPI add(int x, int y, String nameText, String valueText, String unitText);
    /**
     * @param x the horizontal position starting from 0. This value is multiplied by <code>itemWidth + HPAD</code>.
     * @param y the vertical position starting from 0. This value is multiplied by <code>rowHeight + VPAD</code>.
     * @param w multiplied with <code>itemWidth</code> and <code>HPAD - 1</code> to have a row with custom width.
     * @param h multiplied with <code>rowHeight</code> and <code>VPAD - 1</code> to have a row with custom height.
     * @param nameText the text of the name label.
     * @param valueText the text of the value label.
     * @param unitText the text of the unit label. It is only shown when <code>unitsWidth</code> is not 0f.
     */
    GridRowAPI add(int x, int y, int w, int h, String nameText, String valueText, String unitText);

    /**
     * @param x the horizontal position starting from 0. This value is multiplied by <code>itemWidth + HPAD</code>.
     * @param y the vertical position starting from 0. This value is multiplied by <code>rowHeight + VPAD</code>.
     * @param nameText the text of the name label.
     * @param valueText the text of the value label.
     */
    GridRowAPI addOrUpdate(int x, int y, String nameText, String valueText);

    /**
     * @param x the horizontal position starting from 0. This value is multiplied by <code>itemWidth + HPAD</code>.
     * @param y the vertical position starting from 0. This value is multiplied by <code>rowHeight + VPAD</code>.
     * @param w multiplied with <code>itemWidth</code> and <code>HPAD - 1</code> to have a row with custom width.
     * @param h multiplied with <code>rowHeight</code> and <code>VPAD - 1</code> to have a row with custom height.
     * @param nameText the text of the name label.
     * @param valueText the text of the value label.
     */
    GridRowAPI addOrUpdate(int x, int y, int w, int h, String nameText, String valueText);

    /**
     * @param x the horizontal position starting from 0. This value is multiplied by <code>itemWidth + HPAD</code>.
     * @param y the vertical position starting from 0. This value is multiplied by <code>rowHeight + VPAD</code>.
     * @param nameText the text of the name label.
     * @param stat the stat bonus whose value is to be displayed.
     * @param baseValue used to compute the effective value of {@code stat}.
     */
    GridRowAPI add(int x, int y, String nameText, StatBonus stat, float baseValue);
    /**
     * @param x the horizontal position starting from 0. This value is multiplied by <code>itemWidth + HPAD</code>.
     * @param y the vertical position starting from 0. This value is multiplied by <code>rowHeight + VPAD</code>.
     * @param nameText the text of the name label.
     * @param stat the stat bonus whose value is to be displayed.
     * @param baseValue used to compute the effective value of {@code stat}.
     * @param decimalFormatting if {@code true}, intelligently truncates the decimal places. If {@code false}, rounds to the nearest {@code int}.
     * @param showPercent if {@code true}, appends a "%" {@code char} as a suffix to the value.
     * @param invertColor if {@code true}, positive stats get negative color, negative stats get positive color.
     */
    GridRowAPI add(int x, int y, String nameText, StatBonus stat, float baseValue, boolean decimalFormatting, boolean showPercent, boolean invertColor);

    /**
     * @param x the horizontal position starting from 0. This value is multiplied by <code>itemWidth + HPAD</code>.
     * @param y the vertical position starting from 0. This value is multiplied by <code>rowHeight + VPAD</code>.
     * @param nameText the text of the name label.
     * @param valueText the text of the value label.
     * @param valueColor the value label color.
     */
    GridRowAPI add(int x, int y, String nameText, String valueText, Color valueColor);

    /**
     * @param x the horizontal position starting from 0. This value is multiplied by <code>itemWidth + HPAD</code>.
     * @param y the vertical position starting from 0. This value is multiplied by <code>rowHeight + VPAD</code>.
     * @param nameText the text of the name label.
     * @param valueText the text of the value label.
     * @param stat the mutable stat whose value is to be displayed.
     * @param showPercent if {@code true}, appends a "%" {@code char} as a suffix to the value.
     * @param invertColor if {@code true}, positive stats get negative color, negative stats get positive color.
     */
    GridRowAPI add(int x, int y, String nameText, String valueText, MutableStat stat, boolean showPercent, boolean invertColor);

    /**
     * @param x the horizontal position starting from 0. This value is multiplied by <code>itemWidth + HPAD</code>.
     * @param y the vertical position starting from 0. This value is multiplied by <code>rowHeight + VPAD</code>.
     */
    void removeRow(int x, int y);
    void clearRows();

    int getRowCount();
    List<GridRowAPI> getRows();

    /**
     * @param x the horizontal position starting from 0. This value is multiplied by <code>itemWidth + HPAD</code>.
     * @param y the vertical position starting from 0. This value is multiplied by <code>rowHeight + VPAD</code>.
     * @param valueColor the color of the value label.
     * @implNote throws {@link NullPointerException} if no such row exists.
     */
    void updateColor(int x, int y, Color valueColor);
}
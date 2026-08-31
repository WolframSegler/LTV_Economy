package wfg.ltv_econ.ui;

import java.util.List;

import com.fs.starfarer.api.campaign.econ.CommodityOnMarketAPI;
import com.fs.starfarer.api.campaign.econ.CommoditySpecAPI;
import com.fs.starfarer.api.ui.IconRenderMode;
import com.fs.starfarer.api.ui.UIPanelAPI;

// TODO remove this
public interface IconGroupAPI extends UIPanelAPI {

    public interface GroupElementAPI extends UIPanelAPI {
        int getIconCount();
        void setIconCount(int count);
        CommoditySpecAPI getCommodity();
        IconRenderMode getRenderMode();
        float getLastIconScale();
        Object getCustomData();
    }

    float computeRowHeightFor(float width, float maxHeight, float maxIconHeight);
    void autoSizeTo(float iconSize, float width);
    void autoSizeWithAdjust(float iconSize, float width, float maxHeight, float maxIconSize);

    void addIconGroup(String commodityId, IconRenderMode mode, int count, Object custom);
    void addGroup(CommodityOnMarketAPI com, int count, float iconScale, IconRenderMode mode, Object custom);
    void addGroup(CommoditySpecAPI spec, int count, float iconScale, IconRenderMode mode, Object custom);

    boolean isEmpty();
    void clearGroups();
    /** Has one virtual icon count between each group. */
    float getTotalIconsAndSpaces();
    /** The amount of rows (not groups) after a layout calculation. */
    float getNumRows();

    List<GroupElementAPI> getGroups();

    boolean isWideSpacing();
    boolean isMediumSpacing();
    void setWideSpacing(boolean bool);
    void setMediumSpacing(boolean bool);

    boolean isHighlightOnMouseover();
    void setHighlightOnMouseover(boolean bool);
}
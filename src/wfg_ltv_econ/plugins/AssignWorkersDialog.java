package wfg_ltv_econ.plugins;

import com.fs.starfarer.api.ui.CustomPanelAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.ui.UIPanelAPI;
import com.fs.starfarer.api.ui.ValueDisplayMode;
import com.fs.starfarer.api.util.Misc;

import wfg_ltv_econ.industry.LtvBaseIndustry;
import wfg_ltv_econ.util.ReflectionUtils;

import java.awt.Color;
import java.util.List;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CustomDialogDelegate;
import com.fs.starfarer.api.campaign.CustomUIPanelPlugin;
import com.fs.starfarer.api.campaign.econ.Industry;
import com.fs.starfarer.ui.impl.Q;
import com.fs.starfarer.campaign.ui.N; //Current slider class (v.0.98 R8). Do not use directly
import io.github.classgraph.ClassGraph;
import io.github.classgraph.ScanResult;

public class AssignWorkersDialog implements CustomDialogDelegate {

    private final Industry industry;
    private int assignedWorkers;
    private int maxWorkers;
    private AssignWorkersDialoga dialog = new AssignWorkersDialoga();
    Q slidera;

    public AssignWorkersDialog(Industry industry) {
        this.industry = industry;
        this.assignedWorkers = ((LtvBaseIndustry) industry).getWorkerAssigned();
        this.maxWorkers = ((LtvBaseIndustry) industry).getWorkerCap();
    }

    @Override
    public void createCustomDialog(CustomPanelAPI panel, CustomDialogCallback callback) {
        // Create the slider
        Class<?> sliderClass = findSlider();
        if (sliderClass == null) {
            return;
        }

        // public N(String LabelText, float MinValue, float MaxValue)
        Object slider = ReflectionUtils.getConstructorsMatching(sliderClass).get(0).newInstance("Workers", (float)0, (float)maxWorkers);

        TooltipMakerAPI ui = panel.createUIElement(500f, 300f, true);

        ui.addPara("Assign workers to: " + industry.getCurrentName(), 10f);

        // slidera = new Q("Number of Workers", Misc.getBasePlayerColor(), 50, 100);

        // slidera.setMode(ValueDisplayMode.PERCENT);
        // slidera.getBar().setBarColor(Color.WHITE);
        // slidera.getBar().setHighlightOnMouseover(true);
        // slidera.getBar().setUserAdjustable(true);
        // slidera.getBar().setRangeMin(0f);
        // slidera.getBar().setRangeMax(100f);
        // slidera.getBar().forceSync();
        // slidera.getPosition().setSize(100, 15);

        // Optional: Set default value
        // slidera.getBar().setProgress(5f);

        // Add to panel
        // panel.addComponent(slidera).inTL(100f, 180f);
        // the slider class implements UIPanelAPI
        panel.addComponent((UIPanelAPI)slider).inTL(100f, 180f);

        // Add confirm/cancel
        ui.addButton("Confirm", "confirm", 200f, 40f, 10f);
        ui.addButton("Cancel", "cancel", 200f, 40f, 10f);
        panel.addUIElement(ui).inTL(10f, 10f);
    }

    @Override
    public void customDialogConfirm() {
        // Called when the confirm button of the whole dialog is pressed (not custom
        // buttons)
    }

    @Override
    public void customDialogCancel() {
        // Called when dialog is cancelled (e.g. ESC or close)
    }

    public float getCustomDialogWidth() {
        return 520f;
    }

    public float getCustomDialogHeight() {
        return 320f;
    }

    public String getCancelText() {
        return "Cancel";
    }

    public String getConfirmText() {
        return "Confirm";
    }

    public boolean hasCancelButton() {
        return true;
    }

    public CustomUIPanelPlugin getCustomPanelPlugin() {
        return null;
    }

    protected static Class<?> findSlider() {
        ClassGraph classGraph = new ClassGraph().enableClassInfo().acceptPackages("com.fs.starfarer.campaign.ui");

        try (ScanResult scanResult = classGraph.scan()) {
            List<String> classNames = scanResult.getAllClasses().getNames();
    
            for (String className : classNames) {

                Class<?> clazz = Class.forName(className);

                if(!ReflectionUtils.getMethodsMatching(clazz, "getShowNotchOnIfBelowProgress", float.class).isEmpty()) {
                    return clazz; // The class has said method. It should be the slider class
                }

            }
        } catch (Exception e) {
            Global.getLogger(AssignWorkersDialog.class).error("Could not find the slider class: ", e);
        }
        return null;
    }
}
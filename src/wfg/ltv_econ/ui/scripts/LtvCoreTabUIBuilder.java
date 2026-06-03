package wfg.ltv_econ.ui.scripts;

import com.fs.starfarer.api.EveryFrameScript;
import com.fs.starfarer.api.Global;

public interface LtvCoreTabUIBuilder extends EveryFrameScript {
    default boolean isDone() { return !Global.getSector().isPaused(); }
    default boolean runWhilePaused() { return true; }
}
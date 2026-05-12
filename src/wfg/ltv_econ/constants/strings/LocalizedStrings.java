package wfg.ltv_econ.constants.strings;

import com.fs.starfarer.api.Global;

import wfg.ltv_econ.constants.Mods;

public class LocalizedStrings {
    public static final String str(String id) {
        return Global.getSettings().getString(Mods.LTV_ECON, id);
    }

    public static final String strf(String id, Object... args) {
        return String.format(str(id), args);
    }
}
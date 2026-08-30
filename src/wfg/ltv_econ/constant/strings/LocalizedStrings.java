package wfg.ltv_econ.constant.strings;

import com.fs.starfarer.api.Global;

import wfg.ltv_econ.constant.Mods;

public final class LocalizedStrings {
    private LocalizedStrings() {};
    public static final String str(String id) {
        return Global.getSettings().getString(Mods.LTV_ECON, id);
    }

    public static final String strf(String id, Object... args) {
        return String.format(str(id), args);
    }
}
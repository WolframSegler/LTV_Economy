package wfg_ltv_econ.ui;

public class LtvUIState {
    public enum UIStateType {
        NONE,
        DETAIL_DIALOG,
        MARKET_DETAIL_SCREEN,
        CAMPAIGN
    }

    private static UIStateType currentState = UIStateType.NONE;

    public static UIStateType getState() {
        return currentState;
    }

    public static void setState(UIStateType newState) {
        currentState = newState;
    }

    public static boolean is(UIStateType state) {
        return currentState == state;
    }

    public static void reset() {
        currentState = UIStateType.NONE;
    }
}
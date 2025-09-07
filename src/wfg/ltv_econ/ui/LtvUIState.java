package wfg.ltv_econ.ui;

public class LtvUIState {
    public enum UIState {
        NONE,
        DETAIL_DIALOG,
        MARKET_DETAIL_SCREEN,
        CAMPAIGN
    }

    private static UIState currentState = UIState.NONE;

    public static UIState getState() {
        return currentState;
    }

    public static void setState(UIState newState) {
        currentState = newState;
    }

    public static boolean is(UIState state) {
        return currentState == state;
    }

    public static void reset() {
        currentState = UIState.NONE;
    }
}
package wfg.wrap_ui.ui;

/*
 * A basic UI state machine that can be used to disable certain panel functionality based on the state.
 * Feel free to add states based on your needs
 */
public class UIState {
    public enum State {
        NONE,
        DIALOG,
        MARKET_DETAIL_SCREEN,
        CAMPAIGN
    }

    private static State currentState = State.NONE;

    public static State getState() {
        return currentState;
    }

    public static void setState(State newState) {
        currentState = newState;
    }

    public static boolean is(State state) {
        return currentState == state;
    }

    public static void reset() {
        currentState = State.NONE;
    }
}
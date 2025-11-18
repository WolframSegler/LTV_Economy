package wfg.wrap_ui.util;

@FunctionalInterface
public interface CallbackRunnable<CallerType> {
    void run(CallerType caller);
}

package com.example.chatgptreader;

public final class FloatingReaderGestureDetector {
    public enum Gesture {
        SINGLE_TAP,
        DOUBLE_TAP,
        LONG_PRESS_SPEED,
        FIVE_SECOND_SHUTDOWN,
        DRAG,
        NONE
    }

    public static final long DOUBLE_TAP_MS = 300L;
    public static final long LONG_PRESS_MS = 800L;
    public static final long SHUTDOWN_MS = 5000L;

    private long lastTapUpAt;

    public Gesture classifyRelease(long downDurationMs, boolean moved, long upAtMs) {
        if (moved) {
            return Gesture.DRAG;
        }
        if (downDurationMs >= SHUTDOWN_MS) {
            lastTapUpAt = 0L;
            return Gesture.FIVE_SECOND_SHUTDOWN;
        }
        if (downDurationMs >= LONG_PRESS_MS) {
            lastTapUpAt = 0L;
            return Gesture.LONG_PRESS_SPEED;
        }
        if (lastTapUpAt > 0L && upAtMs - lastTapUpAt <= DOUBLE_TAP_MS) {
            lastTapUpAt = 0L;
            return Gesture.DOUBLE_TAP;
        }
        lastTapUpAt = upAtMs;
        return Gesture.SINGLE_TAP;
    }
}

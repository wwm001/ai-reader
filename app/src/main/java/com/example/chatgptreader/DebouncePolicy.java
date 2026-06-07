package com.example.chatgptreader;

import android.view.accessibility.AccessibilityEvent;

public final class DebouncePolicy {
    public static final long CONTENT_CHANGED_DELAY_MS = 800L;
    public static final long SCROLLED_DELAY_MS = 250L;

    private DebouncePolicy() {
    }

    public static long delayForEventType(int eventType) {
        if (eventType == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED
                || eventType == AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED) {
            return CONTENT_CHANGED_DELAY_MS;
        }
        if (eventType == AccessibilityEvent.TYPE_VIEW_SCROLLED) {
            return SCROLLED_DELAY_MS;
        }
        return 0L;
    }
}

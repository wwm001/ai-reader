package com.example.chatgptreader;

import android.view.accessibility.AccessibilityEvent;

import org.junit.Assert;
import org.junit.Test;

public class DebouncePolicyTest {
    @Test
    public void delaysMatchEventTypes() {
        Assert.assertEquals(800L, DebouncePolicy.delayForEventType(AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED));
        Assert.assertEquals(250L, DebouncePolicy.delayForEventType(AccessibilityEvent.TYPE_VIEW_SCROLLED));
        Assert.assertEquals(0L, DebouncePolicy.delayForEventType(AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED));
    }
}

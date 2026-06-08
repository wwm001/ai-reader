package com.example.chatgptreader;

import org.junit.Assert;
import org.junit.Test;

public class FloatingReaderGestureDetectorTest {
    @Test
    public void detectsSingleDoubleLongAndShutdown() {
        FloatingReaderGestureDetector detector = new FloatingReaderGestureDetector();

        Assert.assertEquals(FloatingReaderGestureDetector.Gesture.SINGLE_TAP,
                detector.classifyRelease(100L, false, 1000L));
        Assert.assertEquals(FloatingReaderGestureDetector.Gesture.DOUBLE_TAP,
                detector.classifyRelease(100L, false, 1200L));
        Assert.assertEquals(FloatingReaderGestureDetector.Gesture.LONG_PRESS_SPEED,
                detector.classifyRelease(900L, false, 3000L));
        Assert.assertEquals(FloatingReaderGestureDetector.Gesture.FIVE_SECOND_SHUTDOWN,
                detector.classifyRelease(5000L, false, 9000L));
        Assert.assertEquals(FloatingReaderGestureDetector.Gesture.DRAG,
                detector.classifyRelease(100L, true, 9500L));
    }
}

package com.example.chatgptreader;

import org.junit.Assert;
import org.junit.Test;

public class FloatingTapActionBufferTest {
    @Test
    public void doubleTapCancelsPendingSingleAction() {
        FloatingTapActionBuffer buffer = new FloatingTapActionBuffer();

        buffer.onSingleCandidate();

        Assert.assertTrue(buffer.onDoubleTap());
        Assert.assertFalse(buffer.consumeSingleIfPending());
    }

    @Test
    public void singleActionRunsOnlyWhenConsumedAfterDelay() {
        FloatingTapActionBuffer buffer = new FloatingTapActionBuffer();

        buffer.onSingleCandidate();

        Assert.assertTrue(buffer.consumeSingleIfPending());
        Assert.assertFalse(buffer.consumeSingleIfPending());
    }
}

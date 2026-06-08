package com.example.chatgptreader;

import org.junit.Assert;
import org.junit.Test;

public class SpeechQueueTest {
    @Test
    public void enqueue_dedupesPendingAndSpokenFingerprints() {
        ReadHistory history = new ReadHistory();
        SpeechQueue queue = new SpeechQueue(history);

        Assert.assertTrue(queue.enqueue("これは読み上げ対象の文章です"));
        Assert.assertFalse(queue.enqueue("これは読み上げ対象の文章です"));

        SpeechQueue.Utterance utterance = queue.next();
        Assert.assertNotNull(utterance);
        queue.markDone(utterance.id);

        Assert.assertFalse(queue.enqueue("これは読み上げ対象の文章です"));
        Assert.assertEquals(1, history.spokenSize());
    }

    @Test
    public void stopPending_clearsPendingAndCurrentButKeepsSpoken() {
        ReadHistory history = new ReadHistory();
        SpeechQueue queue = new SpeechQueue(history);
        queue.enqueue("最初に読み上げる文章です");
        SpeechQueue.Utterance utterance = queue.next();
        queue.markDone(utterance.id);
        queue.enqueue("停止時に消える待機中の文章です");
        queue.enqueue("これも停止時に消える文章です");

        queue.stopPending();

        Assert.assertEquals(0, queue.pendingSize());
        Assert.assertEquals(1, history.spokenSize());
        Assert.assertEquals(0, history.pendingSize());
        Assert.assertNull(history.currentFingerprint());
    }

    @Test
    public void pause_keepsCurrentAtFrontOfQueue() {
        ReadHistory history = new ReadHistory();
        SpeechQueue queue = new SpeechQueue(history);
        queue.enqueue("一時停止してから再開する文章です");
        SpeechQueue.Utterance current = queue.next();

        queue.pauseAndKeepCurrent();
        SpeechQueue.Utterance resumed = queue.next();

        Assert.assertEquals(current.fingerprint, resumed.fingerprint);
    }

    @Test
    public void resetReadHistory_clearsSpokenPendingAndCurrent() {
        ReadHistory history = new ReadHistory();
        SpeechQueue queue = new SpeechQueue(history);
        queue.enqueue("リセット前に読んだ文章です");
        SpeechQueue.Utterance utterance = queue.next();
        queue.markDone(utterance.id);

        queue.resetReadHistory();

        Assert.assertEquals(0, history.spokenSize());
        Assert.assertTrue(queue.enqueue("リセット前に読んだ文章です"));
    }
}

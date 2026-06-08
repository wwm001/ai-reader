package com.example.chatgptreader;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;

public final class SpeechQueue {
    private static final int MAX_PENDING = 100;
    public enum EnqueueResult {
        ENQUEUED,
        DUPLICATE_PENDING_OR_CURRENT,
        DUPLICATE_SPOKEN,
        TOO_SHORT,
        FULL
    }

    private final ArrayDeque<Utterance> pending = new ArrayDeque<>();
    private final ReadHistory history;
    private Utterance current;

    public SpeechQueue(ReadHistory history) {
        this.history = history;
    }

    public synchronized boolean enqueue(String text) {
        return enqueueDetailed(text) == EnqueueResult.ENQUEUED;
    }

    public synchronized EnqueueResult enqueueDetailed(String text) {
        String normalized = TextNormalizer.normalize(text);
        if (normalized.length() < 8) {
            return EnqueueResult.TOO_SHORT;
        }
        if (pending.size() >= MAX_PENDING) {
            return EnqueueResult.FULL;
        }
        String fingerprint = TextNormalizer.fingerprint(normalized);
        if (history.isSpoken(fingerprint)) {
            return EnqueueResult.DUPLICATE_SPOKEN;
        }
        if (!history.markPending(fingerprint)) {
            return EnqueueResult.DUPLICATE_PENDING_OR_CURRENT;
        }
        pending.add(new Utterance(normalized, fingerprint));
        return EnqueueResult.ENQUEUED;
    }

    public synchronized Utterance next() {
        current = pending.poll();
        if (current != null) {
            history.markCurrent(current.fingerprint);
        }
        return current;
    }

    public synchronized void markDone(String utteranceId) {
        if (current != null && current.id.equals(utteranceId)) {
            history.markSpoken(current.fingerprint);
            current = null;
        }
    }

    public synchronized void markError(String utteranceId) {
        if (current != null && current.id.equals(utteranceId)) {
            history.clearCurrent(current.fingerprint);
            current = null;
        }
    }

    public synchronized boolean hasCurrent() {
        return current != null;
    }

    public synchronized int pendingSize() {
        return pending.size();
    }

    public synchronized String currentFingerprint() {
        return current == null ? "" : current.fingerprint;
    }

    public synchronized int spokenSize() {
        return history.spokenSize();
    }

    public synchronized int pendingFingerprintSize() {
        return history.pendingSize();
    }

    public synchronized String readCursor() {
        return current == null ? "" : current.fingerprint;
    }

    public synchronized void pauseAndKeepCurrent() {
        if (current != null) {
            history.clearCurrent(current.fingerprint);
            if (history.markPending(current.fingerprint)) {
                pending.addFirst(current);
            }
            current = null;
        }
    }

    public synchronized void requeueCurrentForRateChange() {
        pauseAndKeepCurrent();
    }

    public synchronized void stopPending() {
        pending.clear();
        history.clearPendingAndCurrent();
        current = null;
    }

    public synchronized void resetReadHistory() {
        pending.clear();
        current = null;
        history.clearAll();
    }

    public synchronized List<String> pendingFingerprintsSnapshot() {
        List<String> values = new ArrayList<>();
        for (Utterance utterance : pending) {
            values.add(utterance.fingerprint);
        }
        return values;
    }

    public static final class Utterance {
        public final String text;
        public final String fingerprint;
        public final String id;

        Utterance(String text, String fingerprint) {
            this.text = text;
            this.fingerprint = fingerprint;
            this.id = "reader-" + fingerprint + "-" + System.nanoTime();
        }
    }
}

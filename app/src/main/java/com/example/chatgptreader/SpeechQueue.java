package com.example.chatgptreader;

import java.util.ArrayDeque;
import java.util.Queue;

public final class SpeechQueue {
    private final Queue<Utterance> pending = new ArrayDeque<>();
    private final ReadHistory history;
    private Utterance current;

    public SpeechQueue(ReadHistory history) {
        this.history = history;
    }

    public synchronized boolean enqueue(String text) {
        String normalized = TextNormalizer.normalize(text);
        if (normalized.length() < 8) {
            return false;
        }
        String fingerprint = TextNormalizer.fingerprint(normalized);
        if (!history.markPending(fingerprint)) {
            return false;
        }
        pending.add(new Utterance(normalized, fingerprint));
        return true;
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

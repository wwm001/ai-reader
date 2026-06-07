package com.example.chatgptreader;

import java.util.LinkedHashSet;
import java.util.Set;

public final class ReadHistory {
    private static final int MAX_HISTORY = 500;

    private final LinkedHashSet<String> spoken = new LinkedHashSet<>();
    private final LinkedHashSet<String> pending = new LinkedHashSet<>();
    private String current;

    public synchronized boolean containsAny(String fingerprint) {
        return fingerprint != null
                && (fingerprint.equals(current) || pending.contains(fingerprint) || spoken.contains(fingerprint));
    }

    public synchronized boolean markPending(String fingerprint) {
        if (containsAny(fingerprint)) {
            return false;
        }
        pending.add(fingerprint);
        trim(spoken);
        return true;
    }

    public synchronized void markCurrent(String fingerprint) {
        current = fingerprint;
        pending.remove(fingerprint);
    }

    public synchronized void markSpoken(String fingerprint) {
        if (fingerprint != null) {
            spoken.add(fingerprint);
            trim(spoken);
        }
        if (fingerprint != null && fingerprint.equals(current)) {
            current = null;
        }
    }

    public synchronized void clearCurrent(String fingerprint) {
        if (fingerprint != null && fingerprint.equals(current)) {
            current = null;
        }
    }

    public synchronized void clearPendingAndCurrent() {
        pending.clear();
        current = null;
    }

    public synchronized void clearAll() {
        spoken.clear();
        pending.clear();
        current = null;
    }

    public synchronized int spokenSize() {
        return spoken.size();
    }

    public synchronized int pendingSize() {
        return pending.size();
    }

    public synchronized String currentFingerprint() {
        return current;
    }

    private void trim(Set<String> values) {
        while (values.size() > MAX_HISTORY && values instanceof LinkedHashSet) {
            String first = values.iterator().next();
            values.remove(first);
        }
    }
}

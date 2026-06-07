package com.example.chatgptreader;

import android.graphics.Rect;
import android.view.accessibility.AccessibilityNodeInfo;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public final class AccessibilityTextExtractor {
    private AccessibilityTextExtractor() {
    }

    public static Result extract(AccessibilityNodeInfo root) {
        List<Candidate> candidates = new ArrayList<>();
        Counts counts = new Counts();
        collect(root, candidates, counts);
        candidates.sort(Comparator
                .comparingInt((Candidate candidate) -> candidate.bounds.top)
                .thenComparingInt(candidate -> candidate.bounds.left));

        List<String> deduped = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        for (Candidate candidate : candidates) {
            String fingerprint = TextNormalizer.fingerprint(candidate.text);
            if (seen.add(fingerprint)) {
                deduped.add(candidate.text);
            } else {
                counts.excluded++;
            }
        }
        return new Result(deduped, counts.extracted, deduped.size(), counts.excluded);
    }

    private static void collect(AccessibilityNodeInfo node, List<Candidate> candidates, Counts counts) {
        if (node == null) {
            return;
        }
        CharSequence raw = node.getText();
        if ((raw == null || raw.length() == 0) && node.getContentDescription() != null) {
            raw = node.getContentDescription();
        }
        if (raw != null && raw.length() > 0) {
            counts.extracted++;
            String value = TextNormalizer.normalize(raw.toString());
            if (isCandidate(node, value)) {
                Rect rect = new Rect();
                node.getBoundsInScreen(rect);
                candidates.add(new Candidate(value, rect));
            } else {
                counts.excluded++;
            }
        }
        for (int i = 0; i < node.getChildCount(); i++) {
            AccessibilityNodeInfo child = node.getChild(i);
            try {
                collect(child, candidates, counts);
            } finally {
                if (child != null) {
                    child.recycle();
                }
            }
        }
    }

    private static boolean isCandidate(AccessibilityNodeInfo node, String value) {
        if (value.length() < 8 || !node.isVisibleToUser()) {
            return false;
        }
        CharSequence className = node.getClassName();
        String klass = className == null ? "" : className.toString().toLowerCase(Locale.ROOT);
        if (klass.contains("edittext") || klass.contains("button") || klass.contains("tab")) {
            return false;
        }
        String lower = value.toLowerCase(Locale.ROOT);
        return !lower.contains("chatgpt")
                && !lower.contains("send")
                && !lower.contains("message")
                && !lower.contains("ログイン")
                && !lower.contains("設定");
    }

    private static final class Candidate {
        final String text;
        final Rect bounds;

        Candidate(String text, Rect bounds) {
            this.text = text;
            this.bounds = bounds;
        }
    }

    private static final class Counts {
        int extracted;
        int excluded;
    }

    public static final class Result {
        public final List<String> candidates;
        public final int extractedNodeCount;
        public final int candidateNodeCount;
        public final int excludedNodeCount;

        Result(List<String> candidates, int extractedNodeCount, int candidateNodeCount, int excludedNodeCount) {
            this.candidates = candidates;
            this.extractedNodeCount = extractedNodeCount;
            this.candidateNodeCount = candidateNodeCount;
            this.excludedNodeCount = excludedNodeCount;
        }

        public String snapshotFingerprint() {
            StringBuilder builder = new StringBuilder();
            for (String candidate : candidates) {
                builder.append(TextNormalizer.fingerprint(candidate)).append('|');
            }
            return TextNormalizer.fingerprint(builder.toString());
        }
    }
}

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

    public static Result extract(AccessibilityNodeInfo root, float density, int screenHeight) {
        List<Candidate> candidates = new ArrayList<>();
        Counts counts = new Counts();
        collect(root, candidates, counts, density, screenHeight);
        candidates.sort(Comparator
                .comparingInt((Candidate candidate) -> candidate.bounds.top)
                .thenComparingInt(candidate -> candidate.bounds.left));

        List<String> deduped = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        for (Candidate candidate : candidates) {
            String fingerprint = TextNormalizer.fingerprint(candidate.text);
            if (seen.add(fingerprint)) {
                deduped.addAll(NaturalTextChunker.chunk(candidate.text));
            } else {
                counts.excluded++;
                DiagnosticCounters.inc(DiagnosticCounters.EXCLUDED_DUPLICATE);
            }
        }
        return new Result(deduped, counts.extracted, deduped.size(), counts.excluded);
    }

    private static void collect(AccessibilityNodeInfo node, List<Candidate> candidates, Counts counts, float density, int screenHeight) {
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
            Rect rect = new Rect();
            node.getBoundsInScreen(rect);
            String reason = ChatGptUiFilter.exclusionReason(node, value, rect, density, screenHeight);
            if (reason == null) {
                candidates.add(new Candidate(value, rect));
            } else {
                counts.excluded++;
                DiagnosticCounters.inc(reason);
            }
        }
        for (int i = 0; i < node.getChildCount(); i++) {
            AccessibilityNodeInfo child = node.getChild(i);
            try {
                collect(child, candidates, counts, density, screenHeight);
            } finally {
                if (child != null) {
                    child.recycle();
                }
            }
        }
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

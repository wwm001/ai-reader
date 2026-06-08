package com.example.chatgptreader;

import java.util.ArrayList;
import java.util.List;

public final class NaturalTextChunker {
    private static final int MIN_CHUNK = 8;
    private static final int MAX_CHUNK = 180;

    private NaturalTextChunker() {
    }

    public static List<String> chunk(String text) {
        String normalized = TextNormalizer.normalizeForChunking(text);
        List<String> chunks = new ArrayList<>();
        if (normalized.length() < MIN_CHUNK) {
            return chunks;
        }
        StringBuilder current = new StringBuilder();
        for (int i = 0; i < normalized.length(); i++) {
            char ch = normalized.charAt(i);
            current.append(ch);
            boolean boundary = isBoundary(ch);
            boolean strongBoundary = ch == '\n' || ch == '■' || ch == '●' || ch == '◆';
            if ((strongBoundary && current.length() >= MIN_CHUNK)
                    || (boundary && current.length() >= 40)
                    || current.length() >= MAX_CHUNK) {
                addChunk(chunks, current);
            }
        }
        addChunk(chunks, current);
        return chunks;
    }

    private static boolean isBoundary(char ch) {
        return ch == '。'
                || ch == '？'
                || ch == '！'
                || ch == '\n'
                || ch == '・'
                || ch == '■'
                || ch == '●'
                || ch == '◆';
    }

    private static void addChunk(List<String> chunks, StringBuilder current) {
        String value = TextNormalizer.normalizeForFingerprint(current.toString());
        current.setLength(0);
        if (value.length() >= MIN_CHUNK) {
            chunks.add(value);
        }
    }
}

package com.example.chatgptreader;

import java.text.Normalizer;
import java.util.Locale;

public final class TextNormalizer {
    private TextNormalizer() {
    }

    public static String normalize(String value) {
        return normalizeForFingerprint(value);
    }

    public static String normalizeForFingerprint(String value) {
        if (value == null) {
            return "";
        }
        String normalized = Normalizer.normalize(value, Normalizer.Form.NFKC)
                .replaceAll("\\s+", " ")
                .trim();
        return normalized;
    }

    public static String normalizeForChunking(String value) {
        if (value == null) {
            return "";
        }
        return Normalizer.normalize(value, Normalizer.Form.NFKC)
                .replace("\r\n", "\n")
                .replace('\r', '\n')
                .replaceAll("[\\t ]+", " ")
                .replaceAll(" *\\n *", "\n")
                .trim();
    }

    public static String fingerprint(String value) {
        String normalized = normalizeForFingerprint(value).toLowerCase(Locale.ROOT);
        return Integer.toHexString(normalized.hashCode()) + ":" + normalized.length();
    }
}

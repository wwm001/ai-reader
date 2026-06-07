package com.example.chatgptreader;

import java.text.Normalizer;
import java.util.Locale;

public final class TextNormalizer {
    private TextNormalizer() {
    }

    public static String normalize(String value) {
        if (value == null) {
            return "";
        }
        String normalized = Normalizer.normalize(value, Normalizer.Form.NFKC)
                .replaceAll("\\s+", " ")
                .trim();
        return normalized;
    }

    public static String fingerprint(String value) {
        String normalized = normalize(value).toLowerCase(Locale.ROOT);
        return Integer.toHexString(normalized.hashCode()) + ":" + normalized.length();
    }
}

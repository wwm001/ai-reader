package com.example.chatgptreader;

import android.content.Context;
import android.content.SharedPreferences;

public final class ReaderState {
    private static final String PREFS = "reader_state";
    private static final String KEY_ENABLED = "reader_enabled";
    private static final String KEY_MODE = "reader_mode";
    private static final String KEY_TTS = "tts_state";
    private static final String KEY_TARGET_DETECTED = "target_detected";
    private static final String KEY_ACCESSIBILITY_CONNECTED = "accessibility_connected";
    private static final String KEY_ACCESSIBILITY_RAW = "accessibility_raw";
    private static final String KEY_EXTRACTED = "extracted_count";
    private static final String KEY_CANDIDATE = "candidate_count";
    private static final String KEY_EXCLUDED = "excluded_count";
    private static final String KEY_QUEUED = "queued_count";
    private static final String KEY_STARTED = "started_count";
    private static final String KEY_SPOKEN = "spoken_count";
    private static final String KEY_SKIPPED = "skipped_count";
    private static final String KEY_TTS_ERROR = "tts_error_count";
    private static final String KEY_SPEECH_RATE = "speech_rate";
    private static final String KEY_INCLUDE_SNIPPETS = "include_snippets";
    private static final String KEY_OVERLAY_X = "overlay_x";
    private static final String KEY_OVERLAY_Y = "overlay_y";
    private static final String KEY_OVERLAY_VISIBLE = "overlay_visible";
    private static final String KEY_READ_CURSOR = "read_cursor";

    public static final String TARGET_PACKAGE = "com.openai.chatgpt";

    private ReaderState() {
    }

    public static SharedPreferences prefs(Context context) {
        return context.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public static boolean isReaderEnabled(Context context) {
        return getMode(context).isReadingEnabled();
    }

    public static void setReaderEnabled(Context context, boolean enabled) {
        setMode(context, enabled ? ReaderMode.PLAYING : ReaderMode.STOPPED);
        DiagnosticStore.get().event("readerEnabled", String.valueOf(enabled));
    }

    public static ReaderMode getMode(Context context) {
        String value = prefs(context).getString(KEY_MODE, null);
        if (value == null) {
            return prefs(context).getBoolean(KEY_ENABLED, false) ? ReaderMode.PLAYING : ReaderMode.STOPPED;
        }
        try {
            return ReaderMode.valueOf(value).normalized();
        } catch (IllegalArgumentException ignored) {
            return ReaderMode.STOPPED;
        }
    }

    public static void setMode(Context context, ReaderMode mode) {
        ReaderMode normalized = mode.normalized();
        prefs(context).edit()
                .putString(KEY_MODE, normalized.name())
                .putBoolean(KEY_ENABLED, normalized.isReadingEnabled())
                .apply();
        DiagnosticStore.get().event("readerMode", normalized.name());
    }

    public static String getTtsState(Context context) {
        return prefs(context).getString(KEY_TTS, "idle");
    }

    public static void setTtsState(Context context, String state) {
        prefs(context).edit().putString(KEY_TTS, state).apply();
    }

    public static boolean isTargetDetected(Context context) {
        return prefs(context).getBoolean(KEY_TARGET_DETECTED, false);
    }

    public static void setTargetDetected(Context context, boolean detected) {
        prefs(context).edit().putBoolean(KEY_TARGET_DETECTED, detected).apply();
    }

    public static boolean isAccessibilityServiceConnected(Context context) {
        return prefs(context).getBoolean(KEY_ACCESSIBILITY_CONNECTED, false);
    }

    public static void setAccessibilityServiceConnected(Context context, boolean connected) {
        prefs(context).edit().putBoolean(KEY_ACCESSIBILITY_CONNECTED, connected).apply();
        DiagnosticStore.get().event("accessibilityConnected", String.valueOf(connected));
    }

    public static String getAccessibilityRawValue(Context context) {
        return prefs(context).getString(KEY_ACCESSIBILITY_RAW, "");
    }

    public static void setAccessibilityRawValue(Context context, String rawValue) {
        prefs(context).edit().putString(KEY_ACCESSIBILITY_RAW, rawValue == null ? "" : rawValue).apply();
    }

    public static void updateCounts(Context context, int extracted, int candidates, int excluded) {
        prefs(context).edit()
                .putInt(KEY_EXTRACTED, extracted)
                .putInt(KEY_CANDIDATE, candidates)
                .putInt(KEY_EXCLUDED, excluded)
                .apply();
    }

    public static int getExtractedCount(Context context) {
        return prefs(context).getInt(KEY_EXTRACTED, 0);
    }

    public static int getCandidateCount(Context context) {
        return prefs(context).getInt(KEY_CANDIDATE, 0);
    }

    public static int getExcludedCount(Context context) {
        return prefs(context).getInt(KEY_EXCLUDED, 0);
    }

    public static int getQueuedCount(Context context) {
        return prefs(context).getInt(KEY_QUEUED, 0);
    }

    public static int getStartedCount(Context context) {
        return prefs(context).getInt(KEY_STARTED, 0);
    }

    public static int getSpokenCount(Context context) {
        return prefs(context).getInt(KEY_SPOKEN, 0);
    }

    public static int getSkippedCount(Context context) {
        return prefs(context).getInt(KEY_SKIPPED, 0);
    }

    public static int getTtsErrorCount(Context context) {
        return prefs(context).getInt(KEY_TTS_ERROR, 0);
    }

    public static void incrementQueued(Context context) {
        increment(context, KEY_QUEUED);
    }

    public static void incrementStarted(Context context) {
        increment(context, KEY_STARTED);
    }

    public static void incrementSpoken(Context context) {
        increment(context, KEY_SPOKEN);
    }

    public static void incrementSkipped(Context context) {
        increment(context, KEY_SKIPPED);
    }

    public static void incrementTtsError(Context context) {
        increment(context, KEY_TTS_ERROR);
    }

    public static void resetReadState(Context context) {
        prefs(context).edit()
                .putInt(KEY_SKIPPED, 0)
                .putInt(KEY_STARTED, 0)
                .putInt(KEY_SPOKEN, 0)
                .putInt(KEY_QUEUED, 0)
                .putInt(KEY_TTS_ERROR, 0)
                .apply();
        DiagnosticStore.get().event("readStateReset", "manual");
    }

    public static float getSpeechRate(Context context) {
        return prefs(context).getFloat(KEY_SPEECH_RATE, 1.0f);
    }

    public static void setSpeechRate(Context context, float rate) {
        prefs(context).edit().putFloat(KEY_SPEECH_RATE, ReaderSettingsRepository.clampSpeechRate(rate)).apply();
    }

    public static boolean shouldIncludeDiagnosticSnippets(Context context) {
        return prefs(context).getBoolean(KEY_INCLUDE_SNIPPETS, false);
    }

    public static void setIncludeDiagnosticSnippets(Context context, boolean include) {
        prefs(context).edit().putBoolean(KEY_INCLUDE_SNIPPETS, include).apply();
        if (!include) {
            DiagnosticStore.get().clearSnippets();
        }
    }

    public static int getOverlayX(Context context, int fallback) {
        return prefs(context).getInt(KEY_OVERLAY_X, fallback);
    }

    public static int getOverlayY(Context context, int fallback) {
        return prefs(context).getInt(KEY_OVERLAY_Y, fallback);
    }

    public static void setOverlayPosition(Context context, int x, int y) {
        prefs(context).edit().putInt(KEY_OVERLAY_X, x).putInt(KEY_OVERLAY_Y, y).apply();
    }

    public static boolean isOverlayVisible(Context context) {
        return prefs(context).getBoolean(KEY_OVERLAY_VISIBLE, false);
    }

    public static void setOverlayVisible(Context context, boolean visible) {
        prefs(context).edit().putBoolean(KEY_OVERLAY_VISIBLE, visible).apply();
    }

    public static String getOverlayPositionText(Context context) {
        SharedPreferences preferences = prefs(context);
        return preferences.getInt(KEY_OVERLAY_X, -1) + "," + preferences.getInt(KEY_OVERLAY_Y, -1);
    }

    public static String getReadCursor(Context context) {
        return prefs(context).getString(KEY_READ_CURSOR, "");
    }

    public static void setReadCursor(Context context, String cursor) {
        prefs(context).edit().putString(KEY_READ_CURSOR, cursor == null ? "" : cursor).apply();
    }

    private static void increment(Context context, String key) {
        SharedPreferences preferences = prefs(context);
        preferences.edit().putInt(key, preferences.getInt(key, 0) + 1).apply();
    }
}

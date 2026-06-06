package com.example.chatgptreader;

import android.content.Context;
import android.content.SharedPreferences;

public final class ReaderState {
    private static final String PREFS = "reader_state";
    private static final String KEY_ENABLED = "reader_enabled";
    private static final String KEY_TTS = "tts_state";
    private static final String KEY_TARGET_DETECTED = "target_detected";
    private static final String KEY_EXTRACTED = "extracted_count";
    private static final String KEY_CANDIDATE = "candidate_count";
    private static final String KEY_EXCLUDED = "excluded_count";
    private static final String KEY_QUEUED = "queued_count";
    private static final String KEY_SPOKEN = "spoken_count";
    private static final String KEY_SKIPPED = "skipped_count";

    public static final String TARGET_PACKAGE = "com.openai.chatgpt";

    private ReaderState() {
    }

    public static SharedPreferences prefs(Context context) {
        return context.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public static boolean isReaderEnabled(Context context) {
        return prefs(context).getBoolean(KEY_ENABLED, false);
    }

    public static void setReaderEnabled(Context context, boolean enabled) {
        prefs(context).edit().putBoolean(KEY_ENABLED, enabled).apply();
        DiagnosticStore.get().event("readerEnabled", String.valueOf(enabled));
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

    public static int getSpokenCount(Context context) {
        return prefs(context).getInt(KEY_SPOKEN, 0);
    }

    public static int getSkippedCount(Context context) {
        return prefs(context).getInt(KEY_SKIPPED, 0);
    }

    public static void incrementQueued(Context context) {
        increment(context, KEY_QUEUED);
    }

    public static void incrementSpoken(Context context) {
        increment(context, KEY_SPOKEN);
    }

    public static void incrementSkipped(Context context) {
        increment(context, KEY_SKIPPED);
    }

    public static void resetReadState(Context context) {
        prefs(context).edit().putInt(KEY_SKIPPED, 0).putInt(KEY_SPOKEN, 0).apply();
        DiagnosticStore.get().event("readStateReset", "manual");
    }

    private static void increment(Context context, String key) {
        SharedPreferences preferences = prefs(context);
        preferences.edit().putInt(key, preferences.getInt(key, 0) + 1).apply();
    }
}

package com.example.chatgptreader;

import android.content.Context;

public final class ReaderSettingsRepository {
    private static final float MIN_RATE = 0.5f;
    private static final float MAX_RATE = 2.0f;
    private static final float DEFAULT_RATE = 1.0f;
    private static final float[] SPEED_CYCLE = {0.8f, 1.0f, 1.2f, 1.5f, 2.0f};

    private ReaderSettingsRepository() {
    }

    public static float getSpeechRate(Context context) {
        return ReaderState.getSpeechRate(context);
    }

    public static float setSpeechRate(Context context, float rate) {
        float clamped = clampSpeechRate(rate);
        ReaderState.setSpeechRate(context, clamped);
        ReaderCommandBus.send(context, ReaderCommandBus.COMMAND_RATE_CHANGED);
        return clamped;
    }

    public static float cycleSpeechRate(Context context) {
        float current = getSpeechRate(context);
        for (float speed : SPEED_CYCLE) {
            if (current < speed - 0.01f) {
                return setSpeechRate(context, speed);
            }
        }
        return setSpeechRate(context, SPEED_CYCLE[0]);
    }

    public static float clampSpeechRate(float rate) {
        if (Float.isNaN(rate)) {
            return DEFAULT_RATE;
        }
        if (rate < MIN_RATE) {
            return MIN_RATE;
        }
        if (rate > MAX_RATE) {
            return MAX_RATE;
        }
        return Math.round(rate * 10f) / 10f;
    }

    public static String formatRate(float rate) {
        return String.format(java.util.Locale.US, "%.1fx", clampSpeechRate(rate));
    }
}

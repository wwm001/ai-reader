package com.example.chatgptreader;

import android.accessibilityservice.AccessibilityService;
import android.speech.tts.TextToSpeech;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

public class ChatGptAccessibilityService extends AccessibilityService implements TextToSpeech.OnInitListener {
    private TextToSpeech tts;
    private final Set<String> spoken = new HashSet<>();

    @Override
    public void onCreate() {
        super.onCreate();
        tts = new TextToSpeech(this, this);
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        if (event == null) {
            return;
        }
        CharSequence packageName = event.getPackageName();
        boolean targetDetected = ReaderState.TARGET_PACKAGE.contentEquals(packageName == null ? "" : packageName);
        ReaderState.setTargetDetected(this, targetDetected);
        DiagnosticStore.get().event("accessibilityEvent", eventTypeName(event.getEventType()));
        ReaderNotificationController.update(this);
        if (!targetDetected || !ReaderState.isReaderEnabled(this)) {
            return;
        }
        AccessibilityNodeInfo root = getRootInActiveWindow();
        if (root == null) {
            ReaderState.updateCounts(this, 0, 0, 0);
            return;
        }
        try {
            Counts counts = new Counts();
            collectAndSpeak(root, counts);
            ReaderState.updateCounts(this, counts.extracted, counts.candidates, counts.excluded);
        } catch (RuntimeException e) {
            DiagnosticStore.get().error(e);
        } finally {
            root.recycle();
        }
    }

    @Override
    public void onInterrupt() {
        if (tts != null) {
            tts.stop();
        }
        ReaderState.setTtsState(this, "interrupted");
    }

    @Override
    public void onDestroy() {
        if (tts != null) {
            tts.shutdown();
        }
        super.onDestroy();
    }

    @Override
    public void onInit(int status) {
        if (status == TextToSpeech.SUCCESS) {
            tts.setLanguage(Locale.JAPANESE);
            ReaderState.setTtsState(this, "ready");
        } else {
            ReaderState.setTtsState(this, "error");
            DiagnosticStore.get().event("ttsInitFailed", String.valueOf(status));
        }
    }

    private void collectAndSpeak(AccessibilityNodeInfo node, Counts counts) {
        if (node == null) {
            return;
        }
        CharSequence text = node.getText();
        if (text != null) {
            counts.extracted++;
            String value = text.toString().trim();
            if (isCandidate(value)) {
                counts.candidates++;
                DiagnosticStore.get().candidateSnippet(value);
                speakOnce(value);
            } else {
                counts.excluded++;
            }
        }
        for (int i = 0; i < node.getChildCount(); i++) {
            AccessibilityNodeInfo child = node.getChild(i);
            try {
                collectAndSpeak(child, counts);
            } finally {
                if (child != null) {
                    child.recycle();
                }
            }
        }
    }

    private boolean isCandidate(String value) {
        if (value.length() < 8) {
            return false;
        }
        String lower = value.toLowerCase(Locale.ROOT);
        return !lower.contains("chatgpt") && !lower.contains("send") && !lower.contains("message");
    }

    private void speakOnce(String value) {
        if (tts == null || spoken.contains(value)) {
            ReaderState.incrementSkipped(this);
            return;
        }
        spoken.add(value);
        ReaderState.incrementQueued(this);
        ReaderState.setTtsState(this, "speaking");
        tts.speak(value, TextToSpeech.QUEUE_ADD, null, "reader-" + spoken.size());
        ReaderState.incrementSpoken(this);
    }

    private static String eventTypeName(int type) {
        switch (type) {
            case AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED:
                return "TYPE_WINDOW_STATE_CHANGED";
            case AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED:
                return "TYPE_WINDOW_CONTENT_CHANGED";
            case AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED:
                return "TYPE_VIEW_TEXT_CHANGED";
            case AccessibilityEvent.TYPE_VIEW_SCROLLED:
                return "TYPE_VIEW_SCROLLED";
            default:
                return String.valueOf(type);
        }
    }

    private static final class Counts {
        int extracted;
        int candidates;
        int excluded;
    }
}

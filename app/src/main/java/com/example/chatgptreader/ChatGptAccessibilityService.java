package com.example.chatgptreader;

import android.accessibilityservice.AccessibilityService;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.Looper;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

import java.util.List;
import java.util.Locale;

import androidx.core.content.ContextCompat;

public class ChatGptAccessibilityService extends AccessibilityService implements TextToSpeech.OnInitListener {
    private static final long STREAMING_STABLE_MS = 1000L;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable scanRunnable = this::scanCurrentWindow;
    private final ReadHistory readHistory = new ReadHistory();
    private final SpeechQueue speechQueue = new SpeechQueue(readHistory);
    private final BroadcastReceiver commandReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent != null && ReaderCommandBus.ACTION_COMMAND.equals(intent.getAction())) {
                handleCommand(intent.getStringExtra(ReaderCommandBus.EXTRA_COMMAND));
            }
        }
    };

    private TextToSpeech tts;
    private boolean ttsReady;
    private String lastSnapshotFingerprint = "";
    private String lastTrailingText = "";
    private long lastTrailingChangedAt;

    @Override
    public void onCreate() {
        super.onCreate();
        tts = new TextToSpeech(this, this);
        IntentFilter filter = new IntentFilter(ReaderCommandBus.ACTION_COMMAND);
        ContextCompat.registerReceiver(this, commandReceiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED);
    }

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        ReaderState.setAccessibilityServiceConnected(this, true);
        DiagnosticStore.get().event("serviceConnected", "true");
        if (ReaderState.isReaderEnabled(this)) {
            scheduleScan(0L);
        }
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
        scheduleScan(DebouncePolicy.delayForEventType(event.getEventType()));
    }

    @Override
    public void onInterrupt() {
        stopPlayback("interrupted", false);
    }

    @Override
    public void onDestroy() {
        handler.removeCallbacksAndMessages(null);
        ReaderState.setAccessibilityServiceConnected(this, false);
        try {
            unregisterReceiver(commandReceiver);
        } catch (IllegalArgumentException ignored) {
            // Receiver may already be unregistered during service teardown.
        }
        if (tts != null) {
            tts.stop();
            tts.shutdown();
        }
        super.onDestroy();
    }

    @Override
    public void onInit(int status) {
        if (status == TextToSpeech.SUCCESS) {
            ttsReady = true;
            tts.setLanguage(Locale.JAPANESE);
            tts.setSpeechRate(ReaderSettingsRepository.getSpeechRate(this));
            tts.setOnUtteranceProgressListener(new UtteranceProgressListener() {
                @Override
                public void onStart(String utteranceId) {
                    ReaderState.incrementStarted(ChatGptAccessibilityService.this);
                    ReaderState.setTtsState(ChatGptAccessibilityService.this, "speaking");
                    ReaderNotificationController.update(ChatGptAccessibilityService.this);
                }

                @Override
                public void onDone(String utteranceId) {
                    speechQueue.markDone(utteranceId);
                    ReaderState.incrementSpoken(ChatGptAccessibilityService.this);
                    handler.post(ChatGptAccessibilityService.this::speakNextIfIdle);
                }

                @Override
                public void onError(String utteranceId) {
                    speechQueue.markError(utteranceId);
                    ReaderState.incrementTtsError(ChatGptAccessibilityService.this);
                    ReaderState.setTtsState(ChatGptAccessibilityService.this, "error");
                    handler.post(ChatGptAccessibilityService.this::speakNextIfIdle);
                }
            });
            ReaderState.setTtsState(this, "ready");
        } else {
            ReaderState.setTtsState(this, "error");
            DiagnosticStore.get().event("ttsInitFailed", String.valueOf(status));
        }
    }

    private void handleCommand(String command) {
        if (ReaderCommandBus.COMMAND_ON.equals(command) || ReaderCommandBus.COMMAND_RESUME.equals(command)) {
            ReaderState.setMode(this, ReaderMode.ON);
            ReaderState.setTtsState(this, "ready");
            ReaderNotificationController.update(this);
            scheduleScan(0L);
        } else if (ReaderCommandBus.COMMAND_PAUSE.equals(command)) {
            ReaderState.setMode(this, ReaderMode.PAUSED);
            stopPlayback("paused", true);
        } else if (ReaderCommandBus.COMMAND_OFF.equals(command) || ReaderCommandBus.COMMAND_STOP.equals(command)) {
            ReaderState.setMode(this, ReaderMode.OFF);
            stopPlayback("stopped", false);
        } else if (ReaderCommandBus.COMMAND_RESET_READ.equals(command)) {
            speechQueue.resetReadHistory();
            ReaderState.resetReadState(this);
            if (ReaderState.isReaderEnabled(this)) {
                lastSnapshotFingerprint = "";
                scheduleScan(0L);
            }
            ReaderNotificationController.update(this);
        } else if (ReaderCommandBus.COMMAND_RESCAN.equals(command)) {
            if (ReaderState.isReaderEnabled(this)) {
                lastSnapshotFingerprint = "";
                scheduleScan(0L);
            }
        } else if (ReaderCommandBus.COMMAND_RATE_CHANGED.equals(command)) {
            if (tts != null) {
                tts.setSpeechRate(ReaderSettingsRepository.getSpeechRate(this));
            }
            ReaderNotificationController.update(this);
        }
    }

    private void scheduleScan(long delayMs) {
        handler.removeCallbacks(scanRunnable);
        handler.postDelayed(scanRunnable, delayMs);
    }

    private void scanCurrentWindow() {
        if (!ReaderState.isReaderEnabled(this)) {
            return;
        }
        AccessibilityNodeInfo root = getRootInActiveWindow();
        if (root == null) {
            ReaderState.updateCounts(this, 0, 0, 0);
            return;
        }
        try {
            AccessibilityTextExtractor.Result result = AccessibilityTextExtractor.extract(root);
            ReaderState.updateCounts(this, result.extractedNodeCount, result.candidateNodeCount, result.excludedNodeCount);
            String snapshot = result.snapshotFingerprint();
            if (snapshot.equals(lastSnapshotFingerprint)) {
                DiagnosticStore.get().event("snapshotSkipped", snapshot);
                return;
            }
            boolean deferred = enqueueStableCandidates(result.candidates);
            if (deferred) {
                scheduleScan(STREAMING_STABLE_MS);
            } else {
                lastSnapshotFingerprint = snapshot;
            }
            speakNextIfIdle();
        } catch (RuntimeException e) {
            DiagnosticStore.get().error(e);
        } finally {
            root.recycle();
        }
    }

    private boolean enqueueStableCandidates(List<String> candidates) {
        long now = System.currentTimeMillis();
        boolean deferred = false;
        for (int i = 0; i < candidates.size(); i++) {
            String candidate = candidates.get(i);
            boolean trailing = i == candidates.size() - 1;
            if (trailing && shouldDeferStreamingTail(candidate, now)) {
                DiagnosticStore.get().event("streamingTailDeferred", TextNormalizer.fingerprint(candidate));
                deferred = true;
                continue;
            }
            if (ReaderState.shouldIncludeDiagnosticSnippets(this)) {
                DiagnosticStore.get().candidateSnippet(candidate);
            }
            if (speechQueue.enqueue(candidate)) {
                ReaderState.incrementQueued(this);
            } else {
                ReaderState.incrementSkipped(this);
            }
        }
        return deferred;
    }

    private boolean shouldDeferStreamingTail(String candidate, long now) {
        if (lastTrailingText.isEmpty()
                || (!candidate.startsWith(lastTrailingText) && !lastTrailingText.startsWith(candidate))) {
            lastTrailingText = candidate;
            lastTrailingChangedAt = now;
            return true;
        }
        if (!candidate.equals(lastTrailingText)) {
            lastTrailingText = candidate;
            lastTrailingChangedAt = now;
            return true;
        }
        return now - lastTrailingChangedAt < STREAMING_STABLE_MS;
    }

    private void speakNextIfIdle() {
        if (!ReaderState.isReaderEnabled(this) || !ttsReady || tts == null || speechQueue.hasCurrent()) {
            return;
        }
        SpeechQueue.Utterance utterance = speechQueue.next();
        if (utterance == null) {
            ReaderState.setTtsState(this, "ready");
            ReaderNotificationController.update(this);
            return;
        }
        tts.setSpeechRate(ReaderSettingsRepository.getSpeechRate(this));
        int result = tts.speak(utterance.text, TextToSpeech.QUEUE_FLUSH, null, utterance.id);
        if (result != TextToSpeech.SUCCESS) {
            speechQueue.markError(utterance.id);
            ReaderState.incrementTtsError(this);
            ReaderState.setTtsState(this, "error");
            speakNextIfIdle();
        }
    }

    private void stopPlayback(String ttsState, boolean keepNotification) {
        handler.removeCallbacks(scanRunnable);
        speechQueue.stopPending();
        if (tts != null) {
            tts.stop();
        }
        ReaderState.setTtsState(this, ttsState);
        if (keepNotification) {
            ReaderNotificationController.update(this);
        } else {
            ReaderNotificationController.cancel(this);
        }
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
}

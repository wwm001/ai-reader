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
import android.widget.Toast;
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
    private FloatingReaderController floatingReaderController;
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
    private boolean forceRescan;
    private boolean destroyedByShutdown;
    private int scrollToTopAttempts;
    private String scrollToTopLastSnapshot = "";
    private int scrollToTopSameSnapshotCount;

    @Override
    public void onCreate() {
        super.onCreate();
        floatingReaderController = new FloatingReaderController(this);
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
        updateOverlayVisibility(targetDetected);
        if (event.getEventType() == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED
                || event.getEventType() == AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED) {
            DiagnosticCounters.inc(DiagnosticCounters.CONTENT_CHANGED_EVENT);
        } else if (event.getEventType() == AccessibilityEvent.TYPE_VIEW_SCROLLED) {
            DiagnosticCounters.inc(DiagnosticCounters.SCROLL_EVENT);
        }
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
        if (floatingReaderController != null) {
            floatingReaderController.destroy();
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
            if (ReaderState.getMode(this) == ReaderMode.SHUTDOWN) {
                ReaderState.setMode(this, ReaderMode.READY);
            }
            handler.post(() -> {
                if (ReaderState.isReaderEnabled(ChatGptAccessibilityService.this)) {
                    forceRescanNow();
                }
                speakNextIfIdle();
            });
        } else {
            ReaderState.setTtsState(this, "error");
            DiagnosticStore.get().event("ttsInitFailed", String.valueOf(status));
        }
    }

    private void handleCommand(String command) {
        if (ReaderCommandBus.COMMAND_ON.equals(command) || ReaderCommandBus.COMMAND_RESUME.equals(command)) {
            if (tts == null) {
                destroyedByShutdown = false;
                tts = new TextToSpeech(this, this);
                ttsReady = false;
            }
            ReaderMode previous = ReaderState.getMode(this);
            if (previous == ReaderMode.PAUSED) {
                DiagnosticCounters.inc(DiagnosticCounters.RESUME_FROM_PAUSE);
            } else if (previous == ReaderMode.STOPPED) {
                DiagnosticCounters.inc(DiagnosticCounters.RESUME_FROM_OFF);
            }
            ReaderState.setMode(this, ReaderMode.PLAYING);
            ReaderState.setTtsState(this, "ready");
            if (ReaderState.isTargetDetected(this) && floatingReaderController != null) {
                floatingReaderController.show();
            }
            ReaderNotificationController.update(this);
            forceRescanNow();
        } else if (ReaderCommandBus.COMMAND_PAUSE.equals(command)) {
            ReaderState.setMode(this, ReaderMode.PAUSED);
            pausePlayback();
        } else if (ReaderCommandBus.COMMAND_OFF.equals(command) || ReaderCommandBus.COMMAND_STOP.equals(command)) {
            ReaderState.setMode(this, ReaderMode.STOPPED);
            stopPlayback("stopped", false);
        } else if (ReaderCommandBus.COMMAND_RESET_READ.equals(command)) {
            speechQueue.resetReadHistory();
            ReaderState.resetReadState(this);
            if (ReaderState.isReaderEnabled(this)) {
                forceRescanNow();
            }
            ReaderNotificationController.update(this);
        } else if (ReaderCommandBus.COMMAND_RESCAN.equals(command)) {
            if (ReaderState.isReaderEnabled(this)) {
                forceRescanNow();
            }
        } else if (ReaderCommandBus.COMMAND_RATE_CHANGED.equals(command)) {
            if (tts != null) {
                tts.setSpeechRate(ReaderSettingsRepository.getSpeechRate(this));
                if (ReaderState.getMode(this) == ReaderMode.PLAYING) {
                    speechQueue.requeueCurrentForRateChange();
                    tts.stop();
                    speakNextIfIdle();
                }
            }
            ReaderNotificationController.update(this);
        } else if (ReaderCommandBus.COMMAND_SCROLL_TOP.equals(command)) {
            scrollToTopAndRestart();
        } else if (ReaderCommandBus.COMMAND_SHUTDOWN.equals(command)) {
            shutdownReaderByHold();
        }
    }

    private void scheduleScan(long delayMs) {
        handler.removeCallbacks(scanRunnable);
        DiagnosticCounters.inc(DiagnosticCounters.DEBOUNCED_SCAN);
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
            AccessibilityTextExtractor.Result result = AccessibilityTextExtractor.extract(
                    root,
                    getResources().getDisplayMetrics().density,
                    getResources().getDisplayMetrics().heightPixels);
            ReaderState.updateCounts(this, result.extractedNodeCount, result.candidateNodeCount, result.excludedNodeCount);
            String snapshot = result.snapshotFingerprint();
            if (!forceRescan && snapshot.equals(lastSnapshotFingerprint)) {
                DiagnosticStore.get().event("snapshotSkipped", snapshot);
                DiagnosticCounters.inc(DiagnosticCounters.SKIPPED_SAME_SNAPSHOT);
                return;
            }
            forceRescan = false;
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
                DiagnosticCounters.inc(DiagnosticCounters.STREAMING_DEFERRED);
                deferred = true;
                continue;
            }
            if (ReaderState.shouldIncludeDiagnosticSnippets(this)) {
                DiagnosticStore.get().candidateSnippet(candidate);
            }
            SpeechQueue.EnqueueResult enqueueResult = speechQueue.enqueueDetailed(candidate);
            if (enqueueResult == SpeechQueue.EnqueueResult.ENQUEUED) {
                ReaderState.incrementQueued(this);
                updateQueueDiagnostics();
            } else {
                ReaderState.incrementSkipped(this);
                if (enqueueResult == SpeechQueue.EnqueueResult.DUPLICATE_SPOKEN) {
                    DiagnosticCounters.inc(DiagnosticCounters.SKIPPED_SPOKEN_DUPLICATE);
                    DiagnosticCounters.inc(DiagnosticCounters.EXCLUDED_ALREADY_SPOKEN);
                } else if (enqueueResult == SpeechQueue.EnqueueResult.DUPLICATE_PENDING_OR_CURRENT) {
                    DiagnosticCounters.inc(DiagnosticCounters.SKIPPED_PENDING_DUPLICATE);
                }
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
        updateQueueDiagnostics();
        if (utterance == null) {
            ReaderState.setTtsState(this, "ready");
            if (ReaderState.getMode(this) == ReaderMode.PLAYING) {
                ReaderState.setMode(this, ReaderMode.READY);
            }
            ReaderNotificationController.update(this);
            return;
        }
        ReaderState.setReadCursor(this, utterance.fingerprint);
        updateQueueDiagnostics();
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
        updateQueueDiagnostics();
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

    private void pausePlayback() {
        handler.removeCallbacks(scanRunnable);
        speechQueue.pauseAndKeepCurrent();
        updateQueueDiagnostics();
        if (tts != null) {
            tts.stop();
        }
        ReaderState.setTtsState(this, "paused");
        ReaderNotificationController.update(this);
    }

    private void forceRescanNow() {
        forceRescan = true;
        lastSnapshotFingerprint = "";
        DiagnosticCounters.inc(DiagnosticCounters.FORCE_RESCAN);
        scheduleScan(0L);
        speakNextIfIdle();
    }

    private void updateQueueDiagnostics() {
        DiagnosticStore store = DiagnosticStore.get();
        store.gauge("pendingQueueSize", speechQueue.pendingSize());
        store.gauge("spokenFingerprintCount", speechQueue.spokenSize());
        store.gauge("pendingFingerprintCount", speechQueue.pendingFingerprintSize());
        store.gauge("currentlySpeakingFingerprint", speechQueue.currentFingerprint());
    }

    private void updateOverlayVisibility(boolean targetDetected) {
        if (destroyedByShutdown || ReaderState.getMode(this) == ReaderMode.SHUTDOWN) {
            if (floatingReaderController != null) {
                floatingReaderController.hide();
            }
            return;
        }
        if (floatingReaderController == null) {
            return;
        }
        if (targetDetected) {
            floatingReaderController.show();
        } else {
            floatingReaderController.hide();
        }
    }

    private void scrollToTopAndRestart() {
        if (!ReaderState.isTargetDetected(this)) {
            return;
        }
        DiagnosticCounters.inc(DiagnosticCounters.SCROLL_TOP_REQUESTED);
        stopPlayback("scrollingToTop", true);
        speechQueue.resetReadHistory();
        ReaderState.resetReadState(this);
        scrollToTopAttempts = 0;
        scrollToTopSameSnapshotCount = 0;
        scrollToTopLastSnapshot = "";
        ReaderState.setMode(this, ReaderMode.PLAYING);
        scrollTopStep();
    }

    private void scrollTopStep() {
        if (scrollToTopAttempts++ >= 18) {
            finishScrollTop(false);
            return;
        }
        AccessibilityNodeInfo root = getRootInActiveWindow();
        if (root == null) {
            finishScrollTop(false);
            return;
        }
        try {
            AccessibilityTextExtractor.Result result = AccessibilityTextExtractor.extract(
                    root,
                    getResources().getDisplayMetrics().density,
                    getResources().getDisplayMetrics().heightPixels);
            String snapshot = result.snapshotFingerprint();
            if (snapshot.equals(scrollToTopLastSnapshot)) {
                scrollToTopSameSnapshotCount++;
            } else {
                scrollToTopSameSnapshotCount = 0;
                scrollToTopLastSnapshot = snapshot;
            }
            boolean moved = ChatGptScrollController.scrollBackwardOnce(root);
            if (!moved || scrollToTopSameSnapshotCount >= 2) {
                finishScrollTop(true);
                return;
            }
            handler.postDelayed(this::scrollTopStep, 350L);
        } finally {
            root.recycle();
        }
    }

    private void finishScrollTop(boolean success) {
        if (success) {
            DiagnosticCounters.inc(DiagnosticCounters.SCROLL_TOP_SUCCEEDED);
            ReaderState.setMode(this, ReaderMode.PLAYING);
            forceRescanNow();
        } else {
            DiagnosticCounters.inc(DiagnosticCounters.SCROLL_TOP_FAILED);
            Toast.makeText(this, "チャット先頭へ戻れませんでした", Toast.LENGTH_SHORT).show();
            ReaderState.setMode(this, ReaderMode.PAUSED);
            ReaderNotificationController.update(this);
        }
    }

    private void shutdownReaderByHold() {
        destroyedByShutdown = true;
        DiagnosticStore.get().event("readerShutdownByFiveSecondHold", "true");
        stopPlayback("shutdown", false);
        ReaderState.setMode(this, ReaderMode.SHUTDOWN);
        if (floatingReaderController != null) {
            floatingReaderController.hide();
        }
        if (tts != null) {
            tts.shutdown();
            ttsReady = false;
            tts = null;
        }
        handler.removeCallbacksAndMessages(null);
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

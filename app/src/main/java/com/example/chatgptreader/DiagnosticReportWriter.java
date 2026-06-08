package com.example.chatgptreader;

import android.content.Context;
import android.net.Uri;
import android.os.Build;

import androidx.core.content.FileProvider;

import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;

public final class DiagnosticReportWriter {
    private DiagnosticReportWriter() {
    }

    public static Uri write(Context context, boolean includeSnippets) throws Exception {
        JSONObject report = new JSONObject();
        DiagnosticStore store = DiagnosticStore.get();
        report.put("generatedAt", DiagnosticStore.isoNow());
        report.put("appVersion", BuildConfig.VERSION_NAME + " (" + BuildConfig.VERSION_CODE + ")");
        report.put("buildCommitSha", BuildConfig.BUILD_COMMIT_SHA);
        report.put("deviceManufacturer", Build.MANUFACTURER);
        report.put("deviceModel", Build.MODEL);
        report.put("androidVersion", Build.VERSION.RELEASE);
        report.put("sdkInt", Build.VERSION.SDK_INT);
        report.put("accessibilityServiceEnabled", AccessibilityStatus.isServiceEnabled(context));
        report.put("accessibilityServiceConfigured", AccessibilityStatus.isServiceConfigured(context));
        report.put("accessibilityServiceConnected", AccessibilityStatus.isServiceConnected(context));
        report.put("accessibilityServiceEnabledRawValue", ReaderState.getAccessibilityRawValue(context));
        report.put("readerState", ReaderState.getMode(context).name());
        report.put("readerEnabled", ReaderState.isReaderEnabled(context));
        report.put("readerMode", ReaderState.getMode(context).name());
        report.put("speechRate", ReaderSettingsRepository.getSpeechRate(context));
        report.put("ttsState", ReaderState.getTtsState(context));
        report.put("targetPackage", ReaderState.TARGET_PACKAGE);
        report.put("targetPackageDetected", ReaderState.isTargetDetected(context));
        report.put("overlayVisible", ReaderState.isOverlayVisible(context));
        report.put("overlayPosition", ReaderState.getOverlayPositionText(context));
        report.put("eventTypeCounts", store.eventTypeCountsJson());
        report.put("diagnosticCounters", store.countersJson());
        report.put("extractedNodeCount", ReaderState.getExtractedCount(context));
        report.put("candidateNodeCount", ReaderState.getCandidateCount(context));
        report.put("excludedNodeCount", ReaderState.getExcludedCount(context));
        report.put("pendingQueueSize", store.gaugeInt("pendingQueueSize"));
        report.put("spokenFingerprintCount", store.gaugeInt("spokenFingerprintCount"));
        report.put("pendingFingerprintCount", store.gaugeInt("pendingFingerprintCount"));
        report.put("currentlySpeakingFingerprint", store.gaugeString("currentlySpeakingFingerprint"));
        report.put("readCursor", ReaderState.getReadCursor(context));
        report.put("contentChangedEventCount", store.counter(DiagnosticCounters.CONTENT_CHANGED_EVENT));
        report.put("scrollEventCount", store.counter(DiagnosticCounters.SCROLL_EVENT));
        report.put("debouncedScanCount", store.counter(DiagnosticCounters.DEBOUNCED_SCAN));
        report.put("skippedSameSnapshotCount", store.counter(DiagnosticCounters.SKIPPED_SAME_SNAPSHOT));
        report.put("skippedPendingDuplicateCount", store.counter(DiagnosticCounters.SKIPPED_PENDING_DUPLICATE));
        report.put("skippedSpokenDuplicateCount", store.counter(DiagnosticCounters.SKIPPED_SPOKEN_DUPLICATE));
        report.put("streamingParagraphDeferredCount", store.counter(DiagnosticCounters.STREAMING_DEFERRED));
        report.put("notificationUpdateCount", store.counter(DiagnosticCounters.NOTIFICATION_UPDATE));
        report.put("forceRescanCount", store.counter(DiagnosticCounters.FORCE_RESCAN));
        report.put("resumeFromPauseCount", store.counter(DiagnosticCounters.RESUME_FROM_PAUSE));
        report.put("resumeFromOffCount", store.counter(DiagnosticCounters.RESUME_FROM_OFF));
        report.put("scrollToTopRequestedCount", store.counter(DiagnosticCounters.SCROLL_TOP_REQUESTED));
        report.put("scrollToTopSucceededCount", store.counter(DiagnosticCounters.SCROLL_TOP_SUCCEEDED));
        report.put("scrollToTopFailedCount", store.counter(DiagnosticCounters.SCROLL_TOP_FAILED));
        report.put("floatingSingleTapCount", store.counter(DiagnosticCounters.FLOATING_SINGLE_TAP));
        report.put("floatingDoubleTapCount", store.counter(DiagnosticCounters.FLOATING_DOUBLE_TAP));
        report.put("floatingLongPressSpeedMenuCount", store.counter(DiagnosticCounters.FLOATING_LONG_PRESS_SPEED));
        report.put("floatingFiveSecondShutdownCount", store.counter(DiagnosticCounters.FLOATING_SHUTDOWN));
        report.put("excludedByTopToolbarRegion", store.counter(DiagnosticCounters.EXCLUDED_TOP_TOOLBAR));
        report.put("excludedByBottomComposerRegion", store.counter(DiagnosticCounters.EXCLUDED_BOTTOM_COMPOSER));
        report.put("excludedByClickableUi", store.counter(DiagnosticCounters.EXCLUDED_CLICKABLE_UI));
        report.put("excludedByEditableUi", store.counter(DiagnosticCounters.EXCLUDED_EDITABLE_UI));
        report.put("excludedByKnownUiLabel", store.counter(DiagnosticCounters.EXCLUDED_KNOWN_UI));
        report.put("excludedByDuplicate", store.counter(DiagnosticCounters.EXCLUDED_DUPLICATE));
        report.put("excludedByAlreadySpoken", store.counter(DiagnosticCounters.EXCLUDED_ALREADY_SPOKEN));
        report.put("queuedUtteranceCount", ReaderState.getQueuedCount(context));
        report.put("startedUtteranceCount", ReaderState.getStartedCount(context));
        report.put("spokenUtteranceCount", ReaderState.getSpokenCount(context));
        report.put("skippedAsReadCount", ReaderState.getSkippedCount(context));
        report.put("ttsErrorCount", ReaderState.getTtsErrorCount(context));
        report.put("recentErrors", store.recentErrorsJson());
        report.put("recentDiagnosticEvents", store.recentEventsJson());
        if (includeSnippets) {
            report.put("candidateTextSnippetsFirst80Chars", store.candidateSnippetsJson());
        }

        File dir = new File(context.getCacheDir(), "diagnostics");
        if (!dir.exists() && !dir.mkdirs()) {
            throw new IllegalStateException("Could not create diagnostics cache directory");
        }
        File file = new File(dir, "chatgpt-reader-diagnostic.json");
        try (FileOutputStream output = new FileOutputStream(file, false)) {
            output.write(report.toString(2).getBytes(StandardCharsets.UTF_8));
        }
        return FileProvider.getUriForFile(context, context.getPackageName() + ".fileprovider", file);
    }
}

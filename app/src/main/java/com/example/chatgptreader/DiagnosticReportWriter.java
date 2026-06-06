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
        report.put("readerEnabled", ReaderState.isReaderEnabled(context));
        report.put("ttsState", ReaderState.getTtsState(context));
        report.put("targetPackage", ReaderState.TARGET_PACKAGE);
        report.put("targetPackageDetected", ReaderState.isTargetDetected(context));
        report.put("eventTypeCounts", store.eventTypeCountsJson());
        report.put("extractedNodeCount", ReaderState.getExtractedCount(context));
        report.put("candidateNodeCount", ReaderState.getCandidateCount(context));
        report.put("excludedNodeCount", ReaderState.getExcludedCount(context));
        report.put("queuedUtteranceCount", ReaderState.getQueuedCount(context));
        report.put("spokenUtteranceCount", ReaderState.getSpokenCount(context));
        report.put("skippedAsReadCount", ReaderState.getSkippedCount(context));
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

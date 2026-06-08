package com.example.chatgptreader;

public final class DiagnosticCounters {
    public static final String CONTENT_CHANGED_EVENT = "contentChangedEventCount";
    public static final String SCROLL_EVENT = "scrollEventCount";
    public static final String DEBOUNCED_SCAN = "debouncedScanCount";
    public static final String SKIPPED_SAME_SNAPSHOT = "skippedSameSnapshotCount";
    public static final String SKIPPED_PENDING_DUPLICATE = "skippedPendingDuplicateCount";
    public static final String SKIPPED_SPOKEN_DUPLICATE = "skippedSpokenDuplicateCount";
    public static final String STREAMING_DEFERRED = "streamingParagraphDeferredCount";
    public static final String NOTIFICATION_UPDATE = "notificationUpdateCount";
    public static final String FORCE_RESCAN = "forceRescanCount";
    public static final String RESUME_FROM_PAUSE = "resumeFromPauseCount";
    public static final String RESUME_FROM_OFF = "resumeFromOffCount";
    public static final String SCROLL_TOP_REQUESTED = "scrollToTopRequestedCount";
    public static final String SCROLL_TOP_SUCCEEDED = "scrollToTopSucceededCount";
    public static final String SCROLL_TOP_FAILED = "scrollToTopFailedCount";
    public static final String FLOATING_SINGLE_TAP = "floatingSingleTapCount";
    public static final String FLOATING_DOUBLE_TAP = "floatingDoubleTapCount";
    public static final String FLOATING_LONG_PRESS_SPEED = "floatingLongPressSpeedMenuCount";
    public static final String FLOATING_SHUTDOWN = "floatingFiveSecondShutdownCount";

    public static final String EXCLUDED_TOP_TOOLBAR = "excludedByTopToolbarRegion";
    public static final String EXCLUDED_BOTTOM_COMPOSER = "excludedByBottomComposerRegion";
    public static final String EXCLUDED_CLICKABLE_UI = "excludedByClickableUi";
    public static final String EXCLUDED_EDITABLE_UI = "excludedByEditableUi";
    public static final String EXCLUDED_KNOWN_UI = "excludedByKnownUiLabel";
    public static final String EXCLUDED_DUPLICATE = "excludedByDuplicate";
    public static final String EXCLUDED_ALREADY_SPOKEN = "excludedByAlreadySpoken";

    private DiagnosticCounters() {
    }

    public static void inc(String name) {
        DiagnosticStore.get().increment(name);
    }
}

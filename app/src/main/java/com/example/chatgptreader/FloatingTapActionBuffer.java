package com.example.chatgptreader;

public final class FloatingTapActionBuffer {
    private boolean pendingSingle;

    public void onSingleCandidate() {
        pendingSingle = true;
    }

    public boolean onDoubleTap() {
        boolean canceledSingle = pendingSingle;
        pendingSingle = false;
        return canceledSingle;
    }

    public boolean consumeSingleIfPending() {
        if (!pendingSingle) {
            return false;
        }
        pendingSingle = false;
        return true;
    }

    public boolean hasPendingSingle() {
        return pendingSingle;
    }
}

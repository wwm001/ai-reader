package com.example.chatgptreader;

public enum ReaderMode {
    READY,
    PLAYING,
    PAUSED,
    STOPPED,
    SHUTDOWN,
    ON,
    OFF;

    public boolean isReadingEnabled() {
        return this == PLAYING || this == ON;
    }

    public boolean keepsNotification() {
        return this == PLAYING || this == PAUSED || this == READY || this == ON;
    }

    public ReaderMode normalized() {
        if (this == ON) {
            return PLAYING;
        }
        if (this == OFF) {
            return STOPPED;
        }
        return this;
    }
}

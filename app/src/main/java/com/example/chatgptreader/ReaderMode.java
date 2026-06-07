package com.example.chatgptreader;

public enum ReaderMode {
    ON,
    PAUSED,
    OFF;

    public boolean isReadingEnabled() {
        return this == ON;
    }
}

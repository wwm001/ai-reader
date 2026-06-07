package com.example.chatgptreader;

import android.content.Context;
import android.content.Intent;

public final class ReaderCommandBus {
    public static final String ACTION_COMMAND = "com.example.chatgptreader.action.READER_COMMAND";
    public static final String EXTRA_COMMAND = "command";

    public static final String COMMAND_ON = "ON";
    public static final String COMMAND_OFF = "OFF";
    public static final String COMMAND_PAUSE = "PAUSE";
    public static final String COMMAND_RESUME = "RESUME";
    public static final String COMMAND_STOP = "STOP";
    public static final String COMMAND_RESET_READ = "RESET_READ";
    public static final String COMMAND_RESCAN = "RESCAN";
    public static final String COMMAND_RATE_CHANGED = "RATE_CHANGED";

    private ReaderCommandBus() {
    }

    public static void send(Context context, String command) {
        Intent intent = new Intent(ACTION_COMMAND);
        intent.setPackage(context.getPackageName());
        intent.putExtra(EXTRA_COMMAND, command);
        context.getApplicationContext().sendBroadcast(intent);
        DiagnosticStore.get().event("readerCommand", command);
    }
}

package com.example.chatgptreader;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class ReaderActionReceiver extends BroadcastReceiver {
    public static final String ACTION_PAUSE = "com.example.chatgptreader.action.PAUSE";
    public static final String ACTION_RESUME = "com.example.chatgptreader.action.RESUME";
    public static final String ACTION_STOP = "com.example.chatgptreader.action.STOP";
    public static final String ACTION_RESET_READ = "com.example.chatgptreader.action.RESET_READ";

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent == null ? "" : intent.getAction();
        if (ACTION_PAUSE.equals(action) || ACTION_STOP.equals(action)) {
            ReaderState.setReaderEnabled(context, false);
            ReaderState.setTtsState(context, ACTION_STOP.equals(action) ? "stopped" : "paused");
        } else if (ACTION_RESUME.equals(action)) {
            ReaderState.setReaderEnabled(context, true);
            ReaderState.setTtsState(context, "ready");
        } else if (ACTION_RESET_READ.equals(action)) {
            ReaderState.resetReadState(context);
        }
        ReaderNotificationController.update(context);
    }
}

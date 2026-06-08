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
        if (ACTION_PAUSE.equals(action)) {
            ReaderState.setMode(context, ReaderMode.PAUSED);
            ReaderCommandBus.send(context, ReaderCommandBus.COMMAND_PAUSE);
        } else if (ACTION_STOP.equals(action)) {
            ReaderState.setMode(context, ReaderMode.STOPPED);
            ReaderCommandBus.send(context, ReaderCommandBus.COMMAND_STOP);
        } else if (ACTION_RESUME.equals(action)) {
            ReaderState.setMode(context, ReaderMode.PLAYING);
            ReaderCommandBus.send(context, ReaderCommandBus.COMMAND_RESUME);
        } else if (ACTION_RESET_READ.equals(action)) {
            ReaderCommandBus.send(context, ReaderCommandBus.COMMAND_RESET_READ);
        }
        ReaderNotificationController.update(context);
    }
}

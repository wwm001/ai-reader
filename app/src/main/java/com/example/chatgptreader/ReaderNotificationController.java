package com.example.chatgptreader;

import android.annotation.SuppressLint;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

public final class ReaderNotificationController {
    private static final String CHANNEL_ID = "reader_status";
    private static final int NOTIFICATION_ID = 1001;

    private ReaderNotificationController() {
    }

    @SuppressLint("MissingPermission")
    public static void update(Context context) {
        if (!ReaderState.isReaderEnabled(context) && !"speaking".equals(ReaderState.getTtsState(context))) {
            NotificationManagerCompat.from(context).cancel(NOTIFICATION_ID);
            return;
        }
        if (Build.VERSION.SDK_INT >= 33
                && context.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        ensureChannel(context);
        Intent open = new Intent(context, MainActivity.class);
        PendingIntent contentIntent = PendingIntent.getActivity(
                context, 0, open, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_stat_reader)
                .setContentTitle("ChatGPT Reader")
                .setContentText(ReaderState.isReaderEnabled(context) ? "有効" : "一時停止中")
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setOngoing(false)
                .setContentIntent(contentIntent);

        if (ReaderState.isReaderEnabled(context)) {
            builder.addAction(action(context, "一時停止", ReaderActionReceiver.ACTION_PAUSE, 1));
            builder.addAction(action(context, "停止", ReaderActionReceiver.ACTION_STOP, 2));
        } else {
            builder.addAction(action(context, "再開", ReaderActionReceiver.ACTION_RESUME, 3));
        }
        builder.addAction(action(context, "既読リセット", ReaderActionReceiver.ACTION_RESET_READ, 4));
        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, builder.build());
    }

    private static NotificationCompat.Action action(Context context, String title, String action, int requestCode) {
        Intent intent = new Intent(context, ReaderActionReceiver.class);
        intent.setAction(action);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context, requestCode, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        return new NotificationCompat.Action.Builder(R.drawable.ic_stat_reader, title, pendingIntent).build();
    }

    private static void ensureChannel(Context context) {
        if (Build.VERSION.SDK_INT < 26) {
            return;
        }
        NotificationManager manager = context.getSystemService(NotificationManager.class);
        NotificationChannel existing = manager.getNotificationChannel(CHANNEL_ID);
        if (existing != null) {
            return;
        }
        NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                context.getString(R.string.notification_channel_reader),
                NotificationManager.IMPORTANCE_LOW);
        manager.createNotificationChannel(channel);
    }
}

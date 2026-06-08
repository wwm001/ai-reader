package com.example.chatgptreader;

import android.content.Context;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.TextView;

public final class FloatingReaderController {
    private static final int BUTTON_DP = 60;
    private static final int MOVE_SLOP_DP = 8;

    private final Context context;
    private final WindowManager windowManager;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final FloatingReaderGestureDetector detector = new FloatingReaderGestureDetector();
    private FloatingReaderButtonView button;
    private View speedMenu;
    private WindowManager.LayoutParams params;
    private long downAt;
    private int startX;
    private int startY;
    private float startRawX;
    private float startRawY;
    private boolean moved;
    private boolean shutdownFired;

    private final Runnable holdProgress = new Runnable() {
        @Override
        public void run() {
            if (button == null || downAt == 0L) {
                return;
            }
            long elapsed = SystemClock.uptimeMillis() - downAt;
            button.setHoldProgress(elapsed / 5000f);
            if (elapsed >= FloatingReaderGestureDetector.SHUTDOWN_MS && !shutdownFired) {
                shutdownFired = true;
                button.shutdownHaptic();
                DiagnosticCounters.inc(DiagnosticCounters.FLOATING_SHUTDOWN);
                ReaderCommandBus.send(context, ReaderCommandBus.COMMAND_SHUTDOWN);
                return;
            }
            handler.postDelayed(this, 50L);
        }
    };

    public FloatingReaderController(Context context) {
        this.context = context;
        windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
    }

    public boolean isVisible() {
        return button != null;
    }

    public void show() {
        if (button != null || ReaderState.getMode(context) == ReaderMode.SHUTDOWN) {
            return;
        }
        button = new FloatingReaderButtonView(context);
        int size = dp(BUTTON_DP);
        params = new WindowManager.LayoutParams(
                size,
                size,
                WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                PixelFormat.TRANSLUCENT);
        params.gravity = Gravity.TOP | Gravity.START;
        params.x = ReaderState.getOverlayX(context, dp(300));
        params.y = ReaderState.getOverlayY(context, dp(560));
        button.setOnTouchListener(this::onTouch);
        windowManager.addView(button, params);
        ReaderState.setOverlayVisible(context, true);
        DiagnosticStore.get().event("overlayVisible", "true");
    }

    public void hide() {
        closeSpeedMenu();
        if (button != null) {
            windowManager.removeView(button);
            button = null;
            ReaderState.setOverlayVisible(context, false);
            DiagnosticStore.get().event("overlayVisible", "false");
        }
    }

    public void destroy() {
        handler.removeCallbacksAndMessages(null);
        hide();
    }

    private boolean onTouch(View view, MotionEvent event) {
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                closeSpeedMenu();
                downAt = SystemClock.uptimeMillis();
                startX = params.x;
                startY = params.y;
                startRawX = event.getRawX();
                startRawY = event.getRawY();
                moved = false;
                shutdownFired = false;
                button.shortHaptic();
                handler.post(holdProgress);
                return true;
            case MotionEvent.ACTION_MOVE:
                float dx = event.getRawX() - startRawX;
                float dy = event.getRawY() - startRawY;
                if (Math.hypot(dx, dy) > dp(MOVE_SLOP_DP)) {
                    moved = true;
                    params.x = startX + Math.round(dx);
                    params.y = startY + Math.round(dy);
                    windowManager.updateViewLayout(button, params);
                }
                return true;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                long duration = SystemClock.uptimeMillis() - downAt;
                downAt = 0L;
                handler.removeCallbacks(holdProgress);
                if (button != null) {
                    button.setHoldProgress(0f);
                }
                if (moved) {
                    snapAndSave();
                }
                if (shutdownFired) {
                    return true;
                }
                handleGesture(detector.classifyRelease(duration, moved, SystemClock.uptimeMillis()));
                return true;
            default:
                return false;
        }
    }

    private void handleGesture(FloatingReaderGestureDetector.Gesture gesture) {
        if (gesture == FloatingReaderGestureDetector.Gesture.SINGLE_TAP) {
            DiagnosticCounters.inc(DiagnosticCounters.FLOATING_SINGLE_TAP);
            ReaderMode mode = ReaderState.getMode(context);
            if (mode == ReaderMode.PLAYING) {
                ReaderCommandBus.send(context, ReaderCommandBus.COMMAND_PAUSE);
            } else {
                ReaderCommandBus.send(context, ReaderCommandBus.COMMAND_RESUME);
            }
        } else if (gesture == FloatingReaderGestureDetector.Gesture.DOUBLE_TAP) {
            DiagnosticCounters.inc(DiagnosticCounters.FLOATING_DOUBLE_TAP);
            ReaderCommandBus.send(context, ReaderCommandBus.COMMAND_SCROLL_TOP);
        } else if (gesture == FloatingReaderGestureDetector.Gesture.LONG_PRESS_SPEED) {
            DiagnosticCounters.inc(DiagnosticCounters.FLOATING_LONG_PRESS_SPEED);
            showSpeedMenu();
        }
    }

    private void showSpeedMenu() {
        closeSpeedMenu();
        LinearLayout menu = new LinearLayout(context);
        menu.setOrientation(LinearLayout.VERTICAL);
        menu.setBackgroundColor(Color.argb(235, 32, 38, 46));
        float[] speeds = {0.8f, 1.0f, 1.2f, 1.5f, 2.0f};
        for (float speed : speeds) {
            TextView item = new TextView(context);
            item.setText(ReaderSettingsRepository.formatRate(speed));
            item.setTextColor(Math.abs(speed - ReaderSettingsRepository.getSpeechRate(context)) < 0.01f ? Color.CYAN : Color.WHITE);
            item.setTextSize(18);
            item.setPadding(dp(18), dp(10), dp(18), dp(10));
            item.setOnClickListener(v -> {
                ReaderSettingsRepository.setSpeechRate(context, speed);
                closeSpeedMenu();
            });
            menu.addView(item);
        }
        WindowManager.LayoutParams menuParams = new WindowManager.LayoutParams(
                dp(116),
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT);
        menuParams.gravity = Gravity.TOP | Gravity.START;
        menuParams.x = Math.max(0, params.x - dp(124));
        menuParams.y = Math.max(0, params.y - dp(96));
        speedMenu = menu;
        windowManager.addView(speedMenu, menuParams);
    }

    private void closeSpeedMenu() {
        if (speedMenu != null) {
            windowManager.removeView(speedMenu);
            speedMenu = null;
        }
    }

    private void snapAndSave() {
        int screenWidth = context.getResources().getDisplayMetrics().widthPixels;
        params.x = params.x < screenWidth / 2 ? dp(8) : Math.max(dp(8), screenWidth - dp(BUTTON_DP + 8));
        windowManager.updateViewLayout(button, params);
        ReaderState.setOverlayPosition(context, params.x, params.y);
    }

    private int dp(int value) {
        return Math.round(value * context.getResources().getDisplayMetrics().density);
    }
}

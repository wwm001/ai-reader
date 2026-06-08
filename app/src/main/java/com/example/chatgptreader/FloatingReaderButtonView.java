package com.example.chatgptreader;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.view.HapticFeedbackConstants;
import android.view.View;

public class FloatingReaderButtonView extends View {
    private final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint progress = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint text = new Paint(Paint.ANTI_ALIAS_FLAG);
    private float holdProgress;

    public FloatingReaderButtonView(Context context) {
        super(context);
        fill.setColor(Color.argb(230, 15, 118, 110));
        progress.setColor(Color.WHITE);
        progress.setStyle(Paint.Style.STROKE);
        progress.setStrokeWidth(dp(4));
        text.setColor(Color.WHITE);
        text.setTextAlign(Paint.Align.CENTER);
        text.setTextSize(dp(22));
        setHapticFeedbackEnabled(true);
        setContentDescription("ChatGPT Reader");
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float radius = Math.min(getWidth(), getHeight()) / 2f - dp(2);
        canvas.drawCircle(getWidth() / 2f, getHeight() / 2f, radius, fill);
        if (holdProgress > 0f) {
            canvas.drawArc(dp(4), dp(4), getWidth() - dp(4), getHeight() - dp(4),
                    -90, 360f * holdProgress, false, progress);
        }
        canvas.drawText("▶", getWidth() / 2f, getHeight() / 2f + dp(8), text);
    }

    public void setHoldProgress(float value) {
        holdProgress = Math.max(0f, Math.min(1f, value));
        invalidate();
    }

    public void shortHaptic() {
        performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
    }

    public void shutdownHaptic() {
        performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}

package com.example.chatgptreader;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;

public class MainActivity extends android.app.Activity {
    private static final int REQ_NOTIFICATIONS = 42;

    private TextView status;
    private TextView speedLabel;
    private CheckBox includeSnippets;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestNotificationPermissionIfNeeded();

        ScrollView scrollView = new ScrollView(this);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        int pad = dp(20);
        root.setPadding(pad, pad, pad, pad);
        scrollView.addView(root);

        TextView title = new TextView(this);
        title.setText("ChatGPT Reader");
        title.setTextSize(24);
        root.addView(title);

        status = new TextView(this);
        status.setTextSize(16);
        status.setPadding(0, dp(12), 0, dp(12));
        root.addView(status);

        Button toggle = new Button(this);
        toggle.setText("Reader ON / OFF");
        toggle.setOnClickListener(v -> {
            boolean enable = !ReaderState.isReaderEnabled(this);
            ReaderState.setMode(this, enable ? ReaderMode.ON : ReaderMode.OFF);
            ReaderCommandBus.send(this, enable ? ReaderCommandBus.COMMAND_ON : ReaderCommandBus.COMMAND_OFF);
            ReaderNotificationController.update(this);
            refreshStatus();
        });
        root.addView(toggle);

        Button accessibility = new Button(this);
        accessibility.setText("AccessibilityService 設定を開く");
        accessibility.setOnClickListener(v -> startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)));
        root.addView(accessibility);

        includeSnippets = new CheckBox(this);
        includeSnippets.setText("診断用テキスト断片を含める ON / OFF");
        includeSnippets.setChecked(ReaderState.shouldIncludeDiagnosticSnippets(this));
        includeSnippets.setOnCheckedChangeListener((buttonView, isChecked) -> ReaderState.setIncludeDiagnosticSnippets(this, isChecked));
        root.addView(includeSnippets);

        speedLabel = new TextView(this);
        speedLabel.setTextSize(16);
        speedLabel.setPadding(0, dp(12), 0, 0);
        root.addView(speedLabel);

        SeekBar speed = new SeekBar(this);
        speed.setMax(15);
        speed.setProgress(rateToProgress(ReaderSettingsRepository.getSpeechRate(this)));
        speed.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    ReaderSettingsRepository.setSpeechRate(MainActivity.this, progressToRate(progress));
                    refreshStatus();
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });
        root.addView(speed);

        Button export = new Button(this);
        export.setText("診断レポートを出力");
        export.setOnClickListener(this::shareDiagnosticReport);
        root.addView(export);

        Button clear = new Button(this);
        clear.setText("診断ログを消去");
        clear.setOnClickListener(v -> {
            DiagnosticStore.get().clear();
            Toast.makeText(this, "診断ログを消去しました", Toast.LENGTH_SHORT).show();
            refreshStatus();
        });
        root.addView(clear);

        setContentView(scrollView);
        refreshStatus();
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshStatus();
    }

    private void refreshStatus() {
        String text = "現在のアプリバージョン: " + BuildConfig.VERSION_NAME + " (" + BuildConfig.VERSION_CODE + ")\n"
                + "build commit SHA: " + BuildConfig.BUILD_COMMIT_SHA + "\n"
                + "AccessibilityService の状態: " + enabledText(AccessibilityStatus.isServiceEnabled(this)) + "\n"
                + "ChatGPT 検出状態: " + detectedText(ReaderState.isTargetDetected(this)) + "\n"
                + "TTS 状態: " + ReaderState.getTtsState(this) + "\n"
                + "Reader: " + ReaderState.getMode(this).name() + "\n"
                + "読み上げ速度: " + ReaderSettingsRepository.formatRate(ReaderSettingsRepository.getSpeechRate(this)) + "\n"
                + "対象 packageName: " + ReaderState.TARGET_PACKAGE + "\n"
                + "直近の読み上げ候補数: " + ReaderState.getCandidateCount(this) + "\n"
                + "直近の除外候補数: " + ReaderState.getExcludedCount(this);
        status.setText(text);
        if (speedLabel != null) {
            speedLabel.setText("読み上げ速度 " + ReaderSettingsRepository.formatRate(ReaderSettingsRepository.getSpeechRate(this)));
        }
    }

    private void shareDiagnosticReport(View view) {
        try {
            ReaderState.setIncludeDiagnosticSnippets(this, includeSnippets.isChecked());
            Uri uri = DiagnosticReportWriter.write(this, includeSnippets.isChecked());
            Intent share = new Intent(Intent.ACTION_SEND);
            share.setType("application/json");
            share.putExtra(Intent.EXTRA_STREAM, uri);
            share.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(Intent.createChooser(share, "診断レポートを共有"));
        } catch (Exception e) {
            DiagnosticStore.get().error(e);
            Toast.makeText(this, "診断レポートの作成に失敗しました", Toast.LENGTH_LONG).show();
        }
    }

    private void requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= 33
                && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.POST_NOTIFICATIONS}, REQ_NOTIFICATIONS);
        }
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density);
    }

    private static String enabledText(boolean enabled) {
        return enabled ? "有効" : "無効";
    }

    private static String detectedText(boolean detected) {
        return detected ? "検出中" : "未検出";
    }

    private static int rateToProgress(float rate) {
        return Math.round((ReaderSettingsRepository.clampSpeechRate(rate) - 0.5f) * 10f);
    }

    private static float progressToRate(int progress) {
        return ReaderSettingsRepository.clampSpeechRate(0.5f + progress / 10f);
    }
}

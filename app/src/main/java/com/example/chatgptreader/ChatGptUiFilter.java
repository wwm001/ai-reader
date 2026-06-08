package com.example.chatgptreader;

import android.graphics.Rect;
import android.view.accessibility.AccessibilityNodeInfo;

import java.util.Locale;

public final class ChatGptUiFilter {
    private static final int TOP_TOOLBAR_DP = 132;
    private static final int BOTTOM_COMPOSER_DP = 132;

    private ChatGptUiFilter() {
    }

    public static String exclusionReason(AccessibilityNodeInfo node, String value, Rect bounds, float density, int screenHeight) {
        if (value.length() < 8 || !node.isVisibleToUser()) {
            return DiagnosticCounters.EXCLUDED_KNOWN_UI;
        }
        int topCutoff = Math.round(TOP_TOOLBAR_DP * density);
        int bottomCutoff = screenHeight - Math.round(BOTTOM_COMPOSER_DP * density);
        if (bounds.bottom <= topCutoff) {
            return DiagnosticCounters.EXCLUDED_TOP_TOOLBAR;
        }
        if (screenHeight > 0 && bounds.top >= bottomCutoff) {
            return DiagnosticCounters.EXCLUDED_BOTTOM_COMPOSER;
        }
        CharSequence className = node.getClassName();
        String klass = className == null ? "" : className.toString().toLowerCase(Locale.ROOT);
        if (klass.contains("edittext")) {
            return DiagnosticCounters.EXCLUDED_EDITABLE_UI;
        }
        if (klass.contains("button")
                || klass.contains("imagebutton")
                || klass.contains("tab")
                || klass.contains("toolbar")
                || node.isCheckable()
                || (node.isClickable() && value.length() < 80)
                || (node.isFocusable() && value.length() < 80)) {
            return DiagnosticCounters.EXCLUDED_CLICKABLE_UI;
        }
        String lower = value.toLowerCase(Locale.ROOT);
        if (isKnownUiLabel(lower)) {
            return DiagnosticCounters.EXCLUDED_KNOWN_UI;
        }
        return null;
    }

    public static boolean isKnownUiLabel(String lower) {
        return lower.contains("chatgpt")
                || lower.contains("codex に質問")
                || lower.contains("message")
                || lower.contains("send")
                || lower.contains("ログイン")
                || lower.contains("設定")
                || lower.contains("共有")
                || lower.contains("コピー")
                || lower.contains("再実行")
                || lower.contains("メニュー")
                || lower.contains("戻る")
                || lower.contains("モデル")
                || lower.contains("アクセス")
                || lower.contains("マイク")
                || lower.equals("gpt-4")
                || lower.equals("gpt-4o")
                || lower.equals("gpt-5");
    }
}

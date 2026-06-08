package com.example.chatgptreader;

import android.graphics.Rect;
import android.view.accessibility.AccessibilityNodeInfo;

import java.util.Locale;

public final class ChatGptUiFilter {
    public static final int TOP_TOOLBAR_DP = 132;
    public static final int BOTTOM_COMPOSER_DP = 132;

    private ChatGptUiFilter() {
    }

    public static String exclusionReason(AccessibilityNodeInfo node, String value, Rect bounds, float density, int screenHeight) {
        if (value.length() < 8 || !node.isVisibleToUser()) {
            return DiagnosticCounters.EXCLUDED_KNOWN_UI;
        }
        int topCutoff = Math.round(TOP_TOOLBAR_DP * density);
        int bottomCutoff = screenHeight - Math.round(BOTTOM_COMPOSER_DP * density);
        if (bounds.bottom <= topCutoff) {
            DiagnosticStore.get().event("excludedByTopToolbarRegion", bounds.flattenToString());
            return DiagnosticCounters.EXCLUDED_TOP_TOOLBAR;
        }
        if (screenHeight > 0 && bounds.top >= bottomCutoff) {
            DiagnosticStore.get().event("excludedByBottomComposerRegion", bounds.flattenToString());
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
        String value = lower == null ? "" : lower.trim();
        if (value.length() > 24) {
            return false;
        }
        return value.equals("chatgpt")
                || value.equals("codex に質問")
                || value.equals("message")
                || value.equals("send")
                || value.equals("ログイン")
                || value.equals("設定")
                || value.equals("共有")
                || value.equals("コピー")
                || value.equals("再実行")
                || value.equals("メニュー")
                || value.equals("戻る")
                || value.equals("モデル")
                || value.equals("アクセス")
                || value.equals("マイク")
                || value.equals("gpt-4")
                || value.equals("gpt-4o")
                || value.equals("gpt-5");
    }
}

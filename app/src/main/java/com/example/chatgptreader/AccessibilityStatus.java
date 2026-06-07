package com.example.chatgptreader;

import android.content.Context;
import android.content.ComponentName;
import android.provider.Settings;
import android.text.TextUtils;

public final class AccessibilityStatus {
    private AccessibilityStatus() {
    }

    public static boolean isServiceEnabled(Context context) {
        return isServiceConfigured(context) || ReaderState.isAccessibilityServiceConnected(context);
    }

    public static boolean isServiceConfigured(Context context) {
        String enabled = Settings.Secure.getString(
                context.getContentResolver(),
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
        ReaderState.setAccessibilityRawValue(context, enabled);
        if (TextUtils.isEmpty(enabled)) {
            return false;
        }
        ComponentName expected = new ComponentName(context, ChatGptAccessibilityService.class);
        TextUtils.SimpleStringSplitter splitter = new TextUtils.SimpleStringSplitter(':');
        splitter.setString(enabled);
        while (splitter.hasNext()) {
            String service = splitter.next();
            ComponentName actual = ComponentName.unflattenFromString(service);
            if (expected.equals(actual)) {
                return true;
            }
        }
        return false;
    }

    public static boolean isServiceConnected(Context context) {
        return ReaderState.isAccessibilityServiceConnected(context);
    }
}

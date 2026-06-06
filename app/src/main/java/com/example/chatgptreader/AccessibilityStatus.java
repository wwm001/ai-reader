package com.example.chatgptreader;

import android.content.Context;
import android.provider.Settings;
import android.text.TextUtils;

public final class AccessibilityStatus {
    private AccessibilityStatus() {
    }

    public static boolean isServiceEnabled(Context context) {
        String enabled = Settings.Secure.getString(
                context.getContentResolver(),
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
        if (TextUtils.isEmpty(enabled)) {
            return false;
        }
        String component = context.getPackageName() + "/" + ChatGptAccessibilityService.class.getName();
        String shortComponent = context.getPackageName() + "/." + ChatGptAccessibilityService.class.getSimpleName();
        TextUtils.SimpleStringSplitter splitter = new TextUtils.SimpleStringSplitter(':');
        splitter.setString(enabled);
        while (splitter.hasNext()) {
            String service = splitter.next();
            if (component.equalsIgnoreCase(service) || shortComponent.equalsIgnoreCase(service)) {
                return true;
            }
        }
        return false;
    }
}

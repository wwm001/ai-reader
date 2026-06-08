package com.example.chatgptreader;

import android.view.accessibility.AccessibilityNodeInfo;

public final class ChatGptScrollController {
    private ChatGptScrollController() {
    }

    public static boolean scrollBackwardOnce(AccessibilityNodeInfo root) {
        AccessibilityNodeInfo scrollable = findScrollable(root);
        if (scrollable == null) {
            return false;
        }
        try {
            return scrollable.performAction(AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD);
        } finally {
            scrollable.recycle();
        }
    }

    private static AccessibilityNodeInfo findScrollable(AccessibilityNodeInfo node) {
        if (node == null) {
            return null;
        }
        if (node.isScrollable()) {
            return AccessibilityNodeInfo.obtain(node);
        }
        for (int i = 0; i < node.getChildCount(); i++) {
            AccessibilityNodeInfo child = node.getChild(i);
            try {
                AccessibilityNodeInfo found = findScrollable(child);
                if (found != null) {
                    return found;
                }
            } finally {
                if (child != null) {
                    child.recycle();
                }
            }
        }
        return null;
    }
}

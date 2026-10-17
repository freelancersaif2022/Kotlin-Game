package androidx.core.view;

import android.view.WindowInsets;
import androidx.core.graphics.Insets;

/**
 * Minimal stand-in for androidx.core.view.WindowInsetsCompat (offline APK build only).
 * LibGDX only creates its user (AndroidXKeyboardHeightProvider) on API 30+, so the
 * platform WindowInsets API 30 calls below are always available when this runs.
 */
public class WindowInsetsCompat {
    private final WindowInsets insets;

    public WindowInsetsCompat(WindowInsets insets) {
        this.insets = insets;
    }

    public WindowInsets toWindowInsets() {
        return insets;
    }

    public boolean isVisible(int typeMask) {
        return insets != null && insets.isVisible(typeMask);
    }

    public Insets getInsets(int typeMask) {
        if (insets == null) return new Insets(0, 0, 0, 0);
        android.graphics.Insets i = insets.getInsets(typeMask);
        return new Insets(i.left, i.top, i.right, i.bottom);
    }

    public static final class Type {
        private Type() {
        }

        public static int ime() {
            return WindowInsets.Type.ime();
        }
    }
}

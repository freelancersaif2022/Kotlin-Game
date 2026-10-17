package androidx.core.view;

import android.view.View;
import android.view.WindowInsets;

/** Minimal stand-in for androidx.core.view.ViewCompat (offline APK build only). */
public final class ViewCompat {
    private ViewCompat() {
    }

    public static void setOnApplyWindowInsetsListener(View view, final OnApplyWindowInsetsListener listener) {
        if (listener == null) {
            view.setOnApplyWindowInsetsListener(null);
            return;
        }
        // anonymous class instead of a lambda: no invokedynamic for the dexer
        view.setOnApplyWindowInsetsListener(new View.OnApplyWindowInsetsListener() {
            @Override
            public WindowInsets onApplyWindowInsets(View v, WindowInsets insets) {
                WindowInsetsCompat result = listener.onApplyWindowInsets(v, new WindowInsetsCompat(insets));
                return result != null ? result.toWindowInsets() : insets;
            }
        });
    }
}

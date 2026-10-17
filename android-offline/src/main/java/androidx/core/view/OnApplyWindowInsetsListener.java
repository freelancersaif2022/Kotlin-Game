package androidx.core.view;

import android.view.View;

/** Minimal stand-in for the AndroidX interface (offline APK build only). */
public interface OnApplyWindowInsetsListener {
    WindowInsetsCompat onApplyWindowInsets(View v, WindowInsetsCompat insets);
}

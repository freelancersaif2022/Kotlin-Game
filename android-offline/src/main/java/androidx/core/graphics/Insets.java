package androidx.core.graphics;

/**
 * Minimal stand-in for androidx.core.graphics.Insets, used only by the offline APK build
 * (tools/build_apk_offline.sh) where the real AndroidX library cannot be downloaded.
 * LibGDX's AndroidXKeyboardHeightProvider reads these four fields on Android 11+.
 */
public final class Insets {
    public final int left;
    public final int top;
    public final int right;
    public final int bottom;

    public Insets(int left, int top, int right, int bottom) {
        this.left = left;
        this.top = top;
        this.right = right;
        this.bottom = bottom;
    }
}

# MediaPipe Tasks uses JNI and reflection on its own classes and on protobuf-lite messages.
-keep class com.google.mediapipe.** { *; }
-keep class com.google.protobuf.** { *; }
-keep class com.google.common.flogger.** { *; }
-dontwarn com.google.common.flogger.**
-dontwarn com.google.mediapipe.**
-dontwarn com.google.protobuf.**
-dontwarn javax.annotation.**
-dontwarn com.google.auto.value.**
-dontwarn org.checkerframework.**
-dontwarn com.google.errorprone.annotations.**

# Services and activities referenced from the manifest are kept by AAPT rules already.

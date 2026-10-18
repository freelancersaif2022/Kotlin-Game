# Safety net in case code shrinking is turned back on.
# Room/WorkManager instantiate generated *_Impl classes by reflection.
-keep class * extends androidx.room.RoomDatabase { *; }
-keep class androidx.work.** { *; }
-keep class androidx.room.** { *; }
-keep class com.google.android.gms.ads.** { *; }
-keep class io.flutter.plugins.** { *; }
-keep class com.ryanheise.** { *; }
-keep class com.llfbandit.record.** { *; }
-keep class com.github.dart_lang.jni.** { *; }
-dontwarn com.google.android.play.core.**

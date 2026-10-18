# --- WorkManager / Room (used by Google Mobile Ads) ---
# R8 strips the generated Room database class, which crashes the app at
# startup: "Failed to create an instance of androidx.work.impl.WorkDatabase".
-keep class * extends androidx.room.RoomDatabase { <init>(...); }
-keep class androidx.work.impl.WorkDatabase_Impl { *; }
-keep class androidx.work.** { *; }
-keep class androidx.room.** { *; }

# --- flutter_local_notifications (Gson-serialised scheduled alerts) ---
-keep class com.dexterous.** { *; }
-keepattributes Signature
-keepattributes *Annotation*
-keep class com.google.gson.reflect.TypeToken { *; }
-keep class * extends com.google.gson.reflect.TypeToken

# --- Google Play Billing ---
-keep class com.android.vending.billing.** { *; }

# --- Flutter plugins called from Dart by name (JNI / Pigeon) ---
# shared_preferences calls SharedPreferencesApiRegistrar.getAll via JNI; R8
# renamed it -> NoSuchMethodError and nothing could be saved.
-keep class io.flutter.plugins.** { *; }
-keep class dev.fluttercommunity.plus.** { *; }
-keep class com.baseflow.** { *; }
-keep class io.flutter.plugins.googlemobileads.** { *; }
-keep class kotlin.coroutines.Continuation { *; }

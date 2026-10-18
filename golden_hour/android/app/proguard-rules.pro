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

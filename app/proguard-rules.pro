# Google Mobile Ads / UMP / Billing ship their own consumer rules.
-keep class com.google.android.gms.ads.** { *; }
-keep class com.android.billingclient.** { *; }
-dontwarn com.google.android.gms.**

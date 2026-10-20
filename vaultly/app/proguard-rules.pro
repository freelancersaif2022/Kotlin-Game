# Room, WorkManager, AdMob, Billing and ML Kit ship their own consumer R8 rules.
-keep class com.vaultly.app.reminders.ReminderWorker { *; }
-dontwarn org.slf4j.**
-dontwarn com.google.errorprone.annotations.**

# Inkly – R8 rules. Libraries ship their own consumer rules (Room, WorkManager, Play services).
-keep class com.inkly.notes.drive.BackupWorker { <init>(...); }
-dontwarn org.jetbrains.annotations.**

# Keep line numbers for readable crash reports in Play Console.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Widget providers and workers are referenced from the manifest / WorkManager by name.
-keep class com.netscope.wifianalyzer.widget.** { *; }
-keep class com.netscope.wifianalyzer.work.** { *; }

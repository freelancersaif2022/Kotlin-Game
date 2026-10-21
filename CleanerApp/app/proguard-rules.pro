# Google Mobile Ads, UMP and Play Billing ship their own consumer rules.
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod
-keep class com.novacleaner.app.service.** { *; }
-keep class com.novacleaner.app.worker.** { *; }
-dontwarn org.jetbrains.annotations.**

# Methods called from Python (pm_runner.py) by name via Chaquopy.
-keep class com.saif.pymaster.python.ConsoleBridge { public *; }

# kotlinx.serialization models
-keepattributes *Annotation*, InnerClasses
-keep,includedescriptorclasses class com.saif.pymaster.data.**$$serializer { *; }
-keepclassmembers class com.saif.pymaster.data.** {
    *** Companion;
    kotlinx.serialization.KSerializer serializer(...);
}
-dontwarn org.slf4j.**

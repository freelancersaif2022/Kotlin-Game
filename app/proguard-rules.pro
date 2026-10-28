# kotlinx.serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** { *** Companion; }
-keepclasseswithmembers class kotlinx.serialization.json.** { kotlinx.serialization.KSerializer serializer(...); }
-keep,includedescriptorclasses class com.lexora.english.**$$serializer { *; }
-keepclassmembers class com.lexora.english.** { *** Companion; }
-keepclasseswithmembers class com.lexora.english.** { kotlinx.serialization.KSerializer serializer(...); }

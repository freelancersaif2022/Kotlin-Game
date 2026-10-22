# TensorFlow Lite + Task Library use JNI and reflection.
-keep class org.tensorflow.** { *; }
-keep interface org.tensorflow.** { *; }
-dontwarn org.tensorflow.**
-dontwarn com.google.auto.value.**

# Room entities
-keep class com.calorielens.app.data.db.** { *; }

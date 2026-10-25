# Device admin receiver and system-invoked activities are referenced from the manifest, which R8 keeps automatically.
-keep class com.twinspace.app.admin.** { *; }

pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
        google()
    }
}

rootProject.name = "BrambleDash"

include(":core", ":lwjgl3")

// The Android module needs an Android SDK. It is included automatically when one is
// configured (local.properties sdk.dir or ANDROID_HOME / ANDROID_SDK_ROOT), so desktop
// builds and tests keep working on machines without the SDK.
val localProps = file("local.properties")
val hasSdk = System.getenv("ANDROID_HOME") != null || System.getenv("ANDROID_SDK_ROOT") != null ||
    (localProps.exists() && localProps.readText().contains("sdk.dir"))
if (hasSdk && providers.gradleProperty("skipAndroid").orNull != "true") {
    include(":android")
}
// Fallback APK path used when the Google Maven repository / SDK manager is unreachable
// (see tools/build_apk_offline.sh). It compiles the same Android launcher sources.
if (providers.gradleProperty("offlineApk").orNull == "true") {
    include(":android-offline")
}

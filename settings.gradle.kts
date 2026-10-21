pluginManagement {
    repositories {
        gradlePluginPortal()
        google()
        mavenCentral()
    }
}

dependencyResolutionManagement {
    repositories {
        mavenCentral()
        google()
    }
}

rootProject.name = "SnakeAttack"

include("core", "lwjgl3")
// The Android module needs the Android SDK; skip it where none is installed (e.g. desktop-only dev boxes).
if (System.getenv("ANDROID_HOME") != null || System.getenv("ANDROID_SDK_ROOT") != null || file("local.properties").exists()) {
    include("android")
}

buildscript {
    repositories {
        mavenCentral()
        google()
        gradlePluginPortal()
    }
    dependencies {
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:${property("kotlinVersion")}")
        // Only resolved when the Android module is part of the build (see settings.gradle.kts).
        if (gradle.startParameter.projectProperties["skipAndroid"] != "true" &&
            (System.getenv("ANDROID_HOME") != null || System.getenv("ANDROID_SDK_ROOT") != null ||
                (file("local.properties").exists() && file("local.properties").readText().contains("sdk.dir")))
        ) {
            classpath("com.android.tools.build:gradle:${property("agpVersion")}")
        }
    }
}

val androidIncluded = findProject(":android") != null
allprojects {
    repositories {
        mavenCentral()
        // Google's repository is only needed by the Android Gradle Plugin build.
        if (androidIncluded) google()
    }
}

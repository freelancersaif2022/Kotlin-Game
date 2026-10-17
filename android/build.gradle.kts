// Standard Android build (Android Studio / any machine with the Android SDK and Google Maven access).
// The Android Gradle Plugin is put on the build classpath by the root build.gradle.kts.
plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

val gdxVersion: String by project
val natives: Configuration by configurations.creating

android {
    namespace = "com.bramble.dash.android"
    compileSdk = 35

    defaultConfig {
        applicationId = project.property("appId") as String
        minSdk = 21
        targetSdk = 35
        versionCode = (project.property("appVersionCode") as String).toInt()
        versionName = project.property("appVersionName") as String
    }

    sourceSets["main"].apply {
        manifest.srcFile("AndroidManifest.xml")
        java.srcDirs("src/main/kotlin")
        res.srcDirs("res")
        assets.srcDirs(rootProject.file("assets"))
        jniLibs.srcDirs("libs")
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
            // No signing config is committed: sign release builds with your own keystore.
        }
    }

    androidResources {
        // streamed music must stay uncompressed for Android's MediaPlayer
        noCompress += listOf("ogg", "wav")
    }

    packaging {
        resources.excludes += listOf("META-INF/*.kotlin_module", "META-INF/versions/**", "module-info.class")
    }
}

kotlin {
    compilerOptions { jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17) }
}

dependencies {
    implementation(project(":core"))
    implementation("com.badlogicgames.gdx:gdx-backend-android:$gdxVersion")
    // LibGDX's backend uses androidx.core for keyboard insets on Android 11+; without it the app
    // crashes at launch with NoClassDefFoundError (androidx/core/view/ViewCompat).
    implementation("androidx.core:core:1.13.1")
    natives("com.badlogicgames.gdx:gdx-platform:$gdxVersion:natives-armeabi-v7a")
    natives("com.badlogicgames.gdx:gdx-platform:$gdxVersion:natives-arm64-v8a")
    natives("com.badlogicgames.gdx:gdx-platform:$gdxVersion:natives-x86")
    natives("com.badlogicgames.gdx:gdx-platform:$gdxVersion:natives-x86_64")
}

// Unpack LibGDX's native .so files into libs/<abi>/ before they are merged into the APK.
val copyAndroidNatives by tasks.registering {
    doFirst {
        natives.files.forEach { jar ->
            val abi = jar.nameWithoutExtension.substringAfter("natives-")
            val out = file("libs/$abi")
            out.mkdirs()
            copy { from(zipTree(jar)); into(out); include("*.so") }
        }
    }
}
tasks.matching { it.name.contains("merge") && it.name.contains("JniLibFolders") }.configureEach { dependsOn(copyAndroidNatives) }

plugins {
    id("com.android.application") version "8.7.3"
}

val gdxVersion: String by project
val natives: Configuration by configurations.creating

android {
    namespace = "com.freelancersaif.snakeattack"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.freelancersaif.snakeattack"
        minSdk = 21
        targetSdk = 35
        versionCode = (System.getenv("GITHUB_RUN_NUMBER") ?: "1").toInt()
        versionName = "1.0.${System.getenv("GITHUB_RUN_NUMBER") ?: "0"}"
    }

    sourceSets["main"].apply {
        manifest.srcFile("AndroidManifest.xml")
        res.srcDirs("res")
        assets.srcDirs(rootProject.file("assets"))
        jniLibs.srcDirs("libs")
    }

    signingConfigs {
        create("release") {
            // Sideload signing key kept in the repo so every CI build can update the
            // installed app in place. Replace it with a private key before publishing to Play.
            storeFile = rootProject.file("android/snakeattack.keystore")
            storePassword = "snakeattack"
            keyAlias = "snakeattack"
            keyPassword = "snakeattack"
        }
    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("release")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    implementation(project(":core"))
    implementation("com.badlogicgames.gdx:gdx-backend-android:$gdxVersion")
    natives("com.badlogicgames.gdx:gdx-platform:$gdxVersion:natives-armeabi-v7a")
    natives("com.badlogicgames.gdx:gdx-platform:$gdxVersion:natives-arm64-v8a")
    natives("com.badlogicgames.gdx:gdx-platform:$gdxVersion:natives-x86")
    natives("com.badlogicgames.gdx:gdx-platform:$gdxVersion:natives-x86_64")
}

// Unpack the libGDX native .so files into libs/<abi>/ before they are merged into the APK.
val copyAndroidNatives by tasks.registering {
    doFirst {
        natives.files.forEach { jar ->
            val abi = jar.nameWithoutExtension.substringAfterLast("natives-")
            val outputDir = file("libs/$abi")
            outputDir.mkdirs()
            copy {
                from(zipTree(jar))
                into(outputDir)
                include("*.so")
            }
        }
    }
}

tasks.matching { it.name.contains("merge") && it.name.contains("JniLibFolders") }.configureEach {
    dependsOn(copyAndroidNatives)
}

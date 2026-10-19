plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

// Release signing: provide these env vars (e.g. from GitHub secrets) to sign with your own
// upload key for Google Play. Without them the build uses the fixed test key in
// signing/eyescroll-test.jks, so every test APK installs over the previous one.
// That key is public – never use it for Google Play.
val releaseKeystore: String? = System.getenv("EYESCROLL_KEYSTORE")
val buildNumber: Int = System.getenv("GITHUB_RUN_NUMBER")?.toIntOrNull() ?: 1

android {
    namespace = "com.eyescroll.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.eyescroll.app"
        minSdk = 26
        targetSdk = 35
        versionCode = buildNumber
        versionName = "1.0.$buildNumber"

    }

    signingConfigs {
        create("release") {
            if (releaseKeystore != null) {
                storeFile = file(releaseKeystore)
                storePassword = System.getenv("EYESCROLL_KEYSTORE_PASSWORD")
                keyAlias = System.getenv("EYESCROLL_KEY_ALIAS")
                keyPassword = System.getenv("EYESCROLL_KEY_PASSWORD")
            }
        }
        create("test") {
            storeFile = rootProject.file("signing/eyescroll-test.jks")
            storePassword = "eyescroll-test"
            keyAlias = "eyescroll"
            keyPassword = "eyescroll-test"
        }
    }

    buildTypes {
        release {
            // MediaPipe loads classes by reflection (Flogger, protobuf); shrinking breaks the
            // hand model with ExceptionInInitializerError, so the release build is not minified.
            isMinifyEnabled = false
            isShrinkResources = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = if (releaseKeystore != null) {
                signingConfigs.getByName("release")
            } else {
                signingConfigs.getByName("test")
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    androidResources {
        // MediaPipe memory-maps the model, so it must be stored uncompressed.
        noCompress += "task"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2025.04.01")
    implementation(composeBom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.animation:animation")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-core")
    implementation("androidx.compose.ui:ui-tooling-preview")
    debugImplementation("androidx.compose.ui:ui-tooling")

    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.activity:activity-compose:1.10.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-service:2.8.7")


    val cameraX = "1.4.2"
    implementation("androidx.camera:camera-core:$cameraX")
    implementation("androidx.camera:camera-camera2:$cameraX")
    implementation("androidx.camera:camera-lifecycle:$cameraX")
    implementation("androidx.camera:camera-view:$cameraX")

    implementation("com.google.mediapipe:tasks-vision:0.10.26")

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")

    testImplementation("junit:junit:4.13.2")
}

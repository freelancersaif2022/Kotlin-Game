plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.devtools.ksp")
}

android {
    namespace = "com.inkly.notes"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.inkly.notes"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0.0"
        resourceConfigurations += listOf("en")
    }

    // Drive's OAuth client is bound to the signing certificate's SHA-1, so release builds should use a
    // stable key supplied via env vars (CI secrets). Without one, the debug key is used.
    val keystorePath = System.getenv("INKLY_KEYSTORE")
    val releaseSigning = if (keystorePath != null && file(keystorePath).exists()) {
        signingConfigs.create("release") {
            storeFile = file(keystorePath)
            storePassword = System.getenv("INKLY_KEYSTORE_PASSWORD")
            keyAlias = System.getenv("INKLY_KEY_ALIAS")
            keyPassword = System.getenv("INKLY_KEY_PASSWORD")
        }
    } else {
        signingConfigs.getByName("debug")
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = releaseSigning
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    packaging {
        resources {
            excludes += listOf(
                "/META-INF/{AL2.0,LGPL2.1}",
                "/META-INF/*.version",
                "/META-INF/*.kotlin_module",
                "DebugProbesKt.bin",
                "kotlin-tooling-metadata.json",
            )
        }
    }
    dependenciesInfo {
        includeInApk = false
        includeInBundle = false
    }
}

ksp {
    arg("room.generateKotlin", "true")
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.12.01")
    implementation(composeBom)

    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.core:core-splashscreen:1.0.1")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.navigation:navigation-compose:2.8.5")

    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.animation:animation")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")

    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    ksp("androidx.room:room-compiler:2.6.1")

    implementation("androidx.work:work-runtime-ktx:2.10.0")

    implementation("com.google.android.gms:play-services-auth:21.3.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.9.0")
}

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

// Release signing: set these env vars (GitHub secrets) to sign with your own Play upload key.
// Without them the build uses the public test key in signing/flashshare-test.jks so every
// test APK installs over the previous one. Never upload a test-key build to Google Play.
val releaseKeystore: String? = System.getenv("FLASHSHARE_KEYSTORE")
val buildNumber: Int = System.getenv("GITHUB_RUN_NUMBER")?.toIntOrNull() ?: 1

// AdMob IDs default to Google's public test IDs; set the env vars to your real IDs before publishing.
fun adId(name: String, testId: String): String = System.getenv(name)?.takeIf { it.isNotBlank() } ?: testId
val admobAppId = adId("FLASHSHARE_ADMOB_APP_ID", "ca-app-pub-3940256099942544~3347511713")
val admobInterstitialId = adId("FLASHSHARE_ADMOB_INTERSTITIAL_ID", "ca-app-pub-3940256099942544/1033173712")
val admobRewardedId = adId("FLASHSHARE_ADMOB_REWARDED_ID", "ca-app-pub-3940256099942544/5224354917")

android {
    namespace = "com.flashshare.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.flashshare.app"
        minSdk = 26
        targetSdk = 35
        versionCode = buildNumber
        versionName = "1.0.$buildNumber"

        manifestPlaceholders["admobAppId"] = admobAppId
        buildConfigField("String", "ADMOB_INTERSTITIAL_ID", "\"$admobInterstitialId\"")
        buildConfigField("String", "ADMOB_REWARDED_ID", "\"$admobRewardedId\"")
        // Test-key builds use a mock store so Premium can be tried without Play Console setup.
        buildConfigField("boolean", "MOCK_BILLING", (releaseKeystore == null).toString())

        resourceConfigurations += listOf("en")
    }

    signingConfigs {
        create("release") {
            if (releaseKeystore != null) {
                storeFile = file(releaseKeystore)
                storePassword = System.getenv("FLASHSHARE_KEYSTORE_PASSWORD")
                keyAlias = System.getenv("FLASHSHARE_KEY_ALIAS")
                keyPassword = System.getenv("FLASHSHARE_KEY_PASSWORD")
            }
        }
        create("test") {
            storeFile = rootProject.file("signing/flashshare-test.jks")
            storePassword = "flashshare-test"
            keyAlias = "flashshare"
            keyPassword = "flashshare-test"
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = if (releaseKeystore != null) {
                signingConfigs.getByName("release")
            } else {
                signingConfigs.getByName("test")
            }
        }
        debug {
            signingConfig = signingConfigs.getByName("test")
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
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            excludes += "DebugProbesKt.bin"
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
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.compose.ui:ui-tooling-preview")
    debugImplementation("androidx.compose.ui:ui-tooling")

    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.core:core-splashscreen:1.0.1")
    implementation("androidx.profileinstaller:profileinstaller:1.4.1")
    implementation("androidx.activity:activity-compose:1.10.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.navigation:navigation-compose:2.8.9")
    implementation("androidx.datastore:datastore-preferences:1.1.4")
    implementation("androidx.documentfile:documentfile:1.0.1")

    implementation("io.coil-kt:coil-compose:2.7.0")
    implementation("io.coil-kt:coil-video:2.7.0")

    // QR generation + decoding (pure Java, ~500 KB, no Play services needed).
    implementation("com.google.zxing:core:3.5.3")

    val cameraX = "1.4.2"
    implementation("androidx.camera:camera-core:$cameraX")
    implementation("androidx.camera:camera-camera2:$cameraX")
    implementation("androidx.camera:camera-lifecycle:$cameraX")
    implementation("androidx.camera:camera-view:$cameraX")
    // CameraX returns ListenableFuture; Play services pins the empty listenablefuture stub, so the class comes from Guava.
    implementation("com.google.guava:guava:33.3.1-android")

    implementation("com.google.android.gms:play-services-ads:24.2.0")
    implementation("com.android.billingclient:billing-ktx:7.1.1")
    implementation("com.google.android.ump:user-messaging-platform:3.1.0")

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")

    testImplementation("junit:junit:4.13.2")
}

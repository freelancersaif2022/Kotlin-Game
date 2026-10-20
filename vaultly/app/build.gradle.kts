plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.devtools.ksp")
}

// Release signing: provide these env vars (e.g. from GitHub secrets) to sign with your own
// upload key for Google Play. Without them the build uses the fixed test key in
// signing/vaultly-test.jks, so every test APK installs over the previous one.
// That key is public – never use it for Google Play.
val releaseKeystore: String? = System.getenv("VAULTLY_KEYSTORE")
val buildNumber: Int = System.getenv("GITHUB_RUN_NUMBER")?.toIntOrNull() ?: 1

// AdMob IDs. The defaults are Google's public test IDs; set the env vars (GitHub secrets)
// to your real AdMob app/unit IDs before publishing.
fun adId(name: String, testId: String): String = System.getenv(name)?.takeIf { it.isNotBlank() } ?: testId
val admobAppId = adId("VAULTLY_ADMOB_APP_ID", "ca-app-pub-3940256099942544~3347511713")
val admobInterstitialId = adId("VAULTLY_ADMOB_INTERSTITIAL_ID", "ca-app-pub-3940256099942544/1033173712")
val admobRewardedId = adId("VAULTLY_ADMOB_REWARDED_ID", "ca-app-pub-3940256099942544/5224354917")

android {
    namespace = "com.vaultly.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.vaultly.app"
        minSdk = 26
        targetSdk = 35
        versionCode = buildNumber
        versionName = "1.0.$buildNumber"

        manifestPlaceholders["admobAppId"] = admobAppId
        buildConfigField("String", "ADMOB_INTERSTITIAL_ID", "\"$admobInterstitialId\"")
        buildConfigField("String", "ADMOB_REWARDED_ID", "\"$admobRewardedId\"")
        // Test-key builds get a mock store so purchases can be tried without Play Console setup.
        buildConfigField("boolean", "MOCK_BILLING", (releaseKeystore == null).toString())
        buildConfigField("String", "SUPPORT_EMAIL", "\"${System.getenv("VAULTLY_SUPPORT_EMAIL").orEmpty()}\"")

        resourceConfigurations += listOf("en")
    }

    signingConfigs {
        create("release") {
            if (releaseKeystore != null) {
                storeFile = file(releaseKeystore)
                storePassword = System.getenv("VAULTLY_KEYSTORE_PASSWORD")
                keyAlias = System.getenv("VAULTLY_KEY_ALIAS")
                keyPassword = System.getenv("VAULTLY_KEY_PASSWORD")
            }
        }
        create("test") {
            storeFile = rootProject.file("signing/vaultly-test.jks")
            storePassword = "vaultly-test"
            keyAlias = "vaultly"
            keyPassword = "vaultly-test"
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
        }
    }
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
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
    implementation("androidx.activity:activity-compose:1.10.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.navigation:navigation-compose:2.8.9")
    implementation("androidx.exifinterface:exifinterface:1.4.1")

    val room = "2.7.1"
    implementation("androidx.room:room-runtime:$room")
    implementation("androidx.room:room-ktx:$room")
    ksp("androidx.room:room-compiler:$room")

    implementation("androidx.work:work-runtime-ktx:2.10.1")
    implementation("io.coil-kt:coil-compose:2.7.0")

    // On-device OCR delivered through Google Play services (keeps the APK small).
    implementation("com.google.android.gms:play-services-mlkit-text-recognition:19.0.1")

    implementation("com.google.android.gms:play-services-ads:24.2.0")
    implementation("com.android.billingclient:billing-ktx:7.1.1")
    implementation("com.google.android.ump:user-messaging-platform:3.1.0")

    val cameraX = "1.4.2"
    implementation("androidx.camera:camera-core:$cameraX")
    implementation("androidx.camera:camera-camera2:$cameraX")
    implementation("androidx.camera:camera-lifecycle:$cameraX")
    implementation("androidx.camera:camera-view:$cameraX")

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.9.0")

    testImplementation("junit:junit:4.13.2")
}

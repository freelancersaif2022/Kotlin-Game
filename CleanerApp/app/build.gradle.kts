plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.novacleaner.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.novacleaner.app"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0.0"

        // Only ship English resources from libraries; keeps the APK small.
        resourceConfigurations += listOf("en")

        // ---- AdMob ----------------------------------------------------------------
        // These are Google's official TEST ids. Replace them with your own ids from
        // the AdMob console before publishing (or override via gradle properties).
        val admobAppId = project.findProperty("ADMOB_APP_ID") as String? ?: "ca-app-pub-3940256099942544~3347511713"
        manifestPlaceholders["admobAppId"] = admobAppId
        buildConfigField("String", "AD_BANNER_ID", "\"${project.findProperty("AD_BANNER_ID") ?: "ca-app-pub-3940256099942544/9214589741"}\"")
        buildConfigField("String", "AD_INTERSTITIAL_ID", "\"${project.findProperty("AD_INTERSTITIAL_ID") ?: "ca-app-pub-3940256099942544/1033173712"}\"")
        buildConfigField("String", "AD_APP_OPEN_ID", "\"${project.findProperty("AD_APP_OPEN_ID") ?: "ca-app-pub-3940256099942544/9257395921"}\"")
        buildConfigField("String", "AD_NATIVE_ID", "\"${project.findProperty("AD_NATIVE_ID") ?: "ca-app-pub-3940256099942544/2247696110"}\"")

        // ---- Play Billing ---------------------------------------------------------
        buildConfigField("String", "REMOVE_ADS_PRODUCT_ID", "\"remove_ads_lifetime\"")
    }

    signingConfigs {
        create("release") {
            // CI / local release signing. Override with environment variables for your
            // own Play Store upload key; never commit a production key.
            storeFile = file(System.getenv("SIGNING_STORE_FILE") ?: "../keystore/novacleaner-release.jks")
            storePassword = System.getenv("SIGNING_STORE_PASSWORD") ?: "novacleaner123"
            keyAlias = System.getenv("SIGNING_KEY_ALIAS") ?: "novacleaner"
            keyPassword = System.getenv("SIGNING_KEY_PASSWORD") ?: "novacleaner123"
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = signingConfigs.getByName("release")
        }
        debug {
            applicationIdSuffix = ".debug"
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
            excludes += setOf(
                "/META-INF/{AL2.0,LGPL2.1}",
                "/META-INF/*.version",
                "/META-INF/*.kotlin_module",
                "DebugProbesKt.bin",
                "kotlin-tooling-metadata.json",
            )
        }
    }
    lint {
        checkReleaseBuilds = false
        abortOnError = false
    }
    dependenciesInfo {
        includeInApk = false
        includeInBundle = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.process)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.play.services.ads)
    implementation(libs.ump)
    implementation(libs.billing.ktx)
    implementation(libs.coil.compose)
    implementation(libs.coil.video)
    debugImplementation(libs.androidx.compose.ui.tooling)
}

import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

// Release signing: CI passes the keystore through environment variables (GitHub secrets).
// When they are absent the bundled project keystore is used so release builds are always installable.
val signingProps = Properties().apply {
    val f = rootProject.file("keystore/signing.properties")
    if (f.exists()) f.inputStream().use { load(it) }
}
fun signingValue(env: String, key: String): String? =
    System.getenv(env)?.takeIf { it.isNotBlank() } ?: signingProps.getProperty(key)

android {
    namespace = "com.bornomala.keyboard"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.bornomala.keyboard"
        minSdk = 23
        targetSdk = 35
        versionCode = (System.getenv("VERSION_CODE") ?: "1").toInt()
        versionName = "1.1.0"
        resourceConfigurations += listOf("en", "bn")
        vectorDrawables.useSupportLibrary = false
    }

    signingConfigs {
        create("release") {
            val storePath = signingValue("SIGNING_STORE_FILE", "storeFile")
            if (storePath != null) {
                storeFile = rootProject.file(storePath)
                storePassword = signingValue("SIGNING_STORE_PASSWORD", "storePassword")
                keyAlias = signingValue("SIGNING_KEY_ALIAS", "keyAlias")
                keyPassword = signingValue("SIGNING_KEY_PASSWORD", "keyPassword")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = signingConfigs.getByName("release").takeIf { it.storeFile != null }
                ?: signingConfigs.getByName("debug")
        }
        debug {
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
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
                "/META-INF/**/*.kotlin_builtins",
                "DebugProbesKt.bin",
                "kotlin-tooling-metadata.json",
            )
        }
    }
    dependenciesInfo {
        includeInApk = false
        includeInBundle = false
    }
    lint {
        abortOnError = false
        checkReleaseBuilds = false
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.core.splashscreen)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui.tooling.preview)
    debugImplementation(libs.androidx.compose.ui.tooling)
    testImplementation(libs.junit)
}

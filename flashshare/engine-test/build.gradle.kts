plugins { kotlin("jvm") version "2.1.20" }

repositories { mavenCentral() }

// Compiles the app's Android-free engine sources directly, plus tiny android.* stubs.
sourceSets {
    main {
        kotlin {
            srcDir("../app/src/main/java")
            srcDir("src/stubs/kotlin")
            include(
                "android/**",
                "com/flashshare/app/data/Models.kt",
                "com/flashshare/app/net/TransferEngine.kt",
                "com/flashshare/app/net/Crypto.kt",
                "com/flashshare/app/net/RateLimiter.kt",
                "com/flashshare/app/net/Source.kt",
            )
        }
    }
}

dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.9.0")
    implementation("org.json:json:20240303")
    testImplementation("junit:junit:4.13.2")
}

kotlin { jvmToolchain(17) }

tasks.test {
    testLogging {
        showStandardStreams = true
        events("passed", "failed")
        exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
    }
}

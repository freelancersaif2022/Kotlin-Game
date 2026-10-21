// Robolectric UI flow test that drives every screen with a fake IR emitter and saves screenshots.
// Run tools/build-apk.sh first (it generates R.java and resources.apk), then:
//   gradle -p tools/ui-test test -PandroidJar=/path/to/android.jar -PmonitorJar=/path/to/androidx-test-monitor.jar
// androidx.test:monitor and espresso-idling-resource come from Google Maven; pass them as local jars
// if that repository is unreachable (they are excluded from Robolectric's transitive deps below).
plugins { kotlin("jvm") version "1.9.24" }

val app = rootDir.parentFile.parentFile
val androidJar = (findProperty("androidJar") ?: "/opt/android-sdk/platforms/android-34/android.jar").toString()
val extraJars = (findProperty("monitorJar") ?: "").toString().split(',').filter { it.isNotBlank() }

sourceSets {
    main { kotlin.srcDirs("$app/app/src/main/java"); java.srcDirs("$app/build/gen") }
}
dependencies {
    compileOnly(files(androidJar))
    testImplementation(files(androidJar))
    testImplementation(files(extraJars))
    testImplementation("junit:junit:4.13.2")
    testImplementation("com.google.errorprone:error_prone_annotations:2.28.0")
    testImplementation("org.robolectric:robolectric:4.14.1") {
        exclude(group = "androidx.test")
        exclude(group = "androidx.test.espresso")
    }
}
kotlin { jvmToolchain(21) }
tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> { kotlinOptions.jvmTarget = "17" }
java { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }

val writeTestConfig by tasks.registering {
    val out = layout.buildDirectory.file("test-config/com/android/tools/test_config.properties")
    outputs.file(out)
    doLast {
        out.get().asFile.apply { parentFile.mkdirs() }.writeText(
            """
            android_merged_manifest=$app/build/tmp/AndroidManifest.xml
            android_resource_apk=$app/build/tmp/resources.apk
            android_merged_assets=$app/app/src/main/assets
            android_custom_package=com.irremote.tv
            """.trimIndent(),
        )
    }
}
sourceSets.test { resources.srcDir(layout.buildDirectory.dir("test-config")) }
tasks.processTestResources { dependsOn(writeTestConfig) }

tasks.test {
    systemProperty("robolectric.graphicsMode", "NATIVE")
    systemProperty("shotdir", layout.buildDirectory.dir("screenshots").get().asFile.absolutePath)
    testLogging { showStandardStreams = true; events("passed", "failed") }
    maxHeapSize = "3g"
}

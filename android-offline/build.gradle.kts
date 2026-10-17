// Fallback used by tools/build_apk_offline.sh when Google's Maven repository and SDK manager are
// unreachable: compiles the same Android launcher sources against an android.jar and collects the
// runtime jars and native libraries; the script then dexes, packages, aligns and signs the APK.
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    base
}
apply(plugin = "org.jetbrains.kotlin.jvm")

val gdxVersion: String by project
val androidJar = (findProperty("androidJar") as String?) ?: error("pass -PandroidJar=/path/to/android.jar")
val natives: Configuration by configurations.creating
// gdx-backend-android is published as an AAR; take its classes.jar directly (artifact only)
val backendAar: Configuration by configurations.creating { isTransitive = false }
val extractBackend by tasks.registering(Copy::class) {
    from({ zipTree(backendAar.singleFile) }) { include("classes.jar") }
    into(layout.buildDirectory.dir("aar"))
}

extensions.configure<org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension> {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_1_8)
        freeCompilerArgs.addAll("-Xlambdas=class", "-Xsam-conversions=class", "-Xstring-concat=inline")
    }
    sourceSets["main"].kotlin.srcDir(rootProject.file("android/src/main/kotlin"))
}
extensions.configure<JavaPluginExtension> {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}

dependencies {
    "compileOnly"(files(androidJar))
    "implementation"(project(":core"))
    backendAar("com.badlogicgames.gdx:gdx-backend-android:$gdxVersion@aar")
    "implementation"(files(layout.buildDirectory.file("aar/classes.jar")).builtBy(extractBackend))
    natives("com.badlogicgames.gdx:gdx-platform:$gdxVersion:natives-armeabi-v7a")
    natives("com.badlogicgames.gdx:gdx-platform:$gdxVersion:natives-arm64-v8a")
    natives("com.badlogicgames.gdx:gdx-platform:$gdxVersion:natives-x86")
    natives("com.badlogicgames.gdx:gdx-platform:$gdxVersion:natives-x86_64")
}

val offlineInputs by tasks.registering(Sync::class) {
    dependsOn("jar")
    into(layout.buildDirectory.dir("offline"))
    from(tasks.named("jar")) { into("jars") }
    from(configurations.named("runtimeClasspath")) { into("jars") }
    natives.files.forEach { jar ->
        val abi = jar.nameWithoutExtension.substringAfter("natives-")
        from(zipTree(jar)) { include("*.so"); into("lib/$abi") }
    }
}

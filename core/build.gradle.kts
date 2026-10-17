import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    `java-library`
}
apply(plugin = "org.jetbrains.kotlin.jvm")

val gdxVersion: String by project

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}

extensions.configure<org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension> {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_1_8)
        // Plain classes instead of invokedynamic keeps the bytecode friendly to every Android dexer.
        freeCompilerArgs.addAll("-Xlambdas=class", "-Xsam-conversions=class", "-Xstring-concat=inline")
    }
}

dependencies {
    "api"("com.badlogicgames.gdx:gdx:$gdxVersion")
    "api"("org.jetbrains.kotlin:kotlin-stdlib:${property("kotlinVersion")}")
    "testImplementation"("junit:junit:4.13.2")
    "testImplementation"("com.badlogicgames.gdx:gdx-backend-headless:$gdxVersion")
    "testImplementation"("com.badlogicgames.gdx:gdx-platform:$gdxVersion:natives-desktop")
}

tasks.withType<Test>().configureEach {
    workingDir = rootProject.file("assets")
    systemProperty("bramble.assets", rootProject.file("assets").absolutePath)
    testLogging { events("failed"); exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL }
}

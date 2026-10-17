import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    application
}
apply(plugin = "org.jetbrains.kotlin.jvm")

val gdxVersion: String by project

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}
extensions.configure<org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension> {
    compilerOptions { jvmTarget.set(JvmTarget.JVM_1_8) }
}

dependencies {
    "implementation"(project(":core"))
    "implementation"("com.badlogicgames.gdx:gdx-backend-lwjgl3:$gdxVersion")
    "implementation"("com.badlogicgames.gdx:gdx-platform:$gdxVersion:natives-desktop")
}

application {
    mainClass.set("com.bramble.dash.lwjgl3.Lwjgl3LauncherKt")
}

sourceSets["main"].resources.srcDir(rootProject.file("assets"))

tasks.named<JavaExec>("run") {
    workingDir = rootProject.file("assets")
    isIgnoreExitValue = true
    if (System.getProperty("os.name").lowercase().contains("mac")) jvmArgs("-XstartOnFirstThread")
    // Pass -Pargs="--screenshots out" etc. through to the game.
    project.findProperty("args")?.let { args(it.toString().split(" ")) }
}

tasks.register<Jar>("dist") {
    group = "distribution"
    description = "Runnable desktop jar for testing."
    archiveFileName.set("BrambleDash-desktop.jar")
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    manifest { attributes["Main-Class"] = application.mainClass.get() }
    dependsOn(configurations["runtimeClasspath"])
    from(configurations["runtimeClasspath"].map { if (it.isDirectory) it else zipTree(it) })
    with(tasks.jar.get())
    exclude("META-INF/*.SF", "META-INF/*.DSA", "META-INF/*.RSA")
}

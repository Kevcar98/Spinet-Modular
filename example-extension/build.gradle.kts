plugins {
    kotlin("jvm")
}

dependencies {
    // compileOnly: the app supplies the contract at runtime. Bundling a second
    // copy would give the extension its own MusicSource class, and every cast
    // across the boundary would fail.
    compileOnly(project(":api"))
}

kotlin {
    compilerOptions { jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_11) }
}
java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}

// The desktop loader reads this before it loads any class from the jar, so an
// extension built for a newer app is refused up front.
tasks.jar {
    archiveFileName.set("spinet-example-extension.jar")
    manifest { attributes("Spinet-Extension-Api" to "1") }
}

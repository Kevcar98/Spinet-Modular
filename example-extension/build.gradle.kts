plugins {
    kotlin("jvm")
}

dependencies {
    compileOnly(project(":api"))
}

kotlin {
    compilerOptions { jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_11) }
}
java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}

tasks.jar {
    archiveFileName.set("spinet-example-extension.jar")
    manifest { attributes("Spinet-Extension-Api" to "1") }
}

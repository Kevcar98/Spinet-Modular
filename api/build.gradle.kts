import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    kotlin("multiplatform")
}

// The coordinate the apps depend on. Inside a composite build it is substituted
// with this project; it only needs to be stable, not published.
group = "org.bgdynamix.spinet"
version = "1"

kotlin {
    // No Android target on purpose: an Android consumer accepts a jvm variant,
    // and leaving it out keeps the Android Gradle plugin, and its version, out
    // of the one module every extension has to build against.
    jvm {
        // Java 11 bytecode, matching both apps.
        compilerOptions { jvmTarget.set(JvmTarget.JVM_11) }
    }

    // Declared so the mobile app's iOS source set can still resolve its
    // commonMain dependencies. They only compile on a Mac.
    iosX64()
    iosArm64()
    iosSimulatorArm64()
}

plugins {
    // Same Kotlin as the apps. The contract is consumed from source through a
    // composite build, so a mismatch here would surface as a metadata error in
    // the app rather than here.
    kotlin("multiplatform") version "2.2.20" apply false
    kotlin("jvm") version "2.2.20" apply false
}

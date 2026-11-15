plugins {
    alias(libs.plugins.android.application) apply false
    // AGP 9 compiles Kotlin itself; the Compose compiler plugin must match its Kotlin version.
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.ksp) apply false
}

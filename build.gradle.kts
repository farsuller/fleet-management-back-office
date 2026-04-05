plugins {
    // This file is used to declare plugin versions for the whole project.
    alias(libs.plugins.androidApplication) apply false
    alias(libs.plugins.androidLibrary) apply false
    alias(libs.plugins.kotlinMultiplatform) apply false
    alias(libs.plugins.kotlinAndroid) apply false
    alias(libs.plugins.composeMultiplatform) apply false
    alias(libs.plugins.composeCompiler) apply false
    alias(libs.plugins.kotlinSerialization) apply false
    alias(libs.plugins.spotless)
}

// Apply Spotless formatting to all modules except the platform-specific :androidApp
// This allows the Android app to maintain internal Google-style formatting while
// enforcing project-wide ktlint standards on the shared Multiplatform code.
configure(subprojects.filter { it.name != "androidApp" }) {
    apply(plugin = "com.diffplug.spotless")
    spotless {
        kotlin {
            target("**/*.kt")
            targetExclude("**/build/**/*.kt")
            // Relax rules for Compose Multiplatform (allow PascalCase @Composable names and long lines)
            ktlint().editorConfigOverride(mapOf(
                "ktlint_standard_function-naming" to "disabled",
                "ktlint_standard_max-line-length" to "disabled"
            ))
        }
        kotlinGradle {
            target("*.gradle.kts")
            ktlint()
        }
    }
}
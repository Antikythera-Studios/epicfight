plugins {
    // ModDevGradle is applied in both the ":common" and ":neoforge" projects,
    // so it is declared at the root to ensure the plugin is resolved only once
    // and shared consistently across Gradle projects.
    alias(libs.plugins.moddevgradle) apply false
}

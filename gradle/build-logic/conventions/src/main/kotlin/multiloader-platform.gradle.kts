// Configures shared Gradle setup between mod platform Gradle projects (e.g., neoforge, fabric),
// which enables "multiloader-base".
//
// Adds a dependency on ":common", and bundles all classes and
// processed resources to the consumer Gradle project, so end-users won't need additional JAR files.

plugins {
    id("multiloader-base")
}

val commonProject: Project = project(":common")

dependencies {
    api(commonProject)
}

// Includes the "common" project's sources and processed resources in this platform project

tasks.compileJava { source(commonProject.sourceSets.main.get().allSource) }

val commonProcessResources = commonProject.tasks.processResources

tasks.processResources {
    dependsOn(commonProcessResources)
    from(commonProcessResources.map { it.outputs.files })
}

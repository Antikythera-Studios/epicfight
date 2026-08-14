// Configures shared Gradle setup between mod platform Gradle projects (e.g., neoforge, fabric),
// which enables "multiloader-base" and mod publish plugins.
//
// Adds a dependency on ":common", and bundles all classes and
// processed resources to the consumer Gradle project, so end-users won't need additional JAR files.
//
// Consumers of this Gradle plugin are expected to manually call the following methods:
//
// * configureModPublish: with the mod loader and output JAR file, since it's different based on the mod toolchain.
//    This will set up the mod publishing to CurseForge and Modrinth.

plugins {
    id("multiloader-base")
    id("me.modmuss50.mod-publish-plugin")
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

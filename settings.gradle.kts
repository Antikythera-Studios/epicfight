pluginManagement {
    repositories {
        fun strictMaven(name: String, url: String, filter: InclusiveRepositoryContentDescriptor.() -> Unit) {
            exclusiveContent {
                forRepository { maven { this.name = name; this.url = uri(url) } }
                filter { filter() }
            }
        }

        fun strictMaven(name: String, url: String, vararg includeGroups: String) {
            strictMaven(name, url) {
                includeGroups.forEach { includeGroup(it) }
            }
        }

        gradlePluginPortal()

        // Note: Prefer "strictMaven()" over "maven { ... }" to reduce unnecessary repository lookups (i.e., reduce network calls)

        strictMaven(
            "NeoForged",
            "https://maven.neoforged.net/releases",
            "net.neoforged"
        )
        strictMaven(
            "Fabric",
            "https://maven.fabricmc.net/",
        ) {
            @Suppress("UnstableApiUsage")
            includeGroupAndSubgroups("net.fabricmc")
            includeGroup("fabric-loom")
        }
    }
    // Gradle Convention Plugins: https://docs.gradle.org/current/userguide/implementing_gradle_plugins_convention.html
    includeBuild("gradle/build-logic")
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention").version("1.0.0")
}

rootProject.name = "Epic Fight"

// Local dependencies
includeBuild("../akythera_26_1_multiloader")
includeBuild("../akythera_labs_26_1_multiloader")

include(
    ":common",
    ":neoforge",
    ":fabric",
)
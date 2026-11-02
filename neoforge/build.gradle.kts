plugins {
    id("multiloader-platform")
    alias(libs.plugins.moddevgradle)
}

val neoforgeVersion: String = libs.versions.neoforge.get()

configureBaseArchive("neoforge")

neoForge {
    version = neoforgeVersion

    runs {
        create("client") {
            client()
            systemProperty("akytheralabs.projects_dir", rootProject.file("akythera_projects").absolutePath)
        }

        create("server") {
            server()
            programArgument("--nogui")
        }

        configureEach {
            systemProperty("forge.logging.markers", "REGISTRIES")
            logLevel = org.slf4j.event.Level.DEBUG
        }
    }

    mods {
        register(modId) {
            sourceSet(sourceSets.main.get())
        }
    }
}

dependencies {
    implementation("io.github.yesssssman.akythera:neoforge")
    implementation("io.github.yesssssman.akytheralabs:neoforge")
}

tasks.processResources {
    val replaceProperties = modPlatformMetadataReplaceProperties

    inputs.properties(replaceProperties)

    filesMatching("META-INF/*.mods.toml") {
        expand(replaceProperties)
    }
}

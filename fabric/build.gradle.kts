plugins {
    id("multiloader-platform")
    alias(libs.plugins.fabric.loom)
}

configureBaseArchive("fabric")

repositories {
    terraformersRepository()
}

dependencies {
    minecraft(libs.fabric.minecraft)

    implementation(libs.fabric.loader)

    // Fabric API
    implementation(libs.fabric.api)

    implementation("io.github.yesssssman.akythera:fabric")
    implementation("io.github.yesssssman.akytheralabs:fabric")
}

loom {
    runs {
        named("client") {
            property("akytheralabs.projects_dir", rootProject.file("akythera_projects").absolutePath)
        }
    }

    mods {
        register(modId) {
            sourceSet(sourceSets.main.get())
        }
    }
}

tasks.processResources {
    val replaceProperties = modPlatformMetadataReplaceProperties

    inputs.properties(replaceProperties)

    filesMatching("fabric.mod.json") {
        expand(replaceProperties)
    }
}

fabricApi {
    configureDataGeneration {
        client = true
    }
}
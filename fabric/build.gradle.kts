plugins {
    id("multiloader-platform")
    alias(libs.plugins.fabric.loom)
}

configureBaseArchive("fabric")

repositories {
    parchmentMcRepository()
    terraformersRepository()
}

dependencies {
    minecraft(libs.fabric.minecraft)

    implementation(libs.fabric.loader)

    // Fabric API
    implementation(libs.fabric.api)

    implementation("io.github.yesssssman.akythera:fabric")
    implementation("io.github.yesssssman.akytheralabs:fabric")

    implementation(libs.forgeconfigapiport.fabric) {
        exclude(group = libs.fabric.loader.get().group)
        exclude(group = libs.fabric.api.get().group)
    }

    // Performance profiling
    // implementation("curse.maven:spark-361579:7815875")
}

loom {
    // Removed due to lack of NeoForge support: https://github.com/neoforged/ModDevGradle/issues/3 and https://neoforged.net/news/21.5release/#split-sourcesets
    // splitEnvironmentSourceSets()

    accessWidenerPath = file("../common/src/main/resources/epicfight.accesswidener")

    mods {
        register(modId) {
            sourceSet(sourceSets.main.get())
            // Removed due to lack of NeoForge support: https://github.com/neoforged/ModDevGradle/issues/3 and https://neoforged.net/news/21.5release/#split-sourcesets
            // sourceSet(sourceSets.named("client").get())
        }
    }
/*
    runs {
        register("datagen") {
            inherit(getByName("client"))
            name("Data Generation")
            vmArg("-Dfabric-api.datagen")
            vmArg("-Dfabric-api.datagen.output-dir=${file("src/main/generated")}")
            vmArg("-Dfabric-api.datagen.modid=template-mod")

            runDir("build/datagen")
        }
    }
*/
}

tasks.processResources {
    val replaceProperties = modPlatformMetadataReplaceProperties

    inputs.properties(replaceProperties)

    filesMatching("fabric.mod.json") {
        expand(replaceProperties)
    }
}

configureModPublish(ModLoader.Fabric) { tasks.jar.get().archiveFile }

fabricApi {
    configureDataGeneration {
        client = true
    }
}
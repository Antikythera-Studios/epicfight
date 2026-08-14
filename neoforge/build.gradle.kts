plugins {
    id("multiloader-platform")
    alias(libs.plugins.moddevgradle)
}

val neoforgeVersion: String = libs.versions.neoforge.get()

configureBaseArchive("neoforge")

neoForge {
    version = neoforgeVersion

    accessTransformers.from("../common/src/main/resources/META-INF/accesstransformer.cfg")

    runs {
        create("client") {
            client()
            systemProperty("neoforge.enabledGameTestNamespaces", modId)
        }

        create("server") {
            server()
            programArgument("--nogui")
            systemProperty("neoforge.enabledGameTestNamespaces", modId)
        }

        create("gameTestServer") {
            type = "gameTestServer"
            systemProperty("neoforge.enabledGameTestNamespaces", modId)
        }

        create("data") {
            clientData()
            programArguments.addAll(
                "--mod", modId,
                "--all",
                "--output", file("src/generated/resources/").absolutePath,
                "--existing", file("src/main/resources/").absolutePath
            )
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

sourceSets.main {
    resources.srcDir(layout.projectDirectory.dir("src/generated/resources"))
}

configurations {
    val localRuntime by creating

    runtimeClasspath {
        extendsFrom(localRuntime)
    }
}

dependencies {
    // Prefer "localRuntime" over "runtimeOnly" to not enforce runtime mods as transitive dependencies,
    // when using "maven-publish" plugin.
    val localRuntime by configurations.getting

    implementation("io.github.yesssssman.akythera:neoforge")
    implementation("io.github.yesssssman.akytheralabs:neoforge")

    // compileOnly(libs.jei.common.api)
    // compileOnly(libs.jei.neoforge.api)
    // localRuntime(libs.jei.neoforge)

    // compileOnly(libs.azurelib)
    // compileOnly(libs.azurelib.armor)
    // compileOnly(libs.geckolib)

    // compileOnly(libs.sodium)
    // compileOnly(libs.iris)

    // compileOnly(libs.werewolves)
    // compileOnly(libs.vampirism)

    // compileOnly(libs.curios)
    // compileOnly(libs.playerAnimator)

    // compileOnly(libs.kubejs)
    // compileOnly(libs.rhino)
    // compileOnly(libs.architectury)

    // Libraries for tr7zw featured mods
    // compileOnly("libs:TRansition:1.0.6")
    // compileOnly("libs:TRender:1.0.7")

    // compileOnly(libs.skinlayers)
    // compileOnly(libs.firstPersonModel)
    // compileOnly(libs.shoulderSurfing)

    // compileOnly(libs.controlify) {
    //     // Only need Controlify API, ignore the transitive dependencies (e.g, QuiltMC parsers)
    //     isTransitive = false
    // }
}

tasks.processResources {
    val replaceProperties = modPlatformMetadataReplaceProperties

    inputs.properties(replaceProperties)

    filesMatching("META-INF/*.mods.toml") {
        expand(replaceProperties)
    }
}

configureModPublish(ModLoader.NeoForge) { tasks.jar.get().archiveFile }

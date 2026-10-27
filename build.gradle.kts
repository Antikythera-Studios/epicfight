plugins {
    alias(libs.plugins.eclipse)
    alias(libs.plugins.idea)
    alias(libs.plugins.moddevgradle)
    alias(libs.plugins.publisher)
    alias(libs.plugins.mcSafeResources)
}

val mod_group_id: String by project
val mod_version: String by project
val mod_id: String by project
val minecraft_version: String by project
val forge_version: String by project
val parchment_version: String by project

val minecraft_version_range: String by project
val forge_version_range: String by project
val loader_version_range: String by project
val mod_name: String by project
val mod_license: String by project
val mod_authors: String by project
val mod_description: String by project

group = mod_group_id
version = mod_version

val mixinConfigDirectory: String = "mixins.$mod_id.json"
val mixinArgument: String = "-mixin.config=$mixinConfigDirectory"

val modPascalCase: String = mod_id.split('_').joinToString("") { it.replaceFirstChar { char -> char.uppercase() } }

fun getFullModVersion(): String {
    return "${mod_version}-mc${minecraft_version}-forge"
}

base {
    archivesName.set("epic-fight")
    version = getFullModVersion()
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(17))
    withSourcesJar()
}

// Wires Mixin's annotation processor so a refmap is generated and reobfJar can remap it.
//
// Without this the mod loads in a development run and crashes in a real one. Dev uses named mappings, so a
// @Shadow of getCameraEntity resolves against Minecraft directly; production uses SRG member names, and the
// remapper cannot rename the shadow for us because it is a method declared on the mixin class rather than a
// reference to Minecraft's. The refmap is what carries that translation, and nothing here was producing one:
// every shipped jar failed with "No refMap loaded" on the first @Shadow it reached.
mixin {
    add(sourceSets.main.get(), "mixins.$mod_id.refmap.json")
    config(mixinConfigDirectory)
}

legacyForge {
    version = ("$minecraft_version-$forge_version")
    accessTransformers.from(file("src/main/resources/META-INF/accesstransformer.cfg"))

    parchment {
        mappingsVersion.set(parchment_version)
        minecraftVersion.set(minecraft_version)
    }

    runs {
        configureEach {
            programArgument(mixinArgument)
        }

        create("clientProduction")
        {
            client()
            systemProperty("net.minecraftforge.gradle.GradleStart.srg.srg-mcp", "false")
            devLogin.set(true)
        }

        create("client") {
            client()
            devLogin.set(true)
            systemProperty("forge.logging.markers", "REGISTRIES")
            systemProperty("forge.logging.console.level", "debug")
            systemProperty("forge.enabledGameTestNamespaces", mod_id)
        }

        create("clientNoAuth") {
            client()
            systemProperty("forge.logging.markers", "REGISTRIES")
            systemProperty("forge.logging.console.level", "debug")
            systemProperty("forge.enabledGameTestNamespaces", mod_id)
        }

        create("server") {
            server()
            systemProperty("forge.logging.markers", "REGISTRIES")
            systemProperty("forge.logging.console.level", "debug")
            systemProperty("forge.enabledGameTestNamespaces", mod_id)
        }

        create("data") {
            data()
            systemProperty("forge.logging.markers", "REGISTRIES")
            systemProperty("forge.logging.console.level", "debug")
            systemProperty("forge.enabledGameTestNamespaces", mod_id)
        }
    }

    mods {
        create(mod_id) {
            sourceSet(sourceSets.main.get())
        }
    }


}

sourceSets.main {
    resources {
        srcDir("src/generated/resources")
    }
}

repositories {
    fun RepositoryHandler.strictMaven(url: String, name: String?= null, vararg groups: String) {
        exclusiveContent {
            forRepository {
                maven {
                    this.url = uri(url)
                    if (name != null) this.name = name
                }
            }
            filter {
                groups.forEach { includeGroupAndSubgroups(it) }

            }
        }
    }
    strictMaven("https://cursemaven.com", "Curse Maven", "curse.maven")
    strictMaven("https://api.modrinth.com/maven", "Modrinth", "maven.modrinth")
    strictMaven("https://maven.blamejared.com/", "Jared's maven")
    strictMaven("https://maven.architectury.dev", null, "dev.architectury")
    strictMaven("https://maven.latvian.dev/releases", null, "dev.latvian.mods")
    strictMaven("https://echoellet.github.io/Controlify/", null, "dev.echoellet")

    flatDir {
        dir("./libs")
    }
    mavenCentral()
}

dependencies {
    // Mixin's annotation processor. It writes the refmap that translates named members to SRG at runtime;
    // without it on this configuration the mixin extension has nothing to run and no refmap is produced.
    annotationProcessor("org.spongepowered:mixin:0.8.5:processor")

    // JEI (API for code, Runtime for testing)
    modCompileOnly(libs.jei.common.api)
    modCompileOnly(libs.jei.forge.api)
    modRuntimeOnly(libs.jei.forge.runtime)

    modCompileOnly(libs.female.gender)
    modCompileOnly(libs.azurelib)
    modCompileOnly(libs.geckolib)

    modCompileOnly(libs.vampirism)
    modCompileOnly(libs.curios)
    modCompileOnly(libs.citadel)
    modCompileOnly(libs.iceandfire)
    modCompileOnly(libs.playeranimator)
    modCompileOnly(libs.skinlayer3d)
    modCompileOnly(libs.firstperson)
    modCompileOnly(libs.shouldersurfing)
    modCompileOnly(libs.creativecore)

    // CurseMaven Dependencies
    modCompileOnly(libs.azurelib.armor)
    modCompileOnly(libs.werewolves)
    modCompileOnly(libs.playerrevive)

    // Flat Dir / Custom Maven Libraries
    modCompileOnly(libs.transition)
    modCompileOnly(libs.trender)

    // KubeJS Stack (Using your bundle)
    // Bundles work perfectly with modImplementation
    modImplementation(libs.bundles.kubejs.stack)

    // Controlify & YACL
    modCompileOnly(libs.controlify)
    modRuntimeOnly(libs.yacl)

    // Modrinth-migrated dependencies
    modCompileOnly(libs.embeddium)
    modCompileOnly(libs.oculus)

}

tasks.named<ProcessResources>("processResources").configure {
    val replaceProperties: Map<String, String> = mapOf(
            "minecraft_version" to minecraft_version,
            "minecraft_version_range" to minecraft_version_range,
            "forge_version" to forge_version,
            "forge_version_range" to forge_version_range,
            "loader_version_range" to loader_version_range,
            "mod_id" to mod_id,
            "mod_name" to mod_name,
            "mod_license" to mod_license,
            "mod_version" to mod_version,
            "mod_authors" to mod_authors,
            "mod_description" to mod_description
    )

    inputs.properties(replaceProperties)

    filesMatching(listOf("META-INF/mods.toml", "pack.mcmeta")) {
        expand(replaceProperties + mapOf("project" to project))
    }
}

mcSafeResources {
    namespace.set(mod_id)
    outputPackage.set("yesman.${mod_id}.generated")
}

java.sourceSets.main.get().java.srcDirs(
    tasks.generateLangKeys.map { it.outputs.files.singleFile },
    tasks.generateSoundKeys.map { it.outputs.files.singleFile }
)


tasks.named<Jar>("jar").configure {
    manifest {
        attributes("MixinConfigs" to mixinConfigDirectory)
    }
    finalizedBy(tasks.named("reobfJar"))
}

val apiPackage: String = "yesman/epicfight/api/**"
val apiJarClassifier: String = "api"

val apiJar: TaskProvider<Jar> = tasks.register<Jar>("apiJar") {
    group = "build"
    from(sourceSets.main.get().output) {
        include(apiPackage)
    }
}

val apiSourcesJar: TaskProvider<Jar> = tasks.register<Jar>("apiSourcesJar") {
    group = "build"
    archiveClassifier.set("${apiJarClassifier}-sources")

    from(sourceSets.main.get().allSource) {
        include(apiPackage)
    }
}

val TaskContainer.jar: TaskProvider<Jar>
    get() = named<Jar>("jar")

publishMods {
    val readme: File = project.file("CHANGELOG.md")
    val readmeContent: String = readme.readText()

    val pattern: Regex = """(?s)## \[$mod_version\] - \d{4}-\d{2}-\d{2}\R(.*?)(?=\R### For Devs|\R## \[.*?\] |\Z)""".toRegex()
    val matchResult: MatchResult = pattern.find(readmeContent) ?: throw RuntimeException("No changelog found for version $mod_version in CHANGELOG.md file")

    val latestChangelog: String = matchResult.groupValues[1]
    dryRun = false

    changelog.set("""
        |$latestChangelog
        |
        |**Tested against:**
        |- **Forge:** $forge_version
        |- **Minecraft:** $minecraft_version
    """.trimMargin().trim())

    // The name of the mod loader
    modLoaders.add("forge")

    // Type of the release: ALPHA, BETA, STABLE
    type = BETA

    // The name of the file appeared in publishing websites
    displayName = getFullModVersion()

    file.set(tasks.named<Jar>("reobfJar").flatMap { it.archiveFile })
    additionalFiles.from(tasks.named<Jar>("sourcesJar").flatMap { it.archiveFile }, apiJar, apiSourcesJar)

    // Curseforge publishing info
    curseforge {
        accessToken = providers.environmentVariable("CURSEFORGE_TOKEN")
        projectId = "405076"
        minecraftVersions.add("1.20.1")
        projectSlug = "epic-fight-mod"
    }
    // Modrinth publishing info
    modrinth {
        accessToken = providers.environmentVariable("MODRINTH_TOKEN")
        projectId = "vu3NZ5Ma"
        minecraftVersions.add("1.20.1")
    }
    // Discord webhook and notification settings
    discord {
        webhookUrl = providers.environmentVariable("DISCORD_WEBHOOK")
        dryRunWebhookUrl = providers.environmentVariable("DRY_RUN_DISCORD_WEBHOOK")
        username = "Update Notification"
        avatarUrl = "https://i.imgur.com/FrxDviN.png"
        content = changelog.map { "<@&1074034800849059930>\n# Epic Fight ${mod_version} is out!\nMinecraft version: ${minecraft_version}\nForge version: ${forge_version}\n" + latestChangelog }

        style {
            look = "MODERN"
            link = "EMBED"
            thumbnailUrl = "https://i.imgur.com/nI8xOCy.png"
        }
    }
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
}

tasks.jar {
    from(rootProject.file("LICENSE"))
    from(rootProject.file("LICENSE-ASSETS"))
    from(rootProject.file("LICENSE-ASSETS")) {
        into("assets/${mod_id}")
    }
    from(rootProject.file("LICENSE-ASSETS")) {
        into("assets/minecraft")
    }
}
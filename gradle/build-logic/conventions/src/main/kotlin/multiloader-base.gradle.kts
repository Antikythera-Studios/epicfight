// Configures shared Gradle setup for all Gradle projects.
// This plugin is meant to be applied to every project, including ":common", ":neoforge" and ":fabric".
//
// Consumers of this Gradle plugin are expected to manually call the following methods:
//
// * configureBaseArchive: with the Gradle project name to produce unique JAR file names.

plugins {
    `java-library`
    idea
}

version = modVersion
group = groupId

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(javaVersion))
    }

    sourceCompatibility = JavaVersion.toVersion(javaVersion)
    targetCompatibility = JavaVersion.toVersion(javaVersion)
    withSourcesJar()
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.release.set(javaVersion)
    // Retain method parameter names in the bytecode so the script-modularizer can reflect
    // real names (Method#getParameters) for ScriptFunction pin labels instead of arg0, arg1...
    options.compilerArgs.add("-parameters")
}

idea {
    module {
        isDownloadSources = true
        isDownloadJavadoc = true
    }
}

repositories {
    mavenCentral()

    flatDir { dir(rootProject.file("libs")) }

    // Modrinth maven: https://support.modrinth.com/en/articles/8801191-modrinth-maven
    // Note: Prefer Maven repositories provided by the mod dependencies over Modrinth.
    strictMaven(
        "Modrinth",
        "https://api.modrinth.com/maven",
        "maven.modrinth",
    )

    // Curse maven: https://cursemaven.com
    // Note: Prefer Modrinth or Maven repositories provided by the mod dependencies over Curse Maven.
    strictMaven(
        "CurseMaven",
        "https://cursemaven.com",
        "curse.maven",
    )

    strictMaven(
        "Fuzs Mod Resources (Forge Config API)",
        "https://raw.githubusercontent.com/Fuzss/modresources/main/maven/",
        "fuzs.forgeconfigapiport",
    )

    // JEI
    strictMaven("Jared", "https://maven.blamejared.com", "mezz.jei")
    strictMaven("Architectury", "https://maven.architectury.dev", "dev.architectury")

    // KubeJS
    strictMaven("Latvian", "https://maven.latvian.dev/releases", "dev.latvian.mods")

    strictMaven("IsXander", "https://maven.isxander.dev/releases", "dev.isxander")

    strictMaven("AzureDoom Mods", "https://maven.azuredoom.com/mods", "mod.azure.azurelib", "mod.azure.azurelibarmor")
    strictMaven("GeckoLib", "https://dl.cloudsmith.io/public/geckolib3/geckolib/maven/", "software.bernie.geckolib")
    strictMaven("Illusive Soulworks (Curios API)", "https://maven.theillusivec4.top/", "top.theillusivec4.curios")
    strictMaven("playerAnimator", "https://maven.kosmx.dev", "dev.kosmx.player-anim")
}

tasks.jar {
    from(rootProject.file("LICENSE"))
    from(rootProject.file("LICENSE-ASSETS"))
    from(rootProject.file("LICENSE-ASSETS")) { into("assets/$modId") }
    from(rootProject.file("LICENSE-ASSETS")) { into("assets/minecraft") }
}

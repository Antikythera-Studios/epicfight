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

    // Modrinth maven: https://support.modrinth.com/en/articles/8801191-modrinth-maven
    // Note: Prefer Maven repositories provided by the mod dependencies over Modrinth.
    strictMaven(
        "Modrinth",
        "https://api.modrinth.com/maven",
        "maven.modrinth",
    )

    strictMaven(
        "Fuzs Mod Resources (Forge Config API)",
        "https://raw.githubusercontent.com/Fuzss/modresources/main/maven/",
        "fuzs.forgeconfigapiport",
    )
}

tasks.jar {
    from(rootProject.file("LICENSE"))
}

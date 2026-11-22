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

// Writes the compile spec AkytheraLabs's in-game hot reload (Tools > Compile Source Code) compiles this platform's
// classes with: the source folders and their patterns, the compile classpath and options, the class and resource
// output folders. Written to build/akytheralabs/hotreload_spec.json after every compileJava.
val writeHotReloadSpec = tasks.register("writeHotReloadSpec") {
    val specFile = layout.buildDirectory.file("akytheralabs/hotreload_spec.json")

    outputs.file(specFile)
    outputs.upToDateWhen { false }

    doLast {
        val compileJava = tasks.compileJava.get()
        val sources = listOf(sourceSets.main.get().java, commonProject.sourceSets.main.get().java).flatMap { set ->
            set.srcDirs.map { dir -> mapOf("dir" to dir.absolutePath, "includes" to set.includes.toList(), "excludes" to set.excludes.toList()) }
        }
        val options = compileJava.options.allCompilerArgs.toMutableList()

        compileJava.options.release.orNull?.let { options += listOf("--release", it.toString()) }
        compileJava.options.encoding?.let { options += listOf("-encoding", it) }
        if (compileJava.options.isDebug) options += compileJava.options.debugOptions.debugLevel?.let { "-g:$it" } ?: "-g"

        val spec = mapOf(
            "sources" to sources,
            "classpath" to compileJava.classpath.files.map { it.absolutePath },
            "options" to options,
            "output" to compileJava.destinationDirectory.get().asFile.absolutePath,
            "resources" to tasks.processResources.get().destinationDir.absolutePath,
        )
        val file = specFile.get().asFile

        file.parentFile.mkdirs()
        file.writeText(groovy.json.JsonOutput.prettyPrint(groovy.json.JsonOutput.toJson(spec)))
    }
}

tasks.compileJava { finalizedBy(writeHotReloadSpec) }

// Runs the client ready for AkytheraLabs's in-game hot reload: on the JetBrains Runtime with enhanced class
// redefinition, AkytheraLabs as a Java agent, and the compile specs of this project and of the included Akythera and
// AkytheraLabs builds. Off with -PakytheraHotReload=false.
if (providers.gradleProperty("akytheraHotReload").getOrElse("true").toBoolean()) {
    val specFiles = listOf("akythera_26_1_multiloader", "akythera_labs_26_1_multiloader")
        .mapNotNull { name -> gradle.includedBuilds.find { it.name == name }?.projectDir?.resolve("${project.name}/build/akytheralabs/hotreload_spec.json") }
        .plus(layout.buildDirectory.file("akytheralabs/hotreload_spec.json").get().asFile)

    // After every plugin's own setup, which sets the launcher of its run tasks
    gradle.projectsEvaluated {
        tasks.withType<JavaExec>().matching { it.name == "runClient" }.configureEach {
            javaLauncher.set(javaToolchains.launcherFor {
                languageVersion.set(JavaLanguageVersion.of(project.javaVersion))
                vendor.set(JvmVendorSpec.JETBRAINS)
            })
            jvmArgumentProviders.add(CommandLineArgumentProvider {
                val labsJar = configurations.runtimeClasspath.get().files
                    .firstOrNull { it.name.lowercase().replace(" ", "").startsWith("akytheralabs") && it.name.endsWith(".jar") }

                listOfNotNull(
                    "-XX:+AllowEnhancedClassRedefinition",
                    labsJar?.let { "-javaagent:${it.absolutePath}" },
                    "-Dakytheralabs.hotreload.specs=" + specFiles.joinToString(File.pathSeparator) { it.absolutePath },
                )
            })
        }
    }
}

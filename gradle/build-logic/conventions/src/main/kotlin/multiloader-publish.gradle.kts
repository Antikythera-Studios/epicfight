import me.modmuss50.mpp.ReleaseType

plugins {
    id("me.modmuss50.mod-publish-plugin")
}

fun currentChangelog(): String? {
    val text = rootProject.file("CHANGELOG.md").readText()
    val heading = Regex("""(?s)##\s+${Regex.escape(modVersion)}\s+-\s+\d{4}-\d{2}-\d{2}\s*?\R(.*?)(?=\R##\s|\z)""")
    return heading.find(text)?.groupValues?.get(1)?.trim()
}

val releaseChangelog = currentChangelog()?.let {
    """
    |$it
    |
    |### **Tested against:**
    |- **Minecraft:** $mcVersion
    |- **NeoForge:** $neoForgeVersion
    |- **Fabric Loader:** $fabricLoaderVersion
    |- **Fabric API:** $fabricApiVersion
    """.trimMargin()
}

gradle.taskGraph.whenReady {
    val publishing = allTasks.any { it.project == rootProject && it.name.startsWith("publish") }

    if (publishing && releaseChangelog == null) {
        throw GradleException("cannot publish: no changelog entry for $modVersion in CHANGELOG.md")
    }
}

publishMods {
    dryRun = providers.gradleProperty("publishDryRun").map(String::toBoolean).orElse(false)
    type = ReleaseType.of(providers.gradleProperty("release_type").get())
    changelog = releaseChangelog ?: ""

    val curseforgeOptions = curseforgeOptions {
        accessToken = providers.environmentVariable("EPIC_FIGHT_CURSEFORGE_API")
        projectId = "405076"
        projectSlug = "epic-fight-mod"
        minecraftVersions.add(mcVersion)
        client = true
        server = true
    }

    val modrinthOptions = modrinthOptions {
        accessToken = providers.environmentVariable("EPIC_FIGHT_MODRINTH_API")
        projectId = "vu3NZ5Ma"
        minecraftVersions.add(mcVersion)
    }

    for ((loader, label) in listOf("neoforge" to "NeoForge", "fabric" to "Fabric")) {
        curseforge("curseforge${label}") {
            from(curseforgeOptions)
            file(project(":$loader"))
            modLoaders.add(loader)
            displayName = "$modDisplayName $modVersion - $label $mcVersion"
            version = "$modVersion-mc$mcVersion-$loader"
            announcementTitle = "Download from CurseForge ($loader)"
            requires("akythera")
            if (loader == "fabric") requires("fabric-api")
        }

        modrinth("modrinth${label}") {
            from(modrinthOptions)
            file(project(":$loader"))
            modLoaders.add(loader)
            displayName = "$modDisplayName $modVersion - $label $mcVersion"
            version = "$modVersion-mc$mcVersion-$loader"
            announcementTitle = "Download from Modrinth ($loader)"
            requires("jHm8Ac9W")
            if (loader == "fabric") requires("fabric-api")
        }
    }

    val webhook = providers.environmentVariable("EPIC_FIGHT_DISCORD_WEBHOOK").filter(String::isNotBlank)

    if (webhook.isPresent) {
        discord {
            webhookUrl = webhook
            dryRunWebhookUrl = providers.environmentVariable("DRY_RUN_DISCORD_WEBHOOK")
            username = "Update Notification"
            avatarUrl = "https://i.imgur.com/FrxDviN.png"
            content = changelog.map { "<@&1074034800849059930>\n# $modDisplayName $modVersion is out!\n\n$it" }
            style {
                look = "MODERN"
                link = "EMBED"
                thumbnailUrl = "https://i.imgur.com/nI8xOCy.png"
            }
        }
    }
}

plugins {
    id("multiloader-base")
    alias(libs.plugins.moddevgradle)
    alias(libs.plugins.mcSafeResources)
}

configureBaseArchive("common")

neoForge {
    neoFormVersion = libs.versions.neoform.get()
}

dependencies {
    compileOnly(libs.mixin)
    compileOnly(libs.asm.tree) // Manual import for implementations of IMixinConfigPlugin
    compileOnly(libs.forgeconfigapiport.common)

    //api(libs.playerAnimationLibrary)
    //api(libs.bendableCuboids)

    api("io.github.yesssssman.akythera:common")
    implementation("io.github.yesssssman.akytheralabs:common")
}

mcSafeResources {
    namespace.set(modId)
    outputPackage.set("com.yesman.${modId}.generated")
}

sourceSets.main.get().java {}

java.sourceSets.main.get().java.srcDirs(
    tasks.generateLangKeys.map { it.outputs.files.singleFile },
    tasks.generateSoundKeys.map { it.outputs.files.singleFile }
)

tasks.compileJava { dependsOn(tasks.generateLangKeys, tasks.generateSoundKeys) }
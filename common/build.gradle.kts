plugins {
    id("multiloader-base")
    alias(libs.plugins.moddevgradle)
}

configureBaseArchive("common")

neoForge {
    neoFormVersion = libs.versions.neoform.get()
}

dependencies {
    api("io.github.yesssssman.akythera:common")
    compileOnly("io.github.yesssssman.akytheralabs:common")
}

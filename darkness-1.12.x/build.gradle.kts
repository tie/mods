plugins {
    id("net.minecraftforge.gradle") version "4.0.16"
    id("org.spongepowered.mixin") version "0.7-SNAPSHOT"
    id("com.matthewprenger.cursegradle") version "1.4.0"
    id("co.riiid.gradle") version "0.4.2"
    id("io.github.yamporg.gradle.env-version")
    id("io.github.yamporg.gradle.upload-task")
    id("io.github.yamporg.gradle.curse-release")
    id("io.github.yamporg.gradle.github-release")
    id("io.github.yamporg.gradle.reproducible-builds")
    id("io.github.yamporg.gradle.forge-modinfo-version")
    id("io.github.yamporg.gradle.forge-prepare-runs-fix")
    id("io.github.yamporg.gradle.minecraft-lwjgl-version-fix")
}

minecraft {
    mappings("stable", "39-1.12")
    runs {
        create("client") {
            properties(
                mapOf(
                    "forge.logging.markers" to "SCAN,REGISTRIES,REGISTRYDUMP",
                    "forge.logging.console.level" to "debug",
                    "fml.coreMods.load" to "io.github.yamporg.darkness.DarknessLoadingPlugin"
                )
            )
        }
    }
}

repositories {
    maven { url = uri("https://repo.spongepowered.org/maven") }
}

dependencies {
    minecraft("net.minecraftforge:forge:1.12.2-14.23.5.2855")

    implementation("org.spongepowered:mixin:0.8.2")
    annotationProcessor("org.spongepowered:mixin:0.8.2:processor")
}

java {
    withSourcesJar()
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(8))
    }
}

tasks.jar {
    manifest.attributes(
        "FMLCorePluginContainsFMLMod" to true,
        "FMLCorePlugin" to "io.github.yamporg.darkness.DarknessLoadingPlugin"
    )
}

curseforge {
    project(
        closureOf<com.matthewprenger.cursegradle.CurseProject> {
            id = "363102"
            releaseType = "release"

            changelogType = "markdown"
            changelog = file("CHANGELOG.md")

            mainArtifact(
                tasks["jar"],
                closureOf<com.matthewprenger.cursegradle.CurseArtifact> {
                    relations(
                        closureOf<com.matthewprenger.cursegradle.CurseRelation> {
                            requiredDependency("mixinbootstrap")
                        }
                    )
                }
            )
            addArtifact(tasks["sourcesJar"])
        }
    )
}

github {
    body = file("CHANGELOG.md").readText()
    setAssets(*tasks["jar"].outputs.files.map { name }.toTypedArray())
}

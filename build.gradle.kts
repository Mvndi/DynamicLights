import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    id("io.github.goooler.shadow") version "8.1.8"
    id("java")
    id("java-library")
    `maven-publish`
    id("xyz.jpenilla.run-paper") version "2.3.1"
}

group = "github.xCykrix"
version = "1.6.0"
description="Dynamic Lights for Minecraft Servers without requiring Modding."
val mainMinecraftVersion = "1.21.11"
val supportedMinecraftVersions = "1.21.11 - 1.21.11"

repositories {
    mavenLocal()
    mavenCentral()

    // Paper
    maven("https://repo.papermc.io/repository/maven-public/")

    // Upstream GitHub Packages
    maven {
        url = uri("https://maven.pkg.github.com/xCykrix/SpigotDevkit")
        credentials {
            username = "" //project.findProperty("GITHUB_ACTOR").toString() ?: System.getenv("GITHUB_ACTOR")
            password = "" //project.findProperty("GITHUB_TOKEN").toString() ?: System.getenv("GITHUB_TOKEN")
        }
    }
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:$mainMinecraftVersion-R0.1-SNAPSHOT")
    // implementation("github.xCykrix:spigotdevkit:1.1.2") {
    //     isTransitive = false
    // }
    implementation("co.aikar:acf-paper:0.5.1-SNAPSHOT")
}

// Shadow Task
tasks.named<ShadowJar>("shadowJar") {
    archiveClassifier = null;
}

publishing {
    publications.create<MavenPublication>("maven") {
        from(components["java"])
    }
}

tasks {
    assemble {
        dependsOn(shadowJar)
    }
    processResources {
        val props = mapOf(
            "name" to project.name,
            "version" to project.version,
            "description" to project.description,
            "apiVersion" to "1.21.11",
            "group" to project.group
        )
        inputs.properties(props)
        filesMatching("plugin.yml") {
            expand(props)
        }
    }
    runServer {
        // Configure the Minecraft version for our task.
        // This is the only required configuration besides applying the plugin.
        // Your plugin's jar (or shadowJar if present) will be used automatically.
        minecraftVersion(mainMinecraftVersion)
    }
    runPaper.folia.registerTask()
}

val targetJavaVersion = 21
java {
    val javaVersion = JavaVersion.toVersion(targetJavaVersion)
    sourceCompatibility = javaVersion
    targetCompatibility = javaVersion
    if (JavaVersion.current() < javaVersion) {
        toolchain.languageVersion = JavaLanguageVersion.of(targetJavaVersion)
    }
}

tasks.register("echoVersion") {
    group = "documentation"
    description = "Displays the version."
    doLast {
        println("${project.version}")
    }
}

tasks.register("echoReleaseName") {
    group = "documentation"
    description = "Displays the release name."
    doLast {
        println("${project.version} [${supportedMinecraftVersions}]")
    }
}

val extractChangelog = tasks.register("extractChangelog") {
    group = "documentation"
    description = "Extracts the changelog for the current project version from CHANGELOG.md, including the version header."

    val changelog: Property<String> = project.objects.property(String::class)
    outputs.upToDateWhen { false }

    doLast {
        val version = project.version.toString()
        val changelogFile = project.file("CHANGELOG.md")

        if (!changelogFile.exists()) {
            println("CHANGELOG.md not found.")
            changelog.set("No changelog found.")
            return@doLast
        }

        val lines = changelogFile.readLines()
        val entries = mutableListOf<String>()
        var foundVersion = false

        for (line in lines) {
            when {
                // Include the version line itself
                line.trim().equals("# $version", ignoreCase = true) -> {
                    foundVersion = true
                    entries.add(line)
                }
                // Stop collecting at the next version header
                foundVersion && line.trim().startsWith("# ") -> break
                // Collect lines after the version header
                foundVersion -> entries.add(line)
            }
        }

        val result = if (entries.isEmpty()) {
            "Update to $version."
        } else {
            entries.joinToString("\n").trim()
        }

        // println("Changelog for version $version:\n$result")
        changelog.set(result)
    }

    // Make changelog accessible from other tasks
    extensions.add(Property::class.java, "changelog", changelog)
}

tasks.register("echoLatestVersionChangelog") {
    group = "documentation"
    description = "Displays the latest version change."

    dependsOn(tasks.named("extractChangelog"))

    doLast {
        println((extractChangelog.get().extensions.findByType(Property::class.java) as Property<String>).get())
    }
}


val versionString: String = version as String
val isRelease: Boolean = !versionString.contains("SNAPSHOT")
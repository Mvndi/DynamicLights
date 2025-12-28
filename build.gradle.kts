import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    id("io.github.goooler.shadow") version "8.1.8"
    id("java")
    id("java-library")
    id("xyz.jpenilla.run-paper") version "2.3.1"
}

group = "github.xCykrix"
version = "1.3.4"
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

tasks {
    assemble {
        dependsOn(shadowJar)
    }
    runServer {
        // Configure the Minecraft version for our task.
        // This is the only required configuration besides applying the plugin.
        // Your plugin's jar (or shadowJar if present) will be used automatically.
        minecraftVersion(mainMinecraftVersion)
    }
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

plugins {
    id("io.papermc.paperweight.userdev") version "2.0.0-beta.19"
    id("com.gradleup.shadow") version "9.0.1"
}

repositories {
    mavenCentral()
    mavenLocal()
    maven("https://mvn.lumine.io/repository/maven-public/")
}

dependencies {
    implementation(project(":framework-core"))

    compileOnly("io.nexstudios.commandservice:bukkit:v1.0.0")

    // MythicMobs API
    compileOnly("io.lumine:Mythic-Dist:5.11.2")

    paperweight.paperDevBundle(providers.gradleProperty("paperVersion").get())
}

tasks.named("shadowJar") {
    (this as org.gradle.jvm.tasks.Jar).archiveClassifier.set("")
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}

tasks.build {
    dependsOn(tasks.named("shadowJar"))
}
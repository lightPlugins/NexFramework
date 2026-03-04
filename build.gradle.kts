import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.tasks.testing.Test

plugins {
    // Root stays empty: multi-module library repo
}

allprojects {
    group = "io.nexstudios"
    version = providers.gradleProperty("frameworkVersion").get()

    repositories {
        mavenCentral()
        maven("https://jitpack.io")
        maven("https://repo.papermc.io/repository/maven-public/")
        maven("https://repo.velocitypowered.com/snapshots/")
    }
}

subprojects {
    apply(plugin = "java-library")
    apply(plugin = "maven-publish")

    extensions.configure<JavaPluginExtension> {
        toolchain {
            languageVersion.set(JavaLanguageVersion.of(21))
        }
        withSourcesJar()
        withJavadocJar()
    }

    dependencies {
        val lombokVersion = providers.gradleProperty("lombokVersion").get()

        add("compileOnly", "org.projectlombok:lombok:$lombokVersion")
        add("annotationProcessor", "org.projectlombok:lombok:$lombokVersion")

        add("testCompileOnly", "org.projectlombok:lombok:$lombokVersion")
        add("testAnnotationProcessor", "org.projectlombok:lombok:$lombokVersion")
    }

    tasks.withType(Test::class.java).configureEach {
        useJUnitPlatform()
    }

    extensions.configure<PublishingExtension> {
        publications {
            create("mavenJava", MavenPublication::class.java) {
                from(components.getByName("java"))
            }
        }
    }

    pluginManager.withPlugin("com.gradleup.shadow") {
        if (project.name == "framework-paper" || project.name == "framework-velocity") {

            // 1) Für Endpoints: Gradle Module Metadata (.module) abschalten, weil wir Artefakte ersetzen
            tasks.withType(GenerateModuleMetadata::class.java).configureEach {
                enabled = false
            }

            // 2) ShadowJar als publiziertes Haupt-Artefakt verwenden
            extensions.configure<PublishingExtension> {
                publications.named("mavenJava", MavenPublication::class.java).configure {
                    artifacts.clear()
                    artifact(tasks.named("shadowJar"))
                }
            }
        }
    }
}
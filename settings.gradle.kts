pluginManagement {
    repositories {
        gradlePluginPortal()
        maven("https://repo.papermc.io/repository/maven-public/")
        mavenCentral()
    }
}

rootProject.name = "NexFramework"

include(
    "framework-core",
    "framework-paper",
    "framework-velocity",
)
include("framework-config")
plugins {
    id("io.papermc.paperweight.userdev") version "2.0.0-beta.19"
}

dependencies {
    api(project(":framework-core"))

    paperweight.paperDevBundle(providers.gradleProperty("paperVersion").get())
}
dependencies {
    api(project(":framework-core"))

    compileOnly("com.velocitypowered:velocity-api:${providers.gradleProperty("velocityVersion").get()}")
    annotationProcessor("com.velocitypowered:velocity-api:${providers.gradleProperty("velocityVersion").get()}")
}
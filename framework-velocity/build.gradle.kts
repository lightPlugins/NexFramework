plugins {
    id("com.gradleup.shadow") version "9.0.1"
}

dependencies {
    implementation(project(":framework-core"))
    implementation(project(":framework-config"))

    compileOnly("com.velocitypowered:velocity-api:${providers.gradleProperty("velocityVersion").get()}")
    annotationProcessor("com.velocitypowered:velocity-api:${providers.gradleProperty("velocityVersion").get()}")
}

tasks.named("shadowJar") {
    (this as org.gradle.jvm.tasks.Jar).archiveClassifier.set("")
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}

tasks.build {
    dependsOn(tasks.named("shadowJar"))
}
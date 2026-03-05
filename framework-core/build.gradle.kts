repositories {
    mavenCentral()
    maven("https://jitpack.io")
}

dependencies {
    api("com.github.lightplugins:NexServiceRegistry:${providers.gradleProperty("registryVersion").get()}")
    compileOnly("jakarta.persistence:jakarta.persistence-api:3.1.0")

    compileOnly("net.kyori:adventure-api:${providers.gradleProperty("adventureVersion").get()}")
    compileOnly("net.kyori:adventure-text-minimessage:${providers.gradleProperty("adventureVersion").get()}")

    testImplementation(platform("org.junit:junit-bom:5.12.1"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.test {
    useJUnitPlatform()
}
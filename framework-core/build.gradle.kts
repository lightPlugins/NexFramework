repositories {
    mavenCentral()
}

dependencies {
    api("com.github.lightplugins:NexServiceRegistry:${providers.gradleProperty("registryVersion").get()}")

    testImplementation(platform("org.junit:junit-bom:5.12.1"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.test {
    useJUnitPlatform()
}
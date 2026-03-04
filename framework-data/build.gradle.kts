plugins {
    id("java")
}

group = "io.nexstudios.framework.data"
version = "v1.0.1"

repositories {
    mavenCentral()
}

dependencies {

    api(project(":framework-core"))
    api(project(":framework-config"))

    compileOnly("com.zaxxer:HikariCP:5.1.0")
    compileOnly("org.hibernate.orm:hibernate-core:6.6.13.Final")
    compileOnly("org.mariadb.jdbc:mariadb-java-client:3.5.3")

    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.test {
    useJUnitPlatform()
}
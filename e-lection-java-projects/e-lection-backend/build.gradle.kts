plugins {
    java
    jacoco
    checkstyle
    id("org.springframework.boot") version "3.0.2"
    id("io.spring.dependency-management") version "1.1.0"
}

group = "pse.e-lection"
version = "0.0.1-SNAPSHOT"
java.sourceCompatibility = JavaVersion.VERSION_17

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-oauth2-client")
    implementation("org.springframework.boot:spring-boot-starter-oauth2-resource-server")
    implementation("org.springframework.boot:spring-boot-starter-security")

    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("com.h2database:h2")

    implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-toml:2.14.2")
    implementation("com.auth0:java-jwt:4.3.0")

    implementation(files("../libs/core-0.9.5-SNAPSHOT-all.jar"))

    runtimeOnly("mysql:mysql-connector-java")

    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.8.1")
    testImplementation("junit:junit:4.13.1")
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.8.1")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.security:spring-security-test:6.0.2")
    testImplementation(project(":e-lection-cli"))
}

project.setProperty("testResultsDirName", "$buildDir/junit-xml")

tasks.withType<Test> {
    useJUnitPlatform()
    finalizedBy(tasks.jacocoTestReport)
}

tasks.jacocoTestReport {
    dependsOn(tasks.test)
    reports {
        xml.required.set(true)
        csv.required.set(false)
    }
}

tasks.jacocoTestReport {
    dependsOn(tasks.test)
}

checkstyle {
    toolVersion = "10.8.0"
    maxWarnings = 0
}
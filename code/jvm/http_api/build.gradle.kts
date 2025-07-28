plugins {
    kotlin("jvm")
    //id("com.adarshr.test-logger")
    //id("org.jlleitschuh.gradle.ktlint") version "12.1.1"
}

group = "pt.isel"
version = "0.1.0.SNAPSHOT"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(project(":service"))

    // To use Spring MVC
    implementation("org.springframework:spring-webmvc:6.1.13")

    // To use SLF4J
    implementation("org.slf4j:slf4j-api:2.0.16")

    // To use Kotlin specific date and time functions
    implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.4.1")

    implementation("jakarta.servlet:jakarta.servlet-api:6.0.0")

    // for JDBI and Postgres Tests
    testImplementation(project(":repository-jdbi"))
    testImplementation("org.jdbi:jdbi3-core:3.37.1")
    testImplementation("org.postgresql:postgresql:42.7.2")

    // To use WebTestClient on tests
    testImplementation(kotlin("test"))
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.10.0")
    testImplementation("org.junit.jupiter:junit-jupiter-params:5.10.0")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.10.0")
}

kotlin {
    compilerOptions {
        freeCompilerArgs.addAll("-Xjsr305=strict")
    }
}


tasks.withType<Test> {
    useJUnitPlatform()
    ignoreFailures = true
    environment("DB_URL", "jdbc:postgresql://localhost:5432/db?user=postgres&password=changeit")
    dependsOn(":repository-jdbi:dbTestsWait")
    finalizedBy(":repository-jdbi:dbTestsDown")
}

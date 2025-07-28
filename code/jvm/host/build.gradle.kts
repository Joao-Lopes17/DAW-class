plugins {
    kotlin("jvm")
    kotlin("plugin.spring")
    id("org.springframework.boot")
    id("io.spring.dependency-management")
}

group = "pt.isel"
version = "0.1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    // Module dependencies
    implementation(project(":domain"))
    implementation(project(":http_api"))
    implementation(project(":repository-jdbi"))
    implementation(project(":repository"))
    implementation(project(":http-pipeline"))
    //implementation(project(":SSE"))

    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.4.1")

    // for JDBI and Postgres
    implementation("org.jdbi:jdbi3-core:3.37.1")
    implementation("org.postgresql:postgresql:42.7.2")


    testImplementation(kotlin("test"))
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.boot:spring-boot-starter-webflux")
}

tasks.bootRun {
    environment("DB_URL", "jdbc:postgresql://localhost:5432/db?user=postgres&password=changeit")
}

tasks.withType<Test> {
    useJUnitPlatform()
    ignoreFailures = true
    environment("DB_URL", "jdbc:postgresql://localhost:5432/db?user=postgres&password=changeit")
    dependsOn(":repository-jdbi:dbTestsWait")
    finalizedBy(":repository-jdbi:dbTestsDown")
}
kotlin {
    jvmToolchain(21)
}

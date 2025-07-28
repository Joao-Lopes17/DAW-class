plugins {
    kotlin("jvm")
   // id("org.jlleitschuh.gradle.ktlint") version "12.1.1"
}

group = "pt.isel"
version = "0.1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation(project(":domain"))

    implementation("jakarta.inject:jakarta.inject-api:2.0.1")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.4.1")

    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()

}
kotlin {
    jvmToolchain(21)
}

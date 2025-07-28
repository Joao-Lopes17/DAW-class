plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.8.0"
    kotlin("jvm") version "1.9.25" apply false
    kotlin("plugin.spring") version "1.9.25" apply false
    id("org.springframework.boot") version "3.3.3" apply false
    id("io.spring.dependency-management") version "1.1.6" apply false
}
rootProject.name = "2024-daw-leic53d-g11-53d"

include("service")
include("domain")
include("http_api")
include("repository")
include("repository-jdbi")
include("host")
include("http-pipeline")

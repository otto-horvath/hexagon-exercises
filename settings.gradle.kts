plugins {
    // Allow Gradle to download the Java toolchain automatically
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

rootProject.name = "hexagon-exercises"
include("app")

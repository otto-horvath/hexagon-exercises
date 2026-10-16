plugins {
    java
    id("com.diffplug.spotless") version "8.10.4"
}

repositories {
    mavenCentral()
}

dependencies {
    compileOnly(libs.jspecify)

    testImplementation(libs.junit.jupiter)
    testImplementation(libs.assertj.core)
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(25)
    }
}

tasks.named<Test>("test") {
    useJUnitPlatform()

    // Remo the chameleon reports the whole run: red while anything fails, green when all pass.
    afterSuite(KotlinClosure2<TestDescriptor, TestResult, Unit>({ suite, result ->
        if (suite.parent == null) {
            val total = result.testCount
            val failed = result.failedTestCount
            val skipped = result.skippedTestCount
            val message = if (failed == 0L) {
                "\u001B[32m🦎 Remo is green: ${total - skipped} of $total tests passed, $skipped skipped.\u001B[0m"
            } else {
                "\u001B[31m🦎 Remo turned red: $failed of $total tests failed. Fix one thing, then rerun.\u001B[0m"
            }
            println(message)
        }
    }))
}

spotless {
    java {
        palantirJavaFormat()
        removeUnusedImports()
        trimTrailingWhitespace()
        endWithNewline()
    }
}

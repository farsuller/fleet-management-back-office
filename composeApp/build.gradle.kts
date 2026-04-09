import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.ksp)
    id("jacoco")
    alias(libs.plugins.detekt)
}

// Detekt Static Analysis Configuration
// Scans the codebase for code smells, complexity issues, and architectural violations.
detekt {
    toolVersion = libs.versions.detekt.get()
    source.setFrom(files("src/commonMain/kotlin", "src/wasmJsMain/kotlin"))
    config.setFrom(files("${project.rootDir}/config/detekt/detekt.yml"))
    buildUponDefaultConfig = true
}

// Detekt Report Output Configuration
tasks.withType<io.gitlab.arturbosch.detekt.Detekt>().configureEach {
    reports {
        html.required.set(true)
        xml.required.set(true)
        txt.required.set(false)
        sarif.required.set(true)
    }
}

jacoco {
    toolVersion = "0.8.12"
}

// JVM Coverage Reporting Task
// Generates HTML/XML reports from the execution data produced by jvmTest.
tasks.register<JacocoReport>("jacocoJvmTestReport") {
    group = "Reporting"
    description = "Generate Jacoco coverage reports for JVM unit tests."

    dependsOn("jvmTest")

    // Ensure the console summary table is printed even if the task is technically Up-To-Date.
    outputs.upToDateWhen { false }

    reports {
        xml.required.set(true)
        html.required.set(true)
    }

    // Exclude UI, DI, and platform-specific generated code from coverage metrics
    val excludes =
        listOf(
            "**/features/**",
            "**/ui/**",
            "**/navigation/**",
            "**/components/**",
            "**/App*",
            "**/Platform*",
            "**/Greeting*",
            "**/BuildConfig*",
            "**/di/**",
            "**/*ViewModel*",
            "**/*Composable*",
        )

    classDirectories.setFrom(
        fileTree(layout.buildDirectory.dir("classes/kotlin/jvm/main")) { exclude(excludes) },
        fileTree(layout.buildDirectory.dir("classes/kotlin/metadata/main")) { exclude(excludes) },
    )

    sourceDirectories.setFrom(files("src/commonMain/kotlin", "src/jvmMain/kotlin"))
    executionData.setFrom(layout.buildDirectory.file("jacoco/jvmTest.exec"))

    // Custom Console Reporter
    // Parses the resulting XML and prints a formatted summary table to the terminal,
    // providing immediate visibility into test coverage levels for each class.
    doLast {
        val xmlFile =
            reports.xml.outputLocation
                .get()
                .asFile
        if (xmlFile.exists()) {
            logger.quiet("\n--- Code Coverage Summary (JVM) ---")
            logger.quiet(String.format("%-60s | %-10s", "Class", "Coverage"))
            logger.quiet("-".repeat(75))

            try {
                val xml = xmlFile.readText()

                // Helper to extract missed and covered attributes
                fun extract(
                    counterBlock: String,
                    attr: String,
                ): Double {
                    val search = "$attr=\""
                    if (!counterBlock.contains(search)) return 0.0
                    return counterBlock.substringAfter(search).substringBefore("\"").toDoubleOrNull() ?: 0.0
                }

                // Robust parsing: Split XML into class blocks and iterate
                xml.split("<class ").drop(1).forEach { classBlock ->
                    val name = classBlock.substringAfter("name=\"").substringBefore("\"").replace("/", ".")

                    // Extract aggregate class-level counters (ignoring method-level counters if present)
                    val lastCounters =
                        classBlock
                            .substringAfterLast("</method>")
                            .ifEmpty { classBlock.substringAfter(">") }
                            .substringBefore("</class>")

                    // Prioritize LINE coverage, fallback to INSTRUCTION
                    val lineBlock = lastCounters.substringAfter("type=\"LINE\"", "")
                    val instrBlock = lastCounters.substringAfter("type=\"INSTRUCTION\"", "")

                    val activeBlock = if (lineBlock.isNotEmpty()) lineBlock else instrBlock

                    if (activeBlock.isNotEmpty()) {
                        val missed = extract(activeBlock, "missed")
                        val covered = extract(activeBlock, "covered")
                        val total = missed + covered
                        val coverage = if (total > 0) (covered / total) * 100 else 0.0
                        logger.quiet(String.format("%-60s | %6.2f%%", name, coverage))
                    }
                }

                // Final Summary Logic: Extract the aggregate counters for the entire report
                val reportCounters = xml.substringAfterLast("</package>").substringBefore("</report>")
                val totalLine = reportCounters.substringAfter("type=\"LINE\"", "")
                val totalInstr = reportCounters.substringAfter("type=\"INSTRUCTION\"", "")
                val finalBlock = if (totalLine.isNotEmpty()) totalLine else totalInstr

                if (finalBlock.isNotEmpty()) {
                    val missed = extract(finalBlock, "missed")
                    val covered = extract(finalBlock, "covered")
                    val total = missed + covered
                    val ratio = if (total > 0) (covered / total) * 100 else 0.0

                    logger.quiet("-".repeat(75))
                    logger.quiet(String.format("%-60s | %6.2f%%", "OVERALL PROJECT COVERAGE", ratio))
                    logger.quiet(String.format("%-60s | %6s%%", "EXPECTED MINIMUM TARGET", "40.00"))

                    if (ratio < 40.0) {
                        logger.quiet("\n[WARNING] Coverage is below the required 40% threshold. Build verification may fail.")
                    } else {
                        logger.quiet("\n[SUCCESS] Quality gate passed: Coverage is within expected limits.")
                    }
                }
            } catch (e: Exception) {
                logger.quiet("Note: Detailed coverage overview truncated. Full report: ${reports.html.outputLocation.get()}")
            }
            logger.quiet("-".repeat(75))
        }
    }
}

// JVM Coverage Verification Task
// Enforces a minimum coverage threshold for the business logic layer.
// This task will fail the build in CI if coverage drops below the required percentage.
tasks.register<JacocoCoverageVerification>("jacocoJvmTestCoverageVerification") {
    group = "Verification"
    description = "Verify Jacoco coverage for JVM unit tests."

    dependsOn("jvmTest")

    violationRules {
        rule {
            limit {
                minimum = "0.4".toBigDecimal()
            }
        }
    }

    val excludes =
        listOf(
            "**/features/**",
            "**/ui/**",
            "**/navigation/**",
            "**/components/**",
            "**/App*",
            "**/Platform*",
            "**/Greeting*",
            "**/BuildConfig*",
            "**/di/**",
            "**/*ViewModel*",
            "**/*Composable*",
        )

    classDirectories.setFrom(
        fileTree(layout.buildDirectory.dir("classes/kotlin/jvm/main")) { exclude(excludes) },
        fileTree(layout.buildDirectory.dir("classes/kotlin/metadata/main")) { exclude(excludes) },
    )

    sourceDirectories.setFrom(files("src/commonMain/kotlin", "src/jvmMain/kotlin"))
    executionData.setFrom(layout.buildDirectory.file("jacoco/jvmTest.exec"))
}

kotlin {
    // Shared Compiler Options
    // -Xexpect-actual-classes: Enables the stable usage of expect/actual naming without Beta warnings.
    compilerOptions {
        freeCompilerArgs.addAll("-Xexpect-actual-classes")
    }

    // Web/Wasm Target Configuration
    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        compilerOptions {
            // Enable JavaScript interop features required for some Compose/Web integrations.
            freeCompilerArgs.add("-opt-in=kotlin.js.ExperimentalWasmJsInterop")
        }
        browser()
        binaries.executable()
    }
    // Desktop/JVM Target Configuration
    jvm()

    sourceSets {
        commonMain.dependencies {
            implementation(libs.compose.runtime)
            implementation(libs.compose.foundation)
            implementation(libs.compose.material3)
            implementation(libs.compose.ui)
            implementation(libs.compose.components.resources)
            implementation(libs.compose.uiToolingPreview)
            implementation(libs.androidx.lifecycle.viewmodelCompose)
            implementation(libs.androidx.lifecycle.runtimeCompose)
            implementation(libs.androidx.lifecycle.viewmodel)
            @Suppress("DEPRECATION")
            implementation(compose.materialIconsExtended)
            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.content.negotiation)
            implementation(libs.ktor.serialization.kotlinx.json)
            implementation(libs.ktor.client.websockets)
            // Coil
            implementation(libs.coil.compose)
            implementation(libs.coil.network.ktor)
            // Koin
            implementation(libs.koin.core)
            implementation(libs.koin.compose)
            implementation(libs.koin.compose.viewmodel)
            // kotlinx
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.kotlinx.datetime)
            implementation(libs.charty)
            implementation(libs.ksafe)
        }
        val wasmJsMain by getting {
            dependencies { implementation(libs.ktor.client.js) }
        }
        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
                implementation("org.jetbrains.kotlin:kotlin-test-common")
                implementation("org.jetbrains.kotlin:kotlin-test-annotations-common")
                implementation(libs.kotlin.test)
                implementation(libs.ktor.client.mock)
                implementation(libs.koin.test)
                implementation(libs.kotlinx.coroutines.test)
                implementation(libs.kotlinx.datetime)
            }
        }
        val jvmTest by getting {
            dependencies {
                implementation(kotlin("test-junit"))
            }
        }
    }
}

tasks.withType<Test>().configureEach {
    val failedClasses = mutableSetOf<String>()
    val currentSuite = StringBuilder()

    // Hierarchical Test Logging
    // Configures standard streams and custom hooks to provide a clear,
    // tree-like visualization of test execution (Classes > Methods > Status).
    testLogging {
        events("passed", "skipped", "failed")
        showStandardStreams = true
        exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
    }

    // Test Execution Hook: Ran after every individual test function
    afterTest(
        KotlinClosure2({ desc: TestDescriptor, result: TestResult ->
            val className = desc.className?.substringAfterLast('.') ?: "Unknown"
            // Update current suite if it changes to provide a grouping effect in logs
            if (currentSuite.toString() != className) {
                currentSuite.clear().append(className)
                println("\n> Executing: $className")
            }

            // Print function name and its success/failure status
            val status = result.resultType.toString()
            println("  $status: ${desc.name}")

            if (result.resultType == TestResult.ResultType.FAILURE) {
                failedClasses += className
                result.exception?.let { println("         Error: ${it.message}") }
            }
        }),
    )

    // Suite Completion Hook: Ran after all tests in a task are finished
    afterSuite(
        KotlinClosure2({ desc: TestDescriptor, result: TestResult ->
            // Only execute for the root suite (the overall task)
            if (desc.parent == null) {
                val pass = result.successfulTestCount
                val fail = result.failedTestCount
                val skip = result.skippedTestCount
                val total = result.testCount
                val outcome = if (result.resultType == TestResult.ResultType.SUCCESS) "PASSED" else "FAILED"

                println("\n-----------------------------------------------------------------------")
                if (failedClasses.isNotEmpty()) {
                    println("Failed files: ${failedClasses.sorted().joinToString(", ")}")
                }
                println("Final Summary: $outcome")
                println("Total: $total  |  Passed: $pass  |  Failed: $fail  |  Skipped: $skip")
                println("-----------------------------------------------------------------------")
            }
        }),
    )
}

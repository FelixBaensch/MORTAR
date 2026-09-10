import com.diffplug.spotless.FormatterFunc
import org.gradle.api.tasks.application.CreateStartScripts
import java.io.Serializable

plugins {
    id("application")
    alias(libs.plugins.javafxplugin)
    alias(libs.plugins.spotless)
    id("jacoco")
    // Mutation testing (report-only / advisory — NOT wired into check/build).
    // Run explicitly via `./gradlew pitest`. See the report-only `pitest {}` block below.
    alias(libs.plugins.pitest)
    id("mortar.deploy.linux")
    id("mortar.deploy.mac")
    id("mortar.deploy.win")
    id("mortar.start.scripts")
}

group = providers.gradleProperty("appGroup").get()
version = providers.gradleProperty("appVersion").get()

// Creates javadoc and sources jars
java {
    sourceCompatibility = JavaVersion.VERSION_21
    withJavadocJar()
    withSourcesJar()
}

repositories {
    mavenCentral()
    // CDK SNAPSHOT repository
    maven { url = uri("https://central.sonatype.com/repository/maven-snapshots/") }
    // GitLab Maven repository for MolWURCS
    maven { url = uri("https://gitlab.com/api/v4/projects/20390948/packages/maven") }
    // GitLab Maven repository for WURCSFramework
    maven { url = uri("https://gitlab.com/api/v4/projects/17725126/packages/maven") }
}

dependencies {
    testImplementation(platform(libs.junit))
    testImplementation(libs.jupiter)
    testImplementation(libs.mockitoCore)
    testImplementation(libs.testfxCore)
    testImplementation(libs.testfxJunit5)
    testImplementation(libs.openjfxMonocle)
    // Teaches PIT to discover/run JUnit 5 (Jupiter) tests via the JUnit Platform.
    pitest(libs.pitestJunit5)
    //<editor-fold desc="CDK dependencies">
    implementation(libs.cdkPdb)
    implementation(libs.cdkSilent)
    implementation(libs.cdkExtra)
    implementation(libs.cdkDepict)
    implementation(libs.cdkMisc)
    implementation(libs.cdkCore)
    implementation(libs.cdkHash)
    implementation(libs.cdkInterfaces)
    implementation(libs.cdkIoformats)
    implementation(libs.cdkSmiles)
    implementation(libs.cdkInchi)
    implementation(libs.cdkStandard)
    implementation(libs.cdkValencycheck)
    implementation(libs.cdkFragment)
    implementation(libs.cdkScaffold)
    //</editor-fold>
    implementation(libs.molWURCS)
    implementation(libs.openpdf)
    //<editor-fold desc="JavaFX dependencies">
    implementation(variantOf(libs.javafxControls) { classifier("win") })
    implementation(variantOf(libs.javafxControls) { classifier("linux") })
    implementation(variantOf(libs.javafxControls) { classifier("mac") })
    implementation(variantOf(libs.javafxControls) { classifier("linux-aarch64") })
    implementation(variantOf(libs.javafxControls) { classifier("mac-aarch64") })
    implementation(variantOf(libs.javafxSwing) { classifier("win") })
    implementation(variantOf(libs.javafxSwing) { classifier("linux") })
    implementation(variantOf(libs.javafxSwing) { classifier("mac") })
    implementation(variantOf(libs.javafxSwing) { classifier("linux-aarch64") })
    implementation(variantOf(libs.javafxSwing) { classifier("mac-aarch64") })
    implementation(variantOf(libs.javafxGraphics) { classifier("win") })
    implementation(variantOf(libs.javafxGraphics) { classifier("linux") })
    implementation(variantOf(libs.javafxGraphics) { classifier("mac") })
    implementation(variantOf(libs.javafxGraphics) { classifier("linux-aarch64") })
    implementation(variantOf(libs.javafxGraphics) { classifier("mac-aarch64") })
    implementation(variantOf(libs.javafxBase) { classifier("win") })
    implementation(variantOf(libs.javafxBase) { classifier("linux") })
    implementation(variantOf(libs.javafxBase) { classifier("mac") })
    implementation(variantOf(libs.javafxBase) { classifier("linux-aarch64") })
    implementation(variantOf(libs.javafxBase) { classifier("mac-aarch64") })
    //</editor-fold>
}

javafx {
    modules = listOf("javafx.base","javafx.graphics", "javafx.controls", "javafx.swing")
}

// Every JavaFX platform classifier is on the runtime classpath above so the fat JAR runs cross-platform. That is
// wrong for a test JVM: JavaFX extracts its native libraries by bare resource name (e.g. "/libprism_sw.dylib"), so
// whichever classifier comes first on the classpath wins, regardless of the host architecture. On Apple Silicon the
// -mac (x86_64) jar precedes -mac-aarch64 and the extracted dylib fails to dlopen with "incompatible architecture",
// which brings down the whole toolkit ("No toolkit found"). Linux x64 and Windows only escape this by accident of
// ordering and of having a single classifier. The test classpath therefore keeps the host classifier alone.
val javafxPlatformClassifiers = listOf("win", "linux", "linux-aarch64", "mac", "mac-aarch64")

val javafxHostClassifier = run {
    val tmpOsName = System.getProperty("os.name").lowercase()
    val tmpArch = System.getProperty("os.arch").lowercase()
    val tmpIsAarch64 = tmpArch == "aarch64" || tmpArch == "arm64"
    when {
        tmpOsName.contains("win") -> "win"
        tmpOsName.contains("mac") || tmpOsName.contains("darwin") -> if (tmpIsAarch64) "mac-aarch64" else "mac"
        else -> if (tmpIsAarch64) "linux-aarch64" else "linux"
    }
}

val javafxForeignClassifiers = javafxPlatformClassifiers - javafxHostClassifier

jacoco {
    toolVersion = libs.versions.jacoco.get()
}

tasks.test {
    useJUnitPlatform()
    // Drop the foreign-platform JavaFX artifacts (see javafxHostClassifier above); the host's must be the only
    // source of a given native library name. Captured into a local first: the filter predicate is evaluated at
    // execution time and must not close over the build script instance, which the configuration cache does not
    // restore.
    val tmpForeignClassifiers = javafxForeignClassifiers
    classpath = classpath.filter { tmpFile ->
        !(tmpFile.name.startsWith("javafx-")
                && tmpForeignClassifiers.any { tmpClassifier -> tmpFile.name.endsWith("-$tmpClassifier.jar") })
    }
    systemProperty("java.awt.headless", "true")
    // --- headless JavaFX (TestFX + Monocle) recipe (HARN-02) ---
    // Set BEFORE any FX class loads so Monocle registers its headless platform.
    systemProperty("testfx.robot", "glass")
    systemProperty("testfx.headless", "true")
    systemProperty("glass.platform", "Monocle")
    systemProperty("monocle.platform", "Headless")
    // Monocle's headless screen defaults to 1280x800. A stage sized or resized beyond the virtual screen makes the
    // software pipeline's UploadingPainter copy a stage-sized texture into a screen-sized pixel buffer, which throws
    // BufferOverflowException on the FX render thread: printed to stderr, not propagated, so tests still pass while
    // the output fills with stack traces. Give the virtual screen enough headroom that no view can outgrow it.
    systemProperty("headless.geometry", "1920x1200-32")
    // Defaults; overridable per run by the forwarding block below (e.g. -Dprism.order=es2 to pass a real GPU
    // pipeline on local Mac/Windows dev): do NOT hardcode "sw" unconditionally.
    systemProperty("prism.order", "sw")
    systemProperty("prism.text", "t2k")
    // Gradle's -D options set a property on the *build* JVM, which the forked test JVM does not inherit. Forward
    // the FX-relevant ones explicitly so that diagnosing the headless setup works as expected, e.g.
    //     ./gradlew test --rerun-tasks -Dprism.verbose=true      (quote the -D on Windows PowerShell)
    // Anything passed on the command line overrides the defaults set above.
    for (tmpName in System.getProperties().stringPropertyNames().sorted()) {
        if (tmpName.startsWith("prism.") || tmpName.startsWith("glass.")
                || tmpName.startsWith("monocle.") || tmpName.startsWith("testfx.")
                || tmpName.startsWith("headless.")) {
            systemProperty(tmpName, System.getProperty(tmpName))
        }
    }
    // JavaFX unpacks its native libraries into ${user.home}/.openjfx/cache and keeps them loaded. The FX tests
    // redirect user.home to a per-test temporary directory, and Windows cannot delete a loaded DLL, so the
    // @TempDir cleanup fails there. Pin the cache to a stable build directory instead of a temporary home.
    systemProperty("javafx.cachedir", layout.buildDirectory.dir("javafx-cache").get().asFile.absolutePath)
    // Empirical fallback module args — uncomment ONLY if the plan-02 smoke test
    // throws InaccessibleObjectException / IllegalAccessError naming
    // com.sun.glass.ui (resolved empirically in task 13-02-01). Add the
    // com.sun.glass.utils / com.sun.prism variants only if a further access
    // error names them.
    // jvmArgs(
    //     "--add-opens=javafx.graphics/com.sun.glass.ui=ALL-UNNAMED",
    //     "--add-exports=javafx.graphics/com.sun.glass.ui=ALL-UNNAMED"
    // )
    testLogging {
        events = setOf(
            org.gradle.api.tasks.testing.logging.TestLogEvent.PASSED,
            org.gradle.api.tasks.testing.logging.TestLogEvent.SKIPPED,
            org.gradle.api.tasks.testing.logging.TestLogEvent.FAILED
        )
    }
    finalizedBy(tasks.jacocoTestReport)
}

// Shared measured-scope exclusions so the report and the coverage-verification
// gate can never drift apart (gui/main/message are intentionally out of scope).
val jacocoMeasuredScopeExcludes = listOf(
    "**/de/unijena/cheminf/mortar/gui/**",
    "**/de/unijena/cheminf/mortar/main/**",
    "**/de/unijena/cheminf/mortar/message/**"
)

// Builds the measured class-directory set once, applying the shared exclusions.
fun jacocoMeasuredClassDirectories() = files(
    sourceSets.main.get().output.classesDirs.files.map { tmpDir ->
        fileTree(tmpDir) {
            exclude(jacocoMeasuredScopeExcludes)
        }
    }
)

tasks.jacocoTestReport {
    dependsOn(tasks.test)
    reports {
        html.required.set(true)
        xml.required.set(true)
        csv.required.set(false)
    }
    classDirectories.setFrom(jacocoMeasuredClassDirectories())
}

// GATE-01 / GATE-02: per-package LINE coverage regression gate. Run explicitly (see the note below the task):
// it is not part of `check`/`build`. Measures the exact same scope as jacocoTestReport (gui/main/message excluded).
tasks.jacocoTestCoverageVerification {
    dependsOn(tasks.test)
    classDirectories.setFrom(jacocoMeasuredClassDirectories())
    violationRules {
        // Rule A — core packages (model.*, preference, configuration): minimum 85% LINE.
        // element = PACKAGE evaluates each matching package independently → genuine
        // per-package enforcement. The `model.*` wildcard matches every model sub-package.
        rule {
            element = "PACKAGE"
            includes = listOf(
                "de.unijena.cheminf.mortar.model.*",
                "de.unijena.cheminf.mortar.preference",
                "de.unijena.cheminf.mortar.configuration"
            )
            limit {
                counter = "LINE"
                value = "COVEREDRATIO"
                minimum = "0.85".toBigDecimal()
            }
        }
        // Rule B — controller package: minimum 80% LINE.
        rule {
            element = "PACKAGE"
            includes = listOf("de.unijena.cheminf.mortar.controller")
            limit {
                counter = "LINE"
                value = "COVEREDRATIO"
                minimum = "0.80".toBigDecimal()
            }
        }
    }
}

// GATE-01: the coverage gate is deliberately NOT wired into `check`/`build`. Per-package minimums are only
// reachable on a platform where the whole suite runs: a handful of tests are skipped on Windows because the
// branches they drive (POSIX read-only directories, the non-Windows app-dir resolution) do not exist there, so
// the same source would fail the gate on Windows while passing on Linux. Failing every local `./gradlew build`
// on a platform artefact is not a useful regression signal. CI runs it explicitly on ubuntu-latest
// (`./gradlew build jacocoTestCoverageVerification` in .github/workflows/gradle.yml); run it locally the same way.

// Mutation testing (PITest) — REPORT-ONLY / ADVISORY.
// Deliberately NOT wired into `check`/`build`: run it explicitly with `./gradlew pitest`.
// It measures whether high JaCoCo line coverage is backed by tests that actually catch
// bugs (a surviving mutant on a covered line = "covered but not asserted"). No
// mutationThreshold/coverageThreshold is set, so it can never fail the build.
// PIT runs its own test execution and coverage internally — it does NOT invoke `test`,
// `jacocoTestReport`, or the coverage gate, so it is fully independent of them.
//
// SCOPE (now includes model.fragmentation): the deterministic core — model.util/data/io,
// preference, configuration — plus the CDK/algorithm-heavy fragmentation package
// (model.fragmentation.*, added in the second pass). The remaining CDK-heavy packages
// model.depict and model.settings are still DEFERRED to a later pass (PIT re-runs the
// covering tests per mutant, so runtime is the dominant constraint). FX/controller and
// full-pipeline integration tests are excluded (mutation-hostile + slow).
pitest {
    // Engine versions (pinned for reproducibility; 1.22.1 is already the plugin default).
    pitestVersion.set("1.22.1")
    junit5PluginVersion.set("1.2.3")

    // SCOPE: deterministic non-GUI core + fragmentation algorithms (second pass).
    targetClasses.set(listOf(
        "de.unijena.cheminf.mortar.model.util.*",
        "de.unijena.cheminf.mortar.model.data.*",
        "de.unijena.cheminf.mortar.model.io.*",
        "de.unijena.cheminf.mortar.model.fragmentation.*",
        "de.unijena.cheminf.mortar.preference.*",
        "de.unijena.cheminf.mortar.configuration.*"
        // DEFERRED to a later pass (slower / CDK-heavy):
        // "de.unijena.cheminf.mortar.model.depict.*",
        // "de.unijena.cheminf.mortar.model.settings.*"
    ))
    targetTests.set(listOf(
        "de.unijena.cheminf.mortar.model.util.*",
        "de.unijena.cheminf.mortar.model.data.*",
        "de.unijena.cheminf.mortar.model.io.*",
        "de.unijena.cheminf.mortar.model.fragmentation.*",
        "de.unijena.cheminf.mortar.preference.*",
        "de.unijena.cheminf.mortar.configuration.*"
    ))

    // Keep FX/controller + CDK integration tests out (belt-and-suspenders).
    excludedTestClasses.set(listOf(
        "de.unijena.cheminf.mortar.controller.*",
        "de.unijena.cheminf.mortar.integration.*"
    ))

    // Report-only: HTML for humans, XML for tooling/diffing. Stable path
    // build/reports/pitest (no timestamp subdir).
    outputFormats.set(listOf("HTML", "XML"))
    timestampedReports.set(false)

    // Tractability: bounded parallelism + generous timeouts so slow CDK/IO code is not
    // mis-scored as a spurious TIMED_OUT.
    threads.set(Runtime.getRuntime().availableProcessors().coerceAtMost(4))
    timeoutConstInMillis.set(10000)
    timeoutFactor.set(1.5.toBigDecimal())

    // JVM args for the mutant minion JVMs (they do NOT inherit tasks.test config).
    jvmArgs.set(listOf(
        "-Djava.awt.headless=true",
        "-XX:+EnableDynamicAgentLoading"  // silence Mockito-5 inline/ByteBuddy self-attach warning on JDK 21
    ))

    // No mutationThreshold / coverageThreshold => advisory only, never fails the build.
}

//<editor-fold desc="FatJar tasks">
/**
 * Creates a Gradle task to generate a fat JAR file with all runtime dependencies.
 *
 * @param aDescription a description of the task
 * @param anArchiveClassifier a classifier to append to the archive file name (e.g., "aarch64")
 * @param aManifestTitle a title to include in the JAR manifest
 * @param aJarFilter a filter function to determine which JAR files to include in the fat JAR
 * @return a `TaskProvider<Jar>` representing the created fatJAR task
 */
fun createFatJarTask(
    aDescription: String,
    anArchiveClassifier: String,
    aManifestTitle: String,
    aJarFilter: (File) -> Boolean
) = tasks.registering(Jar::class) {
    group = "build"
    description = aDescription
    val tmpAppVersion = providers.gradleProperty("appVersion").get()
    archiveFileName.set("${providers.gradleProperty("appName").get()}-fat${anArchiveClassifier}-${tmpAppVersion}.jar")

    manifest {
        attributes(
            "Implementation-Title" to aManifestTitle,
            "Implementation-Version" to tmpAppVersion,
            "Main-Class" to providers.gradleProperty("mainClassName").get()
        )
    }

    duplicatesStrategy = DuplicatesStrategy.INCLUDE

    from({
        configurations.runtimeClasspath.get()
            .filter { file -> file.name.endsWith(".jar") && aJarFilter(file) }
            .map { file -> zipTree(file) }
    })

    with(tasks.jar.get())
    // Benefits: Faster JAR creation, smaller metadata overhead, better build cache hits
    // Enables reproducible builds (same inputs = identical outputs)
    isPreserveFileTimestamps = false
    // Benefits: Reproducible builds, better compression ratios, improved build cache efficiency
    // Files are sorted deterministically regardless of filesystem ordering
    isReproducibleFileOrder = true
}

val fatJar by createFatJarTask(
    aDescription = "Creates a fat JAR with all runtime dependencies",
    anArchiveClassifier = "",
    aManifestTitle = "MORTAR Fat Jar File"
) { file -> !file.name.endsWith("aarch64.jar") }

val fatJarAarch64 by createFatJarTask(
    aDescription = "Creates a fat JAR with all runtime dependencies for Aarch64",
    anArchiveClassifier = "-aarch64",
    aManifestTitle = "MORTAR Fat Jar File for AArch64"
) { file -> !file.name.endsWith("linux.jar") && !file.name.endsWith("mac.jar") }
//</editor-fold>

tasks.jar {
    manifest {
        attributes(
            "Main-Class" to providers.gradleProperty("mainClassName").get()
        )
    }
}

// Improved artifacts configuration with proper task dependencies
configurations.create("fatJars")
artifacts {
    add("fatJars", fatJar)
    add("fatJars", fatJarAarch64)
}

// Setting the main class for the application plugin so that the Gradle run task runs as expected
application {
    mainClass.set(providers.gradleProperty("mainClassName"))
}

distributions {
    main {
        contents {
            // Use lazy file collections for better performance
            into("bin") {
                duplicatesStrategy = DuplicatesStrategy.EXCLUDE
                from(tasks.named<CreateStartScripts>("mortarHighMemory"))
                filePermissions {
                    unix("0755") // rwxr-xr-x - owner can read, write, and execute; group and others can read and execute
                }
            }
            from(layout.projectDirectory.dir("AdoptOpenJDK"))
            into("tutorial") {
                from(layout.projectDirectory.dir("Tutorial"))
            }
        }
    }
}

spotless {
    java {
        toggleOffOn() // all code in between spotless:off / spotless:on will be ignored
        licenseHeaderFile("License-header/License-header.txt")
        encoding("UTF-8")
        cleanthat()
        importOrder("com", "de", "javafx", "org", "javax", "java")
        removeUnusedImports()
        leadingTabsToSpaces(4)
        trimTrailingWhitespace()
        endWithNewline()
        // eclipse() // not optimal, because indents with tabs..
        // googleJavaFormat() // not optimal, because indents with two spaces...
        // palantirJavaFormat() // not optimal, because unnecessary line breaks in head of for loop and corrupts editor folds
        // prettier() // needs npm installed, unsuitable...
        // clangFormat() // also needs an installation...
        // Wildcard imports can't be resolved by spotless itself.
        // The Kotlin version of spotless needs the workaround used below, as described in
        // this GitHub issue: https://github.com/diffplug/spotless/issues/2387#issuecomment-2576459901
        custom("Refuse wildcard imports", object : Serializable, FormatterFunc {
            override fun apply(input: String): String {
                if (input.contains(Regex("""\nimport .*\*;"""))) {
                    throw AssertionError("Do not use wildcard imports. 'spotlessApply' cannot resolve this issue.")
                }
                return input
            }
        })
    }
}

import com.diffplug.spotless.FormatterFunc
import org.gradle.api.tasks.application.CreateStartScripts
import java.io.Serializable

plugins {
    id("application")
    alias(libs.plugins.javafxplugin)
    alias(libs.plugins.spotless)
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
    // CDK SNAPSHOT repository, uncomment if needed
    // maven { url = uri("https://s01.oss.sonatype.org/content/repositories/snapshots") }
}

dependencies {
    testImplementation(platform(libs.junit))
    testImplementation(libs.jupiter)
    //<editor-fold desc="CDK dependencies">
    // implementation(libs.cdkBundle)
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

tasks.test {
    useJUnitPlatform()
    testLogging {
        events = setOf(
            org.gradle.api.tasks.testing.logging.TestLogEvent.PASSED,
            org.gradle.api.tasks.testing.logging.TestLogEvent.SKIPPED,
            org.gradle.api.tasks.testing.logging.TestLogEvent.FAILED
        )
    }
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

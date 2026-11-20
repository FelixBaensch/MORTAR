/*
 * MORTAR - MOlecule fRagmenTAtion fRamework
 * Copyright (C) 2026  Felix Baensch, Jonas Schaub (felix.j.baensch@gmail.com, jonas.schaub@uni-jena.de)
 *
 * Source code is available at <https://github.com/FelixBaensch/MORTAR>
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package de.unijena.cheminf.mortar.gradle.deploy

import de.unijena.cheminf.mortar.gradle.bundle.MortarBundle
import de.unijena.cheminf.mortar.gradle.bundle.gen.PropertyNames
import de.unijena.cheminf.mortar.gradle.util.DeployUtil

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction
import org.gradle.process.ExecOperations

import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import javax.inject.Inject

/**
 * Gradle task for local macOS deployment using jpackage.
 *
 * This task prepares and builds a DMG using the jpackage tool. It packages the jars of the
 * installed distribution (installDist), keeping only the JavaFX jars of the target
 * architecture: "mac" for Intel, "mac-aarch64" for Apple Silicon (ARM).
 *
 * Pre-checks before invocation:
 * - current OS must be macOS
 * - jpackage must be available on PATH (provided by JDK 14+)
 * - icon Images/MORTAR-icon.icns must exist
 * - the installed distribution (build/install/MORTAR/lib) must exist
 *
 * @author Martin Urban
 */
open class LocalMacDeploy @Inject constructor(
    private val execOperations: ExecOperations
) : DefaultTask() {
    init {
        description = MortarBundle.message(PropertyNames.LOCAL_DEPLOY_MAC_DESC)
        group = MortarBundle.message(PropertyNames.LOCAL_DEPLOY_GRADLE_GROUP)
    }

    /** Target architecture: "intel" or "arm" (aarch64). Defaults to intel. */
    @Input
    var arch: String = "intel"

    @TaskAction
    fun deploy() {
        require(DeployUtil.isMac()) { MortarBundle.message(PropertyNames.MSG_LOCAL_DEPLOY_RUN_ON_MAC) }
        DeployUtil.ensureJpackageIsAvailable(project)

        val appName = project.providers.gradleProperty("appName").get()
        val appVersion = project.providers.gradleProperty("appVersion").get()
        val appVersionShort = DeployUtil.toThreePartVersion(appVersion)
        val iconFile = File(project.rootDir, MortarBundle.message(PropertyNames.ICON_PATH_MAC))
        require(iconFile.exists()) { "Icon file not found: ${iconFile.absolutePath}" }

        // Application jar plus all dependency jars from installDist; only the JavaFX jars of the target are kept
        val jarName = "$appName-$appVersion.jar"
        val javaFxClassifier = if (arch == "arm") "mac-aarch64" else "mac"
        val buildDir = project.layout.buildDirectory.asFile.get()
        val inputDir = DeployUtil.stageJpackageInput(
            File(buildDir, "install${File.separator}$appName${File.separator}lib"),
            File(buildDir, "jpackage${File.separator}input"),
            javaFxClassifier
        )
        require(File(inputDir, jarName).exists()) { "Application jar not found in ${inputDir.absolutePath}: $jarName" }
        // Write the DMG to the build directory instead of the project root
        val outputDir = File(project.layout.buildDirectory.asFile.get(), "jpackage${File.separator}out")

        // Build jpackage command mirroring deployment.yml
        val cmd = mutableListOf(
            "jpackage",
            "-i", inputDir.absolutePath,
            "--dest", outputDir.absolutePath,
            "-n", appName,
            "--icon", iconFile.absolutePath,
            "--main-jar", jarName,
            "-t", "dmg",
            "--app-version", appVersionShort,
            "--copyright", MortarBundle.message(PropertyNames.LOCAL_DEPLOY_COPYRIGHT),
            "--description", MortarBundle.message(PropertyNames.LOCAL_DEPLOY_DESC),
            "--about-url", MortarBundle.message(PropertyNames.LOCAL_DEPLOY_ABOUT_URL),
            // Pass as two separate options for best compatibility
            "--java-options", MortarBundle.message(PropertyNames.LOCAL_DEPLOY_UNIX_JAVA_OPTION_1),
            "--java-options", MortarBundle.message(PropertyNames.LOCAL_DEPLOY_UNIX_JAVA_OPTION_2)
        )

        execOperations.exec {
            workingDir = project.rootDir
            commandLine(cmd)
        }

        logger.lifecycle("macOS DMG created for $arch architecture")

        if (arch == "arm") {
            val defaultDmgName = "$appName-$appVersionShort.dmg"
            val desiredDmgName = "$appName-aarch64-$appVersionShort.dmg"

            val defaultFile = File(outputDir, defaultDmgName)
            val desiredFile = File(outputDir, desiredDmgName)

            if (defaultFile.exists()) {
                try {
                    Files.move(defaultFile.toPath(), desiredFile.toPath(), StandardCopyOption.REPLACE_EXISTING)
                    logger.lifecycle("Renamed DMG to: ${desiredFile.name}")
                } catch (e: Exception) {
                    throw org.gradle.api.GradleException("Failed to rename DMG: ${e.message}", e)
                }
            }
        }
    }
}

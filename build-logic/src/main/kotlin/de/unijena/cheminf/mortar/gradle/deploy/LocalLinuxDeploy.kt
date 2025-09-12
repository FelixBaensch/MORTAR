/*
 * MORTAR - MOlecule fRagmenTAtion fRamework
 * Copyright (C) 2025  Felix Baensch, Jonas Schaub (felix.j.baensch@gmail.com, jonas.schaub@uni-jena.de)
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

import java.io.File

/**
 * Gradle task for local Linux deployment using jpackage.
 *
 * This task prepares and builds a DEB or RPM using the jpackage tool. It mirrors the
 * GitHub Actions configuration used in .github/workflows/deployment.yml.
 *
 * Pre-checks before invocation:
 * - current OS must be Linux
 * - jpackage must be available on PATH (provided by JDK 14+)
 * - icon Images/MORTAR-icon.icns must exist (matching the workflow)
 * - expected fat jar must exist
 *
 * @author Martin Urban
 */
open class LocalLinuxDeploy : DefaultTask() {
    init {
        description = MortarBundle.message(PropertyNames.LOCAL_DEPLOY_LINUX_DESC)
        group = MortarBundle.message(PropertyNames.LOCAL_DEPLOY_GRADLE_GROUP)
    }

    /** Package type: "deb" or "rpm". Defaults to "deb". */
    @Input
    var type: String = "deb"

    @TaskAction
    fun deploy() {
        require(DeployUtil.isLinux()) { MortarBundle.message(PropertyNames.MSG_LOCAL_DEPLOY_RUN_ON_LINUX) }
        DeployUtil.ensureJpackageIsAvailable(project)

        val appName = project.providers.gradleProperty("appName").get()
        val appVersion = project.providers.gradleProperty("appVersion").get()
        val appVersionShort = DeployUtil.toThreePartVersion(appVersion)
        val iconFile = File(project.rootDir, MortarBundle.message(PropertyNames.ICON_PATH_LINUX))
        require(iconFile.exists()) { "Icon file not found: ${iconFile.absolutePath}" }

        val jarName = "$appName-fat-$appVersion.jar"
        val fatJar = File(project.layout.buildDirectory.asFile.get(), "libs${File.separator}$jarName")
        require(fatJar.exists()) { "Fat jar not found. Build it first: ${fatJar.absolutePath}" }

        val pkgType = when (type.lowercase()) {
            "deb", "rpm" -> type.lowercase()
            else -> throw IllegalArgumentException("Unsupported package type: '$type'. Use 'deb' or 'rpm'.")
        }

        val cmd = mutableListOf(
            "jpackage",
            "-i", project.rootDir.absolutePath,
            "-n", appName,
            "--icon", iconFile.absolutePath,
            "--main-jar", fatJar.absolutePath,
            "-t", pkgType,
            "--app-version", appVersionShort,
            "--copyright", MortarBundle.message(PropertyNames.LOCAL_DEPLOY_COPYRIGHT),
            "--description", MortarBundle.message(PropertyNames.LOCAL_DEPLOY_DESC),
            "--about-url", MortarBundle.message(PropertyNames.LOCAL_DEPLOY_ABOUT_URL),
            // Pass as two separate options for best compatibility
            "--java-options", MortarBundle.message(PropertyNames.LOCAL_DEPLOY_UNIX_JAVA_OPTION_1),
            "--java-options", MortarBundle.message(PropertyNames.LOCAL_DEPLOY_UNIX_JAVA_OPTION_2)
        )

        project.exec {
            workingDir = project.rootDir
            commandLine(cmd)
        }

        logger.lifecycle("Linux package created: $pkgType")
    }
}

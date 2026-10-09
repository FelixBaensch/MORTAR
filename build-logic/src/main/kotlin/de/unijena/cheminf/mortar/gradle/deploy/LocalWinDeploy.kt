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
import org.gradle.api.tasks.TaskAction
import org.gradle.process.ExecOperations

import java.io.File
import java.util.UUID
import javax.inject.Inject

/**
 * Gradle task for local Windows deployment using Inno Setup 6.
 *
 * This task automates the process of preparing a Windows installer for the application.
 * It performs the following steps:
 * 1. Checks for Inno Setup 6 installation.
 * 2. Creates the required directory structure under the build directory.
 * 3. Copies application binaries, libraries, tutorials, and icon files.
 * 4. Copies the Inno Setup script and license file.
 * 5. Downloads and extracts the JRE if not already present.
 * 6. Compiles the Inno Setup script to generate the installer.
 *
 * Note: This task requires Inno Setup 6 to be installed at the default location
 * (`C:\Program Files (x86)\Inno Setup 6\iscc.exe`).
 *
 * @author Martin Urban
 */
open class LocalWinDeploy @Inject constructor(
    private val execOperations: ExecOperations
): DefaultTask() {

    //<editor-fold desc="public methods">
    init {
        description = MortarBundle.message(PropertyNames.LOCAL_DEPLOY_WIN_DESC)
        group = MortarBundle.message(PropertyNames.LOCAL_DEPLOY_GRADLE_GROUP)
    }

    /**
     * Deploys the application locally for Windows.
     *
     * This task performs the following steps:
     * 1. Checks if Inno Setup 6 is installed.
     * 2. Creates the required directory structure.
     * 3. Copies necessary files for deployment.
     * 4. Copies the Inno Setup script and license file.
     * 5. Downloads and extracts the JRE if not present.
     * 6. Compiles the Inno Setup script to build the installer.
     *
     */
    @TaskAction
    fun deploy() {
        require(DeployUtil.isWindows()) { MortarBundle.message(PropertyNames.MSG_LOCAL_DEPLOY_RUN_ON_WINDOWS) }
        ensureInnoSetupIsAvailable()
        createDirectories()
        copyFiles()
        copyInnoScript()
        downloadJRE()
        compileInnoScript()
        logger.lifecycle("Local Windows deployment completed")
    }
    //</editor-fold>
    //
    //<editor-fold desc="private methods">
    /**
     * Checks if Inno Setup 6 is installed by verifying the existence of `iscc.exe`.
     *
     * Note: This method assumes that Inno Setup 6 is installed in the default location.
     *
     * @throws RuntimeException if Inno Setup 6 is not found
     */
    private fun ensureInnoSetupIsAvailable() {
        val tmpInnoPath = File(MortarBundle.message(PropertyNames.LOCAL_DEPLOY_WIN_INNO_SETUP_COMPILER_PATH))
        if (!tmpInnoPath.exists()) {
            throw RuntimeException(MortarBundle.message(PropertyNames.MSG_INNO_SETUP_NOT_FOUND))
        }
    }

    /**
     * Creates the required directory structure for Windows deployment.
     *
     * The following directories are created under the build directory:
     * - winDeploy
     * - winDeploy/in
     * - winDeploy/in/bin
     * - winDeploy/in/lib
     * - winDeploy/in/tutorial
     * - winDeploy/in/icon
     * - winDeploy/out
     *
     * Note: The JRE directory is created when the JRE is downloaded.
     */
    private fun createDirectories() {
        val tmpBuildDir = project.layout.buildDirectory.asFile.get()
        // The directory for the JRE is created when the JRE is downloaded
        listOf(
            "winDeploy",
            "winDeploy/in",
            "winDeploy/in/bin",
            "winDeploy/in/lib",
            "winDeploy/in/tutorial",
            "winDeploy/in/icon",
            "winDeploy/out"
        ).forEach { File(tmpBuildDir, it).mkdirs() }
    }

    /**
     * Copies the necessary files for Windows deployment.
     *
     * Copies the following from `build/install/MORTAR` to the corresponding `winDeploy/in` subdirectories:
     * - `bin` directory to `in/bin`
     * - `lib` directory to `in/lib`
     * - `tutorial` directory to `in/tutorial`
     *
     * Also copies the icon file `Images/Mortar_Logo_Icon1.ico` to `in/icon/Mortar_Logo_Icon1.ico` if it exists.
     */
    private fun copyFiles() {
        logger.lifecycle("Copying files...")
        
        val tmpInstallDir = File("build/install/MORTAR")
        val tmpBuildDir = project.layout.buildDirectory.asFile.get()
        val tmpWinDeployDir = File(tmpBuildDir, "winDeploy")

        // Remove unnecessary unix start scripts
        if (!File(tmpInstallDir, "bin/MORTAR").delete()) {
            logger.lifecycle("Unix MORTAR start script not deleted, this can have several reasons, like not running clean before deploy.")
        }
        if (!File(tmpInstallDir, "bin/MORTAR_20GB").delete()) {
            logger.lifecycle("Unix MORTAR start script not deleted, this can have several reasons, like not running clean before deploy.")
        }

        DeployUtil.copyDir(File(tmpInstallDir, "bin"), File(tmpWinDeployDir, "in/bin"))
        DeployUtil.copyDir(File(tmpInstallDir, "lib"), File(tmpWinDeployDir, "in/lib"))
        DeployUtil.copyDir(File(tmpInstallDir, "tutorial"), File(tmpWinDeployDir, "in/tutorial"))

        File(MortarBundle.message(PropertyNames.ICON_PATH_WINDOWS)).takeIf { it.exists() }
            ?.copyTo(File(tmpWinDeployDir, "in/icon/Mortar_Logo_Icon1.ico"), true)
    }

    /**
     * Copies the Inno Setup script and license file to the `winDeploy` directory.
     *
     * - Finds the `.iss` script file using [findInnoScript] and copies it to `winDeploy`.
     * - Copies `LICENSE.txt` from the project root to `winDeploy` if it exists.
     *
     * This step is important because the paths in the Inno Setup script are relative to the
     * script's location.
     */
    private fun copyInnoScript() {
        logger.lifecycle("Copying Inno Setup script...")
        val tmpScriptFile = findInnoScript()
        val tmpBuildDir = project.layout.buildDirectory.asFile.get()
        val tmpWinDeployDir = File(tmpBuildDir, "winDeploy")
        val tmpTargetScript = File(tmpWinDeployDir, tmpScriptFile.name)

        tmpScriptFile.copyTo(tmpTargetScript, true)

        val tmpLicenseFile = File(project.rootDir, "LICENSE.txt")
        if (tmpLicenseFile.exists()) {
            tmpLicenseFile.copyTo(File(tmpWinDeployDir, "LICENSE.txt"), true)
        }
    }

    /**
     * Compiles the Inno Setup script using the Inno Setup 6 compiler `iscc.exe`.
     *
     * This method searches for a `.iss` script file in the `winDeploy` directory,
     * then invokes the Inno Setup compiler (`iscc.exe`) to build the installer.
     *
     * Note: A pre-installed Inno Setup 6 is required for this task to succeed. In addition, the
     * Inno Setup 6 installation path must be the default one located at
     * `C:\Program Files (x86)\Inno Setup 6\iscc.exe`.
     *
     * @throws RuntimeException if no `.iss` script is found in the `winDeploy` directory.
     */
    private fun compileInnoScript() {
        logger.lifecycle("Compiling Inno Setup script...")
        val tmpBuildDir = project.layout.buildDirectory.asFile.get()
        val tmpWinDeployDir = File(tmpBuildDir, "winDeploy")
        val tmpScriptFile = tmpWinDeployDir.listFiles()?.firstOrNull { it.extension == "iss" }
            ?: throw RuntimeException("No .iss script found in winDeploy directory")

        val appVersion = MortarBundle.message(PropertyNames.APP_VERSION)
        // Resolve AppId from YAML (create file and mapping if missing)
        val appGuid = getOrCreateAppIdForVersion(appVersion)
        // Call Inno Setup, passing both defines via command line
        execOperations.exec {
            workingDir = tmpWinDeployDir
            commandLine(
                MortarBundle.message(PropertyNames.LOCAL_DEPLOY_WIN_INNO_SETUP_COMPILER_PATH),
                "/DthisAppVersion=$appVersion",
                "/DthisAppId=$appGuid",
                tmpScriptFile.absolutePath
            )
        }
    }

    //<editor-fold desc="Windows-specific utilities">
    // YAML helpers: keep YAML handling encapsulated and reusable
    /**
     * Ensures the existence of the `app-ids.yml` file used for holding application version IDs.
     *
     * If the file does not exist at the specified path within the project directory, it:
     * 1. Creates the necessary directory structure.
     * 2. Initializes the file with a default comment indicating that it is auto-generated.
     *
     * @return the `app-ids.yml` file within the project's `build-logic/resources/innoSetup` directory
     */
    private fun getAppIdsYamlFile(): File {
        val tmpFile = File(project.rootDir, MortarBundle.message(PropertyNames.LOCAL_DEPLOY_WIN_APP_ID_YAML_PATH))
        if (!tmpFile.exists()) {
            tmpFile.parentFile?.mkdirs()
            tmpFile.writeText("# App IDs by version (auto-generated)" + System.lineSeparator())
        }
        return tmpFile
    }

    /**
     * Loads application IDs from the specified file.
     *
     * This method reads each line of the `app-ids.yaml` file, ignoring empty lines and lines starting with '#'.
     * Each valid line should contain a key (version number) and value (app id) separated by a colon (':').
     * Leading and trailing whitespace around keys and values are trimmed, and values enclosed in quotes are also unquoted.
     * The resulting key-value pairs are stored in a map.
     *
     * @param aFile the file from which application IDs are loaded. If the file does not exist, an empty map is returned.
     * @return a mutable map containing the application IDs as key-value pairs
     */
    private fun loadAppIds(aFile: File): MutableMap<String, String> {
        val tmpIds = mutableMapOf<String, String>()
        if (!aFile.exists()) {
            return tmpIds
        }
        aFile.forEachLine { tmpLine ->
            val trimmed = tmpLine.trim()
            if (trimmed.isEmpty() || trimmed.startsWith("#")) return@forEachLine
            val tmpIdx = trimmed.indexOf(':')
            if (tmpIdx > 0) {
                val tmpKey = trimmed.substring(0, tmpIdx).trim()
                val tmpValue = trimmed.substring(tmpIdx + 1).trim().trim('"')
                if (tmpKey.isNotEmpty() && tmpValue.isNotEmpty()) tmpIds.set(tmpKey, tmpValue)
            }
        }
        return tmpIds
    }

    /**
     * Retrieves or creates a unique application ID for a given version.
     *
     * If an application ID corresponding to the specified version exists in the `app-ids.yml` file,
     * it is retrieved. If no such ID is found, a new one is generated using `UUID.randomUUID()`,
     * stored in the `app-ids.yml` file, and then returned.
     *
     * @param aVersion the application version for which an ID is required
     * @return the application ID associated with the specified version
     */
    private fun getOrCreateAppIdForVersion(aVersion: String): String {
        val tmpYamlFile = getAppIdsYamlFile()
        val tmpIds = loadAppIds(tmpYamlFile)
        var tmpGuid = tmpIds[aVersion]
        if (tmpGuid.isNullOrBlank()) {
            tmpGuid = UUID.randomUUID().toString()
            val tmpText = tmpYamlFile.readText()
            val tmpEndsWithNewline = tmpText.isNotEmpty() && (tmpText.last() == '\n' || tmpText.last() == '\r')
            if (!tmpEndsWithNewline) {
                tmpYamlFile.appendText(System.lineSeparator())
            }
            tmpYamlFile.appendText("$aVersion: $tmpGuid${System.lineSeparator()}")
            logger.lifecycle("Updated app-ids.yml with new AppId for version $aVersion")
        }
        return tmpGuid
    }

    /**
     * Finds the first Inno Setup script file (`.iss`) in the `build-logic/src/main/resources/inno_setup` directory.
     *
     * @return the `.iss` script file found in the resources directory.
     * @throws RuntimeException if no `.iss` script is found in the directory.
     */
    private fun findInnoScript(): File {
        val tmpResourcesDir = File(project.rootDir, "${MortarBundle.message(PropertyNames.LOCAL_DEPLOY_WIN_RESOURCES_DIR)}/inno_setup")
        val tmpScriptFile = tmpResourcesDir.listFiles()?.firstOrNull { it.extension == "iss" }
            ?: throw RuntimeException("No .iss script found in build-logic/src/main/resources/inno_setup")
        return tmpScriptFile
    }

    /**
     * Downloads and extracts the JRE required for Windows deployment.
    *
    * - Checks if the JRE directory already exists and is not empty.
    *   If so, skips the download.
    * - Downloads the JRE ZIP from Adoptium Temurin
    * - Extracts the ZIP to `winDeploy/in`
    * - Renames the extracted directory
    * - Deletes the ZIP file after extraction
    *
    * @throws RuntimeException if the download or extraction fails.
    */
    private fun downloadJRE() {
        val tmpBuildDir = project.layout.buildDirectory.asFile.get()
        val tmpInnoSetupInputDir = File(tmpBuildDir, "winDeploy/in/")
        val tmpJreDeploymentDir = File(tmpInnoSetupInputDir, MortarBundle.message(PropertyNames.LOCAL_DEPLOY_WIN_JRE_DIR_NAME))

        if (tmpJreDeploymentDir.exists() && tmpJreDeploymentDir.listFiles()?.isNotEmpty() == true) {
            logger.lifecycle("JRE already exists, skipping download")
            return
        }

        logger.lifecycle("Downloading JRE...")
        val tmpTempDir = File(tmpBuildDir, "tmp")
        val tmpJreZip = File(tmpTempDir, "jre.zip")
        val tmpInnoInputsJreExtractDir = File(tmpInnoSetupInputDir, MortarBundle.message(PropertyNames.LOCAL_DEPLOY_WIN_JRE_EXTRACTED_DIR_NAME))
        val tmpInnoInputsJreDir = File(tmpInnoSetupInputDir, MortarBundle.message(PropertyNames.LOCAL_DEPLOY_WIN_JRE_DIR_NAME))

        try {
            DeployUtil.downloadFile(MortarBundle.message(PropertyNames.LOCAL_DEPLOY_WIN_JRE_URL), tmpJreZip)
            DeployUtil.extractZip(tmpJreZip, tmpInnoSetupInputDir)
            tmpInnoInputsJreExtractDir.renameTo(tmpInnoInputsJreDir)
            tmpJreZip.delete()
            logger.lifecycle("JRE download completed")
        } catch (e: Exception) {
            throw RuntimeException("Failed to download or extract JRE: ${e.message}", e)
        }
    }
    //</editor-fold>
    //</editor-fold>
}

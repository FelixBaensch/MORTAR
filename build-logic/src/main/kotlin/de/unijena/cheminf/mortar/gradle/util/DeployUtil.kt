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
package de.unijena.cheminf.mortar.gradle.util

import de.unijena.cheminf.mortar.gradle.bundle.MortarBundle
import de.unijena.cheminf.mortar.gradle.bundle.gen.PropertyNames

import org.gradle.api.Project

import java.io.File
import java.net.URI
import java.util.zip.ZipInputStream

/**
 * Utility functions for deployment tasks.
 *
 * Provides methods for downloading files, extracting ZIP archives, copying directories,
 * checking the operating system, and ensuring the availability of the `jpackage` tool.
 *
 * @author Martin Urban
 */
object DeployUtil {
    //<editor-fold desc="private class variables">
    /**
     * Holds the name of the operating system on which the code is running.
     *
     * The value is fetched from the "os.name" system property, making it
     * dependent on the underlying platform and Java's system property
     * value for OS identification.
     *
     * This property can be useful for determining the operating system
     * type to apply platform-specific configurations or logic.
     */
    private val osName: String = System.getProperty("os.name")
    //</editor-fold>
    //
    //<editor-fold desc="network utilities">
    /**
     * Downloads a file from the specified URL and saves it to the target location.
     *
     * Note: No checks are performed!
     *
     * @param anUrl the URL of the file to download
     * @param aTarget the destination `File` where the downloaded content will be saved
     * @throws RuntimeException if the download fails, providing a message with the URL and target file path
     */
    fun downloadFile(anUrl: String, aTarget: File): Unit {
        try {
            URI(anUrl).toURL().openStream().use { input ->
                aTarget.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
        } catch (anException: Exception) {
            // Rethrow as a RuntimeException abort the Gradle build in a controlled manner
            throw RuntimeException("Failed to download file from $anUrl to ${aTarget.absolutePath}", anException)
        }
    }
    //</editor-fold>
    //
    //<editor-fold desc="filesystem utilities">
    /**
     * Extracts the contents of a ZIP file into the specified target directory.
     *
     * Iterates through each entry in the ZIP file, creating directories as needed,
     * and writes file entries to the target location, preserving the directory structure.
     *
     * Note: No checks are performed!
     *
     * @param aZipFile the ZIP file to extract
     * @param aTargetDir the directory where the contents will be extracted
     * @throws RuntimeException if the extraction fails, providing a message with the ZIP file and target directory paths
     */
    fun extractZip(aZipFile: File, aTargetDir: File): Unit {
        try {
            ZipInputStream(aZipFile.inputStream()).use { zipStream ->
                var entry = zipStream.nextEntry
                while (entry != null) {
                    val entryFile = File(aTargetDir, entry.name)

                    if (entry.isDirectory) {
                        entryFile.mkdirs()
                    } else {
                        entryFile.parentFile?.mkdirs()
                        entryFile.outputStream().use { output ->
                            zipStream.copyTo(output)
                        }
                    }

                    zipStream.closeEntry()
                    entry = zipStream.nextEntry
                }
            }
        } catch (anException: Exception) {
            // Rethrow as a RuntimeException to abort the Gradle build in a controlled manner
            throw RuntimeException("Failed to extract ZIP file ${aZipFile.absolutePath} to ${aTargetDir.absolutePath}", anException)
        }
    }

    /**
     * Copies the contents of the source directory to the destination directory recursively.
     *
     * Notes:
     *  - Existing files in the destination will be overwritten.
     *  - No checks are performed!
     *
     * @param aSource the source directory to copy from
     * @param aDestination the destination directory to copy to
     * @throws RuntimeException if the copy operation fails, providing a message with the source and destination paths
     */
    fun copyDir(aSource: File, aDestination: File): Unit {
            try {
                if (aSource.exists()) aSource.copyRecursively(aDestination, true)
            } catch (anException: Exception) {
                // Rethrow as a RuntimeException to abort the Gradle build in a controlled manner
                throw RuntimeException("Failed to copy directory from ${aSource.absolutePath} to ${aDestination.absolutePath}", anException)
            }
        }
    //</editor-fold>
    //
    //<editor-fold desc="operating system utilities">
    /**
     * Checks if the current operating system is Windows.
     *
     * @return true if the OS name contains "Windows", false otherwise
     */
    fun isWindows(): Boolean = osName.contains(MortarBundle.message(PropertyNames.OS_WIN), true)

    /**
     * Checks if the current operating system is macOS.
     *
     * @return true if the OS name contains "Mac", false otherwise
     */
    fun isMac(): Boolean = osName.contains(MortarBundle.message(PropertyNames.OS_MAC), ignoreCase = true)

    /**
     * Checks if the current operating system is Linux.
     *
     * @return true if the OS name contains "Linux", false otherwise
     */
    fun isLinux(): Boolean = osName.contains(MortarBundle.message(PropertyNames.OS_LINUX), ignoreCase = true)
    //</editor-fold>
    //
    //<editor-fold desc="deployment utilities">
    /**
     * Ensures that the `jpackage` tool is available in the system PATH.
     *
     * Throws an exception if `jpackage` is not found, indicating that a JDK with `jpackage` must be installed.
     *
     * @param aProject the Gradle project context
     * @throws IllegalStateException if `jpackage` is not found
     */
    fun ensureJpackageIsAvailable(aProject: Project): Unit {
        try {
            aProject.exec { commandLine("jpackage", "--version") }
        } catch (anException: Exception) {
            throw IllegalStateException(MortarBundle.message(PropertyNames.MSG_JPACKAGE_NOT_FOUND), anException)
        }
    }
    //</editor-fold>
    //
    /**
     * Converts a full version string to a three-part version string.
     *
     * If the input string has three or more parts, it returns the first three parts joined by dots.
     * Otherwise, it returns the original string.
     *
     * @param aCompleteVersion a version number string, potentially with more than three parts
     * @return a three-part version string or the original string if it has fewer than four parts
     */
    fun toThreePartVersion(aCompleteVersion: String): String {
        val parts = aCompleteVersion.split('.')
        return if (parts.size >= 3) parts.take(3).joinToString(".") else aCompleteVersion
    }
}

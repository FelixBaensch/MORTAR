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
/**
 * Utility function for managing Mortar application scripts and files.
 *
 * @author Felix Baensch
 * @author Jonas Schaub
 * @author Martin Urban
 */
package de.unijena.cheminf.mortar.gradle.util

import java.io.File

/**
 * Modifies the start scripts for the Mortar application to align with specific configurations
 * for both Windows and Unix systems. The method applies various replacements in the script
 * content to update Java runtime settings, classpath definitions, and other executable
 * configurations.
 *
 * Note: Only two specific MORTAR optimization variables are supported: `%MORTAR_OPTS%` and `%MORTAR_20_GB_OPTS%`.
 *
 * @param aWindowsFile the File object representing the Windows start script to be modified
 * @param anUnixFile the File object representing the Unix start script to be modified
 * @param aMortarOptimizationVariable a specific JVM option to be added or replaced in the scripts
 * @throws IllegalArgumentException if the provided `aMortarOptimizationVariable` is not one of the expected values
 */
fun modifyMortarStartScripts(aWindowsFile: File, anUnixFile: File, aMortarOptimizationVariable: String): Unit {
    //<editor-fold desc="checks">
    val tmpOldStartupString: String = when (aMortarOptimizationVariable) {
        "%MORTAR_OPTS%" -> {
            "\"%JAVA_EXE%\" %DEFAULT_JVM_OPTS% %JAVA_OPTS% $aMortarOptimizationVariable  -classpath \"%CLASSPATH%\" de.unijena.cheminf.mortar.main.Main %*"
        }
        "%MORTAR_20_GB_OPTS%" -> {
            "\"%JAVA_EXE%\" %DEFAULT_JVM_OPTS% %JAVA_OPTS% $aMortarOptimizationVariable  -classpath \"%CLASSPATH%\" de.unijena.cheminf.mortar.main.Main %*"
        }
        else -> {
            throw IllegalArgumentException("MORTAR optimization variable must be either %MORTAR_OPTS% or MORTAR_20_GB_OPTS")
        }
    }
    //</editor-fold>
    //
    //<editor-fold desc="Windows script edits">
    aWindowsFile.writeText(
        aWindowsFile.readText()
            .replace("java.exe", "javaw.exe")
            .replace(
                "if defined JAVA_HOME goto findJavaFromJavaHome",
                "goto setJavaFromAppHome\n\n@rem unused because Java home is set in method called above"
            )
            .replace(":findJavaFromJavaHome", ":setJavaFromAppHome")
            .replace(
                "set JAVA_HOME=%JAVA_HOME:\"=%",
                "set JAVA_HOME=%APP_HOME%\\jdk-21.0.1_12_jre\\"
            )
            .replace(Regex("set CLASSPATH=.*"), "set CLASSPATH=.;%APP_HOME%/lib/*")
            .replace(
                tmpOldStartupString,
                "start \"MORTAR\" \"%JAVA_EXE%\" %DEFAULT_JVM_OPTS% %JAVA_OPTS% $aMortarOptimizationVariable  -classpath \"%CLASSPATH%\" de.unijena.cheminf.mortar.main.Main \"-skipJavaVersionCheck\""
            )
            .replace(
                "Please set the JAVA_HOME variable in your environment to match the",
                "Please check your MORTAR installation,"
            )
            .replace(
                "location of your Java installation.",
                "something must be wrong with the Java Runtime Environment shipped with MORTAR."
            )
    )
    //</editor-fold>
    //
    //<editor-fold desc="Unix script edits">
    anUnixFile.writeText(
        anUnixFile.readText().replace(Regex("CLASSPATH=\\\$APP_HOME/lib.*"), "CLASSPATH=\\\$APP_HOME/lib/*")
    )
    //</editor-fold>
}

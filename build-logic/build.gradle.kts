import java.time.LocalDateTime
import java.util.Properties

plugins {
    id("org.gradle.kotlin.kotlin-dsl") version "5.2.0"
}

repositories {
    // Needed to resolve the Kotlin-DSL plugin
    gradlePluginPortal()
}

gradlePlugin {
    plugins {
        register("mortarLinuxDeploy") {
            id = "mortar.deploy.linux"
            implementationClass = "de.unijena.cheminf.mortar.gradle.plugins.LinuxDeploymentPlugin"
        }
        register("mortarMacDeploy") {
            id = "mortar.deploy.mac"
            implementationClass = "de.unijena.cheminf.mortar.gradle.plugins.MacDeploymentPlugin"
        }
        register("mortarWinDeploy") {
            id = "mortar.deploy.win"
            implementationClass = "de.unijena.cheminf.mortar.gradle.plugins.WindowsDeploymentPlugin"
        }
        register("mortarStartScripts") {
            id = "mortar.start.scripts"
            implementationClass = "de.unijena.cheminf.mortar.gradle.plugins.MortarStartScriptsPlugin"
        }
    }
}

//<editor-fold desc="property names generation task">
/**
 * Gradle task to generate PropertyNames object from .properties file
 * with MORTAR license header and specific formatting
 */
tasks.register("generateBundlePropertyNames") {
    group = "build"
    description = "Generate PropertyNames object from MortarBundle.properties"

    // Input and output files
    val tmpPropertiesFile = file("src/main/resources/messages/MortarBundle.properties")
    val tmpOutputFile = file("src/main/kotlin/de/unijena/cheminf/mortar/gradle/bundle/gen/PropertyNames.kt")

    // Declare inputs and outputs for up-to-date checking
    inputs.file(tmpPropertiesFile)
    outputs.file(tmpOutputFile)
    outputs.cacheIf { true }

    doLast {
        fun keyToConstantName(key: String): String {
            return key.uppercase()
                .replace(".", "_")
                .replace("-", "_")
                .replace(" ", "_")
        }

        if (!tmpPropertiesFile.exists()) {
            throw GradleException("Properties file not found: ${tmpPropertiesFile.absolutePath}")
        }

        // Load properties
        val tmpProperties = Properties()
        tmpPropertiesFile.inputStream().use {
            tmpProperties.load(it)
        }

        // Generate the Kotlin file
        val tmpCode = buildString {
            //<editor-fold desc="MORTAR license header">
            appendLine("/*")
            appendLine(" * MORTAR - MOlecule fRagmenTAtion fRamework")
            appendLine(" * Copyright (C) 2025 Felix Baensch, Jonas Schaub (felix.j.baensch@gmail.com, jonas.schaub@uni-jena.de)")
            appendLine(" *")
            appendLine(" * Source code is available at <https://github.com/FelixBaensch/MORTAR>")
            appendLine(" *")
            appendLine(" * Permission is hereby granted, free of charge, to any person obtaining a copy")
            appendLine(" * of this software and associated documentation files (the \"Software\"), to deal")
            appendLine(" * in the Software without restriction, including without limitation the rights")
            appendLine(" * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell")
            appendLine(" * copies of the Software, and to permit persons to whom the Software is")
            appendLine(" * furnished to do so, subject to the following conditions:")
            appendLine(" *")
            appendLine(" * The above copyright notice and this permission notice shall be included in all")
            appendLine(" * copies or substantial portions of the Software.")
            appendLine(" *")
            appendLine(" * THE SOFTWARE IS PROVIDED \"AS IS\", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR")
            appendLine(" * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,")
            appendLine(" * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE")
            appendLine(" * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER")
            appendLine(" * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,")
            appendLine(" * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE")
            appendLine(" * SOFTWARE.")
            appendLine(" */")
            //</editor-fold>

            // Package and imports
            appendLine("package de.unijena.cheminf.mortar.gradle.bundle.gen")
            appendLine()
            appendLine("import de.unijena.cheminf.mortar.gradle.bundle.PropertyKey")
            appendLine()

            // Generation info
            appendLine("// Auto-generated at build time - do not edit!")
            appendLine("// Generated from: ${tmpPropertiesFile.name}")
            appendLine("// Generated at: ${LocalDateTime.now()}")
            appendLine()

            // Object declaration
            appendLine("object PropertyNames {")

            // Generate properties
            val keys = tmpProperties.stringPropertyNames().sorted()
            keys.forEachIndexed { index, tmpKey ->
                val tmpConstantName = keyToConstantName(tmpKey)
                val tmpPropertyValue = tmpProperties.getProperty(tmpKey)
                // val keyDescription = generateKeyDescription(key, propertyValue)
                val tmpKeyDescription = "Key for the ${tmpKey.replace(".", " ")} property."

                appendLine("    /** ")
                appendLine("     * $tmpKeyDescription")
                appendLine("     * ")
                appendLine("     * Property value: $tmpPropertyValue")
                appendLine("     */")
                appendLine("    val $tmpConstantName = PropertyKey(\"$tmpKey\")")

                // Add empty line between properties (except for the last one)
                if (index != keys.lastIndex) {
                    appendLine()
                }
            }

            appendLine("}")
        }

        // Write the generated file
        tmpOutputFile.parentFile.mkdirs()
        tmpOutputFile.writeText(tmpCode)

        logger.lifecycle("Generated PropertyNames with ${tmpProperties.size} properties")
    }
}

// Make compilation depend on property names generation
tasks.compileKotlin {
    dependsOn("generateBundlePropertyNames")
}
//</editor-fold>

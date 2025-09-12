/*
 * MORTAR - MOlecule fRagmenTAtion fRamework
 * Copyright (C) 2025
 */
package de.unijena.cheminf.mortar.gradle.plugins

import de.unijena.cheminf.mortar.gradle.deploy.LocalLinuxDeploy

import org.gradle.api.Plugin
import org.gradle.api.Project

/**
 * Gradle plugin for Linux deployment using jpackage.
 *
 * This plugin registers tasks for creating Linux deployment packages in DEB and RPM formats.
 * It applies Gradle best practices, such as lazy task registration and minimal configuration,
 * for better performance and compatibility with the configuration cache.
 *
 * Registered Tasks:
 * - `localLinuxDeb`: Creates a Debian package. Depends on the `fatJar` task.
 * - `localLinuxRpm`: Creates a Red Hat package. Depends on the `fatJar` task.
 *
 * Task Configuration:
 * - The `LocalLinuxDeploy` task type is used for the packaging process.
 * - Caching is disabled for these tasks due to their external nature.
 *
 * Preconditions for Task Execution:
 * - The operating system must be Linux.
 * - A valid installation of `jpackage` must be accessible on the system PATH (provided by JDK 14+).
 * - The required fat jar must be pre-built.
 * - An application icon file in the expected format must be available.
 *
 * This plugin is part of a series of deployment plugins in the MORTAR framework, designed for various platforms including macOS and Windows.
 *
 * @author Martin Urban
 */
class LinuxDeploymentPlugin : Plugin<Project> {
    /**
     * Applies the LinuxDeploymentPlugin to the given Gradle project.
     *
     * This method registers tasks for creating Linux deployment packages, specifically "localLinuxDeb"
     * and "localLinuxRpm". Each task uses the LocalLinuxDeploy task type to configure and execute
     * the packaging process with the desired options. These tasks disable caching due to their
     * external and side-effectful nature, and they depend on the "fatJar" task to ensure the required
     * artifact is built before packaging.
     *
     * @param project the Gradle project to which the plugin is applied, used for setting up tasks and configurations
     */
    override fun apply(project: Project) {
        with(project) {
            // Build cache: deployment tasks are external/side-effectful, so do not cache them
            // to avoid invalid cache hits across machines.
            tasks.register("localLinuxDeb", LocalLinuxDeploy::class.java) {
                // Use receiver syntax; avoid implicit 'it' which is not available in Kotlin DSL configuration lambdas
                type = "deb"
                outputs.cacheIf { false }
                // Require fatJar to be built before packaging
                dependsOn(tasks.named("fatJar"))
            }
            tasks.register("localLinuxRpm", LocalLinuxDeploy::class.java) {
                // Use receiver syntax; avoid implicit 'it' which is not available in Kotlin DSL configuration lambdas
                type = "rpm"
                outputs.cacheIf { false }
                // Require fatJar to be built before packaging
                dependsOn(tasks.named("fatJar"))
            }
        }
    }
}
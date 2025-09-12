/*
 * MORTAR - MOlecule fRagmenTAtion fRamework
 * Copyright (C) 2025
 */
package de.unijena.cheminf.mortar.gradle.plugins

import de.unijena.cheminf.mortar.gradle.deploy.LocalMacDeploy

import org.gradle.api.Plugin
import org.gradle.api.Project

/**
 * Gradle plugin for macOS deployment using jpackage.
 *
 * This plugin registers tasks for creating macOS DMG packages that are compatible with both Intel and ARM architectures.
 * It applies Gradle best practices, ensuring lazy task registration and minimal configuration for better performance and
 * compatibility with the configuration cache.
 *
 * Registered Tasks:
 * - `localMacDmgIntel`: Creates a DMG for Intel architecture. Depends on the standard fatJar task.
 * - `localMacDmgArm`: Creates a DMG for ARM (Apple Silicon) architecture. Depends on the fatJarAarch64 task.
 *
 * Task Configuration:
 * - The `LocalMacDeploy` task type is used for the DMG packaging process.
 * - Caching is disabled for these tasks due to their external and non-idempotent nature.
 *
 * Preconditions for Task Execution:
 * - The operating system must be macOS.
 * - A valid `jpackage` installation must be accessible on the system PATH (provided by JDK 14+).
 * - The required application fat jar (standard or for aarch64) must be pre-built.
 * - The application icon file (icns format) must be present in the expected location.
 *
 * This plugin is part of the series of deployment plugins in the MORTAR framework, similar to the LinuxDeploymentPlugin and WindowsDeploymentPlugin.
 *
 * @author Martin Urban
 */
class MacDeploymentPlugin : Plugin<Project> {
    /**
     * Configures the macOS deployment tasks for the Gradle project.
     *
     * This method registers two tasks for creating DMG deployment packages:
     * - `localMacDmgIntel`: Builds a DMG package for Intel architectures.
     * - `localMacDmgArm`: Builds a DMG package for ARM (Apple Silicon) architectures.
     *
     * Both tasks use the `LocalMacDeploy` task type, configure the target architecture,
     * and specify dependencies on the appropriate `fatJar` tasks. Caching is disabled
     * for these tasks to ensure fresh builds for deployment.
     *
     * @param project the Gradle project to which the plugin is applied, used for registering and configuring deployment tasks
     */
    override fun apply(project: Project) {
        with(project) {
            tasks.register("localMacDmgIntel", LocalMacDeploy::class.java) {
                // Use receiver syntax; avoid implicit 'it' which is not available in Kotlin DSL configuration lambdas
                arch = "intel"
                outputs.cacheIf { false }
                // Standard fatJar for Intel
                dependsOn(tasks.named("fatJar"))
            }
            tasks.register("localMacDmgArm", LocalMacDeploy::class.java) {
                // Use receiver syntax; avoid implicit 'it' which is not available in Kotlin DSL configuration lambdas
                arch = "arm"
                outputs.cacheIf { false }
                // AArch64 fat jar for Apple Silicon
                dependsOn(tasks.named("fatJarAarch64"))
            }
        }
    }
}
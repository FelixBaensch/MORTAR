/*
 * MORTAR - MOlecule fRagmenTAtion fRamework
 * Copyright (C) 2025
 */
package de.unijena.cheminf.mortar.gradle.plugins

import de.unijena.cheminf.mortar.gradle.deploy.LocalWinDeploy

import org.gradle.api.Plugin
import org.gradle.api.Project

/**
 * A custom Gradle plugin designed for handling Windows-specific deployment tasks within a project.
 *
 * This plugin registers a task named `localWinDeploy`, which is used for managing the deployment
 * of an application specifically for the Windows platform.
 *
 * The `localWinDeploy` task:
 * - Ensures that the application distribution is installed, by depending on the `installDist` task.
 * - Disables build caching for its outputs to ensure a fresh deployment.
 *
 * Preconditions for Task Execution:
 * - The operating system must be Windows.
 * - A valid installation of `Inno Setup 6` must be installed in its default location.
 * - An application icon file in the expected format must be available.
 *
 * This plugin is part of a series of deployment plugins in the MORTAR framework, designed for various platforms including macOS and Windows.
 *
 * @author Martin Urban
 */
class WindowsDeploymentPlugin : Plugin<Project> {
    /**
     * Applies the custom plugin logic to the specified Gradle project.
     *
     * Registers the `localWinDeploy` task of type `LocalWinDeploy`, which is responsible
     * for handling Windows-specific deployment tasks. This task is configured to ensure
     * that the `installDist` task is executed beforehand.
     *
     * @param project the Gradle project to which the plugin is applied
     */
    override fun apply(project: Project) {
        with(project) {
            tasks.register("localWinDeploy", LocalWinDeploy::class.java) {
                // Ensure the application distribution is installed
                dependsOn(tasks.named("installDist"))
            }
        }
    }
}

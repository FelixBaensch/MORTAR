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

package de.unijena.cheminf.mortar.controller;

import de.unijena.cheminf.mortar.configuration.Configuration;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Tests the lookup of the local tutorial PDF in {@link AboutViewController}.
 */
public class AboutViewControllerTest {
    /**
     * Relative path of the tutorial as given in the configuration.
     */
    private static String tutorialRelativePath;
    //
    /**
     * Reads the tutorial path from the app configuration, so the tests fail if the configured value does not match
     * the packaged layout.
     *
     * @throws IOException if the configuration cannot be read
     */
    @BeforeAll
    public static void readTutorialPathFromConfiguration() throws IOException {
        AboutViewControllerTest.tutorialRelativePath = Configuration.getInstance().getProperty("mortar.tutorial.relativeFilePath");
    }
    //
    /**
     * jpackage layout (macOS and Linux installers): the tutorial folder sits next to the jar.
     *
     * @param aTempDir temporary directory standing in for the app directory
     * @throws IOException if the test files cannot be created
     */
    @Test
    public void findTutorialFileNextToJarTest(@TempDir Path aTempDir) throws IOException {
        Path tmpJarDir = aTempDir.resolve("app");
        Path tmpTutorial = AboutViewControllerTest.createTutorial(tmpJarDir);
        File tmpResult = AboutViewController.findTutorialFile(tmpJarDir, AboutViewControllerTest.tutorialRelativePath);
        Assertions.assertEquals(tmpTutorial.toFile(), tmpResult);
    }
    //
    /**
     * Distribution layout (Windows installer, installDist): jars in lib/, tutorial folder next to lib/.
     *
     * @param aTempDir temporary directory standing in for the install root
     * @throws IOException if the test files cannot be created
     */
    @Test
    public void findTutorialFileNextToJarParentTest(@TempDir Path aTempDir) throws IOException {
        Path tmpJarDir = aTempDir.resolve("lib");
        Files.createDirectories(tmpJarDir);
        Path tmpTutorial = AboutViewControllerTest.createTutorial(aTempDir);
        File tmpResult = AboutViewController.findTutorialFile(tmpJarDir, AboutViewControllerTest.tutorialRelativePath);
        Assertions.assertEquals(tmpTutorial.toFile(), tmpResult);
    }
    //
    /**
     * Returns null if the tutorial is in neither location. The jar directory is nested in the temporary directory so
     * that the lookup in its parent stays inside the temporary directory too.
     *
     * @param aTempDir temporary directory standing in for the install folder
     */
    @Test
    public void findTutorialFileMissingTest(@TempDir Path aTempDir) {
        Assertions.assertNull(AboutViewController.findTutorialFile(aTempDir.resolve("app"), AboutViewControllerTest.tutorialRelativePath));
    }
    //
    /**
     * Returns null if no jar directory could be determined.
     */
    @Test
    public void findTutorialFileNullDirectoryTest() {
        Assertions.assertNull(AboutViewController.findTutorialFile(null, AboutViewControllerTest.tutorialRelativePath));
    }
    //
    /**
     * Creates an empty tutorial file at the configured relative path below the given directory.
     *
     * @param aBaseDir directory to create the tutorial in
     * @return path of the created file
     * @throws IOException if the file cannot be created
     */
    private static Path createTutorial(Path aBaseDir) throws IOException {
        Path tmpTutorial = aBaseDir.resolve(AboutViewControllerTest.tutorialRelativePath);
        Files.createDirectories(tmpTutorial.getParent());
        return Files.createFile(tmpTutorial);
    }
}

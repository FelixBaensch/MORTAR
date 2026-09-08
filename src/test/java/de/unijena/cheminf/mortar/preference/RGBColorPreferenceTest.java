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

package de.unijena.cheminf.mortar.preference;

import de.unijena.cheminf.mortar.configuration.Configuration;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.BufferedReader;
import java.io.FileReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;

/**
 * Test class for RGBColorPreference, the largest typed IPreference implementation. Covers the two constructor overloads
 * (int 0 - 255 and double 0 - 1), the four setters, both static isValidContent overloads, the invalid-name guard,
 * copy(), and the malformed-component reload branch.
 *
 * @author Felix Baensch
 * @version 1.0.0.0
 */
public class RGBColorPreferenceTest {
    /**
     * Constructor to initialize locale and configuration.
     *
     * @throws Exception if anything goes wrong
     */
    public RGBColorPreferenceTest() throws Exception {
        Locale.setDefault(Locale.of("en", "GB"));
        Configuration.getInstance();
    }
    //
    /**
     * Tests the double-arg constructor with clean in-range fractions and the out-of-range IllegalArgumentException
     * guard.
     *
     * @throws Exception if anything goes wrong
     */
    @Test
    public void testDoubleConstructor() throws Exception {
        RGBColorPreference tmpPref = new RGBColorPreference("Double colour", 0.0, 0.5, 1.0, 1.0);
        Assertions.assertArrayEquals(new double[]{0.0, 0.5, 1.0, 1.0}, tmpPref.getComponents(), 0.0);
        Assertions.assertThrows(IllegalArgumentException.class,
                () -> new RGBColorPreference("Out of range double", 1.5, 0.0, 0.0, 1.0));
    }
    //
    /**
     * Tests the int-arg constructor with valid 0 - 255 components and the out-of-range IllegalArgumentException guard.
     *
     * @throws Exception if anything goes wrong
     */
    @Test
    public void testIntConstructor() throws Exception {
        RGBColorPreference tmpPref = new RGBColorPreference("Int colour", 0, 255, 0, 255);
        Assertions.assertArrayEquals(new double[]{0.0, 1.0, 0.0, 1.0}, tmpPref.getComponents(), 0.0);
        Assertions.assertThrows(IllegalArgumentException.class,
                () -> new RGBColorPreference("Out of range int", 300, 0, 0, 255));
    }
    //
    /**
     * Tests setContent(double, double, double, double) for valid in-range input and the out-of-range
     * IllegalArgumentException guard.
     *
     * @throws Exception if anything goes wrong
     */
    @Test
    public void testSetContentDouble() throws Exception {
        RGBColorPreference tmpPref = new RGBColorPreference("Double setter", 0.0, 0.0, 0.0, 1.0);
        tmpPref.setContent(1.0, 0.5, 0.25, 0.0);
        Assertions.assertArrayEquals(new double[]{1.0, 0.5, 0.25, 0.0}, tmpPref.getComponents(), 0.0);
        Assertions.assertThrows(IllegalArgumentException.class, () -> tmpPref.setContent(1.5, 0.0, 0.0, 1.0));
    }
    //
    /**
     * Tests setContent(int, int, int, int) for valid in-range input and the out-of-range IllegalArgumentException
     * guard.
     *
     * @throws Exception if anything goes wrong
     */
    @Test
    public void testSetContentInt() throws Exception {
        RGBColorPreference tmpPref = new RGBColorPreference("Int setter", 0, 0, 0, 255);
        tmpPref.setContent(255, 0, 255, 0);
        Assertions.assertArrayEquals(new double[]{1.0, 0.0, 1.0, 0.0}, tmpPref.getComponents(), 0.0);
        Assertions.assertThrows(IllegalArgumentException.class, () -> tmpPref.setContent(300, 0, 0, 255));
    }
    //
    /**
     * Tests that setContent(int, int, int, int) normalizes every channel by dividing by 255, using non-zero,
     * non-maximum green/blue/alpha values so the division is distinguishable from any other arithmetic (a zero or
     * 255 input cannot tell {@code / 255} apart from {@code * 255}). Pins the per-channel normalization arithmetic.
     *
     * @throws Exception if anything goes wrong
     */
    @Test
    public void testSetContentIntNormalizesAllChannels() throws Exception {
        RGBColorPreference tmpPref = new RGBColorPreference("Channel normalisation", 0, 0, 0, 255);
        tmpPref.setContent(255, 128, 64, 200);
        Assertions.assertArrayEquals(
                new double[]{255.0 / 255.0, 128.0 / 255.0, 64.0 / 255.0, 200.0 / 255.0},
                tmpPref.getComponents(), 1e-9);
    }
    //
    /**
     * Tests that setAlpha(double) accepts both inclusive range boundaries (0.0 and 1.0) without throwing and stores
     * them. The rejection condition is {@code anAlphaValue < 0.0 || anAlphaValue > 1.0}, so both boundaries must be
     * accepted; this pins both boundary comparisons so a {@code <=} / {@code >=} mutation (which would wrongly reject
     * a boundary value) is detected.
     *
     * @throws Exception if anything goes wrong
     */
    @Test
    public void testSetAlphaDoubleAcceptsRangeBoundaries() throws Exception {
        RGBColorPreference tmpPref = new RGBColorPreference("Alpha double bounds", 0.0, 0.0, 0.0, 0.5);
        tmpPref.setAlpha(0.0);
        Assertions.assertEquals(0.0, tmpPref.getComponents()[3], 0.0);
        tmpPref.setAlpha(1.0);
        Assertions.assertEquals(1.0, tmpPref.getComponents()[3], 0.0);
    }
    //
    /**
     * Tests that setAlpha(int) accepts the inclusive lower boundary of zero without throwing and stores it as 0.0.
     * The rejection condition is {@code anAlphaValue < 0}, so zero must be accepted; this pins that boundary so a
     * {@code <=} mutation (which would wrongly reject zero) is detected.
     *
     * @throws Exception if anything goes wrong
     */
    @Test
    public void testSetAlphaIntAcceptsLowerBoundary() throws Exception {
        RGBColorPreference tmpPref = new RGBColorPreference("Alpha int bounds", 0.0, 0.0, 0.0, 0.5);
        tmpPref.setAlpha(0);
        Assertions.assertEquals(0.0, tmpPref.getComponents()[3], 0.0);
    }
    //
    /**
     * Tests setAlpha(double) for valid in-range input and the out-of-range IllegalArgumentException guard.
     *
     * @throws Exception if anything goes wrong
     */
    @Test
    public void testSetAlphaDouble() throws Exception {
        RGBColorPreference tmpPref = new RGBColorPreference("Alpha double", 0.0, 0.0, 0.0, 1.0);
        tmpPref.setAlpha(0.25);
        Assertions.assertArrayEquals(new double[]{0.0, 0.0, 0.0, 0.25}, tmpPref.getComponents(), 0.0);
        Assertions.assertThrows(IllegalArgumentException.class, () -> tmpPref.setAlpha(1.5));
    }
    //
    /**
     * Tests setAlpha(int) for valid in-range input and the out-of-range IllegalArgumentException guard.
     *
     * @throws Exception if anything goes wrong
     */
    @Test
    public void testSetAlphaInt() throws Exception {
        RGBColorPreference tmpPref = new RGBColorPreference("Alpha int", 0.0, 0.0, 0.0, 1.0);
        tmpPref.setAlpha(255);
        Assertions.assertArrayEquals(new double[]{0.0, 0.0, 0.0, 1.0}, tmpPref.getComponents(), 0.0);
        Assertions.assertThrows(IllegalArgumentException.class, () -> tmpPref.setAlpha(300));
    }
    //
    /**
     * Tests the static isValidContent(double...) overload for in-range (true) and out-of-range (false) components.
     *
     * @throws Exception if anything goes wrong
     */
    @Test
    public void testIsValidContentDouble() throws Exception {
        Assertions.assertTrue(RGBColorPreference.isValidContent(0.0, 0.5, 1.0, 1.0));
        Assertions.assertFalse(RGBColorPreference.isValidContent(1.5, 0.0, 0.0, 1.0));
    }
    //
    /**
     * Tests the static isValidContent(int...) overload for in-range (true) and out-of-range (false) components.
     *
     * @throws Exception if anything goes wrong
     */
    @Test
    public void testIsValidContentInt() throws Exception {
        Assertions.assertTrue(RGBColorPreference.isValidContent(0, 128, 255, 255));
        Assertions.assertFalse(RGBColorPreference.isValidContent(300, 0, 0, 255));
    }
    //
    /**
     * Tests the invalid-name constructor guard.
     *
     * @throws Exception if anything goes wrong
     */
    @Test
    public void testInvalidNameGuard() throws Exception {
        Assertions.assertThrows(IllegalArgumentException.class,
                () -> new RGBColorPreference("lowercase-invalid-name", 0.0, 0.0, 0.0, 1.0));
    }
    //
    /**
     * Tests copy(): the copy is equal but a distinct object and shares the same GUID.
     *
     * @throws Exception if anything goes wrong
     */
    @Test
    public void testCopy() throws Exception {
        RGBColorPreference tmpPref = new RGBColorPreference("Copy colour", 0.1, 0.2, 0.3, 0.4);
        RGBColorPreference tmpCopy = tmpPref.copy();
        Assertions.assertEquals(tmpPref, tmpCopy);
        Assertions.assertEquals(tmpPref.getGUID(), tmpCopy.getGUID());
        Assertions.assertNotSame(tmpPref, tmpCopy);
    }
    //
    /**
     * Tests that the RGBColorPreference(BufferedReader) reload constructor reaches the reloadVersion1000
     * malformed-component branch and throws an IOException when the persisted content line has a component count
     * other than four.
     *
     * @param aTempDir temporary directory provided by JUnit
     * @throws Exception if anything goes wrong
     */
    @Test
    public void testMalformedComponentReload(@TempDir Path aTempDir) throws Exception {
        Path tmpFile = aTempDir.resolve("malformed-rgb.txt");
        Files.write(tmpFile, List.of(
                "1.0.0.0",
                "Malformed colour",
                "00000000-0000-0000-0000-000000000000",
                "0.0:0.5:1.0"));
        BufferedReader tmpReader = new BufferedReader(new FileReader(tmpFile.toFile()));
        Assertions.assertThrows(java.io.IOException.class, () -> new RGBColorPreference(tmpReader));
        tmpReader.close();
    }
}

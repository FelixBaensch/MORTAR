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

package de.unijena.cheminf.mortar.model.util;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;


/**
 * Test class for the pure-logic MiscUtil routines: compareVersions, getTimestampInStandardFormat, getGloballyUniqueID
 * and the otherwise-unreachable private constructor of this final utility class.
 *
 * @author Felix Baensch
 * @version 1.0.0.0
 */
public class MiscUtilTest {
    //<editor-fold desc="constructor" defaultstate="collapsed">
    /**
     * Constructor that needs nothing to set up.
     */
    public MiscUtilTest() {
    }
    //</editor-fold>
    //
    //<editor-fold desc="compareVersions test methods" defaultstate="collapsed">
    /**
     * Tests whether compareVersions returns a negative integer when the first version is smaller than the second one at
     * the first position that differs (covers the {@code tmpResult < 0} return path).
     */
    @Test
    public void compareVersionsReturnsNegativeWhenFirstSmallerTest() {
        Assertions.assertTrue(MiscUtil.compareVersions("1.0.0", "1.0.1") < 0);
    }
    //
    /**
     * Tests whether compareVersions returns a positive integer when the first version is greater than the second one at
     * the first position that differs (covers the {@code tmpResult > 0} return path).
     */
    @Test
    public void compareVersionsReturnsPositiveWhenFirstGreaterTest() {
        Assertions.assertTrue(MiscUtil.compareVersions("2.0", "1.9") > 0);
    }
    //
    /**
     * Tests whether compareVersions returns zero when both version strings are identical (covers the equal-length,
     * all-positions-equal return path).
     */
    @Test
    public void compareVersionsReturnsZeroWhenEqualTest() {
        Assertions.assertEquals(0, MiscUtil.compareVersions("1.0.0", "1.0.0"));
    }
    //
    /**
     * Tests the length tiebreak: when the shorter version string is a prefix of the longer one and all shared positions
     * are equal, the lengths are compared, so "1.0" is considered smaller than "1.0.0".
     */
    @Test
    public void compareVersionsLengthTiebreakTest() {
        Assertions.assertTrue(MiscUtil.compareVersions("1.0", "1.0.0") < 0);
        Assertions.assertTrue(MiscUtil.compareVersions("1.0.0", "1.0") > 0);
    }
    //
    /**
     * Tests whether compareVersions throws an IllegalArgumentException for null, empty or blank arguments in either
     * position, covering the multi-way OR guard at the start of the method.
     */
    @Test
    public void compareVersionsInvalidArgumentsThrowTest() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> MiscUtil.compareVersions(null, "1.0.0"));
        Assertions.assertThrows(IllegalArgumentException.class, () -> MiscUtil.compareVersions("", "1.0.0"));
        Assertions.assertThrows(IllegalArgumentException.class, () -> MiscUtil.compareVersions("   ", "1.0.0"));
        Assertions.assertThrows(IllegalArgumentException.class, () -> MiscUtil.compareVersions("1.0.0", null));
        Assertions.assertThrows(IllegalArgumentException.class, () -> MiscUtil.compareVersions("1.0.0", ""));
        Assertions.assertThrows(IllegalArgumentException.class, () -> MiscUtil.compareVersions("1.0.0", "   "));
    }
    //</editor-fold>
    //
    //<editor-fold desc="getTimestampInStandardFormat test methods" defaultstate="collapsed">
    /**
     * Tests whether getTimestampInStandardFormat returns a non-null string whose length matches the documented
     * "yyyy/MM/dd - HH:mm:ss" shape (22 characters).
     */
    @Test
    public void getTimestampInStandardFormatTest() {
        String tmpTimestamp = MiscUtil.getTimestampInStandardFormat();
        Assertions.assertNotNull(tmpTimestamp);
        Assertions.assertEquals(BasicDefinitions.STANDARD_TIMESTAMP_FORMAT.length(), tmpTimestamp.length());
    }
    //</editor-fold>
    //
    //<editor-fold desc="getGloballyUniqueID test methods" defaultstate="collapsed">
    /**
     * Tests whether getGloballyUniqueID returns a non-null, non-empty string consisting only of alphanumeric characters
     * (all non-word-numeric characters, i.e. the UUID dashes, are stripped) and whether two consecutive calls produce
     * distinct IDs.
     */
    @Test
    public void getGloballyUniqueIDTest() {
        String tmpFirstID = MiscUtil.getGloballyUniqueID();
        String tmpSecondID = MiscUtil.getGloballyUniqueID();
        Assertions.assertNotNull(tmpFirstID);
        Assertions.assertFalse(tmpFirstID.isEmpty());
        Assertions.assertTrue(tmpFirstID.matches("^[A-Za-z0-9]+$"));
        Assertions.assertNotEquals(tmpFirstID, tmpSecondID);
    }
    //</editor-fold>
    //
    //<editor-fold desc="private constructor coverage" defaultstate="collapsed">
    /**
     * Tests whether the private parameter-less constructor of the MiscUtil utility class can be invoked via reflection,
     * covering the otherwise-unreachable constructor of this final utility class.
     *
     * @throws Exception if reflective instantiation fails
     */
    @Test
    public void privateConstructorTest() throws Exception {
        TestUtil.assertPrivateConstructorIsInvocable(MiscUtil.class);
    }
    //</editor-fold>
}

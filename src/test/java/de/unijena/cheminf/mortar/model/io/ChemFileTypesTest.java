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

package de.unijena.cheminf.mortar.model.io;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Locale;

/**
 * Test class for the ChemFileTypes enum. Smoke-tests the enum constants via values() and valueOf().
 *
 * @author Felix Baensch
 * @version 1.0.0.0
 */
public class ChemFileTypesTest {
    //<editor-fold desc="static initializer">
    /**
     * Sets the default locale to British English.
     */
    static {
        Locale.setDefault(Locale.of("en", "GB"));
    }
    //</editor-fold>
    //
    //<editor-fold desc="constructor">
    /**
     * Default constructor for the ChemFileTypesTest class.
     */
    public ChemFileTypesTest() {
    }
    //</editor-fold>
    //
    //<editor-fold desc="test methods">
    /**
     * Tests that the ChemFileTypes enum declares exactly the two expected constants.
     *
     * @throws Exception if anything goes wrong
     */
    @Test
    public void valuesContainsExactlyTwoConstantsTest() throws Exception {
        Assertions.assertEquals(2, ChemFileTypes.values().length);
    }
    //
    /**
     * Tests that valueOf() round-trips both enum constants by name, loading the enum's static initialiser.
     *
     * @throws Exception if anything goes wrong
     */
    @Test
    public void valueOfRoundTripsBothConstantsTest() throws Exception {
        Assertions.assertEquals(ChemFileTypes.SDF, ChemFileTypes.valueOf("SDF"));
        Assertions.assertEquals(ChemFileTypes.PDB, ChemFileTypes.valueOf("PDB"));
    }
    //
    /**
     * Tests that valueOf() throws an IllegalArgumentException for a name that is not a declared enum constant.
     *
     * @throws Exception if anything goes wrong
     */
    @Test
    public void valueOfOnUnknownNameThrowsTest() throws Exception {
        Assertions.assertThrows(IllegalArgumentException.class, () -> ChemFileTypes.valueOf("NOT_A_TYPE"));
    }
    //</editor-fold>
}

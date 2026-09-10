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
 * Test class for the DynamicSMILESFileFormat data wrapper. Covers both constructors and all getters.
 *
 * @author Felix Baensch
 * @version 1.0.0.0
 */
public class DynamicSMILESFileFormatTest {
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
     * Default constructor for the DynamicSMILESFileFormatTest class.
     */
    public DynamicSMILESFileFormatTest() {
    }
    //</editor-fold>
    //
    //<editor-fold desc="test methods">
    /**
     * Tests the single-argument constructor that defines a one-column SMILES file with no ID column and no separator
     * character. All fields other than the header flag must be set to their placeholder/default values.
     *
     * @throws Exception if anything goes wrong
     */
    @Test
    public void singleArgumentConstructorUsesPlaceholderValuesTest() throws Exception {
        DynamicSMILESFileFormat tmpFormat = new DynamicSMILESFileFormat(true);
        Assertions.assertTrue(tmpFormat.hasHeaderLine());
        Assertions.assertEquals(DynamicSMILESFileFormat.PLACEHOLDER_SEPARATOR_CHAR, tmpFormat.getSeparatorChar());
        Assertions.assertEquals(DynamicSMILESFileFormat.DEFAULT_SMILES_COLUMN_POSITION, tmpFormat.getSMILESCodeColumnPosition());
        Assertions.assertFalse(tmpFormat.hasIDColumn());
        Assertions.assertEquals(DynamicSMILESFileFormat.PLACEHOLDER_ID_COLUMN_POSITION, tmpFormat.getIDColumnPosition());
    }
    //
    /**
     * Tests the four-argument constructor with a real separator character and an ID column. All getters must return the
     * supplied values and hasIDColumn() must be true because the ID column position differs from the placeholder value.
     *
     * @throws Exception if anything goes wrong
     */
    @Test
    public void fourArgumentConstructorWithIDColumnReturnsSuppliedValuesTest() throws Exception {
        DynamicSMILESFileFormat tmpFormat = new DynamicSMILESFileFormat(false, ';', 0, 1);
        Assertions.assertFalse(tmpFormat.hasHeaderLine());
        Assertions.assertEquals(Character.valueOf(';'), tmpFormat.getSeparatorChar());
        Assertions.assertEquals(0, tmpFormat.getSMILESCodeColumnPosition());
        Assertions.assertTrue(tmpFormat.hasIDColumn());
        Assertions.assertEquals(1, tmpFormat.getIDColumnPosition());
    }
    //
    /**
     * Tests the four-argument constructor with the placeholder ID column position, which must result in hasIDColumn()
     * returning false even though the four-argument constructor is used. All other getters must return the supplied
     * values.
     *
     * @throws Exception if anything goes wrong
     */
    @Test
    public void fourArgumentConstructorWithoutIDColumnReportsNoIDColumnTest() throws Exception {
        DynamicSMILESFileFormat tmpFormat = new DynamicSMILESFileFormat(true, ',', 1,
                DynamicSMILESFileFormat.PLACEHOLDER_ID_COLUMN_POSITION);
        Assertions.assertTrue(tmpFormat.hasHeaderLine());
        Assertions.assertEquals(Character.valueOf(','), tmpFormat.getSeparatorChar());
        Assertions.assertEquals(1, tmpFormat.getSMILESCodeColumnPosition());
        Assertions.assertFalse(tmpFormat.hasIDColumn());
        Assertions.assertEquals(DynamicSMILESFileFormat.PLACEHOLDER_ID_COLUMN_POSITION, tmpFormat.getIDColumnPosition());
    }
    //</editor-fold>
}

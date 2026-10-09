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

package de.unijena.cheminf.mortar.model.data;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Direct, headless unit tests for the {@link DataModelPropertiesForTableView} enum. The enum is a pure string mapping
 * (no JavaFX, CDK, or message-bundle resolution), so the tests need no fixtures, no locale guard, and no toolkit: they
 * assert the {@code getText()} representation of every constant and the case-insensitive / null-miss behaviour of the
 * static {@code fromString(String)} lookup.
 *
 * @author Felix Baensch
 * @version 1.0.0.0
 */
public class DataModelPropertiesForTableViewTest {
    //<editor-fold desc="Constructor" defaultstate="collapsed">
    /**
     * Constructor that needs nothing to set up.
     */
    public DataModelPropertiesForTableViewTest() {
    }
    //</editor-fold>
    //
    //<editor-fold desc="getText test methods" defaultstate="collapsed">
    /**
     * Tests that {@code getText()} returns the exact string representation declared in the constructor for every one of
     * the nine enum constants, so that no constant's text mapping is left uncovered.
     */
    @Test
    public void testGetTextReturnsDeclaredTextForEveryConstant() {
        Assertions.assertEquals("name", DataModelPropertiesForTableView.NAME.getText());
        Assertions.assertEquals("uniqueSmiles", DataModelPropertiesForTableView.UNIQUE_SMILES.getText());
        Assertions.assertEquals("parentMoleculeName", DataModelPropertiesForTableView.PARENT_MOLECULE_NAME.getText());
        Assertions.assertEquals("absoluteFrequency", DataModelPropertiesForTableView.ABSOLUTE_FREQUENCY.getText());
        Assertions.assertEquals("absolutePercentage", DataModelPropertiesForTableView.ABSOLUTE_PERCENTAGE.getText());
        Assertions.assertEquals("moleculeFrequency", DataModelPropertiesForTableView.MOLECULE_FREQUENCY.getText());
        Assertions.assertEquals("moleculePercentage", DataModelPropertiesForTableView.MOLECULE_PERCENTAGE.getText());
        Assertions.assertEquals("structure", DataModelPropertiesForTableView.STRUCTURE.getText());
        Assertions.assertEquals("parentMoleculeStructure",
                DataModelPropertiesForTableView.PARENT_MOLECULE_STRUCTURE.getText());
    }
    //</editor-fold>
    //
    //<editor-fold desc="fromString test methods" defaultstate="collapsed">
    /**
     * Tests that {@code fromString(String)} resolves a constant case-insensitively against its text representation
     * (the implementation compares with {@code String.equalsIgnoreCase} on the text value, not the enum name): an
     * all-upper-case spelling of the "uniqueSmiles" text must resolve to {@code UNIQUE_SMILES}.
     */
    @Test
    public void testFromStringResolvesCaseInsensitiveHit() {
        Assertions.assertEquals(DataModelPropertiesForTableView.UNIQUE_SMILES,
                DataModelPropertiesForTableView.fromString("UNIQUESMILES"));
    }
    //
    /**
     * Tests that {@code fromString(String)} returns the exact-case text representation to the matching constant, i.e.
     * the literal text value also resolves (lower-case-first spelling), confirming the hit is driven by the text and
     * not the enum-name spelling.
     */
    @Test
    public void testFromStringResolvesExactTextHit() {
        Assertions.assertEquals(DataModelPropertiesForTableView.NAME,
                DataModelPropertiesForTableView.fromString("name"));
    }
    //
    /**
     * Tests that {@code fromString(String)} returns {@code null} when the given string matches none of the constants'
     * text representations (the no-match fall-through branch).
     */
    @Test
    public void testFromStringReturnsNullForMiss() {
        Assertions.assertNull(DataModelPropertiesForTableView.fromString("nonexistent"));
    }
    //</editor-fold>
}

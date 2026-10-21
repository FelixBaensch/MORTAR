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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Test class that pins the {@link TabNames} enum contract. This is a pure, headless lock test: it needs no booted
 * JavaFX toolkit and touches no filesystem. It exists so the enum's constant set and declaration order cannot silently
 * regress even if the transitive load path that currently exercises {@code TabNames} at runtime disappears.
 *
 * @author Felix Baensch
 * @author Jonas Schaub
 * @version 1.0.0.0
 */
public class TabNamesTest {
    //<editor-fold desc="constructor" defaultstate="collapsed">
    /**
     * Constructor that needs nothing to set up.
     */
    public TabNamesTest() {
    }
    //</editor-fold>
    //
    //<editor-fold desc="TabNames contract test methods" defaultstate="collapsed">
    /**
     * Tests the full {@link TabNames} contract: exactly three constants are declared, they appear in the fixed order
     * MOLECULES, FRAGMENTS, ITEMIZATION, and {@code valueOf} round-trips a constant name back to the matching constant.
     */
    @Test
    public void valuesOrderAndValueOfContractTest() {
        TabNames[] tmpValues = TabNames.values();
        Assertions.assertEquals(3, tmpValues.length);
        Assertions.assertEquals(TabNames.MOLECULES, tmpValues[0]);
        Assertions.assertEquals(TabNames.FRAGMENTS, tmpValues[1]);
        Assertions.assertEquals(TabNames.ITEMIZATION, tmpValues[2]);
        Assertions.assertEquals(TabNames.MOLECULES, TabNames.valueOf("MOLECULES"));
        Assertions.assertEquals(TabNames.FRAGMENTS, TabNames.valueOf("FRAGMENTS"));
        Assertions.assertEquals(TabNames.ITEMIZATION, TabNames.valueOf("ITEMIZATION"));
    }
    //</editor-fold>
}

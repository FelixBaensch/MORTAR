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

package de.unijena.cheminf.mortar.gui.util;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Test class for the deterministic GuiUtil routines.
 *
 * @author Felix Baensch
 * @version 1.0.0.0
 */
public class GuiUtilTest {
    //<editor-fold desc="constructor" defaultstate="collapsed">
    /**
     * Constructor that needs nothing to set up.
     */
    public GuiUtilTest() {
    }
    //</editor-fold>
    //
    //<editor-fold desc="calculatePageCount test methods" defaultstate="collapsed">
    /**
     * Tests that calculatePageCount returns the correct page count across all branches of its logic:
     * the empty-list zero override, a single item and other less-than-one-page sizes, an exact
     * multiple of the items per page, a size with a remainder (page bump), and a large list. Also
     * asserts the page count is always at least 1 for a valid non-empty input.
     */
    @Test
    public void calculatePageCountCoversAllBranchesTest() {
        Assertions.assertEquals(1, GuiUtil.calculatePageCount(0, 10));
        Assertions.assertEquals(1, GuiUtil.calculatePageCount(1, 10));
        Assertions.assertEquals(1, GuiUtil.calculatePageCount(5, 10));
        Assertions.assertEquals(2, GuiUtil.calculatePageCount(20, 10));
        Assertions.assertEquals(3, GuiUtil.calculatePageCount(21, 10));
        Assertions.assertEquals(1000, GuiUtil.calculatePageCount(10000, 10));
        Assertions.assertTrue(GuiUtil.calculatePageCount(7, 10) >= 1);
    }
    //</editor-fold>
}

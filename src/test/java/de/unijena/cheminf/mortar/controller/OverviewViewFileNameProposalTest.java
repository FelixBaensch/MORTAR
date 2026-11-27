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

import java.util.Locale;

/**
 * Tests the file name proposal of the exports and screenshots of the overview view, see
 * {@link OverviewViewController#createFileNameProposal(String, String)}.
 *
 * @author Manuel Schauer
 */
public class OverviewViewFileNameProposalTest {
    //<editor-fold desc="static initializer">
    /**
     * Sets the default locale to British English, which is needed for the MORTAR message bundle.
     */
    static {
        Locale.setDefault(Locale.of("en", "GB"));
    }
    //</editor-fold>
    //
    /**
     * Tests that the number of displayed structures is removed, the parts of the title are joined by underscores,
     * whitespaces are replaced by underscores, and illegal file name characters are replaced.
     */
    @Test
    public void testCreateFileNameProposal() {
        Assertions.assertEquals("Fragments_Ertl_algorithm_Overview",
                OverviewViewController.createFileNameProposal("Fragments - Ertl algorithm - Overview - 120 fragments", "Fallback"));
        Assertions.assertEquals("Molecules_biofacquim-06-2025_Overview",
                OverviewViewController.createFileNameProposal("Molecules - biofacquim-06-2025 - Overview - 583 molecules", "Fallback"));
        Assertions.assertEquals("Parent_Structures_Overview",
                OverviewViewController.createFileNameProposal("Parent Structures - Overview - 5 molecules", "Fallback"));
        Assertions.assertEquals("Fragments_a_b_Overview",
                OverviewViewController.createFileNameProposal("Fragments - a/b - Overview - 3 fragments", "Fallback"));
        Assertions.assertEquals("Overview", OverviewViewController.createFileNameProposal("Overview", "Fallback"));
    }
    //
    /**
     * Tests that the fallback is returned if no proposal can be derived from the title.
     */
    @Test
    public void testCreateFileNameProposalFallback() {
        Assertions.assertEquals("Fallback", OverviewViewController.createFileNameProposal(null, "Fallback"));
        Assertions.assertEquals("Fallback", OverviewViewController.createFileNameProposal("", "Fallback"));
        Assertions.assertEquals("Fallback", OverviewViewController.createFileNameProposal("   ", "Fallback"));
    }
}

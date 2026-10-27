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

package de.unijena.cheminf.mortar.model.fragmentation;

import de.unijena.cheminf.mortar.model.data.FragmentDataModel;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Tests the sorting of fragments and the assignment of fragment IDs of the {@link FragmentationService} class.
 *
 * @author Manuel Schauer
 */
public class FragmentationServiceFragmentIDTest {
    //<editor-fold desc="static initializer">
    /**
     * Sets the default locale to British English.
     */
    static {
        Locale.setDefault(Locale.of("en", "GB"));
    }
    //</editor-fold>
    //
    /**
     * Tests that fragments are sorted by molecule frequency (descending, ties by SMILES) and assigned IDs consisting of
     * the cleaned fragmentation name and their rank, independent of the order of the given map.
     */
    @Test
    public void testSortFragmentsAndAssignIDs() {
        Map<String, FragmentDataModel> tmpFragments = new HashMap<>();
        for (String tmpSmiles : new String[]{"CO", "CCO", "c1ccccc1", "CN"}) {
            tmpFragments.put(tmpSmiles, new FragmentDataModel(tmpSmiles, null, new HashMap<>()));
        }
        tmpFragments.get("CO").setMoleculeFrequency(2);
        tmpFragments.get("CCO").setMoleculeFrequency(5);
        tmpFragments.get("c1ccccc1").setMoleculeFrequency(2);
        tmpFragments.get("CN").setMoleculeFrequency(1);
        Map<String, FragmentDataModel> tmpSorted = FragmentationService.sortFragmentsAndAssignIDs(tmpFragments, "Ertl algorithm_1");
        List<String> tmpSortedSmiles = new ArrayList<>(tmpSorted.keySet());
        Assertions.assertEquals(List.of("CCO", "CO", "c1ccccc1", "CN"), tmpSortedSmiles);
        Assertions.assertEquals("Ertl_algorithm_1_1", tmpSorted.get("CCO").getId());
        Assertions.assertEquals("Ertl_algorithm_1_2", tmpSorted.get("CO").getId());
        Assertions.assertEquals("Ertl_algorithm_1_3", tmpSorted.get("c1ccccc1").getId());
        Assertions.assertEquals("Ertl_algorithm_1_4", tmpSorted.get("CN").getId());
        //fragments without name return their ID as name
        Assertions.assertEquals("Ertl_algorithm_1_1", tmpSorted.get("CCO").getName());
        Assertions.assertNull(FragmentationService.sortFragmentsAndAssignIDs(null, "Ertl algorithm"));
    }
}

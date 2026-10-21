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
import de.unijena.cheminf.mortar.model.data.MoleculeDataModel;
import de.unijena.cheminf.mortar.model.fragmentation.algorithm.ErtlFunctionalGroupsFinderFragmenter;
import de.unijena.cheminf.mortar.model.fragmentation.algorithm.IMoleculeFragmenter;
import de.unijena.cheminf.mortar.model.fragmentation.algorithm.ScaffoldGeneratorFragmenter;
import de.unijena.cheminf.mortar.model.util.ChemUtil;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Test class for the keep-last-fragment branch of {@link FragmentationService#startPipelineFragmentation}.
 *
 * @author Felix Baensch
 */
class PipelineKeepLastFragmentTest {
    /**
     * Constructor that sets the default locale to british english, which is needed because the fragmenters load their
     * settings tooltips from the message bundle.
     */
    public PipelineKeepLastFragmentTest() {
        Locale.setDefault(Locale.of("en", "GB"));
    }
    //
    /**
     * Runs an Ertl-then-Scaffold pipeline with the keep-last-fragment flag set. The Scaffold Generator yields nothing for
     * the acyclic Ertl fragments, so those parent fragments are not in the second stage's results; the flag must carry
     * them forward as they are instead of adding null to the molecule's fragment list (issue #238).
     *
     * @throws Exception if anything goes wrong
     */
    @Test
    void keepLastFragmentCarriesParentForwardInsteadOfNullTest() throws Exception {
        FragmentationService tmpService = new FragmentationService();
        tmpService.setPipelineFragmenter(new IMoleculeFragmenter[] {
                PipelineKeepLastFragmentTest.fragmenterCopy(tmpService, ErtlFunctionalGroupsFinderFragmenter.ALGORITHM_NAME),
                PipelineKeepLastFragmentTest.fragmenterCopy(tmpService, ScaffoldGeneratorFragmenter.ALGORITHM_NAME)
        });
        tmpService.setPipeliningFragmentationName("KeepLastFragmentPipeline");
        List<MoleculeDataModel> tmpMolecules = new ArrayList<>(3);
        //non-aromatic input, the Scaffold Generator fails on un-kekulized aromatic input
        for (String tmpSmiles : List.of("O=C(O)CCC1CCCCC1", "OCCCC1CCC1", "O=C(O)CCCC")) {
            tmpMolecules.add(new MoleculeDataModel(ChemUtil.parseSmilesToAtomContainer(tmpSmiles, false, false), false));
        }
        tmpService.startPipelineFragmentation(tmpMolecules, 1, false, true);
        String tmpFragmentationName = tmpService.getCurrentFragmentationName();
        for (MoleculeDataModel tmpMolecule : tmpMolecules) {
            List<FragmentDataModel> tmpFragments = tmpMolecule.getFragmentsOfSpecificFragmentation(tmpFragmentationName);
            Assertions.assertFalse(tmpFragments.isEmpty(), tmpMolecule.getUniqueSmiles());
            Assertions.assertFalse(tmpFragments.contains(null), tmpMolecule.getUniqueSmiles());
            for (FragmentDataModel tmpFragment : tmpFragments) {
                Assertions.assertTrue(tmpService.getFragments().containsKey(tmpFragment.getUniqueSmiles()),
                        tmpFragment.getUniqueSmiles());
            }
        }
    }
    //
    /**
     * Returns a fresh copy of the registered fragmenter carrying the given algorithm name.
     *
     * @param aService the service whose registered fragmenters are searched
     * @param anAlgorithmName the algorithm name to look up
     * @return an independent copy of that fragmenter
     */
    private static IMoleculeFragmenter fragmenterCopy(FragmentationService aService, String anAlgorithmName) {
        for (IMoleculeFragmenter tmpFragmenter : aService.getFragmenters()) {
            if (tmpFragmenter.getFragmentationAlgorithmName().equals(anAlgorithmName)) {
                return tmpFragmenter.copy();
            }
        }
        throw new IllegalStateException("no fragmenter is registered under the algorithm name " + anAlgorithmName);
    }
}

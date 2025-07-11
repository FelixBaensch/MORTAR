/*
 * MORTAR - MOlecule fRagmenTAtion fRamework
 * Copyright (C) 2025  Felix Baensch, Jonas Schaub (felix.j.baensch@gmail.com, jonas.schaub@uni-jena.de)
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

package de.unijena.cheminf.mortar.model.fragmentation.algorithm;

import de.unijena.cheminf.mortar.model.util.ChemUtil;

import javafx.beans.property.Property;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.silent.SilentChemObjectBuilder;
import org.openscience.cdk.smiles.SmilesParser;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Class to test the correct working of
 * {@link de.unijena.cheminf.mortar.model.fragmentation.algorithm.ConjugatedPiSystemFragmenter}.
 *
 * @author Maximilian Rottmann
 * @version 1.1.1.0
 */
public class ConjugatedPiSystemFragmenterTest {

    /**
     * Constructor that sets the default locale to british english, which is needed for correct functioning of the
     * fragmenter as the settings tooltips are imported from the message.properties file.
     */
    public ConjugatedPiSystemFragmenterTest() {
        Locale.setDefault(new Locale("en", "GB"));
    }

    /**
     * Tests correct instantiation and basic settings retrieval.
     *
     * @throws Exception if anything goes wrong
     */
    @Test
    public void settingsTest() throws Exception {
        ConjugatedPiSystemFragmenter tmpFragmenter = new ConjugatedPiSystemFragmenter();
        List<String> tmpCheckList = new ArrayList<>();
        List<String> tmpExpectList = new ArrayList<>();
        tmpExpectList.add("Fragment saturation setting");
        for (Property tmpSetting: tmpFragmenter.settingsProperties()) {
            tmpCheckList.add(tmpSetting.getName());
        }
        Assertions.assertTrue(this.compareListsIgnoringOrder((ArrayList) tmpExpectList, (ArrayList) tmpCheckList));
    }

    /**
     *  Method to test a default conjugated pi system fragmentation on the natural product CNP0421388
     *  from the Coconut Database (@see <a href="https://coconut.naturalproducts.net/compound/coconut_id/CNP0421388">...</a>).
     *
     * @throws Exception if anything goes wrong
     */
    @Test
    public void defaultFragmentationTest() throws Exception {
        SmilesParser tmpParser = new SmilesParser(SilentChemObjectBuilder.getInstance());
        //test structure: C=C\C=C\C(CC\C=C/c1ccccc1)c1ccccc1
        IAtomContainer tmpTestStructureAC = tmpParser.parseSmiles("C=C\\C=C\\C(CC\\C=C/c1ccccc1)c1ccccc1");
        ConjugatedPiSystemFragmenter tmpCPSF = this.getDefaultCPSFInstance(tmpTestStructureAC, false, false, true);
        List<String> tmpResultSMILESList = this.generateSMILESFromACList(tmpCPSF.fragmentMolecule(tmpTestStructureAC));
        List<String> tmpExpectedSMILESList = new ArrayList<>(3);
        tmpExpectedSMILESList.add("C=CC=C");
        tmpExpectedSMILESList.add("C=Cc1ccccc1");
        tmpExpectedSMILESList.add("c1ccccc1");
        Assertions.assertTrue(this.compareListsIgnoringOrder(new ArrayList<>(tmpResultSMILESList),
                new ArrayList<>(tmpExpectedSMILESList)));
    }

    /**
     * Utility method returning a default instance of the ConjugatedPiSystemFragmenter for a given molecule.
     *
     * @param aMolecule to fragment
     * @param aShouldBeFilteredStatement decide if molecule should be filtered
     * @param aShouldBePreprocessedStatement decide if molecule needs preprocessing
     * @param aCanBeFragmentedStatement decide if molecule can be fragmented
     * @return ConjugatedPiSystemFragmenter instance
     */
    private ConjugatedPiSystemFragmenter getDefaultCPSFInstance(IAtomContainer aMolecule,
                                                                boolean aShouldBeFilteredStatement,
                                                                boolean aShouldBePreprocessedStatement,
                                                                boolean aCanBeFragmentedStatement) {
        ConjugatedPiSystemFragmenter tmpCPSF = new ConjugatedPiSystemFragmenter();
        tmpCPSF.setFragmentSaturationSetting(IMoleculeFragmenter.FragmentSaturationOption.HYDROGEN_SATURATION);
        //assertions for non-set-able pre-fragmentation tasks
        if (aShouldBeFilteredStatement) {
            Assertions.assertTrue(tmpCPSF.shouldBeFiltered(aMolecule));
        } else {
            Assertions.assertFalse(tmpCPSF.shouldBeFiltered(aMolecule));
        }
        if (aShouldBePreprocessedStatement) {
            Assertions.assertTrue(tmpCPSF.shouldBePreprocessed(aMolecule));
        } else {
            Assertions.assertFalse(tmpCPSF.shouldBePreprocessed(aMolecule));
        }
        if (aCanBeFragmentedStatement) {
            Assertions.assertTrue(tmpCPSF.canBeFragmented(aMolecule));
        } else {
            Assertions.assertFalse(tmpCPSF.canBeFragmented(aMolecule));
        }
        return tmpCPSF;
    }

    /**
     * Utility method to generate a list of SMILES Strings corresponding to a list of IAtomContainer.
     *
     * @param anAtomContainerList to generate the SMILES of
     * @return list of SMILES Strings
     */
    private List<String> generateSMILESFromACList(List<IAtomContainer> anAtomContainerList) {
        List<String> tmpReturnSmilesList = new ArrayList<>(anAtomContainerList.size());
        for (IAtomContainer tmpAC: anAtomContainerList) {
            tmpReturnSmilesList.add(ChemUtil.createUniqueSmiles(tmpAC, false));
        }
        return tmpReturnSmilesList;
    }

    /**
     * Utility method comparing two list while ignoring their elements order.
     *
     * @param aList1 to compare
     * @param aList2 to compare
     * @return boolean whether lists contain equal elements or not
     */
    private boolean compareListsIgnoringOrder(ArrayList aList1, ArrayList aList2) {
        if (aList1 == null || aList2 == null) {
            return false;
        }
        if (aList1.size() != aList2.size()) {
            return false;
        }
        for (Object o : aList1) {
            aList2.remove(o);
        }
        return aList2.isEmpty();
    }
}

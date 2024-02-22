/*
 * MORTAR - MOlecule fRagmenTAtion fRamework
 * Copyright (C) 2024  Felix Baensch, Jonas Schaub (felix.baensch@w-hs.de, jonas.schaub@uni-jena.de)
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

import javafx.beans.property.Property;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.silent.SilentChemObjectBuilder;
import org.openscience.cdk.smiles.SmiFlavor;
import org.openscience.cdk.smiles.SmilesGenerator;
import org.openscience.cdk.smiles.SmilesParser;

import java.util.List;
import java.util.Locale;

/**
 * Class to test the correct working of
 * {@link de.unijena.cheminf.mortar.model.fragmentation.algorithm.SugarRemovalUtilityFragmenter}.
 *
 * @author Jonas Schaub
 * @version 1.0.0.0
 */
public class SugarRemovalUtilityFragmenterTest {

    /**
     * Constructor that sets the default locale to british english, which is important for the correct functioning of the
     * fragmenter because the settings tooltips are imported from the message.properties file.
     */
    public SugarRemovalUtilityFragmenterTest() {
        Locale.setDefault(new Locale("en", "GB"));
    }
    //
    /**
     * Tests instantiation and basic settings retrieval.
     *
     * @throws Exception if anything goes wrong
     */
    @Test
    public void basicTest() throws Exception {
        SugarRemovalUtilityFragmenter tmpFragmenter = new SugarRemovalUtilityFragmenter();
        System.out.println(tmpFragmenter.getFragmentationAlgorithmName());
        System.out.println(tmpFragmenter.getSugarTypeToRemoveSetting());
        for (Property tmpSetting : tmpFragmenter.settingsProperties()) {
            System.out.println(tmpSetting.getName());
        }
    }
    //
    /**
     * Does a test fragmentation on the COCONUT natural product CNP0151033 and prints the results.
     *
     * @throws Exception if anything goes wrong
     */
    @Test
    public void fragmentationTest() throws Exception {
        SmilesParser tmpSmiPar = new SmilesParser(SilentChemObjectBuilder.getInstance());
        SmilesGenerator tmpSmiGen = new SmilesGenerator((SmiFlavor.Canonical));
        IAtomContainer tmpOriginalMolecule;
        List<IAtomContainer> tmpFragmentList;
        String tmpSmilesCode;
        SugarRemovalUtilityFragmenter tmpSRUFragmenter = new SugarRemovalUtilityFragmenter();
        tmpSRUFragmenter.setReturnedFragmentsSetting(SugarRemovalUtilityFragmenter.SRUFragmenterReturnedFragmentsOption.ALL_FRAGMENTS);
        tmpOriginalMolecule = tmpSmiPar.parseSmiles(
                //CNP0151033
                "O=C(OC1C(OCC2=COC(OC(=O)CC(C)C)C3C2CC(O)C3(O)COC(=O)C)OC(CO)C(O)C1O)C=CC4=CC=C(O)C=C4");
        Assertions.assertFalse(tmpSRUFragmenter.shouldBeFiltered(tmpOriginalMolecule));
        Assertions.assertFalse(tmpSRUFragmenter.shouldBePreprocessed(tmpOriginalMolecule));
        Assertions.assertTrue(tmpSRUFragmenter.canBeFragmented(tmpOriginalMolecule));
        tmpFragmentList = tmpSRUFragmenter.fragmentMolecule(tmpOriginalMolecule);
        tmpSmilesCode = tmpSmiGen.create(tmpFragmentList.get(0));
        System.out.println(tmpSmilesCode + " " + tmpFragmentList.get(0).getProperty(
                IMoleculeFragmenter.FRAGMENT_CATEGORY_PROPERTY_KEY));
        //The sugar ring is not terminal and should not be removed, so the molecule remains unchanged
        Assertions.assertEquals("O=C(OC1C(OCC2=COC(OC(=O)CC(C)C)C3C2CC(O)C3(O)COC(=O)C)OC(CO)C(O)C1O)C=CC4=CC=C(O)C=C4", tmpSmilesCode);
        tmpSRUFragmenter.setRemoveOnlyTerminalSugarsSetting(false);
        tmpFragmentList = tmpSRUFragmenter.fragmentMolecule(tmpOriginalMolecule);
        tmpSmilesCode = tmpSmiGen.create(tmpFragmentList.get(0));
        System.out.println(tmpSmilesCode + " " + tmpFragmentList.get(0).getProperty(
                IMoleculeFragmenter.FRAGMENT_CATEGORY_PROPERTY_KEY));
        //Now that all sugars are removed, the sugar ring is removed and an unconnected structure remains
        // the unconnected fragments are separated into different atom containers in the returned list
        Assertions.assertEquals("O=C(OCC1(O)C(O)CC2C(=COC(OC(=O)CC(C)C)C21)CO)C", tmpSmilesCode);
        Assertions.assertEquals("O=C(O)C=CC1=CC=C(O)C=C1", tmpSmiGen.create(tmpFragmentList.get(1)));
        System.out.println(tmpSmiGen.create(tmpFragmentList.get(2)) + " " + tmpFragmentList.get(2).getProperty(
                IMoleculeFragmenter.FRAGMENT_CATEGORY_PROPERTY_KEY));
        tmpSRUFragmenter.setRemoveOnlyTerminalSugarsSetting(true);
        Assertions.assertFalse(tmpSRUFragmenter.shouldBeFiltered(tmpFragmentList.get(0)));
        Assertions.assertFalse(tmpSRUFragmenter.shouldBePreprocessed(tmpFragmentList.get(0)));
        Assertions.assertTrue(tmpSRUFragmenter.canBeFragmented(tmpFragmentList.get(0)));
        IAtomContainer tmpAfterPreprocessing = tmpSRUFragmenter.applyPreprocessing(tmpFragmentList.get(0));
        Assertions.assertTrue(tmpSRUFragmenter.canBeFragmented(tmpAfterPreprocessing));
    }
}

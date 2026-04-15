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
 * Class to test the correct workings of
 * {@link de.unijena.cheminf.mortar.model.fragmentation.algorithm.CDKExhaustiveFragmenter}.
 *
 * @author Tom Weiß
 * @version 1.0.0.0
 */
public class CDKExhaustiveFragmenterTest {

    /**
     * Constructor that sets the default locale to British English, which is important for the correct functioning of the
     * fragmenter because the settings tooltips are imported from the message.properties file.
     */
    CDKExhaustiveFragmenterTest() {
        Locale.setDefault(Locale.of("en", "GB"));
    }
    //
    /**
     * Tests instantiation and basic settings retrieval.
     *
     * @throws Exception if anything goes wrong
     */
    @Test
    public void basicTest() throws Exception {
        CDKExhaustiveFragmenter tmpFragmenter = new CDKExhaustiveFragmenter();
        Assertions.assertDoesNotThrow(tmpFragmenter::getFragmentationAlgorithmName);
        Assertions.assertDoesNotThrow(tmpFragmenter::getFragmentationAlgorithmDisplayName);
        for (Property<?> tmpSetting : tmpFragmenter.settingsProperties()) {
            Assertions.assertDoesNotThrow(tmpSetting::getName);
        }
    }
    //
    /**
     * Does a test fragmentation on the COCONUT natural product CNP0151033.
     *
     * @throws Exception if anything goes wrong
     */
    @Test
    public void fragmentationTest() throws Exception {
        SmilesParser tmpSmiPar = new SmilesParser(SilentChemObjectBuilder.getInstance());
        SmilesGenerator tmpSmiGen = new SmilesGenerator((SmiFlavor.Canonical));
        IAtomContainer tmpOriginalMolecule;
        List<IAtomContainer> tmpFragmentList;
        CDKExhaustiveFragmenter tmpFragmenter = new CDKExhaustiveFragmenter();
        tmpOriginalMolecule = tmpSmiPar.parseSmiles(
                //CNP0151033
                "O=C(OC1C(OCC2=COC(OC(=O)CC(C)C)C3C2CC(O)C3(O)COC(=O)C)OC(CO)C(O)C1O)C=CC4=CC=C(O)C=C4");
        Assertions.assertFalse(tmpFragmenter.shouldBeFiltered(tmpOriginalMolecule));
        Assertions.assertFalse(tmpFragmenter.shouldBePreprocessed(tmpOriginalMolecule));
        Assertions.assertTrue(tmpFragmenter.canBeFragmented(tmpOriginalMolecule));
        Assertions.assertDoesNotThrow(tmpFragmenter::minimumFragmentSizeSettingProperty);
        Assertions.assertDoesNotThrow(tmpFragmenter::inclusiveSplittableBondsThresholdSettingProperty);
        Assertions.assertDoesNotThrow(tmpFragmenter::getInclusiveMaxTreeDepthSetting);
        Assertions.assertDoesNotThrow(tmpFragmenter::getSaturationSetting);
        Assertions.assertDoesNotThrow(tmpFragmenter::preserveStereoSettingProperty);
        Assertions.assertDoesNotThrow(tmpFragmenter::getMinimumFragmentSize);
        tmpFragmentList = tmpFragmenter.fragmentMolecule(tmpOriginalMolecule);
        for (IAtomContainer tmpFragment : tmpFragmentList) {
            Assertions.assertDoesNotThrow(() -> tmpSmiGen.create(tmpFragment));
        }
        int tmpMinimumFragmentSize = 8;
        tmpFragmenter.setMinimumFragmentSize(tmpMinimumFragmentSize);
        Assertions.assertEquals(tmpMinimumFragmentSize, tmpFragmenter.getMinimumFragmentSize());
    }
    //
    /**
     * Does a test fragmentation to test stereochemistry functions on
     * the COCONUT natural product CNP0381655.1.
     *
     * @throws Exception if anything goes wrong
     */
    @Test
    public void fragmentationTestStereo() throws Exception {
        SmilesParser tmpSmiPar = new SmilesParser(SilentChemObjectBuilder.getInstance());
        SmilesGenerator tmpSmiGen = new SmilesGenerator((SmiFlavor.Canonical));
        IAtomContainer tmpOriginalMolecule;
        List<IAtomContainer> tmpFragmentList;
        CDKExhaustiveFragmenter tmpFragmenter = new CDKExhaustiveFragmenter();
        tmpOriginalMolecule = tmpSmiPar.parseSmiles(
                // CNP0381655.1
                "CCCCC[C@H]1O[C@@H]1/C=C/C=O");

        Assertions.assertFalse(tmpFragmenter.shouldBeFiltered(tmpOriginalMolecule));
        // setting preserve stereo manually to false to check that preprocessing works correctly
        // as molecules with stereo information should be preprocessed if this setting is false
        tmpFragmenter.preserveStereoSettingProperty().set(false);
        Assertions.assertTrue(tmpFragmenter.shouldBePreprocessed(tmpOriginalMolecule));
        Assertions.assertEquals("O=CC=CC1OC1CCCCC", tmpSmiGen.create(tmpFragmenter.applyPreprocessing(tmpOriginalMolecule)));
        Assertions.assertFalse(tmpFragmenter.canBeFragmented(tmpOriginalMolecule));
        IAtomContainer tmpPreprocessedMol = tmpFragmenter.applyPreprocessing(tmpOriginalMolecule);
        tmpFragmentList = tmpFragmenter.fragmentMolecule(tmpPreprocessedMol);
        for (IAtomContainer tmpFragment : tmpFragmentList) {
            Assertions.assertDoesNotThrow(() -> tmpSmiGen.create(tmpFragment));
        }
    }

}

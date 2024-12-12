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
import org.openscience.cdk.io.MDLV3000Reader;
import org.openscience.cdk.silent.SilentChemObjectBuilder;
import org.openscience.cdk.smiles.SmiFlavor;
import org.openscience.cdk.smiles.SmilesGenerator;

import java.io.FileReader;
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
    public void basicTest() throws Exception {
        ConjugatedPiSystemFragmenter tmpFragmenter = new ConjugatedPiSystemFragmenter();
        List<String> tmpCheckList = new ArrayList<>();
        List<String> tmpExpectList = new ArrayList<>();
        tmpExpectList.add("Fragment saturation setting");
        for (Property tmpSetting: tmpFragmenter.settingsProperties()) {
            tmpCheckList.add(tmpSetting.getName());
        }
        Assertions.assertLinesMatch(tmpExpectList, tmpCheckList);
    }

    /**
     *  Method to test a default conjugated pi system fragmentation on the natural product CNP0421388
     *  from the Coconut Database (@see <a href="https://coconut.naturalproducts.net/compound/coconut_id/CNP0421388">...</a>).
     *
     * @throws Exception if anything goes wrong
     */
    @Test
    public void defaultFragmentationTest() throws Exception {
        try (MDLV3000Reader tmpMDLReader = new MDLV3000Reader(new FileReader("src/test/resources/de.unijena.cheminf.mortar.model.fragmentation.algorithm.ASF/TestCPSFStructure.mol"))) {
            IAtomContainer tmpOriginalMolecule = tmpMDLReader.read(SilentChemObjectBuilder.getInstance().newAtomContainer());
            ConjugatedPiSystemFragmenter tmpFragmenter = new ConjugatedPiSystemFragmenter();
            tmpFragmenter.setFragmentSaturationSetting(ConjugatedPiSystemFragmenter.FRAGMENT_SATURATION_OPTION_DEFAULT);
            Assertions.assertFalse(tmpFragmenter.shouldBeFiltered(tmpOriginalMolecule));
            Assertions.assertFalse(tmpFragmenter.shouldBePreprocessed(tmpOriginalMolecule));
            Assertions.assertTrue(tmpFragmenter.canBeFragmented(tmpOriginalMolecule));
            List<IAtomContainer> tmpFragmentList;
            tmpFragmentList = tmpFragmenter.fragmentMolecule(tmpOriginalMolecule);
            SmilesGenerator tmpGenerator = new SmilesGenerator(SmiFlavor.Canonical);
            for (IAtomContainer tmpFragment : tmpFragmentList) {
                System.out.println(tmpGenerator.create(tmpFragment) + " "
                        + tmpFragment.getProperty(IMoleculeFragmenter.FRAGMENT_CATEGORY_PROPERTY_KEY));
            }
        }
    }
}

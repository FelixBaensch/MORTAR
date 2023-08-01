/*
 * MORTAR - MOlecule fRagmenTAtion fRamework
 * Copyright (C) 2023  Felix Baensch, Jonas Schaub (felix.baensch@w-hs.de, jonas.schaub@uni-jena.de)
 *
 * Source code is available at <https://github.com/FelixBaensch/MORTAR>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package de.unijena.cheminf.mortar.model.fragmentation.algorithm;

import javafx.beans.property.Property;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.openscience.cdk.AtomContainer;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.io.MDLV3000Reader;
import org.openscience.cdk.smiles.SmiFlavor;
import org.openscience.cdk.smiles.SmilesGenerator;

import java.io.File;
import java.io.FileReader;
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
        System.out.println(tmpFragmenter.getFragmentationAlgorithmName());
        for (Property tmpSetting: tmpFragmenter.settingsProperties()) {
            System.out.println(tmpSetting.getName());
        }
    }

    /**
     *  Method to test a default conjugated pi system fragmentation on the natural product CNP0421388
     *  from the Coconut Database (@see <a href="https://coconut.naturalproducts.net/compound/coconut_id/CNP0421388">...</a>).
     *
     * @throws Exception if anything goes wrong
     */
    @Test
    public void defaultFragmentationTest() throws Exception {
        File tmpFile = new File("src/test/resources/TestCPSFStructure.mol");
        try (MDLV3000Reader tmpMDLReader = new MDLV3000Reader(new FileReader(tmpFile))) {
            IAtomContainer tmpOriginalMolecule = tmpMDLReader.read(new AtomContainer());
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

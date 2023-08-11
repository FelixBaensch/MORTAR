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
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.io.MDLV2000Reader;
import org.openscience.cdk.silent.SilentChemObjectBuilder;
import org.openscience.cdk.smiles.SmiFlavor;
import org.openscience.cdk.smiles.SmilesGenerator;

import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Class to test the correct working of
 * {@link de.unijena.cheminf.mortar.model.fragmentation.algorithm.AlkylStructureFragmenter}.
 *
 * @author Maximilian Rottmann
 * @version 1.1.1.0
 */
public class AlkylStructureFragmenterTest {

    /**
     * Constructor that sets the default locale to british english, which is needed for correct functioning of the
     * fragmenter as the settings tooltips are imported from the message.properties file.
     */
    public AlkylStructureFragmenterTest() {
        Locale.setDefault(new Locale("en", "GB"));
    }

    /**
     * Tests correct instantiation and basic settings retrieval.
     *
     * @throws Exception if anything goes wrong
     */
    @Test
    public void basicTest() throws Exception {
        AlkylStructureFragmenter tmpFragmenter = new AlkylStructureFragmenter();
        List<String> tmpCheckList = new ArrayList<>();
        List<String> tmpExpectList = new ArrayList<>();
        tmpExpectList.add("Fragment saturation setting");
        tmpExpectList.add("Carbon side chains maximum length setting");
        for (Property tmpSetting: tmpFragmenter.settingsProperties()) {
            tmpCheckList.add(tmpSetting.getName());
        }
        Assertions.assertLinesMatch(tmpExpectList, tmpCheckList);
    }

    /**
     *  Method to test a default alkyl structure fragmentation on a concept molecule.
     *
     * @throws Exception if anything goes wrong
     */
    @Test
    public void defaultFragmentationTest() throws Exception {
        try (MDLV2000Reader tmpMDLReader = new MDLV2000Reader(new FileReader("src/test/resources/TestASFStructure1.mol"))) {
            IAtomContainer tmpOriginalMolecule = tmpMDLReader.read(SilentChemObjectBuilder.getInstance().newAtomContainer());
            AlkylStructureFragmenter tmpFragmenter = new AlkylStructureFragmenter();
            tmpFragmenter.setFragmentSaturationSetting(AlkylStructureFragmenter.FRAGMENT_SATURATION_OPTION_DEFAULT);
            Assertions.assertFalse(tmpFragmenter.shouldBeFiltered(tmpOriginalMolecule));
            Assertions.assertFalse(tmpFragmenter.shouldBePreprocessed(tmpOriginalMolecule));
            Assertions.assertTrue(tmpFragmenter.canBeFragmented(tmpOriginalMolecule));
            List<IAtomContainer> tmpFragmentList;
            tmpFragmentList = tmpFragmenter.fragmentMolecule(tmpOriginalMolecule);
            SmilesGenerator tmpGenerator = new SmilesGenerator(SmiFlavor.Canonical);
            List<String> tmpCheckList = new ArrayList<>();
            List<String> tmpExpectedList = new ArrayList<>();
            tmpExpectedList.add("C=CC=C1C=2C=CC=CC2CCC1");
            tmpExpectedList.add("CCCC");
            tmpExpectedList.add("CC(C)(C)C");
            for (IAtomContainer tmpFragment : tmpFragmentList) {
                String tmpString = tmpGenerator.create(tmpFragment);
                System.out.println(tmpString);
                tmpCheckList.add(tmpString);
            }
            //Assertions.assertLinesMatch(tmpExpectedList, tmpCheckList);
        }
    }

}

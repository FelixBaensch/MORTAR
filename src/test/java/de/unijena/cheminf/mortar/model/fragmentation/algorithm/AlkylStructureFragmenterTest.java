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
import org.openscience.cdk.AtomContainerSet;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.interfaces.IAtomContainerSet;
import org.openscience.cdk.io.IChemObjectReader;
import org.openscience.cdk.io.MDLV3000Reader;
import org.openscience.cdk.io.iterator.IteratingSDFReader;
import org.openscience.cdk.silent.SilentChemObjectBuilder;
import org.openscience.cdk.smiles.SmiFlavor;
import org.openscience.cdk.smiles.SmilesGenerator;
import org.openscience.cdk.tools.manipulator.AtomContainerManipulator;

import java.io.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

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
        try (MDLV3000Reader tmpMDLReader = new MDLV3000Reader(new FileReader("src/test/resources/TestASFStructure1.mol"), IChemObjectReader.Mode.RELAXED)) {
            IAtomContainer tmpOriginalMolecule = tmpMDLReader.read(SilentChemObjectBuilder.getInstance().newAtomContainer());
            AtomContainerManipulator.percieveAtomTypesAndConfigureAtoms(tmpOriginalMolecule);
            File aFile = new File("src/test/resources/ASF_Test_COCONUT_subset_sample.sdf");
            IteratingSDFReader tmpSDFReader = new IteratingSDFReader(new FileInputStream(aFile),
                    SilentChemObjectBuilder.getInstance());
            IAtomContainerSet tmpOriginalMoleculeSet = new AtomContainerSet();
            while (tmpSDFReader.hasNext()) {
                IAtomContainer tmpAtomContainer = tmpSDFReader.next();
                if (tmpAtomContainer != null) {
                    AtomContainerManipulator.percieveAtomTypesAndConfigureAtoms(tmpAtomContainer);
                    tmpOriginalMoleculeSet.addAtomContainer(tmpAtomContainer);
                    /*
                    for (IAtom tmpAtom: tmpAtomContainer.atoms()) {
                        if (tmpAtom.getMaxBondOrder() == null) {
                            System.out.println(tmpAtom.getIndex());
                        }
                    }
                    */
                }
            }

            //IAtomContainerSet tmpAtomContainerSet = new AtomContainerSet();
            //IteratingSDFReader tmpIterSDFReader = new IteratingSDFReader(new FileReader("testfile"))

            AlkylStructureFragmenter tmpFragmenter = new AlkylStructureFragmenter();
            tmpFragmenter.setFragmentSaturationSetting(AlkylStructureFragmenter.FRAGMENT_SATURATION_OPTION_DEFAULT);
            tmpFragmenter.setMaxChainLengthSetting(AlkylStructureFragmenter.MAX_CHAIN_LENGTH_SETTING_DEFAULT);
            Assertions.assertFalse(tmpFragmenter.shouldBeFiltered(tmpOriginalMolecule));
            Assertions.assertFalse(tmpFragmenter.shouldBePreprocessed(tmpOriginalMolecule));
            Assertions.assertTrue(tmpFragmenter.canBeFragmented(tmpOriginalMolecule));
            List<IAtomContainer> tmpFragmentList;
            tmpFragmentList = tmpFragmenter.fragmentMolecule(tmpOriginalMolecule);
            SmilesGenerator tmpGenerator = new SmilesGenerator(SmiFlavor.Canonical);
            List<String> tmpCheckList = new ArrayList<>();

            //list of expected molecules after fragmentation
            List<String> tmpExpectedList = new ArrayList<>();
            tmpExpectedList.add("C=CC=C1C=2C=CC=CC2CCC1");
            tmpExpectedList.add("*C(*)*");
            tmpExpectedList.add("*C(*)(*)*");
            tmpExpectedList.add("C");
            tmpExpectedList.add("CC");
            tmpExpectedList.add("C");
            tmpExpectedList.add("C");
            tmpExpectedList.add("C");
            tmpExpectedList.add("C");

            /*
            HashSet<String> tmpExpectedHashSet = new HashSet<>(20);
            tmpExpectedHashSet.add("C=CC=C1C=2C=CC=CC2CCC1");
            tmpExpectedHashSet.add("*C(*)*");
            tmpExpectedHashSet.add("*C(*)(*)*");
            tmpExpectedHashSet.add("C");
            tmpExpectedHashSet.add("CC");
            tmpExpectedHashSet.add("C");
            tmpExpectedHashSet.add("C");
            tmpExpectedHashSet.add("C");
            tmpExpectedHashSet.add("C");
            */

            HashSet<String> tmpCheckHashSet = new HashSet<>(tmpFragmentList.size() + (int)(tmpFragmentList.size() * 0.3));
            for (IAtomContainer tmpFragment : tmpFragmentList) {
                String tmpString = tmpGenerator.create(tmpFragment);
                System.out.println(tmpString);
                //tmpCheckHashSet.add(tmpString);
                tmpCheckList.add(tmpString);
            }
            /*
            if (!Objects.equals(tmpExpectedHashSet, tmpCheckHashSet)) {
                Assertions.fail("Fragmentation results differ from expected.");
            }
            */
            Assertions.assertTrue(tmpCheckList.size() == tmpExpectedList.size()
                    && tmpCheckList.containsAll(tmpExpectedList)
                    && tmpExpectedList.containsAll(tmpCheckList));
        }
    }

}

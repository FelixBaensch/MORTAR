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
import org.openscience.cdk.AtomContainerSet;
import org.openscience.cdk.exception.CDKException;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.io.IChemObjectReader;
import org.openscience.cdk.io.MDLV3000Reader;
import org.openscience.cdk.io.iterator.IteratingSDFReader;
import org.openscience.cdk.silent.SilentChemObjectBuilder;
import org.openscience.cdk.smiles.SmiFlavor;
import org.openscience.cdk.smiles.SmilesGenerator;
import org.openscience.cdk.tools.manipulator.AtomContainerManipulator;

import java.io.File;
import java.io.FileNotFoundException;
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
     * File containing the structure data for the expected fragments of structures used in this test.
     */
    public final File testExpectedFragmentsFile = new File("src/test/resources/ASF_Expected_Fragments.sdf");
    /**
     * IteratingSDFReader for the structure data file testExpectedFragmentsFile.
     */
    public final IteratingSDFReader testExpectedSDFReader = new IteratingSDFReader(new FileReader(this.testExpectedFragmentsFile), new SilentChemObjectBuilder());
    /**
     * File containing the structure data of the structures used in this test.
     */
    public final File testStructuresFile = new File("src/test/resources/ASF_Test_Structures.sdf");
    /**
     * IteratingSDFReader for the structure data file testStructuresFile.
     */
    public final IteratingSDFReader testStructuresSDFReader = new IteratingSDFReader(new FileReader(this.testStructuresFile), new SilentChemObjectBuilder());
    /**
     * Private AtomContainerSet containing the expected structures as AtomContainers.
     */
    private AtomContainerSet testStructuresACSet = new AtomContainerSet();
    /**
     * Private AlkylStructureFragmenter used in this test, currently without special parameters.
     */
    private AlkylStructureFragmenter basicAlkylStructureFragmenter;
    /**
     * Constructor that sets the default locale to british english, which is needed for correct functioning of the
     * fragmenter as the settings tooltips are imported from the message.properties file.
     */
    public AlkylStructureFragmenterTest() throws FileNotFoundException {
        this.testStructuresACSet = this.readStructureToACSet(this.testStructuresSDFReader);
        Locale.setDefault(new Locale("en", "GB"));
        this.basicAlkylStructureFragmenter = new AlkylStructureFragmenter();
    }

    private AtomContainerSet readStructureToACSet(IteratingSDFReader aSDFReader) {
        AtomContainerSet tmpACSet = new AtomContainerSet();
        while (aSDFReader.hasNext()) {
            IAtomContainer tmpAtomContainer = aSDFReader.next();
            try {
                AtomContainerManipulator.percieveAtomTypesAndConfigureAtoms(tmpAtomContainer);
            } catch (CDKException aCDKException) {
                throw new RuntimeException(aCDKException);
            }
            tmpACSet.addAtomContainer(tmpAtomContainer);
        }
        return tmpACSet;
    }

    /**
     * Utility method generating SMILES notation strings for a given AtomContainerSet.
     *
     * @param anAtomContainerSet given AtomContainerSet
     * @return List containing the generated Strings
     * @throws CDKException if SmilesGenerator is unable to generate String from structure
     */
    private List<String> generateSMILESFromACSet(AtomContainerSet anAtomContainerSet) throws CDKException {
        SmilesGenerator tmpSmilesGenerator = new SmilesGenerator(SmiFlavor.Canonical);
        List<String> tmpSmilesList = new ArrayList<>(anAtomContainerSet.getAtomContainerCount());
        for (IAtomContainer tmpAtomContainer :
                anAtomContainerSet.atomContainers()) {
            tmpSmilesList.add(tmpSmilesGenerator.create(tmpAtomContainer));
        }
        return tmpSmilesList;
    }

    /**
     * Tests correct instantiation and basic settings retrieval.
     *
     * @throws Exception if anything goes wrong
     */
    @Test
    public void basicSettingsTest() throws Exception {
        List<String> tmpCheckList = new ArrayList<>();
        List<String> tmpExpectList = new ArrayList<>();
        tmpExpectList.add("Fragment saturation setting");
        tmpExpectList.add("Fragmentation of hydrocarbon side chains setting");
        tmpExpectList.add("Carbon side chains maximum length setting");
        for (Property tmpSetting: this.basicAlkylStructureFragmenter.settingsProperties()) {
            tmpCheckList.add(tmpSetting.getName());
        }
        Assertions.assertLinesMatch(tmpExpectList, tmpCheckList);
    }

    @Test
    public void markRingsTest() {
        //ToDo: find way to compare structures without extracting tested substructures
    }
    @Test
    public void markConjugatedPiSystemsTest() {
        //ToDo: find way to compare structures without extracting tested substructures
    }
    @Test
    public void saturateWithImplicitHydrogenTest() {
        //ToDo: generate test structures with open valences to be saturated
    }
    @Test
    public void separateDisconnectedStructuresTest() {
        //ToDo: generate disconnected structures in one AtomContainer
    }
    @Test
    public void extractFragmentsTest() {
        //ToDo: generate structures with marked substructures (mark with code)
    }
    @Test
    public void dissectLinearChainTest() {
        //ToDo: generate/choose linear structure to test linear dissection
    }
    /**
     *  Method to test a default alkyl structure fragmentation on a concept molecule.
     *
     * @throws Exception if anything goes wrong
     */
    @Test
    public void defaultFragmentationTest() throws Exception {
        this.basicAlkylStructureFragmenter.setFragmentSaturationSetting(AlkylStructureFragmenter.FRAGMENT_SATURATION_OPTION_DEFAULT);
        this.basicAlkylStructureFragmenter.setFragmentSideChainsSetting(AlkylStructureFragmenter.FRAGMENT_SIDE_CHAINS_SETTING_DEFAULT);
        this.basicAlkylStructureFragmenter.setMaxChainLengthSetting(AlkylStructureFragmenter.MAX_CHAIN_LENGTH_SETTING_DEFAULT);

        for (IAtomContainer tmpAtomContainer :
                this.testStructuresACSet.atomContainers()) {
            Assertions.assertFalse(this.basicAlkylStructureFragmenter.shouldBeFiltered(tmpAtomContainer));
            Assertions.assertFalse(this.basicAlkylStructureFragmenter.shouldBePreprocessed(tmpAtomContainer));
            Assertions.assertTrue(this.basicAlkylStructureFragmenter.canBeFragmented(tmpAtomContainer));
        }
        try (MDLV3000Reader tmpMDLReader = new MDLV3000Reader(new FileReader("src/test/resources/TestASFStructure1.mol"), IChemObjectReader.Mode.RELAXED)) {
            IAtomContainer tmpOriginalMolecule = tmpMDLReader.read(SilentChemObjectBuilder.getInstance().newAtomContainer());
            AtomContainerManipulator.percieveAtomTypesAndConfigureAtoms(tmpOriginalMolecule);
            /*
            File aFile = new File("src/test/resources/ASF_Test_COCONUT_subset_sample.sdf");
            IteratingSDFReader tmpSDFReader = new IteratingSDFReader(new FileInputStream(aFile),
                    SilentChemObjectBuilder.getInstance());
            IAtomContainerSet tmpOriginalMoleculeSet = new AtomContainerSet();
            while (tmpSDFReader.hasNext()) {
                IAtomContainer tmpAtomContainer = tmpSDFReader.next();
                if (tmpAtomContainer != null) {
                    AtomContainerManipulator.percieveAtomTypesAndConfigureAtoms(tmpAtomContainer);
                    tmpOriginalMoleculeSet.addAtomContainer(tmpAtomContainer);
                }
            }
            */

            this.basicAlkylStructureFragmenter.setFragmentSaturationSetting(AlkylStructureFragmenter.FRAGMENT_SATURATION_OPTION_DEFAULT);
            this.basicAlkylStructureFragmenter.setMaxChainLengthSetting(AlkylStructureFragmenter.MAX_CHAIN_LENGTH_SETTING_DEFAULT);
            //extract to unit tests, keep tmpFragmenter settings
            Assertions.assertFalse(this.basicAlkylStructureFragmenter.shouldBeFiltered(tmpOriginalMolecule));
            Assertions.assertFalse(this.basicAlkylStructureFragmenter.shouldBePreprocessed(tmpOriginalMolecule));
            Assertions.assertTrue(this.basicAlkylStructureFragmenter.canBeFragmented(tmpOriginalMolecule));
            //
            List<IAtomContainer> tmpFragmentList;
            tmpFragmentList = this.basicAlkylStructureFragmenter.fragmentMolecule(tmpOriginalMolecule);
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
            for (IAtomContainer tmpFragment : tmpFragmentList) {
                String tmpString = tmpGenerator.create(tmpFragment);
                System.out.println(tmpString);
                tmpCheckList.add(tmpString);
            }
            Assertions.assertTrue(tmpCheckList.size() == tmpExpectedList.size()
                    && tmpCheckList.containsAll(tmpExpectedList)
                    && tmpExpectedList.containsAll(tmpCheckList));
        }
    }

}

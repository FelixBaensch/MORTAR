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
import org.openscience.cdk.exception.CDKException;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.interfaces.IAtomContainerSet;
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
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
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
    private AtomContainerSet testStructuresACSet;
    /**
     * Private List containing test structures.
     */
    private List<IAtomContainer> testStructuresList;
    /**
     * Private AlkylStructureFragmenter used in this test, currently without special parameters.
     */
    private final AlkylStructureFragmenter basicAlkylStructureFragmenter;
    /**
     * Constructor that sets the default locale to british english, which is needed for correct functioning of the
     * fragmenter as the settings tooltips are imported from the message.properties file.
     */
    public AlkylStructureFragmenterTest() throws FileNotFoundException {
        this.testStructuresACSet = this.readStructureToACSet(this.testStructuresFile);
        this.testStructuresList = new ArrayList<IAtomContainer>(this.testStructuresACSet.getAtomContainerCount());
        for (IAtomContainer tmpAtomContainer :
                this.testStructuresACSet.atomContainers()) {
            this.testStructuresList.add(tmpAtomContainer);
        }
        Locale.setDefault(new Locale("en", "GB"));
        this.basicAlkylStructureFragmenter = new AlkylStructureFragmenter();
    }

    private AtomContainerSet readStructureToACSet(File aFile) throws FileNotFoundException {
        IteratingSDFReader tmpSDFReader = new IteratingSDFReader(new FileReader(aFile), new SilentChemObjectBuilder());
        AtomContainerSet tmpACSet = new AtomContainerSet();
        String tmpIndexString = "ASFTest.AtomContainerIndex";
        int tmpIndex = 0;
        while (tmpSDFReader.hasNext()) {
            IAtomContainer tmpAtomContainer = tmpSDFReader.next();
            tmpAtomContainer.setProperty(tmpIndexString, tmpIndex);
            try {
                AtomContainerManipulator.percieveAtomTypesAndConfigureAtoms(tmpAtomContainer);
            } catch (CDKException aCDKException) {
                throw new RuntimeException(aCDKException);
            }
            tmpACSet.addAtomContainer(tmpAtomContainer);
            tmpIndex++;
        }
        return tmpACSet;
    }

    /**
     * Utility method generating SMILES notation strings for a given AtomContainerSet.
     *
     * @param anAtomContainer given AtomContainerSet
     * @return List containing the generated Strings
     * @throws CDKException if SmilesGenerator is unable to generate String from structure
     */
    private List<String> generateSMILESFromACSet(IAtomContainer anAtomContainer) throws CDKException {
        SmilesGenerator tmpSmilesGenerator = new SmilesGenerator(SmiFlavor.Canonical);
        List<String> tmpSmilesList = new ArrayList<>(anAtomContainer.getAtomCount());
        tmpSmilesList.add(tmpSmilesGenerator.create(anAtomContainer));
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
    /**
     * Test method for AlkylStructureFragmenter.markRings().
     *
     * @throws NoSuchMethodException if method reflection returns null
     * @throws InvocationTargetException if target method cannot be invoked
     * @throws IllegalAccessException if method cannot be accessed
     */
    @Test
    public void markRingsTest() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        IAtomContainer tmpRingsAC = this.testStructuresList.get(0);
        Method tmpMarkRings = this.basicAlkylStructureFragmenter.getClass().getDeclaredMethod("markRings", IAtomContainer.class);
        tmpMarkRings.setAccessible(true);
        //problem: marking on local(ASF) private variables
        tmpMarkRings.invoke(this.basicAlkylStructureFragmenter, tmpRingsAC);
        //ToDo: find way to compare structures without extracting tested substructures
    }
    /**
     * Test method for AlkylStructureFragmenter.markConjugatedPiSystems().
     *
     * @throws NoSuchMethodException if method reflection returns null
     * @throws InvocationTargetException if target method cannot be invoked
     * @throws IllegalAccessException if method cannot be accessed
     */
    @Test
    public void markConjugatedPiSystemsTest() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        IAtomContainer tmpConjugatedAC = this.testStructuresList.get(0);
        Method tmpMarkConjugated = this.basicAlkylStructureFragmenter.getClass().getDeclaredMethod("markConjugatedPiSystems", IAtomContainer.class);
        tmpMarkConjugated.setAccessible(true);
        //problem: marking on local(ASF) private variables
        tmpMarkConjugated.invoke(this.basicAlkylStructureFragmenter, tmpConjugatedAC);
        //ToDo: find way to compare structures without extracting tested substructures
    }
    /**
     * Test method for AlkylStructureFragmenter.saturateWithImplicitHydrogen().
     *
     * @throws NoSuchMethodException if method reflection returns null
     * @throws InvocationTargetException if target method cannot be invoked
     * @throws IllegalAccessException if method cannot be accessed
     */
    @Test
    public void saturateWithImplicitHydrogenTest() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        IAtomContainerSet tmpSaturateACSet = new AtomContainerSet();
        tmpSaturateACSet.addAtomContainer(this.testStructuresList.get(0));
        Method tmpSaturate = this.basicAlkylStructureFragmenter.getClass().getDeclaredMethod("saturateWithImplicitHydrogen", IAtomContainerSet.class);
        tmpSaturate.setAccessible(true);
        //problem?
        List<IAtomContainer> tmpACList = (List<IAtomContainer>) tmpSaturate.invoke(this.basicAlkylStructureFragmenter, tmpSaturateACSet);
        //ToDo: generate test structures with open valences to be saturated
    }
    /**
     * Test method for AlkylStructureFragmenter.separateDisconnectedStructures().
     *
     * @throws NoSuchMethodException if method reflection returns null
     * @throws InvocationTargetException if target method cannot be invoked
     * @throws IllegalAccessException if method cannot be accessed
     */
    @Test
    public void separateDisconnectedStructuresTest() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        IAtomContainer tmpDisconnectedAC = this.testStructuresList.get(0);
        Method tmpSeparateDisconnectedAC = this.basicAlkylStructureFragmenter.getClass().getDeclaredMethod("separateDisconnectedStructures", IAtomContainer.class);
        tmpSeparateDisconnectedAC.setAccessible(true);
        IAtomContainerSet tmpDisconnectedACSet = (IAtomContainerSet) tmpSeparateDisconnectedAC.invoke(this.basicAlkylStructureFragmenter, tmpDisconnectedAC);
        //ToDo: generate disconnected structures in one AtomContainer
    }
    /**
     * Test method for AlkylStructureFragmenter.extractFragments().
     *
     * @throws NoSuchMethodException if method reflection returns null
     * @throws InvocationTargetException if target method cannot be invoked
     * @throws IllegalAccessException if method cannot be accessed
     */
    @Test
    public void extractFragmentsTest() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        //problem: no way to take input AC as they are local(ASF) private variables
        IAtomContainer tmpDisconnectedAC = this.testStructuresList.get(0);
        Method tmpExtractFragments = this.basicAlkylStructureFragmenter.getClass().getDeclaredMethod("extractFragments");
        tmpExtractFragments.setAccessible(true);
        IAtomContainerSet tmpExtractedACSet = (IAtomContainerSet) tmpExtractFragments.invoke(this.basicAlkylStructureFragmenter);
        //ToDo: generate structures with marked substructures (mark with code)
    }
    /**
     * Test method for AlkylStructureFragmenter.dissectLinearChain().
     *
     * @throws NoSuchMethodException if method reflection returns null
     * @throws InvocationTargetException if target method cannot be invoked
     * @throws IllegalAccessException if method cannot be accessed
     */
    @Test
    public void dissectLinearChainTest() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        //get linear test structure from ACList
        IAtomContainer tmpDissectLinearChainAC = this.testStructuresList.get(0);//correct index needed
        Method tmpDissectMethod = this.basicAlkylStructureFragmenter.getClass().getDeclaredMethod("dissectLinearChain", IAtomContainer.class, int.class);
        tmpDissectMethod.setAccessible(true);
        IAtomContainer tmpNoRestrictAC = (IAtomContainer) tmpDissectMethod.invoke(this.basicAlkylStructureFragmenter, tmpDissectLinearChainAC, 0);
        //dissect test structure with different settings
        //compare with expected fragments
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

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
import org.openscience.cdk.AtomContainerSet;
import org.openscience.cdk.exception.CDKException;
import org.openscience.cdk.interfaces.IAtom;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.interfaces.IAtomContainerSet;
import org.openscience.cdk.interfaces.IBond;
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
public class AlkylStructureFragmenterTest extends AlkylStructureFragmenter{

    /**
     * File containing the structure data for the expected fragments of structures used in this test.
     */
    public final File testExpectedFragmentsFile = new File("src\\test\\resources\\ASF_Expected_Fragments.sdf");
    /**
     * IteratingSDFReader for the structure data file testExpectedFragmentsFile.
     */
    public final IteratingSDFReader testExpectedSDFReader = new IteratingSDFReader(new FileReader(this.testExpectedFragmentsFile), new SilentChemObjectBuilder());
    /**
     * File containing the structure data of the structures used in this test.
     */
    public final File testStructuresFile = new File("src\\test\\resources\\ASF_Test_Structures.sdf");
    /**
     * IteratingSDFReader for the structure data file testStructuresFile.
     */
    public final IteratingSDFReader testStructuresSDFReader = new IteratingSDFReader(new FileReader(this.testStructuresFile), new SilentChemObjectBuilder());
    /**
     * Private AtomContainerSet containing the expected structures as AtomContainers.
     */
    private IAtomContainerSet testStructuresACSet;
    private List<IAtomContainer> testResultFragmentsACList;
    private IAtomContainerSet testResultACSet;
    private IAtomContainerSet testExpectedFragmentsACSet;
    private List<IAtomContainer> testExpectedFragmentsACList;
    /**
     * Private List containing test structures.
     */
    //private Hash<IAtomContainer> testStructuresHashSet;
    /**
     * Private AlkylStructureFragmenter used in this test, currently without special parameters.
     */
    private final AlkylStructureFragmenter basicAlkylStructureFragmenter = new AlkylStructureFragmenter();
    private IAtom[] testAtomArray;
    private IBond[] testBondArray;
    static {
        Locale.setDefault(Locale.of("en", "GB"));
    }

    /**
     * Constructor that sets the default locale to british english, which is needed for correct functioning of the
     * fragmenter as the settings tooltips are imported from the message.properties file.
     */
    public AlkylStructureFragmenterTest() throws FileNotFoundException
    {
        this.testStructuresACSet = new AtomContainerSet();
        this.testStructuresACSet = readStructureToACSet(this.testStructuresFile);
        this.testExpectedFragmentsACSet = readStructureToACSet(this.testExpectedFragmentsFile);
        this.testExpectedFragmentsACList = new ArrayList<>(this.testExpectedFragmentsACSet.getAtomContainerCount());
        //this.testAtomArray = this.basicAlkylStructureFragmenter.fillAtomArray(tmpAC);
        this.testAtomArray = this.basicAlkylStructureFragmenter.fillAtomArray(this.testStructuresACSet.getAtomContainer(0));
        //this.testBondArray = this.basicAlkylStructureFragmenter.fillBondArray(tmpAC);
        this.testBondArray = this.basicAlkylStructureFragmenter.fillBondArray(this.testStructuresACSet.getAtomContainer(0));
        //Locale.setDefault(new Locale("en", "GB"));

    }

    //<editor-fold desc="@Test Public Methods">
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
        IAtomContainer tmpRingsAC = this.testStructuresACSet.getAtomContainer(0);
        this.testAtomArray = this.basicAlkylStructureFragmenter.fillAtomArray(tmpRingsAC);
        this.testBondArray = this.basicAlkylStructureFragmenter.fillBondArray(tmpRingsAC);


        //protected methods & variables -> test class extends origin class
        //problem: marking on local(ASF) private variables
        //ToDo: write test structure in fragmenter arrays; create array for comparison with expected markings
        this.basicAlkylStructureFragmenter.markRings(tmpRingsAC, this.testAtomArray, this.testBondArray);
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
        //IAtomContainer tmpConjugatedAC = this.testStructuresHashSet.getFirst();
        //this.basicAlkylStructureFragmenter.markConjugatedPiSystems();
        /*
        Method tmpMarkConjugated = this.basicAlkylStructureFragmenter.getClass().getDeclaredMethod("AlkylStructureFragmenter.markConjugatedPiSystems", IAtomContainer.class);
        tmpMarkConjugated.setAccessible(true);
        //problem: marking on local(ASF) private variables
        tmpMarkConjugated.invoke(this.basicAlkylStructureFragmenter, tmpConjugatedAC);
        */
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
        tmpSaturateACSet.addAtomContainer(this.testStructuresACSet.getAtomContainer(0));
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
        IAtomContainer tmpDisconnectedAC = this.testStructuresACSet.getAtomContainer(0);
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
        IAtomContainer tmpDisconnectedAC = this.testStructuresACSet.getAtomContainer(0);
        IAtomContainerSet tmpExtractFragments;
        try {
            tmpExtractFragments = this.basicAlkylStructureFragmenter.extractFragments(this.testAtomArray, this.testBondArray);
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException(e);
        }
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
        IAtomContainer tmpDissectLinearChainAC = this.testStructuresACSet.getAtomContainer(0);//correct index needed
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
        this.testResultFragmentsACList = this.basicAlkylStructureFragmenter.fragmentMolecule(this.testStructuresACSet.getAtomContainer(0));
        for (IAtomContainer tmpAC : this.testExpectedFragmentsACSet.atomContainers()) {
            this.testExpectedFragmentsACList.add(tmpAC);
        }
        Assertions.assertTrue(this.compareListsIgnoringOrder(new ArrayList<>(this.testResultFragmentsACList),
                new ArrayList<>(this.testExpectedFragmentsACList)));
    }
    //</editor-fold>

    //<editor-fold desc="Private Methods">
    private boolean compareListsIgnoringOrder(ArrayList aList1, ArrayList aList2) {
        if (aList1 == null || aList2 == null) return false;
        if (aList1.size() != aList2.size()) return false;
        for (Object o : aList1) {
            aList2.remove(o);
        }
        return !aList2.isEmpty();
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

    private IAtomContainerSet readStructureToACSet(File aFile) throws FileNotFoundException {
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
    //</editor-fold>

}

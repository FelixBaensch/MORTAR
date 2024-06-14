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

import de.unijena.cheminf.mortar.model.util.ChemUtil;

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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Class to test the correct working of
 * {@link de.unijena.cheminf.mortar.model.fragmentation.algorithm.AlkylStructureFragmenter}.
 *
 * @author Maximilian Rottmann
 * @version 1.0.0.0
 */
public class AlkylStructureFragmenterTest extends AlkylStructureFragmenter{
    /**
     * Private AtomContainerSet containing the test structures.
     */
    private IAtomContainerSet testStructuresACSet;
    /**
     * Private AtomContainer List containing the resulting fragments after fragmentation.
     */
    private List<IAtomContainer> testResultFragmentsACList;
    /**
     * Private AtomContainerSet containing the resulting fragments after fragmentation.
     */
    private IAtomContainerSet testResultACSet;
    /**
     * Private AtomContainerSet containing the expected fragments.
     */
    private IAtomContainerSet testExpectedFragmentsACSet;
    /**
     * Private AtomContainerSet containing the expected structures as AtomContainers.
     */
    private List<IAtomContainer> testExpectedFragmentsACList;
    /**
     * Private AlkylStructureFragmenter used in this test, currently without special parameters.
     */
    private final AlkylStructureFragmenter basicAlkylStructureFragmenter;
    /**
     * Private IAtom Array containing the atoms of a given structure, used in unit testing of internal
     * AlkylStructureFragmenter methods.
     */
    private IAtom[] testAtomArray;
    /**
     * Private IBond Array containing the bonds of a given structure, used in unit testing of internal
     * AlkylStructureFragmenter methods.
     */
    private IBond[] testBondArray;
    /**
     * Static Locale set to its default: "en" and "GB".
     */
    static {
        Locale.setDefault(Locale.of("en", "GB"));
    }

    /**
     * Constructor of AlkylStructureFragmenter test class, setting all necessary settings in order of correct functionality
     * during testing.
     */
    public AlkylStructureFragmenterTest() throws FileNotFoundException, URISyntaxException {
        this.basicAlkylStructureFragmenter = new AlkylStructureFragmenter();
        this.testStructuresACSet = new AtomContainerSet();
        this.testStructuresACSet = readStructureToACSet("ASF_Test_Structures.sdf");
        this.testExpectedFragmentsACSet = readStructureToACSet("ASF_Expected_Fragments.sdf");
        this.testExpectedFragmentsACList = new ArrayList<>(this.testExpectedFragmentsACSet.getAtomContainerCount());
        //this.testAtomArray = this.basicAlkylStructureFragmenter.fillAtomArray(tmpAC);
        this.testAtomArray = this.basicAlkylStructureFragmenter.fillAtomArray(this.testStructuresACSet.getAtomContainer(0));
        //this.testBondArray = this.basicAlkylStructureFragmenter.fillBondArray(tmpAC);
        this.testBondArray = this.basicAlkylStructureFragmenter.fillBondArray(this.testStructuresACSet.getAtomContainer(0));
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
        tmpExpectList.add("Single carbon handling setting");
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
        this.basicAlkylStructureFragmenter.setFragmentSaturationSetting(IMoleculeFragmenter.FRAGMENT_SATURATION_OPTION_DEFAULT);
        this.basicAlkylStructureFragmenter.setFragmentSideChainsSetting(AlkylStructureFragmenter.FRAGMENT_SIDE_CHAINS_SETTING_DEFAULT);
        this.basicAlkylStructureFragmenter.setMaxChainLengthSetting(AlkylStructureFragmenter.MAX_CHAIN_LENGTH_SETTING_DEFAULT);
        //currently alternative handling is explicitly ACTIVATED for testing purposes until new expected fragments are created
        this.basicAlkylStructureFragmenter.setAlternativeSingleCarbonHandlingSetting(
                //AlkylStructureFragmenter.ALTERNATIVE_SINGLE_CARBON_HANDLING_SETTING_DEFAULT
                true
        );
        for (IAtomContainer tmpAtomContainer :
                this.testStructuresACSet.atomContainers()) {
            Assertions.assertFalse(this.basicAlkylStructureFragmenter.shouldBeFiltered(tmpAtomContainer));
            Assertions.assertFalse(this.basicAlkylStructureFragmenter.shouldBePreprocessed(tmpAtomContainer));
            Assertions.assertTrue(this.basicAlkylStructureFragmenter.canBeFragmented(tmpAtomContainer));
        }
        this.testResultFragmentsACList = this.basicAlkylStructureFragmenter.fragmentMolecule(this.testStructuresACSet.getAtomContainer(0));
        this.testResultACSet = new AtomContainerSet();
        for (int i = 0; i < this.testResultFragmentsACList.size(); i++) {
            this.testResultACSet.addAtomContainer(this.testResultFragmentsACList.get(i));
        }
        List<String> tmpExpectedSMILESList = this.generateSMILESFromACSet(this.testExpectedFragmentsACSet);
        List<String> tmpResultSMILESList = this.generateSMILESFromACSet(this.testResultACSet);
        Assertions.assertTrue(this.compareListsIgnoringOrder(new ArrayList<>(tmpResultSMILESList),
                new ArrayList<>(tmpExpectedSMILESList)));
    }
    //</editor-fold>

    //<editor-fold desc="Private Methods">
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

    /**
     * Utility method generating SMILES notation strings for a given AtomContainerSet.
     *
     * @param anACSet given AtomContainerSet
     * @return List containing the generated Strings
     * @throws CDKException if SmilesGenerator is unable to generate String from structure
     */
    private List<String> generateSMILESFromACSet(IAtomContainerSet anACSet) throws CDKException {
        List<String> tmpSmilesList = new ArrayList<>(anACSet.getAtomContainerCount());
        for (IAtomContainer tmpAC : anACSet.atomContainers()) {
            tmpSmilesList.add(ChemUtil.createUniqueSmiles(tmpAC));
        }
        return tmpSmilesList;
    }

    private IAtomContainerSet readStructureToACSet(String aFileName) throws FileNotFoundException, URISyntaxException {
        URL tmpURL = this.getClass().getResource("/" + aFileName);
        File tmpResourceFile = Paths.get(tmpURL.toURI()).toFile();
        IteratingSDFReader tmpSDFReader = new IteratingSDFReader(new FileReader(tmpResourceFile), new SilentChemObjectBuilder());
        AtomContainerSet tmpACSet = new AtomContainerSet();
        String tmpIndexString = "ASFTest.AtomContainerIndex";
        int tmpIndex = 0;
        while (tmpSDFReader.hasNext()) {
            IAtomContainer tmpAtomContainer = tmpSDFReader.next();
            tmpAtomContainer.setProperty(tmpIndexString, tmpIndex);
            try {
                ChemUtil.saturateWithHydrogen(tmpAtomContainer);
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

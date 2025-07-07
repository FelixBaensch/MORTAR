/*
 * MORTAR - MOlecule fRagmenTAtion fRamework
 * Copyright (C) 2025  Felix Baensch, Jonas Schaub (felix.j.baensch@gmail.com, jonas.schaub@uni-jena.de)
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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.openscience.cdk.AtomContainerSet;
import org.openscience.cdk.exception.CDKException;
import org.openscience.cdk.exception.InvalidSmilesException;
import org.openscience.cdk.interfaces.IAtom;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.interfaces.IAtomContainerSet;
import org.openscience.cdk.interfaces.IBond;
import org.openscience.cdk.io.iterator.IteratingSDFReader;
import org.openscience.cdk.silent.AtomContainer;
import org.openscience.cdk.silent.SilentChemObjectBuilder;
import org.openscience.cdk.smiles.SmiFlavor;
import org.openscience.cdk.smiles.SmilesGenerator;
import org.openscience.cdk.smiles.SmilesParser;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * Class to test the correct working of
 * {@link de.unijena.cheminf.mortar.model.fragmentation.algorithm.AlkylStructureFragmenter}.
 *
 * @author Maximilian Rottmann
 * @version 1.0.0.0
 */
public class AlkylStructureFragmenterTest extends AlkylStructureFragmenter{
    /**
     * Static setter for Locale.
     * If Locale is set in Test constructor, a MissingResourceException is thrown when user locale diverts from set Locale.
     */
    static {
        Locale.setDefault(Locale.of("en", "GB"));
    }
    /**
     * Constructor of AlkylStructureFragmenter test class.
     */
    public AlkylStructureFragmenterTest() {}
    //<editor-fold desc="@Test Public Methods">
    /**
     * Test method for AlkylStructureFragmenter.extractFragments().
     */
    @Test
    public void extractFragmentsTest() throws CDKException {
        ArrayList<String> tmpExpectedFragmentsList = new ArrayList<>(6);
        tmpExpectedFragmentsList.add("C=CC1=CC=C2C=CC=CC2=C1");
        tmpExpectedFragmentsList.add("CC(C)(C)C");
        tmpExpectedFragmentsList.add("C=C");
        tmpExpectedFragmentsList.add("CCC");
        tmpExpectedFragmentsList.add("CCC");
        tmpExpectedFragmentsList.add("C1CCCCC1");
        SmilesParser tmpParser = new SmilesParser(SilentChemObjectBuilder.getInstance());
        IAtomContainer tmpTestAC = tmpParser.parseSmiles("C12=CC(=CC=C1C(=CC(=C2)C3CCCCC3)CC(C)(C)C/C=C/CCC)/C=C\\C(C)C");
        MolecularArrays tmpTestMolecularArrays = new MolecularArrays(tmpTestAC);
        AlkylStructureFragmenter tmpASF = new AlkylStructureFragmenter();
        tmpTestMolecularArrays.setAtomArray(tmpASF.fillAtomArray(tmpTestAC));
        tmpTestMolecularArrays.setBondArray(tmpASF.fillBondArray(tmpTestAC));
        tmpASF.markNeighborAtomsAndBonds(tmpTestMolecularArrays,
                tmpTestMolecularArrays.getAtomArray(), tmpTestMolecularArrays.getBondArray());
        tmpASF.markRings(tmpTestMolecularArrays, tmpTestAC,
                tmpTestMolecularArrays.getAtomArray(), tmpTestMolecularArrays.getBondArray());
        tmpASF.markConjugatedPiSystems(tmpTestMolecularArrays, tmpTestAC,
                tmpTestMolecularArrays.getAtomArray(), tmpTestMolecularArrays.getBondArray());
        tmpASF.markMultiBonds(tmpTestMolecularArrays,
                tmpTestMolecularArrays.getAtomArray(), tmpTestMolecularArrays.getBondArray());
        IAtomContainerSet tmpExtractedFrag;
        ArrayList<String> tmpExtractedFragmentList = new ArrayList<>(6);
        try {
            tmpExtractedFrag = tmpASF.extractFragments(tmpTestMolecularArrays.getAtomArray(), tmpTestMolecularArrays.getBondArray());
            for (IAtomContainer tmpAC : tmpExtractedFrag.atomContainers()) {
                ChemUtil.saturateWithHydrogen(tmpAC);
                tmpExtractedFragmentList.add(ChemUtil.createUniqueSmiles(tmpAC, false));
            }
        } catch (CloneNotSupportedException | CDKException e) {
            throw new RuntimeException(e);
        }
        Assertions.assertTrue(this.compareListsIgnoringOrder(tmpExtractedFragmentList, tmpExpectedFragmentsList));
    }
    /**
     * Method testing for correct behavior in dissection and separation of linear carbon chains of varying sizes (including 'no restrictions applied').
     */
    @Test
    public void defaultLinearChainDissectionTest() throws InvalidSmilesException, CloneNotSupportedException {
        SmilesParser tmpParser = new SmilesParser(SilentChemObjectBuilder.getInstance());
        IAtomContainer tmpCarbonChainAC = tmpParser.parseSmiles("CCCCCCCCCCCCCC");
        AlkylStructureFragmenter tmpASF = this.getDefaultASFInstance(tmpCarbonChainAC, false,
                false, true);
        tmpASF.setFragmentSideChainsSetting(true);
        tmpASF.setMaxChainLengthSetting(AlkylStructureFragmenter.MAX_CHAIN_LENGTH_SETTING_DEFAULT);
        List<String> tmpFragmentsACList = this.generateSMILESFromACList(tmpASF.fragmentMolecule(tmpCarbonChainAC));
        List<String> tmpExpectedSMILESList = new ArrayList<>();
        tmpExpectedSMILESList.add("CC");
        tmpExpectedSMILESList.add("CCCCCC");
        tmpExpectedSMILESList.add("CCCCCC");
        Assertions.assertTrue(this.compareListsIgnoringOrder(new ArrayList<>(tmpFragmentsACList),
                new ArrayList<>(tmpExpectedSMILESList)));
        tmpASF.setMaxChainLengthSetting(7);
        tmpFragmentsACList.clear();
        tmpExpectedSMILESList.clear();
        tmpFragmentsACList = this.generateSMILESFromACList(tmpASF.fragmentMolecule(tmpCarbonChainAC));
        tmpExpectedSMILESList.add("CCCCCCC");
        tmpExpectedSMILESList.add("CCCCCCC");
        Assertions.assertTrue(this.compareListsIgnoringOrder(new ArrayList<>(tmpFragmentsACList),
                new ArrayList<>(tmpExpectedSMILESList)));
        tmpASF.setFragmentSideChainsSetting(false);
        tmpFragmentsACList.clear();
        tmpExpectedSMILESList.clear();
        tmpFragmentsACList = this.generateSMILESFromACList(tmpASF.fragmentMolecule(tmpCarbonChainAC));
        tmpExpectedSMILESList.add("CCCCCCCCCCCCCC");
        Assertions.assertTrue(this.compareListsIgnoringOrder(new ArrayList<>(tmpFragmentsACList),
                new ArrayList<>(tmpExpectedSMILESList)));
    }
    /**
     * Method testing correct fragmentation with a basic example molecule.
     * This test focuses on tertiary and quaternary carbon systems, tested with a conceptual molecule.
     *
     * @throws InvalidSmilesException if SMILES is not correctly parsed of otherwise faulty
     * @throws CloneNotSupportedException if something goes wrong during cloning step in fragmentation
     */
    @Test
    public void basicTest01() throws InvalidSmilesException, CloneNotSupportedException {
        //test structure: CC(C)(C)CCCC(C)C
        SmilesParser tmpParser = new SmilesParser(SilentChemObjectBuilder.getInstance());
        IAtomContainer tmpTestStructureAC = tmpParser.parseSmiles("CC(C)(C)CCCC(C)C");
        AlkylStructureFragmenter tmpASF = this.getDefaultASFInstance(tmpTestStructureAC, false,
                false, true);
        List<String> tmpFragmentsACList = this.generateSMILESFromACList(tmpASF.fragmentMolecule(tmpTestStructureAC));
        List<String> tmpExpectedSMILESList = new ArrayList<>();
        tmpExpectedSMILESList.add("C");
        tmpExpectedSMILESList.add("C");
        tmpExpectedSMILESList.add("C");
        tmpExpectedSMILESList.add("C");
        tmpExpectedSMILESList.add("C");
        tmpExpectedSMILESList.add("CCC");
        tmpExpectedSMILESList.add("*C(*)(*)*");
        tmpExpectedSMILESList.add("*C(*)*");
        Assertions.assertTrue(this.compareListsIgnoringOrder(new ArrayList<>(tmpFragmentsACList),
                new ArrayList<>(tmpExpectedSMILESList)));
    }
    /**
     * Method testing correct fragmentation with a basic example molecule.
     * This test focuses on extraction of isolated double and triple bonds,
     * tested with the corresponding butenes and butynes.
     *
     * @throws InvalidSmilesException if SMILES is not correctly parsed of otherwise faulty
     * @throws CloneNotSupportedException if something goes wrong during cloning step in fragmentation
     */
    @Test
    public void basicTest02() throws InvalidSmilesException, CloneNotSupportedException {
        //test structure: CC=CC
        SmilesParser tmpParser = new SmilesParser(SilentChemObjectBuilder.getInstance());
        IAtomContainer tmpTestStructureAC = tmpParser.parseSmiles("CC=CC");
        AlkylStructureFragmenter tmpASF = this.getDefaultASFInstance(tmpTestStructureAC, false,
                false, true);
        List<String> tmpFragmentsACList = this.generateSMILESFromACList(tmpASF.fragmentMolecule(tmpTestStructureAC));
        List<String> tmpExpectedSMILESList = new ArrayList<>();
        tmpExpectedSMILESList.add("C");
        tmpExpectedSMILESList.add("C");
        tmpExpectedSMILESList.add("C=C");
        Assertions.assertTrue(this.compareListsIgnoringOrder(new ArrayList<>(tmpFragmentsACList),
                new ArrayList<>(tmpExpectedSMILESList)));
        //test structure: C=CCC
        tmpTestStructureAC = tmpParser.parseSmiles("C=CCC");
        Assertions.assertFalse(tmpASF.shouldBeFiltered(tmpTestStructureAC));
        Assertions.assertFalse(tmpASF.shouldBePreprocessed(tmpTestStructureAC));
        Assertions.assertTrue(tmpASF.canBeFragmented(tmpTestStructureAC));
        tmpFragmentsACList = this.generateSMILESFromACList(tmpASF.fragmentMolecule(tmpTestStructureAC));
        tmpExpectedSMILESList.clear();
        tmpExpectedSMILESList.add("CC");
        tmpExpectedSMILESList.add("C=C");
        Assertions.assertTrue(this.compareListsIgnoringOrder(new ArrayList<>(tmpFragmentsACList),
                new ArrayList<>(tmpExpectedSMILESList)));
        //test structure: CC#CC
        tmpTestStructureAC = tmpParser.parseSmiles("CC#CC");
        Assertions.assertFalse(tmpASF.shouldBeFiltered(tmpTestStructureAC));
        Assertions.assertFalse(tmpASF.shouldBePreprocessed(tmpTestStructureAC));
        Assertions.assertTrue(tmpASF.canBeFragmented(tmpTestStructureAC));
        tmpFragmentsACList = this.generateSMILESFromACList(tmpASF.fragmentMolecule(tmpTestStructureAC));
        tmpExpectedSMILESList.clear();
        tmpExpectedSMILESList.add("C");
        tmpExpectedSMILESList.add("C");
        tmpExpectedSMILESList.add("C#C");
        Assertions.assertTrue(this.compareListsIgnoringOrder(new ArrayList<>(tmpFragmentsACList),
                new ArrayList<>(tmpExpectedSMILESList)));
    }
    /**
     * Method testing correct fragmentation with a basic example molecule.
     * This test focuses on safe retention of rings, tested with Cyclohexane and Benzene.
     *
     * @throws InvalidSmilesException if SMILES is not correctly parsed of otherwise faulty
     * @throws CloneNotSupportedException if something goes wrong during cloning step in fragmentation
     */
    @Test
    public void basicTest03() throws InvalidSmilesException, CloneNotSupportedException {
        //test structure: C1CCCCC1
        SmilesParser tmpParser = new SmilesParser(SilentChemObjectBuilder.getInstance());
        IAtomContainer tmpTestStructureAC = tmpParser.parseSmiles("C1CCCCC1");
        AlkylStructureFragmenter tmpASF = this.getDefaultASFInstance(tmpTestStructureAC, false,
                false, true);
        List<String> tmpFragmentsACList = this.generateSMILESFromACList(tmpASF.fragmentMolecule(tmpTestStructureAC));
        List<String> tmpExpectedSMILESList = new ArrayList<>();
        tmpExpectedSMILESList.add("C1CCCCC1");
        Assertions.assertTrue(this.compareListsIgnoringOrder(new ArrayList<>(tmpFragmentsACList),
                new ArrayList<>(tmpExpectedSMILESList)));
        //test structure: c1ccccc1
        tmpTestStructureAC = tmpParser.parseSmiles("c1ccccc1");
        Assertions.assertFalse(tmpASF.shouldBeFiltered(tmpTestStructureAC));
        Assertions.assertFalse(tmpASF.shouldBePreprocessed(tmpTestStructureAC));
        Assertions.assertTrue(tmpASF.canBeFragmented(tmpTestStructureAC));
        tmpFragmentsACList = this.generateSMILESFromACList(tmpASF.fragmentMolecule(tmpTestStructureAC));
        tmpExpectedSMILESList.clear();
        tmpExpectedSMILESList.add("C1=CC=CC=C1");
        Assertions.assertTrue(this.compareListsIgnoringOrder(new ArrayList<>(tmpFragmentsACList),
                new ArrayList<>(tmpExpectedSMILESList)));
    }
    /**
     * Method testing correct fragmentation with a basic example molecule.
     * This test focuses on safe retention of more complex ring systems, tested with Pentalene and Biphenyl.
     *
     * @throws InvalidSmilesException if SMILES is not correctly parsed of otherwise faulty
     * @throws CloneNotSupportedException if something goes wrong during cloning step in fragmentation
     */
    @Test
    public void basicTest04() throws InvalidSmilesException, CloneNotSupportedException {
        //test structure: C1CC2CCCC2C1
        SmilesParser tmpParser = new SmilesParser(SilentChemObjectBuilder.getInstance());
        IAtomContainer tmpTestStructureAC = tmpParser.parseSmiles("C1CC2CCCC2C1");
        AlkylStructureFragmenter tmpASF = this.getDefaultASFInstance(tmpTestStructureAC, false,
                false, true);
        List<String> tmpFragmentsACList = this.generateSMILESFromACList(tmpASF.fragmentMolecule(tmpTestStructureAC));
        List<String> tmpExpectedSMILESList = new ArrayList<>();
        tmpExpectedSMILESList.add("C1CC2CCCC2C1");
        Assertions.assertTrue(this.compareListsIgnoringOrder(new ArrayList<>(tmpFragmentsACList),
                new ArrayList<>(tmpExpectedSMILESList)));
        //test structure: C=1C=CC(=CC1)C2=CC=CC=C2
        tmpTestStructureAC = tmpParser.parseSmiles("C=1C=CC(=CC1)C2=CC=CC=C2");
        Assertions.assertFalse(tmpASF.shouldBeFiltered(tmpTestStructureAC));
        Assertions.assertFalse(tmpASF.shouldBePreprocessed(tmpTestStructureAC));
        Assertions.assertTrue(tmpASF.canBeFragmented(tmpTestStructureAC));
        tmpFragmentsACList = this.generateSMILESFromACList(tmpASF.fragmentMolecule(tmpTestStructureAC));
        tmpExpectedSMILESList.clear();
        tmpExpectedSMILESList.add("C=1C=CC(=CC1)C2=CC=CC=C2");
        Assertions.assertTrue(this.compareListsIgnoringOrder(new ArrayList<>(tmpFragmentsACList),
                new ArrayList<>(tmpExpectedSMILESList)));
    }
    /**
     * Method testing correct fragmentation with a basic example molecule.
     * This test focuses on separation of ring systems, tested with Bicyclohexyl.
     *
     * @throws InvalidSmilesException if SMILES is not correctly parsed of otherwise faulty
     * @throws CloneNotSupportedException if something goes wrong during cloning step in fragmentation
     */
    @Test
    public void basicTest05() throws InvalidSmilesException, CloneNotSupportedException {
        //test structure: C1CC(CCC1)C1CCCCC1
        SmilesParser tmpParser = new SmilesParser(SilentChemObjectBuilder.getInstance());
        IAtomContainer tmpTestStructureAC = tmpParser.parseSmiles("C1CC(CCC1)C1CCCCC1");
        AlkylStructureFragmenter tmpASF = this.getDefaultASFInstance(tmpTestStructureAC, false,
                false, true);
        List<String> tmpFragmentsACList = this.generateSMILESFromACList(tmpASF.fragmentMolecule(tmpTestStructureAC));
        List<String> tmpExpectedSMILESList = new ArrayList<>();
        tmpExpectedSMILESList.add("C1CCCCC1");
        tmpExpectedSMILESList.add("C1CCCCC1");
        Assertions.assertTrue(this.compareListsIgnoringOrder(new ArrayList<>(tmpFragmentsACList),
                new ArrayList<>(tmpExpectedSMILESList)));
    }
    /**
     * Method testing the correct handling of spiro configurated rings with an example molecule.
     * The molecule used in this test is Spiro[5.5]undecane.
     *
     * @throws InvalidSmilesException if SMILES can not be parsed
     * @throws CloneNotSupportedException if cloning fails
     */
    @Test
    public void basicTest06() throws InvalidSmilesException, CloneNotSupportedException {
        //test structure: C1CCC2(CCCCC2)CC1
        SmilesParser tmpParser = new SmilesParser(SilentChemObjectBuilder.getInstance());
        IAtomContainer tmpTestStructureAC = tmpParser.parseSmiles("C1CCC2(CCCCC2)CC1");
        AlkylStructureFragmenter tmpASF = this.getDefaultASFInstance(tmpTestStructureAC, false,
                false, true);
        List<String> tmpFragmentsACList = this.generateSMILESFromACList(tmpASF.fragmentMolecule(tmpTestStructureAC));
        List<String> tmpExpectedSMILESList = new ArrayList<>();
        tmpExpectedSMILESList.add("C1CCC2(CC1)CCCCC2");
        Assertions.assertTrue(this.compareListsIgnoringOrder(new ArrayList<>(tmpFragmentsACList),
                new ArrayList<>(tmpExpectedSMILESList)));
    }
    //ToDo: for every option, with better molecule (tert and quat)
    /**
     * Method testing the correct handling of tertiary and quaternary atoms attached to rings with an example molecule.
     * The molecule used in this test is a concept molecule comprised of a cyclohexane bonded to a quaternary carbon system.
     *
     * @throws InvalidSmilesException if SMILES can not be parsed
     * @throws CloneNotSupportedException if cloning fails
     */
    @Test
    public void basicTest07() throws InvalidSmilesException, CloneNotSupportedException {
        //test structure: C1CCCCC1C(C)(C)C
        SmilesParser tmpParser = new SmilesParser(SilentChemObjectBuilder.getInstance());
        IAtomContainer tmpTestStructureAC = tmpParser.parseSmiles("C1CCCCC1C(C)(C)C");
        AlkylStructureFragmenter tmpASF = this.getDefaultASFInstance(tmpTestStructureAC, false,
                false, true);
        tmpASF.setSeparateTertQuatCarbonFromRingSetting(false);
        List<String> tmpFragmentsACList = this.generateSMILESFromACList(tmpASF.fragmentMolecule(tmpTestStructureAC));
        List<String> tmpExpectedSMILESList = new ArrayList<>();
        tmpExpectedSMILESList.add("*C(*)(*)*");
        tmpExpectedSMILESList.add("C1CCCCC1");
        tmpExpectedSMILESList.add("C");
        tmpExpectedSMILESList.add("C");
        tmpExpectedSMILESList.add("C");
        Assertions.assertTrue(this.compareListsIgnoringOrder(new ArrayList<>(tmpFragmentsACList),
                new ArrayList<>(tmpExpectedSMILESList)));
    }
    /**
     * Method testing correct fragmentation with a specific example molecule.
     * This test focuses on extraction of ring systems, tested with a derivative of the molecule Hapalindole B after
     * standard fragmentation with the (in MORTAR included) ErtlFunctionalGroupsFinder.
     *
     * @throws InvalidSmilesException if SMILES is not correctly parsed of otherwise faulty
     * @throws CloneNotSupportedException if something goes wrong during cloning step in fragmentation
     */
    @Test
    public void specificTest01() throws InvalidSmilesException, CloneNotSupportedException {
        //test structure: CC1CCC2C(C1)C(=C)C3=C(C=CC=C3)C2(C)C (Hapalindole B Derivative)
        SmilesParser tmpParser = new SmilesParser(SilentChemObjectBuilder.getInstance());
        IAtomContainer tmpTestStructureAC = tmpParser.parseSmiles("CC1CCC2C(C1)C(=C)C3=C(C=CC=C3)C2(C)C");
        AlkylStructureFragmenter tmpASF = this.getDefaultASFInstance(tmpTestStructureAC, false,
                false, true);
        List<String> tmpFragmentsACList = this.generateSMILESFromACList(tmpASF.fragmentMolecule(tmpTestStructureAC));
        List<String> tmpExpectedSMILESList = new ArrayList<>();
        tmpExpectedSMILESList.add("C");
        tmpExpectedSMILESList.add("C=C1C=2C=CC=CC2CC3CCCCC13");
        tmpExpectedSMILESList.add("C");
        tmpExpectedSMILESList.add("C");
        Assertions.assertTrue(this.compareListsIgnoringOrder(new ArrayList<>(tmpFragmentsACList),
                new ArrayList<>(tmpExpectedSMILESList)));
    }
    /**
     * Method testing correct fragmentation with a specific example molecule.
     * This test focuses on extraction of quaternary carbon systems, tested with the molecule 4,4,6,6-Tetramethylnonane.
     *
     * @throws InvalidSmilesException if SMILES is not correctly parsed of otherwise faulty
     * @throws CloneNotSupportedException if something goes wrong during cloning step in fragmentation
     */
    @Test
    public void specificTest02() throws InvalidSmilesException, CloneNotSupportedException {
        //test structure: CCCC(C)(C)CC(C)(C)CCC
        SmilesParser tmpParser = new SmilesParser(SilentChemObjectBuilder.getInstance());
        IAtomContainer tmpTestStructureAC = tmpParser.parseSmiles("CCCC(C)(C)CC(C)(C)CCC");
        AlkylStructureFragmenter tmpASF = this.getDefaultASFInstance(tmpTestStructureAC, false,
                false, true);
        List<String> tmpFragmentsACList = this.generateSMILESFromACList(tmpASF.fragmentMolecule(tmpTestStructureAC));
        List<String> tmpExpectedSMILESList = new ArrayList<>();
        tmpExpectedSMILESList.add("C");
        tmpExpectedSMILESList.add("C");
        tmpExpectedSMILESList.add("C");
        tmpExpectedSMILESList.add("C");
        tmpExpectedSMILESList.add("C");
        tmpExpectedSMILESList.add("CCC");
        tmpExpectedSMILESList.add("CCC");
        tmpExpectedSMILESList.add("*C(*)(*)*");
        tmpExpectedSMILESList.add("*C(*)(*)*");
        Assertions.assertTrue(this.compareListsIgnoringOrder(new ArrayList<>(tmpFragmentsACList),
                new ArrayList<>(tmpExpectedSMILESList)));
    }
    /**
     * Method testing correct fragmentation with a specific example molecule.
     * This test focuses on extraction of a singular ring and separation of linear, conjugated Pi systems from side chains,
     * tested with a derivative of the molecule Dehydropinguisanin after fragmentation with the (in MORTAR included) ErtlFunctionalGroupsFinder.
     *
     * @throws InvalidSmilesException if SMILES is not correctly parsed of otherwise faulty
     * @throws CloneNotSupportedException if something goes wrong during cloning step in fragmentation
     */
    @Test
    public void specificTest03() throws InvalidSmilesException, CloneNotSupportedException {
        //test structure: C=CC=CCC1(C)C(C)CCC1C (Dehydropinguisanin)
        SmilesParser tmpParser = new SmilesParser(SilentChemObjectBuilder.getInstance());
        IAtomContainer tmpTestStructureAC = tmpParser.parseSmiles("C=CC=CCC1(C)C(C)CCC1C");
        AlkylStructureFragmenter tmpASF = this.getDefaultASFInstance(tmpTestStructureAC, false,
                false, true);
        List<String> tmpFragmentsACList = this.generateSMILESFromACList(tmpASF.fragmentMolecule(tmpTestStructureAC));
        List<String> tmpExpectedSMILESList = new ArrayList<>();
        tmpExpectedSMILESList.add("C=CC=C");
        tmpExpectedSMILESList.add("C");
        tmpExpectedSMILESList.add("C");
        tmpExpectedSMILESList.add("C");
        tmpExpectedSMILESList.add("C");
        tmpExpectedSMILESList.add("C1CCCC1");
        Assertions.assertTrue(this.compareListsIgnoringOrder(new ArrayList<>(tmpFragmentsACList),
                new ArrayList<>(tmpExpectedSMILESList)));
    }
    /**
     * Test for correct deepCopy methods by copying a butene molecule which used to make problems in earlier versions.
     *
     * @throws FileNotFoundException if test structures file can not be found or accessed
     * @throws URISyntaxException if syntax of path to test structures file is wrong
     */
    @Test
    public void deepCopyButeneTest() throws FileNotFoundException, URISyntaxException {
        AlkylStructureFragmenter tmpASF = new AlkylStructureFragmenter();
        tmpASF.setFragmentSaturationSetting(IMoleculeFragmenter.FRAGMENT_SATURATION_OPTION_DEFAULT);
        tmpASF.setKeepNonFragmentableMoleculesSetting(AlkylStructureFragmenter.KEEP_NON_FRAGMENTABLE_MOLECULES_SETTING_DEFAULT);
        tmpASF.setFragmentSideChainsSetting(AlkylStructureFragmenter.FRAGMENT_SIDE_CHAINS_SETTING_DEFAULT);
        tmpASF.setMaxChainLengthSetting(AlkylStructureFragmenter.MAX_CHAIN_LENGTH_SETTING_DEFAULT);
        tmpASF.setIsolateQuatCarbonsSetting(AlkylStructureFragmenter.ISOLATE_TERT_QUAT_CARBONS_SETTING_DEFAULT);
        tmpASF.setSeparateTertQuatCarbonFromRingSetting(AlkylStructureFragmenter.SEPARATE_TERT_QUAT_CARBON_FROM_RING_SETTING_DEFAULT);
        IAtomContainerSet tmpReadAtomContainerSet = this.readStructuresToACSet("de.unijena.cheminf.mortar.model.fragmentation.algorithm.ASF/ASF_Butene_Test.mol");
        IAtomContainer tmpButeneAC = tmpReadAtomContainerSet.getAtomContainer(0);
        //two steps below needed for correct internal index handling
        tmpASF.fillAtomArray(tmpButeneAC);
        tmpASF.fillBondArray(tmpButeneAC);
        IAtomContainer tmpCopyAC = tmpButeneAC.getBuilder().newAtomContainer();
        for (IAtom tmpAtom: tmpButeneAC.atoms()) {
            tmpCopyAC.addAtom(tmpASF.deepCopyAtom(tmpAtom));
        }
        for (IBond tmpBond: tmpButeneAC.bonds()) {
            tmpCopyAC.addBond(deepCopyBond(tmpBond, tmpCopyAC));
        }
        //Comparison of original and copied AtomContainer
        String tmpButeneSMILES = ChemUtil.createUniqueSmiles(tmpButeneAC, false);
        String tmpCopySMILES = ChemUtil.createUniqueSmiles(tmpCopyAC, false);
        Assertions.assertEquals(tmpButeneSMILES, tmpCopySMILES);
    }
    /**
     * Test for correct extraction of isolated double bonds found in linear residues after extraction of all other groups that are deemed interesting.
     *
     * @throws CloneNotSupportedException if cloning of original molecule in AlkylStructureFragmenter is faulty
     * @throws FileNotFoundException if test structures file can not be found or accessed
     * @throws URISyntaxException if syntax of path to test structures file is not correct
     */
    @Test
    public void linearDoubleBondExtractionTest() throws CloneNotSupportedException, FileNotFoundException, URISyntaxException {
        IAtomContainerSet tmpReadAtomContainerSet = this.readStructuresToACSet("de.unijena.cheminf.mortar.model.fragmentation.algorithm.ASF/ASF_Butene_Test.mol");
        IAtomContainer tmpButeneAC = tmpReadAtomContainerSet.getAtomContainer(0);
        AlkylStructureFragmenter tmpASF = this.getDefaultASFInstance(tmpButeneAC, false, false, true);
        List<IAtomContainer> tmpFragmentACList = tmpASF.fragmentMolecule(tmpButeneAC);
        List<String> tmpExpectedList = new ArrayList<>(3);
        tmpExpectedList.add("C");
        tmpExpectedList.add("C");
        tmpExpectedList.add("C=C");
        List<String> tmpFragmentStringList = new ArrayList<>();
        for (IAtomContainer tmpAC: tmpFragmentACList) {
            tmpFragmentStringList.add(ChemUtil.createUniqueSmiles(tmpAC, false));
        }
        Assertions.assertTrue(this.compareListsIgnoringOrder((ArrayList) tmpExpectedList, (ArrayList) tmpFragmentStringList));
    }
    /**
     * Method to test a default alkyl structure fragmentation on a concept molecule covering a broad range of resulting fragments.
     *
     * @throws Exception if fragmentation does not result in expected fragments
     */
    @Test
    public void defaultFragmentationTest() throws Exception {
        SmilesParser tmpParser = new SmilesParser(SilentChemObjectBuilder.getInstance());
        //test structure: C12=CC(=CC=C1C(=CC(=C2)C3CCCCC3)CC(C)(C)C/C=C/CCC)/C=C\C(C)C
        IAtomContainer tmpTestStructureAC = tmpParser.parseSmiles("C12=CC(=CC=C1C(=CC(=C2)C3CCCCC3)CC(C)(C)C/C=C/CCC)/C=C\\C(C)C");
        AlkylStructureFragmenter tmpASF = this.getDefaultASFInstance(tmpTestStructureAC, false, false, true);
        List<String> tmpResultSMILESList = this.generateSMILESFromACList(tmpASF.fragmentMolecule(tmpTestStructureAC));
        List<String> tmpExpectedSMILESList = new ArrayList<>(12);
        tmpExpectedSMILESList.add("C");
        tmpExpectedSMILESList.add("C");
        tmpExpectedSMILESList.add("C");
        tmpExpectedSMILESList.add("C");
        tmpExpectedSMILESList.add("C");
        tmpExpectedSMILESList.add("C");
        tmpExpectedSMILESList.add("*C(*)(*)*");
        tmpExpectedSMILESList.add("*C(*)*");
        tmpExpectedSMILESList.add("C=CC1=CC=C2C=CC=CC2=C1");
        tmpExpectedSMILESList.add("C1CCCCC1");
        tmpExpectedSMILESList.add("C=C");
        tmpExpectedSMILESList.add("CCC");
        Assertions.assertTrue(this.compareListsIgnoringOrder(new ArrayList<>(tmpResultSMILESList),
                new ArrayList<>(tmpExpectedSMILESList)));
    }

    /**
     * Example fragmentation using natural compounds CNP0321029.1; CNP0509407.0 and CNP0242949.0. The natural compounds
     * used in this test can be found in the natural product database COCONUT @see <a href="https://coconut.naturalproducts.net/"<a/>.
     *
     * @throws Exception if molecule is not correctly fragmented
     */
    @Test
    public void naturalCompoundFragmentationTest() throws Exception {
        SmilesParser tmpParser = new SmilesParser(SilentChemObjectBuilder.getInstance());
        //test structure: CNP0321029.1
        IAtomContainer tmpTestStructureAC = tmpParser.parseSmiles("C[C@@H]1C[C@@H](C)[C@@H](C)C1");
        AlkylStructureFragmenter tmpASF = this.getDefaultASFInstance(tmpTestStructureAC, false,
                false, true);
        List<String> tmpResultSMILESList = this.generateSMILESFromACList(tmpASF.fragmentMolecule(tmpTestStructureAC));
        List<String> tmpExpectedSMILESList = new ArrayList<>(5);
        tmpExpectedSMILESList.add("C");
        tmpExpectedSMILESList.add("C");
        tmpExpectedSMILESList.add("C");
        tmpExpectedSMILESList.add("C1CCCC1");
        Assertions.assertTrue(this.compareListsIgnoringOrder(new ArrayList<>(tmpResultSMILESList),
                new ArrayList<>(tmpExpectedSMILESList)));
        //test structure: CNP0509407.0
        tmpTestStructureAC = tmpParser.parseSmiles("CCCCCCCCCC(CC)(CCCCCCC)CCCCCCCC");
        tmpASF = this.getDefaultASFInstance(tmpTestStructureAC, false,
                false, true);
        tmpResultSMILESList = this.generateSMILESFromACList(tmpASF.fragmentMolecule(tmpTestStructureAC));
        tmpExpectedSMILESList.clear();
        tmpExpectedSMILESList.add("CCCCCC");
        tmpExpectedSMILESList.add("CCCCCC");
        tmpExpectedSMILESList.add("CCCCCC");
        tmpExpectedSMILESList.add("CCC");
        tmpExpectedSMILESList.add("CC");
        tmpExpectedSMILESList.add("CC");
        tmpExpectedSMILESList.add("C");
        tmpExpectedSMILESList.add("*C(*)(*)*");
        Assertions.assertTrue(this.compareListsIgnoringOrder(new ArrayList<>(tmpResultSMILESList),
                new ArrayList<>(tmpExpectedSMILESList)));
        //test structure: CNP0242949.0
        tmpTestStructureAC = tmpParser.parseSmiles("CC1CCCCCCCCC1");
        tmpASF = this.getDefaultASFInstance(tmpTestStructureAC, false,
                false, true);
        tmpResultSMILESList = this.generateSMILESFromACList(tmpASF.fragmentMolecule(tmpTestStructureAC));
        tmpExpectedSMILESList.clear();
        tmpExpectedSMILESList.add("C");
        tmpExpectedSMILESList.add("C1CCCCCCCCC1");
        Assertions.assertTrue(this.compareListsIgnoringOrder(new ArrayList<>(tmpResultSMILESList),
                new ArrayList<>(tmpExpectedSMILESList)));
    }

    /**
     * Simple test testing for chemical formula correctness in the same way it is done in the fragmenter.
     *
     * @throws InvalidSmilesException if SMILES cannot be parsed.
     * @throws CloneNotSupportedException if molecule cloning fails.
     */
    @Test
    public void chemicalFormulaTest() throws InvalidSmilesException, CloneNotSupportedException {
        SmilesParser tmpParser = new SmilesParser(SilentChemObjectBuilder.getInstance());
        IAtomContainer tmpAtomContainer = tmpParser.parseSmiles("C1CCCCCC1");
        int tmpPreFragmentationCount = 0;
        int tmpPostFragmentationCount = 0;
        for (IAtom tmpAtom: tmpAtomContainer.atoms()) {
            if (tmpAtom.getAtomicNumber() != 0 && tmpAtom != null) {
                tmpPreFragmentationCount++;
            }
        }
        AlkylStructureFragmenter tmpASF = this.getDefaultASFInstance(tmpAtomContainer, false,
                false, true);
        List<IAtomContainer> tmpACList = tmpASF.fragmentMolecule(tmpAtomContainer);
        for (IAtomContainer tmpAC: tmpACList) {
            for (IAtom tmpAtom: tmpAC.atoms()) {
                if (tmpAtom.getAtomicNumber() != 0 && tmpAtom != null) {
                    tmpPostFragmentationCount++;
                }
            }
        }
        Assertions.assertEquals(tmpPreFragmentationCount, tmpPostFragmentationCount);
    }
    //</editor-fold>

    //<editor-fold desc="Inner Class 'Molecular Arrays'">

    //ToDo: implement check for bonds
    @Test
    public void fillMolecularArraysTest() throws InvalidSmilesException, NoSuchFieldException, IllegalAccessException {
        SmilesParser tmpParser = new SmilesParser(SilentChemObjectBuilder.getInstance());
        IAtomContainer tmpMolecule = tmpParser.parseSmiles("CCCCCCCCCCCC");
        MolecularArrays refMolecularArrays = new MolecularArrays(tmpMolecule);
        //reflection is used to manually insert/inflict a null value
        Field atomsField = AtomContainer.class.getDeclaredField("atoms");
        Field bondsField = AtomContainer.class.getDeclaredField("bonds");
        atomsField.setAccessible(true);
        bondsField.setAccessible(true);
        IBond[] bonds = new IBond[11];
        //"getter" for BaseAtomRef
        Object atoms = atomsField.get(tmpMolecule);
        Object[] newAtoms = (Object[]) Array.newInstance(atoms.getClass().getComponentType(), Array.getLength(atoms));
        //fill newAtoms with actual atoms
        for (int i = 0; i < 12; i++) {
               newAtoms[i] = tmpMolecule.getAtom(i);
               if (i < 11) {
                   bonds[i] = tmpMolecule.getBond(i);
               }
        }
        //set position 5 null
        newAtoms[5] = null;
        bonds[5] = null;
        //insert the fields back into tmpMolecule
        atomsField.set(tmpMolecule, newAtoms);
        bondsField.set(tmpMolecule, bonds);
        //test for expected discrepancy
        MolecularArrays testMolecularArrays = new MolecularArrays(tmpMolecule);
        Assertions.assertNotEquals(testMolecularArrays.getAtomArray().length, refMolecularArrays.getAtomArray().length);
        Assertions.assertNotEquals(testMolecularArrays.getBondArray().length, refMolecularArrays.getBondArray().length);
    }
    //</editor-fold

    //<editor-fold desc="Private Utility Methods">
    /**
     * Compares two provided lists on equality while ignoring the lists' orders.
     *
     * @param aList1 First given list to compare
     * @param aList2 Second given list to compare
     * @return boolean whether given lists are equal
     */
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
     * Utility method to generate SMILES notation for a singular IAtomContainer instance.
     *
     * @param anAtomContainer molecule to generate SMILES fora
     * @return SMILES String for given molecule
     * @throws CDKException if SMILES can not be generated for given atomcontainer
     */
    private String generateSmilesFromAC(IAtomContainer anAtomContainer) throws CDKException {
        SmilesGenerator tmpGenerator = new SmilesGenerator(SmiFlavor.Default);
        return tmpGenerator.create(anAtomContainer);
    }
    /**
     * Utility method generating SMILES notation strings for a given AtomContainerSet.
     *
     * @param anACSet given AtomContainerSet
     * @return List containing the generated Strings
     */
    private List<String> generateSMILESFromACSet(IAtomContainerSet anACSet) {
        List<String> tmpSmilesList = new ArrayList<>(anACSet.getAtomContainerCount());
        for (IAtomContainer tmpAC : anACSet.atomContainers()) {
            tmpSmilesList.add(ChemUtil.createUniqueSmiles(tmpAC, false));
        }
        return tmpSmilesList;
    }
    /**
     * Utility method to generate SMILES notation strings for a given List containing IAtomContainer instances.
     *
     * @param anACList given List of IAtomContainers
     * @return List with SMILES Strings of originally given List of IAtomContainers
     */
    private List<String> generateSMILESFromACList(List<IAtomContainer> anACList) {
        List<String> tmpReturnSmilesList = new ArrayList<>(anACList.size());
        for (IAtomContainer tmpAC: anACList) {
            tmpReturnSmilesList.add(ChemUtil.createUniqueSmiles(tmpAC, false));
        }
        return tmpReturnSmilesList;
    }
    /**
     * Private method to read a given structure file to a CDK atomcontainer set.
     *
     * @param aFileName Name of the file to read from
     * @return IAtomContainerSet with the read structures as AtomContainers
     * @throws FileNotFoundException if no file with the given name can be located
     * @throws URISyntaxException if given name of file cannot be parsed as URI reference
     */
    private IAtomContainerSet readStructuresToACSet(String aFileName) throws FileNotFoundException, URISyntaxException {
        URL tmpURL = this.getClass().getResource("/" + aFileName);
        File tmpResourceFile = Paths.get(Objects.requireNonNull(tmpURL).toURI()).toFile();
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
    /**
     * Private method returning a local instance of AlkylStructureFragmenter with default settings for a given molecule.
     *
     * @param aMolecule given molecule to be assessed
     * @param aShouldBeFilteredStatement boolean value to set pre-fragmentation task
     * @param aShouldBePreprocessedStatement boolean value to set pre-fragmentation task
     * @param aCanBeFragmentedStatement boolean value to set pre-fragmentation task
     * @return AlkylStructureFragmenter instance with default settings
     */
    private AlkylStructureFragmenter getDefaultASFInstance(IAtomContainer aMolecule, boolean aShouldBeFilteredStatement,
                                                           boolean aShouldBePreprocessedStatement, boolean aCanBeFragmentedStatement) {
        AlkylStructureFragmenter tmpASF = new AlkylStructureFragmenter();
        //manual set of setting values to default values
        tmpASF.setFragmentSaturationSetting(FragmentSaturationOption.HYDROGEN_SATURATION);
        tmpASF.setKeepNonFragmentableMoleculesSetting(true);
        tmpASF.setFragmentSideChainsSetting(true);
        tmpASF.setMaxChainLengthSetting(6);
        tmpASF.setIsolateQuatCarbonsSetting(true);
        tmpASF.setSeparateTertQuatCarbonFromRingSetting(false);
        //assertions for non-set-able pre-fragmentation tasks
        if (aShouldBeFilteredStatement) {
            Assertions.assertTrue(tmpASF.shouldBeFiltered(aMolecule));
        } else {
            Assertions.assertFalse(tmpASF.shouldBeFiltered(aMolecule));
        }
        if (aShouldBePreprocessedStatement) {
            Assertions.assertTrue(tmpASF.shouldBePreprocessed(aMolecule));
        } else {
            Assertions.assertFalse(tmpASF.shouldBePreprocessed(aMolecule));
        }
        if (aCanBeFragmentedStatement) {
            Assertions.assertTrue(tmpASF.canBeFragmented(aMolecule));
        } else {
            Assertions.assertFalse(tmpASF.canBeFragmented(aMolecule));
        }
        return tmpASF;
    }
    /**
     * Private method to check and compare the correctness of chemical formulas before and after fragmentation.
     *
     * @param aMolecule the original molecule before fragmentation
     * @param anExpectedACSet the expected fragments of a given molecule
     * @param aResultACSet the resulting fragments of a fragmented given molecule
     * @return true if formula was constant, false if not
     */
    private boolean checkChemicalFormula(IAtomContainer aMolecule, IAtomContainerSet anExpectedACSet, IAtomContainerSet aResultACSet) {
        int tmpPreFragmentationAtomCount = 0;
        for (IAtom tmpAtom: aMolecule.atoms()) {
            if (tmpAtom.getAtomicNumber() != 0) {
                tmpPreFragmentationAtomCount++;
            }
        }
        int tmpResultPostFragmentationAtomCount = 0;
        int tmpExpPostFragmentationAtomCount = 0;
        for (IAtomContainer tmpAtomContainer: anExpectedACSet.atomContainers()) {
            for (IAtom tmpAtom: tmpAtomContainer.atoms()) {
                if (tmpAtom.getAtomicNumber() != 0) {
                    tmpExpPostFragmentationAtomCount++;
                }
            }
        }
        for (IAtomContainer tmpAtomContainer: aResultACSet.atomContainers()) {
            for (IAtom tmpAtom: tmpAtomContainer.atoms()) {
                if (tmpAtom.getAtomicNumber() != 0)
                    tmpResultPostFragmentationAtomCount++;
            }
        }
        return tmpResultPostFragmentationAtomCount == tmpPreFragmentationAtomCount
                && tmpResultPostFragmentationAtomCount == tmpExpPostFragmentationAtomCount;
    }
    //</editor-fold>
}

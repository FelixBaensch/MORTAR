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
import org.junit.jupiter.api.Disabled;
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
import org.openscience.cdk.smiles.SmilesParser;

import java.io.File;
import java.io.FileInputStream;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
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
public class AlkylStructureFragmenterTest extends AlkylStructureFragmenter {
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
    private AlkylStructureFragmenterTest() {}

    /**
     * Method to showcase the usage of the AlkylStructureFragmenter class.
     * The molecules used in this 'test' are all molecules used in the tests below, complemented by various substructures
     * the class will detect and return intact.
     */
    @Test
    public void exampleUsageTest() throws Exception {
        //get file reader with test structures file
        URL tmpURL = this.getClass().getResource("ASF_Test_Substructure_Library.sdf");
        Assertions.assertNotNull(tmpURL);
        File tmpResourceFile = Paths.get(tmpURL.toURI()).toFile();
        IteratingSDFReader tmpSDFReader = new IteratingSDFReader(new FileInputStream(tmpResourceFile),
                SilentChemObjectBuilder.getInstance());
        //read structures into AtomContainerSet
        IAtomContainerSet tmpStructuresSet = new AtomContainerSet();
        while (tmpSDFReader.hasNext()) {
            tmpStructuresSet.addAtomContainer(tmpSDFReader.next());
        }
        //get file reader with test structures expected fragments file
        URL tmpExpectedFragmentsURL = this.getClass().getResource("ASF_Test_Substructure_Library_Fragments_Strings");
        Assertions.assertNotNull(tmpExpectedFragmentsURL);
        List<String> tmpExpectedSMILESStringList = Files.readAllLines(
                Path.of(tmpExpectedFragmentsURL.toURI()),
                StandardCharsets.UTF_8);
        //constructs an AlkylStructureFragmenter with default settings
        AlkylStructureFragmenter tmpASF = new AlkylStructureFragmenter();
        //generate fragments and add SMILES to fragment list
        List<String> tmpFragmentsSMILESList = new ArrayList<>();
        for (IAtomContainer tmpAC: tmpStructuresSet.atomContainers()) {
            this.preprocessTestMolecule(tmpASF, tmpAC,
                    false, false, true);
            tmpFragmentsSMILESList.addAll(this.generateSMILESFromACList(tmpASF.fragmentMolecule(tmpAC)));
        }
        //check if match
        boolean tmpTestSuccessful = this.compareListsIgnoringOrder(tmpExpectedSMILESStringList, tmpFragmentsSMILESList);
        if (!tmpTestSuccessful) {
            System.out.println(tmpFragmentsSMILESList);
            Assertions.fail();
        }
    }

    //<editor-fold desc="Disabled Unit Tests">
    /**
     * Method for unit testing the internal method AlkylStructureFragmenter.extractFragments().
     * The molecule used for testing is a conceptual molecule containing several key structural features detected and
     * extracted by the AlkylStructureFragmenter - an isolated double bond, a quaternary C, an aromatic ring system with
     * a connected conjugated system, an isolated cyclohexane ring and a tertiary C - in default settings.
     *
     * @throws CDKException if SMILES cannot be parsed correctly
     */
    @Disabled
    @Test
    public void extractFragmentsTest() throws CDKException {
        ArrayList<String> tmpExpectedFragmentList = new ArrayList<>(6);
        tmpExpectedFragmentList.add("C=CC1=CC=C2C=CC=CC2=C1");
        tmpExpectedFragmentList.add("CC(C)(C)C");
        tmpExpectedFragmentList.add("C=C");
        tmpExpectedFragmentList.add("CCC");
        tmpExpectedFragmentList.add("CCC");
        tmpExpectedFragmentList.add("C1CCCCC1");
        SmilesParser tmpParser = new SmilesParser(SilentChemObjectBuilder.getInstance());
        IAtomContainer tmpTestAC = tmpParser.parseSmiles("C12=CC(=CC=C1C(=CC(=C2)C3CCCCC3)CC(C)(C)C/C=C/CCC)/C=C\\C(C)C");
        MolecularArrays tmpTestMolecularArrays = new MolecularArrays(tmpTestAC);
        AlkylStructureFragmenter tmpASF = new AlkylStructureFragmenter();
        tmpASF.markTertQuatAndNeighbors(tmpTestMolecularArrays);
        tmpASF.markRings(tmpTestMolecularArrays, tmpTestAC);
        tmpASF.markConjugatedPiSystems(tmpTestMolecularArrays);
        tmpASF.markMultiBonds(tmpTestMolecularArrays);
        IAtomContainerSet tmpExtractedFrag;
        ArrayList<String> tmpActualFragmentList = new ArrayList<>(6);
        try {
            tmpExtractedFrag = tmpASF.extractFragments(tmpTestMolecularArrays);
            for (IAtomContainer tmpAC : tmpExtractedFrag.atomContainers()) {
                ChemUtil.saturateWithHydrogen(tmpAC);
                tmpActualFragmentList.add(ChemUtil.createUniqueSmiles(tmpAC, false));
            }
        } catch (CDKException e) {
            Assertions.fail();
        }
        System.out.println("extractFragmentsTest: Expected: " + tmpExpectedFragmentList + "; Actual Fragments: "+ tmpActualFragmentList);
        Assertions.assertTrue(this.compareListsIgnoringOrder(tmpActualFragmentList, tmpExpectedFragmentList));
    }

    @Disabled
    @Test
    public void testMarkTertQuatAndNeighbors() throws InvalidSmilesException{
        SmilesParser tmpParser = new SmilesParser(SilentChemObjectBuilder.getInstance());
        IAtomContainer tmpMolecule = tmpParser.parseSmiles("CC(C)(C)CCCC(C)C");
        MolecularArrays tmpTestMolecularArrays = new MolecularArrays(tmpMolecule);
        //currently no mark generatable
        markTertQuatAndNeighbors(tmpTestMolecularArrays);
        for (IAtom tmpAtom: tmpTestMolecularArrays.getAtomArray()) {
            if ((boolean) tmpAtom.getProperty(AlkylStructureFragmenter.INTERNAL_ASF_TERTIARY_CARBON_PROPERTY_KEY)) {
                System.out.println(tmpAtom.getProperties());
            }
        }
        //create expected arrays
        MolecularArrays tmpExpectedMolecularArrays = new MolecularArrays(tmpMolecule);
        //create atom array with expected atoms
        IAtom[] tmpExpectedAtomArray = tmpExpectedMolecularArrays.getAtomArray();
        //example:
        //tmpExpectedAtomArray[0].setProperty(AlkylStructureFragmenter.INTERNAL_ASF_TERTIARY_CARBON_PROPERTY_KEY, true);
        //create bond array with expected bonds
        IBond[] tmpExpectedBondArray = tmpExpectedMolecularArrays.getBondArray();
        //example:
        //tmpExpectedBondArray[0].setProperty(AlkylStructureFragmenter.INTERNAL_ASF_TERTIARY_CARBON_PROPERTY_KEY, true);
        tmpExpectedMolecularArrays.setAtomArray(tmpExpectedAtomArray);
        tmpExpectedMolecularArrays.setBondArray(tmpExpectedBondArray);
        //custom assert for MolecularArrays match/equal
        Assertions.assertTrue(this.assertMolecularArraysEquals(tmpExpectedMolecularArrays, tmpTestMolecularArrays));
    }
    @Disabled
    @Test
    public void testMarkRings() throws InvalidSmilesException{
        SmilesParser tmpParser = new SmilesParser(SilentChemObjectBuilder.getInstance());
        IAtomContainer tmpMolecule = tmpParser.parseSmiles("CC(C)(C)CCCC(C)C");
        MolecularArrays tmpTestMolecularArrays = new MolecularArrays(tmpMolecule);
        //currently no mark generatable
        markRings(tmpTestMolecularArrays, tmpMolecule);
        for (IAtom tmpAtom: tmpTestMolecularArrays.getAtomArray()) {
            System.out.println(tmpAtom.getProperties());
        }
        //create expected arrays
        MolecularArrays tmpExpectedMolecularArrays = new MolecularArrays(tmpMolecule);
        //create atom array with expected atoms
        IAtom[] tmpExpectedAtomArray = tmpExpectedMolecularArrays.getAtomArray();
        //example:
        //tmpExpectedAtomArray[0].setProperty(AlkylStructureFragmenter.INTERNAL_ASF_RING_PROPERTY_KEY, true);
        //create bond array with expected bonds
        IBond[] tmpExpectedBondArray = tmpExpectedMolecularArrays.getBondArray();
        //example:
        //tmpExpectedBondArray[0].setProperty(AlkylStructureFragmenter.INTERNAL_ASF_RING_PROPERTY_KEY, true);
        tmpExpectedMolecularArrays.setAtomArray(tmpExpectedAtomArray);
        tmpExpectedMolecularArrays.setBondArray(tmpExpectedBondArray);
        //custom assert for MolecularArrays match/equal
        Assertions.assertTrue(this.assertMolecularArraysEquals(tmpExpectedMolecularArrays, tmpTestMolecularArrays));
    }
    @Disabled
    @Test
    public void testMarkMultiBonds() throws InvalidSmilesException{
        SmilesParser tmpParser = new SmilesParser(SilentChemObjectBuilder.getInstance());
        IAtomContainer tmpMolecule = tmpParser.parseSmiles("CC(C)(C)CCCC(C)C");
        MolecularArrays tmpTestMolecularArrays = new MolecularArrays(tmpMolecule);
        //currently no mark generatable
        markMultiBonds(tmpTestMolecularArrays);
        for (IAtom tmpAtom: tmpTestMolecularArrays.getAtomArray()) {
            System.out.println(tmpAtom.getProperties());
        }
        //create expected arrays
        MolecularArrays tmpExpectedMolecularArrays = new MolecularArrays(tmpMolecule);
        //create atom array with expected atoms
        IAtom[] tmpExpectedAtomArray = tmpExpectedMolecularArrays.getAtomArray();
        //example:
        //tmpExpectedAtomArray[0].setProperty(AlkylStructureFragmenter.INTERNAL_ASF_DOUBLE_PROPERTY_KEY, true);
        //create bond array with expected bonds
        IBond[] tmpExpectedBondArray = tmpExpectedMolecularArrays.getBondArray();
        //example:
        //tmpExpectedBondArray[0].setProperty(AlkylStructureFragmenter.INTERNAL_ASF_DOUBLE_PROPERTY_KEY, true);
        tmpExpectedMolecularArrays.setAtomArray(tmpExpectedAtomArray);
        tmpExpectedMolecularArrays.setBondArray(tmpExpectedBondArray);
        //custom assert for MolecularArrays match/equal
        Assertions.assertTrue(this.assertMolecularArraysEquals(tmpExpectedMolecularArrays, tmpTestMolecularArrays));
    }
    @Disabled
    @Test
    public void testMarkConnectedTertQuatRing() throws InvalidSmilesException{
        SmilesParser tmpParser = new SmilesParser(SilentChemObjectBuilder.getInstance());
        IAtomContainer tmpMolecule = tmpParser.parseSmiles("CC(C)(C)CCCC(C)C");
        MolecularArrays tmpTestMolecularArrays = new MolecularArrays(tmpMolecule);
        //currently no mark generatable
        markConnectedTertQuatRing(tmpTestMolecularArrays);
        for (IAtom tmpAtom: tmpTestMolecularArrays.getAtomArray()) {
            if ((boolean) tmpAtom.getProperty(AlkylStructureFragmenter.INTERNAL_ASF_TERTIARY_CARBON_PROPERTY_KEY)) {
                System.out.println(tmpAtom.getProperties());
            }
        }
        //create expected arrays
        MolecularArrays tmpExpectedMolecularArrays = new MolecularArrays(tmpMolecule);
        //create atom array with expected atoms
        IAtom[] tmpExpectedAtomArray = tmpExpectedMolecularArrays.getAtomArray();
        //example:
        //tmpExpectedAtomArray[0].setProperty(AlkylStructureFragmenter.INTERNAL_ASF_TERTIARY_CARBON_PROPERTY_KEY, true);
        //create bond array with expected bonds
        IBond[] tmpExpectedBondArray = tmpExpectedMolecularArrays.getBondArray();
        //example:
        //tmpExpectedBondArray[0].setProperty(AlkylStructureFragmenter.INTERNAL_ASF_TERTIARY_CARBON_PROPERTY_KEY, true);
        tmpExpectedMolecularArrays.setAtomArray(tmpExpectedAtomArray);
        tmpExpectedMolecularArrays.setBondArray(tmpExpectedBondArray);
        //custom assert for MolecularArrays match/equal
        Assertions.assertTrue(this.assertMolecularArraysEquals(tmpExpectedMolecularArrays, tmpTestMolecularArrays));
    }
    //</editor-fold>
    //
    //<editor-fold desc="Unit Tests">
    /**
     * Method to test the internal algorithm for detecting and marking conjugated pi bond systems.
     * The used molecule to test this functionality is a short C5-chain with alternating double bonds.
     *
     * @throws InvalidSmilesException if SMILES-parser is not able to parse SMILES due to incorrect syntax
     */
    @Test
    public void testMarkConjugatedPiSystems() throws InvalidSmilesException{
        SmilesParser tmpParser = new SmilesParser(SilentChemObjectBuilder.getInstance());
        IAtomContainer tmpMolecule = tmpParser.parseSmiles("C=CC=CC=C");
        MolecularArrays tmpTestMolecularArrays = new MolecularArrays(tmpMolecule);
        //mark actual atoms and bonds
        this.markConjugatedPiSystems(tmpTestMolecularArrays);
//        for (IAtom tmpAtom: tmpTestMolecularArrays.getAtomArray()) {
//            System.out.println(tmpAtom.getProperties());
//        }
        //create expected arrays
        MolecularArrays tmpExpectedMolecularArrays = new MolecularArrays(tmpMolecule);
        //create atom array with expected atoms
        IAtom[] tmpExpectedAtomArray = tmpExpectedMolecularArrays.getAtomArray();
        tmpExpectedAtomArray[0].setProperty(AlkylStructureFragmenter.INTERNAL_ASF_CONJ_PI_MARKER_KEY, true);
        tmpExpectedAtomArray[1].setProperty(AlkylStructureFragmenter.INTERNAL_ASF_CONJ_PI_MARKER_KEY, true);
        tmpExpectedAtomArray[2].setProperty(AlkylStructureFragmenter.INTERNAL_ASF_CONJ_PI_MARKER_KEY, true);
        tmpExpectedAtomArray[3].setProperty(AlkylStructureFragmenter.INTERNAL_ASF_CONJ_PI_MARKER_KEY, true);
        tmpExpectedAtomArray[4].setProperty(AlkylStructureFragmenter.INTERNAL_ASF_CONJ_PI_MARKER_KEY, true);
        tmpExpectedAtomArray[5].setProperty(AlkylStructureFragmenter.INTERNAL_ASF_CONJ_PI_MARKER_KEY, true);
        //create bond array with expected bonds
        IBond[] tmpExpectedBondArray = tmpExpectedMolecularArrays.getBondArray();
        tmpExpectedBondArray[0].setProperty(AlkylStructureFragmenter.INTERNAL_ASF_CONJ_PI_MARKER_KEY, true);
        tmpExpectedBondArray[1].setProperty(AlkylStructureFragmenter.INTERNAL_ASF_CONJ_PI_MARKER_KEY, true);
        tmpExpectedBondArray[2].setProperty(AlkylStructureFragmenter.INTERNAL_ASF_CONJ_PI_MARKER_KEY, true);
        tmpExpectedBondArray[3].setProperty(AlkylStructureFragmenter.INTERNAL_ASF_CONJ_PI_MARKER_KEY, true);
        tmpExpectedBondArray[4].setProperty(AlkylStructureFragmenter.INTERNAL_ASF_CONJ_PI_MARKER_KEY, true);
        tmpExpectedMolecularArrays.setAtomArray(tmpExpectedAtomArray);
        tmpExpectedMolecularArrays.setBondArray(tmpExpectedBondArray);
        //custom assert for MolecularArrays match/equal
        Assertions.assertTrue(this.assertMolecularArraysEquals(tmpExpectedMolecularArrays, tmpTestMolecularArrays));
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
            if (!this.isPseudoAtom(tmpAtom)) {
                tmpPreFragmentationCount++;
            }
        }
        AlkylStructureFragmenter tmpASF = new AlkylStructureFragmenter();
        this.preprocessTestMolecule(tmpASF, tmpAtomContainer,
                false, false, true);
        List<IAtomContainer> tmpACList = tmpASF.fragmentMolecule(tmpAtomContainer);
        for (IAtomContainer tmpAC: tmpACList) {
            for (IAtom tmpAtom: tmpAC.atoms()) {
                if (!this.isPseudoAtom(tmpAtom)) {
                    tmpPostFragmentationCount++;
                }
            }
        }
        Assertions.assertEquals(tmpPreFragmentationCount, tmpPostFragmentationCount);
    }
    /**
     * Test for correct deepCopy methods by copying a butene molecule which used to make problems in earlier versions.
     * A seemingly unused MolecularArrays instance is instanced here. This is needed for correct filling of internal atom
     * and bond arrays, since they are created and filled in the MolecularArrays constructor.
     *
     * @throws InvalidSmilesException if SMILES parser fails to parse SMILES from String with incorrect syntax
     */
    @Test
    public void deepCopyButeneTest() throws InvalidSmilesException {
        AlkylStructureFragmenter tmpASF = new AlkylStructureFragmenter();
        SmilesParser tmpParser = new SmilesParser(SilentChemObjectBuilder.getInstance());
        IAtomContainer tmpButeneContainer = tmpParser.parseSmiles("C=CCC");
        //two steps below needed for correct internal index handling
        MolecularArrays tmpMolecularArraysInstance = new MolecularArrays(tmpButeneContainer);
        IAtomContainer tmpCopyAC = tmpButeneContainer.getBuilder().newAtomContainer();
        for (IAtom tmpAtom: tmpButeneContainer.atoms()) {
            tmpCopyAC.addAtom(tmpASF.deepCopyAtom(tmpAtom));
        }
        for (IBond tmpBond: tmpButeneContainer.bonds()) {
            tmpCopyAC.addBond(deepCopyBond(tmpBond, tmpCopyAC));
        }
        //Comparison of original and copied AtomContainer
        String tmpButeneSMILES = ChemUtil.createUniqueSmiles(tmpButeneContainer, false);
        String tmpCopySMILES = ChemUtil.createUniqueSmiles(tmpCopyAC, false);
        Assertions.assertEquals(tmpButeneSMILES, tmpCopySMILES);
    }
    //</editor-fold>
    //
    //<editor-fold desc="Basic Fragmentation Tests">
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
        AlkylStructureFragmenter tmpASF = new AlkylStructureFragmenter();
        this.preprocessTestMolecule(tmpASF, tmpTestStructureAC,
                false, false, true);
        List<String> tmpFragmentsSMILESList = this.generateSMILESFromACList(tmpASF.fragmentMolecule(tmpTestStructureAC));
        List<String> tmpExpectedSMILESList = new ArrayList<>();
        tmpExpectedSMILESList.add("C");
        tmpExpectedSMILESList.add("C");
        tmpExpectedSMILESList.add("C");
        tmpExpectedSMILESList.add("C");
        tmpExpectedSMILESList.add("C");
        tmpExpectedSMILESList.add("CCC");
        tmpExpectedSMILESList.add("*C(*)(*)*");
        tmpExpectedSMILESList.add("*C(*)*");
        System.out.println("basicTest01: Expected: " + tmpExpectedSMILESList + "; Actual Fragments: "+ tmpFragmentsSMILESList);
        Assertions.assertTrue(this.compareListsIgnoringOrder(new ArrayList<>(tmpFragmentsSMILESList),
                new ArrayList<>(tmpExpectedSMILESList)));
        //alternate setting value
        tmpASF.setIsolateTertQuatCarbonsSetting(false);
        tmpFragmentsSMILESList.clear();
        tmpFragmentsSMILESList = this.generateSMILESFromACList(tmpASF.fragmentMolecule(tmpTestStructureAC));
        tmpExpectedSMILESList.clear();
        tmpExpectedSMILESList.add("CC(C)(C)C");
        tmpExpectedSMILESList.add("CC(C)C");
        tmpExpectedSMILESList.add("C");
        System.out.println("basicTest01: Expected: " + tmpExpectedSMILESList + "; Actual Fragments: "+ tmpFragmentsSMILESList);
        Assertions.assertTrue(this.compareListsIgnoringOrder(new ArrayList<>(tmpFragmentsSMILESList),
                new ArrayList<>(tmpExpectedSMILESList)));
    }
    /**
     * Method testing correct fragmentation with a basic example molecule.
     * This test focuses on extraction of isolated double and triple bonds,
     * tested with the corresponding butenes and butynes.
     * Additionally, the non-fragmentation of a cyclo-hexane with an internal "isolated" double bond is also tested.
     *
     * @throws InvalidSmilesException if SMILES is not correctly parsed of otherwise faulty
     * @throws CloneNotSupportedException if something goes wrong during cloning step in fragmentation
     */
    @Test
    public void basicTest02() throws InvalidSmilesException, CloneNotSupportedException {
        //test structure: CC=CC
        SmilesParser tmpParser = new SmilesParser(SilentChemObjectBuilder.getInstance());
        IAtomContainer tmpTestStructureAC = tmpParser.parseSmiles("CC=CC");
        AlkylStructureFragmenter tmpASF = new AlkylStructureFragmenter();
        this.preprocessTestMolecule(tmpASF, tmpTestStructureAC,
                false, false, true);
        List<String> tmpFragmentsSMILESList = this.generateSMILESFromACList(tmpASF.fragmentMolecule(tmpTestStructureAC));
        List<String> tmpExpectedSMILESList = new ArrayList<>();
        tmpExpectedSMILESList.add("C");
        tmpExpectedSMILESList.add("C");
        tmpExpectedSMILESList.add("C=C");
        System.out.println("basicTest02: Expected: " + tmpExpectedSMILESList + "; Actual Fragments: "+ tmpFragmentsSMILESList);
        Assertions.assertTrue(this.compareListsIgnoringOrder(new ArrayList<>(tmpFragmentsSMILESList),
                new ArrayList<>(tmpExpectedSMILESList)));
        //test structure: C=CCC
        tmpTestStructureAC = tmpParser.parseSmiles("C=CCC");
        Assertions.assertFalse(tmpASF.shouldBeFiltered(tmpTestStructureAC));
        Assertions.assertFalse(tmpASF.shouldBePreprocessed(tmpTestStructureAC));
        Assertions.assertTrue(tmpASF.canBeFragmented(tmpTestStructureAC));
        tmpFragmentsSMILESList = this.generateSMILESFromACList(tmpASF.fragmentMolecule(tmpTestStructureAC));
        tmpExpectedSMILESList.clear();
        tmpExpectedSMILESList.add("CC");
        tmpExpectedSMILESList.add("C=C");
        System.out.println("basicTest02: Expected: " + tmpExpectedSMILESList + "; Actual Fragments: "+ tmpFragmentsSMILESList);
        Assertions.assertTrue(this.compareListsIgnoringOrder(new ArrayList<>(tmpFragmentsSMILESList),
                new ArrayList<>(tmpExpectedSMILESList)));
        //test structure: CC#CC
        tmpTestStructureAC = tmpParser.parseSmiles("CC#CC");
        Assertions.assertFalse(tmpASF.shouldBeFiltered(tmpTestStructureAC));
        Assertions.assertFalse(tmpASF.shouldBePreprocessed(tmpTestStructureAC));
        Assertions.assertTrue(tmpASF.canBeFragmented(tmpTestStructureAC));
        tmpFragmentsSMILESList = this.generateSMILESFromACList(tmpASF.fragmentMolecule(tmpTestStructureAC));
        tmpExpectedSMILESList.clear();
        tmpExpectedSMILESList.add("C");
        tmpExpectedSMILESList.add("C");
        tmpExpectedSMILESList.add("C#C");
        System.out.println("basicTest02: Expected: " + tmpExpectedSMILESList + "; Actual Fragments: "+ tmpFragmentsSMILESList);
        Assertions.assertTrue(this.compareListsIgnoringOrder(new ArrayList<>(tmpFragmentsSMILESList),
                new ArrayList<>(tmpExpectedSMILESList)));
        //test structure: CC#CC
        tmpTestStructureAC = tmpParser.parseSmiles("C1=CCCCC1");
        Assertions.assertFalse(tmpASF.shouldBeFiltered(tmpTestStructureAC));
        Assertions.assertFalse(tmpASF.shouldBePreprocessed(tmpTestStructureAC));
        Assertions.assertTrue(tmpASF.canBeFragmented(tmpTestStructureAC));
        tmpFragmentsSMILESList = this.generateSMILESFromACList(tmpASF.fragmentMolecule(tmpTestStructureAC));
        tmpExpectedSMILESList.clear();
        tmpExpectedSMILESList.add("C1=CCCCC1");
        System.out.println("basicTest02: Expected: " + tmpExpectedSMILESList + "; Actual Fragments: "+ tmpFragmentsSMILESList);
        Assertions.assertTrue(this.compareListsIgnoringOrder(new ArrayList<>(tmpFragmentsSMILESList),
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
        AlkylStructureFragmenter tmpASF = new AlkylStructureFragmenter();
        this.preprocessTestMolecule(tmpASF, tmpTestStructureAC,
                false, false, true);
        List<String> tmpFragmentsSMILESList = this.generateSMILESFromACList(tmpASF.fragmentMolecule(tmpTestStructureAC));
        List<String> tmpExpectedSMILESList = new ArrayList<>();
        tmpExpectedSMILESList.add("C1CCCCC1");
        System.out.println("basicTest03: Expected: " + tmpExpectedSMILESList + "; Actual Fragments: "+ tmpFragmentsSMILESList);
        Assertions.assertTrue(this.compareListsIgnoringOrder(new ArrayList<>(tmpFragmentsSMILESList),
                new ArrayList<>(tmpExpectedSMILESList)));
        //test structure: c1ccccc1
        tmpTestStructureAC = tmpParser.parseSmiles("c1ccccc1");
        Assertions.assertFalse(tmpASF.shouldBeFiltered(tmpTestStructureAC));
        Assertions.assertFalse(tmpASF.shouldBePreprocessed(tmpTestStructureAC));
        Assertions.assertTrue(tmpASF.canBeFragmented(tmpTestStructureAC));
        List<IAtomContainer> tmpFragmentsList = tmpASF.fragmentMolecule(tmpTestStructureAC);
        tmpFragmentsSMILESList = this.generateSMILESFromACList(tmpFragmentsList);
        tmpExpectedSMILESList.clear();
        tmpExpectedSMILESList.add("c1ccccc1");
        System.out.println("basicTest03: Expected: " + tmpExpectedSMILESList + "; Actual Fragments: "+ tmpFragmentsSMILESList);
        Assertions.assertTrue(this.compareListsIgnoringOrder(new ArrayList<>(tmpFragmentsSMILESList),
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
        AlkylStructureFragmenter tmpASF = new AlkylStructureFragmenter();
        this.preprocessTestMolecule(tmpASF, tmpTestStructureAC,
                false, false, true);
        List<String> tmpFragmentsSMILESList = this.generateSMILESFromACList(tmpASF.fragmentMolecule(tmpTestStructureAC));
        List<String> tmpExpectedSMILESList = new ArrayList<>();
        tmpExpectedSMILESList.add("C1CC2CCCC2C1");
        System.out.println("basicTest04: Expected: " + tmpExpectedSMILESList + "; Actual Fragments: "+ tmpFragmentsSMILESList);
        Assertions.assertTrue(this.compareListsIgnoringOrder(new ArrayList<>(tmpFragmentsSMILESList),
                new ArrayList<>(tmpExpectedSMILESList)));
        //test structure: C=1C=CC(=CC1)C2=CC=CC=C2
        tmpTestStructureAC = tmpParser.parseSmiles("C=1C=CC(=CC1)C2=CC=CC=C2");
        Assertions.assertFalse(tmpASF.shouldBeFiltered(tmpTestStructureAC));
        Assertions.assertFalse(tmpASF.shouldBePreprocessed(tmpTestStructureAC));
        Assertions.assertTrue(tmpASF.canBeFragmented(tmpTestStructureAC));
        tmpFragmentsSMILESList = this.generateSMILESFromACList(tmpASF.fragmentMolecule(tmpTestStructureAC));
        tmpExpectedSMILESList.clear();
        tmpExpectedSMILESList.add("C=1C=CC(=CC1)C2=CC=CC=C2");
        System.out.println("basicTest04: Expected: " + tmpExpectedSMILESList + "; Actual Fragments: "+ tmpFragmentsSMILESList);
        Assertions.assertTrue(this.compareListsIgnoringOrder(new ArrayList<>(tmpFragmentsSMILESList),
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
        AlkylStructureFragmenter tmpASF = new AlkylStructureFragmenter();
        this.preprocessTestMolecule(tmpASF, tmpTestStructureAC,
                false, false, true);
        List<String> tmpFragmentsSMILESList = this.generateSMILESFromACList(tmpASF.fragmentMolecule(tmpTestStructureAC));
        List<String> tmpExpectedSMILESList = new ArrayList<>();
        tmpExpectedSMILESList.add("C1CCCCC1");
        tmpExpectedSMILESList.add("C1CCCCC1");
        System.out.println("basicTest05: Expected: " + tmpExpectedSMILESList + "; Actual Fragments: "+ tmpFragmentsSMILESList);
        Assertions.assertTrue(this.compareListsIgnoringOrder(new ArrayList<>(tmpFragmentsSMILESList),
                new ArrayList<>(tmpExpectedSMILESList)));
        //alternate setting value since two connected rings also have at least tertiary configuration at their joint atoms
        //though there should be no difference in resulting fragments
        tmpASF.setSeparateTertQuatCarbonFromRingSetting(true);
        tmpFragmentsSMILESList = this.generateSMILESFromACList(tmpASF.fragmentMolecule(tmpTestStructureAC));
        System.out.println("basicTest05: Expected: " + tmpExpectedSMILESList + "; Actual Fragments: "+ tmpFragmentsSMILESList);
        Assertions.assertTrue(this.compareListsIgnoringOrder(new ArrayList<>(tmpFragmentsSMILESList),
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
        AlkylStructureFragmenter tmpASF = new AlkylStructureFragmenter();
        this.preprocessTestMolecule(tmpASF, tmpTestStructureAC,
                false, false, true);
        List<String> tmpFragmentsSMILESList = this.generateSMILESFromACList(tmpASF.fragmentMolecule(tmpTestStructureAC));
        List<String> tmpExpectedSMILESList = new ArrayList<>();
        tmpExpectedSMILESList.add("C1CCC2(CC1)CCCCC2");
        System.out.println("basicTest06: Expected: " + tmpExpectedSMILESList + "; Actual Fragments: "+ tmpFragmentsSMILESList);
        Assertions.assertTrue(this.compareListsIgnoringOrder(new ArrayList<>(tmpFragmentsSMILESList),
                new ArrayList<>(tmpExpectedSMILESList)));
    }
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
        AlkylStructureFragmenter tmpASF = new AlkylStructureFragmenter();
        this.preprocessTestMolecule(tmpASF, tmpTestStructureAC,
                false, false, true);
        //for default settings:
        List<String> tmpFragmentsSMILESList = this.generateSMILESFromACList(tmpASF.fragmentMolecule(tmpTestStructureAC));
        List<String> tmpExpectedSMILESList = new ArrayList<>();
        tmpExpectedSMILESList.add("CC(C)(C)C1CCCCC1");
        System.out.println("basicTest07: Expected: " + tmpExpectedSMILESList + "; Actual Fragments: "+ tmpFragmentsSMILESList);
        Assertions.assertTrue(this.compareListsIgnoringOrder(new ArrayList<>(tmpFragmentsSMILESList),
                new ArrayList<>(tmpExpectedSMILESList)));
        //combinations of relevant settings
        //tmpASF.setIsolateTertQuatCarbonsSetting(true); -> default value
        tmpASF.setSeparateTertQuatCarbonFromRingSetting(true);
        tmpFragmentsSMILESList.clear();
        tmpFragmentsSMILESList = this.generateSMILESFromACList(tmpASF.fragmentMolecule(tmpTestStructureAC));
        tmpExpectedSMILESList.clear();
        tmpExpectedSMILESList.add("*C(*)(*)*");
        tmpExpectedSMILESList.add("C1CCCCC1");
        tmpExpectedSMILESList.add("C");
        tmpExpectedSMILESList.add("C");
        tmpExpectedSMILESList.add("C");
        System.out.println("basicTest07: Expected: " + tmpExpectedSMILESList + "; Actual Fragments: "+ tmpFragmentsSMILESList);
        Assertions.assertTrue(this.compareListsIgnoringOrder(new ArrayList<>(tmpFragmentsSMILESList),
                    new ArrayList<>(tmpExpectedSMILESList)));
        //
        tmpASF.setIsolateTertQuatCarbonsSetting(false);
        //tmpASF.setSeparateTertQuatCarbonFromRingSetting(true); -> still correct value from test above
        tmpFragmentsSMILESList.clear();
        tmpFragmentsSMILESList = this.generateSMILESFromACList(tmpASF.fragmentMolecule(tmpTestStructureAC));
        tmpExpectedSMILESList.clear();
        tmpExpectedSMILESList.add("*C(C)(C)C");
        tmpExpectedSMILESList.add("C1CCCCC1");
        System.out.println("basicTest07: Expected: " + tmpExpectedSMILESList + "; Actual Fragments: "+ tmpFragmentsSMILESList);
        Assertions.assertTrue(this.compareListsIgnoringOrder(new ArrayList<>(tmpFragmentsSMILESList),
                new ArrayList<>(tmpExpectedSMILESList)));
        //
        //tmpASF.setIsolateTertQuatCarbonsSetting(false); -> still correct value from test above
        tmpASF.setSeparateTertQuatCarbonFromRingSetting(false);
        tmpFragmentsSMILESList.clear();
        tmpFragmentsSMILESList = this.generateSMILESFromACList(tmpASF.fragmentMolecule(tmpTestStructureAC));
        tmpExpectedSMILESList.clear();
        tmpExpectedSMILESList.add("CC(C)(C)C1CCCCC1");
        System.out.println("basicTest07: Expected: " + tmpExpectedSMILESList + "; Actual Fragments: "+ tmpFragmentsSMILESList);
        Assertions.assertTrue(this.compareListsIgnoringOrder(new ArrayList<>(tmpFragmentsSMILESList),
                new ArrayList<>(tmpExpectedSMILESList)));
    }
    /**
     * Method to test correct extraction of allene structures.
     *
     * @throws InvalidSmilesException if SMILES cannot be parsed
     * @throws CloneNotSupportedException if cloning of the atomcontainer in fragmentation is not supported
     */
    @Test
    public void basicTest08() throws InvalidSmilesException, CloneNotSupportedException {
        SmilesParser tmpParser = new SmilesParser(SilentChemObjectBuilder.getInstance());
        IAtomContainer tmpTestStructureAC = tmpParser.parseSmiles("CC=C=CC");
        AlkylStructureFragmenter tmpASF = new AlkylStructureFragmenter();
        this.preprocessTestMolecule(tmpASF, tmpTestStructureAC,
                false, false, true);
        List<String> tmpFragmentsSMILESList = this.generateSMILESFromACList(tmpASF.fragmentMolecule(tmpTestStructureAC));
        List<String> tmpExpectedSMILESList = new ArrayList<>();
        tmpExpectedSMILESList.add("C(=C)=C");
        tmpExpectedSMILESList.add("C");
        tmpExpectedSMILESList.add("C");
        System.out.println("basicTest08: Expected: " + tmpExpectedSMILESList + "; Actual Fragments: "+ tmpFragmentsSMILESList);
        Assertions.assertTrue(this.compareListsIgnoringOrder(new ArrayList<>(tmpFragmentsSMILESList),
                new ArrayList<>(tmpExpectedSMILESList)));
    }
    /**
     * Method testing correct handling of conjugated aromatic rings, conjoined with a single bond,
     * making the whole molecule conjugated.
     * Tested with molecule Biphenyl.
     *
     * @throws InvalidSmilesException if SMILES can not be parsed
     * @throws CloneNotSupportedException if cloning of ASF is not supported
     */
    @Test
    public void basicTest09() throws InvalidSmilesException, CloneNotSupportedException {
        SmilesParser tmpParser = new SmilesParser(SilentChemObjectBuilder.getInstance());
        IAtomContainer tmpTestStructureAC = tmpParser.parseSmiles("c1ccccc1c2ccccc2");
        AlkylStructureFragmenter tmpASF = new AlkylStructureFragmenter();
        this.preprocessTestMolecule(tmpASF, tmpTestStructureAC,
                false, false, true);
        List<String> tmpFragmentsSMILESList = this.generateSMILESFromACList(tmpASF.fragmentMolecule(tmpTestStructureAC));
        List<String> tmpExpectedSMILESList = new ArrayList<>();
        tmpExpectedSMILESList.add("c1ccc(cc1)c2ccccc2");
        System.out.println("basicTest09: Expected: " + tmpExpectedSMILESList + "; Actual Fragments: "+ tmpFragmentsSMILESList);
        Assertions.assertTrue(this.compareListsIgnoringOrder(new ArrayList<>(tmpFragmentsSMILESList),
                new ArrayList<>(tmpExpectedSMILESList)));
    }
    /**
     * Method testing for correct behavior in dissection and separation of linear carbon chains of varying sizes (including 'no restrictions applied').
     */
    @Test
    public void defaultLinearChainDissectionTest() throws InvalidSmilesException, CloneNotSupportedException {
        SmilesParser tmpParser = new SmilesParser(SilentChemObjectBuilder.getInstance());
        IAtomContainer tmpCarbonChainAC = tmpParser.parseSmiles("CCCCCCCCCCCCCC");
        AlkylStructureFragmenter tmpASF = new AlkylStructureFragmenter();
        this.preprocessTestMolecule(tmpASF, tmpCarbonChainAC,
                false, false, true);
        List<String> tmpFragmentsSMILESList = this.generateSMILESFromACList(tmpASF.fragmentMolecule(tmpCarbonChainAC));
        List<String> tmpExpectedSMILESList = new ArrayList<>();
        tmpExpectedSMILESList.add("CC");
        tmpExpectedSMILESList.add("CCCCCC");
        tmpExpectedSMILESList.add("CCCCCC");
        System.out.println("defaultLinearChainDissectionTest: Expected: " + tmpExpectedSMILESList + "; Actual Fragments: "+ tmpFragmentsSMILESList);
        Assertions.assertTrue(this.compareListsIgnoringOrder(new ArrayList<>(tmpFragmentsSMILESList),
                new ArrayList<>(tmpExpectedSMILESList)));
        //alternate setting value -> maximum length of linear chains = 7
        tmpASF.setMaxChainLengthSetting(7);
        tmpFragmentsSMILESList.clear();
        tmpExpectedSMILESList.clear();
        tmpFragmentsSMILESList = this.generateSMILESFromACList(tmpASF.fragmentMolecule(tmpCarbonChainAC));
        tmpExpectedSMILESList.add("CCCCCCC");
        tmpExpectedSMILESList.add("CCCCCCC");
        System.out.println("defaultLinearChainDissectionTest: Expected: " + tmpExpectedSMILESList + "; Actual Fragments: "+ tmpFragmentsSMILESList);
        Assertions.assertTrue(this.compareListsIgnoringOrder(new ArrayList<>(tmpFragmentsSMILESList),
                new ArrayList<>(tmpExpectedSMILESList)));
        tmpASF.setFragmentSideChainsSetting(false);
        tmpFragmentsSMILESList.clear();
        tmpExpectedSMILESList.clear();
        tmpFragmentsSMILESList = this.generateSMILESFromACList(tmpASF.fragmentMolecule(tmpCarbonChainAC));
        tmpExpectedSMILESList.add("CCCCCCCCCCCCCC");
        System.out.println("defaultLinearChainDissectionTest: Expected: " + tmpExpectedSMILESList + "; Actual Fragments: "+ tmpFragmentsSMILESList);
        Assertions.assertTrue(this.compareListsIgnoringOrder(new ArrayList<>(tmpFragmentsSMILESList),
                new ArrayList<>(tmpExpectedSMILESList)));
    }
    //</editor-fold>
    //
    //<editor-fold desc="Specific Fragmentation Tests">
    //all molecules are tested with all relevant settings
    //example: only changing maximum size of linear chains in a linear C6 molecule
    /**
     * Method testing correct fragmentation with a specific example molecule.
     * This test focuses on extraction of ring systems, tested with a derivative of the molecule Hapalindole B after
     * partial fragmentation with the (in MORTAR included) ErtlFunctionalGroupsFinder.
     *
     * @throws InvalidSmilesException if SMILES is not correctly parsed of otherwise faulty
     * @throws CloneNotSupportedException if something goes wrong during cloning step in fragmentation
     */
    @Test
    public void specificTest01() throws InvalidSmilesException, CloneNotSupportedException {
        //test structure: CC1CCC2C(C1)C(=C)C3=C(C=CC=C3)C2(C)C (Hapalindole B Derivative)
        SmilesParser tmpParser = new SmilesParser(SilentChemObjectBuilder.getInstance());
        IAtomContainer tmpTestStructureAC = tmpParser.parseSmiles("CC1CCC2C(C1)C(=C)C3=C(C=CC=C3)C2(C)C");
        AlkylStructureFragmenter tmpASF = new AlkylStructureFragmenter();
        this.preprocessTestMolecule(tmpASF, tmpTestStructureAC,
                false, false, true);
        List<String> tmpFragmentsSMILESList = this.generateSMILESFromACList(tmpASF.fragmentMolecule(tmpTestStructureAC));
        List<String> tmpExpectedSMILESList = new ArrayList<>();
        tmpExpectedSMILESList.add("C");
        tmpExpectedSMILESList.add("C=C1C=2C=CC=CC2CC3CCCCC13");
        tmpExpectedSMILESList.add("C");
        tmpExpectedSMILESList.add("C");
        System.out.println("specificTest01: Expected: " + tmpExpectedSMILESList + "; Actual Fragments: "+ tmpFragmentsSMILESList);
        Assertions.assertTrue(this.compareListsIgnoringOrder(new ArrayList<>(tmpFragmentsSMILESList),
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
        AlkylStructureFragmenter tmpASF = new AlkylStructureFragmenter();
        this.preprocessTestMolecule(tmpASF, tmpTestStructureAC,
                false, false, true);
        List<String> tmpFragmentsSMILESList = this.generateSMILESFromACList(tmpASF.fragmentMolecule(tmpTestStructureAC));
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
        System.out.println("specificTest02: Expected: " + tmpExpectedSMILESList + "; Actual Fragments: "+ tmpFragmentsSMILESList);
        Assertions.assertTrue(this.compareListsIgnoringOrder(new ArrayList<>(tmpFragmentsSMILESList),
                new ArrayList<>(tmpExpectedSMILESList)));
        //alternate setting values
        tmpASF.setIsolateTertQuatCarbonsSetting(false);
        tmpFragmentsSMILESList = this.generateSMILESFromACList(tmpASF.fragmentMolecule(tmpTestStructureAC));
        tmpExpectedSMILESList.clear();
        tmpExpectedSMILESList.add("CC");
        tmpExpectedSMILESList.add("CC");
        tmpExpectedSMILESList.add("CC(C)(C)CC(C)(C)C");
        System.out.println("specificTest02: Expected: " + tmpExpectedSMILESList + "; Actual Fragments: "+ tmpFragmentsSMILESList);
        Assertions.assertTrue(this.compareListsIgnoringOrder(new ArrayList<>(tmpFragmentsSMILESList),
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
        AlkylStructureFragmenter tmpASF = new AlkylStructureFragmenter();
        this.preprocessTestMolecule(tmpASF, tmpTestStructureAC,
                false, false, true);
        List<String> tmpFragmentsSMILESList = this.generateSMILESFromACList(tmpASF.fragmentMolecule(tmpTestStructureAC));
        List<String> tmpExpectedSMILESList = new ArrayList<>();
        tmpExpectedSMILESList.add("C=CC=C");
        tmpExpectedSMILESList.add("C");
        tmpExpectedSMILESList.add("C");
        tmpExpectedSMILESList.add("C");
        tmpExpectedSMILESList.add("C");
        tmpExpectedSMILESList.add("C1CCCC1");
        System.out.println("specificTest03: Expected: " + tmpExpectedSMILESList + "; Actual Fragments: "+ tmpFragmentsSMILESList);
        Assertions.assertTrue(this.compareListsIgnoringOrder(new ArrayList<>(tmpFragmentsSMILESList),
                new ArrayList<>(tmpExpectedSMILESList)));
    }
    /**
     * Tests molecule "CC(C)(C)CC1CCC(=C)C2CC21" which showed difficulties in fragmentation in past versions.
     *
     * @throws InvalidSmilesException if SMILES cannot be parsed
     * @throws CloneNotSupportedException if cloning of the original molecule is not supported
     */
    @Test
    public void specificTest04() throws InvalidSmilesException, CloneNotSupportedException {
        //test structure: CC(C)(C)CC1CCC(=C)C2CC21
        SmilesParser tmpParser = new SmilesParser(SilentChemObjectBuilder.getInstance());
        IAtomContainer tmpTestStructureAC = tmpParser.parseSmiles("CC(C)(C)CC1CCC(=C)C2CC21");
        AlkylStructureFragmenter tmpASF = new AlkylStructureFragmenter();
        this.preprocessTestMolecule(tmpASF, tmpTestStructureAC,
                false, false, true);
        List<String> tmpFragmentsSMILESList = this.generateSMILESFromACList(tmpASF.fragmentMolecule(tmpTestStructureAC));
        List<String> tmpExpectedSMILESList = new ArrayList<>();
        tmpExpectedSMILESList.add("*C(*)(*)*");
        tmpExpectedSMILESList.add("C");
        tmpExpectedSMILESList.add("C");
        tmpExpectedSMILESList.add("C");
        tmpExpectedSMILESList.add("C");
        tmpExpectedSMILESList.add("C=C1CCCC2CC12");
        System.out.println("specificTest04: Expected: " + tmpExpectedSMILESList + "; Actual Fragments: "+ tmpFragmentsSMILESList);
        Assertions.assertTrue(this.compareListsIgnoringOrder(new ArrayList<>(tmpFragmentsSMILESList),
                new ArrayList<>(tmpExpectedSMILESList)));
        tmpASF.setIsolateTertQuatCarbonsSetting(false);
        tmpFragmentsSMILESList = this.generateSMILESFromACList(tmpASF.fragmentMolecule(tmpTestStructureAC));
        tmpExpectedSMILESList.clear();
        tmpExpectedSMILESList.add("CC(C)(C)C");
        tmpExpectedSMILESList.add("C=C1CCCC2CC12");
        System.out.println("specificTest04: Expected: " + tmpExpectedSMILESList + "; Actual Fragments: "+ tmpFragmentsSMILESList);
        Assertions.assertTrue(this.compareListsIgnoringOrder(new ArrayList<>(tmpFragmentsSMILESList),
                new ArrayList<>(tmpExpectedSMILESList)));
    }
    /**
     * Method to test a default alkyl structure fragmentation on a concept molecule covering a broad range of resulting fragments.
     *
     * @throws Exception if fragmentation does not result in expected fragments
     */
    @Test
    public void defaultFragmentationTest() throws Exception {
        SmilesParser tmpParser = new SmilesParser(SilentChemObjectBuilder.getInstance());
        //test structure: CC(CC1C2CC2C(=C)CC1c1ccc(cc1C\C=C/c1ccccc1)C(C)C)(CCCCCCCCC)CC#CC
        IAtomContainer tmpTestStructureAC = tmpParser.parseSmiles("CC(CC1C2CC2C(=C)CC1c1ccc(cc1C\\C=C/c1ccccc1)C(C)C)(CCCCCCCCC)CC#CC");
        AlkylStructureFragmenter tmpASF = new AlkylStructureFragmenter();
        this.preprocessTestMolecule(tmpASF, tmpTestStructureAC,
                false, false, true);
        List<String> tmpFragmentsSMILESList = this.generateSMILESFromACList(tmpASF.fragmentMolecule(tmpTestStructureAC));
        List<String> tmpExpectedSMILESList = new ArrayList<>(12);
        tmpExpectedSMILESList.add("c1ccc(cc1)C(C)C"); //ring structure with connected tertiary carbon system
        tmpExpectedSMILESList.add("C=Cc1ccccc1"); //benzene with connected double bond forming conjugated pi bond system
        tmpExpectedSMILESList.add("C"); //methane between the substructures above
        tmpExpectedSMILESList.add("C=C1CCCC2CC12"); //ring system with connected non-cyclic double bond
        tmpExpectedSMILESList.add("C"); //methane connecting ring system above and quaternary carbon below
        tmpExpectedSMILESList.add("*C(*)(*)*"); //quaternary system in pseudoatom representation
        tmpExpectedSMILESList.add("C"); //methane residual of quaternary system above
        tmpExpectedSMILESList.add("CCCCCC"); //hexane residual of fragmented nonane chain
        tmpExpectedSMILESList.add("CCC"); //propane residual of fragmented nonane chain
        tmpExpectedSMILESList.add("C"); //methane connecting triple bond and quaternary system
        tmpExpectedSMILESList.add("C#C"); //triple bond
        tmpExpectedSMILESList.add("C"); //methane residual of triple bond chain
        System.out.println("defaultFragmentationTest: Expected: " + tmpExpectedSMILESList + "; Actual Fragments: "+ tmpFragmentsSMILESList);
        Assertions.assertTrue(this.compareListsIgnoringOrder(new ArrayList<>(tmpFragmentsSMILESList),
                new ArrayList<>(tmpExpectedSMILESList)));
        //alternate settings
        tmpASF.setFragmentSideChainsSetting(false);
        tmpFragmentsSMILESList = this.generateSMILESFromACList(tmpASF.fragmentMolecule(tmpTestStructureAC));
        tmpExpectedSMILESList.clear();
        tmpExpectedSMILESList.add("c1ccc(cc1)C(C)C"); //ring structure with connected tertiary carbon system
        tmpExpectedSMILESList.add("C=Cc1ccccc1"); //benzene with connected double bond forming conjugated pi bond system
        tmpExpectedSMILESList.add("C"); //methane between the substructures above
        tmpExpectedSMILESList.add("C=C1CCCC2CC12"); //ring system with connected non-cyclic double bond
        tmpExpectedSMILESList.add("C"); //methane connecting ring system above and quaternary carbon below
        tmpExpectedSMILESList.add("*C(*)(*)*"); //quaternary system in pseudoatom representation
        tmpExpectedSMILESList.add("C"); //methane residual of quaternary system above
        tmpExpectedSMILESList.add("CCCCCCCCC"); //nonane chain
        tmpExpectedSMILESList.add("C"); //methane connecting triple bond and quaternary system
        tmpExpectedSMILESList.add("C#C"); //triple bond
        tmpExpectedSMILESList.add("C"); //methane residual of triple bond chain
        System.out.println("defaultFragmentationTest: Expected: " + tmpExpectedSMILESList + "; Actual Fragments: "+ tmpFragmentsSMILESList);
        Assertions.assertTrue(this.compareListsIgnoringOrder(new ArrayList<>(tmpFragmentsSMILESList),
                new ArrayList<>(tmpExpectedSMILESList)));
        //
        tmpASF.setFragmentSideChainsSetting(true);
        tmpASF.setMaxChainLengthSetting(3);
        tmpFragmentsSMILESList = this.generateSMILESFromACList(tmpASF.fragmentMolecule(tmpTestStructureAC));
        tmpExpectedSMILESList.clear();
        tmpExpectedSMILESList.add("c1ccc(cc1)C(C)C"); //ring structure with connected tertiary carbon system
        tmpExpectedSMILESList.add("C=Cc1ccccc1"); //benzene with connected double bond forming conjugated pi bond system
        tmpExpectedSMILESList.add("C"); //methane between the substructures above
        tmpExpectedSMILESList.add("C=C1CCCC2CC12"); //ring system with connected non-cyclic double bond
        tmpExpectedSMILESList.add("C"); //methane connecting ring system above and quaternary carbon below
        tmpExpectedSMILESList.add("*C(*)(*)*"); //quaternary system in pseudoatom representation
        tmpExpectedSMILESList.add("C"); //methane residual of quaternary system above
        tmpExpectedSMILESList.add("CCC"); //propane residual of fragmented nonane chain
        tmpExpectedSMILESList.add("CCC"); //propane residual of fragmented nonane chain
        tmpExpectedSMILESList.add("CCC"); //propane residual of fragmented nonane chain
        tmpExpectedSMILESList.add("C"); //methane connecting triple bond and quaternary system
        tmpExpectedSMILESList.add("C#C"); //triple bond
        tmpExpectedSMILESList.add("C"); //methane residual of triple bond chain
        System.out.println("defaultFragmentationTest: Expected: " + tmpExpectedSMILESList + "; Actual Fragments: "+ tmpFragmentsSMILESList);
        Assertions.assertTrue(this.compareListsIgnoringOrder(new ArrayList<>(tmpFragmentsSMILESList),
                new ArrayList<>(tmpExpectedSMILESList)));
        //
        tmpASF.setMaxChainLengthSetting(6); //max chain length default value
        tmpASF.setSeparateTertQuatCarbonFromRingSetting(true);
        tmpFragmentsSMILESList = this.generateSMILESFromACList(tmpASF.fragmentMolecule(tmpTestStructureAC));
        tmpExpectedSMILESList.clear();
        tmpExpectedSMILESList.add("*C(*)(*)*");
        tmpExpectedSMILESList.add("*C(*)*");
        tmpExpectedSMILESList.add("C");
        tmpExpectedSMILESList.add("C=C1CCCC2CC12");
        tmpExpectedSMILESList.add("c1ccccc1");
        tmpExpectedSMILESList.add("C=Cc1ccccc1");
        tmpExpectedSMILESList.add("C");
        tmpExpectedSMILESList.add("C");
        tmpExpectedSMILESList.add("C");
        tmpExpectedSMILESList.add("C");
        tmpExpectedSMILESList.add("C");
        tmpExpectedSMILESList.add("C");
        tmpExpectedSMILESList.add("C#C");
        tmpExpectedSMILESList.add("CCC");
        tmpExpectedSMILESList.add("CCCCCC");
        System.out.println("defaultFragmentationTest: Expected: " + tmpExpectedSMILESList + "; Actual Fragments: "+ tmpFragmentsSMILESList);
        Assertions.assertTrue(this.compareListsIgnoringOrder(new ArrayList<>(tmpFragmentsSMILESList),
                new ArrayList<>(tmpExpectedSMILESList)));
        //
        tmpASF.setSeparateTertQuatCarbonFromRingSetting(false);
        tmpASF.setIsolateTertQuatCarbonsSetting(false);
        tmpFragmentsSMILESList = this.generateSMILESFromACList(tmpASF.fragmentMolecule(tmpTestStructureAC));
        tmpExpectedSMILESList.clear();
        tmpExpectedSMILESList.add("CC(C)(C)C");
        tmpExpectedSMILESList.add("C=C1CCCC2CC12");
        tmpExpectedSMILESList.add("c1ccc(cc1)C(C)C");
        tmpExpectedSMILESList.add("C=Cc1ccccc1");
        tmpExpectedSMILESList.add("C#C");
        tmpExpectedSMILESList.add("C");
        tmpExpectedSMILESList.add("C");
        tmpExpectedSMILESList.add("CCCCCC");
        tmpExpectedSMILESList.add("CC");
        System.out.println("defaultFragmentationTest: Expected: " + tmpExpectedSMILESList + "; Actual Fragments: "+ tmpFragmentsSMILESList);
        Assertions.assertTrue(this.compareListsIgnoringOrder(new ArrayList<>(tmpFragmentsSMILESList),
                new ArrayList<>(tmpExpectedSMILESList)));
        //
        tmpASF.setSeparateTertQuatCarbonFromRingSetting(true);
        tmpFragmentsSMILESList = this.generateSMILESFromACList(tmpASF.fragmentMolecule(tmpTestStructureAC));
        tmpExpectedSMILESList.clear();
        tmpExpectedSMILESList.add("CC(C)(C)C");
        tmpExpectedSMILESList.add("C=C1CCCC2CC12");
        tmpExpectedSMILESList.add("c1ccccc1");
        tmpExpectedSMILESList.add("C=Cc1ccccc1");
        tmpExpectedSMILESList.add("*C(C)C");
        tmpExpectedSMILESList.add("C#C");
        tmpExpectedSMILESList.add("C");
        tmpExpectedSMILESList.add("CCCCCC");
        tmpExpectedSMILESList.add("CC");
        tmpExpectedSMILESList.add("C");
        System.out.println("defaultFragmentationTest: Expected: " + tmpExpectedSMILESList + "; Actual Fragments: "+ tmpFragmentsSMILESList);
        Assertions.assertTrue(this.compareListsIgnoringOrder(new ArrayList<>(tmpFragmentsSMILESList),
                new ArrayList<>(tmpExpectedSMILESList)));
    }
    /**
     * Example fragmentation using natural compounds CNP0321029.1; CNP0509407.0 and CNP0242949.0. The natural compounds
     * used in this test can be found in the natural product database COCONUT <a href="https://coconut.naturalproducts.net/"<a/>.
     *
     * @throws Exception if molecule is not correctly fragmented
     */
    @Test
    public void naturalCompoundFragmentationTest() throws Exception {
        SmilesParser tmpParser = new SmilesParser(SilentChemObjectBuilder.getInstance());
        //test structure: CNP0321029.1
        IAtomContainer tmpTestStructureAC = tmpParser.parseSmiles("C[C@@H]1C[C@@H](C)[C@@H](C)C1");
        AlkylStructureFragmenter tmpASF = new AlkylStructureFragmenter();
        this.preprocessTestMolecule(tmpASF, tmpTestStructureAC,
                false, false, true);
        List<String> tmpFragmentsSMILESList = this.generateSMILESFromACList(tmpASF.fragmentMolecule(tmpTestStructureAC));
        List<String> tmpExpectedSMILESList = new ArrayList<>(5);
        tmpExpectedSMILESList.add("C");
        tmpExpectedSMILESList.add("C");
        tmpExpectedSMILESList.add("C");
        tmpExpectedSMILESList.add("C1CCCC1");
        System.out.println("naturalCompoundFragmentationTest: Expected: " + tmpExpectedSMILESList + "; Actual Fragments: "+ tmpFragmentsSMILESList);
        Assertions.assertTrue(this.compareListsIgnoringOrder(new ArrayList<>(tmpFragmentsSMILESList),
                new ArrayList<>(tmpExpectedSMILESList)));
        //
        //test structure: CNP0509407.0
        tmpTestStructureAC = tmpParser.parseSmiles("CCCCCCCCCC(CC)(CCCCCCC)CCCCCCCC");
        this.preprocessTestMolecule(tmpASF, tmpTestStructureAC,
                false, false, true);
        tmpFragmentsSMILESList.clear();
        tmpExpectedSMILESList.clear();
        tmpFragmentsSMILESList = this.generateSMILESFromACList(tmpASF.fragmentMolecule(tmpTestStructureAC));
        tmpExpectedSMILESList.add("CCCCCC");
        tmpExpectedSMILESList.add("CCCCCC");
        tmpExpectedSMILESList.add("CCCCCC");
        tmpExpectedSMILESList.add("CCC");
        tmpExpectedSMILESList.add("CC");
        tmpExpectedSMILESList.add("CC");
        tmpExpectedSMILESList.add("C");
        tmpExpectedSMILESList.add("*C(*)(*)*");
        System.out.println("naturalCompoundFragmentationTest: Expected: " + tmpExpectedSMILESList + "; Actual Fragments: "+ tmpFragmentsSMILESList);
        Assertions.assertTrue(this.compareListsIgnoringOrder(new ArrayList<>(tmpFragmentsSMILESList),
                new ArrayList<>(tmpExpectedSMILESList)));
        //alternate settings
        tmpASF.setIsolateTertQuatCarbonsSetting(false);
        tmpFragmentsSMILESList.clear();
        tmpExpectedSMILESList.clear();
        tmpFragmentsSMILESList = this.generateSMILESFromACList(tmpASF.fragmentMolecule(tmpTestStructureAC));
        tmpExpectedSMILESList.add("CCCCCC");
        tmpExpectedSMILESList.add("CCCCCC");
        tmpExpectedSMILESList.add("CCCCCC");
        tmpExpectedSMILESList.add("CC");
        tmpExpectedSMILESList.add("C");
        tmpExpectedSMILESList.add("C");
        tmpExpectedSMILESList.add("CC(C)(C)C");
        System.out.println("naturalCompoundFragmentationTest: Expected: " + tmpExpectedSMILESList + "; Actual Fragments: "+ tmpFragmentsSMILESList);
        Assertions.assertTrue(this.compareListsIgnoringOrder(new ArrayList<>(tmpFragmentsSMILESList),
                new ArrayList<>(tmpExpectedSMILESList)));
        //
        tmpASF.setIsolateTertQuatCarbonsSetting(false);
        tmpASF.setFragmentSideChainsSetting(false);
        tmpFragmentsSMILESList.clear();
        tmpExpectedSMILESList.clear();
        tmpFragmentsSMILESList = this.generateSMILESFromACList(tmpASF.fragmentMolecule(tmpTestStructureAC));
        tmpExpectedSMILESList.add("CCCCCCCC");
        tmpExpectedSMILESList.add("CCCCCCC");
        tmpExpectedSMILESList.add("CCCCCC");
        tmpExpectedSMILESList.add("C");
        tmpExpectedSMILESList.add("CC(C)(C)C");
        System.out.println("naturalCompoundFragmentationTest: Expected: " + tmpExpectedSMILESList + "; Actual Fragments: "+ tmpFragmentsSMILESList);
        Assertions.assertTrue(this.compareListsIgnoringOrder(new ArrayList<>(tmpFragmentsSMILESList),
                new ArrayList<>(tmpExpectedSMILESList)));
        //
        //test structure: CNP0242949.0
        tmpTestStructureAC = tmpParser.parseSmiles("CC1CCCCCCCCC1");
        this.preprocessTestMolecule(tmpASF, tmpTestStructureAC,
                false, false, true);
        tmpFragmentsSMILESList.clear();
        tmpExpectedSMILESList.clear();
        tmpFragmentsSMILESList = this.generateSMILESFromACList(tmpASF.fragmentMolecule(tmpTestStructureAC));
        tmpExpectedSMILESList.add("C");
        tmpExpectedSMILESList.add("C1CCCCCCCCC1");
        System.out.println("naturalCompoundFragmentationTest: Expected: " + tmpExpectedSMILESList + "; Actual Fragments: "+ tmpFragmentsSMILESList);
        Assertions.assertTrue(this.compareListsIgnoringOrder(new ArrayList<>(tmpFragmentsSMILESList),
                new ArrayList<>(tmpExpectedSMILESList)));
    }
    //</editor-fold>
    //
    //<editor-fold desc="Inner Class 'Molecular Arrays'">
    /**
     * Test method for correct functionality of AlkylStructureFragmenter inner class MolecularArrays.
     *
     * Since the CDK does not allow the addition of null atoms to existing atom container instances, reflection is used
     * in this test to access the inner fields of atoms and bonds to manipulate their values.
     *
     * @throws InvalidSmilesException if parser cannot parse the given SMILES
     * @throws NoSuchFieldException if either the atoms or bonds field can not be found
     * @throws IllegalAccessException if reflection can not be done
     */
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
        //"getter" for BaseAtomRef and BondRef
        Object atoms = atomsField.get(tmpMolecule);
        Object[] newAtoms = (Object[]) Array.newInstance(atoms.getClass().getComponentType(), Array.getLength(atoms));
        Object bonds = bondsField.get(tmpMolecule);
        Object[] newBonds = (Object[]) Array.newInstance(bonds.getClass().getComponentType(), Array.getLength(bonds));
        //fill newAtoms with actual atoms
        for (int i = 0; i < 12; i++) {
               newAtoms[i] = tmpMolecule.getAtom(i);
               if (i < 11) {
                   newBonds[i] = tmpMolecule.getBond(i);
               }
        }
        //set position 5 null
        newAtoms[5] = null;
        newBonds[5] = null;
        //insert the fields back into tmpMolecule
        atomsField.set(tmpMolecule, newAtoms);
        bondsField.set(tmpMolecule, newBonds);
        //test for expected discrepancy
        MolecularArrays testMolecularArrays = new MolecularArrays(tmpMolecule);
        Assertions.assertNotEquals(testMolecularArrays.getAtomArray().length, refMolecularArrays.getAtomArray().length);
        Assertions.assertNotEquals(testMolecularArrays.getBondArray().length, refMolecularArrays.getBondArray().length);
        if (testMolecularArrays.getAtomArray().length != 11 && testMolecularArrays.getBondArray().length != 10) {
            Assertions.fail("Arrays length was not in expected range.");
        }
    }

    /**
     * Test to ensure correct detection and handling of allowed/not-allowed molecules.
     *
     * @throws InvalidSmilesException if SMILES cannot be parsed by SmilesParser
     */
    @Test
    public void filterTest() throws InvalidSmilesException{
        SmilesParser tmpParser = new SmilesParser(SilentChemObjectBuilder.getInstance());
        List<IAtomContainer> tmpTestStructuresACList = new ArrayList<>(4);
        tmpTestStructuresACList.add(tmpParser.parseSmiles("CCC"));
        tmpTestStructuresACList.add(tmpParser.parseSmiles("CN"));
        tmpTestStructuresACList.add(tmpParser.parseSmiles("C*"));
        tmpTestStructuresACList.add(tmpParser.parseSmiles("[H]C([H])([H])[H]"));
        boolean[] tmpFilterArray = new boolean[tmpTestStructuresACList.size()];
        setKeepNonFragmentableMoleculesSetting(true);
        for (int i = 0; i < tmpFilterArray.length; i++) {
            IAtomContainer tmpAC = tmpTestStructuresACList.get(i);
            tmpFilterArray[i] = shouldBeFiltered(tmpAC);
        }
        boolean[] tmpExpectedArray = new boolean[tmpTestStructuresACList.size()];
        //add filter cases with expected value
        tmpExpectedArray[0] = false;
        //since keepNon-FragmentableInPipeline == true, values for "CN" and "C*" are false to allow passing through filtering
        // to allow them to be kept in pipeline
        tmpExpectedArray[1] = false; //"CN"
        tmpExpectedArray[2] = false; //"C*"
        tmpExpectedArray[3] = false; //explicit hydrogen
        for (int i = 0; i < tmpFilterArray.length; i++) {
            if (tmpFilterArray[i] != tmpExpectedArray[i]) {
                Assertions.fail();
            }
        }
        //test for excluding non-fragmentables from pipeline
        setKeepNonFragmentableMoleculesSetting(false);
        for (int i = 0; i < tmpFilterArray.length; i++) {
            IAtomContainer tmpAC = tmpTestStructuresACList.get(i);
            tmpFilterArray[i] = shouldBeFiltered(tmpAC);
        }
        tmpExpectedArray[0] = false;
        tmpExpectedArray[1] = true;
        tmpExpectedArray[2] = true;
        tmpExpectedArray[3] = false;
        for (int i = 0; i < tmpFilterArray.length; i++) {
            if (tmpFilterArray[i] != tmpExpectedArray[i]) {
                Assertions.fail();
            }
        }

    }
    //</editor-fold
    //
    //<editor-fold desc="Private Utility Methods">
    /**
     * Compares two provided lists for equality while ignoring the lists' orders.
     * The second given list will be empty after the comparison if both lists contain equal objects.
     *
     * @param aList1 First given list to compare
     * @param aList2 Second given list to compare
     * @return boolean whether given lists are equal
     */
    private boolean compareListsIgnoringOrder(List<?> aList1, List<?> aList2) {
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
     * Utility method to generate SMILES notation strings for a given List containing IAtomContainer instances.
     *
     * @param anACList given List of IAtomContainers
     * @return List with SMILES Strings of originally given List of IAtomContainers
     */
    private List<String> generateSMILESFromACList(List<IAtomContainer> anACList) {
        List<String> tmpReturnSmilesList = new ArrayList<>(anACList.size());
        for (IAtomContainer tmpAC: anACList) {
            tmpReturnSmilesList.add(ChemUtil.createUniqueSmiles(tmpAC, false, true));
        }
        return tmpReturnSmilesList;
    }
    /**
     * Private utility method responsible for calling preprocessing methods of a given AlkylStructureFragmenter instance.
     *
     * @param anAlkylStructureFragmenterInstance the given ASF instance of which to call preprocessing
     * @param aMolecule to preprocess
     * @param aShouldBeFilteredStatement for shouldBeFiltered()
     * @param aShouldBePreprocessedStatement for shouldBePreprocessed()
     * @param aCanBeFragmentedStatement for canBeFragmented()
     */
    private void preprocessTestMolecule(AlkylStructureFragmenter anAlkylStructureFragmenterInstance,
                                        IAtomContainer aMolecule,
                                        boolean aShouldBeFilteredStatement,
                                        boolean aShouldBePreprocessedStatement,
                                        boolean aCanBeFragmentedStatement) {
        Assertions.assertEquals(aShouldBeFilteredStatement,
                anAlkylStructureFragmenterInstance.shouldBeFiltered(aMolecule));
        Assertions.assertEquals(aShouldBePreprocessedStatement,
                anAlkylStructureFragmenterInstance.shouldBePreprocessed(aMolecule));
        Assertions.assertEquals(aCanBeFragmentedStatement,
                anAlkylStructureFragmenterInstance.canBeFragmented(aMolecule));
    }
    /**
     * Private utility method used for asserting two MolecularArrays instances contain arrays with equal property values.
     *
     * @param anExpectedMolecularArrays with expected property values
     * @param anActualMolecularArrays with actual property values
     * @return boolean whether MolArrays instances are equal
     */
    private boolean assertMolecularArraysEquals(MolecularArrays anExpectedMolecularArrays, MolecularArrays anActualMolecularArrays) {
        Objects.requireNonNull(anExpectedMolecularArrays);
        Objects.requireNonNull(anActualMolecularArrays);
        IAtom[] tmpExpectedAtomArray = anExpectedMolecularArrays.getAtomArray();
        IAtom[] tmpActualAtomArray = anActualMolecularArrays.getAtomArray();
        IBond[] tmpExpectedBondArray = anExpectedMolecularArrays.getBondArray();
        IBond[] tmpActualBondArray = anActualMolecularArrays.getBondArray();
        //atom array match
        boolean tmpIsAtomArrayEqual = false;
        for (int i = 0; i < tmpActualAtomArray.length; i++) {
            IAtom tmpActualAtom = tmpActualAtomArray[i];
            IAtom tmpExpectedAtom = tmpExpectedAtomArray[i];
            tmpIsAtomArrayEqual = tmpActualAtom.getProperties().equals(tmpExpectedAtom.getProperties()) && tmpActualAtom.equals(tmpExpectedAtom);
        }
        //bond array match
        boolean tmpIsBondArrayEqual = false;
        for (int i = 0; i < tmpActualBondArray.length; i++) {
            IBond tmpActualBond = tmpActualBondArray[i];
            IBond tmpExpectedBond = tmpExpectedBondArray[i];
            tmpIsBondArrayEqual = tmpActualBond.getProperties().equals(tmpExpectedBond.getProperties()) && tmpActualBond.equals(tmpExpectedBond);
        }
        return (tmpIsAtomArrayEqual && tmpIsBondArrayEqual);
    }
    //</editor-fold>
}

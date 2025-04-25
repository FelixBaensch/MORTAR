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
     * Private AtomContainerSet containing the test structures.
     */
    private IAtomContainerSet testStructuresACSet;
    /**
     * Private AlkylStructureFragmenter used in this test, currently without special parameters.
     */
    private final AlkylStructureFragmenter basicAlkylStructureFragmenter;
    /**
     * Constructor of AlkylStructureFragmenter test class, setting all necessary settings in order of correct functionality
     * during testing.
     */
    public AlkylStructureFragmenterTest() throws FileNotFoundException, URISyntaxException {
        //TODO: rework! proper Test instantiation
        //if Windows Locale is not set to "en" and "GB", a MissingResource Exception is thrown (Localisation for "de" and "DE" not found)
        Locale.setDefault(Locale.of("en", "GB"));
        this.basicAlkylStructureFragmenter = new AlkylStructureFragmenter();
        this.testStructuresACSet = new AtomContainerSet();
        this.testStructuresACSet = this.readStructuresToACSet("de.unijena.cheminf.mortar.model.fragmentation.algorithm.ASF/ASF_Test_Structures.sdf");
     }

    //<editor-fold desc="@Test Public Methods">
    //<editor-fold desc="Unit Tests">
    /**
     * Tests correct instantiation and basic settings retrieval.
     */
    @Test
    public void defaultSettingsTest() {
        List<String> tmpCheckList = new ArrayList<>(8);
        List<String> tmpExpectList = new ArrayList<>(8);
        tmpExpectList.add("Fragment saturation setting");
        tmpExpectList.add("Retention setting for non-fragmentable molecules");
        tmpExpectList.add("Fragmentation of hydrocarbon side chains setting");
        tmpExpectList.add("Carbon side chains maximum length setting");
        tmpExpectList.add("Single carbon handling setting");
        tmpExpectList.add("Single ring detection setting");
        tmpExpectList.add("Keep rings setting");
        tmpExpectList.add("Separate tertiary and quaternary carbon atoms from ring structures setting");
        for (Property tmpSetting: this.basicAlkylStructureFragmenter.settingsProperties()) {
            tmpCheckList.add(tmpSetting.getName());
        }
        Assertions.assertLinesMatch(tmpExpectList, tmpCheckList);
    }
    /**
     * Test method for AlkylStructureFragmenter.extractFragments().
     */
    @Test
    public void extractFragmentsTest() {
        ArrayList<String> tmpExpectedFragmentsList = new ArrayList<>(5);
        tmpExpectedFragmentsList.add("C=1C=CC2=CC(=CC=C2C1)C=CC(C)C");
        tmpExpectedFragmentsList.add("CC(C)(C)C");
        tmpExpectedFragmentsList.add("C=C");
        tmpExpectedFragmentsList.add("CCC");
        tmpExpectedFragmentsList.add("C1CCCCC1");
        IAtomContainer tmpTestAC = this.testStructuresACSet.getAtomContainer(1);
        MolecularArrays tmpTestMolecularArrays = new MolecularArrays(tmpTestAC);
        tmpTestMolecularArrays.setAtomArray(this.basicAlkylStructureFragmenter.fillAtomArray(tmpTestAC));
        tmpTestMolecularArrays.setBondArray(this.basicAlkylStructureFragmenter.fillBondArray(tmpTestAC));
        this.basicAlkylStructureFragmenter.markNeighborAtomsAndBonds(tmpTestMolecularArrays,
                tmpTestMolecularArrays.getAtomArray(), tmpTestMolecularArrays.getBondArray());
        this.basicAlkylStructureFragmenter.markRings(tmpTestMolecularArrays, tmpTestAC,
                tmpTestMolecularArrays.getAtomArray(), tmpTestMolecularArrays.getBondArray());
        this.basicAlkylStructureFragmenter.markConjugatedPiSystems(tmpTestMolecularArrays, tmpTestAC,
                tmpTestMolecularArrays.getAtomArray(), tmpTestMolecularArrays.getBondArray());
        this.basicAlkylStructureFragmenter.markMultiBonds(tmpTestMolecularArrays,
                tmpTestMolecularArrays.getAtomArray(), tmpTestMolecularArrays.getBondArray());
        IAtomContainerSet tmpExtractedFrag;
        ArrayList<String> tmpExtractedFragmentList = new ArrayList<>(5);
        try {
            tmpExtractedFrag = this.basicAlkylStructureFragmenter.extractFragments(tmpTestMolecularArrays.getAtomArray(), tmpTestMolecularArrays.getBondArray());
            for (IAtomContainer tmpAC : tmpExtractedFrag.atomContainers()) {
                ChemUtil.saturateWithHydrogen(tmpAC);
                tmpExtractedFragmentList.add(ChemUtil.createUniqueSmiles(tmpAC));
            }
        } catch (CloneNotSupportedException | CDKException e) {
            throw new RuntimeException(e);
        }
        Assertions.assertTrue(this.compareListsIgnoringOrder(tmpExtractedFragmentList, tmpExpectedFragmentsList));
    }
    /**
     * Test method for AlkylStructureFragmenter.dissectLinearChain().
     */
    @Test
    public void dissectLinearChainTest() {
        //TODO: rework!
        //ToDo: generate/choose linear structure to test linear dissection
        //get linear test structure from ACList
        //IAtomContainer tmpDissectLinearChainAC = this.testStructuresACSet.getAtomContainer(0);//correct index needed
        //Method tmpDissectMethod = this.basicAlkylStructureFragmenter.getClass().getDeclaredMethod("dissectLinearChain", IAtomContainer.class, int.class);
        //tmpDissectMethod.setAccessible(true);
        //IAtomContainer tmpNoRestrictAC = (IAtomContainer) tmpDissectMethod.invoke(this.basicAlkylStructureFragmenter, tmpDissectLinearChainAC, 0);
        //dissect test structure with different settings
        //compare with expected fragments
    }
    /**
     * Test for correct deepCopy methods by copying a butene molecule which used to make problems in earlier versions.
     *
     * @throws FileNotFoundException if test structures file can not be found or accessed
     * @throws URISyntaxException if syntax of path to test structures file is wrong
     */
    @Test
    public void deepCopyButeneTest() throws FileNotFoundException, URISyntaxException {
        this.basicAlkylStructureFragmenter.setFragmentSaturationSetting(IMoleculeFragmenter.FRAGMENT_SATURATION_OPTION_DEFAULT);
        this.basicAlkylStructureFragmenter.setKeepNonFragmentableMoleculesSetting(AlkylStructureFragmenter.KEEP_NON_FRAGMENTABLE_MOLECULES_SETTING_DEFAULT);
        this.basicAlkylStructureFragmenter.setFragmentSideChainsSetting(AlkylStructureFragmenter.FRAGMENT_SIDE_CHAINS_SETTING_DEFAULT);
        this.basicAlkylStructureFragmenter.setMaxChainLengthSetting(AlkylStructureFragmenter.MAX_CHAIN_LENGTH_SETTING_DEFAULT);
        this.basicAlkylStructureFragmenter.setAltHandlingTertQuatCarbonsSetting(AlkylStructureFragmenter.ALT_HANDLING_SINGLE_TERT_QUAT_CARBONS_SETTING_DEFAULT);
        this.basicAlkylStructureFragmenter.setMcbSingleRingDetectionSetting(AlkylStructureFragmenter.MCB_SINGLE_RING_DETECTION_SETTING_DEFAULT);
        this.basicAlkylStructureFragmenter.setKeepRingsSetting(AlkylStructureFragmenter.KEEP_RINGS_SETTING_DEFAULT);
        this.basicAlkylStructureFragmenter.setSeparateTertQuatCarbonFromRingSetting(AlkylStructureFragmenter.SEPARATE_TERT_QUAT_CARBON_FROM_RING_SETTING_DEFAULT);
        IAtomContainerSet tmpReadAtomContainerSet = this.readStructuresToACSet("de.unijena.cheminf.mortar.model.fragmentation.algorithm.ASF/ASF_Butene_Test.mol");
        IAtomContainer tmpButeneAC = tmpReadAtomContainerSet.getAtomContainer(0);
        //two steps below needed for correct internal index handling
        this.basicAlkylStructureFragmenter.fillAtomArray(tmpButeneAC);
        this.basicAlkylStructureFragmenter.fillBondArray(tmpButeneAC);
        IAtomContainer tmpCopyAC = tmpButeneAC.getBuilder().newAtomContainer();
        this.basicAlkylStructureFragmenter.setChemObjectBuilderInstance();
        for (IAtom tmpAtom: tmpButeneAC.atoms()) {
            tmpCopyAC.addAtom(this.basicAlkylStructureFragmenter.deepCopyAtom(tmpAtom));
        }
        for (IBond tmpBond: tmpButeneAC.bonds()) {
            tmpCopyAC.addBond(deepCopyBond(tmpBond, tmpCopyAC));
        }
        //Comparison of original and copied AtomContainer
        String tmpButeneSMILES = ChemUtil.createUniqueSmiles(tmpButeneAC);
        String tmpCopySMILES = ChemUtil.createUniqueSmiles(tmpCopyAC);
        Assertions.assertEquals(tmpButeneSMILES, tmpCopySMILES);
    }

    /**
     * Test for correct extraction of isolated double bonds found in linear residues after extraction of all other groups deemed interesting.
     *
     * @throws CloneNotSupportedException if cloning of original molecule in AlkylStructureFragmenter is faulty
     * @throws FileNotFoundException if test structures file can not be found or accessed
     * @throws URISyntaxException if syntax of path to test structures file is not correct
     */
    @Test
    public void linearDoubleBondExtractionTest() throws CloneNotSupportedException, FileNotFoundException, URISyntaxException {
        this.basicAlkylStructureFragmenter.setFragmentSaturationSetting(IMoleculeFragmenter.FRAGMENT_SATURATION_OPTION_DEFAULT);
        this.basicAlkylStructureFragmenter.setKeepNonFragmentableMoleculesSetting(AlkylStructureFragmenter.KEEP_NON_FRAGMENTABLE_MOLECULES_SETTING_DEFAULT);
        this.basicAlkylStructureFragmenter.setFragmentSideChainsSetting(AlkylStructureFragmenter.FRAGMENT_SIDE_CHAINS_SETTING_DEFAULT);
        this.basicAlkylStructureFragmenter.setMaxChainLengthSetting(AlkylStructureFragmenter.MAX_CHAIN_LENGTH_SETTING_DEFAULT);
        this.basicAlkylStructureFragmenter.setAltHandlingTertQuatCarbonsSetting(AlkylStructureFragmenter.ALT_HANDLING_SINGLE_TERT_QUAT_CARBONS_SETTING_DEFAULT);
        this.basicAlkylStructureFragmenter.setMcbSingleRingDetectionSetting(AlkylStructureFragmenter.MCB_SINGLE_RING_DETECTION_SETTING_DEFAULT);
        this.basicAlkylStructureFragmenter.setKeepRingsSetting(AlkylStructureFragmenter.KEEP_RINGS_SETTING_DEFAULT);
        this.basicAlkylStructureFragmenter.setSeparateTertQuatCarbonFromRingSetting(AlkylStructureFragmenter.SEPARATE_TERT_QUAT_CARBON_FROM_RING_SETTING_DEFAULT);
        this.basicAlkylStructureFragmenter.setChemObjectBuilderInstance();

        IAtomContainerSet tmpReadAtomContainerSet = this.readStructuresToACSet("de.unijena.cheminf.mortar.model.fragmentation.algorithm.ASF/ASF_Butene_Test.mol");
        IAtomContainer tmpButeneAC = tmpReadAtomContainerSet.getAtomContainer(0);
        Assertions.assertFalse(this.basicAlkylStructureFragmenter.shouldBeFiltered(tmpButeneAC));
        Assertions.assertFalse(this.basicAlkylStructureFragmenter.shouldBePreprocessed(tmpButeneAC));
        Assertions.assertTrue(this.basicAlkylStructureFragmenter.canBeFragmented(tmpButeneAC));
        List<IAtomContainer> tmpFragmentACList = this.basicAlkylStructureFragmenter.fragmentMolecule(tmpButeneAC);
        List<String> tmpExpectedList = new ArrayList<>(3);
        tmpExpectedList.add("C");
        tmpExpectedList.add("C");
        tmpExpectedList.add("C=C");
        List<String> tmpFragmentStringList = new ArrayList<>();
        for (IAtomContainer tmpAC: tmpFragmentACList) {
            tmpFragmentStringList.add(ChemUtil.createUniqueSmiles(tmpAC));
        }
        Assertions.assertTrue(this.compareListsIgnoringOrder((ArrayList) tmpExpectedList, (ArrayList) tmpFragmentStringList));
    }
    //</editor-fold>
    //<editor-fold desc="Integration Tests">
    /**
     * Method to test a default alkyl structure fragmentation on a concept molecule.
     *
     * @throws Exception if fragmentation does not result in expected fragments
     */
    @Test
    public void defaultFragmentationTest() throws Exception {
        this.basicAlkylStructureFragmenter.setFragmentSaturationSetting(IMoleculeFragmenter.FRAGMENT_SATURATION_OPTION_DEFAULT);
        this.basicAlkylStructureFragmenter.setKeepNonFragmentableMoleculesSetting(false);
        this.basicAlkylStructureFragmenter.setFragmentSideChainsSetting(AlkylStructureFragmenter.FRAGMENT_SIDE_CHAINS_SETTING_DEFAULT);
        this.basicAlkylStructureFragmenter.setMaxChainLengthSetting(AlkylStructureFragmenter.MAX_CHAIN_LENGTH_SETTING_DEFAULT);
        this.basicAlkylStructureFragmenter.setAltHandlingTertQuatCarbonsSetting(AlkylStructureFragmenter.ALT_HANDLING_SINGLE_TERT_QUAT_CARBONS_SETTING_DEFAULT);
        this.basicAlkylStructureFragmenter.setMcbSingleRingDetectionSetting(AlkylStructureFragmenter.MCB_SINGLE_RING_DETECTION_SETTING_DEFAULT);
        this.basicAlkylStructureFragmenter.setKeepRingsSetting(AlkylStructureFragmenter.KEEP_RINGS_SETTING_DEFAULT);
        for (IAtomContainer tmpAtomContainer :
                this.testStructuresACSet.atomContainers()) {
            Assertions.assertFalse(this.basicAlkylStructureFragmenter.shouldBeFiltered(tmpAtomContainer));
            Assertions.assertFalse(this.basicAlkylStructureFragmenter.shouldBePreprocessed(tmpAtomContainer));
            Assertions.assertTrue(this.basicAlkylStructureFragmenter.canBeFragmented(tmpAtomContainer));
        }
        List<IAtomContainer> tmpResultList = this.basicAlkylStructureFragmenter.fragmentMolecule(this.testStructuresACSet.getAtomContainer(1));
        IAtomContainerSet tmpResultACSet = new AtomContainerSet();
        for (IAtomContainer tmpAtomContainer : tmpResultList) {
            tmpResultACSet.addAtomContainer(tmpAtomContainer);
        }
        List<String> tmpResultSMILESList = this.generateSMILESFromACSet(tmpResultACSet);
        IAtomContainerSet tmpExpectedACSet = this.readStructuresToACSet("de.unijena.cheminf.mortar.model.fragmentation.algorithm.ASF/ASF_Expected_Fragments_Test_Structure.sdf");
        List<String> tmpExpectedSMILESList = this.generateSMILESFromACSet(tmpExpectedACSet);
        boolean tmpChemicalFormulaCheck = this.checkChemicalFormula(this.testStructuresACSet.getAtomContainer(1),
                tmpExpectedACSet, tmpResultACSet);
        Assertions.assertTrue(this.compareListsIgnoringOrder(new ArrayList<>(tmpResultSMILESList),
                new ArrayList<>(tmpExpectedSMILESList)) && tmpChemicalFormulaCheck);
    }

    /**
     * Test for correct fragmentation by fragmentation of real natural product to simulate user environment for fragmentation.
     *
     * @throws Exception if molecule is not correctly fragmented
     */
    //ToDo: change current placeholder molecule to fitting natural compound
    @Test
    public void naturalCompoundFragmentationTest() throws Exception {
        this.basicAlkylStructureFragmenter.setFragmentSaturationSetting(IMoleculeFragmenter.FRAGMENT_SATURATION_OPTION_DEFAULT);
        this.basicAlkylStructureFragmenter.setFragmentSideChainsSetting(AlkylStructureFragmenter.FRAGMENT_SIDE_CHAINS_SETTING_DEFAULT);
        this.basicAlkylStructureFragmenter.setMaxChainLengthSetting(AlkylStructureFragmenter.MAX_CHAIN_LENGTH_SETTING_DEFAULT);
        this.basicAlkylStructureFragmenter.setAltHandlingTertQuatCarbonsSetting(AlkylStructureFragmenter.ALT_HANDLING_SINGLE_TERT_QUAT_CARBONS_SETTING_DEFAULT);
        this.basicAlkylStructureFragmenter.setMcbSingleRingDetectionSetting(AlkylStructureFragmenter.MCB_SINGLE_RING_DETECTION_SETTING_DEFAULT);
        this.basicAlkylStructureFragmenter.setKeepRingsSetting(AlkylStructureFragmenter.KEEP_RINGS_SETTING_DEFAULT);
        for (IAtomContainer tmpAtomContainer :
                this.testStructuresACSet.atomContainers()) {
            Assertions.assertFalse(this.basicAlkylStructureFragmenter.shouldBeFiltered(tmpAtomContainer));
            Assertions.assertFalse(this.basicAlkylStructureFragmenter.shouldBePreprocessed(tmpAtomContainer));
            Assertions.assertTrue(this.basicAlkylStructureFragmenter.canBeFragmented(tmpAtomContainer));
        }
        List<IAtomContainer> tmpAtomContainerList = this.basicAlkylStructureFragmenter.fragmentMolecule(this.testStructuresACSet.getAtomContainer(0));
        IAtomContainerSet tmpResultACSet = new AtomContainerSet();
        for (IAtomContainer tmpAtomContainer : tmpAtomContainerList) {
            tmpResultACSet.addAtomContainer(tmpAtomContainer);
        }
        List<String> tmpResultSMILESList = this.generateSMILESFromACSet(tmpResultACSet);
        IAtomContainerSet tmpExpectedACSet = this.readStructuresToACSet("de.unijena.cheminf.mortar.model.fragmentation.algorithm.ASF/ASF_Expected_Fragments_Natural_Compound.sdf");
        List<String> tmpExpectedSMILESList = this.generateSMILESFromACSet(tmpExpectedACSet);
        Assertions.assertTrue(this.compareListsIgnoringOrder(new ArrayList<>(tmpResultSMILESList),
                new ArrayList<>(tmpExpectedSMILESList)));
    }
    //</editor-fold>
    //</editor-fold>

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
     * Utility method generating SMILES notation strings for a given AtomContainerSet.
     *
     * @param anACSet given AtomContainerSet
     * @return List containing the generated Strings
     */
    private List<String> generateSMILESFromACSet(IAtomContainerSet anACSet) {
        List<String> tmpSmilesList = new ArrayList<>(anACSet.getAtomContainerCount());
        for (IAtomContainer tmpAC : anACSet.atomContainers()) {
            tmpSmilesList.add(ChemUtil.createUniqueSmiles(tmpAC));
        }
        return tmpSmilesList;
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

/*
 * MORTAR - MOlecule fRagmenTAtion fRamework
 * Copyright (C) 2026  Felix Baensch, Jonas Schaub (felix.j.baensch@gmail.com, jonas.schaub@uni-jena.de)
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
import org.openscience.cdk.exception.CDKException;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.silent.SilentChemObjectBuilder;
import org.openscience.cdk.smiles.SmiFlavor;
import org.openscience.cdk.smiles.SmilesGenerator;
import org.openscience.cdk.smiles.SmilesParser;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Class to test the correct workings of
 * {@link de.unijena.cheminf.mortar.model.fragmentation.algorithm.CDKExhaustiveFragmenter}.
 *
 * <p>Most tests use 1,2-diphenylethane (bibenzyl, c1ccccc1CCc1ccccc1), which has three splittable bonds (ring C-CH2,
 * CH2-CH2, CH2-ring C), so that its fragments can be derived by hand.</p>
 *
 * @author Tom Weiß
 * @version 1.0.0.0
 */
public class CDKExhaustiveFragmenterTest {
    /**
     * SMILES code of 1,2-diphenylethane (bibenzyl).
     */
    private static final String BIBENZYL_SMILES = "c1ccccc1CCc1ccccc1";
    //
    /**
     * SMILES code of the COCONUT natural product CNP0381655.1, which has two stereo centres and a stereo double bond.
     */
    private static final String CNP0381655_SMILES = "CCCCC[C@H]1O[C@@H]1/C=C/C=O";
    //
    /**
     * Constructor that sets the default locale to British English, which is important for the correct functioning of the
     * fragmenter because the settings tooltips are imported from the message.properties file.
     */
    public CDKExhaustiveFragmenterTest() {
        Locale.setDefault(Locale.of("en", "GB"));
    }
    //
    /**
     * Tests instantiation and basic settings retrieval.
     *
     * @throws Exception if anything goes wrong
     */
    @Test
    public void basicTest() throws Exception {
        CDKExhaustiveFragmenter tmpFragmenter = new CDKExhaustiveFragmenter();
        Assertions.assertDoesNotThrow(tmpFragmenter::getFragmentationAlgorithmName);
        Assertions.assertDoesNotThrow(tmpFragmenter::getFragmentationAlgorithmDisplayName);
        Assertions.assertDoesNotThrow(tmpFragmenter::minimumFragmentSizeSettingProperty);
        Assertions.assertDoesNotThrow(tmpFragmenter::inclusiveSplittableBondsLimitSettingProperty);
        Assertions.assertDoesNotThrow(tmpFragmenter::getInclusiveMaxTreeDepthSetting);
        Assertions.assertDoesNotThrow(tmpFragmenter::getSaturationSetting);
        Assertions.assertDoesNotThrow(tmpFragmenter::preserveStereoSettingProperty);
        Assertions.assertDoesNotThrow(tmpFragmenter::getMinimumFragmentSize);
        for (Property<?> tmpSetting : tmpFragmenter.settingsProperties()) {
            Assertions.assertDoesNotThrow(tmpSetting::getName);
        }
        int tmpMinimumFragmentSize = 8;
        tmpFragmenter.setMinimumFragmentSize(tmpMinimumFragmentSize);
        Assertions.assertEquals(tmpMinimumFragmentSize, tmpFragmenter.getMinimumFragmentSize());
    }
    //
    /**
     * Does a test fragmentation on the COCONUT natural product CNP0151033.
     *
     * @throws Exception if anything goes wrong
     */
    @Test
    public void fragmentationTest() throws Exception {
        SmilesParser tmpSmiPar = new SmilesParser(SilentChemObjectBuilder.getInstance());
        SmilesGenerator tmpSmiGen = new SmilesGenerator((SmiFlavor.Canonical));
        IAtomContainer tmpOriginalMolecule;
        List<IAtomContainer> tmpFragmentList;
        CDKExhaustiveFragmenter tmpFragmenter = new CDKExhaustiveFragmenter();
        tmpOriginalMolecule = tmpSmiPar.parseSmiles(
                //CNP0151033
                "O=C(OC1C(OCC2=COC(OC(=O)CC(C)C)C3C2CC(O)C3(O)COC(=O)C)OC(CO)C(O)C1O)C=CC4=CC=C(O)C=C4");
        Assertions.assertFalse(tmpFragmenter.shouldBeFiltered(tmpOriginalMolecule));
        Assertions.assertFalse(tmpFragmenter.shouldBePreprocessed(tmpOriginalMolecule));
        Assertions.assertTrue(tmpFragmenter.canBeFragmented(tmpOriginalMolecule));
        tmpFragmentList = tmpFragmenter.fragmentMolecule(tmpOriginalMolecule);
        Assertions.assertFalse(tmpFragmentList.isEmpty());
        for (IAtomContainer tmpFragment : tmpFragmentList) {
            Assertions.assertDoesNotThrow(() -> tmpSmiGen.create(tmpFragment));
        }
    }
    //
    /**
     * Tests the preservation of stereochemistry on the COCONUT natural product CNP0381655.1. It has six splittable
     * bonds, and the ten fragments with at least six heavy atoms are checked exactly. The stereo centre on the
     * pentyl side is only retained if the pentyl carbon atom bonded to it is part of the fragment, the stereo centre
     * on the other side only if the vinyl carbon atom bonded to it is, and the double bond configuration only if the
     * aldehyde group is. The expected stereo descriptors were verified against SMILES codes derived by hand from the
     * input SMILES code.
     *
     * @throws Exception if anything goes wrong
     */
    @Test
    public void fragmentationTestStereo() throws Exception {
        SmilesParser tmpSmiPar = new SmilesParser(SilentChemObjectBuilder.getInstance());
        CDKExhaustiveFragmenter tmpFragmenter = new CDKExhaustiveFragmenter();
        tmpFragmenter.setPreserveStereoSetting(true);
        IAtomContainer tmpOriginalMolecule = tmpSmiPar.parseSmiles(CDKExhaustiveFragmenterTest.CNP0381655_SMILES);
        Assertions.assertFalse(tmpFragmenter.shouldBeFiltered(tmpOriginalMolecule));
        Set<String> tmpExpected = Set.of(
                "C(=C\\[C@@H]1CO1)/C=O",
                "C=C[C@@H]1[C@@H](C)O1",
                "C=C[C@@H]1[C@@H](CC)O1",
                "CCCCC[C@@H]1CO1",
                "CCCCC[C@@H]1[C@@H](C=C)O1",
                "CCC[C@@H]1CO1",
                "CCC[C@@H]1[C@@H](/C=C/C=O)O1",
                "CCC[C@@H]1[C@@H](C=C)O1",
                "CC[C@@H]1[C@@H](/C=C/C=O)O1",
                "C[C@@H]1[C@@H](/C=C/C=O)O1");
        Assertions.assertEquals(tmpExpected, this.getFragmentSmilesSet(
                tmpFragmenter.fragmentMolecule(tmpOriginalMolecule), SmiFlavor.Absolute | SmiFlavor.UseAromaticSymbols));
    }
    //
    /**
     * Negative control for {@link #fragmentationTestStereo()}: with the preserve stereo setting at its default (false),
     * the same ten fragments of CNP0381655.1 are generated, but without any stereo information, even if the SMILES
     * codes are generated with stereo flavour.
     *
     * @throws Exception if anything goes wrong
     */
    @Test
    public void fragmentationTestStereoNotPreserved() throws Exception {
        SmilesParser tmpSmiPar = new SmilesParser(SilentChemObjectBuilder.getInstance());
        CDKExhaustiveFragmenter tmpFragmenter = new CDKExhaustiveFragmenter();
        Assertions.assertFalse(tmpFragmenter.getPreserveStereoSetting());
        IAtomContainer tmpOriginalMolecule = tmpSmiPar.parseSmiles(CDKExhaustiveFragmenterTest.CNP0381655_SMILES);
        Set<String> tmpExpected = Set.of(
                "C(=CC1CO1)C=O",
                "C=CC1C(C)O1",
                "C=CC1C(CC)O1",
                "CC1C(C=CC=O)O1",
                "CCC1C(C=CC=O)O1",
                "CCCC1C(C=C)O1",
                "CCCC1C(C=CC=O)O1",
                "CCCC1CO1",
                "CCCCCC1C(C=C)O1",
                "CCCCCC1CO1");
        Assertions.assertEquals(tmpExpected, this.getFragmentSmilesSet(
                tmpFragmenter.fragmentMolecule(tmpOriginalMolecule), SmiFlavor.Absolute | SmiFlavor.UseAromaticSymbols));
    }
    //
    /**
     * Tests the fragmentation of bibenzyl with the default settings (minimum fragment size 6, hydrogen saturation).
     * The expected fragments are benzene, toluene, and ethylbenzene; the methane and ethane fragments are below the
     * minimum fragment size.
     *
     * @throws Exception if anything goes wrong
     */
    @Test
    public void expectedFragmentsHydrogenSaturatedTest() throws Exception {
        SmilesParser tmpSmiPar = new SmilesParser(SilentChemObjectBuilder.getInstance());
        CDKExhaustiveFragmenter tmpFragmenter = new CDKExhaustiveFragmenter();
        List<IAtomContainer> tmpFragmentList = tmpFragmenter.fragmentMolecule(
                tmpSmiPar.parseSmiles(CDKExhaustiveFragmenterTest.BIBENZYL_SMILES));
        Assertions.assertEquals(Set.of("c1ccccc1", "c1ccc(cc1)C", "c1ccc(cc1)CC"), this.getFragmentSmilesSet(tmpFragmentList));
    }
    //
    /**
     * Tests that with unsaturated fragments, no hydrogen atoms are added at the split positions, i.e. bibenzyl yields
     * the phenyl, benzyl, and 2-phenylethyl radicals.
     *
     * @throws Exception if anything goes wrong
     */
    @Test
    public void expectedFragmentsUnsaturatedTest() throws Exception {
        SmilesParser tmpSmiPar = new SmilesParser(SilentChemObjectBuilder.getInstance());
        CDKExhaustiveFragmenter tmpFragmenter = new CDKExhaustiveFragmenter();
        tmpFragmenter.setSaturationSetting(CDKExhaustiveFragmenter.SaturationDisplay.UNSATURATED_FRAGMENTS);
        List<IAtomContainer> tmpFragmentList = tmpFragmenter.fragmentMolecule(
                tmpSmiPar.parseSmiles(CDKExhaustiveFragmenterTest.BIBENZYL_SMILES));
        Assertions.assertEquals(Set.of("[c]1ccccc1", "[CH2]c1ccccc1", "[CH2]Cc1ccccc1"), this.getFragmentSmilesSet(tmpFragmentList));
    }
    //
    /**
     * Tests that with R-group saturation, every bibenzyl fragment gets exactly one R atom (*) at its split position.
     * Also tests that R atoms do not count towards the minimum fragment size: with a minimum size of 7, the phenyl-R
     * fragment (6 heavy atoms + 1 R atom) must be excluded.
     *
     * @throws Exception if anything goes wrong
     */
    @Test
    public void expectedFragmentsRSaturatedTest() throws Exception {
        SmilesParser tmpSmiPar = new SmilesParser(SilentChemObjectBuilder.getInstance());
        IAtomContainer tmpBibenzyl = tmpSmiPar.parseSmiles(CDKExhaustiveFragmenterTest.BIBENZYL_SMILES);
        CDKExhaustiveFragmenter tmpFragmenter = new CDKExhaustiveFragmenter();
        tmpFragmenter.setSaturationSetting(CDKExhaustiveFragmenter.SaturationDisplay.R_SATURATED_FRAGMENTS);
        Assertions.assertEquals(Set.of("*c1ccccc1", "*Cc1ccccc1", "*CCc1ccccc1"),
                this.getFragmentSmilesSet(tmpFragmenter.fragmentMolecule(tmpBibenzyl)));
        tmpFragmenter.setMinimumFragmentSize(7);
        Assertions.assertEquals(Set.of("*Cc1ccccc1", "*CCc1ccccc1"),
                this.getFragmentSmilesSet(tmpFragmenter.fragmentMolecule(tmpBibenzyl)));
    }
    //
    /**
     * Tests that the minimum fragment size setting is forwarded to the CDK fragmenter and changes the results.
     *
     * @throws Exception if anything goes wrong
     */
    @Test
    public void minimumFragmentSizeTest() throws Exception {
        SmilesParser tmpSmiPar = new SmilesParser(SilentChemObjectBuilder.getInstance());
        IAtomContainer tmpBibenzyl = tmpSmiPar.parseSmiles(CDKExhaustiveFragmenterTest.BIBENZYL_SMILES);
        CDKExhaustiveFragmenter tmpFragmenter = new CDKExhaustiveFragmenter();
        tmpFragmenter.setMinimumFragmentSize(7);
        Assertions.assertEquals(Set.of("c1ccc(cc1)C", "c1ccc(cc1)CC"),
                this.getFragmentSmilesSet(tmpFragmenter.fragmentMolecule(tmpBibenzyl)));
        tmpFragmenter.setMinimumFragmentSize(8);
        Assertions.assertEquals(Set.of("c1ccc(cc1)CC"), this.getFragmentSmilesSet(tmpFragmenter.fragmentMolecule(tmpBibenzyl)));
        tmpFragmenter.setMinimumFragmentSize(9);
        Assertions.assertTrue(tmpFragmenter.fragmentMolecule(tmpBibenzyl).isEmpty());
        tmpFragmenter.setMinimumFragmentSize(1);
        Assertions.assertEquals(Set.of("C", "CC", "c1ccccc1", "c1ccc(cc1)C", "c1ccc(cc1)CC"),
                this.getFragmentSmilesSet(tmpFragmenter.fragmentMolecule(tmpBibenzyl)));
        Assertions.assertThrows(IllegalArgumentException.class, () -> tmpFragmenter.setMinimumFragmentSize(0));
        Assertions.assertEquals(1, tmpFragmenter.getMinimumFragmentSize());
    }
    //
    /**
     * Tests that the maximum tree depth setting is forwarded to the CDK fragmenter and changes the results. With a
     * minimum fragment size of 1 and a tree depth of 1, only one bond is split at a time, so the methane and ethane
     * fragments of bibenzyl (which require two split bonds) are not generated.
     *
     * @throws Exception if anything goes wrong
     */
    @Test
    public void maxTreeDepthTest() throws Exception {
        SmilesParser tmpSmiPar = new SmilesParser(SilentChemObjectBuilder.getInstance());
        IAtomContainer tmpBibenzyl = tmpSmiPar.parseSmiles(CDKExhaustiveFragmenterTest.BIBENZYL_SMILES);
        CDKExhaustiveFragmenter tmpFragmenter = new CDKExhaustiveFragmenter();
        tmpFragmenter.setMinimumFragmentSize(1);
        tmpFragmenter.setInclusiveMaxTreeDepthSetting(1);
        Assertions.assertEquals(Set.of("c1ccccc1", "c1ccc(cc1)C", "c1ccc(cc1)CC"),
                this.getFragmentSmilesSet(tmpFragmenter.fragmentMolecule(tmpBibenzyl)));
        tmpFragmenter.setInclusiveMaxTreeDepthSetting(2);
        Assertions.assertEquals(Set.of("C", "CC", "c1ccccc1", "c1ccc(cc1)C", "c1ccc(cc1)CC"),
                this.getFragmentSmilesSet(tmpFragmenter.fragmentMolecule(tmpBibenzyl)));
        Assertions.assertThrows(IllegalArgumentException.class, () -> tmpFragmenter.setInclusiveMaxTreeDepthSetting(0));
        Assertions.assertThrows(IllegalArgumentException.class, () -> tmpFragmenter.setInclusiveMaxTreeDepthSetting(
                CDKExhaustiveFragmenter.INCLUSIVE_MAX_TREE_DEPTH_LIMIT + 1));
        Assertions.assertEquals(2, tmpFragmenter.getInclusiveMaxTreeDepthSetting());
    }
    //
    /**
     * Tests that the documented upper limit of the maximum tree depth setting is accepted.
     */
    @Test
    public void maxTreeDepthUpperLimitTest() {
        CDKExhaustiveFragmenter tmpFragmenter = new CDKExhaustiveFragmenter();
        Assertions.assertDoesNotThrow(() -> tmpFragmenter.setInclusiveMaxTreeDepthSetting(
                CDKExhaustiveFragmenter.INCLUSIVE_MAX_TREE_DEPTH_LIMIT));
        Assertions.assertEquals(CDKExhaustiveFragmenter.INCLUSIVE_MAX_TREE_DEPTH_LIMIT,
                tmpFragmenter.getInclusiveMaxTreeDepthSetting());
    }
    //
    /**
     * Tests the filtering of molecules based on their number of splittable bonds. Bibenzyl has three splittable bonds;
     * the limit is inclusive.
     *
     * @throws Exception if anything goes wrong
     */
    @Test
    public void splittableBondsLimitTest() throws Exception {
        SmilesParser tmpSmiPar = new SmilesParser(SilentChemObjectBuilder.getInstance());
        IAtomContainer tmpBibenzyl = tmpSmiPar.parseSmiles(CDKExhaustiveFragmenterTest.BIBENZYL_SMILES);
        CDKExhaustiveFragmenter tmpFragmenter = new CDKExhaustiveFragmenter();
        Assertions.assertTrue(tmpFragmenter.getActivateSplittableBondsLimitSetting());
        tmpFragmenter.setLimitForSplittableBonds(3);
        Assertions.assertFalse(tmpFragmenter.shouldBeFiltered(tmpBibenzyl));
        Assertions.assertTrue(tmpFragmenter.canBeFragmented(tmpBibenzyl));
        tmpFragmenter.setLimitForSplittableBonds(2);
        Assertions.assertTrue(tmpFragmenter.shouldBeFiltered(tmpBibenzyl));
        Assertions.assertFalse(tmpFragmenter.canBeFragmented(tmpBibenzyl));
        Assertions.assertThrows(IllegalArgumentException.class, () -> tmpFragmenter.fragmentMolecule(tmpBibenzyl));
        Assertions.assertThrows(IllegalArgumentException.class, () -> tmpFragmenter.applyPreprocessing(tmpBibenzyl));
        tmpFragmenter.setActivateSplittableBondsLimitSetting(false);
        Assertions.assertFalse(tmpFragmenter.shouldBeFiltered(tmpBibenzyl));
        Assertions.assertEquals(Set.of("c1ccccc1", "c1ccc(cc1)C", "c1ccc(cc1)CC"),
                this.getFragmentSmilesSet(tmpFragmenter.fragmentMolecule(tmpBibenzyl)));
        Assertions.assertThrows(IllegalArgumentException.class, () -> tmpFragmenter.setLimitForSplittableBonds(0));
        Assertions.assertEquals(2, tmpFragmenter.getInclusiveLimitForSplittableBonds());
        //empty molecules are always filtered
        Assertions.assertTrue(tmpFragmenter.shouldBeFiltered(SilentChemObjectBuilder.getInstance().newAtomContainer()));
    }
    //
    /**
     * Tests that molecules with more than {@link CDKExhaustiveFragmenter#MAXIMUM_SPLITTABLE_BONDS} splittable bonds are
     * always filtered, even if the splittable bonds limit is deactivated or set higher. A linear alkane with n carbon
     * atoms has n - 3 splittable bonds. The molecules are only checked, not fragmented, because fragmenting 30
     * splittable bonds would take far too long for a unit test.
     *
     * @throws Exception if anything goes wrong
     */
    @Test
    public void maximumSplittableBondsTest() throws Exception {
        SmilesParser tmpSmiPar = new SmilesParser(SilentChemObjectBuilder.getInstance());
        int tmpMaximum = CDKExhaustiveFragmenter.MAXIMUM_SPLITTABLE_BONDS;
        IAtomContainer tmpAlkaneAtMaximum = tmpSmiPar.parseSmiles("C".repeat(tmpMaximum + 3));
        IAtomContainer tmpAlkaneAboveMaximum = tmpSmiPar.parseSmiles("C".repeat(tmpMaximum + 4));
        CDKExhaustiveFragmenter tmpFragmenter = new CDKExhaustiveFragmenter();
        tmpFragmenter.setActivateSplittableBondsLimitSetting(false);
        Assertions.assertFalse(tmpFragmenter.shouldBeFiltered(tmpAlkaneAtMaximum));
        Assertions.assertTrue(tmpFragmenter.shouldBeFiltered(tmpAlkaneAboveMaximum));
        Assertions.assertFalse(tmpFragmenter.canBeFragmented(tmpAlkaneAboveMaximum));
        Assertions.assertThrows(IllegalArgumentException.class, () -> tmpFragmenter.fragmentMolecule(tmpAlkaneAboveMaximum));
    }
    //
    /**
     * Tests that a molecule without splittable bonds yields no fragments and that the input molecule is not altered.
     *
     * @throws Exception if anything goes wrong
     */
    @Test
    public void noSplittableBondsTest() throws Exception {
        SmilesParser tmpSmiPar = new SmilesParser(SilentChemObjectBuilder.getInstance());
        SmilesGenerator tmpSmiGen = new SmilesGenerator(SmiFlavor.Unique);
        CDKExhaustiveFragmenter tmpFragmenter = new CDKExhaustiveFragmenter();
        //naphthalene, only ring bonds
        IAtomContainer tmpNaphthalene = tmpSmiPar.parseSmiles("c1ccc2ccccc2c1");
        Assertions.assertTrue(tmpFragmenter.fragmentMolecule(tmpNaphthalene).isEmpty());
        IAtomContainer tmpBibenzyl = tmpSmiPar.parseSmiles(CDKExhaustiveFragmenterTest.BIBENZYL_SMILES);
        String tmpSmilesBefore = tmpSmiGen.create(tmpBibenzyl);
        tmpFragmenter.fragmentMolecule(tmpBibenzyl);
        Assertions.assertEquals(tmpSmilesBefore, tmpSmiGen.create(tmpBibenzyl));
    }
    //
    /**
     * Tests that copy() transfers all settings, including to the wrapped CDK fragmenter, and that the copy is
     * independent of the original.
     *
     * @throws Exception if anything goes wrong
     */
    @Test
    public void copyTest() throws Exception {
        SmilesParser tmpSmiPar = new SmilesParser(SilentChemObjectBuilder.getInstance());
        CDKExhaustiveFragmenter tmpFragmenter = new CDKExhaustiveFragmenter();
        tmpFragmenter.setMinimumFragmentSize(7);
        tmpFragmenter.setActivateSplittableBondsLimitSetting(false);
        tmpFragmenter.setLimitForSplittableBonds(5);
        tmpFragmenter.setInclusiveMaxTreeDepthSetting(10);
        tmpFragmenter.setSaturationSetting(CDKExhaustiveFragmenter.SaturationDisplay.R_SATURATED_FRAGMENTS);
        tmpFragmenter.setPreserveStereoSetting(true);
        CDKExhaustiveFragmenter tmpCopy = (CDKExhaustiveFragmenter) tmpFragmenter.copy();
        Assertions.assertNotSame(tmpFragmenter, tmpCopy);
        Assertions.assertEquals(7, tmpCopy.getMinimumFragmentSize());
        Assertions.assertFalse(tmpCopy.getActivateSplittableBondsLimitSetting());
        Assertions.assertEquals(5, tmpCopy.getInclusiveLimitForSplittableBonds());
        Assertions.assertEquals(10, tmpCopy.getInclusiveMaxTreeDepthSetting());
        Assertions.assertEquals(CDKExhaustiveFragmenter.SaturationDisplay.R_SATURATED_FRAGMENTS, tmpCopy.getSaturationSetting());
        Assertions.assertTrue(tmpCopy.getPreserveStereoSetting());
        //the wrapped CDK instance of the copy must use the copied settings, i.e. min size 7 and R saturation
        Assertions.assertEquals(Set.of("*Cc1ccccc1", "*CCc1ccccc1"), this.getFragmentSmilesSet(
                tmpCopy.fragmentMolecule(tmpSmiPar.parseSmiles(CDKExhaustiveFragmenterTest.BIBENZYL_SMILES))));
        tmpCopy.setMinimumFragmentSize(6);
        Assertions.assertEquals(7, tmpFragmenter.getMinimumFragmentSize());
    }
    //
    /**
     * Tests that restoreDefaultSettings() resets all settings to the public default constants, including in the
     * wrapped CDK fragmenter.
     *
     * @throws Exception if anything goes wrong
     */
    @Test
    public void restoreDefaultSettingsTest() throws Exception {
        SmilesParser tmpSmiPar = new SmilesParser(SilentChemObjectBuilder.getInstance());
        CDKExhaustiveFragmenter tmpFragmenter = new CDKExhaustiveFragmenter();
        tmpFragmenter.setMinimumFragmentSize(9);
        tmpFragmenter.setActivateSplittableBondsLimitSetting(!CDKExhaustiveFragmenter.DEFAULT_ACTIVATE_SPLITTABLE_BONDS_LIMIT);
        tmpFragmenter.setLimitForSplittableBonds(2);
        tmpFragmenter.setInclusiveMaxTreeDepthSetting(1);
        tmpFragmenter.setSaturationSetting(CDKExhaustiveFragmenter.SaturationDisplay.UNSATURATED_FRAGMENTS);
        tmpFragmenter.setPreserveStereoSetting(!CDKExhaustiveFragmenter.DEFAULT_PRESERVE_STEREO_INFO);
        tmpFragmenter.restoreDefaultSettings();
        Assertions.assertEquals(CDKExhaustiveFragmenter.DEFAULT_MINIMUM_FRAGMENT_SIZE, tmpFragmenter.getMinimumFragmentSize());
        Assertions.assertEquals(CDKExhaustiveFragmenter.DEFAULT_ACTIVATE_SPLITTABLE_BONDS_LIMIT,
                tmpFragmenter.getActivateSplittableBondsLimitSetting());
        Assertions.assertEquals(CDKExhaustiveFragmenter.DEFAULT_LIMIT_FOR_SPLITTABLE_BONDS,
                tmpFragmenter.getInclusiveLimitForSplittableBonds());
        Assertions.assertEquals(CDKExhaustiveFragmenter.DEFAULT_INCLUSIVE_MAX_TREE_DEPTH,
                tmpFragmenter.getInclusiveMaxTreeDepthSetting());
        Assertions.assertEquals(CDKExhaustiveFragmenter.DEFAULT_SATURATION, tmpFragmenter.getSaturationSetting());
        Assertions.assertEquals(CDKExhaustiveFragmenter.DEFAULT_PRESERVE_STEREO_INFO, tmpFragmenter.getPreserveStereoSetting());
        //the wrapped CDK instance must be reset as well
        Assertions.assertEquals(Set.of("c1ccccc1", "c1ccc(cc1)C", "c1ccc(cc1)CC"), this.getFragmentSmilesSet(
                tmpFragmenter.fragmentMolecule(tmpSmiPar.parseSmiles(CDKExhaustiveFragmenterTest.BIBENZYL_SMILES))));
    }
    //
    /**
     * Returns the unique SMILES codes (with aromatic symbols) of the given fragments as a set.
     *
     * @param aFragmentList fragments to convert
     * @return set of fragment SMILES codes
     * @throws CDKException if a SMILES code cannot be generated
     */
    private Set<String> getFragmentSmilesSet(List<IAtomContainer> aFragmentList) throws CDKException {
        return this.getFragmentSmilesSet(aFragmentList, SmiFlavor.Unique | SmiFlavor.UseAromaticSymbols);
    }
    //
    /**
     * Returns the SMILES codes of the given fragments as a set, generated with the given SMILES flavour. Fails if two
     * fragments have the same SMILES code, because the fragmenter is expected to return only unique fragments.
     *
     * @param aFragmentList fragments to convert
     * @param aSmiFlavor SMILES flavour, see {@link SmiFlavor}
     * @return set of fragment SMILES codes
     * @throws CDKException if a SMILES code cannot be generated
     */
    private Set<String> getFragmentSmilesSet(List<IAtomContainer> aFragmentList, int aSmiFlavor) throws CDKException {
        SmilesGenerator tmpSmiGen = new SmilesGenerator(aSmiFlavor);
        Set<String> tmpSmilesSet = new HashSet<>(aFragmentList.size() * 2);
        for (IAtomContainer tmpFragment : aFragmentList) {
            String tmpSmiles = tmpSmiGen.create(tmpFragment);
            Assertions.assertTrue(tmpSmilesSet.add(tmpSmiles), "Duplicate fragment " + tmpSmiles);
        }
        return tmpSmilesSet;
    }
}

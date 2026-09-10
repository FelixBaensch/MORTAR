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
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.silent.AtomContainer;
import org.openscience.cdk.silent.SilentChemObjectBuilder;
import org.openscience.cdk.smiles.SmilesParser;

import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Class to test the correct working of
 * {@link de.unijena.cheminf.mortar.model.fragmentation.algorithm.ScaffoldGeneratorFragmenter}.
 *
 * @author Felix Baensch
 * @version 1.0.0.0
 */
public class ScaffoldGeneratorFragmenterTest {
    //<editor-fold desc="Private static final class constants">
    /**
     * SMILES of a fused bicyclic molecule carrying side chains (substituted tetralin with an ethyl group and a
     * carboxylic acid group), used to drive the full FragmentationType x SideChain matrix so that all scaffold modes
     * and the side-chain branches produce meaningful output.
     */
    private static final String FUSED_BICYCLIC_WITH_SIDE_CHAINS_SMILES = "CCc1ccc2c(c1)CCCC2C(=O)O";
    //</editor-fold>
    //
    //<editor-fold desc="Constructor">
    /**
     * Constructor that sets the default locale to british english, which is important for the correct functioning of the
     * fragmenter because the settings tooltips are imported from the message.properties file.
     */
    public ScaffoldGeneratorFragmenterTest() {
        Locale.setDefault(Locale.of("en", "GB"));
    }
    //</editor-fold>
    //
    //<editor-fold desc="Test methods">
    /**
     * Tests instantiation and basic settings retrieval: the algorithm name/display-name accessors and every
     * settingsProperties() entry's getName() must not throw.
     *
     * @throws Exception if anything goes wrong
     */
    @Test
    public void basicTest() throws Exception {
        ScaffoldGeneratorFragmenter tmpFragmenter = new ScaffoldGeneratorFragmenter();
        Assertions.assertDoesNotThrow(tmpFragmenter::getFragmentationAlgorithmName);
        Assertions.assertDoesNotThrow(tmpFragmenter::getFragmentationAlgorithmDisplayName);
        Assertions.assertEquals(ScaffoldGeneratorFragmenter.ALGORITHM_NAME, tmpFragmenter.getFragmentationAlgorithmName());
        for (Property<?> tmpSetting : tmpFragmenter.settingsProperties()) {
            Assertions.assertDoesNotThrow(tmpSetting::getName);
        }
    }
    //
    /**
     * Tests the lifecycle methods on a fused bicyclic molecule: it is neither filtered nor preprocessed, can be
     * fragmented, yields a non-empty fragment list, and the first fragment carries the scaffold category property.
     *
     * @throws Exception if anything goes wrong
     */
    @Test
    public void lifecycleTest() throws Exception {
        SmilesParser tmpSmiPar = new SmilesParser(SilentChemObjectBuilder.getInstance());
        ScaffoldGeneratorFragmenter tmpFragmenter = new ScaffoldGeneratorFragmenter();
        IAtomContainer tmpOriginalMolecule = tmpSmiPar.parseSmiles(
                ScaffoldGeneratorFragmenterTest.FUSED_BICYCLIC_WITH_SIDE_CHAINS_SMILES);
        Assertions.assertFalse(tmpFragmenter.shouldBeFiltered(tmpOriginalMolecule));
        Assertions.assertFalse(tmpFragmenter.shouldBePreprocessed(tmpOriginalMolecule));
        Assertions.assertTrue(tmpFragmenter.canBeFragmented(tmpOriginalMolecule));
        List<IAtomContainer> tmpFragmentList = tmpFragmenter.fragmentMolecule(tmpOriginalMolecule);
        Assertions.assertFalse(tmpFragmentList.isEmpty());
        Assertions.assertEquals(ScaffoldGeneratorFragmenter.FRAGMENT_CATEGORY_SCAFFOLD_VALUE,
                tmpFragmentList.getFirst().getProperty(IMoleculeFragmenter.FRAGMENT_CATEGORY_PROPERTY_KEY));
    }
    //
    /**
     * Drives fragmentMolecule across all four FragmentationTypeOption values on a fused bicyclic molecule. Each type
     * must produce a non-empty fragment list; SCAFFOLD_ONLY yields exactly one scaffold fragment.
     *
     * @throws Exception if anything goes wrong
     */
    @Test
    public void fragmentationTypeMatrixTest() throws Exception {
        SmilesParser tmpSmiPar = new SmilesParser(SilentChemObjectBuilder.getInstance());
        IAtomContainer tmpOriginalMolecule = tmpSmiPar.parseSmiles(
                ScaffoldGeneratorFragmenterTest.FUSED_BICYCLIC_WITH_SIDE_CHAINS_SMILES);
        for (ScaffoldGeneratorFragmenter.FragmentationTypeOption tmpOption :
                ScaffoldGeneratorFragmenter.FragmentationTypeOption.values()) {
            ScaffoldGeneratorFragmenter tmpFragmenter = new ScaffoldGeneratorFragmenter();
            tmpFragmenter.setFragmentationTypeSetting(tmpOption);
            Assertions.assertEquals(tmpOption, tmpFragmenter.getFragmentationTypeSetting());
            List<IAtomContainer> tmpFragmentList = tmpFragmenter.fragmentMolecule(tmpOriginalMolecule);
            Assertions.assertFalse(tmpFragmentList.isEmpty(),
                    "FragmentationTypeOption " + tmpOption + " produced an empty list.");
            if (tmpOption.equals(ScaffoldGeneratorFragmenter.FragmentationTypeOption.SCAFFOLD_ONLY)) {
                Assertions.assertEquals(1, tmpFragmentList.size());
            }
        }
    }
    //
    /**
     * Drives fragmentMolecule across all three SideChainOption values on a fused bicyclic molecule with side chains.
     * Setting each option and re-fragmenting must not throw, exercising the early-return and side-chain-append branches.
     *
     * @throws Exception if anything goes wrong
     */
    @Test
    public void sideChainMatrixTest() throws Exception {
        SmilesParser tmpSmiPar = new SmilesParser(SilentChemObjectBuilder.getInstance());
        IAtomContainer tmpOriginalMolecule = tmpSmiPar.parseSmiles(
                ScaffoldGeneratorFragmenterTest.FUSED_BICYCLIC_WITH_SIDE_CHAINS_SMILES);
        for (ScaffoldGeneratorFragmenter.SideChainOption tmpOption :
                ScaffoldGeneratorFragmenter.SideChainOption.values()) {
            ScaffoldGeneratorFragmenter tmpFragmenter = new ScaffoldGeneratorFragmenter();
            tmpFragmenter.setSideChainSetting(tmpOption);
            Assertions.assertEquals(tmpOption, tmpFragmenter.getSideChainSetting());
            Assertions.assertDoesNotThrow(() -> tmpFragmenter.fragmentMolecule(tmpOriginalMolecule),
                    "SideChainOption " + tmpOption + " threw during fragmentation.");
        }
    }
    //
    /**
     * Exercises every settings accessor and setter: each getter/property accessor must not throw, each setter must be
     * reflected by the matching getter, at least two CycleFinderOption and two ElectronDonationModelOption values are
     * applied, a non-default ScaffoldMode and SmilesGenerator are set, both fragment-saturation values are applied, the
     * boolean settings are toggled, and the tooltip/display-name maps are non-empty. A final fragmentation exercises the
     * saturation and instance-setter branches.
     *
     * @throws Exception if anything goes wrong
     */
    @Test
    public void settingsTest() throws Exception {
        ScaffoldGeneratorFragmenter tmpFragmenter = new ScaffoldGeneratorFragmenter();
        //all getters / property accessors must not throw
        Assertions.assertDoesNotThrow(tmpFragmenter::getFragmentSaturationSetting);
        Assertions.assertDoesNotThrow(tmpFragmenter::fragmentSaturationSettingProperty);
        Assertions.assertDoesNotThrow(tmpFragmenter::getScaffoldModeSetting);
        Assertions.assertDoesNotThrow(tmpFragmenter::scaffoldModeSettingProperty);
        Assertions.assertDoesNotThrow(tmpFragmenter::getDetermineAromaticitySetting);
        Assertions.assertDoesNotThrow(tmpFragmenter::determineAromaticitySettingProperty);
        Assertions.assertDoesNotThrow(tmpFragmenter::getElectronDonationModelSetting);
        Assertions.assertDoesNotThrow(tmpFragmenter::electronDonationModelSettingProperty);
        Assertions.assertDoesNotThrow(tmpFragmenter::getCycleFinderSetting);
        Assertions.assertDoesNotThrow(tmpFragmenter::cycleFinderSettingProperty);
        Assertions.assertDoesNotThrow(tmpFragmenter::getSmilesGeneratorSetting);
        Assertions.assertDoesNotThrow(tmpFragmenter::smilesGeneratorSettingProperty);
        Assertions.assertDoesNotThrow(tmpFragmenter::getRuleSevenAppliedSetting);
        Assertions.assertDoesNotThrow(tmpFragmenter::ruleSevenAppliedSettingProperty);
        Assertions.assertDoesNotThrow(tmpFragmenter::getRetainOnlyHybridisationsAtAromaticBondsSetting);
        Assertions.assertDoesNotThrow(tmpFragmenter::retainOnlyHybridisationsAtAromaticBondsSetting);
        Assertions.assertDoesNotThrow(tmpFragmenter::getRetainOnlyHybridisationAtAromaticBondsSetting);
        Assertions.assertDoesNotThrow(tmpFragmenter::retainOnlyHybridisationAtAromaticBondsSettingProperty);
        Assertions.assertDoesNotThrow(tmpFragmenter::getFragmentationTypeSetting);
        Assertions.assertDoesNotThrow(tmpFragmenter::fragmentationTypeSettingProperty);
        Assertions.assertDoesNotThrow(tmpFragmenter::getSideChainSetting);
        Assertions.assertDoesNotThrow(tmpFragmenter::sideChainSettingProperty);
        //fragment saturation - both values
        tmpFragmenter.setFragmentSaturationSetting(IMoleculeFragmenter.FragmentSaturationOption.NO_SATURATION);
        Assertions.assertEquals(IMoleculeFragmenter.FragmentSaturationOption.NO_SATURATION,
                tmpFragmenter.getFragmentSaturationSetting());
        tmpFragmenter.setFragmentSaturationSetting(IMoleculeFragmenter.FragmentSaturationOption.HYDROGEN_SATURATION);
        Assertions.assertEquals(IMoleculeFragmenter.FragmentSaturationOption.HYDROGEN_SATURATION,
                tmpFragmenter.getFragmentSaturationSetting());
        //scaffold mode - non-default value
        tmpFragmenter.setScaffoldModeSetting(ScaffoldGeneratorFragmenter.SGFragmenterScaffoldModeOption.MURCKO_FRAMEWORK);
        Assertions.assertEquals(ScaffoldGeneratorFragmenter.SGFragmenterScaffoldModeOption.MURCKO_FRAMEWORK,
                tmpFragmenter.getScaffoldModeSetting());
        //determine aromaticity - toggle
        tmpFragmenter.setDetermineAromaticitySetting(false);
        Assertions.assertFalse(tmpFragmenter.getDetermineAromaticitySetting());
        tmpFragmenter.setDetermineAromaticitySetting(true);
        Assertions.assertTrue(tmpFragmenter.getDetermineAromaticitySetting());
        //electron donation model - at least two values, exercising the instance-setter switch
        tmpFragmenter.setElectronDonationModelSetting(IMoleculeFragmenter.ElectronDonationModelOption.DAYLIGHT);
        Assertions.assertEquals(IMoleculeFragmenter.ElectronDonationModelOption.DAYLIGHT,
                tmpFragmenter.getElectronDonationModelSetting());
        tmpFragmenter.setElectronDonationModelSetting(IMoleculeFragmenter.ElectronDonationModelOption.CDK);
        Assertions.assertEquals(IMoleculeFragmenter.ElectronDonationModelOption.CDK,
                tmpFragmenter.getElectronDonationModelSetting());
        //cycle finder - at least two values, exercising the instance-setter switch
        tmpFragmenter.setCycleFinderSetting(IMoleculeFragmenter.CycleFinderOption.ALL);
        Assertions.assertEquals(IMoleculeFragmenter.CycleFinderOption.ALL, tmpFragmenter.getCycleFinderSetting());
        tmpFragmenter.setCycleFinderSetting(IMoleculeFragmenter.CycleFinderOption.MCB);
        Assertions.assertEquals(IMoleculeFragmenter.CycleFinderOption.MCB, tmpFragmenter.getCycleFinderSetting());
        tmpFragmenter.setCycleFinderSetting(IMoleculeFragmenter.CycleFinderOption.CDK_AROMATIC_SET);
        Assertions.assertEquals(IMoleculeFragmenter.CycleFinderOption.CDK_AROMATIC_SET,
                tmpFragmenter.getCycleFinderSetting());
        //smiles generator - non-default value, exercising the instance-setter switch
        tmpFragmenter.setSmilesGeneratorSetting(ScaffoldGeneratorFragmenter.SmilesGeneratorOption.UNIQUE_WITH_STEREO);
        Assertions.assertEquals(ScaffoldGeneratorFragmenter.SmilesGeneratorOption.UNIQUE_WITH_STEREO,
                tmpFragmenter.getSmilesGeneratorSetting());
        tmpFragmenter.setSmilesGeneratorSetting(ScaffoldGeneratorFragmenter.SmilesGeneratorOption.UNIQUE_WITHOUT_STEREO);
        Assertions.assertEquals(ScaffoldGeneratorFragmenter.SmilesGeneratorOption.UNIQUE_WITHOUT_STEREO,
                tmpFragmenter.getSmilesGeneratorSetting());
        //rule seven - toggle
        tmpFragmenter.setRuleSevenAppliedSetting(false);
        Assertions.assertFalse(tmpFragmenter.getRuleSevenAppliedSetting());
        tmpFragmenter.setRuleSevenAppliedSetting(true);
        Assertions.assertTrue(tmpFragmenter.getRuleSevenAppliedSetting());
        //retain only hybridisation at aromatic bonds - toggle
        tmpFragmenter.setRetainOnlyHybridisationAtAromaticBondsSetting(true);
        Assertions.assertTrue(tmpFragmenter.getRetainOnlyHybridisationsAtAromaticBondsSetting());
        tmpFragmenter.setRetainOnlyHybridisationAtAromaticBondsSetting(false);
        Assertions.assertFalse(tmpFragmenter.getRetainOnlyHybridisationsAtAromaticBondsSetting());
        //fragmentation type and side chain setters reflected by getters
        tmpFragmenter.setFragmentationTypeSetting(ScaffoldGeneratorFragmenter.FragmentationTypeOption.ENUMERATIVE);
        Assertions.assertEquals(ScaffoldGeneratorFragmenter.FragmentationTypeOption.ENUMERATIVE,
                tmpFragmenter.getFragmentationTypeSetting());
        tmpFragmenter.setSideChainSetting(ScaffoldGeneratorFragmenter.SideChainOption.BOTH);
        Assertions.assertEquals(ScaffoldGeneratorFragmenter.SideChainOption.BOTH,
                tmpFragmenter.getSideChainSetting());
        //tooltip and display-name maps non-null and non-empty
        Map<String, String> tmpTooltipMap = tmpFragmenter.getSettingNameToTooltipTextMap();
        Map<String, String> tmpDisplayNameMap = tmpFragmenter.getSettingNameToDisplayNameMap();
        Assertions.assertNotNull(tmpTooltipMap);
        Assertions.assertFalse(tmpTooltipMap.isEmpty());
        Assertions.assertNotNull(tmpDisplayNameMap);
        Assertions.assertFalse(tmpDisplayNameMap.isEmpty());
        //re-fragment once after the setting changes to exercise the saturation / instance-setter branches
        SmilesParser tmpSmiPar = new SmilesParser(SilentChemObjectBuilder.getInstance());
        IAtomContainer tmpOriginalMolecule = tmpSmiPar.parseSmiles(
                ScaffoldGeneratorFragmenterTest.FUSED_BICYCLIC_WITH_SIDE_CHAINS_SMILES);
        Assertions.assertDoesNotThrow(() -> tmpFragmenter.fragmentMolecule(tmpOriginalMolecule));
    }
    //
    /**
     * Tests the filter/preprocess/fragment guard contracts: an empty AtomContainer is filtered, a null molecule throws
     * NullPointerException on shouldBePreprocessed, an empty (unfragmentable) container throws IllegalArgumentException
     * on fragmentMolecule, and applyPreprocessing on a valid molecule returns non-null.
     *
     * @throws Exception if anything goes wrong
     */
    @Test
    public void edgeGuardTest() throws Exception {
        ScaffoldGeneratorFragmenter tmpFragmenter = new ScaffoldGeneratorFragmenter();
        Assertions.assertTrue(tmpFragmenter.shouldBeFiltered(new AtomContainer()));
        Assertions.assertThrows(NullPointerException.class, () -> tmpFragmenter.shouldBePreprocessed(null));
        Assertions.assertThrows(IllegalArgumentException.class, () -> tmpFragmenter.fragmentMolecule(new AtomContainer()));
        SmilesParser tmpSmiPar = new SmilesParser(SilentChemObjectBuilder.getInstance());
        IAtomContainer tmpOriginalMolecule = tmpSmiPar.parseSmiles(
                ScaffoldGeneratorFragmenterTest.FUSED_BICYCLIC_WITH_SIDE_CHAINS_SMILES);
        Assertions.assertNotNull(tmpFragmenter.applyPreprocessing(tmpOriginalMolecule));
    }
    //
    /**
     * Tests copy() and restoreDefaultSettings(): the copy reflects a non-default FragmentationTypeSetting of the
     * original, and restoreDefaultSettings() resets the FragmentationTypeSetting to its default.
     *
     * @throws Exception if anything goes wrong
     */
    @Test
    public void copyAndRestoreTest() throws Exception {
        ScaffoldGeneratorFragmenter tmpFragmenter = new ScaffoldGeneratorFragmenter();
        tmpFragmenter.setFragmentationTypeSetting(ScaffoldGeneratorFragmenter.FragmentationTypeOption.RING_DISSECTION);
        ScaffoldGeneratorFragmenter tmpCopy = (ScaffoldGeneratorFragmenter) tmpFragmenter.copy();
        Assertions.assertEquals(tmpFragmenter.getFragmentationTypeSetting(), tmpCopy.getFragmentationTypeSetting());
        tmpFragmenter.restoreDefaultSettings();
        Assertions.assertEquals(ScaffoldGeneratorFragmenter.FRAGMENTATION_TYPE_OPTION_DEFAULT,
                tmpFragmenter.getFragmentationTypeSetting());
    }
    //</editor-fold>
}

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
import org.openscience.cdk.silent.SilentChemObjectBuilder;
import org.openscience.cdk.smiles.SmiFlavor;
import org.openscience.cdk.smiles.SmilesGenerator;
import org.openscience.cdk.smiles.SmilesParser;

import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Class to test the correct working of
 * {@link de.unijena.cheminf.mortar.model.fragmentation.algorithm.SugarRemovalUtilityFragmenter}.
 *
 * @author Jonas Schaub
 * @version 1.0.0.0
 */
public class SugarRemovalUtilityFragmenterTest {
    /**
     * Constructor that sets the default locale to british english, which is important for the correct functioning of the
     * fragmenter because the settings tooltips are imported from the message.properties file.
     */
    public SugarRemovalUtilityFragmenterTest() {
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
        SugarRemovalUtilityFragmenter tmpFragmenter = new SugarRemovalUtilityFragmenter();
        Assertions.assertDoesNotThrow(tmpFragmenter::getFragmentationAlgorithmName);
        Assertions.assertDoesNotThrow(tmpFragmenter::getFragmentationAlgorithmDisplayName);
        Assertions.assertDoesNotThrow(tmpFragmenter::getSugarTypeToRemoveSetting);
        for (Property<?> tmpSetting : tmpFragmenter.settingsProperties()) {
            Assertions.assertDoesNotThrow(tmpSetting::getName);
        }
    }
    //
    /**
     * Does a test fragmentation on the COCONUT natural product CNP0151033 and prints the results.
     *
     * @throws Exception if anything goes wrong
     */
    @Test
    public void fragmentationTest() throws Exception {
        SmilesParser tmpSmiPar = new SmilesParser(SilentChemObjectBuilder.getInstance());
        SmilesGenerator tmpSmiGen = new SmilesGenerator((SmiFlavor.Canonical));
        IAtomContainer tmpOriginalMolecule;
        List<IAtomContainer> tmpFragmentList;
        String tmpSmilesCode;
        SugarRemovalUtilityFragmenter tmpSRUFragmenter = new SugarRemovalUtilityFragmenter();
        tmpSRUFragmenter.setReturnedFragmentsSetting(SugarRemovalUtilityFragmenter.SRUFragmenterReturnedFragmentsOption.ALL_FRAGMENTS);
        tmpOriginalMolecule = tmpSmiPar.parseSmiles(
                //CNP0151033
                "O=C(OC1C(OCC2=COC(OC(=O)CC(C)C)C3C2CC(O)C3(O)COC(=O)C)OC(CO)C(O)C1O)C=CC4=CC=C(O)C=C4");
        Assertions.assertFalse(tmpSRUFragmenter.shouldBeFiltered(tmpOriginalMolecule));
        Assertions.assertFalse(tmpSRUFragmenter.shouldBePreprocessed(tmpOriginalMolecule));
        Assertions.assertTrue(tmpSRUFragmenter.canBeFragmented(tmpOriginalMolecule));
        tmpFragmentList = tmpSRUFragmenter.fragmentMolecule(tmpOriginalMolecule);
        tmpSmilesCode = tmpSmiGen.create(tmpFragmentList.getFirst());
        Assertions.assertNotNull(tmpFragmentList.getFirst().getProperty(IMoleculeFragmenter.FRAGMENT_CATEGORY_PROPERTY_KEY));
        //The sugar ring is not terminal and should not be removed, so the molecule remains unchanged
        Assertions.assertEquals("O=C(OC1C(OCC2=COC(OC(=O)CC(C)C)C3C2CC(O)C3(O)COC(=O)C)OC(CO)C(O)C1O)C=CC4=CC=C(O)C=C4", tmpSmilesCode);
        tmpSRUFragmenter.setRemoveOnlyTerminalSugarsSetting(false);
        tmpFragmentList = tmpSRUFragmenter.fragmentMolecule(tmpOriginalMolecule);
        tmpSmilesCode = tmpSmiGen.create(tmpFragmentList.getFirst());
        Assertions.assertNotNull(tmpFragmentList.getFirst().getProperty(IMoleculeFragmenter.FRAGMENT_CATEGORY_PROPERTY_KEY));
        //Now that all sugars are removed, the sugar ring is removed and an unconnected structure remains
        // the unconnected fragments are separated into different atom containers in the returned list
        Assertions.assertEquals("O=C(OCC1(O)C(O)CC2C(=COC(OC(=O)CC(C)C)C21)CO)C", tmpSmilesCode);
        Assertions.assertEquals("O=C(O)C=CC1=CC=C(O)C=C1", tmpSmiGen.create(tmpFragmentList.get(1)));
        Assertions.assertNotNull(tmpFragmentList.get(2).getProperty(IMoleculeFragmenter.FRAGMENT_CATEGORY_PROPERTY_KEY));
        tmpSRUFragmenter.setRemoveOnlyTerminalSugarsSetting(true);
        Assertions.assertFalse(tmpSRUFragmenter.shouldBeFiltered(tmpFragmentList.getFirst()));
        Assertions.assertFalse(tmpSRUFragmenter.shouldBePreprocessed(tmpFragmentList.getFirst()));
        Assertions.assertTrue(tmpSRUFragmenter.canBeFragmented(tmpFragmentList.getFirst()));
        IAtomContainer tmpAfterPreprocessing = tmpSRUFragmenter.applyPreprocessing(tmpFragmentList.getFirst());
        Assertions.assertTrue(tmpSRUFragmenter.canBeFragmented(tmpAfterPreprocessing));
    }
    //
    /**
     * Exercises every settings accessor and property getter, drives every value of the
     * {@link SugarRemovalUtilityFragmenter.SugarTypeToRemoveOption},
     * {@link SugarRemovalUtilityFragmenter.SRUFragmenterPreservationMode}, and
     * {@link SugarRemovalUtilityFragmenter.SRUFragmenterReturnedFragmentsOption} enums through their setters
     * (covering the property {@code set()} validation overrides), checks the tooltip and display-name maps, and verifies
     * that {@link SugarRemovalUtilityFragmenter#copy()} preserves settings and
     * {@link SugarRemovalUtilityFragmenter#restoreDefaultSettings()} resets them to the documented defaults.
     *
     * @throws Exception if anything goes wrong
     */
    @Test
    public void settingsTest() throws Exception {
        SugarRemovalUtilityFragmenter tmpFragmenter = new SugarRemovalUtilityFragmenter();
        //every get*Setting / *SettingProperty returns without throwing
        Assertions.assertDoesNotThrow(tmpFragmenter::getReturnedFragmentsSetting);
        Assertions.assertDoesNotThrow(tmpFragmenter::returnedFragmentsSettingProperty);
        Assertions.assertDoesNotThrow(tmpFragmenter::getSugarTypeToRemoveSetting);
        Assertions.assertDoesNotThrow(tmpFragmenter::sugarTypeToRemoveSettingProperty);
        Assertions.assertDoesNotThrow(tmpFragmenter::getDetectCircularSugarsOnlyWithGlycosidicBondSetting);
        Assertions.assertDoesNotThrow(tmpFragmenter::detectCircularSugarsOnlyWithGlycosidicBondSettingProperty);
        Assertions.assertDoesNotThrow(tmpFragmenter::getRemoveOnlyTerminalSugarsSetting);
        Assertions.assertDoesNotThrow(tmpFragmenter::removeOnlyTerminalSugarsSettingProperty);
        Assertions.assertDoesNotThrow(tmpFragmenter::getPreservationModeSetting);
        Assertions.assertDoesNotThrow(tmpFragmenter::preservationModeSettingProperty);
        Assertions.assertDoesNotThrow(tmpFragmenter::getPreservationModeThresholdSetting);
        Assertions.assertDoesNotThrow(tmpFragmenter::preservationModeThresholdSettingProperty);
        Assertions.assertDoesNotThrow(tmpFragmenter::getDetectCircularSugarsOnlyWithEnoughExocyclicOxygenAtomsSetting);
        Assertions.assertDoesNotThrow(tmpFragmenter::detectCircularSugarsOnlyWithEnoughExocyclicOxygenAtomsSettingProperty);
        Assertions.assertDoesNotThrow(tmpFragmenter::getExocyclicOxygenAtomsToAtomsInRingRatioThresholdSetting);
        Assertions.assertDoesNotThrow(tmpFragmenter::exocyclicOxygenAtomsToAtomsInRingRatioThresholdSettingProperty);
        Assertions.assertDoesNotThrow(tmpFragmenter::getDetectLinearSugarsInRingsSetting);
        Assertions.assertDoesNotThrow(tmpFragmenter::detectLinearSugarsInRingsSettingProperty);
        Assertions.assertDoesNotThrow(tmpFragmenter::getLinearSugarCandidateMinimumSizeSetting);
        Assertions.assertDoesNotThrow(tmpFragmenter::linearSugarCandidateMinimumSizeSettingProperty);
        Assertions.assertDoesNotThrow(tmpFragmenter::getLinearSugarCandidateMaximumSizeSetting);
        Assertions.assertDoesNotThrow(tmpFragmenter::linearSugarCandidateMaximumSizeSettingProperty);
        Assertions.assertDoesNotThrow(tmpFragmenter::getDetectLinearAcidicSugarsSetting);
        Assertions.assertDoesNotThrow(tmpFragmenter::detectLinearAcidicSugarsSettingProperty);
        Assertions.assertDoesNotThrow(tmpFragmenter::getDetectSpiroRingsAsCircularSugarsSetting);
        Assertions.assertDoesNotThrow(tmpFragmenter::detectSpiroRingsAsCircularSugarsSettingProperty);
        Assertions.assertDoesNotThrow(tmpFragmenter::getDetectCircularSugarsWithKetoGroupsSetting);
        Assertions.assertDoesNotThrow(tmpFragmenter::detectCircularSugarsWithKetoGroupsSettingProperty);
        Assertions.assertDoesNotThrow(tmpFragmenter::getMarkAttachPointsByRSetting);
        Assertions.assertDoesNotThrow(tmpFragmenter::markAttachPointsByRSettingProperty);
        Assertions.assertDoesNotThrow(tmpFragmenter::getPostProcessSugarsSetting);
        Assertions.assertDoesNotThrow(tmpFragmenter::postProcessSugarsSettingProperty);
        Assertions.assertDoesNotThrow(tmpFragmenter::getLimitPostprocessingBySizeSetting);
        Assertions.assertDoesNotThrow(tmpFragmenter::limitPostprocessingBySizeSettingProperty);
        Assertions.assertDoesNotThrow(tmpFragmenter::getDiscardTooSmallSugarModificationsSetting);
        Assertions.assertDoesNotThrow(tmpFragmenter::discardTooSmallSugarModificationsSettingProperty);
        //every boolean setting accepts a value and the getter reflects it
        tmpFragmenter.setDetectCircularSugarsOnlyWithGlycosidicBondSetting(true);
        Assertions.assertTrue(tmpFragmenter.getDetectCircularSugarsOnlyWithGlycosidicBondSetting());
        tmpFragmenter.setRemoveOnlyTerminalSugarsSetting(false);
        Assertions.assertFalse(tmpFragmenter.getRemoveOnlyTerminalSugarsSetting());
        tmpFragmenter.setDetectCircularSugarsOnlyWithEnoughExocyclicOxygenAtomsSetting(false);
        Assertions.assertFalse(tmpFragmenter.getDetectCircularSugarsOnlyWithEnoughExocyclicOxygenAtomsSetting());
        tmpFragmenter.setDetectLinearSugarsInRingsSetting(true);
        Assertions.assertTrue(tmpFragmenter.getDetectLinearSugarsInRingsSetting());
        tmpFragmenter.setDetectLinearAcidicSugarsSetting(true);
        Assertions.assertTrue(tmpFragmenter.getDetectLinearAcidicSugarsSetting());
        tmpFragmenter.setDetectSpiroRingsAsCircularSugarsSetting(true);
        Assertions.assertTrue(tmpFragmenter.getDetectSpiroRingsAsCircularSugarsSetting());
        tmpFragmenter.setDetectCircularSugarsWithKetoGroupsSetting(true);
        Assertions.assertTrue(tmpFragmenter.getDetectCircularSugarsWithKetoGroupsSetting());
        tmpFragmenter.setMarkAttachPointsByRSetting(true);
        Assertions.assertTrue(tmpFragmenter.getMarkAttachPointsByRSetting());
        tmpFragmenter.setPostProcessSugarsSetting(true);
        Assertions.assertTrue(tmpFragmenter.getPostProcessSugarsSetting());
        tmpFragmenter.setLimitPostprocessingBySizeSetting(true);
        Assertions.assertTrue(tmpFragmenter.getLimitPostprocessingBySizeSetting());
        tmpFragmenter.setDiscardTooSmallSugarModificationsSetting(true);
        Assertions.assertTrue(tmpFragmenter.getDiscardTooSmallSugarModificationsSetting());
        //every int / double setting accepts a valid value and the getter reflects it
        tmpFragmenter.setLinearSugarCandidateMinimumSizeSetting(3);
        Assertions.assertEquals(3, tmpFragmenter.getLinearSugarCandidateMinimumSizeSetting());
        tmpFragmenter.setLinearSugarCandidateMaximumSizeSetting(8);
        Assertions.assertEquals(8, tmpFragmenter.getLinearSugarCandidateMaximumSizeSetting());
        //preservation mode must be set to a mode that allows a threshold before setting the threshold
        tmpFragmenter.setPreservationModeSetting(SugarRemovalUtilityFragmenter.SRUFragmenterPreservationMode.HEAVY_ATOM_COUNT);
        tmpFragmenter.setPreservationModeThresholdSetting(4);
        Assertions.assertEquals(4, tmpFragmenter.getPreservationModeThresholdSetting());
        tmpFragmenter.setExocyclicOxygenAtomsToAtomsInRingRatioThresholdSetting(0.4);
        Assertions.assertEquals(0.4, tmpFragmenter.getExocyclicOxygenAtomsToAtomsInRingRatioThresholdSetting());
        //drive every SugarTypeToRemoveOption arm via the setter
        for (SugarRemovalUtilityFragmenter.SugarTypeToRemoveOption tmpOption
                : SugarRemovalUtilityFragmenter.SugarTypeToRemoveOption.values()) {
            tmpFragmenter.setSugarTypeToRemoveSetting(tmpOption);
            Assertions.assertEquals(tmpOption, tmpFragmenter.getSugarTypeToRemoveSetting());
        }
        //drive every SRUFragmenterPreservationMode arm via the setter
        for (SugarRemovalUtilityFragmenter.SRUFragmenterPreservationMode tmpMode
                : SugarRemovalUtilityFragmenter.SRUFragmenterPreservationMode.values()) {
            tmpFragmenter.setPreservationModeSetting(tmpMode);
            Assertions.assertEquals(tmpMode, tmpFragmenter.getPreservationModeSetting());
        }
        //drive every SRUFragmenterReturnedFragmentsOption arm via the setter
        for (SugarRemovalUtilityFragmenter.SRUFragmenterReturnedFragmentsOption tmpReturnedOption
                : SugarRemovalUtilityFragmenter.SRUFragmenterReturnedFragmentsOption.values()) {
            tmpFragmenter.setReturnedFragmentsSetting(tmpReturnedOption);
            Assertions.assertEquals(tmpReturnedOption, tmpFragmenter.getReturnedFragmentsSetting());
        }
        //tooltip and display-name maps non-null/non-empty with an entry per setting
        Map<String, String> tmpTooltipMap = tmpFragmenter.getSettingNameToTooltipTextMap();
        Map<String, String> tmpDisplayMap = tmpFragmenter.getSettingNameToDisplayNameMap();
        Assertions.assertNotNull(tmpTooltipMap);
        Assertions.assertNotNull(tmpDisplayMap);
        Assertions.assertFalse(tmpTooltipMap.isEmpty());
        Assertions.assertFalse(tmpDisplayMap.isEmpty());
        for (Property<?> tmpSetting : tmpFragmenter.settingsProperties()) {
            Assertions.assertTrue(tmpTooltipMap.containsKey(tmpSetting.getName()));
            Assertions.assertTrue(tmpDisplayMap.containsKey(tmpSetting.getName()));
        }
        //copy() preserves a representative enum setting and a representative boolean setting
        tmpFragmenter.setSugarTypeToRemoveSetting(SugarRemovalUtilityFragmenter.SugarTypeToRemoveOption.LINEAR);
        tmpFragmenter.setPreservationModeSetting(SugarRemovalUtilityFragmenter.SRUFragmenterPreservationMode.MOLECULAR_WEIGHT);
        tmpFragmenter.setDetectLinearAcidicSugarsSetting(true);
        IMoleculeFragmenter tmpCopyAsInterface = tmpFragmenter.copy();
        Assertions.assertInstanceOf(SugarRemovalUtilityFragmenter.class, tmpCopyAsInterface);
        SugarRemovalUtilityFragmenter tmpCopy = (SugarRemovalUtilityFragmenter) tmpCopyAsInterface;
        Assertions.assertEquals(SugarRemovalUtilityFragmenter.SugarTypeToRemoveOption.LINEAR, tmpCopy.getSugarTypeToRemoveSetting());
        Assertions.assertEquals(SugarRemovalUtilityFragmenter.SRUFragmenterPreservationMode.MOLECULAR_WEIGHT, tmpCopy.getPreservationModeSetting());
        Assertions.assertTrue(tmpCopy.getDetectLinearAcidicSugarsSetting());
        //restoreDefaultSettings() resets representative settings to their documented defaults
        tmpFragmenter.restoreDefaultSettings();
        Assertions.assertEquals(SugarRemovalUtilityFragmenter.SUGAR_TYPE_TO_REMOVE_OPTION_DEFAULT, tmpFragmenter.getSugarTypeToRemoveSetting());
        Assertions.assertEquals(SugarRemovalUtilityFragmenter.PRESERVATION_MODE_DEFAULT, tmpFragmenter.getPreservationModeSetting());
        Assertions.assertEquals(SugarRemovalUtilityFragmenter.RETURNED_FRAGMENTS_OPTION_DEFAULT, tmpFragmenter.getReturnedFragmentsSetting());
        Assertions.assertEquals(SugarRemovalUtilityFragmenter.MARK_ATTACH_POINTS_BY_R_DEFAULT, tmpFragmenter.getMarkAttachPointsByRSetting());
        Assertions.assertEquals(SugarRemovalUtilityFragmenter.POST_PROCESS_SUGARS_DEFAULT, tmpFragmenter.getPostProcessSugarsSetting());
    }
    //
    /**
     * Fragments the COCONUT natural product CNP0151033 with every {@link SugarRemovalUtilityFragmenter.SugarTypeToRemoveOption}
     * value (each combined with non-terminal sugar removal) and with both circular and linear preservation modes, asserting that
     * fragmentation does not throw and that the returned fragments carry the
     * {@link IMoleculeFragmenter#FRAGMENT_CATEGORY_PROPERTY_KEY} property. This exercises the remaining
     * {@code fragmentMolecule}/{@code partitionAndSortUnconnectedFragments} branches across the option matrix.
     *
     * @throws Exception if anything goes wrong
     */
    @Test
    public void optionVariantFragmentationTest() throws Exception {
        SmilesParser tmpSmiPar = new SmilesParser(SilentChemObjectBuilder.getInstance());
        //CNP0151033 (same molecule as the existing fragmentationTest)
        String tmpSmiles = "O=C(OC1C(OCC2=COC(OC(=O)CC(C)C)C3C2CC(O)C3(O)COC(=O)C)OC(CO)C(O)C1O)C=CC4=CC=C(O)C=C4";
        for (SugarRemovalUtilityFragmenter.SugarTypeToRemoveOption tmpSugarType
                : SugarRemovalUtilityFragmenter.SugarTypeToRemoveOption.values()) {
            for (SugarRemovalUtilityFragmenter.SRUFragmenterReturnedFragmentsOption tmpReturnedOption
                    : SugarRemovalUtilityFragmenter.SRUFragmenterReturnedFragmentsOption.values()) {
                SugarRemovalUtilityFragmenter tmpFragmenter = new SugarRemovalUtilityFragmenter();
                tmpFragmenter.setSugarTypeToRemoveSetting(tmpSugarType);
                tmpFragmenter.setReturnedFragmentsSetting(tmpReturnedOption);
                //remove non-terminal sugars too, so that the molecule is actually fragmented and the partition branch is hit
                tmpFragmenter.setRemoveOnlyTerminalSugarsSetting(false);
                IAtomContainer tmpMolecule = tmpSmiPar.parseSmiles(tmpSmiles);
                Assertions.assertTrue(tmpFragmenter.canBeFragmented(tmpMolecule));
                List<IAtomContainer> tmpFragmentList = tmpFragmenter.fragmentMolecule(tmpMolecule);
                Assertions.assertNotNull(tmpFragmentList);
                for (IAtomContainer tmpFragment : tmpFragmentList) {
                    Assertions.assertNotNull(tmpFragment.getProperty(IMoleculeFragmenter.FRAGMENT_CATEGORY_PROPERTY_KEY));
                }
            }
        }
        //additionally exercise post-processing with discarding of too small sugar modifications, circular + linear
        SugarRemovalUtilityFragmenter tmpPostProcessFragmenter = new SugarRemovalUtilityFragmenter();
        tmpPostProcessFragmenter.setSugarTypeToRemoveSetting(SugarRemovalUtilityFragmenter.SugarTypeToRemoveOption.CIRCULAR_AND_LINEAR);
        tmpPostProcessFragmenter.setReturnedFragmentsSetting(SugarRemovalUtilityFragmenter.SRUFragmenterReturnedFragmentsOption.ONLY_SUGAR_MOIETIES);
        tmpPostProcessFragmenter.setRemoveOnlyTerminalSugarsSetting(false);
        tmpPostProcessFragmenter.setPostProcessSugarsSetting(true);
        tmpPostProcessFragmenter.setDiscardTooSmallSugarModificationsSetting(true);
        IAtomContainer tmpMoleculeForPostProcess = tmpSmiPar.parseSmiles(tmpSmiles);
        Assertions.assertDoesNotThrow(() -> tmpPostProcessFragmenter.fragmentMolecule(tmpMoleculeForPostProcess));
    }
}

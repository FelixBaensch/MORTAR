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

import de.unijena.cheminf.mortar.model.util.TestUtil;

import javafx.beans.property.Property;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
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
    //<editor-fold desc="Locale setup and teardown" defaultstate="collapsed">
    /**
     * Default locale before this test class ran, restored after all tests.
     */
    private static Locale originalLocale;
    //
    /**
     * Sets the default locale to British English for this test class, remembering the original default locale, so
     * that the fragmenter settings tooltips and display names, which are resolved from the message bundle when a
     * fragmenter is instantiated, are deterministic.
     */
    @BeforeAll
    public static void setLocale() {
        SugarRemovalUtilityFragmenterTest.originalLocale = Locale.getDefault();
        Locale.setDefault(Locale.of("en", "GB"));
    }
    //
    /**
     * Restores the default locale that was in place before this test class ran.
     */
    @AfterAll
    public static void restoreLocale() {
        Locale.setDefault(SugarRemovalUtilityFragmenterTest.originalLocale);
    }
    //</editor-fold>
    //
    //
    /**
     * Tests instantiation and basic settings retrieval: the algorithm name and a non-blank display name are returned, a
     * fresh instance starts with the documented default sugar type to remove, and every settingsProperties() entry has
     * a tooltip and a display name registered under its name.
     *
     * @throws Exception if anything goes wrong
     */
    @Test
    public void basicTest() throws Exception {
        SugarRemovalUtilityFragmenter tmpFragmenter = new SugarRemovalUtilityFragmenter();
        Assertions.assertEquals(SugarRemovalUtilityFragmenter.ALGORITHM_NAME, tmpFragmenter.getFragmentationAlgorithmName());
        Assertions.assertFalse(tmpFragmenter.getFragmentationAlgorithmDisplayName().isBlank());
        Assertions.assertEquals(SugarRemovalUtilityFragmenter.SUGAR_TYPE_TO_REMOVE_OPTION_DEFAULT,
                tmpFragmenter.getSugarTypeToRemoveSetting());
        Assertions.assertFalse(tmpFragmenter.settingsProperties().isEmpty());
        for (Property<?> tmpSetting : tmpFragmenter.settingsProperties()) {
            Assertions.assertTrue(tmpFragmenter.getSettingNameToTooltipTextMap().containsKey(tmpSetting.getName()));
            Assertions.assertTrue(tmpFragmenter.getSettingNameToDisplayNameMap().containsKey(tmpSetting.getName()));
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
     * Checks that every property accessor returns a property exposed by settingsProperties() whose value mirrors the
     * matching getter, drives every value of the
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
        //every property accessor returns a property exposed by settingsProperties() that mirrors its getter
        List<Property<?>> tmpSettings = tmpFragmenter.settingsProperties();
        TestUtil.assertExposedSetting(tmpSettings,
                tmpFragmenter.returnedFragmentsSettingProperty(), tmpFragmenter.getReturnedFragmentsSetting());
        TestUtil.assertExposedSetting(tmpSettings,
                tmpFragmenter.sugarTypeToRemoveSettingProperty(), tmpFragmenter.getSugarTypeToRemoveSetting());
        TestUtil.assertExposedSetting(tmpSettings,
                tmpFragmenter.detectCircularSugarsOnlyWithGlycosidicBondSettingProperty(), tmpFragmenter.getDetectCircularSugarsOnlyWithGlycosidicBondSetting());
        TestUtil.assertExposedSetting(tmpSettings,
                tmpFragmenter.removeOnlyTerminalSugarsSettingProperty(), tmpFragmenter.getRemoveOnlyTerminalSugarsSetting());
        TestUtil.assertExposedSetting(tmpSettings,
                tmpFragmenter.preservationModeSettingProperty(), tmpFragmenter.getPreservationModeSetting());
        TestUtil.assertExposedSetting(tmpSettings,
                tmpFragmenter.preservationModeThresholdSettingProperty(), tmpFragmenter.getPreservationModeThresholdSetting());
        TestUtil.assertExposedSetting(tmpSettings,
                tmpFragmenter.detectCircularSugarsOnlyWithEnoughExocyclicOxygenAtomsSettingProperty(), tmpFragmenter.getDetectCircularSugarsOnlyWithEnoughExocyclicOxygenAtomsSetting());
        TestUtil.assertExposedSetting(tmpSettings,
                tmpFragmenter.exocyclicOxygenAtomsToAtomsInRingRatioThresholdSettingProperty(), tmpFragmenter.getExocyclicOxygenAtomsToAtomsInRingRatioThresholdSetting());
        TestUtil.assertExposedSetting(tmpSettings,
                tmpFragmenter.detectLinearSugarsInRingsSettingProperty(), tmpFragmenter.getDetectLinearSugarsInRingsSetting());
        TestUtil.assertExposedSetting(tmpSettings,
                tmpFragmenter.linearSugarCandidateMinimumSizeSettingProperty(), tmpFragmenter.getLinearSugarCandidateMinimumSizeSetting());
        TestUtil.assertExposedSetting(tmpSettings,
                tmpFragmenter.linearSugarCandidateMaximumSizeSettingProperty(), tmpFragmenter.getLinearSugarCandidateMaximumSizeSetting());
        TestUtil.assertExposedSetting(tmpSettings,
                tmpFragmenter.detectLinearAcidicSugarsSettingProperty(), tmpFragmenter.getDetectLinearAcidicSugarsSetting());
        TestUtil.assertExposedSetting(tmpSettings,
                tmpFragmenter.detectSpiroRingsAsCircularSugarsSettingProperty(), tmpFragmenter.getDetectSpiroRingsAsCircularSugarsSetting());
        TestUtil.assertExposedSetting(tmpSettings,
                tmpFragmenter.detectCircularSugarsWithKetoGroupsSettingProperty(), tmpFragmenter.getDetectCircularSugarsWithKetoGroupsSetting());
        TestUtil.assertExposedSetting(tmpSettings,
                tmpFragmenter.markAttachPointsByRSettingProperty(), tmpFragmenter.getMarkAttachPointsByRSetting());
        TestUtil.assertExposedSetting(tmpSettings,
                tmpFragmenter.postProcessSugarsSettingProperty(), tmpFragmenter.getPostProcessSugarsSetting());
        TestUtil.assertExposedSetting(tmpSettings,
                tmpFragmenter.limitPostprocessingBySizeSettingProperty(), tmpFragmenter.getLimitPostprocessingBySizeSetting());
        TestUtil.assertExposedSetting(tmpSettings,
                tmpFragmenter.discardTooSmallSugarModificationsSettingProperty(), tmpFragmenter.getDiscardTooSmallSugarModificationsSetting());
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
     * value combined with every {@link SugarRemovalUtilityFragmenter.SRUFragmenterReturnedFragmentsOption} value (each
     * with non-terminal sugar removal), asserting that the returned fragments carry the category the returned-fragments
     * option promises: only sugar moieties, only deglycosylated cores, or both. The molecule has one circular sugar and
     * no linear one, so every combination returns at least one fragment except the linear-sugar-only removal restricted
     * to sugar moieties, which must return none. This exercises the remaining
     * {@code fragmentMolecule}/{@code partitionAndSortUnconnectedFragments} branches across the option matrix. A final
     * run with sugar post-processing and discarding of too small sugar modifications must still return the sugar moiety.
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
                String tmpCombination = tmpSugarType + " / " + tmpReturnedOption;
                boolean tmpNoSugarCanBeReturned =
                        tmpSugarType == SugarRemovalUtilityFragmenter.SugarTypeToRemoveOption.LINEAR
                        && tmpReturnedOption == SugarRemovalUtilityFragmenter.SRUFragmenterReturnedFragmentsOption.ONLY_SUGAR_MOIETIES;
                Assertions.assertEquals(tmpNoSugarCanBeReturned, tmpFragmentList.isEmpty(), tmpCombination);
                for (IAtomContainer tmpFragment : tmpFragmentList) {
                    Object tmpCategory = tmpFragment.getProperty(IMoleculeFragmenter.FRAGMENT_CATEGORY_PROPERTY_KEY);
                    Assertions.assertNotNull(tmpCategory, tmpCombination);
                    if (tmpReturnedOption == SugarRemovalUtilityFragmenter.SRUFragmenterReturnedFragmentsOption.ONLY_SUGAR_MOIETIES) {
                        Assertions.assertEquals(SugarRemovalUtilityFragmenter.FRAGMENT_CATEGORY_SUGAR_MOIETY_VALUE, tmpCategory, tmpCombination);
                    } else if (tmpReturnedOption == SugarRemovalUtilityFragmenter.SRUFragmenterReturnedFragmentsOption.ONLY_AGLYCONE) {
                        Assertions.assertEquals(SugarRemovalUtilityFragmenter.FRAGMENT_CATEGORY_DEGLYCOSYLATED_CORE_VALUE, tmpCategory, tmpCombination);
                    }
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
        List<IAtomContainer> tmpPostProcessedFragments = tmpPostProcessFragmenter.fragmentMolecule(tmpMoleculeForPostProcess);
        Assertions.assertFalse(tmpPostProcessedFragments.isEmpty());
        for (IAtomContainer tmpFragment : tmpPostProcessedFragments) {
            Assertions.assertEquals(SugarRemovalUtilityFragmenter.FRAGMENT_CATEGORY_SUGAR_MOIETY_VALUE,
                    tmpFragment.getProperty(IMoleculeFragmenter.FRAGMENT_CATEGORY_PROPERTY_KEY));
        }
    }
}

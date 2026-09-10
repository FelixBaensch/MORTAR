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

package de.unijena.cheminf.mortar.controller;

import de.unijena.cheminf.mortar.configuration.Configuration;
import de.unijena.cheminf.mortar.gui.util.GuiDefinitions;
import de.unijena.cheminf.mortar.message.Message;
import de.unijena.cheminf.mortar.model.util.SimpleIDisplayEnumConstantProperty;

import javafx.beans.property.Property;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Locale;

/**
 * Direct, headless unit tests for {@link HistogramViewController}. The controller is constructed without a Stage
 * (its constructor only builds eight {@code Simple*Property} settings with anonymous {@code set()} overrides, touching
 * no scene graph), so no JavaFX toolkit boot is required and this class deliberately extends no FX harness.
 * <p>
 * The primary purpose is a characterization pin on the pure spacing/axis/enum/abbreviation logic that RFCT-01 made
 * reachable: the four already-pure methods widened from private to package-private
 * ({@code calculateBarSpacing}, {@code calculateXAxisUpperBoundWithSpaceForLabels},
 * {@code getBarWidthOptionEnumConstantFromDisplayName}, {@code getFrequencyOptionEnumConstantFromDisplayName}) and the
 * two blocks extracted from {@code createHistogram} ({@code calculateNiceAxisTickUnit},
 * {@code abbreviateSmilesForDisplay}). The magic-number boundaries (bar-spacing switch at 24/17/13, the
 * {@code tick*number > max} comparison) are pinned on both sides; expected bar-spacing values are computed from the
 * documented formula and the public constants so any future edit to the formula or a constant is caught. These pins
 * are green after the Task 1 widen/extract and must stay green through {@code ./gradlew build}, proving the refactor
 * behavior-preserving.
 * <p>
 * This plan intentionally does NOT chase {@literal >=}80% line coverage on the controller: the Stage/Scene/BarChart
 * /listener remainder is deferred to Phase 15 (COV-03). Real objects only, no mocks.
 *
 * @author Felix Baensch
 * @version 1.0.0.0
 */
public class HistogramViewControllerTest {
    //<editor-fold desc="Constructor" defaultstate="collapsed">
    /**
     * Constructor that sets the default locale to en-GB (so the setting names and messages resolved from the message
     * bundle during controller instantiation and by the abbreviation logic are deterministic) and bootstraps the
     * Configuration singleton from the classpath (the controller reads config; no data directory is touched by this).
     *
     * @throws Exception if the Configuration singleton cannot be initialized
     */
    public HistogramViewControllerTest() throws Exception {
        Locale.setDefault(Locale.of("en", "GB"));
        Configuration.getInstance();
    }
    //</editor-fold>
    //
    //<editor-fold desc="Characterization test methods for bar spacing" defaultstate="collapsed">
    /**
     * Characterization pin for {@link HistogramViewController#calculateBarSpacing(int, HistogramViewController.BarWidthOption)}
     * on the SMALL branch, pinning both sides of the documented magic-number boundary 24. The expected height factor
     * and category gap are computed from the documented formula and the public GUI constants.
     *
     * @throws Exception if anything goes wrong
     */
    @Test
    public void calculateBarSpacingSmallBoundaryCharacterizationTest() throws Exception {
        HistogramViewController tmpController = new HistogramViewController(Configuration.getInstance());
        //<= 24 branch: height factor stays 0.0, category gap follows the small-bar formula
        this.assertSmallBarSpacing(tmpController, 24);
        //> 24 branch: height factor is the small histogram-height constant
        this.assertSmallBarSpacing(tmpController, 25);
    }
    //
    /**
     * Characterization pin for {@link HistogramViewController#calculateBarSpacing(int, HistogramViewController.BarWidthOption)}
     * on the MEDIUM branch, pinning both sides of the documented magic-number boundary 17.
     *
     * @throws Exception if anything goes wrong
     */
    @Test
    public void calculateBarSpacingMediumBoundaryCharacterizationTest() throws Exception {
        HistogramViewController tmpController = new HistogramViewController(Configuration.getInstance());
        this.assertMediumBarSpacing(tmpController, 17);
        this.assertMediumBarSpacing(tmpController, 18);
    }
    //
    /**
     * Characterization pin for {@link HistogramViewController#calculateBarSpacing(int, HistogramViewController.BarWidthOption)}
     * on the LARGE branch, pinning both sides of the documented magic-number boundary 13.
     *
     * @throws Exception if anything goes wrong
     */
    @Test
    public void calculateBarSpacingLargeBoundaryCharacterizationTest() throws Exception {
        HistogramViewController tmpController = new HistogramViewController(Configuration.getInstance());
        this.assertLargeBarSpacing(tmpController, 13);
        this.assertLargeBarSpacing(tmpController, 14);
    }
    //
    /**
     * Asserts the SMALL-branch bar spacing for the given fragment number matches the documented formula.
     *
     * @param aController the controller under test
     * @param aNumberOfFragments the number of displayed fragments
     */
    private void assertSmallBarSpacing(HistogramViewController aController, int aNumberOfFragments) {
        double tmpExpectedHeightFactor;
        double tmpExpectedCategoryGap;
        if (aNumberOfFragments <= 24) {
            double tmpCurrentHistogramHeight = GuiDefinitions.GUI_NOT_SCROLLABLE_HEIGHT / aNumberOfFragments;
            double tmpGapDeviation = tmpCurrentHistogramHeight / (GuiDefinitions.GUI_NOT_SCROLLABLE_HEIGHT / 24.0);
            double tmpGapSpacing = HistogramViewController.GUI_HISTOGRAM_SMALL_BAR_GAP_CONST * tmpGapDeviation;
            double tmpFinalGapSpacing = tmpCurrentHistogramHeight - tmpGapSpacing;
            tmpExpectedHeightFactor = 0.0;
            tmpExpectedCategoryGap = tmpFinalGapSpacing - HistogramViewController.GUI_HISTOGRAM_SMALL_BAR_WIDTH;
        } else {
            tmpExpectedHeightFactor = HistogramViewController.GUI_HISTOGRAM_SMALL_HISTOGRAM_HEIGHT_VALUE;
            double tmpCurrentHistogramHeight = tmpExpectedHeightFactor * aNumberOfFragments - 85.0;
            double tmpGapSpacing = tmpCurrentHistogramHeight / aNumberOfFragments;
            tmpExpectedCategoryGap = tmpGapSpacing - HistogramViewController.GUI_HISTOGRAM_SMALL_BAR_WIDTH;
        }
        Double[] tmpResult = aController.calculateBarSpacing(aNumberOfFragments, HistogramViewController.BarWidthOption.SMALL);
        Assertions.assertEquals(tmpExpectedHeightFactor, tmpResult[0], 1e-9);
        Assertions.assertEquals(tmpExpectedCategoryGap, tmpResult[1], 1e-9);
    }
    //
    /**
     * Asserts the MEDIUM-branch bar spacing for the given fragment number matches the documented formula.
     *
     * @param aController the controller under test
     * @param aNumberOfFragments the number of displayed fragments
     */
    private void assertMediumBarSpacing(HistogramViewController aController, int aNumberOfFragments) {
        double tmpExpectedHeightFactor;
        double tmpExpectedCategoryGap;
        if (aNumberOfFragments <= 17) {
            double tmpCurrentHistogramHeight = GuiDefinitions.GUI_NOT_SCROLLABLE_HEIGHT / aNumberOfFragments;
            double tmpGapDeviation = tmpCurrentHistogramHeight / (GuiDefinitions.GUI_NOT_SCROLLABLE_HEIGHT / 17.0);
            double tmpGapSpacing = HistogramViewController.GUI_HISTOGRAM_MEDIUM_BAR_GAP_CONST * tmpGapDeviation;
            double tmpFinalGapSpacing = tmpCurrentHistogramHeight - tmpGapSpacing;
            tmpExpectedHeightFactor = 0.0;
            tmpExpectedCategoryGap = tmpFinalGapSpacing - HistogramViewController.GUI_HISTOGRAM_MEDIUM_BAR_WIDTH;
        } else {
            tmpExpectedHeightFactor = HistogramViewController.GUI_HISTOGRAM_MEDIUM_HISTOGRAM_HEIGHT_VALUE;
            double tmpCurrentHistogramHeight = tmpExpectedHeightFactor * aNumberOfFragments - 85.0;
            double tmpGapSpacing = tmpCurrentHistogramHeight / aNumberOfFragments;
            tmpExpectedCategoryGap = tmpGapSpacing - HistogramViewController.GUI_HISTOGRAM_MEDIUM_BAR_WIDTH;
        }
        Double[] tmpResult = aController.calculateBarSpacing(aNumberOfFragments, HistogramViewController.BarWidthOption.MEDIUM);
        Assertions.assertEquals(tmpExpectedHeightFactor, tmpResult[0], 1e-9);
        Assertions.assertEquals(tmpExpectedCategoryGap, tmpResult[1], 1e-9);
    }
    //
    /**
     * Asserts the LARGE-branch bar spacing for the given fragment number matches the documented formula.
     *
     * @param aController the controller under test
     * @param aNumberOfFragments the number of displayed fragments
     */
    private void assertLargeBarSpacing(HistogramViewController aController, int aNumberOfFragments) {
        double tmpExpectedHeightFactor;
        double tmpExpectedCategoryGap;
        if (aNumberOfFragments <= 13) {
            double tmpCurrentHistogramHeight = GuiDefinitions.GUI_NOT_SCROLLABLE_HEIGHT / aNumberOfFragments;
            double tmpGapDeviation = tmpCurrentHistogramHeight / (GuiDefinitions.GUI_NOT_SCROLLABLE_HEIGHT / 13.0);
            double tmpGapSpacing = HistogramViewController.GUI_HISTOGRAM_LARGE_BAR_GAP_CONST * tmpGapDeviation;
            double tmpFinalGapSpacing = tmpCurrentHistogramHeight - tmpGapSpacing;
            tmpExpectedHeightFactor = 0.0;
            tmpExpectedCategoryGap = tmpFinalGapSpacing - HistogramViewController.GUI_HISTOGRAM_LARGE_BAR_WIDTH;
        } else {
            tmpExpectedHeightFactor = HistogramViewController.GUI_HISTOGRAM_LARGE_HISTOGRAM_HEIGHT_VALUE;
            double tmpCurrentHistogramHeight = tmpExpectedHeightFactor * aNumberOfFragments - 85.0;
            double tmpGapSpacing = tmpCurrentHistogramHeight / aNumberOfFragments;
            tmpExpectedCategoryGap = tmpGapSpacing - HistogramViewController.GUI_HISTOGRAM_LARGE_BAR_WIDTH;
        }
        Double[] tmpResult = aController.calculateBarSpacing(aNumberOfFragments, HistogramViewController.BarWidthOption.LARGE);
        Assertions.assertEquals(tmpExpectedHeightFactor, tmpResult[0], 1e-9);
        Assertions.assertEquals(tmpExpectedCategoryGap, tmpResult[1], 1e-9);
    }
    //</editor-fold>
    //
    //<editor-fold desc="Characterization test methods for axis logic" defaultstate="collapsed">
    /**
     * Characterization pin for
     * {@link HistogramViewController#calculateXAxisUpperBoundWithSpaceForLabels(int, int)}. Pins both branches of the
     * {@code (aTickValue * tmpTickNumber) > aMaxValue} comparison with inputs that force each side.
     *
     * @throws Exception if anything goes wrong
     */
    @Test
    public void calculateXAxisUpperBoundWithSpaceForLabelsCharacterizationTest() throws Exception {
        HistogramViewController tmpController = new HistogramViewController(Configuration.getInstance());
        //max=95, tick=10: tickNumber=round(9.5)=10; 100 > 95 (true branch) -> 100 + 10 = 110
        Assertions.assertEquals(110, tmpController.calculateXAxisUpperBoundWithSpaceForLabels(95, 10));
        //max=100, tick=10: tickNumber=round(10.0)=10; 100 > 100 is false (else branch) -> 100 + 2*10 = 120
        Assertions.assertEquals(120, tmpController.calculateXAxisUpperBoundWithSpaceForLabels(100, 10));
    }
    //
    /**
     * Characterization pin for {@link HistogramViewController#calculateNiceAxisTickUnit(int)} (extracted from
     * createHistogram). Pins the {@literal <=}5-first-digit rounding path and the {@literal >}5-first-digit power-of-ten
     * path exactly as the extracted logic computes.
     *
     * @throws Exception if anything goes wrong
     */
    @Test
    public void calculateNiceAxisTickUnitCharacterizationTest() throws Exception {
        HistogramViewController tmpController = new HistogramViewController(Configuration.getInstance());
        //first digit <= 5 -> round up to the next multiple of the leading power of ten
        Assertions.assertEquals(400, tmpController.calculateNiceAxisTickUnit(356));
        //first digit <= 5 and already a round multiple -> unchanged
        Assertions.assertEquals(40, tmpController.calculateNiceAxisTickUnit(40));
        //first digit > 5 -> next full power of ten
        Assertions.assertEquals(10000, tmpController.calculateNiceAxisTickUnit(7896));
        Assertions.assertEquals(100, tmpController.calculateNiceAxisTickUnit(60));
    }
    //</editor-fold>
    //
    //<editor-fold desc="Characterization test methods for enum resolution" defaultstate="collapsed">
    /**
     * Characterization pin for {@link HistogramViewController#getBarWidthOptionEnumConstantFromDisplayName(String)}.
     * Null, empty, blank, and a bogus non-matching name all fall back to the default; a valid display name resolves to
     * its matching constant.
     *
     * @throws Exception if anything goes wrong
     */
    @Test
    public void getBarWidthOptionEnumConstantFromDisplayNameCharacterizationTest() throws Exception {
        HistogramViewController tmpController = new HistogramViewController(Configuration.getInstance());
        Assertions.assertEquals(HistogramViewController.DEFAULT_BAR_WIDTH,
                tmpController.getBarWidthOptionEnumConstantFromDisplayName(null));
        Assertions.assertEquals(HistogramViewController.DEFAULT_BAR_WIDTH,
                tmpController.getBarWidthOptionEnumConstantFromDisplayName(""));
        Assertions.assertEquals(HistogramViewController.DEFAULT_BAR_WIDTH,
                tmpController.getBarWidthOptionEnumConstantFromDisplayName("   "));
        Assertions.assertEquals(HistogramViewController.DEFAULT_BAR_WIDTH,
                tmpController.getBarWidthOptionEnumConstantFromDisplayName("not-a-real-display-name"));
        Assertions.assertEquals(HistogramViewController.BarWidthOption.SMALL,
                tmpController.getBarWidthOptionEnumConstantFromDisplayName(
                        HistogramViewController.BarWidthOption.SMALL.getDisplayName()));
    }
    //
    /**
     * Characterization pin for {@link HistogramViewController#getFrequencyOptionEnumConstantFromDisplayName(String)}.
     * Null, empty, blank, and a bogus non-matching name all fall back to the default; a valid display name resolves to
     * its matching constant.
     *
     * @throws Exception if anything goes wrong
     */
    @Test
    public void getFrequencyOptionEnumConstantFromDisplayNameCharacterizationTest() throws Exception {
        HistogramViewController tmpController = new HistogramViewController(Configuration.getInstance());
        Assertions.assertEquals(HistogramViewController.DEFAULT_DISPLAY_FREQUENCY,
                tmpController.getFrequencyOptionEnumConstantFromDisplayName(null));
        Assertions.assertEquals(HistogramViewController.DEFAULT_DISPLAY_FREQUENCY,
                tmpController.getFrequencyOptionEnumConstantFromDisplayName(""));
        Assertions.assertEquals(HistogramViewController.DEFAULT_DISPLAY_FREQUENCY,
                tmpController.getFrequencyOptionEnumConstantFromDisplayName("   "));
        Assertions.assertEquals(HistogramViewController.DEFAULT_DISPLAY_FREQUENCY,
                tmpController.getFrequencyOptionEnumConstantFromDisplayName("not-a-real-display-name"));
        Assertions.assertEquals(HistogramViewController.FrequencyOption.MOLECULE_FREQUENCY,
                tmpController.getFrequencyOptionEnumConstantFromDisplayName(
                        HistogramViewController.FrequencyOption.MOLECULE_FREQUENCY.getDisplayName()));
    }
    //</editor-fold>
    //
    //<editor-fold desc="Characterization test method for SMILES abbreviation" defaultstate="collapsed">
    /**
     * Characterization pin for
     * {@link HistogramViewController#abbreviateSmilesForDisplay(String, int, int)} (extracted from createHistogram).
     * Pins the too-long branch (localized placeholder with the reverse index) and the short branch (SMILES unchanged).
     *
     * @throws Exception if anything goes wrong
     */
    @Test
    public void abbreviateSmilesForDisplayCharacterizationTest() throws Exception {
        HistogramViewController tmpController = new HistogramViewController(Configuration.getInstance());
        //too-long branch: length 5 > max 3 -> localized placeholder + " (" + index + ")"
        String tmpExpectedTooLong = Message.get("HistogramView.smilesTooLong") + " (" + 12 + ")";
        Assertions.assertEquals(tmpExpectedTooLong, tmpController.abbreviateSmilesForDisplay("CCCCC", 3, 12));
        //short branch: length 3 <= max 5 -> unchanged
        Assertions.assertEquals("CCO", tmpController.abbreviateSmilesForDisplay("CCO", 5, 7));
        //boundary: length equal to max is NOT abbreviated (uses strictly greater-than)
        Assertions.assertEquals("CCC", tmpController.abbreviateSmilesForDisplay("CCC", 3, 4));
    }
    //</editor-fold>
    //
    //<editor-fold desc="Headless partial-coverage test methods" defaultstate="collapsed">
    /**
     * Exercises the headless-reachable IViewToolController members: {@code settingsProperties()} exposes the eight
     * histogram settings, {@code getViewToolNameForDisplay()} returns a non-blank name, {@code canBeUsedOnTab(...)}
     * is true for the fragments and itemization tabs and false for the molecules tab, and {@code restoreDefaultSettings()}
     * resets the bar-width and frequency settings to their documented defaults.
     *
     * @throws Exception if anything goes wrong
     */
    @Test
    public void headlessViewToolMembersTest() throws Exception {
        HistogramViewController tmpController = new HistogramViewController(Configuration.getInstance());
        List<Property<?>> tmpSettings = tmpController.settingsProperties();
        Assertions.assertNotNull(tmpSettings);
        Assertions.assertEquals(8, tmpSettings.size());
        Assertions.assertNotNull(tmpController.getViewToolNameForDisplay());
        Assertions.assertFalse(tmpController.getViewToolNameForDisplay().isBlank());
        Assertions.assertTrue(tmpController.canBeUsedOnTab(TabNames.FRAGMENTS));
        Assertions.assertTrue(tmpController.canBeUsedOnTab(TabNames.ITEMIZATION));
        Assertions.assertFalse(tmpController.canBeUsedOnTab(TabNames.MOLECULES));
        //mutate then restore: defaults are LARGE bar width / ABSOLUTE_FREQUENCY (settings list order: [1] bar width, [2] frequency)
        SimpleIDisplayEnumConstantProperty tmpBarWidthSetting = (SimpleIDisplayEnumConstantProperty) tmpSettings.get(1);
        SimpleIDisplayEnumConstantProperty tmpDisplayFrequencySetting = (SimpleIDisplayEnumConstantProperty) tmpSettings.get(2);
        tmpBarWidthSetting.set(HistogramViewController.BarWidthOption.SMALL);
        tmpDisplayFrequencySetting.set(HistogramViewController.FrequencyOption.MOLECULE_FREQUENCY);
        tmpController.restoreDefaultSettings();
        Assertions.assertEquals(HistogramViewController.DEFAULT_BAR_WIDTH, tmpBarWidthSetting.get());
        Assertions.assertEquals(HistogramViewController.DEFAULT_DISPLAY_FREQUENCY, tmpDisplayFrequencySetting.get());
    }
    //
    /**
     * Exercises the headless-reachable enum members: every BarWidthOption and FrequencyOption constant returns a
     * non-blank display name and tooltip text (resolved from the en-GB message bundle).
     *
     * @throws Exception if anything goes wrong
     */
    @Test
    public void enumDisplayNameAndTooltipTest() throws Exception {
        for (HistogramViewController.BarWidthOption tmpOption : HistogramViewController.BarWidthOption.values()) {
            Assertions.assertNotNull(tmpOption.getDisplayName());
            Assertions.assertFalse(tmpOption.getDisplayName().isBlank());
            Assertions.assertNotNull(tmpOption.getTooltipText());
            Assertions.assertFalse(tmpOption.getTooltipText().isBlank());
        }
        for (HistogramViewController.FrequencyOption tmpOption : HistogramViewController.FrequencyOption.values()) {
            Assertions.assertNotNull(tmpOption.getDisplayName());
            Assertions.assertFalse(tmpOption.getDisplayName().isBlank());
            Assertions.assertNotNull(tmpOption.getTooltipText());
            Assertions.assertFalse(tmpOption.getTooltipText().isBlank());
        }
    }
    //</editor-fold>
}

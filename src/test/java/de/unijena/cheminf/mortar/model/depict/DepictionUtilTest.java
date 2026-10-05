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

package de.unijena.cheminf.mortar.model.depict;

import de.unijena.cheminf.mortar.model.util.BasicDefinitions;

import javafx.scene.image.Image;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.silent.SilentChemObjectBuilder;
import org.openscience.cdk.smiles.SmilesParser;

import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

/**
 * Tests for {@link DepictionUtil}. The image-producing methods convert an AWT {@link java.awt.image.BufferedImage} to a
 * JavaFX {@link Image} via {@code SwingFXUtils}; that conversion works without a started JavaFX toolkit (the class
 * passes when run on its own, as do the data-model tests that depict structures), so nothing here boots the toolkit and
 * the tests do not depend on another test class having booted it.
 *
 * @author Jonas Schaub
 * @version 1.0.0.0
 */
class DepictionUtilTest {
    //<editor-fold desc="Locale setup and teardown" defaultstate="collapsed">
    /**
     * Default locale before this test class ran, restored after all tests.
     */
    private static Locale originalLocale;
    //
    /**
     * Pins the default locale to en-GB, the locale the application itself runs under, remembering the original default
     * locale. The integer-formatting methods of {@link DepictionUtil} build their {@link java.text.DecimalFormatSymbols}
     * from {@code Locale.getDefault()}, so the decimal separator of their output, and therefore any exact assertion on
     * it, depends on the locale of the JVM running the tests.
     */
    @BeforeAll
    static void setLocale() {
        DepictionUtilTest.originalLocale = Locale.getDefault();
        Locale.setDefault(Locale.of("en", "GB"));
    }
    //
    /**
     * Restores the default locale that was in place before this test class ran.
     */
    @AfterAll
    static void restoreLocale() {
        Locale.setDefault(DepictionUtilTest.originalLocale);
    }
    //</editor-fold>
    //
    //<editor-fold desc="Tests">
    /**
     * Drives every image-producing overload of {@link DepictionUtil} with a real molecule and asserts that each returns a
     * JavaFX {@link Image} of the requested size: the explicitly given width and height, or the
     * {@link BasicDefinitions} default for the dimension an overload fixes. This covers the depiction (BufferedImage to
     * FX Image) path of each overload and shows that every overload passes its dimensions on correctly.
     *
     * @throws Exception if anything goes wrong
     */
    @Test
    void depictImageOverloadsProduceNonNullImages() throws Exception {
        SmilesParser tmpSmiPar = new SmilesParser(SilentChemObjectBuilder.getInstance());
        IAtomContainer tmpMolecule = tmpSmiPar.parseSmiles("c1ccccc1");
        double tmpDefaultWidth = BasicDefinitions.DEFAULT_IMAGE_WIDTH_DEFAULT;
        double tmpDefaultHeight = BasicDefinitions.DEFAULT_IMAGE_HEIGHT_DEFAULT;
        DepictionUtilTest.assertImageSize(300.0, 200.0,
                DepictionUtil.depictImageWithNoZoomNoFillToFitAndTransparentBackground(tmpMolecule, 300.0, 200.0));
        DepictionUtilTest.assertImageSize(tmpDefaultWidth, 200.0,
                DepictionUtil.depictImageWithDefaultWidthNoZoomNoFillToFitAndTransparentBackground(tmpMolecule, 200.0));
        DepictionUtilTest.assertImageSize(300.0, tmpDefaultHeight,
                DepictionUtil.depictImageWithDefaultHeightNoZoomNoFillToFitAndTransparentBackground(tmpMolecule, 300.0));
        DepictionUtilTest.assertImageSize(tmpDefaultWidth, tmpDefaultHeight,
                DepictionUtil.depictImageWithDefaultWidthDefaultHeightNoFillToFitAndTransparentBackground(tmpMolecule, 1.5));
        DepictionUtilTest.assertImageSize(310.0, 210.0,
                DepictionUtil.depictImageWithNoFillToFitAndTransparentBackground(tmpMolecule, 1.5, 310.0, 210.0));
        DepictionUtilTest.assertImageSize(320.0, 220.0,
                DepictionUtil.depictImageWithTransparentBackground(tmpMolecule, 1.5, 320.0, 220.0, true));
        DepictionUtilTest.assertImageSize(330.0, 230.0,
                DepictionUtil.depictImage(tmpMolecule, 1.5, 330.0, 230.0, true, false));
    }
    //
    /**
     * Drives the two text-annotated image overloads of {@link DepictionUtil}, asserting that each returns a JavaFX
     * {@link Image} of the requested total size (structure plus text label).
     *
     * @throws Exception if anything goes wrong
     */
    @Test
    void depictImageWithTextOverloadsProduceNonNullImages() throws Exception {
        SmilesParser tmpSmiPar = new SmilesParser(SilentChemObjectBuilder.getInstance());
        IAtomContainer tmpMolecule = tmpSmiPar.parseSmiles("c1ccccc1");
        DepictionUtilTest.assertImageSize(300.0, 200.0,
                DepictionUtil.depictImageWithTextNoFillToFitAndTransparentBackground(tmpMolecule, 1.5, 300.0, 200.0, "Benzene"));
        DepictionUtilTest.assertImageSize(310.0, 210.0,
                DepictionUtil.depictImageWithText(tmpMolecule, 1.5, 310.0, 210.0, "Benzene", true, false));
    }
    //
    /**
     * Drives {@link DepictionUtil#depictErrorImage(String, int, int)}: a normal message with valid dimensions plus the
     * fallback branches for a blank message and for non-positive dimensions all return a non-null JavaFX {@link Image}.
     */
    @Test
    void depictErrorImageProducesImageAndCoversFallbacks() {
        Image tmpErrorImage = DepictionUtil.depictErrorImage("boom", 120, 80);
        Assertions.assertNotNull(tmpErrorImage);
        //blank message -> "Error" fallback; non-positive dimensions -> default size fallback
        Assertions.assertNotNull(DepictionUtil.depictErrorImage("   ", -1, -1));
        Assertions.assertNotNull(DepictionUtil.depictErrorImage(null, 0, 0));
    }
    //
    /**
     * Drives the guard branch of {@link DepictionUtil#getGraphicsInstanceWithStandardFont(int, int)} (non-positive
     * dimensions throw) and its happy path (a configured {@link Graphics2D} is returned), plus the early-fit return
     * branch of {@link DepictionUtil#fitIntegerDisplayToImageWidth(double, int, FontMetrics)} where a very wide image
     * keeps the first, most detailed formatting. Because that first pattern is
     * {@link DepictionUtil.IntegerFormatPattern#THREE_DECIMALS_SCIENTIFIC} ({@code "0.000E0"}), the exact output for
     * the value 42 is asserted rather than only its non-nullness — that is what shows the loop returned on the first,
     * most detailed pattern instead of falling through to a shorter one. The class pins the en-GB default locale, so
     * the decimal separator in the expected string is deterministic.
     */
    @Test
    void graphicsInstanceGuardAndEarlyFitReturn() {
        Assertions.assertThrows(IllegalArgumentException.class,
                () -> DepictionUtil.getGraphicsInstanceWithStandardFont(0, 10));
        Graphics2D tmpGraphics = DepictionUtil.getGraphicsInstanceWithStandardFont(100, 50);
        Assertions.assertNotNull(tmpGraphics);
        FontMetrics tmpFontMetrics = tmpGraphics.getFontMetrics();
        tmpGraphics.dispose();
        //a very wide image -> the first (most detailed) formatting already fits, so the loop returns immediately
        String tmpResult = DepictionUtil.fitIntegerDisplayToImageWidth(100000.0, 42, tmpFontMetrics);
        Assertions.assertEquals("4.200E1", tmpResult);
    }
    //
    /**
     * Illustrates the effect of each format pattern defined in {@link DepictionUtil.IntegerFormatPattern}
     * enum on selected large integer values. The patterns are applied progressively (most detail → the least detail)
     * by {@link DepictionUtil#fitIntegerDisplayToImageWidth(double, int)} until the resulting text fits the given
     * image width.
     *
     * <p>The patterns and their intended output for the example value {@code 1,234,567,890}:</p>
     * <pre>
     *   "0.000E0"  →  "1.235E9"   (three decimal places)
     *   "0.00E0"   →  "1.23E9"    (two decimal places)
     *   "0.0E0"    →  "1.2E9"     (one decimal place)
     *   "0E0"      →  "1E9"       (no decimal places, shortest)
     * </pre>
     *
     * <p>Note that rounding can cause the leading digit to increment.  For instance,
     * {@code 9,876,543} formatted with {@code "0E0"} rounds up to {@code "1E7"} rather than {@code "9E6"}.</p>
     */
    @Test
    void testFormatPatternsIntIllustration() {
        DecimalFormatSymbols tmpUsSymbols = DecimalFormatSymbols.getInstance(Locale.US);

        // --- Example 1: 1,234,567,890 ---
        int tmpValue1 = 1_234_567_890;
        String[] tmpExpected1 = {
            "1.235E9",  // 1.2345... rounded at the 4th decimal (digit = 5) → rounds up
            "1.23E9",   // 1.234...  rounded at the 3rd decimal (digit = 4) → rounds down
            "1.2E9",    // 1.23...   rounded at the 2nd decimal (digit = 3) → rounds down
            "1E9"       // 1.2...    rounded at the 1st decimal (digit = 2) → rounds down
        };
        for (int i = 0; i < DepictionUtil.IntegerFormatPattern.values().length; i++) {
            DecimalFormat tmpFmt = new DecimalFormat(DepictionUtil.IntegerFormatPattern.values()[i].getPattern(), tmpUsSymbols);
            String tmpResult = tmpFmt.format(tmpValue1);
            Assertions.assertEquals(
                    tmpExpected1[i], tmpResult,
                    "Pattern \"" + DepictionUtil.IntegerFormatPattern.values()[i].getPattern() + "\" applied to " + tmpValue1);
        }

        // --- Example 2: 9,876,543 – demonstrates carry-propagation on rounding ---
        int tmpValue2 = 9_876_543;
        String[] tmpExpected2 = {
            "9.877E6",  // 9.876543 rounded at the 4th decimal (digit = 5) → rounds up
            "9.88E6",   // 9.8765   rounded at the 3rd decimal (digit = 6) → rounds up
            "9.9E6",    // 9.876    rounded at the 2nd decimal (digit = 7) → rounds up
            "1E7"       // 9.9      rounded at the 1st decimal (digit = 9) → carry: 10 → 1×10^7
        };
        for (int i = 0; i < DepictionUtil.IntegerFormatPattern.values().length; i++) {
            DecimalFormat tmpFmt = new DecimalFormat(DepictionUtil.IntegerFormatPattern.values()[i].getPattern(), tmpUsSymbols);
            String tmpResult = tmpFmt.format(tmpValue2);
            Assertions.assertEquals(
                    tmpExpected2[i], tmpResult,
                    "Pattern \"" + DepictionUtil.IntegerFormatPattern.values()[i].getPattern() + "\" applied to " + tmpValue2);
        }

        // --- Example 3: Integer.MAX_VALUE (2,147,483,647) ---
        int tmpValue3 = Integer.MAX_VALUE;
        String[] tmpExpected3 = {
            "2.147E9",  // 2.147483... rounded at the 4th decimal (digit = 4) → rounds down
            "2.15E9",   // 2.1474...   rounded at the 3rd decimal (digit = 7) → rounds up
            "2.1E9",    // 2.147...    rounded at the 2nd decimal (digit = 4) → rounds down
            "2E9"       // 2.1...      rounded at the 1st decimal (digit = 1) → rounds down
        };
        for (int i = 0; i < DepictionUtil.IntegerFormatPattern.values().length; i++) {
            DecimalFormat tmpFmt = new DecimalFormat(DepictionUtil.IntegerFormatPattern.values()[i].getPattern(), tmpUsSymbols);
            String tmpResult = tmpFmt.format(tmpValue3);
            Assertions.assertEquals(
                    tmpExpected3[i], tmpResult,
                    "Pattern \"" + DepictionUtil.IntegerFormatPattern.values()[i].getPattern() + "\" applied to " + tmpValue3);
        }
    }
    //
    /**
     * Tests that {@link DepictionUtil#fitIntegerDisplayToImageWidth(double, int)} produces a compressed
     * (shorter) representation when the image is too narrow to display the plain integer string.
     * A width of 2 pixels is far too narrow for any text, so the method must fall back to the shortest
     * available scientific-notation format.
     */
    @Test
    void testFitIntegerDisplayToImageWidthCompressesForNarrowImage() {
        // 2 pixels is too narrow for any multi-character text → compression must kick in
        double tmpVeryNarrowImage = 2.0;
        int[] tmpLargeValues = {1_234_567_890, 9_876_543, Integer.MAX_VALUE, 100_000};
        for (int tmpValue : tmpLargeValues) {
            String tmpPlain = String.valueOf(tmpValue);
            String tmpCompressed = DepictionUtil.fitIntegerDisplayToImageWidth(tmpVeryNarrowImage, tmpValue);
            Assertions.assertNotEquals(
                    tmpPlain, tmpCompressed,
                    "Value " + tmpValue + " should be reformatted for a 2-pixel-wide image");
        }
    }
    //
    /**
     * Tests that {@link DepictionUtil#isTextNarrowerThanImage(double, String, FontMetrics)}
     * produces the same result as the no-{@link FontMetrics} overload for a representative set of
     * image widths and texts.  A shared {@link FontMetrics} is built once from a short-lived
     * {@link Graphics2D} (exactly as production code does in
     * {@link de.unijena.cheminf.mortar.gui.views.ItemizationDataTableView}) and reused across all
     * assertions.
     */
    @Test
    void testIsTextNarrowerThanImageWithCachedFontMetrics() {
        Graphics2D tmpGraphics2D = DepictionUtil.getGraphicsInstanceWithStandardFont(1, 1);
        FontMetrics tmpFontMetrics = tmpGraphics2D.getFontMetrics();
        tmpGraphics2D.dispose();

        double[] tmpWidths = {1.0, 50.0, 200.0, 1000.0};
        String[] tmpTexts = {"1", "42", "1234", "999999"};
        for (double tmpWidth : tmpWidths) {
            for (String tmpText : tmpTexts) {
                boolean tmpExpected = DepictionUtil.isTextNarrowerThanImage(tmpWidth, tmpText);
                boolean tmpActual = DepictionUtil.isTextNarrowerThanImage(tmpWidth, tmpText, tmpFontMetrics);
                Assertions.assertEquals(tmpExpected, tmpActual,
                        "isTextNarrowerThanImage mismatch for width=" + tmpWidth + ", text=\"" + tmpText + "\"");
            }
        }
    }
    //
    /**
     * Tests that {@link DepictionUtil#fitIntegerDisplayToImageWidth(double, int, FontMetrics)}
     * produces the same result as the no-{@link FontMetrics} overload for a representative set of
     * image widths and integer values.  A shared {@link FontMetrics} is built once from a short-lived
     * {@link Graphics2D} and reused across all assertions.
     */
    @Test
    void testFitIntegerDisplayToImageWidthWithCachedFontMetrics() {
        Graphics2D tmpGraphics2D = DepictionUtil.getGraphicsInstanceWithStandardFont(1, 1);
        FontMetrics tmpFontMetrics = tmpGraphics2D.getFontMetrics();
        tmpGraphics2D.dispose();

        double tmpVeryNarrowImage = 2.0;
        int[] tmpValues = {1_234_567_890, 9_876_543, Integer.MAX_VALUE, 100_000};
        for (int tmpValue : tmpValues) {
            String tmpExpected = DepictionUtil.fitIntegerDisplayToImageWidth(tmpVeryNarrowImage, tmpValue);
            String tmpActual = DepictionUtil.fitIntegerDisplayToImageWidth(tmpVeryNarrowImage, tmpValue, tmpFontMetrics);
            Assertions.assertEquals(tmpExpected, tmpActual,
                    "fitIntegerDisplayToImageWidth mismatch for width=" + tmpVeryNarrowImage
                            + ", value=" + tmpValue);
        }
    }
    //</editor-fold>
    //
    //<editor-fold desc="Private static methods" defaultstate="collapsed">
    /**
     * Asserts that the given image is non-null and has the expected width and height.
     *
     * @param anExpectedWidth expected image width
     * @param anExpectedHeight expected image height
     * @param anImage the image to check
     */
    private static void assertImageSize(double anExpectedWidth, double anExpectedHeight, Image anImage) {
        Assertions.assertNotNull(anImage);
        Assertions.assertEquals(anExpectedWidth, anImage.getWidth(), "image width");
        Assertions.assertEquals(anExpectedHeight, anImage.getHeight(), "image height");
    }
    //</editor-fold>
}

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

package de.unijena.cheminf.mortar.preference;

import de.unijena.cheminf.mortar.configuration.Configuration;
import de.unijena.cheminf.mortar.model.util.FileUtil;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.awt.Color;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.PrintWriter;
import java.io.StringReader;
import java.lang.reflect.Constructor;
import java.util.Locale;

/**
 * Test class for preferences.
 *
 * @author Jonas Schaub
 * @version 1.0.0.0
 */
public class PreferenceTest {
    /**
     * Constructor to initialize locale and configuration.
     */
    public PreferenceTest() throws Exception {
        Locale.setDefault(Locale.of("en", "GB"));
        Configuration.getInstance();
    }
    //
    /**
     * Tests basic functionalities of class BooleanPreference.
     *
     * @throws Exception if anything goes wrong
     */
    @Test
    public void testBooleanPreference() throws Exception {
        BooleanPreference tmpPreference = new BooleanPreference("MORTAR is cool", false);
        tmpPreference.setContent(true);
        Assertions.assertTrue(tmpPreference.getContent());
        this.testPreferenceBasics(tmpPreference);
    }
    //
    /**
     * Tests basic functionalities of class RGBColorPreference.
     *
     * @throws Exception if anything goes wrong
     */
    @Test
    public void testColorPreference() throws Exception {
        RGBColorPreference tmpPreference = new RGBColorPreference("Color of my soul",
                Color.PINK.getRed()/255.0,
                Color.PINK.getGreen()/255.0,
                Color.PINK.getBlue()/255.0,
                Color.PINK.getAlpha()/255.0);
        this.testPreferenceBasics(tmpPreference);
        RGBColorPreference tmpPreference2 = new RGBColorPreference("Color of my car",
                Color.PINK.getRed(),
                Color.PINK.getGreen(),
                Color.PINK.getBlue(),
                Color.PINK.getAlpha());
        this.testPreferenceBasics(tmpPreference2);
        Assertions.assertArrayEquals(tmpPreference.getComponents(), tmpPreference2.getComponents(), 0);
    }
    //
    /**
     * Tests basic functionalities of class SingleIntegerPreference.
     *
     * @throws Exception if anything goes wrong
     */
    @Test
    public void testSingleIntegerPreference() throws Exception {
        SingleIntegerPreference tmpPreference = new SingleIntegerPreference("Number of simultaneous MORTAR instances", 1);
        this.testPreferenceBasics(tmpPreference);
    }
    //
    /**
     * Tests basic functionalities of class SingleNumberPreference.
     *
     * @throws Exception if anything goes wrong
     */
    @Test
    public void testSingleNumberPreference() throws Exception {
        SingleNumberPreference tmpPreference = new SingleNumberPreference("Layout parameter xy", 2.0);
        this.testPreferenceBasics(tmpPreference);
    }
    //
    /**
     * Tests basic functionalities of class SingleTermPreference.
     *
     * @throws Exception if anything goes wrong
     */
    @Test
    public void testSingleTermPreference() throws Exception {
        SingleTermPreference tmpPreference = new SingleTermPreference("Welcoming message", "Welcome to MORTAR");
        this.testPreferenceBasics(tmpPreference);
    }
    //
    /**
     * Tests the String-arg constructor, setContent overloads, static isValidContent, copy(), and the invalid-name /
     * invalid-content guards of class SingleIntegerPreference with real valid and invalid input.
     *
     * @throws Exception if anything goes wrong
     */
    @Test
    public void testSingleIntegerPreferenceValidationAndCopy() throws Exception {
        SingleIntegerPreference tmpPref = new SingleIntegerPreference("Count", "42");
        Assertions.assertEquals(42, tmpPref.getContent());
        tmpPref.setContent(7);
        Assertions.assertEquals(7, tmpPref.getContent());
        tmpPref.setContent("13");
        Assertions.assertEquals(13, tmpPref.getContent());
        Assertions.assertTrue(SingleIntegerPreference.isValidContent("13"));
        Assertions.assertFalse(SingleIntegerPreference.isValidContent("not-a-number"));
        Assertions.assertFalse(SingleIntegerPreference.isValidContent(""));
        Assertions.assertThrows(IllegalArgumentException.class, () -> new SingleIntegerPreference("Count", "xyz"));
        Assertions.assertThrows(IllegalArgumentException.class, () -> new SingleIntegerPreference("lowercase-invalid-name", 1));
        Assertions.assertThrows(IllegalArgumentException.class, () -> tmpPref.setContent("oops"));
        SingleIntegerPreference tmpCopy = tmpPref.copy();
        Assertions.assertEquals(tmpPref, tmpCopy);
        Assertions.assertEquals(tmpPref.getGUID(), tmpCopy.getGUID());
        Assertions.assertNotSame(tmpPref, tmpCopy);
    }
    //
    /**
     * Tests the String-arg constructor, setContent overloads, static isValidContent overloads, copy(), and the
     * invalid-name / NaN / Infinity guards of class SingleNumberPreference using clean doubles.
     *
     * @throws Exception if anything goes wrong
     */
    @Test
    public void testSingleNumberPreferenceValidationAndCopy() throws Exception {
        SingleNumberPreference tmpPref = new SingleNumberPreference("Layout parameter", "0.5");
        Assertions.assertEquals(0.5, tmpPref.getContent(), 0.0);
        Assertions.assertThrows(IllegalArgumentException.class, () -> new SingleNumberPreference("Layout parameter", "NaN"));
        Assertions.assertThrows(IllegalArgumentException.class, () -> new SingleNumberPreference("lowercase-invalid-name", 1.0));
        tmpPref.setContent(2.0);
        Assertions.assertEquals(2.0, tmpPref.getContent(), 0.0);
        Assertions.assertThrows(IllegalArgumentException.class, () -> tmpPref.setContent(Double.POSITIVE_INFINITY));
        Assertions.assertThrows(IllegalArgumentException.class, () -> tmpPref.setContent(Double.NaN));
        tmpPref.setContent("1.0");
        Assertions.assertEquals(1.0, tmpPref.getContent(), 0.0);
        Assertions.assertThrows(IllegalArgumentException.class, () -> tmpPref.setContent("NaN"));
        Assertions.assertTrue(SingleNumberPreference.isValidContent(2.0));
        Assertions.assertFalse(SingleNumberPreference.isValidContent(Double.POSITIVE_INFINITY));
        Assertions.assertFalse(SingleNumberPreference.isValidContent(Double.NaN));
        Assertions.assertTrue(SingleNumberPreference.isValidContent("2.0"));
        Assertions.assertFalse(SingleNumberPreference.isValidContent(null));
        Assertions.assertFalse(SingleNumberPreference.isValidContent(" "));
        Assertions.assertFalse(SingleNumberPreference.isValidContent("NaN"));
        Assertions.assertFalse(SingleNumberPreference.isValidContent("Infinity"));
        SingleNumberPreference tmpCopy = tmpPref.copy();
        Assertions.assertEquals(tmpPref, tmpCopy);
        Assertions.assertEquals(tmpPref.getGUID(), tmpCopy.getGUID());
        Assertions.assertNotSame(tmpPref, tmpCopy);
    }
    //
    /**
     * Tests setContent, static isValidContent, copy(), and the invalid-content constructor guard of class
     * SingleTermPreference with real valid and pattern-failing input.
     *
     * @throws Exception if anything goes wrong
     */
    @Test
    public void testSingleTermPreferenceValidationAndCopy() throws Exception {
        SingleTermPreference tmpPref = new SingleTermPreference("Welcoming message", "Welcome to MORTAR");
        tmpPref.setContent("Hello world");
        Assertions.assertEquals("Hello world", tmpPref.getContent());
        Assertions.assertThrows(IllegalArgumentException.class, () -> tmpPref.setContent("invalid#term"));
        Assertions.assertTrue(SingleTermPreference.isValidContent("A valid term"));
        //a tilde is legal: it occurs in 8.3 short paths on Windows and in home-relative paths on Linux, and the
        //recent-directory setting is persisted through this preference. The short-path case is what made this matter:
        //a Windows account whose name exceeds eight characters is abbreviated this way in the temporary directory path.
        Assertions.assertTrue(SingleTermPreference.isValidContent("C:\\Users\\LONGUS~1\\AppData\\Local\\Temp"));
        Assertions.assertTrue(SingleTermPreference.isValidContent("~/molecules"));
        Assertions.assertDoesNotThrow(() -> new SingleTermPreference("Tilde path", "~/molecules"));
        Assertions.assertFalse(SingleTermPreference.isValidContent(null));
        Assertions.assertFalse(SingleTermPreference.isValidContent("invalid#term"));
        Assertions.assertThrows(IllegalArgumentException.class,
                () -> new SingleTermPreference("Bad content", "invalid#term"));
        SingleTermPreference tmpCopy = tmpPref.copy();
        Assertions.assertEquals(tmpPref, tmpCopy);
        Assertions.assertEquals(tmpPref.getGUID(), tmpCopy.getGUID());
        Assertions.assertNotSame(tmpPref, tmpCopy);
    }
    //
    /**
     * Tests setContent, copy(), and the invalid-name constructor guard of class BooleanPreference.
     *
     * @throws Exception if anything goes wrong
     */
    @Test
    public void testBooleanPreferenceValidationAndCopy() throws Exception {
        BooleanPreference tmpPref = new BooleanPreference("MORTAR is cool", false);
        tmpPref.setContent(true);
        Assertions.assertTrue(tmpPref.getContent());
        Assertions.assertThrows(IllegalArgumentException.class, () -> new BooleanPreference("lowercase-invalid-name", true));
        BooleanPreference tmpCopy = tmpPref.copy();
        Assertions.assertEquals(tmpPref, tmpCopy);
        Assertions.assertEquals(tmpPref.getGUID(), tmpCopy.getGUID());
        Assertions.assertNotSame(tmpPref, tmpCopy);
    }
    //
    /**
     * Tests the BasePreference equals (self / null / different-class / equal), compareTo (real compare + null
     * NullPointerException), and isValidName(null) branches through a concrete subclass.
     *
     * @throws Exception if anything goes wrong
     */
    @Test
    public void testBasePreferenceEqualsCompareToAndIsValidName() throws Exception {
        BooleanPreference tmpPref = new BooleanPreference("Base preference setting", true);
        Assertions.assertEquals(tmpPref, tmpPref);
        Assertions.assertNotEquals(tmpPref, null);
        Assertions.assertNotEquals(tmpPref, "a non-preference object");
        BooleanPreference tmpEqual = tmpPref.copy();
        Assertions.assertEquals(tmpPref, tmpEqual);
        Assertions.assertEquals(0, tmpPref.compareTo(tmpEqual));
        BooleanPreference tmpOther = new BooleanPreference("Another setting", false);
        Assertions.assertNotEquals(0, tmpPref.compareTo(tmpOther));
        Assertions.assertThrows(NullPointerException.class, () -> tmpPref.compareTo(null));
        Assertions.assertFalse(BasePreference.isValidName(null));
        Assertions.assertTrue(BasePreference.isValidName("Valid name"));
    }
    //
    /**
     * Tests the PreferenceFactory unknown-type-name IllegalArgumentException branch and its reflectively-invoked private
     * parameter-less constructor.
     *
     * @throws Exception if anything goes wrong
     */
    @Test
    public void testPreferenceFactoryUnknownTypeAndPrivateConstructor() throws Exception {
        BufferedReader tmpReader = new BufferedReader(new StringReader("irrelevant"));
        Assertions.assertThrows(IllegalArgumentException.class,
                () -> PreferenceFactory.reinitializePreference("NOT_A_PREFERENCE_TYPE", tmpReader));
        tmpReader.close();
        Constructor<PreferenceFactory> tmpCtor = PreferenceFactory.class.getDeclaredConstructor();
        tmpCtor.setAccessible(true);
        Assertions.assertNotNull(tmpCtor.newInstance());
    }
    //
    /**
     * Tests basic functionalities of given preference object, like management of public properties and persistence.
     */
    private void testPreferenceBasics(IPreference aPreference) throws Exception {
        Assertions.assertDoesNotThrow(aPreference::getType);
        Assertions.assertDoesNotThrow(aPreference::getContentRepresentative);
        Assertions.assertDoesNotThrow(aPreference::getGUID);
        Assertions.assertDoesNotThrow(aPreference::getName);
        Assertions.assertDoesNotThrow(aPreference::toString);

        String tmpDir = FileUtil.getAppDirPath() + File.separatorChar + "Test";
        (new File(tmpDir)).mkdirs();
        File tmpPreferenceFile = new File(tmpDir + File.separatorChar + "preference.txt");
        PrintWriter tmpWriter = new PrintWriter(tmpPreferenceFile);
        tmpWriter.println(aPreference.getType());
        aPreference.writeRepresentation(tmpWriter);
        tmpWriter.flush();
        BufferedReader tmpReader = new BufferedReader(new FileReader(tmpPreferenceFile));
        IPreference tmpPreference = PreferenceFactory.reinitializePreference(tmpReader.readLine(), tmpReader);
        tmpWriter.close();
        tmpReader.close();
        Assertions.assertEquals(aPreference.getContentRepresentative(), tmpPreference.getContentRepresentative());
        Assertions.assertEquals(aPreference.getName(), tmpPreference.getName());
        Assertions.assertEquals(aPreference.getGUID(), tmpPreference.getGUID());
        Assertions.assertEquals(aPreference.toString(), tmpPreference.toString());
    }
}

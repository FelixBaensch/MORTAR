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
        Locale.setDefault(new Locale("en", "GB"));
        Configuration.getInstance();
    }
    //
    /**
     * Tests basic functionalities of class BooleanPreference.
     *
     * @throws Exception
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
     * @throws Exception
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
     * @throws Exception
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
     * @throws Exception
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
     * @throws Exception
     */
    @Test
    public void testSingleTermPreference() throws Exception {
        SingleTermPreference tmpPreference = new SingleTermPreference("Welcoming message", "Welcome to MORTAR");
        this.testPreferenceBasics(tmpPreference);
    }
    //
    /**
     * Tests basic functionalities of given preference object, like management of public properties and persistence.
     */
    private void testPreferenceBasics(IPreference aPreference) throws Exception {
        System.out.println();
        System.out.println(aPreference.getType());
        System.out.println(aPreference.getContentRepresentative());
        System.out.println(aPreference.getGUID());
        System.out.println(aPreference.getName());
        System.out.println(aPreference.toString());

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
        Assertions.assertTrue(aPreference.getContentRepresentative().equals(tmpPreference.getContentRepresentative()));
        Assertions.assertEquals(aPreference.getName(), tmpPreference.getName());
        Assertions.assertEquals(aPreference.getGUID(), tmpPreference.getGUID());
        Assertions.assertEquals(aPreference.toString(), tmpPreference.toString());
    }
}

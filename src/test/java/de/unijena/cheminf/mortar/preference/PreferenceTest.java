/*
 * MORTAR - MOlecule fRagmenTAtion fRamework
 * Copyright (C) 2023  Felix Baensch, Jonas Schaub (felix.baensch@w-hs.de, jonas.schaub@uni-jena.de)
 *
 * Source code is available at <https://github.com/FelixBaensch/MORTAR>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package de.unijena.cheminf.mortar.preference;

/**
 * TODO:
 * - implement comparing test (?)
 * - implement preference name test (?)
 */

import de.unijena.cheminf.mortar.model.util.FileUtil;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.awt.Color;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.PrintWriter;

/**
 * Test class for preferences.
 *
 * @author Jonas Schaub
 * @version 1.0.0.0
 */
public class PreferenceTest {

    /**
     * Constructor (empty)
     */
    public PreferenceTest() {
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
